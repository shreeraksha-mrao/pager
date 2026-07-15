package com.pager.controller;

import com.pager.dto.ManualSessionRequest;
import com.pager.dto.StudySessionDto;
import com.pager.repository.StudySessionRepository;
import com.pager.service.StudySessionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private final StudySessionRepository studySessionRepository;
    private final StudySessionService studySessionService;

    public SessionController(StudySessionRepository studySessionRepository, StudySessionService studySessionService) {
        this.studySessionRepository = studySessionRepository;
        this.studySessionService = studySessionService;
    }

    @GetMapping
    public Page<StudySessionDto> findAll(@RequestParam(required = false) Long taskId,
                                          @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size);
        var result = taskId != null
                ? studySessionRepository.findByTaskWithTask(taskId, pageable)
                : studySessionRepository.findAllWithTask(pageable);
        return result.map(StudySessionDto::from);
    }

    @GetMapping("/{id}")
    public StudySessionDto findById(@PathVariable Long id) {
        return studySessionService.getDto(id);
    }

    @PostMapping("/{id}/complete")
    public StudySessionDto complete(@PathVariable Long id) {
        studySessionService.confirmCompleted(id);
        return studySessionService.getDto(id);
    }

    @PostMapping("/{id}/extend")
    public StudySessionDto extend(@PathVariable Long id, @RequestParam(defaultValue = "30") int minutes) {
        studySessionService.extend(id, minutes);
        return studySessionService.getDto(id);
    }

    @PostMapping("/{id}/abandon")
    public StudySessionDto abandon(@PathVariable Long id) {
        studySessionService.abandon(id);
        return studySessionService.getDto(id);
    }

    /** Manually log a completed session not tied to any page (e.g. finished the task independently). */
    @PostMapping("/manual")
    @ResponseStatus(HttpStatus.CREATED)
    public StudySessionDto logManual(@Valid @RequestBody ManualSessionRequest request) {
        var session = studySessionService.logManualSession(request);
        return studySessionService.getDto(session.getId());
    }
}
