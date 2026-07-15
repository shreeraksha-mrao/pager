package com.pager.repository;

import com.pager.entity.PagerPausePeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PagerPausePeriodRepository extends JpaRepository<PagerPausePeriod, Long> {
    Optional<PagerPausePeriod> findByEndedAtIsNull();
}
