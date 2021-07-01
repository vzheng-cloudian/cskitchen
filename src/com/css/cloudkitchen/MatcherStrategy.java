package com.css.cloudkitchen;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.PriorityQueue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A message consumer.
 * Get both orders and couriers from message bus.
 * Apply different strategies to match the food orders and the couriers.
 * Can apply one strategy at a time, or all strategies together.
 * Print statistics for each strategy separately.
 * Support "MATCH" and "FIFO" strategies.
 */
public class MatcherStrategy implements IMessageHandler, Callable<Integer> {
    private static final Logger logger = LoggerFactory.getLogger(MatcherStrategy.class);

    private boolean alive = false;
    private final ArrayBlockingQueue<CSMessage> inQueue = new ArrayBlockingQueue<>(CSKitchen.maxQueue);
    private final ArrayList<CSOrder> foodList = new ArrayList<>();
    private final PriorityQueue<CSCourier> courierPQ = new PriorityQueue<>((o1, o2) -> {
        if (o1.getReadyTime() <= 0L || o2.getReadyTime() <= 0L) {
            return 0;
        }
        return Long.compare(o1.getReadyTime(), o2.getReadyTime());
    });
    private final int type;
    private String typeName;
    private boolean stopSign = false;
    private final AtomicInteger orderCount = new AtomicInteger(0);
    private final AtomicLong foodLatency = new AtomicLong(0L);
    private final AtomicLong courierLatency = new AtomicLong(0L);

    public MatcherStrategy(final int type) {
        this.type = type;
        switch (type) {
            case 1:
                typeName = "MATCH";
                break;
            case 2:
                typeName = "FIFO";
                break;
            default:
                logger.error("Wrong strategy type {}", type);
        }
    }

    @Override
    public ArrayBlockingQueue<CSMessage> getInQueue() {
        return inQueue;
    }

    @Override
    public void setOutQueue(final ArrayBlockingQueue<CSMessage> outQueue) {
    }

    @Override
    public boolean filter(final CSMessage csMessage) {
        return (csMessage.hasReadyTime() || csMessage.hasCommand());
    }

    @Override
    public boolean isAlive() {
        return alive;
    }

    @Override
    public Integer call() {
        logger.info("Start Matcher strategy : {}", typeName);
        this.alive = true;

        int counter = 0; // total orders matched
        CSMessage msg;
        int grace = CSKitchen.GRACE_TIME;
        int total = 0;
        outerLoop: while (true) {
            try {
                if (stopSign && (grace <= 0 || counter >= total)) {
                    logger.info("Total {} out of {} orders handled, {} quiting...", counter, total, typeName);
                    break;
                }
                if (stopSign) {
                    grace--;
                }

                msg = inQueue.poll(1, TimeUnit.SECONDS);
                if (msg == null || !filter(msg)) {
                    continue;
                }

                if (msg.hasCommand()) {
                    if (msg.getCommand().equals(CSKitchen.CMD_EXIT)) {
                        logger.info("Get exit command, Matcher {} is quiting...", typeName);
                        total = (int) msg.getPickupTime();
                        stopSign = true;
                        continue;
                    }
                }

                // Food ready
                if (msg instanceof CSOrder) {
//                    if (!msg.hasReadyTime()) { //new order
//                        continue;
//                    }

                    // food ready for pickup, apply matching strategy
                    if (type == 1) { // matched strategy
                        for (CSCourier courier : courierPQ) {
                            if (msg.getId().equals(courier.getOrderPickedUp())) {
                                doMatch((CSOrder) msg, courier, System.currentTimeMillis());
                                courierPQ.remove(courier);
                                counter++;
                                continue outerLoop;
                            }
                        }
                        //not match, push to waiting list
                        foodList.add((CSOrder) msg);
                        continue;
                    }

                    // FIFO strategy
                    if (courierPQ.size() > 0) {
                        CSCourier courier = courierPQ.poll();
                        doMatch((CSOrder) msg, courier, System.currentTimeMillis());
                        counter++;
                        continue;
                    }
                    //not match, push to waiting list
                    foodList.add((CSOrder) msg);
                    continue;
                }

                // Courier arrival
                if (type == 1) { // matched strategy
                    for (CSOrder order : foodList) {
                        if (order.getId().equals(((CSCourier) msg).getOrderPickedUp())) {
                            doMatch(order, (CSCourier) msg, System.currentTimeMillis());
                            foodList.remove(order);
                            counter++;
                            continue outerLoop;
                        }
                    }
                    //not match, push to waiting list
                    courierPQ.add((CSCourier) msg);
                    continue;
                }

                // FIFO strategy
                if (foodList.size() > 0) {
                    CSOrder order = foodList.get(0);
                    doMatch(order, (CSCourier) msg, System.currentTimeMillis());
                    foodList.remove(0);
                    counter++;
                    continue;
                }
                //not match, push to waiting list
                courierPQ.add((CSCourier) msg);

            } catch (Exception e) {
                logger.error("Matcher {} catch: ", typeName, e);
            }
        }

        //print statistics
        StringBuilder sb = new StringBuilder("Statistics for ").append(typeName).append(" :\n")
                .append("Total orders: ").append(orderCount.get()).append("\n")
                .append("Total food wait time (ms): ").append(foodLatency.get()).append("\n")
                .append("Total courier wait time (ms): ").append(courierLatency.get()).append("\n")
                .append("Average food wait time (ms): ")
                .append(orderCount.get() == 0 ? 0 : foodLatency.get() / orderCount.get()).append("\n")
                .append("Average courier wait time (ms): ")
                .append(orderCount.get() == 0 ? 0 : courierLatency.get() / orderCount.get()).append("\n");
        logger.info(sb.toString());
        System.out.println(sb);

        this.alive = false;
        return counter;
    }

    private void doMatch(final CSOrder order, final CSCourier courier, final long timestamp) {
        courier.setPickupTime(timestamp);
        order.setPickupTime(timestamp);

        //collect statistics
        orderCount.incrementAndGet();
        long foodWait = order.getPickupTime() - order.getReadyTime();
        foodLatency.addAndGet(foodWait);
        long courierWait = courier.getPickupTime() - courier.getReadyTime();
        courierLatency.addAndGet(courierWait);

        String msg = typeName + ": Order " + order.getId() + " picked up by " + courier.getName()
                + ", food wait " + foodWait + ", courier wait " + courierWait;
        System.out.println(msg);
        logger.info(msg);
    }
}
