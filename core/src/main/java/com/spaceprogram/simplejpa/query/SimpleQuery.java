package com.spaceprogram.simplejpa.query;

import javax.persistence.Query;

/**
 * User: treeder Date: Nov 2, 2008 Time: 12:50:20 AM
 */
public interface SimpleQuery extends Query {

    int MAX_RESULTS_PER_REQUEST = 2500;

    /**
     * Same as getSingleResult, but does not throw NonUniqueResultException or NoResultException
     * @return first result or null if no results.
     */
    Object getSingleResultNoThrow();

    /**
     * Specify if SimpleDB should use a consistent read, or a eventual consistency read with query
     * @param consistentRead true if consistent read, false if an eventual consistency read
     * @return this query
     */
    public SimpleQuery setConsistentRead(boolean consistentRead);

    /**
     * Whether SimpleDB should use a consistent read, or a eventual consistency read with query
     */
    public boolean isConsistentRead();

    /**
     * @return the number of records that this query is going to return
     */
    public int getCount();

    /**
     * @return the maximum number of records that could be returned by this query
     */
    public int getMaxResults();

    /**
     * @param appendLimit whether to append a max number of records limit to the query when building it
     * @return a representation of the SimpleDB specific query string
     */
    public AmazonQueryString createAmazonQuery(boolean appendLimit);
}
