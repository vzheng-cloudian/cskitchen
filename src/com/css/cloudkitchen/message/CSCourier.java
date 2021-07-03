package com.css.cloudkitchen.message;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Define Courier
 */
public class CSCourier extends CSMessage {
    private static final AtomicInteger seq = new AtomicInteger(0);

    private final int arrivePeriod;
    private String orderPickedUp;
    private final String name;
    private final long dispatchTime;
    private long arriveTime = 0L;
    private long pickupTime = 0L;


    public CSCourier(final int start, final int end) {
        int seqId = seq.incrementAndGet();
        this.name = "Courier-#" + seqId;
        this.dispatchTime = System.currentTimeMillis();
        this.arrivePeriod = uniformDistribution(start, end);
    }

    public String getName() {
        return name;
    }

    public long getDispatchTime() {
        return dispatchTime;
    }

    public long getArriveTime() {
        return arriveTime;
    }

    public void setArriveTime(final long arriveTime) {
        this.arriveTime = arriveTime;
    }

    public long getPickupTime() {
        return pickupTime;
    }

    public void setPickupTime(final long pickupTime) {
        this.pickupTime = pickupTime;
    }

    public boolean isArrived() {
        return this.arriveTime > 0L;
    }

    public int getArrivePeriod() {
        return arrivePeriod;
    }

    public String getOrderPickedUp() {
        return orderPickedUp;
    }

    public void setOrderPickedUp(final String orderPickedUp) {
        this.orderPickedUp = orderPickedUp;
    }

    @Override
    public String toString() {
        return "MSG: " + this.msgID + ", CSCourier: " +
                this.name + "," +
                this.dispatchTime + "," +
                this.arrivePeriod + "," +
                this.arriveTime + "," +
                this.pickupTime;
    }

    /**
     * Generate a random number in the range [start, end], following Uniform Distribution
     * @param start Range start
     * @param end Range end
     * @return A random number in the range
     */
    public static int uniformDistribution(final int start, final int end) {
        int range = end - start + 1;
        range = Math.abs(range);
        Random rand = new Random();
        int r = rand.nextInt(range);
        return end > start ? r + start : r + end;
    }
}
