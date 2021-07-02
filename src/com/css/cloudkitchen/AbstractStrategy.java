package com.css.cloudkitchen;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public abstract class AbstractStrategy {
    protected static final Logger logger = LoggerFactory.getLogger(AbstractStrategy.class);

    protected final String name;

    protected final AtomicInteger orderCount = new AtomicInteger(0);
    protected final AtomicLong foodLatency = new AtomicLong(0L);
    protected final AtomicLong courierLatency = new AtomicLong(0L);

    public AbstractStrategy(String name) {
        this.name = name;
    }

    /**
     * The Strategy name.
     * @return The Strategy name
     */
    public String getName() {
        return name;
    }

    /**
     * The function to implement the different strategies.
     * @param msg Input message, can be Order or Courier
     * @return Matched Order or null if not match
     */
    public abstract CSMessage apply(CSMessage msg);

    protected void doMatch(final CSOrder order, final CSCourier courier, final long timestamp) {
        courier.setPickupTime(timestamp);
        order.setPickupTime(timestamp);

        //collect statistics
        orderCount.incrementAndGet();
        long foodWait = order.getPickupTime() - order.getReadyTime();
        foodLatency.addAndGet(foodWait);
        long courierWait = courier.getPickupTime() - courier.getReadyTime();
        courierLatency.addAndGet(courierWait);

        String msg = name + ": Order " + order.getId() + " picked up by " + courier.getName()
                + ", food wait " + foodWait + ", courier wait " + courierWait;
        System.out.println(msg);
        logger.info(msg);
    }

    /**
     * Print statistics
     */
    public void printStatistics() {
        StringBuilder sb = new StringBuilder("Statistics for ").append(name).append(" :\n")
                .append("Total orders: ").append(orderCount.get()).append("\n")
                .append("Total food wait time (ms): ").append(foodLatency.get()).append("\n")
                .append("Total courier wait time (ms): ").append(courierLatency.get()).append("\n")
                .append("Average food wait time (ms): ")
                .append(orderCount.get() == 0 ? 0 : foodLatency.get() / orderCount.get()).append("\n")
                .append("Average courier wait time (ms): ")
                .append(orderCount.get() == 0 ? 0 : courierLatency.get() / orderCount.get()).append("\n");
        logger.info(sb.toString());
        System.out.println(sb);
    }
}
