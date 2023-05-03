package il.co.ilrd.multiprotocolserver;
/*
    Name: Johnathan
    Reviewer: Michael
    Exercise: Chat
*/
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import static java.lang.Thread.sleep;

public class TcpChatServer {
    private static int port = 55551;
    private static final int BUFFER_SIZE = 1024;

    public static void main(String[] args) throws IOException, InterruptedException, ClassNotFoundException {
        SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress("localhost",port));

        ServerMessage<?> registerMessage = new ServerMessage(ServerSupportedProtocols.CHAT,
                new ChatMessage(ChatMessageType.REGISTER, "yoni"));

        readWriteMessage(registerMessage,socketChannel);
        readWriteMessage(registerMessage,socketChannel);

        ServerMessage<?> broadcastMessage = new ServerMessage<>(ServerSupportedProtocols.CHAT,
                new ChatMessage(ChatMessageType.BROADCAST,"hello all"));
        readWriteMessage(broadcastMessage,socketChannel);

        ServerMessage<?> unregisterMessage = new ServerMessage(ServerSupportedProtocols.CHAT,
                new ChatMessage(ChatMessageType.UNREGISTER, "yoni"));
        readWriteMessage(unregisterMessage,socketChannel);

        socketChannel.close();
    }

    private static void readWriteMessage(ServerMessage<?> message,
                                         SocketChannel socketChannel) throws IOException, ClassNotFoundException, InterruptedException {
        ByteBuffer buffer;

        buffer = serialize(message);

        buffer.flip();
        socketChannel.write(buffer);
        buffer.clear();

        socketChannel.read(buffer);
        buffer.flip();

        ClientMessage<?> clientMessage = (ClientMessage<?>) deserialize(buffer);
        System.out.println(clientMessage.getMessage());

        buffer.clear();
        sleep(1000);

    }
    public static Object deserialize(ByteBuffer buffer) throws IOException,ClassNotFoundException{
        ByteArrayInputStream byteArr = new ByteArrayInputStream(buffer.array());
        ObjectInputStream object = new ObjectInputStream(byteArr);

        object.close();

        return object.readObject();
    }
    public static ByteBuffer serialize(ServerMessage<?> message) throws IOException{
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);

        ByteArrayOutputStream byteArr = new ByteArrayOutputStream();
        ObjectOutputStream object = new ObjectOutputStream(byteArr);
        object.writeObject(message);
        object.flush();
        buffer.put(byteArr.toByteArray());

        object.close();
        byteArr.close();

        return buffer;
    }
}

