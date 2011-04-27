package com.spaceprogram.simplejpa.cache;

import java.util.Map;

/**
 * User: treeder
 * Date: Aug 2, 2009
 * Time: 7:32:41 PM
 */
public class KittyCacheFactory implements CacheFactory {

    public static KittyCacheWrapper singletonCache = new KittyCacheWrapper(10000);

    public void init(Map properties) { }

    /**
     * Bad naming due to ehcache stuff. Should be called getCache.
     * @param name
     * @return
     */
    public Cache createCache(String name)  {
        return singletonCache;
    }

    public Cache getCache(String name) {
        return singletonCache;
    }

    public void shutdown() { }
}
