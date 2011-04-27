package com.spaceprogram.simplejpa;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.simpledb.AmazonSimpleDB;
import com.amazonaws.services.simpledb.model.DeleteDomainRequest;
import com.amazonaws.services.simpledb.model.PutAttributesRequest;
import com.amazonaws.services.simpledb.model.ReplaceableAttribute;

import org.junit.*;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

/**
 * User: treeder
 * Date: Apr 3, 2008
 * Time: 2:32:39 PM
 */
public class UtilTests {
    private static EntityManagerFactoryImpl factory;

    @BeforeClass
    public static void setupEntityManagerFactory() throws IOException {
        factory = new EntityManagerFactoryImpl("testunit", null);
        /*
        This doesn't work when not packaged in jar or something.
        (EntityManagerFactoryImpl) Persistence.createEntityManagerFactory("persistenceSDB");
        */
    }

    @AfterClass
    public static void tearDownEntityManagerFactory() {
        factory.close();
    }
    @After
    public void deleteAll() throws AmazonClientException {
        EntityManagerSimpleJPA em = (EntityManagerSimpleJPA) factory.createEntityManager();
        AmazonSimpleDB db = em.getSimpleDb();
        
        String domainName = em.getDomainName(MyTestObject.class);
        db.deleteDomain(new DeleteDomainRequest().withDomainName(domainName));

        domainName = em.getDomainName(MyTestObject2.class);
        db.deleteDomain(new DeleteDomainRequest().withDomainName(domainName));

        domainName = em.getDomainName(MyInheritanceObject1.class);
        db.deleteDomain(new DeleteDomainRequest().withDomainName(domainName));
        
        em.close();
    }

    @Ignore("Rename feature is currently broken")
    @Test
    public void rename() throws IOException, ExecutionException, InterruptedException, AmazonClientException {
        EntityManagerSimpleJPA em = (EntityManagerSimpleJPA) factory.createEntityManager();
        AmazonSimpleDB db = em.getSimpleDb();

        String domainName = em.getFactory().getDomainName(MyTestObject.class);
        em.getFactory().createIfNotExistDomain(domainName);
        
        String id = "abc123";
        List<ReplaceableAttribute> atts = new ArrayList<ReplaceableAttribute>();
        atts.add(new ReplaceableAttribute("id", id, true));
        atts.add(new ReplaceableAttribute("nameOld", "Bullwinkle", true));
        db.putAttributes(new PutAttributesRequest()
        	.withDomainName(domainName)
        	.withItemName(id)
        	.withAttributes(atts));

        MyTestObject object;
        object = em.find(MyTestObject.class, id);
        Assert.assertNull(object.getName());
        System.out.println("name before renameField = " + object.getName());
        Assert.assertEquals(id, object.getId());

        em.renameField(MyTestObject.class, "nameOld", "name");
        em.close();

        // now find it again and the name should be good
        em = (EntityManagerSimpleJPA) factory.createEntityManager();
        object = em.find(MyTestObject.class, id);
        Assert.assertEquals("Bullwinkle", object.getName());
        System.out.println("name after renameField = " + object.getName());
        Assert.assertEquals(id, object.getId());

        // now delete object
        em.remove(object);

        // and make sure it's gone
        object = em.find(MyTestObject.class, object.getId());
        Assert.assertNull(object);
        em.close();
    }

    @Ignore("Rename Subclass feature is currently broken")
    @Test
    public void renameSubclass() throws IOException, ExecutionException, InterruptedException, AmazonClientException {
        EntityManagerSimpleJPA em = (EntityManagerSimpleJPA) factory.createEntityManager();
        AmazonSimpleDB db = em.getSimpleDb();

        String domainName = em.getFactory().getDomainName(MyInheritanceObject1.class);
        em.getFactory().createIfNotExistDomain(domainName);
        
        String id = "abc123";
        List<ReplaceableAttribute> atts = new ArrayList<ReplaceableAttribute>();
        atts.add(new ReplaceableAttribute("id", id, true));
        atts.add(new ReplaceableAttribute(EntityManagerFactoryImpl.DTYPE, "MyInheritanceObjectOld", true));
        atts.add(new ReplaceableAttribute("fieldInSubClass2", "Bullwinkle", true));
        
        db.putAttributes(new PutAttributesRequest()
	    	.withDomainName(domainName)
	    	.withItemName(id)
	    	.withAttributes(atts));

        MyInheritanceObject1 object;
        /*object = em.find(MyInheritanceObject2.class, id);
        Assert.assertNull(object);
        object = em.find(MyInheritanceObject1.class, id);
        Assert.assertNotNull(object);
        Assert.assertEquals(id, object.getId());*/

        em.renameSubclass("MyInheritanceObjectOld", MyInheritanceObject2.class);
        em.close();

        // now find it again and the name should be good
        em = (EntityManagerSimpleJPA) factory.createEntityManager();
        MyInheritanceObject2 object2 = em.find(MyInheritanceObject2.class, id);
        Assert.assertNotNull(object2);
        Assert.assertEquals("Bullwinkle", object2.getFieldInSubClass2());
        Assert.assertEquals(id, object2.getId());

        // now delete object
        em.remove(object2);

        // and make sure it's gone
        object = em.find(MyInheritanceObject1.class, object2.getId());
        Assert.assertNull(object);
        em.close();
    }

}
