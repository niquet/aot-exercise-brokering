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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;


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


	/* FRAGE: Wieso doppelt verschachtelte Map für die Orders? */
	private Map<String, Map<Order, ICommunicationAddress>> currentOrders = new HashMap<>();
	private LinkedList<String> orderQueue = new LinkedList<>();
	/* Hilfsvariable um den fokus erstmal auf anderes zu setzen */
	private Order myOrder = null;
	private Boolean isHandlingOrder = false;
	private Boolean hasArrivedAtTarget = false;

	private Position position = null;


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

		if(!isHandlingOrder) {

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

					ICommunicationAddress broker = message.getSender();

					AssignOrderMessage assignOrderMessage = (AssignOrderMessage) message.getPayload();
					Order order = assignOrderMessage.order;
					ICommunicationAddress server = assignOrderMessage.server;

					Map<Order, ICommunicationAddress> orderWithServer = new HashMap<>();
					orderWithServer.put(order, server);
					currentOrders.put(order.id, orderWithServer);

					// TODO do something / evaluate

					AssignOrderConfirm assignOrderConfirm = new AssignOrderConfirm();
					assignOrderConfirm.orderId = order.id;
					assignOrderConfirm.gameId = assignOrderMessage.gameId;

					if (true) {
						assignOrderConfirm.state = Result.SUCCESS;
					} else {
						assignOrderConfirm.state = Result.FAIL;
					}

					myOrder = order;
					isHandlingOrder = true;
					sendMessage(broker, assignOrderConfirm);

					// Bisher nur zum testen (erste Bewegung)
					WorkerMessage move = new WorkerMessage();
					move.action = getNextMove(position, myOrder.position);
					move.gameId = assignOrderConfirm.gameId;
					move.workerId = thisAgent.getAgentId();

					sendMessage(server, move);

				}

				if (payload instanceof PositionMessage) {
					/** Order to assign to the agent */

					PositionMessage positionMessage = (PositionMessage) message.getPayload();


					ICommunicationAddress broker = message.getSender();
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
						sendMessage(broker, positionConfirm);
						return;
					}

					/**
					 * Only set position if it is not for us
					 */
					if(position == null) {
						position = positionMessage.position;
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

						// TODO

					}

					isHandlingOrder = true;

					if (!hasArrivedAtTarget) {
						// Agent hasn't arrived at target, so next move should be generated and send to the server
						WorkerMessage move = new WorkerMessage();
						move.action = getNextMove(position, myOrder.position);
						move.workerId = thisAgent.getAgentId();
						move.gameId = workerConfirm.gameId;
						sendMessage(message.getReplyToAddress(), move);
					}

					// Agent has arrived at target
					// TODO
					else isHandlingOrder = false;

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
		// [N, S, E, W]
		Position N = new Position(current.x, current.y + 1);
		Position S = new Position(current.x, current.y - 1);
		Position E = new Position(current.x + 1, current.y);
		Position W = new Position(current.x - 1, current.y);

		int[] distances = { current.distance(N), current.distance(S), current.distance(E), current.distance(W) };

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
			default:
				workerAction = WorkerAction.NORTH;
				break;
		}

		return workerAction;

	}

}