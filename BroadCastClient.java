package il.co.ilrd.multiprotocolserver;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import static java.lang.Thread.sleep;

public class BroadCastClient {
    private static DatagramSocket channel;
    private static final int port = 55553;

    public static void main(String[] args) throws IOException, InterruptedException, ClassNotFoundException {

        ServerMessage<?> serverMessage = new ServerMessage<>(ServerSupportedProtocols.PING_PONG,
                new PingPongMessage(PingPongMessageType.PING));
        InetAddress address = InetAddress.getByName("255.255.255.255");
        channel = new DatagramSocket();
        channel.setBroadcast(true);
        ByteBuffer buffer;

        for(int i = 0; i < 10; ++i ) {

            buffer = TCPClient.serialize(serverMessage);
            buffer.flip();

            DatagramPacket packet = new DatagramPacket(buffer.array(), buffer.array().length, address, port);
            channel.send(packet);
            buffer.clear();

            packet = new DatagramPacket(buffer.array(), buffer.array().length);
            channel.receive(packet);
            ClientMessage<?> clientMessage = (ClientMessage<?>) TCPClient.deserialize(buffer);
            clientMessage.getMessage();

            buffer.clear();
            sleep(1000);
        }
    }
}
