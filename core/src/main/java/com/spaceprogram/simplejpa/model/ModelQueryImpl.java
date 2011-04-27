package com.spaceprogram.simplejpa.model;

import java.util.List;

/**
 * User: treeder
 * Date: Oct 18, 2008
 * Time: 4:39:13 PM
 */
public class ModelQueryImpl implements ModelQuery {
    private Class<?> aClass;

    public ModelQueryImpl(Class<?> aClass) {

        this.aClass = aClass;
    }

    public ModelQuery filter(String attribute, String comparison, String value) {
        return null;
    }

    public ModelQuery order(String attribute, String direction) {
        return null;
    }

    public List getResultList() {
        return null;
    }
}
