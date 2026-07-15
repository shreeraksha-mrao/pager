package com.pager.repository;

import com.pager.entity.SessionStatus;
import com.pager.entity.StudySession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StudySessionRepository extends JpaRepository<StudySession, Long> {
    List<StudySession> findByTask_IdAndSessionDateAndStatus(Long taskId, LocalDate date, SessionStatus status);
    List<StudySession> findByTask_IdAndSessionDateBetweenAndStatus(Long taskId, LocalDate from, LocalDate to, SessionStatus status);
    List<StudySession> findByStatus(SessionStatus status);
    List<StudySession> findBySessionDateAndStatus(LocalDate date, SessionStatus status);
    List<StudySession> findByTask_IdOrderBySessionDateAsc(Long taskId);
    boolean existsByStatus(SessionStatus status);

    @Query("select s from StudySession s join fetch s.task order by s.startTime desc")
    Page<StudySession> findAllWithTask(Pageable pageable);

    @Query("select s from StudySession s join fetch s.task where s.task.id = :taskId order by s.startTime desc")
    Page<StudySession> findByTaskWithTask(Long taskId, Pageable pageable);

    @Query("select s from StudySession s join fetch s.task where s.id = :id")
    Optional<StudySession> findByIdWithTask(Long id);
}
