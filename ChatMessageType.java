
package il.co.ilrd.multiprotocolserver;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
enum ChatMessageType implements ProtocolMessageType{
    REGISTER {
        @Override
        public void handleClientMessage(MultiProtocolServer.ReadableConnection connection, String message, MultiProtocolServer.Protocol protocol) {
            MultiProtocolServer.ChatServer chatProtocol = (MultiProtocolServer.ChatServer) protocol;
            ChatMessage responseMessage;

            try {
                if (chatProtocol.getClientsMap().containsKey(connection)) {
                    responseMessage = new ChatMessage(ERROR,"Connection Already Registered");
                } else if (chatProtocol.getClientsMap().containsValue(message)) {
                    responseMessage = new ChatMessage(ERROR,"Nickname exists");
                } else {
                    chatProtocol.getClientsMap().put(connection, message);
                    responseMessage = new ChatMessage(BROADCAST,"New Registration: " + message);
                }
                connection.sendMessage(MultiProtocolServer.serializeChatMsg(responseMessage, connection));
            } catch (IOException e) {throw new RuntimeException(e);}

        }
    },
    UNREGISTER {
        @Override
        public void handleClientMessage(MultiProtocolServer.ReadableConnection connection,
                                        String message,
                                        MultiProtocolServer.Protocol protocol) {
            MultiProtocolServer.ChatServer chatProtocol = (MultiProtocolServer.ChatServer) protocol;
            ChatMessage responseMessage;
            try {
                if (chatProtocol.getClientsMap().containsKey(connection)){
                    chatProtocol.getClientsMap().remove(connection);
                    responseMessage = new ChatMessage(UNREGISTER, "Unregistered");
                }
                else {
                    responseMessage = new ChatMessage(ERROR, "Not registered");
                }
                connection.sendMessage(MultiProtocolServer.serializeChatMsg(responseMessage, connection));

            } catch (IOException e) {throw new RuntimeException(e);}
        }
    },
    BROADCAST {
        @Override
        public void handleClientMessage(MultiProtocolServer.ReadableConnection connection,
                                       String message,
                                        MultiProtocolServer.Protocol protocol) {
            ByteBuffer response = ByteBuffer.allocate(1024);
            MultiProtocolServer.ChatServer chatProtocol = (MultiProtocolServer.ChatServer) protocol;
            ChatMessage responseMessage;

            try {
                if (chatProtocol.getClientsMap().containsKey(connection)) {
                    responseMessage = new ChatMessage(BROADCAST,
                            chatProtocol.getClientsMap().get(connection) + " : " + message);
                }
                else {
                    responseMessage = new ChatMessage(ERROR, "Not registered for broadcast");
                }

                response = MultiProtocolServer.serializeChatMsg(responseMessage, connection);

            } catch (IOException e) {throw new RuntimeException(e);}

            if (responseMessage.getKey().equals(ERROR)){
                connection.sendMessage(response);
            }
            else {
                for(Map.Entry<MultiProtocolServer.ReadableConnection, String> entry: chatProtocol.getClientsMap().entrySet()){
                    entry.getKey().sendMessage(response);
                }
            }
        }
    },

    ERROR {
        @Override
        public void handleClientMessage(MultiProtocolServer.ReadableConnection connection,
                                  String message, MultiProtocolServer.Protocol protocol) {
        }
    };

    abstract public void handleClientMessage(MultiProtocolServer.ReadableConnection connection,
                                       String message, MultiProtocolServer.Protocol protocol);
}
