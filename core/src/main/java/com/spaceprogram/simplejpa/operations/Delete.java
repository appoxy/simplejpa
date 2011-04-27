package com.spaceprogram.simplejpa.operations;

import com.amazonaws.services.simpledb.model.DeleteAttributesRequest;
import com.spaceprogram.simplejpa.EntityManagerSimpleJPA;

import javax.persistence.PostRemove;
import javax.persistence.PreRemove;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * User: treeder
 * Date: Jun 29, 2008
 * Time: 6:01:41 PM
 */
public class Delete implements Callable {
    private static Logger logger = Logger.getLogger(Delete.class.getName());

    private EntityManagerSimpleJPA em;
    private Object toDelete;
    private String id;

    public Delete(EntityManagerSimpleJPA em, Object toDelete) {
        this.em = em;
        this.toDelete = toDelete;
        id = em.getId(toDelete);
        em.cacheRemove(toDelete.getClass(), id);
    }

    public Object call() throws Exception {
        String domainName = em.getOrCreateDomain(toDelete.getClass());
        if(logger.isLoggable(Level.FINE)) logger.fine("deleting item with id: " + id);
        em.invokeEntityListener(toDelete, PreRemove.class);
        this.em.getSimpleDb().deleteAttributes(new DeleteAttributesRequest()
        	.withDomainName(domainName)
        	.withItemName(id));
        em.invokeEntityListener(toDelete, PostRemove.class);
        return toDelete;
    }
}
