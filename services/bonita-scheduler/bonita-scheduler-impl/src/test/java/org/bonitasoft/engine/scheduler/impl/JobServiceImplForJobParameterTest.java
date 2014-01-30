package org.bonitasoft.engine.scheduler.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.bonitasoft.engine.persistence.QueryOptions;
import org.bonitasoft.engine.persistence.ReadPersistenceService;
import org.bonitasoft.engine.persistence.SBonitaReadException;
import org.bonitasoft.engine.persistence.SBonitaSearchException;
import org.bonitasoft.engine.persistence.SelectByIdDescriptor;
import org.bonitasoft.engine.queriablelogger.model.SQueriableLogSeverity;
import org.bonitasoft.engine.recorder.Recorder;
import org.bonitasoft.engine.recorder.SRecorderException;
import org.bonitasoft.engine.recorder.model.DeleteRecord;
import org.bonitasoft.engine.recorder.model.InsertRecord;
import org.bonitasoft.engine.scheduler.exception.jobParameter.SJobParameterCreationException;
import org.bonitasoft.engine.scheduler.exception.jobParameter.SJobParameterDeletionException;
import org.bonitasoft.engine.scheduler.exception.jobParameter.SJobParameterNotFoundException;
import org.bonitasoft.engine.scheduler.exception.jobParameter.SJobParameterReadException;
import org.bonitasoft.engine.scheduler.model.SJobParameter;
import org.bonitasoft.engine.scheduler.recorder.SelectDescriptorBuilder;
import org.bonitasoft.engine.services.QueriableLoggerService;
import org.junit.Assert;
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
public class JobServiceImplForJobParameterTest {

    @Mock
    private QueriableLoggerService queriableLoggerService;

    @Mock
    private ReadPersistenceService readPersistenceService;

    @Mock
    private Recorder recorder;

    @InjectMocks
    private JobServiceImpl jobServiceImpl;

    @Test
    public final void createJobParameters() throws SJobParameterCreationException, SRecorderException {
        final long tenantId = 2;
        final long jobDescriptorId = 9;
        final SJobParameter sJobParameter = mock(SJobParameter.class);
        doReturn(1L).when(sJobParameter).getId();

        doNothing().when(recorder).recordInsert(any(InsertRecord.class));
        doReturn(false).when(queriableLoggerService).isLoggable(anyString(), any(SQueriableLogSeverity.class));

        final List<SJobParameter> result = jobServiceImpl.createJobParameters(Collections.singletonList(sJobParameter), tenantId, jobDescriptorId);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(sJobParameter, result.get(0));
    }

    @Test
    public final void createNullJobParameters() throws Exception {
        final long tenantId = 2;
        final long jobDescriptorId = 9;

        final List<SJobParameter> result = jobServiceImpl.createJobParameters(null, tenantId, jobDescriptorId);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public final void createJobParametersWithEmptyList() throws Exception {
        final long tenantId = 2;
        final long jobDescriptorId = 9;

        final List<SJobParameter> result = jobServiceImpl.createJobParameters(Collections.<SJobParameter> emptyList(), tenantId, jobDescriptorId);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test(expected = SJobParameterCreationException.class)
    public final void createJobParametersThrowException() throws SJobParameterCreationException, SRecorderException {
        final long tenantId = 2;
        final long jobDescriptorId = 9;
        final SJobParameter sJobParameter = mock(SJobParameter.class);
        doReturn(1L).when(sJobParameter).getId();

        doThrow(new SRecorderException("plop")).when(recorder).recordInsert(any(InsertRecord.class));
        doReturn(false).when(queriableLoggerService).isLoggable(anyString(), any(SQueriableLogSeverity.class));

        jobServiceImpl.createJobParameters(Collections.singletonList(sJobParameter), tenantId, jobDescriptorId);
    }

    @Test
    public final void createJobParameter() throws SJobParameterCreationException, SRecorderException {
        final long tenantId = 2;
        final long jobDescriptorId = 9;
        final SJobParameter sJobParameter = mock(SJobParameter.class);
        doReturn(1L).when(sJobParameter).getId();

        doNothing().when(recorder).recordInsert(any(InsertRecord.class));
        doReturn(false).when(queriableLoggerService).isLoggable(anyString(), any(SQueriableLogSeverity.class));

        final SJobParameter result = jobServiceImpl.createJobParameter(sJobParameter, tenantId, jobDescriptorId);
        assertNotNull(result);
        assertEquals(sJobParameter, result);
    }

    @Test(expected = IllegalArgumentException.class)
    public final void createNullJobParameter() throws Exception {
        final long tenantId = 2;
        final long jobDescriptorId = 9;

        jobServiceImpl.createJobParameter(null, tenantId, jobDescriptorId);
    }

    @Test(expected = SJobParameterCreationException.class)
    public final void createJobParameterThrowException() throws SJobParameterCreationException, SRecorderException {
        final long tenantId = 2;
        final long jobDescriptorId = 9;
        final SJobParameter sJobParameter = mock(SJobParameter.class);
        doReturn(1L).when(sJobParameter).getId();

        doThrow(new SRecorderException("plop")).when(recorder).recordInsert(any(InsertRecord.class));
        doReturn(false).when(queriableLoggerService).isLoggable(anyString(), any(SQueriableLogSeverity.class));

        jobServiceImpl.createJobParameter(sJobParameter, tenantId, jobDescriptorId);
    }

    @Test
    public final void deleteJobParameterById() throws SBonitaReadException, SRecorderException, SJobParameterNotFoundException, SJobParameterReadException,
            SJobParameterDeletionException {
        final SJobParameter sJobParameter = mock(SJobParameter.class);
        doReturn(3L).when(sJobParameter).getId();

        doReturn(sJobParameter).when(readPersistenceService).selectById(Matchers.<SelectByIdDescriptor<SJobParameter>> any());
        doNothing().when(recorder).recordDelete(any(DeleteRecord.class));
        doReturn(false).when(queriableLoggerService).isLoggable(anyString(), any(SQueriableLogSeverity.class));

        jobServiceImpl.deleteJobParameter(3);
    }

    @Test(expected = SJobParameterNotFoundException.class)
    public final void deleteNotExistingJobParameterById() throws SBonitaReadException, SJobParameterDeletionException, SJobParameterNotFoundException,
            SJobParameterReadException {
        when(readPersistenceService.selectById(Matchers.<SelectByIdDescriptor<SJobParameter>> any())).thenReturn(null);

        jobServiceImpl.deleteJobParameter(1);
    }

    @Test(expected = SJobParameterDeletionException.class)
    public void deleteJobParameterByIdThrowException() throws Exception {
        final SJobParameter sJobParameter = mock(SJobParameter.class);
        doReturn(3L).when(sJobParameter).getId();

        doReturn(sJobParameter).when(readPersistenceService).selectById(Matchers.<SelectByIdDescriptor<SJobParameter>> any());
        doThrow(new SRecorderException("")).when(recorder).recordDelete(any(DeleteRecord.class));

        jobServiceImpl.deleteJobParameter(3);
    }

    @Test
    public final void deleteJobParameterByObject() throws SRecorderException, SJobParameterDeletionException {
        final SJobParameter sJobParameter = mock(SJobParameter.class);
        doReturn(3L).when(sJobParameter).getId();

        doNothing().when(recorder).recordDelete(any(DeleteRecord.class));
        doReturn(false).when(queriableLoggerService).isLoggable(anyString(), any(SQueriableLogSeverity.class));

        jobServiceImpl.deleteJobParameter(sJobParameter);
    }

    @Test(expected = IllegalArgumentException.class)
    public final void deleteNullJobParameterByObject() throws SJobParameterDeletionException {
        jobServiceImpl.deleteJobParameter(null);
    }

    @Test(expected = SJobParameterDeletionException.class)
    public void deleteJobParameterByObjectThrowException() throws Exception {
        final SJobParameter sJobParameter = mock(SJobParameter.class);
        doReturn(3L).when(sJobParameter).getId();

        doThrow(new SRecorderException("")).when(recorder).recordDelete(any(DeleteRecord.class));

        jobServiceImpl.deleteJobParameter(sJobParameter);
    }

    @Test
    public void getJobParameterById() throws SJobParameterNotFoundException, SJobParameterReadException, SBonitaReadException {
        final long jobParameterId = 1;
        final SJobParameter sJobParameter = mock(SJobParameter.class);
        when(readPersistenceService.selectById(SelectDescriptorBuilder.getElementById(SJobParameter.class, "SJobParameter", jobParameterId))).thenReturn(
                sJobParameter);

        Assert.assertEquals(sJobParameter, jobServiceImpl.getJobParameter(jobParameterId));
    }

    @Test(expected = SJobParameterNotFoundException.class)
    public void getJobParameterByIdNotExist() throws SBonitaReadException, SJobParameterNotFoundException, SJobParameterReadException {
        final long jobParameterId = 455;
        doReturn(null).when(readPersistenceService).selectById(SelectDescriptorBuilder.getElementById(SJobParameter.class, "SJobParameter", jobParameterId));

        jobServiceImpl.getJobParameter(jobParameterId);
    }

    @Test(expected = SJobParameterReadException.class)
    public void getJobParameterByIdThrowException() throws SJobParameterNotFoundException, SJobParameterReadException, SBonitaReadException {
        final long jobParameterId = 1;
        doThrow(new SBonitaReadException("")).when(readPersistenceService).selectById(
                SelectDescriptorBuilder.getElementById(SJobParameter.class, "SJobParameter", jobParameterId));

        jobServiceImpl.getJobParameter(jobParameterId);
    }

    @Test
    public void searchJobParameters() throws SBonitaSearchException, SBonitaReadException {
        final QueryOptions options = new QueryOptions(0, 10);
        final SJobParameter sJobParameter = mock(SJobParameter.class);
        when(readPersistenceService.searchEntity(SJobParameter.class, options, null)).thenReturn(Collections.singletonList(sJobParameter));

        assertEquals(sJobParameter, jobServiceImpl.searchJobParameters(options).get(0));
    }

    @Test(expected = SBonitaSearchException.class)
    public void searchJobParametersThrowException() throws SBonitaSearchException, SBonitaReadException {
        final QueryOptions options = new QueryOptions(0, 10);
        doThrow(new SBonitaReadException("")).when(readPersistenceService).searchEntity(SJobParameter.class, options, null);

        jobServiceImpl.searchJobParameters(options);
    }

    @Test
    public final void setJobParameters() throws SJobParameterCreationException, SBonitaSearchException, SBonitaReadException, SRecorderException {
        final long tenantId = 12;
        final long jobDescriptorId = 8;
        final SJobParameter sJobParameter = mock(SJobParameter.class);

        doReturn(Collections.singletonList(sJobParameter)).when(readPersistenceService).searchEntity(eq(SJobParameter.class), any(QueryOptions.class),
                eq((Map<String, Object>) null));
        doNothing().when(recorder).recordDelete(any(DeleteRecord.class));
        doNothing().when(recorder).recordInsert(any(InsertRecord.class));
        doReturn(false).when(queriableLoggerService).isLoggable(anyString(), any(SQueriableLogSeverity.class));

        final List<SJobParameter> result = jobServiceImpl.setJobParameters(tenantId, jobDescriptorId, Collections.singletonList(sJobParameter));
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(sJobParameter, result.get(0));
    }

    @Test
    public void setJobParametersWithEmptyList() throws SJobParameterCreationException, SBonitaSearchException, SBonitaReadException {
        final long tenantId = 12;
        final long jobDescriptorId = 8;

        doReturn(Collections.EMPTY_LIST).when(readPersistenceService).searchEntity(eq(SJobParameter.class), any(QueryOptions.class),
                eq((Map<String, Object>) null));
        final List<SJobParameter> result = jobServiceImpl.setJobParameters(tenantId, jobDescriptorId, Collections.<SJobParameter> emptyList());
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void setJobParametersWithNullList() throws SJobParameterCreationException, SBonitaSearchException, SBonitaReadException {
        final long tenantId = 12;
        final long jobDescriptorId = 8;

        doReturn(Collections.EMPTY_LIST).when(readPersistenceService).searchEntity(eq(SJobParameter.class), any(QueryOptions.class),
                eq((Map<String, Object>) null));

        final List<SJobParameter> result = jobServiceImpl.setJobParameters(tenantId, jobDescriptorId, Collections.<SJobParameter> emptyList());
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test(expected = SJobParameterCreationException.class)
    public void setJobParametersFailedOnSearch() throws SBonitaSearchException, SBonitaReadException, SJobParameterCreationException {
        final long tenantId = 12;
        final long jobDescriptorId = 8;
        // Build job parameter
        final SJobParameter sJobParameter = mock(SJobParameter.class);

        doThrow(new SBonitaReadException("")).when(readPersistenceService).searchEntity(eq(SJobParameter.class), any(QueryOptions.class),
                eq((Map<String, Object>) null));

        jobServiceImpl.setJobParameters(tenantId, jobDescriptorId, Collections.singletonList(sJobParameter));
    }

    @Test(expected = SJobParameterCreationException.class)
    public void setJobParametersFailedOnDeletion() throws Exception {
        final long tenantId = 12;
        final long jobDescriptorId = 8;
        final SJobParameter sJobParameter = mock(SJobParameter.class);

        doReturn(Collections.singletonList(sJobParameter)).when(readPersistenceService).searchEntity(eq(SJobParameter.class), any(QueryOptions.class),
                eq((Map<String, Object>) null));
        doThrow(new SRecorderException("")).when(recorder).recordDelete(any(DeleteRecord.class));
        doReturn(false).when(queriableLoggerService).isLoggable(anyString(), any(SQueriableLogSeverity.class));

        jobServiceImpl.setJobParameters(tenantId, jobDescriptorId, Collections.singletonList(sJobParameter));
    }

    @Test(expected = SJobParameterCreationException.class)
    public final void setJobParametersFailedOnCreation() throws SJobParameterCreationException, SRecorderException, SBonitaSearchException,
            SBonitaReadException {
        final long tenantId = 12;
        final long jobDescriptorId = 8;
        final SJobParameter sJobParameter = mock(SJobParameter.class);

        doReturn(Collections.singletonList(sJobParameter)).when(readPersistenceService).searchEntity(eq(SJobParameter.class), any(QueryOptions.class),
                eq((Map<String, Object>) null));
        doNothing().when(recorder).recordDelete(any(DeleteRecord.class));
        doThrow(new SRecorderException("plop")).when(recorder).recordInsert(any(InsertRecord.class));
        doReturn(false).when(queriableLoggerService).isLoggable(anyString(), any(SQueriableLogSeverity.class));

        jobServiceImpl.setJobParameters(tenantId, jobDescriptorId, Collections.singletonList(sJobParameter));
    }

}
