package com.css.cloudkitchen;

import com.css.cloudkitchen.handler.IMessageHandler;
import com.css.cloudkitchen.message.CSMessage;
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
        do {
            try {
                final CSMessage message = mainQueue.poll(1, TimeUnit.SECONDS);
                if (message == null) {
                    continue;
                }
                total++;
                for (IMessageHandler mh : this.consumer) {
                    try {
                        if (mh.isAlive() && mh.filter(message)) {
                            mh.getInQueue().add(message);
                        }
                    } catch (Exception e) {
                        logger.error("Failed to handle {} , put to retry thread.", message);
                        addRetryThread(mh, message);
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

    private void addRetryThread(final IMessageHandler mh, final CSMessage msgRetry) {
        final ArrayBlockingQueue<CSMessage> outQueue = mh.getInQueue();

        compServ.submit(() -> {
            try {
                for (int i = 0; i < CSKitchen.MSG_RETRY; i++)  {
                    try {
                        if (!mh.isAlive()) {
                            throw new Exception("The message receiver is inactive, failed to re-send " + msgRetry);
                        }
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
