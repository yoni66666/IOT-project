package il.co.ilrd.multiprotocolserver;

/**
 * @autor yoni
 * @reviewer tali
 */

public class PingPongMessage implements ClientMessage<Void> {
    private PingPongMessageType messageType;
    /* protocolMessageType is  obj  like PingPongMessageType or chat that implement interface protocolMessageType,
     * for use handelMessage that read client msg and create new Server msg to send */

    public PingPongMessage(PingPongMessageType messageType){
        this.messageType = messageType;
    }

    @Override
    public ProtocolMessageType getKey() {
        return messageType;
    }

    @Override
    public Void getMessage() {
        if (messageType.equals(PingPongMessageType.PING) ) {
            System.out.println("Ping");
        }
        else if (messageType.equals(PingPongMessageType.PONG)) {
            System.out.println("Pong");
        }
        else {
            System.out.println("ERROR");
        }
        return null;
    }
}
