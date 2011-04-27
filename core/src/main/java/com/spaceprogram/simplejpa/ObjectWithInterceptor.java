package com.spaceprogram.simplejpa;

/**
 * Just a holder to keep the object and cglib interceptor together.
 *
 * User: treeder
 * Date: Feb 16, 2008
 * Time: 2:58:52 PM
 */
public class ObjectWithInterceptor {
    private Object bean;
    private LazyInterceptor interceptor;

    public ObjectWithInterceptor(Object bean, LazyInterceptor interceptor) {
        this.bean = bean;
        this.interceptor = interceptor;
    }

    public Object getBean() {
        return bean;
    }

    public LazyInterceptor getInterceptor() {
        return interceptor;
    }
}
