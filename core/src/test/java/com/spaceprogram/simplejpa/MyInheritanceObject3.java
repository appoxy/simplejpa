package com.spaceprogram.simplejpa;

import javax.persistence.Entity;

/**
 * User: treeder
 * Date: Feb 20, 2008
 * Time: 7:06:54 PM
 */
@Entity
public class MyInheritanceObject3 extends MyInheritanceObject1{

    private String fieldInSubClass3;

    public String getFieldInSubClass3() {
        return fieldInSubClass3;
    }

    public void setFieldInSubClass3(String fieldInSubClass3) {
        this.fieldInSubClass3 = fieldInSubClass3;
    }
}
