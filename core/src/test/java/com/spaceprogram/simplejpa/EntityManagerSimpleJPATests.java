package com.spaceprogram.simplejpa;

import java.math.BigDecimal;
import java.util.*;

import com.amazonaws.services.simpledb.AmazonSimpleDB;
import org.junit.Test;
import static org.junit.Assert.*;
import org.unitils.UnitilsJUnit4;
import org.unitils.mock.Mock;
import com.spaceprogram.simplejpa.util.AmazonSimpleDBUtil;

/**
 * User: kerrywright
 * Date: 11-06-19
 */
public class EntityManagerSimpleJPATests extends UnitilsJUnit4{
    Mock<EntityManagerFactoryImpl> managerFactoryMock;
    Mock<PersistentProperty> persistentPropertyMock;

    @Test
    public void testConvertDouble() throws Exception {
        EntityManagerSimpleJPA manager = new EntityManagerSimpleJPA(managerFactoryMock.getMock(), true);
        String stored = AmazonSimpleDBUtil.encodeRealNumberRange(new BigDecimal(10.55), AmazonSimpleDBUtil.LONG_DIGITS, AmazonSimpleDBUtil.LONG_DIGITS, EntityManagerSimpleJPA.OFFSET_VALUE);
        assertEquals(10.55, (Double)manager.convert(Collections.singleton(stored), persistentPropertyMock.getMock(), Double.class), 0.001);
    }

    @Test
    public void testConvertNAN() throws Exception {
        EntityManagerSimpleJPA manager = new EntityManagerSimpleJPA(managerFactoryMock.getMock(), true);
        assertEquals(Double.NaN, (Double)manager.convert(Collections.singleton(""+Double.NaN), persistentPropertyMock.getMock(), Double.class), 0.01);
    }

    @Test
    public void testConvertBigDecimal() throws Exception {
        EntityManagerSimpleJPA manager = new EntityManagerSimpleJPA(managerFactoryMock.getMock(), true);
        String stored = AmazonSimpleDBUtil.encodeRealNumberRange(new BigDecimal(10.55), AmazonSimpleDBUtil.LONG_DIGITS, AmazonSimpleDBUtil.LONG_DIGITS, EntityManagerSimpleJPA.OFFSET_VALUE);
        assertEquals(new BigDecimal(10.55).setScale(AmazonSimpleDBUtil.LONG_DIGITS, BigDecimal.ROUND_HALF_DOWN), (BigDecimal)manager.convert(Collections.singleton(stored), persistentPropertyMock.getMock(), BigDecimal.class));
    }

    @Test
    public void testConvertDate() throws Exception {
        EntityManagerSimpleJPA manager = new EntityManagerSimpleJPA(managerFactoryMock.getMock(), true);
        Date now = new Date();
        String stored = AmazonSimpleDBUtil.encodeDate(now);
        assertEquals(now, manager.convert(Collections.singleton(stored), persistentPropertyMock.getMock(), Date.class));
    }

    @Test
    public void testConvertStringCollection() throws Exception {
        persistentPropertyMock.returns(String.class).getPropertyClass();
        EntityManagerSimpleJPA manager = new EntityManagerSimpleJPA(managerFactoryMock.getMock(), true);
        assertEquals(Arrays.asList("me", "myself", "i"), manager.convert(Arrays.asList("me", "myself", "i"), persistentPropertyMock.getMock(), List.class));
    }

    @Test
    public void testConvertDateCollection() throws Exception {
        persistentPropertyMock.returns(Date.class).getPropertyClass();
        EntityManagerSimpleJPA manager = new EntityManagerSimpleJPA(managerFactoryMock.getMock(), true);
        Date now = new Date();
        String encoded = AmazonSimpleDBUtil.encodeDate(now);
        assertEquals(Arrays.asList(now, now), manager.convert(Arrays.asList(encoded, encoded), persistentPropertyMock.getMock(), List.class));
    }
}
