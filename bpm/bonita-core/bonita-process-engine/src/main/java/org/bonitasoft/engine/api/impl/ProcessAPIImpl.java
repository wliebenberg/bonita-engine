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
package org.bonitasoft.engine.api.impl;

import static java.util.Collections.singletonMap;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.bonitasoft.engine.actor.mapping.ActorMappingService;
import org.bonitasoft.engine.actor.mapping.SActorMemberCreationException;
import org.bonitasoft.engine.actor.mapping.SActorNotFoundException;
import org.bonitasoft.engine.actor.mapping.model.SActor;
import org.bonitasoft.engine.actor.mapping.model.SActorMember;
import org.bonitasoft.engine.actor.mapping.model.SActorUpdateBuilder;
import org.bonitasoft.engine.actor.mapping.model.SActorUpdateBuilderFactory;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.api.impl.resolver.ProcessDependencyResolver;
import org.bonitasoft.engine.api.impl.transaction.CustomTransactions;
import org.bonitasoft.engine.api.impl.transaction.activity.GetActivityInstance;
import org.bonitasoft.engine.api.impl.transaction.activity.GetArchivedActivityInstance;
import org.bonitasoft.engine.api.impl.transaction.activity.GetArchivedActivityInstances;
import org.bonitasoft.engine.api.impl.transaction.activity.GetNumberOfActivityInstance;
import org.bonitasoft.engine.api.impl.transaction.actor.ExportActorMapping;
import org.bonitasoft.engine.api.impl.transaction.actor.GetActor;
import org.bonitasoft.engine.api.impl.transaction.actor.GetActorMembers;
import org.bonitasoft.engine.api.impl.transaction.actor.GetActorsByActorIds;
import org.bonitasoft.engine.api.impl.transaction.actor.GetActorsByPagination;
import org.bonitasoft.engine.api.impl.transaction.actor.GetNumberOfActorMembers;
import org.bonitasoft.engine.api.impl.transaction.actor.GetNumberOfActors;
import org.bonitasoft.engine.api.impl.transaction.actor.GetNumberOfGroupsOfActor;
import org.bonitasoft.engine.api.impl.transaction.actor.GetNumberOfMembershipsOfActor;
import org.bonitasoft.engine.api.impl.transaction.actor.GetNumberOfRolesOfActor;
import org.bonitasoft.engine.api.impl.transaction.actor.GetNumberOfUsersOfActor;
import org.bonitasoft.engine.api.impl.transaction.actor.ImportActorMapping;
import org.bonitasoft.engine.api.impl.transaction.actor.RemoveActorMember;
import org.bonitasoft.engine.api.impl.transaction.category.CreateCategory;
import org.bonitasoft.engine.api.impl.transaction.category.DeleteSCategory;
import org.bonitasoft.engine.api.impl.transaction.category.GetCategories;
import org.bonitasoft.engine.api.impl.transaction.category.GetCategory;
import org.bonitasoft.engine.api.impl.transaction.category.GetNumberOfCategories;
import org.bonitasoft.engine.api.impl.transaction.category.GetNumberOfCategoriesOfProcess;
import org.bonitasoft.engine.api.impl.transaction.category.RemoveCategoriesFromProcessDefinition;
import org.bonitasoft.engine.api.impl.transaction.category.RemoveProcessDefinitionsOfCategory;
import org.bonitasoft.engine.api.impl.transaction.category.UpdateCategory;
import org.bonitasoft.engine.api.impl.transaction.comment.AddComment;
import org.bonitasoft.engine.api.impl.transaction.connector.GetConnectorImplementation;
import org.bonitasoft.engine.api.impl.transaction.connector.GetConnectorImplementations;
import org.bonitasoft.engine.api.impl.transaction.connector.GetNumberOfConnectorImplementations;
import org.bonitasoft.engine.api.impl.transaction.document.AttachDocumentVersion;
import org.bonitasoft.engine.api.impl.transaction.document.AttachDocumentVersionAndStoreContent;
import org.bonitasoft.engine.api.impl.transaction.document.GetArchivedDocument;
import org.bonitasoft.engine.api.impl.transaction.document.GetDocument;
import org.bonitasoft.engine.api.impl.transaction.document.GetDocumentByName;
import org.bonitasoft.engine.api.impl.transaction.document.GetDocumentByNameAtActivityCompletion;
import org.bonitasoft.engine.api.impl.transaction.document.GetDocumentByNameAtProcessInstantiation;
import org.bonitasoft.engine.api.impl.transaction.document.GetDocumentContent;
import org.bonitasoft.engine.api.impl.transaction.document.GetDocumentsOfProcessInstance;
import org.bonitasoft.engine.api.impl.transaction.document.GetNumberOfDocumentsOfProcessInstance;
import org.bonitasoft.engine.api.impl.transaction.event.GetEventInstances;
import org.bonitasoft.engine.api.impl.transaction.expression.EvaluateExpressionsDefinitionLevel;
import org.bonitasoft.engine.api.impl.transaction.expression.EvaluateExpressionsInstanceLevel;
import org.bonitasoft.engine.api.impl.transaction.expression.EvaluateExpressionsInstanceLevelAndArchived;
import org.bonitasoft.engine.api.impl.transaction.flownode.GetFlowNodeInstance;
import org.bonitasoft.engine.api.impl.transaction.flownode.HideTasks;
import org.bonitasoft.engine.api.impl.transaction.flownode.IsTaskHidden;
import org.bonitasoft.engine.api.impl.transaction.flownode.SetExpectedEndDate;
import org.bonitasoft.engine.api.impl.transaction.flownode.UnhideTasks;
import org.bonitasoft.engine.api.impl.transaction.identity.GetSUser;
import org.bonitasoft.engine.api.impl.transaction.process.AddProcessDefinitionToCategory;
import org.bonitasoft.engine.api.impl.transaction.process.DeleteArchivedProcessInstances;
import org.bonitasoft.engine.api.impl.transaction.process.EnableProcess;
import org.bonitasoft.engine.api.impl.transaction.process.GetArchivedProcessInstanceList;
import org.bonitasoft.engine.api.impl.transaction.process.GetLastArchivedProcessInstance;
import org.bonitasoft.engine.api.impl.transaction.process.GetLatestProcessDefinitionId;
import org.bonitasoft.engine.api.impl.transaction.process.GetNumberOfArchivedProcessInstance;
import org.bonitasoft.engine.api.impl.transaction.process.GetNumberOfProcessDeploymentInfos;
import org.bonitasoft.engine.api.impl.transaction.process.GetNumberOfProcessDeploymentInfosUnrelatedToCategory;
import org.bonitasoft.engine.api.impl.transaction.process.GetNumberOfProcessInstance;
import org.bonitasoft.engine.api.impl.transaction.process.GetProcessDefinition;
import org.bonitasoft.engine.api.impl.transaction.process.GetProcessDefinitionDeployInfo;
import org.bonitasoft.engine.api.impl.transaction.process.GetProcessDefinitionDeployInfoFromArchivedProcessInstanceIds;
import org.bonitasoft.engine.api.impl.transaction.process.GetProcessDefinitionDeployInfoFromProcessInstanceIds;
import org.bonitasoft.engine.api.impl.transaction.process.GetProcessDefinitionDeployInfos;
import org.bonitasoft.engine.api.impl.transaction.process.GetProcessDefinitionDeployInfosWithActorOnlyForGroup;
import org.bonitasoft.engine.api.impl.transaction.process.GetProcessDefinitionDeployInfosWithActorOnlyForGroups;
import org.bonitasoft.engine.api.impl.transaction.process.GetProcessDefinitionDeployInfosWithActorOnlyForRole;
import org.bonitasoft.engine.api.impl.transaction.process.GetProcessDefinitionDeployInfosWithActorOnlyForRoles;
import org.bonitasoft.engine.api.impl.transaction.process.GetProcessDefinitionDeployInfosWithActorOnlyForUser;
import org.bonitasoft.engine.api.impl.transaction.process.GetProcessDefinitionDeployInfosWithActorOnlyForUsers;
import org.bonitasoft.engine.api.impl.transaction.process.GetProcessDefinitionIDByNameAndVersion;
import org.bonitasoft.engine.api.impl.transaction.process.GetProcessDeploymentInfosFromIds;
import org.bonitasoft.engine.api.impl.transaction.process.GetProcessInstance;
import org.bonitasoft.engine.api.impl.transaction.process.SetProcessInstanceState;
import org.bonitasoft.engine.api.impl.transaction.process.UpdateProcessDeploymentInfo;
import org.bonitasoft.engine.api.impl.transaction.task.AssignOrUnassignUserTask;
import org.bonitasoft.engine.api.impl.transaction.task.GetAssignedTasks;
import org.bonitasoft.engine.api.impl.transaction.task.GetHumanTaskInstance;
import org.bonitasoft.engine.api.impl.transaction.task.GetNumberOfAssignedUserTaskInstances;
import org.bonitasoft.engine.api.impl.transaction.task.GetNumberOfOpenTasksForUsers;
import org.bonitasoft.engine.api.impl.transaction.task.GetNumberOfOverdueOpenTasksForUsers;
import org.bonitasoft.engine.api.impl.transaction.task.SetTaskPriority;
import org.bonitasoft.engine.archive.ArchiveService;
import org.bonitasoft.engine.bpm.actor.ActorCriterion;
import org.bonitasoft.engine.bpm.actor.ActorInstance;
import org.bonitasoft.engine.bpm.actor.ActorMappingExportException;
import org.bonitasoft.engine.bpm.actor.ActorMappingImportException;
import org.bonitasoft.engine.bpm.actor.ActorMember;
import org.bonitasoft.engine.bpm.actor.ActorNotFoundException;
import org.bonitasoft.engine.bpm.actor.ActorUpdater;
import org.bonitasoft.engine.bpm.actor.ActorUpdater.ActorField;
import org.bonitasoft.engine.bpm.bar.BusinessArchive;
import org.bonitasoft.engine.bpm.bar.BusinessArchiveBuilder;
import org.bonitasoft.engine.bpm.bar.BusinessArchiveFactory;
import org.bonitasoft.engine.bpm.bar.InvalidBusinessArchiveFormatException;
import org.bonitasoft.engine.bpm.bar.ProcessDefinitionBARContribution;
import org.bonitasoft.engine.bpm.category.Category;
import org.bonitasoft.engine.bpm.category.CategoryCriterion;
import org.bonitasoft.engine.bpm.category.CategoryNotFoundException;
import org.bonitasoft.engine.bpm.category.CategoryUpdater;
import org.bonitasoft.engine.bpm.category.CategoryUpdater.CategoryField;
import org.bonitasoft.engine.bpm.comment.ArchivedComment;
import org.bonitasoft.engine.bpm.comment.Comment;
import org.bonitasoft.engine.bpm.connector.ArchivedConnectorInstance;
import org.bonitasoft.engine.bpm.connector.ConnectorCriterion;
import org.bonitasoft.engine.bpm.connector.ConnectorExecutionException;
import org.bonitasoft.engine.bpm.connector.ConnectorImplementationDescriptor;
import org.bonitasoft.engine.bpm.connector.ConnectorInstance;
import org.bonitasoft.engine.bpm.connector.ConnectorNotFoundException;
import org.bonitasoft.engine.bpm.data.ArchivedDataInstance;
import org.bonitasoft.engine.bpm.data.ArchivedDataNotFoundException;
import org.bonitasoft.engine.bpm.data.DataDefinition;
import org.bonitasoft.engine.bpm.data.DataInstance;
import org.bonitasoft.engine.bpm.data.DataNotFoundException;
import org.bonitasoft.engine.bpm.document.ArchivedDocument;
import org.bonitasoft.engine.bpm.document.ArchivedDocumentNotFoundException;
import org.bonitasoft.engine.bpm.document.Document;
import org.bonitasoft.engine.bpm.document.DocumentAttachmentException;
import org.bonitasoft.engine.bpm.document.DocumentCriterion;
import org.bonitasoft.engine.bpm.document.DocumentException;
import org.bonitasoft.engine.bpm.document.DocumentNotFoundException;
import org.bonitasoft.engine.bpm.flownode.ActivityDefinitionNotFoundException;
import org.bonitasoft.engine.bpm.flownode.ActivityExecutionException;
import org.bonitasoft.engine.bpm.flownode.ActivityInstance;
import org.bonitasoft.engine.bpm.flownode.ActivityInstanceCriterion;
import org.bonitasoft.engine.bpm.flownode.ActivityInstanceNotFoundException;
import org.bonitasoft.engine.bpm.flownode.ActivityStates;
import org.bonitasoft.engine.bpm.flownode.ArchivedActivityInstance;
import org.bonitasoft.engine.bpm.flownode.ArchivedFlowNodeInstance;
import org.bonitasoft.engine.bpm.flownode.ArchivedFlowNodeInstanceNotFoundException;
import org.bonitasoft.engine.bpm.flownode.ArchivedHumanTaskInstance;
import org.bonitasoft.engine.bpm.flownode.EventCriterion;
import org.bonitasoft.engine.bpm.flownode.EventInstance;
import org.bonitasoft.engine.bpm.flownode.FlowNodeExecutionException;
import org.bonitasoft.engine.bpm.flownode.FlowNodeInstance;
import org.bonitasoft.engine.bpm.flownode.FlowNodeInstanceNotFoundException;
import org.bonitasoft.engine.bpm.flownode.FlowNodeType;
import org.bonitasoft.engine.bpm.flownode.HumanTaskInstance;
import org.bonitasoft.engine.bpm.flownode.HumanTaskInstanceSearchDescriptor;
import org.bonitasoft.engine.bpm.flownode.SendEventException;
import org.bonitasoft.engine.bpm.flownode.TaskPriority;
import org.bonitasoft.engine.bpm.process.ActivationState;
import org.bonitasoft.engine.bpm.process.ArchivedProcessInstance;
import org.bonitasoft.engine.bpm.process.ArchivedProcessInstanceNotFoundException;
import org.bonitasoft.engine.bpm.process.ArchivedProcessInstancesSearchDescriptor;
import org.bonitasoft.engine.bpm.process.ConfigurationState;
import org.bonitasoft.engine.bpm.process.DesignProcessDefinition;
import org.bonitasoft.engine.bpm.process.InvalidProcessDefinitionException;
import org.bonitasoft.engine.bpm.process.Problem;
import org.bonitasoft.engine.bpm.process.ProcessActivationException;
import org.bonitasoft.engine.bpm.process.ProcessDefinition;
import org.bonitasoft.engine.bpm.process.ProcessDefinitionNotFoundException;
import org.bonitasoft.engine.bpm.process.ProcessDeployException;
import org.bonitasoft.engine.bpm.process.ProcessDeploymentInfo;
import org.bonitasoft.engine.bpm.process.ProcessDeploymentInfoCriterion;
import org.bonitasoft.engine.bpm.process.ProcessDeploymentInfoUpdater;
import org.bonitasoft.engine.bpm.process.ProcessEnablementException;
import org.bonitasoft.engine.bpm.process.ProcessExecutionException;
import org.bonitasoft.engine.bpm.process.ProcessExportException;
import org.bonitasoft.engine.bpm.process.ProcessInstance;
import org.bonitasoft.engine.bpm.process.ProcessInstanceCriterion;
import org.bonitasoft.engine.bpm.process.ProcessInstanceNotFoundException;
import org.bonitasoft.engine.bpm.process.ProcessInstanceSearchDescriptor;
import org.bonitasoft.engine.bpm.process.ProcessInstanceState;
import org.bonitasoft.engine.bpm.process.impl.ProcessDeploymentInfoImpl;
import org.bonitasoft.engine.bpm.supervisor.ProcessSupervisor;
import org.bonitasoft.engine.bpm.supervisor.ProcessSupervisorSearchDescriptor;
import org.bonitasoft.engine.builder.BuilderFactory;
import org.bonitasoft.engine.classloader.ClassLoaderException;
import org.bonitasoft.engine.classloader.ClassLoaderService;
import org.bonitasoft.engine.commons.exceptions.SBonitaException;
import org.bonitasoft.engine.commons.exceptions.SBonitaRuntimeException;
import org.bonitasoft.engine.commons.transaction.TransactionContent;
import org.bonitasoft.engine.commons.transaction.TransactionContentWithResult;
import org.bonitasoft.engine.commons.transaction.TransactionExecutor;
import org.bonitasoft.engine.core.category.CategoryService;
import org.bonitasoft.engine.core.category.exception.SCategoryAlreadyExistsException;
import org.bonitasoft.engine.core.category.exception.SCategoryInProcessAlreadyExistsException;
import org.bonitasoft.engine.core.category.exception.SCategoryNotFoundException;
import org.bonitasoft.engine.core.category.model.SCategory;
import org.bonitasoft.engine.core.category.model.SProcessCategoryMapping;
import org.bonitasoft.engine.core.category.model.builder.SCategoryBuilderFactory;
import org.bonitasoft.engine.core.category.model.builder.SCategoryUpdateBuilder;
import org.bonitasoft.engine.core.category.model.builder.SCategoryUpdateBuilderFactory;
import org.bonitasoft.engine.core.category.model.builder.SProcessCategoryMappingBuilderFactory;
import org.bonitasoft.engine.core.connector.ConnectorInstanceService;
import org.bonitasoft.engine.core.connector.ConnectorResult;
import org.bonitasoft.engine.core.connector.ConnectorService;
import org.bonitasoft.engine.core.connector.exception.SConnectorException;
import org.bonitasoft.engine.core.connector.parser.SConnectorImplementationDescriptor;
import org.bonitasoft.engine.core.expression.control.api.ExpressionResolverService;
import org.bonitasoft.engine.core.expression.control.model.SExpressionContext;
import org.bonitasoft.engine.core.filter.FilterResult;
import org.bonitasoft.engine.core.filter.UserFilterService;
import org.bonitasoft.engine.core.operation.OperationService;
import org.bonitasoft.engine.core.operation.model.SOperation;
import org.bonitasoft.engine.core.process.comment.api.SCommentNotFoundException;
import org.bonitasoft.engine.core.process.comment.api.SCommentService;
import org.bonitasoft.engine.core.process.comment.model.SComment;
import org.bonitasoft.engine.core.process.comment.model.archive.SAComment;
import org.bonitasoft.engine.core.process.definition.ProcessDefinitionService;
import org.bonitasoft.engine.core.process.definition.SProcessDefinitionNotFoundException;
import org.bonitasoft.engine.core.process.definition.exception.SProcessDefinitionReadException;
import org.bonitasoft.engine.core.process.definition.exception.SProcessDeletionException;
import org.bonitasoft.engine.core.process.definition.model.SActivityDefinition;
import org.bonitasoft.engine.core.process.definition.model.SActorDefinition;
import org.bonitasoft.engine.core.process.definition.model.SFlowElementContainerDefinition;
import org.bonitasoft.engine.core.process.definition.model.SFlowNodeDefinition;
import org.bonitasoft.engine.core.process.definition.model.SHumanTaskDefinition;
import org.bonitasoft.engine.core.process.definition.model.SProcessDefinition;
import org.bonitasoft.engine.core.process.definition.model.SProcessDefinitionDeployInfo;
import org.bonitasoft.engine.core.process.definition.model.SUserFilterDefinition;
import org.bonitasoft.engine.core.process.definition.model.builder.SProcessDefinitionBuilderFactory;
import org.bonitasoft.engine.core.process.definition.model.builder.SProcessDefinitionDeployInfoBuilderFactory;
import org.bonitasoft.engine.core.process.definition.model.builder.ServerModelConvertor;
import org.bonitasoft.engine.core.process.definition.model.builder.event.trigger.SThrowMessageEventTriggerDefinitionBuilder;
import org.bonitasoft.engine.core.process.definition.model.builder.event.trigger.SThrowMessageEventTriggerDefinitionBuilderFactory;
import org.bonitasoft.engine.core.process.definition.model.builder.event.trigger.SThrowSignalEventTriggerDefinitionBuilderFactory;
import org.bonitasoft.engine.core.process.definition.model.event.trigger.SThrowMessageEventTriggerDefinition;
import org.bonitasoft.engine.core.process.definition.model.event.trigger.SThrowSignalEventTriggerDefinition;
import org.bonitasoft.engine.core.process.document.api.ProcessDocumentService;
import org.bonitasoft.engine.core.process.document.model.SAProcessDocument;
import org.bonitasoft.engine.core.process.document.model.SProcessDocument;
import org.bonitasoft.engine.core.process.document.model.builder.SProcessDocumentBuilder;
import org.bonitasoft.engine.core.process.document.model.builder.SProcessDocumentBuilderFactory;
import org.bonitasoft.engine.core.process.instance.api.ActivityInstanceService;
import org.bonitasoft.engine.core.process.instance.api.ProcessInstanceService;
import org.bonitasoft.engine.core.process.instance.api.event.EventInstanceService;
import org.bonitasoft.engine.core.process.instance.api.exceptions.SAProcessInstanceNotFoundException;
import org.bonitasoft.engine.core.process.instance.api.exceptions.SActivityInstanceNotFoundException;
import org.bonitasoft.engine.core.process.instance.api.exceptions.SFlowNodeNotFoundException;
import org.bonitasoft.engine.core.process.instance.api.exceptions.SFlowNodeReadException;
import org.bonitasoft.engine.core.process.instance.api.exceptions.SProcessInstanceHierarchicalDeletionException;
import org.bonitasoft.engine.core.process.instance.api.exceptions.SProcessInstanceNotFoundException;
import org.bonitasoft.engine.core.process.instance.api.exceptions.SProcessInstanceReadException;
import org.bonitasoft.engine.core.process.instance.api.states.FlowNodeState;
import org.bonitasoft.engine.core.process.instance.model.SActivityInstance;
import org.bonitasoft.engine.core.process.instance.model.SFlowElementsContainerType;
import org.bonitasoft.engine.core.process.instance.model.SFlowNodeInstance;
import org.bonitasoft.engine.core.process.instance.model.SHumanTaskInstance;
import org.bonitasoft.engine.core.process.instance.model.SPendingActivityMapping;
import org.bonitasoft.engine.core.process.instance.model.SProcessInstance;
import org.bonitasoft.engine.core.process.instance.model.SStateCategory;
import org.bonitasoft.engine.core.process.instance.model.STaskPriority;
import org.bonitasoft.engine.core.process.instance.model.SUserTaskInstance;
import org.bonitasoft.engine.core.process.instance.model.archive.SAFlowNodeInstance;
import org.bonitasoft.engine.core.process.instance.model.archive.SAProcessInstance;
import org.bonitasoft.engine.core.process.instance.model.archive.builder.SAProcessInstanceBuilderFactory;
import org.bonitasoft.engine.core.process.instance.model.builder.SAutomaticTaskInstanceBuilderFactory;
import org.bonitasoft.engine.core.process.instance.model.builder.SPendingActivityMappingBuilderFactory;
import org.bonitasoft.engine.core.process.instance.model.builder.SProcessInstanceBuilderFactory;
import org.bonitasoft.engine.core.process.instance.model.event.SEventInstance;
import org.bonitasoft.engine.data.definition.model.SDataDefinition;
import org.bonitasoft.engine.data.definition.model.builder.SDataDefinitionBuilder;
import org.bonitasoft.engine.data.definition.model.builder.SDataDefinitionBuilderFactory;
import org.bonitasoft.engine.data.instance.api.DataInstanceContainer;
import org.bonitasoft.engine.data.instance.api.DataInstanceService;
import org.bonitasoft.engine.data.instance.exception.SDataInstanceException;
import org.bonitasoft.engine.data.instance.model.SDataInstance;
import org.bonitasoft.engine.data.instance.model.archive.SADataInstance;
import org.bonitasoft.engine.dependency.DependencyService;
import org.bonitasoft.engine.dependency.model.ScopeType;
import org.bonitasoft.engine.document.SDocumentNotFoundException;
import org.bonitasoft.engine.exception.AlreadyExistsException;
import org.bonitasoft.engine.exception.BonitaException;
import org.bonitasoft.engine.exception.BonitaHomeNotSetException;
import org.bonitasoft.engine.exception.BonitaRuntimeException;
import org.bonitasoft.engine.exception.CreationException;
import org.bonitasoft.engine.exception.DeletionException;
import org.bonitasoft.engine.exception.ExecutionException;
import org.bonitasoft.engine.exception.NotFoundException;
import org.bonitasoft.engine.exception.NotSerializableException;
import org.bonitasoft.engine.exception.ProcessInstanceHierarchicalDeletionException;
import org.bonitasoft.engine.exception.RetrieveException;
import org.bonitasoft.engine.exception.SearchException;
import org.bonitasoft.engine.exception.UpdateException;
import org.bonitasoft.engine.execution.FlowNodeExecutor;
import org.bonitasoft.engine.execution.ProcessExecutor;
import org.bonitasoft.engine.execution.SUnreleasableTaskException;
import org.bonitasoft.engine.execution.TransactionalProcessInstanceInterruptor;
import org.bonitasoft.engine.execution.event.EventsHandler;
import org.bonitasoft.engine.execution.state.FlowNodeStateManager;
import org.bonitasoft.engine.expression.Expression;
import org.bonitasoft.engine.expression.ExpressionBuilder;
import org.bonitasoft.engine.expression.ExpressionEvaluationException;
import org.bonitasoft.engine.expression.ExpressionType;
import org.bonitasoft.engine.expression.InvalidExpressionException;
import org.bonitasoft.engine.expression.model.SExpression;
import org.bonitasoft.engine.home.BonitaHomeServer;
import org.bonitasoft.engine.identity.IdentityService;
import org.bonitasoft.engine.identity.MemberType;
import org.bonitasoft.engine.identity.SUserNotFoundException;
import org.bonitasoft.engine.identity.User;
import org.bonitasoft.engine.identity.model.SUser;
import org.bonitasoft.engine.io.IOUtil;
import org.bonitasoft.engine.job.FailedJob;
import org.bonitasoft.engine.lock.BonitaLock;
import org.bonitasoft.engine.lock.LockService;
import org.bonitasoft.engine.lock.SLockException;
import org.bonitasoft.engine.log.LogMessageBuilder;
import org.bonitasoft.engine.log.technical.TechnicalLogSeverity;
import org.bonitasoft.engine.log.technical.TechnicalLoggerService;
import org.bonitasoft.engine.operation.LeftOperand;
import org.bonitasoft.engine.operation.Operation;
import org.bonitasoft.engine.operation.OperationBuilder;
import org.bonitasoft.engine.persistence.FilterOption;
import org.bonitasoft.engine.persistence.OrderAndField;
import org.bonitasoft.engine.persistence.OrderByOption;
import org.bonitasoft.engine.persistence.OrderByType;
import org.bonitasoft.engine.persistence.QueryOptions;
import org.bonitasoft.engine.persistence.ReadPersistenceService;
import org.bonitasoft.engine.persistence.SBonitaReadException;
import org.bonitasoft.engine.persistence.SBonitaSearchException;
import org.bonitasoft.engine.recorder.model.EntityUpdateDescriptor;
import org.bonitasoft.engine.scheduler.JobService;
import org.bonitasoft.engine.scheduler.SchedulerService;
import org.bonitasoft.engine.scheduler.builder.SJobParameterBuilderFactory;
import org.bonitasoft.engine.scheduler.exception.SSchedulerException;
import org.bonitasoft.engine.scheduler.model.SFailedJob;
import org.bonitasoft.engine.scheduler.model.SJobParameter;
import org.bonitasoft.engine.search.Order;
import org.bonitasoft.engine.search.SSearchException;
import org.bonitasoft.engine.search.SearchOptions;
import org.bonitasoft.engine.search.SearchOptionsBuilder;
import org.bonitasoft.engine.search.SearchResult;
import org.bonitasoft.engine.search.activity.SearchActivityInstances;
import org.bonitasoft.engine.search.activity.SearchArchivedActivityInstances;
import org.bonitasoft.engine.search.comment.SearchArchivedComments;
import org.bonitasoft.engine.search.comment.SearchComments;
import org.bonitasoft.engine.search.comment.SearchCommentsInvolvingUser;
import org.bonitasoft.engine.search.comment.SearchCommentsManagedBy;
import org.bonitasoft.engine.search.connector.SearchArchivedConnectorInstance;
import org.bonitasoft.engine.search.connector.SearchConnectorInstances;
import org.bonitasoft.engine.search.descriptor.SearchEntitiesDescriptor;
import org.bonitasoft.engine.search.descriptor.SearchProcessDefinitionsDescriptor;
import org.bonitasoft.engine.search.descriptor.SearchProcessInstanceDescriptor;
import org.bonitasoft.engine.search.descriptor.SearchProcessSupervisorDescriptor;
import org.bonitasoft.engine.search.descriptor.SearchUserDescriptor;
import org.bonitasoft.engine.search.document.SearchArchivedDocuments;
import org.bonitasoft.engine.search.document.SearchArchivedDocumentsSupervisedBy;
import org.bonitasoft.engine.search.document.SearchDocuments;
import org.bonitasoft.engine.search.document.SearchDocumentsSupervisedBy;
import org.bonitasoft.engine.search.flownode.SearchArchivedFlowNodeInstances;
import org.bonitasoft.engine.search.flownode.SearchFlowNodeInstances;
import org.bonitasoft.engine.search.identity.SearchUsersWhoCanStartProcessDeploymentInfo;
import org.bonitasoft.engine.search.impl.SearchResultImpl;
import org.bonitasoft.engine.search.process.SearchArchivedProcessInstances;
import org.bonitasoft.engine.search.process.SearchArchivedProcessInstancesInvolvingUser;
import org.bonitasoft.engine.search.process.SearchArchivedProcessInstancesSupervisedBy;
import org.bonitasoft.engine.search.process.SearchArchivedProcessInstancesWithoutSubProcess;
import org.bonitasoft.engine.search.process.SearchOpenProcessInstancesInvolvingUser;
import org.bonitasoft.engine.search.process.SearchOpenProcessInstancesInvolvingUsersManagedBy;
import org.bonitasoft.engine.search.process.SearchOpenProcessInstancesSupervisedBy;
import org.bonitasoft.engine.search.process.SearchProcessDeploymentInfos;
import org.bonitasoft.engine.search.process.SearchProcessDeploymentInfosStartedBy;
import org.bonitasoft.engine.search.process.SearchProcessDeploymentInfosUserCanStart;
import org.bonitasoft.engine.search.process.SearchProcessDeploymentInfosUsersManagedByCanStart;
import org.bonitasoft.engine.search.process.SearchProcessInstances;
import org.bonitasoft.engine.search.process.SearchUncategorizedProcessDeploymentInfos;
import org.bonitasoft.engine.search.process.SearchUncategorizedProcessDeploymentInfosSupervisedBy;
import org.bonitasoft.engine.search.process.SearchUncategorizedProcessDeploymentInfosUserCanStart;
import org.bonitasoft.engine.search.supervisor.SearchArchivedTasksSupervisedBy;
import org.bonitasoft.engine.search.supervisor.SearchAssignedTasksSupervisedBy;
import org.bonitasoft.engine.search.supervisor.SearchProcessDeploymentInfosSupervised;
import org.bonitasoft.engine.search.supervisor.SearchSupervisors;
import org.bonitasoft.engine.search.task.SearchArchivedTasks;
import org.bonitasoft.engine.search.task.SearchArchivedTasksManagedBy;
import org.bonitasoft.engine.search.task.SearchAssignedTaskManagedBy;
import org.bonitasoft.engine.search.task.SearchHumanTaskInstances;
import org.bonitasoft.engine.search.task.SearchPendingHiddenTasks;
import org.bonitasoft.engine.search.task.SearchPendingTasksForUser;
import org.bonitasoft.engine.search.task.SearchPendingTasksManagedBy;
import org.bonitasoft.engine.search.task.SearchPendingTasksSupervisedBy;
import org.bonitasoft.engine.service.ModelConvertor;
import org.bonitasoft.engine.service.TenantServiceAccessor;
import org.bonitasoft.engine.service.TenantServiceSingleton;
import org.bonitasoft.engine.service.impl.ServiceAccessorFactory;
import org.bonitasoft.engine.session.model.SSession;
import org.bonitasoft.engine.sessionaccessor.SessionAccessor;
import org.bonitasoft.engine.supervisor.mapping.SSupervisorAlreadyExistsException;
import org.bonitasoft.engine.supervisor.mapping.SSupervisorCreationException;
import org.bonitasoft.engine.supervisor.mapping.SSupervisorDeletionException;
import org.bonitasoft.engine.supervisor.mapping.SSupervisorNotFoundException;
import org.bonitasoft.engine.supervisor.mapping.SupervisorMappingService;
import org.bonitasoft.engine.supervisor.mapping.model.SProcessSupervisor;
import org.bonitasoft.engine.supervisor.mapping.model.SProcessSupervisorBuilder;
import org.bonitasoft.engine.supervisor.mapping.model.SProcessSupervisorBuilderFactory;
import org.bonitasoft.engine.transaction.UserTransactionService;
import org.bonitasoft.engine.xml.Parser;
import org.bonitasoft.engine.xml.XMLWriter;

/**
 * @author Baptiste Mesta
 * @author Matthieu Chaffotte
 * @author Yanyan Liu
 * @author Elias Ricken de Medeiros
 * @author Zhao Na
 * @author Zhang Bole
 * @author Emmanuel Duchastenier
 * @author Celine Souchet
 * @author Arthur Freycon
 */
public class ProcessAPIImpl implements ProcessAPI {

    private static final String CONTAINER_TYPE_PROCESS_INSTANCE = "PROCESS_INSTANCE";

    private static final String CONTAINER_TYPE_ACTIVITY_INSTANCE = "ACTIVITY_INSTANCE";

    private final ProcessManagementAPIImplDelegate processManagementAPIImplDelegate = instantiateProcessManagementAPIDelegate();

    protected ProcessManagementAPIImplDelegate instantiateProcessManagementAPIDelegate() {
        return new ProcessManagementAPIImplDelegate();
    }

    protected TenantServiceAccessor getTenantAccessor() {
        try {
            final SessionAccessor sessionAccessor = ServiceAccessorFactory.getInstance().createSessionAccessor();
            final long tenantId = sessionAccessor.getTenantId();
            return TenantServiceSingleton.getInstance(tenantId);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public SearchResult<HumanTaskInstance> searchHumanTaskInstances(final SearchOptions searchOptions) throws SearchException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final SearchEntitiesDescriptor searchEntitiesDescriptor = tenantAccessor.getSearchEntitiesDescriptor();
        final ActivityInstanceService activityInstanceService = tenantAccessor.getActivityInstanceService();
        final FlowNodeStateManager flowNodeStateManager = tenantAccessor.getFlowNodeStateManager();
        final SearchHumanTaskInstances searchHumanTasksTransaction = new SearchHumanTaskInstances(activityInstanceService, flowNodeStateManager,
                searchEntitiesDescriptor.getSearchHumanTaskInstanceDescriptor(), searchOptions);
        try {
            searchHumanTasksTransaction.execute();
        } catch (final SBonitaException e) {
            throw new SearchException(e);
        }
        return searchHumanTasksTransaction.getResult();
    }

    @Override
    public void deleteProcessDefinition(final long processDefinitionId) throws DeletionException {
        final SearchOptionsBuilder builder = new SearchOptionsBuilder(0, 1);
        builder.filter(ProcessInstanceSearchDescriptor.PROCESS_DEFINITION_ID, processDefinitionId);
        final SearchOptions searchOptions = builder.done();

        try {
            final boolean hasOpenProcessInstances = searchProcessInstances(getTenantAccessor(), searchOptions).getCount() > 0;
            if (hasOpenProcessInstances) {
                throw new DeletionException("Some active process instances are still found, process #" + processDefinitionId + " can't be deleted.");
            }
            final boolean hasArchivedProcessInstances = searchArchivedProcessInstances(searchOptions).getCount() > 0;
            if (hasArchivedProcessInstances) {
                throw new DeletionException("Some archived process instances are still found, process #" + processDefinitionId + " can't be deleted.");
            }

            processManagementAPIImplDelegate.deleteProcessDefinition(processDefinitionId);
        } catch (final Exception e) {
            throw new DeletionException(e);
        }
    }

    @Override
    public void deleteProcessDefinitions(final List<Long> processDefinitionIds) throws DeletionException {
        for (final Long processDefinitionId : processDefinitionIds) {
            deleteProcessDefinition(processDefinitionId);
        }
    }

    @Override
    @CustomTransactions
    @Deprecated
    public void deleteProcess(final long processDefinitionId) throws DeletionException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        // multiple tx here because we must lock instances when deleting them
        // but if the second tx crash we can relaunch delete process without issues
        final UserTransactionService userTransactionService = tenantAccessor.getUserTransactionService();
        try {
            userTransactionService.executeInTransaction(new Callable<Void>() {

                @Override
                public Void call() throws Exception {
                    deleteProcessInstancesFromProcessDefinition(processDefinitionId, tenantAccessor, tenantAccessor.getTenantId());
                    try {
                        processManagementAPIImplDelegate.deleteProcessDefinition(processDefinitionId);
                    } catch (final BonitaHomeNotSetException e) {
                        throw new SProcessDeletionException(e);
                    } catch (final IOException e) {
                        throw new SProcessDeletionException(e);
                    }
                    return null;
                }
            });

        } catch (final SProcessInstanceHierarchicalDeletionException e) {
            throw new ProcessInstanceHierarchicalDeletionException(e.getMessage(), e.getProcessInstanceId());
        } catch (final Exception e) {
            throw new DeletionException(e);
        }
    }

    private void deleteProcessInstancesFromProcessDefinition(final long processDefinitionId, final TenantServiceAccessor tenantAccessor, final long tenantId)
            throws SBonitaException, SProcessInstanceHierarchicalDeletionException {
        List<ProcessInstance> processInstances;
        final int maxResults = 1000;
        do {
            processInstances = searchProcessInstancesFromProcessDefinition(tenantAccessor, processDefinitionId, 0, maxResults);
            if (processInstances.size() > 0) {
                deleteProcessInstancesInsideLocks(tenantAccessor, true, processInstances, tenantAccessor.getTenantId());
            }
        } while (!processInstances.isEmpty());
    }

    private void deleteProcessInstancesInsideLocks(final TenantServiceAccessor tenantAccessor, final boolean ignoreProcessInstanceNotFound,
            final List<ProcessInstance> processInstances, final long tenantId) throws SBonitaException, SProcessInstanceHierarchicalDeletionException {
        final List<Long> processInstanceIds = new ArrayList<Long>(processInstances.size());
        for (final ProcessInstance processInstance : processInstances) {
            processInstanceIds.add(processInstance.getId());
        }
        deleteProcessInstancesInsideLocksFromIds(tenantAccessor, ignoreProcessInstanceNotFound, processInstanceIds, tenantId);
    }

    private void deleteProcessInstancesInsideLocksFromIds(final TenantServiceAccessor tenantAccessor, final boolean ignoreProcessInstanceNotFound,
            final List<Long> processInstanceIds, final long tenantId) throws SBonitaException, SProcessInstanceHierarchicalDeletionException {
        final LockService lockService = tenantAccessor.getLockService();
        final String objectType = SFlowElementsContainerType.PROCESS.name();
        final List<Long> lockedProcesses = new ArrayList<Long>();
        List<BonitaLock> locks = null;
        try {
            locks = createLocks(lockService, objectType, lockedProcesses, processInstanceIds, tenantId);
            deleteProcessInstancesInTransaction(tenantAccessor, ignoreProcessInstanceNotFound, processInstanceIds);
        } finally {
            releaseLocks(tenantAccessor, lockService, locks, tenantId);
        }
    }

    private void releaseLocks(final TenantServiceAccessor tenantAccessor, final LockService lockService, final List<BonitaLock> locks, final long tenantId) {
        if (locks == null) {
            return;
        }
        for (final BonitaLock lock : locks) {
            try {
                lockService.unlock(lock, tenantId);
            } catch (final SLockException e) {
                log(tenantAccessor, e);
            }
        }
    }

    private ArrayList<BonitaLock> createLocks(final LockService lockService, final String objectType, final List<Long> lockedProcesses,
            final List<Long> processInstanceIds, final long tenantId) throws SLockException {
        final ArrayList<BonitaLock> locks = new ArrayList<BonitaLock>(processInstanceIds.size());
        for (final Long processInstanceId : processInstanceIds) {
            final BonitaLock lock = lockService.lock(processInstanceId, objectType, tenantId);
            locks.add(lock);
            lockedProcesses.add(processInstanceId);
        }
        return locks;
    }

    private void deleteProcessInstancesInTransaction(final TenantServiceAccessor tenantAccessor, final boolean ignoreProcessInstanceNotFound,
            final List<Long> processInstanceIds) throws SBonitaException, SProcessInstanceHierarchicalDeletionException {
        final ProcessInstanceService processInstanceService = tenantAccessor.getProcessInstanceService();
        final TechnicalLoggerService logger = tenantAccessor.getTechnicalLoggerService();
        final ActivityInstanceService activityInstanceService = tenantAccessor.getActivityInstanceService();
        deleteProcessInstances(processInstanceService, logger, ignoreProcessInstanceNotFound, activityInstanceService, processInstanceIds);
    }

    private void deleteProcessInstances(final ProcessInstanceService processInstanceService, final TechnicalLoggerService logger,
            final boolean ignoreProcessInstanceNotFound, final ActivityInstanceService activityInstanceService, final List<Long> processInstanceIds)
            throws SBonitaException, SProcessInstanceHierarchicalDeletionException {
        for (final Long processInstanceId : processInstanceIds) {
            try {
                deleteProcessInstance(processInstanceService, processInstanceId, activityInstanceService);
            } catch (final SProcessInstanceNotFoundException e) {
                if (ignoreProcessInstanceNotFound) {
                    if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.DEBUG)) {
                        logger.log(this.getClass(), TechnicalLogSeverity.DEBUG, e.getMessage() + ". It has probably completed.");
                    }
                } else {
                    throw e;
                }
            }
        }
    }

    private void deleteProcessInstance(final ProcessInstanceService processInstanceService, final Long processInstanceId,
            final ActivityInstanceService activityInstanceService) throws SBonitaException, SProcessInstanceHierarchicalDeletionException {
        final SProcessInstance processInstance = processInstanceService.getProcessInstance(processInstanceId);
        final long callerId = processInstance.getCallerId();
        if (callerId > 0) {
            try {
                final SFlowNodeInstance flowNodeInstance = activityInstanceService.getFlowNodeInstance(callerId);
                throw new SProcessInstanceHierarchicalDeletionException("Unable to delete the process instance because the parent is still active: activity "
                        + flowNodeInstance.getName() + " with id " + flowNodeInstance.getId(), flowNodeInstance.getRootProcessInstanceId());
            } catch (final SFlowNodeNotFoundException e) {
                // ok the activity that called this process do not exists anymore
            }
        }
        processInstanceService.deleteArchivedProcessInstanceElements(processInstanceId, processInstance.getProcessDefinitionId());
        processInstanceService.deleteArchivedProcessInstancesOfProcessInstance(processInstanceId);
        processInstanceService.deleteProcessInstance(processInstance);
    }

    private List<ProcessInstance> searchProcessInstancesFromProcessDefinition(final TenantServiceAccessor tenantAccessor, final long processDefinitionId,
            final int startIndex, final int maxResults) throws SSearchException {
        final SearchOptionsBuilder searchOptionsBuilder = new SearchOptionsBuilder(startIndex, maxResults);
        searchOptionsBuilder.filter(ProcessInstanceSearchDescriptor.PROCESS_DEFINITION_ID, processDefinitionId);
        // Order by caller id ASC because we need to have parent process deleted before their sub processes
        searchOptionsBuilder.sort(ProcessInstanceSearchDescriptor.CALLER_ID, Order.ASC);
        return searchProcessInstances(tenantAccessor, searchOptionsBuilder.done()).getResult();
    }

    @Override
    @CustomTransactions
    @Deprecated
    public void deleteProcesses(final List<Long> processDefinitionIds) throws DeletionException {
        for (final Long processDefinitionId : processDefinitionIds) {
            deleteProcess(processDefinitionId);
        }
    }

    @Override
    public ProcessDefinition deployAndEnableProcess(final DesignProcessDefinition designProcessDefinition) throws ProcessDeployException,
            ProcessEnablementException, AlreadyExistsException, InvalidProcessDefinitionException {
        BusinessArchive businessArchive;
        try {
            businessArchive = new BusinessArchiveBuilder().createNewBusinessArchive().setProcessDefinition(designProcessDefinition).done();
        } catch (final InvalidBusinessArchiveFormatException ibafe) {
            throw new InvalidProcessDefinitionException(ibafe.getMessage());
        }
        return deployAndEnableProcess(businessArchive);
    }

    @Override
    public ProcessDefinition deployAndEnableProcess(final BusinessArchive businessArchive) throws ProcessDeployException, ProcessEnablementException,
            AlreadyExistsException {
        final ProcessDefinition processDefinition = deploy(businessArchive);
        try {
            enableProcess(processDefinition.getId());
        } catch (final ProcessDefinitionNotFoundException pdnfe) {
            throw new ProcessEnablementException(pdnfe.getMessage());
        }
        return processDefinition;
    }

    @Override
    public ProcessDefinition deploy(final DesignProcessDefinition designProcessDefinition) throws AlreadyExistsException, ProcessDeployException {
        try {
            final BusinessArchive businessArchive = new BusinessArchiveBuilder().createNewBusinessArchive().setProcessDefinition(designProcessDefinition)
                    .done();
            return deploy(businessArchive);
        } catch (final InvalidBusinessArchiveFormatException ibafe) {
            throw new ProcessDeployException(ibafe);
        }
    }

    @Override
    public ProcessDefinition deploy(final BusinessArchive businessArchive) throws ProcessDeployException, AlreadyExistsException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final ProcessDefinitionService processDefinitionService = tenantAccessor.getProcessDefinitionService();

        final DependencyService dependencyService = tenantAccessor.getDependencyService();
        final DesignProcessDefinition designProcessDefinition = businessArchive.getProcessDefinition();

        // create the runtime process definition
        final SProcessDefinition sProcessDefinition = BuilderFactory.get(SProcessDefinitionBuilderFactory.class).createNewInstance(designProcessDefinition)
                .done();

        try {
            try {
                processDefinitionService.getProcessDefinitionId(designProcessDefinition.getName(), designProcessDefinition.getVersion());
                throw new AlreadyExistsException("The process " + designProcessDefinition.getName() + " in version " + designProcessDefinition.getVersion()
                        + " already exists.");
            } catch (final SProcessDefinitionReadException e) {
                // ok
            }
            processDefinitionService.store(sProcessDefinition, designProcessDefinition.getDisplayName(), designProcessDefinition.getDisplayDescription());
            unzipBar(businessArchive, sProcessDefinition, tenantAccessor.getTenantId());// TODO first unzip in temp folder
            final boolean isResolved = tenantAccessor.getDependencyResolver().resolveDependencies(this, businessArchive, tenantAccessor, sProcessDefinition);
            if (isResolved) {
                tenantAccessor.getDependencyResolver().resolveAndCreateDependencies(businessArchive, processDefinitionService, dependencyService,
                        sProcessDefinition);
            }
        } catch (final BonitaHomeNotSetException e) {
            throw new ProcessDeployException(e);
        } catch (final IOException e) {
            throw new ProcessDeployException(e);
        } catch (final SBonitaException e) {
            throw new ProcessDeployException(e);
        }

        final ProcessDefinition processDefinition = ModelConvertor.toProcessDefinition(sProcessDefinition);
        final TechnicalLoggerService logger = tenantAccessor.getTechnicalLoggerService();
        if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.INFO)) {
            logger.log(this.getClass(), TechnicalLogSeverity.INFO, "The user <" + SessionInfos.getUserNameFromSession() + "> has installed process <"
                    + sProcessDefinition.getName() + "> in version <" + sProcessDefinition.getVersion() + "> with id <" + sProcessDefinition.getId() + ">");
        }
        return processDefinition;
    }

    @Override
    public void importActorMapping(final long pDefinitionId, final byte[] actorMappingXML) throws ActorMappingImportException {
        if (actorMappingXML != null) {
            final String actorMapping = new String(actorMappingXML, Charset.forName("UTF-8"));
            importActorMapping(pDefinitionId, actorMapping);
        }
    }

    @Override
    // TODO delete files after use/if an exception occurs
    public byte[] exportBarProcessContentUnderHome(final long processDefinitionId) throws ProcessExportException {
        String processesFolder;
        try {
            final long tenantId = getTenantAccessor().getTenantId();
            processesFolder = BonitaHomeServer.getInstance().getProcessesFolder(tenantId);
        } catch (final BonitaHomeNotSetException e) {
            throw new BonitaRuntimeException(e);
        }
        final File file = new File(processesFolder);
        if (!file.exists()) {
            file.mkdirs();
        }
        final File processFolder = new File(file, String.valueOf(processDefinitionId));

        // export actormapping
        try {

            final File actormappF = new File(processFolder.getPath(), "actorMapping.xml");
            if (!actormappF.exists()) {
                actormappF.createNewFile();
            }
            String xmlcontent = "";
            try {
                xmlcontent = exportActorMapping(processDefinitionId);
            } catch (final ActorMappingExportException e) {
                throw new ProcessExportException(e);
            }
            IOUtil.writeContentToFile(xmlcontent, actormappF);

            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final ZipOutputStream zos = new ZipOutputStream(baos);
            try {
                IOUtil.zipDir(processFolder.getPath(), zos, processFolder.getPath());
                return baos.toByteArray();
            } finally {
                zos.close();
            }
        } catch (final IOException e) {
            throw new ProcessExportException(e);
        }
    }

    protected void unzipBar(final BusinessArchive businessArchive, final SProcessDefinition sProcessDefinition, final long tenantId)
            throws BonitaHomeNotSetException, IOException {
        final File processFolder = getProcessFolder(sProcessDefinition.getId(), tenantId);
        BusinessArchiveFactory.writeBusinessArchiveToFolder(businessArchive, processFolder);
    }

    private File getProcessFolder(final long processDefinitionId, final long tenantId) throws BonitaHomeNotSetException {
        final String processesFolder = BonitaHomeServer.getInstance().getProcessesFolder(tenantId);
        final File file = new File(processesFolder);
        if (!file.exists()) {
            file.mkdirs();
        }
        return new File(file, String.valueOf(processDefinitionId));
    }

    @Override
    @CustomTransactions
    @Deprecated
    public void disableAndDelete(final long processDefinitionId) throws ProcessDefinitionNotFoundException, ProcessActivationException, DeletionException {
        final TransactionExecutor transactionExecutor = getTenantAccessor().getTransactionExecutor();
        try {
            transactionExecutor.execute(new TransactionContent() {

                @Override
                public void execute() throws SBonitaException {
                    processManagementAPIImplDelegate.disableProcess(processDefinitionId);
                }
            });
        } catch (final SProcessDefinitionNotFoundException e) {
            throw new ProcessDefinitionNotFoundException(e);
        } catch (final SBonitaException e) {
            throw new ProcessActivationException(e);
        }
        deleteProcess(processDefinitionId);
    }

    @Override
    public void disableAndDeleteProcessDefinition(final long processDefinitionId) throws ProcessDefinitionNotFoundException, ProcessActivationException,
            DeletionException {
        disableProcess(processDefinitionId);
        deleteProcessDefinition(processDefinitionId);
    }

    @Override
    public void disableProcess(final long processDefinitionId) throws ProcessDefinitionNotFoundException, ProcessActivationException {
        try {
            processManagementAPIImplDelegate.disableProcess(processDefinitionId);
        } catch (final SProcessDefinitionNotFoundException e) {
            throw new ProcessDefinitionNotFoundException(e);
        } catch (final SBonitaException e) {
            throw new ProcessActivationException(e);
        }
    }

    @Override
    public void enableProcess(final long processDefinitionId) throws ProcessDefinitionNotFoundException, ProcessEnablementException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final ProcessDefinitionService processDefinitionService = tenantAccessor.getProcessDefinitionService();
        final EventsHandler eventsHandler = tenantAccessor.getEventsHandler();
        try {
            final EnableProcess enableProcess = new EnableProcess(processDefinitionService, processDefinitionId, eventsHandler,
                    tenantAccessor.getTechnicalLoggerService(), SessionInfos.getUserNameFromSession());
            enableProcess.execute();
        } catch (final SProcessDefinitionNotFoundException e) {
            throw new ProcessDefinitionNotFoundException(e);
        } catch (final SBonitaException sbe) {
            throw new ProcessEnablementException(sbe);
        } catch (final Exception e) {
            throw new ProcessEnablementException(e);
        }
    }

    @CustomTransactions
    @Override
    public void executeFlowNode(final long flownodeInstanceId) throws FlowNodeExecutionException {
        executeFlowNode(0, flownodeInstanceId, true);
    }

    @CustomTransactions
    @Override
    public void executeFlowNode(final long userId, final long flownodeInstanceId) throws FlowNodeExecutionException {
        executeFlowNode(userId, flownodeInstanceId, true);
    }

    protected void executeFlowNode(final long userId, final long flownodeInstanceId, final boolean wrapInTransaction) throws FlowNodeExecutionException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final ProcessExecutor processExecutor = tenantAccessor.getProcessExecutor();
        final ActivityInstanceService activityInstanceService = tenantAccessor.getActivityInstanceService();
        final LockService lockService = tenantAccessor.getLockService();
        final TechnicalLoggerService logger = tenantAccessor.getTechnicalLoggerService();
        final TransactionContent transactionContent = new TransactionContent() {

            @Override
            public void execute() throws SBonitaException {
                final SSession session = SessionInfos.getSession();
                final long starterId;
                if (userId == 0) {
                    // the current user or SYSTEM
                    starterId = session != null ? session.getUserId() : -1;
                } else {
                    starterId = userId;
                }

                final SFlowNodeInstance flowNodeInstance = activityInstanceService.getFlowNodeInstance(flownodeInstanceId);
                final boolean isFirstState = flowNodeInstance.getStateId() == 0;
                // no need to handle failed state, all is in the same tx, if the node fail we just have an exception on client side + rollback
                processExecutor.executeFlowNode(flownodeInstanceId, null, null, flowNodeInstance.getParentProcessInstanceId(), starterId, session.getId());
                if (logger.isLoggable(getClass(), TechnicalLogSeverity.INFO) && !isFirstState /* don't log when create subtask */) {
                    final String message = LogMessageBuilder.builUserActionPrefix(session, starterId) + "has performed the task"
                            + LogMessageBuilder.buildFlowNodeContextMessage(flowNodeInstance);
                    logger.log(getClass(), TechnicalLogSeverity.INFO, message);
                }
            }

        };

        try {
            final GetFlowNodeInstance getFlowNodeInstance = new GetFlowNodeInstance(activityInstanceService, flownodeInstanceId);
            executeTransactionContent(tenantAccessor, getFlowNodeInstance, wrapInTransaction);
            final BonitaLock lock = lockService.lock(getFlowNodeInstance.getResult().getParentProcessInstanceId(), SFlowElementsContainerType.PROCESS.name(),
                    tenantAccessor.getTenantId());
            try {
                executeTransactionContent(tenantAccessor, transactionContent, wrapInTransaction);
            } finally {
                lockService.unlock(lock, tenantAccessor.getTenantId());
            }
        } catch (final SBonitaException e) {
            throw new FlowNodeExecutionException(e);
        }

    }

    private void executeTransactionContent(final TenantServiceAccessor tenantAccessor, final TransactionContent transactionContent,
            final boolean wrapInTransaction) throws SBonitaException {
        if (wrapInTransaction) {
            final TransactionExecutor transactionExecutor = tenantAccessor.getTransactionExecutor();
            transactionExecutor.execute(transactionContent);
        } else {
            transactionContent.execute();
        }
    }

    @Override
    public List<ActivityInstance> getActivities(final long processInstanceId, final int startIndex, final int maxResults) {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final ActivityInstanceService activityInstanceService = tenantAccessor.getActivityInstanceService();
        final FlowNodeStateManager flowNodeStateManager = tenantAccessor.getFlowNodeStateManager();
        try {
            return ModelConvertor.toActivityInstances(
                    activityInstanceService.getActivityInstances(processInstanceId, startIndex, maxResults, "id", OrderByType.ASC), flowNodeStateManager);
        } catch (final SBonitaException e) {
            throw new RetrieveException(e);
        }
    }

    @Override
    public long getNumberOfProcessDeploymentInfos() {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final ProcessDefinitionService processDefinitionService = tenantAccessor.getProcessDefinitionService();
        final TransactionContentWithResult<Long> transactionContentWithResult = new GetNumberOfProcessDeploymentInfos(processDefinitionService);
        try {
            transactionContentWithResult.execute();
            return transactionContentWithResult.getResult();
        } catch (final SBonitaException e) {
            throw new RetrieveException(e);
        }
    }

    @Override
    public ProcessDefinition getProcessDefinition(final long processDefinitionId) throws ProcessDefinitionNotFoundException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final ProcessDefinitionService processDefinitionService = tenantAccessor.getProcessDefinitionService();
        try {
            final SProcessDefinition sProcessDefinition = processDefinitionService.getProcessDefinition(processDefinitionId);
            return ModelConvertor.toProcessDefinition(sProcessDefinition);
        } catch (final SProcessDefinitionNotFoundException e) {
            throw new ProcessDefinitionNotFoundException(e);
        } catch (final SProcessDefinitionReadException e) {
            throw new RetrieveException(e);
        }
    }

    @Override
    public DesignProcessDefinition getDesignProcessDefinition(final long processDefinitionId) throws ProcessDefinitionNotFoundException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        try {
            final File processFolder = getProcessFolder(processDefinitionId, tenantAccessor.getTenantId());
            final File processDesignFile = new File(processFolder, ProcessDefinitionBARContribution.PROCESS_DEFINITION_XML);
            final ProcessDefinitionBARContribution processDefinitionBARContribution = new ProcessDefinitionBARContribution();
            return processDefinitionBARContribution.deserializeProcessDefinition(processDesignFile);
        } catch (final BonitaHomeNotSetException e) {
            throw new ProcessDefinitionNotFoundException(e);
        } catch (final InvalidBusinessArchiveFormatException e) {
            throw new ProcessDefinitionNotFoundException(e);
        } catch (final IOException e) {
            throw new ProcessDefinitionNotFoundException(processDefinitionId, e);
        }
    }

    @Override
    public ProcessDeploymentInfo getProcessDeploymentInfo(final long processDefinitionId) throws ProcessDefinitionNotFoundException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final ProcessDefinitionService processDefinitionService = tenantAccessor.getProcessDefinitionService();
        try {
            final TransactionContentWithResult<SProcessDefinitionDeployInfo> transactionContentWithResult = new GetProcessDefinitionDeployInfo(
                    processDefinitionId, processDefinitionService);
            transactionContentWithResult.execute();
            return ModelConvertor.toProcessDeploymentInfo(transactionContentWithResult.getResult());
        } catch (final SProcessDefinitionNotFoundException e) {
            throw new ProcessDefinitionNotFoundException(e);
        } catch (final SBonitaException e) {
            throw new RetrieveException(e);
        }
    }

    private void log(final TenantServiceAccessor tenantAccessor, final Exception e) {
        final TechnicalLoggerService logger = tenantAccessor.getTechnicalLoggerService();
        if (logger.isLoggable(getClass(), TechnicalLogSeverity.ERROR)) {
            logger.log(this.getClass(), TechnicalLogSeverity.ERROR, e);
        }
    }

    @Override
    public ProcessInstance getProcessInstance(final long processInstanceId) throws ProcessInstanceNotFoundException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final ProcessInstanceService processInstanceService = tenantAccessor.getProcessInstanceService();
        final ProcessDefinitionService processDefinitionService = tenantAccessor.getProcessDefinitionService();
        final SearchProcessInstanceDescriptor searchProcessInstanceDescriptor = tenantAccessor.getSearchEntitiesDescriptor()
                .getSearchProcessInstanceDescriptor();
        try {
            final GetProcessInstance getProcessInstance = new GetProcessInstance(processInstanceService, processDefinitionService,
                    searchProcessInstanceDescriptor, processInstanceId);
            getProcessInstance.execute();
            return getProcessInstance.getResult();
        } catch (final SProcessInstanceNotFoundException notFound) {
            throw new ProcessInstanceNotFoundException(notFound);
        } catch (final SBonitaException e) {
            throw new RetrieveException(e);
        }
    }

    @Override
    public List<ArchivedProcessInstance> getArchivedProcessInstances(final long processInstanceId, final int startIndex, final int maxResults) {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final ProcessInstanceService processInstanceService = tenantAccessor.getProcessInstanceService();
        final SearchEntitiesDescriptor searchEntitiesDescriptor = tenantAccessor.getSearchEntitiesDescriptor();
        final GetArchivedProcessInstanceList getProcessInstanceList = new GetArchivedProcessInstanceList(processInstanceService, searchEntitiesDescriptor,
                processInstanceId, startIndex, maxResults);
        try {
            getProcessInstanceList.execute();
        } catch (final SBonitaException e) {
            log(tenantAccessor, e);
            throw new RetrieveException(e);
        }
        return getProcessInstanceList.getResult();
    }

    @Override
    public ArchivedProcessInstance getArchivedProcessInstance(final long id) throws ArchivedProcessInstanceNotFoundException, RetrieveException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final ProcessInstanceService processInstanceService = tenantAccessor.getProcessInstanceService();
        try {
            final SAProcessInstance archivedProcessInstance = processInstanceService.getArchivedProcessInstance(id);
            return ModelConvertor.toArchivedProcessInstance(archivedProcessInstance);
        } catch (final SProcessInstanceNotFoundException e) {
            throw new ArchivedProcessInstanceNotFoundException(e);
        } catch (final SBonitaException e) {
            throw new RetrieveException(e);
        }
    }

    @Override
    public ArchivedProcessInstance getFinalArchivedProcessInstance(final long processInstanceId) throws ArchivedProcessInstanceNotFoundException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final ProcessInstanceService processInstanceService = tenantAccessor.getProcessInstanceService();

        final GetLastArchivedProcessInstance getProcessInstance = new GetLastArchivedProcessInstance(processInstanceService, processInstanceId,
                tenantAccessor.getSearchEntitiesDescriptor());
        try {
            getProcessInstance.execute();
        } catch (final SProcessInstanceNotFoundException e) {
            log(tenantAccessor, e);
            throw new ArchivedProcessInstanceNotFoundException(e);
        } catch (final SBonitaException e) {
            log(tenantAccessor, e);
            throw new RetrieveException(e);
        }
        return getProcessInstance.getResult();
    }

    @Override
    public ProcessInstance startProcess(final long processDefinitionId) throws ProcessActivationException, ProcessExecutionException {
        try {
            return startProcess(getUserId(), processDefinitionId);
        } catch (final ProcessDefinitionNotFoundException e) {
            throw new ProcessExecutionException(e);
        }
    }

    @Override
    public ProcessInstance startProcess(final long userId, final long processDefinitionId) throws ProcessDefinitionNotFoundException,
            ProcessExecutionException, ProcessActivationException {
        return startProcess(userId, processDefinitionId, null, null);
    }

    @Override
    public int getNumberOfActors(final long processDefinitionId) throws ProcessDefinitionNotFoundException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final ProcessDefinitionService processDefinitionService = tenantAccessor.getProcessDefinitionService();

        final GetNumberOfActors getNumberofActors = new GetNumberOfActors(processDefinitionService, processDefinitionId);
        try {
            getNumberofActors.execute();
        } catch (final SBonitaException e) {
            throw new ProcessDefinitionNotFoundException(e);
        }
        return getNumberofActors.getResult();
    }

    @Override
    public List<ActorInstance> getActors(final long processDefinitionId, final int startIndex, final int maxResults, final ActorCriterion sort) {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final ActorMappingService actorMappingService = tenantAccessor.getActorMappingService();
        try {
            final GetActorsByPagination getActorsByPaging = new GetActorsByPagination(actorMappingService, processDefinitionId, startIndex, maxResults, sort);
            getActorsByPaging.execute();
            return ModelConvertor.toActors(getActorsByPaging.getResult());
        } catch (final SBonitaException e) {
            throw new RetrieveException(e);
        }
    }

    @Override
    public List<ActorMember> getActorMembers(final long actorId, final int startIndex, final int maxResults) {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final ActorMappingService actorMappingService = tenantAccessor.getActorMappingService();

        try {
            final GetActorMembers getActorMembers = new GetActorMembers(actorMappingService, actorId, startIndex, maxResults);
            getActorMembers.execute();
            return ModelConvertor.toActorMembers(getActorMembers.getResult());
        } catch (final SBonitaException e) {
            throw new RetrieveException(e);
        }

    }

    @Override
    public long getNumberOfActorMembers(final long actorId) {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final ActorMappingService actorMappingService = tenantAccessor.getActorMappingService();
        final GetNumberOfActorMembers numberOfActorMembers = new GetNumberOfActorMembers(actorMappingService, actorId);
        try {
            numberOfActorMembers.execute();
            return numberOfActorMembers.getResult();
        } catch (final SBonitaException sbe) {
            return 0; // FIXME throw retrieve exception
        }
    }

    @Override
    public long getNumberOfUsersOfActor(final long actorId) {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final ActorMappingService actorMappingService = tenantAccessor.getActorMappingService();
        final GetNumberOfUsersOfActor numberOfUsersOfActor = new GetNumberOfUsersOfActor(actorMappingService, actorId);
        numberOfUsersOfActor.execute();
        return numberOfUsersOfActor.getResult();
    }

    @Override
    public long getNumberOfRolesOfActor(final long actorId) {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final ActorMappingService actorMappingService = tenantAccessor.getActorMappingService();
        final GetNumberOfRolesOfActor numberOfRolesOfActor = new GetNumberOfRolesOfActor(actorMappingService, actorId);
        numberOfRolesOfActor.execute();
        return numberOfRolesOfActor.getResult();
    }

    @Override
    public long getNumberOfGroupsOfActor(final long actorId) {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final ActorMappingService actorMappingService = tenantAccessor.getActorMappingService();
        final GetNumberOfGroupsOfActor numberOfGroupsOfActor = new GetNumberOfGroupsOfActor(actorMappingService, actorId);
        numberOfGroupsOfActor.execute();
        return numberOfGroupsOfActor.getResult();
    }

    @Override
    public long getNumberOfMembershipsOfActor(final long actorId) {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final ActorMappingService actorMappingService = tenantAccessor.getActorMappingService();
        final GetNumberOfMembershipsOfActor getNumber = new GetNumberOfMembershipsOfActor(actorMappingService, actorId);
        getNumber.execute();
        return getNumber.getResult();
    }

    @Override
    public ActorInstance updateActor(final long actorId, final ActorUpdater descriptor) throws ActorNotFoundException, UpdateException {
        if (descriptor == null || descriptor.getFields().isEmpty()) {
            throw new UpdateException("The update descriptor does not contain field updates");
        }
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final ActorMappingService actorMappingService = tenantAccessor.getActorMappingService();
        final SActorUpdateBuilder actorUpdateBuilder = BuilderFactory.get(SActorUpdateBuilderFactory.class).createNewInstance();
        final Map<ActorField, Serializable> fields = descriptor.getFields();
        for (final Entry<ActorField, Serializable> field : fields.entrySet()) {
            switch (field.getKey()) {
                case DISPLAY_NAME:
                    actorUpdateBuilder.updateDisplayName((String) field.getValue());
                    break;
                case DESCRIPTION:
                    actorUpdateBuilder.updateDescription((String) field.getValue());
                    break;
                default:
                    break;
            }
        }
        final EntityUpdateDescriptor updateDescriptor = actorUpdateBuilder.done();
        SActor updateActor;
        try {
            updateActor = actorMappingService.updateActor(actorId, updateDescriptor);
            return ModelConvertor.toActorInstance(updateActor);
        } catch (final SActorNotFoundException e) {
            throw new ActorNotFoundException(e);
        } catch (final SBonitaException e) {
            throw new UpdateException(e);
        }
    }

    @Override
    public ActorMember addUserToActor(final long actorId, final long userId) throws CreationException, AlreadyExistsException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final ActorMappingService actorMappingService = tenantAccessor.getActorMappingService();
        ActorMember clientActorMember;
        long processDefinitionId;
        try {
            final SActorMember actorMember = actorMappingService.addUserToActor(actorId, userId);
            processDefinitionId = actorMappingService.getActor(actorId).getScopeId();
            clientActorMember = ModelConvertor.toActorMember(actorMember);
        } catch (final SBonitaException sbe) {
            throw new CreationException(sbe);
        }
        tenantAccessor.getDependencyResolver().resolveDependencies(processDefinitionId, tenantAccessor);
        return clientActorMember;
    }

    @Override
    public ActorMember addUserToActor(final String actorName, final ProcessDefinition processDefinition, final long userId) throws CreationException,
            AlreadyExistsException, ActorNotFoundException {
        final List<ActorInstance> actors = getActors(processDefinition.getId(), 0, Integer.MAX_VALUE, ActorCriterion.NAME_ASC);
        for (final ActorInstance ai : actors) {
            if (actorName.equals(ai.getName())) {
                return addUserToActor(ai.getId(), userId);
            }
        }
        throw new ActorNotFoundException("Actor " + actorName + " not found in process definition " + processDefinition.getName());
    }

    @Override
    public ActorMember addGroupToActor(final long actorId, final long groupId) throws CreationException, AlreadyExistsException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final ActorMappingService actorMappingService = tenantAccessor.getActorMappingService();
        ActorMember clientActorMember;
        long processDefinitionId;
        try {
            checkAlreadyExistingGroupMapping(actorId, groupId, actorMappingService);
            final SActorMember actorMember = actorMappingService.addGroupToActor(actorId, groupId);
            processDefinitionId = actorMappingService.getActor(actorId).getScopeId();
            clientActorMember = ModelConvertor.toActorMember(actorMember);
        } catch (final SActorNotFoundException e) {
            throw new CreationException(e);
        } catch (final SActorMemberCreationException e) {
            throw new CreationException(e);
        } catch (final SBonitaReadException e) {
            throw new CreationException(e);
        }
        tenantAccessor.getDependencyResolver().resolveDependencies(processDefinitionId, tenantAccessor);
        return clientActorMember;
    }

    @Override
    public ActorMember addGroupToActor(final String actorName, final long groupId, final ProcessDefinition processDefinition) throws CreationException,
            AlreadyExistsException, ActorNotFoundException {
        final List<ActorInstance> actors = getActors(processDefinition.getId(), 0, Integer.MAX_VALUE, ActorCriterion.NAME_ASC);
        for (final ActorInstance ai : actors) {
            if (actorName.equals(ai.getName())) {
                return addGroupToActor(ai.getId(), groupId);
            }
        }
        throw new ActorNotFoundException("Actor " + actorName + " not found in process definition " + processDefinition.getName());
    }

    private void checkAlreadyExistingGroupMapping(final long actorId, final long groupId, final ActorMappingService actorMappingService)
            throws AlreadyExistsException, CreationException {
        try {
            List<SActorMember> actorMembersOfGroup;
            int startIndex = 0;
            do {
                actorMembersOfGroup = actorMappingService.getActorMembers(actorId, startIndex, 50);
                for (final SActorMember sActorMember : actorMembersOfGroup) {
                    if (sActorMember.getGroupId() == groupId && sActorMember.getRoleId() == -1 && sActorMember.getUserId() == -1) {
                        throw new AlreadyExistsException("This group / actor mapping already exists");
                    }
                }
                startIndex += 50;
            } while (actorMembersOfGroup.size() > 0);
        } catch (final SBonitaReadException e2) {
            throw new CreationException("Read problem when checking for duplicate actor member", e2);
        }
    }

    @Override
    public ActorMember addRoleToActor(final long actorId, final long roleId) throws CreationException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final ActorMappingService actorMappingService = tenantAccessor.getActorMappingService();
        ActorMember clientActorMember;
        long processDefinitionId;
        try {
            final SActorMember actorMember = actorMappingService.addRoleToActor(actorId, roleId);
            processDefinitionId = actorMappingService.getActor(actorId).getScopeId();
            clientActorMember = ModelConvertor.toActorMember(actorMember);
        } catch (final SBonitaException sbe) {
            throw new CreationException(sbe);
        }
        tenantAccessor.getDependencyResolver().resolveDependencies(processDefinitionId, tenantAccessor);
        return clientActorMember;
    }

    @Override
    public ActorMember addRoleToActor(final String actorName, final ProcessDefinition processDefinition, final long roleId) throws ActorNotFoundException,
            CreationException {
        final List<ActorInstance> actors = getActors(processDefinition.getId(), 0, Integer.MAX_VALUE, ActorCriterion.NAME_ASC);
        for (final ActorInstance ai : actors) {
            if (actorName.equals(ai.getName())) {
                return addRoleToActor(ai.getId(), roleId);
            }
        }
        throw new ActorNotFoundException("Actor " + actorName + " not found in process definition " + processDefinition.getName());
    }

    @Override
    public ActorMember addRoleAndGroupToActor(final String actorName, final ProcessDefinition processDefinition, final long roleId, final long groupId)
            throws ActorNotFoundException, CreationException {
        final List<ActorInstance> actors = getActors(processDefinition.getId(), 0, Integer.MAX_VALUE, ActorCriterion.NAME_ASC);
        for (final ActorInstance ai : actors) {
            if (actorName.equals(ai.getName())) {
                return addRoleAndGroupToActor(ai.getId(), roleId, groupId);
            }
        }
        throw new ActorNotFoundException("Actor " + actorName + " not found in process definition " + processDefinition.getName());
    }

    @Override
    public ActorMember addRoleAndGroupToActor(final long actorId, final long roleId, final long groupId) throws CreationException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final ActorMappingService actorMappingService = tenantAccessor.getActorMappingService();
        ActorMember clientActorMember;
        long processDefinitionId;
        try {
            final SActorMember actorMember = actorMappingService.addRoleAndGroupToActor(actorId, roleId, groupId);
            processDefinitionId = actorMappingService.getActor(actorId).getScopeId();
            clientActorMember = ModelConvertor.toActorMember(actorMember);
        } catch (final SBonitaException sbe) {
            throw new CreationException(sbe);
        }
        tenantAccessor.getDependencyResolver().resolveDependencies(processDefinitionId, tenantAccessor);
        return clientActorMember;
    }

    @Override
    public void removeActorMember(final long actorMemberId) throws DeletionException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final ActorMappingService actorMappingService = tenantAccessor.getActorMappingService();
        final RemoveActorMember removeActorMember = new RemoveActorMember(actorMappingService, actorMemberId);
        // FIXME remove an actor member when process is running!
        try {
            removeActorMember.execute();
            final SActorMember actorMember = removeActorMember.getResult();
            final long processDefinitionId = getActor(actorMember.getActorId()).getProcessDefinitionId();
            tenantAccessor.getDependencyResolver().resolveDependencies(processDefinitionId, tenantAccessor);
        } catch (final SBonitaException sbe) {
            throw new DeletionException(sbe);
        } catch (final ActorNotFoundException e) {
            throw new DeletionException(e);
        }
    }

    @Override
    public ActorInstance getActor(final long actorId) throws ActorNotFoundException {
        try {
            final TenantServiceAccessor tenantAccessor = getTenantAccessor();
            final ActorMappingService actorMappingService = tenantAccessor.getActorMappingService();

            final GetActor getActor = new GetActor(actorMappingService, actorId);
            getActor.execute();
            return ModelConvertor.toActorInstance(getActor.getResult());
        } catch (final SBonitaException e) {
            throw new ActorNotFoundException(e);
        }
    }

    @Override
    public ActivityInstance getActivityInstance(final long activityInstanceId) throws ActivityInstanceNotFoundException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final ActivityInstanceService activityInstanceService = tenantAccessor.getActivityInstanceService();
        final FlowNodeStateManager flowNodeStateManager = tenantAccessor.getFlowNodeStateManager();
        final GetActivityInstance getActivityInstance = new GetActivityInstance(activityInstanceService, activityInstanceId);
        try {
            getActivityInstance.execute();
        } catch (final SActivityInstanceNotFoundException e) {
            throw new ActivityInstanceNotFoundException(activityInstanceId);
        } catch (final SBonitaException e) {
            throw new RetrieveException(e);
        }
        return ModelConvertor.toActivityInstance(getActivityInstance.getResult(), flowNodeStateManager);
    }

    @Override
    public FlowNodeInstance getFlowNodeInstance(final long flowNodeInstanceId) throws FlowNodeInstanceNotFoundException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final ActivityInstanceService activityInstanceService = tenantAccessor.getActivityInstanceService();
        final FlowNodeStateManager flowNodeStateManager = tenantAccessor.getFlowNodeStateManager();
        try {
            SFlowNodeInstance flowNodeInstance = activityInstanceService.getFlowNodeInstance(flowNodeInstanceId);
            return ModelConvertor.toFlowNodeInstance(flowNodeInstance, flowNodeStateManager);
        } catch (final SFlowNodeNotFoundException e) {
            throw new FlowNodeInstanceNotFoundException(e);
        } catch (final SBonitaException e) {
            throw new RetrieveException(e);
        }
    }

    @Override
    public List<HumanTaskInstance> getAssignedHumanTaskInstances(final long userId, final int startIndex, final int maxResults,
            final ActivityInstanceCriterion pagingCriterion) {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final FlowNodeStateManager flowNodeStateManager = tenantAccessor.getFlowNodeStateManager();
        final OrderAndField orderAndField = OrderAndFields.getOrderAndFieldForActivityInstance(pagingCriterion);

        final ActivityInstanceService instanceService = tenantAccessor.getActivityInstanceService();
        try {
            final GetAssignedTasks getAssignedTasks = new GetAssignedTasks(instanceService, userId, startIndex, maxResults, orderAndField.getField(),
                    orderAndField.getOrder());
            getAssignedTasks.execute();
            final List<SHumanTaskInstance> assignedTasks = getAssignedTasks.getResult();
            return ModelConvertor.toHumanTaskInstances(assignedTasks, flowNodeStateManager);
        } catch (final SUserNotFoundException e) {
            return Collections.emptyList();
        } catch (final SBonitaException e) {
            return Collections.emptyList();
        }
    }

    @Override
    public List<HumanTaskInstance> getPendingHumanTaskInstances(final long userId, final int startIndex, final int maxResults,
            final ActivityInstanceCriterion pagingCriterion) {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final ActorMappingService actorMappingService = tenantAccessor.getActorMappingService();
        final FlowNodeStateManager flowNodeStateManager = tenantAccessor.getFlowNodeStateManager();
        final OrderAndField orderAndField = OrderAndFields.getOrderAndFieldForActivityInstance(pagingCriterion);

        final ProcessDefinitionService definitionService = tenantAccessor.getProcessDefinitionService();
        final ActivityInstanceService instanceService = tenantAccessor.getActivityInstanceService();
        try {
            final Set<Long> actorIds = getActorsForUser(userId, actorMappingService, definitionService);
            final List<SHumanTaskInstance> pendingTasks = instanceService.getPendingTasks(userId, actorIds, startIndex, maxResults, orderAndField.getField(),
                    orderAndField.getOrder());
            return ModelConvertor.toHumanTaskInstances(pendingTasks, flowNodeStateManager);
        } catch (final SBonitaException e) {
            return Collections.emptyList();
        }
    }

    private Set<Long> getActorsForUser(final long userId, final ActorMappingService actorMappingService, final ProcessDefinitionService definitionService)
            throws SBonitaReadException, SProcessDefinitionReadException {
        final long numberOfProcesses = definitionService.getNumberOfProcessDeploymentInfo(ActivationState.ENABLED);
        final List<Long> processDefIds = definitionService.getProcessDefinitionIds(ActivationState.ENABLED, 0, numberOfProcesses);// FIXME dirty....
        final HashSet<Long> processDefinitionIds = new HashSet<Long>(processDefIds);
        if (processDefinitionIds.isEmpty()) {
            return Collections.emptySet();
        }
        final List<SActor> actors = actorMappingService.getActors(processDefinitionIds, userId);
        if (actors.isEmpty()) {
            return Collections.emptySet();
        }
        final Set<Long> actorIds = new HashSet<Long>();
        for (final SActor sActor : actors) {
            actorIds.add(sActor.getId());
        }
        return actorIds;
    }

    @Override
    public ArchivedActivityInstance getArchivedActivityInstance(final long activityInstanceId) throws ActivityInstanceNotFoundException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final ActivityInstanceService activityInstanceService = tenantAccessor.getActivityInstanceService();
        final FlowNodeStateManager flowNodeStateManager = tenantAccessor.getFlowNodeStateManager();
        final GetArchivedActivityInstance getActivityInstance = new GetArchivedActivityInstance(activityInstanceService, activityInstanceId);
        try {
            getActivityInstance.execute();
        } catch (final SActivityInstanceNotFoundException e) {
            throw new ActivityInstanceNotFoundException(activityInstanceId, e);
        } catch (final SBonitaException e) {
            throw new RetrieveException(e);
        }
        return ModelConvertor.toArchivedActivityInstance(getActivityInstance.getResult(), flowNodeStateManager);
    }

    @Override
    public ArchivedFlowNodeInstance getArchivedFlowNodeInstance(final long archivedFlowNodeInstanceId) throws ArchivedFlowNodeInstanceNotFoundException,
            RetrieveException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final ActivityInstanceService activityInstanceService = tenantAccessor.getActivityInstanceService();
        final ArchiveService archiveService = tenantAccessor.getArchiveService();
        final ReadPersistenceService persistenceService = archiveService.getDefinitiveArchiveReadPersistenceService();
        final FlowNodeStateManager flowNodeStateManager = tenantAccessor.getFlowNodeStateManager();

        try {
            final SAFlowNodeInstance archivedFlowNodeInstance = activityInstanceService.getArchivedFlowNodeInstance(archivedFlowNodeInstanceId,
                    persistenceService);
            return ModelConvertor.toArchivedFlowNodeInstance(archivedFlowNodeInstance, flowNodeStateManager);
        } catch (final SFlowNodeNotFoundException e) {
            throw new ArchivedFlowNodeInstanceNotFoundException(archivedFlowNodeInstanceId);
        } catch (final SFlowNodeReadException e) {
            throw new RetrieveException(e);
        }
    }

    @Override
    public List<ProcessInstance> getProcessInstances(final int startIndex, final int maxResults, final ProcessInstanceCriterion criterion) {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final OrderAndField orderAndField = OrderAndFields.getOrderAndFieldForProcessInstance(criterion);
        final SearchOptionsBuilder searchOptionsBuilder = new SearchOptionsBuilder(startIndex, maxResults);
        searchOptionsBuilder.sort(orderAndField.getField(), Order.valueOf(orderAndField.getOrder().name()));
        List<ProcessInstance> result;
        try {
            result = searchProcessInstances(tenantAccessor, searchOptionsBuilder.done()).getResult();
        } catch (final SSearchException e) {
            result = Collections.emptyList();
        }
        return result;
    }

    @Override
    public long getNumberOfProcessInstances() {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final ProcessDefinitionService processDefinitionService = tenantAccessor.getProcessDefinitionService();
        final ProcessInstanceService processInstanceService = tenantAccessor.getProcessInstanceService();
        final SearchEntitiesDescriptor searchEntitiesDescriptor = tenantAccessor.getSearchEntitiesDescriptor();

        try {
            final TransactionContentWithResult<Long> transactionContent = new GetNumberOfProcessInstance(processInstanceService, processDefinitionService,
                    searchEntitiesDescriptor);
            transactionContent.execute();
            return transactionContent.getResult();
        } catch (final SBonitaException e) {
            throw new RetrieveException(e);
        }
    }

    protected SearchResult<ProcessInstance> searchProcessInstances(final TenantServiceAccessor tenantAccessor, final SearchOptions searchOptions)
            throws SSearchException {

        final ProcessDefinitionService processDefinitionService = tenantAccessor.getProcessDefinitionService();
        final ProcessInstanceService processInstanceService = tenantAccessor.getProcessInstanceService();
        final SearchEntitiesDescriptor searchEntitiesDescriptor = tenantAccessor.getSearchEntitiesDescriptor();

        try {
            final SearchProcessInstances searchProcessInstances = new SearchProcessInstances(processInstanceService,
                    searchEntitiesDescriptor.getSearchProcessInstanceDescriptor(), searchOptions, processDefinitionService);
            searchProcessInstances.execute();
            return searchProcessInstances.getResult();
        } catch (final SBonitaException sbe) {
            throw new SSearchException(sbe);
        }
    }

    @Override
    public List<ArchivedProcessInstance> getArchivedProcessInstances(final int startIndex, final int maxResults, final ProcessInstanceCriterion criterion) {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final OrderAndField orderAndField = OrderAndFields.getOrderAndFieldForProcessInstance(criterion);

        final SearchOptionsBuilder searchOptionsBuilder = new SearchOptionsBuilder(startIndex, maxResults);
        searchOptionsBuilder.sort(orderAndField.getField(), Order.valueOf(orderAndField.getOrder().name()));
        searchOptionsBuilder.filter(ArchivedProcessInstancesSearchDescriptor.STATE_ID, ProcessInstanceState.COMPLETED.getId());
        searchOptionsBuilder.filter(ArchivedProcessInstancesSearchDescriptor.CALLER_ID, -1);
        final SearchArchivedProcessInstances searchArchivedProcessInstances = searchArchivedProcessInstances(tenantAccessor, searchOptionsBuilder.done());
        try {
            searchArchivedProcessInstances.execute();
        } catch (final SBonitaException e) {
            throw new RetrieveException(e);
        }

        return searchArchivedProcessInstances.getResult().getResult();
    }

    private SearchArchivedProcessInstances searchArchivedProcessInstances(final TenantServiceAccessor tenantAccessor, final SearchOptions searchOptions)
            throws RetrieveException {
        final ProcessInstanceService processInstanceService = tenantAccessor.getProcessInstanceService();
        final SearchEntitiesDescriptor searchEntitiesDescriptor = tenantAccessor.getSearchEntitiesDescriptor();
        return new SearchArchivedProcessInstances(processInstanceService, searchEntitiesDescriptor.getSearchArchivedProcessInstanceDescriptor(), searchOptions);
    }

    @Override
    public long getNumberOfArchivedProcessInstances() {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final ProcessInstanceService processInstanceService = tenantAccessor.getProcessInstanceService();
        final SearchEntitiesDescriptor searchEntitiesDescriptor = tenantAccessor.getSearchEntitiesDescriptor();

        try {
            final TransactionContentWithResult<Long> transactionContent = new GetNumberOfArchivedProcessInstance(processInstanceService,
                    searchEntitiesDescriptor);
            transactionContent.execute();
            return transactionContent.getResult();
        } catch (final SBonitaException e) {
            log(tenantAccessor, e);
            throw new RetrieveException(e);
        }
    }

    @Override
    public SearchResult<ArchivedProcessInstance> searchArchivedProcessInstances(final SearchOptions searchOptions) throws RetrieveException, SearchException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final ProcessInstanceService processInstanceService = tenantAccessor.getProcessInstanceService();
        final SearchEntitiesDescriptor searchEntitiesDescriptor = tenantAccessor.getSearchEntitiesDescriptor();
        final SearchArchivedProcessInstancesWithoutSubProcess searchArchivedProcessInstances = new SearchArchivedProcessInstancesWithoutSubProcess(
                processInstanceService, searchEntitiesDescriptor.getSearchArchivedProcessInstanceDescriptor(), searchOptions);
        try {
            searchArchivedProcessInstances.execute();
        } catch (final SBonitaException e) {
            throw new SearchException(e);
        }
        return searchArchivedProcessInstances.getResult();
    }

    @Override
    public SearchResult<ArchivedProcessInstance> searchArchivedProcessInstancesInAllStates(final SearchOptions searchOptions) throws SearchException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final ProcessInstanceService processInstanceService = tenantAccessor.getProcessInstanceService();
        final SearchEntitiesDescriptor searchEntitiesDescriptor = tenantAccessor.getSearchEntitiesDescriptor();
        final SearchArchivedProcessInstances searchArchivedProcessInstances = new SearchArchivedProcessInstances(processInstanceService,
                searchEntitiesDescriptor.getSearchArchivedProcessInstanceDescriptor(), searchOptions);
        try {
            searchArchivedProcessInstances.execute();
        } catch (final SBonitaException e) {
            throw new SearchException(e);
        }
        return searchArchivedProcessInstances.getResult();
    }

    @Override
    public List<ActivityInstance> getOpenActivityInstances(final long processInstanceId, final int startIndex, final int maxResults,
            final ActivityInstanceCriterion criterion) {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final ActivityInstanceService activityInstanceService = tenantAccessor.getActivityInstanceService();
        final FlowNodeStateManager flowNodeStateManager = tenantAccessor.getFlowNodeStateManager();

        try {
            final int totalNumber = activityInstanceService.getNumberOfOpenActivityInstances(processInstanceId);
            // If there are no instances, return an empty list:
            if (totalNumber == 0) {
                return Collections.<ActivityInstance> emptyList();
            }
            final OrderAndField orderAndField = OrderAndFields.getOrderAndFieldForActivityInstance(criterion);
            return ModelConvertor.toActivityInstances(
                    activityInstanceService.getOpenActivityInstances(processInstanceId, startIndex, maxResults, orderAndField.getField(),
                            orderAndField.getOrder()), flowNodeStateManager);
        } catch (final SBonitaException e) {
            throw new RetrieveException(e);
        }
    }

    @Override
    public List<ArchivedActivityInstance> getArchivedActivityInstances(final long processInstanceId, final int startIndex, final int maxResults,
            final ActivityInstanceCriterion criterion) {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final List<ArchivedActivityInstance> archivedActivityInstances = getArchivedActivityInstances(processInstanceId, startIndex, maxResults, criterion,
                tenantAccessor);
        return archivedActivityInstances;
    }

    private List<ArchivedActivityInstance> getArchivedActivityInstances(final long processInstanceId, final int pageIndex, final int numberPerPage,
            final ActivityInstanceCriterion pagingCriterion, final TenantServiceAccessor tenantAccessor) throws RetrieveException {

        final ActivityInstanceService activityInstanceService = tenantAccessor.getActivityInstanceService();
        final FlowNodeStateManager flowNodeStateManager = tenantAccessor.getFlowNodeStateManager();
        final OrderAndField orderAndField = OrderAndFields.getOrderAndFieldForActivityInstance(pagingCriterion);
        final GetArchivedActivityInstances getActivityInstances = new GetArchivedActivityInstances(activityInstanceService, processInstanceId, pageIndex,
                numberPerPage, orderAndField.getField(), orderAndField.getOrder());
        try {
            getActivityInstances.execute();
        } catch (final SBonitaException e) {
            log(tenantAccessor, e);
            throw new RetrieveException(e);
        }
        return ModelConvertor.toArchivedActivityInstances(getActivityInstances.getResult(), flowNodeStateManager);
    }

    @Override
    public int getNumberOfOpenedActivityInstances(final long processInstanceId) {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final ActivityInstanceService activityInstanceService = tenantAccessor.getActivityInstanceService();

        final TransactionContentWithResult<Integer> transactionContentWithResult = new GetNumberOfActivityInstance(processInstanceId, activityInstanceService);
        try {
            transactionContentWithResult.execute();
            return transactionContentWithResult.getResult();
        } catch (final SBonitaException e) {
            throw new RetrieveException(e);
        }
    }

    @Override
    public Category createCategory(final String name, final String description) throws AlreadyExistsException, CreationException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final CategoryService categoryService = tenantAccessor.getCategoryService();

        try {
            final CreateCategory createCategory = new CreateCategory(name, description, categoryService);
            createCategory.execute();
            return ModelConvertor.toCategory(createCategory.getResult());
        } catch (final SCategoryAlreadyExistsException scaee) {
            throw new AlreadyExistsException(scaee);
        } catch (final SBonitaException e) {
            throw new CreationException("Category create exception!", e);
        }
    }

    @Override
    public Category getCategory(final long categoryId) throws CategoryNotFoundException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final CategoryService categoryService = tenantAccessor.getCategoryService();
        try {
            final GetCategory getCategory = new GetCategory(categoryService, categoryId);
            getCategory.execute();
            final SCategory sCategory = getCategory.getResult();
            return ModelConvertor.toCategory(sCategory);
        } catch (final SBonitaException sbe) {
            throw new CategoryNotFoundException(sbe);
        }
    }

    @Override
    public long getNumberOfCategories() {
        final CategoryService categoryService = getTenantAccessor().getCategoryService();
        try {
            final GetNumberOfCategories getNumberOfCategories = new GetNumberOfCategories(categoryService);
            getNumberOfCategories.execute();
            return getNumberOfCategories.getResult();
        } catch (final SBonitaException e) {
            return 0;
        }
    }

    @Override
    public List<Category> getCategories(final int startIndex, final int maxResults, final CategoryCriterion sortCriterion) {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final SCategoryBuilderFactory fact = BuilderFactory.get(SCategoryBuilderFactory.class);

        final CategoryService categoryService = tenantAccessor.getCategoryService();
        String field = null;
        OrderByType order = null;
        switch (sortCriterion) {
            case NAME_ASC:
                field = fact.getNameKey();
                order = OrderByType.ASC;
                break;
            case NAME_DESC:
                field = fact.getNameKey();
                order = OrderByType.DESC;
                break;
        }
        try {
            final GetCategories getCategories = new GetCategories(startIndex, maxResults, field, categoryService, order);
            getCategories.execute();
            return ModelConvertor.toCategories(getCategories.getResult());
        } catch (final SBonitaException e) {
            throw new RetrieveException(e);
        }
    }

    @Override
    public void addCategoriesToProcess(final long processDefinitionId, final List<Long> categoryIds) throws AlreadyExistsException, CreationException {
        try {
            final CategoryService categoryService = getTenantAccessor().getCategoryService();
            final ProcessDefinitionService processDefinitionService = getTenantAccessor().getProcessDefinitionService();
            for (final Long categoryId : categoryIds) {
                new AddProcessDefinitionToCategory(categoryId, processDefinitionId, categoryService, processDefinitionService).execute();
            }
        } catch (final SCategoryInProcessAlreadyExistsException scipaee) {
            throw new AlreadyExistsException(scipaee);
        } catch (final SBonitaException sbe) {
            throw new CreationException(sbe);
        }
    }

    @Override
    public void removeCategoriesFromProcess(final long processDefinitionId, final List<Long> categoryIds) throws DeletionException {
        try {
            final CategoryService categoryService = getTenantAccessor().getCategoryService();
            final TransactionContent transactionContent = new RemoveCategoriesFromProcessDefinition(processDefinitionId, categoryIds, categoryService);
            transactionContent.execute();
        } catch (final SBonitaException sbe) {
            throw new DeletionException(sbe);
        }
    }

    @Override
    public void addProcessDefinitionToCategory(final long categoryId, final long processDefinitionId) throws AlreadyExistsException, CreationException {
        try {
            final CategoryService categoryService = getTenantAccessor().getCategoryService();
            final ProcessDefinitionService processDefinitionService = getTenantAccessor().getProcessDefinitionService();
            final TransactionContent transactionContent = new AddProcessDefinitionToCategory(categoryId, processDefinitionId, categoryService,
                    processDefinitionService);
            transactionContent.execute();
        } catch (final SProcessDefinitionNotFoundException e) {
            throw new CreationException(e);
        } catch (final SCategoryNotFoundException scnfe) {
            throw new CreationException(scnfe);
        } catch (final SCategoryInProcessAlreadyExistsException cipaee) {
            throw new AlreadyExistsException(cipaee);
        } catch (final SBonitaException sbe) {
            throw new CreationException(sbe);
        }
    }

    @Override
    public void addProcessDefinitionsToCategory(final long categoryId, final List<Long> processDefinitionIds) throws AlreadyExistsException, CreationException {
        try {
            final CategoryService categoryService = getTenantAccessor().getCategoryService();
            final ProcessDefinitionService processDefinitionService = getTenantAccessor().getProcessDefinitionService();
            for (final Long processDefinitionId : processDefinitionIds) {
                new AddProcessDefinitionToCategory(categoryId, processDefinitionId, categoryService, processDefinitionService).execute();
            }
        } catch (final SProcessDefinitionNotFoundException e) {
            throw new CreationException(e);
        } catch (final SCategoryNotFoundException scnfe) {
            throw new CreationException(scnfe);
        } catch (final SCategoryInProcessAlreadyExistsException cipaee) {
            throw new AlreadyExistsException(cipaee);
        } catch (final SBonitaException sbe) {
            throw new CreationException(sbe);
        }
    }

    @Override
    public long getNumberOfCategories(final long processDefinitionId) {
        try {
            final TenantServiceAccessor tenantAccessor = getTenantAccessor();

            final CategoryService categoryService = tenantAccessor.getCategoryService();
            final GetNumberOfCategoriesOfProcess getNumberOfCategoriesOfProcess = new GetNumberOfCategoriesOfProcess(categoryService, processDefinitionId);
            getNumberOfCategoriesOfProcess.execute();
            return getNumberOfCategoriesOfProcess.getResult();
        } catch (final SBonitaException sbe) {
            throw new RetrieveException(sbe);
        }
    }

    @Override
    public long getNumberOfProcessDefinitionsOfCategory(final long categoryId) {
        try {
            final CategoryService categoryService = getTenantAccessor().getCategoryService();
            final List<Long> ids = categoryService.getProcessDefinitionIdsOfCategory(categoryId);
            if (ids != null) {
                return ids.size();
            }
        } catch (final SBonitaException e) {
            throw new RetrieveException(e);
        }
        return 0;
    }

    @Override
    public List<ProcessDeploymentInfo> getProcessDeploymentInfosOfCategory(final long categoryId, final int startIndex, final int maxResults,
            final ProcessDeploymentInfoCriterion sortCriterion) {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final CategoryService categoryService = tenantAccessor.getCategoryService();

        final ProcessDefinitionService processDefinitionService = tenantAccessor.getProcessDefinitionService();
        final OrderByType order = getOrderByType(sortCriterion);
        List<Long> processDefinitionIds;
        try {
            processDefinitionIds = categoryService.getProcessDefinitionIdsOfCategory(categoryId);
            if (processDefinitionIds != null && processDefinitionIds.size() > 0) {
                final List<SProcessDefinitionDeployInfo> processDefinitionDeployInfoList = processDefinitionService
                        .getProcessDeploymentInfos(processDefinitionIds);
                if (processDefinitionDeployInfoList != null) {
                    Collections.sort(processDefinitionDeployInfoList, new ProcessDefinitionDeployInfoComparator());
                    if (order != null && order == OrderByType.DESC) {
                        Collections.reverse(processDefinitionDeployInfoList);
                    }
                    if (startIndex >= processDefinitionDeployInfoList.size()) {
                        return Collections.emptyList();
                    }
                    final int toIndex = Math.min(processDefinitionDeployInfoList.size(), startIndex + maxResults);
                    return ModelConvertor.toProcessDeploymentInfo(new ArrayList<SProcessDefinitionDeployInfo>(processDefinitionDeployInfoList.subList(
                            startIndex, toIndex)));
                }
            }
            return Collections.emptyList();
        } catch (final SBonitaException e) {
            throw new RetrieveException(e);
        }
    }

    private OrderByType getOrderByType(final ProcessDeploymentInfoCriterion sortCriterion) {
        if (sortCriterion != null) {
            switch (sortCriterion) {
                case NAME_ASC:
                    return OrderByType.ASC;
                case NAME_DESC:
                    return OrderByType.DESC;
                case DEFAULT:
                    return OrderByType.ASC;
                default:
                    return null;
            }
        }
        return null;
    }

    private OrderByType mapPagingCriterionToOrderByType(final CategoryCriterion pagingCriterion) {
        OrderByType order = null;
        switch (pagingCriterion) {
            case NAME_ASC:
                order = OrderByType.ASC;
                break;
            case NAME_DESC:
                order = OrderByType.DESC;
                break;
        }
        return order;
    }

    @Override
    public List<Category> getCategoriesOfProcessDefinition(final long processDefinitionId, final int startIndex, final int maxResults,
            final CategoryCriterion pagingCriterion) {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final CategoryService categoryService = tenantAccessor.getCategoryService();
        try {
            final OrderByType order = mapPagingCriterionToOrderByType(pagingCriterion);
            return ModelConvertor.toCategories(categoryService.getCategoriesOfProcessDefinition(processDefinitionId, startIndex, maxResults, order));
        } catch (final SBonitaException sbe) {
            throw new RetrieveException(sbe);
        }
    }

    @Override
    public List<Category> getCategoriesUnrelatedToProcessDefinition(final long processDefinitionId, final int startIndex, final int maxResults,
            final CategoryCriterion sortingCriterion) {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final CategoryService categoryService = tenantAccessor.getCategoryService();
        try {
            final OrderByType order = mapPagingCriterionToOrderByType(sortingCriterion);
            return ModelConvertor.toCategories(categoryService.getCategoriesUnrelatedToProcessDefinition(processDefinitionId, startIndex, maxResults, order));
        } catch (final SBonitaException sbe) {
            throw new RetrieveException(sbe);
        }
    }

    @Override
    public void updateCategory(final long categoryId, final CategoryUpdater updater) throws CategoryNotFoundException, UpdateException {
        if (updater == null || updater.getFields().isEmpty()) {
            throw new UpdateException("The update descriptor does not contain field updates");
        }
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final CategoryService categoryService = tenantAccessor.getCategoryService();

        try {
            final SCategoryUpdateBuilderFactory fact = BuilderFactory.get(SCategoryUpdateBuilderFactory.class);
            final EntityUpdateDescriptor updateDescriptor = getCategoryUpdateDescriptor(fact.createNewInstance(), updater);
            final UpdateCategory updateCategory = new UpdateCategory(categoryService, categoryId, updateDescriptor);
            updateCategory.execute();
        } catch (final SCategoryNotFoundException scnfe) {
            throw new CategoryNotFoundException(scnfe);
        } catch (final SBonitaException sbe) {
            throw new UpdateException(sbe);
        }
    }

    private EntityUpdateDescriptor getCategoryUpdateDescriptor(final SCategoryUpdateBuilder descriptorBuilder, final CategoryUpdater updater) {
        final Map<CategoryField, Serializable> fields = updater.getFields();
        final String name = (String) fields.get(CategoryField.NAME);
        if (name != null) {
            descriptorBuilder.updateName(name);
        }
        final String description = (String) fields.get(CategoryField.DESCRIPTION);
        if (description != null) {
            descriptorBuilder.updateDescription(description);
        }
        return descriptorBuilder.done();
    }

    @Override
    public void deleteCategory(final long categoryId) throws DeletionException {
        if (categoryId <= 0) {
            throw new DeletionException("Category id can not be less than 0!");
        }
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final CategoryService categoryService = tenantAccessor.getCategoryService();

        final DeleteSCategory deleteSCategory = new DeleteSCategory(categoryService, categoryId);
        try {
            deleteSCategory.execute();
        } catch (final SBonitaException sbe) {
            throw new DeletionException(sbe);
        }
    }

    @Override
    public long getNumberOfUncategorizedProcessDefinitions() {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final ProcessDefinitionService processDefinitionService = tenantAccessor.getProcessDefinitionService();
        final CategoryService categoryService = tenantAccessor.getCategoryService();
        try {
            final long numberOfProcessDefinitions = processDefinitionService.getNumberOfProcessDeploymentInfos();
            final List<Long> processDefinitionIds = processDefinitionService.getProcessDefinitionIds(0, numberOfProcessDefinitions);
            long number;
            if (processDefinitionIds.isEmpty()) {
                number = 0;
            } else {
                number = processDefinitionIds.size() - categoryService.getNumberOfCategorizedProcessIds(processDefinitionIds);
            }
            return number;
        } catch (final SBonitaException e) {
            throw new BonitaRuntimeException(e);// TODO refactor exceptions
        }
    }

    @Override
    public List<ProcessDeploymentInfo> getUncategorizedProcessDeploymentInfos(final int startIndex, final int maxResults,
            final ProcessDeploymentInfoCriterion sortCriterion) {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final ProcessDefinitionService processDefinitionService = tenantAccessor.getProcessDefinitionService();
        final CategoryService categoryService = tenantAccessor.getCategoryService();
        try {
            final long numberOfProcessDefinitions = processDefinitionService.getNumberOfProcessDeploymentInfos();
            final List<Long> processDefinitionIds = processDefinitionService.getProcessDefinitionIds(0, numberOfProcessDefinitions);
            processDefinitionIds.removeAll(categoryService.getCategorizedProcessIds(processDefinitionIds));
            OrderByType order;
            switch (sortCriterion) {
                case NAME_ASC:
                    order = OrderByType.ASC;
                    break;
                case NAME_DESC:
                    order = OrderByType.DESC;
                    break;
                case DEFAULT:
                    order = OrderByType.ASC;
                    break;
                default:
                    order = null;
            }
            final List<SProcessDefinitionDeployInfo> processDefinitionDeployInfos = processDefinitionService.getProcessDeploymentInfos(processDefinitionIds,
                    startIndex, maxResults, "name", order);
            return ModelConvertor.toProcessDeploymentInfo(processDefinitionDeployInfos);
        } catch (final SBonitaException sbe) {
            throw new RetrieveException(sbe);
        }
    }

    @Override
    public long getNumberOfProcessDeploymentInfosUnrelatedToCategory(final long categoryId) {
        try {
            final TenantServiceAccessor tenantAccessor = getTenantAccessor();
            final ProcessDefinitionService processDefinitionService = tenantAccessor.getProcessDefinitionService();

            final GetNumberOfProcessDeploymentInfosUnrelatedToCategory transactionContentWithResult = new GetNumberOfProcessDeploymentInfosUnrelatedToCategory(
                    categoryId, processDefinitionService);
            transactionContentWithResult.execute();
            return transactionContentWithResult.getResult();
        } catch (final SBonitaException sbe) {
            throw new RetrieveException(sbe);
        }
    }

    @Override
    public List<ProcessDeploymentInfo> getProcessDeploymentInfosUnrelatedToCategory(final long categoryId, final int startIndex, final int maxResults,
            final ProcessDeploymentInfoCriterion sortingCriterion) {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final ProcessDefinitionService processDefinitionService = tenantAccessor.getProcessDefinitionService();

        try {
            return ModelConvertor.toProcessDeploymentInfo(processDefinitionService.getProcessDeploymentInfosUnrelatedToCategory(categoryId, startIndex,
                    maxResults, sortingCriterion));
        } catch (final SBonitaException e) {
            throw new RetrieveException(e);
        }
    }

    @Deprecated
    @Override
    public void removeAllCategoriesFromProcessDefinition(final long processDefinitionId) throws DeletionException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final CategoryService categoryService = tenantAccessor.getCategoryService();
        final TransactionContent transactionContent = new RemoveProcessDefinitionsOfCategory(processDefinitionId, categoryService);
        try {
            transactionContent.execute();
        } catch (final SBonitaException sbe) {
            throw new DeletionException(sbe);
        }
    }

    @Deprecated
    @Override
    public void removeAllProcessDefinitionsFromCategory(final long categoryId) throws DeletionException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final CategoryService categoryService = tenantAccessor.getCategoryService();

        final RemoveProcessDefinitionsOfCategory remove = new RemoveProcessDefinitionsOfCategory(categoryService, categoryId);
        try {
            remove.execute();;
        } catch (final SBonitaException sbe) {
            throw new DeletionException(sbe);
        }
    }

    @Override
    public long removeCategoriesFromProcessDefinition(final long processDefinitionId, final int startIndex, final int maxResults) throws DeletionException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final CategoryService categoryService = tenantAccessor.getCategoryService();
        final SProcessCategoryMappingBuilderFactory fact = BuilderFactory.get(SProcessCategoryMappingBuilderFactory.class);

        try {
            final FilterOption filterOption = new FilterOption(SProcessCategoryMapping.class, fact.getProcessIdKey(), processDefinitionId);
            final OrderByOption order = new OrderByOption(SProcessCategoryMapping.class, fact.getIdKey(), OrderByType.ASC);
            final QueryOptions queryOptions = new QueryOptions(startIndex, maxResults, Collections.singletonList(order),
                    Collections.singletonList(filterOption), null);
            final List<SProcessCategoryMapping> processCategoryMappings = categoryService.searchProcessCategoryMappings(queryOptions);
            return categoryService.deleteProcessCategoryMappings(processCategoryMappings);
        } catch (final SBonitaException e) {
            throw new DeletionException(e);
        }
    }

    @Override
    public long removeProcessDefinitionsFromCategory(final long categoryId, final int startIndex, final int maxResults) throws DeletionException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final CategoryService categoryService = tenantAccessor.getCategoryService();
        final SProcessCategoryMappingBuilderFactory fact = BuilderFactory.get(SProcessCategoryMappingBuilderFactory.class);

        try {
            final FilterOption filterOption = new FilterOption(SProcessCategoryMapping.class, fact.getCategoryIdKey(), categoryId);
            final OrderByOption order = new OrderByOption(SProcessCategoryMapping.class, fact.getIdKey(), OrderByType.ASC);
            final QueryOptions queryOptions = new QueryOptions(startIndex, maxResults, Collections.singletonList(order),
                    Collections.singletonList(filterOption), null);
            final List<SProcessCategoryMapping> processCategoryMappings = categoryService.searchProcessCategoryMappings(queryOptions);
            return categoryService.deleteProcessCategoryMappings(processCategoryMappings);
        } catch (final SBonitaException e) {
            throw new DeletionException(e);
        }
    }

    @Override
    public List<EventInstance> getEventInstances(final long rootContainerId, final int startIndex, final int maxResults, final EventCriterion criterion) {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final EventInstanceService eventInstanceService = tenantAccessor.getEventInstanceService();
        final FlowNodeStateManager flowNodeStateManager = tenantAccessor.getFlowNodeStateManager();
        final OrderAndField orderAndField = OrderAndFields.getOrderAndFieldForEvent(criterion);
        final GetEventInstances getEventInstances = new GetEventInstances(eventInstanceService, rootContainerId, startIndex, maxResults,
                orderAndField.getField(), orderAndField.getOrder());
        try {
            getEventInstances.execute();
            final List<SEventInstance> result = getEventInstances.getResult();
            return ModelConvertor.toEventInstances(result, flowNodeStateManager);
        } catch (final SBonitaException e) {
            throw new RetrieveException(e);
        }
    }

    @Override
    public void assignUserTask(final long userTaskId, final long userId) throws UpdateException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final ActivityInstanceService activityInstanceService = tenantAccessor.getActivityInstanceService();
        final SCommentService scommentService = tenantAccessor.getCommentService();
        final IdentityService identityService = tenantAccessor.getIdentityService();
        try {
            final AssignOrUnassignUserTask assignUserTask = new AssignOrUnassignUserTask(userId, userTaskId, activityInstanceService, scommentService,
                    identityService);
            assignUserTask.execute();
        } catch (final SUserNotFoundException sunfe) {
            throw new UpdateException(sunfe);
        } catch (final SActivityInstanceNotFoundException sainfe) {
            throw new UpdateException(sainfe);
        } catch (final SBonitaException sbe) {
            throw new UpdateException(sbe);
        }
    }

    @Override
    public void updateActorsOfUserTask(final long userTaskId) throws UpdateException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        try {
            final SFlowNodeInstance flowNodeInstance = tenantAccessor.getActivityInstanceService().getFlowNodeInstance(userTaskId);
            if (!(flowNodeInstance instanceof SHumanTaskInstance)) {
                throw new UpdateException("The identifier does not refer to a human task");
            }
            final SHumanTaskInstance humanTaskInstance = (SHumanTaskInstance) flowNodeInstance;
            final long processDefinitionId = humanTaskInstance.getLogicalGroup(0);
            final SProcessDefinition processDefinition = tenantAccessor.getProcessDefinitionService().getProcessDefinition(processDefinitionId);
            final SHumanTaskDefinition humanTaskDefinition = (SHumanTaskDefinition) processDefinition.getProcessContainer().getFlowNode(
                    humanTaskInstance.getFlowNodeDefinitionId());
            if (humanTaskDefinition != null) {
                final SUserFilterDefinition sUserFilterDefinition = humanTaskDefinition.getSUserFilterDefinition();
                if (sUserFilterDefinition != null) {
                    final ActivityInstanceService activityInstanceService = tenantAccessor.getActivityInstanceService();
                    // release
                    if (humanTaskInstance.getAssigneeId() > 0) {
                        activityInstanceService.assignHumanTask(userTaskId, 0);
                    }
                    final String actorName = humanTaskDefinition.getActorName();
                    final ClassLoaderService classLoaderService = tenantAccessor.getClassLoaderService();
                    final UserFilterService userFilterService = tenantAccessor.getUserFilterService();
                    final ClassLoader processClassloader = classLoaderService.getLocalClassLoader(ScopeType.PROCESS.name(), processDefinitionId);
                    final SExpressionContext expressionContext = new SExpressionContext(humanTaskInstance.getId(),
                            DataInstanceContainer.ACTIVITY_INSTANCE.name(), humanTaskInstance.getLogicalGroup(0));
                    final FilterResult result = userFilterService.executeFilter(processDefinitionId, sUserFilterDefinition, sUserFilterDefinition.getInputs(),
                            processClassloader, expressionContext, actorName);
                    final List<Long> userIds = result.getResult();
                    if (userIds == null || userIds.isEmpty() || userIds.contains(0l) || userIds.contains(-1l)) {
                        throw new UpdateException("no user id returned by the user filter " + sUserFilterDefinition + " on activity "
                                + humanTaskDefinition.getName());
                    }
                    for (final Long userId : userIds) {
                        final SPendingActivityMapping mapping = BuilderFactory.get(SPendingActivityMappingBuilderFactory.class)
                                .createNewInstanceForUser(humanTaskInstance.getId(), userId).done();
                        activityInstanceService.addPendingActivityMappings(mapping);
                    }
                    if (userIds.size() == 1 && result.shouldAutoAssignTaskIfSingleResult()) {
                        activityInstanceService.assignHumanTask(humanTaskInstance.getId(), userIds.get(0));
                    }
                }
            }
            final TechnicalLoggerService technicalLoggerService = tenantAccessor.getTechnicalLoggerService();
            if (technicalLoggerService.isLoggable(ProcessAPIImpl.class, TechnicalLogSeverity.INFO)) {
                final StringBuilder builder = new StringBuilder("User '");
                builder.append(SessionInfos.getUserNameFromSession()).append("' has re-executed assignation on activity ").append(humanTaskInstance.getId());
                builder.append(" of process instance ").append(humanTaskInstance.getLogicalGroup(1)).append(" of process named '")
                        .append(processDefinition.getName()).append("' in version ").append(processDefinition.getVersion());
                technicalLoggerService.log(ProcessAPIImpl.class, TechnicalLogSeverity.INFO, builder.toString());
            }
        } catch (final SBonitaException sbe) {
            throw new UpdateException(sbe);
        }
    }

    @Override
    public List<DataDefinition> getActivityDataDefinitions(final long processDefinitionId, final String activityName, final int startIndex, final int maxResults)
            throws ActivityDefinitionNotFoundException, ProcessDefinitionNotFoundException {
        List<DataDefinition> subDataDefinitionList = Collections.emptyList();
        List<SDataDefinition> sdataDefinitionList = Collections.emptyList();
        final TenantServiceAccessor tenantAccessor;
        tenantAccessor = getTenantAccessor();

        final ProcessDefinitionService processDefinitionService = tenantAccessor.getProcessDefinitionService();
        final GetProcessDefinition getProcessDefinition = new GetProcessDefinition(processDefinitionId, processDefinitionService);
        try {
            getProcessDefinition.execute();
        } catch (final SProcessDefinitionNotFoundException e) {
            throw new ProcessDefinitionNotFoundException(e);
        } catch (final SBonitaException e) {
            throw new RetrieveException(e);
        }
        final SProcessDefinition sProcessDefinition = getProcessDefinition.getResult();
        if (sProcessDefinition != null) {
            boolean activityFound = false;
            final SFlowElementContainerDefinition processContainer = sProcessDefinition.getProcessContainer();
            final Set<SActivityDefinition> activityDefList = processContainer.getActivities();
            for (final SActivityDefinition sActivityDefinition : activityDefList) {
                if (activityName.equals(sActivityDefinition.getName())) {
                    sdataDefinitionList = sActivityDefinition.getSDataDefinitions();
                    activityFound = true;
                    break;
                }
            }
            if (!activityFound) {
                throw new ActivityDefinitionNotFoundException(activityName);
            }
            final List<DataDefinition> dataDefinitionList = ModelConvertor.toDataDefinitions(sdataDefinitionList);
            if (startIndex >= dataDefinitionList.size()) {
                return Collections.emptyList();
            }
            final int toIndex = Math.min(dataDefinitionList.size(), startIndex + maxResults);
            subDataDefinitionList = new ArrayList<DataDefinition>(dataDefinitionList.subList(startIndex, toIndex));
        }
        return subDataDefinitionList;
    }

    @Override
    public List<DataDefinition> getProcessDataDefinitions(final long processDefinitionId, final int startIndex, final int maxResults)
            throws ProcessDefinitionNotFoundException {
        List<DataDefinition> subDataDefinitionList = Collections.emptyList();
        final TenantServiceAccessor tenantAccessor;
        tenantAccessor = getTenantAccessor();

        final ProcessDefinitionService processDefinitionService = tenantAccessor.getProcessDefinitionService();
        final GetProcessDefinition getProcessDefinition = new GetProcessDefinition(processDefinitionId, processDefinitionService);
        try {
            getProcessDefinition.execute();
        } catch (final SProcessDefinitionNotFoundException e) {
            throw new ProcessDefinitionNotFoundException(e);
        } catch (final SBonitaException e) {
            throw new RetrieveException(e);
        }

        final SProcessDefinition sProcessDefinition = getProcessDefinition.getResult();
        if (sProcessDefinition != null) {
            final SFlowElementContainerDefinition processContainer = sProcessDefinition.getProcessContainer();
            final List<SDataDefinition> sdataDefinitionList = processContainer.getDataDefinitions();
            final List<DataDefinition> dataDefinitionList = ModelConvertor.toDataDefinitions(sdataDefinitionList);
            if (startIndex >= dataDefinitionList.size()) {
                return Collections.emptyList();
            }
            final int toIndex = Math.min(dataDefinitionList.size(), startIndex + maxResults);
            subDataDefinitionList = new ArrayList<DataDefinition>(dataDefinitionList.subList(startIndex, toIndex));
        }
        return subDataDefinitionList;
    }

    @Override
    public HumanTaskInstance getHumanTaskInstance(final long activityInstanceId) throws ActivityInstanceNotFoundException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final ActivityInstanceService activityInstanceService = tenantAccessor.getActivityInstanceService();
        final FlowNodeStateManager flowNodeStateManager = tenantAccessor.getFlowNodeStateManager();
        final GetHumanTaskInstance getHumanTaskInstance = new GetHumanTaskInstance(activityInstanceService, activityInstanceId);
        try {
            getHumanTaskInstance.execute();
        } catch (final SActivityInstanceNotFoundException e) {
            throw new ActivityInstanceNotFoundException(activityInstanceId, e);
        } catch (final SBonitaException e) {
            throw new RetrieveException(e);
        }
        return ModelConvertor.toHumanTaskInstance(getHumanTaskInstance.getResult(), flowNodeStateManager);
    }

    @Override
    public long getNumberOfAssignedHumanTaskInstances(final long userId) {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final ActivityInstanceService activityInstanceService = tenantAccessor.getActivityInstanceService();

        try {
            final TransactionContentWithResult<Long> transactionContent = new GetNumberOfAssignedUserTaskInstances(userId, activityInstanceService);
            transactionContent.execute();
            return transactionContent.getResult();
        } catch (final SBonitaException e) {
            throw new RetrieveException(e);
        }
    }

    @Override
    public Map<Long, Long> getNumberOfOpenTasks(final List<Long> userIds) {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final ActivityInstanceService activityInstanceService = tenantAccessor.getActivityInstanceService();

        try {
            final GetNumberOfOpenTasksForUsers transactionContent = new GetNumberOfOpenTasksForUsers(userIds, activityInstanceService);
            transactionContent.execute();
            return transactionContent.getResult();
        } catch (final SBonitaException e) {
            throw new RetrieveException(e);
        }
    }

    @Override
    public long getNumberOfPendingHumanTaskInstances(final long userId) {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final ActorMappingService actorMappingService = tenantAccessor.getActorMappingService();
        final ActivityInstanceService activityInstanceService = tenantAccessor.getActivityInstanceService();

        final ProcessDefinitionService processDefService = tenantAccessor.getProcessDefinitionService();
        try {
            final Set<Long> actorIds = getActorsForUser(userId, actorMappingService, processDefService);
            if (actorIds.isEmpty()) {
                return 0L;
            }
            return activityInstanceService.getNumberOfPendingTasksForUser(userId, QueryOptions.defaultQueryOptions());
        } catch (final SBonitaException e) {
            throw new RetrieveException(e);
        }
    }

    @Override
    public Map<String, byte[]> getProcessResources(final long processDefinitionId, final String filenamesPattern) throws RetrieveException {
        String processesFolder;
        TenantServiceAccessor tenantAccessor;
        try {
            tenantAccessor = getTenantAccessor();
            processesFolder = BonitaHomeServer.getInstance().getProcessesFolder(tenantAccessor.getTenantId());
        } catch (final BonitaHomeNotSetException e) {
            throw new RetrieveException("Problem accessing basic Bonita Home server resources", e);
        }
        processesFolder = processesFolder.replaceAll("\\\\", "/");
        if (!processesFolder.endsWith("/")) {
            processesFolder = processesFolder + "/";
        }
        processesFolder = processesFolder + processDefinitionId + "/";
        final Collection<File> files = FileUtils.listFiles(new File(processesFolder), new DeepRegexFileFilter(processesFolder + filenamesPattern),
                DirectoryFileFilter.DIRECTORY);
        final Map<String, byte[]> res = new HashMap<String, byte[]>(files.size());
        try {
            for (final File f : files) {
                res.put(f.getAbsolutePath().replaceAll("\\\\", "/").replaceFirst(processesFolder, ""), IOUtil.getAllContentFrom(f));
            }
        } catch (final IOException e) {
            throw new RetrieveException("Problem accessing resources " + filenamesPattern + " for processDefinitionId: " + processDefinitionId, e);
        }
        return res;
    }

    @Override
    public long getLatestProcessDefinitionId(final String processName) throws ProcessDefinitionNotFoundException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final ProcessDefinitionService processDefinitionService = tenantAccessor.getProcessDefinitionService();

        final TransactionContentWithResult<Long> transactionContent = new GetLatestProcessDefinitionId(processDefinitionService, processName);
        try {
            transactionContent.execute();
        } catch (final SBonitaException e) {
            throw new ProcessDefinitionNotFoundException(e);
        }
        return transactionContent.getResult();
    }

    @Override
    public List<DataInstance> getProcessDataInstances(final long processInstanceId, final int startIndex, final int maxResults) {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final DataInstanceService dataInstanceService = tenantAccessor.getDataInstanceService();
        final ClassLoaderService classLoaderService = tenantAccessor.getClassLoaderService();
        final ProcessInstanceService processInstanceService = tenantAccessor.getProcessInstanceService();
        final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            final long processDefinitionId = processInstanceService.getProcessInstance(processInstanceId).getProcessDefinitionId();
            final ClassLoader processClassLoader = classLoaderService.getLocalClassLoader(ScopeType.PROCESS.name(), processDefinitionId);
            Thread.currentThread().setContextClassLoader(processClassLoader);
            final List<SDataInstance> dataInstances = dataInstanceService.getDataInstances(processInstanceId, DataInstanceContainer.PROCESS_INSTANCE.name(),
                    startIndex, maxResults);
            return ModelConvertor.toDataInstances(dataInstances);
        } catch (final SBonitaException e) {
            throw new RetrieveException(e);
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
    }

    @Override
    public DataInstance getProcessDataInstance(final String dataName, final long processInstanceId) throws DataNotFoundException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final DataInstanceService dataInstanceService = tenantAccessor.getDataInstanceService();
        final ClassLoaderService classLoaderService = tenantAccessor.getClassLoaderService();
        final ProcessInstanceService processInstanceService = tenantAccessor.getProcessInstanceService();
        final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            final long processDefinitionId = processInstanceService.getProcessInstance(processInstanceId).getProcessDefinitionId();
            final ClassLoader processClassLoader = classLoaderService.getLocalClassLoader(ScopeType.PROCESS.name(), processDefinitionId);
            Thread.currentThread().setContextClassLoader(processClassLoader);
            final SDataInstance sDataInstance = dataInstanceService.getDataInstance(dataName, processInstanceId,
                    DataInstanceContainer.PROCESS_INSTANCE.toString());
            return ModelConvertor.toDataInstance(sDataInstance);
        } catch (final SBonitaException e) {
            throw new DataNotFoundException(e);
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
    }

    @Override
    public void updateProcessDataInstance(final String dataName, final long processInstanceId, final Serializable dataValue) throws UpdateException {
        updateProcessDataInstances(processInstanceId, singletonMap(dataName, dataValue));
    }

    @Override
    public void updateProcessDataInstances(final long processInstanceId, final Map<String, Serializable> dataNameValues) throws UpdateException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final DataInstanceService dataInstanceService = tenantAccessor.getDataInstanceService();
        final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            final ClassLoader processClassLoader = getProcessInstanceClassloader(tenantAccessor, processInstanceId);
            Thread.currentThread().setContextClassLoader(processClassLoader);
            final List<SDataInstance> sDataInstances = dataInstanceService.getDataInstances(new ArrayList<String>(dataNameValues.keySet()), processInstanceId,
                    DataInstanceContainer.PROCESS_INSTANCE.toString());
            for (SDataInstance sDataInstance : sDataInstances) {
                final EntityUpdateDescriptor entityUpdateDescriptor = new EntityUpdateDescriptor();
                entityUpdateDescriptor.addField("value", dataNameValues.get(sDataInstance.getName()));
                dataInstanceService.updateDataInstance(sDataInstance, entityUpdateDescriptor);
            }
        } catch (final SBonitaException e) {
            throw new UpdateException(e);
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
    }

    /**
     * @param tenantAccessor
     * @param processInstanceId
     * @return
     * @throws SProcessInstanceNotFoundException
     * @throws SProcessInstanceReadException
     * @throws ClassLoaderException
     */
    protected ClassLoader getProcessInstanceClassloader(final TenantServiceAccessor tenantAccessor, final long processInstanceId)
            throws SProcessInstanceNotFoundException, SProcessInstanceReadException,
            ClassLoaderException {
        final ClassLoaderService classLoaderService = tenantAccessor.getClassLoaderService();
        final ProcessInstanceService processInstanceService = tenantAccessor.getProcessInstanceService();
        final long processDefinitionId = processInstanceService.getProcessInstance(processInstanceId).getProcessDefinitionId();
        final ClassLoader processClassLoader = classLoaderService.getLocalClassLoader(ScopeType.PROCESS.name(), processDefinitionId);
        return processClassLoader;
    }

    @Override
    public List<DataInstance> getActivityDataInstances(final long activityInstanceId, final int startIndex, final int maxResults) {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final DataInstanceService dataInstanceService = tenantAccessor.getDataInstanceService();
        final ClassLoaderService classLoaderService = tenantAccessor.getClassLoaderService();
        final ActivityInstanceService activityInstanceService = tenantAccessor.getActivityInstanceService();
        final int processDefinitionIndex = BuilderFactory.get(SAutomaticTaskInstanceBuilderFactory.class).getProcessDefinitionIndex();
        final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            final long parentProcessInstanceId = activityInstanceService.getFlowNodeInstance(activityInstanceId).getLogicalGroup(processDefinitionIndex);
            final ClassLoader processClassLoader = classLoaderService.getLocalClassLoader(ScopeType.PROCESS.name(), parentProcessInstanceId);
            Thread.currentThread().setContextClassLoader(processClassLoader);
            final List<SDataInstance> dataInstances = dataInstanceService.getDataInstances(activityInstanceId, DataInstanceContainer.ACTIVITY_INSTANCE.name(),
                    startIndex, maxResults);
            return ModelConvertor.toDataInstances(dataInstances);
        } catch (final SBonitaException e) {
            throw new RetrieveException(e);
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
    }

    @Override
    public DataInstance getActivityDataInstance(final String dataName, final long activityInstanceId) throws DataNotFoundException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final DataInstanceService dataInstanceService = tenantAccessor.getDataInstanceService();
        final ClassLoaderService classLoaderService = tenantAccessor.getClassLoaderService();
        final ActivityInstanceService activityInstanceService = tenantAccessor.getActivityInstanceService();
        final int processDefinitionIndex = BuilderFactory.get(SAutomaticTaskInstanceBuilderFactory.class).getProcessDefinitionIndex();
        final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            final long parentProcessInstanceId = activityInstanceService.getFlowNodeInstance(activityInstanceId).getLogicalGroup(processDefinitionIndex);
            final ClassLoader processClassLoader = classLoaderService.getLocalClassLoader(ScopeType.PROCESS.name(), parentProcessInstanceId);
            Thread.currentThread().setContextClassLoader(processClassLoader);
            final SDataInstance sDataInstance = dataInstanceService.getDataInstance(dataName, activityInstanceId,
                    DataInstanceContainer.ACTIVITY_INSTANCE.toString());
            return ModelConvertor.toDataInstance(sDataInstance);
        } catch (final SBonitaException e) {
            throw new DataNotFoundException(e);
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
    }

    @Override
    public void updateActivityDataInstance(final String dataName, final long activityInstanceId, final Serializable dataValue) throws UpdateException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final DataInstanceService dataInstanceService = tenantAccessor.getDataInstanceService();
        final ClassLoaderService classLoaderService = tenantAccessor.getClassLoaderService();
        final ActivityInstanceService activityInstanceService = tenantAccessor.getActivityInstanceService();
        final int processDefinitionIndex = BuilderFactory.get(SAutomaticTaskInstanceBuilderFactory.class).getProcessDefinitionIndex();
        final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            final long parentProcessInstanceId = activityInstanceService.getFlowNodeInstance(activityInstanceId).getLogicalGroup(processDefinitionIndex);
            final ClassLoader processClassLoader = classLoaderService.getLocalClassLoader(ScopeType.PROCESS.name(), parentProcessInstanceId);
            Thread.currentThread().setContextClassLoader(processClassLoader);
            final SDataInstance sDataInstance = dataInstanceService.getDataInstance(dataName, activityInstanceId,
                    DataInstanceContainer.ACTIVITY_INSTANCE.toString());
            final EntityUpdateDescriptor entityUpdateDescriptor = new EntityUpdateDescriptor();
            entityUpdateDescriptor.addField("value", dataValue);
            dataInstanceService.updateDataInstance(sDataInstance, entityUpdateDescriptor);
        } catch (final SBonitaException e) {
            throw new UpdateException(e);
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
    }

    @Override
    public void importActorMapping(final long processDefinitionId, final String xmlContent) throws ActorMappingImportException {
        if (xmlContent != null) {
            final TenantServiceAccessor tenantAccessor = getTenantAccessor();
            final ActorMappingService actorMappingService = tenantAccessor.getActorMappingService();
            final IdentityService identityService = tenantAccessor.getIdentityService();
            final Parser parser = tenantAccessor.getActorMappingParser();
            try {
                new ImportActorMapping(actorMappingService, identityService, parser, processDefinitionId, xmlContent).execute();
                tenantAccessor.getDependencyResolver().resolveDependencies(processDefinitionId, tenantAccessor);
            } catch (final SBonitaException sbe) {
                throw new ActorMappingImportException(sbe);
            }
        }
    }

    @Override
    public String exportActorMapping(final long processDefinitionId) throws ActorMappingExportException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final ActorMappingService actorMappingService = tenantAccessor.getActorMappingService();
        final IdentityService identityService = tenantAccessor.getIdentityService();
        final XMLWriter writer = tenantAccessor.getXMLWriter();
        try {
            final ExportActorMapping exportActorMapping = new ExportActorMapping(actorMappingService, identityService, writer, processDefinitionId);
            exportActorMapping.execute();
            return exportActorMapping.getResult();
        } catch (final SBonitaException sbe) {
            throw new ActorMappingExportException(sbe);
        }
    }

    @Override
    public boolean isInvolvedInProcessInstance(final long userId, final long processInstanceId) throws ProcessInstanceNotFoundException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final IdentityService identityService = tenantAccessor.getIdentityService();

        final ActivityInstanceService activityInstanceService = tenantAccessor.getActivityInstanceService();
        final ActorMappingService actorMappingService = tenantAccessor.getActorMappingService();
        try {
            final int totalNumber = activityInstanceService.getNumberOfActivityInstances(processInstanceId);
            final List<SActivityInstance> activityInstances = activityInstanceService.getActivityInstances(processInstanceId, 0, totalNumber, "id",
                    OrderByType.ASC);
            for (final SActivityInstance activityInstance : activityInstances) {
                if (activityInstance instanceof SUserTaskInstance) {
                    final SUserTaskInstance userTaskInstance = (SUserTaskInstance) activityInstance;
                    if (userId == userTaskInstance.getAssigneeId()) {
                        return true;
                    }
                    final long actorId = userTaskInstance.getActorId();
                    final int numOfActorMembers = (int) actorMappingService.getNumberOfActorMembers(actorId);
                    final int numberPerPage = 100;
                    for (int i = 0; i < numOfActorMembers % numberPerPage; i++) {
                        final GetActorMembers getActorMembers = new GetActorMembers(actorMappingService, actorId, i, numberPerPage);
                        getActorMembers.execute();
                        for (final SActorMember actorMember : getActorMembers.getResult()) {
                            if (actorMember.getUserId() == userId) {
                                return true;
                            }
                            // if userId is as id of a user manager, return true
                            final SUser user = identityService.getUser(actorMember.getUserId());
                            if (userId == user.getManagerUserId()) {
                                return true;
                            }
                        }
                    }
                }
            }
            if (activityInstances.isEmpty()) {
                // check if the process exists in case of there is no results
                try {
                    tenantAccessor.getProcessInstanceService().getProcessInstance(processInstanceId);
                } catch (final SProcessInstanceNotFoundException e) {
                    throw new ProcessInstanceNotFoundException(processInstanceId);
                }
            }
            return false;
        } catch (final SBonitaException e) {
            // no rollback, read only method
            throw new BonitaRuntimeException(e);// TODO refactor Exceptions!!!!!!!!!!!!!!!!!!!
        }
    }

    @Override
    public long getProcessInstanceIdFromActivityInstanceId(final long activityInstanceId) throws ProcessInstanceNotFoundException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final ActivityInstanceService activityInstanceService = tenantAccessor.getActivityInstanceService();
        final GetActivityInstance getActivityInstance = new GetActivityInstance(activityInstanceService, activityInstanceId);
        try {
            getActivityInstance.execute();
            final SActivityInstance activity = getActivityInstance.getResult();
            return activity.getRootContainerId();
        } catch (final SBonitaException e) {
            log(tenantAccessor, e);
            throw new ProcessInstanceNotFoundException(e.getMessage());
        }
    }

    @Override
    public long getProcessDefinitionIdFromActivityInstanceId(final long activityInstanceId) throws ProcessDefinitionNotFoundException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final ProcessInstanceService processInstanceService = tenantAccessor.getProcessInstanceService();
        final ActivityInstanceService activityInstanceService = tenantAccessor.getActivityInstanceService();
        try {
            final SActivityInstance activity = activityInstanceService.getActivityInstance(activityInstanceId);
            return processInstanceService.getProcessInstance(activity.getParentProcessInstanceId()).getProcessDefinitionId();
        } catch (final SBonitaException e) {
            throw new ProcessDefinitionNotFoundException(e);
        }
    }

    @Override
    public long getProcessDefinitionIdFromProcessInstanceId(final long processInstanceId) throws ProcessDefinitionNotFoundException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final ProcessInstanceService processInstanceService = tenantAccessor.getProcessInstanceService();
        final ProcessDefinitionService processDefinitionService = tenantAccessor.getProcessDefinitionService();
        final SearchProcessInstanceDescriptor searchProcessInstanceDescriptor = tenantAccessor.getSearchEntitiesDescriptor()
                .getSearchProcessInstanceDescriptor();

        try {
            final GetProcessInstance getProcessInstance = new GetProcessInstance(processInstanceService, processDefinitionService,
                    searchProcessInstanceDescriptor, processInstanceId);
            getProcessInstance.execute();
            final ProcessInstance processInstance = getProcessInstance.getResult();
            return processInstance.getProcessDefinitionId();
        } catch (final SBonitaException e) {
            log(tenantAccessor, e);
            throw new ProcessDefinitionNotFoundException(e.getMessage());
        }
    }

    @Override
    public Date getActivityReachedStateDate(final long activityInstanceId, final String stateName) {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final ActivityInstanceService activityInstanceService = tenantAccessor.getActivityInstanceService();

        final int stateId = ModelConvertor.getServerActivityStateId(stateName);
        final GetArchivedActivityInstance getArchivedActivityInstance = new GetArchivedActivityInstance(activityInstanceId, stateId, activityInstanceService);
        try {
            getArchivedActivityInstance.execute();
            final long reachedDate = getArchivedActivityInstance.getResult().getReachedStateDate();
            return new Date(reachedDate);
        } catch (final SBonitaException e) {
            throw new RetrieveException(e);
        }
    }

    @Override
    public Set<String> getSupportedStates(final FlowNodeType nodeType) {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final FlowNodeStateManager flowNodeStateManager = tenantAccessor.getFlowNodeStateManager();
        return flowNodeStateManager.getSupportedState(nodeType);
    }

    @Override
    public void updateActivityInstanceVariables(final long activityInstanceId, final Map<String, Serializable> variables) throws UpdateException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final DataInstanceService dataInstanceService = tenantAccessor.getDataInstanceService();
        final ClassLoaderService classLoaderService = tenantAccessor.getClassLoaderService();
        final ActivityInstanceService activityInstanceService = tenantAccessor.getActivityInstanceService();
        final int processDefinitionIndex = BuilderFactory.get(SAutomaticTaskInstanceBuilderFactory.class).getProcessDefinitionIndex();
        final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            final long parentProcessInstanceId = activityInstanceService.getFlowNodeInstance(activityInstanceId).getLogicalGroup(processDefinitionIndex);
            final ClassLoader processClassLoader = classLoaderService.getLocalClassLoader(ScopeType.PROCESS.name(), parentProcessInstanceId);
            Thread.currentThread().setContextClassLoader(processClassLoader);
            for (final Entry<String, Serializable> variable : variables.entrySet()) {
                final SDataInstance sDataInstance = dataInstanceService.getDataInstance(variable.getKey(), activityInstanceId,
                        DataInstanceContainer.ACTIVITY_INSTANCE.toString());
                final EntityUpdateDescriptor entityUpdateDescriptor = new EntityUpdateDescriptor();
                entityUpdateDescriptor.addField("value", variable.getValue());
                dataInstanceService.updateDataInstance(sDataInstance, entityUpdateDescriptor);
            }
        } catch (final SBonitaException e) {
            throw new UpdateException(e);
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
    }

    @Override
    public void updateActivityInstanceVariables(final List<Operation> operations, final long activityInstanceId,
            final Map<String, Serializable> expressionContexts) throws UpdateException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final OperationService operationService = tenantAccessor.getOperationService();
        final DataInstanceService dataInstanceService = tenantAccessor.getDataInstanceService();
        final ActivityInstanceService activityInstanceService = tenantAccessor.getActivityInstanceService();
        final int processDefinitionIndex = BuilderFactory.get(SAutomaticTaskInstanceBuilderFactory.class).getProcessDefinitionIndex();
        final List<String> dataNames = new ArrayList<String>(operations.size());
        for (final Operation operation : operations) {
            dataNames.add(operation.getLeftOperand().getName());
        }
        try {
            final List<SDataInstance> dataInstances = dataInstanceService.getDataInstances(dataNames, activityInstanceId,
                    DataInstanceContainer.ACTIVITY_INSTANCE.toString());
            final SActivityInstance activityInstance = activityInstanceService.getActivityInstance(activityInstanceId);
            for (int i = 0; i < dataInstances.size(); i++) {
                // data instances and operation are in the same order
                final SDataInstance dataInstance = dataInstances.get(i);
                final Operation operation = operations.get(i);
                final SOperation sOperation = ModelConvertor.toSOperation(operation);
                final SExpressionContext sExpressionContext = new SExpressionContext(activityInstanceId, DataInstanceContainer.ACTIVITY_INSTANCE.toString(),
                        activityInstance.getLogicalGroup(processDefinitionIndex));
                sExpressionContext.setSerializableInputValues(expressionContexts);
                operationService.execute(sOperation, dataInstance.getContainerId(), dataInstance.getContainerType(), sExpressionContext);
            }
        } catch (final SDataInstanceException e) {
            throw new UpdateException(e);
        } catch (final SBonitaException e) {
            throw new UpdateException(e);
        }
    }

    @Override
    public long getOneAssignedUserTaskInstanceOfProcessInstance(final long processInstanceId, final long userId) throws RetrieveException {
        // FIXME: write specific query that should be more efficient:
        final int assignedUserTaskInstanceNumber = (int) getNumberOfAssignedHumanTaskInstances(userId);
        final List<HumanTaskInstance> userTaskInstances = getAssignedHumanTaskInstances(userId, 0, assignedUserTaskInstanceNumber,
                ActivityInstanceCriterion.DEFAULT);
        for (final HumanTaskInstance userTaskInstance : userTaskInstances) {
            String stateName = userTaskInstance.getState();
            final long userTaskInstanceId = userTaskInstance.getId();
            if (stateName.equals(ActivityStates.READY_STATE) && userTaskInstance.getParentContainerId() == processInstanceId) {
                return userTaskInstanceId;
            }
        }
        return -1;
    }

    @Override
    public long getOneAssignedUserTaskInstanceOfProcessDefinition(final long processDefinitionId, final long userId) {
        // TODO change this method to do that in one request
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final ProcessInstanceService processInstanceService = tenantAccessor.getProcessInstanceService();

        final ProcessDefinitionService processDefinitionService = tenantAccessor.getProcessDefinitionService();
        final SearchProcessInstanceDescriptor searchProcessInstanceDescriptor = tenantAccessor.getSearchEntitiesDescriptor()
                .getSearchProcessInstanceDescriptor();

        final int assignedUserTaskInstanceNumber = (int) getNumberOfAssignedHumanTaskInstances(userId);
        final List<HumanTaskInstance> userTaskInstances = getAssignedHumanTaskInstances(userId, 0, assignedUserTaskInstanceNumber,
                ActivityInstanceCriterion.DEFAULT);
        for (final HumanTaskInstance userTaskInstance : userTaskInstances) {
            String stateName = userTaskInstance.getState();
            ProcessInstance processInstance;
            try {
                final GetProcessInstance getProcessInstance = new GetProcessInstance(processInstanceService, processDefinitionService,
                        searchProcessInstanceDescriptor, userTaskInstance.getRootContainerId());
                getProcessInstance.execute();
                processInstance = getProcessInstance.getResult();
            } catch (final SBonitaException e) {
                throw new RetrieveException(e);
            }
            if (stateName.equals(ActivityStates.READY_STATE) && processInstance.getProcessDefinitionId() == processDefinitionId) {
                return userTaskInstance.getId();
            }
        }
        return -1;
    }

    @Override
    public String getActivityInstanceState(final long activityInstanceId) throws ActivityInstanceNotFoundException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final ActivityInstanceService activityInstanceService = tenantAccessor.getActivityInstanceService();
        final FlowNodeStateManager flowNodeStateManager = tenantAccessor.getFlowNodeStateManager();
        try {
            final GetActivityInstance getActivityInstance = new GetActivityInstance(activityInstanceService, activityInstanceId);
            getActivityInstance.execute();
            final SActivityInstance sActivity = getActivityInstance.getResult();
            final ActivityInstance activityInstance = ModelConvertor.toActivityInstance(sActivity, flowNodeStateManager);
            return activityInstance.getState();
        } catch (final SBonitaException e) {
            log(tenantAccessor, e);
            throw new ActivityInstanceNotFoundException(activityInstanceId);
        }
    }

    @Override
    public boolean canExecuteTask(final long activityInstanceId, final long userId) throws ActivityInstanceNotFoundException, RetrieveException {
        final HumanTaskInstance userTaskInstance = getHumanTaskInstance(activityInstanceId);
        return userTaskInstance.getState().equalsIgnoreCase(ActivityStates.READY_STATE) && userTaskInstance.getAssigneeId() == userId;
    }

    @Override
    public long getProcessDefinitionId(final String name, final String version) throws ProcessDefinitionNotFoundException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final ProcessDefinitionService processDefinitionService = tenantAccessor.getProcessDefinitionService();

        final TransactionContentWithResult<Long> transactionContent = new GetProcessDefinitionIDByNameAndVersion(processDefinitionService, name, version);
        try {
            transactionContent.execute();
        } catch (final SBonitaException e) {
            throw new ProcessDefinitionNotFoundException(e);
        }
        return transactionContent.getResult();
    }

    @Override
    public void releaseUserTask(final long userTaskId) throws ActivityInstanceNotFoundException, UpdateException {
        final TenantServiceAccessor tenantAccessor;
        tenantAccessor = getTenantAccessor();

        final ActivityInstanceService activityInstanceService = tenantAccessor.getActivityInstanceService();
        try {
            final AssignOrUnassignUserTask assignUserTask = new AssignOrUnassignUserTask(0, userTaskId, activityInstanceService, null, null);
            assignUserTask.execute();
        } catch (final SUnreleasableTaskException e) {
            throw new UpdateException(e);
        } catch (final SActivityInstanceNotFoundException e) {
            throw new ActivityInstanceNotFoundException(e);
        } catch (final SBonitaException e) {
            log(tenantAccessor, e);
            throw new UpdateException(e);
        }
    }

    @Override
    public void updateProcessDeploymentInfo(final long processDefinitionId, final ProcessDeploymentInfoUpdater processDeploymentInfoUpdater)
            throws ProcessDefinitionNotFoundException, UpdateException {
        if (processDeploymentInfoUpdater == null || processDeploymentInfoUpdater.getFields().isEmpty()) {
            throw new UpdateException("The update descriptor does not contain field updates");
        }
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final ProcessDefinitionService processDefinitionService = tenantAccessor.getProcessDefinitionService();

        final UpdateProcessDeploymentInfo updateProcessDeploymentInfo = new UpdateProcessDeploymentInfo(processDefinitionService, processDefinitionId,
                processDeploymentInfoUpdater);
        try {
            updateProcessDeploymentInfo.execute();
        } catch (final SProcessDefinitionNotFoundException e) {
            throw new ProcessDefinitionNotFoundException(e);
        } catch (final SBonitaException sbe) {
            throw new UpdateException(sbe);
        }
    }

    @Override
    public List<ProcessDeploymentInfo> getStartableProcessDeploymentInfosForActors(final Set<Long> actorIds, final int startIndex, final int maxResults,
            final ProcessDeploymentInfoCriterion sortingCriterion) {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final ActorMappingService actorMappingService = tenantAccessor.getActorMappingService();

        final ProcessDefinitionService processDefinitionService = tenantAccessor.getProcessDefinitionService();
        final SProcessDefinitionDeployInfoBuilderFactory builder = BuilderFactory.get(SProcessDefinitionDeployInfoBuilderFactory.class);

        try {
            final List<SActor> actors = actorMappingService.getActors(new ArrayList<Long>(actorIds));
            final HashSet<Long> processDefIds = new HashSet<Long>(actors.size());
            for (final SActor sActor : actors) {
                if (sActor.isInitiator()) {
                    processDefIds.add(sActor.getScopeId());
                }
            }

            String field = null;
            OrderByType order = null;
            switch (sortingCriterion) {
                case DEFAULT:
                    break;
                case LABEL_ASC:
                    // field = processDefinitionDeployInfoKyeProvider.get
                    // FIXME add label?
                    break;
                case LABEL_DESC:
                    break;
                case NAME_ASC:
                    field = builder.getNameKey();
                    order = OrderByType.ASC;
                    break;
                case NAME_DESC:
                    field = builder.getNameKey();
                    order = OrderByType.DESC;
                    break;
                case ACTIVATION_STATE_ASC:
                    field = builder.getActivationStateKey();
                    order = OrderByType.ASC;
                    break;
                case ACTIVATION_STATE_DESC:
                    field = builder.getActivationStateKey();
                    order = OrderByType.DESC;
                    break;
                case CONFIGURATION_STATE_ASC:
                    field = builder.getConfigurationStateKey();
                    order = OrderByType.ASC;
                    break;
                case CONFIGURATION_STATE_DESC:
                    field = builder.getConfigurationStateKey();
                    order = OrderByType.DESC;
                    break;
                case VERSION_ASC:
                    field = builder.getVersionKey();
                    order = OrderByType.ASC;
                    break;
                case VERSION_DESC:
                    field = builder.getVersionKey();
                    order = OrderByType.DESC;
                    break;
                default:
                    break;
            }

            final List<SProcessDefinitionDeployInfo> processDefinitionDeployInfos = processDefinitionService.getProcessDeploymentInfos(new ArrayList<Long>(
                    processDefIds), startIndex, maxResults, field, order);
            return ModelConvertor.toProcessDeploymentInfo(processDefinitionDeployInfos);
        } catch (final SBonitaException e) {
            throw new RetrieveException(e);
        }

    }

    @Override
    public boolean isAllowedToStartProcess(final long processDefinitionId, final Set<Long> actorIds) {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final ActorMappingService actorMappingService = tenantAccessor.getActorMappingService();

        final GetActorsByActorIds getActorsByActorIds = new GetActorsByActorIds(actorMappingService, new ArrayList<Long>(actorIds));
        try {
            getActorsByActorIds.execute();
            final List<SActor> actors = getActorsByActorIds.getResult();
            boolean isAllowedToStartProcess = true;
            final Iterator<SActor> iterator = actors.iterator();
            while (isAllowedToStartProcess && iterator.hasNext()) {
                final SActor actor = iterator.next();
                if (actor.getScopeId() != processDefinitionId || !actor.isInitiator()) {
                    isAllowedToStartProcess = false;
                }
            }
            return isAllowedToStartProcess;
        } catch (final SBonitaException e) {
            throw new RetrieveException(e);
        }
    }

    @Override
    public ActorInstance getActorInitiator(final long processDefinitionId) throws ActorNotFoundException, ProcessDefinitionNotFoundException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final ProcessDefinitionService processDefinitionService = tenantAccessor.getProcessDefinitionService();
        final ActorMappingService actorMappingService = tenantAccessor.getActorMappingService();
        ActorInstance actorInstance = null;
        try {
            final SProcessDefinition definition = processDefinitionService.getProcessDefinition(processDefinitionId);
            final SActorDefinition sActorDefinition = definition.getActorInitiator();
            if (sActorDefinition == null) {
                throw new ActorNotFoundException("No actor initiator defined on the process");
            }
            final String name = sActorDefinition.getName();
            final SActor sActor = actorMappingService.getActor(name, processDefinitionId);
            actorInstance = ModelConvertor.toActorInstance(sActor);
        } catch (final SProcessDefinitionNotFoundException e) {
            // no rollback need, we only read
            throw new ProcessDefinitionNotFoundException(e);
        } catch (final SBonitaException e) {
            throw new RetrieveException(e);
        }
        return actorInstance;
    }

    @Override
    public int getNumberOfActivityDataDefinitions(final long processDefinitionId, final String activityName) throws ProcessDefinitionNotFoundException,
            ActivityDefinitionNotFoundException {
        List<SDataDefinition> sdataDefinitionList = Collections.emptyList();
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final ProcessDefinitionService processDefinitionService = tenantAccessor.getProcessDefinitionService();
        final GetProcessDefinition getProcessDefinition = new GetProcessDefinition(processDefinitionId, processDefinitionService);
        try {
            getProcessDefinition.execute();
        } catch (final SProcessDefinitionNotFoundException e) {
            throw new ProcessDefinitionNotFoundException(e);
        } catch (final SBonitaException e) {
            throw new RetrieveException(e);
        }
        final SProcessDefinition sProcessDefinition = getProcessDefinition.getResult();
        if (sProcessDefinition != null) {
            boolean found = false;
            final SFlowElementContainerDefinition processContainer = sProcessDefinition.getProcessContainer();
            final Set<SActivityDefinition> activityDefList = processContainer.getActivities();
            for (final SActivityDefinition sActivityDefinition : activityDefList) {
                if (activityName.equals(sActivityDefinition.getName())) {
                    sdataDefinitionList = sActivityDefinition.getSDataDefinitions();
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new ActivityDefinitionNotFoundException(activityName);
            }
            return sdataDefinitionList.size();
        }
        return 0;
    }

    @Override
    public int getNumberOfProcessDataDefinitions(final long processDefinitionId) throws ProcessDefinitionNotFoundException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final ProcessDefinitionService processDefinitionService = tenantAccessor.getProcessDefinitionService();

        final GetProcessDefinition getProcessDefinition = new GetProcessDefinition(processDefinitionId, processDefinitionService);
        try {
            getProcessDefinition.execute();
        } catch (final SProcessDefinitionNotFoundException e) {
            throw new ProcessDefinitionNotFoundException(e);
        } catch (final SBonitaException e) {
            throw new RetrieveException(e);
        }
        final SProcessDefinition sProcessDefinition = getProcessDefinition.getResult();
        final SFlowElementContainerDefinition processContainer = sProcessDefinition.getProcessContainer();
        return processContainer.getDataDefinitions().size();
    }

    @Override
    public ProcessInstance startProcess(final long processDefinitionId, final Map<String, Serializable> initialVariables)
            throws ProcessDefinitionNotFoundException, ProcessActivationException, ProcessExecutionException {
        final ClassLoaderService classLoaderService = getTenantAccessor().getClassLoaderService();
        final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        final List<Operation> operations = new ArrayList<Operation>();
        try {
            final ClassLoader localClassLoader = classLoaderService.getLocalClassLoader(ScopeType.PROCESS.name(), processDefinitionId);
            Thread.currentThread().setContextClassLoader(localClassLoader);
            if (initialVariables != null) {
                for (final Entry<String, Serializable> initialVariable : initialVariables.entrySet()) {
                    final String name = initialVariable.getKey();
                    final Serializable value = initialVariable.getValue();
                    final Expression expression = new ExpressionBuilder().createExpression(name, name, value.getClass().getName(), ExpressionType.TYPE_INPUT);
                    final Operation operation = new OperationBuilder().createSetDataOperation(name, expression);
                    operations.add(operation);
                }
            }
        } catch (final ClassLoaderException cle) {
            throw new ProcessExecutionException(cle);
        } catch (final InvalidExpressionException iee) {
            throw new ProcessExecutionException(iee);
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
        return startProcess(processDefinitionId, operations, initialVariables);
    }

    @Override
    public ProcessInstance startProcess(final long processDefinitionId, final List<Operation> operations, final Map<String, Serializable> context)
            throws ProcessExecutionException, ProcessDefinitionNotFoundException, ProcessActivationException {
        try {
            return startProcess(0, processDefinitionId, operations, context);
        } catch (final RetrieveException e) {
            throw new ProcessExecutionException(e);
        }
    }

    @Override
    public ProcessInstance startProcess(final long userId, final long processDefinitionId, final List<Operation> operations,
            final Map<String, Serializable> context) throws ProcessDefinitionNotFoundException, ProcessActivationException, ProcessExecutionException {
        ProcessStarter starter = new ProcessStarter(userId, processDefinitionId, operations, context);
        return starter.start();
    }

    @Override
    public long getNumberOfActivityDataInstances(final long activityInstanceId) throws ActivityInstanceNotFoundException {
        try {
            return getNumberOfDataInstancesOfContainer(activityInstanceId, DataInstanceContainer.ACTIVITY_INSTANCE);
        } catch (final SBonitaException e) {
            throw new ActivityInstanceNotFoundException(activityInstanceId);
        }
    }

    private long getNumberOfDataInstancesOfContainer(final long instanceId, final DataInstanceContainer containerType) throws SBonitaException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final DataInstanceService dataInstanceService = tenantAccessor.getDataInstanceService();
        return dataInstanceService.getNumberOfDataInstances(instanceId, containerType);
    }

    @Override
    public long getNumberOfProcessDataInstances(final long processInstanceId) throws ProcessInstanceNotFoundException {
        try {
            return getNumberOfDataInstancesOfContainer(processInstanceId, DataInstanceContainer.PROCESS_INSTANCE);
        } catch (final SBonitaException e) {
            throw new ProcessInstanceNotFoundException(e);
        }
    }

    protected Map<String, Serializable> executeOperations(final ConnectorResult connectorResult, final List<Operation> operations,
            final Map<String, Serializable> operationInputValues, final SExpressionContext expressionContext, final ClassLoader classLoader,
            final TenantServiceAccessor tenantAccessor) throws SBonitaException {
        final ConnectorService connectorService = tenantAccessor.getConnectorService();
        final OperationService operationService = tenantAccessor.getOperationService();
        final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(classLoader);
            final Map<String, Serializable> externalDataValue = new HashMap<String, Serializable>(operations.size());
            for (final Operation operation : operations) {
                // convert the client operation to server operation
                final SOperation sOperation = ModelConvertor.constructSOperation(operation);
                // set input values of expression with connector result + provided input for this operation
                final HashMap<String, Object> inputValues = new HashMap<String, Object>(operationInputValues);
                inputValues.putAll(connectorResult.getResult());
                expressionContext.setInputValues(inputValues);
                // execute
                final Long containerId = expressionContext.getContainerId();
                operationService.execute(sOperation, containerId == null ? -1 : containerId, expressionContext.getContainerType(), expressionContext);
                // return the value of the data if it's an external data
                final LeftOperand leftOperand = operation.getLeftOperand();
                if (leftOperand.isExternal()) {
                    externalDataValue.put(leftOperand.getName(), (Serializable) expressionContext.getInputValues().get(leftOperand.getName()));
                }
            }
            // we finally disconnect the connector
            connectorService.disconnect(connectorResult);
            return externalDataValue;
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
    }

    private Map<String, Serializable> toSerializableMap(final Map<String, Object> map, final String connectorDefinitionId,
            final String connectorDefinitionVersion) throws NotSerializableException {
        final HashMap<String, Serializable> resMap = new HashMap<String, Serializable>(map.size());
        for (final Entry<String, Object> entry : map.entrySet()) {
            try {
                resMap.put(entry.getKey(), (Serializable) entry.getValue());
            } catch (final ClassCastException e) {
                throw new NotSerializableException(connectorDefinitionId, connectorDefinitionVersion, entry.getKey(), entry.getValue());
            }
        }
        return resMap;
    }

    @Override
    public Map<String, Serializable> executeConnectorOnProcessDefinition(final String connectorDefinitionId, final String connectorDefinitionVersion,
            final Map<String, Expression> connectorInputParameters, final Map<String, Map<String, Serializable>> inputValues, final long processDefinitionId)
            throws ConnectorExecutionException {
        return executeConnectorOnProcessDefinitionWithOrWithoutOperations(connectorDefinitionId, connectorDefinitionVersion, connectorInputParameters,
                inputValues, null, null, processDefinitionId);
    }

    @Override
    public Map<String, Serializable> executeConnectorOnProcessDefinition(final String connectorDefinitionId, final String connectorDefinitionVersion,
            final Map<String, Expression> connectorInputParameters, final Map<String, Map<String, Serializable>> inputValues, final List<Operation> operations,
            final Map<String, Serializable> operationInputValues, final long processDefinitionId) throws ConnectorExecutionException {
        return executeConnectorOnProcessDefinitionWithOrWithoutOperations(connectorDefinitionId, connectorDefinitionVersion, connectorInputParameters,
                inputValues, operations, operationInputValues, processDefinitionId);
    }

    /**
     * execute the connector and return connector output if there is no operation or operation output if there is operation
     * 
     * @param operations
     * @param operationInputValues
     */
    private Map<String, Serializable> executeConnectorOnProcessDefinitionWithOrWithoutOperations(final String connectorDefinitionId,
            final String connectorDefinitionVersion, final Map<String, Expression> connectorInputParameters,
            final Map<String, Map<String, Serializable>> inputValues, final List<Operation> operations, final Map<String, Serializable> operationInputValues,
            final long processDefinitionId) throws ConnectorExecutionException {
        checkConnectorParameters(connectorInputParameters, inputValues);
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final ProcessDefinitionService processDefinitionService = tenantAccessor.getProcessDefinitionService();
        final ConnectorService connectorService = tenantAccessor.getConnectorService();
        final ClassLoaderService classLoaderService = tenantAccessor.getClassLoaderService();

        try {
            final ClassLoader classLoader = classLoaderService.getLocalClassLoader(ScopeType.PROCESS.name(), processDefinitionId);

            final Map<String, SExpression> connectorsExps = ModelConvertor.constructExpressions(connectorInputParameters);
            final SExpressionContext expcontext = new SExpressionContext();
            expcontext.setProcessDefinitionId(processDefinitionId);
            final SProcessDefinition processDef = processDefinitionService.getProcessDefinition(processDefinitionId);
            if (processDef != null) {
                expcontext.setProcessDefinition(processDef);
            }
            final ConnectorResult connectorResult = connectorService.executeMutipleEvaluation(processDefinitionId, connectorDefinitionId,
                    connectorDefinitionVersion, connectorsExps, inputValues, classLoader, expcontext);
            if (operations != null) {
                // execute operations
                return executeOperations(connectorResult, operations, operationInputValues, expcontext, classLoader, tenantAccessor);
            } else {
                return getSerializableResultOfConnector(connectorDefinitionVersion, connectorResult, connectorService);
            }
        } catch (final SBonitaException e) {
            throw new ConnectorExecutionException(e);
        } catch (final NotSerializableException e) {
            throw new ConnectorExecutionException(e);
        }
    }

    protected Map<String, Serializable> getSerializableResultOfConnector(final String connectorDefinitionVersion, final ConnectorResult connectorResult,
            final ConnectorService connectorService) throws NotSerializableException, SConnectorException {
        connectorService.disconnect(connectorResult);
        return toSerializableMap(connectorResult.getResult(), connectorDefinitionVersion, connectorDefinitionVersion);
    }

    protected void checkConnectorParameters(final Map<String, Expression> connectorInputParameters, final Map<String, Map<String, Serializable>> inputValues)
            throws ConnectorExecutionException {
        if (connectorInputParameters.size() != inputValues.size()) {
            throw new ConnectorExecutionException("The number of input parameters is not consistent with the number of input values. Input parameters: "
                    + connectorInputParameters.size() + ", number of input values: " + inputValues.size());
        }
    }

    @Override
    public void setActivityStateByName(final long activityInstanceId, final String state) throws UpdateException {
        setActivityStateById(activityInstanceId, ModelConvertor.getServerActivityStateId(state));
    }

    @Override
    public void setActivityStateById(final long activityInstanceId, final int stateId) throws UpdateException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final ActivityInstanceService activityInstanceService = tenantAccessor.getActivityInstanceService();
        final FlowNodeExecutor flowNodeExecutor = tenantAccessor.getFlowNodeExecutor();
        try {
            final GetActivityInstance getActivityInstance = new GetActivityInstance(activityInstanceService, activityInstanceId);
            getActivityInstance.execute();
            final SActivityInstance activityInstance = getActivityInstance.getResult();
            // set state
            flowNodeExecutor.setStateByStateId(activityInstance.getLogicalGroup(0), activityInstance.getId(), stateId);
        } catch (final SBonitaException e) {
            throw new UpdateException(e);
        }
    }

    @Override
    public void setTaskPriority(final long humanTaskInstanceId, final TaskPriority priority) throws UpdateException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final ActivityInstanceService activityInstanceService = tenantAccessor.getActivityInstanceService();

        try {
            final SetTaskPriority transactionContent = new SetTaskPriority(activityInstanceService, humanTaskInstanceId, STaskPriority.valueOf(priority.name()));
            transactionContent.execute();
        } catch (final SBonitaException e) {
            throw new UpdateException(e);
        }
    }

    @Override
    @Deprecated
    public void deleteProcessInstances(final long processDefinitionId) throws DeletionException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        try {
            deleteProcessInstancesFromProcessDefinition(processDefinitionId, tenantAccessor, tenantAccessor.getTenantId());
            new DeleteArchivedProcessInstances(tenantAccessor, processDefinitionId).execute();
        } catch (final SProcessInstanceHierarchicalDeletionException e) {
            throw new ProcessInstanceHierarchicalDeletionException(e.getMessage(), e.getProcessInstanceId());
        } catch (final SBonitaException e) {
            throw new DeletionException(e);
        }
    }

    private void deleteProcessInstanceInTransaction(final TenantServiceAccessor tenantAccessor, final long processInstanceId) throws SBonitaException {
        final UserTransactionService userTransactionService = tenantAccessor.getUserTransactionService();
        final ProcessInstanceService processInstanceService = tenantAccessor.getProcessInstanceService();

        try {
            userTransactionService.executeInTransaction(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    processInstanceService.deleteParentProcessInstanceAndElements(processInstanceId);
                    return null;
                };
            });
        } catch (final SBonitaException e) {
            throw e;
        } catch (Exception e) {
            throw new SBonitaRuntimeException("Error while deleting the parent process instance and elements", e);
        }
    }

    @Override
    @CustomTransactions
    public long deleteProcessInstances(final long processDefinitionId, final int startIndex, final int maxResults) throws DeletionException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final ProcessInstanceService processInstanceService = tenantAccessor.getProcessInstanceService();
        final UserTransactionService userTxService = tenantAccessor.getUserTransactionService();
        try {
            final List<SProcessInstance> sProcessInstances = userTxService.executeInTransaction(new Callable<List<SProcessInstance>>() {
                @Override
                public List<SProcessInstance> call() throws SBonitaSearchException {
                    return searchProcessInstancesFromProcessDefinition(processInstanceService, processDefinitionId, startIndex, maxResults);
                }
            });

            if (sProcessInstances.isEmpty()) {
                return 0;
            }

            final LockService lockService = tenantAccessor.getLockService();
            final String objectType = SFlowElementsContainerType.PROCESS.name();
            List<BonitaLock> locks = null;
            try {
                locks = createLockProcessInstances(lockService, objectType, sProcessInstances, tenantAccessor.getTenantId());
                return userTxService.executeInTransaction(new Callable<Long>() {
                    @Override
                    public Long call() throws Exception {
                        return processInstanceService.deleteParentProcessInstanceAndElements(sProcessInstances);
                    }
                });
            } finally {
                releaseLocks(tenantAccessor, lockService, locks, tenantAccessor.getTenantId());
            }

        } catch (final Exception e) {
            throw new DeletionException(e);
        }
    }

    private List<SProcessInstance> searchProcessInstancesFromProcessDefinition(final ProcessInstanceService processInstanceService,
            final long processDefinitionId, final int startIndex, final int maxResults) throws SBonitaSearchException {
        final SProcessInstanceBuilderFactory keyProvider = BuilderFactory.get(SProcessInstanceBuilderFactory.class);
        final FilterOption filterOption = new FilterOption(SProcessInstance.class, keyProvider.getProcessDefinitionIdKey(), processDefinitionId);
        final OrderByOption order2 = new OrderByOption(SProcessInstance.class, keyProvider.getIdKey(), OrderByType.ASC);
        // Order by caller id ASC because we need to have parent process deleted before their sub processes
        final OrderByOption order = new OrderByOption(SProcessInstance.class, keyProvider.getCallerIdKey(), OrderByType.ASC);
        final ArrayList<OrderByOption> orders = new ArrayList<OrderByOption>();
        orders.add(order);
        orders.add(order2);
        final QueryOptions queryOptions = new QueryOptions(startIndex, maxResults, orders, Collections.singletonList(filterOption), null);
        return processInstanceService.searchProcessInstances(queryOptions);
    }

    @Override
    public long deleteArchivedProcessInstances(final long processDefinitionId, final int startIndex, final int maxResults) throws DeletionException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final ProcessInstanceService processInstanceService = tenantAccessor.getProcessInstanceService();

        try {
            final List<SAProcessInstance> saProcessInstances = searchArchivedProcessInstancesFromProcessDefinition(processInstanceService, processDefinitionId,
                    startIndex, maxResults);
            if (!saProcessInstances.isEmpty()) {
                return processInstanceService.deleteParentArchivedProcessInstancesAndElements(saProcessInstances);
            }
            return 0;
        } catch (final SBonitaException e) {
            throw new DeletionException(e);
        }
    }

    private List<SAProcessInstance> searchArchivedProcessInstancesFromProcessDefinition(final ProcessInstanceService processInstanceService,
            final long processDefinitionId, final int startIndex, final int maxResults) throws SBonitaSearchException {
        final SAProcessInstanceBuilderFactory keyProvider = BuilderFactory.get(SAProcessInstanceBuilderFactory.class);
        final FilterOption filterOption = new FilterOption(SAProcessInstance.class, keyProvider.getProcessDefinitionIdKey(), processDefinitionId);
        final OrderByOption order = new OrderByOption(SAProcessInstance.class, keyProvider.getIdKey(), OrderByType.ASC);
        // Order by caller id ASC because we need to have parent process deleted before their sub processes
        final OrderByOption order2 = new OrderByOption(SAProcessInstance.class, keyProvider.getCallerIdKey(), OrderByType.ASC);
        final ArrayList<OrderByOption> orders = new ArrayList<OrderByOption>();
        orders.add(order);
        orders.add(order2);
        final QueryOptions queryOptions = new QueryOptions(startIndex, maxResults, orders, Collections.singletonList(filterOption), null);
        return processInstanceService.searchArchivedProcessInstances(queryOptions);
    }

    private List<BonitaLock> createLockProcessInstances(final LockService lockService, final String objectType, final List<SProcessInstance> sProcessInstances,
            final long tenantId) throws SLockException {
        final List<BonitaLock> locks = new ArrayList<BonitaLock>();
        for (final SProcessInstance sProcessInstance : sProcessInstances) {
            final BonitaLock bonitaLock = lockService.lock(sProcessInstance.getId(), objectType, tenantId);
            locks.add(bonitaLock);
        }
        return locks;
    }

    @Override
    @CustomTransactions
    public void deleteProcessInstance(final long processInstanceId) throws DeletionException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final LockService lockService = tenantAccessor.getLockService();
        final String objectType = SFlowElementsContainerType.PROCESS.name();
        try {
            final BonitaLock lock = lockService.lock(processInstanceId, objectType, tenantAccessor.getTenantId());
            deleteProcessInstanceInTransaction(tenantAccessor, processInstanceId);
            lockService.unlock(lock, tenantAccessor.getTenantId());
        } catch (final SLockException e) {
            throw new DeletionException("Lock was not released. Object type: " + objectType + ", id: " + processInstanceId);
        } catch (final SProcessInstanceHierarchicalDeletionException e) {
            throw new ProcessInstanceHierarchicalDeletionException(e.getMessage(), e.getProcessInstanceId());
        } catch (final SProcessInstanceNotFoundException e) {
            throw new DeletionException(e);
        } catch (final SBonitaException e) {
            throw new DeletionException(e);
        }
    }

    @Override
    public SearchResult<ProcessInstance> searchOpenProcessInstances(final SearchOptions searchOptions) throws SearchException {
        // To select all process instances completed, without subprocess
        final SearchOptionsBuilder searchOptionsBuilder = new SearchOptionsBuilder(searchOptions);
        searchOptionsBuilder.differentFrom(ProcessInstanceSearchDescriptor.STATE_ID, ProcessInstanceState.COMPLETED.getId());
        searchOptionsBuilder.filter(ProcessInstanceSearchDescriptor.CALLER_ID, -1);
        try {
            return searchProcessInstances(getTenantAccessor(), searchOptionsBuilder.done());
        } catch (final SSearchException e) {
            throw new SearchException(e);
        }
    }

    @Override
    public SearchResult<ProcessInstance> searchProcessInstances(final SearchOptions searchOptions) throws SearchException {
        try {
            return searchProcessInstances(getTenantAccessor(), searchOptions);
        } catch (final SSearchException e) {
            throw new SearchException(e);
        }
    }

    @Override
    public SearchResult<ProcessInstance> searchOpenProcessInstancesSupervisedBy(final long userId, final SearchOptions searchOptions) throws SearchException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final IdentityService identityService = tenantAccessor.getIdentityService();
        final GetSUser getUser = new GetSUser(identityService, userId);
        try {
            getUser.execute();
        } catch (final SBonitaException e) {
            return new SearchResultImpl<ProcessInstance>(0, Collections.<ProcessInstance> emptyList());
        }
        final ProcessInstanceService processInstanceService = tenantAccessor.getProcessInstanceService();
        final SearchEntitiesDescriptor searchEntitiesDescriptor = tenantAccessor.getSearchEntitiesDescriptor();
        final ProcessDefinitionService processDefinitionService = tenantAccessor.getProcessDefinitionService();
        final SearchOpenProcessInstancesSupervisedBy searchOpenProcessInstances = new SearchOpenProcessInstancesSupervisedBy(processInstanceService,
                searchEntitiesDescriptor.getSearchProcessInstanceDescriptor(), userId, searchOptions, processDefinitionService);
        try {
            searchOpenProcessInstances.execute();
            return searchOpenProcessInstances.getResult();
        } catch (final SBonitaException sbe) {
            throw new SearchException(sbe);
        }
    }

    @Override
    public SearchResult<ProcessDeploymentInfo> searchProcessDeploymentInfosStartedBy(final long userId, final SearchOptions searchOptions)
            throws SearchException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final SearchEntitiesDescriptor searchEntitiesDescriptor = tenantAccessor.getSearchEntitiesDescriptor();
        final ProcessDefinitionService processDefinitionService = tenantAccessor.getProcessDefinitionService();
        final SearchProcessDefinitionsDescriptor searchDescriptor = searchEntitiesDescriptor.getSearchProcessDefinitionsDescriptor();
        final SearchProcessDeploymentInfosStartedBy searcher = new SearchProcessDeploymentInfosStartedBy(processDefinitionService, searchDescriptor, userId,
                searchOptions);
        try {
            searcher.execute();
        } catch (final SBonitaException e) {
            throw new SearchException("Can't get ProcessDeploymentInfo startedBy userid " + userId, e);
        }
        return searcher.getResult();
    }

    @Override
    public SearchResult<ProcessDeploymentInfo> searchProcessDeploymentInfos(final SearchOptions searchOptions) throws SearchException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final SearchEntitiesDescriptor searchEntitiesDescriptor = tenantAccessor.getSearchEntitiesDescriptor();
        final ProcessDefinitionService processDefinitionService = tenantAccessor.getProcessDefinitionService();
        final SearchProcessDefinitionsDescriptor searchDescriptor = searchEntitiesDescriptor.getSearchProcessDefinitionsDescriptor();
        final SearchProcessDeploymentInfos transactionSearch = new SearchProcessDeploymentInfos(processDefinitionService, searchDescriptor, searchOptions);
        try {
            transactionSearch.execute();
        } catch (final SBonitaException e) {
            throw new SearchException("Can't get processDefinition's executing searchProcessDefinitions()", e);
        }
        return transactionSearch.getResult();
    }

    @Override
    public SearchResult<ProcessDeploymentInfo> searchProcessDeploymentInfos(final long userId, final SearchOptions searchOptions) throws RetrieveException,
            SearchException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final SearchEntitiesDescriptor searchEntitiesDescriptor = tenantAccessor.getSearchEntitiesDescriptor();
        final ProcessDefinitionService processDefinitionService = tenantAccessor.getProcessDefinitionService();
        final SearchProcessDefinitionsDescriptor searchDescriptor = searchEntitiesDescriptor.getSearchProcessDefinitionsDescriptor();
        final SearchProcessDeploymentInfosUserCanStart transactionSearch = new SearchProcessDeploymentInfosUserCanStart(processDefinitionService,
                searchDescriptor, searchOptions, userId);
        try {
            transactionSearch.execute();
        } catch (final SBonitaException e) {
            throw new SearchException("Error while retrieving process definitions: " + e.getMessage(), e);
        }
        return transactionSearch.getResult();

    }

    @Override
    public SearchResult<ProcessDeploymentInfo> searchProcessDeploymentInfosUsersManagedByCanStart(final long managerUserId, final SearchOptions searchOptions)
            throws SearchException {
        final TenantServiceAccessor serviceAccessor = getTenantAccessor();
        final ProcessDefinitionService processDefinitionService = serviceAccessor.getProcessDefinitionService();
        final SearchEntitiesDescriptor searchEntitiesDescriptor = serviceAccessor.getSearchEntitiesDescriptor();
        final SearchProcessDefinitionsDescriptor searchDescriptor = searchEntitiesDescriptor.getSearchProcessDefinitionsDescriptor();
        final SearchProcessDeploymentInfosUsersManagedByCanStart transactionSearch = new SearchProcessDeploymentInfosUsersManagedByCanStart(
                processDefinitionService, searchDescriptor, searchOptions, managerUserId);
        try {
            transactionSearch.execute();
        } catch (final SBonitaException e) {
            throw new SearchException(e);
        }
        return transactionSearch.getResult();
    }

    @Override
    public SearchResult<ProcessDeploymentInfo> searchProcessDeploymentInfosSupervisedBy(final long userId, final SearchOptions searchOptions)
            throws SearchException {
        final TenantServiceAccessor serviceAccessor = getTenantAccessor();
        final ProcessDefinitionService processDefinitionService = serviceAccessor.getProcessDefinitionService();
        final SearchEntitiesDescriptor searchEntitiesDescriptor = serviceAccessor.getSearchEntitiesDescriptor();
        final SearchProcessDefinitionsDescriptor searchDescriptor = searchEntitiesDescriptor.getSearchProcessDefinitionsDescriptor();
        final SearchProcessDeploymentInfosSupervised searcher = new SearchProcessDeploymentInfosSupervised(processDefinitionService, searchDescriptor,
                searchOptions, userId);
        try {
            searcher.execute();
        } catch (final SBonitaException sbe) {
            throw new SearchException(sbe);
        }
        return searcher.getResult();
    }

    @Override
    public SearchResult<HumanTaskInstance> searchAssignedTasksSupervisedBy(final long supervisorId, final SearchOptions searchOptions) throws SearchException {
        final TenantServiceAccessor serviceAccessor = getTenantAccessor();
        final FlowNodeStateManager flowNodeStateManager = serviceAccessor.getFlowNodeStateManager();
        final ActivityInstanceService activityInstanceService = serviceAccessor.getActivityInstanceService();
        final SearchEntitiesDescriptor searchEntitiesDescriptor = serviceAccessor.getSearchEntitiesDescriptor();
        final SearchAssignedTasksSupervisedBy searchedTasksTransaction = new SearchAssignedTasksSupervisedBy(supervisorId, activityInstanceService,
                flowNodeStateManager, searchEntitiesDescriptor.getSearchHumanTaskInstanceDescriptor(), searchOptions);
        try {
            searchedTasksTransaction.execute();
        } catch (final SBonitaException sbe) {
            throw new SearchException(sbe);
        }
        return searchedTasksTransaction.getResult();

    }

    @Override
    public SearchResult<ArchivedHumanTaskInstance> searchArchivedHumanTasksSupervisedBy(final long supervisorId, final SearchOptions searchOptions)
            throws SearchException {
        final TenantServiceAccessor serviceAccessor = getTenantAccessor();
        final FlowNodeStateManager flowNodeStateManager = serviceAccessor.getFlowNodeStateManager();
        final ActivityInstanceService activityInstanceService = serviceAccessor.getActivityInstanceService();
        final SearchEntitiesDescriptor searchEntitiesDescriptor = serviceAccessor.getSearchEntitiesDescriptor();
        final SearchArchivedTasksSupervisedBy searchedTasksTransaction = new SearchArchivedTasksSupervisedBy(supervisorId, activityInstanceService,
                flowNodeStateManager, searchEntitiesDescriptor.getSearchArchivedHumanTaskInstanceDescriptor(), searchOptions);

        try {
            searchedTasksTransaction.execute();
        } catch (final SBonitaException sbe) {
            throw new SearchException(sbe);
        }
        return searchedTasksTransaction.getResult();
    }

    @Override
    public SearchResult<ProcessSupervisor> searchProcessSupervisors(final SearchOptions searchOptions) throws SearchException {
        final TenantServiceAccessor serviceAccessor = getTenantAccessor();
        final SupervisorMappingService supervisorService = serviceAccessor.getSupervisorService();
        final SearchProcessSupervisorDescriptor searchDescriptor = new SearchProcessSupervisorDescriptor();
        final SearchSupervisors searchSupervisorsTransaction = new SearchSupervisors(supervisorService, searchDescriptor, searchOptions);
        try {
            searchSupervisorsTransaction.execute();
            return searchSupervisorsTransaction.getResult();
        } catch (final SBonitaException e) {
            throw new SearchException(e);
        }
    }

    @Override
    public boolean isUserProcessSupervisor(final long processDefinitionId, final long userId) {
        final TenantServiceAccessor serviceAccessor = getTenantAccessor();
        final SupervisorMappingService supervisorService = serviceAccessor.getSupervisorService();
        try {
            return supervisorService.isProcessSupervisor(processDefinitionId, userId);
        } catch (final SBonitaReadException e) {
            throw new RetrieveException(e);
        }
    }

    @Override
    public void deleteSupervisor(final long supervisorId) throws DeletionException {
        final TenantServiceAccessor serviceAccessor = getTenantAccessor();
        final SupervisorMappingService supervisorService = serviceAccessor.getSupervisorService();
        try {
            supervisorService.deleteSupervisor(supervisorId);
        } catch (final SSupervisorNotFoundException e) {
            throw new DeletionException("supervisor not found with id " + supervisorId);
        } catch (final SSupervisorDeletionException e) {
            throw new DeletionException(e);
        }
    }

    @Override
    public void deleteSupervisor(final Long processDefinitionId, final Long userId, final Long roleId, final Long groupId) throws DeletionException {
        final TenantServiceAccessor serviceAccessor = getTenantAccessor();
        final SupervisorMappingService supervisorService = serviceAccessor.getSupervisorService();

        // Prepare search options
        final SearchOptionsBuilder searchOptionsBuilder = new SearchOptionsBuilder(0, 1);
        searchOptionsBuilder.sort(ProcessSupervisorSearchDescriptor.ID, Order.ASC);
        searchOptionsBuilder.filter(ProcessSupervisorSearchDescriptor.PROCESS_DEFINITION_ID, processDefinitionId == null ? -1 : processDefinitionId);
        searchOptionsBuilder.filter(ProcessSupervisorSearchDescriptor.USER_ID, userId == null ? -1 : userId);
        searchOptionsBuilder.filter(ProcessSupervisorSearchDescriptor.ROLE_ID, roleId == null ? -1 : roleId);
        searchOptionsBuilder.filter(ProcessSupervisorSearchDescriptor.GROUP_ID, groupId == null ? -1 : groupId);
        final SearchProcessSupervisorDescriptor searchDescriptor = new SearchProcessSupervisorDescriptor();
        final SearchSupervisors searchSupervisorsTransaction = new SearchSupervisors(supervisorService, searchDescriptor, searchOptionsBuilder.done());

        try {
            // Search the supervisor corresponding to criteria
            searchSupervisorsTransaction.execute();
            final SearchResult<ProcessSupervisor> result = searchSupervisorsTransaction.getResult();

            if (result.getCount() > 0) {
                // Then, delete it
                final List<ProcessSupervisor> processSupervisors = result.getResult();
                supervisorService.deleteSupervisor(processSupervisors.get(0).getSupervisorId());
            } else {
                throw new SSupervisorNotFoundException("No supervisor was found with userId = " + userId + ", roleId = " + roleId + ", groupId = " + groupId
                        + ", processDefinitionId = " + processDefinitionId);
            }
        } catch (final SBonitaException e) {
            throw new DeletionException(e);
        }
    }

    @Override
    public ProcessSupervisor createProcessSupervisorForUser(final long processDefinitionId, final long userId) throws CreationException, AlreadyExistsException {
        return createSupervisor(processDefinitionId, userId, null, null, MemberType.USER);
    }

    @Override
    public ProcessSupervisor createProcessSupervisorForRole(final long processDefinitionId, final long roleId) throws CreationException, AlreadyExistsException {
        return createSupervisor(processDefinitionId, null, null, roleId, MemberType.ROLE);
    }

    @Override
    public ProcessSupervisor createProcessSupervisorForGroup(final long processDefinitionId, final long groupId) throws CreationException,
            AlreadyExistsException {
        return createSupervisor(processDefinitionId, null, groupId, null, MemberType.GROUP);
    }

    @Override
    public ProcessSupervisor createProcessSupervisorForMembership(final long processDefinitionId, final long groupId, final long roleId)
            throws CreationException, AlreadyExistsException {
        return createSupervisor(processDefinitionId, null, groupId, roleId, MemberType.MEMBERSHIP);
    }

    private ProcessSupervisor createSupervisor(final long processDefinitionId, final Long userId, final Long groupId, final Long roleId,
            final MemberType memberType) throws CreationException, AlreadyExistsException {
        SProcessSupervisor supervisor;
        final TenantServiceAccessor serviceAccessor = getTenantAccessor();
        final SupervisorMappingService supervisorService = serviceAccessor.getSupervisorService();
        try {
            final SProcessSupervisorBuilder supervisorBuilder = BuilderFactory.get(SProcessSupervisorBuilderFactory.class).createNewInstance(
                    processDefinitionId);
            switch (memberType) {
                case USER:
                    supervisorBuilder.setUserId(userId);
                    break;

                case GROUP:
                    supervisorBuilder.setGroupId(groupId);
                    break;

                case ROLE:
                    supervisorBuilder.setRoleId(roleId);
                    break;

                case MEMBERSHIP:
                    supervisorBuilder.setGroupId(groupId);
                    supervisorBuilder.setRoleId(roleId);
                    break;
            }

            supervisor = supervisorBuilder.done();
            supervisor = supervisorService.createSupervisor(supervisor);
            return ModelConvertor.toProcessSupervisor(supervisor);
        } catch (final SSupervisorAlreadyExistsException e) {
            throw new AlreadyExistsException("This supervisor already exists");
        } catch (final SSupervisorCreationException e) {
            throw new CreationException(e);
        }
    }

    @Override
    public SearchResult<ProcessDeploymentInfo> searchUncategorizedProcessDeploymentInfosUserCanStart(final long userId, final SearchOptions searchOptions)
            throws SearchException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final SearchEntitiesDescriptor searchEntitiesDescriptor = tenantAccessor.getSearchEntitiesDescriptor();
        final ProcessDefinitionService processDefinitionService = tenantAccessor.getProcessDefinitionService();
        final SearchProcessDefinitionsDescriptor searchDescriptor = searchEntitiesDescriptor.getSearchProcessDefinitionsDescriptor();
        final SearchUncategorizedProcessDeploymentInfosUserCanStart transactionSearch = new SearchUncategorizedProcessDeploymentInfosUserCanStart(
                processDefinitionService, searchDescriptor, searchOptions, userId);
        try {
            transactionSearch.execute();
        } catch (final SBonitaException e) {
            throw new SearchException("Error while retrieving process definitions", e);
        }
        return transactionSearch.getResult();
    }

    @Override
    public SearchResult<ArchivedHumanTaskInstance> searchArchivedHumanTasksManagedBy(final long managerUserId, final SearchOptions searchOptions)
            throws SearchException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final ActivityInstanceService activityInstanceService = tenantAccessor.getActivityInstanceService();
        final FlowNodeStateManager flowNodeStateManager = tenantAccessor.getFlowNodeStateManager();
        final SearchEntitiesDescriptor searchEntitiesDescriptor = tenantAccessor.getSearchEntitiesDescriptor();
        final SearchArchivedTasksManagedBy searchTransaction = new SearchArchivedTasksManagedBy(managerUserId, searchOptions, activityInstanceService,
                flowNodeStateManager, searchEntitiesDescriptor.getSearchArchivedHumanTaskInstanceDescriptor());
        try {
            searchTransaction.execute();
        } catch (final SBonitaException e) {
            throw new SearchException(e);
        }
        return searchTransaction.getResult();
    }

    @Override
    public SearchResult<ProcessInstance> searchOpenProcessInstancesInvolvingUser(final long userId, final SearchOptions searchOptions) throws SearchException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final ProcessInstanceService processInstanceService = tenantAccessor.getProcessInstanceService();
        final SearchEntitiesDescriptor searchEntitiesDescriptor = tenantAccessor.getSearchEntitiesDescriptor();
        final ProcessDefinitionService processDefinitionService = tenantAccessor.getProcessDefinitionService();
        final SearchOpenProcessInstancesInvolvingUser searchOpenProcessInstances = new SearchOpenProcessInstancesInvolvingUser(processInstanceService,
                searchEntitiesDescriptor.getSearchProcessInstanceDescriptor(), userId, searchOptions, processDefinitionService);
        try {
            searchOpenProcessInstances.execute();
            return searchOpenProcessInstances.getResult();
        } catch (final SBonitaException sbe) {
            throw new SearchException(sbe);
        }
    }

    @Override
    public SearchResult<ProcessInstance> searchOpenProcessInstancesInvolvingUsersManagedBy(final long managerUserId, final SearchOptions searchOptions)
            throws SearchException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final ProcessInstanceService processInstanceService = tenantAccessor.getProcessInstanceService();
        final SearchEntitiesDescriptor searchEntitiesDescriptor = tenantAccessor.getSearchEntitiesDescriptor();
        final ProcessDefinitionService processDefinitionService = tenantAccessor.getProcessDefinitionService();
        final SearchOpenProcessInstancesInvolvingUsersManagedBy searchOpenProcessInstances = new SearchOpenProcessInstancesInvolvingUsersManagedBy(
                processInstanceService, searchEntitiesDescriptor.getSearchProcessInstanceDescriptor(), managerUserId, searchOptions, processDefinitionService);
        try {
            searchOpenProcessInstances.execute();
            return searchOpenProcessInstances.getResult();
        } catch (final SBonitaException sbe) {
            throw new SearchException(sbe);
        }
    }

    @Override
    public SearchResult<ArchivedHumanTaskInstance> searchArchivedHumanTasks(final SearchOptions searchOptions) throws SearchException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final ActivityInstanceService activityInstanceService = tenantAccessor.getActivityInstanceService();
        final FlowNodeStateManager flowNodeStateManager = tenantAccessor.getFlowNodeStateManager();
        final SearchEntitiesDescriptor searchEntitiesDescriptor = tenantAccessor.getSearchEntitiesDescriptor();

        final SearchArchivedTasks searchArchivedTasks = new SearchArchivedTasks(activityInstanceService, flowNodeStateManager,
                searchEntitiesDescriptor.getSearchArchivedHumanTaskInstanceDescriptor(), searchOptions);
        try {
            searchArchivedTasks.execute();
        } catch (final SBonitaException sbe) {
            throw new SearchException(sbe);
        }
        return searchArchivedTasks.getResult();
    }

    @Override
    public SearchResult<HumanTaskInstance> searchAssignedTasksManagedBy(final long managerUserId, final SearchOptions searchOptions) throws SearchException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final SearchEntitiesDescriptor searchEntitiesDescriptor = tenantAccessor.getSearchEntitiesDescriptor();
        final ActivityInstanceService activityInstanceService = tenantAccessor.getActivityInstanceService();
        final FlowNodeStateManager flowNodeStateManager = tenantAccessor.getFlowNodeStateManager();
        final SearchAssignedTaskManagedBy searchAssignedTaskManagedBy = new SearchAssignedTaskManagedBy(activityInstanceService, flowNodeStateManager,
                searchEntitiesDescriptor.getSearchHumanTaskInstanceDescriptor(), managerUserId, searchOptions);
        try {
            searchAssignedTaskManagedBy.execute();
        } catch (final SBonitaException e) {
            throw new SearchException(e);
        }
        return searchAssignedTaskManagedBy.getResult();
    }

    @Override
    public SearchResult<ArchivedProcessInstance> searchArchivedProcessInstancesSupervisedBy(final long userId, final SearchOptions searchOptions)
            throws SearchException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final ProcessInstanceService processInstanceService = tenantAccessor.getProcessInstanceService();
        final SearchEntitiesDescriptor searchEntitiesDescriptor = tenantAccessor.getSearchEntitiesDescriptor();
        final SearchArchivedProcessInstancesSupervisedBy searchArchivedProcessInstances = new SearchArchivedProcessInstancesSupervisedBy(userId,
                processInstanceService, searchEntitiesDescriptor.getSearchArchivedProcessInstanceDescriptor(), searchOptions);
        try {
            searchArchivedProcessInstances.execute();
            return searchArchivedProcessInstances.getResult();
        } catch (final SBonitaException sbe) {
            throw new SearchException(sbe);
        }
    }

    @Override
    public SearchResult<ArchivedProcessInstance> searchArchivedProcessInstancesInvolvingUser(final long userId, final SearchOptions searchOptions)
            throws SearchException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final ProcessInstanceService processInstanceService = tenantAccessor.getProcessInstanceService();
        final SearchEntitiesDescriptor searchEntitiesDescriptor = tenantAccessor.getSearchEntitiesDescriptor();
        final SearchArchivedProcessInstancesInvolvingUser searchArchivedProcessInstances = new SearchArchivedProcessInstancesInvolvingUser(userId,
                processInstanceService, searchEntitiesDescriptor.getSearchArchivedProcessInstanceDescriptor(), searchOptions);
        try {
            searchArchivedProcessInstances.execute();
            return searchArchivedProcessInstances.getResult();
        } catch (final SBonitaException sbe) {
            throw new SearchException(sbe);
        }
    }

    @Override
    public SearchResult<HumanTaskInstance> searchPendingTasksForUser(final long userId, final SearchOptions searchOptions) throws SearchException {
        return searchTasksForUser(userId, searchOptions, false);
    }

    /**
     * @param orAssignedToUser
     *            do we also want to retrieve tasks directly assigned to this user ?
     * @throws SearchException
     */
    private SearchResult<HumanTaskInstance> searchTasksForUser(final long userId, final SearchOptions searchOptions, final boolean orAssignedToUser)
            throws SearchException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final ActivityInstanceService activityInstanceService = tenantAccessor.getActivityInstanceService();
        final FlowNodeStateManager flowNodeStateManager = tenantAccessor.getFlowNodeStateManager();
        final SearchEntitiesDescriptor searchEntitiesDescriptor = tenantAccessor.getSearchEntitiesDescriptor();
        final SearchPendingTasksForUser searchPendingTasksForUser = new SearchPendingTasksForUser(activityInstanceService, flowNodeStateManager,
                searchEntitiesDescriptor.getSearchHumanTaskInstanceDescriptor(), userId, searchOptions, orAssignedToUser);
        try {
            searchPendingTasksForUser.execute();
            return searchPendingTasksForUser.getResult();
        } catch (final SBonitaException sbe) {
            throw new SearchException(sbe);
        }
    }

    @Override
    public SearchResult<HumanTaskInstance> searchMyAvailableHumanTasks(final long userId, final SearchOptions searchOptions) throws SearchException {
        return searchTasksForUser(userId, searchOptions, true);
    }

    @Override
    public SearchResult<HumanTaskInstance> searchPendingTasksSupervisedBy(final long userId, final SearchOptions searchOptions) {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final ActivityInstanceService activityInstanceService = tenantAccessor.getActivityInstanceService();
        final FlowNodeStateManager flowNodeStateManager = tenantAccessor.getFlowNodeStateManager();
        final SearchEntitiesDescriptor searchEntitiesDescriptor = tenantAccessor.getSearchEntitiesDescriptor();
        final SearchPendingTasksSupervisedBy searchPendingTasksSupervisedBy = new SearchPendingTasksSupervisedBy(activityInstanceService, flowNodeStateManager,
                searchEntitiesDescriptor.getSearchHumanTaskInstanceDescriptor(), userId, searchOptions);
        try {
            searchPendingTasksSupervisedBy.execute();
            return searchPendingTasksSupervisedBy.getResult();
        } catch (final SBonitaException sbe) {
            throw new BonitaRuntimeException(sbe);
        }
    }

    @Override
    public SearchResult<Comment> searchComments(final SearchOptions searchOptions) throws SearchException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final SCommentService commentService = tenantAccessor.getCommentService();
        final SearchEntitiesDescriptor searchEntitiesDescriptor = tenantAccessor.getSearchEntitiesDescriptor();
        final SearchComments searchComments = new SearchComments(searchEntitiesDescriptor.getSearchCommentDescriptor(), searchOptions, commentService);
        try {
            searchComments.execute();
            return searchComments.getResult();
        } catch (final SBonitaException sbe) {
            throw new SearchException(sbe);
        }
    }

    @Override
    public Comment addProcessComment(final long processInstanceId, final String comment) throws CreationException {
        try {
            // TODO: refactor this method when deprecated addComment() method is removed from API:
            return addComment(processInstanceId, comment);
        } catch (final RetrieveException e) {
            throw new CreationException("Cannot add a comment on a finished or inexistant process instance", e.getCause());
        }
    }

    @Override
    @Deprecated
    public Comment addComment(final long processInstanceId, final String comment) {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        try {
            tenantAccessor.getProcessInstanceService().getProcessInstance(processInstanceId);
        } catch (final SProcessInstanceReadException e) {
            throw new RetrieveException("Cannot add a comment on a finished or inexistant process instance", e); // FIXME: should be another exception
        } catch (final SProcessInstanceNotFoundException e) {
            throw new RetrieveException("Cannot add a comment on a finished or inexistant process instance", e); // FIXME: should be another exception
        }
        final SCommentService commentService = tenantAccessor.getCommentService();
        final AddComment addComment = new AddComment(commentService, processInstanceId, comment);
        try {
            addComment.execute();
            final SComment sComment = addComment.getResult();
            return ModelConvertor.toComment(sComment);
        } catch (final SBonitaException e) {
            throw new RetrieveException(e);
        }
    }

    @Override
    public List<Comment> getComments(final long processInstanceId) {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final SCommentService commentService = tenantAccessor.getCommentService();
        try {
            final List<SComment> sComments = commentService.getComments(processInstanceId);
            return ModelConvertor.toComments(sComments);
        } catch (final SBonitaReadException sbe) {
            throw new RetrieveException(sbe);
        }
    }

    @Override
    public Document attachDocument(final long processInstanceId, final String documentName, final String fileName, final String mimeType, final String url)
            throws DocumentAttachmentException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final ProcessDocumentService processDocumentService = tenantAccessor.getProcessDocumentService();
        final long author = getUserId();
        try {
            final SProcessDocument document = attachDocument(processInstanceId, documentName, fileName, mimeType, url, processDocumentService, author);
            return ModelConvertor.toDocument(document);
        } catch (final SBonitaException sbe) {
            throw new DocumentAttachmentException(sbe);
        }
    }

    protected SProcessDocument attachDocument(final long processInstanceId, final String documentName, final String fileName, final String mimeType,
            final String url, final ProcessDocumentService processDocumentService, final long authorId) throws SBonitaException {
        final SProcessDocument attachment = buildExternalProcessDocumentReference(processInstanceId, documentName, fileName, mimeType, authorId, url);
        return processDocumentService.attachDocumentToProcessInstance(attachment);
    }

    private SProcessDocument buildExternalProcessDocumentReference(final long processInstanceId, final String documentName, final String fileName,
            final String mimeType, final long authorId, final String url) {
        final SProcessDocumentBuilder documentBuilder = initDocumentBuilder(processInstanceId, documentName, fileName, mimeType, authorId);
        documentBuilder.setURL(url);
        documentBuilder.setHasContent(false);
        return documentBuilder.done();
    }

    private SProcessDocument buildProcessDocument(final long processInstanceId, final String documentName, final String fileName, final String mimetype,
            final long authorId) {
        final SProcessDocumentBuilder documentBuilder = initDocumentBuilder(processInstanceId, documentName, fileName, mimetype, authorId);
        documentBuilder.setHasContent(true);
        return documentBuilder.done();
    }

    private SProcessDocumentBuilder initDocumentBuilder(final long processInstanceId, final String documentName, final String fileName, final String mimetype,
            final long authorId) {
        final SProcessDocumentBuilder documentBuilder = BuilderFactory.get(SProcessDocumentBuilderFactory.class).createNewInstance();
        documentBuilder.setName(documentName);
        documentBuilder.setFileName(fileName);
        documentBuilder.setContentMimeType(mimetype);
        documentBuilder.setProcessInstanceId(processInstanceId);
        documentBuilder.setAuthor(authorId);
        documentBuilder.setCreationDate(System.currentTimeMillis());
        return documentBuilder;
    }

    @Override
    public Document attachDocument(final long processInstanceId, final String documentName, final String fileName, final String mimeType,
            final byte[] documentContent) throws DocumentAttachmentException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final ProcessDocumentService processDocumentService = tenantAccessor.getProcessDocumentService();
        final long authorId = getUserId();
        try {
            final SProcessDocument document = attachDocument(processInstanceId, documentName, fileName, mimeType, documentContent, processDocumentService,
                    authorId);
            return ModelConvertor.toDocument(document);
        } catch (final SBonitaException sbe) {
            throw new DocumentAttachmentException(sbe);
        }
    }

    protected SProcessDocument attachDocument(final long processInstanceId, final String documentName, final String fileName, final String mimeType,
            final byte[] documentContent, final ProcessDocumentService processDocumentService, final long authorId) throws SBonitaException {
        final SProcessDocument attachment = buildProcessDocument(processInstanceId, documentName, fileName, mimeType, authorId);
        return processDocumentService.attachDocumentToProcessInstance(attachment, documentContent);
    }

    @Override
    public Document attachNewDocumentVersion(final long processInstanceId, final String documentName, final String fileName, final String mimeType,
            final String url) throws DocumentAttachmentException {
        getTenantAccessor();
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final ProcessDocumentService processDocumentService = tenantAccessor.getProcessDocumentService();
        final long authorId = getUserId();
        try {
            final SProcessDocument attachment = buildExternalProcessDocumentReference(processInstanceId, documentName, fileName, mimeType, authorId, url);
            final AttachDocumentVersion attachDocumentTransationContent = new AttachDocumentVersion(processDocumentService, attachment);
            attachDocumentTransationContent.execute();
            return ModelConvertor.toDocument(attachDocumentTransationContent.getResult());
        } catch (final SBonitaException sbe) {
            throw new DocumentAttachmentException(sbe);
        }
    }

    @Override
    public Document attachNewDocumentVersion(final long processInstanceId, final String documentName, final String contentFileName,
            final String contentMimeType, final byte[] documentContent) throws DocumentAttachmentException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final ProcessDocumentService processDocumentService = tenantAccessor.getProcessDocumentService();
        final long authorId = getUserId();
        try {
            final SProcessDocument attachment = buildProcessDocument(processInstanceId, documentName, contentFileName, contentMimeType, authorId);
            final AttachDocumentVersionAndStoreContent attachDocumentTransationContent = new AttachDocumentVersionAndStoreContent(processDocumentService,
                    attachment, documentContent);
            attachDocumentTransationContent.execute();
            return ModelConvertor.toDocument(attachDocumentTransationContent.getResult());
        } catch (final SBonitaException sbe) {
            throw new DocumentAttachmentException(sbe);
        }
    }

    @Override
    public Document getDocument(final long documentId) throws DocumentNotFoundException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final ProcessDocumentService processDocumentService = tenantAccessor.getProcessDocumentService();
        try {
            final GetDocument attachDocumentTransationContent = new GetDocument(processDocumentService, documentId);
            attachDocumentTransationContent.execute();
            return ModelConvertor.toDocument(attachDocumentTransationContent.getResult());
        } catch (final SBonitaException sbe) {
            throw new DocumentNotFoundException(sbe);
        }
    }

    @Override
    public List<Document> getLastVersionOfDocuments(final long processInstanceId, final int pageIndex, final int numberPerPage,
            final DocumentCriterion pagingCriterion) throws DocumentException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final ProcessDocumentService processDocumentService = tenantAccessor.getProcessDocumentService();

        final OrderAndField orderAndField = OrderAndFields.getOrderAndFieldForDocument(pagingCriterion);
        try {
            final GetDocumentsOfProcessInstance transationContent = new GetDocumentsOfProcessInstance(processDocumentService, processInstanceId, pageIndex,
                    numberPerPage, orderAndField.getField(), orderAndField.getOrder());
            transationContent.execute();
            final List<SProcessDocument> attachments = transationContent.getResult();
            if (attachments != null && !attachments.isEmpty()) {
                final List<Document> result = new ArrayList<Document>(attachments.size());
                for (final SProcessDocument attachment : attachments) {
                    result.add(ModelConvertor.toDocument(attachment));
                }
                return result;
            } else {
                return Collections.emptyList();
            }
        } catch (final SBonitaException sbe) {
            throw new DocumentException(sbe);
        }
    }

    @Override
    public byte[] getDocumentContent(final String documentStorageId) throws DocumentNotFoundException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final ProcessDocumentService processDocumentService = tenantAccessor.getProcessDocumentService();
        try {
            final GetDocumentContent transationContent = new GetDocumentContent(processDocumentService, documentStorageId);
            transationContent.execute();
            return transationContent.getResult();
        } catch (final SBonitaException sbe) {
            throw new DocumentNotFoundException(sbe);
        }
    }

    @Override
    public Document getLastDocument(final long processInstaneId, final String documentName) throws DocumentNotFoundException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final ProcessDocumentService processDocumentService = tenantAccessor.getProcessDocumentService();
        try {
            final GetDocumentByName transationContent = new GetDocumentByName(processDocumentService, processInstaneId, documentName);
            transationContent.execute();
            final SProcessDocument attachment = transationContent.getResult();
            return ModelConvertor.toDocument(attachment);
        } catch (final SBonitaException sbe) {
            throw new DocumentNotFoundException(sbe);
        }
    }

    @Override
    public long getNumberOfDocuments(final long processInstanceId) throws DocumentException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final ProcessDocumentService processDocumentService = tenantAccessor.getProcessDocumentService();
        try {
            final GetNumberOfDocumentsOfProcessInstance transationContent = new GetNumberOfDocumentsOfProcessInstance(processDocumentService, processInstanceId);
            transationContent.execute();
            return transationContent.getResult();

        } catch (final SBonitaException sbe) {
            throw new DocumentException(sbe);
        }
    }

    @Override
    public Document getDocumentAtProcessInstantiation(final long processInstanceId, final String documentName) throws DocumentNotFoundException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final ProcessDocumentService processDocumentService = tenantAccessor.getProcessDocumentService();

        final ProcessInstanceService processInstanceService = tenantAccessor.getProcessInstanceService();
        final SearchEntitiesDescriptor searchEntitiesDescriptor = tenantAccessor.getSearchEntitiesDescriptor();

        try {
            final GetDocumentByNameAtProcessInstantiation transationContent = new GetDocumentByNameAtProcessInstantiation(processDocumentService,
                    processInstanceService, searchEntitiesDescriptor, processInstanceId, documentName);
            transationContent.execute();
            final SProcessDocument attachment = transationContent.getResult();
            return ModelConvertor.toDocument(attachment);
        } catch (final SBonitaException sbe) {
            throw new DocumentNotFoundException(sbe);
        }
    }

    @Override
    public Document getDocumentAtActivityInstanceCompletion(final long activityInstanceId, final String documentName) throws DocumentNotFoundException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final ProcessDocumentService processDocumentService = tenantAccessor.getProcessDocumentService();
        final ActivityInstanceService activityInstanceService = tenantAccessor.getActivityInstanceService();
        try {
            final GetDocumentByNameAtActivityCompletion transationContent = new GetDocumentByNameAtActivityCompletion(processDocumentService,
                    activityInstanceId, documentName, activityInstanceService);
            transationContent.execute();
            final SProcessDocument attachment = transationContent.getResult();
            return ModelConvertor.toDocument(attachment);
        } catch (final SBonitaException sbe) {
            throw new DocumentNotFoundException(sbe);
        }
    }

    @Override
    public SearchResult<HumanTaskInstance> searchPendingTasksManagedBy(final long managerUserId, final SearchOptions searchOptions) throws SearchException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final SearchEntitiesDescriptor searchEntitiesDescriptor = tenantAccessor.getSearchEntitiesDescriptor();
        final ActivityInstanceService activityInstanceService = tenantAccessor.getActivityInstanceService();
        final FlowNodeStateManager flowNodeStateManager = tenantAccessor.getFlowNodeStateManager();
        final SearchPendingTasksManagedBy searchPendingTasksManagedBy = new SearchPendingTasksManagedBy(activityInstanceService, flowNodeStateManager,
                searchEntitiesDescriptor.getSearchHumanTaskInstanceDescriptor(), managerUserId, searchOptions);
        try {
            searchPendingTasksManagedBy.execute();
        } catch (final SBonitaException e) {
            throw new SearchException(e);
        }
        return searchPendingTasksManagedBy.getResult();
    }

    @Override
    public Map<Long, Long> getNumberOfOverdueOpenTasks(final List<Long> userIds) throws RetrieveException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final ActivityInstanceService activityInstanceService = tenantAccessor.getActivityInstanceService();

        try {
            final GetNumberOfOverdueOpenTasksForUsers transactionContent = new GetNumberOfOverdueOpenTasksForUsers(userIds, activityInstanceService);
            transactionContent.execute();
            return transactionContent.getResult();
        } catch (final SBonitaException e) {
            log(tenantAccessor, e);
            throw new RetrieveException(e.getMessage());
        }
    }

    @Override
    public SearchResult<ProcessDeploymentInfo> searchUncategorizedProcessDeploymentInfos(final SearchOptions searchOptions) throws SearchException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final SearchEntitiesDescriptor searchEntitiesDescriptor = tenantAccessor.getSearchEntitiesDescriptor();
        final ProcessDefinitionService processDefinitionService = tenantAccessor.getProcessDefinitionService();
        final SearchProcessDefinitionsDescriptor searchDescriptor = searchEntitiesDescriptor.getSearchProcessDefinitionsDescriptor();
        final SearchUncategorizedProcessDeploymentInfos transactionSearch = new SearchUncategorizedProcessDeploymentInfos(processDefinitionService,
                searchDescriptor, searchOptions);
        try {
            transactionSearch.execute();
        } catch (final SBonitaException e) {
            throw new SearchException("Problem encountered while searching for Uncategorized Process Definitions", e);
        }
        return transactionSearch.getResult();
    }

    @Override
    public SearchResult<Comment> searchCommentsManagedBy(final long managerUserId, final SearchOptions searchOptions) throws SearchException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final SCommentService commentService = tenantAccessor.getCommentService();
        final SearchEntitiesDescriptor searchEntitiesDescriptor = tenantAccessor.getSearchEntitiesDescriptor();
        final SearchCommentsManagedBy searchComments = new SearchCommentsManagedBy(searchEntitiesDescriptor.getSearchCommentDescriptor(), searchOptions,
                commentService, managerUserId);
        try {
            searchComments.execute();
            return searchComments.getResult();
        } catch (final SBonitaException sbe) {
            throw new SearchException(sbe);
        }
    }

    @Override
    public SearchResult<Comment> searchCommentsInvolvingUser(final long userId, final SearchOptions searchOptions) throws SearchException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final SCommentService commentService = tenantAccessor.getCommentService();
        final SearchEntitiesDescriptor searchEntitiesDescriptor = tenantAccessor.getSearchEntitiesDescriptor();
        final SearchCommentsInvolvingUser searchComments = new SearchCommentsInvolvingUser(searchEntitiesDescriptor.getSearchCommentDescriptor(),
                searchOptions, commentService, userId);
        try {
            searchComments.execute();
            return searchComments.getResult();
        } catch (final SBonitaException sbe) {
            throw new SearchException(sbe);
        }
    }

    @Override
    public List<Long> getChildrenInstanceIdsOfProcessInstance(final long processInstanceId, final int startIndex, final int maxResults,
            final ProcessInstanceCriterion criterion) {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final ProcessInstanceService processInstanceService = tenantAccessor.getProcessInstanceService();

        long totalNumber;
        try {
            totalNumber = processInstanceService.getNumberOfChildInstancesOfProcessInstance(processInstanceId);
            if (totalNumber == 0) {
                return Collections.emptyList();
            }
            final OrderAndField orderAndField = OrderAndFields.getOrderAndFieldForProcessInstance(criterion);
            return processInstanceService.getChildInstanceIdsOfProcessInstance(processInstanceId, startIndex, maxResults, orderAndField.getField(),
                    orderAndField.getOrder());
        } catch (final SProcessInstanceReadException e) {
            throw new RetrieveException(e);
        }
    }

    @Override
    public SearchResult<ProcessDeploymentInfo> searchUncategorizedProcessDeploymentInfosSupervisedBy(final long userId, final SearchOptions searchOptions)
            throws SearchException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final SearchEntitiesDescriptor searchEntitiesDescriptor = tenantAccessor.getSearchEntitiesDescriptor();
        final ProcessDefinitionService processDefinitionService = tenantAccessor.getProcessDefinitionService();
        final SearchProcessDefinitionsDescriptor searchDescriptor = searchEntitiesDescriptor.getSearchProcessDefinitionsDescriptor();
        final SearchUncategorizedProcessDeploymentInfosSupervisedBy transactionSearch = new SearchUncategorizedProcessDeploymentInfosSupervisedBy(
                processDefinitionService, searchDescriptor, searchOptions, userId);
        try {
            transactionSearch.execute();
        } catch (final SBonitaException e) {
            throw new SearchException("Problem encountered while searching for Uncategorized Process Definitions for a supervisor", e);
        }
        return transactionSearch.getResult();
    }

    @Override
    public Map<Long, ProcessDeploymentInfo> getProcessDeploymentInfosFromIds(final List<Long> processDefinitionIds) {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final ProcessDefinitionService processDefinitionService = tenantAccessor.getProcessDefinitionService();

        try {
            final GetProcessDeploymentInfosFromIds processDefinitions = new GetProcessDeploymentInfosFromIds(processDefinitionIds, processDefinitionService);
            processDefinitions.execute();
            final List<ProcessDeploymentInfo> processDeploymentInfos = ModelConvertor.toProcessDeploymentInfo(processDefinitions.getResult());
            final Map<Long, ProcessDeploymentInfo> mProcessDefinitions = new HashMap<Long, ProcessDeploymentInfo>();
            for (final ProcessDeploymentInfo p : processDeploymentInfos) {
                mProcessDefinitions.put(p.getProcessId(), p);
            }
            return mProcessDefinitions;
        } catch (final SBonitaException e) {
            throw new RetrieveException(e);
        }
    }

    @Override
    public List<ConnectorImplementationDescriptor> getConnectorImplementations(final long processDefinitionId, final int startIndex, final int maxsResults,
            final ConnectorCriterion sortingCriterion) {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final ConnectorService connectorService = tenantAccessor.getConnectorService();
        final OrderAndField orderAndField = OrderAndFields.getOrderAndFieldForConnectorImplementation(sortingCriterion);
        final GetConnectorImplementations transactionContent = new GetConnectorImplementations(connectorService, processDefinitionId,
                tenantAccessor.getTenantId(), startIndex, maxsResults, orderAndField.getField(), orderAndField.getOrder());
        try {
            transactionContent.execute();
            final List<SConnectorImplementationDescriptor> sConnectorImplementationDescriptors = transactionContent.getResult();
            return ModelConvertor.toConnectorImplementationDescriptors(sConnectorImplementationDescriptors);
        } catch (final SBonitaException e) {
            throw new RetrieveException(e);
        }
    }

    @Override
    public long getNumberOfConnectorImplementations(final long processDefinitionId) {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final ConnectorService connectorService = tenantAccessor.getConnectorService();
        final GetNumberOfConnectorImplementations transactionContent = new GetNumberOfConnectorImplementations(connectorService, processDefinitionId,
                tenantAccessor.getTenantId());
        try {
            transactionContent.execute();
            return transactionContent.getResult();
        } catch (final SBonitaException e) {
            throw new RetrieveException(e);
        }
    }

    @Override
    public SearchResult<ActivityInstance> searchActivities(final SearchOptions searchOptions) throws SearchException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final SearchEntitiesDescriptor searchEntitiesDescriptor = tenantAccessor.getSearchEntitiesDescriptor();
        final ActivityInstanceService activityInstanceService = tenantAccessor.getActivityInstanceService();
        final FlowNodeStateManager flowNodeStateManager = tenantAccessor.getFlowNodeStateManager();
        final SearchActivityInstances searchActivityInstancesTransaction = new SearchActivityInstances(activityInstanceService, flowNodeStateManager,
                searchEntitiesDescriptor.getSearchActivityInstanceDescriptor(), searchOptions);
        try {
            searchActivityInstancesTransaction.execute();
        } catch (final SBonitaException e) {
            throw new SearchException(e);
        }
        return searchActivityInstancesTransaction.getResult();
    }

    @Override
    public SearchResult<ArchivedFlowNodeInstance> searchArchivedFlowNodeInstances(final SearchOptions searchOptions) throws SearchException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final SearchEntitiesDescriptor searchEntitiesDescriptor = tenantAccessor.getSearchEntitiesDescriptor();
        final ActivityInstanceService activityInstanceService = tenantAccessor.getActivityInstanceService();
        final FlowNodeStateManager flowNodeStateManager = tenantAccessor.getFlowNodeStateManager();
        final SearchArchivedFlowNodeInstances searchTransaction = new SearchArchivedFlowNodeInstances(activityInstanceService, flowNodeStateManager,
                searchEntitiesDescriptor.getSearchArchivedFlowNodeInstanceDescriptor(), searchOptions);
        try {
            searchTransaction.execute();
        } catch (final SBonitaException e) {
            throw new SearchException(e);
        }
        return searchTransaction.getResult();
    }

    @Override
    public SearchResult<FlowNodeInstance> searchFlowNodeInstances(final SearchOptions searchOptions) throws SearchException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final SearchEntitiesDescriptor searchEntitiesDescriptor = tenantAccessor.getSearchEntitiesDescriptor();
        final ActivityInstanceService activityInstanceService = tenantAccessor.getActivityInstanceService();
        final FlowNodeStateManager flowNodeStateManager = tenantAccessor.getFlowNodeStateManager();
        final SearchFlowNodeInstances searchFlowNodeInstancesTransaction = new SearchFlowNodeInstances(activityInstanceService, flowNodeStateManager,
                searchEntitiesDescriptor.getSearchFlowNodeInstanceDescriptor(), searchOptions);
        try {
            searchFlowNodeInstancesTransaction.execute();
        } catch (final SBonitaException e) {
            throw new SearchException(e);
        }
        return searchFlowNodeInstancesTransaction.getResult();
    }

    @Override
    public SearchResult<ArchivedActivityInstance> searchArchivedActivities(final SearchOptions searchOptions) throws SearchException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final SearchEntitiesDescriptor searchEntitiesDescriptor = tenantAccessor.getSearchEntitiesDescriptor();
        final ActivityInstanceService activityInstanceService = tenantAccessor.getActivityInstanceService();
        final FlowNodeStateManager flowNodeStateManager = tenantAccessor.getFlowNodeStateManager();
        final SearchArchivedActivityInstances searchActivityInstancesTransaction = new SearchArchivedActivityInstances(activityInstanceService,
                flowNodeStateManager, searchEntitiesDescriptor.getSearchArchivedActivityInstanceDescriptor(), searchOptions);
        try {
            searchActivityInstancesTransaction.execute();
        } catch (final SBonitaException e) {
            throw new SearchException(e);
        }
        return searchActivityInstancesTransaction.getResult();
    }

    @Override
    public ConnectorImplementationDescriptor getConnectorImplementation(final long processDefinitionId, final String connectorId, final String connectorVersion)
            throws ConnectorNotFoundException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final ConnectorService connectorService = tenantAccessor.getConnectorService();
        final GetConnectorImplementation transactionContent = new GetConnectorImplementation(connectorService, processDefinitionId, connectorId,
                connectorVersion, tenantAccessor.getTenantId());
        try {
            transactionContent.execute();
            final SConnectorImplementationDescriptor sConnectorImplementationDescriptor = transactionContent.getResult();
            return ModelConvertor.toConnectorImplementationDescriptor(sConnectorImplementationDescriptor);
        } catch (final SBonitaException e) {
            throw new ConnectorNotFoundException(e);
        }
    }

    @Override
    public void cancelProcessInstance(final long processInstanceId) throws ProcessInstanceNotFoundException, UpdateException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final LockService lockService = tenantAccessor.getLockService();
        final TransactionalProcessInstanceInterruptor processInstanceInterruptor = buildProcessInstanceInterruptor(tenantAccessor);
        // lock process execution
        final String objectType = SFlowElementsContainerType.PROCESS.name();
        BonitaLock lock = null;
        try {
            lock = lockService.lock(processInstanceId, objectType, tenantAccessor.getTenantId());
            processInstanceInterruptor.interruptProcessInstance(processInstanceId, SStateCategory.CANCELLING, getUserId());
        } catch (final SProcessInstanceNotFoundException spinfe) {
            throw new ProcessInstanceNotFoundException(processInstanceId);
        } catch (final SBonitaException e) {
            throw new UpdateException(e);
        } finally {
            // unlock process execution
            try {
                lockService.unlock(lock, tenantAccessor.getTenantId());
            } catch (final SLockException e) {
                // ignore it
            }
        }
    }

    protected long getUserId() {
        return SessionInfos.getUserIdFromSession();
    }

    protected TransactionalProcessInstanceInterruptor buildProcessInstanceInterruptor(final TenantServiceAccessor tenantAccessor) {
        final ProcessInstanceService processInstanceService = tenantAccessor.getProcessInstanceService();
        final ActivityInstanceService activityInstanceService = tenantAccessor.getActivityInstanceService();
        final ProcessExecutor processExecutor = tenantAccessor.getProcessExecutor();
        return new TransactionalProcessInstanceInterruptor(processInstanceService, activityInstanceService, processExecutor,
                tenantAccessor.getTechnicalLoggerService());
    }

    @Override
    public void setProcessInstanceState(final ProcessInstance processInstance, final String state) throws UpdateException {
        // NOW, is only available for COMPLETED, ABORTED, CANCELLED, STARTED
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final ProcessInstanceService processInstanceService = tenantAccessor.getProcessInstanceService();
        try {
            final ProcessInstanceState processInstanceState = ModelConvertor.getProcessInstanceState(state);
            final SetProcessInstanceState transactionContent = new SetProcessInstanceState(processInstanceService, processInstance.getId(),
                    processInstanceState);
            transactionContent.execute();
        } catch (final IllegalArgumentException e) {
            throw new UpdateException(e.getMessage());
        } catch (final SBonitaException e) {
            throw new UpdateException(e.getMessage());
        }
    }

    @Override
    public Map<Long, ProcessDeploymentInfo> getProcessDeploymentInfosFromProcessInstanceIds(final List<Long> processInstantsIds) {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final ProcessDefinitionService processDefinitionService = tenantAccessor.getProcessDefinitionService();

        final GetProcessDefinitionDeployInfoFromProcessInstanceIds processDefinitions = new GetProcessDefinitionDeployInfoFromProcessInstanceIds(
                processInstantsIds, processDefinitionService);
        try {
            processDefinitions.execute();
        } catch (final SBonitaException e) {
            throw new RetrieveException(e);
        }
        final List<Map<String, String>> sProcessDeploymentInfos = processDefinitions.getResult();
        return getProcessDeploymentInfosFromMap(sProcessDeploymentInfos);

    }

    private Map<Long, ProcessDeploymentInfo> getProcessDeploymentInfosFromMap(final List<Map<String, String>> sProcessDeploymentInfos) {
        final Map<Long, ProcessDeploymentInfo> mProcessDeploymentInfos = new HashMap<Long, ProcessDeploymentInfo>();
        long processInstanceId = 0;
        long id = 0;
        long processDefinitionId = 0;
        String name = "";
        String version = "";
        String description = "";
        long deploymentDate = 0;
        long deployedBy = 0;
        ActivationState activationState = null;
        ConfigurationState configurationState = null;
        String displayName = "";
        long lastUpdateDate = 0;
        String iconPath = "";
        String displayDescription = "";
        for (final Map<String, String> m : sProcessDeploymentInfos) {
            for (final Entry<String, String> entry : m.entrySet()) {
                final String key = entry.getKey();
                final Object value = entry.getValue();
                if ("processInstanceId".equals(key)) {
                    processInstanceId = Long.parseLong(value.toString());
                } else if ("id".equals(key)) {
                    id = Long.parseLong(value.toString());
                } else if ("processId".equals(key)) {
                    processDefinitionId = Long.parseLong(value.toString());
                } else if ("name".equals(key)) {
                    name = m.get(key);
                } else if ("version".equals(key)) {
                    version = m.get(key);
                } else if ("description".equals(key)) {
                    description = String.valueOf(m.get(key));
                } else if ("deploymentDate".equals(key)) {
                    deploymentDate = Long.parseLong(value.toString());
                } else if ("deployedBy".equals(key)) {
                    deployedBy = Long.parseLong(value.toString());
                } else if ("activationState".equals(key)) {
                    activationState = ActivationState.valueOf(m.get(key));
                } else if ("configurationState".equals(key)) {
                    configurationState = ConfigurationState.valueOf(m.get(key));
                } else if ("displayName".equals(key)) {
                    displayName = m.get(key);
                } else if ("lastUpdateDate".equals(key)) {
                    lastUpdateDate = Long.parseLong(value.toString());
                } else if ("iconPath".equals(key)) {
                    iconPath = m.get(key);
                } else if ("displayDescription".equals(key)) {
                    displayDescription = String.valueOf(m.get(key));
                }
            }
            final ProcessDeploymentInfoImpl pDeplInfoImpl = new ProcessDeploymentInfoImpl(id, processDefinitionId, name, version, description, new Date(
                    deploymentDate), deployedBy, activationState, configurationState, displayName, new Date(lastUpdateDate), iconPath, displayDescription);
            mProcessDeploymentInfos.put(processInstanceId, pDeplInfoImpl);
        }
        return mProcessDeploymentInfos;
    }

    @Override
    public SearchResult<Document> searchDocuments(final SearchOptions searchOptions) throws SearchException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final SearchEntitiesDescriptor searchEntitiesDescriptor = tenantAccessor.getSearchEntitiesDescriptor();
        final ProcessDocumentService processDocumentService = tenantAccessor.getProcessDocumentService();

        final SearchDocuments searchDocuments = new SearchDocuments(processDocumentService, searchEntitiesDescriptor.getSearchDocumentDescriptor(),
                searchOptions);
        try {
            searchDocuments.execute();
        } catch (final SBonitaException e) {
            throw new SearchException(e);
        }
        return searchDocuments.getResult();
    }

    @Override
    public SearchResult<Document> searchDocumentsSupervisedBy(final long userId, final SearchOptions searchOptions) throws SearchException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final SearchEntitiesDescriptor searchEntitiesDescriptor = tenantAccessor.getSearchEntitiesDescriptor();
        final ProcessDocumentService processDocumentService = tenantAccessor.getProcessDocumentService();

        final SearchDocumentsSupervisedBy searchDocuments = new SearchDocumentsSupervisedBy(processDocumentService,
                searchEntitiesDescriptor.getSearchDocumentDescriptor(), searchOptions, userId);
        try {
            searchDocuments.execute();
        } catch (final SBonitaException e) {
            throw new SearchException(e);
        }
        return searchDocuments.getResult();
    }

    @Override
    public SearchResult<ArchivedDocument> searchArchivedDocuments(final SearchOptions searchOptions) throws SearchException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final SearchEntitiesDescriptor searchEntitiesDescriptor = tenantAccessor.getSearchEntitiesDescriptor();
        final ProcessDocumentService processDocumentService = tenantAccessor.getProcessDocumentService();
        final SearchArchivedDocuments searchDocuments = new SearchArchivedDocuments(processDocumentService,
                searchEntitiesDescriptor.getSearchArchivedDocumentDescriptor(), searchOptions);
        try {
            searchDocuments.execute();
        } catch (final SBonitaException e) {
            throw new SearchException(e);
        }
        return searchDocuments.getResult();
    }

    @Override
    public SearchResult<ArchivedDocument> searchArchivedDocumentsSupervisedBy(final long userId, final SearchOptions searchOptions) throws SearchException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final SearchEntitiesDescriptor searchEntitiesDescriptor = tenantAccessor.getSearchEntitiesDescriptor();
        final ProcessDocumentService processDocumentService = tenantAccessor.getProcessDocumentService();
        final SearchArchivedDocumentsSupervisedBy searchDocuments = new SearchArchivedDocumentsSupervisedBy(userId, processDocumentService,
                searchEntitiesDescriptor.getSearchArchivedDocumentDescriptor(), searchOptions);
        try {
            searchDocuments.execute();
        } catch (final SBonitaException e) {
            throw new SearchException(e);
        }
        return searchDocuments.getResult();
    }

    @Override
    public void retryTask(final long activityInstanceId) throws ActivityExecutionException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final ActivityInstanceService activityInstanceService = tenantAccessor.getActivityInstanceService();
        final FlowNodeStateManager flowNodeStateManager = tenantAccessor.getFlowNodeStateManager();
        final FlowNodeExecutor flowNodeExecutor = tenantAccessor.getFlowNodeExecutor();
        final ProcessExecutor processExecutor = tenantAccessor.getProcessExecutor();
        final SAutomaticTaskInstanceBuilderFactory keyProvider = BuilderFactory.get(SAutomaticTaskInstanceBuilderFactory.class);

        try {
            final SFlowNodeInstance activity = activityInstanceService.getFlowNodeInstance(activityInstanceId);
            final long processDefinitionId = activity.getLogicalGroup(keyProvider.getProcessDefinitionIndex());
            final FlowNodeState flowNodeState = flowNodeStateManager.getState(activity.getStateId());
            final int stateId = activity.getPreviousStateId();
            final FlowNodeState state = flowNodeStateManager.getState(stateId);

            if (!ActivityStates.FAILED_STATE.equals(flowNodeState.getName())) {
                throw new ActivityExecutionException("Unable to retry a task that is not failed - task name=" + activity.getName() + " id="
                        + activityInstanceId + " that was in state " + flowNodeState);
            }

            flowNodeExecutor.setStateByStateId(processDefinitionId, activity.getId(), stateId);
            // execute the flow node only if it is not the final state
            if (!state.isTerminal()) {
                final long userIdFromSession = getUserId();
                // no need to handle failed state, all is in the same tx, if the node fail we just have an exception on client side + rollback
                processExecutor.executeFlowNode(activityInstanceId, null, null, activity.getParentProcessInstanceId(), userIdFromSession, userIdFromSession);
            }
        } catch (final SBonitaException e) {
            throw new ActivityExecutionException(e);
        }
    }

    @Override
    public ArchivedDocument getArchivedVersionOfProcessDocument(final long sourceObjectId) throws ArchivedDocumentNotFoundException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final ProcessDocumentService processDocumentService = tenantAccessor.getProcessDocumentService();
        final GetArchivedDocument getArchivedDocument = new GetArchivedDocument(processDocumentService, sourceObjectId);
        try {
            getArchivedDocument.execute();
        } catch (final SBonitaException e) {
            throw new ArchivedDocumentNotFoundException(e);
        }
        return ModelConvertor.toArchivedDocument(getArchivedDocument.getResult());
    }

    @Override
    public ArchivedDocument getArchivedProcessDocument(final long archivedProcessDocumentId) throws ArchivedDocumentNotFoundException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final ProcessDocumentService processDocumentService = tenantAccessor.getProcessDocumentService();
        try {
            final SAProcessDocument archivedDocument = processDocumentService.getArchivedDocument(archivedProcessDocumentId);
            return ModelConvertor.toArchivedDocument(archivedDocument);
        } catch (final SDocumentNotFoundException e) {
            throw new ArchivedDocumentNotFoundException(e);
        }
    }

    @Override
    public SearchResult<ArchivedComment> searchArchivedComments(final SearchOptions searchOptions) throws SearchException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final SearchEntitiesDescriptor searchEntitiesDescriptor = tenantAccessor.getSearchEntitiesDescriptor();
        final SCommentService sCommentService = tenantAccessor.getCommentService();
        final SearchArchivedComments searchArchivedComments = new SearchArchivedComments(sCommentService,
                searchEntitiesDescriptor.getSearchArchivedCommentsDescriptor(), searchOptions);
        try {
            searchArchivedComments.execute();
        } catch (final SBonitaException e) {
            throw new SearchException(e);
        }
        return searchArchivedComments.getResult();
    }

    @Override
    public ArchivedComment getArchivedComment(final long archivedCommentId) throws RetrieveException, NotFoundException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final SCommentService sCommentService = tenantAccessor.getCommentService();
        try {
            final SAComment archivedComment = sCommentService.getArchivedComment(archivedCommentId);
            return ModelConvertor.toArchivedComment(archivedComment);
        } catch (final SCommentNotFoundException e) {
            throw new NotFoundException(e);
        } catch (final SBonitaException e) {
            throw new RetrieveException(e);
        }
    }

    @Override
    public Map<Long, ActorInstance> getActorsFromActorIds(final List<Long> actorIds) {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final Map<Long, ActorInstance> res = new HashMap<Long, ActorInstance>();
        final ActorMappingService actormappingService = tenantAccessor.getActorMappingService();
        final GetActorsByActorIds getActorsByActorIds = new GetActorsByActorIds(actormappingService, actorIds);
        try {
            getActorsByActorIds.execute();
        } catch (final SBonitaException e1) {
            throw new RetrieveException(e1);
        }
        final List<SActor> actors = getActorsByActorIds.getResult();
        for (final SActor actor : actors) {
            res.put(actor.getId(), ModelConvertor.toActorInstance(actor));
        }
        return res;
    }

    @Override
    public Map<Long, ProcessDeploymentInfo> getProcessDeploymentInfosFromArchivedProcessInstanceIds(final List<Long> archivedProcessInstantsIds) {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final ProcessDefinitionService processDefinitionService = tenantAccessor.getProcessDefinitionService();

        final GetProcessDefinitionDeployInfoFromArchivedProcessInstanceIds getProcessDeploymentInfoFromArchivedProcessInstanceIds = new GetProcessDefinitionDeployInfoFromArchivedProcessInstanceIds(
                archivedProcessInstantsIds, processDefinitionService);
        try {
            getProcessDeploymentInfoFromArchivedProcessInstanceIds.execute();
        } catch (final SBonitaException e) {
            throw new RetrieveException(e);
        }
        final Map<Long, SProcessDefinitionDeployInfo> sProcessDeploymentInfos = getProcessDeploymentInfoFromArchivedProcessInstanceIds.getResult();
        if (sProcessDeploymentInfos != null && !sProcessDeploymentInfos.isEmpty()) {
            final Map<Long, ProcessDeploymentInfo> processDeploymentInfos = new HashMap<Long, ProcessDeploymentInfo>();
            final Set<Entry<Long, SProcessDefinitionDeployInfo>> entries = sProcessDeploymentInfos.entrySet();
            for (final Entry<Long, SProcessDefinitionDeployInfo> entry : entries) {
                processDeploymentInfos.put(entry.getKey(), ModelConvertor.toProcessDeploymentInfo(entry.getValue()));
            }
            return processDeploymentInfos;
        }
        return Collections.emptyMap();
    }

    @Override
    public SearchResult<HumanTaskInstance> searchPendingHiddenTasks(final long userId, final SearchOptions searchOptions) throws SearchException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final SearchEntitiesDescriptor searchEntitiesDescriptor = tenantAccessor.getSearchEntitiesDescriptor();
        final ActivityInstanceService activityInstanceService = tenantAccessor.getActivityInstanceService();
        final FlowNodeStateManager flowNodeStateManager = tenantAccessor.getFlowNodeStateManager();
        final SearchPendingHiddenTasks searchHiddenTasksTx = new SearchPendingHiddenTasks(activityInstanceService, flowNodeStateManager,
                searchEntitiesDescriptor.getSearchHumanTaskInstanceDescriptor(), userId, searchOptions);
        try {
            searchHiddenTasksTx.execute();
        } catch (final SBonitaException e) {
            throw new SearchException(e);
        }
        return searchHiddenTasksTx.getResult();
    }

    @Override
    public void hideTasks(final long userId, final Long... activityInstanceId) throws UpdateException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final TransactionContent hideTasksTx = new HideTasks(tenantAccessor.getActivityInstanceService(), userId, activityInstanceId);
        try {
            hideTasksTx.execute();
        } catch (final SBonitaException e) {
            throw new UpdateException("Error while trying to hide tasks: " + Arrays.toString(activityInstanceId) + " from user with ID " + userId, e);
        }
    }

    @Override
    public void unhideTasks(final long userId, final Long... activityInstanceId) throws UpdateException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final TransactionContent unhideTasksTx = new UnhideTasks(tenantAccessor.getActivityInstanceService(), userId, activityInstanceId);
        try {
            unhideTasksTx.execute();
        } catch (final SBonitaException e) {
            throw new UpdateException("Error while trying to un-hide tasks: " + Arrays.toString(activityInstanceId) + " from user with ID " + userId, e);
        }
    }

    @Override
    public Serializable evaluateExpressionOnProcessDefinition(final Expression expression, final Map<String, Serializable> context,
            final long processDefinitionId) throws ExpressionEvaluationException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final ExpressionResolverService expressionResolverService = tenantAccessor.getExpressionResolverService();

        final ProcessDefinitionService processDefinitionService = tenantAccessor.getProcessDefinitionService();
        final SExpression sExpression = ServerModelConvertor.convertExpression(expression);
        final SExpressionContext expcontext = new SExpressionContext();
        expcontext.setProcessDefinitionId(processDefinitionId);
        SProcessDefinition processDef;
        try {
            processDef = processDefinitionService.getProcessDefinition(processDefinitionId);
            if (processDef != null) {
                expcontext.setProcessDefinition(processDef);
            }
            final HashMap<String, Object> hashMap = new HashMap<String, Object>(context);
            expcontext.setInputValues(hashMap);
            return (Serializable) expressionResolverService.evaluate(sExpression, expcontext);
        } catch (final SBonitaException e) {
            throw new ExpressionEvaluationException(e);
        }
    }

    @Override
    public void updateDueDateOfTask(final long userTaskId, final Date dueDate) throws UpdateException {
        if (dueDate == null) {
            throw new UpdateException("Unable to update a due date to null");
        }
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final ActivityInstanceService activityInstanceService = tenantAccessor.getActivityInstanceService();
        try {
            final SetExpectedEndDate updateProcessInstance = new SetExpectedEndDate(activityInstanceService, userTaskId, dueDate);
            updateProcessInstance.execute();
        } catch (final SFlowNodeNotFoundException e) {
            throw new UpdateException(e);
        } catch (final SBonitaException e) {
            throw new UpdateException(e);
        }
    }

    @Override
    public boolean isTaskHidden(final long userTaskId, final long userId) {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final IsTaskHidden hideTasksTx = new IsTaskHidden(tenantAccessor.getActivityInstanceService(), userId, userTaskId);
        try {
            hideTasksTx.execute();
            return hideTasksTx.getResult();
        } catch (final SBonitaException e) {
            throw new RetrieveException(e);
        }
    }

    @Override
    public long countComments(final SearchOptions searchOptions) throws SearchException {
        final SearchOptionsBuilder searchOptionsBuilder = new SearchOptionsBuilder(0, 0).setFilters(searchOptions.getFilters()).searchTerm(
                searchOptions.getSearchTerm());
        final SearchResult<Comment> searchResult = searchComments(searchOptionsBuilder.done());
        return searchResult.getCount();
    }

    @Override
    public long countAttachments(final SearchOptions searchOptions) throws SearchException {
        final SearchOptionsBuilder searchOptionsBuilder = new SearchOptionsBuilder(0, 0).setFilters(searchOptions.getFilters()).searchTerm(
                searchOptions.getSearchTerm());
        final SearchResult<Document> searchResult = searchDocuments(searchOptionsBuilder.done());
        return searchResult.getCount();
    }

    @Override
    public void sendSignal(final String signalName) throws SendEventException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final EventsHandler eventsHandler = tenantAccessor.getEventsHandler();
        final SThrowSignalEventTriggerDefinition signalEventTriggerDefinition = BuilderFactory.get(SThrowSignalEventTriggerDefinitionBuilderFactory.class)
                .createNewInstance(signalName).done();
        try {
            eventsHandler.handleThrowEvent(signalEventTriggerDefinition);
        } catch (final SBonitaException e) {
            throw new SendEventException(e);
        }
    }

    @Override
    public void sendMessage(final String messageName, final Expression targetProcess, final Expression targetFlowNode,
            final Map<Expression, Expression> messageContent) throws SendEventException {
        sendMessage(messageName, targetProcess, targetFlowNode, messageContent, null);
    }

    @Override
    public void sendMessage(final String messageName, final Expression targetProcess, final Expression targetFlowNode,
            final Map<Expression, Expression> messageContent, final Map<Expression, Expression> correlations) throws SendEventException {
        if (correlations != null && correlations.size() > 5) {
            throw new SendEventException("Too many correlations: a message can not have more than 5 correlations.");
        }
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final EventsHandler eventsHandler = tenantAccessor.getEventsHandler();
        final ExpressionResolverService expressionResolverService = tenantAccessor.getExpressionResolverService();

        final SExpression targetProcessNameExp = ServerModelConvertor.convertExpression(targetProcess);
        SExpression targetFlowNodeNameExp = null;
        if (targetFlowNode != null) {
            targetFlowNodeNameExp = ServerModelConvertor.convertExpression(targetFlowNode);
        }
        final SThrowMessageEventTriggerDefinitionBuilder builder = BuilderFactory.get(SThrowMessageEventTriggerDefinitionBuilderFactory.class)
                .createNewInstance(messageName, targetProcessNameExp, targetFlowNodeNameExp);
        if (correlations != null && !correlations.isEmpty()) {
            addMessageCorrelations(builder, correlations);
        }
        try {
            if (messageContent != null && !messageContent.isEmpty()) {
                addMessageContent(builder, expressionResolverService, messageContent);
            }
            final SThrowMessageEventTriggerDefinition messageEventTriggerDefinition = builder.done();
            eventsHandler.handleThrowEvent(messageEventTriggerDefinition);
        } catch (final SBonitaException e) {
            throw new SendEventException(e);
        }

    }

    private void addMessageContent(final SThrowMessageEventTriggerDefinitionBuilder messageEventTriggerDefinitionBuilder,
            final ExpressionResolverService expressionResolverService, final Map<Expression, Expression> messageContent) throws SBonitaException {
        for (final Entry<Expression, Expression> entry : messageContent.entrySet()) {
            expressionResolverService.evaluate(ServerModelConvertor.convertExpression(entry.getKey()));
            final SDataDefinitionBuilder dataDefinitionBuilder = BuilderFactory.get(SDataDefinitionBuilderFactory.class).createNewInstance(
                    entry.getKey().getContent(), entry.getValue().getReturnType());
            dataDefinitionBuilder.setDefaultValue(ServerModelConvertor.convertExpression(entry.getValue()));
            messageEventTriggerDefinitionBuilder.addData(dataDefinitionBuilder.done());
        }

    }

    private void addMessageCorrelations(final SThrowMessageEventTriggerDefinitionBuilder messageEventTriggerDefinitionBuilder,
            final Map<Expression, Expression> messageCorrelations) {
        for (final Entry<Expression, Expression> entry : messageCorrelations.entrySet()) {
            messageEventTriggerDefinitionBuilder.addCorrelation(ServerModelConvertor.convertExpression(entry.getKey()),
                    ServerModelConvertor.convertExpression(entry.getValue()));
        }
    }

    @Override
    public List<Problem> getProcessResolutionProblems(final long processDefinitionId) throws ProcessDefinitionNotFoundException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final ProcessDefinitionService processDefinitionService = tenantAccessor.getProcessDefinitionService();

        final List<ProcessDependencyResolver> resolvers = tenantAccessor.getDependencyResolver().getResolvers();
        SProcessDefinition processDefinition;
        try {
            processDefinition = processDefinitionService.getProcessDefinition(processDefinitionId);
        } catch (final SProcessDefinitionNotFoundException e) {
            throw new ProcessDefinitionNotFoundException(e);
        } catch (final SProcessDefinitionReadException e) {
            throw new ProcessDefinitionNotFoundException(e);
        }
        final ArrayList<Problem> problems = new ArrayList<Problem>();
        for (final ProcessDependencyResolver resolver : resolvers) {
            final List<Problem> problem = resolver.checkResolution(tenantAccessor, processDefinition);
            if (problem != null) {
                problems.addAll(problem);
            }
        }
        return problems;
    }

    @Override
    public List<ProcessDeploymentInfo> getProcessDeploymentInfos(final int startIndex, final int maxResults,
            final ProcessDeploymentInfoCriterion pagingCriterion) {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final ProcessDefinitionService processDefinitionService = tenantAccessor.getProcessDefinitionService();

        final SearchProcessDefinitionsDescriptor processDefinitionsDescriptor = tenantAccessor.getSearchEntitiesDescriptor()
                .getSearchProcessDefinitionsDescriptor();
        final GetProcessDefinitionDeployInfos transactionContentWithResult = new GetProcessDefinitionDeployInfos(processDefinitionService,
                processDefinitionsDescriptor, startIndex, maxResults, pagingCriterion);
        try {
            transactionContentWithResult.execute();
            return transactionContentWithResult.getResult();
        } catch (final SBonitaException e) {
            throw new RetrieveException(e);
        }
    }

    @Override
    public List<ProcessDeploymentInfo> getProcessDeploymentInfosWithActorOnlyForGroup(final long groupId, final int startIndex, final int maxResults,
            final ProcessDeploymentInfoCriterion sortingCriterion) {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final ProcessDefinitionService processDefinitionService = tenantAccessor.getProcessDefinitionService();

        final SearchProcessDefinitionsDescriptor processDefinitionsDescriptor = tenantAccessor.getSearchEntitiesDescriptor()
                .getSearchProcessDefinitionsDescriptor();
        final GetProcessDefinitionDeployInfosWithActorOnlyForGroup transactionContentWithResult = new GetProcessDefinitionDeployInfosWithActorOnlyForGroup(
                processDefinitionService, processDefinitionsDescriptor, startIndex, maxResults, sortingCriterion, groupId);
        try {
            transactionContentWithResult.execute();
            return transactionContentWithResult.getResult();
        } catch (final SBonitaException e) {
            throw new RetrieveException(e);
        }
    }

    @Override
    public List<ProcessDeploymentInfo> getProcessDeploymentInfosWithActorOnlyForGroups(final List<Long> groupIds, final int startIndex, final int maxResults,
            final ProcessDeploymentInfoCriterion sortingCriterion) {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final ProcessDefinitionService processDefinitionService = tenantAccessor.getProcessDefinitionService();

        final SearchProcessDefinitionsDescriptor processDefinitionsDescriptor = tenantAccessor.getSearchEntitiesDescriptor()
                .getSearchProcessDefinitionsDescriptor();
        final GetProcessDefinitionDeployInfosWithActorOnlyForGroups transactionContentWithResult = new GetProcessDefinitionDeployInfosWithActorOnlyForGroups(
                processDefinitionService, processDefinitionsDescriptor, startIndex, maxResults, sortingCriterion, groupIds);
        try {
            transactionContentWithResult.execute();
            return transactionContentWithResult.getResult();
        } catch (final SBonitaException e) {
            throw new RetrieveException(e);
        }
    }

    @Override
    public List<ProcessDeploymentInfo> getProcessDeploymentInfosWithActorOnlyForRole(final long roleId, final int startIndex, final int maxResults,
            final ProcessDeploymentInfoCriterion sortingCriterion) {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final ProcessDefinitionService processDefinitionService = tenantAccessor.getProcessDefinitionService();

        final SearchProcessDefinitionsDescriptor processDefinitionsDescriptor = tenantAccessor.getSearchEntitiesDescriptor()
                .getSearchProcessDefinitionsDescriptor();
        final GetProcessDefinitionDeployInfosWithActorOnlyForRole transactionContentWithResult = new GetProcessDefinitionDeployInfosWithActorOnlyForRole(
                processDefinitionService, processDefinitionsDescriptor, startIndex, maxResults, sortingCriterion, roleId);
        try {

            transactionContentWithResult.execute();
            return transactionContentWithResult.getResult();
        } catch (final SBonitaException e) {
            throw new RetrieveException(e);
        }
    }

    @Override
    public List<ProcessDeploymentInfo> getProcessDeploymentInfosWithActorOnlyForRoles(final List<Long> roleIds, final int startIndex, final int maxResults,
            final ProcessDeploymentInfoCriterion sortingCriterion) {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final ProcessDefinitionService processDefinitionService = tenantAccessor.getProcessDefinitionService();

        final SearchProcessDefinitionsDescriptor processDefinitionsDescriptor = tenantAccessor.getSearchEntitiesDescriptor()
                .getSearchProcessDefinitionsDescriptor();
        final GetProcessDefinitionDeployInfosWithActorOnlyForRoles transactionContentWithResult = new GetProcessDefinitionDeployInfosWithActorOnlyForRoles(
                processDefinitionService, processDefinitionsDescriptor, startIndex, maxResults, sortingCriterion, roleIds);
        try {
            transactionContentWithResult.execute();
            return transactionContentWithResult.getResult();
        } catch (final SBonitaException e) {
            throw new RetrieveException(e);
        }
    }

    @Override
    public List<ProcessDeploymentInfo> getProcessDeploymentInfosWithActorOnlyForUser(final long userId, final int startIndex, final int maxResults,
            final ProcessDeploymentInfoCriterion sortingCriterion) {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final ProcessDefinitionService processDefinitionService = tenantAccessor.getProcessDefinitionService();
        final SearchProcessDefinitionsDescriptor processDefinitionsDescriptor = tenantAccessor.getSearchEntitiesDescriptor()
                .getSearchProcessDefinitionsDescriptor();
        final GetProcessDefinitionDeployInfosWithActorOnlyForUser transactionContentWithResult = new GetProcessDefinitionDeployInfosWithActorOnlyForUser(
                processDefinitionService, processDefinitionsDescriptor, startIndex, maxResults, sortingCriterion, userId);
        try {
            transactionContentWithResult.execute();
            return transactionContentWithResult.getResult();
        } catch (final SBonitaException e) {
            throw new RetrieveException(e);
        }
    }

    @Override
    public List<ProcessDeploymentInfo> getProcessDeploymentInfosWithActorOnlyForUsers(final List<Long> userIds, final int startIndex, final int maxResults,
            final ProcessDeploymentInfoCriterion sortingCriterion) {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final ProcessDefinitionService processDefinitionService = tenantAccessor.getProcessDefinitionService();
        final SearchProcessDefinitionsDescriptor processDefinitionsDescriptor = tenantAccessor.getSearchEntitiesDescriptor()
                .getSearchProcessDefinitionsDescriptor();
        final GetProcessDefinitionDeployInfosWithActorOnlyForUsers transactionContentWithResult = new GetProcessDefinitionDeployInfosWithActorOnlyForUsers(
                processDefinitionService, processDefinitionsDescriptor, startIndex, maxResults, sortingCriterion, userIds);
        try {
            transactionContentWithResult.execute();
            return transactionContentWithResult.getResult();
        } catch (final SBonitaException e) {
            throw new RetrieveException(e);
        }
    }

    @Override
    public SearchResult<ConnectorInstance> searchConnectorInstances(final SearchOptions searchOptions) throws RetrieveException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final SearchEntitiesDescriptor searchEntitiesDescriptor = tenantAccessor.getSearchEntitiesDescriptor();
        final ConnectorInstanceService connectorInstanceService = tenantAccessor.getConnectorInstanceService();
        final SearchConnectorInstances searchConnector = new SearchConnectorInstances(connectorInstanceService,
                searchEntitiesDescriptor.getSearchConnectorInstanceDescriptor(), searchOptions);
        try {
            searchConnector.execute();
        } catch (final SBonitaException e) {
            throw new RetrieveException(e);
        }
        return searchConnector.getResult();
    }

    @Override
    public SearchResult<ArchivedConnectorInstance> searchArchivedConnectorInstances(final SearchOptions searchOptions) throws RetrieveException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final SearchEntitiesDescriptor searchEntitiesDescriptor = tenantAccessor.getSearchEntitiesDescriptor();
        final ConnectorInstanceService connectorInstanceService = tenantAccessor.getConnectorInstanceService();
        final ArchiveService archiveService = tenantAccessor.getArchiveService();
        final ReadPersistenceService persistenceService = archiveService.getDefinitiveArchiveReadPersistenceService();
        final SearchArchivedConnectorInstance searchArchivedConnectorInstance = new SearchArchivedConnectorInstance(connectorInstanceService,
                searchEntitiesDescriptor.getSearchArchivedConnectorInstanceDescriptor(), searchOptions, persistenceService);
        try {
            searchArchivedConnectorInstance.execute();
        } catch (final SBonitaException e) {
            throw new RetrieveException(e);
        }
        return searchArchivedConnectorInstance.getResult();
    }

    @Override
    public List<HumanTaskInstance> getHumanTaskInstances(final long processInstanceId, final String taskName, final int startIndex, final int maxResults) {
        try {
            return getHumanTaskInstances(processInstanceId, taskName, startIndex, maxResults, HumanTaskInstanceSearchDescriptor.PROCESS_INSTANCE_ID, Order.ASC);
        } catch (final NotFoundException e) {
            return Collections.emptyList();
        }
    }

    @Override
    public HumanTaskInstance getLastStateHumanTaskInstance(final long processInstanceId, final String taskName) throws NotFoundException {
        return getHumanTaskInstances(processInstanceId, taskName, 0, 1, HumanTaskInstanceSearchDescriptor.REACHED_STATE_DATE, Order.DESC).get(0);
    }

    private List<HumanTaskInstance> getHumanTaskInstances(final long processInstanceId, final String taskName, final int startIndex, final int maxResults,
            final String field, final Order order) throws NotFoundException {
        final SearchOptionsBuilder builder = new SearchOptionsBuilder(startIndex, maxResults);
        builder.filter(HumanTaskInstanceSearchDescriptor.PROCESS_INSTANCE_ID, processInstanceId).filter(HumanTaskInstanceSearchDescriptor.NAME, taskName);
        builder.sort(field, order);
        try {
            final SearchResult<HumanTaskInstance> searchHumanTasks = searchHumanTaskInstances(builder.done());
            if (searchHumanTasks.getCount() == 0) {
                throw new NotFoundException("Task '" + taskName + "' not found");
            }
            return searchHumanTasks.getResult();
        } catch (final SearchException se) {
            throw new RetrieveException(se);
        }
    }

    @Override
    public SearchResult<User> searchUsersWhoCanStartProcessDefinition(final long processDefinitionId, final SearchOptions searchOptions) throws SearchException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();

        final ProcessDefinitionService processDefinitionService = tenantAccessor.getProcessDefinitionService();
        final SearchEntitiesDescriptor searchEntitiesDescriptor = tenantAccessor.getSearchEntitiesDescriptor();
        final SearchUserDescriptor searchDescriptor = searchEntitiesDescriptor.getSearchUserDescriptor();
        final SearchUsersWhoCanStartProcessDeploymentInfo transactionSearch = new SearchUsersWhoCanStartProcessDeploymentInfo(processDefinitionService,
                searchDescriptor, processDefinitionId, searchOptions);
        try {
            transactionSearch.execute();
        } catch (final SBonitaException e) {
            throw new SearchException(e);
        }
        return transactionSearch.getResult();
    }

    @Override
    public Map<String, Serializable> evaluateExpressionsAtProcessInstanciation(final long processInstanceId,
            final Map<Expression, Map<String, Serializable>> expressions) throws ExpressionEvaluationException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final ProcessInstanceService processInstanceService = tenantAccessor.getProcessInstanceService();
        try {
            try {
                final SProcessInstance processInstance = processInstanceService.getProcessInstance(processInstanceId);
                // if it exists and is initializing or started
                final int stateId = processInstance.getStateId();
                if (stateId == 0/* initializing */|| stateId == 1/* started */) {
                    // the evaluation date is either now (initializing) or the start date if available
                    final long evaluationDate = stateId == 0 ? System.currentTimeMillis() : processInstance.getStartDate();
                    return evaluateExpressionsInstanceLevelAndArchived(expressions, processInstanceId, CONTAINER_TYPE_PROCESS_INSTANCE,
                            processInstance.getProcessDefinitionId(), evaluationDate);
                }
            } catch (final SProcessInstanceNotFoundException spinfe) {
                // get it in the archive
            }
            final ArchivedProcessInstance archiveProcessInstance = getStartedArchivedProcessInstance(processInstanceId);
            final Map<String, Serializable> evaluateExpressionInArchiveProcessInstance = evaluateExpressionsInstanceLevelAndArchived(expressions,
                    processInstanceId, CONTAINER_TYPE_PROCESS_INSTANCE, archiveProcessInstance.getProcessDefinitionId(), archiveProcessInstance.getStartDate()
                            .getTime());
            return evaluateExpressionInArchiveProcessInstance;
        } catch (final SBonitaException e) {
            throw new ExpressionEvaluationException(e);
        }
    }

    @Override
    public Map<String, Serializable> evaluateExpressionOnCompletedProcessInstance(final long processInstanceId,
            final Map<Expression, Map<String, Serializable>> expressions) throws ExpressionEvaluationException {
        try {
            final ArchivedProcessInstance lastArchivedProcessInstance = getLastArchivedProcessInstance(processInstanceId);
            return evaluateExpressionsInstanceLevelAndArchived(expressions, processInstanceId, CONTAINER_TYPE_PROCESS_INSTANCE,
                    lastArchivedProcessInstance.getProcessDefinitionId(), lastArchivedProcessInstance.getArchiveDate().getTime());
        } catch (final SProcessInstanceNotFoundException e) {
            throw new ExpressionEvaluationException(e);
        } catch (final SBonitaException e) {
            throw new ExpressionEvaluationException(e);
        }
    }

    @Override
    public Map<String, Serializable> evaluateExpressionsOnProcessInstance(final long processInstanceId,
            final Map<Expression, Map<String, Serializable>> expressions) throws ExpressionEvaluationException {
        try {
            return evaluateExpressionsInstanceLevel(expressions, processInstanceId, CONTAINER_TYPE_PROCESS_INSTANCE, getProcessInstance(processInstanceId)
                    .getProcessDefinitionId());
        } catch (final BonitaException e) {
            throw new ExpressionEvaluationException(e);
        } catch (final SBonitaException e) {
            throw new ExpressionEvaluationException(e);
        }
    }

    @Override
    public Map<String, Serializable> evaluateExpressionsOnProcessDefinition(final long processDefinitionId,
            final Map<Expression, Map<String, Serializable>> expressions) throws ExpressionEvaluationException {
        try {
            return evaluateExpressionsDefinitionLevel(expressions, processDefinitionId);
        } catch (final SProcessInstanceNotFoundException e) {
            throw new ExpressionEvaluationException(e);
        } catch (final SBonitaException e) {
            throw new ExpressionEvaluationException(e);
        }
    }

    @Override
    public Map<String, Serializable> evaluateExpressionsOnActivityInstance(final long activityInstanceId,
            final Map<Expression, Map<String, Serializable>> expressions) throws ExpressionEvaluationException {
        try {
            final ActivityInstance activityInstance = getActivityInstance(activityInstanceId);
            final ProcessInstance processInstance = getProcessInstance(activityInstance.getParentProcessInstanceId());

            return evaluateExpressionsInstanceLevel(expressions, activityInstanceId, CONTAINER_TYPE_ACTIVITY_INSTANCE, processInstance.getProcessDefinitionId());
        } catch (final BonitaException e) {
            throw new ExpressionEvaluationException(e);
        } catch (final SBonitaException e) {
            throw new ExpressionEvaluationException(e);
        }
    }

    @Override
    public Map<String, Serializable> evaluateExpressionsOnCompletedActivityInstance(final long activityInstanceId,
            final Map<Expression, Map<String, Serializable>> expressions) throws ExpressionEvaluationException {
        try {
            final ArchivedActivityInstance activityInstance = getArchivedActivityInstance(activityInstanceId);
            // same archive time to process even if there're many activities in the process
            final ArchivedProcessInstance lastArchivedProcessInstance = getLastArchivedProcessInstance(activityInstance.getParentContainerId());

            return evaluateExpressionsInstanceLevelAndArchived(expressions, activityInstanceId, CONTAINER_TYPE_ACTIVITY_INSTANCE,
                    lastArchivedProcessInstance.getProcessDefinitionId(), activityInstance.getArchiveDate().getTime());
        } catch (final ActivityInstanceNotFoundException e) {
            throw new ExpressionEvaluationException(e);
        } catch (final SBonitaException e) {
            throw new ExpressionEvaluationException(e);
        }
    }

    private Map<String, Serializable> evaluateExpressionsDefinitionLevel(final Map<Expression, Map<String, Serializable>> expressionsAndTheirPartialContext,
            final long processDefinitionId) throws SBonitaException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final ExpressionResolverService expressionResolverService = tenantAccessor.getExpressionResolverService();
        final ProcessDefinitionService processDefinitionService = tenantAccessor.getProcessDefinitionService();
        final EvaluateExpressionsDefinitionLevel evaluations = new EvaluateExpressionsDefinitionLevel(expressionsAndTheirPartialContext, processDefinitionId,
                expressionResolverService, processDefinitionService);
        evaluations.execute();
        return evaluations.getResult();
    }

    private Map<String, Serializable> evaluateExpressionsInstanceLevel(final Map<Expression, Map<String, Serializable>> expressions, final long containerId,
            final String containerType, final long processDefinitionId) throws SBonitaException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final ExpressionResolverService expressionService = tenantAccessor.getExpressionResolverService();

        final EvaluateExpressionsInstanceLevel evaluations = new EvaluateExpressionsInstanceLevel(expressions, containerId, containerType, processDefinitionId,
                expressionService);
        evaluations.execute();
        return evaluations.getResult();
    }

    private Map<String, Serializable> evaluateExpressionsInstanceLevelAndArchived(final Map<Expression, Map<String, Serializable>> expressions,
            final long containerId, final String containerType, final long processDefinitionId, final long time) throws SBonitaException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final ExpressionResolverService expressionService = tenantAccessor.getExpressionResolverService();
        final EvaluateExpressionsInstanceLevelAndArchived evaluations = new EvaluateExpressionsInstanceLevelAndArchived(expressions, containerId,
                containerType, processDefinitionId, time, expressionService);
        evaluations.execute();
        return evaluations.getResult();
    }

    private ArchivedProcessInstance getStartedArchivedProcessInstance(final long processInstanceId) throws SBonitaException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final ProcessInstanceService processInstanceService = tenantAccessor.getProcessInstanceService();
        final SearchEntitiesDescriptor searchEntitiesDescriptor = tenantAccessor.getSearchEntitiesDescriptor();

        final SearchOptionsBuilder searchOptionsBuilder = new SearchOptionsBuilder(0, 2);
        searchOptionsBuilder.sort(ArchivedProcessInstancesSearchDescriptor.ARCHIVE_DATE, Order.ASC);
        searchOptionsBuilder.filter(ArchivedProcessInstancesSearchDescriptor.SOURCE_OBJECT_ID, processInstanceId);
        searchOptionsBuilder.filter(ArchivedProcessInstancesSearchDescriptor.STATE_ID, ProcessInstanceState.STARTED.getId());
        final SearchArchivedProcessInstances searchArchivedProcessInstances = new SearchArchivedProcessInstances(processInstanceService,
                searchEntitiesDescriptor.getSearchArchivedProcessInstanceDescriptor(), searchOptionsBuilder.done());
        searchArchivedProcessInstances.execute();

        try {
            return searchArchivedProcessInstances.getResult().getResult().get(0);
        } catch (final IndexOutOfBoundsException e) {
            throw new SAProcessInstanceNotFoundException(processInstanceId, ProcessInstanceState.STARTED.name());
        }

    }

    private ArchivedProcessInstance getLastArchivedProcessInstance(final long processInstanceId) throws SBonitaException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final ProcessInstanceService processInstanceService = tenantAccessor.getProcessInstanceService();
        final SearchEntitiesDescriptor searchEntitiesDescriptor = tenantAccessor.getSearchEntitiesDescriptor();
        final GetLastArchivedProcessInstance searchArchivedProcessInstances = new GetLastArchivedProcessInstance(processInstanceService, processInstanceId,
                searchEntitiesDescriptor);

        searchArchivedProcessInstances.execute();
        return searchArchivedProcessInstances.getResult();
    }

    @Override
    public List<FailedJob> getFailedJobs(final int startIndex, final int maxResults) {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final JobService jobService = tenantAccessor.getJobService();
        try {
            final List<SFailedJob> failedJobs = jobService.getFailedJobs(startIndex, maxResults);
            return ModelConvertor.toFailedJobs(failedJobs);
        } catch (final SSchedulerException sse) {
            throw new RetrieveException(sse);
        }
    }

    @Override
    public void replayFailedJob(final long jobDescriptorId) throws ExecutionException {
        replayFailedJob(jobDescriptorId, null);
    }

    @Override
    public void replayFailedJob(final long jobDescriptorId, final Map<String, Serializable> parameters) throws ExecutionException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final SchedulerService schedulerService = tenantAccessor.getSchedulerService();
        try {
            if (parameters == null || parameters.isEmpty()) {
                schedulerService.executeAgain(jobDescriptorId);
            } else {
                final List<SJobParameter> jobParameters = getJobParameters(parameters);
                schedulerService.executeAgain(jobDescriptorId, jobParameters);
            }
        } catch (final SSchedulerException sse) {
            throw new ExecutionException(sse);
        }
    }

    protected List<SJobParameter> getJobParameters(final Map<String, Serializable> parameters) {
        final List<SJobParameter> jobParameters = new ArrayList<SJobParameter>();
        for (final Entry<String, Serializable> parameter : parameters.entrySet()) {
            jobParameters.add(getSJobParameterBuilderFactory().createNewInstance(parameter.getKey(), parameter.getValue()).done());
        }
        return jobParameters;
    }

    protected SJobParameterBuilderFactory getSJobParameterBuilderFactory() {
        return BuilderFactory.get(SJobParameterBuilderFactory.class);
    }

    @Override
    public ArchivedDataInstance getArchivedProcessDataInstance(final String dataName, final long processInstanceId) throws ArchivedDataNotFoundException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final DataInstanceService dataInstanceService = tenantAccessor.getDataInstanceService();
        try {
            final SADataInstance dataInstance = dataInstanceService.getLastSADataInstance(dataName, processInstanceId,
                    DataInstanceContainer.PROCESS_INSTANCE.toString());
            return ModelConvertor.toArchivedDataInstance(dataInstance);
        } catch (final SDataInstanceException sdie) {
            throw new ArchivedDataNotFoundException(sdie);
        }
    }

    @Override
    public ArchivedDataInstance getArchivedActivityDataInstance(final String dataName, final long activityInstanceId) throws ArchivedDataNotFoundException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final DataInstanceService dataInstanceService = tenantAccessor.getDataInstanceService();
        try {
            final SADataInstance dataInstance = dataInstanceService.getLastSADataInstance(dataName, activityInstanceId,
                    DataInstanceContainer.ACTIVITY_INSTANCE.toString());
            return ModelConvertor.toArchivedDataInstance(dataInstance);
        } catch (final SDataInstanceException sdie) {
            throw new ArchivedDataNotFoundException(sdie);
        }
    }

    @Override
    public List<ArchivedDataInstance> getArchivedProcessDataInstances(final long processInstanceId, final int startIndex, final int maxResults) {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final DataInstanceService dataInstanceService = tenantAccessor.getDataInstanceService();
        try {
            final List<SADataInstance> dataInstances = dataInstanceService.getLastLocalSADataInstances(processInstanceId,
                    DataInstanceContainer.PROCESS_INSTANCE.toString(), startIndex, maxResults);
            return ModelConvertor.toArchivedDataInstances(dataInstances);
        } catch (final SDataInstanceException sdie) {
            throw new RetrieveException(sdie);
        }
    }

    @Override
    public List<ArchivedDataInstance> getArchivedActivityDataInstances(final long activityInstanceId, final int startIndex, final int maxResults) {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final DataInstanceService dataInstanceService = tenantAccessor.getDataInstanceService();
        try {
            final List<SADataInstance> dataInstances = dataInstanceService.getLastLocalSADataInstances(activityInstanceId,
                    DataInstanceContainer.ACTIVITY_INSTANCE.toString(), startIndex, maxResults);
            return ModelConvertor.toArchivedDataInstances(dataInstances);
        } catch (final SDataInstanceException sdie) {
            throw new RetrieveException(sdie);
        }
    }

    @Override
    public List<User> getPossibleUsersOfPendingHumanTask(final long humanTaskInstanceId, final int startIndex, final int maxResults) {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final ActivityInstanceService activityInstanceService = tenantAccessor.getActivityInstanceService();
        try {
            // pagination of this method is based on order by username:
            final List<Long> userIds = activityInstanceService.getPossibleUserIdsOfPendingTasks(humanTaskInstanceId, startIndex, maxResults);
            final IdentityService identityService = getTenantAccessor().getIdentityService();
            // This method below is also ordered by username, so the order is preserved:
            final List<SUser> sUsers = identityService.getUsers(userIds);
            return ModelConvertor.toUsers(sUsers);
        } catch (final SBonitaException sbe) {
            throw new RetrieveException(sbe);
        }
    }

    @Override
    public List<User> getPossibleUsersOfHumanTask(final long processDefinitionId, final String humanTaskName, final int startIndex, final int maxResults) {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final ProcessDefinitionService processDefinitionService = tenantAccessor.getProcessDefinitionService();
        try {
            final SProcessDefinition processDefinition = processDefinitionService.getProcessDefinition(processDefinitionId);
            final SFlowNodeDefinition flowNode = processDefinition.getProcessContainer().getFlowNode(humanTaskName);
            if (!(flowNode instanceof SHumanTaskDefinition)) {
                return Collections.emptyList();
            }
            final SHumanTaskDefinition humanTask = (SHumanTaskDefinition) flowNode;
            final String actorName = humanTask.getActorName();
            final ActorMappingService actorMappingService = getTenantAccessor().getActorMappingService();
            final SActor actor = actorMappingService.getActor(actorName, processDefinitionId);
            final List<Long> userIds = actorMappingService.getPossibleUserIdsOfActorId(actor.getId(), startIndex, maxResults);
            final List<SUser> users = tenantAccessor.getIdentityService().getUsers(userIds);
            return ModelConvertor.toUsers(users);
        } catch (final SProcessDefinitionNotFoundException spdnfe) {
            return Collections.emptyList();
        } catch (final SBonitaException sbe) {
            throw new RetrieveException(sbe);
        }
    }

}
