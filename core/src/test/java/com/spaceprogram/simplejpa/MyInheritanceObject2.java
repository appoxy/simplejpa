package com.spaceprogram.simplejpa;

import javax.persistence.Entity;

/**
 * User: treeder
 * Date: Feb 19, 2008
 * Time: 11:13:05 PM
 */
@Entity
public class MyInheritanceObject2 extends MyInheritanceObject1 {
    private String fieldInSubClass2;

    public String getFieldInSubClass2() {
        return fieldInSubClass2;
    }

    public void setFieldInSubClass2(String fieldInSubClass2) {
        this.fieldInSubClass2 = fieldInSubClass2;
    }
}
