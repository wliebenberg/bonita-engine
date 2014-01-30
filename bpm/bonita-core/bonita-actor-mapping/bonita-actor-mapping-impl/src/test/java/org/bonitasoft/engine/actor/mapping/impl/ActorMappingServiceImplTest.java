package org.bonitasoft.engine.actor.mapping.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bonitasoft.engine.actor.mapping.SActorCreationException;
import org.bonitasoft.engine.actor.mapping.SActorDeletionException;
import org.bonitasoft.engine.actor.mapping.SActorMemberDeletionException;
import org.bonitasoft.engine.actor.mapping.SActorNotFoundException;
import org.bonitasoft.engine.actor.mapping.SActorUpdateException;
import org.bonitasoft.engine.actor.mapping.model.SActor;
import org.bonitasoft.engine.actor.mapping.model.SActorMember;
import org.bonitasoft.engine.actor.mapping.model.SActorUpdateBuilder;
import org.bonitasoft.engine.actor.mapping.model.SActorUpdateBuilderFactory;
import org.bonitasoft.engine.actor.mapping.persistence.SelectDescriptorBuilder;
import org.bonitasoft.engine.builder.BuilderFactory;
import org.bonitasoft.engine.identity.IdentityService;
import org.bonitasoft.engine.persistence.OrderByType;
import org.bonitasoft.engine.persistence.ReadPersistenceService;
import org.bonitasoft.engine.persistence.SBonitaReadException;
import org.bonitasoft.engine.persistence.SelectByIdDescriptor;
import org.bonitasoft.engine.persistence.SelectListDescriptor;
import org.bonitasoft.engine.persistence.SelectOneDescriptor;
import org.bonitasoft.engine.queriablelogger.model.SQueriableLogSeverity;
import org.bonitasoft.engine.recorder.Recorder;
import org.bonitasoft.engine.recorder.SRecorderException;
import org.bonitasoft.engine.recorder.model.DeleteAllRecord;
import org.bonitasoft.engine.recorder.model.DeleteRecord;
import org.bonitasoft.engine.recorder.model.InsertRecord;
import org.bonitasoft.engine.recorder.model.UpdateRecord;
import org.bonitasoft.engine.services.QueriableLoggerService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.MockitoAnnotations;

/**
 * @author Celine Souchet
 */
public class ActorMappingServiceImplTest {

    private Recorder recorder;

    private ReadPersistenceService persistenceService;

    private QueriableLoggerService queriableLoggerService;

    private IdentityService identityService;

    private ActorMappingServiceImpl actorMappingServiceImpl;

    @Before
    public void initialize() {
        recorder = mock(Recorder.class);
        persistenceService = mock(ReadPersistenceService.class);
        queriableLoggerService = mock(QueriableLoggerService.class);
        identityService = mock(IdentityService.class);
        actorMappingServiceImpl = new ActorMappingServiceImpl(persistenceService, recorder, queriableLoggerService, identityService);
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public final void getActorById() throws SActorNotFoundException, SBonitaReadException {
        final SActor actor = mock(SActor.class);
        when(persistenceService.selectById(Matchers.<SelectByIdDescriptor<SActor>> any())).thenReturn(actor);

        Assert.assertEquals(actor, actorMappingServiceImpl.getActor(456L));
    }

    @Test(expected = SActorNotFoundException.class)
    public final void getActorByIdNotExists() throws SBonitaReadException, SActorNotFoundException {
        when(persistenceService.selectById(Matchers.<SelectByIdDescriptor<SActor>> any())).thenReturn(null);

        actorMappingServiceImpl.getActor(456L);
    }

    @Test
    public final void getNumberOfActorMembers() throws SBonitaReadException {
        final long actorId = 456L;
        final long numberOfActorMemebers = 1L;
        when(persistenceService.selectOne(Matchers.<SelectOneDescriptor<Long>> any())).thenReturn(numberOfActorMemebers);

        Assert.assertEquals(numberOfActorMemebers, actorMappingServiceImpl.getNumberOfActorMembers(actorId));
    }

    @Test
    public final void getNumberOfUsersOfActor() throws SBonitaReadException {
        final long numberOfUsersOfActor = 155L;
        when(persistenceService.selectOne(Matchers.<SelectOneDescriptor<Long>> any())).thenReturn(numberOfUsersOfActor);

        Assert.assertEquals(numberOfUsersOfActor, actorMappingServiceImpl.getNumberOfUsersOfActor(456L));
    }

    @Test(expected = RuntimeException.class)
    public final void getNumberOfUsersOfActorThrowException() throws SBonitaReadException {
        when(persistenceService.selectOne(Matchers.<SelectOneDescriptor<SActor>> any())).thenThrow(new SBonitaReadException(""));

        actorMappingServiceImpl.getNumberOfUsersOfActor(456L);
    }

    @Test
    public final void getNumberOfRolesOfActor() throws SBonitaReadException {
        final long numberOfRolesOfActor = 155L;
        when(persistenceService.selectOne(Matchers.<SelectOneDescriptor<Long>> any())).thenReturn(numberOfRolesOfActor);

        Assert.assertEquals(numberOfRolesOfActor, actorMappingServiceImpl.getNumberOfRolesOfActor(456L));
    }

    @Test(expected = RuntimeException.class)
    public final void getNumberOfRolesOfActorThrowException() throws SBonitaReadException {
        when(persistenceService.selectOne(Matchers.<SelectOneDescriptor<SActor>> any())).thenThrow(new SBonitaReadException(""));

        actorMappingServiceImpl.getNumberOfRolesOfActor(456L);
    }

    @Test
    public final void getNumberOfGroupsOfActor() throws SBonitaReadException {
        final long numberOfGroupsOfActor = 155L;
        when(persistenceService.selectOne(Matchers.<SelectOneDescriptor<Long>> any())).thenReturn(numberOfGroupsOfActor);

        Assert.assertEquals(numberOfGroupsOfActor, actorMappingServiceImpl.getNumberOfGroupsOfActor(456L));
    }

    @Test(expected = RuntimeException.class)
    public final void getNumberOfGroupsOfActorThrowException() throws SBonitaReadException {
        when(persistenceService.selectOne(Matchers.<SelectOneDescriptor<SActor>> any())).thenThrow(new SBonitaReadException(""));

        actorMappingServiceImpl.getNumberOfGroupsOfActor(456L);
    }

    @Test
    public final void getNumberOfMembershipsOfActor() throws SBonitaReadException {
        final long numberOfGroupsOfActor = 155L;
        when(persistenceService.selectOne(Matchers.<SelectOneDescriptor<Long>> any())).thenReturn(numberOfGroupsOfActor);

        Assert.assertEquals(numberOfGroupsOfActor, actorMappingServiceImpl.getNumberOfMembershipsOfActor(456L));
    }

    @Test(expected = RuntimeException.class)
    public final void getNumberOfMembershipsOfActorThrowException() throws SBonitaReadException {
        when(persistenceService.selectOne(Matchers.<SelectOneDescriptor<SActor>> any())).thenThrow(new SBonitaReadException(""));

        actorMappingServiceImpl.getNumberOfMembershipsOfActor(456L);
    }

    @Test
    public final void getActorByNameAndScopeId() throws SActorNotFoundException, SBonitaReadException {
        final SActor actor = mock(SActor.class);
        when(persistenceService.selectOne(Matchers.<SelectOneDescriptor<SActor>> any())).thenReturn(actor);

        Assert.assertEquals(actor, actorMappingServiceImpl.getActor("actorName", 69L));
    }

    @Test(expected = SActorNotFoundException.class)
    public final void getActorByNameAndScopeIdNotExists() throws SActorNotFoundException, SBonitaReadException {
        when(persistenceService.selectOne(Matchers.<SelectOneDescriptor<SActor>> any())).thenReturn(null);

        actorMappingServiceImpl.getActor("actorName", 69L);
    }

    @Test(expected = SActorNotFoundException.class)
    public final void getActorByNameAndScopeIdThrowException() throws SActorNotFoundException, SBonitaReadException {
        when(persistenceService.selectOne(Matchers.<SelectOneDescriptor<SActor>> any())).thenThrow(new SBonitaReadException(""));

        actorMappingServiceImpl.getActor("actorName", 69L);
    }

    @Test
    public final void getActorMembersIntIntStringOrderByType() throws SBonitaReadException {
        final List<SActorMember> actors = new ArrayList<SActorMember>();
        when(persistenceService.selectList(Matchers.<SelectListDescriptor<SActorMember>> any())).thenReturn(actors);

        Assert.assertEquals(actors, actorMappingServiceImpl.getActorMembers(0, 1));
    }

    @Test
    public final void getActorMembersLongIntInt() throws SBonitaReadException {
        final List<SActorMember> actors = new ArrayList<SActorMember>();
        when(persistenceService.selectList(Matchers.<SelectListDescriptor<SActorMember>> any())).thenReturn(actors);

        Assert.assertEquals(actors, actorMappingServiceImpl.getActorMembers(4115L, 0, 1));
    }

    @Test
    public final void getActorMembersOfGroup() throws SBonitaReadException {
        final List<SActorMember> actors = new ArrayList<SActorMember>(6);
        when(persistenceService.selectList(Matchers.<SelectListDescriptor<SActorMember>> any())).thenReturn(actors);

        Assert.assertEquals(actors, actorMappingServiceImpl.getActorMembersOfGroup(41L));
    }

    @Test
    public final void getActorMembersOfRole() throws SBonitaReadException {
        final List<SActorMember> actors = new ArrayList<SActorMember>(3);
        when(persistenceService.selectList(Matchers.<SelectListDescriptor<SActorMember>> any())).thenReturn(actors);

        Assert.assertEquals(actors, actorMappingServiceImpl.getActorMembersOfRole(41L));
    }

    @Test
    public final void getActorMembersOfUser() throws SBonitaReadException {
        final List<SActorMember> actors = new ArrayList<SActorMember>(3);
        when(persistenceService.selectList(Matchers.<SelectListDescriptor<SActorMember>> any())).thenReturn(actors);

        Assert.assertEquals(actors, actorMappingServiceImpl.getActorMembersOfUser(41L));
    }

    @Test
    public final void getActorsByListOfIds() throws SBonitaReadException {
        final List<SActor> actors = new ArrayList<SActor>(3);
        when(persistenceService.selectList(Matchers.<SelectListDescriptor<SActor>> any())).thenReturn(actors);

        final List<Long> actorIds = new ArrayList<Long>(1);
        actorIds.add(589L);
        Assert.assertEquals(actors, actorMappingServiceImpl.getActors(actorIds));
    }

    @Test
    public final void getActorsByListOfIdsWithEmptyList() throws SBonitaReadException {
        Assert.assertEquals(Collections.emptyList(), actorMappingServiceImpl.getActors(new ArrayList<Long>(0)));
    }

    @Test
    public final void getActorsByListOfIdsWithNullList() throws SBonitaReadException {
        Assert.assertEquals(Collections.emptyList(), actorMappingServiceImpl.getActors(null));
    }

    @Test
    public final void getActorsByScopeId() throws SBonitaReadException {
        final List<SActor> actors = new ArrayList<SActor>(3);
        when(persistenceService.selectList(Matchers.<SelectListDescriptor<SActor>> any())).thenReturn(actors);

        Assert.assertEquals(actors, actorMappingServiceImpl.getActors(1654L));
    }

    @Test
    public final void getActorsLongIntIntStringOrderByType() throws SBonitaReadException {
        final List<SActor> actors = new ArrayList<SActor>(3);
        when(persistenceService.selectList(Matchers.<SelectListDescriptor<SActor>> any())).thenReturn(actors);

        Assert.assertEquals(actors, actorMappingServiceImpl.getActors(41564L, 0, 1, "id", OrderByType.ASC));
    }

    @Test
    public final void getActorsOfUserCanStartProcessDefinition() throws SBonitaReadException {
        final List<SActor> actors = new ArrayList<SActor>(3);
        when(persistenceService.selectList(Matchers.<SelectListDescriptor<SActor>> any())).thenReturn(actors);

        Assert.assertEquals(actors, actorMappingServiceImpl.getActorsOfUserCanStartProcessDefinition(315L, 5484L));
    }

    @Test
    public final void getActorsByScopeIdsAndUserId() throws SBonitaReadException {
        final List<SActor> actors = new ArrayList<SActor>(3);
        when(persistenceService.selectList(Matchers.<SelectListDescriptor<SActor>> any())).thenReturn(actors);

        Assert.assertEquals(actors, actorMappingServiceImpl.getActors(new HashSet<Long>(), 5484L));
    }

    @Test
    public final void addActors() throws SActorCreationException {
        final Set<SActor> actors = new HashSet<SActor>();
        actors.add(mock(SActor.class));

        final ActorMappingServiceImpl mockedActorMappingServiceImpl = mock(ActorMappingServiceImpl.class, withSettings().spiedInstance(actorMappingServiceImpl));
        final SActor sActor = mock(SActor.class);
        when(mockedActorMappingServiceImpl.addActor(any(SActor.class))).thenReturn(sActor);

        // Let's call it for real:
        doCallRealMethod().when(mockedActorMappingServiceImpl).addActors(actors);
        final Set<SActor> result = mockedActorMappingServiceImpl.addActors(actors);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(sActor, result.toArray()[0]);

        // and check methods are called:
        verify(mockedActorMappingServiceImpl, times(1)).addActor(any(SActor.class));
    }

    @Test
    public final void addActorsEmptyList() throws SActorCreationException {
        final Set<SActor> actors = new HashSet<SActor>();

        final Set<SActor> result = actorMappingServiceImpl.addActors(actors);
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test(expected = SActorCreationException.class)
    public final void addActorsThrowException() throws SActorCreationException {
        final Set<SActor> actors = new HashSet<SActor>();
        actors.add(mock(SActor.class));

        final ActorMappingServiceImpl mockedActorMappingServiceImpl = mock(ActorMappingServiceImpl.class, withSettings().spiedInstance(actorMappingServiceImpl));
        when(mockedActorMappingServiceImpl.addActor(any(SActor.class))).thenThrow(new SActorCreationException(""));

        // Let's call it for real:
        doCallRealMethod().when(mockedActorMappingServiceImpl).addActors(actors);
        mockedActorMappingServiceImpl.addActors(actors);
    }

    @Test
    public final void addActor() throws Exception {
        final SActor sActor = mock(SActor.class);
        doReturn(1L).when(sActor).getId();

        doNothing().when(recorder).recordInsert(any(InsertRecord.class));
        doReturn(false).when(queriableLoggerService).isLoggable(anyString(), any(SQueriableLogSeverity.class));

        final SActor result = actorMappingServiceImpl.addActor(sActor);
        assertNotNull(result);
        assertEquals(sActor, result);
    }

    @Test(expected = IllegalArgumentException.class)
    public final void addNullActor() throws Exception {
        actorMappingServiceImpl.addActor(null);
    }

    @Test
    public final void updateActor() throws SActorNotFoundException, SActorUpdateException, SBonitaReadException, SRecorderException {
        final SActor sActor = mock(SActor.class);
        doReturn(3L).when(sActor).getId();

        final SActorUpdateBuilder sActorUpdateBuilder = BuilderFactory.get(SActorUpdateBuilderFactory.class).createNewInstance();
        sActorUpdateBuilder.updateDescription("newDescription");
        sActorUpdateBuilder.updateDisplayName("newDisplayName");

        doReturn(sActor).when(persistenceService).selectById(Matchers.<SelectByIdDescriptor<SActor>> any());
        doReturn(false).when(queriableLoggerService).isLoggable(anyString(), any(SQueriableLogSeverity.class));

        final SActor result = actorMappingServiceImpl.updateActor(3, sActorUpdateBuilder.done());
        assertNotNull(result);
        assertEquals(sActor, result);
    }

    @Test(expected = SActorNotFoundException.class)
    public final void updateActorNotExists() throws SActorUpdateException, SActorNotFoundException, SBonitaReadException {
        final SActorUpdateBuilder sActorUpdateBuilder = BuilderFactory.get(SActorUpdateBuilderFactory.class).createNewInstance();;
        doReturn(null).when(persistenceService).selectById(Matchers.<SelectByIdDescriptor<SActor>> any());

        actorMappingServiceImpl.updateActor(4, sActorUpdateBuilder.done());
    }

    @Test(expected = SActorUpdateException.class)
    public final void updateActorThrowException() throws SActorUpdateException, SActorNotFoundException, SBonitaReadException, SRecorderException {
        final SActor sActor = mock(SActor.class);
        doReturn(3L).when(sActor).getId();

        final SActorUpdateBuilder sActorUpdateBuilder = BuilderFactory.get(SActorUpdateBuilderFactory.class).createNewInstance();
        sActorUpdateBuilder.updateDescription("newDescription");
        sActorUpdateBuilder.updateDisplayName("newDisplayName");

        doReturn(sActor).when(persistenceService).selectById(Matchers.<SelectByIdDescriptor<SActor>> any());
        doThrow(new SRecorderException("plop")).when(recorder).recordUpdate(any(UpdateRecord.class));

        actorMappingServiceImpl.updateActor(3, sActorUpdateBuilder.done());
    }

    @Test
    public final void deleteActors() throws Exception {
        final int scopeId = 9;
        final List<SActor> sActors = new ArrayList<SActor>(3);
        final SActor sActor = mock(SActor.class);
        doReturn(3L).when(sActor).getId();
        sActors.add(sActor);

        final List<SActorMember> sActorMembers = new ArrayList<SActorMember>();
        final SActorMember sActorMember = mock(SActorMember.class);
        doReturn(4L).when(sActorMember).getId();
        sActorMembers.add(sActorMember);

        doReturn(sActors).when(persistenceService).selectList(SelectDescriptorBuilder.getActorsOfScope(scopeId));
        doReturn(sActorMembers).doReturn(new ArrayList<SActorMember>()).when(persistenceService).selectList(SelectDescriptorBuilder.getActorMembers(3, 0, 50));
        doNothing().when(recorder).recordDelete(any(DeleteRecord.class));
        doReturn(false).when(queriableLoggerService).isLoggable(anyString(), any(SQueriableLogSeverity.class));

        actorMappingServiceImpl.deleteActors(scopeId);
        // verifyPrivate(actorMappingServiceImpl, times(1)).invoke("deleteActor", any());
        // verifyPrivate(actorMappingServiceImpl, times(1)).invoke("removeActorMember", any());
    }

    @Test
    public final void deleteNoActorMembers() throws SBonitaReadException, SRecorderException, SActorDeletionException {
        final int scopeId = 9;
        final List<SActor> sActors = new ArrayList<SActor>(3);
        final SActor sActor = mock(SActor.class);
        doReturn(3L).when(sActor).getId();
        sActors.add(sActor);

        final List<SActorMember> sActorMembers = new ArrayList<SActorMember>();

        doReturn(sActors).when(persistenceService).selectList(SelectDescriptorBuilder.getActorsOfScope(scopeId));
        doReturn(sActorMembers).when(persistenceService).selectList(SelectDescriptorBuilder.getActorMembers(3, 0, 50));
        doNothing().when(recorder).recordDelete(any(DeleteRecord.class));
        doReturn(false).when(queriableLoggerService).isLoggable(anyString(), any(SQueriableLogSeverity.class));

        actorMappingServiceImpl.deleteActors(scopeId);
    }

    @Test
    public final void deleteNoActors() throws SBonitaReadException, SRecorderException, SActorDeletionException {
        final int scopeId = 9;
        final List<SActor> sActors = new ArrayList<SActor>(3);

        doReturn(sActors).when(persistenceService).selectList(SelectDescriptorBuilder.getActorsOfScope(scopeId));
        doNothing().when(recorder).recordDelete(any(DeleteRecord.class));
        doReturn(false).when(queriableLoggerService).isLoggable(anyString(), any(SQueriableLogSeverity.class));

        actorMappingServiceImpl.deleteActors(scopeId);
    }

    @Test
    public final void deleteAllActorMembers() throws SRecorderException, SActorMemberDeletionException {
        doNothing().when(recorder).recordDeleteAll(any(DeleteAllRecord.class));

        actorMappingServiceImpl.deleteAllActorMembers();
    }

    @Test(expected = SActorMemberDeletionException.class)
    public final void deleteAllActorMembersThrowException() throws SRecorderException, SActorMemberDeletionException {
        doThrow(new SRecorderException("plop")).when(recorder).recordDeleteAll(any(DeleteAllRecord.class));

        actorMappingServiceImpl.deleteAllActorMembers();
    }

}
