package org.bonitasoft.engine.work;

import static org.fest.assertions.Assertions.assertThat;

import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

import org.bonitasoft.engine.commons.Pair;
import org.junit.Test;

public class DefaultBonitaExecutorServiceFactoryTest {

    @Test
    public void ThreadNameInExecutorServiceShouldContainsTenantId() throws Exception {
        long tenantId = 999;
        DefaultBonitaExecutorServiceFactory defaultBonitaExecutorServiceFactory = new DefaultBonitaExecutorServiceFactory(tenantId, 1,
                20, 15, 10);

        Pair<ExecutorService, Queue<Runnable>> createExecutorService = defaultBonitaExecutorServiceFactory.createExecutorService();
        Runnable r = new Runnable() {

            @Override
            public void run() {
            }
        };

        String name = ((ThreadPoolExecutor) createExecutorService.getKey()).getThreadFactory().newThread(r).getName();
        assertThat(name).as("thread name should contains the tenantId").contains(Long.toString(tenantId));
    }
}
