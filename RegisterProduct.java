package il.co.ilrd.multiprotocolserver;

import org.json.simple.JSONObject;

public class RegisterProduct implements IOTCommand {

    @Override
    public boolean runCommand(JSONObject obj, IOTDatabaseManager manager) {
        return manager.registerProduct(obj);
    }
}
