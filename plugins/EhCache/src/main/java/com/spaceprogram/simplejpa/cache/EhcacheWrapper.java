package com.spaceprogram.simplejpa.cache;

import java.beans.PropertyChangeListener;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.Statistics;
import net.sf.ehcache.Status;
import net.sf.ehcache.bootstrap.BootstrapCacheLoader;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.event.RegisteredEventListeners;
import net.sf.ehcache.exceptionhandler.CacheExceptionHandler;
import net.sf.ehcache.extension.CacheExtension;
import net.sf.ehcache.loader.CacheLoader;
import net.sf.ehcache.statistics.CacheUsageListener;
import net.sf.ehcache.statistics.LiveCacheStatistics;
import net.sf.ehcache.statistics.sampled.SampledCacheStatistics;
import net.sf.ehcache.store.Policy;
import net.sf.ehcache.transaction.manager.TransactionManagerLookup;
import net.sf.ehcache.writer.CacheWriter;
import net.sf.ehcache.writer.CacheWriterManager;

/**
 * User: treeder Date: Aug 2, 2009 Time: 9:44:53 PM
 */
public class EhcacheWrapper implements Ehcache, com.spaceprogram.simplejpa.cache.Cache {
    private Cache cache;

    public EhcacheWrapper(Cache cache) {
        this.cache = cache;
    }

    public void bootstrap() {
        cache.bootstrap();
    }

    public long calculateInMemorySize() throws IllegalStateException, CacheException {
        return cache.calculateInMemorySize();
    }

    public void clear() {
        cache.removeAll();
    }

    public void clearStatistics() throws IllegalStateException {
        cache.clearStatistics();
    }


    public Object clone() throws CloneNotSupportedException {
        return cache.clone();
    }


    public void disableDynamicFeatures() {
        cache.disableDynamicFeatures();
    }

    public void dispose() throws IllegalStateException {
        cache.dispose();
    }


    public boolean equals(Object o) {
        return cache.equals(o);
    }

    public void evictExpiredElements() {
        cache.evictExpiredElements();
    }

    public void flush() {
        cache.flush();
    }


    public Element get(Object key) {
        return cache.get(key);
    }

    public Element get(Serializable serializable) {
        return cache.get(serializable);
    }

    public Map getAllWithLoader(Collection collection, Object o) {
        return cache.getAllWithLoader(collection, o);
    }

    public float getAverageGetTime() {
        return cache.getAverageGetTime();
    }

    public BootstrapCacheLoader getBootstrapCacheLoader() {
        return cache.getBootstrapCacheLoader();
    }

    public CacheConfiguration getCacheConfiguration() {
        return cache.getCacheConfiguration();
    }

    public RegisteredEventListeners getCacheEventNotificationService() {
        return cache.getCacheEventNotificationService();
    }

    public CacheExceptionHandler getCacheExceptionHandler() {
        return cache.getCacheExceptionHandler();
    }

    public CacheManager getCacheManager() {
        return cache.getCacheManager();
    }

    public int getDiskStoreSize() {
        return cache.getDiskStoreSize();
    }

    public String getGuid() {
        return cache.getGuid();
    }


    public Object getInternalContext() {
        return cache.getInternalContext();
    }

    public List getKeys() {
        return cache.getKeys();
    }

    public List getKeysNoDuplicateCheck() {
        return cache.getKeysNoDuplicateCheck();
    }

    public List getKeysWithExpiryCheck() {
        return cache.getKeysWithExpiryCheck();
    }


    public LiveCacheStatistics getLiveCacheStatistics() {
        return cache.getLiveCacheStatistics();
    }

    public Policy getMemoryStoreEvictionPolicy() {
        return cache.getMemoryStoreEvictionPolicy();
    }

    public long getMemoryStoreSize() {
        return cache.getMemoryStoreSize();
    }

    public String getName() {
        return cache.getName();
    }

    public Object getObj(Object o) {
        Element elem = cache.get(o);
        return elem == null ? null : elem.getValue();
    }

    public Element getQuiet(Object o) {
        return cache.getQuiet(o);
    }

    public Element getQuiet(Serializable serializable) {
        return cache.getQuiet(serializable);
    }

    public List<CacheExtension> getRegisteredCacheExtensions() {
        return cache.getRegisteredCacheExtensions();
    }

    public List<CacheLoader> getRegisteredCacheLoaders() {
        return cache.getRegisteredCacheLoaders();
    }


    public CacheWriter getRegisteredCacheWriter() {
        return cache.getRegisteredCacheWriter();
    }


    public SampledCacheStatistics getSampledCacheStatistics() {
        return cache.getSampledCacheStatistics();
    }

    public int getSize() {
        return cache.getSize();
    }


    public int getSizeBasedOnAccuracy(int arg0) {
        return cache.getSizeBasedOnAccuracy(arg0);
    }

    public Statistics getStatistics() {
        return cache.getStatistics();
    }

    public int getStatisticsAccuracy() {
        return cache.getStatisticsAccuracy();
    }

    public Status getStatus() {
        return cache.getStatus();
    }

    public Element getWithLoader(Object o, CacheLoader cacheLoader, Object o1) {
        return cache.getWithLoader(o, cacheLoader, o1);
    }


    public CacheWriterManager getWriterManager() {
        return cache.getWriterManager();
    }


    public int hashCode() {
        return cache.hashCode();
    }

    public void initialise() {
        cache.initialise();
    }


    public boolean isClusterCoherent() {
        return cache.isClusterCoherent();
    }

    public boolean isDisabled() {
        return cache.isDisabled();
    }

    public boolean isElementInMemory(Object o) {
        return cache.isElementInMemory(o);
    }

    public boolean isElementInMemory(Serializable serializable) {
        return cache.isElementInMemory(serializable);
    }

    public boolean isElementOnDisk(Object o) {
        return cache.isElementOnDisk(o);
    }

    public boolean isElementOnDisk(Serializable serializable) {
        return cache.isElementOnDisk(serializable);
    }

    public boolean isExpired(Element element) {
        return cache.isExpired(element);
    }

    public boolean isKeyInCache(Object o) {
        return cache.isKeyInCache(o);
    }


    public boolean isNodeCoherent() {
        return cache.isNodeCoherent();
    }

    public void setNodeCoherent(boolean b) {
        //To change body of implemented methods use File | Settings | File Templates.
    }


    public boolean isSampledStatisticsEnabled() {
        return cache.isSampledStatisticsEnabled();
    }


    public boolean isStatisticsEnabled() {
        return cache.isSampledStatisticsEnabled();
    }

    public boolean isValueInCache(Object o) {
        return cache.isValueInCache(o);
    }

    public void load(Object o) throws CacheException {
        cache.load(o);
    }

    public void loadAll(Collection collection, Object o) {
        cache.loadAll(collection, o);
    }

    public void put(Element element) {
        cache.put(element);
    }

    public void put(Element element, boolean b) {
        cache.put(element, b);
    }

    public void put(Object o, Object o1) {
        cache.put(new Element(o, o1));
    }

    public void putQuiet(Element element) {
        cache.putQuiet(element);
    }


    public void putWithWriter(Element arg0) {
        cache.putWithWriter(arg0);
    }

    public Element putIfAbsent(Element element) {
        return cache.putIfAbsent(element);
    }

    public boolean removeElement(Element element) {
        return removeElement(element);
    }

    public boolean replace(Element element, Element element1) {
        return cache.replace(element, element1);
    }

    public Element replace(Element element) {
        return cache.replace(element);
    }

    public void registerCacheExtension(CacheExtension cacheExtension) {
        cache.registerCacheExtension(cacheExtension);
    }

    public void registerCacheLoader(CacheLoader cacheLoader) {
        cache.registerCacheLoader(cacheLoader);
    }


    public void registerCacheUsageListener(CacheUsageListener arg0) {
        cache.registerCacheUsageListener(arg0);
    }


    public void registerCacheWriter(CacheWriter arg0) {
        cache.registerCacheWriter(arg0);
    }

    public boolean remove(Object o) {
        return cache.remove(o);
    }

    public boolean remove(Object o, boolean b) {
        return cache.remove(o, b);
    }

    public boolean remove(Serializable serializable) {
        return cache.remove(serializable);
    }

    public boolean remove(Serializable serializable, boolean b) {
        return cache.remove(serializable, b);
    }

    public void removeAll() {
        cache.removeAll();
    }

    public void removeAll(boolean b) {
        cache.removeAll(b);
    }


    public void removeCacheUsageListener(CacheUsageListener arg0) {
        cache.removeCacheUsageListener(arg0);
    }

    public boolean removeQuiet(Object o) {
        return cache.removeQuiet(o);
    }

    public boolean removeQuiet(Serializable serializable) {
        return cache.removeQuiet(serializable);
    }


    public boolean removeWithWriter(Object arg0) {
        return cache.removeWithWriter(arg0);
    }

    public void setBootstrapCacheLoader(BootstrapCacheLoader bootstrapCacheLoader) {
        cache.setBootstrapCacheLoader(bootstrapCacheLoader);
    }

    public void setCacheExceptionHandler(CacheExceptionHandler cacheExceptionHandler) {
        cache.setCacheExceptionHandler(cacheExceptionHandler);
    }

    public void setCacheManager(CacheManager cacheManager) {
        cache.setCacheManager(cacheManager);
    }

    public void setDisabled(boolean b) {
        cache.setDisabled(b);
    }

    public void setDiskStorePath(String s) {
        cache.setDiskStorePath(s);
    }

    public void setMemoryStoreEvictionPolicy(Policy policy) {
        cache.setMemoryStoreEvictionPolicy(policy);
    }

    public void setName(String s) {
        cache.setName(s);
    }

    public void setNodeCoherence(boolean arg0) {
        cache.setNodeCoherent(arg0);
    }


    public void setSampledStatisticsEnabled(boolean arg0) {
        cache.setSampledStatisticsEnabled(arg0);
    }

    public void setStatisticsAccuracy(int i) {
        cache.setStatisticsAccuracy(i);
    }


    public void setStatisticsEnabled(boolean arg0) {
        cache.setStatisticsEnabled(arg0);
    }


    public void setTransactionManagerLookup(TransactionManagerLookup arg0) {
        cache.setTransactionManagerLookup(arg0);
    }

    public void addPropertyChangeListener(PropertyChangeListener propertyChangeListener) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void removePropertyChangeListener(PropertyChangeListener propertyChangeListener) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public int size() {
        return getSize();
    }


    public String toString() {
        return cache.toString();
    }

    public void unregisterCacheExtension(CacheExtension cacheExtension) {
        cache.unregisterCacheExtension(cacheExtension);
    }

    public void unregisterCacheLoader(CacheLoader cacheLoader) {
        cache.unregisterCacheLoader(cacheLoader);
    }


    public void unregisterCacheWriter() {
        cache.unregisterCacheWriter();
    }


    public void waitUntilClusterCoherent() {
        cache.waitUntilClusterCoherent();
    }
}
