package com.spaceprogram.simplejpa.query;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.simpledb.model.Attribute;
import com.amazonaws.services.simpledb.model.Item;
import com.amazonaws.services.simpledb.model.NoSuchDomainException;
import com.amazonaws.services.simpledb.model.SelectResult;
import com.spaceprogram.simplejpa.DomainHelper;
import com.spaceprogram.simplejpa.EntityManagerSimpleJPA;
import com.spaceprogram.simplejpa.LazyList;
import com.spaceprogram.simplejpa.PersistentProperty;
import com.spaceprogram.simplejpa.util.AmazonSimpleDBUtil;
import com.spaceprogram.simplejpa.util.EscapeUtils;
import org.apache.commons.lang.NotImplementedException;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.*;

/**
 * Kerry Wright
 */
public abstract class AbstractQuery implements SimpleQuery {
    protected final EntityManagerSimpleJPA em;
    protected final Map<String, Object> parameters = new HashMap<String, Object>();
    protected int maxResults = -1;
    protected Class tClass;
    protected boolean consistentRead = false;

    public AbstractQuery(EntityManagerSimpleJPA em) {
        this.em = em;
    }

    public int getMaxResults() {
        return maxResults;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    protected String getParamValueAsStringForAmazonQuery(String param, PersistentProperty property) {
        String paramName = paramName(param);
        if (paramName == null) {
            // no colon, so just a value
            return param;
        }
        Object paramOb = parameters.get(paramName);
        if (paramOb == null) {
            throw new PersistenceException("parameter is null for: " + paramName);
        }
        if (property.isForeignKeyRelationship()) {
            String id2 = em.getId(paramOb);
            param = EscapeUtils.escapeQueryParam(id2);
        } else {
            Class retType = property.getPropertyClass();
            param = convertToSimpleDBValue(paramOb, retType);
        }
        return "'" + param + "'";
    }

    protected String convertToSimpleDBValue(Object paramOb, Class retType) {
        String param;
        if (Integer.class.isAssignableFrom(retType)) {
            Integer x = (Integer) paramOb;
            param = AmazonSimpleDBUtil.encodeRealNumberRange(new BigDecimal(x), AmazonSimpleDBUtil.LONG_DIGITS, EntityManagerSimpleJPA.OFFSET_VALUE)
                    .toString();
        } else if (Long.class.isAssignableFrom(retType)) {
            Long x = (Long) paramOb;
            param = AmazonSimpleDBUtil.encodeRealNumberRange(new BigDecimal(x), AmazonSimpleDBUtil.LONG_DIGITS, EntityManagerSimpleJPA.OFFSET_VALUE)
                    .toString();
        } else if (Double.class.isAssignableFrom(retType)) {
            Double x = (Double) paramOb;
            if (!x.isInfinite() && !x.isNaN()) {
                param = AmazonSimpleDBUtil.encodeRealNumberRange(new BigDecimal(x), AmazonSimpleDBUtil.LONG_DIGITS, AmazonSimpleDBUtil.LONG_DIGITS,
                        EntityManagerSimpleJPA.OFFSET_VALUE).toString();
            } else {
                param = x.toString();
            }
        } else if (BigDecimal.class.isAssignableFrom(retType)) {
            BigDecimal x = (BigDecimal) paramOb;
            param = AmazonSimpleDBUtil.encodeRealNumberRange(x, AmazonSimpleDBUtil.LONG_DIGITS, AmazonSimpleDBUtil.LONG_DIGITS,
                    EntityManagerSimpleJPA.OFFSET_VALUE).toString();
        } else if (Date.class.isAssignableFrom(retType)) {
            Date x = (Date) paramOb;
            param = AmazonSimpleDBUtil.encodeDate(x);
        } else { // string
            param = EscapeUtils.escapeQueryParam(paramOb.toString());
            //amazon now supports like queries starting with %
        }
        return param;
    }

    protected String paramName(String param) {
        int colon = param.indexOf(":");
        if (colon == -1) {
            return null;
        }
        String paramName = param.substring(colon + 1);
        return paramName;
    }

    public SimpleQuery setConsistentRead(boolean consistentRead) {
        this.consistentRead = consistentRead;
        return this;
    }

    public Query setFirstResult(int i) {
        throw new NotImplementedException("TODO");
    }

    public Query setFlushMode(FlushModeType flushModeType) {
        throw new NotImplementedException("TODO");
    }

    public Query setHint(String s, Object o) {
        throw new NotImplementedException("TODO");
    }

    public Query setMaxResults(int maxResults) {
        this.maxResults = maxResults;
        return this;
    }

    public Query setParameter(String s, Calendar calendar, TemporalType temporalType) {
        throw new NotImplementedException("TODO");
    }

    public Query setParameter(String s, Date date, TemporalType temporalType) {
        throw new NotImplementedException("TODO");
    }

    public Query setParameter(String s, Object o) {
        parameters.put(s, o);
        return this;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters.clear();
        this.parameters.putAll(parameters);
    }

    public Query setParameter(int i, Calendar calendar, TemporalType temporalType) {
        throw new NotImplementedException("TODO");
    }

    public Query setParameter(int i, Date date, TemporalType temporalType) {
        throw new NotImplementedException("TODO");
    }

    public Query setParameter(int i, Object o) {
        throw new NotImplementedException("TODO");
    }

    public boolean isConsistentRead() {
        return consistentRead;
    }

    public AmazonQueryString createAmazonQuery() throws NoResultsException, AmazonClientException {
        return createAmazonQuery(true);
    }

    public List getResultList() {

        // convert to amazon query
        AmazonQueryString amazonQuery;
        try {
            amazonQuery = createAmazonQuery();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if (amazonQuery == null) {
            return new ArrayList();
        }

        try {

// String qToSend = amazonQuery != null ? amazonQuery.toString() : null;
            em.incrementQueryCount();
            if (amazonQuery.isCount()) {
// String domainName = em.getDomainName(tClass);
                String nextToken = null;
                SelectResult qr;
                long count = 0;

                while ((qr = DomainHelper.selectItems(this.em.getSimpleDb(), amazonQuery.getValue(), nextToken)) != null) {
                    Map<String, List<Attribute>> itemMap = new HashMap<String, List<Attribute>>();
                    for (Item item : qr.getItems()) {
                        itemMap.put(item.getName(), item.getAttributes());
                    }

                    for (String id : itemMap.keySet()) {
                        List<Attribute> list = itemMap.get(id);
                        for (Attribute itemAttribute : list) {
                            if (itemAttribute.getName().equals("Count")) {
                                count += Long.parseLong(itemAttribute.getValue());
                            }
                        }
                    }
                    nextToken = qr.getNextToken();
                    if (nextToken == null) {
                        break;
                    }
                }
                return Arrays.asList(count);
            } else {
                LazyList ret = new LazyList(em, tClass, this);
                return ret;
            }
        } catch (NoSuchDomainException e) {
            return new ArrayList(); // no need to throw here
        } catch (Exception e) {
            throw new PersistenceException(e);
        }
    }

    public Object getSingleResult() {
        List<?> resultList = getResultList();
        if (resultList instanceof LazyList<?>) {
            ((LazyList<?>) resultList).setMaxResultsPerToken(2);
        }
        Iterator<?> itr = resultList.iterator();
        if (!itr.hasNext()) {
            throw new NoResultException();
        }
        Object obj = itr.next();
        if (itr.hasNext()) {
            throw new NonUniqueResultException();
        }
        return obj;
    }

    public Object getSingleResultNoThrow() {
        List<?> resultList = getResultList();
        if (resultList instanceof LazyList<?>) {
            ((LazyList<?>) resultList).setMaxResultsPerToken(1);
        }
        Iterator<?> itr = resultList.iterator();
        if (itr.hasNext()) {
            return itr.next();
        }
        return null;
    }

    public int executeUpdate() {
        throw new NotImplementedException("TODO");
    }
}
