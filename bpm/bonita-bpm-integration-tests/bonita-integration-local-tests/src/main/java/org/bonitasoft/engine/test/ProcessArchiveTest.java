package org.bonitasoft.engine.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.concurrent.Callable;

import org.bonitasoft.engine.bpm.comment.Comment;
import org.bonitasoft.engine.bpm.data.DataInstance;
import org.bonitasoft.engine.bpm.flownode.ActivityInstance;
import org.bonitasoft.engine.bpm.flownode.ArchivedActivityInstance;
import org.bonitasoft.engine.bpm.flownode.ArchivedFlowNodeInstanceNotFoundException;
import org.bonitasoft.engine.bpm.process.DesignProcessDefinition;
import org.bonitasoft.engine.bpm.process.ProcessDefinition;
import org.bonitasoft.engine.bpm.process.ProcessInstance;
import org.bonitasoft.engine.bpm.process.impl.ProcessDefinitionBuilder;
import org.bonitasoft.engine.bpm.process.impl.UserTaskDefinitionBuilder;
import org.bonitasoft.engine.core.process.comment.api.SCommentService;
import org.bonitasoft.engine.core.process.instance.api.TransitionService;
import org.bonitasoft.engine.data.instance.api.DataInstanceService;
import org.bonitasoft.engine.data.instance.model.archive.SADataInstance;
import org.bonitasoft.engine.exception.BonitaException;
import org.bonitasoft.engine.expression.ExpressionBuilder;
import org.bonitasoft.engine.identity.User;
import org.bonitasoft.engine.operation.LeftOperandBuilder;
import org.bonitasoft.engine.operation.OperatorType;
import org.bonitasoft.engine.service.TenantServiceAccessor;
import org.bonitasoft.engine.transaction.UserTransactionService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ProcessArchiveTest extends CommonAPILocalTest {

    private static final String JOHN = "john";

    private User john;

    @Before
    public void beforeTest() throws BonitaException {
        login();
        john = createUser(JOHN, "bpm");
    }

    @After
    public void afterTest() throws BonitaException {
        deleteUser(john);
        logout();
    }

    @Test
    public void deleteProcessDefinitionDeleteArchivedInstances() throws Exception {
        final long initialNumberOfArchivedProcessInstance = getProcessAPI().getNumberOfArchivedProcessInstances();
        final DesignProcessDefinition designProcessDefinition = new ProcessDefinitionBuilder().createNewInstance("ProcessToDelete", "1.0")
                .addAutomaticTask("step1").getProcess();
        final ProcessDefinition processDefinition = deployAndEnableProcess(designProcessDefinition);
        final ProcessInstance p1 = getProcessAPI().startProcess(processDefinition.getId());
        final ProcessInstance p2 = getProcessAPI().startProcess(processDefinition.getId());
        final ProcessInstance p3 = getProcessAPI().startProcess(processDefinition.getId());
        waitForProcessToFinish(p1);
        waitForProcessToFinish(p2);
        waitForProcessToFinish(p3);
        assertEquals(initialNumberOfArchivedProcessInstance + 3, getProcessAPI().getNumberOfArchivedProcessInstances());
        disableAndDeleteProcess(processDefinition);
        assertEquals(initialNumberOfArchivedProcessInstance, getProcessAPI().getNumberOfArchivedProcessInstances());
    }

    @Test()
    public void deleteProcessDefinitionDeleteArchivedInstancesWithData() throws Exception {
        setSessionInfo(getSession()); // the session was cleaned by api call. This must be improved
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final UserTransactionService userTransactionService = tenantAccessor.getUserTransactionService();
        final DataInstanceService dataInstanceService = tenantAccessor.getDataInstanceService();
        final long initialNumberOfArchivedProcessInstance = getProcessAPI().getNumberOfArchivedProcessInstances();
        final ProcessDefinitionBuilder processDefinitionBuilder = new ProcessDefinitionBuilder().createNewInstance("ProcessToDelete", "1.0");
        processDefinitionBuilder.addActor("actor");
        processDefinitionBuilder.addShortTextData("procData", new ExpressionBuilder().createConstantStringExpression("procDataBalue"));
        final UserTaskDefinitionBuilder userTaskDefinitionBuilder = processDefinitionBuilder.addUserTask("step1", "actor");
        userTaskDefinitionBuilder.addOperation(new LeftOperandBuilder().createNewInstance("procData").done(), OperatorType.ASSIGNMENT, "=", null,
                new ExpressionBuilder().createConstantStringExpression("updated proc value"));
        userTaskDefinitionBuilder.addOperation(new LeftOperandBuilder().createNewInstance("activityData").done(), OperatorType.ASSIGNMENT, "=", null,
                new ExpressionBuilder().createConstantStringExpression("updated a value"));
        processDefinitionBuilder.addShortTextData("activityData", new ExpressionBuilder().createConstantStringExpression("activityDataBalue")).getProcess();
        final DesignProcessDefinition designProcessDefinition = processDefinitionBuilder.getProcess();
        final ProcessDefinition processDefinition = deployAndEnableWithActor(designProcessDefinition, "actor", john);
        final ProcessInstance p1 = getProcessAPI().startProcess(processDefinition.getId());
        final ProcessInstance p2 = getProcessAPI().startProcess(processDefinition.getId());
        final ProcessInstance p3 = getProcessAPI().startProcess(processDefinition.getId());
        final ActivityInstance step1 = waitForUserTask("step1", p1);
        final DataInstance activityDataInstance = getProcessAPI().getActivityDataInstance("activityData", step1.getId());
        final DataInstance processDataInstance = getProcessAPI().getProcessDataInstance("procData", p1.getId());
        assertNotNull(activityDataInstance);
        assignAndExecuteStep(step1, john.getId());
        waitForUserTaskAndExecuteIt("step1", p2, john);
        waitForUserTaskAndExecuteIt("step1", p3, john);
        setSessionInfo(getSession()); // the session was cleaned by api call. This must be improved
        userTransactionService.executeInTransaction(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                List<SADataInstance> saActDataInstances = dataInstanceService.getSADataInstances(activityDataInstance.getId());
                assertTrue(saActDataInstances.size() > 0);
                List<SADataInstance> saProcDataInstances = dataInstanceService.getSADataInstances(processDataInstance.getId());
                assertTrue(saProcDataInstances.size() > 0);

                return null;
            };
        });
        waitForProcessToFinish(p1);
        waitForProcessToFinish(p2);
        waitForProcessToFinish(p3);
        assertEquals(initialNumberOfArchivedProcessInstance + 3, getProcessAPI().getNumberOfArchivedProcessInstances());
        disableAndDeleteProcess(processDefinition);
        assertEquals(initialNumberOfArchivedProcessInstance, getProcessAPI().getNumberOfArchivedProcessInstances());

        setSessionInfo(getSession()); // the session was cleaned by api call. This must be improved
        userTransactionService.executeInTransaction(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                List<SADataInstance> saActDataInstances = dataInstanceService.getSADataInstances(activityDataInstance.getId());
                List<SADataInstance> saProcDataInstances = dataInstanceService.getSADataInstances(processDataInstance.getId());
                assertEquals(toString(saActDataInstances), 0, saActDataInstances.size());
                assertEquals(0, saProcDataInstances.size());
                return null;
            };

            private String toString(final List<SADataInstance> saActDataInstances) {
                final StringBuilder stb = new StringBuilder("[");
                for (final SADataInstance saDataInstance : saActDataInstances) {
                    stb.append("name=");
                    stb.append(saDataInstance.getName());
                    stb.append("value=");
                    stb.append(saDataInstance.getValue());
                    stb.append(", ");
                }
                stb.append("]");
                return stb.toString();
            }

        });
        // TODO check data instance visibility mapping when archived
    }

    @Test
    public void deleteProcessDefinitionDeleteArchivedInstancesWithTransition() throws Exception {
        setSessionInfo(getSession());
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final UserTransactionService userTransactionService = tenantAccessor.getUserTransactionService();
        final TransitionService transitionService = tenantAccessor.getTransitionInstanceService();
        Callable<Long> getNumberOfArchivedTransitionInstances = new Callable<Long>() {
            @Override
            public Long call() throws Exception {
                return transitionService.getNumberOfArchivedTransitionInstances(null);
            }
        };

        final long initialNumber = userTransactionService.executeInTransaction(getNumberOfArchivedTransitionInstances);
        final long initialNumberOfArchivedProcessInstance = getProcessAPI().getNumberOfArchivedProcessInstances();
        final DesignProcessDefinition designProcessDefinition = new ProcessDefinitionBuilder().createNewInstance("ProcessToDelete", "1.0").addActor("actor")
                .addAutomaticTask("step1").addAutomaticTask("step2").addTransition("step1", "step2").getProcess();
        final ProcessDefinition processDefinition = deployAndEnableWithActor(designProcessDefinition, "actor", john);
        final ProcessInstance p1 = getProcessAPI().startProcess(processDefinition.getId());
        final ProcessInstance p2 = getProcessAPI().startProcess(processDefinition.getId());
        final ProcessInstance p3 = getProcessAPI().startProcess(processDefinition.getId());
        waitForProcessToFinish(p1);
        waitForProcessToFinish(p2);
        waitForProcessToFinish(p3);

        assertEquals(initialNumberOfArchivedProcessInstance + 3, getProcessAPI().getNumberOfArchivedProcessInstances());
        setSessionInfo(getSession()); // the session was cleaned by api call. This must be improved
        final long newNumber = userTransactionService.executeInTransaction(getNumberOfArchivedTransitionInstances);
        assertTrue(newNumber > initialNumber);
        disableAndDeleteProcess(processDefinition);
        assertEquals(initialNumberOfArchivedProcessInstance, getProcessAPI().getNumberOfArchivedProcessInstances());
        setSessionInfo(getSession()); // the session was cleaned by api call. This must be improved
        final long lastNumber = userTransactionService.executeInTransaction(getNumberOfArchivedTransitionInstances);
        assertEquals(initialNumber, lastNumber);
        cleanSession();
    }

    @Test
    public void deleteProcessDefinitionDeleteArchivedInstancesWithComment() throws Exception {
        setSessionInfo(getSession());
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final UserTransactionService userTransactionService = tenantAccessor.getUserTransactionService();
        final SCommentService commentService = tenantAccessor.getCommentService();
        logout();
        loginWith("john", "bpm");
        final long initialNumberOfArchivedProcessInstance = getProcessAPI().getNumberOfArchivedProcessInstances();
        final ProcessDefinitionBuilder processDefinitionBuilder = new ProcessDefinitionBuilder().createNewInstance("ProcessToDelete", "1.0");
        processDefinitionBuilder.addActor("actor");
        processDefinitionBuilder.addUserTask("step1", "actor");
        final DesignProcessDefinition designProcessDefinition = processDefinitionBuilder.getProcess();
        final ProcessDefinition processDefinition = deployAndEnableWithActor(designProcessDefinition, "actor", john);
        final ProcessInstance p1 = getProcessAPI().startProcess(processDefinition.getId());
        final ProcessInstance p2 = getProcessAPI().startProcess(processDefinition.getId());
        final ProcessInstance p3 = getProcessAPI().startProcess(processDefinition.getId());
        final ActivityInstance step1 = waitForUserTask("step1", p1);
        getProcessAPI().addProcessComment(p1.getId(), "A cool comment on p1");
        getProcessAPI().addProcessComment(p2.getId(), "A cool comment on p2");
        getProcessAPI().addProcessComment(p3.getId(), "A cool comment on p3");
        final List<Comment> comments = getProcessAPI().getComments(p1.getId());
        assertEquals(1, comments.size());
        assertEquals("A cool comment on p1", comments.get(0).getContent());

        setSessionInfo(getSession()); // the session was cleaned by api call. This must be improved
        userTransactionService.executeInTransaction(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                assertEquals(3, commentService.getNumberOfComments(null));
                assertEquals(0, commentService.getNumberOfArchivedComments(null));
                return null;
            }
        });
        assignAndExecuteStep(step1, john.getId());
        waitForUserTaskAndExecuteIt("step1", p2, john);
        waitForUserTaskAndExecuteIt("step1", p3, john);
        waitForProcessToFinish(p1);
        waitForProcessToFinish(p2);
        waitForProcessToFinish(p3);
        assertEquals(initialNumberOfArchivedProcessInstance + 3, getProcessAPI().getNumberOfArchivedProcessInstances());

        setSessionInfo(getSession()); // the session was cleaned by api call. This must be improved
        userTransactionService.executeInTransaction(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                assertEquals(0, commentService.getNumberOfComments(null));
                // 3 comments + 3 system comments
                assertEquals(6, commentService.getNumberOfArchivedComments(null));
                return null;
            }
        });
        disableAndDeleteProcess(processDefinition);
        assertEquals(initialNumberOfArchivedProcessInstance, getProcessAPI().getNumberOfArchivedProcessInstances());

        setSessionInfo(getSession()); // the session was cleaned by api call. This must be improved
        userTransactionService.executeInTransaction(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                assertEquals(0, commentService.getNumberOfComments(null));
                assertEquals(0, commentService.getNumberOfArchivedComments(null));
                return null;
            }
        });
        cleanSession();
    }

    @Test
    public void archivedFlowNodeInstance() throws Exception {
        logout();
        loginWith("john", "bpm");
        final ProcessDefinitionBuilder processDefinitionBuilder = new ProcessDefinitionBuilder().createNewInstance("ProcessToDelete", "1.0");
        processDefinitionBuilder.addActor("actor");
        processDefinitionBuilder.addUserTask("step1", "actor").addDescription("My Description")
        .addDisplayName(new ExpressionBuilder().createConstantStringExpression("My Display Name"))
        .addDisplayDescriptionAfterCompletion(new ExpressionBuilder().createConstantStringExpression("My Display Description"));
        final DesignProcessDefinition designProcessDefinition = processDefinitionBuilder.getProcess();
        final ProcessDefinition processDefinition = deployAndEnableWithActor(designProcessDefinition, "actor", john);
        final ProcessInstance p1 = getProcessAPI().startProcess(processDefinition.getId());
        final ActivityInstance userTask = waitForUserTask("step1", p1);
        assignAndExecuteStep(userTask, john.getId());
        waitForProcessToFinish(p1);
        waitForArchivedActivity(userTask.getId(), TestStates.getNormalFinalState());
        final ArchivedActivityInstance archivedUserTask = getProcessAPI().getArchivedActivityInstance(userTask.getId());
        assertEquals("My Description", archivedUserTask.getDescription());
        assertEquals("My Display Description", archivedUserTask.getDisplayDescription());
        assertEquals("My Display Name", archivedUserTask.getDisplayName());
        assertEquals("step1", archivedUserTask.getName());
        disableAndDeleteProcess(processDefinition);
    }

    @Test
    public void getArchivedFlowNodeInstance() throws Exception {
        logout();
        loginWith("john", "bpm");
        final ProcessDefinitionBuilder processDefinitionBuilder = new ProcessDefinitionBuilder().createNewInstance("ProcessToDelete", "1.0");
        processDefinitionBuilder.addActor("actor");
        processDefinitionBuilder.addUserTask("step1", "actor").addDescription("My Description")
        .addDisplayName(new ExpressionBuilder().createConstantStringExpression("My Display Name"))
        .addDisplayDescriptionAfterCompletion(new ExpressionBuilder().createConstantStringExpression("My Display Description"));
        final DesignProcessDefinition designProcessDefinition = processDefinitionBuilder.getProcess();
        final ProcessDefinition processDefinition = deployAndEnableWithActor(designProcessDefinition, "actor", john);
        final ProcessInstance p1 = getProcessAPI().startProcess(processDefinition.getId());
        final ActivityInstance userTask = waitForUserTask("step1", p1);
        assignAndExecuteStep(userTask, john.getId());
        waitForProcessToFinish(p1);
        final ArchivedActivityInstance archivedUserTask = getProcessAPI().getArchivedActivityInstance(userTask.getId());
        assertEquals(archivedUserTask, getProcessAPI().getArchivedFlowNodeInstance(archivedUserTask.getId()));
        disableAndDeleteProcess(processDefinition);
    }

    @Test(expected = ArchivedFlowNodeInstanceNotFoundException.class)
    public void getArchivedFlowNodeInstanceNotFound() throws ArchivedFlowNodeInstanceNotFoundException {
        getProcessAPI().getArchivedFlowNodeInstance(123456789l);
    }

}
