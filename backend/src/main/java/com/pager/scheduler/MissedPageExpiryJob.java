package com.pager.scheduler;

import com.pager.config.SchedulerProperties;
import com.pager.entity.PageEvent;
import com.pager.entity.PageStatus;
import com.pager.repository.PageEventRepository;
import com.pager.service.PageEventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Auto-expires PENDING pages nobody responded to within the configured
 * timeout. Debt is left untouched (the obligation stands) and the task goes
 * through the same cooldown/replacement path as an explicit decline, just
 * with a shorter cooldown since a miss doesn't necessarily mean "no".
 */
@Component
public class MissedPageExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(MissedPageExpiryJob.class);

    private final PageEventRepository pageEventRepository;
    private final PageEventService pageEventService;
    private final SchedulerProperties props;

    public MissedPageExpiryJob(PageEventRepository pageEventRepository,
                                PageEventService pageEventService,
                                SchedulerProperties props) {
        this.pageEventRepository = pageEventRepository;
        this.pageEventService = pageEventService;
        this.props = props;
    }

    @Scheduled(fixedRate = 60_000) // every 1 minute
    @Transactional
    public void expireStalePages() {
        List<PageEvent> pending = pageEventRepository.findByStatus(PageStatus.PENDING);
        LocalDateTime now = LocalDateTime.now();
        for (PageEvent event : pending) {
            long minutesSinceSent = Duration.between(event.getSentAt(), now).toMinutes();
            if (minutesSinceSent >= props.getMissedPageTimeoutMinutes()) {
                log.info("Page {} for task '{}' expired as MISSED after {} min", event.getId(),
                        event.getTask().getName(), minutesSinceSent);
                pageEventService.expireMissed(event.getId());
            }
        }
    }
}
