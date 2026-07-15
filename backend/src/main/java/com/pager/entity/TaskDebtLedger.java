package com.pager.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "task_debt_ledger")
public class TaskDebtLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @Column(name = "ledger_date", nullable = false)
    private LocalDate ledgerDate;

    @Column(name = "target_minutes", nullable = false)
    private Integer targetMinutes;

    @Column(name = "completed_minutes", nullable = false)
    private Integer completedMinutes;

    @Column(name = "shortfall_minutes", nullable = false)
    private Integer shortfallMinutes;

    @Column(name = "cumulative_debt_minutes", nullable = false)
    private Integer cumulativeDebtMinutes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Task getTask() { return task; }
    public void setTask(Task task) { this.task = task; }

    public LocalDate getLedgerDate() { return ledgerDate; }
    public void setLedgerDate(LocalDate ledgerDate) { this.ledgerDate = ledgerDate; }

    public Integer getTargetMinutes() { return targetMinutes; }
    public void setTargetMinutes(Integer targetMinutes) { this.targetMinutes = targetMinutes; }

    public Integer getCompletedMinutes() { return completedMinutes; }
    public void setCompletedMinutes(Integer completedMinutes) { this.completedMinutes = completedMinutes; }

    public Integer getShortfallMinutes() { return shortfallMinutes; }
    public void setShortfallMinutes(Integer shortfallMinutes) { this.shortfallMinutes = shortfallMinutes; }

    public Integer getCumulativeDebtMinutes() { return cumulativeDebtMinutes; }
    public void setCumulativeDebtMinutes(Integer cumulativeDebtMinutes) { this.cumulativeDebtMinutes = cumulativeDebtMinutes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
