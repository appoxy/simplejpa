package com.spaceprogram.simplejpa.cache;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Cache;
import net.sf.ehcache.event.CacheEventListener;
import net.sf.ehcache.util.ClassLoaderUtil;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * User: treeder
 * Date: Jun 7, 2008
 * Time: 3:55:32 PM
 */
// would extend EhCacheProvider, but it's final for some reason??
public class EhCacheFactory implements CacheFactory{

    private static Logger log = Logger.getLogger(EhCacheFactory.class.getName());

    private CacheManager manager;
    private static final String NET_SF_EHCACHE_CONFIGURATION_RESOURCE_NAME = "net.sf.ehcache.configurationResourceName";
    private boolean initializing;
    private List<CacheEventListener> listeners = new ArrayList<CacheEventListener>();

    public synchronized void init(Map properties) {
        if (manager != null) {
            log.warning("Attempt to restart an already started CacheFactory. Using previously created EhCacheFactory.");
            return;
        }
        initializing = true;
        try {
            String configurationResourceName = null;
            if (properties != null) {
                configurationResourceName = (String) properties.get(NET_SF_EHCACHE_CONFIGURATION_RESOURCE_NAME);
            }
            if (configurationResourceName == null || configurationResourceName.length() == 0) {
                manager = new CacheManager();
            } else {
                if (!configurationResourceName.startsWith("/")) {
                    configurationResourceName = "/" + configurationResourceName;
                    if (log.isLoggable(Level.FINE)) {
                        log.fine("prepending / to " + configurationResourceName + ". It should be placed in the root"
                                + "of the classpath rather than in a package.");
                    }
                }
                URL url = loadResource(configurationResourceName);
                manager = new CacheManager(url);
            }
        } catch (net.sf.ehcache.CacheException e) {
            if (e.getMessage().startsWith("Cannot parseConfiguration CacheManager. Attempt to create a new instance of " +
                    "CacheManager using the diskStorePath")) {
                throw new CacheException("Could not init EhCacheFactory.", e);
            } else {
                throw e;
            }
        } finally {
            initializing = false;
        }

    }


    public Cache createCache(Map env) {
        throw new UnsupportedOperationException("Use createCache(String name) instead.");
    }

    public synchronized EhcacheWrapper createCache(String name) {
        if (manager == null) {
            throw new CacheException("CacheFactory was not initialized. Call init() before creating a cache.");
        }
        try {
            Cache cache = manager.getCache(name);
            if (cache == null) {
                log.warning("Could not find a specific ehcache configuration for cache named [" + name + "]; using defaults.");
                manager.addCache(name);
                cache = manager.getCache(name);
            }
            Ehcache backingCache = cache;
            if (!backingCache.getCacheEventNotificationService().hasCacheEventListeners()) {
                if (listeners.size() > 0) {
                    for (CacheEventListener listener : listeners) {
                        if (!backingCache.getCacheEventNotificationService().getCacheEventListeners().contains(listener)) {
                            backingCache.getCacheEventNotificationService().registerListener(listener);
                        } else {
                        }
                    }
                }
            }
            return new EhcacheWrapper(cache);
        } catch (net.sf.ehcache.CacheException e) {
            throw new CacheException("Could not create cache: " + name, e);
        }
    }


    public void shutdown() {
        if (manager != null) {
            manager.shutdown();
            manager = null;
        }
    }

    public void clearAll() {
        manager.clearAll();
    }

    public CacheManager getCacheManager() {
        return manager;
    }

    public void addDefaultListener(CacheEventListener cacheEventListener) {
        listeners.add(cacheEventListener);
    }

    private URL loadResource(String configurationResourceName) {
        ClassLoader standardClassloader = ClassLoaderUtil.getStandardClassLoader();
        URL url = null;
        if (standardClassloader != null) {
            url = standardClassloader.getResource(configurationResourceName);
        }
        if (url == null) {
            url = this.getClass().getResource(configurationResourceName);
        }
        if (log.isLoggable(Level.FINE)) {
            log.fine("Creating EhCacheFactory from a specified resource: "
                    + configurationResourceName + " Resolved to URL: " + url);
        }
        if (url == null) {
            log.warning("A configurationResourceName was set to " + configurationResourceName +
                    " but the resource could not be loaded from the classpath." +
                    "Ehcache will configure itself using defaults.");
        }
        return url;
    }

}
