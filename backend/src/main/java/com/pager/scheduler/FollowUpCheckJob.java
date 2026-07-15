package com.pager.scheduler;

import com.pager.config.SchedulerProperties;
import com.pager.entity.PageEvent;
import com.pager.entity.StudySession;
import com.pager.repository.StudySessionRepository;
import com.pager.service.StudySessionService;
import com.pager.service.TelegramMessagingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Sends the explicit completion check-in when a session's planned end_time
 * is reached, and keeps re-sending reminders (capped backoff) while the
 * session sits AWAITING_CONFIRMATION. Time/debt is only ever credited when
 * the user taps "Completed" via a Telegram callback (see StudySessionService)
 * — this job never marks anything COMPLETED on its own.
 */
@Component
public class FollowUpCheckJob {

    private static final Logger log = LoggerFactory.getLogger(FollowUpCheckJob.class);

    private final StudySessionRepository studySessionRepository;
    private final StudySessionService studySessionService;
    private final TelegramMessagingService telegramMessagingService;
    private final SchedulerProperties props;

    public FollowUpCheckJob(StudySessionRepository studySessionRepository,
                             StudySessionService studySessionService,
                             TelegramMessagingService telegramMessagingService,
                             SchedulerProperties props) {
        this.studySessionRepository = studySessionRepository;
        this.studySessionService = studySessionService;
        this.telegramMessagingService = telegramMessagingService;
        this.props = props;
    }

    @Scheduled(fixedRate = 60_000) // every 1 minute
    @Transactional
    public void checkSessions() {
        LocalDateTime now = LocalDateTime.now();

        for (StudySession session : studySessionService.findInProgress()) {
            if (!now.isBefore(session.getEndTime())) {
                studySessionService.requestConfirmation(session.getId());
                sendCheckin(session);
            }
        }

        for (StudySession session : studySessionService.findAwaitingConfirmation()) {
            if (session.getReminderCount() >= props.getConfirmationReminderMaxCount()) {
                continue; // stop nagging after the cap; session stays AWAITING_CONFIRMATION indefinitely
            }
            LocalDateTime lastPrompt = session.getConfirmationRequestedAt() != null
                    ? session.getConfirmationRequestedAt() : session.getEndTime();
            long minutesSincePrompt = Duration.between(lastPrompt, now).toMinutes();
            if (minutesSincePrompt >= props.getConfirmationReminderIntervalMinutes()) {
                studySessionService.requestConfirmation(session.getId());
                sendCheckin(session);
            }
        }
    }

    /**
     * Sends (or re-sends) the completion check-in for a session right now, bypassing the
     * normal end-time/reminder-interval wait. Shared by the real 1-minute tick and the
     * dev-only debug trigger endpoint, so a debug-triggered check-in exercises the exact
     * same send+persist path as a scheduler-generated one.
     */
    @Transactional
    public void sendCheckinNow(Long sessionId) {
        studySessionService.requestConfirmation(sessionId);
        sendCheckin(studySessionService.getOrThrow(sessionId));
    }

    private void sendCheckin(StudySession session) {
        String chatId = resolveChatId(session);
        if (chatId == null) return;
        String messageId = telegramMessagingService.sendFollowUpCheckin(session, chatId);
        if (messageId != null) {
            studySessionService.recordCheckinMessage(session.getId(), chatId, messageId);
        }
    }

    private String resolveChatId(StudySession session) {
        PageEvent pageEvent = session.getPageEvent();
        if (pageEvent != null && pageEvent.getTelegramChatId() != null) {
            return pageEvent.getTelegramChatId();
        }
        return telegramMessagingService.resolveChatId().orElse(null);
    }
}
