package com.pager.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.LocalTime;

@Configuration
@ConfigurationProperties(prefix = "pager.scheduler")
public class SchedulerProperties {

    private long tickIntervalMs = 300_000;
    private LocalTime workingHoursStart = LocalTime.of(11, 0);
    private LocalTime workingHoursEnd = LocalTime.of(17, 0);
    private LocalTime nightLowWindowStart = LocalTime.of(1, 0);
    private LocalTime nightLowWindowEnd = LocalTime.of(7, 0);
    private double nightLowProbability = 0.30;
    private LocalTime nightMediumWindowStart = LocalTime.of(7, 0);
    private LocalTime nightMediumWindowEnd = LocalTime.of(11, 0);
    private double nightMediumProbability = 0.75;
    private double baseTickProbability = 0.15;
    private double maxTickProbability = 0.60;
    private int minGapBetweenPagesMinutes = 15;
    private int defaultPageDurationMinutes = 60;
    private int missedPageTimeoutMinutes = 20;
    private int declineCooldownMinMinutes = 15;
    private int declineCooldownMaxMinutes = 90;
    private int confirmationReminderIntervalMinutes = 25;
    private int confirmationReminderMaxCount = 6;
    private int dailyMinimumTargetMinutes = 210;

    public long getTickIntervalMs() { return tickIntervalMs; }
    public void setTickIntervalMs(long tickIntervalMs) { this.tickIntervalMs = tickIntervalMs; }

    public LocalTime getWorkingHoursStart() { return workingHoursStart; }
    public void setWorkingHoursStart(LocalTime workingHoursStart) { this.workingHoursStart = workingHoursStart; }

    public LocalTime getWorkingHoursEnd() { return workingHoursEnd; }
    public void setWorkingHoursEnd(LocalTime workingHoursEnd) { this.workingHoursEnd = workingHoursEnd; }

    public LocalTime getNightLowWindowStart() { return nightLowWindowStart; }
    public void setNightLowWindowStart(LocalTime nightLowWindowStart) { this.nightLowWindowStart = nightLowWindowStart; }

    public LocalTime getNightLowWindowEnd() { return nightLowWindowEnd; }
    public void setNightLowWindowEnd(LocalTime nightLowWindowEnd) { this.nightLowWindowEnd = nightLowWindowEnd; }

    public double getNightLowProbability() { return nightLowProbability; }
    public void setNightLowProbability(double nightLowProbability) { this.nightLowProbability = nightLowProbability; }

    public LocalTime getNightMediumWindowStart() { return nightMediumWindowStart; }
    public void setNightMediumWindowStart(LocalTime nightMediumWindowStart) { this.nightMediumWindowStart = nightMediumWindowStart; }

    public LocalTime getNightMediumWindowEnd() { return nightMediumWindowEnd; }
    public void setNightMediumWindowEnd(LocalTime nightMediumWindowEnd) { this.nightMediumWindowEnd = nightMediumWindowEnd; }

    public double getNightMediumProbability() { return nightMediumProbability; }
    public void setNightMediumProbability(double nightMediumProbability) { this.nightMediumProbability = nightMediumProbability; }

    public double getBaseTickProbability() { return baseTickProbability; }
    public void setBaseTickProbability(double baseTickProbability) { this.baseTickProbability = baseTickProbability; }

    public double getMaxTickProbability() { return maxTickProbability; }
    public void setMaxTickProbability(double maxTickProbability) { this.maxTickProbability = maxTickProbability; }

    public int getMinGapBetweenPagesMinutes() { return minGapBetweenPagesMinutes; }
    public void setMinGapBetweenPagesMinutes(int minGapBetweenPagesMinutes) { this.minGapBetweenPagesMinutes = minGapBetweenPagesMinutes; }

    public int getDefaultPageDurationMinutes() { return defaultPageDurationMinutes; }
    public void setDefaultPageDurationMinutes(int defaultPageDurationMinutes) { this.defaultPageDurationMinutes = defaultPageDurationMinutes; }

    public int getMissedPageTimeoutMinutes() { return missedPageTimeoutMinutes; }
    public void setMissedPageTimeoutMinutes(int missedPageTimeoutMinutes) { this.missedPageTimeoutMinutes = missedPageTimeoutMinutes; }

    public int getDeclineCooldownMinMinutes() { return declineCooldownMinMinutes; }
    public void setDeclineCooldownMinMinutes(int declineCooldownMinMinutes) { this.declineCooldownMinMinutes = declineCooldownMinMinutes; }

    public int getDeclineCooldownMaxMinutes() { return declineCooldownMaxMinutes; }
    public void setDeclineCooldownMaxMinutes(int declineCooldownMaxMinutes) { this.declineCooldownMaxMinutes = declineCooldownMaxMinutes; }

    public int getConfirmationReminderIntervalMinutes() { return confirmationReminderIntervalMinutes; }
    public void setConfirmationReminderIntervalMinutes(int confirmationReminderIntervalMinutes) { this.confirmationReminderIntervalMinutes = confirmationReminderIntervalMinutes; }

    public int getConfirmationReminderMaxCount() { return confirmationReminderMaxCount; }
    public void setConfirmationReminderMaxCount(int confirmationReminderMaxCount) { this.confirmationReminderMaxCount = confirmationReminderMaxCount; }

    public int getDailyMinimumTargetMinutes() { return dailyMinimumTargetMinutes; }
    public void setDailyMinimumTargetMinutes(int dailyMinimumTargetMinutes) { this.dailyMinimumTargetMinutes = dailyMinimumTargetMinutes; }
}
