package com.css.cloudkitchen;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Helper class, provides helper APIs.
 */
public class Helpers {

    private Helpers() {
    }

    /**
     * Create a Thread Pool with bounded waiting queue length ,
     * and with CallerRunsPolicy when the queue is full.
     * @param name Thread name prefix
     * @param numThreads Max thread count
     * @param keepAlive Keep alive time
     * @return A new instance of ThreadPoolExecutor
     */
    public static ThreadPoolExecutor createConstraintPool(final String name,
                                                          final int numThreads,
                                                          final int keepAlive) {

        ThreadFactory threadFactory = new ThreadFactory() {
            private int threadCount = 1;

            public Thread newThread(final Runnable r) {
                Thread thread = new Thread(r);
                thread.setName(name + threadCount++);
                return thread;
            }
        };

        LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<>(numThreads * 10);
        final ThreadPoolExecutor tPool = new ThreadPoolExecutor(numThreads, numThreads,
                keepAlive, TimeUnit.SECONDS, queue, threadFactory, new ThreadPoolExecutor.CallerRunsPolicy());
        tPool.allowCoreThreadTimeOut(true);
        return tPool;
    }

}
