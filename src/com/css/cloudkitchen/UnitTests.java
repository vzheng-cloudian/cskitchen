package com.css.cloudkitchen;

import org.junit.Test;

import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import static org.junit.Assert.*;

public class UnitTests {

    /**
     * Test the Uniform Distribution implementation.
     * Verifying the bias is less than 1% .
     */
    @Test
    public void courierGeneratorTest() {
        int start = 3;
        int end = 15;
        int total = 2000000;
        int avg = total / (end - start + 1);
        int ret;
        int[] results = new int[end];
        for (int i = 0; i < end; i++) {
            results[i] = 0;
        }
        for (int i = 0; i < total; i++) {
            ret = CSCourier.uniformDistribution(3, 15);
            results[ret - 1] = results[ret - 1] + 1;
        }
        for (int i = 0; i < 15; i++) {
            System.out.println(results[i]);
            if (results[i] > 0) {
                assertTrue((Math.abs(results[i] - avg) * 100 / avg) <= 1);
            }
        }
    }

    /**
     * Test the FIFO algorithm, implemented by the structure PriorityQueue
     * 1. generate a courier as a base;
     * 2. put it to the queue;
     * 3. create a new courier with random arrival time, compare it with the base, save the smaller one as the new base;
     * 4. put it to the queue, and peek the header of the queue;
     * 5. compare it with the base, they should be the same;
     * 6. repeat from step 3.
     */
    @Test
    public void priorityQTest() throws Exception {
        PriorityQueue<CSCourier> courierPQ = new PriorityQueue<>((o1, o2) -> {
            if (o1.getReadyTime() <= 0L || o2.getReadyTime() <= 0L) {
                return 0;
            }
            return Long.compare(o1.getReadyTime(), o2.getReadyTime());
        });

        int total = 100;
        CSCourier cbase = new CSCourier(9, 15); // the smallest readyTime as the base
        cbase.setReadyTime(cbase.getCreateTime() + cbase.getArrivePeriod() * 1000L);
        System.out.println("B:" + cbase.getReadyTime());
        courierPQ.add(cbase);

        for (int i = 0; i < total; i++) {
            Thread.sleep(1);
            // add new couries
            CSCourier courier = new CSCourier(3, 15);
            courier.setReadyTime(courier.getCreateTime() + courier.getArrivePeriod() * 1000L);
            if (cbase.getReadyTime() > courier.getReadyTime()) {
                cbase = new CSCourier(courier);
                System.out.println("B:" + cbase.getReadyTime());
            }
            courierPQ.add(courier);

            CSCourier cnew = courierPQ.peek();
            assert cnew != null;
            assertEquals(cbase.getReadyTime(), cnew.getReadyTime());
        }
    }

    /**
     * Test the class OrderGenerator.
     * 1. set the parameters of OrderGenerator and start it;
     * 2. the correct number of orders should be created at the designated rate;
     * 3. exit as expected.
     */
    @Test
    public void orderGeneratorTest() {
        ArrayBlockingQueue<CSMessage> mainQueue = new ArrayBlockingQueue<>(CSKitchen.maxQueue);
        OrderGenerator og = new OrderGenerator(9, 20, true);
        Queue<CSMessage> inQueue = og.getInQueue();
        og.setOutQueue(mainQueue);
        long starttime = System.currentTimeMillis();
        Integer total = og.call();
        long runtime = (System.currentTimeMillis() - starttime) / 1000;
        assertEquals(20, (int) total);
        assertEquals(3 + 13, runtime);
        assertNull(inQueue);
        assertEquals(21, mainQueue.size()); // include exit command
    }

    /**
     * Test the class MessageDispatcher
     * 1. assemble the workflow with some consumers and producers;
     * 2. start the workflow, no issue should be seen;
     * 3. set the stop sign, the MessageDispatcher stopped as expected without delay.
     */
    @Test
    public void messageDispatcherTest() throws Exception {
        final OrderGenerator og = new OrderGenerator(12, 20, true);
        final CourierAssigner ca = new CourierAssigner();
        final FoodCooker fc = new FoodCooker();
        final MessageDispatcher md = MessageDispatcher.getInstance();
        md.register(og);
        md.register(ca);
        md.register(fc);
        Thread thread1 = new Thread(ca::call);
        Thread thread2 = new Thread(fc::call);
        Thread thread3 = new Thread(og::call);
        Thread thread = new Thread(md::call);
        long starttime = System.currentTimeMillis();
        thread.start();
        thread1.start();
        thread2.start();
        thread3.start();
        Thread.sleep(15000);
        MessageDispatcher.stopDispatcher();
        Thread.sleep(1000);
        assertFalse(thread.isAlive());
        long runtime = (System.currentTimeMillis() - starttime) / 1000;
        assertTrue(runtime <= 16L);
    }

    /**
     * Test the class CourierAssigner.
     * 1. send all kinds of messages to CourierAssigner;
     * 2. it can deal with all these messages;
     * 3. it only handle those messages it wants.
     * 4. exit when get the EXIT command.
     */
    @Test
    public void courierAssignerTest() {
        ArrayBlockingQueue<CSMessage> mainQueue = new ArrayBlockingQueue<>(CSKitchen.maxQueue);
        CourierAssigner ca = new CourierAssigner();
        Queue<CSMessage> inQueue = ca.getInQueue();
        ca.setOutQueue(mainQueue);

        for (int i = 0; i < 100; i++) {
            // new orders
            CSOrder order1 = new CSOrder(true);
            if (ca.filter(order1)) {
                inQueue.add(order1);
            }
            // cooked orders
            CSOrder order2 = new CSOrder(true);
            order2.setReadyTime(System.currentTimeMillis());
            if (ca.filter(order2)) {
                inQueue.add(order2);
            }
            // new couriers
            CSCourier courier1 = new CSCourier(3, 15);
            if (ca.filter(courier1)) {
                inQueue.add(courier1);
            }
            // arrived couriers
            CSCourier courier2 = new CSCourier(3, 15);
            courier2.setReadyTime(System.currentTimeMillis());
            if (ca.filter(courier2)) {
                inQueue.add(courier2);
            }
        }

        // send exit command
        CSOrder order = new CSOrder(false);
        order.setCommand(CSKitchen.CMD_EXIT);
        inQueue.add(order);

        Integer total = ca.call();
        assertEquals(100, (int) total);
        assertEquals(100, mainQueue.size());
    }

    /**
     * Test the class FoodCooker.
     * 1. send all kinds of messages to FoodCooker;
     * 2. it can deal with all these messages;
     * 3. it only handle those messages it wants.
     * 4. exit when get the EXIT command.
     */
    @Test
    public void foodCookerTest() {
        ArrayBlockingQueue<CSMessage> mainQueue = new ArrayBlockingQueue<>(CSKitchen.maxQueue);
        FoodCooker fc = new FoodCooker();
        Queue<CSMessage> inQueue = fc.getInQueue();
        fc.setOutQueue(mainQueue);

        for (int i = 0; i < 100; i++) {
            // new orders
            CSOrder order1 = new CSOrder(true);
            if (fc.filter(order1)) {
                inQueue.add(order1);
            }
            // cooked orders
            CSOrder order2 = new CSOrder(true);
            order2.setReadyTime(System.currentTimeMillis());
            if (fc.filter(order2)) {
                inQueue.add(order2);
            }
            // new couriers
            CSCourier courier1 = new CSCourier(3, 15);
            if (fc.filter(courier1)) {
                inQueue.add(courier1);
            }
            // arrived couriers
            CSCourier courier2 = new CSCourier(3, 15);
            courier2.setReadyTime(System.currentTimeMillis());
            if (fc.filter(courier2)) {
                inQueue.add(courier2);
            }
        }

        // send exit command
        CSOrder order = new CSOrder(false);
        order.setCommand(CSKitchen.CMD_EXIT);
        inQueue.add(order);

        Integer total = fc.call();
        assertEquals(100, (int) total);
        assertEquals(100, mainQueue.size());
    }

    /**
     * Test the MATCH strategy.
     * 1. create some orders;
     * 2. create the same amount of couriers, but only assign 1/2 of them to the orders;
     * 3. apply the MATCH strategy, only half orders will be matched.
     * 4. exit when get the EXIT command.
     */
    @Test
    public void strategyMATCHTest() {
        ArrayBlockingQueue<CSMessage> mainQueue = new ArrayBlockingQueue<>(CSKitchen.maxQueue);
        MatcherStrategy ms1 = new MatcherStrategy(new StrategyMatch());
        Queue<CSMessage> inQueue = ms1.getInQueue();
        ms1.setOutQueue(mainQueue);

        for (int i = 0; i < 100; i++) {
            // new orders
            CSOrder order1 = new CSOrder(true);
            if (i > 50) { // total 49 new orders
                inQueue.add(order1);
            }
            // cooked orders
            CSOrder order2 = new CSOrder(true);
            order2.setReadyTime(System.currentTimeMillis());
            inQueue.add(order2);
            // new couriers, should be rejected
            CSCourier courier1 = new CSCourier(3, 15);
            inQueue.add(courier1);
            // arrived couriers
            CSCourier courier2 = new CSCourier(3, 15);
            courier2.setReadyTime(System.currentTimeMillis());
            if (i % 2 == 1) {
                courier2.setOrderPickedUp(order2.getId());
            }
            inQueue.add(courier2);
        }

        // send exit command
        CSOrder order = new CSOrder(false);
        order.setCommand(CSKitchen.CMD_EXIT);
        inQueue.add(order);

        Integer total = ms1.call();
        assertEquals(50, (int) total);
        assertEquals(0, mainQueue.size());
    }

    /**
     * Test the FIFO strategy.
     * 1. create some couriers;
     * 2. create some orders without couriers assigned;
     * 3. apply FIFO strategy, those orders and couriers will be matched.
     * 4. exit when get the EXIT command.
     */
    @Test
    public void strategyFIFOTest() throws InterruptedException {
        ArrayBlockingQueue<CSMessage> mainQueue = new ArrayBlockingQueue<>(CSKitchen.maxQueue);
        MatcherStrategy ms2 = new MatcherStrategy(new StrategyFIFO());
        Queue<CSMessage> inQueue = ms2.getInQueue();
        ms2.setOutQueue(mainQueue);
        for (int i = 0; i < 110; i++) {
            CSCourier courier = new CSCourier(3, 15);
            courier.setReadyTime(courier.getCreateTime() + courier.getArrivePeriod() * 1000L);
            inQueue.add(courier);
            Thread.sleep(1);
        }
        for (int i = 0; i < 100; i++) {
            // new orders
            CSOrder order1 = new CSOrder(true);
            inQueue.add(order1);
            // cooked orders
            CSOrder order2 = new CSOrder(true);
            order2.setReadyTime(System.currentTimeMillis());
            inQueue.add(order2);
            // new couriers, should be rejected
            CSCourier courier1 = new CSCourier(3, 15);
            inQueue.add(courier1);
        }

        // send exit command
        CSOrder order = new CSOrder(false);
        order.setCommand(CSKitchen.CMD_EXIT);
        inQueue.add(order);

        Integer total = ms2.call();
        assertEquals(100, (int) total);
        assertEquals(0, mainQueue.size());

    }
}
