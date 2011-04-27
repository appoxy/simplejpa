package com.spaceprogram.simplejpa.model;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

/**
 * Can use as a common base class.
 * 
 * User: treeder
 * Date: Feb 16, 2008
 * Time: 4:45:39 PM
 */

@MappedSuperclass
public class IdedBase implements Ided {
    protected String id;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
