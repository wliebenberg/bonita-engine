/**
 * Copyright (C) 2013 BonitaSoft S.A.
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

import org.bonitasoft.engine.persistence.QueryOptions;
import org.bonitasoft.engine.persistence.SBonitaSearchException;
import org.bonitasoft.engine.scheduler.exception.failedJob.SFailedJobReadException;
import org.bonitasoft.engine.scheduler.exception.jobDescriptor.SJobDescriptorCreationException;
import org.bonitasoft.engine.scheduler.exception.jobDescriptor.SJobDescriptorDeletionException;
import org.bonitasoft.engine.scheduler.exception.jobDescriptor.SJobDescriptorNotFoundException;
import org.bonitasoft.engine.scheduler.exception.jobDescriptor.SJobDescriptorReadException;
import org.bonitasoft.engine.scheduler.exception.jobLog.SJobLogCreationException;
import org.bonitasoft.engine.scheduler.exception.jobLog.SJobLogDeletionException;
import org.bonitasoft.engine.scheduler.exception.jobLog.SJobLogNotFoundException;
import org.bonitasoft.engine.scheduler.exception.jobLog.SJobLogReadException;
import org.bonitasoft.engine.scheduler.exception.jobParameter.SJobParameterCreationException;
import org.bonitasoft.engine.scheduler.exception.jobParameter.SJobParameterDeletionException;
import org.bonitasoft.engine.scheduler.exception.jobParameter.SJobParameterNotFoundException;
import org.bonitasoft.engine.scheduler.exception.jobParameter.SJobParameterReadException;
import org.bonitasoft.engine.scheduler.model.SFailedJob;
import org.bonitasoft.engine.scheduler.model.SJobDescriptor;
import org.bonitasoft.engine.scheduler.model.SJobLog;
import org.bonitasoft.engine.scheduler.model.SJobParameter;

/**
 * @author Celine Souchet
 * @since 6.1
 */
public interface JobService {

    String JOB_DESCRIPTOR = "JOB_DESCRIPTOR";

    String JOB_PARAMETER = "JOB_PARAMETER";

    String JOB_LOG = "JOB_LOG";

    /**
     * Create a new job descriptor for a specific tenant
     * 
     * @param sJobDescriptor
     *            JobDescriptor to create
     * @param tenantId
     *            Identifier of tenant
     * @return The created jobDescriptor
     * @throws SJobDescriptorCreationException
     * @since 6.1
     */
    SJobDescriptor createJobDescriptor(SJobDescriptor sJobDescriptor, long tenantId) throws SJobDescriptorCreationException;

    /**
     * Delete the specified job descriptor
     * 
     * @param id
     *            Identifier of job descriptor to delete
     * @throws SJobDescriptorReadException
     * @throws SJobDescriptorNotFoundException
     * @throws SJobDescriptorDeletionException
     * @since 6.1
     */
    void deleteJobDescriptor(long id) throws SJobDescriptorNotFoundException, SJobDescriptorReadException, SJobDescriptorDeletionException;

    /**
     * Delete the specified job descriptor
     * 
     * @param sJobDescriptor
     *            JobDescriptor to delete
     * @throws SJobDescriptorDeletionException
     * @since 6.1
     */
    void deleteJobDescriptor(SJobDescriptor sJobDescriptor) throws SJobDescriptorDeletionException;
    
    /**
     * Delete a job descriptor corresponding to the given job name
     * 
     * @param jobName name of job we want the jobDsecriptor to be deleted
     * @since 6.3
     */
    void deleteJobDescriptorByJobName(String jobName) throws SJobDescriptorDeletionException;

    /**
     * Get a specific job descriptor
     * 
     * @param id
     *            Identifier of job descriptor
     * @return
     * @throws SJobDescriptorReadException
     * @throws SJobDescriptorNotFoundException
     * @since 6.1
     */
    SJobDescriptor getJobDescriptor(long id) throws SJobDescriptorNotFoundException, SJobDescriptorReadException;

    /**
     * Get total number of job descriptors
     * 
     * @param queryOptions
     *            a map of specific parameters of a query
     * @return total number of job logs
     * @throws SBonitaSearchException
     */
    long getNumberOfJobDescriptors(QueryOptions queryOptions) throws SBonitaSearchException;

    /**
     * Search all job descriptors according to specific criteria
     * 
     * @param queryOptions
     *            a map of specific parameters of a query
     * @return A list of SJobParameter objects
     * @throws SBonitaSearchException
     * @since 6.1
     */
    List<SJobDescriptor> searchJobDescriptors(QueryOptions queryOptions) throws SBonitaSearchException;

    /**
     * Create new job parameters for a specific tenant
     * 
     * @param sJobParameters
     *            JobParameters to create
     * @param tenantId
     *            Identifier of tenant
     * @param jobDescriptorId
     *            Identifier of job descriptor
     * @return
     * @throws SJobParameterCreationException
     * @since 6.2
     */
    List<SJobParameter> createJobParameters(List<SJobParameter> parameters, long tenantId, long jobDescriptorId) throws SJobParameterCreationException;

    /**
     * Delete jobs parameters corresponding to tenant and job descriptor, if exist. After, create new job parameters for a specific tenant
     * 
     * @param tenantId
     * @param jobDescriptorId
     * @param parameters
     * @return A list of new SJobParameter objects
     * @throws SJobParameterCreationException
     * @since 6.1
     */
    List<SJobParameter> setJobParameters(final long tenantId, long jobDescriptorId, List<SJobParameter> parameters) throws SJobParameterCreationException;

    /**
     * Create a new job parameter for a specific tenant
     * 
     * @param sJobParameter
     *            JobParameter to create
     * @param tenantId
     *            Identifier of tenant
     * @param jobDescriptorId
     *            Identifier of job descriptor
     * @return
     * @throws SJobParameterCreationException
     * @since 6.2
     */
    SJobParameter createJobParameter(SJobParameter sJobParameter, long tenantId, long jobDescriptorId) throws SJobParameterCreationException;

    /**
     * Delete the specified job parameter
     * 
     * @param id
     *            Identifier of job parameter to delete
     * @throws SJobParameterReadException
     * @throws SJobParameterNotFoundException
     * @throws SJobParameterDeletionException
     * @since 6.1
     */
    void deleteJobParameter(long id) throws SJobParameterNotFoundException, SJobParameterReadException, SJobParameterDeletionException;

    /**
     * Delete the specified job parameter
     * 
     * @param sJobParameter
     *            JobParameter to delete
     * @throws SJobParameterDeletionException
     * @since 6.1
     */
    void deleteJobParameter(SJobParameter sJobParameter) throws SJobParameterDeletionException;

    /**
     * Get a specific job parameter
     * 
     * @param id
     *            Identifier of job parameter
     * @return
     * @throws SJobParameterReadException
     * @throws SJobParameterNotFoundException
     * @since 6.1
     */
    SJobParameter getJobParameter(long id) throws SJobParameterNotFoundException, SJobParameterReadException;

    /**
     * Search all job parameters according to specific criteria
     * 
     * @param queryOptions
     *            a map of specific parameters of a query
     * @return A list of SJobParameter objects
     * @throws SBonitaSearchException
     * @since 6.1
     */
    List<SJobParameter> searchJobParameters(QueryOptions queryOptions) throws SBonitaSearchException;

    /**
     * Create a new job log for a specific tenant
     * 
     * @param sJobLog
     *            JobLog to create
     * @return
     * @throws SJobLogCreationException
     * @since 6.2
     */
    SJobLog createJobLog(SJobLog sJobLog) throws SJobLogCreationException;

    /**
     * Delete the specified job log
     * 
     * @param id
     *            Identifier of job log to delete
     * @throws SJobLogReadException
     * @throws SJobLogNotFoundException
     * @throws SJobLogDeletionException
     * @since 6.1
     */
    void deleteJobLog(long id) throws SJobLogNotFoundException, SJobLogReadException, SJobLogDeletionException;

    /**
     * Delete the specified job log
     * 
     * @param sJobLog
     *            JobLog to delete
     * @throws SJobLogDeletionException
     * @since 6.1
     */
    void deleteJobLog(SJobLog sJobLog) throws SJobLogDeletionException;

    /**
     * Get a specific job log
     * 
     * @param id
     *            Identifier of job log
     * @return
     * @throws SJobLogReadException
     * @throws SJobLogNotFoundException
     * @since 6.1
     */
    SJobLog getJobLog(long id) throws SJobLogNotFoundException, SJobLogReadException;

    /**
     * Get total number of job logs
     * 
     * @param queryOptions
     *            a map of specific parameters of a query
     * @return total number of job logs
     * @throws SBonitaSearchException
     */
    long getNumberOfJobLogs(QueryOptions queryOptions) throws SBonitaSearchException;

    /**
     * Search all job logs according to specific criteria
     * 
     * @param queryOptions
     *            a map of specific parameters of a query
     * @return A list of SJobLog objects
     * @throws SBonitaSearchException
     * @since 6.1
     */
    List<SJobLog> searchJobLogs(QueryOptions queryOptions) throws SBonitaSearchException;

    /**
     * Get list of failed jobs
     * 
     * @param startIndex
     * @param maxResults
     * @return A list of SFailedJob objects
     * @throws SFailedJobReadException
     * @since 6.2
     */
    List<SFailedJob> getFailedJobs(int startIndex, int maxResults) throws SFailedJobReadException;

}
