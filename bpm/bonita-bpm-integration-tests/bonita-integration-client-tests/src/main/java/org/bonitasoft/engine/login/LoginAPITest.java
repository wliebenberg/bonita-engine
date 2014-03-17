/**
 * Copyright (C) 2013 BonitaSoft S.A.
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
package org.bonitasoft.engine.login;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.bonitasoft.engine.BPMRemoteTests;
import org.bonitasoft.engine.CommonAPITest;
import org.bonitasoft.engine.api.IdentityAPI;
import org.bonitasoft.engine.api.LoginAPI;
import org.bonitasoft.engine.api.PlatformAPIAccessor;
import org.bonitasoft.engine.api.PlatformCommandAPI;
import org.bonitasoft.engine.api.TenantAPIAccessor;
import org.bonitasoft.engine.command.CommandExecutionException;
import org.bonitasoft.engine.command.CommandNotFoundException;
import org.bonitasoft.engine.command.CommandParameterizationException;
import org.bonitasoft.engine.command.DependencyNotFoundException;
import org.bonitasoft.engine.exception.AlreadyExistsException;
import org.bonitasoft.engine.exception.BonitaException;
import org.bonitasoft.engine.exception.CreationException;
import org.bonitasoft.engine.exception.DeletionException;
import org.bonitasoft.engine.identity.User;
import org.bonitasoft.engine.identity.UserUpdater;
import org.bonitasoft.engine.platform.LoginException;
import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.engine.session.PlatformSession;
import org.bonitasoft.engine.session.SessionNotFoundException;
import org.bonitasoft.engine.test.annotation.Cover;
import org.bonitasoft.engine.test.annotation.Cover.BPMNConcept;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Elias Ricken de Medeiros
 */
public class LoginAPITest extends CommonAPITest {

    private static final String COMMAND_NAME = "deleteSession";

    private static final String COMMAND_DEPENDENCY_NAME = "deleteSessionCommand";

    private static PlatformCommandAPI platformCommandAPI;

    private static PlatformSession session;

    @Before
    public void before() throws BonitaException {
        session = loginPlatform();
        platformCommandAPI = PlatformAPIAccessor.getPlatformCommandAPI(session);
    }

    @After
    public void after() throws BonitaException {
        logoutPlatform(session);
    }

    @Test(expected = SessionNotFoundException.class)
    public void testSessionNotFoundExceptionIsThrownAfterSessionDeletion() throws Exception {
        // login to create a session
        login();
        final long sessionId = getSession().getId();

        // delete the session created by the login
        deleteSession(sessionId);

        // will throw SessionNotFoundException
        logout();
    }

    private void deleteSession(final long sessionId) throws IOException, AlreadyExistsException, CreationException, CreationException,
            CommandNotFoundException, CommandParameterizationException, CommandExecutionException, DeletionException, DependencyNotFoundException {
        // deploy and execute a command to delete a session
        final InputStream stream = BPMRemoteTests.class.getResourceAsStream("/session-commands.jar.bak");
        assertNotNull(stream);
        final byte[] byteArray = IOUtils.toByteArray(stream);
        platformCommandAPI.addDependency(COMMAND_DEPENDENCY_NAME, byteArray);
        platformCommandAPI.register(COMMAND_NAME, "Delete a session", "org.bonitasoft.engine.command.DeleteSessionCommand");
        final Map<String, Serializable> parameters = new HashMap<String, Serializable>();
        parameters.put("sessionId", sessionId);
        platformCommandAPI.execute(COMMAND_NAME, parameters);
        platformCommandAPI.unregister(COMMAND_NAME);
        platformCommandAPI.removeDependency(COMMAND_DEPENDENCY_NAME);
    }

    @Test(expected = LoginException.class)
    public void loginFailsWithNullUsername() throws BonitaException {
        final LoginAPI loginTenant = TenantAPIAccessor.getLoginAPI();
        loginTenant.login(null, null);
    }

    @Test(expected = LoginException.class)
    public void loginFailsWithEmptyUsername() throws BonitaException {
        final LoginAPI loginTenant = TenantAPIAccessor.getLoginAPI();
        loginTenant.login("", null);
    }

    @Cover(classes = LoginAPI.class, concept = BPMNConcept.NONE, keywords = { "Login", "Password" }, story = "Try to login with null password", jira = "ENGINE-622")
    @Test(expected = LoginException.class)
    public void loginFailsWithNullPassword() throws BonitaException {
        final LoginAPI loginTenant = TenantAPIAccessor.getLoginAPI();
        loginTenant.login("matti", null);
    }

    @Cover(classes = LoginAPI.class, concept = BPMNConcept.NONE, keywords = { "Login", "Password" }, story = "Try to login with wrong password", jira = "ENGINE-622")
    @Test(expected = LoginException.class)
    public void loginFailsWithWrongPassword() throws BonitaException {
        final String userName = "Truc";
        createUserOnDefaultTenant(userName, "goodPassword");
        try {
            final LoginAPI loginTenant = TenantAPIAccessor.getLoginAPI();
            loginTenant.login(userName, "WrongPassword");
            fail("Should not be reached");
        } finally {
            final APISession session = loginDefaultTenant();
            final IdentityAPI identityAPI = TenantAPIAccessor.getIdentityAPI(session);
            identityAPI.deleteUser(userName);
        }
    }

    @Cover(classes = LoginAPI.class, concept = BPMNConcept.NONE, keywords = { "Login", "Password" }, story = "Try to login with empty password", jira = "ENGINE-622")
    @Test(expected = LoginException.class)
    public void loginFailsWithEmptyPassword() throws BonitaException {
        final LoginAPI loginTenant = TenantAPIAccessor.getLoginAPI();
        loginTenant.login("matti", "");
    }

    @Test
    public void userLoginDefaultTenant() throws BonitaException, InterruptedException {
        final String userName = "matti";
        final String password = "tervetuloa";
        createUserOnDefaultTenant(userName, password);

        final Date now = new Date();
        Thread.sleep(300);
        final LoginAPI loginAPI = TenantAPIAccessor.getLoginAPI();
        final APISession apiSession = loginAPI.login(userName, password);
        final IdentityAPI identityAPI = TenantAPIAccessor.getIdentityAPI(apiSession);
        final User user = identityAPI.getUserByUserName(userName);
        identityAPI.deleteUser(userName);

        assertEquals(userName, user.getUserName());
        assertNotSame(password, user.getPassword());
        assertTrue(now.before(user.getLastConnection()));
    }

    @Test
    public void loginWithExistingUserAndCheckId() throws BonitaException {
        final APISession session = loginDefaultTenant();
        final IdentityAPI identityAPI = TenantAPIAccessor.getIdentityAPI(session);
        final String userName = "corvinus";
        final String password = "underworld";
        final User user = createUserOnDefaultTenant(userName, password);
        final LoginAPI loginTenant = TenantAPIAccessor.getLoginAPI();
        final APISession login = loginTenant.login(userName, password);
        assertTrue("userId should be valuated", user.getId() != -1);
        assertEquals(user.getId(), login.getUserId());

        identityAPI.deleteUser(user.getId());
        logoutTenant(session);
    }

    @Test
    public void loginWithNonTechnicalUser() throws BonitaException {
        final String username = "install";
        final String pwd = "install";
        final LoginAPI loginTenant = TenantAPIAccessor.getLoginAPI();
        APISession session = loginTenant.login(username, pwd);
        IdentityAPI identityAPI = TenantAPIAccessor.getIdentityAPI(session);
        final User user = createUserOnDefaultTenant("matti", "kieli");
        loginTenant.logout(session);

        session = loginTenant.login("matti", "kieli");
        assertTrue("Should be logged in as a NON-Technical user", !session.isTechnicalUser());
        loginTenant.logout(session);

        session = loginTenant.login(username, pwd);
        identityAPI = TenantAPIAccessor.getIdentityAPI(session);
        identityAPI.deleteUser(user.getId());
        loginTenant.logout(session);
    }

    @Test
    public void loginWithTechnicalUser() throws BonitaException {
        final String username = "install";
        final String pwd = "install";
        final LoginAPI loginTenant = TenantAPIAccessor.getLoginAPI();
        final APISession session = loginTenant.login(username, pwd);
        assertTrue("Should be logged in as Technical user", session.isTechnicalUser());
        loginTenant.logout(session);
    }

    @Cover(jira = "ENGINE-1653", classes = { User.class, LoginAPI.class }, concept = BPMNConcept.NONE, keywords = { "disable user", "login" })
    @Test(expected = LoginException.class)
    public void unableToLoginWhenTheUserIsDisable() throws BonitaException {
        final APISession session = loginDefaultTenant();
        final IdentityAPI identityAPI = TenantAPIAccessor.getIdentityAPI(session);
        final String userName = "matti";
        final String password = "bpm";
        final User user = identityAPI.createUser(userName, password);
        final UserUpdater updater = new UserUpdater();
        updater.setEnabled(false);
        identityAPI.updateUser(user.getId(), updater);
        final LoginAPI loginTenant = TenantAPIAccessor.getLoginAPI();
        try {
            loginTenant.login(userName, password);
            fail("It is not possible to login when the user is disable.");
        } finally {
            identityAPI.deleteUser(user.getId());
            logoutTenant(session);
        }
    }

}
