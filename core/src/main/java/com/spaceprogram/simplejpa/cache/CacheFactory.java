package com.spaceprogram.simplejpa.cache;

import java.util.Map;

/**
 * The JCache CacheFactory is pretty limited so added this one.
 *
 * User: treeder
 * Date: Jun 7, 2008
 * Time: 4:56:15 PM
 */
public interface CacheFactory {

    /**
     * Called once to load up the CacheManager.
     *
     * @param properties
     */
    void init(Map properties);

    Cache createCache(String name);

    void shutdown();
//
//    void clearAll();

//    CacheManager getCacheManager();

    /**
     * This allows you to add a cache listener that will be applied to every Cache that is created from this point on.
     * @param cacheEventListener
     */
//    void addDefaultListener(CacheEventListener cacheEventListener);

}
