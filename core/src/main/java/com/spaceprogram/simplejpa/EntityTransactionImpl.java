package com.spaceprogram.simplejpa;

import javax.persistence.EntityTransaction;

/**
 * Doesn't do anything, not sure if it ever will, but need it for JPA spec.
 *
 * User: treeder
 * Date: Feb 18, 2008
 * Time: 3:40:06 PM
 */
public class EntityTransactionImpl implements EntityTransaction {
    public void begin() {

    }

    public void commit() {

    }

    public void rollback() {

    }

    public void setRollbackOnly() {

    }

    public boolean getRollbackOnly() {
        return false;
    }

    public boolean isActive() {
        return false;
    }
}
