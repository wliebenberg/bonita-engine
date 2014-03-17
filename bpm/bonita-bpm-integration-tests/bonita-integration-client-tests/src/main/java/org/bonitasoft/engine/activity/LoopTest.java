package org.bonitasoft.engine.activity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bonitasoft.engine.CommonAPITest;
import org.bonitasoft.engine.api.ProcessManagementAPI;
import org.bonitasoft.engine.bpm.flownode.ActivityInstance;
import org.bonitasoft.engine.bpm.flownode.ActivityInstanceCriterion;
import org.bonitasoft.engine.bpm.flownode.ArchivedActivityInstance;
import org.bonitasoft.engine.bpm.flownode.ArchivedLoopActivityInstance;
import org.bonitasoft.engine.bpm.flownode.HumanTaskInstance;
import org.bonitasoft.engine.bpm.process.ProcessDefinition;
import org.bonitasoft.engine.bpm.process.ProcessInstance;
import org.bonitasoft.engine.bpm.process.impl.ProcessDefinitionBuilder;
import org.bonitasoft.engine.connectors.VariableStorage;
import org.bonitasoft.engine.exception.BonitaException;
import org.bonitasoft.engine.expression.Expression;
import org.bonitasoft.engine.expression.ExpressionBuilder;
import org.bonitasoft.engine.expression.ExpressionConstants;
import org.bonitasoft.engine.identity.User;
import org.bonitasoft.engine.operation.LeftOperandBuilder;
import org.bonitasoft.engine.operation.OperatorType;
import org.bonitasoft.engine.test.annotation.Cover;
import org.bonitasoft.engine.test.annotation.Cover.BPMNConcept;
import org.bonitasoft.engine.test.check.CheckNbPendingTaskOf;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Matthieu Chaffotte
 */
public class LoopTest extends CommonAPITest {

    private static final String JOHN = "john";

    private User john;

    @Before
    public void beforeTest() throws BonitaException {
        login();
        john = createUser(JOHN, "bpm");
        logout();
        loginWith(JOHN, "bpm");
    }

    @After
    public void afterTest() throws BonitaException {
        deleteUser(JOHN);
        VariableStorage.clearAll();
        logout();
    }

    @Test
    public void executeAStandardLoopUserTaskWhichDoesNotLoop() throws Exception {
        final Expression condition = new ExpressionBuilder().createConstantBooleanExpression(false);

        final String delivery = "Delivery men";

        final ProcessDefinitionBuilder builder = new ProcessDefinitionBuilder().createNewInstance("executeAStandardLoopUserTaskWhichDoesNotLoop", "1.0");
        builder.addActor(delivery).addDescription("Delivery all day and night long");
        builder.addUserTask("step1", delivery).addLoop(true, condition);

        final ProcessDefinition processDefinition = deployAndEnableWithActor(builder.done(), delivery, john);
        final ProcessInstance instance = getProcessAPI().startProcess(processDefinition.getId());
        assertTrue(waitProcessToFinishAndBeArchived(instance));
        final List<ArchivedActivityInstance> archivedActivityInstances = getProcessAPI().getArchivedActivityInstances(instance.getId(), 0, 100,
                ActivityInstanceCriterion.NAME_ASC);
        assertEquals(2, archivedActivityInstances.size());
        for (final ArchivedActivityInstance archivedActivityInstance : archivedActivityInstances) {
            assertTrue(ArchivedLoopActivityInstance.class.isInstance(archivedActivityInstance));
        }

        disableAndDeleteProcess(processDefinition);
    }

    @Test
    public void executeAStandardLoopUserTask() throws Exception {
        final Expression condition = new ExpressionBuilder().createConstantBooleanExpression(true);

        final String delivery = "Delivery men";

        final ProcessDefinitionBuilder builder = new ProcessDefinitionBuilder().createNewInstance("executeAStandardLoopUserTask", "1.0");
        builder.addActor(delivery).addDescription("Delivery all day and night long");
        builder.addUserTask("step1", delivery).addLoop(false, condition);

        final ProcessDefinition processDefinition = deployAndEnableWithActor(builder.done(), delivery, john);
        getProcessAPI().startProcess(processDefinition.getId());

        assertTrue(new CheckNbPendingTaskOf(getProcessAPI(), 50, 5000, false, 1, john).waitUntil());
        List<HumanTaskInstance> pendingTasks = getProcessAPI().getPendingHumanTaskInstances(john.getId(), 0, 10, null);
        final HumanTaskInstance pendingTask = pendingTasks.get(0);

        assignAndExecuteStep(pendingTask, john.getId());

        assertTrue(new CheckNbPendingTaskOf(getProcessAPI(), 50, 500, false, 1, john).waitUntil());
        pendingTasks = getProcessAPI().getPendingHumanTaskInstances(john.getId(), 0, 10, null);

        disableAndDeleteProcess(processDefinition);
    }

    @Test
    @Cover(classes = { ProcessManagementAPI.class }, concept = BPMNConcept.EXPRESSIONS, keywords = { "expression context", "flow node container hierarchy" }, jira = "ENGINE-1848")
    public void evaluateExpressionsOnLoopUserTask() throws Exception {
        final String actorName = "Golf Players";

        final ProcessDefinitionBuilder builder = new ProcessDefinitionBuilder().createNewInstance("evaluateExpressionsOnLoopUserTask", "1.0");
        builder.addActor(actorName).addDescription("For Golf players only");
        final String activityName = "launch";
        builder.addStartEvent("dummy");
        builder.addUserTask(activityName, actorName).addLoop(false, new ExpressionBuilder().createConstantBooleanExpression(true));
        builder.addTransition("dummy", activityName);

        final ProcessDefinition processDefinition = deployAndEnableWithActor(builder.done(), actorName, john);
        try {
            final ProcessInstance processInstance = getProcessAPI().startProcess(processDefinition.getId());
            final ActivityInstance userTask = waitForUserTask(activityName, processInstance);
            final Map<Expression, Map<String, Serializable>> expressions = new HashMap<Expression, Map<String, Serializable>>();
            expressions.put(new ExpressionBuilder().createConstantBooleanExpression(true), new HashMap<String, Serializable>(0));
            getProcessAPI().evaluateExpressionsOnActivityInstance(userTask.getId(), expressions);
        } finally {
            disableAndDeleteProcess(processDefinition);
        }
    }

    @Test
    public void executeAStandardLoopWithMaxIteration() throws Exception {
        final Expression condition = new ExpressionBuilder().createConstantBooleanExpression(true);

        final String delivery = "Delivery men";

        final ProcessDefinitionBuilder builder = new ProcessDefinitionBuilder().createNewInstance("executeAStandardLoopUserTask", "1.0");
        builder.addActor(delivery).addDescription("Delivery all day and night long");
        final int loopMax = 3;
        builder.addUserTask("step1", delivery).addLoop(false, condition, new ExpressionBuilder().createConstantIntegerExpression(loopMax));

        final ProcessDefinition processDefinition = deployAndEnableWithActor(builder.done(), delivery, john);
        getProcessAPI().startProcess(processDefinition.getId());

        for (int i = 0; i < loopMax; i++) {
            assertTrue(new CheckNbPendingTaskOf(getProcessAPI(), 50, 5000, false, 1, john).waitUntil());
            final List<HumanTaskInstance> pendingTasks = getProcessAPI().getPendingHumanTaskInstances(john.getId(), 0, 10, null);
            final HumanTaskInstance pendingTask = pendingTasks.get(0);

            assignAndExecuteStep(pendingTask, john.getId());
        }
        Thread.sleep(500);
        assertTrue(new CheckNbPendingTaskOf(getProcessAPI(), 50, 5000, false, 0, john).waitUntil());

        disableAndDeleteProcess(processDefinition);
    }

    @Test
    public void executeAStandardLoopWithConditionUsingLoopCounter() throws Exception {
        final Expression condition = new ExpressionBuilder().createGroovyScriptExpression("executeAStandardLoopWithConditionUsingLoopCounter",
                "loopCounter < 3", Boolean.class.getName(), Arrays.asList(new ExpressionBuilder().createEngineConstant(ExpressionConstants.LOOP_COUNTER)));

        final String delivery = "Delivery men";

        final ProcessDefinitionBuilder builder = new ProcessDefinitionBuilder().createNewInstance("executeAStandardLoopUserTask", "1.0");
        builder.addActor(delivery).addDescription("Delivery all day and night long");
        final int loopMax = 3;
        builder.addUserTask("step1", delivery).addLoop(false, condition);

        final ProcessDefinition processDefinition = deployAndEnableWithActor(builder.done(), delivery, john);
        getProcessAPI().startProcess(processDefinition.getId());

        for (int i = 0; i < loopMax; i++) {
            assertTrue(new CheckNbPendingTaskOf(getProcessAPI(), 50, 5000, false, 1, john).waitUntil());
            final List<HumanTaskInstance> pendingTasks = getProcessAPI().getPendingHumanTaskInstances(john.getId(), 0, 10, null);
            final HumanTaskInstance pendingTask = pendingTasks.get(0);

            assignAndExecuteStep(pendingTask, john.getId());
        }
        Thread.sleep(500);
        assertTrue(new CheckNbPendingTaskOf(getProcessAPI(), 50, 5000, false, 0, john).waitUntil());

        disableAndDeleteProcess(processDefinition);
    }

    @Test
    public void executeAStandardLoopWithConditionUsingData() throws Exception {
        final Expression condition = new ExpressionBuilder().createGroovyScriptExpression("executeAStandardLoopWithConditionUsingData1", "myData < 3",
                Boolean.class.getName(), Arrays.asList(new ExpressionBuilder().createDataExpression("myData", Integer.class.getName())));

        final String delivery = "Delivery men";

        final ProcessDefinitionBuilder builder = new ProcessDefinitionBuilder().createNewInstance("executeAStandardLoopUserTaskWithData", "1.0");
        builder.addActor(delivery).addDescription("Delivery all day and night long");
        final int loopMax = 3;
        builder.addData("myData", Integer.class.getName(), new ExpressionBuilder().createConstantIntegerExpression(0));
        builder.addUserTask("step1", delivery)
                .addLoop(false, condition)
                .addOperation(
                        new LeftOperandBuilder().createNewInstance("myData").done(),
                        OperatorType.ASSIGNMENT,
                        "=",
                        null,
                        new ExpressionBuilder().createGroovyScriptExpression("executeAStandardLoopWithConditionUsingData1", "myData + 1",
                                Integer.class.getName(), Arrays.asList(new ExpressionBuilder().createDataExpression("myData", Integer.class.getName()))));

        final ProcessDefinition processDefinition = deployAndEnableWithActor(builder.done(), delivery, john);
        getProcessAPI().startProcess(processDefinition.getId());

        for (int i = 0; i < loopMax; i++) {
            assertTrue(new CheckNbPendingTaskOf(getProcessAPI(), 50, 5000, false, 1, john).waitUntil());
            final List<HumanTaskInstance> pendingTasks = getProcessAPI().getPendingHumanTaskInstances(john.getId(), 0, 10, null);
            final HumanTaskInstance pendingTask = pendingTasks.get(0);

            assignAndExecuteStep(pendingTask, john.getId());
        }
        Thread.sleep(500);
        assertTrue(new CheckNbPendingTaskOf(getProcessAPI(), 50, 5000, false, 0, john).waitUntil());

        disableAndDeleteProcess(processDefinition);
    }

}
