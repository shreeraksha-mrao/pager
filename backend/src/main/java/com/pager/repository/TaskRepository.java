package com.pager.repository;

import com.pager.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByActiveTrueOrderBySortOrderAsc();
    List<Task> findAllByOrderBySortOrderAsc();
    Optional<Task> findByName(String name);
}
