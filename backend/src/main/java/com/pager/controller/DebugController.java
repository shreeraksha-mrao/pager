package com.pager.controller;

import com.pager.dto.PageEventDto;
import com.pager.dto.StudySessionDto;
import com.pager.entity.PageEvent;
import com.pager.entity.SessionStatus;
import com.pager.entity.StudySession;
import com.pager.entity.Task;
import com.pager.repository.StudySessionRepository;
import com.pager.repository.TaskRepository;
import com.pager.scheduler.FollowUpCheckJob;
import com.pager.scheduler.PagingTickJob;
import com.pager.service.StudySessionService;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Development-only helper for exercising the paging flow on demand, without
 * waiting for a real scheduler tick outside working hours. Reuses the exact
 * same task-selection and page-sending code paths as {@link PagingTickJob},
 * so a debug-triggered page is indistinguishable from a scheduler-generated
 * one (same PageEventService/Telegram flow, same debt-weighted selection).
 *
 * Only registered when the "dev" Spring profile is active
 * (e.g. run with {@code --spring.profiles.active=dev}).
 */
@Profile("dev")
@RestController
@RequestMapping("/api/debug")
public class DebugController {

    private final PagingTickJob pagingTickJob;
    private final FollowUpCheckJob followUpCheckJob;
    private final TaskRepository taskRepository;
    private final StudySessionRepository studySessionRepository;
    private final StudySessionService studySessionService;

    public DebugController(PagingTickJob pagingTickJob,
                            FollowUpCheckJob followUpCheckJob,
                            TaskRepository taskRepository,
                            StudySessionRepository studySessionRepository,
                            StudySessionService studySessionService) {
        this.pagingTickJob = pagingTickJob;
        this.followUpCheckJob = followUpCheckJob;
        this.taskRepository = taskRepository;
        this.studySessionRepository = studySessionRepository;
        this.studySessionService = studySessionService;
    }

    @PostMapping("/trigger-page")
    public PageEventDto triggerPage(@RequestBody(required = false) Map<String, Object> body) {
        Task task = resolveTask(body);
        PageEvent event = pagingTickJob.sendPage(task);
        return PageEventDto.from(event);
    }

    /**
     * Forces the completion check-in ("Did you complete your session? Completed / Extend / Abandoned")
     * to be (re)sent right now for a session, bypassing the normal end-time/reminder-interval wait.
     * Reuses FollowUpCheckJob.sendCheckinNow so it exercises the exact same send+persist path (including
     * recording the Telegram message id for later editing) as a real scheduler-driven check-in.
     * If no sessionId is given, picks the most recently started IN_PROGRESS or AWAITING_CONFIRMATION session.
     */
    @PostMapping("/trigger-followup-checkin")
    public StudySessionDto triggerFollowUpCheckin(@RequestBody(required = false) Map<String, Object> body) {
        Long sessionId = extractId(body, "sessionId");
        if (sessionId == null) {
            sessionId = resolveMostRecentActiveSessionId();
        }
        followUpCheckJob.sendCheckinNow(sessionId);
        return studySessionService.getDto(sessionId);
    }

    private Long resolveMostRecentActiveSessionId() {
        List<StudySession> candidates = studySessionRepository.findByStatus(SessionStatus.IN_PROGRESS);
        if (candidates.isEmpty()) {
            candidates = studySessionRepository.findByStatus(SessionStatus.AWAITING_CONFIRMATION);
        }
        return candidates.stream()
                .max(Comparator.comparing(StudySession::getStartTime))
                .map(StudySession::getId)
                .orElseThrow(() -> new NoSuchElementException(
                        "No IN_PROGRESS or AWAITING_CONFIRMATION session found — accept a page first, or pass an explicit sessionId"));
    }

    private Task resolveTask(Map<String, Object> body) {
        Long taskId = extractId(body, "taskId");
        if (taskId != null) {
            return taskRepository.findById(taskId)
                    .orElseThrow(() -> new NoSuchElementException("Task not found: " + taskId));
        }
        return pagingTickJob.selectTaskForPaging()
                .orElseThrow(() -> new IllegalStateException(
                        "No eligible task available for paging (paging is disabled, an IN_PROGRESS study session is active, all tasks on cooldown, or none active)"));
    }

    private Long extractId(Map<String, Object> body, String key) {
        if (body == null) return null;
        Object raw = body.get(key);
        if (raw == null) return null;
        if (raw instanceof Number n) return n.longValue();
        return Long.parseLong(raw.toString());
    }
}

