package de.dailab.jiactng.aot.gridworld.client;

import de.dailab.jiactng.agentcore.AbstractAgentBean;
import de.dailab.jiactng.agentcore.action.Action;
import de.dailab.jiactng.agentcore.comm.ICommunicationAddress;
import de.dailab.jiactng.agentcore.comm.ICommunicationBean;
import de.dailab.jiactng.agentcore.comm.message.JiacMessage;
import de.dailab.jiactng.agentcore.knowledge.IFact;
import de.dailab.jiactng.agentcore.ontology.AgentDescription;
import de.dailab.jiactng.agentcore.ontology.IAgentDescription;
import de.dailab.jiactng.aot.gridworld.messages.*;
import de.dailab.jiactng.aot.gridworld.model.GridworldGame;
import de.dailab.jiactng.aot.gridworld.model.Order;
import de.dailab.jiactng.aot.gridworld.model.Position;
import de.dailab.jiactng.aot.gridworld.model.Worker;


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
				this.hasGameStarted = true;
				StartGameResponse startGameResponse = (StartGameResponse) message.getPayload();

				int maxNumberOfAgents = startGameResponse.initialWorkers.size();
				this.agentDescriptions = getMyWorkerAgents(maxNumberOfAgents);

				// TODO handle movements and obstacles
				this.gridworldGame = new GridworldGame();
				for (Position position : startGameResponse.obstacles) {
					this.gridworldGame.obstacles.add(position);
				}

				// TODO maxturns fehlt

			}

			if (payload instanceof OrderMessage) {

				// TODO entscheide, ob wir die Order wirklich annehmen wollen / können
				// Take Order ?!
				OrderMessage orderMessage = (OrderMessage) message.getPayload();
				TakeOrderMessage takeOrderMessage = new TakeOrderMessage();
				takeOrderMessage.orderId = orderMessage.order.id;
				takeOrderMessage.brokerId = thisAgent.getAgentId();
				takeOrderMessage.gameId = this.gridworldGame.gameId;
				sendMessage(server, takeOrderMessage);

				// Save order into orderMap
				Order order = (Order) message.getPayload();
				this.orderMap.put(order.id, order);

			}

			if (payload instanceof TakeOrderConfirm) {

				// TODO
				// Got Order ?!
				TakeOrderConfirm takeOrderMessage = (TakeOrderConfirm) message.getPayload();
				Result result = takeOrderMessage.state;

				if (result == Result.FAIL) {
					// Handle failed confirmation

					// Remove order from orderMap as it was rejected by the server
					this.orderMap.remove(takeOrderMessage.orderId);
					continue;
				}

				// TODO send serverAddress
				// Assign order to Worker(Bean)
				// Send the order to the first agent
				AssignOrderMessage assignOrderMessage = new AssignOrderMessage();
				assignOrderMessage.order = this.orderMap.get(takeOrderMessage.orderId);
				assignOrderMessage.server = this.server;

				for(IAgentDescription agentDescription: this.agentDescriptions) {
					ICommunicationAddress workerAddress = agentDescription.getMessageBoxAddress();
					sendMessage(workerAddress, assignOrderMessage);
					break;
				}

			}

			if (payload instanceof AssignOrderConfirm) {

				// TODO
				AssignOrderConfirm assignOrderConfirm = (AssignOrderConfirm) message.getPayload();
				Result result = assignOrderConfirm.state;

				if (result == Result.FAIL) {
					// Handle failed confirmation
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

				// TODO handle the reward

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
