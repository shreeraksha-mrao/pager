package com.pager.dto;

import java.time.LocalDateTime;

public class TaskDto {
    public Long id;
    public String name;
    public String description;
    public Integer dailyTargetMinutes;
    public Double priorityWeight;
    public String color;
    public String icon;
    public Boolean active;
    public Integer sortOrder;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
}
