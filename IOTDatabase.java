package il.co.ilrd.multiprotocolserver;

import org.json.simple.JSONObject;

public interface IOTDatabase {
    public abstract boolean registerCompany(JSONObject o);
    public abstract boolean registerProduct(JSONObject o);
    public abstract boolean registerIOT(JSONObject o);
    public abstract boolean updateIOT(JSONObject o);
}
