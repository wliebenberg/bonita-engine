package org.bonitasoft.engine.scheduler.impl;

import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.when;

import java.util.ArrayList;

import org.assertj.core.api.Fail;
import org.bonitasoft.engine.sessionaccessor.ReadSessionAccessor;
import org.bonitasoft.engine.transaction.TransactionService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.Trigger.TriggerState;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.StdSchedulerFactory;

@RunWith(MockitoJUnitRunner.class)
public class QuartzSchedulerExecutorIT {

    private Scheduler scheduler;

    @Mock
    private BonitaSchedulerFactory schedulerFactory;

    @Mock
    private ReadSessionAccessor sessionAccessor;

    @Mock
    private TransactionService transactionService;

    @Mock
    private SchedulerServiceImpl schedulerService;

    private QuartzSchedulerExecutor quartzSchedulerExecutor;

    @Before
    public void setUp() throws Exception {
        scheduler = StdSchedulerFactory.getDefaultScheduler();
        quartzSchedulerExecutor = new QuartzSchedulerExecutor(schedulerFactory, new ArrayList<AbstractJobListener>(), sessionAccessor, transactionService,
                false);
        when(schedulerFactory.getScheduler()).thenReturn(scheduler);
        quartzSchedulerExecutor.start();
    }

    @After
    public void tearDown() throws Exception {
        quartzSchedulerExecutor.shutdown();
    }

    @Test
    public void rescheduleTriggersInError() throws Exception {
        final String groupName = "group";
        final String triggerName = "trigger";
        final String jobName = "job";
        scheduleAJobWithAPrivateDefaultConstructor(groupName, triggerName, jobName);
        waitUntilTheTriggerIsInState(groupName, triggerName, TriggerState.ERROR, 4000);
        changeTheJobWithAPublicDefaultConstructor(groupName, jobName);

        quartzSchedulerExecutor.rescheduleErroneousTriggers();

        waitUntilTheTriggerIsInState(groupName, triggerName, TriggerState.NORMAL, 4000);
    }

    private void scheduleAJobWithAPrivateDefaultConstructor(final String groupName, final String triggerName, final String jobName) throws SchedulerException {
        final JobDetail jobDetail = buildJobDetail(jobName, groupName);
        final Trigger trigger = buildTrigger(triggerName, groupName);
        scheduler.scheduleJob(jobDetail, trigger);
    }

    private void waitUntilTheTriggerIsInState(final String groupName, final String triggerName, final TriggerState state, final long timeout) throws Exception {
        final long startTime = System.currentTimeMillis();
        boolean isInState = false;
        do {
            final TriggerState triggerState = scheduler.getTriggerState(new TriggerKey(triggerName, groupName));
            if (triggerState.equals(state)) {
                isInState = true;
            }
            Thread.sleep(50);
        } while (!isInState && startTime + timeout > System.currentTimeMillis());
        if (!isInState) {
            fail("the trigger is not in the state" + state);
        }
    }

    private void changeTheJobWithAPublicDefaultConstructor(final String groupName, final String jobName) throws SchedulerException {
        final JobDetail jobDetail = scheduler.getJobDetail(new JobKey(jobName, groupName));
        final JobDetail updatedJobDetail = jobDetail.getJobBuilder().ofType(LogJob.class).storeDurably(true).build();
        scheduler.addJob(updatedJobDetail, true);
    }

    private JobDetail buildJobDetail(final String jobName, final String groupName) {
        return JobBuilder.newJob(FailingJob.class).withIdentity(jobName, groupName).build();
    }

    private Trigger buildTrigger(final String triggerName, final String groupName) {
        return TriggerBuilder.newTrigger().withIdentity(triggerName, groupName).startNow()
                .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(1).repeatForever()).build();
    }

}
