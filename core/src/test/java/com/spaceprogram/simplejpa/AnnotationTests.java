package com.spaceprogram.simplejpa;

import org.junit.Test;
import org.scannotation.AnnotationDB;
import org.scannotation.ClasspathUrlFinder;

import javax.persistence.Entity;
import java.io.IOException;
import java.net.URL;
import java.util.Set;

/**
 * User: treeder
 * Date: Feb 8, 2008
 * Time: 12:53:30 PM
 */
public class AnnotationTests {
    @Test
    public void findAnnotations() throws IOException {
        URL[] urls = ClasspathUrlFinder.findClassPaths(); // scan java.class.path
        AnnotationDB db = new AnnotationDB();
        db.scanArchives(urls);
        Set<String> entities = db.getAnnotationIndex().get(Entity.class.getName());
        for (String entity : entities) {
            System.out.println("entity=" + entity);
        }
    }
}
