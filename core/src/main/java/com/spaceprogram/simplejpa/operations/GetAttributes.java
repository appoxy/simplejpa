package com.spaceprogram.simplejpa.operations;

import com.spaceprogram.simplejpa.ItemAndAttributes;
import com.spaceprogram.simplejpa.EntityManagerSimpleJPA;
import com.spaceprogram.simplejpa.SdbItem;

import java.util.concurrent.Callable;

/**
 * For our concurrent SimpleDB accesses.
 *
 * User: treeder
 * Date: Feb 8, 2008
 * Time: 7:55:30 PM
 */
public class GetAttributes implements Callable<ItemAndAttributes> {
    private SdbItem item;
    private EntityManagerSimpleJPA em;

    public GetAttributes(SdbItem item, EntityManagerSimpleJPA em) {
        this.item = item;
        this.em = em;
    }

    public ItemAndAttributes call() throws Exception {
        em.getTotalOpStats().incrementGets();
        return new ItemAndAttributes(item, item.getAttributes());
    }
}

