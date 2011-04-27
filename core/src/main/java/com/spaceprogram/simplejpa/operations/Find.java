package com.spaceprogram.simplejpa.operations;


import com.spaceprogram.simplejpa.EntityManagerSimpleJPA;

import java.util.concurrent.Callable;

/**
 * User: treeder
 * Date: Dec 12, 2008
 * Time: 10:59:08 AM
 */
public class Find<T> implements Callable<T> {
    private EntityManagerSimpleJPA em;
    private Class<T> c;
    private Object id;

     public Find(EntityManagerSimpleJPA em, Class<T> c, Object id) {
        this.em = em;
        this.c = c;
        this.id = id;
    }

    public T call() throws Exception {
        return em.find(c, id);
    }
}
