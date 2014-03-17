/**
 * Copyright (C) 2011 BonitaSoft S.A.
 * BonitaSoft, 32 rue Gustave Eiffel - 38000 Grenoble
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2.0 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.bonitasoft.engine.connector;

import java.util.Map;

import org.bonitasoft.engine.commons.ServiceWithLifecycle;
import org.bonitasoft.engine.connector.exception.SConnectorException;

/**
 * @author Feng Hui
 * @since 6.0
 */
public interface ConnectorExecutor extends ServiceWithLifecycle {

    /**
     * Execute a connector.
     * 
     * @param sConnector
     *            The connector will be executed
     * @param inputParameters
     *            The input parameters of connector
     * @return
     *         The output after connector executing
     * @throws SConnectorException
     *             Error thrown when error occurs in connector executing
     */
    Map<String, Object> execute(SConnector sConnector, Map<String, Object> inputParameters) throws SConnectorException;

    /**
     * call disconnect method of the connector
     * 
     * @param sConnector
     * @throws SConnectorException
     */
    void disconnect(SConnector sConnector) throws SConnectorException;

}
