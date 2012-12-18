package net.i2geo.xwiki;

import net.i2geo.api.NamesMap;
import org.xwiki.component.phase.Initializable;

import javax.servlet.http.HttpServletRequest;

import net.i2geo.api.search.UserQuery;
import net.i2geo.api.search.QueryExpansionResult;

/**
 * @version $Id: $
 */
public interface SearchI2GXWiki extends Initializable
{
    /** The role associated with the component. */
    String ROLE = SearchI2GXWiki.class.getName();
    
    public String renderNodes(String[] uris, String languages, HttpServletRequest request);

    public String[] getNodeParents(String uris, HttpServletRequest request);

    public QueryExpansionResult expandUserQuery(UserQuery query);

    public QueryExpansionResult expandSubjectQuery(String uq);

    public NamesMap listNames(String uri);
}

