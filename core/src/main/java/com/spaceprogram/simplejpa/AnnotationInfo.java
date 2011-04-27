package com.spaceprogram.simplejpa;

import com.spaceprogram.simplejpa.AnnotationManager.ClassMethodEntry;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

/**
 * User: treeder
 * Date: Feb 8, 2008
 * Time: 1:23:04 PM
 */
public class AnnotationInfo {

    private Annotation[] classAnnotations;
    private Method idMethod;
    private Map<String, Method> getterMap = new HashMap();
    private String discriminatorValue;
    private String domainName;
    private Class rootClass;
    private Class mainClass;
    private Map<Class, List<ClassMethodEntry>> entityListeners = new HashMap<Class, List<ClassMethodEntry>>();

    public void setClassAnnotations(Annotation[] classAnnotations) {
        this.classAnnotations = classAnnotations;
    }

    public void setIdMethod(Method idMethod) {
        this.idMethod = idMethod;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public Annotation[] getClassAnnotations() {
        return classAnnotations;
    }

    public Method getIdMethod() {
        return idMethod;
    }

	public String getDomainName()
	{
		return domainName;
	}

    public void addGetter(Method method) {
        getterMap.put(method.getName(), method);
    }

    public Collection<Method> getGetters() {
        return getterMap.values();
    }

    public Method getGetter(String field) {
        String getterName = NamingHelper.getterName(field);
        return getterMap.get(getterName);
    }

    public void setDiscriminatorValue(String discriminatorValue) {
        this.discriminatorValue = discriminatorValue;
    }

    public String getDiscriminatorValue() {
        return discriminatorValue;
    }

    public void setRootClass(Class rootClass) {
        this.rootClass = rootClass;
    }

    public Class getRootClass() {
        return rootClass;
    }

    public void setMainClass(Class mainClass) {
        this.mainClass = mainClass;
    }

    public Class getMainClass() {
        return mainClass;
    }

	/**
	 * @return the entityListeners
	 */
	public Map<Class, List<ClassMethodEntry>> getEntityListeners() {
		return entityListeners;
	}

	/**
	 * @param entityListeners the entityListeners to set
	 */
	public void setEntityListeners(Map<Class, List<ClassMethodEntry>> entityListeners) {
		this.entityListeners = entityListeners;
	}
}
