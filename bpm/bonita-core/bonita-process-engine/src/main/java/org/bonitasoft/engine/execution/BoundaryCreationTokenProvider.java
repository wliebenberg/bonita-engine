/**
 * Copyright (C) 2014 BonitaSoft S.A.
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
package org.bonitasoft.engine.execution;

import org.bonitasoft.engine.commons.exceptions.SObjectCreationException;
import org.bonitasoft.engine.commons.exceptions.SObjectNotFoundException;
import org.bonitasoft.engine.commons.exceptions.SObjectReadException;
import org.bonitasoft.engine.core.process.definition.model.event.SBoundaryEventDefinition;
import org.bonitasoft.engine.core.process.instance.api.TokenService;
import org.bonitasoft.engine.core.process.instance.model.SActivityInstance;
import org.bonitasoft.engine.core.process.instance.model.SToken;
import org.bonitasoft.engine.execution.flowmerger.TokenInfo;

/**
 * @author Elias Ricken de Medeiros
 */
public class BoundaryCreationTokenProvider implements TokenProvider {

    private SBoundaryEventDefinition boundaryEventDefinition;

    private TokenService tokenService;

    private SActivityInstance relatedActivityInstance;

    public BoundaryCreationTokenProvider(SActivityInstance relatedActivityInstance, SBoundaryEventDefinition boundaryEventDefinition, TokenService tokenService) {
        this.boundaryEventDefinition = boundaryEventDefinition;
        this.tokenService = tokenService;
        this.relatedActivityInstance = relatedActivityInstance;
    }

    @Override
    public TokenInfo getOutputTokenInfo() throws SObjectReadException, SObjectNotFoundException, SObjectCreationException {
        if (boundaryEventDefinition.isInterrupting()) {
            SToken token = tokenService.getToken(relatedActivityInstance.getParentProcessInstanceId(), relatedActivityInstance.getTokenRefId());
            return new TokenInfo(token.getRefId(), token.getParentRefId());
        }
        return new TokenInfo(boundaryEventDefinition.getId());
    }

}
