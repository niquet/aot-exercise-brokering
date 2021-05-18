package examples.pingpong;

import de.dailab.jiactng.agentcore.AbstractAgentBean;
import de.dailab.jiactng.agentcore.action.Action;
import de.dailab.jiactng.agentcore.comm.ICommunicationBean;
import de.dailab.jiactng.agentcore.comm.IMessageBoxAddress;
import de.dailab.jiactng.agentcore.comm.message.JiacMessage;
import de.dailab.jiactng.agentcore.ontology.AgentDescription;
import de.dailab.jiactng.agentcore.ontology.IActionDescription;
import de.dailab.jiactng.agentcore.ontology.IAgentDescription;

import java.io.Serializable;
import java.util.List;

public class PingBean extends AbstractAgentBean {

    private IActionDescription sendAction = null;

    @Override
    public void doStart() throws Exception {
        super.doStart();
        log.info("PingAgent - starting....");
        log.info("PingAgent - my ID: " + this.thisAgent.getAgentId());
        log.info("PingAgent - my Name: " + this.thisAgent.getAgentName());
        log.info("PingAgent - my Node: " + this.thisAgent.getAgentNode().getName());

        // Retrieve the send-action provided by CommunicationBean
        IActionDescription template = new Action(ICommunicationBean.ACTION_SEND);
        sendAction = memory.read(template);
        if(sendAction == null) {
            sendAction = thisAgent.searchAction(template);
        }
        // shorter: retrieveAction(ICommunicationBean.ACTION_SEND);
        // If no send action is available, check your agent configuration
        // CommunicationBean is needed
        if(sendAction == null) {
            throw new RuntimeException("Send action not found.");
        }
    }

    @Override
    public void execute() {

        // Retrieve all Pong Agents
        List<IAgentDescription> agentDescriptions = thisAgent.searchAllAgents(new AgentDescription());
        for (IAgentDescription agent: agentDescriptions) {
            if (agent.getName().equals("PongAgent")) {

                // Send a "Ping" to each of the PongAgents
                JiacMessage message = new JiacMessage(new Ping("ping"));
                IMessageBoxAddress receiver = agent.getMessageBoxAddress();

                // Invoke sendAction
                log.info("Ping Agent - sending Ping to: " + receiver);
                invoke(sendAction, new Serializable[] { message, receiver });

            }
        }

    }

}
