/**
 * Copyright (C) 2013 BonitaSoft S.A.
 * BonitaSoft, 32 rue Gustave Eiffel - 38000 Grenoble
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2.0 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.bonitasoft.engine.test.wait;

import static org.bonitasoft.engine.bpm.process.ArchivedProcessInstancesSearchDescriptor.SOURCE_OBJECT_ID;
import static org.bonitasoft.engine.bpm.process.ArchivedProcessInstancesSearchDescriptor.STATE_ID;

import java.util.List;

import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.bpm.process.ArchivedProcessInstance;
import org.bonitasoft.engine.bpm.process.ProcessInstance;
import org.bonitasoft.engine.exception.SearchException;
import org.bonitasoft.engine.search.SearchOptionsBuilder;
import org.bonitasoft.engine.test.APITestUtil;
import org.bonitasoft.engine.test.TestStates;
import org.bonitasoft.engine.test.WaitUntil;

@Deprecated
public final class WaitProcessToFinishAndBeArchived extends WaitUntil {

    private final long processInstanceId;

    private final ProcessAPI processAPI;

    private String state = null;

    @Deprecated
    public WaitProcessToFinishAndBeArchived(final int repeatEach, final int timeout, final boolean throwExceptions, final ProcessInstance processInstance,
            final ProcessAPI processAPI, final String state) {
        super(repeatEach, timeout, throwExceptions);
        processInstanceId = processInstance.getId();
        this.processAPI = processAPI;
        this.state = state;
    }

    @Deprecated
    public WaitProcessToFinishAndBeArchived(final int repeatEach, final int timeout, final boolean throwExceptions, final long processInstanceId,
            final ProcessAPI processAPI, final String state) {
        super(repeatEach, timeout, throwExceptions);
        this.processInstanceId = processInstanceId;
        this.processAPI = processAPI;
        this.state = state;
    }

    @Deprecated
    public WaitProcessToFinishAndBeArchived(final int repeatEach, final int timeout, final boolean throwExceptions, final long processInstanceId,
            final ProcessAPI processAPI) {
        super(repeatEach, timeout, throwExceptions);
        this.processInstanceId = processInstanceId;
        this.processAPI = processAPI;
    }

    @Deprecated
    public WaitProcessToFinishAndBeArchived(final int repeatEach, final int timeout, final boolean throwExceptions, final ProcessInstance processInstance,
            final ProcessAPI processAPI) {
        this(repeatEach, timeout, throwExceptions, processInstance, processAPI, TestStates.getNormalFinalState());
    }

    @Deprecated
    public WaitProcessToFinishAndBeArchived(final int repeatEach, final int timeout, final long processInstanceId, final ProcessAPI processAPI) {
        this(repeatEach, timeout, true, processInstanceId, processAPI);
    }

    @Deprecated
    public WaitProcessToFinishAndBeArchived(final int repeatEach, final int timeout, final ProcessInstance processInstance, final ProcessAPI processAPI) {
        this(repeatEach, timeout, false, processInstance, processAPI);
    }

    @Override
    protected boolean check() throws SearchException {
        if (state != null) {
            final List<ArchivedProcessInstance> archivedProcessInstances = processAPI.getArchivedProcessInstances(processInstanceId, 0, 20);
            return APITestUtil.containsState(archivedProcessInstances, state);
        } else {
            return processAPI.searchArchivedProcessInstances(
                    new SearchOptionsBuilder(0, 20).filter(SOURCE_OBJECT_ID, processInstanceId).filter(STATE_ID, 6).done()).getCount() == 1;
        }
    }

}
