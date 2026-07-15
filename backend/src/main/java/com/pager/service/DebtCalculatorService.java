package com.pager.service;

import com.pager.entity.SessionStatus;
import com.pager.entity.Task;
import com.pager.entity.TaskDebtLedger;
import com.pager.repository.StudySessionRepository;
import com.pager.repository.TaskDebtLedgerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Computes the persistent, day-crossing debt figures that drive both the
 * paging scheduler's task-selection weighting and the debt-dashboard
 * analytics. See plan.md section 5 for the full model.
 */
@Service
public class DebtCalculatorService {

    private static final double FLOOR_WEIGHT = 5.0;
    private static final double DEBT_EXPONENT = 1.3;

    private final StudySessionRepository studySessionRepository;
    private final TaskDebtLedgerRepository debtLedgerRepository;
    private final PagerPauseService pagerPauseService;
    private final PagerDayService pagerDayService;

    public DebtCalculatorService(StudySessionRepository studySessionRepository,
                                  TaskDebtLedgerRepository debtLedgerRepository,
                                  PagerPauseService pagerPauseService,
                                  PagerDayService pagerDayService) {
        this.studySessionRepository = studySessionRepository;
        this.debtLedgerRepository = debtLedgerRepository;
        this.pagerPauseService = pagerPauseService;
        this.pagerDayService = pagerDayService;
    }

    @Transactional(readOnly = true)
    public int todayCompletedMinutes(Task task) {
        return todayCompletedMinutes(task, pagerDayService.currentPagerDate());
    }

    @Transactional(readOnly = true)
    public int todayCompletedMinutes(Task task, LocalDate date) {
        List<com.pager.entity.StudySession> sessions =
                studySessionRepository.findByTask_IdAndSessionDateAndStatus(task.getId(), date, SessionStatus.COMPLETED);
        return sessions.stream().mapToInt(s -> s.getDurationMinutes() != null ? s.getDurationMinutes() : 0).sum();
    }

    /**
     * Live shortfall for today: simply target minus completed minutes so far, with
     * NO paused-minutes discount. Pausing must never permanently reduce today's target —
     * only shortfallForDate (used solely by the nightly rollover for already-closed past
     * days) applies the paused-minutes discount, and only to days that are fully in the past.
     */
    @Transactional(readOnly = true)
    public int todayShortfall(Task task) {
        return Math.max(0, task.getDailyTargetMinutes() - todayCompletedMinutes(task));
    }

    /**
     * Shortfall for an arbitrary (typically past/closed) date, reduced minute-for-minute by
     * any time paging was paused that day (see PagerPauseService) — so a fully paused day
     * contributes zero shortfall/debt, and a partially paused day only expects the non-paused
     * portion of the daily target. Used ONLY by DebtRolloverJob when closing out a finished
     * calendar day — never by the live "today" figure (see todayShortfall above).
     */
    @Transactional(readOnly = true)
    public int shortfallForDate(Task task, LocalDate date) {
        int pausedMinutes = pagerPauseService.pausedMinutesOn(date);
        int effectiveTarget = Math.max(0, task.getDailyTargetMinutes() - pausedMinutes);
        return Math.max(0, effectiveTarget - todayCompletedMinutes(task, date));
    }

    /** Cumulative debt frozen as of the last nightly rollover (prior days' unresolved debt). */
    @Transactional(readOnly = true)
    public int cumulativeDebt(Task task) {
        return debtLedgerRepository.findTopByTask_IdOrderByLedgerDateDesc(task.getId())
                .map(TaskDebtLedger::getCumulativeDebtMinutes)
                .orElse(0);
    }

    /** effectiveDebt = cumulativeDebt (prior days) + todayShortfall (so far today) — unless paging
     * is currently paused, in which case debt is held exactly at its pause-time snapshot value
     * (see PagerPauseService), neither rising nor falling until resume. */
    @Transactional(readOnly = true)
    public int effectiveDebt(Task task) {
        return pagerPauseService.frozenDebtFor(task.getId())
                .orElseGet(() -> cumulativeDebt(task) + todayShortfall(task));
    }

    /** taskWeight = (effectiveDebt^1.3 + floor) * priorityWeight — never fully zero. */
    @Transactional(readOnly = true)
    public double taskWeight(Task task) {
        double debt = effectiveDebt(task);
        double weight = Math.pow(debt, DEBT_EXPONENT) + FLOOR_WEIGHT;
        return weight * (task.getPriorityWeight() != null ? task.getPriorityWeight() : 1.0);
    }

    @Transactional(readOnly = true)
    public long daysBehindTarget(Task task) {
        return debtLedgerRepository.countByTask_IdAndShortfallMinutesGreaterThan(task.getId(), 0);
    }
}

