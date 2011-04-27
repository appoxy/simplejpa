package com.spaceprogram.simplejpa;

import com.spaceprogram.simplejpa.model.Timestamped;

import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import java.util.Date;

/**
 * User: treeder
 * Date: Jul 29, 2008
 * Time: 6:49:33 PM
 */
public class SomeEntityListener {
    @PrePersist
    public void prePersist(Object object) {
        System.out.println("prePersist");
        if(object instanceof Timestamped){
            System.out.println("Setting timestamps.");
            Timestamped timestamped = (Timestamped) object;
            Date now = new Date();
            timestamped.setCreated(now);
            timestamped.setUpdated(now);
        }
    }

    @PreUpdate
    public void preUpdate(Object object) {
        System.out.println("preUpdate.");
        if(object instanceof Timestamped){
            System.out.println("Setting timestamps.");
            Timestamped timestamped = (Timestamped) object;
            timestamped.setUpdated(new Date());
        }
    }
}
