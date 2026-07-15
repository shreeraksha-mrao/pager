package com.pager.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class TaskUpsertRequest {

    @NotBlank
    public String name;

    public String description;

    @Min(1)
    public Integer dailyTargetMinutes = 60;

    public Double priorityWeight = 1.0;

    public String color;

    public String icon;

    public Boolean active = true;

    public Integer sortOrder = 0;
}
