package il.co.ilrd.multiprotocolserver;

/**
 * @autor yoni
 * @reviewer tali
 */

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;

public enum PingPongMessageType implements ProtocolMessageType {
    PING {
        @Override
        public void handleMessage(MultiProtocolServer.ReadableConnection connection) {
            ByteBuffer response = ByteBuffer.allocate(1024);

            try {
                /* create a client msg from Ping */
                PingPongMessage pingPongMessage = new PingPongMessage(PingPongMessageType.PONG);

                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                ObjectOutputStream outputStream = new ObjectOutputStream(byteArrayOutputStream);
                outputStream.writeObject(pingPongMessage);
                outputStream.flush();
                response.put(byteArrayOutputStream.toByteArray());
                response.flip();

            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            connection.sendMessage(response);
        }
    },
    PONG {
        @Override
        public void handleMessage(MultiProtocolServer.ReadableConnection connection) {
            ByteBuffer response = ByteBuffer.allocate(1024);

            try {
                /* create a client msg from Ping */
                PingPongMessage pingPongMessage = new PingPongMessage(PingPongMessageType.PING);

                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                ObjectOutputStream outputStream = new ObjectOutputStream(byteArrayOutputStream);
                outputStream.writeObject(pingPongMessage);
                outputStream.flush();
                response.put(byteArrayOutputStream.toByteArray());
                response.flip();
            } catch (IOException e) {throw new RuntimeException(e);}

            connection.sendMessage(response);
        }
    },
    ERROR{
        @Override
        public void handleMessage(MultiProtocolServer.ReadableConnection connection) {
            System.out.println("ERROR");
            ByteBuffer response = ByteBuffer.allocate(1024);

            PingPongMessage pingPongMessage = new PingPongMessage(ERROR);

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream outputStream = null;

            try {
                outputStream = new ObjectOutputStream(byteArrayOutputStream);
                outputStream.writeObject(pingPongMessage);
                outputStream.flush();
                response.put(byteArrayOutputStream.toByteArray());
                response.flip();

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            connection.sendMessage(response);
        }
    };


    public abstract void handleMessage(MultiProtocolServer.ReadableConnection connection);
}

