package com.spaceprogram.simplejpa;

/**
 * This attribute will be used to define multi-value attributes.
 *
 * Probably should be defined as a Set property on the class, but stored as a multi-value attribute.
 *
 * Then queries could be applied on the attribute, wouldn't need multiple queries.
 *
 * eg: select o from MyObject o where o.multiValProperty = 'keyword' 
 *
 * see: http://developer.amazonwebservices.com/connect/entry.jspa?externalID=1231&categoryID=152
 *
 * User: treeder
 * Date: Feb 19, 2008
 * Time: 7:05:43 PM
 */
public @interface MultiValueProperty {
}
