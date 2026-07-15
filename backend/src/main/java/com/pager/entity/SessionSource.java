package com.pager.entity;

/** Distinguishes how a StudySession's completed work was recorded. */
public enum SessionSource {
    /** Created from a Telegram page that the user accepted and confirmed. */
    PAGE_ACCEPTED,
    /** Manually logged by the user from the dashboard, independent of any page. */
    MANUAL
}
