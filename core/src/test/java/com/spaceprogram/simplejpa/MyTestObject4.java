package com.spaceprogram.simplejpa;

import javax.persistence.Entity;

/**
 * User: treeder
 * Date: Jun 9, 2008
 * Time: 11:03:44 PM
 */
@Entity
public class MyTestObject4 extends MySuperClass {
    private String name4;

    public String getName4() {
        return name4;
    }

    public void setName4(String name4) {
        this.name4 = name4;
    }
}
