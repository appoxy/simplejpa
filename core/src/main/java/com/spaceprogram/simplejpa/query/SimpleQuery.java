package com.spaceprogram.simplejpa.query;

import javax.persistence.Query;

/**
 * User: treeder Date: Nov 2, 2008 Time: 12:50:20 AM
 */
public interface SimpleQuery extends Query {

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
}
