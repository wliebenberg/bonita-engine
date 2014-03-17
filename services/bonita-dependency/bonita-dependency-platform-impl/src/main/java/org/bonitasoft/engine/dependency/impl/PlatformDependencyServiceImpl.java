/**
 * Copyright (C) 2012; 2014 BonitaSoft S.A.
 * BonitaSoft, 31 rue Gustave Eiffel - 38000 Grenoble
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
package org.bonitasoft.engine.dependency.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bonitasoft.engine.classloader.ClassLoaderException;
import org.bonitasoft.engine.classloader.ClassLoaderService;
import org.bonitasoft.engine.commons.NullCheckingUtil;
import org.bonitasoft.engine.dependency.ArtifactAccessor;
import org.bonitasoft.engine.dependency.DependencyService;
import org.bonitasoft.engine.dependency.SDependencyCreationException;
import org.bonitasoft.engine.dependency.SDependencyDeletionException;
import org.bonitasoft.engine.dependency.SDependencyException;
import org.bonitasoft.engine.dependency.SDependencyMappingNotFoundException;
import org.bonitasoft.engine.dependency.SDependencyNotFoundException;
import org.bonitasoft.engine.dependency.model.SDependency;
import org.bonitasoft.engine.dependency.model.SDependencyMapping;
import org.bonitasoft.engine.dependency.model.ScopeType;
import org.bonitasoft.engine.log.technical.TechnicalLoggerService;
import org.bonitasoft.engine.persistence.QueryOptions;
import org.bonitasoft.engine.persistence.SBonitaReadException;
import org.bonitasoft.engine.persistence.SelectByIdDescriptor;
import org.bonitasoft.engine.persistence.SelectListDescriptor;
import org.bonitasoft.engine.persistence.SelectOneDescriptor;
import org.bonitasoft.engine.recorder.model.EntityUpdateDescriptor;
import org.bonitasoft.engine.services.PersistenceService;
import org.bonitasoft.engine.services.SPersistenceException;
import org.bonitasoft.engine.services.UpdateDescriptor;

/**
 * @author Matthieu Chaffotte
 * @author Celine Souchet
 */
public class PlatformDependencyServiceImpl implements DependencyService {

    private final PersistenceService platformPersistenceService;

    private final ClassLoaderService classLoaderService;

    private final Map<String, Long> lastUpdates = Collections.synchronizedMap(new HashMap<String, Long>());

    public PlatformDependencyServiceImpl(final PersistenceService platformPersistenceService, final TechnicalLoggerService logger,
            final ClassLoaderService classLoaderService) {
        super();
        this.platformPersistenceService = platformPersistenceService;
        this.classLoaderService = classLoaderService;
    }

    @Override
    public void createDependency(final SDependency dependency) throws SDependencyCreationException {
        NullCheckingUtil.checkArgsNotNull(dependency);
        try {
            platformPersistenceService.insert(dependency);
        } catch (final SPersistenceException pe) {
            throw new SDependencyCreationException(pe);
        }
    }

    @Override
    public void deleteDependency(final long id) throws SDependencyNotFoundException, SDependencyDeletionException {
        final SDependency dependency = getDependency(id);
        deleteDependency(dependency);
    }

    @Override
    public void deleteDependency(final SDependency dependency) throws SDependencyDeletionException {
        try {
            platformPersistenceService.delete(dependency);
        } catch (final SPersistenceException pe) {
            throw new SDependencyDeletionException(pe);
        }
    }

    @Override
    public void deleteDependency(final String name) throws SDependencyNotFoundException, SDependencyDeletionException {
        final SDependency dependency = getDependency(name);
        deleteDependency(dependency);
    }

    @Override
    public void deleteAllDependencies() throws SDependencyDeletionException {
        final QueryOptions queryOptions = QueryOptions.defaultQueryOptions();
        List<SDependency> dependencies = null;
        do {
            try {
                dependencies = getDependencies(queryOptions);
            } catch (final SDependencyException e) {
                throw new SDependencyDeletionException(e);
            }
            for (final SDependency dependency : dependencies) {
                deleteDependency(dependency);
            }
        } while (dependencies.size() == queryOptions.getNumberOfResults());
    }

    @Override
    public void updateDependency(final SDependency dependency, final EntityUpdateDescriptor descriptor) throws SDependencyException {
        final UpdateDescriptor desc = new UpdateDescriptor(dependency);
        desc.addFields(descriptor.getFields());
        try {
            platformPersistenceService.update(desc);
            QueryOptions queryOptions = QueryOptions.defaultQueryOptions();
            List<SDependencyMapping> dependencyMappings = null;
            final long updateTimeStamp = System.currentTimeMillis();
            do {
                dependencyMappings = getDependencyMappings(dependency.getId(), queryOptions);
                for (final SDependencyMapping dependencyMapping : dependencyMappings) {
                    lastUpdates.put(getKey(dependencyMapping.getArtifactType(), dependencyMapping.getArtifactId()), updateTimeStamp);
                }
                queryOptions = QueryOptions.getNextPage(queryOptions);
            } while (dependencyMappings.size() == queryOptions.getNumberOfResults());
        } catch (final SPersistenceException pe) {
            throw new SDependencyException(pe);
        }
        refreshClassLoader();
    }

    @Override
    public SDependency getDependency(final long id) throws SDependencyNotFoundException {
        final SelectByIdDescriptor<SDependency> selectByIdDescriptor = new SelectByIdDescriptor<SDependency>("getPlatformDependencyById", SDependency.class, id);
        try {
            final SDependency sDependency = platformPersistenceService.selectById(selectByIdDescriptor);
            if (sDependency == null) {
                throw new SDependencyNotFoundException("No dependency exists using id: " + id);
            }
            return sDependency;
        } catch (final SBonitaReadException bre) {
            throw new SDependencyNotFoundException(bre);
        }
    }

    private SDependency getDependency(final String name) throws SDependencyNotFoundException {
        final Map<String, Object> parameters = Collections.singletonMap("name", (Object) name);
        try {
            final SDependency sDependency = platformPersistenceService.selectOne(new SelectOneDescriptor<SDependency>("getPlatformDependencyByName",
                    parameters, SDependency.class));
            if (sDependency == null) {
                throw new SDependencyNotFoundException("No dependency exists using name: " + name);
            }
            return sDependency;
        } catch (final SBonitaReadException bre) {
            throw new SDependencyNotFoundException(bre);
        }
    }

    @Override
    public List<SDependency> getDependencies(final QueryOptions queryOptions) throws SDependencyException {
        final Map<String, Object> parameters = Collections.emptyMap();
        try {
            final List<SDependency> dependencies = platformPersistenceService.selectList(new SelectListDescriptor<SDependency>("getPlatformDependencies",
                    parameters, SDependency.class, queryOptions));
            return dependencies;
        } catch (final SBonitaReadException bre) {
            throw new SDependencyException(bre);
        }
    }

    @Override
    public List<SDependency> getDependencies(final Collection<Long> ids) throws SDependencyException {
        final Map<String, Object> parameters = Collections.singletonMap("ids", (Object) ids);
        try {
            final List<SDependency> dependencies = platformPersistenceService.selectList(new SelectListDescriptor<SDependency>("getPlatformDependenciesById",
                    parameters, SDependency.class));
            return dependencies;
        } catch (final SBonitaReadException bre) {
            throw new SDependencyException(bre);
        }
    }

    @Override
    public void createDependencyMapping(final SDependencyMapping dependencyMapping) throws SDependencyException {
        try {
            platformPersistenceService.insert(dependencyMapping);
            lastUpdates.put(getKey(dependencyMapping.getArtifactType(), dependencyMapping.getArtifactId()), System.currentTimeMillis());
            refreshClassLoader();
        } catch (final SPersistenceException pe) {
            throw new SDependencyException(pe);
        }
    }

    @Override
    public void deleteDependencyMapping(final long id) throws SDependencyException, SDependencyMappingNotFoundException {
        final SDependencyMapping dependencyMapping = getDependencyMapping(id);
        deleteDependencyMapping(dependencyMapping);
    }

    @Override
    public void deleteDependencyMapping(final SDependencyMapping dependencyMapping) throws SDependencyException {
        try {
            platformPersistenceService.delete(dependencyMapping);
            lastUpdates.put(getKey(dependencyMapping.getArtifactType(), dependencyMapping.getArtifactId()), System.currentTimeMillis());
            refreshClassLoader();
        } catch (final SPersistenceException pe) {
            throw new SDependencyException(pe);
        }
    }

    @Override
    public void deleteAllDependencyMappings() throws SDependencyException {
        final QueryOptions queryOptions = QueryOptions.defaultQueryOptions();
        List<SDependencyMapping> dependencyMappings = null;
        do {
            dependencyMappings = getDependencyMappings(queryOptions);
            for (final SDependencyMapping dependencyMapping : dependencyMappings) {
                deleteDependencyMapping(dependencyMapping);
            }
        } while (dependencyMappings.size() == queryOptions.getNumberOfResults());
    }

    @Override
    public void updateDependencyMapping(final SDependencyMapping dependencyMapping, final EntityUpdateDescriptor descriptor) throws SDependencyException {
        final UpdateDescriptor desc = new UpdateDescriptor(dependencyMapping);
        desc.addFields(descriptor.getFields());
        try {
            platformPersistenceService.update(desc);
            lastUpdates.put(getKey(dependencyMapping.getArtifactType(), dependencyMapping.getArtifactId()), System.currentTimeMillis());
            refreshClassLoader();
        } catch (final SPersistenceException pe) {
            throw new SDependencyException(pe);
        }
    }

    @Override
    public SDependencyMapping getDependencyMapping(final long id) throws SDependencyMappingNotFoundException {
        final SelectByIdDescriptor<SDependencyMapping> selectByIdDescriptor = new SelectByIdDescriptor<SDependencyMapping>("getPlatformDependencyMappingById",
                SDependencyMapping.class, id);
        try {
            final SDependencyMapping sDependency = platformPersistenceService.selectById(selectByIdDescriptor);
            if (sDependency == null) {
                throw new SDependencyMappingNotFoundException("No dependency mapping exists using id: " + id);
            }
            return sDependency;
        } catch (final SBonitaReadException bre) {
            throw new SDependencyMappingNotFoundException(bre);
        }
    }

    @Override
    public List<SDependencyMapping> getDependencyMappings(final QueryOptions queryOptions) throws SDependencyException {
        try {
            return platformPersistenceService.selectList(new SelectListDescriptor<SDependencyMapping>("getPlatformDependencyMappings", null,
                    SDependencyMapping.class, queryOptions));
        } catch (final SBonitaReadException e) {
            throw new SDependencyException("can't get dependency mappings", e);
        }
    }

    @Override
    public List<SDependencyMapping> getDependencyMappings(final long artifactId, final ScopeType artifactType, final QueryOptions queryOptions)
            throws SDependencyException {
        try {
            final Map<String, Object> parameters = new HashMap<String, Object>();
            parameters.put("artifactId", artifactId);
            parameters.put("artifactType", artifactType);
            final SelectListDescriptor<SDependencyMapping> desc = new SelectListDescriptor<SDependencyMapping>("getPlatformDependencyMappingsByArtifact",
                    parameters, SDependencyMapping.class, queryOptions);
            return platformPersistenceService.selectList(desc);
        } catch (final SBonitaReadException e) {
            throw new SDependencyException("can't get dependency mappings by artifact: " + artifactType + ", " + artifactId, e);
        }
    }

    @Override
    public List<SDependencyMapping> getDependencyMappings(final long dependencyId, final QueryOptions queryOptions) throws SDependencyException {
        try {
            final Map<String, Object> parameters = new HashMap<String, Object>();
            parameters.put("dependencyId", dependencyId);
            final SelectListDescriptor<SDependencyMapping> desc = new SelectListDescriptor<SDependencyMapping>("getPlatformDependencyMappingsByDependency",
                    parameters, SDependencyMapping.class, queryOptions);
            return platformPersistenceService.selectList(desc);
        } catch (final SBonitaReadException e) {
            throw new SDependencyException("can't get dependency mappings by dependencyId: " + dependencyId, e);
        }
    }

    @Override
    public List<Long> getDependencyIds(final long artifactId, final ScopeType artifactType, final QueryOptions queryOptions) throws SDependencyException {
        try {
            final Map<String, Object> parameters = new HashMap<String, Object>();
            parameters.put("artifactId", artifactId);
            parameters.put("artifactType", artifactType);
            final SelectListDescriptor<Long> desc = new SelectListDescriptor<Long>("getPlatformDependencyIds", parameters, SDependency.class, Long.class,
                    queryOptions);
            return platformPersistenceService.selectList(desc);
        } catch (final SBonitaReadException e) {
            throw new SDependencyException(e);
        }
    }

    @Override
    public long getLastUpdatedTimestamp(final ScopeType artifactType, final long artifactId) {
        final String key = getKey(artifactType, artifactId);
        if (lastUpdates.containsKey(key)) {
            return lastUpdates.get(key);
        }
        return 0;
    }

    @Override
    public List<SDependencyMapping> removeDisconnectedDependencyMappings(final ArtifactAccessor artifactAccessor) throws SDependencyException {
        QueryOptions loopQueryOptions = QueryOptions.defaultQueryOptions();
        List<SDependencyMapping> dependencyMappings = null;
        final List<SDependencyMapping> result = new ArrayList<SDependencyMapping>();
        do {
            dependencyMappings = getDependencyMappings(loopQueryOptions);
            for (final SDependencyMapping dependencyMapping : dependencyMappings) {
                if (!artifactAccessor.artifactExists(dependencyMapping.getArtifactType(), dependencyMapping.getArtifactId())) {
                    result.add(dependencyMapping);
                    deleteDependencyMapping(dependencyMapping);
                }
            }
            loopQueryOptions = QueryOptions.getNextPage(loopQueryOptions);
        } while (dependencyMappings.size() == loopQueryOptions.getNumberOfResults());
        return result;
    }

    @Override
    public List<SDependencyMapping> getDisconnectedDependencyMappings(final ArtifactAccessor artifactAccessor, final QueryOptions queryOptions)
            throws SDependencyException {
        QueryOptions loopQueryOptions = new QueryOptions(queryOptions.getFromIndex(), queryOptions.getNumberOfResults(), queryOptions.getOrderByOptions());
        List<SDependencyMapping> dependencyMappings = null;
        final List<SDependencyMapping> result = new ArrayList<SDependencyMapping>();
        int numberOfResultsFound = 0;
        final int startIndex = queryOptions.getFromIndex();
        do {
            dependencyMappings = getDependencyMappings(loopQueryOptions);
            for (final SDependencyMapping dependencyMapping : dependencyMappings) {
                if (!artifactAccessor.artifactExists(dependencyMapping.getArtifactType(), dependencyMapping.getArtifactId())) {
                    numberOfResultsFound++;
                    if (numberOfResultsFound > startIndex) {
                        // add it in the results
                        result.add(dependencyMapping);
                    }
                    if (result.size() == queryOptions.getNumberOfResults()) {
                        // stop the for iteration, we have the number of results we want
                        break;
                    }
                }
            }
            loopQueryOptions = QueryOptions.getNextPage(loopQueryOptions);
        } while (dependencyMappings.size() == loopQueryOptions.getNumberOfResults() && result.size() < queryOptions.getNumberOfResults());
        return result;
    }

    private String getKey(final ScopeType scopeType, final long artifactId) {
        final StringBuffer sb = new StringBuffer(scopeType.name());
        sb.append("________");
        sb.append(artifactId);
        return sb.toString();
    }

    @Override
    public void deleteDependencies(final long id, final ScopeType type) throws SDependencyException, SDependencyNotFoundException, SDependencyDeletionException {
        QueryOptions queryOptions = QueryOptions.defaultQueryOptions();
        final ArrayList<Long> allDependencyIds = new ArrayList<Long>();
        List<Long> dependencyIds;
        do {
            dependencyIds = getDependencyIds(id, type, queryOptions);
            allDependencyIds.addAll(dependencyIds);
            queryOptions = QueryOptions.getNextPage(queryOptions);
        } while (dependencyIds.size() == QueryOptions.DEFAULT_NUMBER_OF_RESULTS);
        for (final Long dependencyId : allDependencyIds) {
            final List<SDependencyMapping> dependencyMappings = getDependencyMappings(dependencyId, QueryOptions.defaultQueryOptions());
            if (dependencyMappings.size() == 1) {// only when the dependency is linked only to on element
                deleteDependencyMapping(dependencyMappings.get(0));
                deleteDependency(dependencyId);
            }
        }
    }

    private void refreshClassLoader() throws SDependencyException {
        refreshClassLoader(ScopeType.valueOf(classLoaderService.getGlobalClassLoaderType()), classLoaderService.getGlobalClassLoaderId());
    }

    @Override
    public void refreshClassLoader(final ScopeType type, final long id) throws SDependencyException {
        final Map<String, byte[]> resources = getDependenciesResources();
        try {
            classLoaderService.refreshGlobalClassLoader(resources);
        } catch (final ClassLoaderException e) {
            throw new SDependencyException("can't refresh global classLoader", e);
        }
    }

    private Map<String, byte[]> getDependenciesResources() throws SDependencyException {
        final Map<String, byte[]> resources = new HashMap<String, byte[]>();
        int fromIndex = 0;
        final int pageSize = QueryOptions.DEFAULT_NUMBER_OF_RESULTS;
        List<Long> dependencyIds = null;

        do {
            final QueryOptions queryOptions = new QueryOptions(fromIndex, pageSize);
            dependencyIds = getDependencyIds(classLoaderService.getGlobalClassLoaderId(), ScopeType.valueOf(classLoaderService.getGlobalClassLoaderType()),
                    queryOptions);
            if (dependencyIds != null && dependencyIds.size() > 0) {
                final List<SDependency> dependencies = getDependencies(dependencyIds);
                for (final SDependency dependency : dependencies) {
                    resources.put(dependency.getFileName(), dependency.getValue());
                }
            }
            fromIndex = fromIndex + pageSize;
        } while (dependencyIds.size() == pageSize);
        return resources;
    }

    @Override
    public void updateDependenciesOfArtifact(final long id, final ScopeType type, final ArrayList<SDependency> dependencies) throws SDependencyException {
        throw new UnsupportedOperationException("only one artifact at platform level, no need to update in batch all dependencies");
    }

}
