package org.bonitasoft.engine.scheduler.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.bonitasoft.engine.builder.BuilderFactory;
import org.bonitasoft.engine.events.EventService;
import org.bonitasoft.engine.events.model.FireEventException;
import org.bonitasoft.engine.events.model.SEvent;
import org.bonitasoft.engine.events.model.builders.SEventBuilder;
import org.bonitasoft.engine.events.model.builders.SEventBuilderFactory;
import org.bonitasoft.engine.log.technical.TechnicalLoggerService;
import org.bonitasoft.engine.queriablelogger.model.SQueriableLog;
import org.bonitasoft.engine.queriablelogger.model.SQueriableLogSeverity;
import org.bonitasoft.engine.scheduler.InjectedService;
import org.bonitasoft.engine.scheduler.JobService;
import org.bonitasoft.engine.scheduler.SchedulerExecutor;
import org.bonitasoft.engine.scheduler.ServicesResolver;
import org.bonitasoft.engine.scheduler.StatelessJob;
import org.bonitasoft.engine.scheduler.builder.SJobQueriableLogBuilder;
import org.bonitasoft.engine.scheduler.builder.SJobQueriableLogBuilderFactory;
import org.bonitasoft.engine.scheduler.builder.SSchedulerQueriableLogBuilder;
import org.bonitasoft.engine.scheduler.builder.SSchedulerQueriableLogBuilderFactory;
import org.bonitasoft.engine.scheduler.exception.SJobConfigurationException;
import org.bonitasoft.engine.scheduler.exception.SJobExecutionException;
import org.bonitasoft.engine.scheduler.exception.SSchedulerException;
import org.bonitasoft.engine.scheduler.exception.jobDescriptor.SJobDescriptorCreationException;
import org.bonitasoft.engine.scheduler.model.SJobDescriptor;
import org.bonitasoft.engine.scheduler.model.SJobParameter;
import org.bonitasoft.engine.scheduler.trigger.Trigger;
import org.bonitasoft.engine.sessionaccessor.SessionAccessor;
import org.bonitasoft.engine.transaction.TransactionService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(BuilderFactory.class)
public class SchedulerServiceImplTest {

    SchedulerServiceImpl schedulerService;

    @Mock
    private SchedulerExecutor schedulerExecutor;

    @Mock
    private JobService jobService;

    @Mock
    private EventService eventService;

    ServicesResolver servicesResolver;

    @Before
    public void setUp() {
        PowerMockito.mockStatic(BuilderFactory.class);
        initMocks(this);

        final TechnicalLoggerService logger = mock(TechnicalLoggerService.class);
        final TransactionService transactionService = mock(TransactionService.class);
        final SessionAccessor sessionAccessor = mock(SessionAccessor.class);

        final SEventBuilder sEventBuilder = mock(SEventBuilder.class);
        final SEventBuilderFactory sEventBuilderFactory = mock(SEventBuilderFactory.class);
        Mockito.when(BuilderFactory.get(SEventBuilderFactory.class)).thenReturn(sEventBuilderFactory);

        when(sEventBuilderFactory.createNewInstance(anyString())).thenReturn(sEventBuilder);
        when(sEventBuilderFactory.createInsertEvent(anyString())).thenReturn(sEventBuilder);
        when(sEventBuilder.setObject(any(Object.class))).thenReturn(sEventBuilder);

        final SJobQueriableLogBuilder jobLogBuilder = mock(SJobQueriableLogBuilder.class);
        final SJobQueriableLogBuilderFactory jobLogBuilderFact = mock(SJobQueriableLogBuilderFactory.class);

        final SSchedulerQueriableLogBuilderFactory schedulerLogBuilderFact = mock(SSchedulerQueriableLogBuilderFactory.class);

        final SSchedulerQueriableLogBuilder sLogBuilder = mock(SSchedulerQueriableLogBuilder.class);
        when(schedulerLogBuilderFact.createNewInstance()).thenReturn(sLogBuilder);

        final SQueriableLog sQueriableLog = mock(SQueriableLog.class);
        when(jobLogBuilder.done()).thenReturn(sQueriableLog);

        when(jobLogBuilderFact.createNewInstance()).thenReturn(sLogBuilder);
        when(sLogBuilder.actionStatus(any(int.class))).thenReturn(sLogBuilder);
        when(sLogBuilder.severity(any(SQueriableLogSeverity.class))).thenReturn(sLogBuilder);
        when(sLogBuilder.rawMessage(anyString())).thenReturn(sLogBuilder);
        servicesResolver = mock(ServicesResolver.class);
        schedulerService = new SchedulerServiceImpl(schedulerExecutor, jobService, logger, eventService,
                transactionService, sessionAccessor, servicesResolver);
    }

    @Test
    public void isStarted_return_true_if_schedulorExecutor_is_started() throws Exception {
        when(schedulerExecutor.isStarted()).thenReturn(true);

        boolean started = schedulerService.isStarted();

        assertTrue(started);
    }

    @Test
    public void isStarted_return_false_if_schedulorExecutor_is_not_started() throws Exception {
        when(schedulerExecutor.isStarted()).thenReturn(false);

        boolean started = schedulerService.isStarted();

        assertFalse(started);
    }

    @Test
    public void isStopped_return_true_if_executor_is_shutodown() throws Exception {
        when(schedulerExecutor.isShutdown()).thenReturn(true);

        boolean stopped = schedulerService.isStopped();

        assertTrue(stopped);
    }

    @Test
    public void isStopped_return_false_if_executor_is_not_shutodown() throws Exception {
        when(schedulerExecutor.isShutdown()).thenReturn(false);

        boolean stopped = schedulerService.isStopped();

        assertFalse(stopped);
    }

    @Test
    public void start_start_schedulerexecutor_and_fire_a_start_event() throws Exception {
        schedulerService.start();

        verify(schedulerExecutor).start();
        verify(eventService).fireEvent(any(SEvent.class));
    }

    @Test
    public void stop_shutdown_schedulerexecutor_and_fire_a_stop_event() throws Exception {
        schedulerService.stop();

        verify(schedulerExecutor).shutdown();
        verify(eventService).fireEvent(any(SEvent.class));
    }

    @Test
    public void delete_delete_job_and_jobDescription() throws Exception {
        String jobName = "aJobName";

        schedulerService.delete(jobName);

        verify(schedulerExecutor).delete(jobName);
        verify(jobService).deleteJobDescriptorByJobName(jobName);
    }

    @Test
    public void delete_return_schedulerexecutor_deletion_status() throws Exception {
        boolean expectedDeletionStatus = new Random().nextBoolean();
        String jobName = "jobName";
        when(schedulerExecutor.delete(jobName)).thenReturn(expectedDeletionStatus);

        boolean deletionStatus = schedulerExecutor.delete(jobName);

        assertThat(deletionStatus).isEqualTo(expectedDeletionStatus);
    }

    @Test(expected = SSchedulerException.class)
    public void cannot_execute_a_job_with_a_null_trigger() throws Exception {
        SJobDescriptor jobDescriptor = mock(SJobDescriptor.class);
        List<SJobParameter> parameters = new ArrayList<SJobParameter>();

        schedulerService.schedule(jobDescriptor, parameters, null);
    }

    @Test(expected = SSchedulerException.class)
    public void cannot_schedule_a_null_job() throws Exception {
        Trigger trigger = mock(Trigger.class);
        when(jobService.createJobDescriptor(any(SJobDescriptor.class), any(Long.class))).thenThrow(new SJobDescriptorCreationException(""));

        schedulerService.schedule(null, trigger);
    }

    @Test
    public void rescheduleErroneousTriggers_call_same_method_in_schedulerexecutor() throws Exception {
        schedulerService.rescheduleErroneousTriggers();

        verify(schedulerExecutor).rescheduleErroneousTriggers();
    }

    @Test
    public void should_pauseJobs_of_tenant_call_schedulerExecutor() throws Exception {
        schedulerService.resumeJobs(123l);

        verify(schedulerExecutor, times(1)).resumeJobs(123l);
    }

    @Test
    public void should_pauseJobs_of_tenant_call_schedulerExecutor_rethrow_exception() throws Exception {
        SSchedulerException theException = new SSchedulerException("My exception");
        doThrow(theException).when(schedulerExecutor).resumeJobs(123l);
        try {
            schedulerService.resumeJobs(123l);
            fail("should have rethrown the exception");
        } catch (SSchedulerException e) {
            assertEquals(theException, e);
        }
    }

    @Test
    public void should_injectService_inject_setter_hacing_the_annotation() throws Exception {
        BeanThatNeedMyService beanThatNeedMyService = new BeanThatNeedMyService();

        Long myService = new Long(1);
        when(servicesResolver.lookup("myService")).thenReturn(myService);

        schedulerService.injectServices(beanThatNeedMyService);

        assertEquals(myService, beanThatNeedMyService.getMyService());

    }

    private final class BeanThatNeedMyService implements StatelessJob {

        private static final long serialVersionUID = 1L;

        private Object myService;

        @InjectedService
        public void setMyService(final Object myService) {
            this.myService = myService;
        }

        public Object getMyService() {
            return myService;
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public String getDescription() {
            return null;
        }

        @Override
        public void execute() throws SJobExecutionException, FireEventException {
        }

        @Override
        public void setAttributes(final Map<String, Serializable> attributes) throws SJobConfigurationException {
        }
    }
}
