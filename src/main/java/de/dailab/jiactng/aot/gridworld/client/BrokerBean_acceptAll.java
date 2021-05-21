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

public class BrokerBean_acceptAll extends AbstractAgentBean {

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

	//OrderID mapped to Order, for all taken orders
	private final Map<String, Order> orderMap = new HashMap<>();
	// Worker position
	// ! -- Get workerId String from workerIdReverseAId to use this map
	private final Map<String, Position> positionMap = new HashMap<>();

	//A Map to map worker Ids to IAgentDescription to get an address with a worker Id. This Map is not registered by the server and only a mapping for us
	private Map<String, IAgentDescription> workerIdMap = new HashMap<>();
	// Map AgentID to worker ID
	private Map<String, String> workerIdReverseAID = new HashMap<>();
	//Communication Addresses of all active workers
	private ArrayList<ICommunicationAddress> activeWorkers = new ArrayList<>();
	// All incoming Ordermessages TODO besserer Kommentar
	private ArrayList<String> orderMessages = new ArrayList<>();

	// TODO
	private ArrayList<WorkerEstimate> workerEstimates = new ArrayList<>();
	private int reward;
	private int gameId;

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
			//send startgamemessage, if game has not started
			if (!hasGameStarted) {
				//--In Methode auslagern
				StartGameMessage startGameMessage = new StartGameMessage();
				startGameMessage.brokerId = thisAgent.getAgentId();
				startGameMessage.gridFile = "/grids/24_02.grid";
				// Send StartGameMessage(BrokerID)
				sendMessage(server, startGameMessage);
				//bis hier --
				reward = 0;
				this.hasGameStarted = true;
			}

		} else {
			System.out.println("SERVER NOT FOUND!");
		}


		/* example of handling incoming messages without listener */
		for (JiacMessage message : memory.removeAll(new JiacMessage())) {
			Object payload = message.getPayload();

			// if server sends gameStartResponse
			if (payload instanceof StartGameResponse) {
				// TODO
				StartGameResponse startGameResponse = (StartGameResponse) message.getPayload();

				int maxNumberOfAgents = startGameResponse.initialWorkers.size();
				// save inital workers globally
				this.agentDescriptions = getMyWorkerAgents(maxNumberOfAgents);
				// save gameId for later reference globally
				this.gameId = startGameResponse.gameId;

				/**
				 * DEBUGGING
				 */
				System.out.println("BROKER RECEIVING " + startGameResponse.toString());

				// TODO handle movements and obstacles
				// init new game
				this.gridworldGame = new GridworldGame();
				// save Obstacles globally
				this.gridworldGame.obstacles.addAll(startGameResponse.obstacles);

				// new Position message to send each worker its current position
				PositionMessage positionMessage = new PositionMessage();

				// TODO nicht mehr worker verwenden als zur Verfügung stehen

				//TODO brauchen wir die grünen Kommentare hier noch?
				/**
				 * Initialize the workerIdMap to get the agentDescription and the
				 * MailBoxAdress of the workerAgent which we associated with a specific worker
				 *

				for (Worker worker: startGameResponse.initialWorkers) {
					workerIdMap.put(worker.id, this.agentDescriptions.get(startGameResponse.initialWorkers.indexOf(worker)));
					workerIdReverseAId.put(this.agentDescriptions.get(startGameResponse.initialWorkers.indexOf(worker)).getAid(), worker.id);
				} */

				/**
				 * Send the Position messages to each Agent,
				 * for each worker a PositionMessage is sent to inform the worker where it is located,
				 * additionally put the position of the worker in the positionMap
				 */
				// for all workers (nur die, die wir am anfang haben, oder auch später hinzukommende?)
				for (Worker worker: startGameResponse.initialWorkers) {
					// save worker Position into global positionMap
					this.positionMap.put(worker.id, worker.position);
					// Map WorkerIDs to IAgentDescriptions
					this.workerIdMap.put(worker.id, this.agentDescriptions.get(startGameResponse.initialWorkers.indexOf(worker)));
					// Map AID (a-04523720, ...) to workerIds (w1, w2, ...)
					this.workerIdReverseAID.put(this.agentDescriptions.get(startGameResponse.initialWorkers.indexOf(worker)).getAid(), worker.id);

					// get worker comm. Address for the positionMessage from agentDescription
					// --in Methode auslagern
					IAgentDescription agentDescription = this.workerIdMap.get(worker.id);
					ICommunicationAddress workerAddress = agentDescription.getMessageBoxAddress();

					// fill in the Position message
					positionMessage.workerId = agentDescription.getAid();
					positionMessage.gameId = startGameResponse.gameId;
					positionMessage.position = worker.position;
					positionMessage.workerIdForServer = worker.id;
					//System.out.println("ADRESS IS " + workerAddress);
					// send positionMessage to worker
					sendMessage(workerAddress, positionMessage);
					// bis hier--
				}
				// TODO maxturns fehlt
			}

			// if Broker receives a positionMessage,
			if (payload instanceof PositionConfirm) {
				PositionConfirm positionConfirm = (PositionConfirm) message.getPayload();
				//and it was not for him,
				if(positionConfirm.state == Result.FAIL) {
					// get the complaining workers id and Agentdescription and from there its comm. Address
					String workerId = this.workerIdReverseAID.get(positionConfirm.workerId);
					IAgentDescription agentDescription = workerIdMap.get(workerId);
					ICommunicationAddress workerAddress = agentDescription.getMessageBoxAddress();

					// create,
					PositionMessage positionMessage = new PositionMessage();
					// fill in
					positionMessage.workerId = agentDescription.getAid();
					positionMessage.gameId = positionConfirm.gameId;
					positionMessage.position = positionMap.get(workerId);
					positionMessage.workerIdForServer = workerId;
					// and send new PositionMessage to the worker
					sendMessage(workerAddress, positionMessage);
				} else {
					// if positionMessage was send to and received by right worker
					// add worker to active workers
					this.activeWorkers.add(message.getSender());
					// for each unassigned order
					for (String orderId: this.orderMessages) {
						// find closest worker to order
						ICommunicationAddress workerAddress = decideOrderAssigment(this.orderMap.get(orderId));
						// if worker who sent positionConfirm(success) is closest
						if(workerAddress.equals(message.getSender())){
							// assign order to him
							AssignOrderMessage assignOrderMessage = new AssignOrderMessage();
							assignOrderMessage.order = this.orderMap.get(orderId);
							assignOrderMessage.gameId = gameId;
							assignOrderMessage.server = this.server;
							sendMessage(workerAddress, assignOrderMessage);
						}
					}
				}
			}

			// if Broker receives a new Order
			if (payload instanceof OrderMessage) {

				// accept it with takeOrderMessage
				OrderMessage orderMessage = (OrderMessage) message.getPayload();
				TakeOrderMessage takeOrderMessage = new TakeOrderMessage();
				takeOrderMessage.orderId = orderMessage.order.id;
				takeOrderMessage.brokerId = thisAgent.getAgentId();
				takeOrderMessage.gameId = orderMessage.gameId;
				sendMessage(server, takeOrderMessage);

				/**
				 * DEBUGGING
				 */
				System.out.println("BROKER RECEIVING " + orderMessage.toString());

				// Save order into global orderMap
				Order order = ((OrderMessage) message.getPayload()).order;
				this.orderMap.put(order.id, order);

			}

			// if server responds to takeOrderMessage
			if (payload instanceof TakeOrderConfirm) {

				// TODO
				// get confirmation state (fail or success)
				TakeOrderConfirm takeOrderConfirm = (TakeOrderConfirm) message.getPayload();
				Result result = takeOrderConfirm.state;

				/**
				 * DEBUGGING
				 */
				System.out.println("BROKER RECEIVING " + takeOrderConfirm.toString());

				// if order could not be taken
				if (result == Result.FAIL) {
					// Remove order from orderMap as it was rejected by the server
					this.orderMap.remove(takeOrderConfirm.orderId);
					continue;
				}


				// If server accepted the broker taking the order (only reachable if result is SUCCESS)
				// put together assignOrderMessage for order
				AssignOrderMessage assignOrderMessage = new AssignOrderMessage();
				assignOrderMessage.order = this.orderMap.get(takeOrderConfirm.orderId);
				assignOrderMessage.gameId = takeOrderConfirm.gameId;
				assignOrderMessage.server = this.server;
				// decide which worker gets order
				ICommunicationAddress workerAddress = decideOrderAssigment(assignOrderMessage.order);
				// if determined worker is active try to assign order to it, else put order into global accepted order List (orderMessages)
				if(activeWorkers.contains(workerAddress)){
					sendMessage(workerAddress, assignOrderMessage);
				} else {
					this.orderMessages.add(takeOrderConfirm.orderId);
				}

			}

			// if worker received assignOrder and sent its response
			if (payload instanceof AssignOrderConfirm) {
				// TODO
				AssignOrderConfirm assignOrderConfirm = (AssignOrderConfirm) message.getPayload();
				Result result = assignOrderConfirm.state;

				//if worker refused order
				if (result == Result.FAIL) {
					// TODO
					//give order to different worker
					ICommunicationAddress alternativeWorkerAddress = getAlternativeWorkerAddress(((AssignOrderConfirm) message.getPayload()).workerId);
					reassignOrder(alternativeWorkerAddress, assignOrderConfirm);
					continue;
				}
				// if order was assigned successfully remove order from brokers order List
				this.orderMessages.remove(assignOrderConfirm.orderId);
				// TODO Inform other workers that this task is taken - notwendig??

			}
			// if worker completed Order
			if (payload instanceof OrderCompleted) {

				// add reward
				OrderCompleted orderCompleted = (OrderCompleted) message.getPayload();
				Result result = orderCompleted.state;
				reward += orderCompleted.reward;
				// TODO remove order from the worker specific order queues

			}

			// if worker changed position
			if (payload instanceof PositionUpdate) {
				// update worker position in position map
				PositionUpdate positionUpdate = (PositionUpdate) message.getPayload();
				updateWorkerPosition(positionUpdate.position, positionUpdate.workerId);

			}

			// if game ends
			if (payload instanceof EndGameMessage) {
				// TODO lernen
				// GAME OVER
				EndGameMessage endGameMessage = (EndGameMessage) message.getPayload();
				System.out.println("Reward: " + endGameMessage.totalReward);
			}

		}
	}




	/*
	 * You can implement some functions and helper methods here.
	 */

	/**
	 * get a different workerAddress than the one passed as the argument
	 */
	private void updateWorkerPosition(Position position, String workerAgentId) {

		String workerId = workerIdReverseAID.get(workerAgentId);
		positionMap.replace(workerId, position);

	}

	/**
	 * determine worker with shortest distance to order
	 */
	private ICommunicationAddress decideOrderAssigment(Order order) {

		ICommunicationAddress workerAddress = null;
		Position orderPosition = order.position;
		Position currentWorkerPosition = null;

		int minimum = Integer.MAX_VALUE;
		int currentDistance = 0;

		String[] workerAgentId = this.workerIdReverseAID.values().toArray(new String[0]);
		for (String currentWorkerId: workerAgentId) {

			currentWorkerPosition = this.positionMap.get(currentWorkerId);
			currentDistance = orderPosition.distance(currentWorkerPosition);
			if (orderPosition.distance(currentWorkerPosition) < minimum) {
				minimum = currentDistance;
				workerAddress = workerIdMap.get(currentWorkerId).getMessageBoxAddress();
			}

		}

		return workerAddress;

	}

	/**
	 * get a different workerAddress than the one passed as the argument
	 */
	private ICommunicationAddress getAlternativeWorkerAddress(String workerId) {
		ICommunicationAddress workerAddress = null;
		for(IAgentDescription agentDescription: this.agentDescriptions) {

			// TODO add datastructure to find most likely alternative for assigning an order to alternative worker
	 		String workerAgentId = workerIdReverseAID.get(workerId);

			if (agentDescription.getAid().equals(workerAgentId)) {
				continue;
			}

			 workerAddress = agentDescription.getMessageBoxAddress();
			break;
		}

		return workerAddress;
	}

	/**
	 * sends assignOrderMessage to workerAdress
	 * TODO -> können wir evtl. zu assignorder umbenennen und direkt für alle assign order messages nutzen
	*/
	private void reassignOrder(ICommunicationAddress workerAddress, AssignOrderConfirm assignOrderConfirm) {

		// TODO
		AssignOrderMessage assignOrderMessage = new AssignOrderMessage();
		assignOrderMessage.order = this.orderMap.get(assignOrderConfirm.orderId);
		assignOrderMessage.gameId = assignOrderConfirm.gameId;
		assignOrderMessage.server = this.server;

		sendMessage(workerAddress, assignOrderMessage);

	}

	/**
	 * example function for using getAgentNode() and retrieving a list of all worker agents
	 */
	private List<IAgentDescription> getMyWorkerAgents(int maxNum) {
		String nodeId = thisAgent.getAgentNode().getUUID();
		return thisAgent.searchAllAgents(new AgentDescription(null, null, null, null, null, nodeId)).stream()
				.filter(a -> a.getName().startsWith("WorkerAgent"))
				.limit(maxNum)
				.collect(Collectors.toList());
	}

	/**
	 * example function to send messages to other agents
	 */
	private void sendMessage(ICommunicationAddress receiver, IFact payload) {
		Action sendAction = retrieveAction(ICommunicationBean.ACTION_SEND);
		JiacMessage message = new JiacMessage(payload);
		invoke(sendAction, new Serializable[] {message, receiver});
		System.out.println("BROKER SENDING " + payload);
	}

}
