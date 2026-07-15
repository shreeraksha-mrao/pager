package com.pager.controller;

import com.pager.dto.ReorderRequest;
import com.pager.dto.TaskDto;
import com.pager.dto.TaskUpsertRequest;
import com.pager.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public List<TaskDto> findAll() {
        return taskService.findAll();
    }

    @GetMapping("/{id}")
    public TaskDto findById(@PathVariable Long id) {
        return taskService.findById(id);
    }

    @PostMapping
    public TaskDto create(@Valid @RequestBody TaskUpsertRequest request) {
        return taskService.create(request);
    }

    @PutMapping("/{id}")
    public TaskDto update(@PathVariable Long id, @Valid @RequestBody TaskUpsertRequest request) {
        return taskService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    public TaskDto setStatus(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        return taskService.setActive(id, Boolean.TRUE.equals(body.get("active")));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        taskService.delete(id);
    }

    @PutMapping("/reorder")
    public void reorder(@RequestBody ReorderRequest request) {
        taskService.reorder(request);
    }
}
