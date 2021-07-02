package com.css.cloudkitchen;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The core of the Message Bus system.
 * It registers message producers and consumers ( also called publisher and subscribers ).
 * It gets messages from the Message Bus, and dispatches them to the subscribers.
 * Adopt Observer and Singleton and Mediator design pattern.
 */
public class MessageDispatcher implements Callable<Integer> {
    private static final Logger logger = LoggerFactory.getLogger(MessageDispatcher.class);

    private static final AtomicBoolean stopSign = new AtomicBoolean(false);
    // main message bus
    private final ArrayBlockingQueue<CSMessage> mainQueue = new ArrayBlockingQueue<>(CSKitchen.maxQueue);
    private final List<IMessageHandler> consumer = new ArrayList<>();
    private final ThreadPoolExecutor retryTPool =
            Helpers.createConstraintPool("Dispatch-Retry ", CSKitchen.maxQueue, CSKitchen.KEEP_ALIVE);
    private final ExecutorCompletionService<Integer> compServ = new ExecutorCompletionService<>(retryTPool);

    private static final MessageDispatcher self = new MessageDispatcher();

    private MessageDispatcher() {}

    public static MessageDispatcher getInstance() {
        return self;
    }

    public void register(final IMessageHandler subscriber) {
        if (subscriber.getInQueue() != null) {
            this.consumer.add(subscriber);
        }
        subscriber.setOutQueue(this.mainQueue);
    }

    @Override
    public Integer call() {
        logger.info("Message Dispatcher start.");
        Integer total = 0;
        CSMessage message;
        do {
            try {
                message = mainQueue.poll(1, TimeUnit.SECONDS);
                if (message == null) {
                    continue;
                }
                total++;
                for (IMessageHandler oh : this.consumer) {
                    try {
                        if (oh.isAlive() && oh.filter(message)) {
                            oh.getInQueue().add(message);
                        }
                    } catch (Exception e) {
                        logger.error("Failed to handle {} , put to retry thread.", message);
                        addRetryThread(oh.getInQueue(), message);
                    }
                }
            } catch (Throwable e) {
                logger.error("Message Dispatcher caught: ", e);
            }
        } while (!stopSign.get());

        logger.info("Message Dispatcher total dispatched {}, quiting...", total);

        Future<Integer> ret;
        final int interval = 1;
        while (retryTPool.getCompletedTaskCount() < retryTPool.getTaskCount()) {
            try {
                ret = compServ.poll(interval, TimeUnit.SECONDS);
                if (ret != null && ret.get() != 1) {
                    logger.info("Message re-send failed, completed with {} .", ret.get());
                }
            } catch (Exception e) {
                logger.info("Caught: ", e);
            }
        }

        logger.info("Message Dispatcher re-send threads completed.");

        return total;
    }

    private void addRetryThread(final ArrayBlockingQueue<CSMessage> outQueue, final CSMessage message) {
        final CSMessage msgRetry;
        if (message instanceof CSOrder) {
            msgRetry = new CSOrder((CSOrder) message);
        } else {
            msgRetry = new CSCourier((CSCourier) message);
        }

        compServ.submit(() -> {
            try {
                for (int i = 0; i < CSKitchen.MSG_RETRY; i++)  {
                    try {
                        outQueue.add(msgRetry);
                        logger.info("Re-send message {} successfully.", msgRetry);
                        break;
                    } catch (Exception e) {
                        logger.error("Failed to put message {} to queue {} times.", msgRetry, i);
                        Thread.sleep((i + 1) * CSKitchen.THOUSAND);
                    }
                }
                return 1;
            } catch (Exception e) {
                logger.info("Message dispatcher re-send message caught:", e);
            }
            return 0;
        });
    }

    public static void stopDispatcher() {
        stopSign.set(true);
    }
}
