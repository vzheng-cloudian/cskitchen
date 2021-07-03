package com.css.cloudkitchen.message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;
import java.util.UUID;

/**
 * Define the Order type.
 * Provide an API to randomly choose a food type from the list.
 */
public class CSOrder extends CSMessage {
    private static final Logger logger = LoggerFactory.getLogger(CSOrder.class);

    private final String orderId;
    private final String name;
    private final long createTime;
    private final int prepTime;
    private long readyTime = 0L;
    private long pickupTime = 0L;

    /**
     * Construct an order with random food or static food (CheesePizza)
     * @param randomFood Randomly select food type or not
     */
    public CSOrder(final boolean randomFood) {
        this.createTime = System.currentTimeMillis();
        UUID uuid = new UUID(createTime, System.nanoTime());
        FoodOffering fo;
        if (randomFood) {
            fo = FoodOffering.getRandomFood();
        } else {
            fo = FoodOffering.CheesePizza;
        }
        this.orderId = uuid.toString();
        this.name = fo.getFood();
        this.prepTime = fo.getPrepTime();
    }

    public int getPrepTime() {
        return prepTime;
    }

    public String getOrderId() {
        return orderId;
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

    public boolean isReady() {
        return this.readyTime > 0L;
    }

    @Override
    public String toString() {
        return "MSG: " + this.msgID + ", CSOrder: " +
                this.orderId + "," +
                this.name + "," +
                this.createTime + "," +
                this.prepTime + "," +
                this.readyTime + "," +
                this.pickupTime;
    }

    public enum FoodOffering {
        CheesePizza("Cheese Pizza", 13),
        CheeseBurger("Cheese Burger", 14),
        IceCream("Ice Cream", 3),
        Sandwich("Sandwich", 4),
        HotDog("Hot Dog", 5),
        Chocolate("Chocolate", 6),
        Candy("Candy", 7),
        Tea("Tea", 8),
        Coffee("Coffee", 9),
        Cake("Cake", 10),
        Bread("Bread", 11),
        Salad("Salad", 12);

        private final String food;
        private final int prepTime;

        FoodOffering(final String n, final int t) {
            this.food = n;
            this.prepTime = t;
        }

        public String getFood() {
            return this.food;
        }

        public int getPrepTime() {
            return this.prepTime;
        }

        public static FoodOffering getRandomFood() {
            Random rand = new Random();
            int idx = rand.nextInt(FoodOffering.values().length);
            for (FoodOffering item : FoodOffering.values()) {
                if (item.ordinal() == idx) {
                    return item;
                }
            }
            logger.info("Unable to get a random food, Salad returned.");
            return Salad;
        }
    }
}
