package com.css.cloudkitchen;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Main class
 * Assemble the main workflow by creating threads for each component.
 * Monitoring the threads, terminate them when done or in error.
 * Adopt Chain of Responsibility design pattern.
 */
public class CSKitchen{
    public static final int MAX_THREADS = 10;
    public static final int KEEP_ALIVE = 10;
    public static final int THOUSAND = 1000;
    public static final String CMD_EXIT = "_EXIT_";
    public static final int COURIER_START = 3; //arrival delay left range
    public static final int COURIER_END = 15; //arrival delay right range
    public static final int MSG_RETRY = 6; //max retries for sending a message
    public static final int GRACE_TIME = 300; // grace time in second before quiting

    private static final Logger logger = LoggerFactory.getLogger(CSKitchen.class);

    public static int maxQueue = 1000; // max queue length
    private final int runType; //  1 is Matched method, 2 is First-in-first-out method, 3 is both
    private final int totalOrders; //total orders to be run
    private final int orderPerSecond;
    private final boolean randomFood; //generate orders with random food

    private final ThreadPoolExecutor tPool = Helpers.createConstraintPool("CSKitchen ", MAX_THREADS, KEEP_ALIVE);
    private final ExecutorCompletionService<Integer> compServ = new ExecutorCompletionService<>(tPool);

    public CSKitchen(final int orders, final int orderPerSecond, final int type, final boolean randomFood) {
        this.runType = type;
        this.totalOrders = orders;
        this.orderPerSecond = orderPerSecond;
        this.randomFood = randomFood;
    }

    private void assembleChain() {
        MessageDispatcher mBus = MessageDispatcher.getInstance();
        // generate orders
        OrderGenerator og = new OrderGenerator(this.orderPerSecond, this.totalOrders, this.randomFood);
        mBus.register(og);

        // prepare food
        FoodCooker fc = new FoodCooker();
        mBus.register(fc);
        compServ.submit(fc);

        // dispatch courier for delivery
        CourierAssigner ca = new CourierAssigner();
        mBus.register(ca);
        compServ.submit(ca);

        // apply different strategy
        if (runType == 1 || runType == 3) {
            MatcherStrategy ms1 = new MatcherStrategy(new StrategyMatch());
            mBus.register(ms1);
            compServ.submit(ms1);
        }
        if (runType == 2 || runType == 3) {
            MatcherStrategy ms2 = new MatcherStrategy(new StrategyFIFO());
            mBus.register(ms2);
            compServ.submit(ms2);
        }

        compServ.submit(mBus);

        // start generating orders, workflow is running now
        compServ.submit(og);
    }

    public void run() {
        Future<Integer> ret;
        final int interval = 1;
        try {

            assembleChain();

            // monitoring thread state, check for completion
            while (tPool.getCompletedTaskCount() < tPool.getTaskCount()) {
                if (tPool.getCompletedTaskCount() + 1 == tPool.getTaskCount()) {
                    //stop dispatcher explicitly
                    MessageDispatcher.stopDispatcher();
                }
                try {
                    ret = compServ.poll(interval, TimeUnit.SECONDS);
                    if (ret != null) {
                        logger.info("Thread completed with {} .", ret.get());
                    }
                } catch (InterruptedException ie) {
                    // quit current task
                    tPool.shutdownNow();
                } catch (Exception e) {
                    logger.info("Checking thread status, caught: ", e);
                }
            }
        } catch (Throwable e) {
            logger.error("Caught: ", e);
        }
    }

    public static void usage() {
        System.out.println("usage: -o <number> -ops <number> -q <number> -r <y|n> -t <1|2|3>");
        System.out.println("-o <number>   --> Total number of orders, default is 100, range from 1 to 100,000.");
        System.out.println("-ops <number> --> Order per second, default is 2, range from 1 to 100.");
        System.out.println("-q <number>   --> Max queue length, default is 1000, range from 1 to 100,000.");
        System.out.println("-r <y|n>      --> Randomly choosing food for orders, "
                + "otherwise CheesePizza wil be chosen, default is [y]es.");
        System.out.println("-t <1|2|3>    --> Match type, 1: MATCH, 2: FIFO, 3: both 1 & 2 , default is 3.");
        System.exit(1);
    }

    public static void main(String[] args) {
        // default value for parameters
        int orders = 100;
        int ops = 2;
        int type = 3;
        boolean randomFood = true;

        int idx = 0;
        while (idx < args.length) {
            String key = args[idx];
            idx++;

            if (idx >= args.length){
                usage();
            }
            switch (key) {
                case "-o":
                    try {
                        orders = Integer.parseInt(args[idx]);
                        if (orders < 1 || orders > 100000) {
                            throw new Exception();
                        }
                        logger.info("Will generate {} orders.", orders);
                    } catch (Exception e) {
                        System.out.println("Invalid number for orders : " + args[idx]);
                        System.exit(1);
                    }
                    break;
                case "-ops":
                    try {
                        ops = Integer.parseInt(args[idx]);
                        if (ops < 1 || ops > 100) {
                            throw new Exception();
                        }
                        logger.info("Will generate {} orders.", orders);
                    } catch (Exception e) {
                        System.out.println("Invalid number for ops : " + args[idx]);
                        System.exit(1);
                    }
                    break;
                case "-q":
                    try {
                        maxQueue = Integer.parseInt(args[idx]);
                        if (maxQueue < 1 || maxQueue > 100000) {
                            throw new Exception();
                        }
                        logger.info("Will set max queue length to {}.", maxQueue);
                    } catch (Exception e) {
                        System.out.println("Invalid number for max queue length : " + args[idx]);
                        System.exit(1);
                    }
                    break;
                case "-r":
                    try {
                        if ("n".equalsIgnoreCase(args[idx])) {
                            randomFood = false;
                        } else if ("y".equalsIgnoreCase(args[idx])) {
                            randomFood = true;
                        } else {
                            throw new Exception();
                        }
                        logger.info("Will generate random food for orders : {} .", randomFood);
                    } catch (Exception e) {
                        System.out.println("Invalid value for random food : " + args[idx]);
                        System.exit(1);
                    }
                    break;
                case "-t":
                    try {
                        type = Integer.parseInt(args[idx]);
                        if (type < 1 || type > 3) {
                            throw new Exception();
                        }
                        logger.info("The match method will be {}.", type);
                    } catch (Exception e) {
                        System.out.println("Invalid number for type : " + args[idx]);
                        System.exit(1);
                    }
                    break;
                default:
                    usage();
            }
            idx++;
        }

        long start = System.currentTimeMillis();
        CSKitchen csk = new CSKitchen(orders, ops, type, randomFood);
        csk.run();
        long runtime = System.currentTimeMillis() - start;
        System.out.println("CSKitchen end, total time (in ms) spend " + runtime);
        logger.info("CSKitchen end, total time (in ms) spend " + runtime);
        System.exit(0);
    }
}