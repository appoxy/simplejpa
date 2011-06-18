package com.spaceprogram.simplejpa;

import org.apache.commons.lang.StringUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;

/**
 * PersistentProperty that supports access to data through getter/setter pair. Used to support persistence annotations
 * on methods
 *
 * Kerry Wright
 */
public class PersistentMethod extends PersistentProperty {
    private final Method getter;
    private final Method setter;

    public PersistentMethod(Method method) {
        super(method);
        this.getter = method;
        this.getter.setAccessible(true);
        String setterName = getter.getName().replaceFirst("get", "set");
        try {
            this.setter = method.getDeclaringClass().getDeclaredMethod(setterName, getter.getReturnType());
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("No setter found for method provided: "+getter.getName());
        }
        this.setter.setAccessible(true);
    }

    public Method getGetter() {
        return getter;
    }

    public Method getSetter() {
        return setter;
    }

    public String getFieldName() {
        return StringUtils.uncapitalize(getGetter().getName().replaceFirst("get", ""));
    }

}
