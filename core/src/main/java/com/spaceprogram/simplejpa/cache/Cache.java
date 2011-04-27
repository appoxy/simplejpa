package com.spaceprogram.simplejpa.cache;

import java.util.Map;
import java.util.Collection;

/**
 * User: treeder
 * Date: Aug 2, 2009
 * Time: 7:07:41 PM
 */
public interface Cache {
    int size();

    Object getObj(Object o);

    void put(Object o, Object o1);

    boolean remove(Object o);

    void clear();
}
