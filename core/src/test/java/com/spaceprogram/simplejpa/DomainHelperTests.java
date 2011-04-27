package com.spaceprogram.simplejpa;

import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import com.amazonaws.services.simpledb.AmazonSimpleDB;
import com.amazonaws.services.simpledb.model.CreateDomainRequest;
import com.amazonaws.services.simpledb.model.DeleteDomainRequest;
import com.amazonaws.services.simpledb.model.Item;
import com.amazonaws.services.simpledb.model.PutAttributesRequest;
import com.amazonaws.services.simpledb.model.ReplaceableAttribute;
import com.amazonaws.services.simpledb.model.SelectResult;

/**
 * Must enable 2nd level cache to make this tests useful.
 *
 * User: normj
 * Date: Sept 1, 2010
 * Time: 12:56:00 PM
 */
public class DomainHelperTests extends BaseTestClass {

	@Test
	public void findByIdTest() {
        EntityManagerSimpleJPA em = (EntityManagerSimpleJPA)factory.createEntityManager();
        
        AmazonSimpleDB sdbClient = em.getSimpleDb();
        
        String domainName = "simplejpa-domainhelper-tests";
        
        sdbClient.createDomain(new CreateDomainRequest().withDomainName(domainName));
        try {
        	Assert.assertNull(DomainHelper.findItemById(sdbClient, domainName, "noexist"));
        	
        	sdbClient.putAttributes(
        			new PutAttributesRequest()
        				.withItemName("exist")
        				.withDomainName(domainName)
        				.withAttributes(new ReplaceableAttribute("name", "value", true)));
        	
        	Assert.assertNotNull(DomainHelper.findItemById(sdbClient, domainName, "exist"));
        }
        finally {
        	sdbClient.deleteDomain(new DeleteDomainRequest().withDomainName(domainName));
        }
	}
	
	@Test
	public void listAllItemsTests() {
        EntityManagerSimpleJPA em = (EntityManagerSimpleJPA)factory.createEntityManager();
        
        AmazonSimpleDB sdbClient = em.getSimpleDb();
        
        String domainName = "simplejpa-domainhelper-tests";
        
        sdbClient.createDomain(new CreateDomainRequest().withDomainName(domainName));
        try {
        	
        	for(int i = 0; i < 3; i++) {
            	sdbClient.putAttributes(
            			new PutAttributesRequest()
            				.withItemName("thing" + i)
            				.withDomainName(domainName)
            				.withAttributes(new ReplaceableAttribute("name", "value", true)));        		
        	}

        	List<Item> items = DomainHelper.listAllItems(sdbClient, domainName);
        	Assert.assertEquals(3, items.size());
        }
        finally {
        	sdbClient.deleteDomain(new DeleteDomainRequest().withDomainName(domainName));
        }
	}
	
	@Test
	public void selectItemsTests() {
        EntityManagerSimpleJPA em = (EntityManagerSimpleJPA)factory.createEntityManager();
        
        AmazonSimpleDB sdbClient = em.getSimpleDb();
        
        String domainName = "simplejpa-domainhelper-tests";
        
        sdbClient.createDomain(new CreateDomainRequest().withDomainName(domainName));
        try {
        	for(int i = 0; i < 10; i++) {
            	sdbClient.putAttributes(
            			new PutAttributesRequest()
            				.withItemName("thing" + i)
            				.withDomainName(domainName)
            				.withAttributes(new ReplaceableAttribute("name", "value", true)));        		
        	}
        	
        	SelectResult result = DomainHelper.selectItems(sdbClient, String.format("select * from `%s` LIMIT 3", domainName), null);
        	Assert.assertEquals(3, result.getItems().size());
        	Assert.assertNotNull(result.getNextToken());

        	result = DomainHelper.selectItems(sdbClient, String.format("select * from `%s` LIMIT 3", domainName), result.getNextToken());
        	Assert.assertEquals(3, result.getItems().size());
        	Assert.assertNotNull(result.getNextToken());

        	result = DomainHelper.selectItems(sdbClient, String.format("select * from `%s` LIMIT 3", domainName), result.getNextToken());
        	Assert.assertEquals(3, result.getItems().size());
        	Assert.assertNotNull(result.getNextToken());

        	result = DomainHelper.selectItems(sdbClient, String.format("select * from `%s` LIMIT 3", domainName), result.getNextToken());
        	Assert.assertEquals(1, result.getItems().size());
        	Assert.assertNull(result.getNextToken());
        }
        finally {
        	sdbClient.deleteDomain(new DeleteDomainRequest().withDomainName(domainName));
        }
	}	
	
	@Test
	public void selectItemsWithWhereTests() {
        EntityManagerSimpleJPA em = (EntityManagerSimpleJPA)factory.createEntityManager();
        
        AmazonSimpleDB sdbClient = em.getSimpleDb();
        
        String domainName = "simplejpa-domainhelper-tests";
        
        sdbClient.createDomain(new CreateDomainRequest().withDomainName(domainName));
        try {
        	for(int i = 0; i < 10; i++) {
            	sdbClient.putAttributes(
            			new PutAttributesRequest()
            				.withItemName("thing" + i)            				
            				.withDomainName(domainName)
            				.withAttributes(new ReplaceableAttribute("name", "value", true)));        		
        	}
        	
        	SelectResult result = DomainHelper.selectItems(sdbClient, domainName, "name = 'value' LIMIT 3", null);
        	Assert.assertEquals(3, result.getItems().size());
        	Assert.assertNotNull(result.getNextToken());

        	result = DomainHelper.selectItems(sdbClient, domainName, "name = 'value' LIMIT 3", result.getNextToken());
        	Assert.assertEquals(3, result.getItems().size());
        	Assert.assertNotNull(result.getNextToken());

        	result = DomainHelper.selectItems(sdbClient, domainName, "name = 'value' LIMIT 3", result.getNextToken());
        	Assert.assertEquals(3, result.getItems().size());
        	Assert.assertNotNull(result.getNextToken());

        	result = DomainHelper.selectItems(sdbClient, domainName, "name = 'value' LIMIT 3", result.getNextToken());
        	Assert.assertEquals(1, result.getItems().size());
        	Assert.assertNull(result.getNextToken());
        }
        finally {
        	sdbClient.deleteDomain(new DeleteDomainRequest().withDomainName(domainName));
        }
	}		
}
