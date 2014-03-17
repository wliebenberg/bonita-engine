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
package org.bonitasoft.engine.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.bonitasoft.engine.BPMRemoteTests;
import org.bonitasoft.engine.bpm.bar.BarResource;
import org.bonitasoft.engine.bpm.bar.BusinessArchive;
import org.bonitasoft.engine.bpm.bar.BusinessArchiveBuilder;
import org.bonitasoft.engine.bpm.connector.ConnectorEvent;
import org.bonitasoft.engine.bpm.connector.ConnectorInstance;
import org.bonitasoft.engine.bpm.connector.ConnectorInstancesSearchDescriptor;
import org.bonitasoft.engine.bpm.flownode.ActivityInstance;
import org.bonitasoft.engine.bpm.flownode.HumanTaskInstance;
import org.bonitasoft.engine.bpm.flownode.TimerType;
import org.bonitasoft.engine.bpm.flownode.impl.FlowElementContainerDefinitionImpl;
import org.bonitasoft.engine.bpm.process.ConfigurationState;
import org.bonitasoft.engine.bpm.process.Problem;
import org.bonitasoft.engine.bpm.process.ProcessDefinition;
import org.bonitasoft.engine.bpm.process.ProcessDeploymentInfo;
import org.bonitasoft.engine.bpm.process.ProcessInstance;
import org.bonitasoft.engine.bpm.process.impl.AutomaticTaskDefinitionBuilder;
import org.bonitasoft.engine.bpm.process.impl.ProcessDefinitionBuilder;
import org.bonitasoft.engine.bpm.process.impl.UserTaskDefinitionBuilder;
import org.bonitasoft.engine.cache.CacheService;
import org.bonitasoft.engine.connector.Connector;
import org.bonitasoft.engine.connectors.ConnectorExecutionTest;
import org.bonitasoft.engine.connectors.VariableStorage;
import org.bonitasoft.engine.exception.BonitaException;
import org.bonitasoft.engine.exception.BonitaRuntimeException;
import org.bonitasoft.engine.exception.SearchException;
import org.bonitasoft.engine.expression.Expression;
import org.bonitasoft.engine.expression.ExpressionBuilder;
import org.bonitasoft.engine.expression.ExpressionConstants;
import org.bonitasoft.engine.operation.OperationBuilder;
import org.bonitasoft.engine.operation.OperatorType;
import org.bonitasoft.engine.search.SearchOptionsBuilder;
import org.bonitasoft.engine.search.SearchResult;
import org.bonitasoft.engine.service.TenantServiceAccessor;
import org.bonitasoft.engine.service.TenantServiceSingleton;
import org.bonitasoft.engine.service.impl.ServiceAccessorFactory;
import org.bonitasoft.engine.sessionaccessor.SessionAccessor;
import org.bonitasoft.engine.test.annotation.Cover;
import org.bonitasoft.engine.test.annotation.Cover.BPMNConcept;
import org.junit.Test;

/**
 * @author Baptiste Mesta
 * @author Celine Souchet
 * @author Elias Ricken de Medeiros
 */
@SuppressWarnings("javadoc")
public class ConnectorExecutionsTestsLocal extends ConnectorExecutionTest {

    protected TenantServiceAccessor getTenantAccessor() {
        try {
            final SessionAccessor sessionAccessor = ServiceAccessorFactory.getInstance().createSessionAccessor();
            final long tenantId = sessionAccessor.getTenantId();
            return TenantServiceSingleton.getInstance(tenantId);
        } catch (final Exception e) {
            throw new BonitaRuntimeException(e);
        }
    }

    @Cover(classes = Connector.class, concept = BPMNConcept.CONNECTOR, keywords = { "Connector", "On finish", "Data input", "Automatic activity" }, story = "Test connector on finish of an automatic activity with data input.", jira = "")
    @Test
    public void executeConnectorOnFinishOfAnAutomaticActivityWithDataAsInput() throws Exception {
        final String valueOfInput1 = "valueOfInput1";
        final Expression input1Expression = new ExpressionBuilder().createConstantStringExpression(valueOfInput1);
        final String dataName = "myData1";
        final String inputName = "input1";
        final Expression myData1Expression = new ExpressionBuilder().createDataExpression(dataName, String.class.getName());
        final ProcessDefinitionBuilder designProcessDefinition = new ProcessDefinitionBuilder().createNewInstance(
                "executeConnectorOnFinishOfAnAutomaticActivityWithDataAsInput", "1.0");
        designProcessDefinition.addShortTextData(dataName, input1Expression);
        designProcessDefinition.addActor(ACTOR_NAME).addDescription(DESCRIPTION);
        designProcessDefinition.addAutomaticTask("step1")
                .addConnector("myConnector", "org.bonitasoft.connector.testConnector", "1.0", ConnectorEvent.ON_FINISH).addInput(inputName, myData1Expression);
        designProcessDefinition.addUserTask("step2", ACTOR_NAME);
        designProcessDefinition.addTransition("step1", "step2");

        final ProcessDefinition processDefinition = deployProcessWithDefaultTestConnector(ACTOR_NAME, johnUserId, designProcessDefinition, false);
        final ProcessInstance processInstance = getProcessAPI().startProcess(processDefinition.getId());
        waitForUserTask("step2", processInstance);
        assertTrue(VariableStorage.getInstance().getVariableValue(inputName).equals(valueOfInput1));

        disableAndDeleteProcess(processDefinition);
    }

    @Cover(classes = Connector.class, concept = BPMNConcept.CONNECTOR, keywords = { "Connector, Several, On start, User task" }, jira = "ENGINE-472", story = "Test of several connectors on start of an user task.")
    @Test
    public void executeSeveralConnectorsOnUserTaskOnStart() throws Exception {
        final String valueOfInput1 = "valueOfInput1";
        final Expression input1Expression = new ExpressionBuilder().createConstantStringExpression(valueOfInput1);
        final String valueOfInput2 = "valueOfInput2";
        final Expression input2Expression = new ExpressionBuilder().createConstantStringExpression(valueOfInput2);
        final String inputName1 = "input1";
        final String inputName2 = "input2";

        final ProcessDefinitionBuilder processBuilder = new ProcessDefinitionBuilder().createNewInstance("executeSeveralConnectorsOnUserTaskOnStart", "1.0");
        processBuilder.addActor(ACTOR_NAME).addDescription(DESCRIPTION);

        final UserTaskDefinitionBuilder taskBuilder = new UserTaskDefinitionBuilder(processBuilder, (FlowElementContainerDefinitionImpl) processBuilder
                .getProcess().getProcessContainer(), "step1", ACTOR_NAME);
        taskBuilder.addConnector("myConnector1", "org.bonitasoft.connector.testConnector3", "1.0", ConnectorEvent.ON_ENTER).addInput(inputName1,
                input1Expression);
        taskBuilder.addConnector("myConnector2", "org.bonitasoft.connector.testConnector3", "1.0", ConnectorEvent.ON_ENTER).addInput(inputName2,
                input2Expression);

        processBuilder.addUserTask("step2", ACTOR_NAME).addTransition("step1", "step2");

        final ProcessDefinition processDefinition = deployProcessWithDefaultTestConnector(ACTOR_NAME, johnUserId, processBuilder, false);
        final ProcessInstance processInstance = getProcessAPI().startProcess(processDefinition.getId());
        final ActivityInstance task1 = waitForUserTask("step1", processInstance);

        waitUntilStore2Variables(valueOfInput1, valueOfInput2, inputName1, inputName2);
        assignAndExecuteStep(task1, johnUserId);

        final ActivityInstance task2 = waitForUserTask("step2", processInstance);
        assignAndExecuteStep(task2, johnUserId);

        waitForProcessToFinish(processInstance);
        disableAndDeleteProcess(processDefinition);
    }

    @Cover(classes = Connector.class, concept = BPMNConcept.CONNECTOR, keywords = { "Connector, Several, On start, Automatic task" }, jira = "ENGINE-472", story = "Test of several connectors on start of an automatic task.")
    @Test
    public void executeSeveralConnectorsOnAutomaticTaskOnStart() throws Exception {
        final String valueOfInput1 = "valueOfInput1";
        final Expression input1Expression = new ExpressionBuilder().createConstantStringExpression(valueOfInput1);
        final String valueOfInput2 = "valueOfInput2";
        final Expression input2Expression = new ExpressionBuilder().createConstantStringExpression(valueOfInput2);
        final String inputName1 = "input1";
        final String inputName2 = "input2";

        final ProcessDefinitionBuilder processBuilder = new ProcessDefinitionBuilder().createNewInstance("executeSeveralConnectorsOnAutomaticTaskOnStart",
                "1.0");
        processBuilder.addActor(ACTOR_NAME).addDescription(DESCRIPTION);
        final AutomaticTaskDefinitionBuilder taskBuilder = new AutomaticTaskDefinitionBuilder(processBuilder,
                (FlowElementContainerDefinitionImpl) processBuilder.getProcess().getProcessContainer(), "step1");
        taskBuilder.addConnector("myConnector1", "org.bonitasoft.connector.testConnector3", "1.0", ConnectorEvent.ON_ENTER).addInput(inputName1,
                input1Expression);
        taskBuilder.addConnector("myConnector2", "org.bonitasoft.connector.testConnector3", "1.0", ConnectorEvent.ON_ENTER).addInput(inputName2,
                input2Expression);
        processBuilder.addAutomaticTask("step2").addTransition("step1", "step2");

        final ProcessDefinition processDefinition = deployProcessWithDefaultTestConnector(ACTOR_NAME, johnUserId, processBuilder, false);
        final ProcessInstance processInstance = getProcessAPI().startProcess(processDefinition.getId());

        waitUntilStore2Variables(valueOfInput1, valueOfInput2, inputName1, inputName2);
        waitForProcessToFinish(processInstance);

        disableAndDeleteProcess(processDefinition);
    }

    @Cover(classes = Connector.class, concept = BPMNConcept.CONNECTOR, keywords = { "Connector, Several, On finish, User task" }, jira = "ENGINE-472", story = "Test of several connectors on finish of an user task.")
    @Test
    public void executeSeveralConnectorsOnUserTaskOnFinish() throws Exception {
        final String valueOfInput1 = "valueOfInput1";
        final Expression input1Expression = new ExpressionBuilder().createConstantStringExpression(valueOfInput1);
        final String valueOfInput2 = "valueOfInput2";
        final Expression input2Expression = new ExpressionBuilder().createConstantStringExpression(valueOfInput2);
        final String inputName1 = "input1";
        final String inputName2 = "input2";

        final ProcessDefinitionBuilder processBuilder = new ProcessDefinitionBuilder().createNewInstance("executeSeveralConnectorsOnUserTaskOnFinish", "1.0");
        processBuilder.addActor(ACTOR_NAME).addDescription(DESCRIPTION);
        final UserTaskDefinitionBuilder taskBuilder = new UserTaskDefinitionBuilder(processBuilder, (FlowElementContainerDefinitionImpl) processBuilder
                .getProcess().getProcessContainer(), "step1", ACTOR_NAME);
        taskBuilder.addConnector("myConnector1", "org.bonitasoft.connector.testConnector3", "1.0", ConnectorEvent.ON_FINISH).addInput(inputName1,
                input1Expression);
        taskBuilder.addConnector("myConnector2", "org.bonitasoft.connector.testConnector3", "1.0", ConnectorEvent.ON_FINISH).addInput(inputName2,
                input2Expression);
        processBuilder.addUserTask("step2", ACTOR_NAME).addTransition("step1", "step2");

        final ProcessDefinition processDefinition = deployProcessWithDefaultTestConnector(ACTOR_NAME, johnUserId, processBuilder, false);
        final ProcessInstance processInstance = getProcessAPI().startProcess(processDefinition.getId());

        final ActivityInstance task1 = waitForUserTask("step1", processInstance);
        assignAndExecuteStep(task1, johnUserId);

        final ActivityInstance task2 = waitForUserTask("step2", processInstance);
        waitUntilStore2Variables(valueOfInput1, valueOfInput2, inputName1, inputName2);

        assignAndExecuteStep(task2, johnUserId);
        waitForProcessToFinish(processInstance);

        disableAndDeleteProcess(processDefinition);
    }

    private void waitUntilStore2Variables(final String valueOfInput1, final String valueOfInput2, final String inputName1, final String inputName2)
            throws Exception {
        final WaitUntil waitUntil = new WaitUntil(50, DEFAULT_TIMEOUT, false) {

            @Override
            protected boolean check() {
                final boolean check1 = VariableStorage.getInstance().getVariableValue(inputName1).equals(valueOfInput1);
                final boolean check2 = VariableStorage.getInstance().getVariableValue(inputName2).equals(valueOfInput2);
                return check1 && check2;
            }
        };
        assertTrue(waitUntil.waitUntil());
    }

    @Cover(classes = Connector.class, concept = BPMNConcept.CONNECTOR, keywords = { "Connector, Several, On finish, Automatic task" }, jira = "ENGINE-472", story = "Test of several connectors on finish of an automatic task.")
    @Test
    public void executeSeveralConnectorsOnAutomaticTaskOnFinish() throws Exception {
        final String valueOfInput1 = "valueOfInput1";
        final Expression input1Expression = new ExpressionBuilder().createConstantStringExpression(valueOfInput1);
        final String valueOfInput2 = "valueOfInput2";
        final Expression input2Expression = new ExpressionBuilder().createConstantStringExpression(valueOfInput2);
        final String inputName1 = "input1";
        final String inputName2 = "input2";

        final ProcessDefinitionBuilder processBuilder = new ProcessDefinitionBuilder().createNewInstance("executeSeveralConnectorsOnAutomaticTaskOnFinish",
                "1.0");
        processBuilder.addActor(ACTOR_NAME).addDescription(DESCRIPTION);

        final AutomaticTaskDefinitionBuilder taskBuilder = new AutomaticTaskDefinitionBuilder(processBuilder,
                (FlowElementContainerDefinitionImpl) processBuilder.getProcess().getProcessContainer(), "step1");
        taskBuilder.addConnector("myConnector1", "org.bonitasoft.connector.testConnector3", "1.0", ConnectorEvent.ON_FINISH).addInput(inputName1,
                input1Expression);
        taskBuilder.addConnector("myConnector2", "org.bonitasoft.connector.testConnector3", "1.0", ConnectorEvent.ON_FINISH).addInput(inputName2,
                input2Expression);

        final ProcessDefinition processDefinition = deployProcessWithDefaultTestConnector(ACTOR_NAME, johnUserId, processBuilder, false);
        final ProcessInstance processInstance = getProcessAPI().startProcess(processDefinition.getId());

        waitUntilStore2Variables(valueOfInput1, valueOfInput2, inputName1, inputName2);
        waitForProcessToFinish(processInstance);

        disableAndDeleteProcess(processDefinition);
    }

    @Cover(classes = Connector.class, concept = BPMNConcept.CONNECTOR, keywords = { "Connector, Several, On start, On finish" }, jira = "ENGINE-472", story = "Test of several connectors on start and finish of an user task.")
    @Test
    public void executeSeveralConnectorsOnStartAndOnFinishWithDataInput() throws Exception {
        final String valueOfInput1 = "valueOfInput1";
        final Expression input1Expression = new ExpressionBuilder().createConstantStringExpression(valueOfInput1);
        final String valueOfInput2 = "valueOfInput2";
        final Expression input2Expression = new ExpressionBuilder().createConstantStringExpression(valueOfInput2);
        final String valueOfInput3 = "valueOfInput3";
        final Expression input3Expression = new ExpressionBuilder().createConstantStringExpression(valueOfInput3);
        final String valueOfInput4 = "valueOfInput4";
        final Expression input4Expression = new ExpressionBuilder().createConstantStringExpression(valueOfInput4);
        final String inputName1 = "input1";
        final String inputName2 = "input2";
        final String inputName3 = "input3";
        final String inputName4 = "input4";

        final ProcessDefinitionBuilder processBuilder = new ProcessDefinitionBuilder().createNewInstance("executeSeveralConnectorsOnStartAndOnFinish", "1.0");
        processBuilder.addActor(ACTOR_NAME).addDescription(DESCRIPTION);

        final UserTaskDefinitionBuilder taskBuilder = new UserTaskDefinitionBuilder(processBuilder, (FlowElementContainerDefinitionImpl) processBuilder
                .getProcess().getProcessContainer(), "step1", ACTOR_NAME);
        taskBuilder.addConnector("myConnector1", "org.bonitasoft.connector.testConnector3", "1.0", ConnectorEvent.ON_ENTER).addInput(inputName1,
                input1Expression);
        taskBuilder.addConnector("myConnector2", "org.bonitasoft.connector.testConnector3", "1.0", ConnectorEvent.ON_ENTER).addInput(inputName2,
                input2Expression);
        taskBuilder.addConnector("myConnector3", "org.bonitasoft.connector.testConnector3", "1.0", ConnectorEvent.ON_FINISH).addInput(inputName3,
                input3Expression);
        taskBuilder.addConnector("myConnector4", "org.bonitasoft.connector.testConnector3", "1.0", ConnectorEvent.ON_FINISH).addInput(inputName4,
                input4Expression);

        processBuilder.addUserTask("step2", ACTOR_NAME).addTransition("step1", "step2");

        final ProcessDefinition processDefinition = deployProcessWithDefaultTestConnector(ACTOR_NAME, johnUserId, processBuilder, false);
        final ProcessInstance processInstance = getProcessAPI().startProcess(processDefinition.getId());

        final ActivityInstance task1 = waitForUserTask("step1", processInstance);
        waitUntilStore2Variables(valueOfInput1, valueOfInput2, inputName1, inputName2);

        assignAndExecuteStep(task1, johnUserId);

        final ActivityInstance task2 = waitForUserTask("step2", processInstance);
        waitUntilStore2Variables(valueOfInput3, valueOfInput4, inputName3, inputName4);
        assignAndExecuteStep(task2, johnUserId);

        waitForProcessToFinish(processInstance);
        disableAndDeleteProcess(processDefinition);
    }

    @Cover(classes = Connector.class, concept = BPMNConcept.CONNECTOR, keywords = { "Connector", "On finish", "Automatic activity" }, story = "Test connector on finish of an automatic activity.", jira = "")
    @Test
    public void executeConnectorOnFinishOfAnAutomaticActivity() throws Exception {
        final String valueOfInput1 = "valueOfInput1";
        final Expression input1Expression = new ExpressionBuilder().createConstantStringExpression(valueOfInput1);
        final ProcessDefinitionBuilder designProcessDefinition = new ProcessDefinitionBuilder().createNewInstance(
                "executeConnectorOnStartOfAnAutomaticActivity", "1.0");
        designProcessDefinition.addActor(ACTOR_NAME).addDescription(DESCRIPTION);
        final String inputName = "input1";
        designProcessDefinition.addAutomaticTask("step1")
                .addConnector("myConnector", "org.bonitasoft.connector.testConnector", "1.0", ConnectorEvent.ON_FINISH).addInput(inputName, input1Expression);

        final ProcessDefinition processDefinition = deployProcessWithDefaultTestConnector(ACTOR_NAME, johnUserId, designProcessDefinition, false);
        final ProcessInstance startProcess = getProcessAPI().startProcess(processDefinition.getId());

        final WaitUntil waitUntil = waitForVariableStorage(50, DEFAULT_TIMEOUT, inputName, valueOfInput1);
        assertTrue(waitUntil.waitUntil());
        waitForProcessToFinish(startProcess);

        disableAndDeleteProcess(processDefinition);
    }

    @Cover(classes = Connector.class, concept = BPMNConcept.CONNECTOR, keywords = { "Connector", "On enter", "User task" }, story = "Test connector on start of an user task.", jira = "")
    @Test
    public void executeConnectorOnEnterOfAnUserTask() throws Exception {
        final String valueOfInput1 = "valueOfInput1";
        final Expression input1Expression = new ExpressionBuilder().createConstantStringExpression(valueOfInput1);
        final ProcessDefinitionBuilder designProcessDefinition = new ProcessDefinitionBuilder().createNewInstance(
                "executeConnectorOnStartOfAnAutomaticActivity", "1.0");
        designProcessDefinition.addActor(ACTOR_NAME).addDescription(DESCRIPTION);
        final String inputName = "input1";
        designProcessDefinition.addUserTask("step1", ACTOR_NAME)
                .addConnector("myConnector", "org.bonitasoft.connector.testConnector", "1.0", ConnectorEvent.ON_ENTER).addInput(inputName, input1Expression);

        final ProcessDefinition processDefinition = deployProcessWithDefaultTestConnector(ACTOR_NAME, johnUserId, designProcessDefinition, false);
        getProcessAPI().startProcess(processDefinition.getId());

        final WaitUntil waitUntil = waitForVariableStorage(50, DEFAULT_TIMEOUT, inputName, valueOfInput1);
        assertTrue(waitUntil.waitUntil());

        disableAndDeleteProcess(processDefinition);
    }

    @Cover(classes = Connector.class, concept = BPMNConcept.CONNECTOR, keywords = { "Connector", "Connector deletion", "On enter", "User task", }, story = "Test connectors are deleted when the task is completed.", jira = "")
    @Test
    public void connectorsAreDeletedAfterTaskCompletion() throws Exception {
        // deploy process

        final String taskName = "step1";
        final ProcessDefinition processDefinition = deployProcessWithConnectorOnUserTask(johnUserId, taskName);

        // start the process
        final ProcessInstance processInstance = getProcessAPI().startProcess(processDefinition.getId());

        // wait for step containing the connector and execute it
        final ActivityInstance step1 = waitForUserTask(taskName, processInstance);
        assignAndExecuteStep(step1, johnUserId);
        waitForArchivedActivity(step1.getId(), TestStates.getNormalFinalState());

        // check that there are no more connector instances
        final SearchResult<ConnectorInstance> searchResult = searchConnectors(step1.getId(), ConnectorInstance.FLOWNODE_TYPE, 10);
        assertEquals(0, searchResult.getCount());

        // clean up
        disableAndDeleteProcess(processDefinition);
    }

    private SearchResult<ConnectorInstance> searchConnectors(final long containerId, final String containerType, final int maxResults) throws SearchException {
        final SearchOptionsBuilder optionsBuilder = new SearchOptionsBuilder(0, maxResults);
        optionsBuilder.filter(ConnectorInstancesSearchDescriptor.CONTAINER_ID, containerId);
        optionsBuilder.filter(ConnectorInstancesSearchDescriptor.CONTAINER_TYPE, containerType);
        return getProcessAPI().searchConnectorInstances(optionsBuilder.done());
    }

    private ProcessDefinition deployProcessWithConnectorOnUserTask(final long userId, final String taskName) throws BonitaException, IOException {
        final Expression input1Expression = new ExpressionBuilder().createConstantStringExpression("valueOfInput1");
        final ProcessDefinitionBuilder designProcessDefinition = new ProcessDefinitionBuilder().createNewInstance(
                "executeConnectorOnStartOfAnAutomaticActivity", "1.0");
        designProcessDefinition.addActor(ACTOR_NAME).addDescription(DESCRIPTION);
        designProcessDefinition.addUserTask(taskName, ACTOR_NAME)
                .addConnector("myConnector", "org.bonitasoft.connector.testConnector", "1.0", ConnectorEvent.ON_ENTER).addInput("input1", input1Expression);

        return deployProcessWithDefaultTestConnector(ACTOR_NAME, userId, designProcessDefinition, false);
    }

    @Cover(classes = { Connector.class, HumanTaskInstance.class }, concept = BPMNConcept.CONNECTOR, keywords = { "Connector", "On finish", "User task",
            "Starting State" }, story = "Test connector on finish on starting state of an user task.", jira = "ENGINE-604")
    @Test
    public void executeConnectorOnFinishOfAnUserTask() throws Exception {
        final String inputName = "input1";
        final String valueOfInput1 = "valueOfInput1";

        // Configure process and human tasks
        final Expression input1Expression = new ExpressionBuilder().createConstantStringExpression(valueOfInput1);
        final ProcessDefinitionBuilder designProcessDefinition = new ProcessDefinitionBuilder().createNewInstance(
                "executeConnectorOnStartOfAnAutomaticActivity", "1.0");
        designProcessDefinition.addActor(ACTOR_NAME).addDescription(DESCRIPTION);
        designProcessDefinition.addUserTask("step1", ACTOR_NAME)
                .addConnector("myConnector", "org.bonitasoft.connector.testConnector", "1.0", ConnectorEvent.ON_FINISH).addInput(inputName, input1Expression);
        designProcessDefinition.addUserTask("step2", ACTOR_NAME);
        designProcessDefinition.addTransition("step1", "step2");

        // Deploy and start process

        final ProcessDefinition processDefinition = deployProcessWithDefaultTestConnector(ACTOR_NAME, johnUserId, designProcessDefinition, false);
        final ProcessInstance processInstance = getProcessAPI().startProcess(processDefinition.getId());

        // Assign human task with connector
        final long step1Id = waitForUserTaskAndAssigneIt("step1", processInstance, johnUserId).getId();

        // Check Ready state of human task
        final List<HumanTaskInstance> assignedTasks = getProcessAPI().getAssignedHumanTaskInstances(johnUserId, 0, 10, null);
        assertEquals("ready", assignedTasks.get(0).getState());

        // Check that the "input1" variable has no value for "valueOfInput1"
        WaitUntil waitUntil = waitForVariableStorage(50, 800, inputName, valueOfInput1);
        assertFalse(waitUntil.waitUntil());

        // Run Started state of the human task
        executeFlowNodeUntilEnd(step1Id);
        waitForArchivedActivity(step1Id, TestStates.getNormalFinalState());

        // Check that the "input1" variable has value for "valueOfInput1", in Started state of human task
        waitUntil = waitForVariableStorage(50, DEFAULT_TIMEOUT, inputName, valueOfInput1);
        assertTrue(waitUntil.waitUntil());

        // Remove all for the end
        disableAndDeleteProcess(processDefinition);
    }

    @Cover(classes = { Connector.class, HumanTaskInstance.class }, concept = BPMNConcept.CONNECTOR, keywords = { "Connector", "On finish", "User task",
            "Boundary event", "Timer event", "Starting State" }, story = "Test connector on finish on starting state of an user task, with a boundary timer evnet.", jira = "ENGINE-604")
    @Test
    public void executeConnectorOnFinishStateOfAnUserTaskWithTimerEvent() throws Exception {
        final String inputName = "input1";
        final String valueOfInput1 = "valueOfInput1";

        // Configure process and human tasks
        final ProcessDefinitionBuilder designProcessDefinition = new ProcessDefinitionBuilder().createNewInstance(
                "executeConnectorOnStartOfAnAutomaticActivity", "1.0");
        designProcessDefinition.addActor(ACTOR_NAME).addDescription(DESCRIPTION);
        final UserTaskDefinitionBuilder userTaskDefinitionBuilder = designProcessDefinition.addUserTask("step1", ACTOR_NAME);
        userTaskDefinitionBuilder.addConnector("myConnector", "org.bonitasoft.connector.testConnector", "1.0", ConnectorEvent.ON_FINISH).addInput(inputName,
                new ExpressionBuilder().createConstantStringExpression(valueOfInput1));
        designProcessDefinition.addStartEvent("start");
        userTaskDefinitionBuilder.addBoundaryEvent("timer", true).addTimerEventTriggerDefinition(TimerType.DURATION,
                new ExpressionBuilder().createConstantLongExpression(3000));
        userTaskDefinitionBuilder.addUserTask("exceptionStep", ACTOR_NAME);
        designProcessDefinition.addEndEvent("end");
        designProcessDefinition.addAutomaticTask("step2");
        designProcessDefinition.addTransition("step1", "step2");
        designProcessDefinition.addTransition("timer", "exceptionStep");

        // Deploy and start process

        final ProcessDefinition processDefinition = deployProcessWithDefaultTestConnector(ACTOR_NAME, johnUserId, designProcessDefinition, false);
        final ProcessInstance processInstance = getProcessAPI().startProcess(processDefinition.getId());

        // Assign human task with connector
        waitForStep("step1", processInstance);
        final List<HumanTaskInstance> pendingTasks = getProcessAPI().getPendingHumanTaskInstances(johnUserId, 0, 10, null);
        final long activityInstanceId = pendingTasks.get(0).getId();
        getProcessAPI().assignUserTask(activityInstanceId, johnUserId);

        // Check Ready state of human task
        assertEquals("ready", pendingTasks.get(0).getState());

        // Check that the "input1" variable has no value for "valueOfInput1", in Ready state of human task
        WaitUntil waitUntil = waitForVariableStorage(50, 800, inputName, valueOfInput1);
        assertFalse(waitUntil.waitUntil());

        // Run Started state of the human task
        getProcessAPI().executeFlowNode(activityInstanceId);

        // Check that the "input1" variable has value for "valueOfInput1", in Started state of human task
        waitUntil = waitForVariableStorage(50, DEFAULT_TIMEOUT, inputName, valueOfInput1);
        assertTrue(waitUntil.waitUntil());

        // Remove all for the end
        waitForProcessToFinish(processInstance);
        disableAndDeleteProcess(processDefinition);
    }

    private WaitUntil waitForVariableStorage(final int repeatEach, final int timeout, final String inputName, final String valueOfInput) {
        final WaitUntil waitUntil = new WaitUntil(repeatEach, timeout, false) {

            @Override
            protected boolean check() {
                return VariableStorage.getInstance().getVariableValue(inputName).equals(valueOfInput);
            }
        };
        return waitUntil;
    }

    @Cover(classes = Connector.class, concept = BPMNConcept.CONNECTOR, keywords = { "Connector", "On enter", "Process" }, story = "Test connector on start of a process.", jira = "")
    @Test
    public void executeConnectorOnEnterOfProcess() throws Exception {
        final String valueOfInput1 = "valueOfInput1";
        final Expression input1Expression = new ExpressionBuilder().createConstantStringExpression(valueOfInput1);
        final ProcessDefinitionBuilder designProcessDefinition = new ProcessDefinitionBuilder().createNewInstance(
                "executeConnectorOnStartOfAnAutomaticActivity", "1.0");
        designProcessDefinition.addActor(ACTOR_NAME).addDescription(DESCRIPTION);
        final String inputName = "input1";
        designProcessDefinition.addConnector("myConnector", "org.bonitasoft.connector.testConnector", "1.0", ConnectorEvent.ON_ENTER).addInput(inputName,
                input1Expression);
        designProcessDefinition.addAutomaticTask("step1");
        designProcessDefinition.addUserTask("step2", ACTOR_NAME);
        designProcessDefinition.addTransition("step1", "step2");

        final ProcessDefinition processDefinition = deployProcessWithDefaultTestConnector(ACTOR_NAME, johnUserId, designProcessDefinition, false);
        final ProcessInstance processInstance = getProcessAPI().startProcess(processDefinition.getId());

        final WaitUntil waitUntil = waitForVariableStorage(50, DEFAULT_TIMEOUT, inputName, valueOfInput1);
        assertTrue(waitUntil.waitUntil());
        waitForUserTask("step2", processInstance);

        disableAndDeleteProcess(processDefinition);
    }

    @Cover(classes = Connector.class, concept = BPMNConcept.CONNECTOR, keywords = { "Connector", "On finish", "Process" }, story = "Test connector on finish of a process.", jira = "")
    @Test
    public void executeConnectorOnFinishOfAProcess() throws Exception {
        final String valueOfInput = "valueOfInput1";
        final String inputName = "input1";

        final Expression input1Expression = new ExpressionBuilder().createConstantStringExpression(valueOfInput);
        final ProcessDefinitionBuilder designProcessDefinition = new ProcessDefinitionBuilder().createNewInstance(
                "executeConnectorOnStartOfAnAutomaticActivity", PROCESS_VERSION);
        designProcessDefinition.addActor(ACTOR_NAME).addDescription(DESCRIPTION);
        designProcessDefinition.addConnector("myConnector", "org.bonitasoft.connector.testConnector", "1.0", ConnectorEvent.ON_FINISH).addInput(inputName,
                input1Expression);
        designProcessDefinition.addUserTask("step1", ACTOR_NAME);
        final ProcessDefinition processDefinition = deployProcessWithDefaultTestConnector(ACTOR_NAME, johnUserId, designProcessDefinition, false);
        final ProcessInstance processInstance = getProcessAPI().startProcess(processDefinition.getId());

        final HumanTaskInstance step1 = waitForUserTask("step1", processInstance);
        WaitUntil waitUntil = waitForVariableStorage(50, 800, inputName, valueOfInput);
        assertFalse(waitUntil.waitUntil());

        assignAndExecuteStep(step1, johnUserId);
        waitUntil = waitForVariableStorage(50, DEFAULT_TIMEOUT, inputName, valueOfInput);
        assertTrue(waitUntil.waitUntil());

        // Clean up
        waitForProcessToFinish(processInstance);
        disableAndDeleteProcess(processDefinition);
    }

    @Cover(classes = Connector.class, concept = BPMNConcept.CONNECTOR, keywords = { "Connector", "Connector deletion", "On finish", "Process" }, story = "Test connectors attached to a process are deleted when the process completes.", jira = "")
    @Test
    public void connectorsAreDeletedAfterProcessCompletion() throws Exception {
        // deploy the a process with a connector

        final String taskName = "step1";
        final ProcessDefinition processDefinition = deployProcessWithConnectorOnFinish(johnUserId, ACTOR_NAME, taskName);

        // execute the process
        final ProcessInstance processInstance = getProcessAPI().startProcess(processDefinition.getId());
        waitForUserTaskAndExecuteIt(taskName, processInstance, johnUserId);
        waitForProcessToFinish(processInstance);

        // check there are no connector instances
        final SearchResult<ConnectorInstance> searchResult = searchConnectors(processInstance.getId(), ConnectorInstance.PROCESS_TYPE, 10);
        assertEquals(0, searchResult.getCount());

        // clean up
        disableAndDeleteProcess(processDefinition);
    }

    private ProcessDefinition deployProcessWithConnectorOnFinish(final long userId, final String ACTOR_NAME, final String taskName) throws BonitaException,
            IOException {
        final Expression input1Expression = new ExpressionBuilder().createConstantStringExpression("valueOfInput1");
        final ProcessDefinitionBuilder designProcessDefinition = new ProcessDefinitionBuilder().createNewInstance(
                "executeConnectorOnStartOfAnAutomaticActivity", "1.0");
        designProcessDefinition.addActor(ACTOR_NAME).addDescription("ACTOR_NAME all day and night long");
        designProcessDefinition.addConnector("myConnector", "org.bonitasoft.connector.testConnector", "1.0", ConnectorEvent.ON_FINISH).addInput("input1",
                input1Expression);
        designProcessDefinition.addUserTask(taskName, ACTOR_NAME);
        return deployProcessWithDefaultTestConnector(ACTOR_NAME, userId, designProcessDefinition, false);
    }

    @Cover(classes = Connector.class, concept = BPMNConcept.CONNECTOR, keywords = { "Connector", "On enter", "Automatic activity" }, story = "Test connector on start of an automatic activity.", jira = "")
    @Test
    public void executeConnectorOnEnterOfAnAutomaticActivity() throws Exception {
        final String valueOfInput1 = "valueOfInput1";
        final Expression input1Expression = new ExpressionBuilder().createConstantStringExpression(valueOfInput1);
        final ProcessDefinitionBuilder designProcessDefinition = new ProcessDefinitionBuilder().createNewInstance(
                "executeConnectorOnStartOfAnAutomaticActivity", "1.0");
        designProcessDefinition.addActor(ACTOR_NAME).addDescription(DESCRIPTION);
        final String inputName = "input1";
        designProcessDefinition.addAutomaticTask("step1").addConnector("myConnector", "org.bonitasoft.connector.testConnector", "1.0", ConnectorEvent.ON_ENTER)
                .addInput(inputName, input1Expression);
        designProcessDefinition.addUserTask("step2", ACTOR_NAME);
        designProcessDefinition.addTransition("step1", "step2");

        final ProcessDefinition processDefinition = deployProcessWithDefaultTestConnector(ACTOR_NAME, johnUserId, designProcessDefinition, false);
        final ProcessInstance processInstance = getProcessAPI().startProcess(processDefinition.getId());

        final WaitUntil waitUntil = waitForVariableStorage(50, DEFAULT_TIMEOUT, inputName, valueOfInput1);
        assertTrue(waitUntil.waitUntil());
        assertNotNull(waitForUserTask("step2", processInstance));

        disableAndDeleteProcess(processDefinition);
    }

    @Cover(classes = Connector.class, concept = BPMNConcept.CONNECTOR, keywords = { "Connector", "Missing implementation", "Process instance" }, story = "Execute connector with missing implementation on process instance.", jira = "")
    @Test
    public void executeMissingImplConnectorOnProcessInstance() throws Exception {
        final ProcessDefinitionBuilder designProcessDefinition = new ProcessDefinitionBuilder().createNewInstance("executeConnectorOnActivityInstance", "1.0");
        designProcessDefinition.addActor(ACTOR_NAME).addUserTask("step1", ACTOR_NAME)
                .addConnector("UnkownConnector", "unkownConnectorId", "1.0.0", ConnectorEvent.ON_ENTER);
        final ProcessDefinition processDefinition = getProcessAPI().deploy(
                new BusinessArchiveBuilder().createNewBusinessArchive().setProcessDefinition(designProcessDefinition.done()).done());
        addMappingOfActorsForUser(ACTOR_NAME, johnUserId, processDefinition);
        final ProcessDeploymentInfo processDeploymentInfo = getProcessAPI().getProcessDeploymentInfo(processDefinition.getId());
        assertEquals(ConfigurationState.UNRESOLVED, processDeploymentInfo.getConfigurationState());
        final List<Problem> processResolutionProblems = getProcessAPI().getProcessResolutionProblems(processDefinition.getId());
        assertEquals(1, processResolutionProblems.size());
        final Problem problem = processResolutionProblems.get(0);
        assertEquals("connector", problem.getResource());

        deleteProcess(processDefinition);
    }

    @Cover(classes = Connector.class, concept = BPMNConcept.CONNECTOR, keywords = { "Connector", "Missing class connector", "Process instance" }, story = "Execute connector with missing class on process instance.", jira = "")
    @Test
    public void executeMissingClassConnectorOnProcessInstance() throws Exception {
        final PrintStream stdout = System.out;
        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        System.setOut(new PrintStream(myOut));
        try {
            final ProcessDefinitionBuilder processDefBuilder = new ProcessDefinitionBuilder().createNewInstance("executeConnectorOnActivityInstance", "1.0");
            processDefBuilder.addActor(ACTOR_NAME).addUserTask("step1", ACTOR_NAME)
                    .addConnector("UnkownClassConnector", "unkownClassConnectorDef", "1.0.0", ConnectorEvent.ON_ENTER);
            final BusinessArchiveBuilder businessArchiveBuilder = new BusinessArchiveBuilder().createNewBusinessArchive().setProcessDefinition(
                    processDefBuilder.done());
            businessArchiveBuilder.addConnectorImplementation(new BarResource("UnknownClassConnector.impl", IOUtils.toByteArray(BPMRemoteTests.class
                    .getResourceAsStream("/org/bonitasoft/engine/connectors/UnknownClassConnector.impl"))));
            final ProcessDefinition processDefinition = getProcessAPI().deploy(businessArchiveBuilder.done());
            addMappingOfActorsForUser(ACTOR_NAME, johnUserId, processDefinition);
            getProcessAPI().enableProcess(processDefinition.getId());

            final ProcessInstance processInstance = getProcessAPI().startProcess(processDefinition.getId());
            final ActivityInstance failTask = waitForTaskToFail(processInstance);
            assertEquals("step1", failTask.getName());
            disableAndDeleteProcess(processDefinition);
        } finally {
            System.setOut(stdout);
        }
        final String logs = myOut.toString();
        System.out.println(logs);
        assertTrue("should have written in logs an exception", logs.contains("SConnectorException"));
        assertTrue("should have written in logs an exception", logs.contains("java.lang.ClassNotFoundException: org.unknown.MyUnknownClass"));
    }

    @Cover(classes = Connector.class, concept = BPMNConcept.OTHERS, keywords = { "Connector", "Classpath" }, jira = "ENGINE-773")
    @Test
    public void executeConnectorWithCustomOutputTypeOnActivity() throws Exception {
        final ProcessDefinitionBuilder designProcessDefinition = new ProcessDefinitionBuilder().createNewInstance("testConnectorWithExecutionTooLong", "1.0");
        designProcessDefinition.addActor(ACTOR_NAME).addDescription(DESCRIPTION);
        designProcessDefinition.addShortTextData("value", null);
        designProcessDefinition
                .addAutomaticTask("step1")
                .addConnector("myConnector1", "connectorWithCustomType", "1.0.0", ConnectorEvent.ON_ENTER)
                .addOutput(
                        new OperationBuilder()
                                .createNewInstance()
                                .setLeftOperand("value", false)
                                .setType(OperatorType.ASSIGNMENT)
                                .setRightOperand(
                                        new ExpressionBuilder().createGroovyScriptExpression("script", "output.getValue()", String.class.getName(),
                                                new ExpressionBuilder().createInputExpression("output", "org.connector.custom.CustomType"))).done());
        designProcessDefinition.addUserTask("step2", ACTOR_NAME);
        designProcessDefinition.addTransition("step1", "step2");

        final List<BarResource> resources = new ArrayList<BarResource>();
        addResource(resources, "/org/bonitasoft/engine/connectors/TestConnectorWithCustomType.impl", "TestConnectorWithCustomType.impl");
        addResource(resources, "/org/bonitasoft/engine/connectors/connector-with-custom-type.bak", "connector-with-custom-type.jar");
        final BusinessArchiveBuilder businessArchiveBuilder = new BusinessArchiveBuilder().createNewBusinessArchive();
        businessArchiveBuilder.addConnectorImplementation(resources.get(0));
        businessArchiveBuilder.addClasspathResource(resources.get(1));
        businessArchiveBuilder.setProcessDefinition(designProcessDefinition.done());
        final ProcessDefinition processDefinition = getProcessAPI().deploy(businessArchiveBuilder.done());
        addMappingOfActorsForUser(ACTOR_NAME, johnUserId, processDefinition);
        getProcessAPI().enableProcess(processDefinition.getId());

        final ProcessInstance process = getProcessAPI().startProcess(processDefinition.getId());
        waitForUserTask("step2", process);
        assertEquals("value", getProcessAPI().getProcessDataInstance("value", process.getId()).getValue());

        disableAndDeleteProcess(processDefinition);
    }

    @Cover(classes = Connector.class, concept = BPMNConcept.OTHERS, keywords = { "Connector", "Classpath" }, jira = "ENGINE-773")
    @Test
    public void executeConnectorWithCustomOutputTypeOnProcess() throws Exception {
        final ProcessDefinitionBuilder designProcessDefinition = new ProcessDefinitionBuilder().createNewInstance("testConnectorWithExecutionTooLong", "1.0");
        designProcessDefinition.addActor(ACTOR_NAME).addDescription(DESCRIPTION);
        designProcessDefinition.addShortTextData("value", null);
        designProcessDefinition.addConnector("myConnector1", "connectorWithCustomType", "1.0.0", ConnectorEvent.ON_ENTER).addOutput(
                new OperationBuilder()
                        .createNewInstance()
                        .setLeftOperand("value", false)
                        .setType(OperatorType.ASSIGNMENT)
                        .setRightOperand(
                                new ExpressionBuilder().createGroovyScriptExpression("script", "output.getValue()", String.class.getName(),
                                        new ExpressionBuilder().createInputExpression("output", "org.connector.custom.CustomType"))).done());
        designProcessDefinition.addAutomaticTask("step1");
        designProcessDefinition.addUserTask("step2", ACTOR_NAME);
        designProcessDefinition.addTransition("step1", "step2");

        final List<BarResource> resources = new ArrayList<BarResource>();
        addResource(resources, "/org/bonitasoft/engine/connectors/TestConnectorWithCustomType.impl", "TestConnectorWithCustomType.impl");
        addResource(resources, "/org/bonitasoft/engine/connectors/connector-with-custom-type.bak", "connector-with-custom-type.jar");
        final BusinessArchiveBuilder businessArchiveBuilder = new BusinessArchiveBuilder().createNewBusinessArchive();
        businessArchiveBuilder.addConnectorImplementation(resources.get(0));
        businessArchiveBuilder.addClasspathResource(resources.get(1));
        businessArchiveBuilder.setProcessDefinition(designProcessDefinition.done());
        final ProcessDefinition processDefinition = getProcessAPI().deploy(businessArchiveBuilder.done());
        addMappingOfActorsForUser(ACTOR_NAME, johnUserId, processDefinition);
        getProcessAPI().enableProcess(processDefinition.getId());
        final ProcessInstance process = getProcessAPI().startProcess(processDefinition.getId());
        waitForUserTask("step2", process);
        assertEquals("value", getProcessAPI().getProcessDataInstance("value", process.getId()).getValue());

        disableAndDeleteProcess(processDefinition);
    }

    @Cover(classes = Connector.class, concept = BPMNConcept.CONNECTOR, jira = "ENGINE-783", keywords = { "Connector", "No connector implementation", "cache" }, story = "get connector implementation still work avec cache is cleared.")
    @Test
    public void getConnectorImplementationWorksAfterCacheCleared() throws Exception {
        final String inputName = "input1";
        final String valueOfInput1 = "valueOfInput1";

        // Configure process and human tasks
        final Expression input1Expression = new ExpressionBuilder().createConstantStringExpression(valueOfInput1);
        final ProcessDefinitionBuilder designProcessDefinition = new ProcessDefinitionBuilder().createNewInstance(
                "executeConnectorOnStartOfAnAutomaticActivity", "1.0");
        designProcessDefinition.addActor(ACTOR_NAME).addDescription(DESCRIPTION);
        designProcessDefinition.addUserTask("step1", ACTOR_NAME)
                .addConnector("myConnector", "org.bonitasoft.connector.testConnector", "1.0", ConnectorEvent.ON_FINISH).addInput(inputName, input1Expression);
        designProcessDefinition.addUserTask("step2", ACTOR_NAME);
        designProcessDefinition.addTransition("step1", "step2");

        // Deploy and start process
        final ProcessDefinition processDefinition = deployProcessWithDefaultTestConnector(ACTOR_NAME, johnUserId, designProcessDefinition, false);
        assertTrue(getProcessAPI().getProcessResolutionProblems(processDefinition.getId()).isEmpty());

        final SessionAccessor sessionAccessor = ServiceAccessorFactory.getInstance().createSessionAccessor();
        sessionAccessor.setSessionInfo(getSession().getId(), getSession().getTenantId());

        final CacheService cacheservice = getTenantAccessor().getCacheService();
        cacheservice.clearAll();
        sessionAccessor.deleteSessionId();

        assertTrue(getProcessAPI().getProcessResolutionProblems(processDefinition.getId()).isEmpty());
        disableAndDeleteProcess(processDefinition);
    }

    @Cover(classes = Connector.class, concept = BPMNConcept.CONNECTOR, keywords = { "Failed", "Database", "Connector", "On enter", "Automatic activity" }, jira = "ENGINE-936")
    @Test
    public void executeFailedNonSerializableOutputConnectorOnEnterOfAnAutomaticActivity() throws Exception {
        executeFailedNonSerializableOutputConnectorOfAnAutomaticActivity(ConnectorEvent.ON_ENTER);
    }

    @Cover(classes = Connector.class, concept = BPMNConcept.CONNECTOR, keywords = { "Failed", "Database", "Connector", "On finish", "Automatic activity" }, jira = "ENGINE-936")
    @Test
    public void executeFailedNonSerializableOutputConnectorOnFinishOfAnAutomaticActivity() throws Exception {
        executeFailedNonSerializableOutputConnectorOfAnAutomaticActivity(ConnectorEvent.ON_FINISH);
    }

    protected void executeFailedNonSerializableOutputConnectorOfAnAutomaticActivity(final ConnectorEvent connectorEvent) throws Exception {
        final ProcessDefinitionBuilder processBuilder = new ProcessDefinitionBuilder().createNewInstance("ProcessWithNonSerializableOutputConnector", "1.0");
        processBuilder.addStartEvent("start").addEndEvent("end");
        processBuilder.addActor(ACTOR_NAME);
        processBuilder.addData("resultProcess", Object.class.getName(), new ExpressionBuilder().createConstantLongExpression(0L));
        final AutomaticTaskDefinitionBuilder autoTask = processBuilder.addAutomaticTask("Step1");
        processBuilder.addTransition("start", "end");
        autoTask.addConnector("nonSerializableFailedConnector", "org.bonitasoft.connector.testConnectorWithNotSerializableOutput", "1.0", connectorEvent)
                .addOutput(
                        new OperationBuilder().createNewInstance().setLeftOperand("resultProcess", false).setType(OperatorType.ASSIGNMENT)
                                .setRightOperand(new ExpressionBuilder().createInputExpression("output1", Object.class.getName())).done());

        final ProcessDefinition processDefinition = deployProcessWithDefaultTestConnector(ACTOR_NAME, johnUserId, processBuilder, false);
        final ProcessInstance processInstance = getProcessAPI().startProcess(processDefinition.getId());

        final ActivityInstance task = waitForTaskToFail(processInstance);
        assertEquals("Step1", task.getName());

        // Clean-up
        Thread.sleep(1000);
        disableAndDeleteProcess(processDefinition);
    }

    @Cover(classes = Expression.class, concept = BPMNConcept.EXPRESSIONS, keywords = { "Failed", "Database", "Connector", "On finish", "Automatic activity" }, jira = "ENGINE-1814", story = "execute expression on process that is in initializing, e.g. process execute a connector on on enter, the result must be no exception and be able to retrive data")
    @Test
    public void executeExpressionAtProcessInstantiationOnProcessInInitializing() throws Exception {
        // will block the connector
        final ProcessDefinitionBuilder processBuilder = new ProcessDefinitionBuilder().createNewInstance("processWithBlockingConnector", "1.0");
        processBuilder.addConnector("myConnector", "blocking-connector", "1.0", ConnectorEvent.ON_ENTER);
        processBuilder.addData("a", String.class.getName(), new ExpressionBuilder().createConstantStringExpression("avalue"));
        processBuilder.addData("b", String.class.getName(), new ExpressionBuilder().createConstantStringExpression("bvalue"));
        processBuilder.addData("c", String.class.getName(), new ExpressionBuilder().createConstantStringExpression("cvalue"));
        final AutomaticTaskDefinitionBuilder addAutomaticTask = processBuilder.addAutomaticTask("step1");
        addAutomaticTask.addOperation(new OperationBuilder().createSetDataOperation("a", new ExpressionBuilder().createConstantStringExpression("changed")));
        addAutomaticTask.addOperation(new OperationBuilder().createSetDataOperation("b", new ExpressionBuilder().createConstantStringExpression("changed")));
        addAutomaticTask.addOperation(new OperationBuilder().createSetDataOperation("c", new ExpressionBuilder().createConstantStringExpression("changed")));

        // Add user task for confirmation form evaluation:
        processBuilder.addActor(ACTOR_NAME);
        String userTaskName = "userTaskWithOnFinishConnector";
        UserTaskDefinitionBuilder userTaskDef = processBuilder.addUserTask(userTaskName, ACTOR_NAME);
        userTaskDef.addConnector("myConnector2", "blocking-connector", "1.0", ConnectorEvent.ON_FINISH);

        final BusinessArchive businessArchive = new BusinessArchiveBuilder()
                .createNewBusinessArchive()
                .setProcessDefinition(processBuilder.done())
                .addConnectorImplementation(
                        new BarResource("blocking-connector.impl", getConnectorImplementationFile("blocking-connector", "1.0", "blocking-connector-impl",
                                "1.0", BlockingConnector.class.getName()))).done();

        final ProcessDefinition processDefinition = deployAndEnableWithActor(businessArchive, ACTOR_NAME, johnUser);

        BlockingConnector.semaphore.acquire();
        final ProcessInstance processInstance = getProcessAPI().startProcess(processDefinition.getId());
        final Map<Expression, Map<String, Serializable>> expressions = new HashMap<Expression, Map<String, Serializable>>(2);
        expressions.put(
                new ExpressionBuilder().createGroovyScriptExpression("ascripte", "a+b+c", String.class.getName(),
                        new ExpressionBuilder().createDataExpression("a", String.class.getName()),
                        new ExpressionBuilder().createDataExpression("b", String.class.getName()),
                        new ExpressionBuilder().createDataExpression("c", String.class.getName())), Collections.<String, Serializable> emptyMap());
        expressions.put(new ExpressionBuilder().createDataExpression("a", String.class.getName()), Collections.<String, Serializable> emptyMap());
        final Map<String, Serializable> evaluateExpressionsAtProcessInstanciation = getProcessAPI().evaluateExpressionsAtProcessInstanciation(
                processInstance.getId(), expressions);
        assertEquals("avaluebvaluecvalue", evaluateExpressionsAtProcessInstanciation.get("ascripte"));
        assertEquals("avalue", evaluateExpressionsAtProcessInstanciation.get("a"));

        BlockingConnector.semaphore.release();

        HumanTaskInstance userTask = waitForUserTask(userTaskName, processInstance.getId());
        BlockingConnector.semaphore.acquire();
        assignAndExecuteStep(userTask, johnUserId);
        // Try to evaluate expression on non-completed activity:
        Expression engineConstantExpr = new ExpressionBuilder().createEngineConstant(ExpressionConstants.PROCESS_INSTANCE_ID);
        final Map<Expression, Map<String, Serializable>> exprToEvaluate = new HashMap<Expression, Map<String, Serializable>>(1);
        exprToEvaluate.put(engineConstantExpr, Collections.<String, Serializable> emptyMap());
        Map<String, Serializable> evaluatedExpressions = getProcessAPI().evaluateExpressionsOnCompletedActivityInstance(userTask.getId(), exprToEvaluate);
        assertEquals(processInstance.getId(), ((Long) evaluatedExpressions.get("processInstanceId")).longValue());
        // Release the connector for the user task to complete:
        BlockingConnector.semaphore.release();

        waitForProcessToFinish(processInstance.getId());
        getProcessAPI().evaluateExpressionsAtProcessInstanciation(processInstance.getId(), expressions);
        assertEquals("avaluebvaluecvalue", evaluateExpressionsAtProcessInstanciation.get("ascripte"));
        assertEquals("avalue", evaluateExpressionsAtProcessInstanciation.get("a"));

        disableAndDeleteProcess(processDefinition);
    }
}
