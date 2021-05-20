package de.dailab.jiactng.aot.gridworld.model;

import java.util.ArrayList;
import java.util.LinkedList;

public class WorkerEstimate {

    private String workerId;
    private Worker worker;
    private Position position;
    private LinkedList<Order> orderQueue;
    private Integer duration;

    public WorkerEstimate(Worker worker) {

        this.worker = worker;
        this.workerId = this.worker.id;
        this.position = this.worker.position;

    }

    /**
     * This function is used to check whether an order should be accepted by the worker
     * Especially it returns the index of the position where the worker should put the order
     * If bestIndex == -1 then no feasible position to insert the order is found and the worker
     * should not accept the order since it is not able to finish all orders within the deadline
     */
    public int checkOrder(Order order) {

        int bestIndex = -1;
        int minDuration = Integer.MAX_VALUE;

        for (int index = 0; index <= orderQueue.size(); index++) {
            LinkedList<Order> temporaryOrders = orderQueue;
            temporaryOrders.add(index, order);

            int currentDuration = calculateDuration(temporaryOrders);
            if(currentDuration == -1) {
                temporaryOrders.remove(order);
                continue;
            }

            if(currentDuration < minDuration) {
                minDuration = currentDuration;
                bestIndex = index;
            }

            temporaryOrders.remove(order);
        }

        return bestIndex;
    }

    private int calculateDuration(LinkedList<Order> orders) {
        Position currentPosition = this.position;
        int currentDuration = 0;

        for(Order currentOrder: orders) {

            currentDuration += currentOrder.position.distance(currentPosition);
            currentDuration += 1;
            currentPosition = currentOrder.position;
            if (currentDuration > currentOrder.deadline)
                return -1;
        }
        return currentDuration;
    }

    public void assignOrder(Order order) {

        this.orderQueue.add(order);

    }

    public void removeOrder(Order order) {

        this.orderQueue.remove(order);

    }

    public void updateDuration() {

        Position currentPosition = this.position;
        int currentDistance = 0;

        for(Order currentOrder: orderQueue) {

            currentDistance += currentOrder.position.distance(currentPosition);
            currentDistance += 1;
            currentPosition = currentOrder.position;

        }

        this.duration = currentDistance;

    }

}