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
package org.bonitasoft.engine.bpm.flownode;

import org.bonitasoft.engine.bpm.BaseElement;
import org.bonitasoft.engine.bpm.DescriptionElement;

/**
 * @author Baptiste Mesta
 * @author Elias Ricken de Medeiros
 * @author Celine Souchet
 */
public interface FlowNodeInstance extends DescriptionElement, BaseElement {

    /**
     * Returns the task's direct container ID. For a sub-task for CallActivity, would point to the containing activity ID of the current element.
     * For a normal Task / Activity, would point to its containing process instance ID.
     * 
     * @return the ID of the direct containing element (activity instance of process instance).
     */
    long getParentContainerId();

    /**
     * Returns the root container ID. It is the ID of the root-level containing process instance.
     * 
     * @return the root container ID.
     */
    long getRootContainerId();

    /**
     * Returns the ID of the process definition where this <code>FlowNodeInstance</code> is defined.
     * 
     * @return the ID of the process definition.
     */
    long getProcessDefinitionId();

    /**
     * Always returns the directly containing process instance ID (at the lower level, if several levels of containing processes).
     * 
     * @return the ID of the lowest-level containing process instance.
     */
    long getParentProcessInstanceId();

    /**
     * Returns a String representation of this FlowNodeInstance state.
     * 
     * @return this FlowNodeInstance state
     */
    String getState();

    StateCategory getStateCategory();

    /**
     * Returns the <code>FlowNodeType</code> that precises the concrete type of this <code>FlowNodeInstance</code>.
     * 
     * @return the <code>FlowNodeType</code>
     */
    FlowNodeType getType();

    String getDisplayDescription();

    String getDisplayName();

    /**
     * @return id of the user who originally executed the flow node
     * @since 6.0.1
     */
    long getExecutedBy();

    /**
     * @return id of the user (delegate) who executed the flow node for the original executer
     * @since 6.0.1
     */
    long getExecutedByDelegate();

    /**
     * Returns the ID of the flow node definition of this instance.
     * 
     * @return the ID of the flow node definition that this <code>FlowNodeInstance</code> is an instance of.
     */
    long getFlownodeDefinitionId();
}
