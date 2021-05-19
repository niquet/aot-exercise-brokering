package de.dailab.jiactng.aot.gridworld.model;

import java.util.ArrayList;

public class WorkerEstimate {

    private String workerId;
    private Worker worker;
    private Position position;
    private ArrayList<Order> orders;
    private Integer duration;

    public WorkerEstimate(Worker worker) {

        this.worker = worker;
        this.workerId = this.worker.id;
        this.position = this.worker.position;

    }

    public void assignOrder(Order order) {

        this.orders.add(order);

    }

    public void removeOrder(Order order) {

        this.orders.remove(order);

    }

    public void updateDuration() {

        Position currentPosition = this.position;
        int currentDistance = 0;

        for(Order currentOrder: orders) {

            currentDistance += currentOrder.position.distance(currentPosition);
            currentDistance += 1;
            currentPosition = currentOrder.position;

        }

        this.duration = currentDistance;

    }

}