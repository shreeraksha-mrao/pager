package com.pager.service;

import com.pager.config.SchedulerProperties;
import com.pager.dto.SettingsDto;
import org.springframework.stereotype.Service;

/**
 * Exposes the scheduler's tunable parameters for the Settings UI.
 * NOTE: updates mutate the live SchedulerProperties bean for the current
 * process; for durable persistence across restarts, back this with
 * app_settings rows in a future iteration.
 */
@Service
public class SettingsService {

    private final SchedulerProperties props;
    private final TelegramMessagingService telegramMessagingService;

    public SettingsService(SchedulerProperties props, TelegramMessagingService telegramMessagingService) {
        this.props = props;
        this.telegramMessagingService = telegramMessagingService;
    }

    public SettingsDto get() {
        SettingsDto dto = new SettingsDto();
        dto.workingHoursStart = props.getWorkingHoursStart();
        dto.workingHoursEnd = props.getWorkingHoursEnd();
        dto.nightLowWindowStart = props.getNightLowWindowStart();
        dto.nightLowWindowEnd = props.getNightLowWindowEnd();
        dto.nightLowProbability = props.getNightLowProbability();
        dto.nightMediumWindowStart = props.getNightMediumWindowStart();
        dto.nightMediumWindowEnd = props.getNightMediumWindowEnd();
        dto.nightMediumProbability = props.getNightMediumProbability();
        dto.baseTickProbability = props.getBaseTickProbability();
        dto.maxTickProbability = props.getMaxTickProbability();
        dto.minGapBetweenPagesMinutes = props.getMinGapBetweenPagesMinutes();
        dto.defaultPageDurationMinutes = props.getDefaultPageDurationMinutes();
        dto.missedPageTimeoutMinutes = props.getMissedPageTimeoutMinutes();
        dto.declineCooldownMinMinutes = props.getDeclineCooldownMinMinutes();
        dto.declineCooldownMaxMinutes = props.getDeclineCooldownMaxMinutes();
        dto.confirmationReminderIntervalMinutes = props.getConfirmationReminderIntervalMinutes();
        dto.confirmationReminderMaxCount = props.getConfirmationReminderMaxCount();
        dto.dailyMinimumTargetMinutes = props.getDailyMinimumTargetMinutes();
        dto.telegramRegistered = telegramMessagingService.resolveChatId().isPresent();
        return dto;
    }

    public SettingsDto update(SettingsDto req) {
        if (req.workingHoursStart != null) props.setWorkingHoursStart(req.workingHoursStart);
        if (req.workingHoursEnd != null) props.setWorkingHoursEnd(req.workingHoursEnd);
        if (req.nightLowWindowStart != null) props.setNightLowWindowStart(req.nightLowWindowStart);
        if (req.nightLowWindowEnd != null) props.setNightLowWindowEnd(req.nightLowWindowEnd);
        if (req.nightLowProbability != null) props.setNightLowProbability(req.nightLowProbability);
        if (req.nightMediumWindowStart != null) props.setNightMediumWindowStart(req.nightMediumWindowStart);
        if (req.nightMediumWindowEnd != null) props.setNightMediumWindowEnd(req.nightMediumWindowEnd);
        if (req.nightMediumProbability != null) props.setNightMediumProbability(req.nightMediumProbability);
        if (req.baseTickProbability != null) props.setBaseTickProbability(req.baseTickProbability);
        if (req.maxTickProbability != null) props.setMaxTickProbability(req.maxTickProbability);
        if (req.minGapBetweenPagesMinutes != null) props.setMinGapBetweenPagesMinutes(req.minGapBetweenPagesMinutes);
        if (req.defaultPageDurationMinutes != null) props.setDefaultPageDurationMinutes(req.defaultPageDurationMinutes);
        if (req.missedPageTimeoutMinutes != null) props.setMissedPageTimeoutMinutes(req.missedPageTimeoutMinutes);
        if (req.declineCooldownMinMinutes != null) props.setDeclineCooldownMinMinutes(req.declineCooldownMinMinutes);
        if (req.declineCooldownMaxMinutes != null) props.setDeclineCooldownMaxMinutes(req.declineCooldownMaxMinutes);
        if (req.confirmationReminderIntervalMinutes != null) props.setConfirmationReminderIntervalMinutes(req.confirmationReminderIntervalMinutes);
        if (req.confirmationReminderMaxCount != null) props.setConfirmationReminderMaxCount(req.confirmationReminderMaxCount);
        if (req.dailyMinimumTargetMinutes != null) props.setDailyMinimumTargetMinutes(req.dailyMinimumTargetMinutes);
        return get();
    }
}
