package com.pager.dto;

import com.pager.entity.SessionSource;
import com.pager.entity.SessionStatus;
import com.pager.entity.StudySession;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record StudySessionDto(
        Long id,
        Long taskId,
        String taskName,
        String taskColor,
        String taskIcon,
        Long pageEventId,
        SessionStatus status,
        SessionSource source,
        String notes,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Integer durationMinutes,
        LocalDateTime confirmationRequestedAt,
        LocalDateTime confirmedAt,
        Integer reminderCount,
        LocalDate sessionDate,
        LocalDateTime createdAt
) {
    public static StudySessionDto from(StudySession s) {
        return new StudySessionDto(
                s.getId(),
                s.getTask().getId(),
                s.getTask().getName(),
                s.getTask().getColor(),
                s.getTask().getIcon(),
                s.getPageEvent() != null ? s.getPageEvent().getId() : null,
                s.getStatus(),
                s.getSource(),
                s.getNotes(),
                s.getStartTime(),
                s.getEndTime(),
                s.getDurationMinutes(),
                s.getConfirmationRequestedAt(),
                s.getConfirmedAt(),
                s.getReminderCount(),
                s.getSessionDate(),
                s.getCreatedAt()
        );
    }
}
