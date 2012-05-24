package net.i2geo.api;

//import rocket.remoting.client.JsonRpcService;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.RemoteService;
import net.i2geo.api.search.UserQuery;

public interface SkillsSearchServiceAsync extends RemoteService {
    
    void getSkillItem(String query, String acceptedLangs, AsyncCallback callback);

    void renderItem(String itemText, String acceptedLangs, AsyncCallback callback);

    void searchSkillItem(String queryString, String[] authorizedTypes, String acceptedLangs, AsyncCallback callback);

    void log(String itemText, AsyncCallback callback);

    void getNodeParents(String uri, AsyncCallback callback);

}
