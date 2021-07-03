package com.css.cloudkitchen.handler;

import com.css.cloudkitchen.CSKitchen;
import com.css.cloudkitchen.message.CSCourier;
import com.css.cloudkitchen.message.CSMessage;
import com.css.cloudkitchen.message.CSOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * Both message consumer and producer.
 * Get Order message from message bus, dispatch the order to a courier,
 * Exit when all orders have been handled.
 * Return the total number of courier been dispatched.
 */
public class CourierDispatcher implements IMessageHandler, Callable<Integer> {
    private static final Logger logger = LoggerFactory.getLogger(CourierDispatcher.class);

    private boolean alive = false;
    private ArrayBlockingQueue<CSMessage> mainQueue = null;
    private final ArrayBlockingQueue<CSMessage> courierQueue = new ArrayBlockingQueue<>(CSKitchen.maxQueue);

    public CourierDispatcher() {
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
        return csMessage.hasCommand() || (csMessage instanceof CSOrder && !((CSOrder) csMessage).isReady());
    }

    @Override
    public boolean isAlive() {
        return alive;
    }

    @Override
    public Integer call() {

        logger.info("Start Courier dispatching.");
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
                        logger.info("Get exit command, total {} orders, CourierDispatcher is quiting...", total);
                        break;
                    }
                }
                final CSCourier courier = new CSCourier(CSKitchen.COURIER_START, CSKitchen.COURIER_END);
                courier.setOrderPickedUp(((CSOrder) msg).getOrderId());
                for (int i = 0; i < CSKitchen.MSG_RETRY; i++) {
                    try {
                        mainQueue.add(courier);
                        break;
                    } catch (Exception e) {
                        logger.error("Failed to put to queue {} times, caught:", i, e);
                        Thread.sleep((i + 1) * CSKitchen.THOUSAND);
                    }
                }
                String logMsg2 = courier.getName() + " dispatched at " + courier.getDispatchTime()
                        + ", will arrive in " + courier.getArrivePeriod() + "s.";
                System.out.println(logMsg2);
                logger.info(logMsg2 + "\n" + courier);
                counter++;
            } catch (Exception e) {
                logger.error("Courier catch: ", e);
            }
        }
        this.alive = false;

        return counter;
    }
}