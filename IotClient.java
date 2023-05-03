package il.co.ilrd.multiprotocolserver;

/*
    Name: Johnathan
    Reviewer: tali
    Exercise: Multi Protocol Server - IOT
*/

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.io.FileReader;

import org.json.simple.parser.JSONParser;

import org.json.simple.JSONObject;
import org.json.*;
import org.json.simple.parser.ParseException;


import static java.lang.Thread.sleep;

class IotClient {
    private static int port = 55551;
    private static final int BUFFER_SIZE = 10240;

    public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException {

        JSONParser parser = new JSONParser();
        JSONObject jsonMessageRegisterCompany = null;
        JSONObject jsonMessageRegisterCompanyByUser = null;

        JSONObject jsonMessageRegisterProduct = null;
        JSONObject jsonMessageRegisterIOT = null;
        JSONObject jsonMessageRegisterUPDATE = null;


        try {
            jsonMessageRegisterCompanyByUser = (JSONObject) parser.parse(new FileReader("/home/jonathan/git/jonathan.shapiro/fs/projects/src/il/co/ilrd/multiprotocolserver/registerCompanyByUser.json"));
            jsonMessageRegisterCompany = (JSONObject) parser.parse(new FileReader("/home/jonathan/git/jonathan.shapiro/fs/projects/src/il/co/ilrd/multiprotocolserver/registerCompany.json"));
            jsonMessageRegisterProduct = (JSONObject) parser.parse(new FileReader("/home/jonathan/git/jonathan.shapiro/fs/projects/src/il/co/ilrd/multiprotocolserver/registerProduct.json"));
            jsonMessageRegisterIOT = (JSONObject) parser.parse(new FileReader("/home/jonathan/git/jonathan.shapiro/fs/projects/src/il/co/ilrd/multiprotocolserver/registerIOT.json"));
            jsonMessageRegisterUPDATE = (JSONObject) parser.parse(new FileReader("/home/jonathan/git/jonathan.shapiro/fs/projects/src/il/co/ilrd/multiprotocolserver/updateIOT.json"));

            System.out.println(jsonMessageRegisterCompany);
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }

        IOTMessage iotMessage = new IOTMessage(jsonMessageRegisterUPDATE);

        ServerMessage<?> serverMessage = new ServerMessage(ServerSupportedProtocols.IOT, iotMessage);
        ByteBuffer buffer;
        SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress("localhost",port));
        //SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress("10.1.0.62",port));

        for (int i = 0; i < 1; ++i) {
            buffer = serialize(serverMessage);

            buffer.flip();
            socketChannel.write(buffer);
            buffer.clear();

            //sleep(200000);
/*
            socketChannel.read(buffer);
            buffer.flip();

            ClientMessage<?> clientMessage = (ClientMessage<?>) deserialize(buffer);
            clientMessage.getMessage();

            buffer.clear();
            sleep(1000);
 */
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