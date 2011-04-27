package com.spaceprogram.simplejpa;

import javax.persistence.Entity;

/**
 * User: treeder
 * Date: Jul 29, 2008
 * Time: 6:50:21 PM
 */
@Entity
public class MyTestObject5Ext3 extends MyTestObject3{
    private String ob5String;

    public String getOb5String() {
        return ob5String;
    }

    public void setOb5String(String ob5String) {
        this.ob5String = ob5String;
    }
}
