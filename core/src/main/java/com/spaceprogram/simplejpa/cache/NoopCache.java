package com.spaceprogram.simplejpa.cache;

import java.util.Set;
import java.util.Map;
import java.util.Collection;

/**
 * User: treeder
 * Date: Jun 7, 2008
 * Time: 4:47:01 PM
 */
public class NoopCache implements Cache {
    public boolean containsKey(Object o) {
        return false;
    }

    public boolean containsValue(Object o) {
        return false;
    }

    public Set entrySet() {
        return null;
    }

    public boolean isEmpty() {
        return false;
    }

    public Set keySet() {
        return null;
    }

    public void putAll(Map map) {

    }

    public int size() {
        return 0;
    }

    public Collection values() {
        return null;
    }

    public Object getObj(Object o) {
        return null;
    }

    public Map getAll(Collection collection) {
        return null;
    }

    public void put(Object o, Object o1) {

    }

    public boolean remove(Object o) {
        return true;
    }

    public void clear() {

    }

}
