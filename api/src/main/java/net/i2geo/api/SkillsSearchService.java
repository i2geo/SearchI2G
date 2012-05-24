package net.i2geo.api;

//import rocket.messaging.client.Payload;
//import rocket.remoting.client.JsonRpcService;
//import rocket.messaging.client.Payload;

import com.google.gwt.user.client.rpc.RemoteService;
import net.i2geo.api.SkillItem;
import net.i2geo.api.search.UserQuery;

//import java.util.HashSet;
//import java.util.Set;

public interface SkillsSearchService extends RemoteService {
    
    /**
     * @jsonRpc-inputArguments requestParameters
     * @jsonRpc-httpMethod POST
     * @jsonRpc-parameterName query
     */
    
    SkillItem[] getSkillItem(String query, String acceptedLangs);


    /**
     * @jsonRpc-inputArguments requestParameters
     * @jsonRpc-httpMethod POST
     * @jsonRpc-parameterName queryString
     */
    SkillItem[] searchSkillItem(String queryString, String[] authorizedTypes, String acceptedLangs);


    /**
     * @jsonRpc-inputArguments requestParameters
     * @jsonRpc-httpMethod POST
     * @jsonRpc-parameterName query
     */
    SkillItem renderItem(String uri, String acceptedLangs);

    /**
     * @jsonRpc-inputArguments requestParameters
     * @jsonRpc-httpMethod POST
     * @jsonRpc-parameterName query
     */
    String[] getNodeParents(String uri);

    /**
     * @jsonRpc-inputArguments requestParameters
     * @jsonRpc-httpMethod POST
     * @jsonRpc-parameterName query
     */
    void log(String itemText);

}
