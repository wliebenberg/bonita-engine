/**
 * Copyright (C) 2014 BonitaSoft S.A.
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
 ** 
 * @since 6.2
 */
package org.bonitasoft.engine.api.impl;

import java.util.List;

import org.bonitasoft.engine.commons.CollectionUtil;
import org.bonitasoft.engine.scheduler.JobRegister;

/**
 * 
 * Bean that returns configuration
 * 
 * @author Baptiste Mesta
 * 
 */
public class TenantConfiguration {

    private String eventHandlingJobCron = "*/5 * * * * ?";

    private String cleanInvalidSessionsJobCron = "0 0 */2 * * ?";

    private List<JobRegister> jobsToRegister;

    /**
     * Specify how often we look for matching matching event couple
     * 
     * @return a String representing a Unix Cron
     */
    public String getEventHandlingJobCron() {
        return eventHandlingJobCron;
    }

    public void setEventHandlingJobCron(final String eventHandlingJobCron) {
        this.eventHandlingJobCron = eventHandlingJobCron;
    }

    public void setJobsToRegister(final List<JobRegister> jobsToRegister) {
        this.jobsToRegister = jobsToRegister;
    }

    /**
     * Give a list of job to register when the tenant is activated
     * 
     * @return
     */
    public List<JobRegister> getJobsToRegister() {
        return CollectionUtil.emptyOrUnmodifiable(jobsToRegister);
    }

    /**
     * Specify how often invalid sessions will be cleaned
     * 
     * @return a String representing a Unix Cron
     */
    public String getCleanInvalidSessionsJobCron() {
        return cleanInvalidSessionsJobCron;
    }

    public void setCleanInvalidSessionsJobCron(final String cleanInvalidSessionsJobCron) {
        this.cleanInvalidSessionsJobCron = cleanInvalidSessionsJobCron;
    }

}
