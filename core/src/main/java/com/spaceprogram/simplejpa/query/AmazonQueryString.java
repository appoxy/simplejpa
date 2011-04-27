package com.spaceprogram.simplejpa.query;

public class AmazonQueryString {
    private final String value;
    private boolean count;

    public AmazonQueryString(String value, boolean count) {
        this.value = value;
        this.count = count;
    }


    public String getValue() {
        return value;
    }

    public boolean isCount() {
        return count;
    }
}
