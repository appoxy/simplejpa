package com.spaceprogram.simplejpa;

import javax.persistence.*;
import java.util.Collection;

/**
 * Kerry Wright
 */
@Entity
public class ManyToManyTestObject2 {
    @Id
    private String id;

    @ManyToMany
    private Collection<ManyToManyTestObject1> otherObjects;

    public Collection<ManyToManyTestObject1> getOtherObjects() {
        return otherObjects;
    }

    public void setOtherObjects(Collection<ManyToManyTestObject1> otherObjects) {
        this.otherObjects = otherObjects;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
