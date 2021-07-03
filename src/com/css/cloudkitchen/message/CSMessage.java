package com.css.cloudkitchen.message;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Basic message type
 */
public abstract class CSMessage {
    //message id generator
    private static final AtomicInteger msgIDs = new AtomicInteger(0);

    protected final int msgID = msgIDs.incrementAndGet();
    protected String command = null; // command for notifying all components

    public int getMsgID() {
        return this.msgID;
    }

    public String getCommand() {
        return command;
    }

    public String getCommandOption() {
        return command.split(":")[1];
    }

    public void setCommand(final String command, final String option) {
        this.command = command + ":" + option;
    }

    public boolean hasCommand() {
        return this.command != null;
    }

}
