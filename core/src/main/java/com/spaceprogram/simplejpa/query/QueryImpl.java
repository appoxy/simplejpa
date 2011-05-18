package com.spaceprogram.simplejpa.query;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.FlushModeType;
import javax.persistence.ManyToOne;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import javax.persistence.TemporalType;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.simpledb.model.Attribute;
import com.amazonaws.services.simpledb.model.Item;
import com.amazonaws.services.simpledb.model.NoSuchDomainException;
import com.amazonaws.services.simpledb.model.SelectResult;
import com.spaceprogram.simplejpa.AnnotationInfo;
import com.spaceprogram.simplejpa.DomainHelper;
import com.spaceprogram.simplejpa.EntityManagerFactoryImpl;
import com.spaceprogram.simplejpa.EntityManagerSimpleJPA;
import com.spaceprogram.simplejpa.LazyList;
import com.spaceprogram.simplejpa.NamingHelper;
import com.spaceprogram.simplejpa.util.AmazonSimpleDBUtil;
import com.spaceprogram.simplejpa.util.EscapeUtils;

import org.apache.commons.lang.NotImplementedException;

/**
 * Need to support the following: <p/> <p/> - Navigation operator (.) DONE - Arithmetic operators: +, - unary *, / multiplication and division +, - addition and subtraction -
 * Comparison operators : =, >, >=, <, <=, <> (not equal), [NOT] BETWEEN, [NOT] LIKE, [NOT] IN, IS [NOT] NULL, IS [NOT] EMPTY, [NOT] MEMBER [OF] - Logical operators: NOT AND OR
 * <p/> see: http://docs.solarmetric.com/full/html/ejb3_langref.html#ejb3_langref_where <p/> User: treeder Date: Feb 8, 2008 Time: 7:33:20 PM
 */
public class QueryImpl implements SimpleQuery {
    public static final int MAX_RESULTS_PER_REQUEST = 2500;

    private static Logger logger = Logger.getLogger(QueryImpl.class.getName());

    public static List<String> tokenizeWhere(String where) {
        List<String> split = new ArrayList<String>();
        Pattern pattern = Pattern.compile(conditionRegex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(where);
        int lastIndex = 0;
        String s;
        int i = 0;
        while (matcher.find()) {
            s = where.substring(lastIndex, matcher.start()).trim();
            logger.finest("value: " + s);
            split.add(s);
            s = matcher.group();
            split.add(s);
            logger.finest("matcher found: " + s + " at " + matcher.start() + " to " + matcher.end());
            lastIndex = matcher.end();
            i++;
        }
        s = where.substring(lastIndex).trim();
        logger.finest("final:" + s);
        split.add(s);
        return split;
    }

    private EntityManagerSimpleJPA em;
    private JPAQuery q;

    private Map<String, Object> parameters = new HashMap<String, Object>();
    public static String conditionRegex = "(<>)|(>=)|(<=)|=|>|<|\\band\\b|\\bor\\b|\\bis\\b|\\blike\\b";
    private int maxResults = -1;
    private String qString;
    private Class tClass;
    private boolean consistentRead = true;

    // private AmazonQueryString amazonQuery;
    private Map<String, List<String>> foreignIds = new HashMap();

    public QueryImpl(EntityManagerSimpleJPA em, JPAQuery q) {
        this.em = em;
        this.q = q;
        this.qString = q.toString();
        init(em);
    }

    public QueryImpl(EntityManagerSimpleJPA em, String qString) {
        this.em = em;
        this.qString = qString;
        logger.fine("query=" + qString);
        this.q = new JPAQuery();
        JPAQueryParser parser = new JPAQueryParser(q, qString);
        parser.parse();
        init(em);
    }

    private Boolean appendCondition(Class tClass, StringBuilder sb, String field, String comparator, String param) {
        comparator = comparator.toLowerCase();
        AnnotationInfo ai = em.getAnnotationManager().getAnnotationInfo(tClass);

        String fieldSplit[] = field.split("\\.");
        if (fieldSplit.length == 1) {
            field = fieldSplit[0];
// System.out.println("split: " + field + " param=" + param);
            if (field.equals(param)) {
                return false;
            }
        } else if (fieldSplit.length == 2) {
            field = fieldSplit[1];
        } else if (fieldSplit.length == 3) {
            // NOTE: ONLY SUPPORTING SECOND LEVEL OF GRAPH RIGHT NOW
            // then we have to reach down the graph here. eg: myOb.ob2.name or myOb.ob2.id
            // if filtering by id, then don't need to query for second object, just add a filter on the id field
            String refObjectField = fieldSplit[1];
            field = fieldSplit[2];
// System.out.println("field=" + field);
            Method getterForReference = ai.getGetter(refObjectField);
            Class refType = getterForReference.getReturnType();
            AnnotationInfo refAi = em.getAnnotationManager().getAnnotationInfo(refType);
            Method getterForField = refAi.getGetter(field);
// System.out.println("getter=" + getterForField);
            String paramValue = getParamValueAsStringForAmazonQuery(param, getterForField);
            logger.finest("paramValue=" + paramValue);
            Method refIdMethod = refAi.getIdMethod();
            if (NamingHelper.attributeName(refIdMethod).equals(field)) {
                logger.finer("Querying using id field, no second query required.");
                appendFilter(sb, NamingHelper.foreignKey(refObjectField), comparator, paramValue);
            } else {
                // no id method, so query for other object(s) first, then apply the returned value to the original query.
                // todo: this needs some work (multiple ref objects? multiple params on same ref object?)
                List<String> ids = foreignIds.get(field);
// System.out.println("got foreign ids=" + ids);
                if (ids == null) {
                    Query sub = em.createQuery("select o from " + refType.getName() + " o where o." + field + " " + comparator + " :paramValue");
                    sub.setParameter("paramValue", parameters.get(paramName(param)));
                    List subResults = sub.getResultList();
                    ids = new ArrayList<String>();
                    for (Object subResult : subResults) {
                        ids.add(em.getId(subResult));
                    }
                    foreignIds.put(field, ids); // Store the ids for next use, really reduces queries when using this repetitively
                }
                if (ids.size() > 0) {
                    appendIn(sb, NamingHelper.foreignKey(refObjectField), ids);
                } else {
                    // no matches so should return nothing right? only if an AND query I guess
                    return null;
                }
            }
            return true;
        } else {
            throw new PersistenceException("Invalid field used in query: " + field);
        }
        logger.finest("field=" + field);
// System.out.println("field=" + field + " paramValue=" + param);
        Method getterForField = ai.getGetter(field);
        if (getterForField == null) {
            throw new PersistenceException("No getter for field: " + field);
        }
        String columnName = NamingHelper.getColumnName(getterForField);
        if (columnName == null) {
            columnName = field;
        }
        if (comparator.equals("is")) {
            if (param.equalsIgnoreCase("null")) {
                sb.append(columnName).append(" is null");
// appendFilter(sb, true, columnName, "starts-with", "");
            } else if (param.equalsIgnoreCase("not null")) {
                sb.append(columnName).append(" is not null");
// appendFilter(sb, false, columnName, "starts-with", "");
            } else {
                throw new PersistenceException("Must use only 'is null' or 'is not null' with where condition containing 'is'");
            }
        } else if (comparator.equals("like")) {
            comparator = "like";
            String paramValue = getParamValueAsStringForAmazonQuery(param, getterForField);
// System.out.println("param=" + paramValue + "___");
// paramValue = paramValue.endsWith("%") ? paramValue.substring(0, paramValue.length() - 1) : paramValue;
// System.out.println("param=" + paramValue + "___");
// param = param.startsWith("%") ? param.substring(1) : param;
            appendFilter(sb, columnName, comparator, paramValue);
        } else {
            String paramValue = getParamValueAsStringForAmazonQuery(param, getterForField);
            logger.finer("paramValue=" + paramValue);
            logger.finer("comp=[" + comparator + "]");
            appendFilter(sb, columnName, comparator, paramValue);
        }
        return true;
    }

    private void appendFilter(StringBuilder sb, boolean not, String field, String comparator, String param, boolean quoteParam) {
        if (not) {
            sb.append("not ");
        }
        boolean quoteField = !NamingHelper.NAME_FIELD_REF.equals(field);
        if (quoteField) {
            sb.append("`");
        }
        sb.append(field);
        if (quoteField) {
            sb.append("`");
        }
        sb.append(" ");
        sb.append(comparator);
        sb.append(" ");
        if (quoteParam) {
            sb.append("'");
        }
        sb.append(param);
        if (quoteParam) {
            sb.append("'");
        }
    }

    private void appendFilter(StringBuilder sb, String field, String comparator, String param) {
        appendFilter(sb, false, field, comparator, param, false);
    }

    /*
     * public StringBuilder toAmazonQuery(){ return toAmazonQuery( }
     */

    private void appendFilterMultiple(StringBuilder sb, String field, String comparator, List params) {
        int count = 0;
        for (Object param : params) {
            if (count > 0) {
                sb.append(" and ");
            }
            sb.append(field);
            sb.append(comparator).append(" '").append(param).append("'");
            count++;
        }
    }

    private void appendIn(StringBuilder sb, String field, List<String> params) {
        sb.append("`").append(field).append("`");
        sb.append(" ");
        sb.append("IN");
        sb.append(" (");
        for(int i = 0; i < params.size(); i++){
            if(i != 0){
                sb.append(",");
            }
            sb.append("'").append(params.get(i)).append("'");
        }
        sb.append(")");
    }

    public AmazonQueryString createAmazonQuery() throws NoResultsException, AmazonClientException {
        return createAmazonQuery(true);
    }

    public AmazonQueryString createAmazonQuery(boolean appendLimit) throws NoResultsException, AmazonClientException {
        String select = q.getResult();
        boolean count = false;
        if (select != null && select.contains("count")) {
// System.out.println("HAS COUNT: " + select);
            count = true;
        }
        AnnotationInfo ai = em.getAnnotationManager().getAnnotationInfo(tClass);

        // Make sure querying the root Entity class
        String domainName = em.getDomainName(ai.getRootClass());
        if (domainName == null) {
            return null;
// throw new NoResultsException();
        }
        StringBuilder amazonQuery;
        if (q.getFilter() != null) {
            amazonQuery = toAmazonQuery(tClass, q);
            if (amazonQuery == null) {
// throw new NoResultsException();
                return null;
            }
        } else {
            amazonQuery = new StringBuilder();
        }
        if (ai.getDiscriminatorValue() != null) {
            if (amazonQuery.length() == 0) {
                amazonQuery = new StringBuilder();
            } else {
                amazonQuery.append(" and ");
            }
            appendFilter(amazonQuery, EntityManagerFactoryImpl.DTYPE, "=", "'" + ai.getDiscriminatorValue() + "'");
        }

        // now for sorting
        String orderBy = q.getOrdering();
        if (orderBy != null && orderBy.length() > 0) {
// amazonQuery.append(" sort ");
            amazonQuery.append(" order by ");
            String orderByOrder = "asc";
            String orderBySplit[] = orderBy.split(" ");
            if (orderBySplit.length > 2) {
                throw new PersistenceException("Can only sort on a single attribute in SimpleDB. Your order by is: " + orderBy);
            }
            if (orderBySplit.length == 2) {
                orderByOrder = orderBySplit[1];
            }
            String orderByAttribute = orderBySplit[0];
            String fieldSplit[] = orderByAttribute.split("\\.");
            if (fieldSplit.length == 1) {
                orderByAttribute = fieldSplit[0];
            } else if (fieldSplit.length == 2) {
                orderByAttribute = fieldSplit[1];
            }
// amazonQuery.append("'");
            amazonQuery.append(orderByAttribute);
// amazonQuery.append("'");
            amazonQuery.append(" ").append(orderByOrder);
        }
        StringBuilder fullQuery = new StringBuilder();
        fullQuery.append("select ");
        fullQuery.append(count ? "count(*)" : "*");
        fullQuery.append(" from `").append(domainName).append("` ");
        if (amazonQuery.length() > 0) {
            fullQuery.append("where ");
            fullQuery.append(amazonQuery);
        }
        String logString = "amazonQuery: Domain=" + domainName + ", query=" + fullQuery;
        logger.fine(logString);
        if (em.getFactory().isPrintQueries()) {
            System.out.println(logString);
        }

        if (!count && appendLimit && maxResults >= 0) {
            fullQuery.append(" limit ").append(Math.min(MAX_RESULTS_PER_REQUEST, maxResults));
        }
        return new AmazonQueryString(fullQuery.toString(), count);
    }

    public int executeUpdate() {
        throw new NotImplementedException("TODO");
    }

    public Map<String, List<String>> getForeignIds() {
        return foreignIds;
    }

    public int getMaxResults() {
        return maxResults;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    private String getParamValueAsStringForAmazonQuery(String param, Method getter) {
        String paramName = paramName(param);
        if (paramName == null) {
            // no colon, so just a value
            return param;
        }
        Object paramOb = parameters.get(paramName);
        if (paramOb == null) {
            throw new PersistenceException("parameter is null for: " + paramName);
        }
        if (getter.getAnnotation(ManyToOne.class) != null) {
            String id2 = em.getId(paramOb);
            param = EscapeUtils.escapeQueryParam(id2);
        } else {
            Class retType = getter.getReturnType();
            if (Integer.class.isAssignableFrom(retType)) {
                Integer x = (Integer) paramOb;
                param = AmazonSimpleDBUtil.encodeRealNumberRange(new BigDecimal(x), AmazonSimpleDBUtil.LONG_DIGITS, EntityManagerSimpleJPA.OFFSET_VALUE)
                        .toString();
                logger.finer("encoded int " + x + " to " + param);
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
        }
        return "'" + param + "'";
    }

    public JPAQuery getQ() {
        return q;
    }

    public String getQString() {
        return qString;
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

    private void init(EntityManagerSimpleJPA em) {

        String from = q.getFrom();
        logger.finer("from=" + from);
        logger.finer("where=" + q.getFilter());
        if (q.getOrdering() != null && q.getFilter() == null) {
            throw new PersistenceException("Attribute in ORDER BY [" + q.getOrdering() + "] must be included in a WHERE filter.");
        }

        String split[] = q.getFrom().split(" ");
        String obClass = split[0];
        tClass = em.ensureClassIsEntity(obClass);
        consistentRead = em.isConsistentRead();
    }

    public boolean isConsistentRead() {
        return consistentRead;
    }

    private String paramName(String param) {
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

    public void setForeignIds(Map<String, List<String>> foreignIds) {
        this.foreignIds = foreignIds;
    }

    public Query setHint(String s, Object o) {
        throw new NotImplementedException("TODO");
    }

    public Query setMaxResults(int maxResults) {
        this.maxResults = maxResults;
        return this;
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

    /*
     * public AmazonQueryString getAmazonQuery() { return amazonQuery; } public void setAmazonQuery(AmazonQueryString amazonQuery) { this.amazonQuery = amazonQuery; }
     */

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    public void setQ(JPAQuery q) {
        this.q = q;
    }

    public void setQString(String qString) {
        this.qString = qString;
    }

    public StringBuilder toAmazonQuery(Class tClass, JPAQuery q) {
        StringBuilder sb = new StringBuilder();
        String where = q.getFilter();
        where = where.trim();
        // now split it into pieces
        List<String> whereTokens = tokenizeWhere(where);
        Boolean aok = false;
        for (int i = 0; i < whereTokens.size();) {
            if (aok && i > 0) {
                String andOr = whereTokens.get(i);
                if (andOr.equalsIgnoreCase("OR")) {
                    sb.append(" or ");
                } else {
                    sb.append(" and ");
                }
            }
            if (i > 0) {
                i++;
            }
// System.out.println("sbbefore=" + sb);
            // special null cases: is null and is not null
            String firstParam = whereTokens.get(i);
            i++;
            String secondParam = whereTokens.get(i);
            i++;
            String thirdParam = whereTokens.get(i);
            if (thirdParam.equalsIgnoreCase("not")) {
                i++;
                thirdParam += " " + whereTokens.get(i);
            }
            i++;
            aok = appendCondition(tClass, sb, firstParam, secondParam, thirdParam);
// System.out.println("sbafter=" + sb);
            if (aok == null) {
                return null; // todo: only return null if it's an AND query, or's should still continue, but skip the intersection part
            }
        }

        logger.fine("query=" + sb);
        return sb;
    }

    @Override
    public String toString() {
        return "QueryImpl{" + "em=" + em + ", q=" + q + ", parameters=" + parameters + ", maxResults=" + maxResults + ", qString='" + qString + '\'' + '}';
    }

}
