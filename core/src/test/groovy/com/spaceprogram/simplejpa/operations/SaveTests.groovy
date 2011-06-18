package com.spaceprogram.simplejpa.operations

import org.junit.Test
import static org.junit.Assert.*
import javax.persistence.Entity
import javax.persistence.Enumerated
import javax.persistence.EnumType
import com.spaceprogram.simplejpa.PersistentField
import javax.persistence.Id

/**
 * User: kerrywright
 * Date: 11-06-16
 */
class SaveTests {
    @Test
    public void testGetOrdinalEnumValue() {
        PersistentField testField = new PersistentField(SaveTestObject.class.getDeclaredField("ordinalEnum"))
        assertEquals("1", Save.getEnumValue(testField, new SaveTestObject(ordinalEnum: SaveTestEnum.Myself)))

    }

    @Test
    public void testGetStringEnumValue() {
        PersistentField testField = new PersistentField(SaveTestObject.class.getDeclaredField("stringEnum"))
        assertEquals("Myself", Save.getEnumValue(testField, new SaveTestObject(stringEnum: SaveTestEnum.Myself)))
    }

    @Test
    public void testGetDefaultEnumValue() {
        PersistentField testField = new PersistentField(SaveTestObject.class.getDeclaredField("defaultEnum"))
        assertEquals("1", Save.getEnumValue(testField, new SaveTestObject(defaultEnum: SaveTestEnum.Myself)))
    }
}

public enum SaveTestEnum {
    Me,
    Myself,
    I
}

@Entity
class SaveTestObject {
    @Id
    String id

    @Enumerated
    SaveTestEnum defaultEnum

    @Enumerated(EnumType.ORDINAL)
    SaveTestEnum ordinalEnum

    @Enumerated(EnumType.STRING)
    SaveTestEnum stringEnum
}
