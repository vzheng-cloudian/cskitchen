package com.css.cloudkitchen.strategy;

import com.css.cloudkitchen.message.CSCourier;
import com.css.cloudkitchen.message.CSMessage;
import com.css.cloudkitchen.message.CSOrder;

import java.util.ArrayList;

public class StrategyMatch extends AbstractStrategy {
    private final ArrayList<CSOrder> foodList = new ArrayList<>();
    private final ArrayList<CSCourier> courierList = new ArrayList<>();

    public StrategyMatch() {
        super("MATCH");
    }

    @Override
    public CSMessage apply(CSMessage msg) {
        // Food ready
        if (msg instanceof CSOrder) {
            for (CSCourier courier : courierList) {
                if (((CSOrder) msg).getOrderId().equals(courier.getOrderPickedUp())) {
                    doMatch((CSOrder) msg, courier, System.currentTimeMillis());
                    courierList.remove(courier);
                    return msg;
                }
            }
            //not match, push to waiting list
            foodList.add((CSOrder) msg);
            return null;
        }

        // Courier arrival
        for (CSOrder order : foodList) {
            if (order.getOrderId().equals(((CSCourier) msg).getOrderPickedUp())) {
                doMatch(order, (CSCourier) msg, System.currentTimeMillis());
                foodList.remove(order);
                return order;
            }
        }
        //not match, push to waiting list
        courierList.add((CSCourier) msg);
        return null;
    }
}
