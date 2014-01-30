package org.bonitasoft.engine.identity.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;

import org.bonitasoft.engine.identity.SGroupNotFoundException;
import org.bonitasoft.engine.identity.SIdentityException;
import org.bonitasoft.engine.identity.model.SGroup;
import org.bonitasoft.engine.identity.recorder.SelectDescriptorBuilder;
import org.bonitasoft.engine.log.technical.TechnicalLoggerService;
import org.bonitasoft.engine.persistence.OrderByType;
import org.bonitasoft.engine.persistence.QueryOptions;
import org.bonitasoft.engine.persistence.ReadPersistenceService;
import org.bonitasoft.engine.persistence.SBonitaReadException;
import org.bonitasoft.engine.persistence.SBonitaSearchException;
import org.bonitasoft.engine.persistence.SelectOneDescriptor;
import org.bonitasoft.engine.recorder.Recorder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * @author Matthieu Chaffotte
 * @author Celine Souchet
 */
public class IdentityServiceImplForGroupTest {

    @Mock
    private Recorder recorder;

    @Mock
    private ReadPersistenceService persistenceService;

    @Mock
    private TechnicalLoggerService logger;

    @InjectMocks
    private IdentityServiceImpl identityServiceImpl;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void getNumberOfGroupChildren() throws Exception {
        final SGroup group = mock(SGroup.class);
        when(group.getParentPath()).thenReturn("/thePath");
        when(persistenceService.selectById(SelectDescriptorBuilder.getElementById(SGroup.class, "Group", 123l))).thenReturn(group);
        when(persistenceService.selectOne(SelectDescriptorBuilder.getNumberOfGroupChildren("/thePath"))).thenReturn(12l);

        assertEquals(12l, identityServiceImpl.getNumberOfGroupChildren(123l));
    }

    @Test(expected = SIdentityException.class)
    public void getNumberOfGroupChildrenThrowException() throws Exception {
        final SGroup group = mock(SGroup.class);
        when(group.getParentPath()).thenReturn("/thePath");
        when(persistenceService.selectById(SelectDescriptorBuilder.getElementById(SGroup.class, "Group", 123l))).thenReturn(group);
        when(persistenceService.selectOne(SelectDescriptorBuilder.getNumberOfGroupChildren("/thePath"))).thenThrow(new SBonitaReadException(""));

        identityServiceImpl.getNumberOfGroupChildren(123l);
    }

    @Test
    public void getGroupChildrenById() throws Exception {
        final SGroup group = mock(SGroup.class);
        final SGroup child = mock(SGroup.class);
        when(persistenceService.selectById(SelectDescriptorBuilder.getElementById(SGroup.class, "Group", 123l))).thenReturn(group);
        when(persistenceService.selectList(SelectDescriptorBuilder.getChildrenOfGroup(group))).thenReturn(Collections.singletonList(child));

        assertEquals(child, identityServiceImpl.getGroupChildren(123l).get(0));
    }

    @Test(expected = SIdentityException.class)
    public void getGroupChildrenByIdThrowException() throws Exception {
        final SGroup group = mock(SGroup.class);
        when(persistenceService.selectById(SelectDescriptorBuilder.getElementById(SGroup.class, "Group", 123l))).thenReturn(group);
        when(persistenceService.selectList(SelectDescriptorBuilder.getChildrenOfGroup(group))).thenThrow(new SBonitaReadException(""));

        identityServiceImpl.getGroupChildren(123l);
    }

    @Test
    public void getGroupChildrenPaginatedById() throws Exception {
        final SGroup group = mock(SGroup.class);
        final SGroup child = mock(SGroup.class);
        when(persistenceService.selectById(SelectDescriptorBuilder.getElementById(SGroup.class, "Group", 123l))).thenReturn(group);
        when(persistenceService.selectList(SelectDescriptorBuilder.getChildrenOfGroup(group, 0, 10))).thenReturn(Collections.singletonList(child));

        assertEquals(child, identityServiceImpl.getGroupChildren(123l, 0, 10).get(0));
    }

    @Test(expected = SIdentityException.class)
    public void getGroupChildrenPaginatedByIdThrowException() throws Exception {
        final SGroup group = mock(SGroup.class);
        when(persistenceService.selectById(SelectDescriptorBuilder.getElementById(SGroup.class, "Group", 123l))).thenReturn(group);
        when(persistenceService.selectList(SelectDescriptorBuilder.getChildrenOfGroup(group, 0, 10))).thenThrow(new SBonitaReadException(""));

        identityServiceImpl.getGroupChildren(123l, 0, 10);
    }

    @Test
    public void getGroupChildrenPaginatedByIdWithOrder() throws Exception {
        final SGroup group = mock(SGroup.class);
        final SGroup child = mock(SGroup.class);
        when(persistenceService.selectById(SelectDescriptorBuilder.getElementById(SGroup.class, "Group", 123l))).thenReturn(group);
        when(persistenceService.selectList(SelectDescriptorBuilder.getChildrenOfGroup(group, "name", OrderByType.ASC, 0, 10))).thenReturn(
                Collections.singletonList(child));

        assertEquals(child, identityServiceImpl.getGroupChildren(123l, 0, 10, "name", OrderByType.ASC).get(0));
    }

    @Test(expected = SIdentityException.class)
    public void getGroupChildrenPaginatedByIdWithOrderThrowException() throws Exception {
        final SGroup group = mock(SGroup.class);
        when(persistenceService.selectById(SelectDescriptorBuilder.getElementById(SGroup.class, "Group", 123l))).thenReturn(group);
        when(persistenceService.selectList(SelectDescriptorBuilder.getChildrenOfGroup(group, "name", OrderByType.ASC, 0, 10))).thenThrow(
                new SBonitaReadException(""));

        identityServiceImpl.getGroupChildren(123l, 0, 10, "name", OrderByType.ASC);
    }

    @Test
    public void getNumberOfGroupsWithOptions() throws Exception {
        final QueryOptions options = new QueryOptions(0, 10);
        when(persistenceService.getNumberOfEntities(SGroup.class, options, null)).thenReturn(125l);

        assertEquals(125, identityServiceImpl.getNumberOfGroups(options));
    }

    @Test(expected = SBonitaSearchException.class)
    public void getNumberOfGroupsWithOptionsThrowException() throws Exception {
        final QueryOptions options = new QueryOptions(0, 10);
        when(persistenceService.getNumberOfEntities(SGroup.class, options, null)).thenThrow(new SBonitaReadException(""));

        identityServiceImpl.getNumberOfGroups(options);
    }

    @Test
    public void getNumberOfGroups() throws Exception {
        when(persistenceService.selectOne(SelectDescriptorBuilder.getNumberOfElement("SGroup", SGroup.class))).thenReturn(125l);

        assertEquals(125, identityServiceImpl.getNumberOfGroups());
    }

    @Test(expected = SIdentityException.class)
    public void getNumberOfGroupsThrowException() throws Exception {
        when(persistenceService.selectOne(SelectDescriptorBuilder.getNumberOfElement("SGroup", SGroup.class))).thenThrow(new SBonitaReadException(""));

        identityServiceImpl.getNumberOfGroups();
    }

    @Test
    public void searchGroups() throws Exception {
        final QueryOptions options = new QueryOptions(0, 10);
        final SGroup group = mock(SGroup.class);
        when(persistenceService.searchEntity(SGroup.class, options, null)).thenReturn(Collections.singletonList(group));

        assertEquals(group, identityServiceImpl.searchGroups(options).get(0));
    }

    @Test(expected = SBonitaSearchException.class)
    public void searchGroupsThrowException() throws Exception {
        final QueryOptions options = new QueryOptions(0, 10);
        when(persistenceService.searchEntity(SGroup.class, options, null)).thenThrow(new SBonitaReadException(""));

        identityServiceImpl.searchGroups(options);
    }

    @Test
    public void getGroup() throws Exception {
        final SGroup group = mock(SGroup.class);
        when(persistenceService.selectById(SelectDescriptorBuilder.getElementById(SGroup.class, "Group", 123l))).thenReturn(group);

        assertEquals(group, identityServiceImpl.getGroup(123l));
    }

    @Test(expected = SIdentityException.class)
    public void getGroupThrowException() throws Exception {
        when(persistenceService.selectById(SelectDescriptorBuilder.getElementById(SGroup.class, "Group", 123l))).thenThrow(new SBonitaReadException(""));

        identityServiceImpl.getGroup(123l);
    }

    @Test(expected = SGroupNotFoundException.class)
    public void getGroupNotExist() throws Exception {
        final SGroup group = mock(SGroup.class);

        assertEquals(group, identityServiceImpl.getGroup(123l));
    }

    @Test
    public void getGroupsByName() throws Exception {
        final SGroup group = mock(SGroup.class);
        when(persistenceService.selectList(SelectDescriptorBuilder.getGroupsByName("name"))).thenReturn(Collections.singletonList(group));

        assertEquals(group, identityServiceImpl.getGroupsByName("name").iterator().next());
    }

    @Test(expected = SIdentityException.class)
    public void getGroupByNameThrowException() throws Exception {
        when(persistenceService.selectList(SelectDescriptorBuilder.getGroupsByName("name"))).thenThrow(new SBonitaReadException(""));

        identityServiceImpl.getGroupsByName("name");
    }

    @Test
    public void getGroupByPath() throws Exception {
        final SGroup group = mock(SGroup.class);
        when(persistenceService.selectOne(SelectDescriptorBuilder.getGroupByName("path"))).thenReturn(group);

        assertEquals(group, identityServiceImpl.getGroupByPath("/path"));
    }

    @Test(expected = SGroupNotFoundException.class)
    public void getGroupByPathNotExist() throws Exception {
        final SGroup group = mock(SGroup.class);

        assertEquals(group, identityServiceImpl.getGroupByPath("/path"));
    }

    @Test
    public void getGroupByPathWithNoSlash() throws Exception {
        final SGroup group = mock(SGroup.class);
        when(persistenceService.selectOne(SelectDescriptorBuilder.getGroupByName("path"))).thenReturn(group);

        assertEquals(group, identityServiceImpl.getGroupByPath("path"));
    }

    @Test
    public void getGroupByPathThatIsNotRoot() throws Exception {
        final SGroup group = mock(SGroup.class);
        when(persistenceService.selectOne(SelectDescriptorBuilder.getGroupByPath("/path", "subPath"))).thenReturn(group);

        assertEquals(group, identityServiceImpl.getGroupByPath("/path/subPath"));
    }

    @Test(expected = SIdentityException.class)
    public void getGroupByPathThrowException() throws Exception {
        when(persistenceService.selectOne(Matchers.<SelectOneDescriptor<SGroup>> any())).thenThrow(new SBonitaReadException(""));

        identityServiceImpl.getGroupByPath("path");
    }

    @Test
    public final void getGroupsByIds() throws SBonitaReadException, SGroupNotFoundException {
        final SGroup group = mock(SGroup.class);
        when(persistenceService.selectList(SelectDescriptorBuilder.getElementsByIds(SGroup.class, "Group", Arrays.asList(123l)))).thenReturn(
                Arrays.asList(group));

        assertEquals(group, identityServiceImpl.getGroups(Arrays.asList(123l)).get(0));
    }

    @Test
    public void getGroupsByNullIds() throws Exception {
        assertTrue(identityServiceImpl.getGroups(null).isEmpty());
    }

    @Test
    public void getGroupsByEmptyIds() throws Exception {
        assertTrue(identityServiceImpl.getGroups(Collections.<Long> emptyList()).isEmpty());
    }

    @Test(expected = SIdentityException.class)
    public void getGroupsByIdsThrowException() throws Exception {
        when(persistenceService.selectList(SelectDescriptorBuilder.getElementsByIds(SGroup.class, "Group", Arrays.asList(123l)))).thenThrow(
                new SBonitaReadException(""));

        identityServiceImpl.getGroups(Arrays.asList(123l));
    }

    @Test
    public final void getGroupsPaginated() throws SBonitaReadException, SIdentityException {
        final SGroup group = mock(SGroup.class);
        when(persistenceService.selectList(SelectDescriptorBuilder.getElements(SGroup.class, "Group", 0, 10))).thenReturn(Arrays.asList(group));

        assertEquals(group, identityServiceImpl.getGroups(0, 10).get(0));
    }

    @Test(expected = SIdentityException.class)
    public void getGroupsPaginatedThrowException() throws Exception {
        when(persistenceService.selectList(SelectDescriptorBuilder.getElements(SGroup.class, "Group", 0, 10))).thenThrow(new SBonitaReadException(""));

        identityServiceImpl.getGroups(0, 10);
    }

    @Test
    public void getGroupsPaginatedWithOrder() throws Exception {
        final SGroup group = mock(SGroup.class);
        when(persistenceService.selectList(SelectDescriptorBuilder.getElements(SGroup.class, "Group", "name", OrderByType.ASC, 0, 10))).thenReturn(
                Arrays.asList(group));

        assertEquals(group, identityServiceImpl.getGroups(0, 10, "name", OrderByType.ASC).get(0));
    }

    @Test(expected = SIdentityException.class)
    public void getGroupsPaginatedWithOrderThrowException() throws Exception {
        when(persistenceService.selectList(SelectDescriptorBuilder.getElements(SGroup.class, "Group", "name", OrderByType.ASC, 0, 10))).thenThrow(
                new SBonitaReadException(""));

        identityServiceImpl.getGroups(0, 10, "name", OrderByType.ASC);
    }

}
