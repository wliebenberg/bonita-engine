package org.bonitasoft.engine.execution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.bonitasoft.engine.bpm.connector.ConnectorState.EXECUTING;
import static org.bonitasoft.engine.bpm.connector.ConnectorState.TO_BE_EXECUTED;
import static org.bonitasoft.engine.execution.StateBehaviors.AFTER_ON_FINISH;
import static org.bonitasoft.engine.execution.StateBehaviors.BEFORE_ON_ENTER;
import static org.bonitasoft.engine.execution.StateBehaviors.BEFORE_ON_FINISH;
import static org.bonitasoft.engine.execution.StateBehaviors.DURING_ON_ENTER;
import static org.bonitasoft.engine.execution.StateBehaviors.DURING_ON_FINISH;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.bonitasoft.engine.bpm.bar.xml.XMLProcessDefinition.BEntry;
import org.bonitasoft.engine.bpm.connector.ConnectorEvent;
import org.bonitasoft.engine.bpm.connector.ConnectorState;
import org.bonitasoft.engine.core.connector.ConnectorInstanceService;
import org.bonitasoft.engine.core.connector.exception.SConnectorInstanceReadException;
import org.bonitasoft.engine.core.process.definition.model.SConnectorDefinition;
import org.bonitasoft.engine.core.process.definition.model.SFlowElementContainerDefinition;
import org.bonitasoft.engine.core.process.definition.model.SFlowNodeDefinition;
import org.bonitasoft.engine.core.process.definition.model.SProcessDefinition;
import org.bonitasoft.engine.core.process.instance.api.exceptions.SActivityStateExecutionException;
import org.bonitasoft.engine.core.process.instance.model.SConnectorInstance;
import org.bonitasoft.engine.core.process.instance.model.SFlowNodeInstance;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Phase order is: BEFORE_ON_ENTER > DURING_ON_ENTER > BEFORE_ON_FINISH > DURING_ON_FINISH > AFTER_ON_FINISH.
 * 
 * @author Emmanuel Duchastenier
 */
@RunWith(MockitoJUnitRunner.class)
public class StateBehaviorsTest {

    @Mock
    private SFlowNodeInstance flowNodeInstance;

    @Mock
    private SProcessDefinition processDefinition;

    @Mock
    private SFlowElementContainerDefinition containerDefinition;

    @Mock
    private SFlowNodeDefinition flowNodeDefinition;

    @Mock
    private ConnectorInstanceService connectorInstanceService;

    @InjectMocks
    private StateBehaviors behaviors;

    long flownodeInstanceId = 3541L;

    private final static Map<Integer, String> phaseNames = new HashMap<Integer, String>(5);
    static {
        phaseNames.put(1, "BEFORE_ON_ENTER");
        phaseNames.put(2, "DURING_ON_ENTER");
        phaseNames.put(4, "BEFORE_ON_FINISH");
        phaseNames.put(5, "DURING_ON_FINISH");
        phaseNames.put(16, "AFTER_ON_FINISH");
    }

    private static final List<Integer> KNOWN_PHASES = Arrays.asList(BEFORE_ON_ENTER, // 1
            DURING_ON_ENTER, // 2
            BEFORE_ON_FINISH, // 4
            DURING_ON_FINISH, // 8
            AFTER_ON_FINISH); // 16

    @Before
    public void setConstants() {
        when(processDefinition.getProcessContainer()).thenReturn(containerDefinition);
        when(containerDefinition.getFlowNode(anyLong())).thenReturn(flowNodeDefinition);
        when(flowNodeInstance.getId()).thenReturn(flownodeInstanceId);
    }

    protected void checkConnectorsPhases(final BEntry<Integer, BEntry<SConnectorInstance, SConnectorDefinition>> connectorAndFlag,
            final Integer... expectedPhases) {
        final List<Integer> expectedPhaseList = Arrays.asList(expectedPhases);
        for (final Integer expectedPhase : KNOWN_PHASES) {
            if (expectedPhaseList.contains(expectedPhase)) {
                assertTrue("Phase " + getPhaseName(expectedPhase) + " should be present", isPhaseIncluded(connectorAndFlag.getKey(), expectedPhase));
            } else {
                assertFalse("Phase " + getPhaseName(expectedPhase) + " should NOT be present", isPhaseIncluded(connectorAndFlag.getKey(), expectedPhase));
            }
        }
    }

    private String getPhaseName(final Integer expectedPhase) {
        return phaseNames.get(expectedPhase);
    }

    protected void checkNoConnectorsAndAllDefaultPhases(final BEntry<Integer, BEntry<SConnectorInstance, SConnectorDefinition>> connectorAndFlag) {
        checkConnectorsPhases(connectorAndFlag, BEFORE_ON_ENTER, BEFORE_ON_FINISH, AFTER_ON_FINISH);
        assertThat(connectorAndFlag.getValue().getKey()).isEqualTo(null);
        assertThat(connectorAndFlag.getValue().getValue()).isEqualTo(null);
    }

    private boolean isPhaseIncluded(final int actualPhases, final int expectedPhase) {
        return (actualPhases & expectedPhase) != 0;
    }

    @Test
    public void nullFlowNodeDefinitionShouldReturnNoConnectorsAndAllPhases() throws Exception {
        // given:
        when(containerDefinition.getFlowNode(anyLong())).thenReturn(null);
        // when:
        final BEntry<Integer, BEntry<SConnectorInstance, SConnectorDefinition>> connectorAndFlag = behaviors.getConnectorToExecuteAndFlag(processDefinition,
                flowNodeInstance, true, true);
        // then:
        checkNoConnectorsAndAllDefaultPhases(connectorAndFlag);
    }

    @Test
    public void flowNodeWithNoConnectorsShouldReturnNoConnectorsAndAllPhases() throws Exception {
        // given:
        when(flowNodeDefinition.getConnectors(ConnectorEvent.ON_ENTER)).thenReturn(mock(List.class));
        // when:
        final BEntry<Integer, BEntry<SConnectorInstance, SConnectorDefinition>> connectorAndFlag = behaviors.getConnectorToExecuteAndFlag(processDefinition,
                flowNodeInstance, true, true);
        checkNoConnectorsAndAllDefaultPhases(connectorAndFlag);
    }

    @Test
    public void flowNodeWithNoConnectorInstanceToExecuteShouldReturnNoConnectorsAndAllPhases() throws Exception {
        // given:
        final List<SConnectorDefinition> connectors = mock(List.class);
        when(flowNodeDefinition.getConnectors(ConnectorEvent.ON_ENTER)).thenReturn(connectors);
        when(connectors.size()).thenReturn(1);
        when(connectorInstanceService.getNextExecutableConnectorInstance(eq(flownodeInstanceId), anyString(), eq(ConnectorEvent.ON_ENTER))).thenReturn(null);
        // when:
        final BEntry<Integer, BEntry<SConnectorInstance, SConnectorDefinition>> connectorAndFlag = behaviors.getConnectorToExecuteAndFlag(processDefinition,
                flowNodeInstance, true, true);
        // then:
        verify(connectorInstanceService, times(1)).getNextExecutableConnectorInstance(eq(flownodeInstanceId), anyString(), eq(ConnectorEvent.ON_ENTER));
        checkNoConnectorsAndAllDefaultPhases(connectorAndFlag);
    }

    @Test(expected = SActivityStateExecutionException.class)
    public void flowNodeWithNoConnectorInstanceOnEnterInStateTO_BE_EXECUTEDShouldThrowException() throws Exception {
        // given:
        final List<SConnectorDefinition> connectors = mock(List.class);
        when(flowNodeDefinition.getConnectors(ConnectorEvent.ON_ENTER)).thenReturn(connectors);
        when(connectors.size()).thenReturn(1);
        final SConnectorInstance nextConnectorInstanceToExecute = mock(SConnectorInstance.class);
        when(connectorInstanceService.getNextExecutableConnectorInstance(eq(flownodeInstanceId), anyString(), eq(ConnectorEvent.ON_ENTER))).thenReturn(
                nextConnectorInstanceToExecute);
        when(nextConnectorInstanceToExecute.getState()).thenReturn("dummyState");
        when(nextConnectorInstanceToExecute.getName()).thenReturn("no_matching_name");
        final SConnectorDefinition conn = mock(SConnectorDefinition.class);
        when(conn.getName()).thenReturn("some_name");
        final Iterator<SConnectorDefinition> connIterator = addConnectorIterator(connectors, conn);

        // when:
        try {
            behaviors.getConnectorToExecuteAndFlag(processDefinition, flowNodeInstance, true, true);
        } finally {
            // then:
            verify(connectorInstanceService, times(1)).getNextExecutableConnectorInstance(eq(flownodeInstanceId), anyString(), eq(ConnectorEvent.ON_ENTER));
            verify(connIterator, times(1)).next();
        }
    }

    @Test
    public void firstConnectorNotYetInExecutingStateShouldReturnFirst2Phases() throws Exception {
        // given:
        final List<SConnectorDefinition> connectors = mock(List.class);
        when(flowNodeDefinition.getConnectors(ConnectorEvent.ON_ENTER)).thenReturn(connectors);
        when(connectors.size()).thenReturn(1);
        final SConnectorDefinition connectorDefinition = mock(SConnectorDefinition.class);
        when(connectors.get(0)).thenReturn(connectorDefinition);
        final SConnectorInstance nextConnectorInstanceToExecute = mock(SConnectorInstance.class);
        when(connectorInstanceService.getNextExecutableConnectorInstance(eq(flownodeInstanceId), anyString(), eq(ConnectorEvent.ON_ENTER))).thenReturn(
                nextConnectorInstanceToExecute);
        final String connectorName = "connector_name";
        when(nextConnectorInstanceToExecute.getState()).thenReturn(TO_BE_EXECUTED.name());
        when(connectorDefinition.getName()).thenReturn(connectorName);
        when(nextConnectorInstanceToExecute.getName()).thenReturn(connectorName);

        // when:
        final BEntry<Integer, BEntry<SConnectorInstance, SConnectorDefinition>> connectorAndFlag = behaviors.getConnectorToExecuteAndFlag(processDefinition,
                flowNodeInstance, true, true);

        // then:
        verify(connectorInstanceService, times(1)).getNextExecutableConnectorInstance(eq(flownodeInstanceId), anyString(), eq(ConnectorEvent.ON_ENTER));
        checkConnectorsPhases(connectorAndFlag, BEFORE_ON_ENTER, DURING_ON_ENTER);
        assertThat(connectorAndFlag.getValue().getKey()).isEqualTo(nextConnectorInstanceToExecute);
        assertThat(connectorAndFlag.getValue().getValue()).isEqualTo(connectorDefinition);
    }

    @Test
    public void firstConnectorAlreadyExecutingShouldReturnDURING_ON_ENTERonly() throws Exception {
        // given:
        final List<SConnectorDefinition> connectors = mock(List.class);
        when(flowNodeDefinition.getConnectors(ConnectorEvent.ON_ENTER)).thenReturn(connectors);
        when(connectors.size()).thenReturn(1);
        final SConnectorDefinition connectorDefinition = mock(SConnectorDefinition.class);
        when(connectors.get(0)).thenReturn(connectorDefinition);
        final SConnectorInstance nextConnectorInstanceToExecute = mock(SConnectorInstance.class);
        when(connectorInstanceService.getNextExecutableConnectorInstance(eq(flownodeInstanceId), anyString(), eq(ConnectorEvent.ON_ENTER))).thenReturn(
                nextConnectorInstanceToExecute);
        final String connectorName = "connector_name";
        when(nextConnectorInstanceToExecute.getState()).thenReturn(EXECUTING.name());
        when(connectorDefinition.getName()).thenReturn(connectorName);
        when(nextConnectorInstanceToExecute.getName()).thenReturn(connectorName);

        final Iterator<SConnectorDefinition> connIterator = mock(Iterator.class);
        when(connectors.iterator()).thenReturn(connIterator);
        when(connIterator.hasNext()).thenReturn(true, false);
        when(connIterator.next()).thenReturn(connectorDefinition);

        // when:
        final BEntry<Integer, BEntry<SConnectorInstance, SConnectorDefinition>> connectorAndFlag = behaviors.getConnectorToExecuteAndFlag(processDefinition,
                flowNodeInstance, true, true);

        // then:
        verify(connectorInstanceService, times(1)).getNextExecutableConnectorInstance(eq(flownodeInstanceId), anyString(), eq(ConnectorEvent.ON_ENTER));
        checkConnectorsPhases(connectorAndFlag, DURING_ON_ENTER);
        assertThat(connectorAndFlag.getValue().getKey()).isEqualTo(nextConnectorInstanceToExecute);
        assertThat(connectorAndFlag.getValue().getValue()).isEqualTo(connectorDefinition);
    }

    @Test
    public void secondConnectorOnExecutingFlownodeShouldReturnPhase_2_only() throws Exception {
        // given:
        final List<SConnectorDefinition> connectors = mock(List.class);
        when(flowNodeDefinition.getConnectors(ConnectorEvent.ON_ENTER)).thenReturn(connectors);
        when(connectors.size()).thenReturn(2);
        final SConnectorDefinition firstConnector = mock(SConnectorDefinition.class);
        final SConnectorDefinition secondConnector = mock(SConnectorDefinition.class);
        when(connectors.get(0)).thenReturn(firstConnector);
        final SConnectorInstance nextConnectorInstanceToExecute = mock(SConnectorInstance.class);
        when(connectorInstanceService.getNextExecutableConnectorInstance(eq(flownodeInstanceId), anyString(), eq(ConnectorEvent.ON_ENTER))).thenReturn(
                nextConnectorInstanceToExecute);
        final String connectorName = "connector_name";
        when(nextConnectorInstanceToExecute.getState()).thenReturn(TO_BE_EXECUTED.name());
        when(firstConnector.getName()).thenReturn("non_matching_name");
        when(secondConnector.getName()).thenReturn(connectorName);
        when(nextConnectorInstanceToExecute.getName()).thenReturn(connectorName);

        when(flowNodeInstance.isStateExecuting()).thenReturn(true);

        addConnectorIterator(connectors, firstConnector, secondConnector);

        // when:
        final BEntry<Integer, BEntry<SConnectorInstance, SConnectorDefinition>> connectorAndFlag = behaviors.getConnectorToExecuteAndFlag(processDefinition,
                flowNodeInstance, true, true);

        // then:
        verify(connectorInstanceService, times(1)).getNextExecutableConnectorInstance(eq(flownodeInstanceId), anyString(), eq(ConnectorEvent.ON_ENTER));
        checkConnectorsPhases(connectorAndFlag, DURING_ON_ENTER);
        assertThat(connectorAndFlag.getValue().getKey()).isEqualTo(nextConnectorInstanceToExecute);
        assertThat(connectorAndFlag.getValue().getValue()).isEqualTo(secondConnector);
    }

    @Test
    public void connectorsOnFinishToExecuteWithOnEnterConnAlreadyExecutedShouldReturnPhases_3_4() throws Exception {
        // given:
        haveConnectorAlreadyExecuted(ConnectorEvent.ON_ENTER);
        haveConnectorsToBeExecuted(ConnectorEvent.ON_FINISH);

        // when:
        final BEntry<Integer, BEntry<SConnectorInstance, SConnectorDefinition>> connectorAndFlag = behaviors.getConnectorToExecuteAndFlag(processDefinition,
                flowNodeInstance, true, true);

        // then:
        verify(connectorInstanceService, times(1)).getNextExecutableConnectorInstance(eq(flownodeInstanceId), anyString(), eq(ConnectorEvent.ON_ENTER));
        verify(connectorInstanceService, times(1)).getNextExecutableConnectorInstance(eq(flownodeInstanceId), anyString(), eq(ConnectorEvent.ON_FINISH));
        checkConnectorsPhases(connectorAndFlag, BEFORE_ON_FINISH, DURING_ON_FINISH);
    }

    @Test
    public void connectorsOnFinishAlreadyExecutedWithOnEnterConnAlreadyExecutedShouldReturnPhase_5_only() throws Exception {
        // given:
        haveConnectorAlreadyExecuted(ConnectorEvent.ON_ENTER);
        haveConnectorAlreadyExecuted(ConnectorEvent.ON_FINISH);

        // when:
        final BEntry<Integer, BEntry<SConnectorInstance, SConnectorDefinition>> connectorAndFlag = behaviors.getConnectorToExecuteAndFlag(processDefinition,
                flowNodeInstance, true, true);

        // then:
        verify(connectorInstanceService, times(1)).getNextExecutableConnectorInstance(eq(flownodeInstanceId), anyString(), eq(ConnectorEvent.ON_ENTER));
        verify(connectorInstanceService, times(1)).getNextExecutableConnectorInstance(eq(flownodeInstanceId), anyString(), eq(ConnectorEvent.ON_FINISH));
        checkConnectorsPhases(connectorAndFlag, AFTER_ON_FINISH);
    }

    @Test
    public void secondConnectorOnFinishShouldReturnPhase_4_only() throws Exception {
        // given:
        // no connectors on enter, only connectors on finish:
        final List<SConnectorDefinition> connectors = mock(List.class);
        when(flowNodeDefinition.getConnectors(ConnectorEvent.ON_FINISH)).thenReturn(connectors);
        when(connectors.size()).thenReturn(2);
        final SConnectorDefinition firstConnector = mock(SConnectorDefinition.class);
        final SConnectorDefinition secondConnector = mock(SConnectorDefinition.class);
        when(connectors.get(0)).thenReturn(firstConnector);
        final SConnectorInstance nextConnectorInstanceToExecute = mock(SConnectorInstance.class);
        when(connectorInstanceService.getNextExecutableConnectorInstance(eq(flownodeInstanceId), anyString(), eq(ConnectorEvent.ON_FINISH))).thenReturn(
                nextConnectorInstanceToExecute);
        final String connectorName = "connector_name";
        when(nextConnectorInstanceToExecute.getState()).thenReturn(TO_BE_EXECUTED.name());
        when(firstConnector.getName()).thenReturn("non_matching_name");
        when(secondConnector.getName()).thenReturn(connectorName);
        when(nextConnectorInstanceToExecute.getName()).thenReturn(connectorName);

        addConnectorIterator(connectors, firstConnector, secondConnector);

        // when:
        final BEntry<Integer, BEntry<SConnectorInstance, SConnectorDefinition>> connectorAndFlag = behaviors.getConnectorToExecuteAndFlag(processDefinition,
                flowNodeInstance, true, true);

        // then:
        verify(connectorInstanceService, times(0)).getNextExecutableConnectorInstance(eq(flownodeInstanceId), anyString(), eq(ConnectorEvent.ON_ENTER));
        verify(connectorInstanceService, times(1)).getNextExecutableConnectorInstance(eq(flownodeInstanceId), anyString(), eq(ConnectorEvent.ON_FINISH));
        checkConnectorsPhases(connectorAndFlag, DURING_ON_FINISH);
        assertThat(connectorAndFlag.getValue().getKey()).isEqualTo(nextConnectorInstanceToExecute);
        assertThat(connectorAndFlag.getValue().getValue()).isEqualTo(secondConnector);
    }

    protected Iterator<SConnectorDefinition> addConnectorIterator(final List<SConnectorDefinition> connectors,
            final SConnectorDefinition... connectorDefinitions) {
        final Iterator<SConnectorDefinition> connIterator = mock(Iterator.class);
        when(connectors.iterator()).thenReturn(connIterator);
        final Boolean[] next = new Boolean[connectorDefinitions.length];
        for (int i = 0; i < next.length - 1; i++) {
            next[i] = true;
        }
        next[next.length - 1] = false;
        when(connIterator.hasNext()).thenReturn(true, next);
        when(connIterator.next()).thenReturn(connectorDefinitions[0], Arrays.copyOfRange(connectorDefinitions, 1, connectorDefinitions.length));
        return connIterator;
    }

    @Test
    public void ignoredConnectorOnEnterWithOnEnterConnAndNoOnFinishConnExecutedShouldReturnPhases_1_3_5() throws Exception {
        // given:
        haveConnectorAlreadyExecuted(ConnectorEvent.ON_ENTER);

        // when:
        final BEntry<Integer, BEntry<SConnectorInstance, SConnectorDefinition>> connectorAndFlag = behaviors.getConnectorToExecuteAndFlag(processDefinition,
                flowNodeInstance, false, true);

        // then:
        checkConnectorsPhases(connectorAndFlag, BEFORE_ON_ENTER, BEFORE_ON_FINISH, AFTER_ON_FINISH);
    }

    @Test
    public void ignoredConnectorOnEnterWithOnEnterConnExecutedAndOnFinishConnNotExecutedShouldReturnPhases_1_3_4() throws Exception {
        // given:
        haveConnectorAlreadyExecuted(ConnectorEvent.ON_ENTER);
        haveConnectorsToBeExecuted(ConnectorEvent.ON_FINISH);

        // when:
        final BEntry<Integer, BEntry<SConnectorInstance, SConnectorDefinition>> connectorAndFlag = behaviors.getConnectorToExecuteAndFlag(processDefinition,
                flowNodeInstance, false, true);

        // then:
        verify(connectorInstanceService, times(1)).getNextExecutableConnectorInstance(eq(flownodeInstanceId), anyString(), eq(ConnectorEvent.ON_FINISH));
        checkConnectorsPhases(connectorAndFlag, BEFORE_ON_ENTER, BEFORE_ON_FINISH, DURING_ON_FINISH);
    }

    @Test
    public void ignoredConnectorOnFinishWithOnEnterConnExecutedAndOnFinishConnNotExecutedShouldReturnPhases_3_5() throws Exception {
        // given:
        haveConnectorAlreadyExecuted(ConnectorEvent.ON_ENTER);
        haveConnectorsToBeExecuted(ConnectorEvent.ON_FINISH);
        when(flowNodeInstance.isStateExecuting()).thenReturn(true);

        // when:
        final BEntry<Integer, BEntry<SConnectorInstance, SConnectorDefinition>> connectorAndFlag = behaviors.getConnectorToExecuteAndFlag(processDefinition,
                flowNodeInstance, true, false);

        // then:
        verify(connectorInstanceService, times(1)).getNextExecutableConnectorInstance(eq(flownodeInstanceId), anyString(), eq(ConnectorEvent.ON_ENTER));
        checkConnectorsPhases(connectorAndFlag, BEFORE_ON_FINISH, AFTER_ON_FINISH);
    }

    @Test
    public void ignoredConnectorOnFinishWithOnEnterConnExecutedAndOnFinishConnExecutedShouldReturnPhases_3_5() throws Exception {
        // given:
        haveConnectorAlreadyExecuted(ConnectorEvent.ON_ENTER);
        haveConnectorAlreadyExecuted(ConnectorEvent.ON_FINISH);
        when(flowNodeInstance.isStateExecuting()).thenReturn(true);

        // when:
        final BEntry<Integer, BEntry<SConnectorInstance, SConnectorDefinition>> connectorAndFlag = behaviors.getConnectorToExecuteAndFlag(processDefinition,
                flowNodeInstance, true, false);

        // then:
        verify(connectorInstanceService, times(1)).getNextExecutableConnectorInstance(eq(flownodeInstanceId), anyString(), eq(ConnectorEvent.ON_ENTER));
        checkConnectorsPhases(connectorAndFlag, BEFORE_ON_FINISH, AFTER_ON_FINISH);
    }

    protected void haveConnectorsToBeExecuted(final ConnectorEvent connectorEventPhase) throws SConnectorInstanceReadException {
        haveConnector(connectorEventPhase, true);
    }

    protected void haveConnectorAlreadyExecuted(final ConnectorEvent connectorEventPhase) throws SConnectorInstanceReadException {
        haveConnector(connectorEventPhase, false);
    }

    protected void haveConnector(final ConnectorEvent connectorEventPhase, final boolean haveConnectorToBeExecuted) throws SConnectorInstanceReadException {
        haveConnector(connectorEventPhase, haveConnectorToBeExecuted, TO_BE_EXECUTED);
    }

    protected void haveConnector(final ConnectorEvent connectorEventPhase, final boolean haveConnectorToBeExecuted, final ConnectorState connectorState)
            throws SConnectorInstanceReadException {
        final List<SConnectorDefinition> connectors = mock(List.class);
        when(flowNodeDefinition.getConnectors(connectorEventPhase)).thenReturn(connectors);
        when(connectors.size()).thenReturn(1);
        final SConnectorDefinition connectorDefinition = mock(SConnectorDefinition.class);
        when(connectors.get(0)).thenReturn(connectorDefinition);
        SConnectorInstance connectorInstance = null;
        final String connectorName = "connector_name";
        if (haveConnectorToBeExecuted) {
            connectorInstance = mock(SConnectorInstance.class);
            when(connectorInstance.getState()).thenReturn(connectorState.name());
            when(connectorInstance.getName()).thenReturn(connectorName);
        }
        when(connectorInstanceService.getNextExecutableConnectorInstance(eq(flownodeInstanceId), anyString(), eq(connectorEventPhase))).thenReturn(
                connectorInstance);
        when(connectorDefinition.getName()).thenReturn(connectorName);
    }
}
