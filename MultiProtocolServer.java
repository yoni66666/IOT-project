package il.co.ilrd.multiprotocolserver;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import il.co.ilrd.threadpool.ThreadPool;
import org.bson.types.ObjectId;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.Callable;


public class MultiProtocolServer {
    private ConnectionManager connectionManager;
    private MessageHandler messageHandler;
    public MultiProtocolServer(){
        connectionManager = new ConnectionManager();
        messageHandler = new MessageHandler();
    }
    public void createConnection(ConnectionType connectionType, int port) {

        connectionManager.createConnection(connectionType, port);
    }
    public void runServer(){
        connectionManager.start();
    }

    public enum ConnectionType {
        TCP,
        UDP,
        UDP_BROADCAST
    }

    private class ConnectionManager {
        private  boolean isRunning = true;
        private Selector selector;
        private TcpServerConnection tcpServer;
        private UdpServerConnection udpServer;
        private ConnectionType connectionType;
        private HashMap<ConnectionType, ServerforConnectionType> tableServer = new HashMap<>();

        public ConnectionManager(){
            try {
                selector = Selector.open();
            }catch (IOException e){
                throw new RuntimeException(e);
            }
            udpServer = new UdpServerConnection();
            tcpServer = new TcpServerConnection();

            tableServer.put(ConnectionType.TCP, tcpServer);
            tableServer.put(ConnectionType.UDP, udpServer);
            tableServer.put(ConnectionType.UDP_BROADCAST, udpServer);
        }

        public void createConnection(ConnectionType connectionType, int port){
            this.connectionType = connectionType;

            try {
                tableServer.get(connectionType).setPort(port);
                tableServer.get(connectionType).bindChannel();
                tableServer.get(connectionType).setNonBlocking();
                tableServer.get(connectionType).register();
            }catch (Exception e){
                throw new RuntimeException(e);
            }
        }
        private void start(){
            Thread thread = new Thread(){
                public void run(){
                    System.out.println("is running");
                    while (isRunning) {
                        try {
                            if( selector.select(5000)== 0){
                                System.out.println("pass 5 second");
                                continue;
                            }
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }

                        Set<SelectionKey> keys = selector.selectedKeys();

                        for (Iterator iterator = keys.iterator(); iterator.hasNext();) {
                            SelectionKey key = (SelectionKey) iterator.next();
                            Connection connect = (Connection) key.attachment();
                            connect.handleConnection();
                            iterator.remove();
                        }
                    }
                }
            };
            thread.start();
        }

        private abstract class ServerforConnectionType {
            public abstract void setPort(int port);

            public  abstract void bindChannel() throws IOException;

            public  abstract void setNonBlocking() throws IOException;

            public abstract void register() throws ClosedChannelException;
        }
        private class TcpServerConnection extends ServerforConnectionType {
            private SocketChannel client;
            private SocketAddress tcpLocalport;
            private ServerSocketChannel tcpChannel;
            @Override
            public void setPort(int port) {
                tcpLocalport = new InetSocketAddress(port);
            }
            @Override
            public void bindChannel() throws IOException {
                tcpChannel = ServerSocketChannel.open();
                tcpChannel.socket().bind(tcpLocalport);
            }
            @Override
            public void setNonBlocking() throws IOException {
                tcpChannel.configureBlocking(false);
            }
            @Override
            public void register() throws ClosedChannelException {
                tcpChannel.register(selector, SelectionKey.OP_ACCEPT, new TCPAcceptable());
            }
        }

        private class UdpServerConnection extends ServerforConnectionType {
            private DatagramChannel udpChannel;
            private SocketAddress udpLocalport;

            int port;

            @Override
            public void setPort(int port) {
                this.port = port;
                udpLocalport = new InetSocketAddress(port);
            }
            @Override
            public void bindChannel() throws IOException {
                udpChannel = DatagramChannel.open();
                udpChannel.socket().bind(udpLocalport);
            }
            @Override
            public void setNonBlocking() throws IOException {
                udpChannel.configureBlocking(false);
            }
            @Override
            public void register() throws ClosedChannelException {
                if (connectionType == ConnectionType.UDP_BROADCAST){
                    try {
                        udpChannel.setOption(StandardSocketOptions.SO_BROADCAST,true);
                    }catch (IOException e){
                        throw new RuntimeException(e);
                    }
                }
                udpChannel.register(selector, SelectionKey.OP_READ, new UDPReadable(udpChannel));
            }
        }
    }
    private interface Connection {
        public void handleConnection();
    }

    public interface ReadableConnection extends Connection{
        public void handleConnection();
        public void sendMessage(ByteBuffer message);
    }

    private class UDPReadable implements ReadableConnection{
        private DatagramChannel channel;

        public UDPReadable(DatagramChannel channel){
            this.channel = channel;
        }
        private SocketAddress clientAddress;
        @Override
        public void handleConnection(){
            ByteBuffer receiveBuffer = ByteBuffer.allocate(10240);
            try {
                clientAddress = channel.receive(receiveBuffer);
                messageHandler.handleMessage(this, receiveBuffer);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void sendMessage(ByteBuffer message) {
            try {
                channel.send(message, clientAddress);
                message.clear();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private class TCPReadable implements ReadableConnection{

        @Override
        public void handleConnection() {

            ByteBuffer bufferToRead = ByteBuffer.allocate(10240);
            try {
                connectionManager.tcpServer.client.read(bufferToRead);
                if(bufferIsEmpty(bufferToRead)) {
                    connectionManager.tcpServer.client.close();
                }
                else {
                    messageHandler.handleMessage(this, bufferToRead);
                    bufferToRead.clear();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        private Boolean bufferIsEmpty( ByteBuffer bufferToRead){
            return bufferToRead.position() == 0;
        }

        @Override
        public void sendMessage(ByteBuffer message) {
            Objects.requireNonNull(message);
            try {
                connectionManager.tcpServer.client.write(message);
                message.clear();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private class TCPAcceptable implements Connection{

        @Override
        public void handleConnection() {
            try {
                connectionManager.tcpServer.client = connectionManager.tcpServer.tcpChannel.accept();
                connectionManager.tcpServer.client.configureBlocking(false);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            try {
                connectionManager.tcpServer.client.register(connectionManager.selector, SelectionKey.OP_READ, new TCPReadable());
            } catch (ClosedChannelException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private class MessageHandler{
        private HashMap<ServerSupportedProtocols, Protocol> protocolsMap;

        public MessageHandler(){
            protocolsMap = new HashMap<>();
            protocolsMap.put(ServerSupportedProtocols.PING_PONG, new PingPong());
            protocolsMap.put(ServerSupportedProtocols.CHAT, new ChatServer());
            protocolsMap.put(ServerSupportedProtocols.IOT, new Iot());
        }

        public void handleMessage(ReadableConnection connection, ByteBuffer buffer){

            ClientMessage<?> clientMessage;
            ByteArrayInputStream byteInput = new ByteArrayInputStream(buffer.array());
            ObjectInputStream objectInput;
            ServerMessage<?> serverMessage;

            try {
                /* deserialization */
                objectInput = new ObjectInputStream(byteInput);
                /* creating Server Message object */
                serverMessage = (ServerMessage<?>) objectInput.readObject();


                /* creating clientMessage from Server Message object */
                /* getMessage return clientMessage */
                clientMessage = (ClientMessage<?>) serverMessage.getMessage();

                /* getKey return protocolType */
                /* protocolsMap.get(serverMessage.getKey()) return obj of PingPong protocol */
                /* processMessage is method of Protocol*/
                protocolsMap.get(serverMessage.getKey()).processMessage(connection, clientMessage);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    protected interface Protocol{
        public void processMessage(ReadableConnection connection, ClientMessage<?> message);
    }
    protected class PingPong implements Protocol {

        @Override
        public void processMessage(ReadableConnection connection, ClientMessage<?> message) {
            try {
                ((PingPongMessageType) message.getKey()).handleMessage(connection);
            }catch (ClassCastException e){
                PingPongMessageType.ERROR.handleMessage(connection);
            }
        }
    }

    protected static class ChatServer implements Protocol{

        private Map<MultiProtocolServer.ReadableConnection, String> clients = new HashMap<>();

        public void processMessage(ReadableConnection connection, ClientMessage<?> message) {
            Objects.requireNonNull(connection);
            Objects.requireNonNull(message);
            ChatMessage clientMessage = (ChatMessage) message;

            try {
                ((ChatMessageType) clientMessage.getKey()).handleClientMessage(connection, clientMessage.getMessage(), this);
            } catch (ClassCastException e) {
                System.err.println("Invalid message type");
            }
        }
        public Map<MultiProtocolServer.ReadableConnection, String> getClientsMap(){
            return clients;
        }
    }

    protected class Iot implements Protocol {
        private IOTDatabaseManager databaseManager;
        private SingletonPlugAndPlayFactory factory;
        private ThreadPool threadPool;
        private HTTPConnection httpServerConnection;

        public Iot() {
            databaseManager = new IOTDatabaseManager();
            factory = SingletonPlugAndPlayFactory.getInstance();
            factory.play();
            threadPool = new ThreadPool();
            httpServerConnection = new HTTPConnection();
        }

        @Override
        public void processMessage(ReadableConnection connection, ClientMessage<?> message) {
            JSONObject jsonMsg;

            try {
                jsonMsg = ((IOTMessage)message).getMessage();
            } catch (ClassCastException ex) {
                throw new IllegalStateException();
            }

            String command = (String) jsonMsg.get("iot_command");

            Callable<Boolean> task = new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    System.out.println("task2 running from "+Thread.currentThread().getName());
                    return factory.create(command).runCommand(jsonMsg,databaseManager);
                }
            };
            try {
                threadPool.submit(task);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        private class HTTPConnection {
            private HttpServer httpServer;
            private static final int PORT = 55554;

            public HTTPConnection() {

                try {
                    httpServer = HttpServer.create(new InetSocketAddress(PORT), 0);
                    //create a Http connection - that represent a mapping from URL
                    httpServer.createContext("/", new HTTPIOT());
                    httpServer.setExecutor(null); // creates a default executor

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                start();
            }

            public void start() {
                httpServer.start();
                System.out.println("http server is running");
            }
        }

        private class HTTPIOT implements HttpHandler {
            private RecRouter root;
            private LinkedList<String> paramList;

            public HTTPIOT() {
                initializeRouters();
            }

            private JSONObject extractJson(HttpExchange httpExchange) throws IOException, ParseException {
                BufferedReader reader = new BufferedReader(new InputStreamReader(httpExchange.getRequestBody()));
                StringBuilder str = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    str.append(line);
                }
                return (JSONObject) new JSONParser().parse(str.toString());
            }

            @Override
            public void handle(HttpExchange httpExchange) throws IOException {
                String urlString = httpExchange.getRequestURI().toString();
                String requestMethod = httpExchange.getRequestMethod();
                JSONObject jsonObject = null;
                try {
                    jsonObject = extractJson(httpExchange);
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }

                HTTPCommand command = root.route( URIParser(urlString),requestMethod);
                httpExchange = setAttributes(httpExchange);
                command.execute(jsonObject, databaseManager,threadPool, httpExchange);
            }

            private HttpExchange setAttributes(HttpExchange httpExchange){
                for(String param : paramList){
                    httpExchange.setAttribute("company_id", param);
                }

                for(int i = 0; i<paramList.size(); ++i){
                    if(i ==0){
                        httpExchange.setAttribute("company_id", paramList.get(0));
                    }
                    else if (i ==1){
                        httpExchange.setAttribute("product_id", paramList.get(1));
                    }
                    else if (i== 2){
                        httpExchange.setAttribute("iotDevice_id", paramList.get(2));
                    }
                    else if(i ==3){
                        httpExchange.setAttribute("last_num_of_updates", paramList.get(3));
                    }
                }
                return httpExchange;
            }

            private String URIParser(String uri){
                String[] parts = uri.split("/");
                LinkedList<String> idsList = new LinkedList<>();
                StringBuilder stringBuilder = new StringBuilder();

                for (String part : parts) {
                    if(ObjectId.isValid(part)||part.matches("\\d+") ) { // is SQL ID or mongoID
                        idsList.add(part);
                    }
                    else {
                        stringBuilder.append(part).append("/");
                    }
                }
                paramList = idsList;

                System.out.println("parsed String URI: " + stringBuilder.toString());
                return stringBuilder.toString();
            }

            private void initializeRouters() {
                root = new RecRouter();

                RecRouter companyRouter = new RecRouter();
                RecRouter productRouter = new RecRouter();
                RecRouter IOTDeviceRouter = new RecRouter();
                RecRouter updateRouter = new RecRouter();

                EndPointRouter companyEndPoint = new EndPointRouter();
                EndPointRouter productEndPoint = new EndPointRouter();
                EndPointRouter iotDeviceEndPoint = new EndPointRouter();
                EndPointRouter iotUpdateEndPoint = new EndPointRouter();

                root.use("GIOTI", root);
                companyRouter.use("companies", companyRouter);
                productRouter.use("product", productRouter);
                IOTDeviceRouter.use("IOTDevice",IOTDeviceRouter);
                updateRouter.use("update", updateRouter);

                companyRouter.use("/", companyEndPoint);
                productRouter.use("/", productEndPoint);
                IOTDeviceRouter.use("/", iotDeviceEndPoint);
                updateRouter.use("/", iotUpdateEndPoint);

                PostCompanyCommand postCompanyHandler = new PostCompanyCommand
                        (new CompanyValidator(),new PostCompanyController());

                PostProductCommand postProductCommand = new PostProductCommand
                        (new ProductValidator(), new PostProductController());

                PostIOTDeviceCommand postIOTDeviceCommand = new PostIOTDeviceCommand
                        (new IOTDeviceValidator(), new PostIOTDeviceController());

                PostIOTUpdateCommand postIOTUpdateCommand = new PostIOTUpdateCommand
                        (new IOTUpdateValidator(), new PostIOTUpdateController());


                GetCompanyCommand getCompanyHandler = new GetCompanyCommand
                        (new GetCompanyValidator());

                GetProductCommand getProductCommand = new GetProductCommand
                        (new GetProductValidator());

                GetIOTDeviceCommand getIOTDeviceCommand = new GetIOTDeviceCommand
                        (new GetIOTDeviceValidator());

                GetIOTUpdateCommand getIOTUpdateCommand = new GetIOTUpdateCommand
                        (new GetIOTUpdateValidator());


                companyEndPoint.addHTTPCommand("POST", postCompanyHandler);
                productEndPoint.addHTTPCommand("POST", postProductCommand);
                iotDeviceEndPoint.addHTTPCommand("POST", postIOTDeviceCommand);
                iotUpdateEndPoint.addHTTPCommand("POST", postIOTUpdateCommand);

                companyEndPoint.addHTTPCommand("GET", getCompanyHandler);
                productEndPoint.addHTTPCommand("GET",getProductCommand);
                iotDeviceEndPoint.addHTTPCommand("GET", getIOTDeviceCommand);
                iotUpdateEndPoint.addHTTPCommand("GET",getIOTUpdateCommand);

            }
        }
    }

    public static ByteBuffer serializeChatMsg(ChatMessage responseMessage, ReadableConnection connection) throws IOException {
        ByteBuffer response = ByteBuffer.allocate(1024);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream outputStream = new ObjectOutputStream(byteArrayOutputStream);

        outputStream.writeObject(responseMessage);
        outputStream.flush();

        response.put(byteArrayOutputStream.toByteArray());
        response.flip();

        return response;
    }

    public static void main(String[] args) throws IOException {
        MultiProtocolServer mps = new MultiProtocolServer();
        mps.createConnection(ConnectionType.TCP, 55551);
        mps.createConnection(ConnectionType.UDP, 55552);
        mps.createConnection(ConnectionType.UDP_BROADCAST, 55553);
        mps.runServer();
    }
}

