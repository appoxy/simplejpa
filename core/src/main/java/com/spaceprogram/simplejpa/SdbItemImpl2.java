package com.spaceprogram.simplejpa;




import javax.persistence.PersistenceException;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.simpledb.model.Attribute;
import com.amazonaws.services.simpledb.model.Item;

import java.util.List;

/**
 * User: treeder
 * Date: Mar 8, 2009
 * Time: 10:42:36 PM
 */
public class SdbItemImpl2 implements SdbItem {
    private Item item;

    public SdbItemImpl2(Item item) {
        this.item = item;
    }

    public String getIdentifier() {
        return item.getName();
    }

    public List<Attribute> getAttributes() {
        return item.getAttributes();
    }
}
