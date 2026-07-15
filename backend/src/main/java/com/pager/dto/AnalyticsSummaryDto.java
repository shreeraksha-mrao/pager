package com.pager.dto;

import java.util.List;

public class AnalyticsSummaryDto {
    /** Which scope this summary was computed for: "today" or "lifetime". */
    public String range;

    public List<TaskMinutesDto> todayHoursByTask;
    public List<TaskMinutesDto> weekHoursByTask;
    public List<TaskMinutesDto> lifetimeHoursByTask;

    public long pagesReceived;
    public long pagesAccepted;
    public long pagesDeclined;
    public long pagesMissed;
    /** Accepted pages whose session was ultimately confirmed COMPLETED. */
    public long acceptedCompletedPages;
    /** Accepted pages whose session was ultimately confirmed ABANDONED. */
    public long acceptedAbandonedPages;
    public double acceptanceRate;
    public double completionRate;
    public int productivityDebtMinutes;
    public List<DeclineReasonCountDto> topDeclineReasons;
    /** Completed minutes originating from accepted+confirmed pages, scoped by range. */
    public int minutesFromPages;
    /** Completed minutes logged manually (independent of any page), scoped by range. */
    public int minutesFromManual;
    /** minutesFromPages + minutesFromManual, scoped by range. */
    public int totalProductiveMinutes;

    public record TaskMinutesDto(Long taskId, String taskName, String color, String icon, int minutes) {}
    public record DeclineReasonCountDto(String reason, long count) {}
}

