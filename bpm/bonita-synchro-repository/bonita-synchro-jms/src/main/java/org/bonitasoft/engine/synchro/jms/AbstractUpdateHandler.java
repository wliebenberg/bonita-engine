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
package org.bonitasoft.engine.synchro.jms;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.bonitasoft.engine.events.model.SEvent;
import org.bonitasoft.engine.events.model.SHandler;
import org.bonitasoft.engine.events.model.SHandlerExecutionException;
import org.bonitasoft.engine.service.TenantServiceAccessor;
import org.bonitasoft.engine.service.impl.ServiceAccessorFactory;
import org.bonitasoft.engine.transaction.BonitaTransactionSynchronization;
import org.bonitasoft.engine.transaction.STransactionNotFoundException;
import org.bonitasoft.engine.transaction.UserTransactionService;

/**
 * @author Baptiste Mesta
 */
public abstract class AbstractUpdateHandler implements SHandler<SEvent> {

    private static final long serialVersionUID = 1L;

    private final long tenantId;

    private final Long messageTimeout;

    private final Map<Class<?>, Method> getIdMethods = Collections.synchronizedMap(new HashMap<Class<?>, Method>());

    public AbstractUpdateHandler(final long tenantId, final long messageTimeout) {
        super();
        this.tenantId = tenantId;
        this.messageTimeout = messageTimeout;
    }

    protected abstract Map<String, Serializable> getEvent(final SEvent sEvent);

    @Override
    public void execute(final SEvent sEvent) throws SHandlerExecutionException {
        // System.err.println(Thread.currentThread().getName() + " executing event : " + sEvent);
        try {
            final Map<String, Serializable> event = getEvent(sEvent);
            final Long id = getObjectId(sEvent);

            final TenantServiceAccessor tenantServiceAccessor = getTenantServiceAccessor();
            final BonitaTransactionSynchronization synchronization = new SendJMSMessageSynchronization(event, id, JMSProducer.getInstance(messageTimeout));

            UserTransactionService userTransactionService = tenantServiceAccessor.getUserTransactionService();
            userTransactionService.registerBonitaSynchronization(synchronization);
            // System.out.println("++++++++++++++++++++ " + this.getClass().getSimpleName() + " executing for event " + event + " +++++++++++++++++++++");
        } catch (final STransactionNotFoundException e) {
            e.printStackTrace();
            throw new SHandlerExecutionException(e);
        }
    }

    /**
     * @param sEvent
     * @return
     */
    private Long getObjectId(final SEvent sEvent) {
        Long id = null;
        Object object = null;
        try {
            object = sEvent.getObject();
            final Class<?> clazz = object.getClass();
            Method method = null;
            if (getIdMethods.containsKey(clazz)) {
                method = getIdMethods.get(clazz);
            } else {
                method = clazz.getMethod("getId");
            }
            final Object invoke = method.invoke(object);
            id = (Long) invoke;
        } catch (final Throwable e) {
            System.err.println("AbstractUpdateHandler: No id on object " + object);
        }
        return id;
    }

    private TenantServiceAccessor getTenantServiceAccessor() throws SHandlerExecutionException {
        try {
            return ServiceAccessorFactory.getInstance().createTenantServiceAccessor(tenantId);
        } catch (Exception e) {
            throw new SHandlerExecutionException(e.getMessage(), null);
        }
    }

}
