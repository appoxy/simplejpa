package com.spaceprogram.simplejpa;

import com.spaceprogram.simplejpa.model.Model;

import javax.persistence.Id;

/**
 * User: treeder
 * Date: Oct 18, 2008
 * Time: 4:41:51 PM
 */
public class ModelSub extends Model {

    private String id;
    private String title;

    @Id
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }


    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
