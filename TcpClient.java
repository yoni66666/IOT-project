package il.co.ilrd.multiprotocolserver;

/*
    Name: Johnathan
    Reviewer:
    Exercise: Multi Protocol Server
*/

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import static java.lang.Thread.sleep;

class TCPClient {
    private static int port = 55551;
    private static final int BUFFER_SIZE = 1024;

    public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException {
        ServerMessage<?> serverMessage = new ServerMessage(ServerSupportedProtocols.PING_PONG,
                new PingPongMessage(PingPongMessageType.PING));
        ByteBuffer buffer;
        SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress("localhost",port));

        for (int i = 0; i < 10; ++i) {
            buffer = serialize(serverMessage);

            buffer.flip();
            socketChannel.write(buffer);
            buffer.clear();

            socketChannel.read(buffer);
            buffer.flip();

            ClientMessage<?> clientMessage = (ClientMessage<?>) deserialize(buffer);
            clientMessage.getMessage();

            buffer.clear();
            sleep(1000);
        }

        socketChannel.close();
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