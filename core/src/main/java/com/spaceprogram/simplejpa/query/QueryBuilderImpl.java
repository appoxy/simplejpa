package com.spaceprogram.simplejpa.query;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * User: treeder
 * Date: Nov 7, 2008
 * Time: 5:31:27 PM
 */
public class QueryBuilderImpl implements QueryBuilder {
    private static Logger logger = Logger.getLogger(QueryBuilderImpl.class.getName());
    private StringBuffer sb = new StringBuffer();
    private Map params = new HashMap();
    
    public QueryBuilderImpl append(String s) {
        sb.append(" ").append(s);
        return this;
    }

    /**
     *
     *
     *
     * @param s This is the piece of the query to append, for example: and "o.someField3 like :x"
     * @param parameterName the name of the param to replace, for example: "x"
     * @param parameterValue the value for "x"
     */
    public QueryBuilderImpl append(String s, String parameterName, Object parameterValue) {
        append(s);
        set(parameterName, parameterValue);
        return this;
    }

    public QueryBuilderImpl set(String parameterName, Object parameterValue){
        params.put(parameterName, parameterValue);
        return this;
    }

    public Query makeQuery(EntityManager em) {
        Query q = em.createQuery(sb.toString());
        for (Object o : getParams().entrySet()) {
            Map.Entry entry = (Map.Entry) o;
            logger.finer("entry: " + entry.getKey() + ", " + entry.getValue());
            if (entry.getValue() instanceof Collection) {
                /*
                 do we handle this?
                HibernateQuery hq = (HibernateQuery) q;
                hq.getHibernateQuery().setParameterList((String) (entry.getKey()), (Collection) entry.getValue());*/
            } else {
                q.setParameter((String) entry.getKey(), entry.getValue());
            }
        }
        return q;
    }


    public Map getParams() {
        return params;
    }
}
