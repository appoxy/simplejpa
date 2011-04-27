package com.spaceprogram.simplejpa;

import org.junit.Before;
import org.junit.Test;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;

/**
 * User: treeder
 * Date: Aug 1, 2008
 * Time: 2:23:53 PM
 */
public class SessionlessTests extends BaseTestClass {

    @Before
    public void makeSessionless() {
        BaseTestClass.factory.setSessionless(true);
    }

    @Test(expected = PersistenceException.class)
    public void failWithSession() {
        BaseTestClass.factory.setSessionless(false);
        getReference();
        BaseTestClass.factory.setSessionless(true);
    }

    @Test
    public void noFailSessionless() {
        getReference();
    }

    private void getReference() {
        EntityManager em = factory.createEntityManager();
        MyTestObject2 ob2 = new MyTestObject2("my ob 2", 123);
        ob2.setMyTestObject(new MyTestObject("referenced object"));
        em.persist(ob2.getMyTestObject());
        em.persist(ob2);
        em.close();
        String id = ob2.getId();

        factory.clearSecondLevelCache();

        em = factory.createEntityManager();
        ob2 = em.find(MyTestObject2.class, id);
        em.close();
        System.out.println("closed EM, getting ref");
        MyTestObject ob3 = ob2.getMyTestObject();
        System.out.println("ob3=" + ob3);
    }

   /* *//**
     * Writes to a cache with only a few in memory elements, then gets the objects that should have been written
     * to disk to see if they reattach properly.
     *
     * If it doesn't throw, then it's all good.
     *//*
    @Test
    public void testCacheOverflowToDiskAndBack() throws InterruptedException {
        // todo: make a new cache for this particular test with small in memory size
        EntityManager em = factory.createEntityManager();
        List<String> ids = new ArrayList<String>();
        for (int i = 0; i < 20; i++) {
            MyTestObject2 ob2 = new MyTestObject2("my ob " + i, i);
            ob2.setMyTestObject(new MyTestObject("referenced object"));
            em.persist(ob2.getMyTestObject());
            em.persist(ob2);
            ids.add(ob2.getId());
        }
        em.close();
        JCache cache = (JCache) factory.getCache(MyTestObject2.class);
        System.out.println("maxelementsinmemory=" + cache.getBackingCache().getMaxElementsInMemory());
        Thread.sleep(1000); // let the expiry thread run

        System.out.println("cacheFactory=" + factory.getCacheFactory());
        System.out.println("cachemanager=" + factory.getCacheManager());

        // get rid of all instances of any objects
        factory.getCacheManager().clearAll();

        // now load up objects again from database without loading the ManyToOne's
        em = factory.createEntityManager();
        for (int i = 0; i < ids.size(); i++) {
            String id = ids.get(i);
            MyTestObject2 object2 = em.find(MyTestObject2.class, id);
        }
        em.close();

        // now load them one last time from cache/disk to see if we can load the refs
        em = factory.createEntityManager();
        for (int i = 0; i < ids.size(); i++) {
            String id = ids.get(i);
            MyTestObject2 object2 = em.find(MyTestObject2.class, id);
            MyTestObject refOb = object2.getMyTestObject();
            System.out.println("ref=" + refOb);
        }
        em.close();

    }
*/

}
