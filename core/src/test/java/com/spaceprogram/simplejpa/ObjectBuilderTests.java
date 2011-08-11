package com.spaceprogram.simplejpa;

import org.unitils.UnitilsJUnit4;
import org.unitils.mock.*;
import static org.junit.Assert.*;
import org.junit.Test;

import java.util.*;

/**
 * Initial set of unit tests for ObjectBuilder
 * Kerry Wright
 */
public class ObjectBuilderTests extends UnitilsJUnit4{
    Mock<AnnotationInfo> refInfo;

    @Test
    public void testBuildMutiValuedAttribute() {

    }

    @Test
    public void testGetOrdinalEnumValue() throws NoSuchMethodException {
        PersistentMethod method = new PersistentMethod(MyTestObject.class.getDeclaredMethod("getMyEnumOrdinal"));
        assertEquals(MyEnum.me, ObjectBuilder.getEnumValue(method, "0"));
        assertEquals(MyEnum.myself, ObjectBuilder.getEnumValue(method, "1"));
    }

    @Test
    public void testGetStringEnumValue() throws NoSuchMethodException {
        PersistentMethod method = new PersistentMethod(MyTestObject.class.getDeclaredMethod("getMyEnumString"));
        assertEquals(MyEnum.me, ObjectBuilder.getEnumValue(method, "me"));
        assertEquals(MyEnum.myself, ObjectBuilder.getEnumValue(method, "myself"));
    }

    @Test
    public void testCreateSimpleOneToManyQuery() throws NoSuchMethodException {
        refInfo.returns(new PersistentMethod(MySuperClass.class.getDeclaredMethod("getId"))).getIdMethod();
        refInfo.returns(MyTestObject2.class).getMainClass();

        String query = ObjectBuilder.createOneToManyQuery(MyTestObject.class, "fkField", refInfo.getMock(), "Value", null);
        assertEquals(query, "select o from com.spaceprogram.simplejpa.MyTestObject o where o.fkField.id = 'Value'");
    }

    @Test
    public void testCreateOneToManyWithOrderBy() throws NoSuchMethodException {
        refInfo.returns(new PersistentMethod(MySuperClass.class.getDeclaredMethod("getId"))).getIdMethod();
        refInfo.returns(MyTestObject2.class).getMainClass();
        PersistentProperty.OrderClause orderBy = new PersistentProperty.OrderClause("orderField", PersistentProperty.OrderClause.Order.ASC);

        String query = ObjectBuilder.createOneToManyQuery(MyTestObject.class, "fkField", refInfo.getMock(), "Value", Arrays.asList(orderBy));
        assertEquals(query, "select o from com.spaceprogram.simplejpa.MyTestObject o where o.fkField.id = 'Value' and o.orderField is not null order by o.orderField ASC");
    }
}
