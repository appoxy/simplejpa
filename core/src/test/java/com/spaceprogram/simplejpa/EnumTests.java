package com.spaceprogram.simplejpa;

import org.junit.Test;

/**
 * User: treeder
 * Date: May 10, 2008
 * Time: 12:42:17 PM
 */
public class EnumTests {
    
    @Test
    public void enumTest() {
        Class retType = MyEnum.class;
        Object[] enumConstants = retType.getEnumConstants();
        for (Object enumConstant : enumConstants) {
            System.out.println("const=" + enumConstant.getClass() + " - " + enumConstant);
        }

    }
}
