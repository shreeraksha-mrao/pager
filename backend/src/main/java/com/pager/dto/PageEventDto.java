package com.pager.dto;

import com.pager.entity.DeclineReasonType;
import com.pager.entity.PageEvent;
import com.pager.entity.PageStatus;

import java.time.LocalDateTime;

public record PageEventDto(
        Long id,
        Long taskId,
        String taskName,
        String taskColor,
        String taskIcon,
        PageStatus status,
        Integer durationMinutes,
        Integer debtSnapshotMinutes,
        LocalDateTime sentAt,
        LocalDateTime respondedAt,
        String declineReason,
        DeclineReasonType declineReasonType,
        Long replacementOfEventId,
        LocalDateTime createdAt
) {
    public static PageEventDto from(PageEvent e) {
        return new PageEventDto(
                e.getId(),
                e.getTask().getId(),
                e.getTask().getName(),
                e.getTask().getColor(),
                e.getTask().getIcon(),
                e.getStatus(),
                e.getDurationMinutes(),
                e.getDebtSnapshotMinutes(),
                e.getSentAt(),
                e.getRespondedAt(),
                e.getDeclineReason(),
                e.getDeclineReasonType(),
                e.getReplacementOfEventId(),
                e.getCreatedAt()
        );
    }
}
