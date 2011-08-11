package com.spaceprogram.simplejpa;

import com.amazonaws.services.simpledb.model.Attribute;
import com.spaceprogram.simplejpa.query.QueryImpl;
import net.sf.cglib.proxy.Enhancer;

import javax.persistence.EnumType;
import javax.persistence.PersistenceException;
import java.util.*;
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
            for (PersistentProperty field : ai.getPersistentProperties()) {
                String attName = field.getFieldName();
                String columnName = field.getColumnName();
                if (field.isForeignKeyRelationship()) {
                    // lazy it up
                    Set<String> keys = getForeignKeys(em, field, columnName, atts);
                    logger.finest("keys=" + keys);
                    if (keys == null || keys.isEmpty()) {
                        continue;
                    }
                    // todo: stick a cache in here and check the cache for the instance before creating the lazy loader.
                    logger.finest("creating new lazy loading instance for field " + field.getFieldName() + " of class " + tClass.getSimpleName() + " with id " + id);
//                    Object toSet = newLazyLoadingInstance(retType, keys);
                    owi.getInterceptor().putForeignKey(attName, keys);
                } else if (field.isInverseRelationship()) {
                    Class typeInList = field.getPropertyClass();
                    // todo: should this return null if there are no elements??
//                    LazyList lazyList = new LazyList(this, newInstance, annotation.mappedBy(), id, typeInList, factory.getAnnotationManager().getAnnotationInfo(typeInList));

                    List<PersistentProperty.OrderClause> orderBy = null;
                    if (List.class.isAssignableFrom(field.getRawClass()))
                    {
                        orderBy = field.getOrderClauses();
                    }
                    
                    LazyList lazyList = new LazyList(em, typeInList, oneToManyQuery(em, attName, field.getMappedBy(), id, typeInList, orderBy));
//                    Class retType = field.getReturnType();
                    // todo: assuming List for now, handle other collection types
                    field.setProperty(newInstance, lazyList);
                } else if (field.isLob()) {
                    // handled in Proxy
                    String lobKeyAttributeName = field.getColumnName();
                    String lobKeyVal = getValueToSet(atts, lobKeyAttributeName, columnName);
                    logger.finest("lobkeyval to set on interceptor=" + lobKeyVal + " - fromatt=" + lobKeyAttributeName);
                    // TODO add multivalue support for LOB keys
                    if (lobKeyVal != null) owi.getInterceptor().putForeignKey(attName, Collections.singleton(lobKeyVal));
                } else if (field.getEnumType() != null) {
                    String val = getValueToSet(atts, attName, columnName);
                    if(val != null){
                        Object enumVal = getEnumValue(field, val);
                        field.setProperty(newInstance, enumVal);
                    }
                }
                else if(field.isId()) {
                    field.setProperty(newInstance, id);
                }
                else {
                    Collection<String> val = getValuesToSet(atts, attName, columnName);
                    if (val != null && !val.isEmpty()) {
                        em.setFieldValue(tClass, newInstance, field, val);
                    }
                }
            }
        } catch (Exception e) {
            throw new PersistenceException(e);
        }
        em.cachePut(id, newInstance);
        return newInstance;

    }

    static Object getEnumValue(PersistentProperty field, String val) {
        EnumType enumType = field.getEnumType();
        Class<? extends Enum> retType = (Class<? extends Enum>)field.getPropertyClass();
        Object enumVal = null;
        if (enumType == EnumType.STRING) {
            enumVal = Enum.valueOf(retType, val);
        } else { // ordinal
            enumVal = retType.getEnumConstants()[Integer.parseInt(val)];
        }
        return enumVal;
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

    private static Set<String> getForeignKeys(EntityManagerSimpleJPA em, PersistentProperty getter, String columnName, List<Attribute> atts) {
        String fkAttName = columnName != null ? columnName : NamingHelper.foreignKey(getter.getFieldName());
        HashSet<String> keys = new HashSet<String>(atts.size());
        for (Attribute att : atts) {
            if (att.getName().equals(fkAttName)) {
                keys.add(att.getValue());
            }
        }
        return keys;
    }

    private static Collection<String> getValuesToSet(List<Attribute> atts, String propertyName, String columnName) {
        Collection<String> values = new ArrayList<String>();
        if(columnName != null) propertyName = columnName;
        for (Attribute att : atts) {
            String attName = att.getName();
            if (attName.equals(propertyName)) {
                values.add(att.getValue());
            }
        }
        return values;
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


    private static QueryImpl oneToManyQuery(EntityManagerSimpleJPA em, String attName, String foreignKeyFieldName, Object id, Class typeInList, List<PersistentProperty.OrderClause> orderBy) {
        if (foreignKeyFieldName == null || foreignKeyFieldName.length() == 0) {
            // use the class containing the OneToMany
            foreignKeyFieldName = attName;
        }
        AnnotationInfo ai = em.getFactory().getAnnotationManager().getAnnotationInfo(typeInList);
        Class refType = ai.getPersistentProperty(foreignKeyFieldName).getPropertyClass();
        AnnotationInfo refAi = em.getAnnotationManager().getAnnotationInfo(refType);
        String query = createOneToManyQuery(typeInList, foreignKeyFieldName, refAi, id, orderBy);
        
        logger.finer("OneToMany query=" + query);
        return new QueryImpl(em, query);
    }

    static String createOneToManyQuery(Class typeInList, String foreignKeyFieldName, AnnotationInfo refAi, Object id, List<PersistentProperty.OrderClause> orderBy) {
        String foreignIdAttr = refAi.getIdMethod().getFieldName();
        String query = "select o from " + typeInList.getName() + " o where o." + foreignKeyFieldName + "." + foreignIdAttr + " = '" + id + "'";

        if (orderBy != null) {
            for(PersistentProperty.OrderClause clause : orderBy)
            {
                query += " and o." + clause.field + " is not null";
                query += " order by o." + clause.field;
                if (clause.order != null) query += " " + clause.order.toString();
            }
        }
        return query;
    }


}
