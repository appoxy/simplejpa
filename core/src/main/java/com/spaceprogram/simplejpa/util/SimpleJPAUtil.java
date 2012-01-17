package com.spaceprogram.simplejpa.util;

import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;

import com.spaceprogram.simplejpa.EntityManagerFactoryImpl;

/**
 * For using the thread local pattern. Good for web apps.
 *
 * User: treeder
 * Date: Feb 10, 2008
 * Time: 6:16:53 PM
 */
public class SimpleJPAUtil {

    private static Logger logger = Logger.getLogger(SimpleJPAUtil.class.getName());
    private static EntityManagerFactoryImpl entityManagerFactory;
    private static final ThreadLocal<EntityManager> entityManagerThreadLocal = new ThreadLocal<EntityManager>();

    private static String persistenceUnitName;
    private static Set<String> libsToScan;
    
    private static Map props;

    /**
     * Must call this before using SimpleJPAUtil.
     *
     * @param persistenceUnitName
     */
    public static void setPersistenceUnitName(String persistenceUnitName) {
        SimpleJPAUtil.persistenceUnitName = persistenceUnitName;
    }

    /**
     * CALL THIS FIRST THING IN A WEBAPP, CAN BE DONE VIA A LOAD-ON-STARTUP SERVLET.
     * see: http://code.google.com/p/simplejpa/wiki/WebApplications
     *
     * @param libsToScan
     */
    public static void setLibsToScan(Set<String> libsToScan){
        SimpleJPAUtil.libsToScan = libsToScan;
    }
    
    public static Map getProps() {
        return props;
    }
    
    public static void setProps(Map props) {
        SimpleJPAUtil.props = props;
    }

    private synchronized static void init() {
        if(entityManagerFactory != null) return;
        try {
            if(persistenceUnitName == null){
                throw new PersistenceException("SimpleJPAUtil requires a call to setPersistenceUnitName before using.");
            }
            entityManagerFactory = new EntityManagerFactoryImpl(persistenceUnitName, getProps(), libsToScan, null);
            //           todo: use Persistence class: entityManagerFactory = Persistence.createEntityManagerFactory(persistenceUnitName != null ? persistenceUnitName : DEFAULT_PERSISTENCE_UNIT);
        } catch (Throwable ex) {
            // Log exception!
            ex.printStackTrace();
            logger.log(Level.SEVERE, "Initial SessionFactory creation failed.", ex);
            throw new ExceptionInInitializerError(ex);
        }
    }
    
    public static EntityManagerFactory getFactory() {
    	return entityManagerFactory;
    }

    public static EntityManager createSession() {
        init();
        return entityManagerFactory.createEntityManager();
    }

    public static EntityManager currentSession() {
        return currentSession(true);
    }

    public static EntityManager currentSession(boolean createIfNonExistant) {
        init();
        EntityManager s = (EntityManager) entityManagerThreadLocal.get();
        // Open a new Session, if this Thread has none yet
        if (s == null && createIfNonExistant) {
            s = entityManagerFactory.createEntityManager();
            entityManagerThreadLocal.set(s);
        }
        return s;
    }

    public static void closeSession() {
        closeSession(false);
    }

    public static void commitAndClose() {
        closeSession(true);
    }

    private static void closeSession(boolean commit) {
        init();
        EntityManager em = (EntityManager) entityManagerThreadLocal.get();
        entityManagerThreadLocal.set(null);
        if (em != null) {
            if (commit) {
                commit(em);
            }
            logger.fine("SESSION CLOSED");
            em.close();
        }
    }

    private static void commit(EntityManager em) {
        /*try {
            if (em.getTransaction().isActive()) {
                logger.fine("Committing active trans");
                em.getTransaction().commit();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }*/
    }

    public static void commit() {
       /* EntityManager em = (EntityManager) entityManagerThreadLocal.get();
        if (em != null) {
            commit(em);
        }*/
    }

    public static void shutdown() {
        entityManagerFactory.close();
    }

    public static void rollback() {
       /* EntityManager em = currentSession(false);
        if (em != null) {
            try {
                if (em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
            } catch (Exception e) {
                logger.logger(Level.WARNING, "Could not rollback transaction after exception!", e);
            }
        }*/
    }

}
