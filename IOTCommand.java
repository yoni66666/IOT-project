package il.co.ilrd.multiprotocolserver;

import org.json.simple.JSONObject;

public interface IOTCommand {
    public boolean runCommand(JSONObject obj, IOTDatabaseManager manager);

}
