package il.co.ilrd.multiprotocolserver;
import java.util.HashMap;

public interface BaseRouter {
    public HTTPCommand route(String url, String command);
}

class RecRouter implements BaseRouter {
    private static HashMap<String, BaseRouter> map = new HashMap<>();
    private EndPointRouter endPointRouter = null;

    public void use(String nextRoute, BaseRouter router) {

        if(nextRoute.equals("/")){
            endPointRouter = (EndPointRouter) router;
        }
        else {
            map.put(nextRoute,router);
        }
    }
    @Override
    public HTTPCommand route(String routeString, String command) {

        if(routeString.length() > 1) {
            String firstWord = getFirstWord(routeString);
            String restRoute = getRemainderOfUrl(routeString);
            return map.get(firstWord).route(restRoute, command);
        }
        return endPointRouter.route(routeString, command);
    }

    private String getRemainderOfUrl(String routeString){
        int index = routeString.indexOf("/");
        int nextIndex = routeString.indexOf("/", index + 1);
        return routeString.substring(nextIndex);
    }
    private String getFirstWord(String routeString){
        int index = routeString.indexOf("/");
        int nextIndex = routeString.indexOf("/", index + 1);
        return routeString.substring(index +1, nextIndex);
    }
}

class EndPointRouter implements BaseRouter{
    private HashMap<String, HTTPCommand> routersMap = new HashMap<>();
    public void addHTTPCommand(String method, HTTPCommand commandHandler) {
        routersMap.put(method, commandHandler);
    }

    @Override
    public HTTPCommand route(String route, String command) {
        HTTPCommand httpCommand = routersMap.get(command);
        return httpCommand;
    }
}