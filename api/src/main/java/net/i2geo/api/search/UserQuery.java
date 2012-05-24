package net.i2geo.api.search;

import java.util.List;

/** Simple data object representing a user query.
 */
public class UserQuery {

    public UserQuery(String terms, UserQuery.Level level, List<String> languages) {
        this.terms = terms;
        this.level= level;
        this.languages = languages;
    }

    private String terms;
    private List<String> languages;
    private UserQuery.Level level;

    public String getTerms() {
        return terms;
    }

    public void setTerms(String terms) {
        this.terms = terms;
    }

    public UserQuery.Level getLevel() {
        return level;
    }

    public static enum Level {
        ZERO, LITTLE, FULL;
    }

    public List<String> getLanguages() {
        return languages;
    }
}
