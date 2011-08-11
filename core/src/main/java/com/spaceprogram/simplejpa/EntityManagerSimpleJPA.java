package com.spaceprogram.simplejpa;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.*;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.simpledb.AmazonSimpleDB;
import com.amazonaws.services.simpledb.model.Attribute;
import com.amazonaws.services.simpledb.model.DeleteAttributesRequest;
import com.amazonaws.services.simpledb.model.GetAttributesRequest;
import com.amazonaws.services.simpledb.model.GetAttributesResult;
import com.amazonaws.services.simpledb.model.Item;
import com.amazonaws.services.simpledb.model.PutAttributesRequest;
import com.amazonaws.services.simpledb.model.ReplaceableAttribute;
import com.amazonaws.services.simpledb.model.SelectResult;
import com.spaceprogram.simplejpa.AnnotationManager.ClassMethodEntry;
import com.spaceprogram.simplejpa.cache.Cache;
import com.spaceprogram.simplejpa.operations.Delete;
import com.spaceprogram.simplejpa.operations.Find;
import com.spaceprogram.simplejpa.operations.Save;
import com.spaceprogram.simplejpa.query.QueryImpl;
import com.spaceprogram.simplejpa.query.SimpleDBQuery;
import com.spaceprogram.simplejpa.stats.OpStats;
import com.spaceprogram.simplejpa.util.AmazonSimpleDBUtil;
import com.spaceprogram.simplejpa.util.ConcurrentRetriever;

import net.sf.cglib.proxy.Factory;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;

/**
 * User: treeder Date: Feb 8, 2008 Time: 12:59:38 PM
 * 
 * Additional Contributions - Eric Molitor eric@molitor.org - Eric Wei e.pwei84@gmail.com
 */
public class EntityManagerSimpleJPA implements SimpleEntityManager, DatabaseManager {

    private static Logger logger = Logger.getLogger(EntityManagerSimpleJPA.class.getName());
    private boolean closed = false;
    public EntityManagerFactoryImpl factory;
    private boolean sessionless;
    private boolean consistentRead = true;
    /**
     * cache is used to store objects retrieved in this EntityManager session
     */
    private Map sessionCache;
    /**
     * used for converting numbers to strings
     */
    public static final BigDecimal OFFSET_VALUE = new BigDecimal(Long.MIN_VALUE).negate();
    private OpStats lastOpStats = new OpStats(); // todo: thread local this
    private OpStats totalOpStats = new OpStats();

    EntityManagerSimpleJPA(EntityManagerFactoryImpl factory, boolean sessionless) {
        this.factory = factory;
        this.sessionless = sessionless;
        if (!sessionless) {
            sessionCache = new ConcurrentHashMap();
        }
        this.consistentRead = factory.isConsistentRead();
    }

    public void persist(Object o) {
        resetLastOpStats();
        try {
            new Save(this, o).call();
        } catch (AmazonClientException e) {
            throw new PersistenceException("Could not get SimpleDb Domain", e);
        } catch (Exception e) {
            throw new PersistenceException(e);
        }
    }

    public Future persistAsync(Object o) {
        Future future = getExecutor().submit(new Save(this, o));
        return future;
    }

    public Future removeAsync(Object o) {
        Future future = getExecutor().submit(new Delete(this, o));
        return future;
    }

    public <T> Future<T> findAsync(Class<T> tClass, Object o) {
        Future<T> future = getExecutor().submit(new Find(this, tClass, o));
        return future;
    }

    private void resetLastOpStats() {
        lastOpStats = new OpStats();
    }

    public <T> T merge(T t) {
        // todo: should probably behave a bit different
        persist(t);
        return t;
    }

    public static String padOrConvertIfRequired(Object ob) {
        if (ob instanceof Integer || ob instanceof Long) {
            // then pad
            return AmazonSimpleDBUtil.encodeRealNumberRange(new BigDecimal(ob.toString()), AmazonSimpleDBUtil.LONG_DIGITS, OFFSET_VALUE);
        } else if ((ob instanceof Double && !((Double) ob).isInfinite() && !((Double) ob).isNaN())
                || (ob instanceof Float && !((Float) ob).isInfinite() && !((Float) ob).isNaN())) {
            // then pad
            return AmazonSimpleDBUtil.encodeRealNumberRange(new BigDecimal(ob.toString()), AmazonSimpleDBUtil.LONG_DIGITS, AmazonSimpleDBUtil.LONG_DIGITS,
                    OFFSET_VALUE);
        } else if (ob instanceof BigDecimal) {
            // then pad
            return AmazonSimpleDBUtil.encodeRealNumberRange((BigDecimal) ob, AmazonSimpleDBUtil.LONG_DIGITS, AmazonSimpleDBUtil.LONG_DIGITS, OFFSET_VALUE);
        } else if (ob instanceof Date) {
            Date d = (Date) ob;
            return AmazonSimpleDBUtil.encodeDate(d);
        } else if (ob instanceof byte[]) {
            return AmazonSimpleDBUtil.encodeByteArray((byte[]) ob);
        }
        return ob.toString();
    }

    /**
     * Get's the identifier for the object based on @Id
     * 
     * @param o
     * @return
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    public String getId(Object o) {
        AnnotationInfo ai = factory.getAnnotationManager().getAnnotationInfo(o);
        if (ai == null)
            return null; // todo: should it throw?
        String id = null;
        return (String) ai.getIdMethod().getProperty(o);
    }

    public String getDomainName(Class<? extends Object> aClass) {
        return factory.getDomainName(aClass);
    }

    public String getOrCreateDomain(Class c) {
        return factory.getOrCreateDomain(c);
    }

    public void checkEntity(Object o) {
// String className = o.getClass().getName();
// ensureClassIsEntity(className); THIS IS DONE IN getAnnotationInfo now
        // now if it the reflection data hasn't been cached, do it now
        AnnotationInfo ai = factory.getAnnotationManager().getAnnotationInfo(o);
        String domainName = getDomainName(o.getClass());
        factory.setupDbDomain(domainName);
    }

    public Class ensureClassIsEntity(String className) {
// System.out.println("className=" + className);
        className = factory.getAnnotationManager().stripEnhancerClass(className);
        String fullClassName = factory.getEntityMap().get(className);
        if (fullClassName == null) {
// throw new PersistenceException("Class not marked as an Entity: " + className);
            fullClassName = className;
        }
// System.out.println("fullclassName=" + fullClassName);
        Class tClass = factory.getAnnotationManager().getClass(fullClassName, null);
        AnnotationInfo ai = factory.getAnnotationManager().getAnnotationInfo(tClass); // sets up metadata if not already done
        return tClass;
    }

    public AmazonSimpleDB getSimpleDb() {
        return factory.getSimpleDb();
    }

    /**
     * Deletes an object from SimpleDB.
     * 
     * @param o
     */
    public void remove(Object o) {
        if (o == null)
            return;
        try {
            Delete d = new Delete(this, o);
            d.call();
        } catch (Exception e) {
            throw new PersistenceException(e);
        }
    }

    /**
     * Finds an object by id.
     * 
     * @param tClass
     * @param id
     * @return
     */
    public <T> T find(Class<T> tClass, Object id) {
        if (!sessionless && closed)
            throw new PersistenceException("EntityManager already closed.");
        if (id == null)
            throw new IllegalArgumentException("Id value must not be null.");
        try {
            T ob = cacheGet(tClass, id);
            if (ob != null) {
                if (logger.isLoggable(Level.FINEST)) {
                    logger.finest("found in cache: " + ob);
                }
                return ob;
            }
            return findInDb(tClass, id);
        } catch (AmazonClientException e) {
            throw new PersistenceException(e);
        }
    }

    private <T> T findInDb(Class<T> tClass, Object id) throws AmazonClientException {
        String domainName = getDomainName(tClass);
        if (domainName == null)
            return null;
        Item iraw = DomainHelper.findItemById(factory.getSimpleDb(), domainName, id.toString(), consistentRead);
// logger.fine("got back item=" + item);
        if (iraw == null)
            return null;
        SdbItem item = new SdbItemImpl2(iraw);
        return getItemAttributesBuildAndCache(tClass, id, item);

    }

    /**
     * If you already have reference to an Item, can use this build the object and cache it.
     * @param tClass
     * @param id
     * @param item
     * @return
     * @throws AmazonClientException
     */
    public <T> T getItemAttributesBuildAndCache(Class<T> tClass, Object id, SdbItem item) throws AmazonClientException {
        // todo: update stats for this get
        List<Attribute> atts = item.getAttributes();
        if (atts == null || atts.size() == 0)
            return null;
        return buildObject(tClass, id, atts);
    }

    public void renameField(Class tClass, String oldAttributeName, String newAttributeName) {
        // get list of all items in the domain
        try {
            String domainName = getDomainName(tClass);
            SelectResult result;
            List<Item> items;
            int i = 0;
            String nextToken = null;
            while (i == 0 || nextToken != null) {
                result = executeQueryForRename(oldAttributeName, newAttributeName, domainName, nextToken);
                items = result.getItems();
                putAndDelete(domainName, oldAttributeName, newAttributeName, items);
                nextToken = result.getNextToken();
                i++;
                if (i % 100 == 0) {
                    System.out.println("Renamed " + i + " fields so far...");
                }
            }
        } catch (AmazonClientException e) {
            e.printStackTrace();
        }
    }

    public void renameSubclass(String oldClassName, Class newClass) {
        logger.info("Renaming DTYPE for " + oldClassName + " to " + newClass.getSimpleName());
        try {
            String newClassName = newClass.getSimpleName();
            String domainName = factory.getDomainName(newClass);
            SelectResult result;
            List<Item> items;
            int i = 0;
            String nextToken = null;
            while (i == 0 || nextToken != null) {
                result = executeQueryForRenameSubclass(oldClassName, newClass, domainName, nextToken);
                items = result.getItems();
                putNewValue(domainName, items, EntityManagerFactoryImpl.DTYPE, newClassName);
                nextToken = result.getNextToken();
                i++;
                if (i % 100 == 0) {
                    System.out.println("Renamed " + i + " subclassed objects so far...");
                }
            }
        } catch (AmazonClientException e) {
            e.printStackTrace();
        }
    }

    private void putNewValue(String domainName, List<Item> items, String dtype, String newClassName) throws AmazonClientException {
        AmazonSimpleDB db = factory.getSimpleDb();
        for (Item item : items) {
            List<ReplaceableAttribute> atts = new ArrayList<ReplaceableAttribute>();

            atts.add(new ReplaceableAttribute(dtype, newClassName, true));
            db.putAttributes(new PutAttributesRequest(domainName, item.getName(), atts));
        }
    }

    private SelectResult executeQueryForRenameSubclass(String oldClassName, Class newClass, String domainName, String nextToken) throws AmazonClientException {
        SelectResult result = DomainHelper.selectItems(factory.getSimpleDb(), domainName, "'DTYPE' = '" + oldClassName + "'", nextToken, consistentRead);
        return result;
    }

    private SelectResult executeQueryForRename(String oldAttributeName, String newAttributeName, String domainName, String nextToken)
            throws AmazonClientException {
        SelectResult result = DomainHelper.selectItems(factory.getSimpleDb(), domainName, "['" + oldAttributeName + "' starts-with ''] intersection not ['"
                + newAttributeName + "' starts-with ''] ", nextToken, consistentRead);
        return result;
    }

    private void putAndDelete(String domainName, String oldAttributeName, String newAttributeName, List<Item> items) throws AmazonClientException {
        AmazonSimpleDB db = factory.getSimpleDb();
        for (Item item : items) {
            GetAttributesResult getOldResults = db.getAttributes(new GetAttributesRequest().withDomainName(domainName).withConsistentRead(true).withItemName(
                    item.getName()).withAttributeNames(oldAttributeName));

            List<Attribute> oldAtts = getOldResults.getAttributes();
            if (oldAtts.size() > 0) {
                Attribute oldAtt = oldAtts.get(0);
                List<ReplaceableAttribute> atts = new ArrayList<ReplaceableAttribute>();
                atts.add(new ReplaceableAttribute(newAttributeName, oldAtt.getValue(), true));

                db.putAttributes(new PutAttributesRequest().withDomainName(domainName).withItemName(item.getName()).withAttributes(atts));

                db.deleteAttributes(new DeleteAttributesRequest().withDomainName(domainName).withItemName(item.getName()).withAttributes(oldAtts));
            }
        }
    }

    /**
     * This method puts together an object from the SimpleDB data.
     * 
     * @param tClass
     * @param id
     * @param atts
     * @return
     */
    public <T> T buildObject(Class<T> tClass, Object id, List<Attribute> atts) {
        return ObjectBuilder.buildObject(this, tClass, id, atts);
    }

    public <T> T cacheGet(Class<T> aClass, Object id) {
        String key = cacheKey(aClass, id);
        logger.finest("getting item from cache with cachekey=" + key);
        T o = sessionCache != null ? (T) sessionCache.get(key) : null;
        if (o == null) {
            Cache c = getFactory().getCache(aClass);
            if (c != null) {
                o = (T) c.getObj(id);
                if (o != null) {
                    logger.finest("Got item from second level cache!");
                    replaceEntityManager(o, this);
                }
            }
        }
        logger.finest("got item from cache=" + o);
        return o;
    }

    public void cachePut(Object id, Object newInstance) {
        String key = cacheKey(newInstance.getClass(), id);
        logger.finest("putting item in cache with cachekey=" + key + " - " + newInstance);
        if (sessionCache != null)
            sessionCache.put(key, newInstance);
        Cache c = getFactory().getCache(newInstance.getClass());
        if (c != null) {
            c.put(id, newInstance);
        }
    }

    public void cachePut(Object o) {
        String id = getId(o);
        cachePut(id, o);
    }

    public Object cacheRemove(Class aClass, String id) {
        String key = cacheKey(aClass, id);
        logger.finest("removing item from cache with cachekey=" + key);
        Object o = sessionCache != null ? sessionCache.remove(key) : null;
        Cache c = getFactory().getCache(aClass);
        if (c != null) {
            Object o2 = c.remove(id);
            if (o == null)
                o = o2;
        }
        logger.finest("removed object from cache=" + o);
        return o;
    }

    public String cacheKey(Class tClass, Object id) {
        return AnnotationManager.stripEnhancerClass(tClass).getName() + "_" + id;
    }

    public Method getSetterFromGetter(Class tClass, Method getter, Class retType) throws NoSuchMethodException {
        return tClass.getMethod(getSetterNameFromGetter(getter), retType);
    }

    String getSetterNameFromGetter(Method getter) {
        return NamingHelper.setterName(NamingHelper.attributeName(getter));
    }

    /**
     * Sets the value on an object field after applying any necessary conversions from SimpleDB strings.
     * 
     * @param tClass
     * @param newInstance
     * @param getter
     * @param val
     */
    public <T> void setFieldValue(Class tClass, T newInstance, PersistentProperty property, Collection<String> vals) {
        try {
            // need param type
            String attName = property.getFieldName();
            Class retType = property.getRawClass();
// logger.fine("getter in setFieldValue = " + attName + " - valAsString=" + valAsString + " rettype=" + retType);
            Method setMethod = tClass.getMethod("set" + StringUtils.capitalize(attName), retType);
            Object newField = convert(vals, property, property.getRawClass());
            setMethod.invoke(newInstance, newField);
        } catch (Exception e) {
            throw new PersistenceException("Failed setting field of getter: " + property.getFieldName() + ", using value: " + vals, e);
        }
    }

    public <T> T getReference(Class<T> tClass, Object o) {
        throw new NotImplementedException("TODO");
    }

    public void flush() {
        // we're always flushed in the current version so this doesn't have to do anything
// throw new NotImplementedException("TODO");
    }

    public void setFlushMode(FlushModeType flushModeType) {
        throw new NotImplementedException("TODO");
    }

    public FlushModeType getFlushMode() {
        throw new NotImplementedException("TODO");
    }

    public void lock(Object o, LockModeType lockModeType) {
        throw new NotImplementedException("TODO");
    }

    public void refresh(Object o) {
        throw new NotImplementedException("TODO");
    }

    public void clear() {
        checkClosed();
        // this is really only useful with transactions
        if (sessionCache != null) {
            sessionCache = new ConcurrentHashMap();
        }
    }

    private void checkClosed() {
        if (!isOpen()) {
            throw new IllegalStateException("EntityManager has been closed.");
        }
    }

    public boolean contains(Object o) {
        checkClosed();
        Object ob = cacheGet(o.getClass(), getId(o));
        return ob != null;
    }

    public Query createQuery(String s) {
        return new QueryImpl(this, s);
    }

    public Query createNamedQuery(String s) {
        throw new NotImplementedException("TODO");
    }

    public Query createNativeQuery(String s) {
        return new SimpleDBQuery(this, s);
    }

    public Query createNativeQuery(String s, Class aClass) {
        return new SimpleDBQuery(this, s, aClass);
    }

    public Query createNativeQuery(String s, String s1) {
        throw new NotImplementedException("TODO");
    }

    public void joinTransaction() {
        throw new NotImplementedException("TODO");
    }

    public Object getDelegate() {
        throw new NotImplementedException("TODO");
    }

    /**
     * Clears cache and marks as closed.
     */
    public void close() {
        closed = true;
        sessionCache = null;
    }

    public boolean isOpen() {
        return sessionless || !closed;
    }

    public EntityTransaction getTransaction() {
        return new EntityTransactionImpl();
    }

    public ExecutorService getExecutor() {
        return factory.getExecutor();
    }

    public Object getObjectFromS3(String idOnS3) throws AmazonClientException, IOException, ClassNotFoundException {
        long start = System.currentTimeMillis();
        AmazonS3 s3 = factory.getS3Service();
        S3Object s3o = s3.getObject(factory.getS3BucketName(), idOnS3);
        logger.fine("got s3object=" + s3o);
        Object ret = null;
        try {
            ObjectInputStream reader = new ObjectInputStream(new BufferedInputStream((s3o.getObjectContent())));
            ret = reader.readObject();
        } finally {
            s3o.getObjectContent().close();
        }

        statsS3Get(System.currentTimeMillis() - start);
        return ret;
    }

    public void incrementQueryCount() {
        totalOpStats.queries.incrementAndGet();
        factory.getGlobalStats().queries.incrementAndGet();

    }

    /**
     * @return the number of actual queries sent to amazon.
     */
    public int getQueryCount() {
        return totalOpStats.queries.get();
    }

    /**
     * This is mainly for debugging purposes. Will print to system out all of the items in the domain represented by the class parameter.
     * 
     * @param c
     * @throws AmazonClientException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public void listAllObjectsRaw(Class c) throws AmazonClientException, ExecutionException, InterruptedException {

        String domainName = factory.getDomainName(c);
        List<Item> items = DomainHelper.listAllItems(factory.getSimpleDb(), domainName, consistentRead);

        List<ItemAndAttributes> ia = ConcurrentRetriever.getAttributesFromSdb(toSdbItem(items), getExecutor(), this);
        for (ItemAndAttributes itemAndAttributes : ia) {
            System.out.println("item=" + itemAndAttributes.getItem().getIdentifier());
            List<Attribute> atts = itemAndAttributes.getAtts();
            for (Attribute att : atts) {
                System.out.println("\t=" + att.getName() + "=" + att.getValue());
            }
        }
    }

    private List<SdbItem> toSdbItem(List<Item> items) {
        List<SdbItem> ret = new ArrayList<SdbItem>();
        for (Item item : items) {
            ret.add(new SdbItemImpl2(item));
        }
        return ret;
    }

    public void invokeEntityListener(Object o, Class event) {
// System.out.println("invoking entity listeners on " + o + " for " + event);
        Map<Class, List<ClassMethodEntry>> listeners = getAnnotationManager().getAnnotationInfo(o).getEntityListeners();
        if (listeners != null && listeners.containsKey(event)) {
// System.out.println("founder listeners: " + listeners);
            List<ClassMethodEntry> listenerList = listeners.get(event);
            if (listenerList != null) {
                for (ClassMethodEntry listener : listenerList) {
                    try {
                        listener.invoke(o);
                    } catch (Exception e) {
                        throw new PersistenceException("Error invoking entity listener", e);
                    }
                }
            }
        }
    }

    public AnnotationManager getAnnotationManager() {
        return factory.getAnnotationManager();
    }

    public EntityManagerFactoryImpl getFactory() {
        return factory;
    }

    public OpStats getLastOpStats() {
        return lastOpStats;
    }

    public static <T> void replaceEntityManager(T newInstance, EntityManagerSimpleJPA em) {
        if (newInstance instanceof Factory) {
            Factory factory = (Factory) newInstance;
            LazyInterceptor interceptor = (LazyInterceptor) factory.getCallback(0);
            interceptor.setEntityManager(em);
        }
    }

    public void statsS3Get(long duration) {
        getLastOpStats().s3Get(duration);
        totalOpStats.s3Get(duration);
        factory.getGlobalStats().s3Get(duration);
    }

    public void statsS3Put(long duration) {
        getLastOpStats().s3Put(duration);
        totalOpStats.s3Put(duration);
        factory.getGlobalStats().s3Put(duration);
    }

    public void statsAttsPut(int numAtts, long duration2) {
        getLastOpStats().attsPut(numAtts, duration2);
        totalOpStats.attsPut(numAtts, duration2);
        factory.getGlobalStats().attsPut(numAtts, duration2);
    }

    public void statsAttsDeleted(int numAtts, long duration2) {
        getLastOpStats().attsDeleted(numAtts, duration2);
        totalOpStats.attsDeleted(numAtts, duration2);
        factory.getGlobalStats().attsDeleted(numAtts, duration2);
    }

    public OpStats getTotalOpStats() {
        return totalOpStats;
    }

    public OpStats getGlobalOpStats() {
        return factory.getGlobalStats();
    }

    public void statsGets(int numItems, long duration2) {
        getLastOpStats().got(numItems, duration2);
        totalOpStats.got(numItems, duration2);
        factory.getGlobalStats().got(numItems, duration2);
    }

    public String getS3BucketName() {
        return factory.getS3BucketName();
    }

    public AmazonS3 getS3Service() {
        return factory.getS3Service();
    }

    public void setConsistentRead(boolean consistentRead) {
        this.consistentRead = consistentRead;
    }

    public boolean isConsistentRead() {
        return consistentRead;
    }

    @SuppressWarnings("unchecked")
    public <T> T convert(Collection<String> values, PersistentProperty property, Class retType) throws ParseException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {
        Object newField = null;
        Object val = null;
        if (Integer.class.isAssignableFrom(retType) || retType == int.class) {
            val = AmazonSimpleDBUtil.decodeRealNumberRange(values.iterator().next(), EntityManagerSimpleJPA.OFFSET_VALUE).toString();
            if (retType == int.class)
                retType = Integer.class;
        } else if (Long.class.isAssignableFrom(retType) || retType == long.class) {
            val = AmazonSimpleDBUtil.decodeRealNumberRange(values.iterator().next(), EntityManagerSimpleJPA.OFFSET_VALUE).toString();
            if (retType == long.class)
                retType = Long.class;
        } else if (Float.class.isAssignableFrom(retType) || retType == float.class) {
            // Ignore NaN and Infinity
            if (!values.iterator().next().matches(".*Infinity|NaN")) {
                val = AmazonSimpleDBUtil.decodeRealNumberRange(values.iterator().next(), AmazonSimpleDBUtil.LONG_DIGITS, EntityManagerSimpleJPA.OFFSET_VALUE).toString();
            }
            else newField = Float.NaN;
            if (retType == float.class)
                retType = Float.class;
        } else if (Double.class.isAssignableFrom(retType) || retType == double.class) {
            // Ignore NaN and Infinity
            if (!values.iterator().next().matches(".*Infinity|NaN")) {
                val = AmazonSimpleDBUtil.decodeRealNumberRange(values.iterator().next(), AmazonSimpleDBUtil.LONG_DIGITS, EntityManagerSimpleJPA.OFFSET_VALUE).toString();
            }
            else newField = Double.NaN;
            if (retType == double.class)
                retType = Double.class;
        } else if (BigDecimal.class.isAssignableFrom(retType)) {
            val = AmazonSimpleDBUtil.decodeRealNumberRange(values.iterator().next(), AmazonSimpleDBUtil.LONG_DIGITS, EntityManagerSimpleJPA.OFFSET_VALUE).toString();
        } else if (byte[].class.isAssignableFrom(retType)) {
            newField = AmazonSimpleDBUtil.decodeByteArray(values.iterator().next());
        } else if (Date.class.isAssignableFrom(retType)) {
            newField = AmazonSimpleDBUtil.decodeDate(values.iterator().next());
        } else if (Collection.class.isAssignableFrom(retType)) { // convert all of the contents of the collection
            Collection coll = new ArrayList(); //TODO support other collection types
            for (String value : values) {
                coll.add(convert(Collections.singleton(value), property, property.getPropertyClass()));
            }
            newField = coll;
        } else {
            val = values.iterator().next();
        }
        // If newField has not been created yet then we create it from val.
        if (newField == null) {
            // We build a new field object here because we may get an argument mismatch otherwise, eg: BigDecimal for an Integer field.
            // todo: getConstructor throws a NoSuchMethodException here, should ensure that these are second class object fields
            Constructor forNewField = retType.getConstructor(val.getClass());
            if (forNewField == null) {
                throw new PersistenceException("No constructor for field type: " + retType + " that can take a " + val.getClass());
            }
            newField = forNewField.newInstance(val);
        }
        return (T)newField;
    }
}
