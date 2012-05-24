package net.i2geo.api.search;

/** tiny interface to link the function of query expansion from xwiki to SearchI2G
 */
public interface QueryExpander {

    public QueryExpansionResult expandUserQuery(UserQuery query);


    public QueryExpansionResult expandSubjectQuery(String query);

}
