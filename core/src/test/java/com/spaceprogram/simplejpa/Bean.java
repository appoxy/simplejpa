package com.spaceprogram.simplejpa;

import java.beans.PropertyChangeListener;

/**
 * User: treeder
 * Date: Feb 10, 2008
 * Time: 7:53:00 PM
 */
public abstract class Bean implements java.io.Serializable {

    String sampleProperty;
    private Bean2 bean2;

    abstract public void addPropertyChangeListener(PropertyChangeListener listener);

    abstract public void removePropertyChangeListener(PropertyChangeListener listener);

    public String getSampleProperty() {
        return sampleProperty;
    }

    public void setSampleProperty(String value) {
        this.sampleProperty = value;
    }

    public String toString() {
        return "sampleProperty is " + sampleProperty;
    }

    public Bean2 getBean2() {
        return bean2;
    }

    public void setBean2(Bean2 bean2) {
        System.out.println("setBean2");
        this.bean2 = bean2;
    }
}

