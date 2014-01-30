package org.bonitasoft.engine.core.connector.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.bonitasoft.engine.core.process.instance.model.SConnectorInstanceWithFailureInfo;
import org.bonitasoft.engine.core.process.instance.model.builder.SConnectorInstanceWithFailureInfoBuilderFactory;
import org.bonitasoft.engine.persistence.ReadPersistenceService;
import org.bonitasoft.engine.recorder.Recorder;
import org.bonitasoft.engine.recorder.model.UpdateRecord;
import org.bonitasoft.engine.services.QueriableLoggerService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * @author Elias Ricken de Medeiros
 */
@RunWith(MockitoJUnitRunner.class)
public class ConnectorInstanceServiceImplTest {

    private static final String STACK_TRACE = "stackTrace";

    private static final String EXCEPTION_MESSAGE = "exceptionMessage";

    private final String message = "An exception occured during execution.";

    private final String causedByMessage = "This is the caused by message.";

    @Mock
    private ReadPersistenceService readPersitenceService;

    @Mock
    private Recorder recorder;

    @Mock
    private QueriableLoggerService queriableLoggerService;

    @Mock
    private SConnectorInstanceWithFailureInfo connectorInstanceWithFailureMock;

    @InjectMocks
    private ConnectorInstanceServiceImpl connectorInstanceServiceImpl;

    @Before
    public void setUp() {
        final SConnectorInstanceWithFailureInfoBuilderFactory fact = mock(SConnectorInstanceWithFailureInfoBuilderFactory.class);
        when(fact.getExceptionMessageKey()).thenReturn(EXCEPTION_MESSAGE);
        when(fact.getStackTraceKey()).thenReturn(STACK_TRACE);
    }

    @Test
    public void setConnectorInstanceFailureException() throws Exception {
        final Exception exception = new Exception(message);

        // call method
        connectorInstanceServiceImpl.setConnectorInstanceFailureException(connectorInstanceWithFailureMock, exception);

        // verify
        final ArgumentCaptor<UpdateRecord> updateRecordCaptor = ArgumentCaptor.forClass(UpdateRecord.class);
        verify(recorder, times(1)).recordUpdate(updateRecordCaptor.capture());
        final UpdateRecord updateRecord = updateRecordCaptor.getValue();
        final String stackTrace = (String) updateRecord.getFields().get(STACK_TRACE);

        assertEquals(message, updateRecord.getFields().get(EXCEPTION_MESSAGE));
        assertTrue(stackTrace.startsWith(Exception.class.getName() + ": " + message));
        assertTrue(stackTrace.contains(getClass().getName() + ".setConnectorInstanceFailureException"));
    }

    @Test
    public void setConnectorInstanceFailureExceptionWithNullMessage() throws Exception {
        final Exception exception = new Exception();

        // call method
        connectorInstanceServiceImpl.setConnectorInstanceFailureException(connectorInstanceWithFailureMock, exception);

        // verify
        final ArgumentCaptor<UpdateRecord> updateRecordCaptor = ArgumentCaptor.forClass(UpdateRecord.class);
        verify(recorder, times(1)).recordUpdate(updateRecordCaptor.capture());
        final UpdateRecord updateRecord = updateRecordCaptor.getValue();
        final String stackTrace = (String) updateRecord.getFields().get(STACK_TRACE);

        assertNull(updateRecord.getFields().get(EXCEPTION_MESSAGE));
        assertTrue(stackTrace.startsWith(Exception.class.getName()));
        assertTrue(stackTrace.contains(getClass().getName() + ".setConnectorInstanceFailureExceptionWithNullMessage"));
    }

    @Test
    public void setConnectorInstanceFailureExceptionWithCausedBy() throws Exception {
        final Exception causedByException = new Exception(causedByMessage);
        final Exception exception = new Exception(message, causedByException);

        // call method
        connectorInstanceServiceImpl.setConnectorInstanceFailureException(connectorInstanceWithFailureMock, exception);

        // verify
        final ArgumentCaptor<UpdateRecord> updateRecordCaptor = ArgumentCaptor.forClass(UpdateRecord.class);
        verify(recorder, times(1)).recordUpdate(updateRecordCaptor.capture());
        final UpdateRecord updateRecord = updateRecordCaptor.getValue();
        final String stackTrace = (String) updateRecord.getFields().get(STACK_TRACE);

        assertEquals(causedByMessage, updateRecord.getFields().get(EXCEPTION_MESSAGE));
        assertTrue(stackTrace.startsWith(Exception.class.getName() + ": " + message));
        assertTrue(stackTrace.contains(getClass().getName() + ".setConnectorInstanceFailureExceptionWithCausedBy"));
        assertTrue(stackTrace.contains("Caused by: " + Exception.class.getName() + ": " + causedByMessage));
    }

    @Test
    public void cleanConnectorInstanceFailureException() throws Exception {
        // call method
        connectorInstanceServiceImpl.setConnectorInstanceFailureException(connectorInstanceWithFailureMock, null);

        // verify
        final ArgumentCaptor<UpdateRecord> updateRecordCaptor = ArgumentCaptor.forClass(UpdateRecord.class);
        verify(recorder, times(1)).recordUpdate(updateRecordCaptor.capture());
        final UpdateRecord updateRecord = updateRecordCaptor.getValue();
        final String stackTrace = (String) updateRecord.getFields().get(STACK_TRACE);

        assertNull(updateRecord.getFields().get(EXCEPTION_MESSAGE));
        assertNull(stackTrace);
    }

    @Test
    public void setConnectorInstanceFailureExceptionMessageGreaterThen255() throws Exception {
        final String messageToRepeat = "This is a message repeated many times. ";
        final StringBuilder stb = new StringBuilder();
        int currentLength = 0;
        while (currentLength < 256) {
            stb.append(messageToRepeat);
            currentLength += messageToRepeat.length();
        }
        final String longMessage = stb.toString();

        final Exception exception = new Exception(longMessage);

        // call method
        connectorInstanceServiceImpl.setConnectorInstanceFailureException(connectorInstanceWithFailureMock, exception);

        // verify
        final ArgumentCaptor<UpdateRecord> updateRecordCaptor = ArgumentCaptor.forClass(UpdateRecord.class);
        verify(recorder, times(1)).recordUpdate(updateRecordCaptor.capture());
        final UpdateRecord updateRecord = updateRecordCaptor.getValue();
        final String stackTrace = (String) updateRecord.getFields().get(STACK_TRACE);

        final String retrievedMessage = (String) updateRecord.getFields().get(EXCEPTION_MESSAGE);
        assertEquals(longMessage.substring(0, 255), retrievedMessage);
        assertTrue(stackTrace.startsWith(Exception.class.getName() + ": " + longMessage));
        assertTrue(stackTrace.contains(getClass().getName() + ".setConnectorInstanceFailureExceptionMessageGreaterThen255"));
    }

}
