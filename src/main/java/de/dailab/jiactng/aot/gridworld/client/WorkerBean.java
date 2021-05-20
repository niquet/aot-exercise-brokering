package de.dailab.jiactng.aot.gridworld.client;


import de.dailab.jiactng.agentcore.action.Action;
import de.dailab.jiactng.agentcore.comm.ICommunicationAddress;
import de.dailab.jiactng.agentcore.comm.ICommunicationBean;
import de.dailab.jiactng.aot.gridworld.messages.*;
import de.dailab.jiactng.aot.gridworld.model.Order;
import de.dailab.jiactng.aot.gridworld.model.Position;
import de.dailab.jiactng.aot.gridworld.model.WorkerAction;
import org.sercho.masp.space.event.SpaceEvent;
import org.sercho.masp.space.event.SpaceObserver;
import org.sercho.masp.space.event.WriteCallEvent;

import de.dailab.jiactng.agentcore.AbstractAgentBean;
import de.dailab.jiactng.agentcore.comm.message.JiacMessage;
import de.dailab.jiactng.agentcore.knowledge.IFact;

import java.io.Serializable;
import java.util.*;


/**
 * You can use this stub as a starting point for your worker bean or start from scratch.
 */



public class WorkerBean extends AbstractAgentBean {
	/*
	 * this bean represents one of your Worker agents (i.e. each Worker Agent you initialize with this bean
	 * will have a separate copy); it's structure will be similar to your Broker agent's
	 *
	 *
	 * note that the number of workers may vary from grid to grid, but the number of worker
	 * agents will always be the same (specified in the client.xml); you will have to have your Broker somehow tell
	 * the worker agents which of them are currently needed and who may idle
	 *
	 * you could, theoretically, also control all your Workers from a single worker agent (and
	 * bean), or even implement both the Broker and the Worker in the same bean, but that would
	 * of course defeat the purpose of this exercise and may not be possible in "real life"
	 */

	private Map<String, Order> currentOrders = new HashMap<>();
	private Map<Order, ICommunicationAddress> orderToAddress = new HashMap<>();
	private LinkedList<String> orderQueue = new LinkedList<>();
	private Comparator<Order> compareOrder = new Comparator<Order>() {
		@Override
		public int compare(Order o1, Order o2) {
			int p1 = position.distance(o1.position);
			int p2 = position.distance(o2.position);
			if(p1 < p2) return -1;
			if(p1 > p2) return 1;
			/*if(o1.deadline < o2.deadline) return -1;
			if(o1.deadline > o2.deadline) return 1;*/
			return 0;
		}
	};
	private PriorityQueue<Order> priorityQueue = new PriorityQueue<>(compareOrder);
	private Boolean isHandlingOrder = false;
	private Order handleOrder = null;
	private Boolean hasArrivedAtTarget = false;
	private Integer gameId = null;

	private Position position = null;
	private String workerIdForServer = null;
	private ICommunicationAddress broker = null;


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
		memory.attach(new MessageObserver(), new JiacMessage());

		log.info("starting...");
	}

	@Override
	public void execute() {
		/*
		 * this is executed periodically by the agent; check the BrokerBean.java for an example.
		 */
		// TODO

		// notify -> if new Order arrived
			// evaluateOrder();
			// sortOrders();


		if(!priorityQueue.isEmpty()) {
			/*String orderId = orderQueue.peekFirst();
			Order firstOrder = currentOrders.get(orderId);
			System.out.println("ORDER IS " + firstOrder);*/

			Order firstOrder = handleOrder; //priorityQueue.element();

			/**
			 * We handle the order
			 * send message to server
			 */
			/*if(firstOrder.position.equals(position)) {
				hasArrivedAtTarget = true;
				// TODO Send message to server
				WorkerMessage orderFinished = new WorkerMessage();
				orderFinished.gameId = gameId;
				orderFinished.workerId = workerIdForServer;
				orderFinished.action = WorkerAction.ORDER;

				sendMessage(orderToAddress.get(handleOrder), orderFinished);

			} else {*/
				if(position == null)
					return;
				WorkerMessage move = new WorkerMessage();
				move.action = getNextMove(position, firstOrder.position);
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
						//Map<Order, ICommunicationAddress> orderWithServer = new HashMap<>();
						orderToAddress.put(order, server);
						//currentOrders.put(order.id, order);
						priorityQueue.add(order);
						//orderQueue.push(order.id);

						// TODO do something / evaluate

						AssignOrderConfirm assignOrderConfirm = new AssignOrderConfirm();
						assignOrderConfirm.orderId = order.id;
						assignOrderConfirm.gameId = assignOrderMessage.gameId;
						assignOrderConfirm.workerId = thisAgent.getAgentId();

						if (handleOrder == null) handleOrder = order;
						//position.distance(order.position) <= position.distance(handleOrder.position)
						//TODO reasons we don't want to take the order
						if (position.distance(order.position) <= position.distance(handleOrder.position)) {
							if (priorityQueue.element() == order) {
								assignOrderConfirm.state = Result.SUCCESS;
								handleOrder = order;
							} else {
								assignOrderConfirm.state = Result.FAIL;
							}

							sendMessage(broker, assignOrderConfirm);
						}
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
					//System.out.println("WORKER NAME IST: " + thisAgent.getAgentName());
					//System.out.println("WORKER NODE IST: " + thisAgent.getAgentNode());
					System.out.println("WORKER.toString() IST: " + thisAgent.toString());

					/**
					 * Send Position confirm with FAIL if the message is not for us
					 */
					if(!positionMessage.workerId.equals(thisAgent.getAgentId()) && position == null) {
						positionConfirm.state = Result.FAIL;
						sendMessage(brokerAddress, positionConfirm);
						return;
					}

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

					if (result == Result.FAIL) {
						System.out.println("FAIL - Message not confirmed");
						// TODO
						return;
					}

					if (!hasArrivedAtTarget) {
						// Agent hasn't arrived at target, so conduct the planned move
						doMove(workerConfirm.action);
						// Update position at broker
						PositionUpdate positionUpdate = new PositionUpdate();
						positionUpdate.workerId = thisAgent.getAgentId();
						positionUpdate.position = position;
						positionUpdate.gameId = gameId;
						sendMessage(broker, positionUpdate);

						System.out.println("POSITION " + position);

					}
				}

				if (payload instanceof OrderCompleted){
					// TODO if FAIL anders reagieren?
					priorityQueue.poll();
					System.out.println("SUCCESS " + handleOrder);
					handleOrder = priorityQueue.element();
					hasArrivedAtTarget = false;
				}

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

	/** calculate next move */
	private WorkerAction getNextMove(Position current, Position target) {
		// TODO
		if (current.equals(target)) {
			hasArrivedAtTarget = true;
			return WorkerAction.ORDER;
		}
		// [N, S, E, W]
		Position N = new Position(current.x, current.y - 1);
		Position S = new Position(current.x, current.y + 1);
		Position E = new Position(current.x + 1, current.y);
		Position W = new Position(current.x - 1, current.y);

		int[] distances = { target.distance(N), target.distance(S), target.distance(E), target.distance(W) };

		WorkerAction workerAction;
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

	/**
	 * This function is used to check whether an order should be accepted by the worker
	 * Especially it returns the index of the position where the worker should put the order
	 * If bestIndex == -1 then no feasible position to insert the order is found and the worker
	 * should not accept the order since it is not able to finish all orders within the deadline
	 */
	public int checkOrder(Order order) {

		int bestIndex = -1;
		int minDuration = Integer.MAX_VALUE;

		for (int index = 0; index <= orders.size(); index++) {
			ArrayList<Order> temporaryOrders = orders;
			temporaryOrders.add(index, order);

			int currentDuration = calculateDuration(temporaryOrders);
			if(currentDuration == -1)
				continue;
			if(currentDuration < minDuration) {
				minDuration = currentDuration;
				bestIndex = index;
			}

		}

		return bestIndex;
	}

	private int calculateDuration(ArrayList<Order> orders) {
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

}