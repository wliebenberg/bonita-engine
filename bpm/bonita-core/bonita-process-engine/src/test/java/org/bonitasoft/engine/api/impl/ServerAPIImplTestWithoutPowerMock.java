package org.bonitasoft.engine.api.impl;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bonitasoft.engine.api.internal.ServerWrappedException;
import org.bonitasoft.engine.exception.BonitaException;
import org.bonitasoft.engine.exception.BonitaHomeNotSetException;
import org.bonitasoft.engine.service.APIAccessResolver;
import org.bonitasoft.engine.session.Session;
import org.bonitasoft.engine.sessionaccessor.SessionAccessor;
import org.junit.Test;

public class ServerAPIImplTestWithoutPowerMock {


    @Test
    public void should_BonitaException_caught_by_invokeMethod_on_beforeInvokeMethod_set_context_information() throws Exception {

        APIAccessResolver accessResolver = mock(APIAccessResolver.class);
        ServerAPIImpl serverAPIImpl = spy(new ServerAPIImpl(true, accessResolver));

        final BonitaHomeNotSetException bonitaException = mock(BonitaHomeNotSetException.class);
        doThrow(bonitaException).when(serverAPIImpl).beforeInvokeMethod(any(Session.class), anyString());


        Map<String, Serializable> options = new HashMap<String, Serializable>();
        String apiInterfaceName = null;
        String methodName = null;
        List<String> classNameParameters = null;
        Object[] parametersValues = null;
        try {
            serverAPIImpl.invokeMethod(options, apiInterfaceName, methodName, classNameParameters, parametersValues);
            fail("An exception should have been thrown.");
        } catch (ServerWrappedException e) {
//            BonitaException cause = (BonitaException) e.getCause();
            verify(bonitaException).setHostname(anyString());
        }
    }

    @Test
    public void should_BonitaException_caught_by_invokeMethod_on_invokeAPI_set_context_information() throws Throwable {

        APIAccessResolver accessResolver = mock(APIAccessResolver.class);
        ServerAPIImpl serverAPIImpl = spy(new ServerAPIImpl(true, accessResolver));

        doReturn(mock(SessionAccessor.class)).when(serverAPIImpl).beforeInvokeMethod(any(Session.class), anyString());

        Map<String, Serializable> options = new HashMap<String, Serializable>();
        final Session session = mock(Session.class);
        doReturn("userName").when(session).getUserName();
        options.put("session", session);
        BonitaException bonitaException = mock(BonitaException.class);

        doThrow(bonitaException).when(serverAPIImpl).invokeAPI(anyString(), anyString(), anyListOf(String.class), any(Class[].class), eq(session));

        String apiInterfaceName = null;
        String methodName = null;
        List<String> classNameParameters = null;
        Object[] parametersValues = null;
        try {
            serverAPIImpl.invokeMethod(options, apiInterfaceName, methodName, classNameParameters, parametersValues);
            fail("An exception should have been thrown.");
        } catch (ServerWrappedException e) {
//            BonitaException cause = (BonitaException) e.getCause();
            verify(bonitaException).setHostname(anyString());
            verify(bonitaException).setTenantId(anyLong());
            verify(bonitaException).setUserName(eq("userName"));
        }
    }

}
