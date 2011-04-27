package com.spaceprogram.simplejpa;

import javax.persistence.Entity;
import javax.persistence.Inheritance;

/**
 * User: treeder
 * Date: Feb 19, 2008
 * Time: 11:12:59 PM
 */
@Entity
@Inheritance
public class MyInheritanceObject1 extends MySuperClass{
    private String field;

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }
}
