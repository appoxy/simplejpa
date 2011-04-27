package com.spaceprogram.simplejpa;

import com.amazonaws.services.simpledb.model.Attribute;

import java.util.List;

/**
 * Holds a item with its Attributes.
 *
 * User: treeder
 * Date: Feb 8, 2008
 * Time: 7:55:06 PM
 */
public class ItemAndAttributes {
    private SdbItem item;
    private List<Attribute> atts;

    public ItemAndAttributes(SdbItem item, List<Attribute> atts) {
        this.item = item;
        this.atts = atts;
    }

    public SdbItem getItem() {
        return item;
    }

    public void setItem(SdbItem item) {
        this.item = item;
    }

    public List<Attribute> getAtts() {
        return atts;
    }

    public void setAtts(List<Attribute> atts) {
        this.atts = atts;
    }
}
