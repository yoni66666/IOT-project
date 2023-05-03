package il.co.ilrd.multiprotocolserver;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Pattern;

public interface IOTValidator {
    public boolean validate(JSONObject jsonObject);
}

class JsonValidator {
    public static boolean isValidId(String id){
        //return (0 < Integer.parseInt(id));
        return true;
    }

    public static boolean checkValidString(Object... values) {
        for (Object value : values) {
            if (!(value instanceof String)) {
                return false;
            }
        }
        return true;
    }
    public static boolean isValidEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        Pattern pat = Pattern.compile(emailRegex);
        if (email == null)
            return false;
        return pat.matcher(email).matches();
    }

    public static boolean isValidPhoneNumber(Object phoneNumber) {
        if(JsonValidator.checkValidString(phoneNumber)) {
            String phoneNumberRegex = "^\\(?([0-9]{3})\\)?[-.\\s]?([0-9]{3})[-.\\s]?([0-9]{4})$";
            Pattern pat = Pattern.compile(phoneNumberRegex);
            if (phoneNumber == null)
                return false;
            return pat.matcher((String)phoneNumber).matches();
        }
        return false;
    }

    public static boolean isValidPayment(JSONArray paymentsInfo){

        try {
            for (Object paymentObj : paymentsInfo) {
                JSONObject json = (JSONObject) paymentObj;
                JsonValidator.checkValidString(json.get("expiration_date"), json.get("credit_card"), json.get("cvv"));
                if(!JsonValidator.checkValidString(json.get("expiration_date"), json.get("credit_card"), json.get("cvv"))){
                    return false;
                }
                String creditCard = (String) (json.get("credit_card"));
                String cvv = (String) (json.get("cvv"));
                if(!creditCard.matches("^\\d{8,16}$")||
                        !isValidDate((String) json.get("expiration_date"))||
                        !cvv.matches("^\\d{3}$") )  {
                    return false;
                }
            }
        }catch (NullPointerException npe){
            return false;
        }
        return true;
    }
    public static boolean isValidAddress(JSONObject json){
        try {

            if(JsonValidator.checkValidString(json.get("country"),
                    json.get("city"), json.get("street"), json.get("postal_code")) &&
                    (isContainsOnlyDigits((String) (json.get("postal_code"))))){
                return true;
            }
        }catch (NullPointerException npe){
            return false;
        }
        return false;
    }
    private static boolean isContainsOnlyDigits(String value){
        return value.matches("^[0-9]+$");
    }



    private static boolean isValidDate(String dateString){

        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/yy");
        dateFormat.setLenient(false); // set to strict parsing
        try {
            Date expirationDate = dateFormat.parse(dateString);
            if (expirationDate.after(new Date())) {
                return true;
            } else {
                return false;
            }
        } catch (ParseException |NullPointerException e) {
            return false;
        }
    }



}

class CompanyValidator implements IOTValidator{

    @Override
    public boolean validate(JSONObject jsonObject) {
        int numOfErrors = 0;
        numOfErrors +=  JsonValidator.checkValidString(jsonObject.get("company_name"),
                jsonObject.get("username"), jsonObject.get("password"))? 0 : 1;

        numOfErrors += isValidContacts((JSONArray) jsonObject.get("company_contacts"))?0:1;
        numOfErrors += isValidAddress((JSONObject) jsonObject.get("address"))?0:1;

        return numOfErrors == 0;

    }
    private boolean isValidContacts(JSONArray paymentsInfo){
        try {
            for (Object contactObj : paymentsInfo) {
                JSONObject contact = (JSONObject) contactObj;

                if( !JsonValidator.checkValidString(contact.get("contact_name"),
                        contact.get("contact_email"), contact.get("contact_phone")) ||
                        !JsonValidator.isValidEmail((String) contact.get("contact_email")) ||
                        !JsonValidator.isValidPhoneNumber(contact.get("contact_phone"))){
                    return false;
                }
            }
        }catch (NullPointerException npe){
            return false;
        }
        return true;
    }


    private boolean isValidEmail(Object email) {
        if(JsonValidator.checkValidString(email)) {
            String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
            Pattern pat = Pattern.compile(emailRegex);
            return pat.matcher((String)email).matches();
        }
        return false;
    }

    private boolean isValidPhoneNumber(Object phoneNumber) {
        if(JsonValidator.checkValidString(phoneNumber)) {
            String phoneNumberRegex = "^\\(?([0-9]{3})\\)?[-.\\s]?([0-9]{3})[-.\\s]?([0-9]{4})$";
            Pattern pat = Pattern.compile(phoneNumberRegex);
            return pat.matcher((String)phoneNumber).matches();
        }
        return false;
    }

    private boolean isValidAddress(JSONObject json){
        int numOfErrors = 0;

        numOfErrors += JsonValidator.checkValidString(json.get("country"),
                json.get("city"), json.get("street"), json.get("postal_code"))?0:1;

        numOfErrors += ( ((String)json.get("postal_code")).matches("^[0-9]+$"))?0:1;

        return numOfErrors == 0;
    }
}

class ProductValidator implements IOTValidator{
    @Override
    public boolean validate(JSONObject jsonObject) {

        if(JsonValidator.checkValidString(jsonObject.get("company_name"),
                jsonObject.get("username"), jsonObject.get("password"), jsonObject.get("product_name")) &&
                isValidSpecification((JSONObject)jsonObject.get("product_specification"))){
            return true;
        }

        return false;
    }
    private boolean isValidSpecification(JSONObject json){
        try {
            return (json.get("product_price") instanceof Double &&
                    json.get("product_weight") instanceof Double &&
                    JsonValidator.checkValidString(json.get("product_category")));
        }catch (NullPointerException npe){
            return false;
        }
    }
}

class IOTDeviceValidator implements IOTValidator{

    @Override
    public boolean validate(JSONObject jsonObject) {
        if(!JsonValidator.checkValidString(jsonObject.get("company_name"),
                jsonObject.get("username"), jsonObject.get("password"),
                jsonObject.get("product_name"))){
            return false;
        }

        JSONObject ownerDetails = (JSONObject) jsonObject.get("owner_details");
        if(ownerDetails == null){
            return false;
        }
        if(JsonValidator.isValidPayment((JSONArray) ownerDetails.get("payments_info")) &&
                JsonValidator.isValidAddress((JSONObject) ownerDetails.get("address"))&&
                isValidOwnerDetails(ownerDetails)){
            return true;
        }
        return false;
    }
    private boolean isValidOwnerDetails (JSONObject json){

        try {
            if(JsonValidator.checkValidString(json.get("owner_name"),
                    json.get("owner_email"), json.get("owner_phone")) &&
                    JsonValidator.isValidEmail((String) json.get("owner_email")) &&
                    JsonValidator.isValidPhoneNumber((String) json.get("owner_phone"))){
                return true;
            }
        }catch (NullPointerException npe){
            return false;
        }
        return false;
    }
}

class IOTUpdateValidator implements IOTValidator{

    @Override
    public boolean validate(JSONObject jsonObject) {
        if(JsonValidator.checkValidString(jsonObject.get("company_name"),
                jsonObject.get("username"), jsonObject.get("password"))
               /* && isValidIOTtDetails((JSONObject) jsonObject.get("iot_details"))*/
        ){
            return true;
        }
        return false;
    }

    private boolean isValidIOTtDetails(JSONObject json){
        try {
            if( JsonValidator.checkValidString(json.get("iot_timestamp"),
                    json.get("iot_data")) && json.get("device_serial_number") instanceof Long
                    &&isValidTimestamp((String) json.get("iot_timestamp"))){
                return true;
            }
        }catch (NullPointerException npe){
            return false;
        }
        return false;
    }

    private boolean isValidTimestamp(String timestampString){

        SimpleDateFormat timestampFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            Date timestamp = timestampFormat.parse(timestampString);
            if (timestamp != null) {
                return true;
            } else {
                return false;
            }
        } catch (ParseException | NullPointerException e) {
            return false;
        }
    }
}
class GetCompanyValidator implements IOTValidator {

    @Override
    public boolean validate(JSONObject jsonObject) {
        /*
        String companyId = (String) jsonObject.get("company_id");
        if(0 < Integer.parseInt(companyId)){
            return true;
        }
        return false;

         */
        return true;
    }
}
class GetProductValidator implements IOTValidator {

    @Override
    public boolean validate(JSONObject jsonObject) {
        String companyId = (String) jsonObject.get("company_id");
        String productId = (String) jsonObject.get("product_id");

        if(companyId != null && JsonValidator.isValidId(companyId)) {
            if (productId == null || JsonValidator.isValidId(productId)) {
                return true;
            }
        }
        return false;
    }
}
class GetIOTDeviceValidator implements IOTValidator {

    @Override
    public boolean validate(JSONObject jsonObject) {

        String iotDeviceId = (String) jsonObject.get("iotDevice_id");
        String companyId = (String) jsonObject.get("company_id");
        String productId = (String) jsonObject.get("product_id");

        if(companyId!=null && productId != null && JsonValidator.isValidId(companyId)
                && JsonValidator.isValidId(productId)){
                return true;
        }
        return false;
    }
}
class GetIOTUpdateValidator implements IOTValidator {
    @Override
    public boolean validate(JSONObject jsonObject) {
        String iotDeviceId = (String) jsonObject.get("iotDevice_id");
        String companyId = (String) jsonObject.get("company_id");
        String lastNumOfUpdates  = (String) jsonObject.get("last_num_of_updates");

        if(iotDeviceId != null && companyId != null && lastNumOfUpdates != null
                && JsonValidator.isValidId(iotDeviceId) && JsonValidator.isValidId(companyId)&&
                0 <= Integer.parseInt(lastNumOfUpdates)){
            return true;
        }
        return false;
    }
}


