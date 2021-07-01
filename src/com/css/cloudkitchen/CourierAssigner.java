package com.css.cloudkitchen;

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
 * Get Order message from message bus, dispatch the order to a courier,
 * create a thread to simulate courier arrival.
 * After courier arrival, send Courier message to message bus.
 * Exit when all orders have been handled.
 * Return the total number of orders been handled.
 */
public class CourierAssigner implements IMessageHandler, Callable<Integer> {
    private static final Logger logger = LoggerFactory.getLogger(CourierAssigner.class);

    private boolean alive = false;
    private ArrayBlockingQueue<CSMessage> mainQueue = null;
    private final ArrayBlockingQueue<CSMessage> courierQueue = new ArrayBlockingQueue<>(CSKitchen.maxQueue);
    private final ThreadPoolExecutor courierTPool;
    private final ExecutorCompletionService<Integer> compServ;

    public CourierAssigner() {
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
        return csMessage.hasCommand() || csMessage instanceof CSOrder && !csMessage.hasReadyTime();
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
                    if (msg.getCommand().equals(CSKitchen.CMD_EXIT)) {
                        runState = false;
                        total = (int) msg.getPickupTime();
                        logger.info("Get exit command, total {} orders, Courier is quiting...", total);
                        break;
                    }
                }
                final CSCourier courier = new CSCourier(CSKitchen.COURIER_START, CSKitchen.COURIER_END);
                courier.setOrderPickedUp(msg.getId());
                compServ.submit(() -> {
                    try {
                        Thread.sleep((long) courier.getArrivePeriod() * CSKitchen.THOUSAND);
                        courier.setReadyTime(System.currentTimeMillis());
                        for (int i = 0; i < CSKitchen.MSG_RETRY; i++) {
                            try {
                                mainQueue.add(courier);
                                break;
                            } catch (Exception e) {
                                logger.error("Failed to put to queue {} times, caught:", i, e);
                                Thread.sleep((i + 1) * CSKitchen.THOUSAND);
                            }
                        }
                        String logMsg1 = courier.getName() + " arrived at " + courier.getReadyTime();
                        System.out.println(logMsg1);
                        logger.info(logMsg1);
                        return 1;
                    } catch (Exception e) {
                        logger.info("Courier arriving caught:", e);
                    }
                    return 0;
                });
                String logMsg2 = courier.getName() + " dispatched at " + courier.getCreateTime()
                        + ", will arrive in " + courier.getArrivePeriod() + "s.";
                System.out.println(logMsg2);
                logger.info(logMsg2);
                counter++;
            } catch (Exception e) {
                logger.error("Courier catch: ", e);
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