package org.bonitasoft.engine.recorder.impl;

import org.bonitasoft.engine.persistence.PersistentObject;

public class Author implements PersistentObject {

    private static final long serialVersionUID = -1926883906396447119L;

    private String name;

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    @Override
    public long getId() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getDiscriminator() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setId(final long id) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setTenantId(final long id) {
        // TODO Auto-generated method stub

    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (name == null ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Author other = (Author) obj;
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        return true;
    }

}
