/**
 * Copyright (C) 2011 BonitaSoft S.A.
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
package org.bonitasoft.engine.platform.model;

import org.bonitasoft.engine.persistence.PersistentObject;

/**
 * @author Charles Souillard
 */
public interface STenant extends PersistentObject {

    public static final String PAUSED = "PAUSED";

    public static final String DEACTIVATED = "DEACTIVATED";

    public static final String ACTIVATED = "ACTIVATED";

    String getName();

    String getDescription();

    String getIconName();

    String getIconPath();

    long getCreated();

    String getCreatedBy();

    String getStatus();

    boolean isDefaultTenant();

    boolean isPaused();

}
