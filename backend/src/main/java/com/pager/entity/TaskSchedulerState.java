package com.pager.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "task_scheduler_state")
public class TaskSchedulerState {

    @Id
    @Column(name = "task_id")
    private Long taskId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "task_id")
    private Task task;

    @Column(name = "next_eligible_at")
    private LocalDateTime nextEligibleAt;

    @Column(name = "last_paged_at")
    private LocalDateTime lastPagedAt;

    @Column(name = "consecutive_declines", nullable = false)
    private Integer consecutiveDeclines = 0;

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }

    public Task getTask() { return task; }
    public void setTask(Task task) { this.task = task; }

    public LocalDateTime getNextEligibleAt() { return nextEligibleAt; }
    public void setNextEligibleAt(LocalDateTime nextEligibleAt) { this.nextEligibleAt = nextEligibleAt; }

    public LocalDateTime getLastPagedAt() { return lastPagedAt; }
    public void setLastPagedAt(LocalDateTime lastPagedAt) { this.lastPagedAt = lastPagedAt; }

    public Integer getConsecutiveDeclines() { return consecutiveDeclines; }
    public void setConsecutiveDeclines(Integer consecutiveDeclines) { this.consecutiveDeclines = consecutiveDeclines; }
}
