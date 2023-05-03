package il.co.ilrd.multiprotocolserver;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class IOTMongoDataBase  implements IOTDatabase{
    private static final String companiesCollection = "companies" ;
    private String dataBaseName;
    private String username;
    private String password;
    private MongoClient mongoClient;
    private MongoDatabase companeyDatabase;

    private static final String urlOfManagerDB = "mongodb://localhost:27017";
    private static final String MONGO_MABAGER_DB_NAME = "MongoManagerDB";

    public IOTMongoDataBase(String dataBaseName, String username, String password){
        this.dataBaseName = dataBaseName;
        this.username = username;
        this.password = password;

    }

    @Override
    public boolean registerCompany(JSONObject jsonObject) {
        // Connect to MongoDB
        MongoClient managerMongoClient = MongoClients.create("mongodb://localhost:27017");
        MongoDatabase ManagerDatabase = managerMongoClient.getDatabase(MONGO_MABAGER_DB_NAME);
        InsertCompanyDataToManagerDB(ManagerDatabase, jsonObject);
        managerMongoClient.close();

        return true;
    }

    @Override
    public boolean registerProduct(JSONObject jsonObject) {
        mongoClient = MongoClients.create("mongodb://localhost:27017");
        companeyDatabase = mongoClient.getDatabase(dataBaseName);
        InsertProductDataToComaneyDB(jsonObject);
        mongoClient.close();

        return true;
    }

    @Override
    public boolean registerIOT(JSONObject jsonObject) {
        mongoClient = MongoClients.create("mongodb://localhost:27017");
        companeyDatabase = mongoClient.getDatabase(dataBaseName);
        Document document = new Document();
        jsonObject = removeUnnecessaryFields(jsonObject);
        document.putAll(jsonObject);

        companeyDatabase.getCollection("iot_devices").insertOne(document);
        mongoClient.close();

        return true;
    }

    @Override
    public boolean updateIOT(JSONObject jsonObject) {
        mongoClient = MongoClients.create("mongodb://localhost:27017");
        companeyDatabase = mongoClient.getDatabase(dataBaseName);
        InsertUpdateIOTDataToComaneyDB(jsonObject);
        mongoClient.close();

        return true;
    }

    private void InsertCompanyDataToManagerDB(MongoDatabase database, JSONObject jsonObject){
        //database.createCollection(name);
        MongoCollection<Document> collection = database.getCollection(companiesCollection);

        // Create the JSON document as a BSON Document
        Document document = new Document()
                .append("database_type", "mongoDB")
                .append("iot_command", "registerCompany")
                .append("company_name", dataBaseName)
                .append("username", username)
                .append("password", password);
        // Insert the document into the collection
        collection.insertOne(document);
        insertCompanyPaymentsInfo(jsonObject, collection);
        insertCompanyContacts(jsonObject, collection);
        InsertAdresses(jsonObject, collection);
    }
    private void insertCompanyPaymentsInfo(JSONObject jsonObject, MongoCollection<Document> collection){
        JSONArray company_payments_info = (JSONArray) jsonObject.get("payments_info");
        int i = 1;
        for(Object contactObj : company_payments_info) {
            JSONObject contact = (JSONObject) contactObj;
            collection.updateOne(
                    new Document("company_name", dataBaseName),
                    Updates.combine(
                            Updates.set("payments_info"+i, Arrays.asList(
                                    new Document()
                                            .append("credit_card", contact.get("credit_card"))
                                            .append("expiration_date", contact.get("expiration_date"))
                                            .append("cvv", contact.get("cvv"))
                            ))
                    )
            );
            ++i;
        }
    }

    private void insertCompanyContacts(JSONObject jsonObject, MongoCollection<Document> collection){
        JSONArray company_payments_info = (JSONArray) jsonObject.get("company_contacts");
        for(Object contactObj : company_payments_info) {
            JSONObject contact = (JSONObject) contactObj;
            collection.updateOne(
                    new Document("company_name", dataBaseName),
                    Updates.combine(
                            Updates.set("company_contacts", Arrays.asList(
                                    new Document()
                                            .append("contact_name", contact.get("contact_name"))
                                            .append("contact_email", contact.get("contact_email"))
                                            .append("contact_phone", contact.get("contact_phone"))
                            ))
                    )
            );
        }
    }

    private void InsertAdresses(JSONObject jsonObject, MongoCollection<Document> collection){
        JSONObject companyAddress = (JSONObject) jsonObject.get("address");
            collection.updateOne(
                    new Document("company_name", dataBaseName),
                    Updates.combine(
                            Updates.set("address", new Document()
                                    .append("country", companyAddress.get("country"))
                                    .append("city", companyAddress.get("city"))
                                    .append("street", companyAddress.get("street"))
                                    .append("postal_code",companyAddress.get("postal_code"))
                            )
                    )
            );
    }

    private void InsertProductDataToComaneyDB(JSONObject jsonObject){
         if (!collectionExists("products", dataBaseName)){
             companeyDatabase.createCollection("products");
         }

        MongoCollection<Document> collection = companeyDatabase.getCollection("products");
        JSONObject productSpecificationData = (JSONObject) jsonObject.get("product_specification");

        Document document = new Document()
                .append("product_name", jsonObject.get("product_name"))
                .append("product_specification", Arrays.asList(
                        new Document()
                                .append("product_price", productSpecificationData.get("product_price"))
                                .append("product_category", productSpecificationData.get("product_category"))
                                .append("product_weight", productSpecificationData.get("product_weight"))
                ));

        collection.insertOne(document);
    }
    private boolean collectionExists(String collectionName, String companeyName) {
        MongoClient mongoClient = MongoClients.create("mongodb://localhost:27017");
        MongoDatabase database = mongoClient.getDatabase(companeyName);
        List<String> collectionNames = database.listCollectionNames().into(new ArrayList<>());
        if (collectionNames.contains(collectionName)) {
            System.out.println("The collection exists in the database");
            return true;
        } else {
            System.out.println("The collection does not exist in the database");
            return false;
        }
    }

    private void InsertUpdateIOTDataToComaneyDB(JSONObject jsonObject){

        String companyName = jsonObject.get("company_name").toString();
        MongoDatabase database = mongoClient.getDatabase(companyName);
        Document doc = new Document();
        jsonObject = (JSONObject) jsonObject.get("iot_details");
        jsonObject.put("data_timestamp", new Timestamp(System.currentTimeMillis()));
        doc.putAll(jsonObject);
        MongoCollection<Document> collection = database.getCollection("IOTUpdates");
        collection.insertOne(doc);
/*
        MongoCollection<Document> collection = companeyDatabase.getCollection("iot_devices");
        JSONObject IOTDetailsData = (JSONObject) jsonObject.get("iot_details");

        collection.updateOne(
                new Document("products",  jsonObject.get("product_name")),
                Updates.combine(
                        Updates.set("iot_details", new Document()
                                .append("iot_data", IOTDetailsData.get("iot_data"))
                                .append("device_serial_number", IOTDetailsData.get("device_serial_number"))
                                .append("data_timestamp", new Timestamp(System.currentTimeMillis()))
                        )
                )
        );

 */
    }

    private JSONObject removeUnnecessaryFields(JSONObject jsonObject) {
        jsonObject.remove("database_type");
        jsonObject.remove("iot_command");
        jsonObject.remove("company_name");
        jsonObject.remove("username");
        jsonObject.remove("password");
        return jsonObject;
    }

}
