/**
 * Copyright (C) 2011-2013 BonitaSoft S.A.
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
package org.bonitasoft.engine.exception;

/**
 * @author Matthieu Chaffotte
 * @author Baptiste Mesta
 */
public class BonitaException extends Exception implements BonitaContextException {

    private static final long serialVersionUID = -5413586694735909486L;

    private long tenantId;
    private String hostname;
    private String userName;


    public BonitaException(final String message) {
        super(message);
    }

    public BonitaException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public BonitaException(final Throwable cause) {
        super(cause);
    }


    public long getTenantId() {
        return tenantId;
    }

    public void setTenantId(final long tenantId) {
        this.tenantId = tenantId;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(final String hostname) {
        this.hostname = hostname;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(final String userName) {
        this.userName = userName;
    }

    @Override
    public String getMessage() {
        return "Hostname : " + hostname + " tenantId: " + tenantId + " " + super.getMessage();
    }
}
