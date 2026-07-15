package com.pager.scheduler;

import com.pager.config.SchedulerProperties;
import com.pager.entity.PageEvent;
import com.pager.entity.SessionStatus;
import com.pager.entity.Task;
import com.pager.entity.TaskSchedulerState;
import com.pager.repository.StudySessionRepository;
import com.pager.repository.TaskRepository;
import com.pager.repository.TaskSchedulerStateRepository;
import com.pager.service.DebtCalculatorService;
import com.pager.service.PageEventService;
import com.pager.service.PagerPauseService;
import com.pager.service.TelegramMessagingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Core debt-driven scheduler tick. Runs every {@code tick-interval-ms}. The
 * primary objective is ensuring the configured daily-minimum minutes of
 * *confirmed* work get done; page frequency/urgency rises as that minimum
 * stays unmet and as persistent cross-day debt (see DebtCalculatorService)
 * remains outstanding. There is deliberately no fixed page count — everything
 * emerges from effectiveDebt, time-of-day, and cooldown state.
 */
@Component
public class PagingTickJob {

    private static final Logger log = LoggerFactory.getLogger(PagingTickJob.class);

    private final TaskRepository taskRepository;
    private final TaskSchedulerStateRepository schedulerStateRepository;
    private final StudySessionRepository studySessionRepository;
    private final DebtCalculatorService debtCalculatorService;
    private final PageEventService pageEventService;
    private final TelegramMessagingService telegramMessagingService;
    private final PagerPauseService pagerPauseService;
    private final SchedulerProperties props;

    public PagingTickJob(TaskRepository taskRepository,
                          TaskSchedulerStateRepository schedulerStateRepository,
                          StudySessionRepository studySessionRepository,
                          DebtCalculatorService debtCalculatorService,
                          PageEventService pageEventService,
                          TelegramMessagingService telegramMessagingService,
                          PagerPauseService pagerPauseService,
                          SchedulerProperties props) {
        this.taskRepository = taskRepository;
        this.schedulerStateRepository = schedulerStateRepository;
        this.studySessionRepository = studySessionRepository;
        this.debtCalculatorService = debtCalculatorService;
        this.pageEventService = pageEventService;
        this.telegramMessagingService = telegramMessagingService;
        this.pagerPauseService = pagerPauseService;
        this.props = props;
    }

    @Scheduled(fixedRateString = "${pager.scheduler.tick-interval-ms:300000}")
    @Transactional
    public void tick() {
        LocalDateTime now = LocalDateTime.now();
        LocalTime time = now.toLocalTime();

        if (pagerPauseService.isPaused()) {
            return; // user has manually disabled paging (e.g. a day off) — no pages, no debt accrual
        }

        if (isWithinWorkingHours(time)) {
            return; // never page during protected working hours
        }

        if (hasActiveStudySession()) {
            return; // user is actively IN_PROGRESS on a session — never interrupt, regardless of debt/urgency
        }

        List<Task> activeTasks = taskRepository.findByActiveTrueOrderBySortOrderAsc();
        if (activeTasks.isEmpty()) {
            return;
        }

        double timeBucketMultiplier = timeBucketMultiplier(time);
        double urgencyMultiplier = urgencyMultiplier(activeTasks, time);
        double tickProbability = Math.min(
                props.getBaseTickProbability() * timeBucketMultiplier * urgencyMultiplier,
                props.getMaxTickProbability());

        if (!globalGapSatisfied(now)) {
            return;
        }

        if (ThreadLocalRandom.current().nextDouble() > tickProbability) {
            return;
        }

        Task chosen = selectTaskForPaging().orElse(null);
        if (chosen == null) {
            return;
        }

        PageEvent event = sendPage(chosen);
        log.info("Paging tick triggered for task '{}' (debt={} min, prob={})",
                chosen.getName(), event.getDebtSnapshotMinutes(), String.format("%.3f", tickProbability));
    }

    /**
     * Debt-weighted task selection among currently eligible active tasks (not on
     * cooldown, and no IN_PROGRESS study session anywhere). Shared by the scheduled
     * tick and the dev-only debug trigger endpoint so both exercise identical
     * selection logic.
     */
    @Transactional(readOnly = true)
    public Optional<Task> selectTaskForPaging() {
        if (pagerPauseService.isPaused()) {
            return Optional.empty(); // paging manually disabled
        }
        if (hasActiveStudySession()) {
            return Optional.empty(); // never page while an IN_PROGRESS session exists
        }
        List<Task> activeTasks = taskRepository.findByActiveTrueOrderBySortOrderAsc();
        List<Task> eligible = activeTasks.stream().filter(this::isEligible).toList();
        if (eligible.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(weightedPick(eligible));
    }

    /**
     * Global page-suppression gate: only an IN_PROGRESS study session blocks new pages.
     * PENDING/DECLINED/MISSED page events and AWAITING_CONFIRMATION sessions never block —
     * AWAITING_CONFIRMATION means the scheduled work period already ended, so new pages are fine.
     */
    private boolean hasActiveStudySession() {
        return studySessionRepository.existsByStatus(SessionStatus.IN_PROGRESS);
    }

    /**
     * Creates and sends a page for the given task through the standard
     * PageEventService/Telegram flow. Shared by the scheduled tick and the
     * dev-only debug trigger endpoint.
     */
    @Transactional
    public PageEvent sendPage(Task task) {
        int debtSnapshot = debtCalculatorService.effectiveDebt(task);
        String chatId = telegramMessagingService.resolveChatId().orElse(null);
        return pageEventService.createAndSend(
                task, props.getDefaultPageDurationMinutes(), debtSnapshot, null, chatId);
    }

    private boolean isWithinWorkingHours(LocalTime time) {
        return !time.isBefore(props.getWorkingHoursStart()) && time.isBefore(props.getWorkingHoursEnd());
    }

    private double timeBucketMultiplier(LocalTime time) {
        if (isWithin(time, props.getNightLowWindowStart(), props.getNightLowWindowEnd())) {
            return props.getNightLowProbability();
        }
        if (isWithin(time, props.getNightMediumWindowStart(), props.getNightMediumWindowEnd())) {
            return props.getNightMediumProbability();
        }
        return 1.0;
    }

    private boolean isWithin(LocalTime time, LocalTime start, LocalTime end) {
        if (start.isBefore(end)) {
            return !time.isBefore(start) && time.isBefore(end);
        }
        // window wraps midnight
        return !time.isBefore(start) || time.isBefore(end);
    }

    /**
     * Rises as (a) the daily minimum objective remains unmet while the
     * eligible paging window narrows, and (b) total persistent debt across
     * tasks grows.
     */
    private double urgencyMultiplier(List<Task> tasks, LocalTime time) {
        int totalCompletedToday = tasks.stream().mapToInt(debtCalculatorService::todayCompletedMinutes).sum();
        int minutesUnmet = Math.max(0, props.getDailyMinimumTargetMinutes() - totalCompletedToday);
        double unmetRatio = minutesUnmet / (double) Math.max(props.getDailyMinimumTargetMinutes(), 1);

        int totalDebt = tasks.stream().mapToInt(debtCalculatorService::effectiveDebt).sum();
        double debtRatio = Math.min(totalDebt / 300.0, 2.0); // normalize, cap contribution

        double dayProgress = dayWindowProgress(time); // 0 (window just opened) -> 1 (window nearly closed)

        return 1.0 + (unmetRatio * dayProgress * 1.5) + (debtRatio * 0.75);
    }

    /** Rough progress through the non-working paging window (17:00 -> next 11:00), for urgency ramp-up. */
    private double dayWindowProgress(LocalTime time) {
        LocalTime windowStart = props.getWorkingHoursEnd(); // e.g. 17:00
        LocalTime windowEnd = props.getWorkingHoursStart();  // e.g. 11:00 next day
        long totalMinutes = minutesBetweenWrapping(windowStart, windowEnd);
        long elapsed = minutesBetweenWrapping(windowStart, time);
        if (totalMinutes <= 0) return 0.5;
        return Math.min(1.0, elapsed / (double) totalMinutes);
    }

    private long minutesBetweenWrapping(LocalTime from, LocalTime to) {
        long fromSec = from.toSecondOfDay();
        long toSec = to.toSecondOfDay();
        long diff = toSec - fromSec;
        if (diff < 0) diff += 24 * 3600;
        return diff / 60;
    }

    private boolean isEligible(Task task) {
        Optional<TaskSchedulerState> state = schedulerStateRepository.findById(task.getId());
        if (state.isEmpty() || state.get().getNextEligibleAt() == null) {
            return true;
        }
        return !LocalDateTime.now().isBefore(state.get().getNextEligibleAt());
    }

    private boolean globalGapSatisfied(LocalDateTime now) {
        return pageEventService.findLastSent()
                .map(last -> java.time.Duration.between(last.getSentAt(), now).toMinutes() >= props.getMinGapBetweenPagesMinutes())
                .orElse(true);
    }

    private Task weightedPick(List<Task> tasks) {
        double totalWeight = tasks.stream().mapToDouble(debtCalculatorService::taskWeight).sum();
        double roll = ThreadLocalRandom.current().nextDouble() * totalWeight;
        double cumulative = 0;
        for (Task task : tasks) {
            cumulative += debtCalculatorService.taskWeight(task);
            if (roll <= cumulative) {
                return task;
            }
        }
        return tasks.get(tasks.size() - 1);
    }
}
