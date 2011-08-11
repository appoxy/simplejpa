package com.spaceprogram.simplejpa;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.time.DateUtils;
import org.junit.Assert;
import org.junit.Test;

import com.amazonaws.AmazonClientException;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * User: treeder
 * Date: Feb 8, 2008
 * Time: 1:03:57 PM
 */
public class PersistenceTests extends BaseTestClass {

    @Test
    public void listAllObjects() throws IOException, AmazonClientException, ExecutionException, InterruptedException {
        Class c = MyTestObject.class;
        listAllObjects(c);
        listAllObjects(MyTestObject2.class);
    }

    private void listAllObjects(Class c) throws AmazonClientException, InterruptedException, ExecutionException {
        System.out.println("listing all objects of type " + c);
        EntityManagerSimpleJPA em = (EntityManagerSimpleJPA) factory.createEntityManager();
        em.listAllObjectsRaw(c);
        em.close();
    }

    @Test
    public void persistObject() throws IOException {
        EntityManager em = factory.createEntityManager();

        MyTestObject object = new MyTestObject();
        object.setName("Scooby doo");
        object.setAge(100);
        Date now = new Date();
        object.setBirthday(now);
        object.setMultiValueProperty(Arrays.asList("me", "myself", "i"));
        em.persist(object);
        String id = object.getId();

        MyTestObject2 myObject2 = new MyTestObject2("shaggy", 131);
        object.setMyTestObject2(myObject2);
        em.persist(myObject2);
        em.persist(object); // had to resave to get object2's id
        em.close();

        em = factory.createEntityManager();

        object = em.find(MyTestObject.class, object.getId());
        Assert.assertEquals("Scooby doo", object.getName());
        Assert.assertEquals(id, object.getId());
        Assert.assertEquals(myObject2.getName(), object.getMyTestObject2().getName());
        Assert.assertEquals(new Integer(100), object.getAge());
        Assert.assertEquals(now, object.getBirthday());
        Assert.assertTrue(CollectionUtils.isEqualCollection(Arrays.asList("me", "myself", "i"), object.getMultiValueProperty()));

        // now delete object
        em.remove(object);
        em.remove(myObject2);

        // and make sure it's gone
        object = em.find(MyTestObject.class, object.getId());
        Assert.assertNull(object);
        em.close();
    }

    /**
     * This test also ensures that modifications on non-enhanced classes still work.
     *
     * @throws IOException
     * @throws InterruptedException
     */
    @Test
    public void persistThenModify() throws IOException, InterruptedException {
        EntityManagerSimpleJPA em = (EntityManagerSimpleJPA) factory.createEntityManager();

        MyTestObject object = new MyTestObject();
        object.setName("Scooby doo");
        object.setAge(100);
        object.setSomeDouble(new Double("123.456"));
        object.setMultiValueProperty(Arrays.asList("me", "myself", "i"));
        em.persist(object);
        String id = object.getId();

        MyTestObject2 myObject2 = new MyTestObject2("shaggy", 131);
        object.setMyTestObject2(myObject2);
        em.persist(myObject2);
        em.persist(object); // had to resave to get object2's id

        // now delete an attribute with the non-enhanced class
        object.setSomeDouble(null);
        object.setMultiValueProperty(Arrays.asList("not", "myself", "today"));
        object = em.merge(object);
        Assert.assertEquals(10, em.getLastOpStats().getAttsDeleted());

        em.close();

        Thread.sleep(1000);

        clearCaches();

        em = (EntityManagerSimpleJPA) factory.createEntityManager();
        object = em.find(MyTestObject.class, object.getId());
        Assert.assertEquals("Scooby doo", object.getName());
        Assert.assertEquals(id, object.getId());
        System.out.println("myobject2=" + myObject2);
        System.out.println("object22= " + object.getMyTestObject2());
        Assert.assertEquals(myObject2.getName(), object.getMyTestObject2().getName());
        Assert.assertEquals(new Integer(100), object.getAge());
        Assert.assertTrue(CollectionUtils.isEqualCollection(Arrays.asList("not", "myself", "today"), object.getMultiValueProperty()));

        object.setIncome(null);
        object = em.merge(object); // should not delete attributes because income was always null
        Assert.assertEquals(0, em.getLastOpStats().getAttsDeleted());

        System.out.println("age=" + object.getAge());
        object.setAge(null);
        object = em.merge(object);
        Assert.assertEquals(1, em.getLastOpStats().getAttsDeleted());
        em.close();

        Thread.sleep(1000);

        clearCaches();

        em = (EntityManagerSimpleJPA) factory.createEntityManager();
        object = em.find(MyTestObject.class, object.getId());
        Assert.assertEquals("Scooby doo", object.getName());
        Assert.assertEquals(id, object.getId());
        Assert.assertEquals(myObject2.getName(), object.getMyTestObject2().getName());
        System.out.println("new age=" + object.getAge());
        Assert.assertNull(object.getAge());

        // now delete object
        em.remove(object);
        em.remove(myObject2);

        // and make sure it's gone
        object = em.find(MyTestObject.class, object.getId());
        Assert.assertNull(object);
        em.close();
    }

    @Test
    public void persistAsync() throws IOException, ExecutionException, InterruptedException {
        SimpleEntityManager em = (SimpleEntityManager) factory.createEntityManager();

        MyTestObject object = new MyTestObject();
        object.setName("Scooby doo");
        object.setAge(100);
        Future future = em.persistAsync(object);
        future.get();
        String id = object.getId();
        em.close();

        em = (SimpleEntityManager) factory.createEntityManager();

        object = em.find(MyTestObject.class, object.getId());
        Assert.assertEquals("Scooby doo", object.getName());
        Assert.assertEquals(id, object.getId());

        // now delete object
        em.remove(object);

        // and make sure it's gone
        object = em.find(MyTestObject.class, object.getId());
        Assert.assertNull(object);
        em.close();
    }

    int counter = 0;

    private MyTestObject makeTestObjects(EntityManager em) {

        MyTestObject object = new MyTestObject();
        object.setName("Some Random Object");
        object.setAge(100);
        em.persist(object); // saving here first to get an ID for bi-directional stuff (temp solution)

        object = new MyTestObject();
        object.setName("Scooby doo");
        object.setAge(12);
        object.setIncome(50507d);
        object.setSomeDouble(44444.55555);
        object.setSomeLong(88888L);
        object.setBirthday(new Date());
        object.setSomeBigDecimal(new BigDecimal("888888.999999999"));
        object.setBigString("this is a huge string");
        em.persist(object); // saving here first to get an ID for bi-directional stuff (temp solution)

        MyTestObject2 ob2 = new MyTestObject2("shaggy1", counter++);
        em.persist(ob2);
        object.setMyTestObject2(ob2);

        ob2 = new MyTestObject2("shaggy2", counter++);
        ob2.setMyTestObject(object);
        em.persist(ob2);
        object.addToMyList(ob2);

        MyTestObject3 ob3 = new MyTestObject3();
        ob3.setSomeField3("querythis");
        object.setMyTestObject3(ob3);
        em.persist(ob3);

        em.persist(object);
        return object;
    }

    @Test
    public void queryObject() throws IOException {

        EntityManager em = factory.createEntityManager();
        Query query;
        List<MyTestObject> obs;
        MyTestObject originalObject = makeTestObjects(em);

        // no params
        query = em.createQuery("select o from " + MyTestObject.class.getName() + " o");
        obs = query.getResultList();
        Assert.assertEquals(2, obs.size());
        for (MyTestObject ob : obs) {
            System.out.println(ob);
            if (ob.getMyList() != null) {
                System.out.println("list not null: " + ob.getMyList().getClass());
                List<MyTestObject2> ob2s = ob.getMyList();
                for (MyTestObject2 ob2 : ob2s) {
                    System.out.println("ob2=" + ob2);
                }
            }
        }

        query = em.createQuery("select o from " + MyTestObject.class.getName() + " o where 1=1 and o.age = :age");
        query.setParameter("age", 12);
        obs = query.getResultList();
        Assert.assertEquals(1, obs.size());
        for (MyTestObject ob : obs) {
            System.out.println(ob);
            if (ob.getMyList() != null) {
                System.out.println("list not null: " + ob.getMyList().getClass());
                List<MyTestObject2> ob2s = ob.getMyList();
                for (MyTestObject2 ob2 : ob2s) {
                    System.out.println("ob2=" + ob2);
                }
            }
        }
        Assert.assertEquals(originalObject.getId(), obs.get(0).getId());
        Assert.assertEquals(originalObject.getName(), obs.get(0).getName());
        Assert.assertEquals(originalObject.getIncome(), obs.get(0).getIncome());
        Assert.assertEquals(originalObject.getBirthday(), obs.get(0).getBirthday());
        Assert.assertEquals(originalObject.getSomeLong(), obs.get(0).getSomeLong());
        Assert.assertEquals(originalObject.getSomeDouble(), obs.get(0).getSomeDouble());
        Assert.assertEquals(originalObject.getSomeBigDecimal(), obs.get(0).getSomeBigDecimal());
        Assert.assertEquals(originalObject.getBigString(), obs.get(0).getBigString());
        Assert.assertEquals(originalObject.getAge(), obs.get(0).getAge());
        System.out.println("Getting my list.size....");
        Assert.assertEquals(1, obs.get(0).getMyList().size());
        Assert.assertEquals(originalObject.getMyList().get(0).getName(), obs.get(0).getMyList().get(0).getName());

        // two filters
        query = em.createQuery("select o from MyTestObject o where o.income = :income and o.age = :age");
        query.setParameter("income", 50507.0);
        query.setParameter("age", 12);
        obs = query.getResultList();
        Assert.assertEquals(1, obs.size());
        for (MyTestObject ob : obs) {
            System.out.println(ob);
            if (ob.getMyList() != null) {
                System.out.println("list not null: " + ob.getMyList().getClass());
                List<MyTestObject2> ob2s = ob.getMyList();
                for (MyTestObject2 ob2 : ob2s) {
                    System.out.println("ob2=" + ob2);
                }
            }
        }
        Assert.assertEquals(originalObject.getId(), obs.get(0).getId());
        Assert.assertEquals(originalObject.getIncome(), obs.get(0).getIncome());
        Assert.assertEquals(obs.get(0).getMyList().size(), 1);
        Assert.assertEquals(originalObject.getMyList().get(0).getName(), obs.get(0).getMyList().get(0).getName());

        // no matches
        query = em.createQuery("select o from MyTestObject o where o.income = :income and o.age = :age");
        query.setParameter("income", 123.0);
        query.setParameter("age", 12);
        obs = query.getResultList();
        Assert.assertEquals(0, obs.size());

        em.close();
    }

    @Test
    public void queryOrderBy() throws ExecutionException, InterruptedException {
        EntityManagerSimpleJPA em = (EntityManagerSimpleJPA) factory.createEntityManager();

        Query query;
        List<MyTestObject> obs;

        System.out.println("make sure domain is empty");        
        query = em.createQuery("select o from MyTestObject o where o.age >= '0' order by o.age");
        obs = query.getResultList();
        Assert.assertEquals(0, obs.size());
        
        Future future = null;
        int numItems = 120;
        for (int i = 0; i < numItems; i++) {
            MyTestObject object = new MyTestObject();
            object.setName("Scooby doo");
            object.setAge(i);
            System.out.println("persisting " + i);
            future = em.persistAsync(object);
        }
        future.get();

        Thread.sleep(7000);

        System.out.println("querying for all objects and checking order...");
        query = em.createQuery("select o from MyTestObject o where o.age >= '0' order by o.age");
        obs = query.getResultList();
        System.out.println("obs.size=" + obs.size());
        Assert.assertEquals(numItems, obs.size());

        for (int i = 0; i < obs.size(); i++) {
            MyTestObject myTestObject = obs.get(i);
            System.out.println("age=" + myTestObject.getAge());
            Assert.assertEquals(i, myTestObject.getAge().intValue());
        }

        System.out.println("querying for all objects and checking REVERSE order...");
        query = em.createQuery("select o from MyTestObject o where o.age >= '0' order by o.age desc");
        obs = query.getResultList();
        System.out.println("obs.size=" + obs.size());
        Assert.assertEquals(numItems, obs.size());

        int j = numItems - 1;
        for (int i = 0; i < obs.size(); i++) {
            MyTestObject myTestObject = obs.get(i);
            System.out.println("age=" + myTestObject.getAge());
            Assert.assertEquals(j, myTestObject.getAge().intValue());
            j--;
        }
        em.close();
    }

    @Test
    public void count() throws ExecutionException, InterruptedException {
        EntityManagerSimpleJPA em = (EntityManagerSimpleJPA) factory.createEntityManager();

        Query query;
        List<MyTestObject> obs;

        Future future = null;
        int numItems = 120;
        for (int i = 0; i < numItems; i++) {
            MyTestObject object = new MyTestObject();
            object.setName("Scooby doo");
            object.setAge(i);
            System.out.println("persisting " + i);
            future = em.persistAsync(object);
        }
        future.get();

        Thread.sleep(5000);

        query = em.createQuery("select count(o) from MyTestObject o");
        List results = query.getResultList();
        System.out.println("obs.size=" + results.size());
        Assert.assertEquals(1, results.size());

        for (Object ob : results) {
            System.out.println("ob=" + ob);
        }
        Assert.assertEquals(120L, results.get(0));

        em.close();
    }


    @Test
    public void deleteObject() {
        EntityManager em = factory.createEntityManager();

        MyTestObject ob = new MyTestObject();
        ob.setName("some name");
        em.persist(ob);
        em.getTransaction().commit();
        em.close();

        em = factory.createEntityManager();
        // make sure it's saved
        MyTestObject found = em.find(MyTestObject.class, ob.getId());
        Assert.assertEquals(ob.getId(), found.getId());

        // now we'll delete it
        em.remove(found);
        found = em.find(MyTestObject.class, ob.getId());
        Assert.assertNull(found);

        em.close();
    }

    @Test
    public void nullingAttributes() throws InterruptedException {
        EntityManager em = factory.createEntityManager();
        Query query;
        List<MyTestObject> obs;

        MyTestObject originalObject = makeTestObjects(em);
        System.out.println("age before=" + originalObject.getAge());
        originalObject.setAge(null);
        em.merge(originalObject);

        clearCaches();

        Thread.sleep(3000);

        // now query for it
        MyTestObject fresh = em.find(MyTestObject.class, originalObject.getId());

        Assert.assertEquals(null, fresh.getAge());
        // and other stuff is intact
        Assert.assertEquals(originalObject.getName(), fresh.getName());
        em.close();

    }

    /**
     * THIS FAILS IF TOO MANY OBJECTS COME BACK FOR THE SUB QUERY! MIGHT WANT TO JUST DISALLOW QUERYING DOWN COLLECTIONS IN A GRAPH.
     * <p/>
     * javax.persistence.PersistenceException: com.xerox.amazonws.sdb.SDBException: Client error : Too many predicates in the filter expression.
     */
    @Test
    public void queryDownGraph() {
        EntityManagerSimpleJPA em = (EntityManagerSimpleJPA) factory.createEntityManager();
        Query query;
        List<MyTestObject> obs;

        MyTestObject originalObject = makeTestObjects(em);

        int queryCountBefore = em.getQueryCount();
        // This should not query for the second object, just use id reference
        query = em.createQuery("select o from MyTestObject o where o.myTestObject2.id = :id2");
        query.setParameter("id2", originalObject.getMyTestObject2().getId());
        obs = query.getResultList();
        Assert.assertEquals(1, obs.size());
        Assert.assertEquals(queryCountBefore + 1, em.getQueryCount());
        for (MyTestObject ob : obs) {
            System.out.println(ob);
            if (ob.getMyList() != null) {
                System.out.println("list not null: " + ob.getMyList().getClass());
                List<MyTestObject2> ob2s = ob.getMyList();
                for (MyTestObject2 ob2 : ob2s) {
                    System.out.println("ob2=" + ob2);
                }
            }
        }

        // This should query for the sub object, then apply it to the first query for a total of 2 queries
        queryCountBefore = em.getQueryCount();
        System.out.println("STARTING querycount=" + queryCountBefore);
        query = em.createQuery("select o from MyTestObject o where o.myTestObject2.name = :id2");
        System.out.println("p2=" + em.getQueryCount());
        query.setParameter("id2", originalObject.getMyTestObject2().getName());
        System.out.println("p3=" + em.getQueryCount());
        obs = query.getResultList(); // +2 for querying down graph
        System.out.println("p4=" + em.getQueryCount());
        Assert.assertEquals(1, obs.size()); // shouldn't hit database.
        System.out.println("p5=" + em.getQueryCount());
        Assert.assertEquals(queryCountBefore + 2, em.getQueryCount());
        Assert.assertEquals(originalObject.getMyTestObject2().getName(), obs.get(0).getMyTestObject2().getName());
        for (MyTestObject ob : obs) {
            System.out.println(ob);
            if (ob.getMyList() != null) {
                System.out.println("list not null: " + ob.getMyList().getClass());
                List<MyTestObject2> ob2s = ob.getMyList();
                for (MyTestObject2 ob2 : ob2s) {
                    System.out.println("ob2=" + ob2);
                }
            }
        }

        // now test querying on two different objects down graph
        queryCountBefore = em.getQueryCount();
        query = em.createQuery("select o from MyTestObject o where o.myTestObject2.name = :id2 and o.myTestObject3.someField3 = :field3");
        query.setParameter("id2", originalObject.getMyTestObject2().getName());
        query.setParameter("field3", originalObject.getMyTestObject3().getSomeField3());
        obs = query.getResultList();
        Assert.assertEquals(1, obs.size());
        Assert.assertEquals(queryCountBefore + 3, em.getQueryCount());
        Assert.assertEquals(originalObject.getMyTestObject2().getName(), obs.get(0).getMyTestObject2().getName());
        for (MyTestObject ob : obs) {
            System.out.println(ob);
            if (ob.getMyList() != null) {
                System.out.println("list not null: " + ob.getMyList().getClass());
                List<MyTestObject2> ob2s = ob.getMyList();
                for (MyTestObject2 ob2 : ob2s) {
                    System.out.println("ob2=" + ob2);
                }
            }
        }
        em.close();
    }

    @Test
    public void testInheritance() {
        EntityManager em = factory.createEntityManager();

        MyInheritanceObject1 object1 = new MyInheritanceObject1();
        object1.setField("field value 1");
        em.persist(object1);

        MyInheritanceObject2 object2 = new MyInheritanceObject2();
        object2.setField("field value 2");
        object2.setFieldInSubClass2("sub class field 2");
        em.persist(object2);

        MyInheritanceObject3 object3 = new MyInheritanceObject3();
        object3.setField("field value 3");
        object3.setFieldInSubClass3("sub class field 3");
        em.persist(object3);

        em.getTransaction().commit();
        em.close();

        em = factory.createEntityManager();
        // make sure it's saved
        {
            MyInheritanceObject1 found = em.find(MyInheritanceObject1.class, object1.getId());
            Assert.assertEquals(object1.getId(), found.getId());
            Assert.assertTrue(object1.getClass().isAssignableFrom(found.getClass()));
            Assert.assertEquals(object1.getField(), found.getField());
        }
        {
            MyInheritanceObject2 found = em.find(MyInheritanceObject2.class, object2.getId());
            Assert.assertEquals(object2.getId(), found.getId());
            Assert.assertTrue(object2.getClass().isAssignableFrom(found.getClass()));
            Assert.assertEquals(object2.getField(), found.getField());
            Assert.assertEquals(object2.getFieldInSubClass2(), found.getFieldInSubClass2());
        }
        {
            MyInheritanceObject3 found = em.find(MyInheritanceObject3.class, object3.getId());
            Assert.assertEquals(object3.getId(), found.getId());
            Assert.assertTrue(object3.getClass().isAssignableFrom(found.getClass()));
            Assert.assertEquals(object3.getField(), found.getField());
            Assert.assertEquals(object3.getFieldInSubClass3(), found.getFieldInSubClass3());
        }
        em.close();

        {
            em = factory.createEntityManager();
            Query query = em.createQuery("select o from MyInheritanceObject2 o where o.field = :field");
            query.setParameter("field", object2.getField());
            List<MyInheritanceObject2> obs2 = query.getResultList();
            Assert.assertEquals(1, obs2.size());
            for (MyInheritanceObject2 found : obs2) {
                System.out.println("ob2=" + found);
                Assert.assertTrue(object2.getClass().isAssignableFrom(found.getClass()));
                Assert.assertEquals(object2.getField(), found.getField());
                Assert.assertEquals(object2.getFieldInSubClass2(), found.getFieldInSubClass2());
            }
            em.close();
        }

        {
            // now query on root class to make sure we get all objects back
            em = factory.createEntityManager();
            Query query = em.createQuery("select o from MyInheritanceObject1 o ");
            List<MyInheritanceObject1> obs2 = query.getResultList();
            Assert.assertEquals(3, obs2.size());
            for (MyInheritanceObject1 found : obs2) {
                System.out.println("ob=" + found);
                Assert.assertTrue(MyInheritanceObject1.class.isAssignableFrom(found.getClass()));
            }
            em.close();
        }
    }

    @Test
    public void testDifferentSyntax() {
        EntityManager em = factory.createEntityManager();

        Query query;
        List<MyTestObject> obs;

        MyTestObject originalObject = makeTestObjects(em);

        query = em.createQuery("select o from MyTestObject o where o.myTestObject2.id = :id2 ");
        query.setParameter("id2", originalObject.getMyTestObject2().getId());
        obs = query.getResultList();
        Assert.assertEquals(1, obs.size());
        for (MyTestObject ob : obs) {
            System.out.println(ob);
            if (ob.getMyList() != null) {
                System.out.println("list not null: " + ob.getMyList().getClass());
                List<MyTestObject2> ob2s = ob.getMyList();
                for (MyTestObject2 ob2 : ob2s) {
                    System.out.println("ob2=" + ob2);
                }
            }
        }
        em.close();
    }

    @Test
    public void testMoreThanMaxPerQuery() throws ExecutionException, InterruptedException {
        EntityManagerSimpleJPA em = (EntityManagerSimpleJPA) factory.createEntityManager();

        Query query;
        List<MyTestObject> obs;

        Future future = null;
        int numItems = 120;
        for (int i = 0; i < numItems; i++) {
            MyTestObject object = new MyTestObject();
            object.setName("Scooby doo");
            object.setAge(i);
            System.out.println("persisting " + i);
            future = em.persistAsync(object);
        }
        future.get();

        Thread.sleep(10000);

        System.out.println("querying for all objects...");
        query = em.createQuery("select o from MyTestObject o ");
        obs = query.getResultList();
        System.out.println("obs.size=" + obs.size());
        Assert.assertEquals(numItems, obs.size());
        Collections.sort(obs, new Comparator<MyTestObject>() {
            public int compare(MyTestObject o1, MyTestObject o2) {
                return o1.getAge().compareTo(o2.getAge());
            }
        });
        for (int i = 0; i < obs.size(); i++) {
            MyTestObject myTestObject = obs.get(i);
            System.out.println("age=" + i);
            Assert.assertEquals(i, myTestObject.getAge().intValue());
        }
        em.close();
    }


    @Test
    public void persistObjectWithEnum() throws IOException {
        EntityManager em = factory.createEntityManager();

        MyTestObject object = new MyTestObject();
        object.setMyEnumOrdinal(MyEnum.me);
        object.setMyEnumString(MyEnum.i);
        em.persist(object);
        String id = object.getId();
        em.close();

        em = factory.createEntityManager();
        object = em.find(MyTestObject.class, id);
        Assert.assertEquals(MyEnum.me, object.getMyEnumOrdinal());
        Assert.assertEquals(MyEnum.i, object.getMyEnumString());

        // now delete object
        em.remove(object);
        em.close();
    }

    @Test
    public void queryIsNull() throws InterruptedException {
        EntityManager em = factory.createEntityManager();

        MyTestObject object = new MyTestObject();
        object.setName("fred");
        em.persist(object);
        String id = object.getId();
        em.close();

        Thread.sleep(3000);

        em = factory.createEntityManager();
        Query query = em.createQuery("select o from MyTestObject o where o.income is null");
        List<MyTestObject> obs = query.getResultList();
        for (MyTestObject ob : obs) {
            System.out.println(ob);
        }
        Assert.assertEquals(1, obs.size());

        query = em.createQuery("select o from MyTestObject o where o.income is not null");
        obs = query.getResultList();
        for (MyTestObject ob : obs) {
            System.out.println(ob);
        }
        Assert.assertEquals(0, obs.size());

        query = em.createQuery("select o from MyTestObject o where o.name is not null");
        obs = query.getResultList();
        for (MyTestObject ob : obs) {
            System.out.println(ob);
        }
        Assert.assertEquals(1, obs.size());

        em.close();
    }

    @Test
    public void testEntityListeners() throws InterruptedException {
        EntityManager em = factory.createEntityManager();

        MyTestObject3 object = new MyTestObject3();
        object.setSomeField3("fred");
        em.persist(object);
        Date firstCreated = object.getCreated();
        Date firstUpdated = object.getUpdated();
        Assert.assertNotNull(firstCreated);
        Assert.assertNotNull(firstUpdated);
        em.close();

        Thread.sleep(3000);

        em = factory.createEntityManager();
        object = em.find(MyTestObject3.class, object.getId());
        System.out.println("object=" + object);
        Assert.assertNotNull(object);
        Assert.assertEquals(firstCreated, object.getCreated());
        Assert.assertEquals(firstUpdated, object.getUpdated());

        object.setSomeField3("fred updated");
        em.merge(object);
        Assert.assertFalse(object.getUpdated().equals(firstUpdated));
        em.close();

        // ensure it works when inherited too
        em = factory.createEntityManager();
        MyTestObject5Ext3 object5Ext3 = new MyTestObject5Ext3();
        object5Ext3.setSomeField3("fred");
        em.persist(object5Ext3);
        firstCreated = object5Ext3.getCreated();
        firstUpdated = object5Ext3.getUpdated();
        Assert.assertNotNull(firstCreated);
        Assert.assertNotNull(firstUpdated);
        em.close();
    }

    @Test
    public void testStartsWithQuery() {
        EntityManager em = factory.createEntityManager();

        MyTestObject3 object = new MyTestObject3();
        object.setSomeField3("fred and barney");
        em.persist(object);
        em.close();

        em = factory.createEntityManager();
        Query query = em.createQuery("select o from MyTestObject3 o where o.someField3 like :x");
        query.setParameter("x", "fred and%");
        List<MyTestObject3> obs = query.getResultList();
        for (MyTestObject3 ob : obs) {
            System.out.println(ob);
        }
        Assert.assertEquals(1, obs.size());
        em.close();
    }

    @Test
    public void testSimpleDBQuery() {
        EntityManager em = factory.createEntityManager();
        String name = UUID.randomUUID().toString();
        MyTestObject o = new MyTestObject(name);
        em.persist(o);
        em.close();

        em = factory.createEntityManager();
        Query q = em.createNativeQuery("select * from MyTestObject where name = :name");
        q.setParameter("name", name);
        MyTestObject o1 = (MyTestObject)q.getSingleResult();
        Assert.assertEquals(name, o1.getName());
        Assert.assertNotNull(o1.getId());
        em.close();
    }

    @Test(expected = PersistenceException.class)
    public void testEndsWithQuery() {
        EntityManager em = factory.createEntityManager();

        MyTestObject3 object = new MyTestObject3();
        object.setSomeField3("fred and barney");
        em.persist(object);
        em.close();

        em = factory.createEntityManager();
        Query query = em.createQuery("select o from MyTestObject3 o where o.someField3 like :x");
        query.setParameter("x", "%fred and"); // bad
        System.out.println("query=" + query);
        List<MyTestObject3> obs = query.getResultList();
        System.out.println("shouldn't make it here");
        for (MyTestObject3 ob : obs) {
            System.out.println(ob);
        }
        Assert.assertEquals(1, obs.size());
        em.close();
    }

    @Test
    public void testEscaping() throws IOException {
        EntityManager em = factory.createEntityManager();

        MyTestObject object = new MyTestObject();
        object.setName("Scooby 'doo");
        em.persist(object);
        object = new MyTestObject();
        object.setName("Shaggy \\");
        em.persist(object);

        em.close();

        em = factory.createEntityManager();
        {
            Query query = em.createQuery("select o from MyTestObject o where o.name like :x and o.name > ''");
            query.setParameter("x", "Scooby 'd%");
            List<MyTestObject> obs = query.getResultList();
            for (MyTestObject ob : obs) {
                System.out.println(ob);
            }
            Assert.assertEquals(1, obs.size());
        }
        {
            Query query = em.createQuery("select o from MyTestObject o where o.name like :x");
            query.setParameter("x", "Shaggy \\%");
            List<MyTestObject> obs = query.getResultList();
            for (MyTestObject ob : obs) {
                System.out.println(ob);
            }
            Assert.assertEquals(1, obs.size());
        }

        em.close();
    }

    @Test
    public void testDates() throws IOException {
        EntityManager em = factory.createEntityManager();

        MyTestObject object = new MyTestObject();
        object.setName("Scooby 'doo");
        object.setBirthday(DateUtils.add(new Date(), Calendar.DAY_OF_MONTH, -5));
        em.persist(object);
        String id = object.getId();
        object = new MyTestObject();
        object.setName("Shaggy 2");
        object.setBirthday(new Date());
        em.persist(object);

        em.close();

        em = factory.createEntityManager();
        {
            Query query = em.createQuery("select o from MyTestObject o where o.birthday > :from and o.birthday < :to and o.id = :id");
            query.setParameter("from", DateUtils.add(new Date(), Calendar.DAY_OF_MONTH, -7));
            query.setParameter("to", DateUtils.add(new Date(), Calendar.DAY_OF_MONTH, -5));
            query.setParameter("id", id);
            List<MyTestObject> obs = query.getResultList();
            for (MyTestObject ob : obs) {
                System.out.println(ob);
            }
            Assert.assertEquals(1, obs.size());
        }
        em.close();
    }

    @Test
    public void testPersistManyToMany() {
        EntityManager em = factory.createEntityManager();

        ManyToManyTestObject1 objectOneOne = new ManyToManyTestObject1();
        ManyToManyTestObject1 objectOneTwo = new ManyToManyTestObject1();
        ManyToManyTestObject2 objectTwoOne = new ManyToManyTestObject2();
        ManyToManyTestObject2 objectTwoTwo = new ManyToManyTestObject2();

        em.persist(objectOneOne);
        em.persist(objectOneTwo);
        em.persist(objectTwoOne);
        em.persist(objectTwoTwo);

        objectOneOne.setOtherObjects(Arrays.asList(objectTwoOne, objectTwoTwo));
        objectOneTwo.setOtherObjects(Arrays.asList(objectTwoOne, objectTwoTwo));
        objectTwoOne.setOtherObjects(Arrays.asList(objectOneOne, objectOneTwo));
        objectTwoTwo.setOtherObjects(Arrays.asList(objectOneOne, objectOneTwo));

        em.persist(objectOneOne);
        em.persist(objectOneTwo);
        em.persist(objectTwoOne);
        em.persist(objectTwoTwo);

        objectOneOne = em.find(ManyToManyTestObject1.class, objectOneOne.getId());
        Assert.assertEquals(2, objectOneOne.getOtherObjects().size());
        Assert.assertEquals(2, objectOneOne.getOtherObjects().iterator().next().getOtherObjects().size());
    }
}
