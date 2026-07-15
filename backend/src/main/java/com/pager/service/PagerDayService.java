package com.pager.service;

import com.pager.config.SchedulerProperties;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Defines the "pager day" boundary used for all debt/target/analytics "today"
 * semantics. Unlike a plain calendar day (00:00-24:00), the pager day runs from
 * a configurable cutoff hour (default 2:00 AM) through to the same time on the
 * next calendar day — so work logged or a page accepted at, say, 1:30 AM still
 * counts toward the *previous* day's target instead of prematurely starting a
 * fresh day (and fresh debt) at midnight.
 */
@Service
public class PagerDayService {

    private final SchedulerProperties props;

    public PagerDayService(SchedulerProperties props) {
        this.props = props;
    }

    private LocalTime cutoff() {
        return LocalTime.of(props.getDayCutoffHour(), 0);
    }

    /** The pager-day date containing "now". */
    public LocalDate currentPagerDate() {
        return pagerDateFor(LocalDateTime.now());
    }

    /** The pager-day date containing the given instant. */
    public LocalDate pagerDateFor(LocalDateTime dateTime) {
        LocalTime cutoff = cutoff();
        return dateTime.toLocalTime().isBefore(cutoff)
                ? dateTime.toLocalDate().minusDays(1)
                : dateTime.toLocalDate();
    }

    /** The real calendar instant at which the given pager-day date begins. */
    public LocalDateTime startOfPagerDay(LocalDate pagerDate) {
        return pagerDate.atTime(cutoff());
    }
}
