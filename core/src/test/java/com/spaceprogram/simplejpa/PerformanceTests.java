package com.spaceprogram.simplejpa;

import org.apache.commons.lang.time.StopWatch;
import org.junit.Test;

import javax.persistence.Query;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * User: treeder
 * Date: May 14, 2008
 * Time: 3:47:32 PM
 */
public class PerformanceTests extends BaseTestClass{

    @Test
    public void testPutQueryDelete() throws ExecutionException, InterruptedException {
        int numItems = 1;
        String x;
        EntityManagerSimpleJPA em = (EntityManagerSimpleJPA) factory.createEntityManager();
        PerformanceTestObject o = new PerformanceTestObject();
        o.setS1("first to create domain");
        em.persist(o);
        StopWatch stopWatch = new StopWatch();

        String s1a = "attribute1";
        String s2a = "attribute2";
        Future<PerformanceTestObject> lastFuture = null;
        stopWatch.start();
        for (int i = 0; i < numItems; i++) {
            o = new PerformanceTestObject();
            o.setS1(s1a);
            o.setS2(s2a);
            lastFuture = em.persistAsync(o);
        }
        lastFuture.get(); // not 100% accurate, but good enough
        stopWatch.stop();
        System.out.println("puts duration=" + stopWatch.getTime() + ", " + em.getTotalOpStats().getPuts() + " items put.");

        Thread.sleep(5000);

        stopWatch.reset();
        stopWatch.start();
        Query query = em.createQuery("select o from PerformanceTestObject o");
        List<PerformanceTestObject> resultList = query.getResultList();
        System.out.println("iterating result list...");
        int i = 0;
        for (PerformanceTestObject performanceTestObject : resultList) {

            i++;
            if(i % 100 == 0){
                System.out.println(i);
            }
        }
        stopWatch.stop();
        System.out.println("query ALL duration=" + stopWatch.getTime() + ", " + em.getTotalOpStats().getGets() + " items got.");

        stopWatch.reset();
        stopWatch.start();
        System.out.println("Deleting ALL...");
        for (PerformanceTestObject performanceTestObject : resultList) {
            lastFuture = em.removeAsync(o);
        }
        lastFuture.get();
        stopWatch.stop();
        System.out.println("delete duration=" + stopWatch.getTime() + ", " + resultList.size() + " items deleted.");
        System.out.println("sleeping...");
        Thread.sleep(30000);

        em.close();

    }

    @Test
    public void testLazyListRetrievalPerformance() throws InterruptedException, ExecutionException {
        EntityManagerSimpleJPA em = (EntityManagerSimpleJPA) factory.createEntityManager();

        Query query;
        List<MyTestObject> obs;

        long start = System.currentTimeMillis();
        int numItems = 120;
        Future future = null;
        for (int i = 0; i < numItems; i++) {
            MyTestObject object = new MyTestObject();
            object.setName("Scooby doo");
            object.setAge(i);
            System.out.println("persisting " + i);
            future = em.persistAsync(object);
        }

        // let them save
        System.out.println("Waiting for all persists to complete.");
        future.get();
        long duration = System.currentTimeMillis() - start;
        printAndLog("duration of persists=" + duration);

        start = System.currentTimeMillis();
        System.out.println("querying for all objects...");
        query = em.createQuery("select o from MyTestObject o ");
        obs = query.getResultList();
        for (MyTestObject ob : obs) {
            System.out.println("ob=" + ob);
        }
        duration = System.currentTimeMillis() - start;
        printAndLog("duration of retreival and prints=" + duration);

        start = System.currentTimeMillis();
        System.out.println("querying for all objects...");
        query = em.createQuery("select o from MyTestObject o ");
        obs = query.getResultList();
        for (MyTestObject ob : obs) {
            System.out.println("ob=" + ob);
        }
        duration = System.currentTimeMillis() - start;
        printAndLog("duration of retreival and prints after first load=" + duration);

        em.close();
    }

}
