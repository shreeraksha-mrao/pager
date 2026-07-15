package com.pager.dto;

import java.time.LocalDateTime;

/** Current pager pause/enabled state, returned by GET/POST /api/pager/*. */
public record PagerStatusDto(boolean paused, LocalDateTime pausedSince, String reason) {
}
