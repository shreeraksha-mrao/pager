package com.pager.controller;

import com.pager.dto.PageEventDto;
import com.pager.entity.DeclineReasonType;
import com.pager.entity.PageStatus;
import com.pager.repository.PageEventRepository;
import com.pager.service.PageEventService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/pages")
public class PageController {

    private final PageEventRepository pageEventRepository;
    private final PageEventService pageEventService;

    public PageController(PageEventRepository pageEventRepository, PageEventService pageEventService) {
        this.pageEventRepository = pageEventRepository;
        this.pageEventService = pageEventService;
    }

    @GetMapping
    public Page<PageEventDto> findAll(@RequestParam(required = false) PageStatus status,
                                       @RequestParam(required = false) Long taskId,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size);
        Page<com.pager.entity.PageEvent> result;
        if (status != null && taskId != null) {
            result = pageEventRepository.findByStatusAndTaskWithTask(status, taskId, pageable);
        } else if (status != null) {
            result = pageEventRepository.findByStatusWithTask(status, pageable);
        } else if (taskId != null) {
            result = pageEventRepository.findByTaskWithTask(taskId, pageable);
        } else {
            result = pageEventRepository.findAllWithTask(pageable);
        }
        return result.map(PageEventDto::from);
    }

    @GetMapping("/{id}")
    public PageEventDto findById(@PathVariable Long id) {
        return pageEventService.getDto(id);
    }

    @PostMapping("/{id}/respond")
    public PageEventDto respond(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String status = body.get("status");
        if ("ACCEPTED".equalsIgnoreCase(status)) {
            pageEventService.accept(id);
        } else if ("DECLINED".equalsIgnoreCase(status)) {
            pageEventService.decline(id, body.get("reason"), DeclineReasonType.FREE_TEXT);
        } else {
            throw new IllegalArgumentException("status must be ACCEPTED or DECLINED");
        }
        return pageEventService.getDto(id);
    }
}
