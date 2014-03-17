/**
 * Copyright (C) 2013-2014 BonitaSoft S.A.
 * BonitaSoft, 32 rue Gustave Eiffel - 38000 Grenoble
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
package org.bonitasoft.engine.api.impl.resolver;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.api.impl.transaction.dependency.AddSDependency;
import org.bonitasoft.engine.bpm.bar.BusinessArchive;
import org.bonitasoft.engine.bpm.process.ConfigurationState;
import org.bonitasoft.engine.builder.BuilderFactory;
import org.bonitasoft.engine.commons.exceptions.SBonitaException;
import org.bonitasoft.engine.core.process.definition.ProcessDefinitionService;
import org.bonitasoft.engine.core.process.definition.model.SProcessDefinition;
import org.bonitasoft.engine.core.process.definition.model.SProcessDefinitionDeployInfo;
import org.bonitasoft.engine.core.process.definition.model.builder.SProcessDefinitionDeployInfoUpdateBuilderFactory;
import org.bonitasoft.engine.dependency.DependencyService;
import org.bonitasoft.engine.dependency.SDependencyCreationException;
import org.bonitasoft.engine.dependency.SDependencyException;
import org.bonitasoft.engine.dependency.model.SDependency;
import org.bonitasoft.engine.dependency.model.ScopeType;
import org.bonitasoft.engine.exception.BonitaException;
import org.bonitasoft.engine.exception.BonitaHomeNotSetException;
import org.bonitasoft.engine.exception.BonitaRuntimeException;
import org.bonitasoft.engine.home.BonitaHomeServer;
import org.bonitasoft.engine.log.technical.TechnicalLogSeverity;
import org.bonitasoft.engine.log.technical.TechnicalLoggerService;
import org.bonitasoft.engine.persistence.QueryOptions;
import org.bonitasoft.engine.recorder.model.EntityUpdateDescriptor;
import org.bonitasoft.engine.service.TenantServiceAccessor;

/**
 * Handles the resolution of Process Dependencies. A process can have a list of <code>ProcessDependencyResolver</code>s which validates different aspects of the
 * process to validate (or "resolve")
 * 
 * @author Emmanuel Duchastenier
 * @author Matthieu Chaffotte
 * @author Celine Souchet
 */
public class DependencyResolver {

    private final List<ProcessDependencyResolver> dependencyResolvers;

    public DependencyResolver(final List<ProcessDependencyResolver> dependencyResolvers) {
        this.dependencyResolvers = dependencyResolvers;
    }

    public boolean resolveDependencies(final ProcessAPI processAPI, final BusinessArchive businessArchive, final TenantServiceAccessor tenantAccessor,
            final SProcessDefinition sDefinition) {
        final List<ProcessDependencyResolver> resolvers = getResolvers();
        boolean resolved = true;
        for (final ProcessDependencyResolver resolver : resolvers) {
            try {
                resolved &= resolver.resolve(processAPI, tenantAccessor, businessArchive, sDefinition);
            } catch (final BonitaException e) {
                // not logged, we will check later why the process is not resolved
                resolved = false;
            }
        }
        return resolved;
    }

    /*
     * Done in a separated transaction
     * We try here to check if now the process is resolved so it must not be done in the same transaction that did the modification
     * this does not throw exception, it only log because it can be retried after.
     */
    public void resolveDependencies(final long processDefinitionId, final TenantServiceAccessor tenantAccessor) {
        final List<ProcessDependencyResolver> resolvers = getResolvers();
        resolveDependencies(processDefinitionId, tenantAccessor, resolvers.toArray(new ProcessDependencyResolver[resolvers.size()]));
    }

    public void resolveDependencies(final long processDefinitionId, final TenantServiceAccessor tenantAccessor, final ProcessDependencyResolver... resolvers) {
        final TechnicalLoggerService loggerService = tenantAccessor.getTechnicalLoggerService();
        final ProcessDefinitionService processDefinitionService = tenantAccessor.getProcessDefinitionService();
        final DependencyService dependencyService = tenantAccessor.getDependencyService();
        try {
            boolean resolved = true;
            for (final ProcessDependencyResolver dependencyResolver : resolvers) {
                final SProcessDefinition processDefinition = processDefinitionService.getProcessDefinition(processDefinitionId);
                resolved &= dependencyResolver.checkResolution(tenantAccessor, processDefinition).isEmpty();
            }
            changeResolutionStatus(processDefinitionId, tenantAccessor, processDefinitionService, dependencyService, resolved);
        } catch (final SBonitaException e) {
            final Class<DependencyResolver> clazz = DependencyResolver.class;
            if (loggerService.isLoggable(clazz, TechnicalLogSeverity.DEBUG)) {
                loggerService.log(clazz, TechnicalLogSeverity.DEBUG, e);
            }
            if (loggerService.isLoggable(clazz, TechnicalLogSeverity.WARNING)) {
                loggerService.log(clazz, TechnicalLogSeverity.WARNING, "Unable to resolve dependencies after they were modified because of " + e.getMessage()
                        + ". Please retry it manually");
            }
        } catch (final BonitaHomeNotSetException e) {
            throw new BonitaRuntimeException("Bonita home not set", e);
        }
    }

    private void changeResolutionStatus(final long processDefinitionId, final TenantServiceAccessor tenantAccessor,
            final ProcessDefinitionService processDefinitionService, final DependencyService dependencyService,
            final boolean resolved) throws SBonitaException, BonitaHomeNotSetException {
        final SProcessDefinitionDeployInfo processDefinitionDeployInfo = processDefinitionService.getProcessDeploymentInfo(processDefinitionId);
        if (resolved) {
            if (ConfigurationState.UNRESOLVED.name().equals(processDefinitionDeployInfo.getConfigurationState())) {
                resolveAndCreateDependencies(
                        new File(new File(BonitaHomeServer.getInstance().getProcessesFolder(tenantAccessor.getTenantId())), String.valueOf(processDefinitionId)),
                        processDefinitionService, dependencyService, processDefinitionId);
            }
        } else {
            if (ConfigurationState.RESOLVED.name().equals(processDefinitionDeployInfo.getConfigurationState())) {
                final EntityUpdateDescriptor updateDescriptor = BuilderFactory.get(SProcessDefinitionDeployInfoUpdateBuilderFactory.class).createNewInstance()
                        .updateConfigurationState(ConfigurationState.UNRESOLVED).done();
                processDefinitionService.updateProcessDefinitionDeployInfo(processDefinitionId, updateDescriptor);
            }
        }
    }

    /**
     * create dependencies based on the bonita home (the process folder)
     * 
     * @param processFolder
     * @param processDefinitionService
     * @param dependencyService
     * @param dependencyBuilderAccessor
     * @param processDefinitionId
     * @throws SBonitaException
     */
    public void resolveAndCreateDependencies(final File processFolder, final ProcessDefinitionService processDefinitionService,
            final DependencyService dependencyService, final long processDefinitionId) throws SBonitaException {
        final Map<String, byte[]> resources = new HashMap<String, byte[]>();

        final File file = new File(processFolder, "classpath");
        if (file.exists() && file.isDirectory()) {
            final File[] listFiles = file.listFiles();
            for (final File jarFile : listFiles) {
                final String name = jarFile.getName();
                try {
                    final byte[] jarContent = FileUtils.readFileToByteArray(jarFile);
                    resources.put(name, jarContent);
                } catch (final IOException e) {
                    throw new SDependencyCreationException(e);
                }
            }
        }
        addDependencies(resources, dependencyService, processDefinitionId);
        processDefinitionService.resolveProcess(processDefinitionId);
    }

    private String getDependencyName(final long processDefinitionId, final String name) {
        return processDefinitionId + "_" + name;
    }

    private void addDependencies(final Map<String, byte[]> resources, final DependencyService dependencyService,
            final long processDefinitionId) throws SBonitaException {
        final List<Long> dependencyIds = getDependencyMappingsOfProcess(dependencyService, processDefinitionId);
        final List<String> dependencies = getDependenciesOfProcess(dependencyService, dependencyIds);

        final Iterator<Entry<String, byte[]>> iterator = resources.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<java.lang.String, byte[]> entry = iterator.next();
            if (!dependencies.contains(getDependencyName(processDefinitionId, entry.getKey()))) {
                addDependency(entry.getKey(), entry.getValue(), dependencyService, processDefinitionId);
            }
        }

    }

    private List<String> getDependenciesOfProcess(final DependencyService dependencyService, final List<Long> dependencyIds) throws SBonitaException {
        if (dependencyIds.isEmpty()) {
            return Collections.emptyList();
        }
        final List<SDependency> dependencies = dependencyService.getDependencies(dependencyIds);
        final ArrayList<String> dependencyNames = new ArrayList<String>(dependencies.size());
        for (final SDependency sDependency : dependencies) {
            dependencyNames.add(sDependency.getName());
        }
        return dependencyNames;
    }

    private List<Long> getDependencyMappingsOfProcess(final DependencyService dependencyService, final long processDefinitionId) throws SDependencyException {
        final List<Long> dependencyIds = new ArrayList<Long>();
        QueryOptions queryOptions = QueryOptions.defaultQueryOptions();
        List<Long> currentPage;
        do {
            currentPage = dependencyService.getDependencyIds(processDefinitionId, ScopeType.PROCESS, queryOptions);
            dependencyIds.addAll(currentPage);
            queryOptions = QueryOptions.getNextPage(queryOptions);
        } while (currentPage.size() == QueryOptions.DEFAULT_NUMBER_OF_RESULTS);
        return dependencyIds;
    }

    /**
     * create dependencies based on the business archive
     * 
     * @param businessArchive
     * @param processDefinitionService
     * @param dependencyService
     * @param dependencyBuilderAccessor
     * @param sDefinition
     * @throws SBonitaException
     */
    public void resolveAndCreateDependencies(final BusinessArchive businessArchive, final ProcessDefinitionService processDefinitionService,
            final DependencyService dependencyService, final SProcessDefinition sDefinition) throws SBonitaException {
        final Long processDefinitionId = sDefinition.getId();
        if (businessArchive != null) {
            final Map<String, byte[]> resources = businessArchive.getResources("^classpath/.*$");

            // remove the classpath/ on path of dependencies
            final Map<String, byte[]> resourcesWithRealName = new HashMap<String, byte[]>(resources.size());
            for (final Entry<String, byte[]> resource : resources.entrySet()) {
                final String name = resource.getKey().substring(10);
                final byte[] jarContent = resource.getValue();
                resourcesWithRealName.put(name, jarContent);
            }
            addDependencies(resourcesWithRealName, dependencyService, sDefinition.getId());
        }
        processDefinitionService.resolveProcess(processDefinitionId);
    }

    private void addDependency(final String name, final byte[] jarContent, final DependencyService dependencyService,
            final long processdefinitionId) throws SDependencyException {
        final AddSDependency addSDependency = new AddSDependency(dependencyService, name, jarContent, processdefinitionId, ScopeType.PROCESS);
        addSDependency.execute();
    }

    public List<ProcessDependencyResolver> getResolvers() {
        return dependencyResolvers;
    }
}
