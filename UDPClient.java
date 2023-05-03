package il.co.ilrd.multiprotocolserver;

/**
 * @autor yoni
 * @reviewer tali
 */


import java.io.IOException;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

import static java.lang.Thread.sleep;

public class UDPClient {
    private static DatagramChannel channel;
    private ByteBuffer buffer;
    private ServerMessage message;
    private InetSocketAddress address;

    public UDPClient(int port, ServerMessage<?> message) {
        this.message = message;
        this.address = new InetSocketAddress("localhost", port);
        //this.address = new InetSocketAddress("10.1.0.62", port);

        try {
            channel = DatagramChannel.open();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        buffer = ByteBuffer.allocate(1024);
    }

    public void action() {
        int x = 10;

        while (true) {
            try {
                buffer.clear();
                ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
                ObjectOutputStream objectOutput = new ObjectOutputStream(byteOutput);

                objectOutput.writeObject(message);
                objectOutput.flush();
                buffer.put(byteOutput.toByteArray());

                objectOutput.close();
                byteOutput.close();

                buffer.flip();
                channel.send(buffer, address);
                buffer.clear();

                channel.receive(buffer);

                ByteArrayInputStream byteInput = new ByteArrayInputStream(buffer.array());
                ObjectInputStream objectInput = new ObjectInputStream(byteInput);

                ClientMessage<?> obj = (ClientMessage<?>) objectInput.readObject();
                obj.getMessage();

                objectInput.close();
                buffer.clear();
                --x;
                sleep(500);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void main(String[] args) {
        ClientMessage<?> clientMessage = new PingPongMessage(PingPongMessageType.PING);
        ServerMessage<?> serverMessage = new ServerMessage<>(ServerSupportedProtocols.PING_PONG, clientMessage);
        UDPClient client = new UDPClient(55552, serverMessage);

        client.action();
    }
}
