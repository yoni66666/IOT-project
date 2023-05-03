package il.co.ilrd.multiprotocolserver;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.apache.ibatis.jdbc.ScriptRunner;

import java.io.FileReader;
import java.sql.*;

public class IOTMySQLDatabase implements IOTDatabase {
    String dataBaseName;
    String username;
    String password;
    String url = "jdbc:mysql://localhost:3306/";
    Connection currentDBconnection;

    public IOTMySQLDatabase(String dataBaseName, String username, String password){
        this.dataBaseName = dataBaseName;
        this.username = username;
        this.password = password;
    }

    @Override
    public boolean registerCompany(JSONObject jsonObject) {
        if (!CreateNewDB()){
            return false;
        };
        CreateTableForDB();
        try {
            InsertAdresses(jsonObject);
            insertCompanyContacts(jsonObject);
            insertCompanyPaymentsInfo(jsonObject, true);
            currentDBconnection.commit();
/*
            Statement statement = currentDBconnection.createStatement();
            ResultSet  resultSet = statement.executeQuery("select * from CompanyContacts");
            while (resultSet.next()) {
                System.out.println(resultSet.getString("contact_email"));
            };

 */

            currentDBconnection.close();
        }catch (SQLException ex ){
            throw new RuntimeException();
        }

        return true;
    }

    @Override
    public boolean registerProduct(JSONObject jsonObject) {
        //TODO  get  password from json
        String curUrl = url+dataBaseName;
        try{
            currentDBconnection = DriverManager.getConnection(curUrl, username, password);
            currentDBconnection.setAutoCommit(false);
            if (!insertProductData(jsonObject)){
                return false;
            };
            currentDBconnection.commit();
            currentDBconnection.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return true;
    }

    @Override
    public boolean registerIOT(JSONObject jsonObject) {
        String curUrl = url+dataBaseName;
        try {
            currentDBconnection = DriverManager.getConnection(curUrl, username, password);
            currentDBconnection.setAutoCommit(false);
            if (!insertIOTDataToDatabase(jsonObject)){
                return false;
            };
            currentDBconnection.commit();
            currentDBconnection.close();

        }catch (SQLException e){
            throw new RuntimeException(e);
        }

        return true;
    }

    @Override
    public boolean updateIOT(JSONObject jsonObject) {
        String curUrl = url+dataBaseName;
        try {
            currentDBconnection = DriverManager.getConnection(curUrl, username, password);
            currentDBconnection.setAutoCommit(false);
            if (!updateIOTData(jsonObject)){
                return false;
            };
            currentDBconnection.commit();
            currentDBconnection.close();

        }catch (SQLException e){
            throw new RuntimeException(e);
        }
        return true;
    }


    private boolean CreateNewDB(){
        Connection connection = null;
        Statement statement = null;
        String url = "jdbc:mysql://localhost:3306/";
        try {
            connection = DriverManager.getConnection(url, username, password);
            //connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/", "yoni", "password");

            statement = connection.createStatement();
            statement.executeUpdate("CREATE DATABASE " + dataBaseName);

            System.out.println("Database created successfully");
        } catch ( SQLException e) {
            e.printStackTrace();
        } finally {
            // Close the connection
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return true;
    }

    private void CreateTableForDB(){
        String url = "jdbc:mysql://localhost:3306/" +dataBaseName ;

        try {
            currentDBconnection = DriverManager.getConnection(url, username, password);

            ScriptRunner runner = new ScriptRunner(currentDBconnection);
            runner.runScript(new FileReader("/home/jonathan/git/jonathan.shapiro/fs/projects/src/sql/createIOT.sql"));
            //use.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private  void InsertAdresses(JSONObject jsonObject) throws SQLException {
        JSONObject companyAddress = (JSONObject) jsonObject.get("address");

        String sql = "INSERT INTO Addresses(country, city, street, postal_code) VALUES (?, ?, ?,?);";
        PreparedStatement preStatement = currentDBconnection.prepareStatement(sql);

        preStatement.setString(1, (String) companyAddress.get("country"));
        preStatement.setString(2, (String) companyAddress.get("city"));
        preStatement.setString(3, (String) companyAddress.get("street"));
        preStatement.setString(4, (String) companyAddress.get("postal_code"));
        int rowsAffected = preStatement.executeUpdate();
        System.out.println(rowsAffected + " row(s) inserted.");
    }

    private void insertCompanyContacts(JSONObject jsonObject) throws SQLException {
        PreparedStatement preStatement = null;
        JSONArray company_contacts = (JSONArray) jsonObject.get("company_contacts");

        for (Object contactObj : company_contacts){
            JSONObject contact = (JSONObject) contactObj;

            String sql = "INSERT INTO CompanyContacts(contact_name, contact_email, contact_phone, address_id) VALUES (?, ?, ?, ?);";
            preStatement = currentDBconnection.prepareStatement(sql);

            preStatement.setString(1, (String) contact.get("contact_name"));
            preStatement.setString(2, (String) contact.get("contact_email"));
            preStatement.setString(3, (String) contact.get("contact_phone"));
            int lastInsertId = getLastAddressId();
            System.out.println("lastInsertId " + lastInsertId);

            preStatement.setInt(4, lastInsertId);

            int rowsAffected = preStatement.executeUpdate();
            System.out.println(rowsAffected + " insertCompanyContacts row(s) inserted.");
        }
    }

    private void insertCompanyPaymentsInfo(JSONObject jsonObject, boolean isCompany) throws SQLException {
        JSONArray company_payments_info = (JSONArray) jsonObject.get("payments_info");
        for(Object contactObj : company_payments_info) {
            JSONObject contact = (JSONObject) contactObj;

            String insertPaymentInfoQuery = "INSERT INTO PaymentInfo (credit_card, expiration_date, cvv, is_company) VALUES (?, ?, ?, " + isCompany + ")";
            PreparedStatement preStatement = currentDBconnection.prepareStatement(insertPaymentInfoQuery);

            preStatement.setString(1, (String) contact.get("credit_card"));
            preStatement.setString(2, (String) contact.get("expiration_date"));
            preStatement.setString(3, (String) contact.get("cvv"));
            preStatement.executeUpdate();
            preStatement.close();
        }
    }

    private int getLastAddressId() throws SQLException {
        Statement selectAddressStatement = currentDBconnection.createStatement();
        ResultSet selectAddressQueryData = selectAddressStatement.executeQuery("SELECT LAST_INSERT_ID() FROM Addresses;");
        selectAddressQueryData.next();
        return selectAddressQueryData.getInt(1);
    }

    private boolean insertProductData(JSONObject jsonObject) throws SQLException {
        Statement statement = currentDBconnection.createStatement();
        String productName = (String) jsonObject.get("product_name");
        JSONObject productSpecificationData = (JSONObject) jsonObject.get("product_specification");

        Statement use = currentDBconnection.createStatement();
        use.executeUpdate("USE " + dataBaseName);
        if(isProductExist(statement, productName)){
            return false;
        }
        statement.executeUpdate("INSERT INTO Products (product_name) VALUES ('" + productName + "')");

        Double product_price = (Double) productSpecificationData.get("product_price");
        String product_category = (String) productSpecificationData.get("product_category");
        Double product_weight = (Double) productSpecificationData.get("product_weight");
        Integer product_id = getLastInsertedIDFromTable("Products");

        Statement addressStatement = currentDBconnection.createStatement();
        addressStatement.executeUpdate("INSERT INTO Specification (product_price, product_category, product_weight, product_id) VALUES('" +
                product_price + "', '" + product_category + "', '" + product_weight + "', '" + product_id + "')");
        return true;
    }

    private Boolean isProductExist(Statement statement, String productName) throws SQLException {
        ResultSet  resultSet = statement.executeQuery("select * from Products");
        while (resultSet.next()) {
            String result = resultSet.getString("product_name");
            if (productName.equals(result)) {
                System.out.println(productName + " is already exist");
                return true;
            }
        }
        return false;
    }
    
    private int getLastInsertedIDFromTable(String tableName) throws SQLException {
        Statement selectAddressStatement = currentDBconnection.createStatement();
        ResultSet selectAddressQueryData = selectAddressStatement.executeQuery("SELECT LAST_INSERT_ID() FROM " + tableName);
        selectAddressQueryData.next();
        return selectAddressQueryData.getInt(1);
    }

    private boolean insertIOTDataToDatabase(JSONObject jsonObject) throws SQLException {
        JSONObject ownerDetailsData = (JSONObject) jsonObject.get("owner_details");
        insertOwnerDetailsData(ownerDetailsData);

        String productName = (String) jsonObject.get("product_name");

        Statement statement = currentDBconnection.createStatement();

        if (!isProductExist(statement, productName)) {
            System.out.println("IOT is already exists");
            return false;
        }
        statement = currentDBconnection.createStatement();
        ResultSet selectProductIDQueryData = statement.executeQuery("SELECT product_id FROM Products WHERE product_name = '" + productName + "'");
        selectProductIDQueryData.next();
        int productID = selectProductIDQueryData.getInt(1);
        int workerID = getLastInsertedIDFromTable("IOTOwners");

        Statement IOTDeviceStatement = currentDBconnection.createStatement();
        IOTDeviceStatement.executeUpdate("INSERT INTO IOTDevices (product_id, owner_id) VALUES (" + productID + ", " + workerID + ")");
        System.out.println("IOT data was inserted successfully");
        return true;
    }

    private void insertOwnerDetailsData(JSONObject ownerDetailsData) throws SQLException {
        String ownerName = (String) ownerDetailsData.get("owner_name");
        String ownerEmail = (String) ownerDetailsData.get("owner_email");
        String ownerPhone = (String) ownerDetailsData.get("owner_phone");

        //TODO check that this owner dose not exist

        InsertAdresses(ownerDetailsData);
        int addressesID = getLastInsertedIDFromTable("Addresses");

        insertCompanyPaymentsInfo(ownerDetailsData, false);
        int paymentID = getLastInsertedIDFromTable("PaymentInfo");

        Statement addressStatement = currentDBconnection.createStatement();
        addressStatement.executeUpdate("INSERT INTO IOTOwners (owner_name, owner_email, owner_phone, address_id, payment_id) VALUES('" +
                ownerName + "', '" + ownerEmail + "', '" + ownerPhone + "', " + addressesID + ", " + paymentID + ")");
    }

    private boolean updateIOTData(JSONObject data) throws SQLException {
        JSONObject IOTDetailsData = (JSONObject) data.get("iot_details");
        String IOTData = (String) IOTDetailsData.get("iot_data");
        Long deviceSerialNumber = (Long) IOTDetailsData.get("device_serial_number");

        if (!isDeviceSerialNumberExist(data, deviceSerialNumber)) {
            return false;
        }

        Statement IOTDeviceStatement = currentDBconnection.createStatement();
        IOTDeviceStatement.executeUpdate("INSERT INTO IOTData (iot_data, iot_timestamp, device_serial_number) VALUES ('" +
                IOTData + "', '" + new Timestamp(System.currentTimeMillis()) + "', " + deviceSerialNumber + ")");
        return true;
    }

    private boolean isDeviceSerialNumberExist(JSONObject data, Long deviceSerialNumber) throws SQLException {
        Statement selectDeviceSerialNumberStatement = currentDBconnection.createStatement();
        ResultSet selectDeviceSerialNumberQueryData = selectDeviceSerialNumberStatement.executeQuery("SELECT device_serial_number FROM IOTDevices WHERE device_serial_number = " + deviceSerialNumber);
        return selectDeviceSerialNumberQueryData.next();
    }
}