package com.spaceprogram.simplejpa.util;

/**
 * User: treeder
 * Date: Aug 18, 2008
 * Time: 2:32:12 AM
 */
public class EscapeUtils {

    public static String escapeQueryParam(String str){
         if (str == null) {
            return null;
        }
        String s = str;
        s = s.replace("\\", "\\\\");
        s = s.replace("'", "''");
        return s;
    }
}
