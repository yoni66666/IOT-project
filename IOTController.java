package il.co.ilrd.multiprotocolserver;

import il.co.ilrd.threadpool.ThreadPool;
import org.json.simple.JSONObject;

import java.util.concurrent.Callable;

public interface IOTController {
    public JSONObject control(JSONObject jsonObject, IOTDatabaseManager databaseManager, ThreadPool threadPool);
}


    class PostCompanyController implements IOTController{
        @Override
        public JSONObject control(JSONObject jsonObject, IOTDatabaseManager databaseManager, ThreadPool threadPool) {

            String command = (String) jsonObject.get("iot_command");

            Callable<Boolean> callable = () -> (SingletonPlugAndPlayFactory.getInstance().create(command).runCommand(jsonObject, databaseManager));
            try {
                threadPool.submit(callable);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return null;
        }
    }

    class PostProductController implements IOTController{

        @Override
        public JSONObject control(JSONObject jsonObject, IOTDatabaseManager databaseManager, ThreadPool threadPool) {

            String command = (String) jsonObject.get("iot_command");

            Callable<Boolean> callable = () -> (SingletonPlugAndPlayFactory.getInstance().create(command).runCommand(jsonObject, databaseManager));
            try {
                threadPool.submit(callable);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return null;
        }
    }

    class PostIOTDeviceController implements IOTController{

        @Override
        public JSONObject control(JSONObject jsonObject, IOTDatabaseManager databaseManager, ThreadPool threadPool) {

            String command = (String) jsonObject.get("iot_command");

            Callable<Boolean> callable = () -> (SingletonPlugAndPlayFactory.getInstance().create(command).runCommand(jsonObject, databaseManager));
            try {
                threadPool.submit(callable);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return null;
        }
    }

    class PostIOTUpdateController implements IOTController {

        @Override
        public JSONObject control(JSONObject jsonObject, IOTDatabaseManager databaseManager, ThreadPool threadPool) {

            System.out.println("PostIOTUpdateController");
            String command = (String) jsonObject.get("iot_command");

            Callable<Boolean> callable = () -> (SingletonPlugAndPlayFactory.getInstance().create(command).runCommand(jsonObject, databaseManager));
            try {
                threadPool.submit(callable);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return null;
        }
    }

    class GetCompaniesController implements IOTController {

        @Override
        public JSONObject control(JSONObject jsonObject, IOTDatabaseManager databaseManager,
                                  ThreadPool threadPool) {
            return databaseManager.getCompanies();
        }
    }

    class GetSpecificCompanyController implements IOTController {
        private String companyId;

        public GetSpecificCompanyController(String companyId) {
            this.companyId = companyId;
        }

        @Override
        public JSONObject control(JSONObject jsonObject, IOTDatabaseManager databaseManager,
                                  ThreadPool threadPool) {
            return databaseManager.getCompany(companyId);
        }
    }

    class GetProductsController implements IOTController {
        private String companyId;

        public GetProductsController(String companyId) {
            this.companyId = companyId;
        }

        @Override
        public JSONObject control(JSONObject jsonObject, IOTDatabaseManager databaseManager, ThreadPool threadPool) {
            return databaseManager.getProducts(companyId);
        }
    }

    class GetSpecificProductController implements IOTController {
        private String companyId;
        private String productId;

        public GetSpecificProductController(String companyId, String productId) {
            this.companyId = companyId;
            this.productId = productId;
        }

        @Override
        public JSONObject control(JSONObject jsonObject, IOTDatabaseManager databaseManager, ThreadPool threadPool) {
            return databaseManager.getProduct(companyId, productId);
        }
    }

    class GetIOTDevicesController implements IOTController {

        private String companyId;
        private String productId;

        public GetIOTDevicesController(String companyId, String productId) {
            this.companyId = companyId;
            this.productId = productId;
        }

        @Override
        public JSONObject control(JSONObject jsonObject, IOTDatabaseManager databaseManager,
                                  ThreadPool threadPool) {
            return databaseManager.getIOTDevices(companyId, productId);
        }
    }

    class GetSpecificIOTDevice implements IOTController {

        private String companyId;
        private String deviceSerialNumber;

        public GetSpecificIOTDevice(String companyId, String deviceSerialNumber) {
            this.companyId = companyId;
            this.deviceSerialNumber = deviceSerialNumber;
        }

        @Override
        public JSONObject control(JSONObject jsonObject, IOTDatabaseManager databaseManager,
                                  ThreadPool threadPool) {
            return databaseManager.getIOTDevice(companyId, deviceSerialNumber);
        }
    }

    class GetIOTUpdateController implements IOTController {

        private String companyId;
        private String deviceSerialNumber;
        private int lastNumOfUpdates;

        public GetIOTUpdateController(String companyId, String deviceSerialNumber, int lastNumOfUpdates) {
            this.companyId = companyId;
            this.deviceSerialNumber = deviceSerialNumber;
            this.lastNumOfUpdates = lastNumOfUpdates;
        }

        @Override
        public JSONObject control(JSONObject jsonObject, IOTDatabaseManager databaseManager,
                                  ThreadPool threadPool) {
            return databaseManager.getIOTUpdate(companyId, deviceSerialNumber, lastNumOfUpdates);
        }

    }
