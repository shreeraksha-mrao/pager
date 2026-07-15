package com.pager.dto;

import java.time.LocalTime;

public class SettingsDto {
    public LocalTime workingHoursStart;
    public LocalTime workingHoursEnd;
    public LocalTime nightLowWindowStart;
    public LocalTime nightLowWindowEnd;
    public Double nightLowProbability;
    public LocalTime nightMediumWindowStart;
    public LocalTime nightMediumWindowEnd;
    public Double nightMediumProbability;
    public Double baseTickProbability;
    public Double maxTickProbability;
    public Integer minGapBetweenPagesMinutes;
    public Integer defaultPageDurationMinutes;
    public Integer missedPageTimeoutMinutes;
    public Integer declineCooldownMinMinutes;
    public Integer declineCooldownMaxMinutes;
    public Integer confirmationReminderIntervalMinutes;
    public Integer confirmationReminderMaxCount;
    public Integer dailyMinimumTargetMinutes;
    public Boolean telegramRegistered;
}
