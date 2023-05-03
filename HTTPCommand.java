package il.co.ilrd.multiprotocolserver;

import com.sun.net.httpserver.HttpExchange;
import il.co.ilrd.threadpool.ThreadPool;
import org.json.simple.JSONObject;

import java.io.IOException;

public interface HTTPCommand {
    public void execute(JSONObject jsonObject, IOTDatabaseManager databaseManager,
                        ThreadPool threadPool, HttpExchange httpExchange);
}

class PostCompanyCommand implements HTTPCommand{
    private CompanyValidator companyValidator;
    private PostCompanyController companyController;
    public PostCompanyCommand(IOTValidator companyValidator, IOTController companyController) {
        this.companyValidator =(CompanyValidator) companyValidator;
        this.companyController =  (PostCompanyController)companyController;
    }

    @Override
    public void execute(JSONObject jsonObject, IOTDatabaseManager databaseManager,
                        ThreadPool threadPool, HttpExchange httpExchange){
        ResponseStatus responseStatus;
        JSONObject jsonRes = new JSONObject();

        if(companyValidator.validate(jsonObject)) {
            companyController.control(jsonObject, databaseManager, threadPool);
            responseStatus = ResponseStatus.OK;
            HttpResponse.sendResponse(httpExchange, jsonObject,responseStatus);
        }
        else {
            responseStatus = ResponseStatus.MESSAGE_ERROR;
            jsonRes.put("error", "company registration failed");
            HttpResponse.sendResponse(httpExchange, jsonRes,responseStatus);
        }
    }

}

class PostProductCommand implements HTTPCommand{
    private ProductValidator productValidator;
    private PostProductController productController;
    public PostProductCommand(IOTValidator productValidator, IOTController productController) {
        this.productValidator =(ProductValidator) productValidator;
        this.productController =  (PostProductController)productController;
    }

    @Override
    public void execute(JSONObject jsonObject, IOTDatabaseManager databaseManager,
                        ThreadPool threadPool,HttpExchange httpExchange){

        ResponseStatus responseStatus;
        JSONObject jsonRes = new JSONObject();
        if(productValidator.validate(jsonObject)) {
            productController.control(jsonObject, databaseManager, threadPool);
            responseStatus = ResponseStatus.OK;
            HttpResponse.sendResponse(httpExchange,jsonObject, responseStatus);
        }
        else {
            responseStatus = ResponseStatus.MESSAGE_ERROR;
            jsonRes.put("error", "product registration failed");
            HttpResponse.sendResponse(httpExchange,jsonRes, responseStatus);
        }
    }
}

class PostIOTDeviceCommand implements HTTPCommand{
    private IOTDeviceValidator iotDeviceValidator;
    private PostIOTDeviceController iotDeviceController;

    public PostIOTDeviceCommand(IOTValidator iotDeviceValidator, IOTController iotDeviceController) {
        this.iotDeviceValidator =(IOTDeviceValidator) iotDeviceValidator;
        this.iotDeviceController =  (PostIOTDeviceController)iotDeviceController;
    }

    @Override
    public void execute(JSONObject jsonObject, IOTDatabaseManager databaseManager,
                        ThreadPool threadPool, HttpExchange httpExchange){
        ResponseStatus responseStatus;
        JSONObject jsonRes = new JSONObject();

        if(iotDeviceValidator.validate(jsonObject)) {
            iotDeviceController.control(jsonObject, databaseManager,threadPool);
            responseStatus = ResponseStatus.OK;
            HttpResponse.sendResponse(httpExchange,jsonObject, responseStatus);
        }
        else {
            responseStatus = ResponseStatus.MESSAGE_ERROR;
            jsonRes.put("error", "iotDevice registration failed");
        }

    }
}


class PostIOTUpdateCommand implements HTTPCommand {
    private IOTUpdateValidator iotUpdateValidator;
    private PostIOTUpdateController iotUpdateController;

    public PostIOTUpdateCommand(IOTValidator iotUpdateValidator, IOTController iotUpdateController) {
        this.iotUpdateValidator = (IOTUpdateValidator) iotUpdateValidator;
        this.iotUpdateController = (PostIOTUpdateController) iotUpdateController;
    }

    @Override
    public void execute(JSONObject jsonObject, IOTDatabaseManager databaseManager,
                        ThreadPool threadPool, HttpExchange httpExchange) {

        ResponseStatus responseStatus;
        JSONObject jsonRes = new JSONObject();
        if( iotUpdateValidator.validate(jsonObject)){
            iotUpdateController.control(jsonObject, databaseManager,threadPool);
            responseStatus = ResponseStatus.OK;
            HttpResponse.sendResponse(httpExchange, jsonObject, responseStatus);
        }
        else {
            responseStatus = ResponseStatus.MESSAGE_ERROR;
            jsonRes.put("error", "iotUpdate failed");
            HttpResponse.sendResponse(httpExchange, jsonRes, responseStatus);
        }
    }
}

class GetCompanyCommand implements HTTPCommand {

    GetCompanyValidator validator;
    public GetCompanyCommand(IOTValidator validator){
        this.validator = (GetCompanyValidator) validator;
    }

    @Override
    public void execute(JSONObject jsonObject, IOTDatabaseManager databaseManager,
                        ThreadPool threadPool, HttpExchange httpExchange) {

        JSONObject jsonRes = null;
        String companyId = (String) httpExchange.getAttribute("company_id");

        jsonObject.put("company_id", companyId);

        if(companyId != null && !validator.validate(jsonObject)){
            JSONObject jsonError = new JSONObject();
            jsonError.put("error", ResponseStatus.MESSAGE_ERROR);
            HttpResponse.sendResponse(httpExchange, jsonError, ResponseStatus.MESSAGE_ERROR);
            return;
        }
        if(companyId == null){
            jsonRes = new GetCompaniesController().control(jsonObject, databaseManager, threadPool);
        }
        else {
            jsonRes = new GetSpecificCompanyController(companyId).control(jsonObject, databaseManager,threadPool);
        }

        HttpResponse.sendResponse(httpExchange, jsonRes, HttpResponse.checkReturnedJson(jsonRes));
    }
}

class GetProductCommand implements HTTPCommand {

    private GetProductValidator validator;
    public GetProductCommand(IOTValidator validator){
        this.validator = (GetProductValidator) validator;
    }
    @Override
    public void execute(JSONObject jsonObject, IOTDatabaseManager databaseManager,
                        ThreadPool threadPool, HttpExchange httpExchange) {

        JSONObject jsonRes = new JSONObject();
        String companyId = (String) httpExchange.getAttribute("company_id");
        String productId = (String) httpExchange.getAttribute("product_id");
        jsonObject.put("company_id", companyId);
        jsonObject.put("product_id", productId);

        if(!validator.validate(jsonObject)){
            jsonRes.put("error", ResponseStatus.MESSAGE_ERROR);
            HttpResponse.sendResponse(httpExchange,jsonRes, ResponseStatus.MESSAGE_ERROR);
            return;
        }

        if(productId == null){
            jsonRes = new GetProductsController(companyId).control(jsonObject, databaseManager, threadPool);
        }
        else {
            jsonRes =new GetSpecificProductController(companyId, productId).control(jsonObject,
                    databaseManager, threadPool);
        }
        HttpResponse.sendResponse(httpExchange, jsonRes, HttpResponse.checkReturnedJson(jsonRes));
    }
}

class GetIOTDeviceCommand implements HTTPCommand {
    private GetIOTDeviceValidator validator;
    public GetIOTDeviceCommand(IOTValidator validator){
        this.validator = (GetIOTDeviceValidator) validator;
    }

    @Override
    public void execute(JSONObject jsonObject, IOTDatabaseManager databaseManager,
                        ThreadPool threadPool, HttpExchange httpExchange) {

        JSONObject jsonRes = new JSONObject();
        String iotDeviceId = (String) httpExchange.getAttribute("iotDevice_id");
        String companyId = (String) httpExchange.getAttribute("company_id");
        String productId = (String) httpExchange.getAttribute("product_id");

        jsonObject.put("iotDevice_id", iotDeviceId);
        jsonObject.put("company_id", companyId);
        jsonObject.put("product_id", productId);

        if(!validator.validate(jsonObject)){
            jsonRes.put("error", ResponseStatus.MESSAGE_ERROR);
            HttpResponse.sendResponse(httpExchange,jsonRes, ResponseStatus.MESSAGE_ERROR);
            return;
        }

        if(iotDeviceId == null){
            jsonRes =new GetIOTDevicesController(companyId, productId).control(jsonObject, databaseManager, threadPool);
        }
        else {
            jsonRes =new GetSpecificIOTDevice(companyId,iotDeviceId).control(jsonObject,databaseManager,threadPool);
        }
        HttpResponse.sendResponse(httpExchange,jsonRes, HttpResponse.checkReturnedJson(jsonRes));
    }

}

class GetIOTUpdateCommand implements HTTPCommand {

    private GetIOTUpdateValidator validator;
    public GetIOTUpdateCommand(IOTValidator validator){
        this.validator = (GetIOTUpdateValidator) validator;
    }
    @Override
    public void execute(JSONObject jsonObject, IOTDatabaseManager databaseManager,
                        ThreadPool threadPool, HttpExchange httpExchange) {

        JSONObject jsonRes = new JSONObject();
        String iotDeviceId = (String) httpExchange.getAttribute("iotDevice_id");
        String companyId = (String) httpExchange.getAttribute("company_id");
        String lastNumOfUpdates  = (String) httpExchange.getAttribute("last_num_of_updates");

        jsonObject.put("iotDevice_id", iotDeviceId);
        jsonObject.put("company_id", companyId);
        jsonObject.put("last_num_of_updates", lastNumOfUpdates);

        if(!validator.validate(jsonObject)){
            jsonRes.put("error", ResponseStatus.MESSAGE_ERROR);
            HttpResponse.sendResponse(httpExchange,jsonRes,ResponseStatus.MESSAGE_ERROR);
            return;
        }
        jsonRes =  new GetIOTUpdateController(companyId, iotDeviceId,Integer.parseInt(lastNumOfUpdates)).
                control(jsonObject, databaseManager, threadPool);
        HttpResponse.sendResponse(httpExchange,jsonRes,HttpResponse.checkReturnedJson(jsonRes));
    }
}


class HttpResponse{
    public static void sendResponse(HttpExchange httpExchange, JSONObject json, ResponseStatus status){
        String response = json.toJSONString();
        try {
            httpExchange.sendResponseHeaders(status.getValue(), response.length());
            httpExchange.getResponseBody().write(response.getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        httpExchange.close();
    }

    public static ResponseStatus checkReturnedJson(JSONObject jsonRes){
        String value;
        if((value = (String) jsonRes.get("error"))!= null){
            if(value.equals("Error while retrieving data")){
                return ResponseStatus.MESSAGE_ERROR;
            }
            else {
                return ResponseStatus.INTERNAL_SERVER_ERROR;
            }
        }
        return ResponseStatus.OK;
    }
}

enum ResponseStatus {
    OK (200),
    MESSAGE_ERROR (404),
    INTERNAL_SERVER_ERROR (500);
    private final int value;
    ResponseStatus(final int newValue) {
        value = newValue;
    }
    public int getValue() { return value; }
}





