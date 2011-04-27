package com.spaceprogram.simplejpa;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.*;

/**
 * User: treeder
 * Date: Apr 1, 2008
 * Time: 12:04:09 PM
 */
public class ConcurrencyTests {
    private ExecutorService service = Executors.newSingleThreadExecutor();

    @Test
    public void throwExceptionInFuture() throws ExecutionException, InterruptedException {
        Future<String> future = service.submit(new Callable<String>(){
            public String call() throws Exception {
                System.out.println("about to throw exception in call.");
                if(true) throw new Exception("something went wrong");
                return "all good";
            }
        });
        Thread.sleep(3000);
        System.out.println("No exception seen yet, about to call get()");

        try {
            String s = future.get();
            System.out.println("s=" + s);
            Assert.assertTrue(false); // should never reach here
        } catch (ExecutionException e) {
//            e.printStackTrace();
            System.out.println("caught ExecutionException that occurred calling get(): " + e.getCause().getMessage());
        }
    }
}
