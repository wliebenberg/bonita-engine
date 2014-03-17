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
package org.bonitasoft.engine.api.impl;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.bonitasoft.engine.api.NoSessionRequired;
import org.bonitasoft.engine.api.PlatformAPI;
import org.bonitasoft.engine.api.impl.transaction.CustomTransactions;
import org.bonitasoft.engine.api.internal.ServerAPI;
import org.bonitasoft.engine.api.internal.ServerWrappedException;
import org.bonitasoft.engine.classloader.ClassLoaderException;
import org.bonitasoft.engine.classloader.ClassLoaderService;
import org.bonitasoft.engine.commons.ClassReflector;
import org.bonitasoft.engine.commons.exceptions.SBonitaException;
import org.bonitasoft.engine.core.login.LoginService;
import org.bonitasoft.engine.core.platform.login.PlatformLoginService;
import org.bonitasoft.engine.dependency.model.ScopeType;
import org.bonitasoft.engine.exception.BonitaException;
import org.bonitasoft.engine.exception.BonitaHomeConfigurationException;
import org.bonitasoft.engine.exception.BonitaHomeNotSetException;
import org.bonitasoft.engine.exception.BonitaRuntimeException;
import org.bonitasoft.engine.log.technical.TechnicalLogSeverity;
import org.bonitasoft.engine.log.technical.TechnicalLoggerService;
import org.bonitasoft.engine.platform.NodeNotStartedException;
import org.bonitasoft.engine.platform.PlatformService;
import org.bonitasoft.engine.platform.session.PlatformSessionService;
import org.bonitasoft.engine.scheduler.SchedulerService;
import org.bonitasoft.engine.scheduler.exception.SSchedulerException;
import org.bonitasoft.engine.service.APIAccessResolver;
import org.bonitasoft.engine.service.PlatformServiceAccessor;
import org.bonitasoft.engine.service.TenantServiceAccessor;
import org.bonitasoft.engine.service.impl.ServiceAccessorFactory;
import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.engine.session.InvalidSessionException;
import org.bonitasoft.engine.session.PlatformSession;
import org.bonitasoft.engine.session.Session;
import org.bonitasoft.engine.session.SessionService;
import org.bonitasoft.engine.sessionaccessor.SessionAccessor;
import org.bonitasoft.engine.transaction.UserTransactionService;

/**
 * @author Matthieu Chaffotte
 * @author Baptiste Mesta
 * @author Celine Souchet
 */
public class ServerAPIImpl implements ServerAPI {

    private static final String IS_NODE_STARTED_METHOD_NAME = "isNodeStarted";

    private static final long serialVersionUID = -161775388604256321L;

    protected final APIAccessResolver accessResolver;

    private final boolean cleanSession;

    private TechnicalLoggerService technicalLogger;

    private enum SessionType {
        PLATFORM, API;
    }

    public ServerAPIImpl() {
        this(true);
    }

    public ServerAPIImpl(final boolean cleanSession, final APIAccessResolver accessResolver) {
        this.cleanSession = cleanSession;
        this.accessResolver = accessResolver;
    }

    public ServerAPIImpl(final boolean cleanSession) {
        try {
            this.cleanSession = cleanSession;
            accessResolver = getServiceAccessorFactoryInstance().createAPIAccessResolver();
        } catch (final Exception e) {
            throw new BonitaRuntimeException(e);
        }
    }

    void setTechnicalLogger(final TechnicalLoggerService technicalLoggerService) {
        technicalLogger = technicalLoggerService;
    }

    @Override
    public Object invokeMethod(final Map<String, Serializable> options, final String apiInterfaceName, final String methodName,
            final List<String> classNameParameters, final Object[] parametersValues) throws ServerWrappedException {
        final ClassLoader baseClassLoader = Thread.currentThread().getContextClassLoader();
        SessionAccessor sessionAccessor = null;
        try {
            try {
                sessionAccessor = beforeInvokeMethod(options, apiInterfaceName);
                final Session session = (Session) options.get("session");
                return invokeAPI(apiInterfaceName, methodName, classNameParameters, parametersValues, session);
            } catch (final ServerAPIRuntimeException sapire) {
                throw sapire.getCause();
            }
        } catch (final BonitaRuntimeException bre) {
            throw new ServerWrappedException(bre);
        } catch (final BonitaException be) {
            throw new ServerWrappedException(be);
        } catch (final UndeclaredThrowableException ute) {
            if (technicalLogger != null && technicalLogger.isLoggable(this.getClass(), TechnicalLogSeverity.DEBUG)) {
                technicalLogger.log(this.getClass(), TechnicalLogSeverity.DEBUG, ute);
            }
            throw new ServerWrappedException(ute);
        } catch (final Throwable cause) {
            if (technicalLogger != null && technicalLogger.isLoggable(this.getClass(), TechnicalLogSeverity.DEBUG)) {
                technicalLogger.log(this.getClass(), TechnicalLogSeverity.DEBUG, cause);
            }
            throw new ServerWrappedException(new BonitaRuntimeException(cause));
        } finally {
            if (cleanSession) {
                // clean session id
                if (sessionAccessor != null) {
                    sessionAccessor.deleteSessionId();
                }
            }
            // reset class loader
            Thread.currentThread().setContextClassLoader(baseClassLoader);
        }
    }

    private static final class ServerAPIRuntimeException extends RuntimeException {

        private static final long serialVersionUID = -5675131531953146131L;

        ServerAPIRuntimeException(final Throwable cause) {
            super(cause);

        }
    }

    private SessionAccessor beforeInvokeMethod(final Map<String, Serializable> options, final String apiInterfaceName) throws BonitaHomeNotSetException,
            InstantiationException, IllegalAccessException, ClassNotFoundException, BonitaHomeConfigurationException, IOException, NoSuchMethodException,
            InvocationTargetException, SBonitaException {
        SessionAccessor sessionAccessor = null;

        final ServiceAccessorFactory serviceAccessorFactory = getServiceAccessorFactoryInstance();
        final PlatformServiceAccessor platformServiceAccessor = serviceAccessorFactory.createPlatformServiceAccessor();

        ClassLoader serverClassLoader = null;
        final Session session = (Session) options.get("session");
        if (session != null) {
            final SessionType sessionType = getSessionType(session);
            sessionAccessor = serviceAccessorFactory.createSessionAccessor();
            switch (sessionType) {
                case PLATFORM:
                    final PlatformSessionService platformSessionService = platformServiceAccessor.getPlatformSessionService();
                    final PlatformLoginService loginService = platformServiceAccessor.getPlatformLoginService();

                    if (!loginService.isValid(session.getId())) {
                        throw new InvalidSessionException("Invalid session");
                    }
                    platformSessionService.renewSession(session.getId());
                    sessionAccessor.setSessionInfo(session.getId(), -1);
                    serverClassLoader = getPlatformClassLoader(platformServiceAccessor);
                    setTechnicalLogger(platformServiceAccessor.getTechnicalLoggerService());
                    break;

                case API:
                    final SessionService sessionService = platformServiceAccessor.getSessionService();

                    checkTenantSession(platformServiceAccessor, session);
                    sessionService.renewSession(session.getId());
                    sessionAccessor.setSessionInfo(session.getId(), ((APISession) session).getTenantId());
                    serverClassLoader = getTenantClassLoader(platformServiceAccessor, session);
                    setTechnicalLogger(serviceAccessorFactory.createTenantServiceAccessor(((APISession) session).getTenantId()).getTechnicalLoggerService());
                    break;

                default:
                    throw new InvalidSessionException("Unknown session type: " + session.getClass().getName());
            }
        } else if (accessResolver.needSession(apiInterfaceName)) {
            throw new InvalidSessionException("Session is null!");
        }
        if (serverClassLoader != null) {
            Thread.currentThread().setContextClassLoader(serverClassLoader);
        }

        return sessionAccessor;
    }

    private ServiceAccessorFactory getServiceAccessorFactoryInstance() {
        return ServiceAccessorFactory.getInstance();
    }

    private SessionType getSessionType(final Session session) {
        SessionType sessionType = null;
        if (session instanceof PlatformSession) {
            sessionType = SessionType.PLATFORM;
        } else if (session instanceof APISession) {
            sessionType = SessionType.API;
        }
        return sessionType;
    }

    protected Object invokeAPI(final String apiInterfaceName, final String methodName, final List<String> classNameParameters, final Object[] parametersValues,
            final Session session) throws Throwable {
        final Class<?>[] parameterTypes = getParameterTypes(classNameParameters);

        final Object apiImpl = accessResolver.getAPIImplementation(apiInterfaceName);
        final Method method = ClassReflector.getMethod(apiImpl.getClass(), methodName, parameterTypes);
        checkMethodAccessibility(apiImpl, apiInterfaceName, method, session);
        // No session required means that there is no transaction
        if (method.isAnnotationPresent(CustomTransactions.class) || method.isAnnotationPresent(NoSessionRequired.class)) {
            return invokeAPI(parametersValues, apiImpl, method);
        } else {
            return invokeAPIInTransaction(parametersValues, apiImpl, method, session);
        }
    }

    protected void checkMethodAccessibility(final Object apiImpl, final String apiInterfaceName, final Method method, final Session session) {
        if (!isNodeInAValidStateFor(method)) {
            logNodeNotStartedMessage(apiInterfaceName, method.getName());
            throw new NodeNotStartedException();
        }
    }

    protected void logNodeNotStartedMessage(final String apiInterfaceName, final String methodName) {
        logTechnicalErrorMessage("Node not started. Method '" + apiInterfaceName + "." + methodName
                + "' cannot be called until node has been started (PlatformAPI.startNode())");
    }

    protected void logTechnicalErrorMessage(final String message) {
        if (technicalLogger != null) {
            technicalLogger.log(this.getClass(), TechnicalLogSeverity.ERROR, message);
        } else {
            System.err.println(message);
        }
    }

    protected boolean isNodeInAValidStateFor(final Method method) {
        return method.isAnnotationPresent(AvailableOnStoppedNode.class) || isNodeStarted();
    }

    /**
     * @return true if the node is started, false otherwise.
     */
    private boolean isNodeStarted() {
        try {
            final Object apiImpl = accessResolver.getAPIImplementation(PlatformAPI.class.getName());
            final Method method = ClassReflector.getMethod(apiImpl.getClass(), IS_NODE_STARTED_METHOD_NAME, new Class[0]);
            return (Boolean) invokeAPI(new Object[0], apiImpl, method);
        } catch (Throwable e) {
            return false;
        }
    }

    protected Object invokeAPIInTransaction(final Object[] parametersValues, final Object apiImpl, final Method method, final Session session) throws Throwable {
        if (session == null) {
            throw new BonitaRuntimeException("session is null");
        }
        final UserTransactionService userTransactionService = selectUserTransactionService(session, getSessionType(session));

        final Callable<Object> callable = new Callable<Object>() {

            @Override
            public Object call() throws Exception {
                try {
                    return invokeAPI(parametersValues, apiImpl, method);
                } catch (final Throwable cause) {
                    throw new ServerAPIRuntimeException(cause);
                }
            }
        };

        return userTransactionService.executeInTransaction(callable);
    }

    protected UserTransactionService selectUserTransactionService(final Session session, final SessionType sessionType) throws BonitaHomeNotSetException,
            InstantiationException, IllegalAccessException, ClassNotFoundException, IOException, BonitaHomeConfigurationException {
        UserTransactionService transactionService = null;
        final ServiceAccessorFactory serviceAccessorFactory = getServiceAccessorFactoryInstance();
        final PlatformServiceAccessor platformServiceAccessor = serviceAccessorFactory.createPlatformServiceAccessor();
        switch (sessionType) {
            case PLATFORM:
                transactionService = platformServiceAccessor.getTransactionService();
                break;
            case API:
                final TenantServiceAccessor tenantAccessor = platformServiceAccessor.getTenantServiceAccessor(((APISession) session).getTenantId());
                transactionService = tenantAccessor.getUserTransactionService();
                break;
            default:
                throw new InvalidSessionException("Unknown session type: " + session.getClass().getName());
        }
        return transactionService;
    }

    protected Object invokeAPI(final Object[] parametersValues, final Object apiImpl, final Method method) throws Throwable {
        try {
            return method.invoke(apiImpl, parametersValues);
        } catch (final InvocationTargetException e) {
            throw e.getCause();
        }
    }

    protected Class<?>[] getParameterTypes(final List<String> classNameParameters) throws ClassNotFoundException {
        Class<?>[] parameterTypes = null;
        if (classNameParameters != null && !classNameParameters.isEmpty()) {
            parameterTypes = new Class<?>[classNameParameters.size()];
            for (int i = 0; i < parameterTypes.length; i++) {
                final String className = classNameParameters.get(i);
                Class<?> classType = null;
                if ("int".equals(className)) {
                    classType = int.class;
                } else if ("long".equals(className)) {
                    classType = long.class;
                } else if ("boolean".equals(className)) {
                    classType = boolean.class;
                } else {
                    classType = Class.forName(className);
                }
                parameterTypes[i] = classType;
            }
        }
        return parameterTypes;
    }

    private void checkTenantSession(final PlatformServiceAccessor platformAccessor, final Session session) throws SSchedulerException {
        final SchedulerService schedulerService = platformAccessor.getSchedulerService();
        final TechnicalLoggerService logger = platformAccessor.getTechnicalLoggerService();
        if (!schedulerService.isStarted() && logger.isLoggable(this.getClass(), TechnicalLogSeverity.DEBUG)) {
            logger.log(this.getClass(), TechnicalLogSeverity.DEBUG, "The scheduler is not started!");
        }
        final APISession apiSession = (APISession) session;
        final TenantServiceAccessor tenantAccessor = platformAccessor.getTenantServiceAccessor(apiSession.getTenantId());
        final LoginService tenantLoginService = tenantAccessor.getLoginService();
        if (!tenantLoginService.isValid(apiSession.getId())) {
            throw new InvalidSessionException("Invalid session");
        }
    }

    private ClassLoader getTenantClassLoader(final PlatformServiceAccessor platformServiceAccessor, final Session session) throws ClassLoaderException {
        final APISession apiSession = (APISession) session;
        final TenantServiceAccessor tenantAccessor = platformServiceAccessor.getTenantServiceAccessor(apiSession.getTenantId());
        final ClassLoaderService classLoaderService = tenantAccessor.getClassLoaderService();
        return classLoaderService.getLocalClassLoader(ScopeType.TENANT.name(), apiSession.getTenantId());
    }

    private ClassLoader getPlatformClassLoader(final PlatformServiceAccessor platformServiceAccessor) throws ClassLoaderException {
        ClassLoader classLoader = null;
        final PlatformService platformService = platformServiceAccessor.getPlatformService();
        // get the platform to put it in cache if needed
        if (!platformService.isPlatformCreated()) {
            try {
                platformServiceAccessor.getTransactionService().executeInTransaction(new Callable<Void>() {

                    @Override
                    public Void call() throws Exception {
                        platformService.getPlatform();
                        return null;
                    }

                });
            } catch (Exception e) {
                // do not throw exceptions: it's just in case the platform was not in cache
            }
        }
        if (platformService.isPlatformCreated()) {
            final ClassLoaderService classLoaderService = platformServiceAccessor.getClassLoaderService();
            classLoader = classLoaderService.getGlobalClassLoader();
        }
        return classLoader;
    }

}
