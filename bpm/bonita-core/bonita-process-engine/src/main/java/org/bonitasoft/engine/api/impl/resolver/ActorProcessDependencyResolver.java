/**
 * Copyright (C) 2012-2013 BonitaSoft S.A.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bonitasoft.engine.actor.mapping.ActorMappingService;
import org.bonitasoft.engine.actor.mapping.model.SActor;
import org.bonitasoft.engine.actor.mapping.model.SActorBuilder;
import org.bonitasoft.engine.actor.mapping.model.SActorBuilderFactory;
import org.bonitasoft.engine.actor.mapping.model.SActorMember;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.api.impl.transaction.actor.ImportActorMapping;
import org.bonitasoft.engine.bpm.actor.ActorMappingImportException;
import org.bonitasoft.engine.bpm.bar.ActorMappingContribution;
import org.bonitasoft.engine.bpm.bar.BusinessArchive;
import org.bonitasoft.engine.bpm.process.Problem;
import org.bonitasoft.engine.bpm.process.Problem.Level;
import org.bonitasoft.engine.bpm.process.impl.ProblemImpl;
import org.bonitasoft.engine.builder.BuilderFactory;
import org.bonitasoft.engine.commons.exceptions.SBonitaException;
import org.bonitasoft.engine.core.process.definition.model.SActorDefinition;
import org.bonitasoft.engine.core.process.definition.model.SProcessDefinition;
import org.bonitasoft.engine.identity.IdentityService;
import org.bonitasoft.engine.persistence.SBonitaReadException;
import org.bonitasoft.engine.service.TenantServiceAccessor;
import org.bonitasoft.engine.xml.Parser;

/**
 * @author Baptiste Mesta
 * @author Matthieu Chaffotte
 */
public class ActorProcessDependencyResolver extends ProcessDependencyResolver {

    @Override
    public boolean resolve(final ProcessAPI processApi, final TenantServiceAccessor tenantAccessor, final BusinessArchive businessArchive,
            final SProcessDefinition sDefinition) throws ActorMappingImportException {
        final ActorMappingService actorMappingService = tenantAccessor.getActorMappingService();
        final SActorBuilderFactory sActorBuilderFactory = BuilderFactory.getInstance().get(SActorBuilderFactory.class);
        final IdentityService identityService = tenantAccessor.getIdentityService();
        Parser parser = null;
        try {
            parser = tenantAccessor.getActorMappingParser();
        } catch (final IllegalArgumentException e) {
            throw new ActorMappingImportException("unable to instantiate parser of actor mapping", e);
        }
        final Set<SActorDefinition> actors = sDefinition.getActors();
        final Set<SActor> sActors = new HashSet<SActor>(actors.size() + 1);
        final SActorDefinition actorInitiator = sDefinition.getActorInitiator();
        String initiatorName = null;
        if (actorInitiator != null) {
            initiatorName = actorInitiator.getName();
            final SActorBuilder sActorBuilder = sActorBuilderFactory.create(initiatorName, sDefinition.getId(), true);
            sActorBuilder.addDescription(actorInitiator.getDescription());
            sActors.add(sActorBuilder.getActor());
        }
        for (final SActorDefinition actor : actors) {
            if (initiatorName == null || !initiatorName.equals(actor.getName())) {
                final SActorBuilder sActorBuilder = sActorBuilderFactory.create(actor.getName(), sDefinition.getId(), false);
                sActorBuilder.addDescription(actor.getDescription());
                sActors.add(sActorBuilder.getActor());
            }
        }
        try {
            actorMappingService.addActors(sActors);
            final byte[] actorMappingXML = businessArchive.getResource(ActorMappingContribution.ACTOR_MAPPING_FILE);
            if (actorMappingXML != null) {
                final String actorMapping = new String(actorMappingXML);
                final ImportActorMapping importActorMapping = new ImportActorMapping(actorMappingService, identityService, parser, sDefinition.getId(),
                        actorMapping);
                try {
                    importActorMapping.execute();
                } catch (final SBonitaException e) {
                    // ignore
                }
            }
        } catch (final SBonitaException e) {
            // ignored
        }
        return checkResolution(actorMappingService, sDefinition.getId()).isEmpty();
    }

    @Override
    public List<Problem> checkResolution(final TenantServiceAccessor tenantAccessor, final SProcessDefinition processDefinition) {
        final ActorMappingService actorMappingService = tenantAccessor.getActorMappingService();
        final long processDefinitionId = processDefinition.getId();
        return checkResolution(actorMappingService, processDefinitionId);
    }

    public List<Problem> checkResolution(final ActorMappingService actorMappingService, final long processDefinitionId) {
        List<SActor> actors;
        try {
            actors = actorMappingService.getActors(processDefinitionId);
            final List<Problem> problems = new ArrayList<Problem>();
            for (final SActor sActor : actors) {
                final List<SActorMember> actorMembers = actorMappingService.getActorMembers(sActor.getId(), 0, 1);
                if (actorMembers.isEmpty()) {
                    final Problem problem = new ProblemImpl(Level.ERROR, sActor.getId(), "actor", "Actor '" + sActor.getName()
                            + "' does not contain any members");
                    problems.add(problem);
                }
            }
            return problems;
        } catch (final SBonitaReadException e) {
            return Collections.singletonList((Problem) new ProblemImpl(Level.ERROR, processDefinitionId, "process", "Unable to read actors"));
        }
    }
}
