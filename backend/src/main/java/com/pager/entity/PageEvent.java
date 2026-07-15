package com.pager.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "page_events")
public class PageEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PageStatus status = PageStatus.PENDING;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Column(name = "debt_snapshot_minutes", nullable = false)
    private Integer debtSnapshotMinutes = 0;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt = LocalDateTime.now();

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @Column(name = "decline_reason")
    private String declineReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "decline_reason_type")
    private DeclineReasonType declineReasonType;

    @Column(name = "telegram_message_id")
    private String telegramMessageId;

    @Column(name = "telegram_chat_id")
    private String telegramChatId;

    @Column(name = "replacement_of_event_id")
    private Long replacementOfEventId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Task getTask() { return task; }
    public void setTask(Task task) { this.task = task; }

    public PageStatus getStatus() { return status; }
    public void setStatus(PageStatus status) { this.status = status; }

    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }

    public Integer getDebtSnapshotMinutes() { return debtSnapshotMinutes; }
    public void setDebtSnapshotMinutes(Integer debtSnapshotMinutes) { this.debtSnapshotMinutes = debtSnapshotMinutes; }

    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }

    public LocalDateTime getRespondedAt() { return respondedAt; }
    public void setRespondedAt(LocalDateTime respondedAt) { this.respondedAt = respondedAt; }

    public String getDeclineReason() { return declineReason; }
    public void setDeclineReason(String declineReason) { this.declineReason = declineReason; }

    public DeclineReasonType getDeclineReasonType() { return declineReasonType; }
    public void setDeclineReasonType(DeclineReasonType declineReasonType) { this.declineReasonType = declineReasonType; }

    public String getTelegramMessageId() { return telegramMessageId; }
    public void setTelegramMessageId(String telegramMessageId) { this.telegramMessageId = telegramMessageId; }

    public String getTelegramChatId() { return telegramChatId; }
    public void setTelegramChatId(String telegramChatId) { this.telegramChatId = telegramChatId; }

    public Long getReplacementOfEventId() { return replacementOfEventId; }
    public void setReplacementOfEventId(Long replacementOfEventId) { this.replacementOfEventId = replacementOfEventId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
