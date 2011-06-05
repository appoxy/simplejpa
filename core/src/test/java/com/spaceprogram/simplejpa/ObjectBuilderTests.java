package com.spaceprogram.simplejpa;

import org.unitils.UnitilsJUnit4;
import org.unitils.mock.*;
import static org.junit.Assert.*;
import org.junit.Test;

import javax.persistence.OrderBy;
import java.lang.reflect.Method;

/**
 * Initial set of unit tests for ObjectBuilder
 * Kerry Wright
 */
public class ObjectBuilderTests extends UnitilsJUnit4{
    Mock<AnnotationInfo> refInfo;
    Mock<OrderBy> orderBy;

    @Test
    public void testCreateSimpleOneToManyQuery() throws NoSuchMethodException {
        refInfo.returns(MySuperClass.class.getDeclaredMethod("getId")).getIdMethod();
        refInfo.returns(MyTestObject2.class).getMainClass();

        String query = ObjectBuilder.createOneToManyQuery(MyTestObject.class, "fkField", refInfo.getMock(), "Value", null);
        assertEquals(query, "select o from com.spaceprogram.simplejpa.MyTestObject o where o.fkField.id = 'Value'");
    }

    @Test
    public void testCreateOneToManyWithOrderBy() throws NoSuchMethodException {
        refInfo.returns(MySuperClass.class.getDeclaredMethod("getId")).getIdMethod();
        refInfo.returns(MyTestObject2.class).getMainClass();
        orderBy.returns("orderField ASC").value();

        String query = ObjectBuilder.createOneToManyQuery(MyTestObject.class, "fkField", refInfo.getMock(), "Value", orderBy.getMock());
        assertEquals(query, "select o from com.spaceprogram.simplejpa.MyTestObject o where o.fkField.id = 'Value' and o.orderField is not null order by o.orderField ASC");
    }
}
