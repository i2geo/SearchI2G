package net.i2geo.index.rsearch;

import net.i2geo.api.SKBServiceContext;
import net.i2geo.api.search.UserQuery;

import java.util.List;

/**
 */
public class RSearchContext {

    public RSearchContext(List<String> languages) {
        this.languages = languages;
    }

    public RSearchContext(UserQuery query) {
        this.languages = query.getLanguages();
    }


    private List<String> languages;

    public List<String> getAcceptedLanguages() {
        return languages;
    }
}
