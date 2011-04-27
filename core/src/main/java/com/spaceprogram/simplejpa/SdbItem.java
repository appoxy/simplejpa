package com.spaceprogram.simplejpa;

import com.amazonaws.services.simpledb.model.Attribute;

import java.util.List;

/**
 *
 * User: treeder
 * Date: Mar 8, 2009
 * Time: 10:34:27 PM
 */
public interface SdbItem {
    String getIdentifier();

    List<Attribute> getAttributes();
}
