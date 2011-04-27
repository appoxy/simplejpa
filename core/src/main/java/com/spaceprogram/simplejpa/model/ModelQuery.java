package com.spaceprogram.simplejpa.model;

import java.util.List;

/**
 * query = Story.all()

query.filter('title =', 'Foo')
query.order('-date')
query.ancestor(key)

# These methods can be chained together on one line.
query.filter('title =', 'Foo').order('-date').ancestor(key)

 * User: treeder
 * Date: Oct 18, 2008
 * Time: 4:32:59 PM
 */
public interface ModelQuery {
    /**
     *
     * @param attribute eg: "title"
     * @param comparison eg: "="
     * @param value eg: "some title"
     * @return
     */
    ModelQuery filter(String attribute, String comparison, String value);

    /**
     *
     * @param attribute eg: "dateCreated"
     * @param direction eg: "asc" or "desc"
     * @return
     */
    ModelQuery order(String attribute, String direction);

    /**
     *
     * @return result list just like Query.getResultList()
     */
    List getResultList();
}
