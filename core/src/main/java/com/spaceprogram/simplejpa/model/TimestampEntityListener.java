package com.spaceprogram.simplejpa.model;

import javax.persistence.PreUpdate;
import javax.persistence.PrePersist;
import java.util.Date;

/**
 * User: treeder
 * Date: Jun 2, 2008
 * Time: 11:22:45 AM
 */
public class TimestampEntityListener {

    @PrePersist
    public void prePersist(Object object) {
        if(object instanceof Timestamped){
            Timestamped timestamped = (Timestamped) object;
            Date now = new Date();
            timestamped.setCreated(now);
            timestamped.setUpdated(now);
        }
    }

    @PreUpdate
    public void preUpdate(Object object) {
        if(object instanceof Timestamped){
            Timestamped timestamped = (Timestamped) object;
            timestamped.setUpdated(new Date());
        }
    }
}
