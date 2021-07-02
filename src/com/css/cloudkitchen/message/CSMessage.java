package com.css.cloudkitchen.message;

/**
 * Basic message type
 */
public abstract class CSMessage {
    protected String id;
    protected String name;
    protected long createTime = 0L;
    protected long readyTime = 0L;
    protected long pickupTime = 0L;
    protected String command = null; // command for notifying all components

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public long getCreateTime() {
        return createTime;
    }

    public long getReadyTime() {
        return readyTime;
    }

    public void setReadyTime(final long readyTime) {
        this.readyTime = readyTime;
    }

    public long getPickupTime() {
        return pickupTime;
    }

    public void setPickupTime(final long pickupTime) {
        this.pickupTime = pickupTime;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(final String command) {
        this.command = command;
    }

    public boolean hasReadyTime() {
        return this.readyTime > 0L;
    }

    public boolean hasCommand() {
        return this.command != null;
    }

    protected void deepCopy(CSMessage source) {
        this.id = source.id;
        this.name = source.name;
        this.createTime = source.createTime;
        this.readyTime = source.readyTime;
        this.pickupTime = source.pickupTime;
        this.command = source.command;
    }

}
