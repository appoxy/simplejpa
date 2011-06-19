package com.spaceprogram.simplejpa;

import org.apache.commons.lang.StringUtils;

import javax.persistence.*;
import java.lang.reflect.*;
import java.util.*;

/**
 * Kerry Wright
 */
public abstract class PersistentProperty {
    protected final AnnotatedElement element;
    protected final List<OrderClause> orderBys;

    protected PersistentProperty(AnnotatedElement annotatedElement) {
        this.element = annotatedElement;
        orderBys = parseOrderBy(annotatedElement.getAnnotation(OrderBy.class));
    }

    public Object getProperty(Object target) {
        try {
            return getGetter().invoke(target);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }

    public void setProperty(Object target, Object value) {
        try {
            getSetter().invoke(target, value);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }

    public Class<?> getPropertyClass() {
        Class clazz = getGetter().getReturnType();
        if (Collection.class.isAssignableFrom(clazz)) {
            return (Class<?>)((ParameterizedType)getGetter().getGenericReturnType()).getActualTypeArguments()[0];
        }
        return clazz;
    }

    public Class<?> getRawClass() {
        return getGetter().getReturnType();
    }

    public abstract Method getGetter();

    public abstract Method getSetter();

    public abstract String getFieldName();

    public boolean isLob() {
        return element.isAnnotationPresent(Lob.class);
    }

    public boolean isForeignKeyRelationship() {
        // TODO add support for non "mapped" OneToMany (ie: unidirectional one-to-many as multivalued attribute)
        return element.isAnnotationPresent(ManyToOne.class) || element.isAnnotationPresent(ManyToMany.class);
    }

    public boolean isInverseRelationship() {
        return element.isAnnotationPresent(OneToMany.class);
    }

    public boolean isId() {
        return element.isAnnotationPresent(Id.class);
    }

    public EnumType getEnumType() {
        if (element.isAnnotationPresent(Enumerated.class)) {
            if (element.getAnnotation(Enumerated.class).value() == EnumType.STRING) return EnumType.STRING;
            else return EnumType.ORDINAL;
        }
        return null;
    }

    public String getMappedBy() {
        if (element.isAnnotationPresent(OneToMany.class)) {
            return element.getAnnotation(OneToMany.class).mappedBy();
        }
        else if (element.isAnnotationPresent(OneToOne.class)) {
            return element.getAnnotation(OneToMany.class).mappedBy();
        }
        else if (element.isAnnotationPresent(ManyToMany.class)) {
            return element.getAnnotation(ManyToMany.class).mappedBy();
        }
        return null;
    }

    public String getColumnName() {
        if (element.isAnnotationPresent(Column.class)) {
            Column column = element.getAnnotation(Column.class);
            if (column.name() != null && !column.name().trim().isEmpty()) {
                String columnName = column.name();
                return columnName;
            }
        }
        if (isForeignKeyRelationship()) {
            return NamingHelper.foreignKey(getFieldName());
        }
        if (isLob()) {
            return NamingHelper.lobKeyAttributeName(getFieldName());
        }
        if (isId()) {
            return NamingHelper.NAME_FIELD_REF;
        }
        return StringUtils.uncapitalize(getFieldName());
    }

    public List<OrderClause> getOrderClauses() {
        return orderBys;
    }

    List<OrderClause> parseOrderBy(OrderBy orderAnnotation) {
        if (orderAnnotation == null || orderAnnotation.value().trim().isEmpty()) return Collections.emptyList();

        List<OrderClause> clauses = new ArrayList<OrderClause>();
        for (String orderBy : orderAnnotation.value().split(",")) {
            orderBy = orderBy.trim();
            if(orderBy.isEmpty()) continue;

            String[] parts = orderBy.trim().split("\\s");
            if (parts.length == 1) {
                clauses.add(new OrderClause(parts[0], OrderClause.Order.ASC));
            }
            else if (parts.length == 2) {
                clauses.add(new OrderClause(parts[0], OrderClause.Order.valueOf(parts[1])));
            }
            else throw new IllegalArgumentException("Invalid order by clause: "+orderAnnotation.value());
        }
        return clauses;
    }

    @Override
    public String toString() {
        return getFieldName();
    }

    public static class OrderClause {
        public enum Order {
            ASC,
            DESC
        }
        public final String field;
        public final Order order;

        public OrderClause(String field, Order order) {
            this.field = field;
            this.order = order;
        }
    }
}
