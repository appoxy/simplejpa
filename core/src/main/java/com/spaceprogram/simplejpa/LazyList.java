package com.spaceprogram.simplejpa;

import java.io.Serializable;
import java.util.AbstractList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.PersistenceException;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.simpledb.model.Item;
import com.amazonaws.services.simpledb.model.SelectResult;
import com.spaceprogram.simplejpa.query.JPAQuery;
import com.spaceprogram.simplejpa.query.QueryImpl;

import com.spaceprogram.simplejpa.query.SimpleQuery;
import org.apache.commons.collections.list.GrowthList;

/**
 * Loads objects in the list on demand from SimpleDB. <p/> <p/> User: treeder Date: Feb 10, 2008 Time: 9:06:16 PM
 */
@SuppressWarnings("unchecked")
public class LazyList<E> extends AbstractList<E> implements Serializable {
    private static Logger logger = Logger.getLogger(LazyList.class.getName());

    private transient EntityManagerSimpleJPA em;
    private Class genericReturnType;
    private SimpleQuery origQuery;

    /**
     * Stores the actual objects for this list
     */
    private List<E> backingList;
    private String nextToken;
    private int count = -1;
    private String realQuery;
    private String domainName;
    private int maxResults = -1;
    private int maxResultsPerToken = SimpleQuery.MAX_RESULTS_PER_REQUEST;
    private boolean consistentRead = true;

    public LazyList(EntityManagerSimpleJPA em, Class tClass, SimpleQuery query) {
        this.em = em;
        this.genericReturnType = tClass;
        this.origQuery = query;
        this.maxResults = query.getMaxResults();
        this.consistentRead = query.isConsistentRead();
        AnnotationInfo ai = em.getAnnotationManager().getAnnotationInfo(genericReturnType);
        try {
            domainName = em.getDomainName(ai.getRootClass());
            if (domainName == null) {
                logger.warning("Domain does not exist for " + ai.getRootClass());
                backingList = new GrowthList(0);
            } else {
                // Do not include the limit in the query since will specify in loadAtLeastItems()
                realQuery = query.createAmazonQuery(false).getValue();
            }
        } catch (Exception e) {
            throw new PersistenceException(e);
        }
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public int size() {
        if (count > -1) return count;

        if (backingList != null && nextToken == null) {
            count = backingList.size();
        }
        return origQuery.getCount();
    }

    public int getFetchedSize() {
        return backingList == null ? 0 : backingList.size();
    }

    public void add(int index, E element) {
        backingList.add(index, element);
    }

    public E set(int index, E element) {
        return backingList.set(index, element);
    }

    public E remove(int index) {
        return backingList.remove(index);
    }

    public void setMaxResultsPerToken(int maxResultsPerToken) {
        // SimpleDB currently has a maximum limit of 2500
        this.maxResultsPerToken = Math.min(maxResultsPerToken, QueryImpl.MAX_RESULTS_PER_REQUEST);
    }

    public int getMaxResultsPerToken() {
        return maxResultsPerToken;
    }

    public E get(int i) {
        if (logger.isLoggable(Level.FINER))
            logger.finer("getting from lazy list at index=" + i);
        loadAtleastItems(i);
        return backingList.get(i);
    }

    private synchronized void loadAtleastItems(int index) {
        if ((backingList != null && nextToken == null) || (!noLimit() && index >= maxResults)) {
            return;
        }

        if (backingList == null) {
            backingList = new GrowthList();
        }

        while (backingList.size() <= index) {
            SelectResult qr;
            try {
                if (logger.isLoggable(Level.FINER))
                    logger.finer("query for lazylist=" + origQuery);

                int limit = maxResults - backingList.size();
                String limitQuery = realQuery + " limit " + (noLimit() ? maxResultsPerToken : Math.min(maxResultsPerToken, limit));
                if (em.getFactory().isPrintQueries())
                    System.out.println("query in lazylist=" + limitQuery);
                qr = DomainHelper.selectItems(this.em.getSimpleDb(), limitQuery, nextToken, isConsistentRead());

                if (logger.isLoggable(Level.FINER))
                    logger.finer("got items for lazylist=" + qr.getItems().size());

                for (Item item : qr.getItems()) {
                    backingList.add((E) em.buildObject(genericReturnType, item.getName(), item.getAttributes()));
                }

                if (qr.getNextToken() == null || (!noLimit() && qr.getItems().size() == limit)) {
                    nextToken = null;
                    break;
                }

                if (!noLimit() && qr.getItems().size() > limit) {
                    throw new PersistenceException("Got more results than the limit.");
                }

                nextToken = qr.getNextToken();
            } catch (AmazonClientException e) {
                throw new PersistenceException("Query failed: Domain=" + domainName + " -> " + origQuery, e);
            }
        }

    }

    private boolean noLimit() {
        return maxResults < 0;
    }

    @Override
    public Iterator<E> iterator() {
        return new LazyListIterator();
    }

    public void setConsistentRead(boolean consistentRead) {
        this.consistentRead = consistentRead;
    }

    public boolean isConsistentRead() {
        return consistentRead;
    }

    private class LazyListIterator implements Iterator<E> {
        private int iNext = 0;

        public boolean hasNext() {
            loadAtleastItems(iNext);
            return backingList.size() > iNext;
        }

        public E next() {
            return get(iNext++);
        }

        public void remove() {
            LazyList.this.remove(iNext - 1);
        }
    }
}