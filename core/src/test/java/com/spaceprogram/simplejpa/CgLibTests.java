package com.spaceprogram.simplejpa;

import net.sf.cglib.proxy.Factory;
import net.sf.cglib.proxy.Callback;
import org.junit.Test;

import java.lang.reflect.Method;

/**
 * User: treeder
 * Date: Mar 25, 2008
 * Time: 12:19:32 PM
 */
public class CgLibTests {

    @Test
    public void testToCheckEnhancedMethods(){
        Bean bean = (Bean) BeansCglib.newInstance(Bean.class);
        for (Class<?> aClass : bean.getClass().getInterfaces()) {
            System.out.println("interface=" + aClass);
        }
        for (Method method : bean.getClass().getDeclaredMethods()) {
            System.out.println("method=" + method);
        }
        Factory factory = (Factory) bean;
        for (Callback callback : factory.getCallbacks()) {
            System.out.println("callback=" + callback);
        }
    }
}
