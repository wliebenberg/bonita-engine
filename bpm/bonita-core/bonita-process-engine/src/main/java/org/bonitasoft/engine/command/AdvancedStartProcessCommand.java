/**
 * Copyright (C) 2013-2014 BonitaSoft S.A.
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
package org.bonitasoft.engine.command;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.bonitasoft.engine.api.impl.ProcessStarter;
import org.bonitasoft.engine.bpm.process.ProcessInstance;
import org.bonitasoft.engine.command.system.CommandWithParameters;
import org.bonitasoft.engine.execution.AdvancedStartProcessValidator;
import org.bonitasoft.engine.operation.Operation;
import org.bonitasoft.engine.service.TenantServiceAccessor;

/**
 * This command starts the process in the specified activity. Connectors on process start will be executed.
 * Parameters:
 * - started_by: the user id (long) is used as the process starter. It's a mandatory parameter.
 * - process_definition_id: the process definition id (long) identifies the process to start. It's a mandatory parameter.
 * - activity_name: the name of the activity (String) where the process will start the execution. It's a mandatory
 * parameter.
 * - operations: the operations (ArrayList<Operation>) are executed when the process starts (set variables and documents). It's an optional parameter.
 * - context: the context (HashMap<String, Serializable>) is used during operations execution. It's an optional parameter.
 * Limitations:
 * - It is not possible to start the execution of a process from a gateway, a boundary event or an event sub-process
 * - The process must be started when there is only one active branch.
 * Example:
 * start -> step1 -> gateway1 -> (step2 || step3) -> gateway2 -> step4 -> end
 * - Ok: start from "start" or "step1" or "step4" or "end"
 * - All other start points are invalid.
 * 
 * @author Vincent Elcrin
 */
public class AdvancedStartProcessCommand extends CommandWithParameters {

    public static final String STARTED_BY = "started_by";

    public static final String PROCESS_DEFINITION_ID = "process_definition_id";

    public static final String ACTIVITY_NAME = "activity_name";

    public static final String OPERATIONS = "operations";

    public static final String CONTEXT = "context";

    @Override
    public Serializable execute(final Map<String, Serializable> parameters, final TenantServiceAccessor serviceAccessor)
            throws SCommandParameterizationException, SCommandExecutionException {
        // get parameters
        final long processDefinitionId = getProcessDefinitionId(parameters);
        final List<String> activityNames = Collections.singletonList(getActivityName(parameters));
        final long startedBy = getStartedBy(parameters);
        final Map<String, Serializable> context = getContext(parameters);
        final List<Operation> operations = getOperations(parameters);

        ProcessInstance processInstance;
        try {
            // validate inputs
            final AdvancedStartProcessValidator validator = new AdvancedStartProcessValidator(serviceAccessor.getProcessDefinitionService(),
                    processDefinitionId);
            final List<String> problems = validator.validate(activityNames);
            handleProblems(problems);

            // start the process
            final ProcessStarter starter = new ProcessStarter(startedBy, processDefinitionId, operations, context, activityNames);
            processInstance = starter.start();
        } catch (final Exception e) {
            throw new SCommandExecutionException(e);
        }
        return processInstance;
    }

    private void handleProblems(final List<String> problems) throws SCommandExecutionException {
        if (!problems.isEmpty()) {
            final StringBuilder stb = new StringBuilder();
            for (final String problem : problems) {
                stb.append(problem);
                stb.append("\n");
            }
            throw new SCommandExecutionException(stb.toString());
        }
    }

    private Long getStartedBy(final Map<String, Serializable> parameters) throws SCommandParameterizationException {
        return getMandatory(STARTED_BY, parameters);
    }

    private Long getProcessDefinitionId(final Map<String, Serializable> parameters) throws SCommandParameterizationException {
        return getMandatory(PROCESS_DEFINITION_ID, parameters);
    }

    private List<Operation> getOperations(final Map<String, Serializable> parameters) throws SCommandParameterizationException {
        return get(OPERATIONS, parameters);
    }

    private Map<String, Serializable> getContext(final Map<String, Serializable> parameters) throws SCommandParameterizationException {
        return get(CONTEXT, parameters);
    }

    private String getActivityName(final Map<String, Serializable> parameters) throws SCommandParameterizationException {
        return getMandatory(ACTIVITY_NAME, parameters);
    }

    private <T> T get(final String parameter, final Map<String, Serializable> parameters) throws SCommandParameterizationException {
        return getParameter(parameters, parameter, "An error occurred while parsing " + parameter);
    }

    private <T> T getMandatory(final String parameter, final Map<String, Serializable> parameters) throws SCommandParameterizationException {
        return getMandatoryParameter(parameters, parameter, "Missing mandatory field: " + parameter);
    }

}
