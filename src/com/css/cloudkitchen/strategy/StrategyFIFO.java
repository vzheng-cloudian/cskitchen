package com.css.cloudkitchen.strategy;

import com.css.cloudkitchen.message.CSCourier;
import com.css.cloudkitchen.message.CSMessage;
import com.css.cloudkitchen.message.CSOrder;

import java.util.ArrayList;
import java.util.PriorityQueue;

public class StrategyFIFO extends AbstractStrategy {
    private final ArrayList<CSOrder> foodList = new ArrayList<>();
    private final PriorityQueue<CSCourier> courierPQ = new PriorityQueue<>((o1, o2) -> {
        if (o1.getReadyTime() <= 0L || o2.getReadyTime() <= 0L) {
            return 0;
        }
        return Long.compare(o1.getReadyTime(), o2.getReadyTime());
    });

    public StrategyFIFO() {
        super("FIFO");
    }

    @Override
    public CSMessage apply(CSMessage msg) {
        // Food ready
        if (msg instanceof CSOrder) {
            if (courierPQ.size() > 0) {
                CSCourier courier = courierPQ.poll();
                doMatch((CSOrder) msg, courier, System.currentTimeMillis());
                return msg;
            }
            //not match, push to waiting list
            foodList.add((CSOrder) msg);
            return null;
        }

        // Courier arrival
        if (foodList.size() > 0) {
            CSOrder order = foodList.get(0);
            doMatch(order, (CSCourier) msg, System.currentTimeMillis());
            foodList.remove(0);
            return order;
        }
        //not match, push to waiting list
        courierPQ.add((CSCourier) msg);
        return null;
    }
}
