package com.spaceprogram.simplejpa

import org.junit.Test
import javax.persistence.*
import static org.junit.Assert.*
import com.spaceprogram.simplejpa.PersistentProperty.OrderClause.Order

/**
 * User: kerrywright
 * Date: 11-06-12
 */
class PersistentPropertyTests {
    @Test
    public void testGetMethodClasses() {
        PersistentMethod method = new PersistentMethod(MyTestObject.class.getDeclaredMethod("getMyList"))
        assertEquals(List.class, method.getRawClass())
        assertEquals(MyTestObject2.class, method.getPropertyClass())
    }

    @Test
    public void testGetFieldClasses() {
        PersistentField field = new PersistentField(MyTestObject.class.getDeclaredField("myList"))
        assertEquals(List.class, field.getRawClass())
        assertEquals(MyTestObject2.class, field.getPropertyClass())
    }

    @Test
    public void testGetIdColumnName() {
        PersistentMethod method = new PersistentMethod(MySuperClass.class.getDeclaredMethod("getId"))
        assertEquals(NamingHelper.NAME_FIELD_REF, method.getColumnName())
    }

    @Test
    public void testGetColumnNameNoAnnotation() {
        PersistentField field = new PersistentField(OverAnnotatedClass.class.getDeclaredField("unannotated"))
        assertEquals("unannotated", field.getColumnName())
    }

    @Test
    public void testGetColumnNameNoColumnValue() {
        PersistentField field = new PersistentField(OverAnnotatedClass.class.getDeclaredField("emptyColumn"))
        assertEquals("emptyColumn", field.getColumnName())
    }

    @Test
    public void testGetFKColumnName() {
        PersistentField field = new PersistentField(OverAnnotatedClass.class.getDeclaredField("refObject"))
        assertEquals("refObject_id", field.getColumnName())
    }

    @Test
    public void testGetLOBColumnName() {
        PersistentField field = new PersistentField(OverAnnotatedClass.class.getDeclaredField("reallyBigField"))
        assertEquals("reallyBigField-lobkey", field.getColumnName())
    }

    @Test
    public void testGetColumnNameColumnOverride() {
        PersistentField field = new PersistentField(OverAnnotatedClass.class.getDeclaredField("id"))
        assertEquals("override", field.getColumnName())
    }

    @Test
    public void testParseEmptyOrderBy() {
        PersistentProperty prop = new PersistentField(OverAnnotatedClass.class.getDeclaredField("emptyOrderBy"))
        assertTrue(prop.getOrderClauses().isEmpty())
    }

    @Test
    public void testParseSimpleOrderBy() {
        PersistentProperty prop = new PersistentField(OverAnnotatedClass.class.getDeclaredField("simpleOrderBy"))
        assertEquals(1, prop.getOrderClauses().size())
        assertEquals("id", prop.getOrderClauses().get(0).field)
        assertEquals(Order.ASC, prop.getOrderClauses().get(0).order)
    }

    @Test
    public void testParseOrderByWithOrder() {
        PersistentProperty prop = new PersistentField(OverAnnotatedClass.class.getDeclaredField("orderByWithOrder"))
        assertEquals(1, prop.getOrderClauses().size())
        assertEquals("id", prop.getOrderClauses().get(0).field)
        assertEquals(Order.DESC, prop.getOrderClauses().get(0).order)
    }

    @Test
    public void testParseOrderByWithMultipleColumns() {
        PersistentProperty prop = new PersistentField(OverAnnotatedClass.class.getDeclaredField("orderByMultiColumn"))
        assertEquals(2, prop.getOrderClauses().size())
        assertEquals("id", prop.getOrderClauses().get(0).field)
        assertEquals(Order.DESC, prop.getOrderClauses().get(0).order)
        assertEquals("name", prop.getOrderClauses().get(1).field)
        assertEquals(Order.ASC, prop.getOrderClauses().get(1).order)
    }

    @Test
    public void testParseOrderByTrailingComma() {
        PersistentProperty prop = new PersistentField(OverAnnotatedClass.class.getDeclaredField("orderByTrailingComma"))
        assertEquals(1, prop.getOrderClauses().size())
        assertEquals("id", prop.getOrderClauses().get(0).field)
        assertEquals(Order.DESC, prop.getOrderClauses().get(0).order)
    }

    @Test(expected=IllegalArgumentException.class)
    public void testParseMalformedOrderBy() {
        PersistentProperty prop = new PersistentField(OverAnnotatedClass.class.getDeclaredField("invalidOrderBy"))
    }
}

class OverAnnotatedClass {
    @Id
    @Column(name="override")
    String id

    @Column
    String emptyColumn

    @ManyToOne
    MyTestObject refObject

    @Lob
    String reallyBigField

    Long unannotated

    @OneToMany
    @OrderBy
    List<MyTestObject> emptyOrderBy

    @OneToMany
    @OrderBy("id")
    List<MyTestObject> simpleOrderBy

    @OneToMany
    @OrderBy("id DESC")
    List<MyTestObject> orderByWithOrder

    @OneToMany
    @OrderBy("id DESC,")
    List<MyTestObject> orderByTrailingComma

    @OneToMany
    @OrderBy("id DESC, name")
    List<MyTestObject> orderByMultiColumn

    @OneToMany
    @OrderBy("id DESC name")
    List<MyTestObject> invalidOrderBy
}