package il.co.ilrd.multiprotocolserver;
public class ChatMessage implements ClientMessage<String>{
    private ChatMessageType chatMessageType;
    private String message;

    public ChatMessage(ChatMessageType chatMessageType, String message) {
        this.chatMessageType = chatMessageType;
        this.message = message;
    }

    @Override
    public ProtocolMessageType getKey() {
        return chatMessageType;
    }

    @Override
    public String getMessage() {
        return message;
    }
}


