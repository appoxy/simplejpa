package com.spaceprogram.simplejpa;

import com.spaceprogram.simplejpa.query.JPAQuery;
import com.spaceprogram.simplejpa.query.JPAQueryParser;
import com.spaceprogram.simplejpa.query.QueryImpl;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * User: treeder
 * Date: Mar 2, 2008
 * Time: 6:00:24 PM
 */
public class QueryTests {
    @Test
    public void testWhere(){
        JPAQuery query = new JPAQuery();
        JPAQueryParser parser;
        List<String> split;

        parser = new JPAQueryParser(query, ("select o from MyTestObject o where o.myTestObject2.id = :id2 and 1=1 OR o.myTestObject2.name = 'larry'"));
        parser.parse();
        split = QueryImpl.tokenizeWhere(query.getFilter());
        Assert.assertEquals(11, split.size());
        Assert.assertEquals("o.myTestObject2.id = :id2 and 1 = 1 OR o.myTestObject2.name = 'larry' ", toString(split));
    }

    @Test
    public void testDates(){
        String q = "select o from MyTestObject o where o.birthday > :from and o.birthday < :to and o.id = :id";
        JPAQuery query = new JPAQuery();
        JPAQueryParser parser;
        List<String> split;

        parser = new JPAQueryParser(query, (q));
        parser.parse();
        split = QueryImpl.tokenizeWhere(query.getFilter());
        String delimited = toString(split, " || ");
        System.out.println("delimited: " + delimited);
        Assert.assertEquals("o.birthday > :from and o.birthday < :to and o.id = :id ", toString(split));
        Assert.assertEquals(11, split.size());
    }

    private String toString(List<String> split) {
        return toString(split, " ");
    }
    private String toString(List<String> split, String delimiter) {
        StringBuilder sb = new StringBuilder();
        for (String s : split) {
            System.out.print(s + delimiter);
            sb.append(s + delimiter);
        }
        return sb.toString();
    }
}
