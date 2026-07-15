package com.pager.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** Request body for POST /api/sessions/manual — logging completed work not tied to a page. */
public class ManualSessionRequest {

    @NotNull
    public Long taskId;

    @NotNull
    @Min(1)
    public Integer durationMinutes;

    public String notes;

    /** Defaults to today if omitted — lets you log work done earlier the same day. */
    public LocalDate sessionDate;
}
