package com.spaceprogram.simplejpa

import org.junit.Test
import javax.persistence.Entity
import javax.persistence.OneToMany
import javax.persistence.ManyToOne
import static org.junit.Assert.*
import javax.persistence.Id

/**
 * Basic test set to validate support for groovy object mappings
 */
public class AnnotationManagerGroovyTests {
    @Test
    public void testParseField() {
        AnnotationManager annotationManager = new AnnotationManager(new SimpleJPAConfig(groovyBeans: true))
        AnnotationInfo ai = new AnnotationInfo()
        annotationManager.parseProperty(ai, OneToManyObject.class, OneToManyObject.getDeclaredField("id"))

        assertEquals(1, ai.getPersistentProperties().size())
        assertNotNull(ai.getPersistentProperty("id"))
        assertTrue(ai.getPersistentProperty("id").isId())
    }

    @Test
    public void testParseAnnotations() {
        AnnotationManager annotationManager = new AnnotationManager(new SimpleJPAConfig(groovyBeans: true))
        AnnotationInfo ai = annotationManager.getAnnotationInfo(OneToManyObject.class)
        assertEquals("Expected to find 'manyToOnes and id, found: ${ai.getPersistentProperties()}", 2, ai.getPersistentProperties().size())
        assertNotNull(ai.getPersistentProperty('manyToOnes'))
        assertNotNull(ai.getPersistentProperty('id'))
        assertTrue(ai.getPersistentProperty('id').isId())
        assertTrue(ai.getPersistentProperty('manyToOnes').isInverseRelationship())
    }
}

@Entity
public class OneToManyObject {
    @Id
    String id

    @OneToMany
    Collection<ManyToOneObject> manyToOnes
}

@Entity
public class ManyToOneObject {
    @Id
    String id

    @ManyToOne
    OneToMany oneToMany
}