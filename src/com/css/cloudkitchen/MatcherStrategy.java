package com.css.cloudkitchen;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * A message consumer.
 * Get both orders and couriers from message bus.
 * Apply different strategies to match the food orders and the couriers.
 * Adopt Strategy design pattern.
 */
public class MatcherStrategy implements IMessageHandler, Callable<Integer> {
    private static final Logger logger = LoggerFactory.getLogger(MatcherStrategy.class);

    private boolean alive = false;
    private final ArrayBlockingQueue<CSMessage> inQueue = new ArrayBlockingQueue<>(CSKitchen.maxQueue);
    private boolean stopSign = false;
    private final AbstractStrategy strategy;

    public MatcherStrategy(final AbstractStrategy strategy) {
        this.strategy = strategy;
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
        logger.info("Start Matcher strategy : {}", strategy.getName());
        this.alive = true;

        int counter = 0; // total orders matched
        CSMessage msg;
        int grace = CSKitchen.GRACE_TIME;
        int total = 0;
        while (true) {
            try {
                if (stopSign && (grace <= 0 || counter >= total)) {
                    logger.info("Total {} out of {} orders handled, {} quiting...", counter, total, strategy.getName());
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
                        logger.info("Get exit command, Matcher {} is quiting...", strategy.getName());
                        total = (int) msg.getPickupTime();
                        stopSign = true;
                        continue;
                    }
                }

                CSMessage matched = strategy.apply(msg);
                if (matched != null) {
                    counter++;
                }

            } catch (Exception e) {
                logger.error("Matcher {} catch: ", strategy.getName(), e);
            }
        }

        this.alive = false;
        strategy.printStatistics();
        return counter;
    }
}
