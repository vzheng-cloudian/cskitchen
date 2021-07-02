package com.css.cloudkitchen.handler;

import com.css.cloudkitchen.CSKitchen;
import com.css.cloudkitchen.message.CSMessage;
import com.css.cloudkitchen.message.CSOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;

/**
 * A Message producer.
 * Generate new orders at the specified rate,
 * put order message to the message bus.
 * When all orders have been sent out, send an EXIT message to notify other components the completion.
 * Return the total number of orders been generated.
 */
public class OrderGenerator implements IMessageHandler, Callable<Integer> {
    private static final Logger logger = LoggerFactory.getLogger(OrderGenerator.class);

    private boolean alive = false;
    private final int totalOrders;
    private final int orderPerSecond;
    private final boolean randomFood;
    private ArrayBlockingQueue<CSMessage> mainQueue = null;

    public OrderGenerator(final int orderPerSecond, final int totalOrders, final boolean randomFood) {
        this.totalOrders = totalOrders;
        this.orderPerSecond = orderPerSecond;
        this.randomFood = randomFood;
    }

    @Override
    public ArrayBlockingQueue<CSMessage> getInQueue() {
        return null;
    }

    @Override
    public void setOutQueue(final ArrayBlockingQueue<CSMessage> outQueue) {
        mainQueue = outQueue;
    }

    @Override
    public boolean filter(final CSMessage csMessage) {
        return false;
    }

    @Override
    public boolean isAlive() {
        return alive;
    }

    @Override
    public Integer call() {
        logger.info("Start to generate {} orders per second, total orders will be {}.",
                this.orderPerSecond, this.totalOrders);
        this.alive = true;
        CSOrder order;
        long ts;
        int errors = 0;
        for (int i = 0; i < totalOrders; ) {
            try {
                ts = System.currentTimeMillis();
                for (int j = 0; j < orderPerSecond && j + i < totalOrders; j++) {
                    order = new CSOrder(randomFood);
                    try {
                        mainQueue.add(order);
                        errors = errors > 0 ? errors - 1 : 0;
                    } catch (Exception e) { // retry when queue is full or other errors
                        logger.error("Failed to put to queue, caught:", e);
                        j--;
                        errors++;
                        Thread.sleep((long) errors * CSKitchen.THOUSAND);
                        continue;
                    }
                    String msg = "Order " + order.getId() + " received at " + order.getCreateTime()
                            + ", will be ready in " + order.getPrepTime() + "s.";
                    System.out.println(msg);
                    logger.info(msg);
                }
                i += orderPerSecond;
                if (System.currentTimeMillis() - ts < CSKitchen.THOUSAND) {
                    Thread.sleep(CSKitchen.THOUSAND - (System.currentTimeMillis() - ts));
                }
            } catch (InterruptedException ie) {
                logger.info("Interrupted, Order Generator stopped.");
                return -1;
            } catch (Exception e) {
                logger.error("Order Generator catch: ", e);
            }
        }

        // send exit command to notify other components
        order = new CSOrder(false);
        order.setPickupTime(totalOrders);
        order.setCommand(CSKitchen.CMD_EXIT);
        try {
            Thread.sleep((long) order.getPrepTime() * CSKitchen.THOUSAND);
        } catch (InterruptedException e) {
            //ignore
        }
        while (true) {
            try {
                mainQueue.add(order);
                logger.info("Exit command send, total orders {}.", totalOrders);
                break;
            } catch (Exception e) { // retry when queue is full or other errors
                logger.error("Failed to send Exit command, caught:", e);
                if (errors < CSKitchen.GRACE_TIME) {
                    errors++;
                }
                try {
                    Thread.sleep((long) errors * CSKitchen.THOUSAND);
                } catch (InterruptedException ignore) {
                    //ignore
                }
            }
        }

        this.alive = false;
        return this.totalOrders;
    }
}
