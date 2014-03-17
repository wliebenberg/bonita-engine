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
package org.bonitasoft.engine.scheduler;

import java.util.List;

import org.bonitasoft.engine.commons.ServiceWithLifecycle;
import org.bonitasoft.engine.scheduler.exception.SSchedulerException;
import org.bonitasoft.engine.scheduler.model.SJobDescriptor;
import org.bonitasoft.engine.scheduler.model.SJobParameter;
import org.bonitasoft.engine.scheduler.trigger.Trigger;

/**
 * @author Matthieu Chaffotte
 * @since 6.0
 */
public interface SchedulerService extends ServiceWithLifecycle {

    String JOB_DESCRIPTOR = "JOB_DESCRIPTOR";

    String JOB_PARAMETER = "JOB_PARAMETER";

    /**
     * This service will fire the following events :
     * <ul>
     * <li>SCHEDULER_STARTED = "SCHEDULER_STARTED"</li>
     * <li>SCHEDULER_STOPPED = "SCHEDULER_STOPPED"</li>
     * <li>JOB_FAILED = "JOB_FAILED"</li>
     * </ul>
     */
    String SCHEDULER_STARTED = "SCHEDULER_STARTED";

    String SCHEDULER_STOPPED = "SCHEDULER_STOPPED";

    String JOB_FAILED = "JOB_FAILED";

    /**
     * Checks whether the service is started.
     * 
     * @return true if the service is started; false otherwise.
     * @throws SSchedulerException
     *             if an exception occurs.
     */
    boolean isStarted() throws SSchedulerException;

    /**
     * Checks whether the service is shutdown.
     * 
     * @return true if the service is shutdown; false otherwise.
     * @throws SSchedulerException
     */
    boolean isStopped() throws SSchedulerException;

    /**
     * Schedules a job.
     * 
     * @param jobDescriptor
     * @param trigger
     * @throws SSchedulerException
     *             if an exception occurs.
     */
    void schedule(SJobDescriptor jobDescriptor, Trigger trigger) throws SSchedulerException;

    /**
     * Schedules a job.
     * 
     * @param jobDescriptor
     * @param jobParameters
     * @param trigger
     * @throws SSchedulerException
     *             if an exception occurs.
     */
    void schedule(SJobDescriptor jobDescriptor, List<SJobParameter> parameters, Trigger trigger) throws SSchedulerException;

    void executeAgain(long jobDescriptorId) throws SSchedulerException;

    /**
     * Schedules a job.
     * 
     * @param jobDescriptorId
     * @param jobParameters
     * @throws SSchedulerException
     *             if an exception occurs.
     */
    void executeAgain(long jobDescriptorId, List<SJobParameter> parameters) throws SSchedulerException;

    /**
     * execute a job.
     * 
     * @param jobDescriptor
     * @param jobParameters
     * @throws SSchedulerException
     *             if an exception occurs.
     */
    void executeNow(SJobDescriptor jobDescriptor, List<SJobParameter> parameters) throws SSchedulerException;

    /**
     * Deletes a job according to its name.
     * 
     * @param jobName
     *            the job name
     * @return true if delete a job, otherwise return false.
     * @throws SSchedulerException
     *             if an exception occurs.
     */
    boolean delete(String jobName) throws SSchedulerException;

    /**
     * Deletes all jobs.
     * 
     * @throws SSchedulerException
     *             if an exception occurs.
     */
    void deleteJobs() throws SSchedulerException;

    /**
     * Get all jobs on the current tenant
     * 
     * @return all jobs on the current tenant
     * @throws SSchedulerException
     *             if an exception occurs.
     */
    List<String> getJobs() throws SSchedulerException;

    /**
     * Get all jobs on all tenants
     * \/!\Must be replaced by a platform scheduler/!\
     * 
     * @return all jobs on the current tenant
     * @throws SSchedulerException
     *             if an exception occurs.
     */
    List<String> getAllJobs() throws SSchedulerException;

    boolean isStillScheduled(SJobDescriptor jobDescriptor) throws SSchedulerException;

    void rescheduleErroneousTriggers() throws SSchedulerException;

    /**
     * 
     * Pause all jobs running on the tenant
     * 
     * @param tenantId
     * @throws SSchedulerException
     */
    void pauseJobs(long tenantId) throws SSchedulerException;

    /**
     * Resume all jobs paused on the tenant
     * 
     * @param tenantId
     * @throws SSchedulerException
     */
    void resumeJobs(long tenantId) throws SSchedulerException;

}
