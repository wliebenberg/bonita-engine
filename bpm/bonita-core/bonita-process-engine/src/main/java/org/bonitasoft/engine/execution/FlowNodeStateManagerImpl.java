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
package org.bonitasoft.engine.execution;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.bonitasoft.engine.actor.mapping.ActorMappingService;
import org.bonitasoft.engine.archive.ArchiveService;
import org.bonitasoft.engine.bpm.flownode.FlowNodeType;
import org.bonitasoft.engine.bpm.model.impl.BPMInstancesCreator;
import org.bonitasoft.engine.classloader.ClassLoaderService;
import org.bonitasoft.engine.core.connector.ConnectorInstanceService;
import org.bonitasoft.engine.core.expression.control.api.ExpressionResolverService;
import org.bonitasoft.engine.core.filter.UserFilterService;
import org.bonitasoft.engine.core.operation.OperationService;
import org.bonitasoft.engine.core.process.comment.api.SCommentService;
import org.bonitasoft.engine.core.process.definition.ProcessDefinitionService;
import org.bonitasoft.engine.core.process.definition.model.SFlowNodeType;
import org.bonitasoft.engine.core.process.definition.model.SProcessDefinition;
import org.bonitasoft.engine.core.process.document.mapping.DocumentMappingService;
import org.bonitasoft.engine.core.process.instance.api.ActivityInstanceService;
import org.bonitasoft.engine.core.process.instance.api.ProcessInstanceService;
import org.bonitasoft.engine.core.process.instance.api.TokenService;
import org.bonitasoft.engine.core.process.instance.api.event.EventInstanceService;
import org.bonitasoft.engine.core.process.instance.api.exceptions.SActivityExecutionException;
import org.bonitasoft.engine.core.process.instance.api.states.FlowNodeState;
import org.bonitasoft.engine.core.process.instance.model.SFlowNodeInstance;
import org.bonitasoft.engine.data.instance.api.DataInstanceService;
import org.bonitasoft.engine.execution.event.EventsHandler;
import org.bonitasoft.engine.execution.state.AbortedFlowNodeStateImpl;
import org.bonitasoft.engine.execution.state.AbortingActivityWithBoundaryStateImpl;
import org.bonitasoft.engine.execution.state.AbortingBoundaryAndIntermediateCatchEventStateImpl;
import org.bonitasoft.engine.execution.state.AbortingCallActivityStateImpl;
import org.bonitasoft.engine.execution.state.AbortingFlowNodeContainerStateImpl;
import org.bonitasoft.engine.execution.state.AbortingFlowNodeStateImpl;
import org.bonitasoft.engine.execution.state.AbortingReceiveTaskStateImpl;
import org.bonitasoft.engine.execution.state.CancelledFlowNodeStateImpl;
import org.bonitasoft.engine.execution.state.CancellingActivityWithBoundaryStateImpl;
import org.bonitasoft.engine.execution.state.CancellingBoundaryAndIntermediateCatchEventStateImpl;
import org.bonitasoft.engine.execution.state.CancellingCallActivityStateImpl;
import org.bonitasoft.engine.execution.state.CancellingFlowNodeContainerChildrenStateImpl;
import org.bonitasoft.engine.execution.state.CancellingFlowNodeStateImpl;
import org.bonitasoft.engine.execution.state.CancellingReceiveTaskStateImpl;
import org.bonitasoft.engine.execution.state.CompletedActivityStateImpl;
import org.bonitasoft.engine.execution.state.CompletingActivityWithBoundaryStateImpl;
import org.bonitasoft.engine.execution.state.CompletingCallActivityStateImpl;
import org.bonitasoft.engine.execution.state.CompletingSubTaskStateImpl;
import org.bonitasoft.engine.execution.state.ExecutingAutomaticActivityStateImpl;
import org.bonitasoft.engine.execution.state.ExecutingBoundaryEventStateImpl;
import org.bonitasoft.engine.execution.state.ExecutingCallActivityStateImpl;
import org.bonitasoft.engine.execution.state.ExecutingFlowNodeStateImpl;
import org.bonitasoft.engine.execution.state.ExecutingLoopActivityStateImpl;
import org.bonitasoft.engine.execution.state.ExecutingMultiInstanceActivityStateImpl;
import org.bonitasoft.engine.execution.state.ExecutingThrowEventStateImpl;
import org.bonitasoft.engine.execution.state.FailedActivityStateImpl;
import org.bonitasoft.engine.execution.state.FlowNodeStateManager;
import org.bonitasoft.engine.execution.state.InitializinAndExecutingFlowNodeStateImpl;
import org.bonitasoft.engine.execution.state.InitializingActivityStateImpl;
import org.bonitasoft.engine.execution.state.InitializingActivityWithBoundaryEventsStateImpl;
import org.bonitasoft.engine.execution.state.InitializingBoundaryEventStateImpl;
import org.bonitasoft.engine.execution.state.InitializingLoopActivityStateImpl;
import org.bonitasoft.engine.execution.state.InitializingMultiInstanceActivityStateImpl;
import org.bonitasoft.engine.execution.state.InterruptedFlowNodeState;
import org.bonitasoft.engine.execution.state.ReadyActivityStateImpl;
import org.bonitasoft.engine.execution.state.SkippedFlowNodeStateImpl;
import org.bonitasoft.engine.execution.state.WaitingFlowNodeStateImpl;
import org.bonitasoft.engine.identity.IdentityService;
import org.bonitasoft.engine.log.technical.TechnicalLoggerService;
import org.bonitasoft.engine.scheduler.SchedulerService;
import org.bonitasoft.engine.work.WorkService;

/**
 * Default implementation of the activity state manager.
 * 
 * @author Baptiste Mesta
 * @author Matthieu Chaffotte
 * @author Yanyan Liu
 * @author Zhang Bole
 * @author Celine Souchet
 */
public class FlowNodeStateManagerImpl implements FlowNodeStateManager {

    protected final Map<Integer, FlowNodeState> states = new HashMap<Integer, FlowNodeState>();

    protected final Map<SFlowNodeType, Map<Integer, FlowNodeState>> normalTransitions = new HashMap<SFlowNodeType, Map<Integer, FlowNodeState>>();

    protected final Map<SFlowNodeType, Map<Integer, FlowNodeState>> abortTransitions = new HashMap<SFlowNodeType, Map<Integer, FlowNodeState>>();

    protected final Map<SFlowNodeType, Map<Integer, FlowNodeState>> cancelTransitions = new HashMap<SFlowNodeType, Map<Integer, FlowNodeState>>();

    protected Set<Integer> unstableStates;

    protected Set<Integer> stableStates;

    protected Set<Integer> allStates;

    protected FailedActivityStateImpl failed;

    protected InitializingActivityStateImpl initializing;

    protected InitializingActivityWithBoundaryEventsStateImpl initializingActivityWithBoundary;

    protected InitializingBoundaryEventStateImpl initializingBoundaryEvent;

    protected ReadyActivityStateImpl ready;

    protected ExecutingAutomaticActivityStateImpl executingAutomaticActivity;

    protected ExecutingThrowEventStateImpl executingThrowEvent;

    protected CompletingCallActivityStateImpl completingCallActivity;

    protected CompletingActivityWithBoundaryStateImpl completingActivityWithBoundary;

    protected ExecutingCallActivityStateImpl executingCallActivity;

    protected CompletedActivityStateImpl completed;

    protected WaitingFlowNodeStateImpl waiting;

    protected SkippedFlowNodeStateImpl skipped;

    protected InitializingLoopActivityStateImpl initializingLoop;

    protected ExecutingLoopActivityStateImpl executingLoop;

    protected InitializingMultiInstanceActivityStateImpl initializingMultiInstance;

    protected ExecutingMultiInstanceActivityStateImpl executingMultiInstance;

    protected CancelledFlowNodeStateImpl cancelled;

    protected CancellingFlowNodeContainerChildrenStateImpl cancellingContainer;

    protected CancellingActivityWithBoundaryStateImpl cancellingActivityWithBoundary;

    protected CancellingFlowNodeStateImpl cancellingFlowNode;

    protected CancellingBoundaryAndIntermediateCatchEventStateImpl cancelingBoundaryAndIntermediateCatchEvent;

    protected CancellingCallActivityStateImpl cancellingCallActivity;

    protected AbortingFlowNodeContainerStateImpl abortingContainer;

    protected AbortingCallActivityStateImpl abortingCallActivity;

    protected AbortingFlowNodeStateImpl abortingFlowNode;

    protected AbortingActivityWithBoundaryStateImpl abortingActivityWithBoundary;

    protected AbortingBoundaryAndIntermediateCatchEventStateImpl abortingBoundaryAndIntermediateCatchEvent;

    protected AbortedFlowNodeStateImpl aborted;

    protected InterruptedFlowNodeState interruptedFlowNodeState;

    protected ExecutingFlowNodeStateImpl executing;

    protected ExecutingBoundaryEventStateImpl executingBoundaryEvent;

    protected InitializinAndExecutingFlowNodeStateImpl initializingAndExecuting;

    protected StateBehaviors stateBehaviors;

    protected CompletingSubTaskStateImpl completingSubTaskState;

    protected AbortingReceiveTaskStateImpl abortingReceiveTask;

    protected CancellingReceiveTaskStateImpl cancellingReceiveTask;

    public FlowNodeStateManagerImpl(final ProcessDefinitionService processDefinitionService, final ProcessInstanceService processInstanceService,
            final ActivityInstanceService activityInstanceService, final ConnectorInstanceService connectorInstanceService,
            final ClassLoaderService classLoaderService,
            final ExpressionResolverService expressionResolverService, final SchedulerService schedulerService, final DataInstanceService dataInstanceService,
            final EventInstanceService eventInstanceService,
            final OperationService operationService, final BPMInstancesCreator bpmInstancesCreator, final ContainerRegistry containerRegistry,
            final ArchiveService archiveService, final TechnicalLoggerService logger, final DocumentMappingService documentMappingService,
            final SCommentService commentService, final EventsHandler eventsHandler,
            final UserFilterService userFilterService, final ActorMappingService actorMappingService, final WorkService workService,
            final TokenService tokenService, final IdentityService identityService) {
        initStates(connectorInstanceService, classLoaderService, expressionResolverService, schedulerService, dataInstanceService, eventInstanceService,
                operationService, activityInstanceService, bpmInstancesCreator, containerRegistry,
                processDefinitionService, processInstanceService, archiveService, logger, documentMappingService, commentService,
                eventsHandler, userFilterService, actorMappingService, workService, tokenService, identityService);
        defineTransitionsForAllNodesType();
        initializeFirstStatesIdsOnBPMInstanceCreator(bpmInstancesCreator);
    }

    private void initializeFirstStatesIdsOnBPMInstanceCreator(final BPMInstancesCreator bpmInstancesCreator) {
        final Set<Entry<SFlowNodeType, Map<Integer, FlowNodeState>>> entrySet = normalTransitions.entrySet();
        final HashMap<SFlowNodeType, Integer> firstStateIds = new HashMap<SFlowNodeType, Integer>(entrySet.size());
        final HashMap<SFlowNodeType, String> firstStateNames = new HashMap<SFlowNodeType, String>(entrySet.size());
        for (final Entry<SFlowNodeType, Map<Integer, FlowNodeState>> entry : entrySet) {
            firstStateIds.put(entry.getKey(), entry.getValue().get(-1).getId());
            firstStateNames.put(entry.getKey(), entry.getValue().get(-1).getName());
        }
        bpmInstancesCreator.setFirstStateIds(firstStateIds);
        bpmInstancesCreator.setFirstStateNames(firstStateNames);
    }

    @Override
    public void setProcessExecutor(ProcessExecutor processExecutor) {
        stateBehaviors.setProcessExecutor(processExecutor);
    }

    private void defineTransitionsForAllNodesType() {
        defineTransitionsForAutomaticTask();
        defineTransitionsForUserTask();
        defineTransitionsForManualTask();
        defineTransitionsForReceiveTask();
        defineTransitionsForSendTask();
        defineTransitionsForCallActivity();
        defineTransitionsForSubProcessActivity();
        defineTransitionsForGateways();
        defineTransitionsForStartEvent();
        defineTransitionsForIntermediateCatchEvent();
        defineTransitionsForBoundaryEvents();
        defineTransitionsForIntermediateThrowEvent();
        defineTransitionsForEndEvent();
        defineTransitionsForLoopActivity();
        defineTransitionsForMultiInstanceActivity();
    }

    private void defineTransitionsForEndEvent() {
        defineNormalTransitionForFlowNode(SFlowNodeType.END_EVENT, executingThrowEvent, completed);
        defineCancelTransitionForFlowNode(SFlowNodeType.END_EVENT, cancellingFlowNode, cancelled);
        defineAbortTransitionForFlowNode(SFlowNodeType.END_EVENT, abortingFlowNode, aborted);
    }

    private void defineTransitionsForIntermediateCatchEvent() {
        defineNormalTransitionForFlowNode(SFlowNodeType.INTERMEDIATE_CATCH_EVENT, initializing, waiting, executing, completed);
        defineCancelTransitionForFlowNode(SFlowNodeType.INTERMEDIATE_CATCH_EVENT, cancelingBoundaryAndIntermediateCatchEvent, cancelled);
        defineAbortTransitionForFlowNode(SFlowNodeType.INTERMEDIATE_CATCH_EVENT, abortingBoundaryAndIntermediateCatchEvent, aborted);
    }

    private void defineTransitionsForIntermediateThrowEvent() {
        defineNormalTransitionForFlowNode(SFlowNodeType.INTERMEDIATE_THROW_EVENT, executingThrowEvent, completed);
        defineCancelTransitionForFlowNode(SFlowNodeType.INTERMEDIATE_THROW_EVENT, cancellingFlowNode, cancelled);
        defineAbortTransitionForFlowNode(SFlowNodeType.INTERMEDIATE_THROW_EVENT, abortingFlowNode, aborted);
    }

    private void defineTransitionsForStartEvent() {
        defineNormalTransitionForFlowNode(SFlowNodeType.START_EVENT, initializingAndExecuting, completed);
        defineCancelTransitionForFlowNode(SFlowNodeType.START_EVENT, cancellingFlowNode, cancelled);
        defineAbortTransitionForFlowNode(SFlowNodeType.START_EVENT, abortingFlowNode, aborted);
    }

    private void defineTransitionsForBoundaryEvents() {
        defineNormalTransitionForFlowNode(SFlowNodeType.BOUNDARY_EVENT, initializingBoundaryEvent, waiting, executingBoundaryEvent, completed);
        defineAbortTransitionForFlowNode(SFlowNodeType.BOUNDARY_EVENT, aborted);
        defineCancelTransitionForFlowNode(SFlowNodeType.BOUNDARY_EVENT, cancelled);
    }

    private void defineTransitionsForGateways() {
        defineNormalTransitionForFlowNode(SFlowNodeType.GATEWAY, initializingAndExecuting, completed);
        defineCancelTransitionForFlowNode(SFlowNodeType.GATEWAY, cancellingFlowNode, cancelled);
        defineAbortTransitionForFlowNode(SFlowNodeType.GATEWAY, abortingFlowNode, aborted);
    }

    private void defineTransitionsForUserTask() {
        defineNormalTransitionForFlowNode(SFlowNodeType.USER_TASK, initializingActivityWithBoundary, ready, completingActivityWithBoundary,
                completingSubTaskState, completed);
        defineAbortTransitionForFlowNode(SFlowNodeType.USER_TASK, abortingActivityWithBoundary, abortingContainer, aborted);
        defineCancelTransitionForFlowNode(SFlowNodeType.USER_TASK, cancellingActivityWithBoundary, cancellingContainer, cancelled);
    }

    private void defineTransitionsForManualTask() {
        defineNormalTransitionForFlowNode(SFlowNodeType.MANUAL_TASK, initializing, ready, completingSubTaskState, completed);
        defineAbortTransitionForFlowNode(SFlowNodeType.MANUAL_TASK, abortingContainer, aborted);
        defineCancelTransitionForFlowNode(SFlowNodeType.MANUAL_TASK, cancellingContainer, cancelled);
    }

    private void defineTransitionsForCallActivity() {
        defineNormalTransitionForFlowNode(SFlowNodeType.CALL_ACTIVITY, initializingActivityWithBoundary, executingCallActivity, completingActivityWithBoundary,
                completingCallActivity, completed);
        defineAbortTransitionForFlowNode(SFlowNodeType.CALL_ACTIVITY, abortingActivityWithBoundary, abortingCallActivity, aborted);
        defineCancelTransitionForFlowNode(SFlowNodeType.CALL_ACTIVITY, cancellingActivityWithBoundary, cancellingCallActivity, cancelled);
    }

    private void defineTransitionsForSubProcessActivity() {
        defineNormalTransitionForFlowNode(SFlowNodeType.SUB_PROCESS, executingCallActivity, completed);
        defineAbortTransitionForFlowNode(SFlowNodeType.SUB_PROCESS, abortingCallActivity, aborted);
        defineCancelTransitionForFlowNode(SFlowNodeType.SUB_PROCESS, cancellingCallActivity, cancelled);
    }

    private void defineTransitionsForAutomaticTask() {
        defineNormalTransitionForFlowNode(SFlowNodeType.AUTOMATIC_TASK, executingAutomaticActivity, completingActivityWithBoundary, completed);
        defineAbortTransitionForFlowNode(SFlowNodeType.AUTOMATIC_TASK, abortingActivityWithBoundary, abortingContainer, aborted);
        defineCancelTransitionForFlowNode(SFlowNodeType.AUTOMATIC_TASK, cancellingActivityWithBoundary, cancellingContainer, cancelled);
    }

    private void defineTransitionsForReceiveTask() {
        defineNormalTransitionForFlowNode(SFlowNodeType.RECEIVE_TASK, initializingActivityWithBoundary, waiting, executing, completingActivityWithBoundary,
                completed);
        defineAbortTransitionForFlowNode(SFlowNodeType.RECEIVE_TASK, abortingActivityWithBoundary, abortingReceiveTask, aborted);
        defineCancelTransitionForFlowNode(SFlowNodeType.RECEIVE_TASK, cancellingActivityWithBoundary, cancellingReceiveTask, cancelled);
    }

    private void defineTransitionsForSendTask() {
        defineNormalTransitionForFlowNode(SFlowNodeType.SEND_TASK, executingAutomaticActivity, completingActivityWithBoundary, completed);
        defineAbortTransitionForFlowNode(SFlowNodeType.SEND_TASK, abortingActivityWithBoundary, abortingReceiveTask, aborted);
        defineCancelTransitionForFlowNode(SFlowNodeType.SEND_TASK, cancellingActivityWithBoundary, cancellingReceiveTask, cancelled);
    }

    private void defineTransitionsForLoopActivity() {
        defineNormalTransitionForFlowNode(SFlowNodeType.LOOP_ACTIVITY, initializingLoop, executingLoop, completingActivityWithBoundary, completed);
        defineAbortTransitionForFlowNode(SFlowNodeType.LOOP_ACTIVITY, abortingActivityWithBoundary, abortingContainer, aborted);
        defineCancelTransitionForFlowNode(SFlowNodeType.LOOP_ACTIVITY, cancellingActivityWithBoundary, cancellingContainer, cancelled);
    }

    private void defineTransitionsForMultiInstanceActivity() {
        defineNormalTransitionForFlowNode(SFlowNodeType.MULTI_INSTANCE_ACTIVITY, initializingMultiInstance, executingMultiInstance,
                completingActivityWithBoundary, completed);
        defineAbortTransitionForFlowNode(SFlowNodeType.MULTI_INSTANCE_ACTIVITY, abortingActivityWithBoundary, abortingContainer, aborted);
        defineCancelTransitionForFlowNode(SFlowNodeType.MULTI_INSTANCE_ACTIVITY, cancellingActivityWithBoundary, cancellingContainer, cancelled);
    }

    private void initStates(final ConnectorInstanceService connectorInstanceService, final ClassLoaderService classLoaderService,
            final ExpressionResolverService expressionResolverService, final SchedulerService schedulerService, final DataInstanceService dataInstanceService,
            final EventInstanceService eventInstanceService, 
            final OperationService operationService, final ActivityInstanceService activityInstanceService, final BPMInstancesCreator bpmInstancesCreator,
            final ContainerRegistry containerRegistry, final ProcessDefinitionService processDefinitionService,
            final ProcessInstanceService processInstanceService, final ArchiveService archiveService, final TechnicalLoggerService logger,
            final DocumentMappingService documentMappingService, final SCommentService commentService,
            final EventsHandler eventsHandler, final UserFilterService userFilterService,
            final ActorMappingService actorMappingService, final WorkService workService, final TokenService tokenService, final IdentityService identityService) {
        stateBehaviors = new StateBehaviors(bpmInstancesCreator, eventsHandler, activityInstanceService, userFilterService, classLoaderService,
                actorMappingService, connectorInstanceService, expressionResolverService, processDefinitionService, dataInstanceService,
                operationService, workService, containerRegistry, eventInstanceService, schedulerService, commentService, identityService, logger);
        failed = new FailedActivityStateImpl();
        initializing = new InitializingActivityStateImpl(stateBehaviors);
        initializingActivityWithBoundary = new InitializingActivityWithBoundaryEventsStateImpl(stateBehaviors);
        initializingBoundaryEvent = new InitializingBoundaryEventStateImpl(stateBehaviors);
        initializingLoop = new InitializingLoopActivityStateImpl(expressionResolverService, bpmInstancesCreator, activityInstanceService, stateBehaviors);
        ready = new ReadyActivityStateImpl(stateBehaviors);
        executing = new ExecutingFlowNodeStateImpl(stateBehaviors);
        executingBoundaryEvent = new ExecutingBoundaryEventStateImpl(activityInstanceService, containerRegistry, tokenService);
        initializingAndExecuting = new InitializinAndExecutingFlowNodeStateImpl(stateBehaviors);
        executingAutomaticActivity = new ExecutingAutomaticActivityStateImpl(stateBehaviors);
        executingThrowEvent = new ExecutingThrowEventStateImpl(stateBehaviors);
        executingLoop = new ExecutingLoopActivityStateImpl(expressionResolverService, bpmInstancesCreator, containerRegistry, activityInstanceService);
        completingCallActivity = new CompletingCallActivityStateImpl(stateBehaviors, operationService, processInstanceService, dataInstanceService,
                documentMappingService, logger, archiveService, commentService, processDefinitionService,
                connectorInstanceService);
        completingActivityWithBoundary = new CompletingActivityWithBoundaryStateImpl(stateBehaviors);
        executingCallActivity = new ExecutingCallActivityStateImpl(stateBehaviors);
        completed = new CompletedActivityStateImpl();
        waiting = new WaitingFlowNodeStateImpl();
        skipped = new SkippedFlowNodeStateImpl();
        cancelled = new CancelledFlowNodeStateImpl();
        cancellingContainer = new CancellingFlowNodeContainerChildrenStateImpl(stateBehaviors);
        cancellingFlowNode = new CancellingFlowNodeStateImpl();
        cancelingBoundaryAndIntermediateCatchEvent = new CancellingBoundaryAndIntermediateCatchEventStateImpl(stateBehaviors);
        cancellingCallActivity = new CancellingCallActivityStateImpl(activityInstanceService, processInstanceService, containerRegistry,
                archiveService, commentService, dataInstanceService, documentMappingService, logger, processDefinitionService,
                connectorInstanceService);
        cancellingActivityWithBoundary = new CancellingActivityWithBoundaryStateImpl(stateBehaviors);
        cancellingReceiveTask = new CancellingReceiveTaskStateImpl(stateBehaviors);
        initializingMultiInstance = new InitializingMultiInstanceActivityStateImpl(expressionResolverService, bpmInstancesCreator, activityInstanceService,
                dataInstanceService, stateBehaviors);
        executingMultiInstance = new ExecutingMultiInstanceActivityStateImpl(expressionResolverService, bpmInstancesCreator, containerRegistry,
                activityInstanceService, dataInstanceService, stateBehaviors);
        abortingContainer = new AbortingFlowNodeContainerStateImpl(stateBehaviors);
        abortingCallActivity = new AbortingCallActivityStateImpl(activityInstanceService, processInstanceService, containerRegistry,
                archiveService, commentService, dataInstanceService, documentMappingService, logger, processDefinitionService,
                connectorInstanceService);
        abortingFlowNode = new AbortingFlowNodeStateImpl();
        abortingBoundaryAndIntermediateCatchEvent = new AbortingBoundaryAndIntermediateCatchEventStateImpl(stateBehaviors);
        abortingActivityWithBoundary = new AbortingActivityWithBoundaryStateImpl(stateBehaviors);
        abortingReceiveTask = new AbortingReceiveTaskStateImpl(stateBehaviors);
        aborted = new AbortedFlowNodeStateImpl();
        interruptedFlowNodeState = new InterruptedFlowNodeState();
        completingSubTaskState = new CompletingSubTaskStateImpl(stateBehaviors);

        final HashSet<Integer> unstableStatesModifiable = new HashSet<Integer>();
        final HashSet<Integer> stableStatesModifiable = new HashSet<Integer>();

        // fill map of states
        addToMap(failed);
        addToMap(initializing);
        addToMap(initializingBoundaryEvent);
        addToMap(initializingActivityWithBoundary);
        addToMap(initializingLoop);
        addToMap(completingCallActivity);
        addToMap(executingCallActivity);
        addToMap(completed);
        addToMap(executing);
        addToMap(executingBoundaryEvent);
        addToMap(executingThrowEvent);
        addToMap(executingLoop);
        addToMap(ready);
        addToMap(waiting);
        addToMap(skipped);
        addToMap(cancelled);
        addToMap(cancellingContainer);
        addToMap(cancellingFlowNode);
        addToMap(cancellingCallActivity);
        addToMap(cancelingBoundaryAndIntermediateCatchEvent);
        addToMap(cancellingActivityWithBoundary);
        addToMap(cancellingReceiveTask);
        addToMap(initializingMultiInstance);
        addToMap(executingMultiInstance);
        addToMap(abortingContainer);
        addToMap(abortingCallActivity);
        addToMap(abortingFlowNode);
        addToMap(abortingActivityWithBoundary);
        addToMap(abortingBoundaryAndIntermediateCatchEvent);
        addToMap(abortingReceiveTask);
        addToMap(aborted);
        addToMap(interruptedFlowNodeState);
        addToMap(executingAutomaticActivity);
        addToMap(initializingAndExecuting);
        addToMap(completingSubTaskState);
        addToMap(completingActivityWithBoundary);

        allStates = Collections.unmodifiableSet(states.keySet());

        for (final FlowNodeState state : states.values()) {
            if (!state.isStable()) {
                unstableStatesModifiable.add(state.getId());
            } else {
                stableStatesModifiable.add(state.getId());
            }
        }
        unstableStates = Collections.unmodifiableSet(unstableStatesModifiable);
        stableStates = Collections.unmodifiableSet(stableStatesModifiable);
    }

    private void addToMap(final FlowNodeState state) {
        if (states.containsKey(state.getId())) {
            throw new RuntimeException("states already contains a state with id= " + state.getId() + " try to add<" + state.getClass() + "> but was already <"
                    + states.get(state.getId()).getClass() + ">");
        }
        states.put(state.getId(), state);
    }

    private void defineNormalTransitionForFlowNode(final SFlowNodeType flowNodeType, final FlowNodeState... states) {
        defineTransitionsForFlowNode(flowNodeType, normalTransitions, states);
    }

    private void defineAbortTransitionForFlowNode(final SFlowNodeType flowNodeType, final FlowNodeState... states) {
        defineTransitionsForFlowNode(flowNodeType, abortTransitions, states);
    }

    private void defineCancelTransitionForFlowNode(final SFlowNodeType flowNodeType, final FlowNodeState... states) {
        defineTransitionsForFlowNode(flowNodeType, cancelTransitions, states);
    }

    private void defineTransitionsForFlowNode(final SFlowNodeType flowNodeType, final Map<SFlowNodeType, Map<Integer, FlowNodeState>> transitions,
            final FlowNodeState... states) {
        final HashMap<Integer, FlowNodeState> taskTransitions = new HashMap<Integer, FlowNodeState>();
        int stateIndex = 0;
        taskTransitions.put(-1, states[0]);
        while (stateIndex < states.length - 1) {
            taskTransitions.put(states[stateIndex].getId(), states[stateIndex + 1]);
            stateIndex++;
        }
        transitions.put(flowNodeType, taskTransitions);
    }

    @Override
    public FlowNodeState getNextNormalState(final SProcessDefinition processDefinition, final SFlowNodeInstance flowNodeInstance, final int currentState)
            throws SActivityExecutionException {
        FlowNodeState flowNodeState;
        final Map<Integer, FlowNodeState> normalTransition = normalTransitions.get(flowNodeInstance.getType());
        final Map<Integer, FlowNodeState> flowNodeAbortTransitions = abortTransitions.get(flowNodeInstance.getType());
        final Map<Integer, FlowNodeState> flowNodeCancelTransitions = cancelTransitions.get(flowNodeInstance.getType());
        final FlowNodeState current = getState(currentState);
        int nextState;
        if (current.isInterrupting()) {
            final int previousStateId = flowNodeInstance.getPreviousStateId();
            nextState = states.get(previousStateId).getId();
        } else {
            nextState = currentState;
        }
        do {
            switch (flowNodeInstance.getStateCategory()) {
                case ABORTING:
                    flowNodeState = flowNodeAbortTransitions.get(nextState);
                    // next state should be aborting
                    if (flowNodeState == null) {
                        flowNodeState = flowNodeAbortTransitions.get(-1);
                    }

                    break;
                case CANCELLING:
                    flowNodeState = flowNodeCancelTransitions.get(nextState);
                    // next state should be canceling
                    if (flowNodeState == null) {
                        flowNodeState = flowNodeCancelTransitions.get(-1);
                    }
                    break;

                default:
                    flowNodeState = normalTransition.get(nextState);
                    break;
            }
            if (flowNodeState == null) {
                throw new SActivityExecutionException("no state found after " + states.get(currentState).getClass() + " for " + flowNodeInstance.getClass()
                        + " in state category " + flowNodeInstance.getStateCategory() + " activity id=" + flowNodeInstance.getId());
            }
            nextState = flowNodeState.getId();
        } while (!flowNodeState.shouldExecuteState(processDefinition, flowNodeInstance));
        return flowNodeState;
    }

    @Override
    public FlowNodeState getFailedState() {
        return failed;
    }

    @Override
    public FlowNodeState getState(final int stateId) {
        return states.get(stateId);
    }

    @Override
    public FlowNodeState getNormalFinalState(final SFlowNodeInstance flowNodeInstance) {
        return completed;
    }

    @Override
    public FlowNodeState getSkippedState(final SFlowNodeInstance flownNodeInstance) {
        return skipped;
    }

    @Override
    public FlowNodeState getCanceledState(final SFlowNodeInstance flownNodeInstance) {
        return cancelled;
    }

    @Override
    public FlowNodeState getInitialState(final SFlowNodeInstance flowNodeInstance) {
        return initializing;
    }

    @Override
    public Set<Integer> getUnstableStateIds() {
        return unstableStates;
    }

    @Override
    public Set<Integer> getStableStateIds() {
        return stableStates;
    }

    @Override
    public Set<Integer> getAllStates() {
        return allStates;
    }

    @Override
    public Set<String> getSupportedState(final FlowNodeType nodeType) {
        final SFlowNodeType type = SFlowNodeType.valueOf(nodeType.toString());
        final Map<Integer, FlowNodeState> states = normalTransitions.get(type);
        final Set<String> stateNames = new HashSet<String>();
        for (final FlowNodeState state : states.values()) {
            stateNames.add(state.getName());
        }
        return stateNames;
    }

}
