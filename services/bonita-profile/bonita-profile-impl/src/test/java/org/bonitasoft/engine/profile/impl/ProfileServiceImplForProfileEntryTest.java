package org.bonitasoft.engine.profile.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bonitasoft.engine.log.technical.TechnicalLoggerService;
import org.bonitasoft.engine.persistence.OrderByType;
import org.bonitasoft.engine.persistence.QueryOptions;
import org.bonitasoft.engine.persistence.ReadPersistenceService;
import org.bonitasoft.engine.persistence.SBonitaReadException;
import org.bonitasoft.engine.persistence.SBonitaSearchException;
import org.bonitasoft.engine.persistence.SelectByIdDescriptor;
import org.bonitasoft.engine.persistence.SelectListDescriptor;
import org.bonitasoft.engine.profile.builder.SProfileEntryUpdateBuilder;
import org.bonitasoft.engine.profile.builder.impl.SProfileEntryUpdateBuilderImpl;
import org.bonitasoft.engine.profile.exception.profileentry.SProfileEntryCreationException;
import org.bonitasoft.engine.profile.exception.profileentry.SProfileEntryDeletionException;
import org.bonitasoft.engine.profile.exception.profileentry.SProfileEntryNotFoundException;
import org.bonitasoft.engine.profile.exception.profileentry.SProfileEntryReadException;
import org.bonitasoft.engine.profile.exception.profileentry.SProfileEntryUpdateException;
import org.bonitasoft.engine.profile.model.SProfileEntry;
import org.bonitasoft.engine.queriablelogger.model.SQueriableLogSeverity;
import org.bonitasoft.engine.recorder.Recorder;
import org.bonitasoft.engine.recorder.SRecorderException;
import org.bonitasoft.engine.recorder.model.DeleteRecord;
import org.bonitasoft.engine.recorder.model.InsertRecord;
import org.bonitasoft.engine.recorder.model.UpdateRecord;
import org.bonitasoft.engine.services.QueriableLoggerService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * @author Celine Souchet
 */
@RunWith(MockitoJUnitRunner.class)
public class ProfileServiceImplForProfileEntryTest {

    @Mock
    private TechnicalLoggerService logger;

    @Mock
    private ReadPersistenceService persistenceService;

    @Mock
    private QueriableLoggerService queriableLoggerService;

    @Mock
    private Recorder recorder;

    @InjectMocks
    private ProfileServiceImpl profileServiceImpl;

    @Test
    public final void createProfileEntry() throws SRecorderException, SProfileEntryCreationException {
        final SProfileEntry sProfileEntry = mock(SProfileEntry.class);
        doReturn(1L).when(sProfileEntry).getId();

        doNothing().when(recorder).recordInsert(any(InsertRecord.class));
        doReturn(false).when(queriableLoggerService).isLoggable(anyString(), any(SQueriableLogSeverity.class));

        final SProfileEntry result = profileServiceImpl.createProfileEntry(sProfileEntry);
        assertNotNull(result);
        assertEquals(sProfileEntry, result);
    }

    @Test(expected = IllegalArgumentException.class)
    public final void createNullProfileEntry() throws Exception {
        profileServiceImpl.createProfileEntry(null);
    }

    @Test(expected = SProfileEntryCreationException.class)
    public final void createProfileThrowException() throws SRecorderException, SProfileEntryCreationException {
        final SProfileEntry sProfileEntry = mock(SProfileEntry.class);
        doReturn(1L).when(sProfileEntry).getId();

        doThrow(new SRecorderException("plop")).when(recorder).recordInsert(any(InsertRecord.class));
        doReturn(false).when(queriableLoggerService).isLoggable(anyString(), any(SQueriableLogSeverity.class));

        profileServiceImpl.createProfileEntry(sProfileEntry);
    }

    @Test
    public final void deleteProfileEntryById() throws SProfileEntryNotFoundException, SProfileEntryDeletionException, SBonitaReadException, SRecorderException {
        final SProfileEntry sProfileEntry = mock(SProfileEntry.class);
        doReturn(6L).when(sProfileEntry).getId();

        doReturn(sProfileEntry).when(persistenceService).selectById(Matchers.<SelectByIdDescriptor<SProfileEntry>> any());
        doNothing().when(recorder).recordDelete(any(DeleteRecord.class));
        doReturn(false).when(queriableLoggerService).isLoggable(anyString(), any(SQueriableLogSeverity.class));

        profileServiceImpl.deleteProfileEntry(1);
    }

    @Test(expected = SProfileEntryNotFoundException.class)
    public final void deleteNoProfileEntryById() throws SBonitaReadException, SProfileEntryNotFoundException, SProfileEntryDeletionException {
        when(persistenceService.selectById(Matchers.<SelectByIdDescriptor<SProfileEntry>> any())).thenReturn(null);

        profileServiceImpl.deleteProfileEntry(1);
    }

    @Test(expected = SProfileEntryDeletionException.class)
    public void deleteProfileEntryByIdThrowException() throws Exception {
        final SProfileEntry sProfileEntry = mock(SProfileEntry.class);
        doReturn(6L).when(sProfileEntry).getId();

        doReturn(sProfileEntry).when(persistenceService).selectById(Matchers.<SelectByIdDescriptor<SProfileEntry>> any());
        doThrow(new SRecorderException("")).when(recorder).recordDelete(any(DeleteRecord.class));

        profileServiceImpl.deleteProfileEntry(1);
    }

    @Test
    public final void deleteProfileEntryByObject() throws SRecorderException, SProfileEntryDeletionException {
        final SProfileEntry sProfileEntry = mock(SProfileEntry.class);
        doReturn(6L).when(sProfileEntry).getId();

        doNothing().when(recorder).recordDelete(any(DeleteRecord.class));
        doReturn(false).when(queriableLoggerService).isLoggable(anyString(), any(SQueriableLogSeverity.class));

        profileServiceImpl.deleteProfileEntry(sProfileEntry);
    }

    @Test(expected = IllegalArgumentException.class)
    public final void deleteNoProfileEntryByObject() throws SProfileEntryDeletionException {
        profileServiceImpl.deleteProfileEntry(null);
    }

    @Test(expected = SProfileEntryDeletionException.class)
    public void deleteProfileEntryByObjectThrowException() throws Exception {
        final SProfileEntry sProfileEntry = mock(SProfileEntry.class);
        doReturn(6L).when(sProfileEntry).getId();

        doThrow(new SRecorderException("")).when(recorder).recordDelete(any(DeleteRecord.class));

        profileServiceImpl.deleteProfileEntry(sProfileEntry);
    }

    @Test
    public final void getEntriesOfProfileByParentId() throws SBonitaReadException, SProfileEntryReadException {
        final List<SProfileEntry> sProfileEntries = new ArrayList<SProfileEntry>();
        final SProfileEntry sProfileEntry = mock(SProfileEntry.class);
        sProfileEntries.add(sProfileEntry);

        doReturn(sProfileEntries).when(persistenceService).selectList(Matchers.<SelectListDescriptor<SProfileEntry>> any());

        assertEquals(sProfileEntries, profileServiceImpl.getEntriesOfProfileByParentId(1, 0, 0, 0, null, OrderByType.ASC));
    }

    @Test
    public final void getNoEntriesOfProfileByParentId() throws SBonitaReadException, SProfileEntryReadException {
        final List<SProfileEntry> sProfileEntries = new ArrayList<SProfileEntry>();

        doReturn(sProfileEntries).when(persistenceService).selectList(Matchers.<SelectListDescriptor<SProfileEntry>> any());

        assertEquals(sProfileEntries, profileServiceImpl.getEntriesOfProfileByParentId(1, 0, 0, 0, null, OrderByType.ASC));
    }

    @Test(expected = SProfileEntryReadException.class)
    public final void getEntriesOfProfileByParentIdThrowException() throws SBonitaReadException, SProfileEntryReadException {
        doThrow(new SBonitaReadException("plop")).when(persistenceService).selectList(Matchers.<SelectListDescriptor<SProfileEntry>> any());

        profileServiceImpl.getEntriesOfProfileByParentId(1, 0, 0, 0, null, null);
    }

    @Test
    public final void getEntriesOfProfile() throws SProfileEntryReadException, SBonitaReadException {
        final List<SProfileEntry> sProfileEntries = new ArrayList<SProfileEntry>();
        final SProfileEntry sProfileEntry = mock(SProfileEntry.class);
        sProfileEntries.add(sProfileEntry);

        doReturn(sProfileEntries).when(persistenceService).selectList(Matchers.<SelectListDescriptor<SProfileEntry>> any());

        assertEquals(sProfileEntries, profileServiceImpl.getEntriesOfProfile(1, 0, 0, null, OrderByType.ASC));
    }

    @Test
    public final void getNoEntriesOfProfile() throws SBonitaReadException, SProfileEntryReadException {
        final List<SProfileEntry> sProfileEntries = new ArrayList<SProfileEntry>();

        doReturn(sProfileEntries).when(persistenceService).selectList(Matchers.<SelectListDescriptor<SProfileEntry>> any());

        assertEquals(sProfileEntries, profileServiceImpl.getEntriesOfProfile(1, 0, 0, null, OrderByType.ASC));
    }

    @Test(expected = SProfileEntryReadException.class)
    public final void getEntriesOfProfileThrowException() throws SBonitaReadException, SProfileEntryReadException {
        doThrow(new SBonitaReadException("plop")).when(persistenceService).selectList(Matchers.<SelectListDescriptor<SProfileEntry>> any());

        profileServiceImpl.getEntriesOfProfile(1, 0, 0, null, null);
    }

    @Test
    public final void getNumberOfProfileEntries() throws SBonitaSearchException, SBonitaReadException {
        final QueryOptions options = new QueryOptions(0, 10);
        when(persistenceService.getNumberOfEntities(SProfileEntry.class, options, Collections.<String, Object> emptyMap())).thenReturn(1L);

        assertEquals(1L, profileServiceImpl.getNumberOfProfileEntries(options));
    }

    @Test(expected = SBonitaSearchException.class)
    public void getNumberOfProfilesThrowException() throws Exception {
        final QueryOptions options = new QueryOptions(0, 10);
        when(persistenceService.getNumberOfEntities(SProfileEntry.class, options, Collections.<String, Object> emptyMap())).thenThrow(
                new SBonitaReadException(""));

        profileServiceImpl.getNumberOfProfileEntries(options);
    }

    @Test
    public final void getProfileEntryById() throws SProfileEntryNotFoundException, SBonitaReadException {
        final SProfileEntry sProfileEntry = mock(SProfileEntry.class);

        doReturn(sProfileEntry).when(persistenceService).selectById(Matchers.<SelectByIdDescriptor<SProfileEntry>> any());

        assertEquals(sProfileEntry, profileServiceImpl.getProfileEntry(1));
    }

    @Test(expected = SProfileEntryNotFoundException.class)
    public void getNoProfileEntryById() throws Exception {
        when(persistenceService.selectById(Matchers.<SelectByIdDescriptor<SProfileEntry>> any())).thenReturn(null);

        profileServiceImpl.getProfileEntry(1);
    }

    @Test(expected = SProfileEntryNotFoundException.class)
    public void getProfileEntryByIdThrowException() throws Exception {
        when(persistenceService.selectById(Matchers.<SelectByIdDescriptor<SProfileEntry>> any())).thenThrow(new SBonitaReadException(""));

        profileServiceImpl.getProfileEntry(1);
    }

    @Test
    public final void searchProfileEntries() throws SBonitaSearchException, SBonitaReadException {
        final QueryOptions options = new QueryOptions(0, 10);
        when(persistenceService.searchEntity(SProfileEntry.class, options, Collections.<String, Object> emptyMap())).thenReturn(new ArrayList<SProfileEntry>());

        assertNotNull(profileServiceImpl.searchProfileEntries(options));
    }

    @Test(expected = SBonitaSearchException.class)
    public void searchProfileEntriesThrowException() throws Exception {
        final QueryOptions options = new QueryOptions(0, 10);
        when(persistenceService.searchEntity(SProfileEntry.class, options, Collections.<String, Object> emptyMap())).thenThrow(new SBonitaReadException(""));

        profileServiceImpl.searchProfileEntries(options);
    }

    @Test
    public final void updateProfileEntry() throws SProfileEntryUpdateException {
        final SProfileEntry sProfileEntry = mock(SProfileEntry.class);
        doReturn(3L).when(sProfileEntry).getId();
        final SProfileEntryUpdateBuilder sProfileEntryUpdateBuilder = new SProfileEntryUpdateBuilderImpl();
        sProfileEntryUpdateBuilder.setDescription("description").setName("newName").setIndex(6).setPage("page").setParentId(5858).setProfileId(9)
                .setType("type");

        doReturn(false).when(queriableLoggerService).isLoggable(anyString(), any(SQueriableLogSeverity.class));

        final SProfileEntry result = profileServiceImpl.updateProfileEntry(sProfileEntry, sProfileEntryUpdateBuilder.done());
        assertNotNull(result);
        assertEquals(sProfileEntry, result);
    }

    @Test(expected = IllegalArgumentException.class)
    public final void updateNullProfileEntry() throws SProfileEntryUpdateException {
        final SProfileEntryUpdateBuilder sProfileEntryUpdateBuilder = new SProfileEntryUpdateBuilderImpl();

        profileServiceImpl.updateProfileEntry(null, sProfileEntryUpdateBuilder.done());
    }

    @Test(expected = IllegalArgumentException.class)
    public final void updateProfileWithNullDescriptor() throws SProfileEntryUpdateException {
        final SProfileEntry sProfileEntry = mock(SProfileEntry.class);
        doReturn(3L).when(sProfileEntry).getId();

        profileServiceImpl.updateProfileEntry(sProfileEntry, null);
    }

    @Test(expected = SProfileEntryUpdateException.class)
    public final void updateProfileEntryThrowException() throws SRecorderException, SProfileEntryUpdateException {
        final SProfileEntry sProfileEntry = mock(SProfileEntry.class);
        doReturn(3L).when(sProfileEntry).getId();
        final SProfileEntryUpdateBuilder sProfileEntryUpdateBuilder = new SProfileEntryUpdateBuilderImpl();
        sProfileEntryUpdateBuilder.setDescription("newDescription").setName("newName");

        doThrow(new SRecorderException("plop")).when(recorder).recordUpdate(any(UpdateRecord.class));

        profileServiceImpl.updateProfileEntry(sProfileEntry, sProfileEntryUpdateBuilder.done());
    }

}
