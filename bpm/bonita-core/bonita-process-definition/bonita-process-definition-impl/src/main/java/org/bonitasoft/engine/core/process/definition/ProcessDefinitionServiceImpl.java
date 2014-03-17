/**
 * Copyright (C) 2011-2014 BonitaSoft S.A.
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
package org.bonitasoft.engine.core.process.definition;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.bonitasoft.engine.bpm.process.ActivationState;
import org.bonitasoft.engine.bpm.process.ConfigurationState;
import org.bonitasoft.engine.bpm.process.ProcessDeploymentInfoCriterion;
import org.bonitasoft.engine.builder.BuilderFactory;
import org.bonitasoft.engine.cache.CacheException;
import org.bonitasoft.engine.cache.CacheService;
import org.bonitasoft.engine.commons.ClassReflector;
import org.bonitasoft.engine.commons.NullCheckingUtil;
import org.bonitasoft.engine.commons.ReflectException;
import org.bonitasoft.engine.core.process.definition.exception.SDeletingEnabledProcessException;
import org.bonitasoft.engine.core.process.definition.exception.SProcessDefinitionReadException;
import org.bonitasoft.engine.core.process.definition.exception.SProcessDeletionException;
import org.bonitasoft.engine.core.process.definition.exception.SProcessDeploymentInfoUpdateException;
import org.bonitasoft.engine.core.process.definition.exception.SProcessDisablementException;
import org.bonitasoft.engine.core.process.definition.exception.SProcessEnablementException;
import org.bonitasoft.engine.core.process.definition.model.SFlowElementContainerDefinition;
import org.bonitasoft.engine.core.process.definition.model.SFlowNodeDefinition;
import org.bonitasoft.engine.core.process.definition.model.SProcessDefinition;
import org.bonitasoft.engine.core.process.definition.model.SProcessDefinitionDeployInfo;
import org.bonitasoft.engine.core.process.definition.model.STransitionDefinition;
import org.bonitasoft.engine.core.process.definition.model.builder.SProcessDefinitionBuilderFactory;
import org.bonitasoft.engine.core.process.definition.model.builder.SProcessDefinitionDeployInfoBuilderFactory;
import org.bonitasoft.engine.core.process.definition.model.builder.SProcessDefinitionLogBuilder;
import org.bonitasoft.engine.core.process.definition.model.builder.SProcessDefinitionLogBuilderFactory;
import org.bonitasoft.engine.dependency.DependencyService;
import org.bonitasoft.engine.dependency.SDependencyDeletionException;
import org.bonitasoft.engine.dependency.SDependencyException;
import org.bonitasoft.engine.dependency.SDependencyNotFoundException;
import org.bonitasoft.engine.dependency.model.ScopeType;
import org.bonitasoft.engine.events.EventActionType;
import org.bonitasoft.engine.events.EventService;
import org.bonitasoft.engine.events.model.SDeleteEvent;
import org.bonitasoft.engine.events.model.SInsertEvent;
import org.bonitasoft.engine.events.model.SUpdateEvent;
import org.bonitasoft.engine.events.model.builders.SEventBuilderFactory;
import org.bonitasoft.engine.exception.BonitaRuntimeException;
import org.bonitasoft.engine.home.BonitaHomeServer;
import org.bonitasoft.engine.identity.model.SUser;
import org.bonitasoft.engine.persistence.OrderByType;
import org.bonitasoft.engine.persistence.QueryOptions;
import org.bonitasoft.engine.persistence.ReadPersistenceService;
import org.bonitasoft.engine.persistence.SBonitaReadException;
import org.bonitasoft.engine.persistence.SBonitaSearchException;
import org.bonitasoft.engine.persistence.SelectListDescriptor;
import org.bonitasoft.engine.persistence.SelectOneDescriptor;
import org.bonitasoft.engine.queriablelogger.model.SQueriableLog;
import org.bonitasoft.engine.queriablelogger.model.SQueriableLogSeverity;
import org.bonitasoft.engine.queriablelogger.model.builder.ActionType;
import org.bonitasoft.engine.queriablelogger.model.builder.HasCRUDEAction;
import org.bonitasoft.engine.queriablelogger.model.builder.SLogBuilder;
import org.bonitasoft.engine.queriablelogger.model.builder.SPersistenceLogBuilder;
import org.bonitasoft.engine.recorder.Recorder;
import org.bonitasoft.engine.recorder.SRecorderException;
import org.bonitasoft.engine.recorder.model.DeleteRecord;
import org.bonitasoft.engine.recorder.model.EntityUpdateDescriptor;
import org.bonitasoft.engine.recorder.model.InsertRecord;
import org.bonitasoft.engine.recorder.model.UpdateRecord;
import org.bonitasoft.engine.services.QueriableLoggerService;
import org.bonitasoft.engine.session.SSessionNotFoundException;
import org.bonitasoft.engine.session.SessionService;
import org.bonitasoft.engine.session.model.SSession;
import org.bonitasoft.engine.sessionaccessor.ReadSessionAccessor;
import org.bonitasoft.engine.sessionaccessor.SessionIdNotSetException;
import org.bonitasoft.engine.xml.ElementBindingsFactory;
import org.bonitasoft.engine.xml.Parser;
import org.bonitasoft.engine.xml.ParserFactory;
import org.bonitasoft.engine.xml.XMLWriter;

/**
 * @author Baptiste Mesta
 * @author Matthieu Chaffotte
 * @author Zhao Na
 * @author Yanyan Liu
 * @author Hongwen Zang
 * @author Celine Souchet
 * @author Arthur Freycon
 */
public class ProcessDefinitionServiceImpl implements ProcessDefinitionService {

    private static final String SERVER_PROCESS_DEFINITION_XML = "server-process-definition.xml";

    private final CacheService cacheService;

    private final Recorder recorder;

    private final ReadPersistenceService persistenceService;

    private final EventService eventService;

    private final SessionService sessionService;

    private final ReadSessionAccessor sessionAccessor;

    private final Parser parser;

    private final XMLWriter xmlWriter;

    private final QueriableLoggerService queriableLoggerService;

    private final DependencyService dependencyService;

    public ProcessDefinitionServiceImpl(final CacheService cacheService, final Recorder recorder, final ReadPersistenceService persistenceService,
            final EventService eventService, final SessionService sessionService,
            final ReadSessionAccessor sessionAccessor, final ParserFactory parserFactory,
            final XMLWriter xmlWriter,
            final QueriableLoggerService queriableLoggerService, final DependencyService dependencyService) {
        this.cacheService = cacheService;
        this.recorder = recorder;
        this.persistenceService = persistenceService;
        this.eventService = eventService;
        this.sessionService = sessionService;
        this.sessionAccessor = sessionAccessor;
        this.xmlWriter = xmlWriter;
        this.queriableLoggerService = queriableLoggerService;
        this.dependencyService = dependencyService;
        final ElementBindingsFactory bindings = BuilderFactory.get(SProcessDefinitionBuilderFactory.class).getElementsBindings();// FIXME
        parser = parserFactory.createParser(bindings);
        final InputStream schemaStream = BuilderFactory.get(SProcessDefinitionBuilderFactory.class).getModelSchema();
        try {
            parser.setSchema(schemaStream);
        } catch (final Exception e) {
            throw new BonitaRuntimeException("Unable to configure process definition service", e);
        }
    }

    @Override
    public void delete(final long processId) throws SProcessDefinitionNotFoundException, SProcessDeletionException, SDeletingEnabledProcessException {
        final SProcessDefinitionLogBuilder logBuilder = getQueriableLog(ActionType.DELETED, "Deleting a Process definition");
        try {
            final SProcessDefinitionDeployInfo processDefinitionDeployInfo = getProcessDeploymentInfo(processId);
            if (ActivationState.ENABLED.name().equals(processDefinitionDeployInfo.getActivationState())) {
                throw new SDeletingEnabledProcessException("Process with id " + processId + " is enabled");
            }
            cacheService.remove(PROCESS_CACHE_NAME, processId);
            SDeleteEvent deleteEvent = null;
            if (eventService.hasHandlers(PROCESSDEFINITION, EventActionType.DELETED)) {
                deleteEvent = (SDeleteEvent) BuilderFactory.get(SEventBuilderFactory.class).createDeleteEvent(PROCESSDEFINITION)
                        .setObject(processDefinitionDeployInfo).done();
            }
            final DeleteRecord deleteRecord = new DeleteRecord(processDefinitionDeployInfo);
            recorder.recordDelete(deleteRecord, deleteEvent);
            initiateLogBuilder(processId, SQueriableLog.STATUS_OK, logBuilder, "delete");
            dependencyService.deleteDependencies(processId, ScopeType.PROCESS);
        } catch (final CacheException e) {
            initiateLogBuilder(processId, SQueriableLog.STATUS_FAIL, logBuilder, "delete");
            throw new SProcessDefinitionNotFoundException(e);
        } catch (final SProcessDefinitionReadException e) {
            initiateLogBuilder(processId, SQueriableLog.STATUS_FAIL, logBuilder, "delete");
            throw new SProcessDeletionException(e);
        } catch (final SRecorderException e) {
            initiateLogBuilder(processId, SQueriableLog.STATUS_FAIL, logBuilder, "delete");
            throw new SProcessDeletionException(e);
        } catch (final SDependencyNotFoundException e) {
            initiateLogBuilder(processId, SQueriableLog.STATUS_FAIL, logBuilder, "delete");
            throw new SProcessDeletionException(e);
        } catch (final SDependencyDeletionException e) {
            initiateLogBuilder(processId, SQueriableLog.STATUS_FAIL, logBuilder, "delete");
            throw new SProcessDeletionException(e);
        } catch (final SDependencyException e) {
            initiateLogBuilder(processId, SQueriableLog.STATUS_FAIL, logBuilder, "delete");
            throw new SProcessDeletionException(e);
        }
    }

    @Override
    public void disableProcessDeploymentInfo(final long processId) throws SProcessDefinitionNotFoundException, SProcessDisablementException {
        SProcessDefinitionDeployInfo processDefinitionDeployInfo;
        try {
            processDefinitionDeployInfo = getProcessDeploymentInfo(processId);
        } catch (final SProcessDefinitionReadException e) {
            throw new SProcessDisablementException(e);
        }
        if (ActivationState.DISABLED.name().equals(processDefinitionDeployInfo.getActivationState())) {
            final StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Process ");
            stringBuilder.append(processDefinitionDeployInfo.getName());
            stringBuilder.append(" with version ");
            stringBuilder.append(processDefinitionDeployInfo.getVersion());
            stringBuilder.append(" is already disabled");
            throw new SProcessDisablementException(stringBuilder.toString());
        }

        final EntityUpdateDescriptor descriptor = new EntityUpdateDescriptor();
        descriptor.addField(BuilderFactory.get(SProcessDefinitionDeployInfoBuilderFactory.class).getActivationStateKey(), ActivationState.DISABLED.name());

        final UpdateRecord updateRecord = getUpdateRecord(descriptor, processDefinitionDeployInfo);
        final SPersistenceLogBuilder logBuilder = getQueriableLog(ActionType.UPDATED, "Disabling the process");

        SUpdateEvent updateEvent = null;
        if (eventService.hasHandlers(PROCESSDEFINITION_IS_DISABLED, EventActionType.UPDATED)) {
            updateEvent = (SUpdateEvent) BuilderFactory.get(SEventBuilderFactory.class).createUpdateEvent(PROCESSDEFINITION_IS_DISABLED)
                    .setObject(processDefinitionDeployInfo)
                    .done();
        }
        try {
            recorder.recordUpdate(updateRecord, updateEvent);
            initiateLogBuilder(processDefinitionDeployInfo.getId(), SQueriableLog.STATUS_OK, logBuilder, "disableProcess");
        } catch (final SRecorderException e) {
            initiateLogBuilder(processDefinitionDeployInfo.getId(), SQueriableLog.STATUS_FAIL, logBuilder, "disableProcess");
            throw new SProcessDisablementException(e);
        }
    }

    @Override
    public void enableProcessDeploymentInfo(final long processId) throws SProcessDefinitionNotFoundException, SProcessEnablementException {
        SProcessDefinitionDeployInfo processDefinitionDeployInfo;
        try {
            processDefinitionDeployInfo = getProcessDeploymentInfo(processId);
        } catch (final SProcessDefinitionReadException e) {
            throw new SProcessEnablementException(e);
        }
        if (ActivationState.ENABLED.name().equals(processDefinitionDeployInfo.getActivationState())) {
            final StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Process ");
            stringBuilder.append(processDefinitionDeployInfo.getName());
            stringBuilder.append(" with version ");
            stringBuilder.append(processDefinitionDeployInfo.getVersion());
            stringBuilder.append(" is already enabled");
            throw new SProcessEnablementException(stringBuilder.toString());
        }
        if (ConfigurationState.UNRESOLVED.name().equals(processDefinitionDeployInfo.getConfigurationState())) {
            final StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Process ");
            stringBuilder.append(processDefinitionDeployInfo.getName());
            stringBuilder.append(" with version ");
            stringBuilder.append(processDefinitionDeployInfo.getVersion());
            stringBuilder.append(" can't be enabled since all dependencies are not resolved yet");
            throw new SProcessEnablementException(stringBuilder.toString());
        }
        final EntityUpdateDescriptor descriptor = new EntityUpdateDescriptor();
        descriptor.addField(BuilderFactory.get(SProcessDefinitionDeployInfoBuilderFactory.class).getActivationStateKey(), ActivationState.ENABLED.name());

        final UpdateRecord updateRecord = getUpdateRecord(descriptor, processDefinitionDeployInfo);
        final SPersistenceLogBuilder logBuilder = getQueriableLog(ActionType.UPDATED, "Enabling the process");
        SUpdateEvent updateEvent = null;
        if (eventService.hasHandlers(PROCESSDEFINITION_IS_ENABLED, EventActionType.UPDATED)) {
            updateEvent = (SUpdateEvent) BuilderFactory.get(SEventBuilderFactory.class).createUpdateEvent(PROCESSDEFINITION_IS_ENABLED)
                    .setObject(processDefinitionDeployInfo)
                    .done();
        }
        try {
            recorder.recordUpdate(updateRecord, updateEvent);
            initiateLogBuilder(processId, SQueriableLog.STATUS_OK, logBuilder, "enableProcess");
        } catch (final SRecorderException e) {
            initiateLogBuilder(processId, SQueriableLog.STATUS_FAIL, logBuilder, "enableProcess");
            throw new SProcessEnablementException(e);
        }
    }

    private SProcessDefinitionLogBuilder getQueriableLog(final ActionType actionType, final String message) {
        final SProcessDefinitionLogBuilder logBuilder = BuilderFactory.get(SProcessDefinitionLogBuilderFactory.class).createNewInstance();
        this.initializeLogBuilder(logBuilder, message);
        this.updateLog(actionType, logBuilder);
        return logBuilder;
    }

    @Override
    public List<SProcessDefinitionDeployInfo> getProcessDeploymentInfos(final int fromIndex, final int numberPerPage, final String field,
            final OrderByType order) throws SProcessDefinitionReadException {
        try {
            final Map<String, Object> emptyMap = Collections.emptyMap();
            final List<SProcessDefinitionDeployInfo> processes = persistenceService
                    .selectList(new SelectListDescriptor<SProcessDefinitionDeployInfo>("", emptyMap, SProcessDefinitionDeployInfo.class, new QueryOptions(
                            fromIndex, numberPerPage, SProcessDefinitionDeployInfo.class, field, order)));
            return processes;
        } catch (final SBonitaReadException e) {
            throw new SProcessDefinitionReadException(e);
        }
    }

    @Override
    public long getNumberOfProcessDeploymentInfos() throws SProcessDefinitionReadException {
        final Map<String, Object> parameters = Collections.emptyMap();
        final SelectOneDescriptor<Long> selectDescriptor = new SelectOneDescriptor<Long>("getNumberOfProcessDefinitions", parameters,
                SProcessDefinitionDeployInfo.class);
        try {
            return persistenceService.selectOne(selectDescriptor);
        } catch (final SBonitaReadException bre) {
            throw new SProcessDefinitionReadException(bre);
        }
    }

    @Override
    public SProcessDefinition getProcessDefinition(final long processId) throws SProcessDefinitionNotFoundException, SProcessDefinitionReadException {
        try {
            final long tenantId = sessionAccessor.getTenantId();
            SProcessDefinition sProcessDefinition = null;
            sProcessDefinition = (SProcessDefinition) cacheService.get(PROCESS_CACHE_NAME, processId);
            if (sProcessDefinition == null) {
                getProcessDeploymentInfo(processId);
                final String processesFolder = BonitaHomeServer.getInstance().getProcessesFolder(tenantId);
                final File processFolder = new File(processesFolder, String.valueOf(processId));
                // read it from the saved XML file server side
                final File xmlFile = new File(processFolder, SERVER_PROCESS_DEFINITION_XML);
                parser.validate(xmlFile);
                sProcessDefinition = (SProcessDefinition) parser.getObjectFromXML(xmlFile);
                storeProcessDefinition(processId, sProcessDefinition);
            }
            return sProcessDefinition;
        } catch (final CacheException e) {
            throw new SProcessDefinitionNotFoundException(e);
        } catch (final SProcessDefinitionNotFoundException e) {
            throw e;
        } catch (final Exception e) {
            throw new SProcessDefinitionReadException(e);
        }
    }

    private long setIdOnProcessDefinition(final SProcessDefinition sProcessDefinition) throws ReflectException {
        final long id = generateId();
        ClassReflector.invokeSetter(sProcessDefinition, "setId", Long.class, id);
        return id;
    }

    protected long generateId() {
        return Math.abs(UUID.randomUUID().getLeastSignificantBits());
    }

    private void storeProcessDefinition(final Long id, final SProcessDefinition sProcessDefinition) throws CacheException {
        cacheService.store(PROCESS_CACHE_NAME, id, sProcessDefinition);
    }

    @Override
    public SProcessDefinitionDeployInfo getProcessDeploymentInfo(final long processId) throws SProcessDefinitionNotFoundException,
            SProcessDefinitionReadException {
        try {
            final Map<String, Object> parameters = Collections.singletonMap("processId", (Object) processId);
            final SelectOneDescriptor<SProcessDefinitionDeployInfo> descriptor = new SelectOneDescriptor<SProcessDefinitionDeployInfo>(
                    "getDeployInfoByProcessDefId", parameters, SProcessDefinitionDeployInfo.class);
            final SProcessDefinitionDeployInfo processDefinitionDeployInfo = persistenceService.selectOne(descriptor);
            if (processDefinitionDeployInfo == null) {
                throw new SProcessDefinitionNotFoundException("Unable to find the process definition deployment info with process id " + processId);
            }
            return processDefinitionDeployInfo;
        } catch (final SBonitaReadException e) {
            throw new SProcessDefinitionReadException(e);
        }
    }

    private <T extends SLogBuilder> void initializeLogBuilder(final T logBuilder, final String message) {
        logBuilder.actionStatus(SQueriableLog.STATUS_FAIL).severity(SQueriableLogSeverity.INTERNAL).rawMessage(message);
    }

    @Override
    public SProcessDefinition store(final SProcessDefinition definition, String displayName, String displayDescription) throws SProcessDefinitionException {
        NullCheckingUtil.checkArgsNotNull(definition);
        final SProcessDefinitionLogBuilder logBuilder = getQueriableLog(ActionType.CREATED, "Creating a new Process definition");
        try {
            final SSession session = getSession();
            final long tenantId = sessionAccessor.getTenantId();
            final long processId = setIdOnProcessDefinition(definition);
            // storeProcessDefinition(processId, tenantId, definition);// FIXME remove that to check the read of processes

            final String processesFolder = BonitaHomeServer.getInstance().getProcessesFolder(tenantId);
            final File processFolder = new File(processesFolder, String.valueOf(processId));
            if (!processFolder.exists()) {
                processFolder.mkdirs();
                processFolder.mkdir();
            }
            final FileOutputStream outputStream = new FileOutputStream(new File(processFolder, SERVER_PROCESS_DEFINITION_XML));
            try {
                xmlWriter.write(BuilderFactory.get(SProcessDefinitionBuilderFactory.class).getXMLProcessDefinition(definition), outputStream);
            } finally {
                outputStream.close();
            }

            if (displayName == null || displayName.isEmpty()) {
                displayName = definition.getName();
            }
            if (displayDescription == null || displayDescription.isEmpty()) {
                displayDescription = definition.getDescription();
            }
            final SProcessDefinitionDeployInfo definitionDeployInfo = BuilderFactory.get(SProcessDefinitionDeployInfoBuilderFactory.class)
                    .createNewInstance(definition.getName(), definition.getVersion()).setProcessId(processId).setDescription(definition.getDescription())
                    .setDeployedBy(session.getUserId()).setDeploymentDate(System.currentTimeMillis()).setActivationState(ActivationState.DISABLED.name())
                    .setConfigurationState(ConfigurationState.UNRESOLVED.name()).setDisplayName(displayName).setDisplayDescription(displayDescription).done();

            final InsertRecord record = new InsertRecord(definitionDeployInfo);
            SInsertEvent insertEvent = null;
            if (eventService.hasHandlers(PROCESSDEFINITION, EventActionType.CREATED)) {
                insertEvent = (SInsertEvent) BuilderFactory.get(SEventBuilderFactory.class).createInsertEvent(PROCESSDEFINITION)
                        .setObject(definitionDeployInfo).done();
            }
            recorder.recordInsert(record, insertEvent);
            initiateLogBuilder(definition.getId(), SQueriableLog.STATUS_OK, logBuilder, "store");
        } catch (final SRecorderException e) {
            initiateLogBuilder(definition.getId(), SQueriableLog.STATUS_FAIL, logBuilder, "store");
            throw new SProcessDefinitionException(e);
        } catch (final Exception e) {
            initiateLogBuilder(definition.getId(), SQueriableLog.STATUS_FAIL, logBuilder, "store");
            throw new SProcessDefinitionException(e);
        }
        return definition;
    }

    private SSession getSession() throws SSessionNotFoundException {
        long sessionId;
        try {
            sessionId = sessionAccessor.getSessionId();
        } catch (SessionIdNotSetException e) {
            // system
            return null;
        }
        final SSession session = sessionService.getSession(sessionId);
        return session;
    }

    private <T extends HasCRUDEAction> void updateLog(final ActionType actionType, final T logBuilder) {
        logBuilder.setActionType(actionType);
    }

    @Override
    public void resolveProcess(final long processId) throws SProcessDefinitionNotFoundException, SProcessDisablementException {
        SProcessDefinitionDeployInfo processDefinitionDeployInfo;
        try {
            processDefinitionDeployInfo = getProcessDeploymentInfo(processId);
        } catch (final SProcessDefinitionReadException e) {
            throw new SProcessDisablementException(e);
        }
        if (!ConfigurationState.UNRESOLVED.name().equals(processDefinitionDeployInfo.getConfigurationState())) {
            final StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Process ");
            stringBuilder.append(processDefinitionDeployInfo.getName());
            stringBuilder.append(" with version");
            stringBuilder.append(processDefinitionDeployInfo.getVersion());
            stringBuilder.append(" is not unresolved");
            throw new SProcessDisablementException(stringBuilder.toString());
        }

        final EntityUpdateDescriptor descriptor = new EntityUpdateDescriptor();
        descriptor
                .addField(BuilderFactory.get(SProcessDefinitionDeployInfoBuilderFactory.class).getConfigurationStateKey(), ConfigurationState.RESOLVED.name());

        final UpdateRecord updateRecord = getUpdateRecord(descriptor, processDefinitionDeployInfo);
        final SPersistenceLogBuilder logBuilder = getQueriableLog(ActionType.UPDATED, "Resolved the process");

        SUpdateEvent updateEvent = null;
        if (eventService.hasHandlers(PROCESSDEFINITION_IS_RESOLVED, EventActionType.UPDATED)) {
            updateEvent = (SUpdateEvent) BuilderFactory.get(SEventBuilderFactory.class).createUpdateEvent(PROCESSDEFINITION_IS_RESOLVED)
                    .setObject(processDefinitionDeployInfo)
                    .done();
        }
        try {
            recorder.recordUpdate(updateRecord, updateEvent);
            initiateLogBuilder(processDefinitionDeployInfo.getId(), SQueriableLog.STATUS_OK, logBuilder, "resolveProcess");
        } catch (final SRecorderException e) {
            initiateLogBuilder(processDefinitionDeployInfo.getId(), SQueriableLog.STATUS_FAIL, logBuilder, "resolveProcess");
            throw new SProcessDisablementException(e);
        }
    }

    @Override
    public long getNumberOfProcessDeploymentInfo(final ActivationState activationState) throws SProcessDefinitionReadException {
        final Map<String, Object> parameters = Collections.singletonMap("activationState", (Object) activationState.name());
        final SelectOneDescriptor<Long> selectDescriptor = new SelectOneDescriptor<Long>("getNumberOfProcessDefinitionsInActivationState", parameters,
                SProcessDefinitionDeployInfo.class);
        try {
            return persistenceService.selectOne(selectDescriptor);
        } catch (final SBonitaReadException e) {
            throw new SProcessDefinitionReadException(e);
        }
    }

    @Override
    public List<Long> getProcessDefinitionIds(final ActivationState activationState, final int fromIndex, final long numberOfResults)
            throws SProcessDefinitionReadException {
        // FIXME
        final Map<String, Object> parameters = Collections.singletonMap("activationState", (Object) activationState.name());
        final SelectListDescriptor<Long> selectDescriptor = new SelectListDescriptor<Long>("getProcessDefinitionsIdsInActivationState", parameters,
                SProcessDefinitionDeployInfo.class, new QueryOptions(fromIndex, (int) numberOfResults));
        try {
            return persistenceService.selectList(selectDescriptor);
        } catch (final SBonitaReadException e) {
            throw new SProcessDefinitionReadException(e);
        }
    }

    @Override
    public List<Long> getProcessDefinitionIds(final int fromIndex, final long numberOfResults) throws SProcessDefinitionReadException {
        // FIXME
        final Map<String, Object> parameters = Collections.emptyMap();
        final SelectListDescriptor<Long> selectDescriptor = new SelectListDescriptor<Long>("getProcessDefinitionsIds", parameters,
                SProcessDefinitionDeployInfo.class, new QueryOptions(fromIndex, (int) numberOfResults));
        try {
            return persistenceService.selectList(selectDescriptor);
        } catch (final SBonitaReadException e) {
            throw new SProcessDefinitionReadException(e);
        }
    }

    @Override
    public SFlowNodeDefinition getNextFlowNode(final SProcessDefinition definition, final String source) {
        final SFlowElementContainerDefinition processContainer = definition.getProcessContainer();
        final STransitionDefinition sourceNode = processContainer.getTransition(source);
        final long targetId = sourceNode.getTarget();
        return processContainer.getFlowNode(targetId);
    }

    @Override
    public List<SProcessDefinitionDeployInfo> getProcessDeploymentInfos(final List<Long> processIds, final int fromIndex, final int numberOfProcesses,
            final String field, final OrderByType order) throws SProcessDefinitionReadException {
        if (processIds == null || processIds.size() == 0) {
            return Collections.emptyList();
        }
        try {
            final Map<String, Object> emptyMap = Collections.singletonMap("processIds", (Object) processIds);
            final QueryOptions queryOptions = new QueryOptions(fromIndex, numberOfProcesses, SProcessDefinitionDeployInfo.class, field, order);
            final List<SProcessDefinitionDeployInfo> results = persistenceService.selectList(new SelectListDescriptor<SProcessDefinitionDeployInfo>(
                    "getSubSetOfProcessDefinitionDeployInfos", emptyMap, SProcessDefinitionDeployInfo.class, queryOptions));
            return results;
        } catch (final SBonitaReadException e) {
            throw new SProcessDefinitionReadException(e);
        }
    }

    @Override
    public List<SProcessDefinitionDeployInfo> getProcessDeploymentInfos(final List<Long> processIds) throws SProcessDefinitionReadException {
        if (processIds == null || processIds.size() == 0) {
            return Collections.emptyList();
        }
        try {
            final Map<String, Object> emptyMap = Collections.singletonMap("processIds", (Object) processIds);
            final List<SProcessDefinitionDeployInfo> results = persistenceService.selectList(new SelectListDescriptor<SProcessDefinitionDeployInfo>(
                    "getSubSetOfProcessDefinitionDeployInfos", emptyMap, SProcessDefinitionDeployInfo.class));
            return results;
        } catch (final SBonitaReadException e) {
            throw new SProcessDefinitionReadException(e);
        }
    }

    @Override
    public long getLatestProcessDefinitionId(final String processName) throws SProcessDefinitionReadException {
        return getProcessDeploymentInfosByTimeDesc(processName).get(0).getProcessId();
    }

    private List<SProcessDefinitionDeployInfo> getProcessDeploymentInfosByTimeDesc(final String processName) throws SProcessDefinitionReadException {
        try {
            final Map<String, Object> parameters = Collections.singletonMap("name", (Object) processName);
            final SelectListDescriptor<SProcessDefinitionDeployInfo> selectDescriptor = new SelectListDescriptor<SProcessDefinitionDeployInfo>(
                    "getProcessDefinitionDeployInfosByTimeDesc", parameters, SProcessDefinitionDeployInfo.class);
            final List<SProcessDefinitionDeployInfo> processDefinitionDeployInfos = persistenceService.selectList(selectDescriptor);
            if (processDefinitionDeployInfos != null && processDefinitionDeployInfos.size() > 0) {
                return processDefinitionDeployInfos;
            } else {
                throw new SProcessDefinitionReadException("Unable to find the process definition deployment info with process name " + processName);
            }
        } catch (final SBonitaReadException e) {
            throw new SProcessDefinitionReadException(e);
        }
    }

    @Override
    public long getProcessDefinitionId(final String name, final String version) throws SProcessDefinitionReadException {
        try {
            final Map<String, Object> parameters = new HashMap<String, Object>();
            parameters.put("name", name);
            parameters.put("version", version);
            final Long processDefId = persistenceService.selectOne(new SelectOneDescriptor<Long>("getProcessDefinitionIdByNameAndVersion", parameters,
                    SProcessDefinitionDeployInfo.class, Long.class));
            if (processDefId != null) {
                return processDefId;
            } else {
                throw new SProcessDefinitionReadException("process definition id not found with name " + name + " and version " + version);
            }
        } catch (final SBonitaReadException e) {
            throw new SProcessDefinitionReadException(e);
        }
    }

    @Override
    public void updateProcessDefinitionDeployInfo(final long processId, final EntityUpdateDescriptor descriptor) throws SProcessDefinitionNotFoundException,
            SProcessDeploymentInfoUpdateException {
        SProcessDefinitionDeployInfo processDefinitionDeployInfo;
        try {
            processDefinitionDeployInfo = getProcessDeploymentInfo(processId);
        } catch (final SProcessDefinitionReadException e) {
            throw new SProcessDefinitionNotFoundException(e);
        }
        final SProcessDefinitionLogBuilder logBuilder = getQueriableLog(ActionType.UPDATED, "Updating a processDefinitionDeployInfo");
        final UpdateRecord updateRecord = getUpdateRecord(descriptor, processDefinitionDeployInfo);
        SUpdateEvent updateEvent = null;
        if (eventService.hasHandlers(PROCESSDEFINITION_DEPLOY_INFO, EventActionType.UPDATED)) {
            updateEvent = (SUpdateEvent) BuilderFactory.get(SEventBuilderFactory.class).createUpdateEvent(PROCESSDEFINITION_DEPLOY_INFO)
                    .setObject(processDefinitionDeployInfo).done();
        }
        try {
            recorder.recordUpdate(updateRecord, updateEvent);
            initiateLogBuilder(processId, SQueriableLog.STATUS_OK, logBuilder, "updateProcessDeploymentInfo");
        } catch (final SRecorderException e) {
            initiateLogBuilder(processId, SQueriableLog.STATUS_FAIL, logBuilder, "updateProcessDeploymentInfo");
            throw new SProcessDeploymentInfoUpdateException(e);
        }
    }

    private UpdateRecord getUpdateRecord(final EntityUpdateDescriptor descriptor, final SProcessDefinitionDeployInfo processDefinitionDeployInfo) {
        final long now = System.currentTimeMillis();
        descriptor.addField(BuilderFactory.get(SProcessDefinitionDeployInfoBuilderFactory.class).getLastUpdateDateKey(), now);
        return UpdateRecord.buildSetFields(processDefinitionDeployInfo, descriptor);
    }

    @Override
    public List<SProcessDefinitionDeployInfo> searchProcessDeploymentInfosStartedBy(final long startedBy, final QueryOptions searchOptions)
            throws SBonitaSearchException {
        try {
            final Map<String, Object> parameters = Collections.singletonMap("startedBy", (Object) startedBy);
            return persistenceService.searchEntity(SProcessDefinitionDeployInfo.class, STARTED_BY_SUFFIX, searchOptions, parameters);
        } catch (final SBonitaReadException bre) {
            throw new SBonitaSearchException(bre);
        }
    }

    @Override
    public long getNumberOfProcessDeploymentInfosStartedBy(final long startedBy, final QueryOptions countOptions) throws SBonitaSearchException {
        try {
            final Map<String, Object> parameters = Collections.singletonMap("startedBy", (Object) startedBy);
            return persistenceService.getNumberOfEntities(SProcessDefinitionDeployInfo.class, STARTED_BY_SUFFIX, countOptions, parameters);
        } catch (final SBonitaReadException bre) {
            throw new SBonitaSearchException(bre);
        }
    }

    @Override
    public List<SProcessDefinitionDeployInfo> searchProcessDeploymentInfos(final QueryOptions searchOptions) throws SBonitaSearchException {
        try {
            return persistenceService.searchEntity(SProcessDefinitionDeployInfo.class, searchOptions, null);
        } catch (final SBonitaReadException bre) {
            throw new SBonitaSearchException(bre);
        }
    }

    @Override
    public long getNumberOfProcessDeploymentInfos(final QueryOptions countOptions) throws SBonitaSearchException {
        try {
            return persistenceService.getNumberOfEntities(SProcessDefinitionDeployInfo.class, countOptions, null);
        } catch (final SBonitaReadException bre) {
            throw new SBonitaSearchException(bre);
        }
    }

    @Override
    public List<SProcessDefinitionDeployInfo> searchProcessDeploymentInfos(final long userId, final QueryOptions searchOptions) throws SBonitaSearchException {
        try {
            return persistenceService.searchEntity(SProcessDefinitionDeployInfo.class, "UserCanStart", searchOptions,
                    Collections.singletonMap(USER_ID, (Object) userId));
        } catch (final SBonitaReadException bre) {
            throw new SBonitaSearchException(bre);
        }
    }

    @Override
    public long getNumberOfProcessDeploymentInfos(final long userId, final QueryOptions countOptions) throws SBonitaSearchException {
        try {
            return persistenceService.getNumberOfEntities(SProcessDefinitionDeployInfo.class, "UserCanStart", countOptions,
                    Collections.singletonMap(USER_ID, (Object) userId));
        } catch (final SBonitaReadException bre) {
            throw new SBonitaSearchException(bre);
        }
    }

    @Override
    public List<SProcessDefinitionDeployInfo> searchProcessDeploymentInfosUsersManagedByCanStart(final long managerUserId, final QueryOptions searchOptions)
            throws SBonitaSearchException {
        try {
            return persistenceService.searchEntity(SProcessDefinitionDeployInfo.class, "UsersManagedByCanStart", searchOptions,
                    Collections.singletonMap("managerUserId", (Object) managerUserId));
        } catch (final SBonitaReadException bre) {
            throw new SBonitaSearchException(bre);
        }
    }

    @Override
    public long getNumberOfProcessDeploymentInfosUsersManagedByCanStart(final long managerUserId, final QueryOptions countOptions)
            throws SBonitaSearchException {
        try {
            return persistenceService.getNumberOfEntities(SProcessDefinitionDeployInfo.class, "UsersManagedByCanStart", countOptions,
                    Collections.singletonMap("managerUserId", (Object) managerUserId));
        } catch (final SBonitaReadException bre) {
            throw new SBonitaSearchException(bre);
        }
    }

    @Override
    public List<SProcessDefinitionDeployInfo> searchProcessDeploymentInfos(final long userId, final QueryOptions searchOptions, final String querySuffix)
            throws SBonitaSearchException {
        try {
            return persistenceService.searchEntity(SProcessDefinitionDeployInfo.class, querySuffix, searchOptions,
                    Collections.singletonMap(USER_ID, (Object) userId));
        } catch (final SBonitaReadException bre) {
            throw new SBonitaSearchException(bre);
        }
    }

    @Override
    public long getNumberOfProcessDeploymentInfos(final long userId, final QueryOptions countOptions, final String querySuffix) throws SBonitaSearchException {
        try {
            return persistenceService.getNumberOfEntities(SProcessDefinitionDeployInfo.class, querySuffix, countOptions,
                    Collections.singletonMap(USER_ID, (Object) userId));
        } catch (final SBonitaReadException bre) {
            throw new SBonitaSearchException(bre);
        }
    }

    @Override
    public long getNumberOfUncategorizedProcessDeploymentInfos(final QueryOptions countOptions) throws SBonitaSearchException {
        try {
            return persistenceService.getNumberOfEntities(SProcessDefinitionDeployInfo.class, UNCATEGORIZED_SUFFIX, countOptions, null);
        } catch (final SBonitaReadException bre) {
            throw new SBonitaSearchException(bre);
        }
    }

    @Override
    public List<SProcessDefinitionDeployInfo> searchUncategorizedProcessDeploymentInfos(final QueryOptions searchOptions) throws SBonitaSearchException {
        try {
            return persistenceService.searchEntity(SProcessDefinitionDeployInfo.class, UNCATEGORIZED_SUFFIX, searchOptions, null);
        } catch (final SBonitaReadException bre) {
            throw new SBonitaSearchException(bre);
        }
    }

    @Override
    public long getNumberOfUncategorizedProcessDeploymentInfosSupervisedBy(final long userId, final QueryOptions countOptions) throws SBonitaSearchException {
        try {
            return persistenceService.getNumberOfEntities(SProcessDefinitionDeployInfo.class, UNCATEGORIZED_SUPERVISED_BY_SUFFIX, countOptions,
                    Collections.singletonMap(USER_ID, (Object) userId));
        } catch (final SBonitaReadException bre) {
            throw new SBonitaSearchException(bre);
        }
    }

    @Override
    public List<SProcessDefinitionDeployInfo> searchUncategorizedProcessDeploymentInfosSupervisedBy(final long userId, final QueryOptions searchOptions)
            throws SBonitaSearchException {
        try {
            return persistenceService.searchEntity(SProcessDefinitionDeployInfo.class, UNCATEGORIZED_SUPERVISED_BY_SUFFIX, searchOptions,
                    Collections.singletonMap(USER_ID, (Object) userId));
        } catch (final SBonitaReadException bre) {
            throw new SBonitaSearchException(bre);
        }
    }

    @Override
    public List<SProcessDefinitionDeployInfo> searchUncategorizedProcessDeploymentInfosUserCanStart(final long userId, final QueryOptions searchOptions)
            throws SBonitaSearchException {
        try {
            return persistenceService.searchEntity(SProcessDefinitionDeployInfo.class, UNCATEGORIZED_USERCANSTART_SUFFIX, searchOptions,
                    Collections.singletonMap(USER_ID, (Object) userId));
        } catch (final SBonitaReadException bre) {
            throw new SBonitaSearchException(bre);
        }
    }

    @Override
    public long getNumberOfUncategorizedProcessDeploymentInfosUserCanStart(final long userId, final QueryOptions countOptions) throws SBonitaSearchException {
        try {
            return persistenceService.getNumberOfEntities(SProcessDefinitionDeployInfo.class, UNCATEGORIZED_USERCANSTART_SUFFIX, countOptions,
                    Collections.singletonMap(USER_ID, (Object) userId));
        } catch (final SBonitaReadException bre) {
            throw new SBonitaSearchException(bre);
        }
    }

    @Override
    public List<Map<String, String>> getProcessDeploymentInfosFromProcessInstanceIds(final List<Long> processInstanceIds) throws SBonitaSearchException {
        if (processInstanceIds == null || processInstanceIds.size() == 0) {
            return Collections.emptyList();
        }
        try {
            final Map<String, Object> parameters = Collections.singletonMap("processInstanceIds", (Object) processInstanceIds);
            final List<Map<String, String>> result = persistenceService.selectList(new SelectListDescriptor<Map<String, String>>(
                    "getProcessDeploymentInfoFromProcessInstanceIds", parameters, SProcessDefinitionDeployInfo.class));
            return result;
        } catch (final SBonitaReadException e) {
            throw new SBonitaSearchException(e);
        }
    }

    @Override
    public Map<Long, SProcessDefinitionDeployInfo> getProcessDeploymentInfosFromArchivedProcessInstanceIds(final List<Long> archivedProcessInstantsIds)
            throws SProcessDefinitionReadException {
        if (archivedProcessInstantsIds == null || archivedProcessInstantsIds.size() == 0) {
            return Collections.emptyMap();
        }
        try {
            final Map<String, Object> parameters = Collections.singletonMap("archivedProcessInstanceIds", (Object) archivedProcessInstantsIds);
            final List<Map<String, Object>> result = persistenceService.selectList(new SelectListDescriptor<Map<String, Object>>(
                    "getProcessDeploymentInfoFromArchivedProcessInstanceIds", parameters, SProcessDefinitionDeployInfo.class));
            if (result != null && result.size() > 0) {
                return getProcessDeploymentInfosFromMap(result);
            }
        } catch (final SBonitaReadException e) {
            throw new SProcessDefinitionReadException(e);
        }
        return Collections.emptyMap();
    }

    private Map<Long, SProcessDefinitionDeployInfo> getProcessDeploymentInfosFromMap(final List<Map<String, Object>> sProcessDeploymentInfos) {

        final Map<Long, SProcessDefinitionDeployInfo> mProcessDeploymentInfos = new HashMap<Long, SProcessDefinitionDeployInfo>();
        long archivedProcessInstanceId = 0;
        long id = 0;
        long processId = 0;
        String name = "";
        String version = "";
        String description = "";
        long deploymentDate = 0;
        long deployedBy = 0;
        String activationState = "";
        String configurationState = "";
        String displayName = "";
        long lastUpdateDate = 0;
        String iconPath = "";
        String displayDescription = "";
        final SProcessDefinitionDeployInfoBuilderFactory fact = BuilderFactory.get(SProcessDefinitionDeployInfoBuilderFactory.class);
        for (final Map<String, Object> sProcessDeploymentInfo : sProcessDeploymentInfos) {
            fact.createNewInstance(displayName, version);
            for (final Entry<String, Object> entry : sProcessDeploymentInfo.entrySet()) {
                final String key = entry.getKey();
                final Object value = entry.getValue();
                if ("archivedProcessInstanceId".equals(key)) {
                    archivedProcessInstanceId = (Long) value;
                } else if (fact.getIdKey().equals(key)) {
                    id = (Long) value;
                } else if (fact.getProcessIdKey().equals(key)) {
                    processId = (Long) value;
                } else if (fact.getNameKey().equals(key)) {
                    name = (String) value;
                } else if (fact.getVersionKey().equals(key)) {
                    version = (String) value;
                } else if (fact.getDescriptionKey().equals(key)) {
                    description = (String) value;
                } else if (fact.getDeploymentDateKey().equals(key)) {
                    deploymentDate = (Long) value;
                } else if (fact.getDeployedByKey().equals(key)) {
                    deployedBy = (Long) value;
                } else if (fact.getActivationStateKey().equals(key)) {
                    activationState = (String) value;
                } else if (fact.getConfigurationStateKey().equals(key)) {
                    configurationState = (String) value;
                } else if (fact.getDisplayNameKey().equals(key)) {
                    displayName = (String) value;
                } else if (fact.getLastUpdateDateKey().equals(key)) {
                    lastUpdateDate = (Long) value;
                } else if (fact.getIconPathKey().equals(key)) {
                    iconPath = (String) value;
                } else if (fact.getDisplayDescriptionKey().equals(key)) {
                    displayDescription = (String) value;
                }
            }

            final SProcessDefinitionDeployInfo info = fact.createNewInstance(name, version).setId(id).setDescription(description)
                    .setDisplayDescription(displayDescription)
                    .setActivationState(activationState).setConfigurationState(configurationState).setDeployedBy(deployedBy).setProcessId(processId)
                    .setLastUpdateDate(lastUpdateDate).setDisplayName(displayName).setDeploymentDate(deploymentDate).setIconPath(iconPath).done();
            mProcessDeploymentInfos.put(archivedProcessInstanceId, info);
        }
        return mProcessDeploymentInfos;
    }

    private void initiateLogBuilder(final long objectId, final int sQueriableLogStatus, final SPersistenceLogBuilder logBuilder, final String callerMethodName) {
        logBuilder.actionScope(String.valueOf(objectId));
        logBuilder.actionStatus(sQueriableLogStatus);
        logBuilder.objectId(objectId);
        final SQueriableLog log = logBuilder.done();
        if (queriableLoggerService.isLoggable(log.getActionType(), log.getSeverity())) {
            queriableLoggerService.log(this.getClass().getName(), callerMethodName, log);
        }
    }

    @Override
    public List<SProcessDefinitionDeployInfo> getProcessDeploymentInfosUnrelatedToCategory(final long categoryId, final int pageIndex, final int numberPerPage,
            final ProcessDeploymentInfoCriterion pagingCriterion) throws SProcessDefinitionReadException {
        final Map<String, Object> parameters = Collections.singletonMap("categoryId", (Object) categoryId);
        final QueryOptions queryOptions = createQueryOptions(pageIndex, numberPerPage, pagingCriterion);
        final SelectListDescriptor<SProcessDefinitionDeployInfo> selectDescriptor = new SelectListDescriptor<SProcessDefinitionDeployInfo>(
                "searchSProcessDefinitionDeployInfoUnrelatedToCategory", parameters, SProcessDefinitionDeployInfo.class, queryOptions);
        try {
            return persistenceService.selectList(selectDescriptor);
        } catch (final SBonitaReadException e) {
            throw new SProcessDefinitionReadException(e);
        }
    }

    @Override
    public Long getNumberOfProcessDeploymentInfosUnrelatedToCategory(final long categoryId) throws SProcessDefinitionReadException {
        final Map<String, Object> parameters = Collections.singletonMap("categoryId", (Object) categoryId);
        final SelectOneDescriptor<Long> selectDescriptor = new SelectOneDescriptor<Long>("getNumberOfSProcessDefinitionDeployInfoUnrelatedToCategory",
                parameters, SProcessDefinitionDeployInfo.class);
        try {
            return persistenceService.selectOne(selectDescriptor);
        } catch (final SBonitaReadException bre) {
            throw new SProcessDefinitionReadException(bre);
        }
    }

    private QueryOptions createQueryOptions(final int pageIndex, final int numberPerPage, final ProcessDeploymentInfoCriterion pagingCriterion) {
        String field = null;
        OrderByType order = null;
        final SProcessDefinitionDeployInfoBuilderFactory fact = BuilderFactory.get(SProcessDefinitionDeployInfoBuilderFactory.class);
        switch (pagingCriterion) {
            case DEFAULT:
                break;
            case LABEL_ASC:
                // field = processDefinitionDeployInfoKyeProvider.get
                // FIXME add label?
                break;
            case LABEL_DESC:
                break;
            case NAME_ASC:
                field = fact.getNameKey();
                order = OrderByType.ASC;
                break;
            case NAME_DESC:
                field = fact.getNameKey();
                order = OrderByType.DESC;
                break;
            case ACTIVATION_STATE_ASC:
                field = fact.getActivationStateKey();
                order = OrderByType.ASC;
                break;
            case ACTIVATION_STATE_DESC:
                field = fact.getActivationStateKey();
                order = OrderByType.DESC;
                break;
            case CONFIGURATION_STATE_ASC:
                field = fact.getConfigurationStateKey();
                order = OrderByType.ASC;
                break;
            case CONFIGURATION_STATE_DESC:
                field = fact.getConfigurationStateKey();
                order = OrderByType.DESC;
                break;
            case VERSION_ASC:
                field = fact.getVersionKey();
                order = OrderByType.ASC;
                break;
            case VERSION_DESC:
                field = fact.getVersionKey();
                order = OrderByType.DESC;
                break;
            default:
                break;
        }

        QueryOptions queryOptions;
        if (field == null) {
            queryOptions = new QueryOptions(pageIndex, numberPerPage);
        } else {
            queryOptions = new QueryOptions(pageIndex, numberPerPage, SProcessDefinitionDeployInfo.class, field, order);
        }
        return queryOptions;
    }

    @Override
    public List<SProcessDefinitionDeployInfo> getProcessDeploymentInfos(final QueryOptions queryOptions) throws SProcessDefinitionReadException {
        try {
            final List<SProcessDefinitionDeployInfo> results = persistenceService.selectList(new SelectListDescriptor<SProcessDefinitionDeployInfo>(
                    "getProcessDefinitionDeployInfos", Collections.<String, Object> emptyMap(), SProcessDefinitionDeployInfo.class, queryOptions));
            return results;
        } catch (final SBonitaReadException e) {
            throw new SProcessDefinitionReadException(e);
        }
    }

    @Override
    public List<SProcessDefinitionDeployInfo> getProcessDeploymentInfosWithActorOnlyForGroup(final long groupId, final QueryOptions queryOptions)
            throws SProcessDefinitionReadException {
        final Map<String, Object> parameters = Collections.singletonMap(GROUP_ID, (Object) groupId);
        final SelectListDescriptor<SProcessDefinitionDeployInfo> selectDescriptor = new SelectListDescriptor<SProcessDefinitionDeployInfo>(
                "getProcessesWithActorOnlyForGroup", parameters, SProcessDefinitionDeployInfo.class, queryOptions);
        try {
            return persistenceService.selectList(selectDescriptor);
        } catch (final SBonitaReadException e) {
            throw new SProcessDefinitionReadException(e);
        }
    }

    @Override
    public List<SProcessDefinitionDeployInfo> getProcessDeploymentInfosWithActorOnlyForGroups(final List<Long> groupIds, final QueryOptions queryOptions)
            throws SProcessDefinitionReadException {
        final Map<String, Object> parameters = Collections.singletonMap("groupIds", (Object) groupIds);
        final SelectListDescriptor<SProcessDefinitionDeployInfo> selectDescriptor = new SelectListDescriptor<SProcessDefinitionDeployInfo>(
                "getProcessesWithActorOnlyForGroups", parameters, SProcessDefinitionDeployInfo.class, queryOptions);
        try {
            return persistenceService.selectList(selectDescriptor);
        } catch (final SBonitaReadException e) {
            throw new SProcessDefinitionReadException(e);
        }
    }

    @Override
    public List<SProcessDefinitionDeployInfo> getProcessDeploymentInfosWithActorOnlyForRole(final long roleId, final QueryOptions queryOptions)
            throws SProcessDefinitionReadException {
        final Map<String, Object> parameters = Collections.singletonMap(ROLE_ID, (Object) roleId);
        final SelectListDescriptor<SProcessDefinitionDeployInfo> selectDescriptor = new SelectListDescriptor<SProcessDefinitionDeployInfo>(
                "getProcessesWithActorOnlyForRole", parameters, SProcessDefinitionDeployInfo.class, queryOptions);
        try {
            return persistenceService.selectList(selectDescriptor);
        } catch (final SBonitaReadException e) {
            throw new SProcessDefinitionReadException(e);
        }
    }

    @Override
    public List<SProcessDefinitionDeployInfo> getProcessDeploymentInfosWithActorOnlyForRoles(final List<Long> roleIds, final QueryOptions queryOptions)
            throws SProcessDefinitionReadException {
        final Map<String, Object> parameters = Collections.singletonMap("roleIds", (Object) roleIds);
        final SelectListDescriptor<SProcessDefinitionDeployInfo> selectDescriptor = new SelectListDescriptor<SProcessDefinitionDeployInfo>(
                "getProcessesWithActorOnlyForRoles", parameters, SProcessDefinitionDeployInfo.class, queryOptions);
        try {
            return persistenceService.selectList(selectDescriptor);
        } catch (final SBonitaReadException e) {
            throw new SProcessDefinitionReadException(e);
        }
    }

    @Override
    public List<SProcessDefinitionDeployInfo> getProcessDeploymentInfosWithActorOnlyForUser(final long userId, final QueryOptions queryOptions)
            throws SProcessDefinitionReadException {
        final Map<String, Object> parameters = Collections.singletonMap(USER_ID, (Object) userId);
        final SelectListDescriptor<SProcessDefinitionDeployInfo> selectDescriptor = new SelectListDescriptor<SProcessDefinitionDeployInfo>(
                "getProcessesWithActorOnlyForUser", parameters, SProcessDefinitionDeployInfo.class, queryOptions);
        try {
            return persistenceService.selectList(selectDescriptor);
        } catch (final SBonitaReadException e) {
            throw new SProcessDefinitionReadException(e);
        }
    }

    @Override
    public List<SProcessDefinitionDeployInfo> getProcessDeploymentInfosWithActorOnlyForUsers(final List<Long> userIds, final QueryOptions queryOptions)
            throws SProcessDefinitionReadException {
        final Map<String, Object> parameters = Collections.singletonMap("userIds", (Object) userIds);
        final SelectListDescriptor<SProcessDefinitionDeployInfo> selectDescriptor = new SelectListDescriptor<SProcessDefinitionDeployInfo>(
                "getProcessesWithActorOnlyForUsers", parameters, SProcessDefinitionDeployInfo.class, queryOptions);
        try {
            return persistenceService.selectList(selectDescriptor);
        } catch (final SBonitaReadException e) {
            throw new SProcessDefinitionReadException(e);
        }
    }

    @Override
    public long getNumberOfUsersWhoCanStartProcessDeploymentInfo(final long processDefinitionId, final QueryOptions searchOptions)
            throws SBonitaSearchException {
        try {
            final Map<String, Object> parameters = Collections.singletonMap(PROCESS_DEFINITION_ID, (Object) processDefinitionId);
            return persistenceService.getNumberOfEntities(SUser.class, WHOCANSTART_PROCESS_SUFFIX, searchOptions, parameters);
        } catch (final SBonitaReadException bre) {
            throw new SBonitaSearchException(bre);
        }
    }

    @Override
    public List<SUser> searchUsersWhoCanStartProcessDeploymentInfo(final long processDefinitionId, final QueryOptions searchOptions)
            throws SBonitaSearchException {
        try {
            final Map<String, Object> parameters = Collections.singletonMap(PROCESS_DEFINITION_ID, (Object) processDefinitionId);
            return persistenceService.searchEntity(SUser.class, WHOCANSTART_PROCESS_SUFFIX, searchOptions, parameters);
        } catch (final SBonitaReadException bre) {
            throw new SBonitaSearchException(bre);
        }
    }

}
