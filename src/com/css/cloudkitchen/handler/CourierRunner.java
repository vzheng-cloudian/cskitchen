package com.css.cloudkitchen.handler;

import com.css.cloudkitchen.CSKitchen;
import com.css.cloudkitchen.Helpers;
import com.css.cloudkitchen.message.CSCourier;
import com.css.cloudkitchen.message.CSMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Both message consumer and producer.
 * Get Courier message from message bus,
 * create a thread to simulate courier arrival.
 * After courier arrival, send Courier message to message bus.
 * Exit when all orders have been handled.
 * Return the total number of couriers been handled.
 */
public class CourierRunner implements IMessageHandler, Callable<Integer> {
    private static final Logger logger = LoggerFactory.getLogger(CourierRunner.class);

    private boolean alive = false;
    private ArrayBlockingQueue<CSMessage> mainQueue = null;
    private final ArrayBlockingQueue<CSMessage> courierQueue = new ArrayBlockingQueue<>(CSKitchen.maxQueue);
    private final ThreadPoolExecutor courierTPool;
    private final ExecutorCompletionService<Integer> compServ;

    public CourierRunner() {
        courierTPool = Helpers.createConstraintPool("Courier ", CSKitchen.maxQueue, CSKitchen.KEEP_ALIVE);
        compServ = new ExecutorCompletionService<>(courierTPool);
    }

    @Override
    public ArrayBlockingQueue<CSMessage> getInQueue() {
        return courierQueue;
    }

    @Override
    public void setOutQueue(final ArrayBlockingQueue<CSMessage> outQueue) {
        mainQueue = outQueue;
    }

    @Override
    public boolean filter(final CSMessage csMessage) {
        return csMessage.hasCommand() || (csMessage instanceof CSCourier && !((CSCourier) csMessage).isArrived());
    }

    @Override
    public boolean isAlive() {
        return alive;
    }

    @Override
    public Integer call() {

        logger.info("Start Courier assigning.");
        this.alive = true;
        int counter = 0;
        int total = 0;
        boolean runState = true;
        int grace = CSKitchen.GRACE_TIME;

        while (runState || counter < total) {

            if (!runState) {
                grace--;
                if (grace < 0) {
                    break;
                }
            }

            try {
                final CSMessage msg = courierQueue.poll(1, TimeUnit.SECONDS);
                if (msg == null || !filter(msg)) {
                    continue;
                }
                if (msg.hasCommand()) {
                    if (msg.getCommand().startsWith(CSKitchen.CMD_EXIT)) {
                        runState = false;
                        total = Integer.parseInt(msg.getCommandOption());
                        logger.info("Get exit command, total {} orders, CourierRunner is quiting...", total);
                        break;
                    }
                }
                compServ.submit(() -> {
                    try {
                        Thread.sleep((long) ((CSCourier)msg).getArrivePeriod() * CSKitchen.THOUSAND);
                        ((CSCourier) msg).setArriveTime(System.currentTimeMillis());
                        for (int i = 0; i < CSKitchen.MSG_RETRY; i++) {
                            try {
                                mainQueue.add(msg);
                                break;
                            } catch (Exception e) {
                                logger.error("Failed to put to queue {} times, caught:", i, e);
                                Thread.sleep((i + 1) * CSKitchen.THOUSAND);
                            }
                        }
                        String logMsg1 = ((CSCourier) msg).getName() + " arrived at "
                                + ((CSCourier) msg).getArriveTime();
                        System.out.println(logMsg1);
                        logger.info(logMsg1);
                        return 1;
                    } catch (Exception e) {
                        logger.info("Courier arriving caught:", e);
                    }
                    return 0;
                });
                counter++;
            } catch (Exception e) {
                logger.error("CourierRunner catch: ", e);
            }
        }
        this.alive = false;

        Future<Integer> ret;
        final int interval = 1;
        while (courierTPool.getCompletedTaskCount() < courierTPool.getTaskCount()) {
            try {
                ret = compServ.poll(interval, TimeUnit.SECONDS);
                if (ret != null && ret.get() != 1) {
                    logger.info("Courier arriving failed, completed with {} .", ret.get());
                }
            } catch (Exception e) {
                logger.info("Caught: ", e);
            }
        }

        return counter;
    }
}