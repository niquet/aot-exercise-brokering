package de.dailab.jiactng.aot.gridworld.client;

import de.dailab.jiactng.agentcore.AbstractAgentBean;
import de.dailab.jiactng.agentcore.Agent;
import de.dailab.jiactng.agentcore.action.Action;
import de.dailab.jiactng.agentcore.comm.ICommunicationAddress;
import de.dailab.jiactng.agentcore.comm.ICommunicationBean;
import de.dailab.jiactng.agentcore.comm.message.JiacMessage;
import de.dailab.jiactng.agentcore.knowledge.IFact;
import de.dailab.jiactng.agentcore.ontology.AgentDescription;
import de.dailab.jiactng.agentcore.ontology.IAgentDescription;
import de.dailab.jiactng.aot.gridworld.messages.*;
import de.dailab.jiactng.aot.gridworld.model.*;


import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * You can use this stub as a starting point for your broker bean or start from scratch.
 */

public class BrokerBean extends AbstractAgentBean {

	/*
	 * it's probably a good idea to keep track of a few variables here, like
	 * the communication address of the server and your workers, the current game ID,
	 * your active orders, etc.
	 */
	// TODO
	private Boolean hasGameStarted = false;
	private ICommunicationAddress server = null;
	private List<IAgentDescription> agentDescriptions = null;
	private GridworldGame gridworldGame = null;

	private Map<String, Order> orderMap = new HashMap<>();
	// Worker mapped to position
	// ! -- Get workerId String from workerIdReverseAId to use this map
	private Map<String, Position> positionMap = new HashMap<>();

	/* A Map to map worker Ids to IAgentDescription to get an address with a worker Id
	 this Map is not registered by the server and but only a mapping for us
	 */
	private Map<String, IAgentDescription> workerIdMap = new HashMap<>();
	// Map AgentID to wroker ID
	private Map<String, String> workerIdReverseAId = new HashMap<>();

	// TODO
	private ArrayList<WorkerEstimate> workerEstimates = new ArrayList<>();

	@Override
	public void doStart() throws Exception {
		/*
		 * this will be called once when the agent starts and can be used for initialization work
		 * note that when this method is executed, (a) it is not guaranteed that all the other
		 * agents are already started and/or their actions are known, and (b) the agent's execution
		 * has not yet started, so do not wait for any actions to be completed in this method (you
		 * can invoke actions, though, if they are already known to the agent)
		 *
		 * if you want to use a SpaceObserver to listen to messages, this is might be a good place
		 * to add it, but you could also check messages in execute() and only temporarily attach
		 * a SpaceObserver for specific purposes
		 */
		log.info("starting...");

	}



	@Override
	public void execute() {
		/*
		 * this is executed periodically by the agent; use the executeInterval in the XML file
		 * to configure how often exactly
		 *
		 * this is probably where the bulk of your logic will go; if you are not using a listener
		 * to receive messages (see WorkerBean.java), you can use memory.readAll or memory.removeAll to get messages
		 * from the memory, where they are stored when received; make sure to remove messages from
		 * memory to not create a memory leak
		 *
		 * you may find the methods thisAgent::getAgentNode and thisAgent::searchAllAgents useful
		 * for finding your fellow Worker agents. Examples are included in here
		 */
		log.info("running...");


		/* example for finding the server agent */
		IAgentDescription serverAgent = thisAgent.searchAgent(new AgentDescription(null, "ServerAgent", null, null, null, null));
		if (serverAgent != null) {
			this.server = serverAgent.getMessageBoxAddress();

			// TODO
			if (!hasGameStarted) {
				StartGameMessage startGameMessage = new StartGameMessage();
				startGameMessage.brokerId = thisAgent.getAgentId();
				startGameMessage.gridFile = "/grids/04_1.grid";
				// Send StartGameMessage(BrokerID)
				sendMessage(server, startGameMessage);

				this.hasGameStarted = true;
			}

		} else {
			System.out.println("SERVER NOT FOUND!");
		}

		/* example of handling incoming messages without listener */
		for (JiacMessage message : memory.removeAll(new JiacMessage())) {
			Object payload = message.getPayload();

			if (payload instanceof StartGameResponse) {
				/* do something */

				// TODO
				// this.hasGameStarted = true;
				StartGameResponse startGameResponse = (StartGameResponse) message.getPayload();

				int maxNumberOfAgents = startGameResponse.initialWorkers.size();
				this.agentDescriptions = getMyWorkerAgents(maxNumberOfAgents);

				/**
				 *
				 * DEBUGGING
				 *
				 */
				System.out.println("SERVER SENDING " + startGameResponse.toString());

				// TODO handle movements and obstacles
				this.gridworldGame = new GridworldGame();
				for (Position position : startGameResponse.obstacles) {
					this.gridworldGame.obstacles.add(position);
				}

				// Send each Agent their current position
				PositionMessage positionMessage = new PositionMessage();

				// TODO nicht mehr worker verwenden als zur Verfügung stehen

				/**
				 * Initialize the workerIdMap to get the agentDescription and especially the
				 * MailBoxAdress of the workerAgent which we associated with a specific worker
				 *

				for (Worker worker: startGameResponse.initialWorkers) {
					workerIdMap.put(worker.id, this.agentDescriptions.get(startGameResponse.initialWorkers.indexOf(worker)));
					workerIdReverseAId.put(this.agentDescriptions.get(startGameResponse.initialWorkers.indexOf(worker)).getAid(), worker.id);
				} */

				/**
				 * Send the Position messages to each Agent for a specific worker
				 * PositionMessages are sent to inform the worker where it is located
				 * additionally put the position of the worker in the positionMap
				 */
				for (Worker worker: startGameResponse.initialWorkers) {
					positionMap.put(worker.id, worker.position);

					workerIdMap.put(worker.id, this.agentDescriptions.get(startGameResponse.initialWorkers.indexOf(worker)));
					workerIdReverseAId.put(this.agentDescriptions.get(startGameResponse.initialWorkers.indexOf(worker)).getAid(), worker.id);

					IAgentDescription agentDescription = workerIdMap.get(worker.id);
					ICommunicationAddress workerAddress = agentDescription.getMessageBoxAddress();


					positionMessage.workerId = agentDescription.getAid();
					positionMessage.gameId = startGameResponse.gameId;
					positionMessage.position = worker.position;
					positionMessage.workerIdForServer = worker.id;
					//System.out.println("ADRESS IS " + workerAddress);

					sendMessage(workerAddress, positionMessage);
					//break;
				}

				// TODO maxturns fehlt

			}

			if (payload instanceof PositionConfirm) {
				PositionConfirm positionConfirm = (PositionConfirm) message.getPayload();
				if(positionConfirm.state == Result.FAIL) {
					String workerId = workerIdReverseAId.get(positionConfirm.workerId);
					IAgentDescription agentDescription = workerIdMap.get(workerId);
					ICommunicationAddress workerAddress = agentDescription.getMessageBoxAddress();

					PositionMessage positionMessage = new PositionMessage();

					positionMessage.workerId = agentDescription.getAid();
					positionMessage.gameId = positionConfirm.gameId;
					positionMessage.position = positionMap.get(workerId);
					positionMessage.workerIdForServer = workerId;

					sendMessage(workerAddress, positionMessage);
				}


			}


			if (payload instanceof OrderMessage) {

				// TODO entscheide, ob wir die Order wirklich annehmen wollen / können
				// Take Order ?!
				OrderMessage orderMessage = (OrderMessage) message.getPayload();
				TakeOrderMessage takeOrderMessage = new TakeOrderMessage();
				takeOrderMessage.orderId = orderMessage.order.id;
				takeOrderMessage.brokerId = thisAgent.getAgentId();
				takeOrderMessage.gameId = orderMessage.gameId;
				sendMessage(server, takeOrderMessage);

				/**
				 *
				 * DEBUGGING
				 *
				 */
				System.out.println("SERVER SENDING " + orderMessage.toString());

				// Save order into orderMap
				Order order = ((OrderMessage) message.getPayload()).order;
				this.orderMap.put(order.id, order);

			}

			if (payload instanceof TakeOrderConfirm) {

				// TODO
				// Got Order ?!
				TakeOrderConfirm takeOrderConfirm = (TakeOrderConfirm) message.getPayload();
				Result result = takeOrderConfirm.state;

				/**
				 *
				 * DEBUGGING
				 *
				 */
				System.out.println("SERVER SENDING " + takeOrderConfirm.toString());

				if (result == Result.FAIL) {
					// Handle failed confirmation

					// Remove order from orderMap as it was rejected by the server
					this.orderMap.remove(takeOrderConfirm.orderId);
					continue;
				}

				// TODO send serverAddress
				// Assign order to Worker(Bean)
				// Send the order to the first agent
				AssignOrderMessage assignOrderMessage = new AssignOrderMessage();
				assignOrderMessage.order = this.orderMap.get(takeOrderConfirm.orderId);
				assignOrderMessage.gameId = takeOrderConfirm.gameId;
				assignOrderMessage.server = this.server;

				ICommunicationAddress workerAddress = decideOrderAssigment(assignOrderMessage.order);
				sendMessage(workerAddress, assignOrderMessage);

			}

			if (payload instanceof AssignOrderConfirm) {

				// TODO
				AssignOrderConfirm assignOrderConfirm = (AssignOrderConfirm) message.getPayload();
				Result result = assignOrderConfirm.state;

				if (result == Result.FAIL) {
					// Handle failed confirmation
					// TODO
					ICommunicationAddress alternativeWorkerAddress = getAlternativeWorkerAddress(((AssignOrderConfirm) message.getPayload()).workerId);
					reassignOrder(assignOrderConfirm, alternativeWorkerAddress);

					continue;
				}

				// TODO Inform other workers that this task is taken

			}

			if (payload instanceof OrderCompleted) {

				OrderCompleted orderCompleted = (OrderCompleted) message.getPayload();
				Result result = orderCompleted.state;

				if (result == Result.FAIL) {
					// TODO Handle failed order completion
					continue;
				}

				// TODO remove order from the worker specific order queues

			}

			if (payload instanceof PositionUpdate) {

				PositionUpdate positionUpdate = (PositionUpdate) message.getPayload();
				updateWorkerPosition(positionUpdate.position, positionUpdate.workerId);

			}

			if (payload instanceof EndGameMessage) {

				EndGameMessage endGameMessage = (EndGameMessage) message.getPayload();
				// TODO lernen lernen lernen lol

			}

		}
	}




	/*
	 * You can implement some functions and helper methods here.
	 */
	/** get a different workerAddress than the one passed as the argument */
	private void updateWorkerPosition(Position position, String workerAgentId) {

		String workerId = workerIdReverseAId.get(workerAgentId);
		positionMap.replace(workerId, position);

	}

	/** get a different workerAddress than the one passed as the argument */
	private ICommunicationAddress decideOrderAssigment(Order order) {

		ICommunicationAddress workerAddress = null;
		Position orderPosition = order.position;
		Position currentWorkerPosition = null;

		int minimum = Integer.MAX_VALUE;
		int currentDistance = 0;

		String[] workerAgentId = workerIdReverseAId.values().toArray(new String[0]);
		for (String currentWorkerId: workerAgentId) {

			currentWorkerPosition = positionMap.get(currentWorkerId);
			currentDistance = orderPosition.distance(currentWorkerPosition);
			if (orderPosition.distance(currentWorkerPosition) < minimum) {
				minimum = currentDistance;
				workerAddress = workerIdMap.get(currentWorkerId).getMessageBoxAddress();
			}

		}

		return workerAddress;

	}

	/** get a different workerAddress than the one passed as the argument */
	private ICommunicationAddress getAlternativeWorkerAddress(String workerId) {

		for(IAgentDescription agentDescription: this.agentDescriptions) {

			// TODO add datastructure to find most likely alternative for assigning an order to alternative worker
	 		String workerAgentId = workerIdReverseAId.get(workerId);

			if (agentDescription.getAid().equals(workerAgentId)) {
				continue;
			}

			ICommunicationAddress workerAddress = agentDescription.getMessageBoxAddress();
			return workerAddress;

		}

	}

	/** get a different workerAddress than the one passed as the argument */
	private void reassignOrder(ICommunicationAddress workerAddress, AssignOrderConfirm assignOrderConfirm) {

		// TODO
		AssignOrderMessage assignOrderMessage = new AssignOrderMessage();
		assignOrderMessage.order = this.orderMap.get(assignOrderConfirm.orderId);
		assignOrderMessage.gameId = assignOrderConfirm.gameId;
		assignOrderMessage.server = this.server;

		sendMessage(workerAddress, assignOrderMessage);

	}

	/** example function for using getAgentNode() and retrieving a list of all worker agents */
	private List<IAgentDescription> getMyWorkerAgents(int maxNum) {
		String nodeId = thisAgent.getAgentNode().getUUID();
		return thisAgent.searchAllAgents(new AgentDescription(null, null, null, null, null, nodeId)).stream()
				.filter(a -> a.getName().startsWith("WorkerAgent"))
				.limit(maxNum)
				.collect(Collectors.toList());
	}

	/** example function to send messages to other agents */
	private void sendMessage(ICommunicationAddress receiver, IFact payload) {
		Action sendAction = retrieveAction(ICommunicationBean.ACTION_SEND);
		JiacMessage message = new JiacMessage(payload);
		invoke(sendAction, new Serializable[] {message, receiver});
		System.out.println("BROKER SENDING " + payload);
	}

}
