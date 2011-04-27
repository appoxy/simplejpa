package com.spaceprogram.simplejpa;

/**
 * User: treeder
 * Date: Aug 13, 2008
 * Time: 1:10:52 PM
 */
public interface DatabaseManager {
    /**
     * This method will rename an attribute in SimpleDB by iterating through EVERY element in the Domain
     * putting the old attribute value to the new attribute name, then deleting the old one.
     * <p/>
     * This can take a long time to complete if the Domain is large.
     *
     * @param domainClass      specifies the Domain
     * @param oldAttributeName the attribute name you want values moved from
     * @param newAttributeName the attribute name you want values moved to
     */
    void renameField(Class domainClass, String oldAttributeName, String newAttributeName);

    /**
     * This will basically change the values for the differentiator column (DTYPE) from
     * the oldClassName to the newClass name.
     *
     * @param oldClassName
     * @param newClass
     */
    void renameSubclass(String oldClassName, Class newClass);
}
