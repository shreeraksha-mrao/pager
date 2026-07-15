package com.pager.service;

import com.pager.dto.PageEventDto;
import com.pager.entity.*;
import com.pager.repository.PageEventRepository;
import com.pager.repository.TaskSchedulerStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Owns the lifecycle of a single page/notification: creation, sending,
 * accept/decline/miss handling, and scheduling the cooldown that gates when
 * the same task is eligible for its next page.
 */
@Service
public class PageEventService {

    private static final Logger log = LoggerFactory.getLogger(PageEventService.class);

    private final PageEventRepository pageEventRepository;
    private final TaskSchedulerStateRepository schedulerStateRepository;
    private final StudySessionService studySessionService;
    private final TelegramMessagingService telegramMessagingService;

    @Value("${pager.scheduler.decline-cooldown-min-minutes:15}")
    private int cooldownMinMinutes;

    @Value("${pager.scheduler.decline-cooldown-max-minutes:90}")
    private int cooldownMaxMinutes;

    public PageEventService(PageEventRepository pageEventRepository,
                             TaskSchedulerStateRepository schedulerStateRepository,
                             StudySessionService studySessionService,
                             TelegramMessagingService telegramMessagingService) {
        this.pageEventRepository = pageEventRepository;
        this.schedulerStateRepository = schedulerStateRepository;
        this.studySessionService = studySessionService;
        this.telegramMessagingService = telegramMessagingService;
    }

    @Transactional
    public PageEvent createAndSend(Task task, int durationMinutes, int debtSnapshotMinutes, Long replacementOfEventId, String chatId) {
        PageEvent event = new PageEvent();
        event.setTask(task);
        event.setDurationMinutes(durationMinutes);
        event.setDebtSnapshotMinutes(debtSnapshotMinutes);
        event.setReplacementOfEventId(replacementOfEventId);
        event.setTelegramChatId(chatId);
        event.setSentAt(LocalDateTime.now());
        event.setStatus(PageStatus.PENDING);
        event = pageEventRepository.save(event);

        String messageId = telegramMessagingService.sendPageMessage(event);
        event.setTelegramMessageId(messageId);
        event = pageEventRepository.save(event);

        touchLastPaged(task.getId());
        return event;
    }

    @Transactional
    public PageEvent accept(Long pageEventId) {
        PageEvent event = getOrThrow(pageEventId);
        if (event.getStatus() != PageStatus.PENDING) {
            return event;
        }
        event.setStatus(PageStatus.ACCEPTED);
        event.setRespondedAt(LocalDateTime.now());
        event = pageEventRepository.save(event);

        studySessionService.startSession(event);
        resetConsecutiveDeclines(event.getTask().getId());
        telegramMessagingService.editToAccepted(event);
        return event;
    }

    @Transactional
    public PageEvent decline(Long pageEventId, String reason, DeclineReasonType type) {
        PageEvent event = getOrThrow(pageEventId);
        if (event.getStatus() != PageStatus.PENDING) {
            return event;
        }
        event.setStatus(PageStatus.DECLINED);
        event.setRespondedAt(LocalDateTime.now());
        event.setDeclineReason(reason);
        event.setDeclineReasonType(type);
        event = pageEventRepository.save(event);

        applyCooldown(event.getTask().getId(), true);
        telegramMessagingService.editToDeclined(event);
        return event;
    }

    @Transactional
    public void expireMissed(Long pageEventId) {
        PageEvent event = getOrThrow(pageEventId);
        if (event.getStatus() != PageStatus.PENDING) {
            return;
        }
        event.setStatus(PageStatus.MISSED);
        event.setRespondedAt(LocalDateTime.now());
        pageEventRepository.save(event);

        applyCooldown(event.getTask().getId(), false);
        telegramMessagingService.editToMissed(event);
    }

    @Transactional(readOnly = true)
    public List<PageEvent> findPending() {
        return pageEventRepository.findByStatus(PageStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public boolean hasPendingForTask(Long taskId) {
        return !pageEventRepository.findByTask_IdAndStatus(taskId, PageStatus.PENDING).isEmpty();
    }

    @Transactional(readOnly = true)
    public Optional<PageEvent> findLastSent() {
        return pageEventRepository.findTopByOrderBySentAtDesc();
    }

    @Transactional(readOnly = true)
    public PageEvent getOrThrow(Long id) {
        return pageEventRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("PageEvent not found: " + id));
    }

    /** Fetches a page event with its task eagerly loaded, safe to serialize outside a Hibernate session. */
    @Transactional(readOnly = true)
    public PageEventDto getDto(Long id) {
        return PageEventDto.from(pageEventRepository.findByIdWithTask(id)
                .orElseThrow(() -> new NoSuchElementException("PageEvent not found: " + id)));
    }

    private void touchLastPaged(Long taskId) {
        TaskSchedulerState state = schedulerStateRepository.findById(taskId).orElse(null);
        if (state == null) return;
        state.setLastPagedAt(LocalDateTime.now());
        schedulerStateRepository.save(state);
    }

    private void resetConsecutiveDeclines(Long taskId) {
        TaskSchedulerState state = schedulerStateRepository.findById(taskId).orElse(null);
        if (state == null) return;
        state.setConsecutiveDeclines(0);
        schedulerStateRepository.save(state);
    }

    /** Shrinks cooldown as consecutive declines rise, never below the configured floor. */
    private void applyCooldown(Long taskId, boolean isDecline) {
        TaskSchedulerState state = schedulerStateRepository.findById(taskId).orElse(null);
        if (state == null) {
            log.warn("No scheduler state found for task {}, skipping cooldown", taskId);
            return;
        }
        int consecutive = isDecline ? state.getConsecutiveDeclines() + 1 : state.getConsecutiveDeclines();
        if (isDecline) {
            state.setConsecutiveDeclines(consecutive);
        }
        int range = Math.max(cooldownMaxMinutes - cooldownMinMinutes, 0);
        int reduction = Math.min(consecutive * 10, range);
        int cooldownMinutes = Math.max(cooldownMaxMinutes - reduction, cooldownMinMinutes);
        state.setNextEligibleAt(LocalDateTime.now().plusMinutes(cooldownMinutes));
        schedulerStateRepository.save(state);
    }
}
