package com.pager.scheduler;

import com.pager.entity.Task;
import com.pager.entity.TaskDebtLedger;
import com.pager.repository.TaskDebtLedgerRepository;
import com.pager.repository.TaskRepository;
import com.pager.service.DebtCalculatorService;
import com.pager.service.PagerDayService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Finalizes yesterday's shortfall/excess into the persistent debt ledger for
 * every active task, once per day at the day boundary. This is what makes
 * debt carry forward: a missed day keeps elevating scheduling weight until
 * enough extra completed work on later days repays it.
 */
@Component
public class DebtRolloverJob {

    private static final Logger log = LoggerFactory.getLogger(DebtRolloverJob.class);

    private final TaskRepository taskRepository;
    private final TaskDebtLedgerRepository debtLedgerRepository;
    private final DebtCalculatorService debtCalculatorService;
    private final PagerDayService pagerDayService;

    public DebtRolloverJob(TaskRepository taskRepository,
                            TaskDebtLedgerRepository debtLedgerRepository,
                            DebtCalculatorService debtCalculatorService,
                            PagerDayService pagerDayService) {
        this.taskRepository = taskRepository;
        this.debtLedgerRepository = debtLedgerRepository;
        this.debtCalculatorService = debtCalculatorService;
        this.pagerDayService = pagerDayService;
    }

    @Scheduled(cron = "${pager.scheduler.rollover-cron:0 1 2 * * *}") // 02:01 by default — see day-cutoff-hour
    @Transactional
    public void rolloverYesterday() {
        rolloverForDate(pagerDayService.currentPagerDate().minusDays(1));
    }

    @Transactional
    public void rolloverForDate(LocalDate date) {
        for (Task task : taskRepository.findAll()) {
            if (debtLedgerRepository.findByTask_IdAndLedgerDate(task.getId(), date).isPresent()) {
                continue; // already rolled over
            }
            int completed = debtCalculatorService.todayCompletedMinutes(task, date);
            int target = task.getDailyTargetMinutes();
            int shortfall = debtCalculatorService.shortfallForDate(task, date); // paused-day-aware
            int excessCredit = Math.max(0, completed - target);
            int previousCumulative = debtCalculatorService.cumulativeDebt(task);
            int newCumulative = Math.max(0, previousCumulative + shortfall - excessCredit);

            TaskDebtLedger ledger = new TaskDebtLedger();
            ledger.setTask(task);
            ledger.setLedgerDate(date);
            ledger.setTargetMinutes(target);
            ledger.setCompletedMinutes(completed);
            ledger.setShortfallMinutes(shortfall);
            ledger.setCumulativeDebtMinutes(newCumulative);
            debtLedgerRepository.save(ledger);

            log.info("Debt rollover [{}] {}: completed={} target={} shortfall={} cumulativeDebt={}",
                    date, task.getName(), completed, target, shortfall, newCumulative);
        }
    }
}
