package com.css.cloudkitchen.handler;

import com.css.cloudkitchen.CSKitchen;
import com.css.cloudkitchen.Helpers;
import com.css.cloudkitchen.message.CSMessage;
import com.css.cloudkitchen.message.CSOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ExecutorCompletionService;

/**
 * Both message consumer and producer.
 * Get Order message from message bus,
 * create a thread to simulate preparing the food.
 * After food is ready, send Order message to message bus.
 * Exit when all orders have been handled.
 * Return the total number of orders been handled.
 */
public class FoodCooker implements IMessageHandler, Callable<Integer> {
    private static final Logger logger = LoggerFactory.getLogger(FoodCooker.class);

    private boolean alive = false;
    private ArrayBlockingQueue<CSMessage> mainQueue = null;
    private final ArrayBlockingQueue<CSMessage> foodQueue = new ArrayBlockingQueue<>(CSKitchen.maxQueue);
    private final ThreadPoolExecutor cookTPool;
    private final ExecutorCompletionService<Integer> compServ;

    public FoodCooker() {
        cookTPool = Helpers.createConstraintPool("Cooker ", CSKitchen.maxQueue, CSKitchen.KEEP_ALIVE);
        compServ = new ExecutorCompletionService<>(cookTPool);
    }

    @Override
    public ArrayBlockingQueue<CSMessage> getInQueue() {
        return foodQueue;
    }

    @Override
    public void setOutQueue(final ArrayBlockingQueue<CSMessage> outQueue) {
        mainQueue = outQueue;
    }

    @Override
    public boolean filter(final CSMessage csMessage) {
        return  (csMessage.hasCommand() || (csMessage instanceof CSOrder && !((CSOrder) csMessage).isReady()));
    }

    @Override
    public boolean isAlive() {
        return alive;
    }

    @Override
    public Integer call() {

        logger.info("Start Food Cooker.");
        this.alive = true;

        int counter = 0;
        int total = 0;
        int grace = CSKitchen.GRACE_TIME;
        boolean runState = true;

        while (runState || counter < total) {

            if (!runState) {
                grace--;
                if (grace < 0) {
                    break;
                }
            }

            try {
                final CSMessage msg = foodQueue.poll(1, TimeUnit.SECONDS);
                if (msg == null || !filter(msg)) {
                    continue;
                }
                if (msg.hasCommand()) {
                    if (msg.getCommand().startsWith(CSKitchen.CMD_EXIT)) {
                        runState = false;
                        total = Integer.parseInt(msg.getCommandOption());
                        logger.info("Get exit command, total {} orders, Cooker is quiting...", total);
                        break;
                    }
                }
                final CSOrder order = (CSOrder) msg;
                compServ.submit(() -> {
                    try {
                        Thread.sleep((long) order.getPrepTime() * CSKitchen.THOUSAND);
                        order.setReadyTime(System.currentTimeMillis());
                        for (int i = 0; i < CSKitchen.MSG_RETRY; i++)  {
                            try {
                                mainQueue.add(order);
                                break;
                            } catch (Exception e) {
                                logger.error("Failed to put to queue {} times, caught:", i, e);
                                Thread.sleep((i + 1) * CSKitchen.THOUSAND);
                            }
                        }

                        String log = "Order " + order.getOrderId() + " prepared at " + order.getReadyTime();
                        System.out.println(log);
                        logger.info(log);
                        return 1;
                    } catch (Exception e) {
                        logger.info("Cooker cooking caught:", e);
                    }
                    return 0;
                });
                counter++;
            } catch (Exception e) {
                logger.error("Cooker catch: ", e);
            }
        }
        this.alive = false;

        Future<Integer> ret;
        final int interval = 1;
        while (this.cookTPool.getCompletedTaskCount() < this.cookTPool.getTaskCount()) {
            try {
                ret = this.compServ.poll(interval, TimeUnit.SECONDS);
                if (ret != null && ret.get() != 1) {
                    logger.info("Food cooker failed, completed with {} .", ret.get());
                }
            } catch (Exception e) {
                logger.info("Caught: ", e);
            }
        }

        return counter;
    }
}
