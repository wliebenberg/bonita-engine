package org.bonitasoft.engine.test.persistence.builder;

import java.util.Random;

import org.bonitasoft.engine.persistence.PersistentObject;


public abstract class PersistentObjectBuilder<T extends PersistentObject> {
    static final long DEFAULT_TENANT_ID = 1L;
    
    protected long id = new Random().nextLong();
    
    public T build() {
        T persistentObject = _build();
        persistentObject.setId(id);
        persistentObject.setTenantId(DEFAULT_TENANT_ID);
        return persistentObject;
    }
    
    abstract T _build();
    
}
