package com.spaceprogram.simplejpa;

import com.amazonaws.services.simpledb.model.Attribute;
import com.spaceprogram.simplejpa.query.QueryImpl;
import net.sf.cglib.proxy.Enhancer;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.PersistenceException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

/**
 * User: treeder
 * Date: May 11, 2008
 * Time: 4:01:28 PM
 * 
 * Additional Contributions
 *  - Yair Ben-Meir reformy@gmail.com
 */
public class ObjectBuilder {

    private static Logger logger = Logger.getLogger(ObjectBuilder.class.getName());

    public static <T> T buildObject(EntityManagerSimpleJPA em, Class<T> tClass, Object id, List<Attribute> atts) {
        T newInstance;
        /*
        Why was this here?  Should we merge if it exists though?
        newInstance = em.cacheGet(tClass, id);
        if (newInstance != null) {
            return newInstance;
        }*/
        AnnotationInfo ai = em.getFactory().getAnnotationManager().getAnnotationInfo(tClass);
        try {
//            newInstance = tClass.newInstance();
            // check for DTYPE to see if it's a subclass, must be a faster way to do this you'd think?
            for (Attribute att : atts) {
                if (att.getName().equals(EntityManagerFactoryImpl.DTYPE)) {
                    logger.finest("dtype=" + att.getValue());
                    ai = em.getFactory().getAnnotationManager().getAnnotationInfoByDiscriminator(att.getValue());
                    if(ai == null) {
                        throw new PersistenceException(new ClassNotFoundException("Could not build object with dtype = " + att.getValue() + ". Class not found or is not an @Entity."));
                    }
                    tClass = ai.getMainClass();
                    // check cache again with new class
                    newInstance = em.cacheGet(tClass, id);
                    if (newInstance != null) return newInstance;
                    break;
                }
            }
            ObjectWithInterceptor owi = newEnancedInstance(em, tClass);
            newInstance = (T) owi.getBean();
            Collection<Method> getters = ai.getGetters();
            for (Method getter : getters) {
                String attName = NamingHelper.attributeName(getter);
                String columnName = NamingHelper.getColumnName(getter);
                if (getter.getAnnotation(ManyToOne.class) != null) {
                    // lazy it up
                    String identifierForManyToOne = getIdForManyToOne(em, getter, columnName, atts);
                    logger.finest("identifierForManyToOne=" + identifierForManyToOne);
                    if (identifierForManyToOne == null) {
                        continue;
                    }
                    // todo: stick a cache in here and check the cache for the instance before creating the lazy loader.
                    logger.finest("creating new lazy loading instance for getter " + getter.getName() + " of class " + tClass.getSimpleName() + " with id " + id);
//                    Object toSet = newLazyLoadingInstance(retType, identifierForManyToOne);
                    owi.getInterceptor().putForeignKey(attName, identifierForManyToOne);
                } else if (getter.getAnnotation(OneToMany.class) != null) {
                    OneToMany annotation = getter.getAnnotation(OneToMany.class);
                    ParameterizedType type = (ParameterizedType) getter.getGenericReturnType();
//                    logger.fine("type for manytoone=" + type + " " + type.getClass().getName()  + " " + type.getRawType() + " " + type.getOwnerType());
                    Type[] types = type.getActualTypeArguments();
                    Class typeInList = (Class) types[0];
                    // todo: should this return null if there are no elements??
//                    LazyList lazyList = new LazyList(this, newInstance, annotation.mappedBy(), id, typeInList, factory.getAnnotationManager().getAnnotationInfo(typeInList));
                    
                    OrderBy orderBy = null;
                    if (type.getRawType() == List.class)
                    {
                        orderBy = getter.getAnnotation(OrderBy.class);
                    }
                    
                    LazyList lazyList = new LazyList(em, typeInList, oneToManyQuery(em, attName, annotation.mappedBy(), id, typeInList, orderBy));
                    Class retType = getter.getReturnType();
                    // todo: assuming List for now, handle other collection types
                    String setterName = em.getSetterNameFromGetter(getter);
                    Method setter = tClass.getMethod(setterName, retType);
                    setter.invoke(newInstance, lazyList);
                } else if (getter.getAnnotation(Lob.class) != null) {
                    // handled in Proxy
                    String lobKeyAttributeName = NamingHelper.lobKeyAttributeName(getter);
                    String lobKeyVal = getValueToSet(atts, lobKeyAttributeName, columnName);
                    logger.finest("lobkeyval to set on interceptor=" + lobKeyVal + " - fromatt=" + lobKeyAttributeName);
                    if (lobKeyVal != null) owi.getInterceptor().putForeignKey(attName, lobKeyVal);
                } else if (getter.getAnnotation(Enumerated.class) != null) {
                    Enumerated enumerated = getter.getAnnotation(Enumerated.class);
                    Class retType = getter.getReturnType();
                    EnumType enumType = enumerated.value();
                    String val = getValueToSet(atts, attName, columnName);
                    if(val != null){
                        Object enumVal = null;
                        if (enumType == EnumType.STRING) {
                            Object[] enumConstants = retType.getEnumConstants();
                            for (Object enumConstant : enumConstants) {
                                if (enumConstant.toString().equals(val)) {
                                    enumVal = enumConstant;
                                }
                            }
                        } else { // ordinal
                            enumVal = retType.getEnumConstants()[Integer.parseInt(val)];
                        }
                        Method setMethod = em.getSetterFromGetter(tClass, getter, retType);
                        setMethod.invoke(newInstance, enumVal);
                    }
                }
                else if(getter.getAnnotation(Id.class) != null) {
                	Class retType = getter.getReturnType();
                	String setterName = em.getSetterNameFromGetter(getter);
                    Method setter = tClass.getMethod(setterName, retType);
                    setter.invoke(newInstance, id);
                }
                else {
                    String val = getValueToSet(atts, attName, columnName);
                    if (val != null) {
                        em.setFieldValue(tClass, newInstance, getter, val);
                    }
                }
            }
        } catch (Exception e) {
            throw new PersistenceException(e);
        }
        em.cachePut(id, newInstance);
        return newInstance;

    }

    private static ObjectWithInterceptor newEnancedInstance(EntityManagerSimpleJPA em, Class tClass) {
        LazyInterceptor interceptor = new LazyInterceptor(em);
        Enhancer e = new Enhancer();
        e.setSuperclass(tClass);
        e.setCallback(interceptor);
        Object bean = e.create();
        ObjectWithInterceptor cwi = new ObjectWithInterceptor(bean, interceptor);
        return cwi;
    }

    private static String getIdForManyToOne(EntityManagerSimpleJPA em, Method getter, String columnName, List<Attribute> atts) {
        String fkAttName = columnName != null ? columnName : NamingHelper.foreignKey(getter);
        for (Attribute att : atts) {
            if (att.getName().equals(fkAttName)) {
                return att.getValue();
            }
        }
        return null;
    }

    private static String getValueToSet(List<Attribute> atts, String propertyName, String columnName) {
        if(columnName != null) propertyName = columnName;
        for (Attribute att : atts) {
            String attName = att.getName();
            if (attName.equals(propertyName)) {
                String val = att.getValue();
                return val;
            }
        }
        return null;
    }


    private static QueryImpl oneToManyQuery(EntityManagerSimpleJPA em, String attName, String foreignKeyFieldName, Object id, Class typeInList, OrderBy orderBy) {
        if (foreignKeyFieldName == null || foreignKeyFieldName.length() == 0) {
            // use the class containing the OneToMany
            foreignKeyFieldName = attName;
        }
        AnnotationInfo ai = em.getFactory().getAnnotationManager().getAnnotationInfo(typeInList);
        Method getterForReference = ai.getGetter(foreignKeyFieldName);
        Class refType = getterForReference.getReturnType();
        AnnotationInfo refAi = em.getAnnotationManager().getAnnotationInfo(refType);
        String query = createOneToManyQuery(typeInList, foreignKeyFieldName, refAi, id, orderBy);
        
        logger.finer("OneToMany query=" + query);
        return new QueryImpl(em, query);
    }

    static String createOneToManyQuery(Class typeInList, String foreignKeyFieldName, AnnotationInfo refAi, Object id, OrderBy orderBy) {
        String foreignIdAttr = NamingHelper.attributeName(refAi.getIdMethod());
        String query = "select o from " + typeInList.getName() + " o where o." + foreignKeyFieldName + "." + foreignIdAttr + " = '" + id + "'";

        if (orderBy != null)
        {
        	query += " and o." + orderBy.value().split("\\s")[0] + " is not null";
        	query += " order by o." + orderBy.value();
        }
        return query;
    }


}
