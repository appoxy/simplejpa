package com.spaceprogram.simplejpa.util;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.simpledb.AmazonSimpleDB;
import com.amazonaws.services.simpledb.model.Attribute;
import com.amazonaws.services.simpledb.model.Item;
import com.amazonaws.services.simpledb.model.SelectRequest;
import com.amazonaws.services.simpledb.model.SelectResult;
import com.spaceprogram.simplejpa.DomainHelper;
import com.spaceprogram.simplejpa.EntityManagerSimpleJPA;
import com.spaceprogram.simplejpa.ItemAndAttributes;
import com.spaceprogram.simplejpa.SdbItem;
import com.spaceprogram.simplejpa.SdbItemImpl2;
import com.spaceprogram.simplejpa.operations.GetAttributes;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorCompletionService;

/**
 * User: treeder
 * Date: May 14, 2008
 * Time: 3:11:34 PM
 */
public class ConcurrentRetriever {
    private static boolean parallel = true;

    public static List<ItemAndAttributes> test(EntityManagerSimpleJPA em, String domainName) throws AmazonClientException, ExecutionException, InterruptedException {
    	AmazonSimpleDB db = em.getSimpleDb();
    	SelectResult result = DomainHelper.selectItems(db, "select * from `" + domainName + "`", null);

    	List<SdbItem> list = new ArrayList<SdbItem>();
        for (Item item : result.getItems()) {
            list.add(new SdbItemImpl2(item));
        }
        return getAttributesFromSdb(list, em.getExecutor(), em);
    }

    public static List<ItemAndAttributes> getAttributesFromSdb(List<SdbItem> items, Executor executor, EntityManagerSimpleJPA em) throws AmazonClientException, InterruptedException, ExecutionException {
        if (parallel) {
            return getParallel(items, executor, em);
        } else {
            return getSerially(items);
        }
    }

    private static List<ItemAndAttributes> getParallel(List<SdbItem> items, Executor executor, EntityManagerSimpleJPA em) throws InterruptedException, ExecutionException {
        CompletionService<ItemAndAttributes> ecs = new ExecutorCompletionService<ItemAndAttributes>(executor);
        for (SdbItem item : items) {
            Callable callable = new GetAttributes(item, em);
            ecs.submit(callable);
        }
        List<ItemAndAttributes> ret = new ArrayList<ItemAndAttributes>();
        int n = items.size();
        for (int i = 0; i < n; ++i) {
            ItemAndAttributes r = ecs.take().get();
            if (r != null) {
                ret.add(r);
            }
        }
        return ret;
    }

    private static List<ItemAndAttributes> getSerially(List<SdbItem> items) throws AmazonClientException {
        List<ItemAndAttributes> ret = new ArrayList<ItemAndAttributes>();
        for (SdbItem item : items) {
//            logger.fine("item=" + item.getIdentifier());
            List<Attribute> atts = item.getAttributes();
            ret.add(new ItemAndAttributes(item, atts));
        }
        return ret;
    }
}
