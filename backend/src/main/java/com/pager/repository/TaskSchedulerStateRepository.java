package com.pager.repository;

import com.pager.entity.TaskSchedulerState;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskSchedulerStateRepository extends JpaRepository<TaskSchedulerState, Long> {
}
