package com.pager.service;

import com.pager.dto.ManualSessionRequest;
import com.pager.entity.PageEvent;
import com.pager.entity.SessionSource;
import com.pager.entity.SessionStatus;
import com.pager.entity.StudySession;
import com.pager.entity.Task;
import com.pager.repository.StudySessionRepository;
import com.pager.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Owns the study-session lifecycle: IN_PROGRESS -> AWAITING_CONFIRMATION ->
 * COMPLETED/ABANDONED. Time is only ever credited (duration_minutes set,
 * status COMPLETED) on an explicit user confirmation — never automatically.
 */
@Service
public class StudySessionService {

    private static final Logger log = LoggerFactory.getLogger(StudySessionService.class);

    private final StudySessionRepository studySessionRepository;
    private final TaskRepository taskRepository;
    private final PagerDayService pagerDayService;

    public StudySessionService(StudySessionRepository studySessionRepository, TaskRepository taskRepository,
                                PagerDayService pagerDayService) {
        this.studySessionRepository = studySessionRepository;
        this.taskRepository = taskRepository;
        this.pagerDayService = pagerDayService;
    }

    @Transactional
    public StudySession startSession(PageEvent pageEvent) {
        StudySession session = new StudySession();
        session.setTask(pageEvent.getTask());
        session.setPageEvent(pageEvent);
        session.setStatus(SessionStatus.IN_PROGRESS);
        LocalDateTime start = LocalDateTime.now();
        session.setStartTime(start);
        session.setEndTime(start.plusMinutes(pageEvent.getDurationMinutes()));
        session.setSessionDate(pagerDayService.pagerDateFor(start));
        return studySessionRepository.save(session);
    }

    /** Called by FollowUpCheckJob when a session's planned end_time is reached and unconfirmed. */
    @Transactional
    public StudySession requestConfirmation(Long sessionId) {
        StudySession session = getOrThrow(sessionId);
        if (session.getStatus() == SessionStatus.IN_PROGRESS) {
            session.setStatus(SessionStatus.AWAITING_CONFIRMATION);
            session.setConfirmationRequestedAt(LocalDateTime.now());
        }
        session.setReminderCount(session.getReminderCount() + 1);
        return studySessionRepository.save(session);
    }

    /** Persists which Telegram message the check-in was sent as, so a later response can edit it for visible confirmation. */
    @Transactional
    public StudySession recordCheckinMessage(Long sessionId, String chatId, String messageId) {
        StudySession session = getOrThrow(sessionId);
        session.setCheckinChatId(chatId);
        session.setCheckinMessageId(messageId);
        return studySessionRepository.save(session);
    }

    /** Explicit user confirmation only — this is the sole path that credits duration/debt. */
    @Transactional
    public StudySession confirmCompleted(Long sessionId) {
        StudySession session = getOrThrow(sessionId);
        int elapsedMinutes = (int) java.time.Duration.between(session.getStartTime(), LocalDateTime.now()).toMinutes();
        session.setStatus(SessionStatus.COMPLETED);
        session.setDurationMinutes(Math.max(elapsedMinutes, 0));
        session.setConfirmedAt(LocalDateTime.now());
        return studySessionRepository.save(session);
    }

    /**
     * Logs work completed outside the page flow entirely (e.g. after declining a page but
     * finishing the task independently). Immediately COMPLETED with source MANUAL — counts
     * toward today's completed minutes and effective debt exactly like a confirmed page session,
     * since DebtCalculatorService sums all COMPLETED sessions for the task/date regardless of source.
     */
    @Transactional
    public StudySession logManualSession(ManualSessionRequest request) {
        Task task = taskRepository.findById(request.taskId)
                .orElseThrow(() -> new NoSuchElementException("Task not found: " + request.taskId));
        LocalDate date = request.sessionDate != null ? request.sessionDate : pagerDayService.currentPagerDate();
        int minutes = request.durationMinutes;

        StudySession session = new StudySession();
        session.setTask(task);
        session.setSource(SessionSource.MANUAL);
        session.setStatus(SessionStatus.COMPLETED);
        LocalDateTime now = LocalDateTime.now();
        session.setStartTime(now.minusMinutes(minutes));
        session.setEndTime(now);
        session.setDurationMinutes(minutes);
        session.setConfirmedAt(now);
        session.setSessionDate(date);
        session.setNotes(request.notes);

        StudySession saved = studySessionRepository.save(session);
        log.info("Manual session logged: task={} minutes={} date={}", task.getName(), minutes, date);
        return saved;
    }

    @Transactional
    public StudySession extend(Long sessionId, int extraMinutes) {
        StudySession session = getOrThrow(sessionId);
        session.setStatus(SessionStatus.IN_PROGRESS);
        session.setEndTime(session.getEndTime().plusMinutes(extraMinutes));
        return studySessionRepository.save(session);
    }

    @Transactional
    public StudySession abandon(Long sessionId) {
        StudySession session = getOrThrow(sessionId);
        session.setStatus(SessionStatus.ABANDONED);
        session.setDurationMinutes(0);
        session.setConfirmedAt(LocalDateTime.now());
        return studySessionRepository.save(session);
    }

    @Transactional(readOnly = true)
    public List<StudySession> findAwaitingConfirmation() {
        return studySessionRepository.findByStatus(SessionStatus.AWAITING_CONFIRMATION);
    }

    @Transactional(readOnly = true)
    public List<StudySession> findInProgress() {
        return studySessionRepository.findByStatus(SessionStatus.IN_PROGRESS);
    }

    @Transactional(readOnly = true)
    public StudySession getOrThrow(Long id) {
        return studySessionRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("StudySession not found: " + id));
    }

    /** Fetches a session with its task eagerly loaded, safe to serialize outside a Hibernate session. */
    @Transactional(readOnly = true)
    public com.pager.dto.StudySessionDto getDto(Long id) {
        return com.pager.dto.StudySessionDto.from(studySessionRepository.findByIdWithTask(id)
                .orElseThrow(() -> new NoSuchElementException("StudySession not found: " + id)));
    }

    /** Fetches a session with its task eagerly loaded — for callers (e.g. Telegram edit messages) that need
     * to read task fields (like getTask().getName()) outside the original persistence-context boundary. */
    @Transactional(readOnly = true)
    public StudySession getWithTask(Long id) {
        return studySessionRepository.findByIdWithTask(id)
                .orElseThrow(() -> new NoSuchElementException("StudySession not found: " + id));
    }
}
