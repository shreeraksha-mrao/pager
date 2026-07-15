package com.pager.repository;

import com.pager.entity.PageEvent;
import com.pager.entity.PageStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PageEventRepository extends JpaRepository<PageEvent, Long> {
    List<PageEvent> findByStatus(PageStatus status);
    List<PageEvent> findByTask_IdAndStatus(Long taskId, PageStatus status);
    Optional<PageEvent> findTopByOrderBySentAtDesc();
    List<PageEvent> findBySentAtBetween(LocalDateTime from, LocalDateTime to);
    long countByStatus(PageStatus status);
    List<PageEvent> findByTelegramChatIdAndStatus(String chatId, PageStatus status);

    @Query("select p from PageEvent p join fetch p.task order by p.sentAt desc")
    Page<PageEvent> findAllWithTask(Pageable pageable);

    @Query("select p from PageEvent p join fetch p.task where p.status = :status order by p.sentAt desc")
    Page<PageEvent> findByStatusWithTask(PageStatus status, Pageable pageable);

    @Query("select p from PageEvent p join fetch p.task where p.task.id = :taskId order by p.sentAt desc")
    Page<PageEvent> findByTaskWithTask(Long taskId, Pageable pageable);

    @Query("select p from PageEvent p join fetch p.task where p.status = :status and p.task.id = :taskId order by p.sentAt desc")
    Page<PageEvent> findByStatusAndTaskWithTask(PageStatus status, Long taskId, Pageable pageable);

    @Query("select p from PageEvent p join fetch p.task where p.id = :id")
    Optional<PageEvent> findByIdWithTask(Long id);
}
