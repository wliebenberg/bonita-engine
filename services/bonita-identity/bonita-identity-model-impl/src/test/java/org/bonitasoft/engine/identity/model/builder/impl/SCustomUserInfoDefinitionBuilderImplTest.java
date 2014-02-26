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
 */
package org.bonitasoft.engine.identity.model.builder.impl;

import org.bonitasoft.engine.identity.model.SCustomUserInfoDefinition;
import org.bonitasoft.engine.identity.model.impl.SCustomUserInfoDefinitionImpl;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Vincent Elcrin
 */
public class SCustomUserInfoDefinitionBuilderImplTest {

    private SCustomUserInfoDefinitionBuilderImpl builder;

    @Before
    public void setUp() throws Exception {
        builder = new SCustomUserInfoDefinitionBuilderImpl(new SCustomUserInfoDefinitionImpl());
    }

    @Test
    public void should_build_an_entity_with_the_right_id() throws Exception {
        builder.setId(1L);

        SCustomUserInfoDefinition entity = builder.done();

        assertEquals(1L, entity.getId());
    }

    @Test
    public void should_build_an_entity_with_the_right_name() throws Exception {
        builder.setName("name");

        SCustomUserInfoDefinition entity = builder.done();

        assertEquals("name", entity.getName());
    }

    @Test
    public void should_build_an_entity_with_the_right_display_name() throws Exception {
        builder.setDisplayName("display name");

        SCustomUserInfoDefinition entity = builder.done();

        assertEquals("display name", entity.getDisplayName());
    }

    @Test
    public void should_build_an_entity_with_the_right_description() throws Exception {
        builder.setDescription("description");

        SCustomUserInfoDefinition entity = builder.done();

        assertEquals("description", entity.getDescription());
    }
}