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
 * @author Matthieu Chaffotte
 */
public interface SGroupUpdateBuilder extends SIdentityUpdateBuilder {

    SGroupUpdateBuilder updateName(final String name);

    SGroupUpdateBuilder updateDisplayName(final String displayName);

    SGroupUpdateBuilder updateDescription(final String description);

    SGroupUpdateBuilder updateParentPath(final String parentPath);

    SGroupUpdateBuilder updateIconName(final String iconName);

    SGroupUpdateBuilder updateIconPath(final String iconPath);

    SGroupUpdateBuilder updateCreatedBy(final long createdBy);

    SGroupUpdateBuilder updateCreationDate(final long creationDate);

    SGroupUpdateBuilder updateLastUpdate(final long lastUpdate);

}
