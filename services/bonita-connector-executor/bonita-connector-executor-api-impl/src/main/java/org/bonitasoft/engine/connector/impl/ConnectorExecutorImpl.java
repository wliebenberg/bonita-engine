/**
 * Copyright (C) 2011-2013 BonitaSoft S.A.
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
package org.bonitasoft.engine.connector.impl;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.bonitasoft.engine.commons.exceptions.SBonitaException;
import org.bonitasoft.engine.connector.ConnectorExecutor;
import org.bonitasoft.engine.connector.SConnector;
import org.bonitasoft.engine.connector.exception.SConnectorException;
import org.bonitasoft.engine.connector.exception.SConnectorValidationException;
import org.bonitasoft.engine.log.technical.TechnicalLogSeverity;
import org.bonitasoft.engine.log.technical.TechnicalLoggerService;
import org.bonitasoft.engine.session.SessionService;
import org.bonitasoft.engine.sessionaccessor.SessionAccessor;
import org.bonitasoft.engine.sessionaccessor.SessionIdNotSetException;

/**
 * Execute connectors directly
 * 
 * @author Baptiste Mesta
 */
public class ConnectorExecutorImpl implements ConnectorExecutor {

    private ThreadPoolExecutor threadPoolExecutor;

    private final SessionAccessor sessionAccessor;

    private final SessionService sessionService;

    private final int queueCapacity;

    private final int corePoolSize;

    private final int maximumPoolSize;

    private final long keepAliveTimeSeconds;

    private final TechnicalLoggerService loggerService;

    /**
     * The handling of threads relies on the JVM
     * The rules to create new thread are:
     * - If the number of threads is less than the corePoolSize, create a new Thread to run a new task.
     * - If the number of threads is equal (or greater than) the corePoolSize, put the task into the queue.
     * - If the queue is full, and the number of threads is less than the maxPoolSize, create a new thread to run tasks in.
     * - If the queue is full, and the number of threads is greater than or equal to maxPoolSize, reject the task.
     * 
     * @param queueCapacity
     *            The maximum number of execution of connector to queue for each thread
     * @param corePoolSize
     *            the number of threads to keep in the pool, even
     *            if they are idle, unless {@code allowCoreThreadTimeOut} is set
     * @param loggerService
     * @param timeout
     *            if the execution of the connector is above this time in milliseconds the execution will fail
     * @param maximumPoolSize
     *            the maximum number of threads to allow in the
     *            pool
     * @param keepAliveTime
     *            when the number of threads is greater than
     *            the core, this is the maximum time that excess idle threads
     *            will wait for new tasks before terminating. (in seconds)
     */
    public ConnectorExecutorImpl(final int queueCapacity, final int corePoolSize, final TechnicalLoggerService loggerService, final int maximumPoolSize,
            final long keepAliveTimeSeconds, final SessionAccessor sessionAccessor, final SessionService sessionService) {

        this.queueCapacity = queueCapacity;
        this.corePoolSize = corePoolSize;
        this.loggerService = loggerService;
        this.maximumPoolSize = maximumPoolSize;
        this.keepAliveTimeSeconds = keepAliveTimeSeconds;
        this.sessionAccessor = sessionAccessor;
        this.sessionService = sessionService;
    }

    @Override
    public Map<String, Object> execute(final SConnector sConnector, final Map<String, Object> inputParameters) throws SConnectorException {
        if (threadPoolExecutor == null) {
            throw new SConnectorException("Unable to execute a connector if the node is node started. Start it first");
        }
        final Callable<Map<String, Object>> callable = new ExecuteConnectorCallable(inputParameters, sConnector);
        final Future<Map<String, Object>> submit = threadPoolExecutor.submit(callable);
        try {
            return getValue(submit);
        } catch (final InterruptedException e) {
            disconnect(sConnector);
            throw new SConnectorException(e);
        } catch (final ExecutionException e) {
            disconnect(sConnector);
            throw new SConnectorException(e);
        } catch (final TimeoutException e) {
            submit.cancel(true);
            disconnect(sConnector);
            throw new SConnectorException("The connector timed out " + sConnector);
        }
    }

    protected Map<String, Object> getValue(final Future<Map<String, Object>> submit) throws InterruptedException, ExecutionException, TimeoutException {
        return submit.get();
    }

    @Override
    public void disconnect(final SConnector sConnector) throws SConnectorException {
        try {
            sConnector.disconnect();
        } catch (SConnectorException e) {
            throw e;
        } catch (Throwable t) {
            throw new SConnectorException(t);
        }
    }

    /**
     * @author Baptiste Mesta
     */
    private final class ExecuteConnectorCallable implements Callable<Map<String, Object>> {

        private final Map<String, Object> inputParameters;

        private final SConnector sConnector;

        private ExecuteConnectorCallable(final Map<String, Object> inputParameters, final SConnector sConnector) {
            this.inputParameters = inputParameters;
            this.sConnector = sConnector;
        }

        @Override
        public Map<String, Object> call() throws Exception {
            sConnector.setInputParameters(inputParameters);
            try {
                sConnector.validate();
                sConnector.connect();
                return sConnector.execute();
            } catch (final SConnectorValidationException e) {
                throw new SConnectorException(e);
            } finally {
                // in case a session has been created: see ConnectorAPIAccessorImpl
                try {
                    final long sessionId = sessionAccessor.getSessionId();
                    sessionAccessor.deleteSessionId();
                    sessionService.deleteSession(sessionId);
                } catch (SessionIdNotSetException e) {
                    // nothing, no session has been created
                }
            }
        }
    }

    private final class QueueRejectedExecutionHandler implements RejectedExecutionHandler {

        private final TechnicalLoggerService logger;

        public QueueRejectedExecutionHandler(final TechnicalLoggerService logger) {
            this.logger = logger;
        }

        @Override
        public void rejectedExecution(final Runnable task, final ThreadPoolExecutor executor) {
            if (logger.isLoggable(getClass(), TechnicalLogSeverity.WARNING)) {
                logger.log(ThreadPoolExecutor.class, TechnicalLogSeverity.WARNING, "The work was rejected, requeue work: " + task.toString());
            }
            try {
                executor.getQueue().put(task);
            } catch (final InterruptedException e) {
                throw new RejectedExecutionException("queuing " + task + " got interrupted", e);
            }
        }

    }

    @Override
    public void start() throws SBonitaException {
        final BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<Runnable>(queueCapacity);
        final RejectedExecutionHandler handler = new QueueRejectedExecutionHandler(loggerService);
        final ConnectorExecutorThreadFactory threadFactory = new ConnectorExecutorThreadFactory("ConnectorExecutor");
        threadPoolExecutor = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTimeSeconds, TimeUnit.SECONDS, workQueue, threadFactory, handler);
    }

    @Override
    public void stop() {
        if (threadPoolExecutor != null) {
            threadPoolExecutor.shutdown();
            try {
                if (!threadPoolExecutor.awaitTermination(5000, TimeUnit.MILLISECONDS)) {
                    loggerService.log(getClass(), TechnicalLogSeverity.WARNING, "Timeout  (5s) trying to stop the connector executor thread pool");
                }
            } catch (InterruptedException e) {
                loggerService.log(getClass(), TechnicalLogSeverity.WARNING, "Error while stopping the connector executor thread pool", e);
            }
        }
    }

    @Override
    public void pause() throws SBonitaException, TimeoutException {
        // nothing to do
    }

    @Override
    public void resume() throws SBonitaException {
        // nothing to do
    }
}
