package org.bonitasoft.engine.scheduler.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.bonitasoft.engine.events.EventService;
import org.bonitasoft.engine.persistence.ReadPersistenceService;
import org.bonitasoft.engine.persistence.SBonitaReadException;
import org.bonitasoft.engine.persistence.SelectListDescriptor;
import org.bonitasoft.engine.recorder.Recorder;
import org.bonitasoft.engine.scheduler.exception.failedJob.SFailedJobReadException;
import org.bonitasoft.engine.scheduler.model.SFailedJob;
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
public class JobServiceImplForFailedJobTest {

    @Mock
    private EventService eventService;

    @Mock
    private QueriableLoggerService queriableLoggerService;

    @Mock
    private ReadPersistenceService readPersistenceService;

    @Mock
    private Recorder recorder;

    @InjectMocks
    private JobServiceImpl jobServiceImpl;

    @Test
    public final void getFailedJobs() throws SBonitaReadException, SFailedJobReadException {
        final SFailedJob sFailedJob = mock(SFailedJob.class);
        when(readPersistenceService.selectList(Matchers.<SelectListDescriptor<SFailedJob>> any())).thenReturn(Collections.singletonList(sFailedJob));

        assertEquals(sFailedJob, jobServiceImpl.getFailedJobs(0, 10).get(0));
    }

    @Test(expected = SFailedJobReadException.class)
    public void getFailedJobsThrowException() throws SBonitaReadException, SFailedJobReadException {
        doThrow(new SBonitaReadException("")).when(readPersistenceService).selectList(Matchers.<SelectListDescriptor<SFailedJob>> any());

        jobServiceImpl.getFailedJobs(0, 10);
    }

}
