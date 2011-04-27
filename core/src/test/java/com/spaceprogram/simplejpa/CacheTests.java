package com.spaceprogram.simplejpa;

import org.junit.Test;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.List;

/**
 * Must enable 2nd level cache to make this tests useful.
 *
 * User: treeder
 * Date: Jun 9, 2008
 * Time: 10:55:56 PM
 */
public class CacheTests extends BaseTestClass{

    /**
     * This test ensures that objects in the second level cache where the EntityManager has been closed already
     * can still lazy load their references.
     */
    @Test
    public void testManyToOneAfterClosed(){
        EntityManager em = factory.createEntityManager();
        // create MyTestObject1 and a 2 with a reference from 1 to 2.
        MyTestObject4 o4 = new MyTestObject4();
        o4.setName4("04");
        em.persist(o4);
        MyTestObject2 o2 = new MyTestObject2("ob2", 123);
        o2.setMyTestObject4(o4);
        em.persist(o2);
        MyTestObject o1 = new MyTestObject();
        o1.setMyTestObject2(o2);
        em.persist(o1);
        em.getTransaction().commit();
        // close em
        em.close();

        // clean out the caches
        clearCaches();

        em = factory.createEntityManager();
        // get all MyTestObject2's to get them in 2nd level cache
        Query query = em.createQuery("select o from MyTestObject2 o");
        List<MyTestObject2> resultList = query.getResultList();
        for (MyTestObject2 myTestObject2 : resultList) {
            System.out.println(myTestObject2);
        }
        // close em
        em.close();

        em = factory.createEntityManager();
        // now get 1's and touch the referenced 2's, ie: getMyTestObject2();
        query = em.createQuery("select o from MyTestObject o");
        List<MyTestObject> resultList2 = query.getResultList();
        for (MyTestObject myTestObject : resultList2) {
            System.out.println(myTestObject.getMyTestObject2().getMyTestObject4());
        }
        em.close();
    }
}
