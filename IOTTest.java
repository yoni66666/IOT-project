package il.co.ilrd.multiprotocolserver;

/**
 Name: Tali Truneh
 Reviewer: Eliraz
 Exercise: IOT
 */


import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.text.ParseException;

public class IOTTest {
    private static int port = 55551;
    private static final int BUFFER_SIZE = 10240;
    private static SocketChannel  socketChannel;

    static {
        try {
            socketChannel = SocketChannel.open(new InetSocketAddress("10.1.0.62",port));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws IOException, ParseException,
            ClassNotFoundException, InterruptedException, org.json.simple.parser.ParseException {
        sendMsg(parse("registerCompany.json"));
        Thread.sleep(1000);
        sendMsg(parse("registerProduct.json"));
        Thread.sleep(1000);
        sendMsg(parse("registerIOT.json"));
        Thread.sleep(1000);
        sendMsg(parse("updateIOT.json"));
        Thread.sleep(1000);
    }

    private static JSONObject parse(String fileName) throws IOException, org.json.simple.parser.ParseException {
        JSONParser parser = new JSONParser();

        Object obj = (JSONObject) parser.parse(new FileReader("/home/jonathan/git/jonathan.shapiro/fs/projects/src/il/co/ilrd/multiprotocolserver/registerProduct.json"));
        //Object obj = parser.parse(new FileReader("/home/tali/git/fs/projects/src/il/co/ilrd/multiprotocolserver/"+ fileName));
        JSONObject jsonObject =  (JSONObject) obj;
        return jsonObject;
    }
    private static void sendMsg(JSONObject jsonObject) throws IOException {
        IOTMessage iotMessage = new IOTMessage(jsonObject);
        ServerMessage<?> serverMessage = new ServerMessage(ServerSupportedProtocols.IOT, iotMessage);
        serialize(serverMessage, socketChannel);
    }

    public static ByteBuffer serialize(ServerMessage<?> message,SocketChannel socketChannel) throws IOException{
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);

        ByteArrayOutputStream byteArr = new ByteArrayOutputStream();
        ObjectOutputStream object = new ObjectOutputStream(byteArr);
        object.writeObject(message);
        object.flush();
        buffer.put(byteArr.toByteArray());

        buffer.flip();
        socketChannel.write(buffer);
        buffer.rewind();
        buffer.clear();
        buffer.flip();

        object.close();
        byteArr.close();

        return buffer;
    }
    

    public static Object deserialize(ByteBuffer buffer) throws IOException,ClassNotFoundException{
        ByteArrayInputStream byteArr = new ByteArrayInputStream(buffer.array());
        ObjectInputStream object = new ObjectInputStream(byteArr);

        object.close();

        return object.readObject();
    }

}
