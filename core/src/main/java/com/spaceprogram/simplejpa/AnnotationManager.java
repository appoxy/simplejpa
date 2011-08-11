package com.spaceprogram.simplejpa;

import javax.persistence.*;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * User: treeder
 * Date: Mar 22, 2008
 * Time: 11:39:54 PM
 */
public class AnnotationManager {

    private static Logger logger = Logger.getLogger(AnnotationManager.class.getName());

    // todo: implement EntityListeners for timestamps
    private Map<String, AnnotationInfo> annotationMap = new HashMap<String, AnnotationInfo>();
    private Map<String, AnnotationInfo> discriminatorMap = new HashMap<String, AnnotationInfo>();
    private SimpleJPAConfig config;

    public AnnotationManager(SimpleJPAConfig config) {
        this.config = config;
    }

    public AnnotationInfo getAnnotationInfo(Object o) {
        Class c = o.getClass();
        AnnotationInfo ai = getAnnotationInfo(c);
        return ai;
    }

    public Map<String, AnnotationInfo> getAnnotationMap() {
        return annotationMap;
    }


    public AnnotationInfo getAnnotationInfo(Class c) {
        c = stripEnhancerClass(c);
        AnnotationInfo ai = getAnnotationInfo(c.getName());
        if (ai == null) {
            ai = putAnnotationInfo(c);
        }
        return ai;
    }

    private AnnotationInfo getAnnotationInfo(String className) {
        AnnotationInfo ai = getAnnotationMap().get(className);
        return ai;
    }

    /**
     * This strips the cglib class name out of the enhanced classes.
     *
     * @param c
     * @return
     */
    public static Class stripEnhancerClass(Class c) {
        String className = c.getName();
        className = stripEnhancerClass(className);
        if(className.equals(c.getName())){
            // no change, did this to fix groovy issue
            return c;
        } else {
            c = getClass(className, c.getClassLoader());
        }
        return c;
    }

    public static String stripEnhancerClass(String className) {
        int enhancedIndex = className.indexOf("$$EnhancerByCGLIB");
        if (enhancedIndex != -1) {
            className = className.substring(0, enhancedIndex);
        }
        return className;
    }


    /**
     *
     * @param obClass
     * @param classLoader pass in null if you want to use the current threads classloader only
     * @return
     */
    public static Class getClass(String obClass, ClassLoader classLoader) {
        try {
            Class c = null;
            try {
                c = Class.forName(obClass, true, Thread.currentThread().getContextClassLoader());
            } catch (ClassNotFoundException e) {
                try {
                    c = Class.forName(obClass);
                } catch (ClassNotFoundException e1) {
//                    e1.printStackTrace();
//                    System.out.println("THIRD LEVEL CLASS LODER");
//                    c = obClass.getClass().getClassLoader().loadClass(obClass);
                    if(classLoader == null){
                        throw e1;
                    } else {
                        c = classLoader.loadClass(obClass);
                    }
                }
            }
            return c;
        } catch (ClassNotFoundException e) {
            throw new PersistenceException(e);
        }
    }

    /**
     * Gets all the annotation info for a particular class and puts it in our annotation info cache.
     *
     * @param c
     * @return
     */
    public AnnotationInfo putAnnotationInfo(Class c) {
        {
            Entity entity = (Entity) c.getAnnotation(Entity.class);
            if(entity == null){
                throw new PersistenceException("Class not marked as an @Entity: " + c.getName());
            }
        }
        AnnotationInfo ai = new AnnotationInfo();
        ai.setClassAnnotations(c.getAnnotations());
        ai.setMainClass(c);
        Class superClass = c;
        Class rootClass = null;
        while ((superClass = superClass.getSuperclass()) != null) {
            MappedSuperclass mappedSuperclass = (MappedSuperclass) superClass.getAnnotation(MappedSuperclass.class);
            Entity entity = (Entity) superClass.getAnnotation(Entity.class);
            Inheritance inheritance = (Inheritance) superClass.getAnnotation(Inheritance.class);
            if (mappedSuperclass != null || entity != null) {
                putProperties(ai, superClass);
                putMethods(ai, superClass);
                if (entity != null) {
                    rootClass = superClass;
                }
                putEntityListeners(ai, superClass);
            }
        }
        if (rootClass != null) {
            ai.setRootClass(rootClass);
            DiscriminatorValue dv = (DiscriminatorValue) c.getAnnotation(DiscriminatorValue.class);
            String discriminatorValue;
            if (dv != null) {
                discriminatorValue = dv.value();
                if (discriminatorValue == null) {
                    throw new PersistenceException("You must specify a value= for @DiscriminatorValue on " + c.getName());
                }
            } else {
                discriminatorValue = c.getSimpleName();
            }
            ai.setDiscriminatorValue(discriminatorValue);
            discriminatorMap.put(discriminatorValue, ai);
        } else {
            ai.setRootClass(c);
        }
        putTableDeclaration(ai, c);
        putProperties(ai, c);
        putMethods(ai, c);
        if (ai.getIdMethod() == null) {
            throw new PersistenceException("No ID method specified for: " + c.getName());
        }
        putEntityListeners(ai, c);

        getAnnotationMap().put(c.getName(), ai);
        return ai;
    }

    private void putMethods(AnnotationInfo ai, Class c) {
        Method[] methods = c.getDeclaredMethods();
        for (Method method : methods) {
//            logger.fine("method=" + method.getName());
            String methodName = method.getName();
            if (!methodName.startsWith("get")) continue;
//            System.out.println("method=" + methodName);
            if (config.isGroovyBeans() && (methodName.equals("getProperty") || methodName.equals("getMetaClass")))
                continue;
            Transient transientM = method.getAnnotation(Transient.class);
            if (transientM != null) continue; // we don't save this one
            ai.addGetter(method);
        }
    }


    /**
     * For field based annotations.
     * TODO add OneToOne and ManyToOne support
     *
     * @param ai
     * @param c
     */
    private void putProperties(AnnotationInfo ai, Class c) {
        for (Field field : c.getDeclaredFields()) {
            parseProperty(ai, c, field);
        }
    }

    private void parseProperty(AnnotationInfo ai, Class c, Field field) {
        // TODO add support for OneToOne
        if (!field.isAnnotationPresent(Transient.class) && (field.isAnnotationPresent(ManyToMany.class) || field.isAnnotationPresent(OneToMany.class) || field.isAnnotationPresent(ManyToOne.class) || field.isAnnotationPresent(Id.class))) {
            ai.addField(field);
        }
    }

    private void putTableDeclaration(AnnotationInfo ai, Class<?> c)
	{
    	Table table = c.getAnnotation(Table.class);
    	if(table != null)
    	{
    		if(table.name() == null)
    			throw new PersistenceException("You must specify a name= for @Table on " + c.getName());
    		
    		ai.setDomainName(table.name());
    	}
	}


    private void putEntityListeners(AnnotationInfo ai, Class c) {
        EntityListeners listeners = (EntityListeners) c.getAnnotation(EntityListeners.class);
        if (listeners != null) {
            logger.fine("Found EntityListeners for " + c + " - " + listeners);
            putEntityListeners(ai, listeners);
        }
    }

    @SuppressWarnings("unchecked")
    private void putEntityListeners(AnnotationInfo ai, EntityListeners entityListeners) {
        Class[] entityListenerClasses = entityListeners.value();
        if (entityListenerClasses == null) return;

        Map<Class, List<ClassMethodEntry>> listeners = ai.getEntityListeners();

        List<Class<? extends Annotation>> annotations = Arrays.asList(
                PrePersist.class,
                PreUpdate.class,
                PreRemove.class,
                PostLoad.class,
                PostPersist.class,
                PostUpdate.class,
                PostRemove.class
        );
        // TODO: More than one listener per event cannot be handled like this...

        for (Class clazz : entityListenerClasses) {
//            System.out.println("class=" + clazz);
            for (Method method : clazz.getMethods()) {
//                System.out.println("method=" + method.getName());
                for (Class<? extends Annotation> annotationClass : annotations) {
                    Annotation annotation = method.getAnnotation(annotationClass);
                    addListener(listeners, clazz, method, annotation, annotationClass);
                }

            }
        }
    }

    private void addListener(Map<Class, List<ClassMethodEntry>> listeners, Class clazz, Method method, java.lang.annotation.Annotation annotation, Class<? extends Annotation> annotationClass) {
        if (annotation != null) {
            List<ClassMethodEntry> entryList = listeners.get(annotationClass);
            if (entryList == null) {
                entryList = new ArrayList<ClassMethodEntry>();
                listeners.put(annotationClass, entryList);
            }
//            System.out.println("adding " + method + " for " + annotation);
            entryList.add(new ClassMethodEntry(clazz, method));
        }
    }

    public AnnotationInfo getAnnotationInfoByDiscriminator(String discriminatorValue) {
        return discriminatorMap.get(discriminatorValue);
    }

    public class ClassMethodEntry {
        private Class clazz;
        private Method method;

        public ClassMethodEntry(Class clazz, Method method) {
            this.clazz = clazz;
            this.method = method;
        }

        public void invoke(Object... args) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, InstantiationException {
            this.method.invoke(clazz.newInstance(), args);
        }
    }
}
