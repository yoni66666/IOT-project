package il.co.ilrd.multiprotocolserver;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//TODO may mongo will do the getCompanyMongo method...

public class IOTDatabaseManager {
    private Map<String, IOTDatabase> companyDatabases;
    private Map<String, IOTDatabase> mongoCompanyDatabases;
    private static final String urlOfMongoManagerDB = "mongodb://localhost:27017";
    private static final String MONGO_DB = "mongoDB";
    private static final String MONGO_MABAGER_DB_NAME = "MongoManagerDB";

    private static final String mysqlUrl = "jdbc:mysql://localhost:3306/";
    private static final String username = "yoni";
    private static final String password = "password";
    private static final String DBManager = "ManagerDB";
    private static final String CREATE_DATABASE = "CREATE DATABASE IF NOT EXISTS " + DBManager;
    private static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS listOfAllCompanies(\n" +
            "        company_id INT PRIMARY KEY AUTO_INCREMENT,\n" +
            "        company_name VARCHAR(50) UNIQUE KEY\n" +
            "    )";
    private static final String ERROR_VALUE = "Error while retrieving data";

    private final String listOfAllCompanies = "listOfAllCompanies";

    private Connection connection;
    private MongoClient managerMongoClient;
    private MongoDatabase ManagerDatabase;
    private static final Pattern pattern = Pattern.compile("^\\d+$");

    public IOTDatabaseManager(){
        companyDatabases = new HashMap<>();
        mongoCompanyDatabases = new HashMap<>();
        Statement statement = null;
        try {
            connection = DriverManager.getConnection(mysqlUrl ,username, password);
            statement = connection.createStatement();
            statement.executeUpdate(CREATE_DATABASE);
            statement.executeUpdate("USE " + DBManager);
            statement.executeUpdate(CREATE_TABLE);

            loadOldDB(statement);
            connection.close();
            statement.close();

            loadMongoDB();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean registerCompany(JSONObject jsonObject) {

        String companyName = (String) jsonObject.get("company_name");
        String username = (String) jsonObject.get("username");
        String password = (String) jsonObject.get("password");

        if (MONGO_DB.equals((String) jsonObject.get("database_type"))){

            //if MONGO_MABAGER_DB isExist so it wil return a instance to the DB
            //else - it will be created when you first store data in it.
            managerMongoClient = MongoClients.create(urlOfMongoManagerDB);
            ManagerDatabase = managerMongoClient.getDatabase(MONGO_MABAGER_DB_NAME);

            if (!mongoCompanyDatabases.containsKey(companyName)){
                IOTMongoDataBase iotMongoDataBase = new IOTMongoDataBase(companyName, username, password);
                mongoCompanyDatabases.put(companyName, iotMongoDataBase);
                return iotMongoDataBase.registerCompany(jsonObject);
            }
            else {
                System.out.println("The company '" + companyName + "' exists in the database.");
                return false;
            }
        }

        /*** SQL ***/
        else {
            if(companyDatabases.containsKey(companyName)) {
                System.out.println(companyName+ " already exist");
                return false;
            }

            try {
                connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/", "yoni", "password");
                Statement statement = connection.createStatement();
                statement.executeUpdate("USE " + DBManager);
                statement.executeUpdate("insert into listOfAllCompanies (company_name) values ('" + companyName + "');");

            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

            IOTMySQLDatabase iotMySQLDatabase = new IOTMySQLDatabase(companyName, username, password);
            companyDatabases.put(companyName, iotMySQLDatabase);

            return iotMySQLDatabase.registerCompany(jsonObject);
        }
    }

    public boolean registerProduct(JSONObject jsonObject) {
        String companyName = (String) jsonObject.get("company_name");

        if (MONGO_DB.equals((String) jsonObject.get("database_type"))){
            managerMongoClient = MongoClients.create(urlOfMongoManagerDB);
            if (!mongoCompanyDatabases.containsKey(companyName)){
                System.out.println("The company " + companyName + " does not exist in the database.");
                return false;
            }
            return mongoCompanyDatabases.get(companyName).registerProduct(jsonObject);
        }

        /** SQL **/
        else if(!companyDatabases.containsKey(companyName)) {
            System.out.println("company name dose not exist");
            return false;
        }
        return  companyDatabases.get(companyName).registerProduct(jsonObject);
    }

    public boolean registerIOT(JSONObject jsonObject) {

        String companyName = (String) jsonObject.get("company_name");

        if (MONGO_DB.equals((String) jsonObject.get("database_type"))){
            managerMongoClient = MongoClients.create(urlOfMongoManagerDB);
            if (!mongoCompanyDatabases.containsKey(companyName)){
                System.out.println("The company " + companyName + " does not exist in the database.");
                return false;
            }
            return  mongoCompanyDatabases.get(companyName).registerIOT(jsonObject);
        }

        IOTMySQLDatabase iotMySQLDatabase = (IOTMySQLDatabase) companyDatabases.get(companyName);
        if(iotMySQLDatabase == null){
            System.out.println("company name dose not exist");
            return false;
        }
        return iotMySQLDatabase.registerIOT(jsonObject);
    }
    public boolean updateIOT(JSONObject jsonObject) {
        String companyName = (String) jsonObject.get("company_name");

        if (MONGO_DB.equals(jsonObject.get("database_type"))){
            managerMongoClient = MongoClients.create(urlOfMongoManagerDB);
            if (!!mongoCompanyDatabases.containsKey(companyName)){
                System.out.println("The company " + companyName + " does not exist in the database.");
                return false;
            }
            return  mongoCompanyDatabases.get(companyName).updateIOT(jsonObject);
        }

        IOTMySQLDatabase iotMySQLDatabase = (IOTMySQLDatabase) companyDatabases.get(companyName);

        if(iotMySQLDatabase == null){
            System.out.println("company name dose not exist");
            return false;
        }

        return iotMySQLDatabase.updateIOT(jsonObject);
    }

    private void loadOldDB(Statement statement) throws SQLException {
        ResultSet resultSet = statement.executeQuery("select * from " + "listOfAllCompanies");
        while (resultSet.next()) {
            String company_name = resultSet.getString("company_name");
            companyDatabases.put(company_name, new IOTMySQLDatabase(company_name, username, password));
        }
    }

    private void loadMongoDB(){
        MongoClient mongoClient = MongoClients.create("mongodb://localhost:27017");
        MongoDatabase database = mongoClient.getDatabase(MONGO_MABAGER_DB_NAME);
        MongoCollection<Document> collection = database.getCollection("companies");

        for (Document document : collection.find()) {
            String companyName = document.get("company_name").toString();
            mongoCompanyDatabases.put(companyName, new IOTMongoDataBase(companyName, username, password));
            System.out.println("Collection: " + companyName);
        }

        mongoClient.close();
    }

    public JSONObject getCompanies(){
        JSONObject json = new JSONObject();
        Statement statement;
        ResultSet resultSet;

        /***********    SQL    ***********/
        try {
            connection = DriverManager.getConnection(mysqlUrl + DBManager, username, password);
            statement = connection.createStatement();
            resultSet = statement.executeQuery("SELECT company_id, company_name FROM "+ listOfAllCompanies);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        JSONArray companiesArray = createJsonArrayOfData(resultSet);
        /***********  END SQL    ***********/


        /**********************     MONGO    ****************/

        MongoCollection<Document> collection = gecollectionOfAllCompanies();

        // Retrieve all the documents from the collection
        List<Document> documents = collection.find().into(new ArrayList<>());

        for (Document document : documents) {
            String companyName = document.getString("company_name");
            String companyID = document.getObjectId("_id").toString();

            // Create a JSON object for each company and add the company name and ID to it
            JSONObject company = new JSONObject();
            company.put("company_name", companyName);
            company.put("company_id", companyID);
            companiesArray.add(company);
        }
        /**********************    END MONGO    ****************/

        if(companiesArray == null){
            json.put("error",ERROR_VALUE);
            return json;
        }
        json.put("companies", companiesArray);

        try {
            connection.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        managerMongoClient.close();

        return json;
    }

    public JSONObject getCompany(String companyID){
        //TODO may mongo will do the getCompanyMongo method...

        Matcher matcher = pattern.matcher(companyID);
        if (matcher.matches()) {
            System.out.println("The string contains only numbers.");
            return getCompanySql(companyID);
        } else {
            System.out.println("The string does not contain only numbers.");
            return getCompanyMongo(companyID);
        }
    }

    private  JSONObject getCompanyMongo(String companyID){

        MongoCollection<Document> collection = gecollectionOfAllCompanies();
        Document doc = collection.find(new Document("_id", new ObjectId(companyID))).first();
        if (doc != null) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("company_name", doc.get("company_name").toString());
            jsonObject.put("address", doc.get("address"));
            jsonObject.put("company_contacts", doc.get("company_contacts"));
            jsonObject.put("payments_info", doc.get("payments_info"));
            return jsonObject;
        }
        JSONObject jsonError = new JSONObject();
        jsonError.put("error", ERROR_VALUE);
        return  jsonError;
    }

    private  JSONObject getCompanySql(String companyID){

        JSONObject jsonError = new JSONObject();
        String companyName = getCompanyName(companyID);
        if(companyName == null){
            jsonError.put("error", ERROR_VALUE);
            return jsonError;
        }

        JSONObject jsonRes = new JSONObject();
        Connection connection = getCompanyConnection(companyName);

        try {
            Statement statement = connection.createStatement();
            jsonRes.put("company_name", companyName);
            jsonRes.put("Addresses", createJsonArrayOfData(statement.executeQuery("select * from Addresses;")));
            jsonRes.put("PaymentInfo", createJsonArrayOfData(statement.executeQuery("select * from PaymentInfo;")));
            jsonRes.put("CompanyContacts", createJsonArrayOfData(statement.executeQuery("select * from CompanyContacts;")));
        } catch (SQLException e) {
            jsonError.put("error",  "Internal server error");
            return jsonError;
        }
        return jsonRes;

    }

    private String getCompanyName(String companyID){
        int company_id = Integer.parseInt(companyID);
        String getCompanyQuery = "SELECT company_name FROM " + listOfAllCompanies + " where company_id = "+ company_id+";";
        String companyName;
        try {
            connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/"+DBManager, "yoni", "password");
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(getCompanyQuery);
            resultSet.next();
            companyName = resultSet.getString("company_name");
            statement.close();
            connection.close();

        } catch (SQLException e) {
            throw new RuntimeException();
        }
        return companyName;
    }

    private Connection getCompanyConnection(String companyName){
        try {
            return DriverManager.getConnection(mysqlUrl +companyName, username, password);
        }catch (SQLException e){
            throw new RuntimeException();
        }
    }

    public JSONObject getProducts(String companyID) {

        Matcher matcher = pattern.matcher(companyID);
        if (matcher.matches()) {
            return getProductsSql(companyID);
        } else {
            return getProductsMongo(companyID);
        }
    }
    private MongoCollection<Document> gecollectionOfAllCompanies(){
        managerMongoClient = MongoClients.create(urlOfMongoManagerDB);
        ManagerDatabase = managerMongoClient.getDatabase(MONGO_MABAGER_DB_NAME);
        MongoCollection<Document> collection = ManagerDatabase.getCollection("companies");;
        return collection;
    }
    private String getCompanyNameFromId(String companyID){
        MongoCollection<Document> collection = gecollectionOfAllCompanies();
        Document doc = collection.find(new Document("_id", new ObjectId(companyID))).first();
        String companyName = doc.get("company_name").toString();
        return companyName;
    }

    private  JSONObject getProductsSql(String companyID){
        JSONObject jsonRes = new JSONObject();
        JSONObject jsonError = new JSONObject();
        String companyName = getCompanyName(companyID);

        if(companyName == null){
            jsonError.put("error", ERROR_VALUE);
            return jsonError;
        }

        String getProductsQuery = "SELECT product_id, product_name FROM Products";
        Connection connection = getCompanyConnection(companyName);

        try {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(getProductsQuery);

            JSONArray productsArray = createJsonArrayOfData(resultSet);
            if(productsArray == null){
                jsonError.put("error", ERROR_VALUE);
                return jsonError;
            }

            jsonRes.put("products", productsArray);

        } catch (SQLException e) {
            jsonError.put("error",  "Internal server error");
            return jsonError;
        }
        return jsonRes;
    }

    private JSONObject getProductsMongo(String companyID){

            MongoCollection<Document> collection = gecollectionOfAllCompanies();
            Document doc = collection.find(new Document("_id", new ObjectId(companyID))).first();
            if (doc != null) {
                String companyName = doc.get("company_name").toString();
                MongoDatabase database = managerMongoClient.getDatabase(companyName);
                collection = database.getCollection("products");
                JSONArray jsonArray = new JSONArray();
                JSONObject jsonObject = new JSONObject();
                for (Document document : collection.find()) {
                    jsonObject.put("product_id", document.get("_id").toString());
                    jsonObject.put("product_name", document.get("product_name").toString());
                    jsonArray.add(jsonObject);
                }
                JSONObject jsonRes = new JSONObject();
                jsonRes.put("AllProducts", jsonArray);
                managerMongoClient.close();
                return jsonRes;
            }
        JSONObject jsonError = new JSONObject();
        jsonError.put("error", ERROR_VALUE);
        managerMongoClient.close();
        return  jsonError;
    }

    public JSONObject getProduct(String companyID, String productID) {

        Matcher matcher = pattern.matcher(companyID);
        if (matcher.matches()) {
            return getProductSql(companyID, productID);
        } else {
            return getProductMongo(companyID, productID);
        }
    }
    private JSONObject getProductSql(String companyID, String productID) {
        JSONObject jsonRes = new JSONObject();
        JSONObject jsonError = new JSONObject();
        int productId = Integer.parseInt(productID);
        String companyName = getCompanyName(companyID);
        JSONObject specification;

        if(companyName == null){
            jsonError.put("error", ERROR_VALUE);
            return jsonError;
        }
        Connection connection = getCompanyConnection(companyName);
        String getProductQuery = "SELECT * FROM Specification where product_id = "+productId+";";
        String productName = getNameFromId(connection, "Products","product_name","product_id", productID);
        if(productName == null){
            jsonError.put("error", ERROR_VALUE);
            return jsonError;
        }
        jsonRes.put("product_id", productId);
        jsonRes.put("product_name", productName);
        try {
            Statement stmt = connection.createStatement();
            ResultSet resultSet = stmt.executeQuery(getProductQuery);
            if((specification = createJsonObjectOfData(resultSet)) == null){
                jsonError.put("error", ERROR_VALUE);
                return  jsonError;
            }

            jsonRes.put("Specification", specification);
        } catch (SQLException e) {
            jsonError.put("error",  "Internal server error");
            return jsonError;
        }
        return jsonRes;
    }

    private JSONObject getProductMongo(String companyID, String productID) {
        String companyName = getCompanyNameFromId(companyID);
        Document productDocument = getproductDocumentFromCompanyNameAndProductID(companyName,productID);
        if (productDocument != null) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("product_name", productDocument.get("product_name").toString());
            jsonObject.put("product_specification", productDocument.get("product_specification").toString());
            managerMongoClient.close();
            return jsonObject;
        }
        JSONObject jsonError = new JSONObject();
        jsonError.put("error", ERROR_VALUE);
        managerMongoClient.close();
        return  jsonError;
    }
    private Document getproductDocumentFromCompanyNameAndProductID(String companyName, String productID){
        MongoDatabase database = managerMongoClient.getDatabase(companyName);
        MongoCollection<Document> productCollection = database.getCollection("products");
        Document productDocument = productCollection.find(new Document("_id", new ObjectId(productID))).first();
        return productDocument;
    }
    private String getNameFromId(Connection connection,String tableName, String nameField ,String idfieldName,String id){
        int intID = Integer.parseInt(id);
        String getCompanyQuery = "SELECT "+nameField +" FROM " + tableName + " where "+idfieldName+"="+ intID+";";
        String resName;
        try {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(getCompanyQuery);
            if (resultSet.next()) {
                resName =  resultSet.getString(nameField);
                statement.close();
                return resName;
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    private JSONArray createJsonArrayOfData(ResultSet resultSet){
        JSONArray companyArray = new JSONArray();
        JSONObject jsonObject;
        while((jsonObject = createJsonObjectOfData(resultSet) )!= null){
            companyArray.add(jsonObject);
        }
        return companyArray;
    }

    private JSONObject createJsonObjectOfData(ResultSet resultSet){
        JSONObject jsonObject = new JSONObject();

        try {
            int columnCount = resultSet.getMetaData().getColumnCount();
            if (!resultSet.next()) {
                return null;
            }
            for(int i = 1; i <= columnCount; ++i){
                String columnName = resultSet.getMetaData().getColumnName(i);
                Object columnValue = resultSet.getObject(i);
                jsonObject.put(columnName, columnValue);
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return jsonObject;
    }

    public JSONObject getIOTDevices(String companyID, String productID){
        Matcher matcher = pattern.matcher(companyID);
        if (matcher.matches()) {
            return getIOTDevicesSql(companyID, productID);
        } else {
            return getIOTDevicesMongo(companyID, productID);
        }
    }
    private JSONObject getIOTDevicesMongo(String companyID, String productID) {
        String companyName = getCompanyNameFromId(companyID);
        Document productDocument = getproductDocumentFromCompanyNameAndProductID(companyName,productID);
        if (productDocument != null) {
            JSONArray jsonArray = new JSONArray();
            MongoDatabase database = managerMongoClient.getDatabase(companyName);
            MongoCollection<Document> collection = database.getCollection("iot_devices");
            for (Document doc : collection.find()) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("device_serial_number", doc.get("_id").toString());
                jsonArray.add(jsonObject);
            }
            JSONObject res = new JSONObject();
            res.put("IOTDevices", jsonArray);
            return res;
        }
        JSONObject jsonError = new JSONObject();
        jsonError.put("error", ERROR_VALUE);
        return  jsonError;
    }
    private JSONObject getIOTDevicesSql(String companyID, String productID){
        JSONObject jsonRes = new JSONObject();
        JSONObject jsonError = new JSONObject();
        int productId = Integer.parseInt(productID);
        String getIOTDevicesQuery = "select device_serial_number from IOTDevices where product_id ="+productId+";";
        String companyName = getCompanyName(companyID);
        if(companyName == null){
            jsonError.put("error", ERROR_VALUE);
            return jsonError;
        }
        Connection connection = getCompanyConnection(companyName);
        try {
            Statement statement = connection.createStatement();
            JSONArray IOTDevicesArr;
            if((IOTDevicesArr= createJsonArrayOfData(statement.executeQuery(getIOTDevicesQuery))) == null){
                jsonError.put("error", ERROR_VALUE);
                return jsonError;
            }
            jsonRes.put("IOTDevices", IOTDevicesArr);
        } catch (SQLException e) {
            jsonError.put("error",  "Internal server error");
            return jsonError;
        }
        return jsonRes;
    }

    public JSONObject getIOTDevice(String companyID, String iotDeviceId) {

        Matcher matcher = pattern.matcher(companyID);
        if (matcher.matches()) {
            return getIOTDeviceSql(companyID, iotDeviceId);
        } else {
            return getIOTDeviceMongo(companyID, iotDeviceId);
        }
    }
    private JSONObject getIOTDeviceSql(String companyID, String iotDeviceId) {
        JSONObject jsonRes = new JSONObject();
        String companyName = getCompanyName(companyID);
        Connection connection = getCompanyConnection(companyName);

        Integer ownerId = getId(connection,"IOTDevices", "owner_id",Integer.parseInt(iotDeviceId));
        String ownerName = getNameFromId(connection, "IOTOwners",
                "owner_name", "owner_id", iotDeviceId);
        jsonRes.put("owner_name", ownerName);

        Integer addressId = getId(connection,"IOTOwners", "address_id",ownerId);
        String ownerPhone = getNameFromId(connection, "IOTOwners",
                "owner_phone", "owner_id", iotDeviceId);
        jsonRes.put("owner_phone", ownerPhone);

        Integer paymentId = getId(connection,"IOTOwners","payment_id", ownerId);
        String ownerEmail = getNameFromId(connection, "IOTOwners",
                "owner_email", "owner_id", iotDeviceId);
        jsonRes.put("owner_email", ownerEmail);

        try {
            Statement statement = connection.createStatement();
            jsonRes.put("address", createJsonObjectOfData(
                    statement.executeQuery("select * from Addresses where address_id = "+ addressId+";")));
            jsonRes.put("payment_Info", createJsonObjectOfData(
                    statement.executeQuery("select credit_card, expiration_date,cvv" +
                            " from PaymentInfo where payment_id="+paymentId+";")));
        } catch (SQLException e) {
            jsonRes.put("error",  "Internal server error");
            return jsonRes;
        }
        return jsonRes;
    }
    private JSONObject getIOTDeviceMongo(String companyID, String iotDeviceId) {
        String companyName = getCompanyNameFromId(companyID);
        Document IOTDeviceDocument = getIOTDeviceDocumentFromCompanyNameAndIOTDeviceID(companyName,iotDeviceId);
        if (IOTDeviceDocument != null) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.putAll(IOTDeviceDocument);
            return jsonObject;
        }

        JSONObject jsonError = new JSONObject();
        jsonError.put("error", ERROR_VALUE);
        return  jsonError;
    }
    private Document getIOTDeviceDocumentFromCompanyNameAndIOTDeviceID(String companyName, String iotDeviceId){
        MongoDatabase database = managerMongoClient.getDatabase(companyName);
        MongoCollection<Document> IOTDeviceCollection = database.getCollection("iot_devices");
        Document IOTDeviceDocument = IOTDeviceCollection.find(new Document("_id", new ObjectId(iotDeviceId))).first();
        return IOTDeviceDocument;
    }

    private Integer getId(Connection connection, String tableName,String idRequested, int currId) {
        String getIdQuery = "select " + idRequested + " from " + tableName + " where owner_id=" + currId + ";";
        int id;
        try {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(getIdQuery);
            resultSet.next();
            id = resultSet.getInt(idRequested);
            statement.close();
        } catch (SQLException e) {
            return null;
        }
        return id;
    }

    public JSONObject getIOTUpdate(String companyID, String iotDevice, int lastNumOfUpdates) {

        Matcher matcher = pattern.matcher(companyID);
        if (matcher.matches()) {
            return getIOTUpdateSql(companyID, iotDevice, lastNumOfUpdates);
        } else {
            return getIOTUpdateMongo(companyID, iotDevice, lastNumOfUpdates);
        }
    }

    private JSONObject getIOTUpdateMongo(String companyID, String iotDeviceId, int lastNumOfUpdates){
        String companyName = getCompanyNameFromId(companyID);
        Document IOTDeviceDocument = getIOTDeviceDocumentFromCompanyNameAndIOTDeviceID(companyName,iotDeviceId);

        if (IOTDeviceDocument != null) {
            JSONArray jsonArray = new JSONArray();
            JSONObject jsonObject = new JSONObject();
            MongoDatabase database = managerMongoClient.getDatabase(companyName);
            MongoCollection<Document> collection = database.getCollection("IOTUpdates");
            for (Document doc1 : collection.find().sort(new Document("data_timestamp", -1)).limit(lastNumOfUpdates)){
                jsonObject.put("iot_data", doc1.get("iot_data"));
                jsonObject.put("data_timestamp", doc1.get("data_timestamp"));
                jsonArray.add(jsonObject);
            }
            JSONObject jsonObjectRes = new JSONObject();
            jsonObjectRes.put("IOTUpdates", jsonArray);
            return jsonObjectRes;
        }

        JSONObject jsonError = new JSONObject();
        jsonError.put("error", ERROR_VALUE);
        return  jsonError;
    }

    private JSONObject getIOTUpdateSql(String companyID, String iotDeviceId,  int lastNumOfUpdates){

        JSONObject jsonRes = new JSONObject();
        JSONObject jsonError = new JSONObject();
        String getLastUpdatesQuery = "SELECT iot_data,iot_timestamp FROM IOTData" +
                " ORDER BY iot_data_id DESC LIMIT "+ lastNumOfUpdates+";";

        Connection connection = getCompanyConnection(getCompanyName(companyID));
        try {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(getLastUpdatesQuery);
            jsonRes.put("IOT_Updates" , createJsonArrayOfData(resultSet));
        } catch (SQLException e) {
            jsonError.put("error",  "Internal server error");
            return jsonError;
        }
        return jsonRes;
    }
}
