package javking.rest.websocket.stomp;

import org.json.JSONObject;

public class SocketMessage {
    private final String op;
    private final JSONObject d;

    public SocketMessage() {
        this(null, null);
    }

    public SocketMessage(String op, JSONObject d) {
        this.op = op;
        this.d = d;
    }

    public String getOperation() {
        return op;
    }

    public JSONObject getData() {
        return d;
    }
}
