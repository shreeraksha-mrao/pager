package com.pager.entity;

import jakarta.persistence.*;

/**
 * Frozen effectiveDebt for one task, captured the instant a PagerPausePeriod begins.
 * While that pause is active, DebtCalculatorService.effectiveDebt returns this value
 * verbatim instead of recomputing live — so debt neither increases nor decreases at
 * all while paging is disabled. Normal live computation resumes the moment the pause ends.
 */
@Entity
@Table(name = "pager_pause_debt_snapshot")
public class PagerPauseDebtSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pause_period_id", nullable = false)
    private Long pausePeriodId;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "frozen_debt_minutes", nullable = false)
    private Integer frozenDebtMinutes;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPausePeriodId() { return pausePeriodId; }
    public void setPausePeriodId(Long pausePeriodId) { this.pausePeriodId = pausePeriodId; }

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }

    public Integer getFrozenDebtMinutes() { return frozenDebtMinutes; }
    public void setFrozenDebtMinutes(Integer frozenDebtMinutes) { this.frozenDebtMinutes = frozenDebtMinutes; }
}
