package il.co.ilrd.multiprotocolserver;

/*
    Name: Jonathan
    Reviewer: Tali
    Exercise: Multi Protocol Server
*/

public class ServerMessage <T> implements Message<ServerSupportedProtocols, Message<?,?>> {

    private Message<?,?> message;
    private ServerSupportedProtocols key;
    public ServerMessage(ServerSupportedProtocols key,Message<?,?> message) {
        this.message = message;
        this.key = key;
    }
    public ServerSupportedProtocols getKey() {
        return key;
    }
    public Message<?, ?> getMessage() {
        return message;
    }
}