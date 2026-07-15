package com.pager.controller;

import com.pager.dto.PagerStatusDto;
import com.pager.entity.Task;
import com.pager.repository.TaskRepository;
import com.pager.service.DebtCalculatorService;
import com.pager.service.PagerPauseService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global "disable pages" toggle for days the user can't take pages at all.
 * While paused, the scheduler sends no pages and every task's debt is frozen
 * exactly at its pause-start value (see PagerPauseService / DebtCalculatorService.effectiveDebt) —
 * it does not rise or fall at all until resumed.
 */
@RestController
@RequestMapping("/api/pager")
public class PagerController {

    private final PagerPauseService pagerPauseService;
    private final TaskRepository taskRepository;
    private final DebtCalculatorService debtCalculatorService;

    public PagerController(PagerPauseService pagerPauseService,
                            TaskRepository taskRepository,
                            DebtCalculatorService debtCalculatorService) {
        this.pagerPauseService = pagerPauseService;
        this.taskRepository = taskRepository;
        this.debtCalculatorService = debtCalculatorService;
    }

    @GetMapping("/status")
    public PagerStatusDto status() {
        return pagerPauseService.status();
    }

    @PostMapping("/pause")
    public PagerStatusDto pause(@RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        // Snapshot each task's live debt BEFORE the pause takes effect, so it's the true
        // unfrozen value that gets held constant for the duration of the pause.
        Map<Long, Integer> currentDebtByTaskId = taskRepository.findAll().stream()
                .collect(Collectors.toMap(Task::getId, debtCalculatorService::effectiveDebt));
        return pagerPauseService.pause(reason, currentDebtByTaskId);
    }

    @PostMapping("/resume")
    public PagerStatusDto resume() {
        return pagerPauseService.resume();
    }
}

