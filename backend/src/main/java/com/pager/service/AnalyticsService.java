package com.pager.service;

import com.pager.dto.AnalyticsSummaryDto;
import com.pager.dto.DebtDashboardDto;
import com.pager.entity.*;
import com.pager.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private final TaskRepository taskRepository;
    private final StudySessionRepository studySessionRepository;
    private final PageEventRepository pageEventRepository;
    private final TaskDebtLedgerRepository debtLedgerRepository;
    private final DebtCalculatorService debtCalculatorService;
    private final PagerDayService pagerDayService;

    public AnalyticsService(TaskRepository taskRepository,
                             StudySessionRepository studySessionRepository,
                             PageEventRepository pageEventRepository,
                             TaskDebtLedgerRepository debtLedgerRepository,
                             DebtCalculatorService debtCalculatorService,
                             PagerDayService pagerDayService) {
        this.taskRepository = taskRepository;
        this.studySessionRepository = studySessionRepository;
        this.pageEventRepository = pageEventRepository;
        this.debtLedgerRepository = debtLedgerRepository;
        this.debtCalculatorService = debtCalculatorService;
        this.pagerDayService = pagerDayService;
    }

    /** range: "today" (since the pager-day cutoff, default 2:00 AM local time) or "lifetime" (all recorded history). */
    @Transactional(readOnly = true)
    public AnalyticsSummaryDto getSummary(String range) {
        boolean todayOnly = "today".equalsIgnoreCase(range);
        List<Task> tasks = taskRepository.findAllByOrderBySortOrderAsc();
        LocalDate today = pagerDayService.currentPagerDate();
        LocalDate weekStart = today.minusDays(6);

        AnalyticsSummaryDto dto = new AnalyticsSummaryDto();
        dto.range = todayOnly ? "today" : "lifetime";
        dto.todayHoursByTask = minutesByTask(tasks, today, today);
        dto.weekHoursByTask = minutesByTask(tasks, weekStart, today);
        dto.lifetimeHoursByTask = lifetimeMinutesByTask(tasks);

        List<PageEvent> pageEvents = todayOnly
                ? pageEventRepository.findBySentAtBetween(pagerDayService.startOfPagerDay(today), LocalDateTime.now())
                : pageEventRepository.findAll();

        long total = pageEvents.size();
        long pending = pageEvents.stream().filter(p -> p.getStatus() == PageStatus.PENDING).count();
        long accepted = pageEvents.stream().filter(p -> p.getStatus() == PageStatus.ACCEPTED).count();
        long declined = pageEvents.stream().filter(p -> p.getStatus() == PageStatus.DECLINED).count();
        long missed = pageEvents.stream().filter(p -> p.getStatus() == PageStatus.MISSED).count();

        dto.pagesReceived = total;
        dto.pagesAccepted = accepted;
        dto.pagesDeclined = declined;
        dto.pagesMissed = missed;

        long nonPending = total - pending;
        dto.acceptanceRate = nonPending > 0 ? (double) accepted / nonPending : 0.0;

        List<StudySession> sessionsInScope = todayOnly
                ? studySessionRepository.findAll().stream().filter(s -> today.equals(s.getSessionDate())).toList()
                : studySessionRepository.findAll();

        List<StudySession> completedInScope = sessionsInScope.stream()
                .filter(s -> s.getStatus() == SessionStatus.COMPLETED).toList();
        List<StudySession> abandonedFromPagesInScope = sessionsInScope.stream()
                .filter(s -> s.getStatus() == SessionStatus.ABANDONED && s.getSource() == SessionSource.PAGE_ACCEPTED)
                .toList();
        long completedFromPages = completedInScope.stream()
                .filter(s -> s.getSource() == SessionSource.PAGE_ACCEPTED).count();

        dto.acceptedCompletedPages = completedFromPages;
        dto.acceptedAbandonedPages = abandonedFromPagesInScope.size();

        dto.completionRate = accepted > 0 ? (double) completedInScope.size() / accepted : 0.0;

        dto.productivityDebtMinutes = todayOnly
                ? tasks.stream().mapToInt(debtCalculatorService::todayShortfall).sum()
                : tasks.stream().mapToInt(debtCalculatorService::effectiveDebt).sum();

        dto.topDeclineReasons = topDeclineReasons(pageEvents);

        int fromPages = completedInScope.stream()
                .filter(s -> s.getSource() == SessionSource.PAGE_ACCEPTED)
                .mapToInt(s -> s.getDurationMinutes() != null ? s.getDurationMinutes() : 0)
                .sum();
        int fromManual = completedInScope.stream()
                .filter(s -> s.getSource() == SessionSource.MANUAL)
                .mapToInt(s -> s.getDurationMinutes() != null ? s.getDurationMinutes() : 0)
                .sum();
        dto.minutesFromPages = fromPages;
        dto.minutesFromManual = fromManual;
        dto.totalProductiveMinutes = fromPages + fromManual;

        return dto;
    }

    /** Breaks lifetime completed minutes down by how they were recorded — via an accepted page vs manually logged. */
    @Transactional(readOnly = true)
    public Map<String, Object> getSessionSourceTotals() {
        List<StudySession> completed = studySessionRepository.findByStatus(SessionStatus.COMPLETED);
        int fromPages = completed.stream()
                .filter(s -> s.getSource() == SessionSource.PAGE_ACCEPTED)
                .mapToInt(s -> s.getDurationMinutes() != null ? s.getDurationMinutes() : 0)
                .sum();
        int fromManual = completed.stream()
                .filter(s -> s.getSource() == SessionSource.MANUAL)
                .mapToInt(s -> s.getDurationMinutes() != null ? s.getDurationMinutes() : 0)
                .sum();
        return Map.of(
                "minutesFromPages", fromPages,
                "minutesFromManual", fromManual,
                "totalProductiveMinutes", fromPages + fromManual);
    }

    @Transactional(readOnly = true)
    public List<AnalyticsSummaryDto.TaskMinutesDto> getHours(String range) {
        List<Task> tasks = taskRepository.findAllByOrderBySortOrderAsc();
        LocalDate today = pagerDayService.currentPagerDate();
        return switch (range) {
            case "week" -> minutesByTask(tasks, today.minusDays(6), today);
            case "lifetime" -> lifetimeMinutesByTask(tasks);
            default -> minutesByTask(tasks, today, today);
        };
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getPagesStats() {
        long total = pageEventRepository.count();
        long pending = pageEventRepository.countByStatus(PageStatus.PENDING);
        long accepted = pageEventRepository.countByStatus(PageStatus.ACCEPTED);
        long declined = pageEventRepository.countByStatus(PageStatus.DECLINED);
        long missed = pageEventRepository.countByStatus(PageStatus.MISSED);
        long nonPending = total - pending;
        double acceptanceRate = nonPending > 0 ? (double) accepted / nonPending : 0.0;
        long completedSessions = studySessionRepository.findByStatus(SessionStatus.COMPLETED).size();
        double completionRate = accepted > 0 ? (double) completedSessions / accepted : 0.0;
        return Map.of(
                "received", total, "accepted", accepted, "declined", declined, "missed", missed,
                "acceptanceRate", acceptanceRate, "completionRate", completionRate);
    }

    @Transactional(readOnly = true)
    public List<AnalyticsSummaryDto.DeclineReasonCountDto> getDeclineReasons() {
        return topDeclineReasons(pageEventRepository.findByStatus(PageStatus.DECLINED));
    }

    @Transactional(readOnly = true)
    public DebtDashboardDto getDebtDashboard() {
        List<Task> tasks = taskRepository.findAllByOrderBySortOrderAsc();
        DebtDashboardDto dto = new DebtDashboardDto();
        dto.tasks = new ArrayList<>();
        int total = 0;
        for (Task task : tasks) {
            int effectiveDebt = debtCalculatorService.effectiveDebt(task);
            total += effectiveDebt;
            long daysBehind = debtCalculatorService.daysBehindTarget(task);
            List<TaskDebtLedger> ledger = debtLedgerRepository.findByTask_IdOrderByLedgerDateAsc(task.getId());
            List<DebtDashboardDto.TrendEntry> trend = ledger.stream()
                    .map(l -> new DebtDashboardDto.TrendEntry(
                            l.getLedgerDate().toString(), l.getCumulativeDebtMinutes(), l.getShortfallMinutes()))
                    .collect(Collectors.toList());
            dto.tasks.add(new DebtDashboardDto.TaskDebtDto(
                    task.getId(), task.getName(), task.getColor(), task.getIcon(),
                    effectiveDebt, daysBehind, trend));
        }
        dto.totalDebtMinutes = total;
        return dto;
    }

    // ---- helpers ----

    private List<AnalyticsSummaryDto.TaskMinutesDto> minutesByTask(List<Task> tasks, LocalDate from, LocalDate to) {
        List<AnalyticsSummaryDto.TaskMinutesDto> result = new ArrayList<>();
        for (Task task : tasks) {
            int minutes = studySessionRepository
                    .findByTask_IdAndSessionDateBetweenAndStatus(task.getId(), from, to, SessionStatus.COMPLETED)
                    .stream().mapToInt(s -> s.getDurationMinutes() != null ? s.getDurationMinutes() : 0).sum();
            result.add(new AnalyticsSummaryDto.TaskMinutesDto(task.getId(), task.getName(), task.getColor(), task.getIcon(), minutes));
        }
        return result;
    }

    private List<AnalyticsSummaryDto.TaskMinutesDto> lifetimeMinutesByTask(List<Task> tasks) {
        List<AnalyticsSummaryDto.TaskMinutesDto> result = new ArrayList<>();
        for (Task task : tasks) {
            int minutes = studySessionRepository.findByTask_IdOrderBySessionDateAsc(task.getId()).stream()
                    .filter(s -> s.getStatus() == SessionStatus.COMPLETED)
                    .mapToInt(s -> s.getDurationMinutes() != null ? s.getDurationMinutes() : 0).sum();
            result.add(new AnalyticsSummaryDto.TaskMinutesDto(task.getId(), task.getName(), task.getColor(), task.getIcon(), minutes));
        }
        return result;
    }

    private List<AnalyticsSummaryDto.DeclineReasonCountDto> topDeclineReasons(List<PageEvent> events) {
        return events.stream()
                .filter(p -> p.getStatus() == PageStatus.DECLINED)
                .map(PageEvent::getDeclineReason)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(r -> r, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(e -> new AnalyticsSummaryDto.DeclineReasonCountDto(e.getKey(), e.getValue()))
                .limit(10)
                .toList();
    }
}

