package com.spaceprogram.simplejpa;

import net.sf.cglib.asm.Type;
import net.sf.cglib.core.Signature;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import com.amazonaws.AmazonClientException;

import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.PersistenceException;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Lazy loader for second class objects that need it.
 * <p/>
 * User: treeder
 * Date: Feb 16, 2008
 * Time: 2:38:04 PM
 */
public class LazyInterceptor implements MethodInterceptor, Serializable {
    private static Logger logger = Logger.getLogger(LazyInterceptor.class.getName());

    /** Used to lazy load */
    private transient EntityManagerSimpleJPA em;
    /** Just for reference */
    private Map<String, String> foreignKeys;

    /** So we know which fields to delete */
    private Map<String, Object> nulledFields = new HashMap<String, Object>();
    private boolean dirty;

    public LazyInterceptor(EntityManagerSimpleJPA em) {
        this.em = em;
    }

    public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
        String name = method.getName();
        if (name.startsWith("get")) {
            Object ret = proxy.invokeSuper(obj, args);
            if (ret != null) {
                // then we've already loaded this up
                return ret;
            }
            if (handleGetMethod(obj, method)) return null;
        } else if (name.startsWith("set")) {
            handleSetMethod(obj, method, args);
        }
        return proxy.invokeSuper(obj, args);
    }

    private void handleSetMethod(Object obj, Method method, Object[] args) throws Throwable {
        // we basically want to mark this object as dirty if this is called and to only delete attributes if it's dirty
        dirty = true;
        String attributeName = NamingHelper.attributeName(method);
        if (args != null && args.length == 1) {
            Object valueToSet = args[0];
            if (valueToSet == null) {
                // FIXME support direct field accessors better here
                PersistentMethod persistentMethod = (PersistentMethod)(PersistentMethod)em.getFactory().getAnnotationManager().getAnnotationInfo(obj).getPersistentProperty(attributeName);
                Method getter = persistentMethod.getGetter();
                MethodProxy getterProxy = MethodProxy.find(obj.getClass(), new Signature(persistentMethod.getGetter().getName(), Type.getType(getter.getReturnType()), new Type[]{}));
                Object ret = getterProxy.invokeSuper(obj, null);
                if (ret != null) {
                    nulledFields.put(attributeName, ret);
                    logger.fine("field " + attributeName + " is being nulled. Old value = " + ret);
                }
            }
        }
    }

    private boolean handleGetMethod(Object obj, Method method) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, AmazonClientException, IOException, ClassNotFoundException {
        // TODO user persistentproperty methods here instead
        PersistentProperty property = em.getAnnotationManager().getAnnotationInfo(obj.getClass()).getPersistentProperty(NamingHelper.attributeName(method));

        if (property.isForeignKeyRelationship()) {
            logger.fine("intercepting many to one");
            if (foreignKeys != null) {
                String foreignKey = foreignKeys.get(NamingHelper.attributeName(method));
                logger.finer("ManyToOne key=" + foreignKey);
                if (foreignKey == null) {
                    return true;
                }
                checkEntityManager();
                Class retType = method.getReturnType();
                logger.fine("loading ManyToOne object for type=" + retType + " with id=" + foreignKey);
                Object toSet = em.find(retType, foreignKey);
                if(logger.isLoggable(Level.FINEST)){
                    logger.finest("got object for ManyToOne=" + toSet);
                }
                String setterName = em.getSetterNameFromGetter(method);
                Method setter = obj.getClass().getMethod(setterName, retType);
                setter.invoke(obj, toSet);
            }
        } else if (property.isLob()) {
            if (foreignKeys != null) {
                String lobKey = foreignKeys.get(NamingHelper.attributeName(method));
                if (lobKey == null) {
                    return true;
                }
                checkEntityManager();
                logger.finer("intercepting lob. key==" + lobKey);
                Class retType = method.getReturnType();
                Object toSet = em.getObjectFromS3(lobKey);
                // System.out.println("toset=" + toSet);
                String setterName = em.getSetterNameFromGetter(method);
                Method setter = obj.getClass().getMethod(setterName, retType);
                setter.invoke(obj, toSet);
            }
        }
        return false;
    }

    private void checkEntityManager() {
        if(em == null){
            throw new PersistenceException("Could not lazy load for getter, EntityManager is null. This could be due to this object having been deserialized.");
        }
    }

    public void putForeignKey(String attributeName, String foreignKeyVal) {
        if (foreignKeys == null) foreignKeys = new HashMap<String, String>();
        foreignKeys.put(attributeName, foreignKeyVal);
    }

    public Map<String, Object> getNulledFields() {
        return nulledFields;
    }

    public  void setEntityManager(EntityManagerSimpleJPA entityManager) {
        this.em = entityManager;
    }

    public void reset() {
//        System.out.println("Resetting nulled fields.");
        nulledFields = new HashMap();
    }

    public EntityManagerSimpleJPA getEntityManager() {
        return em;
    }
}
