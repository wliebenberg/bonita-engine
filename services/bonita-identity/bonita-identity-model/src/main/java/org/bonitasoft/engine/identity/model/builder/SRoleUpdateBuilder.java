/**
 * Copyright (C) 2011-2012 BonitaSoft S.A.
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
package org.bonitasoft.engine.identity.model.builder;

/**
 * @author Baptiste Mesta
 * @author Bole Zhang
 * @author Matthieu Chaffotte
 */
public interface SRoleUpdateBuilder extends SIdentityUpdateBuilder {

    SRoleUpdateBuilder updateName(final String name);

    SRoleUpdateBuilder updateDisplayName(final String displayName);

    SRoleUpdateBuilder updateDescription(final String description);

    SRoleUpdateBuilder updateIconName(final String iconName);

    SRoleUpdateBuilder updateIconPath(final String iconPath);

    SRoleUpdateBuilder updateCreatedBy(final long createdBy);

    SRoleUpdateBuilder updateCreationDate(final long creationDate);

    SRoleUpdateBuilder updateLastUpdate(final long lastUpdate);

}
