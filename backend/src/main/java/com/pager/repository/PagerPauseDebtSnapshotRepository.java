package com.pager.repository;

import com.pager.entity.PagerPauseDebtSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PagerPauseDebtSnapshotRepository extends JpaRepository<PagerPauseDebtSnapshot, Long> {
    Optional<PagerPauseDebtSnapshot> findByPausePeriodIdAndTaskId(Long pausePeriodId, Long taskId);
}
