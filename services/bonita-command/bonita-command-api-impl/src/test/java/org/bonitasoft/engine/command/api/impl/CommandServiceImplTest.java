package org.bonitasoft.engine.command.api.impl;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.bonitasoft.engine.command.SCommandGettingException;
import org.bonitasoft.engine.command.SCommandNotFoundException;
import org.bonitasoft.engine.command.model.SCommand;
import org.bonitasoft.engine.command.model.SCommandCriterion;
import org.bonitasoft.engine.log.technical.TechnicalLoggerService;
import org.bonitasoft.engine.persistence.QueryOptions;
import org.bonitasoft.engine.persistence.ReadPersistenceService;
import org.bonitasoft.engine.persistence.SBonitaReadException;
import org.bonitasoft.engine.persistence.SBonitaSearchException;
import org.bonitasoft.engine.persistence.SelectByIdDescriptor;
import org.bonitasoft.engine.persistence.SelectListDescriptor;
import org.bonitasoft.engine.persistence.SelectOneDescriptor;
import org.bonitasoft.engine.recorder.Recorder;
import org.bonitasoft.engine.services.QueriableLoggerService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Celine Souchet
 */
public class CommandServiceImplTest {

    private Recorder recorder;

    private ReadPersistenceService persistence;

    private TechnicalLoggerService logger;

    private QueriableLoggerService queriableLoggerService;

    private CommandServiceImpl commandServiceImpl;

    @Before
    public final void setUp() throws Exception {
        recorder = mock(Recorder.class);
        persistence = mock(ReadPersistenceService.class);
        logger = mock(TechnicalLoggerService.class);
        queriableLoggerService = mock(QueriableLoggerService.class);
        commandServiceImpl = new CommandServiceImpl(persistence, recorder, logger, queriableLoggerService);
    }

    @Test
    public final void getAllCommands() throws SCommandGettingException, SBonitaReadException {
        final List<SCommand> sCommands = new ArrayList<SCommand>();
        when(persistence.selectList(any(SelectListDescriptor.class))).thenReturn(sCommands);

        Assert.assertEquals(sCommands, commandServiceImpl.getAllCommands(0, 1, SCommandCriterion.NAME_ASC));
    }

    @Test(expected = SCommandGettingException.class)
    public final void getAllCommandsThrowException() throws SCommandGettingException, SBonitaReadException {
        when(persistence.selectList(any(SelectListDescriptor.class))).thenThrow(new SBonitaReadException(""));

        commandServiceImpl.getAllCommands(0, 1, SCommandCriterion.NAME_DESC);
    }

    @Test
    public final void getById() throws SCommandNotFoundException, SBonitaReadException {
        final SCommand sCommand = mock(SCommand.class);
        when(persistence.selectById(any(SelectByIdDescriptor.class))).thenReturn(sCommand);

        Assert.assertEquals(sCommand, commandServiceImpl.get(456L));
    }

    @Test(expected = SCommandNotFoundException.class)
    public final void getByIdNotExists() throws SBonitaReadException, SCommandNotFoundException {
        when(persistence.selectById(any(SelectByIdDescriptor.class))).thenReturn(null);

        commandServiceImpl.get(456L);
    }

    @Test(expected = SCommandNotFoundException.class)
    public final void getByIdThrowException() throws SBonitaReadException, SCommandNotFoundException {
        when(persistence.selectById(any(SelectByIdDescriptor.class))).thenThrow(new SBonitaReadException(""));

        commandServiceImpl.get(456L);
    }

    @Test
    public final void getByName() throws SCommandNotFoundException, SBonitaReadException {
        final SCommand sCommand = mock(SCommand.class);
        when(persistence.selectOne(any(SelectOneDescriptor.class))).thenReturn(sCommand);

        Assert.assertEquals(sCommand, commandServiceImpl.get("name"));
    }

    @Test(expected = SCommandNotFoundException.class)
    public final void getByNameNotExists() throws SBonitaReadException, SCommandNotFoundException {
        when(persistence.selectOne(any(SelectOneDescriptor.class))).thenReturn(null);

        commandServiceImpl.get("name");
    }

    @Test(expected = SCommandNotFoundException.class)
    public final void getByNameThrowException() throws SBonitaReadException, SCommandNotFoundException {
        when(persistence.selectOne(any(SelectOneDescriptor.class))).thenThrow(new SBonitaReadException(""));

        commandServiceImpl.get("name");
    }

    @Test
    public final void getNumberOfCommands() throws SBonitaSearchException, SBonitaReadException {
        final long numberOfCommands = 54165L;
        final QueryOptions options = mock(QueryOptions.class);
        when(persistence.getNumberOfEntities(SCommand.class, options, null)).thenReturn(numberOfCommands);

        Assert.assertEquals(numberOfCommands, commandServiceImpl.getNumberOfCommands(options));
    }

    @Test(expected = SBonitaSearchException.class)
    public final void getNumberOfCommandsThrowException() throws SBonitaSearchException, SBonitaReadException {
        final QueryOptions options = mock(QueryOptions.class);
        when(persistence.getNumberOfEntities(SCommand.class, options, null)).thenThrow(new SBonitaReadException(""));

        commandServiceImpl.getNumberOfCommands(options);
    }

    @Test
    public final void getUserCommands() throws SCommandGettingException, SBonitaReadException {
        final List<SCommand> sCommands = new ArrayList<SCommand>();
        when(persistence.selectList(any(SelectListDescriptor.class))).thenReturn(sCommands);

        Assert.assertEquals(sCommands, commandServiceImpl.getUserCommands(0, 1, SCommandCriterion.NAME_ASC));
    }

    @Test(expected = SCommandGettingException.class)
    public final void getUserCommandsThrowException() throws SCommandGettingException, SBonitaReadException {
        when(persistence.selectList(any(SelectListDescriptor.class))).thenThrow(new SBonitaReadException(""));

        commandServiceImpl.getUserCommands(0, 1, SCommandCriterion.NAME_DESC);
    }

    @Test
    public final void searchCommands() throws SBonitaSearchException, SBonitaReadException {
        final List<SCommand> sCommands = new ArrayList<SCommand>();
        final QueryOptions options = mock(QueryOptions.class);
        when(persistence.searchEntity(SCommand.class, options, null)).thenReturn(sCommands);

        Assert.assertEquals(sCommands, commandServiceImpl.searchCommands(options));
    }

    @Test(expected = SBonitaSearchException.class)
    public final void searchCommandsThrowException() throws SBonitaSearchException, SBonitaReadException {
        final QueryOptions options = mock(QueryOptions.class);
        when(persistence.searchEntity(SCommand.class, options, null)).thenThrow(new SBonitaReadException(""));

        commandServiceImpl.searchCommands(options);
    }

}
