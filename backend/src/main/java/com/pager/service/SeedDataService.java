package com.pager.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pager.entity.Task;
import com.pager.entity.TaskSchedulerState;
import com.pager.repository.TaskRepository;
import com.pager.repository.TaskSchedulerStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.List;

/**
 * Seeds default tasks from the bundled JSON file, but only on first boot
 * (i.e. when the tasks table is empty). Once tasks exist, this file is never
 * consulted again — all further changes happen through the Task REST API.
 */
@Service
public class SeedDataService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedDataService.class);

    private final TaskRepository taskRepository;
    private final TaskSchedulerStateRepository schedulerStateRepository;
    private final ObjectMapper objectMapper;

    public SeedDataService(TaskRepository taskRepository,
                            TaskSchedulerStateRepository schedulerStateRepository,
                            ObjectMapper objectMapper) {
        this.taskRepository = taskRepository;
        this.schedulerStateRepository = schedulerStateRepository;
        this.objectMapper = objectMapper;
    }

    private record SeedTask(String name, String description, Integer dailyTargetMinutes,
                             Double priorityWeight, String color, String icon) {}

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        if (taskRepository.count() > 0) {
            log.info("Tasks already present ({}) — skipping seed load.", taskRepository.count());
            return;
        }
        try (InputStream in = new ClassPathResource("seed/default-tasks.json").getInputStream()) {
            List<SeedTask> seedTasks = objectMapper.readValue(in, objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, SeedTask.class));
            int order = 0;
            for (SeedTask seed : seedTasks) {
                Task task = new Task();
                task.setName(seed.name());
                task.setDescription(seed.description());
                task.setDailyTargetMinutes(seed.dailyTargetMinutes() != null ? seed.dailyTargetMinutes() : 60);
                task.setPriorityWeight(seed.priorityWeight() != null ? seed.priorityWeight() : 1.0);
                task.setColor(seed.color());
                task.setIcon(seed.icon());
                task.setActive(true);
                task.setSortOrder(order++);
                task = taskRepository.save(task);

                TaskSchedulerState state = new TaskSchedulerState();
                state.setTask(task);
                state.setConsecutiveDeclines(0);
                schedulerStateRepository.save(state);
            }
            log.info("Seeded {} default tasks from default-tasks.json", seedTasks.size());
        }
    }
}
