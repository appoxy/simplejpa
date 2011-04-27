package com.spaceprogram.simplejpa;

/**
 * User: treeder
 * Date: Feb 10, 2008
 * Time: 7:56:39 PM
 */
public class Bean2 {
    private String name = "my name";

    public Bean2() {
        System.out.println("CONSTRUCTING BEAN2");
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
