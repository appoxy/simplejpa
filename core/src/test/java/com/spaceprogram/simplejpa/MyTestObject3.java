package com.spaceprogram.simplejpa;

import com.spaceprogram.simplejpa.model.TimestampEntityListener;
import com.spaceprogram.simplejpa.model.Timestamped;

import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import java.util.Date;

/**
 * User: treeder
 * Date: Feb 18, 2008
 * Time: 5:26:21 PM
 */
@Entity
@EntityListeners({TimestampEntityListener.class})
public class MyTestObject3 extends MySuperClass implements Timestamped {
    private String someField3;

    private Date created;
    private Date updated;

    public String getSomeField3() {
        return someField3;
    }

    public void setSomeField3(String someField3) {
        this.someField3 = someField3;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Date getUpdated() {
        return updated;
    }

    public void setUpdated(Date updated) {
        this.updated = updated;
    }
}
