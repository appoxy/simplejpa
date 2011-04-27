package com.spaceprogram.simplejpa;

import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.EntityManagerFactory;
import java.util.Map;

/**
 * Needs work.
 *
 * User: treeder
 * Date: Feb 10, 2008
 * Time: 6:25:13 PM
 */
public class PersistenceProviderImpl implements PersistenceProvider {

    public PersistenceProviderImpl() {
    }

    public EntityManagerFactory createEntityManagerFactory(String s, Map map) {
        System.out.println("createEntityManagerFactory");
        return new EntityManagerFactoryImpl(s, map);
    }

    public EntityManagerFactory createContainerEntityManagerFactory(PersistenceUnitInfo persistenceUnitInfo, Map map) {
        System.out.println("createContainerEntityManagerFactory");
        return new EntityManagerFactoryImpl(persistenceUnitInfo, map);
    }
}
