/**
 * Copyright (C) 2011-2014 BonitaSoft S.A.
 * BonitaSoft, 32 rue Gustave Eiffel - 38000 Grenoble
 * This library is free software; you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation
 * version 2.1 of the License.
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth
 * Floor, Boston, MA 02110-1301, USA.
 **/
package org.bonitasoft.engine.scheduler.impl;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;

import org.bonitasoft.engine.builder.BuilderFactory;
import org.bonitasoft.engine.commons.LogUtil;
import org.bonitasoft.engine.commons.exceptions.SBonitaException;
import org.bonitasoft.engine.events.EventService;
import org.bonitasoft.engine.events.model.FireEventException;
import org.bonitasoft.engine.events.model.SEvent;
import org.bonitasoft.engine.events.model.builders.SEventBuilderFactory;
import org.bonitasoft.engine.log.technical.TechnicalLogSeverity;
import org.bonitasoft.engine.log.technical.TechnicalLoggerService;
import org.bonitasoft.engine.persistence.FilterOption;
import org.bonitasoft.engine.persistence.QueryOptions;
import org.bonitasoft.engine.persistence.SBonitaSearchException;
import org.bonitasoft.engine.queriablelogger.model.SQueriableLog;
import org.bonitasoft.engine.queriablelogger.model.SQueriableLogSeverity;
import org.bonitasoft.engine.queriablelogger.model.builder.ActionType;
import org.bonitasoft.engine.queriablelogger.model.builder.HasCRUDEAction;
import org.bonitasoft.engine.queriablelogger.model.builder.SLogBuilder;
import org.bonitasoft.engine.scheduler.InjectedService;
import org.bonitasoft.engine.scheduler.JobIdentifier;
import org.bonitasoft.engine.scheduler.JobService;
import org.bonitasoft.engine.scheduler.SchedulerExecutor;
import org.bonitasoft.engine.scheduler.SchedulerService;
import org.bonitasoft.engine.scheduler.ServicesResolver;
import org.bonitasoft.engine.scheduler.StatelessJob;
import org.bonitasoft.engine.scheduler.builder.SSchedulerQueriableLogBuilder;
import org.bonitasoft.engine.scheduler.builder.SSchedulerQueriableLogBuilderFactory;
import org.bonitasoft.engine.scheduler.exception.SSchedulerException;
import org.bonitasoft.engine.scheduler.model.SJobDescriptor;
import org.bonitasoft.engine.scheduler.model.SJobParameter;
import org.bonitasoft.engine.scheduler.trigger.Trigger;
import org.bonitasoft.engine.sessionaccessor.SessionAccessor;
import org.bonitasoft.engine.sessionaccessor.TenantIdNotSetException;
import org.bonitasoft.engine.transaction.TransactionService;

/**
 * @author Matthieu Chaffotte
 * @author Baptiste Mesta
 * @author Elias Ricken de Medeiros
 * @author Yanyan Liu
 * @author Celine Souchet
 */
public class SchedulerServiceImpl implements SchedulerService {

    private final TechnicalLoggerService logger;

    // this recorder must not use async logging else we have an infinite loop.
    private final SchedulerExecutor schedulerExecutor;

    private final JobService jobService;

    private final EventService eventService;

    private final SEvent schedulStarted;

    private final SEvent schedulStopped;

    private final SEvent jobFailed;

    private final SessionAccessor sessionAccessor;

    private final TransactionService transactionService;

    private final ServicesResolver servicesResolver;

    /**
     * Create a new instance of scheduler service. Synchronous
     * QueriableLoggerService must be used to avoid an infinite loop.
     */
    public SchedulerServiceImpl(final SchedulerExecutor schedulerExecutor,
            final JobService jobService, final TechnicalLoggerService logger,
            final EventService eventService, final TransactionService transactionService,
            final SessionAccessor sessionAccessor,
            final ServicesResolver servicesResolver) {
        this.schedulerExecutor = schedulerExecutor;
        this.jobService = jobService;
        this.logger = logger;
        this.servicesResolver = servicesResolver;
        schedulStarted = BuilderFactory.get(SEventBuilderFactory.class)
                .createNewInstance(SCHEDULER_STARTED).done();
        schedulStopped = BuilderFactory.get(SEventBuilderFactory.class)
                .createNewInstance(SCHEDULER_STOPPED).done();
        jobFailed = BuilderFactory.get(SEventBuilderFactory.class)
                .createNewInstance(JOB_FAILED).done();
        this.eventService = eventService;
        this.transactionService = transactionService;
        this.sessionAccessor = sessionAccessor;
        schedulerExecutor.setBOSSchedulerService(this);
    }

    private void logBeforeMethod(
            final TechnicalLogSeverity technicalLogSeverity,
            final String methodName) {
        if (logger.isLoggable(this.getClass(), technicalLogSeverity)) {
            logger.log(this.getClass(), technicalLogSeverity,
                    LogUtil.getLogBeforeMethod(this.getClass(), methodName));
        }
    }

    private void logAfterMethod(
            final TechnicalLogSeverity technicalLogSeverity,
            final String methodName) {
        if (logger.isLoggable(this.getClass(), technicalLogSeverity)) {
            logger.log(this.getClass(), technicalLogSeverity,
                    LogUtil.getLogAfterMethod(this.getClass(), methodName));
        }
    }

    private void logOnExceptionMethod(
            final TechnicalLogSeverity technicalLogSeverity,
            final String methodName, final Exception e) {
        if (logger.isLoggable(this.getClass(), technicalLogSeverity)) {
            logger.log(this.getClass(), technicalLogSeverity, LogUtil
                    .getLogOnExceptionMethod(this.getClass(), methodName, e));
        }
    }

    private <T extends SLogBuilder> void initializeLogBuilder(
            final T logBuilder, final String message) {
        logBuilder.actionStatus(SQueriableLog.STATUS_FAIL)
                .severity(SQueriableLogSeverity.INTERNAL).rawMessage(message);
    }

    private <T extends HasCRUDEAction> void updateLog(
            final ActionType actionType, final T logBuilder) {
        logBuilder.setActionType(actionType);
    }

    private SSchedulerQueriableLogBuilder getLogBuilder(
            final ActionType actionType, final String message,
            final String scope) {
        final SSchedulerQueriableLogBuilder logBuilder = BuilderFactory.get(
                SSchedulerQueriableLogBuilderFactory.class).createNewInstance();
        initializeLogBuilder(logBuilder, message);
        updateLog(actionType, logBuilder);
        logBuilder.actionScope(scope);
        return logBuilder;
    }

    @Override
    public void schedule(final SJobDescriptor jobDescriptor,
            final Trigger trigger) throws SSchedulerException {
        logBeforeMethod(TechnicalLogSeverity.TRACE, "schedule");
        final SJobDescriptor createdJobDescriptor = createJobDescriptor(
                jobDescriptor, Collections.<SJobParameter> emptyList());
        internalSchedule(createdJobDescriptor, trigger);
        logAfterMethod(TechnicalLogSeverity.TRACE, "schedule");
    }

    @Override
    public void schedule(final SJobDescriptor jobDescriptor,
            final List<SJobParameter> parameters, final Trigger trigger)
            throws SSchedulerException {
        if (trigger == null) {
            throw new SSchedulerException("The trigger is null");
        }
        final SJobDescriptor createdJobDescriptor = createJobDescriptor(
                jobDescriptor, parameters);
        internalSchedule(createdJobDescriptor, trigger);
    }

    @Override
    public void executeAgain(final long jobDescriptorId)
            throws SSchedulerException {
        final SJobDescriptor jobDescriptor = jobService
                .getJobDescriptor(jobDescriptorId);
        schedulerExecutor.executeAgain(jobDescriptorId, getTenantId(),
                jobDescriptor.getJobName(),
                jobDescriptor.disallowConcurrentExecution());
    }

    @Override
    public void executeAgain(final long jobDescriptorId,
            final List<SJobParameter> parameters) throws SSchedulerException {
        final SJobDescriptor jobDescriptor = jobService
                .getJobDescriptor(jobDescriptorId);
        jobService.setJobParameters(getTenantId(), jobDescriptor.getId(),
                parameters);
        schedulerExecutor.executeAgain(jobDescriptorId, getTenantId(),
                jobDescriptor.getJobName(),
                jobDescriptor.disallowConcurrentExecution());
    }

    private SJobDescriptor createJobDescriptor(
            final SJobDescriptor sJobDescriptor,
            final List<SJobParameter> parameters) throws SSchedulerException {
        logBeforeMethod(TechnicalLogSeverity.TRACE, "createJobDescriptor");
        final long tenantId = getTenantId();
        try {
            final SJobDescriptor createdJobDescriptor = jobService
                    .createJobDescriptor(sJobDescriptor, tenantId);
            jobService.createJobParameters(parameters, tenantId,
                    createdJobDescriptor.getId());
            logAfterMethod(TechnicalLogSeverity.TRACE, "createJobDescriptor");
            return createdJobDescriptor;
        } catch (final SBonitaException sbe) {
            logOnExceptionMethod(TechnicalLogSeverity.TRACE,
                    "createJobDescriptor", sbe);
            throw new SSchedulerException(sbe);
        }
    }

    private void internalSchedule(final SJobDescriptor jobDescriptor,
            final Trigger trigger) throws SSchedulerException {
        logBeforeMethod(TechnicalLogSeverity.TRACE, "internalSchedule");
        final long tenantId = getTenantId();
        final SSchedulerQueriableLogBuilder schedulingLogBuilder = getLogBuilder(
                ActionType.SCHEDULED, "Scheduled job with name "
                        + jobDescriptor.getJobName(),
                jobDescriptor.getJobName());
        try {
            if (trigger == null) {
                schedulerExecutor.executeNow(jobDescriptor.getId(), tenantId,
                        jobDescriptor.getJobName(),
                        jobDescriptor.disallowConcurrentExecution());
            } else {
                schedulerExecutor.schedule(jobDescriptor.getId(), tenantId,
                        jobDescriptor.getJobName(), trigger,
                        jobDescriptor.disallowConcurrentExecution());
            }
            schedulingLogBuilder.actionStatus(SQueriableLog.STATUS_OK);
        } catch (final Throwable e) {
            schedulingLogBuilder.actionStatus(SQueriableLog.STATUS_FAIL);
            logger.log(this.getClass(), TechnicalLogSeverity.ERROR, e);
            try {
                eventService.fireEvent(jobFailed);
            } catch (final FireEventException e1) {
                logger.log(this.getClass(), TechnicalLogSeverity.ERROR, e1);
            }
            throw new SSchedulerException(e);
        }

        logAfterMethod(TechnicalLogSeverity.TRACE, "internalSchedule");
    }

    private long getTenantId() throws SSchedulerException {
        final long tenantId;
        try {
            tenantId = sessionAccessor.getTenantId();
        } catch (final TenantIdNotSetException e) {
            logOnExceptionMethod(TechnicalLogSeverity.TRACE, "getTenantId", e);
            throw new SSchedulerException(e);
        }
        return tenantId;
    }

    @Override
    public void executeNow(final SJobDescriptor jobDescriptor,
            final List<SJobParameter> parameters) throws SSchedulerException {
        final SJobDescriptor createdJobDescriptor = createJobDescriptor(
                jobDescriptor, parameters);
        internalSchedule(createdJobDescriptor, null);
    }

    @Override
    public boolean isStarted() throws SSchedulerException {
        logBeforeMethod(TechnicalLogSeverity.TRACE, "isStarted");
        final boolean isStarted = schedulerExecutor.isStarted();
        logAfterMethod(TechnicalLogSeverity.TRACE, "isStarted");
        return isStarted;
    }

    @Override
    public boolean isStopped() throws SSchedulerException {
        logBeforeMethod(TechnicalLogSeverity.TRACE, "isShutdown");
        final boolean isShutdown = schedulerExecutor.isShutdown();
        logAfterMethod(TechnicalLogSeverity.TRACE, "isShutdown");
        return isShutdown;
    }

    @Override
    public void start() throws SSchedulerException, FireEventException {
        logBeforeMethod(TechnicalLogSeverity.TRACE, "start");
        schedulerExecutor.start();
        eventService.fireEvent(schedulStarted);
        logAfterMethod(TechnicalLogSeverity.TRACE, "start");
    }

    @Override
    public void stop() throws SSchedulerException, FireEventException {
        logBeforeMethod(TechnicalLogSeverity.TRACE, "shutdown");
        schedulerExecutor.shutdown();
        eventService.fireEvent(schedulStopped);
        logAfterMethod(TechnicalLogSeverity.TRACE, "shutdown");
    }

    @Override
    public void pauseJobs(final long tenantId) throws SSchedulerException {
        schedulerExecutor.pauseJobs(tenantId);
    }

    @Override
    public void resumeJobs(final long tenantId) throws SSchedulerException {
        schedulerExecutor.resumeJobs(tenantId);
    }

    @Override
    public boolean delete(final String jobName) throws SSchedulerException {
        logBeforeMethod(TechnicalLogSeverity.TRACE, "delete");
        boolean delete = schedulerExecutor.delete(jobName);
        jobService.deleteJobDescriptorByJobName(jobName);
        logAfterMethod(TechnicalLogSeverity.TRACE, "delete");
        return delete;
    }

    @Override
    public void deleteJobs() throws SSchedulerException {
        logBeforeMethod(TechnicalLogSeverity.TRACE, "deleteJobs");
        schedulerExecutor.deleteJobs();
        final List<FilterOption> filters = new ArrayList<FilterOption>();
        final QueryOptions queryOptions = new QueryOptions(0, 100, null,
                filters, null);
        try {
            final List<SJobDescriptor> jobDescriptors = jobService
                    .searchJobDescriptors(queryOptions);
            for (final SJobDescriptor sJobDescriptor : jobDescriptors) {
                jobService.deleteJobDescriptor(sJobDescriptor);
            }
        } catch (final SBonitaSearchException sbse) {
            throw new SSchedulerException(sbse);
        }
        logAfterMethod(TechnicalLogSeverity.TRACE, "deleteJobs");
    }

    @Override
    public List<String> getJobs() throws SSchedulerException {
        logBeforeMethod(TechnicalLogSeverity.TRACE, "getJobs");
        final List<String> list = schedulerExecutor.getJobs();
        logAfterMethod(TechnicalLogSeverity.TRACE, "getJobs");
        return list;
    }

    @Override
    public List<String> getAllJobs() throws SSchedulerException {
        logBeforeMethod(TechnicalLogSeverity.TRACE, "getAllJobs");
        final List<String> list = schedulerExecutor.getAllJobs();
        logAfterMethod(TechnicalLogSeverity.TRACE, "getAllJobs");
        return list;
    }

    /**
     * get the persisted job from the database It opens a transaction!
     * 
     * @param jobIdentifier
     * @return the job
     * @throws SSchedulerException
     */
    public StatelessJob getPersistedJob(final JobIdentifier jobIdentifier)
            throws SSchedulerException {
        logBeforeMethod(TechnicalLogSeverity.TRACE, "getPersistedJob");
        try {
            sessionAccessor.setTenantId(jobIdentifier.getTenantId());
            logAfterMethod(TechnicalLogSeverity.TRACE, "getPersistedJob");
            return transactionService.executeInTransaction(new PersistedJobCallable(jobIdentifier));
        } catch (final Exception e) {
            throw new SSchedulerException(
                    "The job class couldn't be instantiated", e);
        } finally {
            sessionAccessor.deleteTenantId();
        }
    }


    private class PersistedJobCallable implements Callable<JobWrapper> {

        private final JobIdentifier jobIdentifier;

        public PersistedJobCallable(JobIdentifier jobIdentifier) {
            this.jobIdentifier = jobIdentifier;
        }

            @Override
            public JobWrapper call() throws Exception {
                final SJobDescriptor sJobDescriptor = jobService
                        .getJobDescriptor(jobIdentifier.getId());
                // FIXME do something here if the job does not exist
                if (sJobDescriptor == null) {
                    return null;
                }
                final String jobClassName = sJobDescriptor.getJobClassName();
                final Class<?> jobClass = Class.forName(jobClassName);
                final StatelessJob statelessJob = (StatelessJob) jobClass
                        .newInstance();

            final FilterOption filterOption = new FilterOption(SJobParameter.class, "jobDescriptorId", jobIdentifier.getId());
            final QueryOptions queryOptions = new QueryOptions(0, QueryOptions.UNLIMITED_NUMBER_OF_RESULTS, null, Collections.singletonList(filterOption), null);
            final List<SJobParameter> parameters = jobService.searchJobParameters(queryOptions);
                final HashMap<String, Serializable> parameterMap = new HashMap<String, Serializable>();
                for (final SJobParameter sJobParameterImpl : parameters) {
                    parameterMap.put(sJobParameterImpl.getKey(),
                            sJobParameterImpl.getValue());
                }
                statelessJob.setAttributes(parameterMap);
                if (servicesResolver != null) {
                    injectServices(statelessJob);
                }
                final JobWrapper jobWrapper = new JobWrapper(
                        jobIdentifier.getJobName(), statelessJob, logger,
                        jobIdentifier.getTenantId(), eventService,
                        sessionAccessor, transactionService);
                return jobWrapper;
            }
    }

    /**
     * @param statelessJob
     */
    protected void injectServices(final StatelessJob statelessJob)
            throws Exception {
        Method[] methods = statelessJob.getClass().getMethods();
        for (Method method : methods) {
            if (method.getAnnotation(InjectedService.class) != null) {
                String serviceName = method.getName().substring(3);
                serviceName = serviceName.substring(0, 1).toLowerCase()
                        + serviceName.substring(1);
                Object lookup = servicesResolver.lookup(serviceName);
                method.invoke(statelessJob, lookup);
            }
        }
    }

    @Override
    public boolean isStillScheduled(final SJobDescriptor jobDescriptor)
            throws SSchedulerException {
        return schedulerExecutor.isStillScheduled(getTenantId(),
                jobDescriptor.getJobName());
    }

    @Override
    public void pause() throws SBonitaException, TimeoutException {
        pauseJobs(getTenantId());
    }

    @Override
    public void resume() throws SBonitaException {
        resumeJobs(getTenantId());
    }

    /*
     * (non-Javadoc)
     * @see
     * org.bonitasoft.engine.scheduler.SchedulerService#rescheduleErroneousTriggers
     * ()
     */
    @Override
    public void rescheduleErroneousTriggers() throws SSchedulerException {
        schedulerExecutor.rescheduleErroneousTriggers();
    }

}
