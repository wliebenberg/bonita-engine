package org.bonitasoft.engine.recorder.impl;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.bonitasoft.engine.commons.exceptions.SBonitaException;
import org.bonitasoft.engine.events.EventActionType;
import org.bonitasoft.engine.events.EventService;
import org.bonitasoft.engine.events.model.FireEventException;
import org.bonitasoft.engine.events.model.SEvent;
import org.bonitasoft.engine.log.technical.TechnicalLoggerService;
import org.bonitasoft.engine.recorder.SRecorderException;
import org.bonitasoft.engine.recorder.model.DeleteRecord;
import org.bonitasoft.engine.recorder.model.InsertRecord;
import org.bonitasoft.engine.recorder.model.UpdateRecord;
import org.bonitasoft.engine.services.PersistenceService;
import org.bonitasoft.engine.services.SPersistenceException;
import org.bonitasoft.engine.services.UpdateDescriptor;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class RecorderImplTest {

    private static final String AUTHOR = "AUTHOR";

    @Mock
    private PersistenceService persistenceService;

    @Mock
    private TechnicalLoggerService logger;

    @Mock
    private EventService eventService;

    @InjectMocks
    private RecorderImpl recorder;

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void recordInsertAnEntityAndFireAnEvent() throws SBonitaException {
        final Author matti = new Author();
        matti.setName("Matti");
        final InsertRecord record = new InsertRecord(matti, AUTHOR);
        when(eventService.hasHandlers(AUTHOR, EventActionType.CREATED)).thenReturn(true);

        recorder.recordInsert(record);

        verify(persistenceService).insert(matti);
        verify(eventService).hasHandlers(AUTHOR, EventActionType.CREATED);
        verify(eventService).fireEvent(any(SEvent.class));
    }

    @Test
    public void recordInsertJustAnEntity() throws SBonitaException {
        final Author matti = new Author();
        matti.setName("Matti");
        final InsertRecord record = new InsertRecord(matti, AUTHOR);
        when(eventService.hasHandlers(AUTHOR, EventActionType.CREATED)).thenReturn(false);

        recorder.recordInsert(record);

        verify(persistenceService).insert(matti);
        verify(eventService).hasHandlers(AUTHOR, EventActionType.CREATED);
        verify(eventService, never()).fireEvent(any(SEvent.class));
    }

    @Test(expected = SRecorderException.class)
    public void recordInsertAnEntityButFailsDueToExceptionOfPersistence() throws SBonitaException {
        final Author matti = new Author();
        matti.setName("Matti");
        final InsertRecord record = new InsertRecord(matti, AUTHOR);

        doThrow(new SPersistenceException("ouch")).when(persistenceService).insert(matti);

        recorder.recordInsert(record);
    }

    @Test(expected = SRecorderException.class)
    public void recordInsertAnEntityButFailsDueToExceptionOfEventService() throws SBonitaException {
        final Author matti = new Author();
        matti.setName("Matti");
        final InsertRecord record = new InsertRecord(matti, AUTHOR);
        when(eventService.hasHandlers(AUTHOR, EventActionType.CREATED)).thenReturn(true);
        doThrow(new FireEventException("ouch")).when(eventService).fireEvent(any(SEvent.class));

        recorder.recordInsert(record);
    }

    @Test
    public void recordUpdateAnEntityAndFireAnEvent() throws SBonitaException {
        final Author matti = new Author();
        matti.setName("Matti");
        final Map<String, Object> fields = new HashMap<String, Object>();
        fields.put("name", "Jaani");
        final UpdateRecord record = UpdateRecord.buildSetFields(matti, AUTHOR, fields);
        when(eventService.hasHandlers(AUTHOR, EventActionType.UPDATED)).thenReturn(true);

        recorder.recordUpdate(record);

        verify(persistenceService).update(any(UpdateDescriptor.class));
        verify(eventService).hasHandlers(AUTHOR, EventActionType.UPDATED);
        verify(eventService).fireEvent(any(SEvent.class));
    }

    @Test
    public void recordUpdateJustAnEntity() throws SBonitaException {
        final Author matti = new Author();
        matti.setName("Matti");
        final Map<String, Object> fields = new HashMap<String, Object>();
        fields.put("name", "Jaani");
        final UpdateRecord record = UpdateRecord.buildSetFields(matti, AUTHOR, fields);
        when(eventService.hasHandlers(AUTHOR, EventActionType.UPDATED)).thenReturn(false);

        recorder.recordUpdate(record);

        verify(persistenceService).update(any(UpdateDescriptor.class));
        verify(eventService).hasHandlers(AUTHOR, EventActionType.UPDATED);
        verify(eventService, never()).fireEvent(any(SEvent.class));
    }

    @Test(expected = SRecorderException.class)
    public void recordUpdateAnEntityButFailsDueToExceptionOfPersistence() throws SBonitaException {
        final Author matti = new Author();
        matti.setName("Matti");
        final Map<String, Object> fields = new HashMap<String, Object>();
        fields.put("name", "Jaani");
        final UpdateRecord record = UpdateRecord.buildSetFields(matti, AUTHOR, fields);

        doThrow(new SPersistenceException("ouch")).when(persistenceService).update(any(UpdateDescriptor.class));

        recorder.recordUpdate(record);
    }

    @Test(expected = SRecorderException.class)
    public void recordUpdateAnEntityButFailsDueToExceptionOfEventService() throws SBonitaException {
        final Author matti = new Author();
        matti.setName("Matti");
        final Map<String, Object> fields = new HashMap<String, Object>();
        fields.put("name", "Jaani");
        final UpdateRecord record = UpdateRecord.buildSetFields(matti, AUTHOR, fields);
        when(eventService.hasHandlers(AUTHOR, EventActionType.UPDATED)).thenReturn(true);
        doThrow(new FireEventException("ouch")).when(eventService).fireEvent(any(SEvent.class));

        recorder.recordUpdate(record);
    }

    @Test
    public void recordDeleteAnEntityAndFireAnEvent() throws SBonitaException {
        final Author matti = new Author();
        matti.setName("Matti");
        final DeleteRecord record = new DeleteRecord(matti, AUTHOR);
        when(eventService.hasHandlers(AUTHOR, EventActionType.DELETED)).thenReturn(true);

        recorder.recordDelete(record);

        verify(persistenceService).delete(matti);
        verify(eventService).hasHandlers(AUTHOR, EventActionType.DELETED);
        verify(eventService).fireEvent(any(SEvent.class));
    }

    @Test
    public void recordDeleteJustAnEntity() throws SBonitaException {
        final Author matti = new Author();
        matti.setName("Matti");
        final DeleteRecord record = new DeleteRecord(matti, AUTHOR);
        when(eventService.hasHandlers(AUTHOR, EventActionType.DELETED)).thenReturn(false);

        recorder.recordDelete(record);

        verify(persistenceService).delete(matti);
        verify(eventService).hasHandlers(AUTHOR, EventActionType.DELETED);
        verify(eventService, never()).fireEvent(any(SEvent.class));
    }

    @Test(expected = SRecorderException.class)
    public void recordDeleteAnEntityButFailsDueToExceptionOfPersistence() throws SBonitaException {
        final Author matti = new Author();
        matti.setName("Matti");
        final DeleteRecord record = new DeleteRecord(matti, AUTHOR);
        doThrow(new SPersistenceException("ouch")).when(persistenceService).delete(matti);

        recorder.recordDelete(record);
    }

    @Test(expected = SRecorderException.class)
    public void recordDeleteAnEntityButFailsDueToExceptionOfEventService() throws SBonitaException {
        final Author matti = new Author();
        matti.setName("Matti");
        final DeleteRecord record = new DeleteRecord(matti, AUTHOR);
        when(eventService.hasHandlers(AUTHOR, EventActionType.DELETED)).thenReturn(true);
        doThrow(new FireEventException("ouch")).when(eventService).fireEvent(any(SEvent.class));

        recorder.recordDelete(record);
    }

}
