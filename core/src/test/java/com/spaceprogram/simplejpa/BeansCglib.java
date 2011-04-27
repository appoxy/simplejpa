package com.spaceprogram.simplejpa;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.LazyLoader;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * User: treeder
 * Date: Feb 10, 2008
 * Time: 7:51:49 PM
 */
public class BeansCglib implements MethodInterceptor {

    private PropertyChangeSupport propertySupport;

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertySupport.addPropertyChangeListener(listener);

    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertySupport.removePropertyChangeListener(listener);
    }

    public static Object newInstance(Class clazz) {
        try {
            BeansCglib interceptor = new BeansCglib();
            Enhancer e = new Enhancer();
            e.setSuperclass(clazz);
            e.setCallback(interceptor);
            Object bean = e.create();
            interceptor.propertySupport = new PropertyChangeSupport(bean);
            return bean;
        } catch (Throwable e) {
            e.printStackTrace();
            throw new Error(e.getMessage());
        }
    }

    static final Class C[] = new Class[0];
    static final Object emptyArgs[] = new Object[0];


    public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
        Object retValFromSuper = null;
        try {
            if (!Modifier.isAbstract(method.getModifiers())) {
                retValFromSuper = proxy.invokeSuper(obj, args);
            }
        } finally {
            String name = method.getName();
            if (name.equals("addPropertyChangeListener")) {
                addPropertyChangeListener((PropertyChangeListener) args[0]);
            } else if (name.equals("removePropertyChangeListener")) {
                removePropertyChangeListener((PropertyChangeListener) args[0]);
            }
            if (name.startsWith("set") &&
                    args.length == 1 &&
                    method.getReturnType() == Void.TYPE) {

                char propName[] = name.substring("set".length()).toCharArray();

                propName[0] = Character.toLowerCase(propName[0]);
                propertySupport.firePropertyChange(new String(propName), null, args[0]);

            }
        }
        return retValFromSuper;
    }

    public static void main(String args[]) {

        Bean bean = (Bean) newInstance(Bean.class);
        System.out.println("setting lazy bean2");
        bean.setBean2((Bean2) newLazyLoadingInstance(Bean2.class));
        System.out.println("adding prop listener");
        bean.addPropertyChangeListener(
                new PropertyChangeListener() {
                    public void propertyChange(PropertyChangeEvent evt) {
                        System.out.println(evt);
                    }
                }
        );

        bean.setSampleProperty("TEST");
        System.out.println("getting bean2");
        Bean2 bean2 = bean.getBean2();
        System.out.println(bean2.getName());

    }

    private static Object newLazyLoadingInstance(final Class c) {
        return Enhancer.create(c,
                new LazyLoader() {
                    public Object loadObject() {
                        try {
                            System.out.println("loadObject called");
                            return c.newInstance();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
    }

}
