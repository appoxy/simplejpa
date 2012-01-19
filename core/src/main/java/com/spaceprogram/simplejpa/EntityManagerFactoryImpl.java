package com.spaceprogram.simplejpa;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;
import javax.persistence.spi.PersistenceUnitInfo;

import com.amazonaws.AmazonClientException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.simpledb.AmazonSimpleDB;
import com.amazonaws.services.simpledb.AmazonSimpleDBClient;
import com.amazonaws.services.simpledb.model.CreateDomainRequest;
import com.amazonaws.services.simpledb.model.ListDomainsRequest;
import com.amazonaws.services.simpledb.model.ListDomainsResult;
import com.spaceprogram.simplejpa.cache.Cache;
import com.spaceprogram.simplejpa.cache.CacheFactory;
import com.spaceprogram.simplejpa.cache.NoopCache;
import com.spaceprogram.simplejpa.cache.NoopCacheFactory;
import com.spaceprogram.simplejpa.stats.OpStats;

import org.apache.commons.collections.MapUtils;
import org.scannotation.AnnotationDB;
import org.scannotation.ClasspathUrlFinder;

/**
 * User: treeder Date: Feb 10, 2008 Time: 6:20:23 PM
 * 
 * Additional Contributions - Eric Molitor eric@molitor.org - Eric Wei e.pwei84@gmail.com
 */
public class EntityManagerFactoryImpl implements EntityManagerFactory {
    private static Logger logger = Logger.getLogger(EntityManagerFactoryImpl.class.getName());

    /**
     * User Agent Postfix
     */
    private static final String USER_AGENT = "SimpleJPA";
    /**
     * Default SDB endpoint
     */
    private static final String DEFAULT_SDB_ENDPOINT = "sdb.amazonaws.com";
    /**
     * Default S3 endpoint
     */
    private static final String DEFAULT_S3_ENDPOINT = "s3.amazonaws.com";
    /**
     * Whether or not the factory has been closed
     */
    private boolean closed = false;
    /**
     * This is a set of all the objects we found that are marked as @Entity
     */
    private Set<String> entities;
    /**
     * quick access to the entities
     */
    private Map<String, String> entityMap = new HashMap<String, String>();
    /**
     * properties file values
     */
    private Map props;
    /**
     * Stores annotation info about our entities for easy retrieval when needed
     */
    private AnnotationManager annotationManager;
    /**
     * for all the concurrent action. todo: It might make sense to have two executors, one fast one for queries, and one slow one used for slow things like puts/deletes
     */
    private ExecutorService executor;
    /**
     * Also the prefix that will be applied to each Domain
     */
    private String persistenceUnitName;
    /**
     * Cached set of existing Amazon SimpleDB domains.
     */
    private Set<String> domainSet;
    /**
     * same as domainsList, but map access
     */
    private HashSet<String> bucketSet = new HashSet<String>();
    /**
     * SimpleDB client
     */
    private AmazonSimpleDB simpleDbClient;
    /**
     * S3 client for lob access
     */
    private AmazonS3 s3Client;

    private static final int DEFAULT_GET_THREADS = 100;
    private int numExecutorThreads = DEFAULT_GET_THREADS;
    public static final String DTYPE = "DTYPE";

    private static final String AWSACCESS_KEY_PROP_NAME = "accessKey";
    private static final String AWSSECRET_KEY_PROP_NAME = "secretKey";

    // Global stats across all EntityManager's
    private OpStats stats = new OpStats();

    /**
     * Whether to display amazon queries or not.
     */
    private boolean printQueries = false;
    private String sdbEndpoint;
    private boolean sdbSecure;
    private String s3Endpoint;
    private boolean s3Secure;
    private String cacheFactoryClassname;
    private CacheFactory cacheFactory;
    private boolean sessionless;
    private boolean cacheless;
    public SimpleJPAConfig config;
    private String lobBucketName;
    private Cache cache;
    private String cacheClassname;
    private boolean consistentRead = true;

    /**
     * This one is generally called via the PersistenceProvider.
     * 
     * @param persistenceUnitInfo only using persistenceUnitName for now
     * @param props
     */
    public EntityManagerFactoryImpl(PersistenceUnitInfo persistenceUnitInfo, Map props) {
        this(persistenceUnitInfo != null ? persistenceUnitInfo.getPersistenceUnitName() : null, props);
    }

    /**
     * Use this if you want to construct this directly.
     * 
     * @param persistenceUnitName used to prefix the SimpleDB domains
     * @param props should have accessKey and secretKey
     */
    public EntityManagerFactoryImpl(String persistenceUnitName, Map props) {
        this(persistenceUnitName, props, null);
    }

    /**
     * Use this one in web applications, see: http://code.google.com/p/simplejpa/wiki/WebApplications
     * 
     * @param persistenceUnitName
     * @param props
     * @param libsToScan a set of
     */
    public EntityManagerFactoryImpl(String persistenceUnitName, Map props, Set<String> libsToScan) {
        if (persistenceUnitName == null) {
            throw new IllegalArgumentException("Must have a persistenceUnitName!");
        }
        config = new SimpleJPAConfig();
        this.persistenceUnitName = persistenceUnitName;
        annotationManager = new AnnotationManager(config);
        this.props = props;
        if (props == null || props.isEmpty()) {
            try {
                loadProps2();
            } catch (IOException e) {
                throw new PersistenceException(e);
            }
        }

        init(libsToScan);

        createClients();
    }

    private void createClients() {
        AWSCredentials awsCredentials = null;
        InputStream credentialsFile = getClass().getClassLoader().getResourceAsStream("AwsCredentials.properties");
        if (credentialsFile != null) {
            logger.info("Loading credentials from AwsCredentials.properties");
            try {
                awsCredentials = new PropertiesCredentials(credentialsFile);
            } catch (IOException e) {
                throw new PersistenceException("Failed loading credentials from AwsCredentials.properties.", e);
            }
        } else {
            logger.info("Loading credentials from simplejpa.properties");
            String awsAccessKey = (String) this.props.get(AWSACCESS_KEY_PROP_NAME);
            String awsSecretKey = (String) this.props.get(AWSSECRET_KEY_PROP_NAME);
            if (awsAccessKey == null || awsAccessKey.length() == 0) {
                throw new PersistenceException("AWS Access Key not found. It is a required property.");
            }
            if (awsSecretKey == null || awsSecretKey.length() == 0) {
                throw new PersistenceException("AWS Secret Key not found. It is a required property.");
            }

            awsCredentials = new BasicAWSCredentials(awsAccessKey, awsSecretKey);
        }

        this.simpleDbClient = new AmazonSimpleDBClient(awsCredentials, createConfiguration(sdbSecure));
        this.simpleDbClient.setEndpoint(sdbEndpoint);

        this.s3Client = new AmazonS3Client(awsCredentials, createConfiguration(s3Secure));
        this.s3Client.setEndpoint(s3Endpoint);
    }

    private ClientConfiguration createConfiguration(boolean isSecure) {
        ClientConfiguration config = new ClientConfiguration();
        config.setUserAgent(USER_AGENT);
        Protocol protocol = isSecure ? Protocol.HTTPS : Protocol.HTTP;
        config.setProtocol(protocol);
        return config;
    }

    /**
     * * SimpleJPA entity manager, which gets classes names instead of "libs-to-scan".
     * @author Yair Ben-Meir
     * @param persistenceUnitName
     * @param props
     * @param classNames
     * @throws PersistenceException
     */
    public static EntityManagerFactoryImpl newInstanceWithClassNames(String persistenceUnitName, Map<String, String> props, Set<String> classNames)
            throws PersistenceException {
        return new EntityManagerFactoryImpl(persistenceUnitName, props, getLibsToScan(classNames));
    }

    private static Set<String> getLibsToScan(Set<String> classNames) throws PersistenceException {
        Set<String> libs = new HashSet<String>();
        for (String className : classNames) {
            try {
                Class<?> clazz = Class.forName(className);
                URL resource = clazz.getResource(clazz.getSimpleName() + ".class");
                if (resource.getProtocol().equals("jar")) {
                    libs.add(resource.getFile().split("!")[0].substring(6));
                } else if (resource.getProtocol().equals("file")) {
                    libs.add(resource.getFile().substring(1));
                } else {
                    throw new PersistenceException("Unknown protocol in URL: " + resource);
                }
            } catch (Throwable e) {
                throw new PersistenceException("Failed getting lib of class: " + className, e);
            }
        }
        return libs;
    }

    private void init(Set<String> libsToScan) {
        lobBucketName = (String) props.get("lobBucketName");
        printQueries = Boolean.parseBoolean((String) props.get("printQueries"));
        cacheFactoryClassname = (String) props.get("cacheFactory");
        cacheClassname = (String) props.get("cacheClass");
        String s1 = (String) props.get("sessionless");
        if (s1 == null) {
            sessionless = true;
        } else {
            sessionless = Boolean.parseBoolean(s1);
        }
        config.setGroovyBeans(Boolean.parseBoolean((String) props.get("groovyBeans")));
        String consistentRead = (String) props.get("consistentRead");
        this.consistentRead = consistentRead == null ? true : Boolean.parseBoolean((String) props.get("groovyBeans"));
        String prop = (String) props.get("threads");
        if (prop != null)
            numExecutorThreads = Integer.parseInt(prop);

        sdbEndpoint = MapUtils.getString(props, "sdbEndpoint", DEFAULT_SDB_ENDPOINT);
        sdbSecure = MapUtils.getBoolean(props, "sdbSecure", false);

        s3Endpoint = MapUtils.getString(props, "s3Endpoint", DEFAULT_S3_ENDPOINT);
        s3Secure = MapUtils.getBoolean(props, "s3Secure", false);

        try {
            logger.info("Scanning for entity classes...");
            URL[] urls;
            try {
                urls = ClasspathUrlFinder.findClassPaths();
            } catch (Exception e) {
                System.err.println("CAUGHT");
                e.printStackTrace();
                urls = new URL[0];
            }
            if (libsToScan != null) {
                URL[] urls2 = new URL[urls.length + libsToScan.size()];
                System.arraycopy(urls, 0, urls2, 0, urls.length);
// urls = new URL[libsToScan.size()];
                int count = 0;
                for (String s : libsToScan) {
                    logger.fine("libinset=" + s);
                    urls2[count + urls.length] = new File(s).toURL();
                    count++;
                }
                urls = urls2;
            }
            logger.info("classpath=" + System.getProperty("java.class.path"));
            for (URL url : urls) {
                logger.info("Scanning: " + url.toString());
            }
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("classpath urls:");
                for (URL url : urls) {
                    logger.fine(url.toString());
                }
            }
            AnnotationDB annotationDB = new AnnotationDB();
            annotationDB.scanArchives(urls);
            entities = annotationDB.getAnnotationIndex().get(Entity.class.getName());
            if (entities != null) {
                for (String entity : entities) {
                    initEntity(entity);
                }
            }
            logger.info("Finished scanning for entity classes.");

            initSecondLevelCache();

            executor = Executors.newFixedThreadPool(numExecutorThreads);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void initEntity(String entity) {
        logger.info("entity=" + entity);
        entityMap.put(entity, entity);
        // also add simple name to it
        String simpleName = entity.substring(entity.lastIndexOf(".") + 1);
        entityMap.put(simpleName, entity);
        Class c = getAnnotationManager().getClass(entity, null);
        getAnnotationManager().putAnnotationInfo(c);
    }

    private void initSecondLevelCache() {
        System.out.println("Initing second level cache: " + cacheFactoryClassname);
        if (cacheFactoryClassname != null) {
            try {
                Class<CacheFactory> cacheFactoryClass = (Class<CacheFactory>) Class.forName(cacheFactoryClassname);
                cacheFactory = cacheFactoryClass.newInstance();
                cacheFactory.init(props);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        if (cacheFactory == null) {
            cacheFactory = new NoopCacheFactory();
        }
    }

    /**
     * Call this to load the props from a file in the root of our classpath called: sdb.properties
     * 
     * @throws IOException
     * @deprecated don't use this.
     */
    public void loadProps() throws IOException {

    }

    private void loadProps2() throws IOException {
        Properties props2 = new Properties();
        String propsFileName = "/simplejpa.properties";
        InputStream stream = this.getClass().getResourceAsStream(propsFileName);
        if (stream == null) {
            throw new FileNotFoundException(propsFileName + " not found on classpath. Could not initialize SimpleJPA.");
        }
        props2.load(stream);
        props = props2;
        logger.info("Properties loaded from [" + propsFileName + "].");
        stream.close();
    }

    /**
     * @return a new EntityManager for you to use.
     */
    public EntityManager createEntityManager() {
        return new EntityManagerSimpleJPA(this, sessionless);
    }

    public EntityManager createEntityManager(Map map) {
        return createEntityManager();
    }

    public void close() {
        closed = true;
        executor.shutdown();
        cacheFactory.shutdown();
    }

    public boolean isOpen() {
        return !closed;
    }

    public Map<String, String> getEntityMap() {
        return entityMap;
    }

    public Map getProps() {
        return props;
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    public String getPersistenceUnitName() {
        return persistenceUnitName;
    }

    public void setPersistenceUnitName(String persistenceUnitName) {
        this.persistenceUnitName = persistenceUnitName;
    }

    public synchronized void setupDbDomain(String domainName) {
        try {
            if (!doesDomainExist(domainName)) {
                logger.info("creating domain: " + domainName);
                AmazonSimpleDB db = getSimpleDb();
                db.createDomain(new CreateDomainRequest().withDomainName(domainName));
                domainSet.add(domainName);
            }
        } catch (AmazonClientException e) {
            throw new PersistenceException("Could not create SimpleDB domain.", e);
        }
    }

    public boolean doesDomainExist(String domainName) {
        if (domainSet == null)
            loadDomains();
        return domainSet.contains(domainName);
    }

    public boolean doesDomainExist(Class c) {
        return doesDomainExist(getDomainName(c));
    }

    public void createIfNotExistDomain(String domainName) {
        if (!doesDomainExist(domainName)) {
            setupDbDomain(domainName);
        }
    }

    public String getOrCreateDomain(Class c) {
        String domainName = getDomainName(c);
        createIfNotExistDomain(domainName);
        return domainName;
    }

    private synchronized void loadDomains() {
        if (domainSet != null)
            return;

        try {
            domainSet = new HashSet<String>();
            logger.info("getting all domains");
            AmazonSimpleDB db = getSimpleDb();
            ListDomainsResult listDomainsResult = db.listDomains();
            domainSet.addAll(listDomainsResult.getDomainNames());
            while (listDomainsResult.getNextToken() != null) {
                ListDomainsRequest request = new ListDomainsRequest().withNextToken(listDomainsResult.getNextToken());
                listDomainsResult = db.listDomains(request);
                domainSet.addAll(listDomainsResult.getDomainNames());
            }
        } catch (AmazonClientException e) {
            throw new PersistenceException(e);
        }
    }

    public AmazonSimpleDB getSimpleDb() {
        return this.simpleDbClient;
    }

    public AnnotationManager getAnnotationManager() {
        return annotationManager;
    }

    public String getDomainName(Class<? extends Object> aClass) {
        String className = getRootClassName(aClass);
        AnnotationInfo ai = getAnnotationManager().getAnnotationInfo(aClass);
        String domainName = ai.getDomainName();
        if (domainName == null || domainName.length() <= 0)
            domainName = getDomainName(className);
        createIfNotExistDomain(domainName);
        return domainName;
    }

    public String getDomainName(String className) {
        String domainName = getPersistenceUnitName() + "-" + className;
        return domainName;
    }

    private String getRootClassName(Class<? extends Object> aClass) {
        AnnotationInfo ai = getAnnotationManager().getAnnotationInfo(aClass);
        String className = ai.getRootClass().getSimpleName();
        return className;
    }

    public boolean isPrintQueries() {
        return printQueries;
    }

    public void setPrintQueries(boolean printQueries) {
        this.printQueries = printQueries;
    }

    public String getSdbEndpoint() {
        return sdbEndpoint;
    }

    public Cache getCache(Class aClass) {
        AnnotationInfo ai = getAnnotationManager().getAnnotationInfo(aClass);
        return cacheFactory.createCache(ai.getRootClass().getName());
    }

    /**
     * This will turn on sessionless mode which means that you do not need to keep EntityManager's open, nor do you need to close them. But you should ALWAYS use the second level
     * cache in this case.
     * 
     * @param sessionless
     */
    public void setSessionless(boolean sessionless) {
        this.sessionless = sessionless;
    }

    public boolean isSessionless() {
        return sessionless;
    }

    public void clearSecondLevelCache() {
        if (cache != null)
            cache.clear();
    }

    /**
     * Turns off caches. Useful for testing. This will also shutdown and recreate any existing cache if cacheless is true.
     * 
     * @param cacheless
     */
    public void setCacheless(boolean cacheless) {
        this.cacheless = cacheless;
        if (cacheless) {
            cache = new NoopCache();
// cacheFactory.shutdown();
// cacheFactory = new NoopCacheFactory();
        } else {
// cacheFactory.shutdown();
            initSecondLevelCache();
        }
    }

    public AmazonS3 getS3Service() {
        return this.s3Client;
    }

    public synchronized String getS3BucketName() {
        String bucketName;
        if (lobBucketName != null) {
            bucketName = lobBucketName;
        } else {
        	// AWS requires lower case S3 bucket names.
            bucketName = getPersistenceUnitName().toLowerCase() + "-lobs";
        }

        // See if we have checked if the bucket already exists.
        if (!this.bucketSet.contains(bucketName)) {

            // If the bucket doesn't already exist then we need to add it.
            if (!this.s3Client.doesBucketExist(bucketName)) {
                this.s3Client.createBucket(bucketName);
            }
            this.bucketSet.add(bucketName);
        }

        return bucketName;
    }

    public OpStats getGlobalStats() {
        return stats;
    }

    public void setConsistentRead(boolean consistentRead) {
        this.consistentRead = consistentRead;
    }

    public boolean isConsistentRead() {
        return consistentRead;
    }
}
