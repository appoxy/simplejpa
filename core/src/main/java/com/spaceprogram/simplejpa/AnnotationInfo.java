package com.spaceprogram.simplejpa;

import com.spaceprogram.simplejpa.AnnotationManager.ClassMethodEntry;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
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
    private PersistentProperty idMethod;
    private Map<String, PersistentProperty> persistentProperties = new HashMap();
    private String discriminatorValue;
    private String domainName;
    private Class rootClass;
    private Class mainClass;
    private Map<Class, List<ClassMethodEntry>> entityListeners = new HashMap<Class, List<ClassMethodEntry>>();

    public void setClassAnnotations(Annotation[] classAnnotations) {
        this.classAnnotations = classAnnotations;
    }

    public void setIdProperty(PersistentProperty property) {
        this.idMethod = property;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public Annotation[] getClassAnnotations() {
        return classAnnotations;
    }

    public PersistentProperty getIdMethod() {
        return idMethod;
    }

	public String getDomainName()
	{
		return domainName;
	}

    public PersistentProperty addGetter(Method method) {
        PersistentMethod persistentMethod = new PersistentMethod(method);
        // if we already have an accessor in the list, don't overwrite it
        if (persistentProperties.containsKey(persistentMethod.getFieldName())) return persistentProperties.get(persistentMethod.getFieldName());
        persistentProperties.put(persistentMethod.getFieldName(), persistentMethod);
        if (persistentMethod.isId()) setIdProperty(persistentMethod);
        return persistentMethod;
    }

    public PersistentProperty addField(Field field) {
        PersistentField persistentField = new PersistentField(field);
        // if we already have an accessor in the list, don't overwrite it
        if (persistentProperties.containsKey(persistentField.getFieldName())) return persistentProperties.get(persistentField.getFieldName());
        persistentProperties.put(persistentField.getFieldName(), persistentField);
        if (persistentField.isId()) setIdProperty(persistentField);
        return persistentField;
    }

    public Collection<PersistentProperty> getPersistentProperties() {
        return persistentProperties.values();
    }

    public PersistentProperty getPersistentProperty(String field) {
        return persistentProperties.get(field);
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
