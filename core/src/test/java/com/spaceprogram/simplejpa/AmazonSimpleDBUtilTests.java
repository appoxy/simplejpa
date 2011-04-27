package com.spaceprogram.simplejpa;

import com.spaceprogram.simplejpa.util.AmazonSimpleDBUtil;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;

/**
 * User: treeder
 * Date: Feb 8, 2008
 * Time: 5:36:59 PM
 */
public class AmazonSimpleDBUtilTests {

    @Test
    public void testSimpleJPADefaults(){
        long x = 12345L;
        String encoded = EntityManagerSimpleJPA.padOrConvertIfRequired(x);
        System.out.println("encoded=" + encoded);
    }
    @Test
    public void testInt() {
        System.out.println("MAX INT=" + Integer.MAX_VALUE);
        System.out.println("MIN INT=" + Integer.MIN_VALUE);
        int x = 5432;
        int idecoded;
        String encoded;

        encoded = AmazonSimpleDBUtil.encodeZeroPadding(x, 10);
        System.out.println("encoded=" + encoded);
        idecoded = AmazonSimpleDBUtil.decodeZeroPaddingInt(encoded);
        Assert.assertEquals(x, idecoded);

        x = Integer.MAX_VALUE;
        encoded = AmazonSimpleDBUtil.encodeZeroPadding(x, 10);
        System.out.println("encoded=" + encoded);
        idecoded = AmazonSimpleDBUtil.decodeZeroPaddingInt(encoded);
        Assert.assertEquals(x, idecoded);

        BigDecimal bdx;
        BigDecimal bgdecoded;
        x = 500;


        x = -390293;
        bdx = new BigDecimal(x);
        encoded = AmazonSimpleDBUtil.encodeRealNumberRange(bdx, 11, new BigDecimal(Integer.MIN_VALUE).negate());
        System.out.println("encoded=" + encoded);
        bgdecoded = AmazonSimpleDBUtil.decodeRealNumberRange(encoded, new BigDecimal(Integer.MIN_VALUE).negate());
        Assert.assertEquals(bdx, bgdecoded);

        x = Integer.MIN_VALUE;
        bdx = new BigDecimal(x);
        encoded = AmazonSimpleDBUtil.encodeRealNumberRange(bdx, 11, new BigDecimal(Integer.MIN_VALUE).negate());
        System.out.println("encoded=" + encoded);
        bgdecoded = AmazonSimpleDBUtil.decodeRealNumberRange(encoded, new BigDecimal(Integer.MIN_VALUE).negate());
        System.out.println("decoded=" + bgdecoded);
        Assert.assertEquals(bdx, bgdecoded);
    }

    @Test
    public void testLong() {
        System.out.println("MAX LONG=" + Long.MAX_VALUE);
        System.out.println("MIN LONG=" + Long.MIN_VALUE);
        long x;

        x = 209323021234234498L;
        encodeAndDecode(x);

        x = Long.MAX_VALUE;
        encodeAndDecode(x);

        x = Long.MIN_VALUE;
        encodeAndDecode(x);
    }

    @Test
    public void testDouble() {
        System.out.println("MAX DOUBLE=" + Double.MAX_VALUE);
        System.out.println("MIN DOUBLE=" + Double.MIN_VALUE);
        double x;

        x = 500.0;
        encodeAndDecode(x);

        x = 20932304234498.039;
        encodeAndDecode(x);

        x = Double.MAX_VALUE;
        encodeAndDecode(x);

/*
Does not work
        x = Double.MIN_VALUE;
        encodeAndDecode(x);*/

        x = -209323021234234498.902938903849082349;
        encodeAndDecode(x);
    }

    @Test
    public void testBigDecimals() {
        BigDecimal x;

        x = new BigDecimal("20932304234498.039");
        encodeAndDecode(x);

        x = new BigDecimal("-209323021234234498.902938903849082349");
        encodeAndDecode(x);

        /*
        nope, bigger than a long i'm assuming
        x = new BigDecimal("-20932234323021234234498.902938923423403849082349");
        encodeAndDecode(x);*/
    }

    private void encodeAndDecode(double x) {
        encodeAndDecode(new BigDecimal(x));
    }

    private void encodeAndDecode(long x) {
        String encoded;
        BigDecimal bgdecoded;
        BigDecimal bdx = new BigDecimal(x);

        encoded = AmazonSimpleDBUtil.encodeRealNumberRange(bdx, 20, new BigDecimal(Long.MIN_VALUE).negate());
        System.out.println("encoded=" + encoded);
        bgdecoded = AmazonSimpleDBUtil.decodeRealNumberRange(encoded, new BigDecimal(Long.MIN_VALUE).negate());
        System.out.println("decoded=" + bgdecoded);
        Assert.assertEquals(bdx, bgdecoded);
    }

    private void encodeAndDecode(BigDecimal bdx) {
        System.out.println("incoming=" + bdx);
        String encoded;
        BigDecimal bgdecoded;
        encoded = AmazonSimpleDBUtil.encodeRealNumberRange(bdx, 20, 20, new BigDecimal(Long.MIN_VALUE).negate());
        System.out.println("encoded=" + encoded);
        bgdecoded = AmazonSimpleDBUtil.decodeRealNumberRange(encoded, 20, new BigDecimal(Long.MIN_VALUE).negate());
        System.out.println("decoded=" + bgdecoded);
        Assert.assertTrue(bdx.compareTo(bgdecoded) == 0);
    }

    @Test
    public void bigDecimalTests() {
        BigDecimal bd = new BigDecimal("123.455");
        bd = bd.setScale(0, BigDecimal.ROUND_HALF_UP);
        System.out.println("bd=" + bd);
    }
}
