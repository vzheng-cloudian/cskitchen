package com.css.cloudkitchen;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Define Courier
 */
public class CSCourier extends CSMessage{
    private static final AtomicInteger seq = new AtomicInteger(0);

    private final int arrivePeriod;
    private String orderPickedUp;

    public CSCourier(final int start, final int end) {
        int seqId = seq.incrementAndGet();
        this.id = Integer.toString(seqId);
        this.name = "Courier-#" + this.id;
        this.createTime = System.currentTimeMillis();
        this.arrivePeriod = uniformDistribution(start, end);
    }

    public CSCourier(final CSCourier source) {
        super.deepCopy(source);
        this.arrivePeriod = source.getArrivePeriod();
        this.orderPickedUp = source.getOrderPickedUp();
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
        String sb = "CSCourier: " +
                this.id + "," +
                this.createTime + "," +
                this.arrivePeriod + "," +
                this.readyTime + "," +
                this.pickupTime;
        return sb;
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
