package il.co.ilrd.multiprotocolserver;

import org.json.simple.JSONObject;

public class RegisterIOT implements IOTCommand{

    @Override
    public boolean runCommand(JSONObject jsonObject, IOTDatabaseManager manager) {
        return manager.registerIOT(jsonObject);
    }
}
