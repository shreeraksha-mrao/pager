package com.pager.repository;

import com.pager.entity.TaskDebtLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TaskDebtLedgerRepository extends JpaRepository<TaskDebtLedger, Long> {
    Optional<TaskDebtLedger> findTopByTask_IdOrderByLedgerDateDesc(Long taskId);
    List<TaskDebtLedger> findByTask_IdAndLedgerDateGreaterThanEqualOrderByLedgerDateAsc(Long taskId, LocalDate from);
    List<TaskDebtLedger> findByTask_IdOrderByLedgerDateAsc(Long taskId);
    Optional<TaskDebtLedger> findByTask_IdAndLedgerDate(Long taskId, LocalDate ledgerDate);
    long countByTask_IdAndShortfallMinutesGreaterThan(Long taskId, Integer minutes);
}
