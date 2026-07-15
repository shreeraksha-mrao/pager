package com.pager.controller;

import com.pager.dto.AnalyticsSummaryDto;
import com.pager.dto.DebtDashboardDto;
import com.pager.service.AnalyticsService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/summary")
    public AnalyticsSummaryDto summary(@RequestParam(defaultValue = "today") String range) {
        return analyticsService.getSummary(range);
    }

    @GetMapping("/hours")
    public List<AnalyticsSummaryDto.TaskMinutesDto> hours(@RequestParam(defaultValue = "today") String range) {
        return analyticsService.getHours(range);
    }

    @GetMapping("/pages-stats")
    public Map<String, Object> pagesStats() {
        return analyticsService.getPagesStats();
    }

    @GetMapping("/decline-reasons")
    public List<AnalyticsSummaryDto.DeclineReasonCountDto> declineReasons() {
        return analyticsService.getDeclineReasons();
    }

    @GetMapping("/debt")
    public DebtDashboardDto debt() {
        return analyticsService.getDebtDashboard();
    }

    @GetMapping("/session-sources")
    public Map<String, Object> sessionSources() {
        return analyticsService.getSessionSourceTotals();
    }
}
