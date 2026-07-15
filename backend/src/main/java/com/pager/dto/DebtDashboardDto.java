package com.pager.dto;

import java.util.List;

public class DebtDashboardDto {
    public List<TaskDebtDto> tasks;
    public int totalDebtMinutes;

    public record TaskDebtDto(Long taskId, String taskName, String color, String icon,
                               int effectiveDebtMinutes, long daysBehindTarget,
                               List<TrendEntry> trend) {}

    public record TrendEntry(String date, int cumulativeDebtMinutes, int shortfallMinutes) {}
}
