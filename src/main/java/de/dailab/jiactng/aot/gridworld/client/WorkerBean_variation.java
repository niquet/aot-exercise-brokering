package de.dailab.jiactng.aot.gridworld.client;


import de.dailab.jiactng.agentcore.AbstractAgentBean;
import de.dailab.jiactng.agentcore.action.Action;
import de.dailab.jiactng.agentcore.comm.ICommunicationAddress;
import de.dailab.jiactng.agentcore.comm.ICommunicationBean;
import de.dailab.jiactng.agentcore.comm.message.JiacMessage;
import de.dailab.jiactng.agentcore.knowledge.IFact;
import de.dailab.jiactng.aot.gridworld.messages.*;
import de.dailab.jiactng.aot.gridworld.model.Order;
import de.dailab.jiactng.aot.gridworld.model.Position;
import de.dailab.jiactng.aot.gridworld.model.WorkerAction;
import org.sercho.masp.space.event.SpaceEvent;
import org.sercho.masp.space.event.SpaceObserver;
import org.sercho.masp.space.event.WriteCallEvent;

import java.io.Serializable;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;


/**
 * You can use this stub as a starting point for your worker bean or start from scratch.
 */



public class WorkerBean_variation extends AbstractAgentBean {
	/*
	 * If you want to create your variations you can simply create a copy of your
	 * working WorkerBean or BrokerBean and rename as you like. Include your variation in the client.xml by
	 * naming the new agent in the client node list and have that new agents class point to
	 * your new bean. You can find an example in the client.xml
	 *
	 * Note: As this method most likely will reuse a lot of code from your standard implementation
	 * this is not the most elegant way to do this but since your Agent and AgentBean should represented
	 * the real world and for the sake of simplicity it is allowed/desired. There will be no point deduction
	 * for "bad coding" behavior on that matter.
	 */
	private final Map<String, Order> currentOrders = new HashMap<>();
	private final Map<Order, ICommunicationAddress> orderToAddress = new HashMap<>();
	private final Comparator<Order> compareOrder = new Comparator<Order>() {
		@Override
		public int compare(Order o1, Order o2) {
			int p1 = position.distance(o1.position);
			int p2 = position.distance(o2.position);
			if(o1.deadline - o1.value < o2.deadline - o2.value){
				if(p1 + time + o2.position.distance(o1.position) < o2.deadline) return -1;
			} else
			if(p1 < p2) return -1;
			if(p1 > p2) return 1;
			//if(o1.deadline < o2.deadline) return -1;
			//if(o1.deadline > o2.deadline) return 1;
			return 0;
		}
	};
	private final PriorityQueue<Order> priorityQueue = new PriorityQueue<>(compareOrder);
	private Order handleOrder = null;
	private Boolean hasArrivedAtTarget = false;
	private Integer gameId = null;

	private Position position = null;
	private WorkerAction lastMove = null;
	private Boolean lastMoveFailed = false;

	private String workerIdForServer = null;
	private ICommunicationAddress broker = null;
	private int time;


	@Override
	public void doStart() throws Exception {
		/*
		 * this will be called once when the agent starts and can be used for initialization work
		 * note that when this method is executed, (a) it is not guaranteed that all the other
		 * agents are already started and/or their actions are known, and (b) the agent's execution
		 * has not yet started, so do not wait for any actions to be completed in this method (you
		 * can invoke actions, though, if they are already known to the agent)
		 *
		 *
		 * You can use a SpaceObserver to listen to messages, but you can also check messages in execute()
		 * and only temporarily attach a SpaceObserver for specific purposes
		 *
		 * As an example it is added here at the beginning.
		 */
		memory.attach(new WorkerBean_variation.MessageObserver(), new JiacMessage());

		log.info("starting...");
	}

	@Override
	public void execute() {
		/*
		 * this is executed periodically by the agent; check the BrokerBean.java for an example.
		 */

		// if we already have assignments
		if(!priorityQueue.isEmpty()) {

			Order firstOrder = handleOrder; //priorityQueue.peek();
			time += 1;
			/**
			 * We handle the order
			 * send message to server
			 */

			if(position == null)
				return;
			WorkerMessage move = new WorkerMessage();
			move.action = getNextMove(position, firstOrder.position, lastMoveFailed);
			lastMove = move.action;
			move.gameId = gameId;
			move.workerId = workerIdForServer;
			//System.out.println("WORKERIDFORSERVER " + workerIdForServer);

			sendMessage(orderToAddress.get(firstOrder), move);

		}

	}




	/*
	 * You can implement some functions and helper methods here.
	 */



	/** This is an example of using the SpaceObeserver for message processing. */
	@SuppressWarnings({"serial", "rawtypes"})
	class MessageObserver implements SpaceObserver<IFact> {

		@Override
		public void notify(SpaceEvent<? extends IFact> event) {
			if (event instanceof WriteCallEvent) {
				JiacMessage message = (JiacMessage) ((WriteCallEvent) event).getObject();
				Object payload = message.getPayload();

				if (payload instanceof AssignOrderMessage) {
					/** Order to assign to the agent */
					if (gameId == null) gameId = ((AssignOrderMessage) message.getPayload()).gameId;

					ICommunicationAddress broker = message.getSender();

					AssignOrderMessage assignOrderMessage = (AssignOrderMessage) message.getPayload();

					Order order = assignOrderMessage.order;
					ICommunicationAddress server = assignOrderMessage.server;

					if (position != null) {

						AssignOrderConfirm assignOrderConfirm = new AssignOrderConfirm();
						assignOrderConfirm.orderId = order.id;
						assignOrderConfirm.gameId = assignOrderMessage.gameId;
						assignOrderConfirm.workerId = thisAgent.getAgentId();
						assignOrderConfirm.state = Result.FAIL;
						priorityQueue.add(order);

						if(possibleEnd()) {
						orderToAddress.put(order, server);

						assignOrderConfirm.state = Result.SUCCESS;
						} else {
							priorityQueue.remove(order);
						}

						if (priorityQueue.contains(order) && handleOrder == null) handleOrder = order;
						sendMessage(broker, assignOrderConfirm);
					}
				}

				if (payload instanceof PositionMessage) {
					/** Order to assign to the agent */

					PositionMessage positionMessage = (PositionMessage) message.getPayload();


					ICommunicationAddress brokerAddress = message.getSender();
					broker = brokerAddress;

					PositionConfirm positionConfirm = new PositionConfirm();
					positionConfirm.workerId = thisAgent.getAgentId();
					positionConfirm.gameId = positionMessage.gameId;

					/**
					 * Send Position confirm with FAIL if the message is not for us
					 */
					if(!positionMessage.workerId.equals(thisAgent.getAgentId()) && position == null) {
						positionConfirm.state = Result.FAIL;
						sendMessage(brokerAddress, positionConfirm);
						return;
					}

					positionConfirm.state = Result.SUCCESS;
					sendMessage(brokerAddress, positionConfirm);
					time = 1;
					/**
					 * Only set position if it is for us
					 */
					if(position == null || workerIdForServer == null) {
						position = positionMessage.position;
						workerIdForServer = positionMessage.workerIdForServer;
					}


					/**
					 *
					 * DEBUGGING
					 *
					 */
					//System.out.println("WORKER RECEIVED " + positionMessage.toString());
					log.info("WORKER RECEIVED " + positionMessage.toString());

				}

				if (payload instanceof WorkerConfirm) {

					WorkerConfirm workerConfirm = (WorkerConfirm) message.getPayload();
					Result result = workerConfirm.state;

					if(workerConfirm.action == WorkerAction.ORDER){
						priorityQueue.poll();
						System.out.println("SUCCESS " + handleOrder);
						handleOrder = priorityQueue.peek();
						hasArrivedAtTarget = false;
					}

					if (result == Result.FAIL) {
						// TODO unbekannte obstacles
						if(workerConfirm.action != WorkerAction.ORDER) {
							lastMoveFailed = true;
						}

						return;
					}

					if (!hasArrivedAtTarget) {
						// Agent hasn't arrived at target, so conduct the planned move
						doMove(workerConfirm.action);
						lastMoveFailed = false;
						// Update position at broker
						PositionUpdate positionUpdate = new PositionUpdate();
						positionUpdate.workerId = thisAgent.getAgentId();
						positionUpdate.position = position;
						positionUpdate.gameId = gameId;
						sendMessage(broker, positionUpdate);

						System.out.println("POSITION " + position);

					}
				}

				/*if (payload instanceof OrderCompleted){
					// TODO if FAIL anders reagieren?
					if(((OrderCompleted) payload).state == Result.SUCCESS){
					priorityQueue.poll();
					System.out.println("SUCCESS " + handleOrder);
					handleOrder = priorityQueue.peek();
					hasArrivedAtTarget = false;
					}
				}*/

			}
		}
	}

	/** example function to send messages to other agents */
	private void sendMessage(ICommunicationAddress receiver, IFact payload) {
		Action sendAction = retrieveAction(ICommunicationBean.ACTION_SEND);
		JiacMessage message = new JiacMessage(payload);
		invoke(sendAction, new Serializable[] {message, receiver});
		System.out.println("WORKER SENDING " + payload);
	}

	/** sort the orders according to a score and put them into a queue
	 *
	 */
	private void sortOrders() {
		// TODO
	}

	/** evaluate order score */
	private void evaluateOrder(Order order) {
		// TODO
	}

	/** evaluate when we'll probably be at the target to decide if move is possible before deadline */
	private boolean possibleEnd(){
		Position goal = position;
		int jetzt = time;
		boolean inTime = true;
		for (Order order: priorityQueue) {
			jetzt += order.position.distance(goal);
			if(jetzt > order.deadline) return false;
			goal = order.position;
		}
		return inTime;
	}

	/** calculate next move */
	private WorkerAction getNextMove(Position current, Position target, Boolean lastMoveFailed) {
		// TODO
		if (current.equals(target)) {
			hasArrivedAtTarget = true;
			return WorkerAction.ORDER;
		}

		int[] distances = null;

		if (lastMoveFailed) {

			switch (lastMove) {
				case NORTH:
				case SOUTH:
					Position E = new Position(current.x + 1, current.y);
					Position W = new Position(current.x - 1, current.y);
					distances = new int[]{target.distance(E), target.distance(W)};
					return (distances[0] > distances[1]) ? WorkerAction.EAST:WorkerAction.WEST;
				case EAST:
				case WEST:
					Position N = new Position(current.x, current.y - 1);
					Position S = new Position(current.x, current.y + 1);
					distances = new int[]{target.distance(N), target.distance(S)};
					return (distances[0] > distances[1]) ? WorkerAction.NORTH:WorkerAction.SOUTH;
			}

		} else {

			// [N, S, E, W]
			Position N = new Position(current.x, current.y - 1);
			Position S = new Position(current.x, current.y + 1);
			Position E = new Position(current.x + 1, current.y);
			Position W = new Position(current.x - 1, current.y);

			distances = new int[]{target.distance(N), target.distance(S), target.distance(E), target.distance(W)};

		}

		WorkerAction workerAction = null;
		int index = -1;
		int min = Integer.MAX_VALUE;

		for (int i = 0; i < distances.length; i++) {

			if (distances[i] < min) {
				min = distances[i];
				index = i;
			}

		}

		switch(index) {
			case 1:
				workerAction = WorkerAction.SOUTH;
				break;
			case 2:
				workerAction = WorkerAction.EAST;
				break;
			case 3:
				workerAction = WorkerAction.WEST;
				break;
			case 0:
				workerAction = WorkerAction.NORTH;
				break;
			default:
				workerAction = WorkerAction.ORDER;
				break;
		}

		return workerAction;

	}

	private void doMove(WorkerAction action) {
		if (action == WorkerAction.NORTH) position = new Position(position.x, position.y - 1);
		if (action == WorkerAction.SOUTH) position = new Position(position.x, position.y + 1);
		if (action == WorkerAction.WEST)  position = new Position(position.x - 1, position.y);
		if (action == WorkerAction.EAST)  position = new Position(position.x + 1, position.y);
	}


}