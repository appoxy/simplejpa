package com.spaceprogram.simplejpa.cache;


import java.util.Map;

/**
 * User: treeder
 * Date: Jun 7, 2008
 * Time: 4:46:11 PM
 */
public class NoopCacheFactory implements CacheFactory {
    private Cache noopCache = new NoopCache();

    public void init(Map properties)  {

    }

    public Cache createCache(String name) {
        return noopCache;
    }

    public void shutdown() {

    }

    public void clearAll() {

    }

    public Cache createCache(Map map)  {
        return noopCache;
    }
}
