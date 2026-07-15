package com.pager.service;

import com.pager.dto.PagerStatusDto;
import com.pager.entity.PagerPauseDebtSnapshot;
import com.pager.entity.PagerPausePeriod;
import com.pager.repository.PagerPauseDebtSnapshotRepository;
import com.pager.repository.PagerPausePeriodRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Global "disable pages" toggle for days the user can't take pages at all.
 * While paused: the scheduler never selects/sends a page (see
 * PagingTickJob.selectTaskForPaging), and every task's debt is held exactly
 * where it was the instant pausing began (see frozenDebtFor / the
 * PagerPauseDebtSnapshot rows created in pause()) — it neither rises nor
 * falls until resume() is called. Re-enabling resumes fully live computation
 * immediately; historical pause intervals also permanently exempt whichever
 * calendar days they overlap from ever entering cross-day cumulative debt
 * (see DebtCalculatorService.shortfallForDate, used by the nightly rollover).
 */
@Service
public class PagerPauseService {

    private static final Logger log = LoggerFactory.getLogger(PagerPauseService.class);

    private final PagerPausePeriodRepository repository;
    private final PagerPauseDebtSnapshotRepository snapshotRepository;

    public PagerPauseService(PagerPausePeriodRepository repository,
                              PagerPauseDebtSnapshotRepository snapshotRepository) {
        this.repository = repository;
        this.snapshotRepository = snapshotRepository;
    }

    @Transactional(readOnly = true)
    public boolean isPaused() {
        return repository.findByEndedAtIsNull().isPresent();
    }

    @Transactional(readOnly = true)
    public PagerStatusDto status() {
        return repository.findByEndedAtIsNull()
                .map(p -> new PagerStatusDto(true, p.getStartedAt(), p.getReason()))
                .orElseGet(() -> new PagerStatusDto(false, null, null));
    }

    /**
     * Idempotent: pausing while already paused just returns the existing (unchanged) pause,
     * ignoring the new snapshot. currentEffectiveDebtByTaskId must be computed by the caller
     * (DebtCalculatorService, via the controller) BEFORE calling this — i.e. while still
     * genuinely live/unpaused — so it captures the true debt at the exact moment of pausing.
     */
    @Transactional
    public PagerStatusDto pause(String reason, Map<Long, Integer> currentEffectiveDebtByTaskId) {
        Optional<PagerPausePeriod> existing = repository.findByEndedAtIsNull();
        if (existing.isPresent()) {
            return status();
        }
        PagerPausePeriod period = new PagerPausePeriod();
        period.setStartedAt(LocalDateTime.now());
        period.setReason(reason);
        repository.save(period);

        currentEffectiveDebtByTaskId.forEach((taskId, debtMinutes) -> {
            PagerPauseDebtSnapshot snapshot = new PagerPauseDebtSnapshot();
            snapshot.setPausePeriodId(period.getId());
            snapshot.setTaskId(taskId);
            snapshot.setFrozenDebtMinutes(debtMinutes);
            snapshotRepository.save(snapshot);
        });

        log.info("Paging paused (reason: {}), froze debt for {} tasks",
                reason != null ? reason : "none given", currentEffectiveDebtByTaskId.size());
        return status();
    }

    /** Idempotent: resuming while not paused is a no-op. */
    @Transactional
    public PagerStatusDto resume() {
        repository.findByEndedAtIsNull().ifPresent(period -> {
            period.setEndedAt(LocalDateTime.now());
            repository.save(period);
            log.info("Paging resumed (was paused since {})", period.getStartedAt());
        });
        return status();
    }

    /** The frozen debt value for a task, if paging is currently paused and a snapshot exists for it. */
    @Transactional(readOnly = true)
    public Optional<Integer> frozenDebtFor(Long taskId) {
        return repository.findByEndedAtIsNull()
                .flatMap(period -> snapshotRepository.findByPausePeriodIdAndTaskId(period.getId(), taskId))
                .map(PagerPauseDebtSnapshot::getFrozenDebtMinutes);
    }

    /**
     * Total minutes of pause overlapping the given calendar date, clamped to
     * that day's [00:00, 24:00) window. Used only for historical (past-date)
     * debt-rollover exemption — NOT for live "today" freezing, which instead
     * uses the exact frozen snapshot via frozenDebtFor while a pause is open.
     */
    @Transactional(readOnly = true)
    public int pausedMinutesOn(LocalDate date) {
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = dayStart.plusDays(1);
        LocalDateTime now = LocalDateTime.now();

        List<PagerPausePeriod> all = repository.findAll();
        int totalMinutes = 0;
        for (PagerPausePeriod period : all) {
            LocalDateTime start = period.getStartedAt();
            LocalDateTime end = period.getEndedAt() != null ? period.getEndedAt() : now;
            LocalDateTime overlapStart = start.isAfter(dayStart) ? start : dayStart;
            LocalDateTime overlapEnd = end.isBefore(dayEnd) ? end : dayEnd;
            if (overlapEnd.isAfter(overlapStart)) {
                totalMinutes += (int) java.time.Duration.between(overlapStart, overlapEnd).toMinutes();
            }
        }
        return totalMinutes;
    }
}

