package org.bonitasoft.engine.exception;

public interface BonitaContextException {

    long getTenantId();

    String getHostname();

    String getUserName();

}
