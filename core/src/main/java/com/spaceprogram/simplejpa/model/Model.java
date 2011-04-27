package com.spaceprogram.simplejpa.model;

import java.io.Serializable;

/**
 * NOTE: Just toying with this idea, you probably shouldn't use it... yet.
 *
 * Adds functionality to an object to allow it to persist itself. If you'd like to use this functionality,
 * just extend this class. You can use a common base class in your own model that extends this class
 * to make this easy to swap out (which you might have already for IDs and dates anyways).
 *
 *
 * User: treeder
 * Date: Oct 18, 2008
 * Time: 4:28:14 PM
 */
public class Model implements Serializable {

    public void persist(){
    }

    public static ModelQuery query() throws ClassNotFoundException {
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        String entityClass = null;
        for (int i = 0; i < Thread.currentThread().getStackTrace().length; i++) {
            StackTraceElement element = stackTraceElements[i];
            System.out.println("class=" + element.getClassName());
            if (element.getClassName().endsWith("Model")) {
                entityClass = stackTraceElements[i+1].getClassName();
                break;
            }
        }
//        StackTraceElement element = Thread.currentThread().getStackTrace()[1];
//        System.out.println("class=" + " - " + element.getClassName());
        System.out.println("entityclass=" + entityClass);
        return query(Class.forName(entityClass));
    }

    public static ModelQuery query(Class c) throws ClassNotFoundException {
        return new ModelQueryImpl(c);
    }
}
