/**
 * Copyright (C) 2012-2013 BonitaSoft S.A.
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
package org.bonitasoft.engine.api.impl;

import java.util.List;

import org.bonitasoft.engine.commons.CollectionUtil;
import org.bonitasoft.engine.commons.RestartHandler;
import org.bonitasoft.engine.execution.work.TenantRestartHandler;

/**
 * @author Baptiste Mesta
 * @author Matthieu Chaffotte
 * @author Laurent Vaills
 */
public class NodeConfigurationImpl implements NodeConfiguration {

    private boolean shouldStartScheduler = true;

    private boolean shouldRestartElements = true;

    private List<RestartHandler> restartHandlers;

    private boolean shouldStartEventHandlingJob = true;

    private List<TenantRestartHandler> tenantRestartHandlers;

    @Override
    public boolean shouldStartScheduler() {
        return shouldStartScheduler;
    }

    @Override
    public boolean shouldResumeElements() {
        return shouldRestartElements;
    }

    @Override
    public List<RestartHandler> getRestartHandlers() {
        return CollectionUtil.emptyOrUnmodifiable(restartHandlers);
    }

    public void setRestartHandlers(final List<RestartHandler> restartHandlers) {
        this.restartHandlers = restartHandlers;
    }

    @Override
    public List<TenantRestartHandler> getTenantRestartHandlers() {
        return CollectionUtil.emptyOrUnmodifiable(tenantRestartHandlers);
    }

    public void setTenantRestartHandlers(final List<TenantRestartHandler> tenantRestartHandlers) {
        this.tenantRestartHandlers = tenantRestartHandlers;
    }

    @Override
    public boolean shouldStartEventHandlingJob() {
        return shouldStartEventHandlingJob;
    }

    public void setShouldStartScheduler(final boolean shouldStartScheduler) {
        this.shouldStartScheduler = shouldStartScheduler;
    }

    public void setShouldRestartElements(final boolean shouldRestartElements) {
        this.shouldRestartElements = shouldRestartElements;
    }

    public void setShouldStartEventHandlingJob(final boolean shouldStartEventHandlingJob) {
        this.shouldStartEventHandlingJob = shouldStartEventHandlingJob;
    }

    @Override
    public boolean shouldClearSessions() {
        return true;
    }

}
