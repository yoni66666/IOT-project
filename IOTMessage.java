package il.co.ilrd.multiprotocolserver;

import org.json.simple.JSONObject;

public class IOTMessage implements ClientMessage<JSONObject> {
    private JSONObject jsonMessage;

    public IOTMessage(JSONObject jsonMessage) {
        this.jsonMessage = jsonMessage;
    }

    @Override
    public ProtocolMessageType getKey() {
        return null;
    }

    @Override
    public JSONObject getMessage() {
        return jsonMessage;
    }
}
