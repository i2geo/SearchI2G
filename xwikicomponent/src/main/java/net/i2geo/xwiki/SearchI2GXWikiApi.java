package net.i2geo.xwiki;

import net.i2geo.api.NamesMap;
import org.xwiki.component.phase.InitializationException;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Api;

import javax.servlet.http.HttpServletRequest;

import net.i2geo.api.search.UserQuery;
import net.i2geo.api.search.QueryExpansionResult;

/**
 */
public class SearchI2GXWikiApi extends Api
    {

        public SearchI2GXWikiApi(SearchI2GXWikiPlugin plugin, XWikiContext context) {
            super(context);
            this.plugin = plugin;
        }

        private SearchI2GXWikiPlugin plugin;

        public void initialize() throws InitializationException {

        }

        /** @deprecated */
        public String renderNodes(String[] uris, HttpServletRequest request) {
            return plugin.getImplementation().renderNodes(uris,null,request);
        }
        public String renderNodes(String[] uris, String languages, HttpServletRequest request) {
            return plugin.getImplementation().renderNodes(uris,languages, request);
        }

        public String[] getNodeParents(String uri, HttpServletRequest request) {
            return plugin.getImplementation().getNodeParents(uri,request);
        }

        public QueryExpansionResult expandUserQuery(UserQuery userQuery) {
            return plugin.getImplementation().expandUserQuery(userQuery);
        }

        public QueryExpansionResult expandSubjectQuery(String uq) {
            return plugin.getImplementation().expandSubjectQuery(uq);
        }

        public NamesMap listNames(String uri) {
            return plugin.getImplementation().listNames(uri);
        }

    }
