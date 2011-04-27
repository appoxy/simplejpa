package com.spaceprogram.simplejpa;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User: treeder
 * Date: Mar 2, 2008
 * Time: 5:46:06 PM
 */
public class RegexTests {
    @Test
    public void testFindingConditionals(){
        String s;
        Pattern pattern = Pattern.compile("[(<>)(>=)=>(<=)]+");
        s = "x=y";
        find(s, pattern);
        s = "x =y and y> z";
        find(s, pattern);
        s = "x >=y and y<= z or z <> x";
        find(s, pattern);

    }

    private void find(String s, Pattern pattern) {
        Matcher matcher = pattern.matcher(s);
        while(matcher.find()){
            System.out.println("matcher found: " + matcher.group() + " at " + matcher.start() + " to " + matcher.end());

        }
    }
}
