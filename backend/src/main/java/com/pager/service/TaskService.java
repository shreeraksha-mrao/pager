package com.pager.service;

import com.pager.dto.ReorderRequest;
import com.pager.dto.TaskDto;
import com.pager.dto.TaskUpsertRequest;
import com.pager.entity.Task;
import com.pager.entity.TaskSchedulerState;
import com.pager.repository.TaskRepository;
import com.pager.repository.TaskSchedulerStateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskSchedulerStateRepository schedulerStateRepository;

    public TaskService(TaskRepository taskRepository, TaskSchedulerStateRepository schedulerStateRepository) {
        this.taskRepository = taskRepository;
        this.schedulerStateRepository = schedulerStateRepository;
    }

    public List<TaskDto> findAll() {
        return taskRepository.findAllByOrderBySortOrderAsc().stream().map(this::toDto).toList();
    }

    public List<Task> findAllActive() {
        return taskRepository.findByActiveTrueOrderBySortOrderAsc();
    }

    public TaskDto findById(Long id) {
        return toDto(getOrThrow(id));
    }

    @Transactional
    public TaskDto create(TaskUpsertRequest req) {
        Task task = new Task();
        applyRequest(task, req);
        Integer maxOrder = taskRepository.findAllByOrderBySortOrderAsc().stream()
                .map(Task::getSortOrder).max(Integer::compareTo).orElse(-1);
        if (req.sortOrder == null || req.sortOrder == 0) {
            task.setSortOrder(maxOrder + 1);
        }
        task = taskRepository.save(task);

        TaskSchedulerState state = new TaskSchedulerState();
        state.setTask(task);
        schedulerStateRepository.save(state);

        return toDto(task);
    }

    @Transactional
    public TaskDto update(Long id, TaskUpsertRequest req) {
        Task task = getOrThrow(id);
        applyRequest(task, req);
        return toDto(taskRepository.save(task));
    }

    @Transactional
    public TaskDto setActive(Long id, boolean active) {
        Task task = getOrThrow(id);
        task.setActive(active);
        return toDto(taskRepository.save(task));
    }

    @Transactional
    public void delete(Long id) {
        Task task = getOrThrow(id);
        taskRepository.delete(task);
    }

    @Transactional
    public void reorder(ReorderRequest request) {
        for (ReorderRequest.Item item : request.items) {
            Task task = getOrThrow(item.id);
            if (item.sortOrder != null) task.setSortOrder(item.sortOrder);
            if (item.priorityWeight != null) task.setPriorityWeight(item.priorityWeight);
            taskRepository.save(task);
        }
    }

    private void applyRequest(Task task, TaskUpsertRequest req) {
        task.setName(req.name);
        task.setDescription(req.description);
        task.setDailyTargetMinutes(req.dailyTargetMinutes);
        task.setPriorityWeight(req.priorityWeight);
        task.setColor(req.color);
        task.setIcon(req.icon);
        task.setActive(req.active != null ? req.active : true);
        if (req.sortOrder != null) task.setSortOrder(req.sortOrder);
        task.setUpdatedAt(java.time.LocalDateTime.now());
    }

    private Task getOrThrow(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Task not found: " + id));
    }

    private TaskDto toDto(Task task) {
        TaskDto dto = new TaskDto();
        dto.id = task.getId();
        dto.name = task.getName();
        dto.description = task.getDescription();
        dto.dailyTargetMinutes = task.getDailyTargetMinutes();
        dto.priorityWeight = task.getPriorityWeight();
        dto.color = task.getColor();
        dto.icon = task.getIcon();
        dto.active = task.getActive();
        dto.sortOrder = task.getSortOrder();
        dto.createdAt = task.getCreatedAt();
        dto.updatedAt = task.getUpdatedAt();
        return dto;
    }
}
