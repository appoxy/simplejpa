package com.spaceprogram.simplejpa.query;

import com.spaceprogram.simplejpa.EntityManagerSimpleJPA;
import com.spaceprogram.simplejpa.util.AmazonSimpleDBUtil;
import org.junit.Test;
import org.unitils.UnitilsJUnit4;
import org.unitils.mock.Mock;

import java.util.Date;

import static org.junit.Assert.*;

/**
 * Kerry Wright
 */
public class SimpleDBQueryTests extends UnitilsJUnit4 {
    Mock<EntityManagerSimpleJPA> mockEM;

    @Test
    public void testNoParameterReplacement() {
        SimpleDBQuery q = new SimpleDBQuery(mockEM.getMock(), "select count(*) from DomainClass");
        AmazonQueryString aq = q.createAmazonQuery(false);
        assertTrue(aq.isCount());
        assertEquals("select count(*) from DomainClass", aq.getValue());
    }

    @Test
    public void testSimpleParameterReplacement() {
        SimpleDBQuery q = new SimpleDBQuery(mockEM.getMock(), "select count(*) from DomainClass where col1 = :val1");
        q.setParameter("val1", "value1");
        AmazonQueryString aq = q.createAmazonQuery(false);
        assertTrue(aq.isCount());
        assertEquals("select count(*) from DomainClass where col1 = 'value1'", aq.getValue());
    }

    @Test
    public void testIntegerValueReplacement() {
        SimpleDBQuery q = new SimpleDBQuery(mockEM.getMock(), "select count(*) from DomainClass where col1 = :val1");
        q.setParameter("val1", 10);
        AmazonQueryString aq = q.createAmazonQuery(false);
        assertTrue(aq.isCount());
        assertEquals("select count(*) from DomainClass where col1 = '09223372036854775818'", aq.getValue());
    }

    @Test
    public void testDateValueReplacement() {
        Date now = new Date();
        SimpleDBQuery q = new SimpleDBQuery(mockEM.getMock(), "select count(*) from DomainClass where col1 = :val1");
        q.setParameter("val1", now);
        AmazonQueryString aq = q.createAmazonQuery(false);
        assertTrue(aq.isCount());
        assertEquals("select count(*) from DomainClass where col1 = '"+ AmazonSimpleDBUtil.encodeDate(now)+"'", aq.getValue());
    }

    @Test
    public void testMultipleReplacements() {
        Date now = new Date();
        SimpleDBQuery q = new SimpleDBQuery(mockEM.getMock(), "select * from DomainClass where col1 = :val1 and col10 > :val10");
        q.setParameter("val1", now);
        q.setParameter("val10", 10);
        AmazonQueryString aq = q.createAmazonQuery(false);
        assertFalse(aq.isCount());
        String expected = new StringBuilder().append("select * from DomainClass where col1 = '")
                .append(AmazonSimpleDBUtil.encodeDate(now))
                .append("' and col10 > '09223372036854775818'").toString();
        assertEquals(expected, aq.getValue());
    }

    @Test
    public void testConvertToCountQuery() {
        AmazonQueryString aq = new AmazonQueryString("select * from DomainClass", false);
        assertEquals("select count(*) from DomainClass", SimpleDBQuery.convertToCountQuery(aq));
    }

    @Test
    public void testConvertCountToCountQuery() {
        AmazonQueryString aq = new AmazonQueryString("select count(*) from DomainClass", true);
        assertEquals("select count(*) from DomainClass", SimpleDBQuery.convertToCountQuery(aq));
    }

    @Test
    public void testConvertFieldQueryToCount() {
        AmazonQueryString aq = new AmazonQueryString("select itemName(), col1, col2 from DomainClass", true);
        assertEquals("select count(*) from DomainClass", SimpleDBQuery.convertToCountQuery(aq));
    }
}
