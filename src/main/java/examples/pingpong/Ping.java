package examples.pingpong;

import de.dailab.jiactng.agentcore.knowledge.IFact;

public class Ping implements IFact {

    private String message;

    public Ping(String pingMessage) {
        this.message = pingMessage;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
