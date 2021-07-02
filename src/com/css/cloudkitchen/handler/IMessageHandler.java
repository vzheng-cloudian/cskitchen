package com.css.cloudkitchen.handler;

import com.css.cloudkitchen.message.CSMessage;

import java.util.concurrent.ArrayBlockingQueue;

/**
 * Define interface for message plugins.
 * A message handler can be a message consumer or a message producer, or both of them.
 * It plug into the message bus, get messages from it / put messages to it.
 */
public interface IMessageHandler {

    /**
     * For message consumer.
     * The In-Queue is the message source to be consumed.
     * The message handler gets messages from this queue.
     * @return a reference to a Message Queue
     */
    ArrayBlockingQueue<CSMessage> getInQueue();

    /**
     * For message producer.
     * The Out-Queue is the destination of the messages which have been produced.
     * The message handler puts messages to this queue.
     * @param outQueue An instance of a Message Queue
     */
    void setOutQueue(ArrayBlockingQueue<CSMessage> outQueue);

    /**
     * For message consumer.
     * The message handler will only receive messages that it wants.
     * When a new message comes in, call this filter to check if it is wanted.
     * @param csMessage A new message
     * @return True if it is wanted
     */
    boolean filter(CSMessage csMessage);

    /**
     * Get the current state of the message handler.
     * @return Ture if alive
     */
    boolean isAlive();
}
