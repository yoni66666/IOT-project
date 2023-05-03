package il.co.ilrd.multiprotocolserver;

public interface ClientMessage <T> extends Message<ProtocolMessageType, T> {
    public ProtocolMessageType getKey();
    public T getMessage();
}
