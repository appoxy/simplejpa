package com.spaceprogram.simplejpa;

import javax.persistence.*;
import java.util.Collection;

/**
 * Kerry Wright
 */
@Entity
public class ManyToManyTestObject1 {
    @Id
    private String id;

    @ManyToMany
    private Collection<ManyToManyTestObject2> otherObjects;

    public Collection<ManyToManyTestObject2> getOtherObjects() {
        return otherObjects;
    }

    public void setOtherObjects(Collection<ManyToManyTestObject2> otherObjects) {
        this.otherObjects = otherObjects;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
