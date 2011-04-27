package com.spaceprogram.simplejpa;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;

/**
 * User: treeder
 * Date: Oct 18, 2008
 * Time: 4:53:48 PM
 */
public class ModelTests {
	@Ignore("This feature is not implemented")
    @Test
    public void testPut() throws ClassNotFoundException {
        ModelSub modelSub = new ModelSub();
        modelSub.setTitle("TitleX");
        modelSub.persist();
        Assert.assertNotNull(modelSub.getId());

        List<ModelSub> resultList = ModelSub.query(ModelSub.class).filter("title", "=", "TitleX").order("title", "asc").getResultList();
        Assert.assertEquals(1, resultList.size());
        ModelSub ret = resultList.get(0);
        Assert.assertEquals(ret.getTitle(), modelSub.getTitle());
    }
}
