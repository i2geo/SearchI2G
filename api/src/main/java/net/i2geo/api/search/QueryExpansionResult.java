package net.i2geo.api.search;

import java.util.List;

/**
 */
public class QueryExpansionResult {


    private Object query;
    
    private List<String> messages;

    public Object getQuery() {
        return query;
    }

    public void setQuery(Object query) {
        this.query = query;
    }

    public List<String> getMessages() {
        return messages;
    }

    public void setMessages(List<String> messages) {
        this.messages = messages;
    }
}
