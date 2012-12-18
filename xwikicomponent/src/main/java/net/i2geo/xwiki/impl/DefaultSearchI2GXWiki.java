package net.i2geo.xwiki.impl;



import net.i2geo.api.NamesMap;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import com.xpn.xwiki.web.XWikiServletContext;
import net.i2geo.xwiki.SearchI2GXWiki;
import net.i2geo.xwiki.SearchI2GXWikiPlugin;
import net.i2geo.api.*;
import net.i2geo.api.search.UserQuery;
import net.i2geo.api.search.QueryExpander;
import net.i2geo.api.search.QueryExpansionResult;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.XWikiContext;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.ServletContext;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Concrete implementation of a <tt>SearchI2GXWiki</tt> component.  
 The component is configured via the Plexus container.
 *
 * @version $Id: $
 */
public class DefaultSearchI2GXWiki extends Api implements SearchI2GXWiki, Initializable
{

    public DefaultSearchI2GXWiki(SearchI2GXWikiPlugin plugin, XWikiContext context) {
        super(context);
    }

    private boolean initialized = false;
    private SkillsSearchService skillsSearchService = null;
    private QueryExpander expander = null;

    private static final String SKBLangs_ATTKEY = "SKBLangs",
        SKBService_ATTKEY = SKBServiceContext.class.getName(),
        SKBLangsLastModif_ATTKEY = "Last Modified: " + SKBServiceContext.class.getName(),
        QueryExpander_ATTKEY=QueryExpander.class.getName();

    public void initialize() throws InitializationException {
        this.initialized = false;
        makeSureServiceIsInitted(((XWikiServletContext) context.getEngineContext()).getServletContext());
        System.out.println("XWiki Search I2G is initted from: " + this.getClass().getResource("DefaultSearchI2GXWiki.class"));
    }

    public void makeSureServiceIsInitted(HttpServletRequest request) {
        if(request==null) return;
        makeSureServiceIsInitted(request.getSession().getServletContext());
    }

    public void makeSureServiceIsInitted(ServletContext c) {
        if(c == null) return;
        skillsSearchService = (SkillsSearchService)
                c.getContext("/SearchI2G").getAttribute(SkillsSearchService.class.getName());
        MatchingResourcesCounter counter = (MatchingResourcesCounter) c.getAttribute(MatchingResourcesCounter.class.getName());
        // TODO: init in the bootstrap in this webapp
        // TODO: adjust the query to prefer the ones with non-zero
        // TODO: adjust the oracle (but not today) to display the number
        c.getContext("/SearchI2G").setAttribute(MatchingResourcesCounter.class.getName(),counter);
        expander = (QueryExpander)
                c.getContext("/SearchI2G").getAttribute(QueryExpander_ATTKEY);
        if(skillsSearchService==null) System.err.println("Warning: SearchI2G SkillsSearchService is not found.");
    }

    public Map<String,Object> listServices() {
        Map<String,Object> l = new HashMap<String,Object>();
        if(expander!=null) l.put("expander",expander);
        if(skillsSearchService!=null) l.put("skillsSearchService",skillsSearchService);
        return l;
    }


    /** Responsible of setting the session attributes necessary for a proper rendering to happen;
     * except for a string comparison, this method only runs at start and every 5 seconds. */
    public void prepareSessionAttributes(HttpServletRequest request, String languages) {
        try {
            Date lastCheckedContext = (Date)
                    request.getSession().getAttribute(SKBLangsLastModif_ATTKEY);

            // we cache the result of this computation because there may be pages when renderItem is called 100 a times
            if(lastCheckedContext!=null && lastCheckedContext.getTime()-System.currentTimeMillis()> 1000) {
                request.getSession().getServletContext().setAttribute(SKBService_ATTKEY,null);
            }
            if(request.getSession().getServletContext().getAttribute(SKBService_ATTKEY)==null) {
                request.getSession().setAttribute(SKBLangs_ATTKEY,languages);
                SKBServiceContextProvider serviceProvider = (SKBServiceContextProvider) request.getSession().getServletContext().getContext("/SearchI2G")
                        .getAttribute(SKBServiceContextProvider.class.getName());
                SKBServiceContext serviceContext = serviceProvider.tryToGetOrMakeServiceForLangs(languages);
                request.getSession().setAttribute(SKBService_ATTKEY, serviceContext);
                lastCheckedContext = new Date();
                request.getSession().setAttribute(SKBLangsLastModif_ATTKEY,lastCheckedContext);
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    public String renderNodes(String[] uris, String languages, HttpServletRequest request) {

        StringBuffer buff = new StringBuffer(64);
        makeSureServiceIsInitted(request);
        prepareSessionAttributes(request,languages);
        SKBRenderer renderer = new SKBRenderer((SKBServiceContext) request.getSession().getAttribute(SKBService_ATTKEY));
        //String langs = (String) request.getSession().getAttribute(SKBLangs_ATTKEY);
        for(String uri:uris) {
            try {
                SkillItem item = skillsSearchService.renderItem(uri,languages);
                if(item!=null)
                buff.append(renderer.render(item));
            } catch (Exception e) {
                e.printStackTrace();
                // but don't do anything else
            }
        }
        return buff.toString();
    }
    public String renderNodes(String uris, String languages, HttpServletRequest request) {
        String[] urisArray = uris.split(",| ");
        return renderNodes(urisArray, languages, request);
    }

    public String[] getNodeParents(String url, HttpServletRequest request) {
        makeSureServiceIsInitted(request);
        return skillsSearchService.getNodeParents(url);
    }

    public QueryExpansionResult expandUserQuery(UserQuery uq) {
        return expander.expandUserQuery(uq);
    }

    public QueryExpansionResult expandSubjectQuery(String uq) {
        return expander.expandSubjectQuery(uq);
    }

    public NamesMap listNames(String uri) {
        return (NamesMap) invokeTokenSearchServerMethod("listNames", uri);
    }

    public List<String> getAncestorFragIDs(String uri) {
        return (List<String>) invokeTokenSearchServerMethod("getAncestorFragIDs", uri);
    }

    public List<String> getChildrenFragIDs(String uri) {
        return (List<String>) invokeTokenSearchServerMethod("getChildrenFragIDs", uri);
    }

    public int getLevelAge(String uri) {
        return (Integer) invokeTokenSearchServerMethod("getLevelAge", uri);
    }

    public List<String> getLevelsOfAge(int age) {
        return (List<String>) invokeTokenSearchServerMethod("getLevelsOfAge", age);
    }

    private Object invokeTokenSearchServerMethod(String methodName, Object... params) {
        try {
            Class[] paramClasses = new Class[params.length];
            int i=0;
            for(Object p: params) paramClasses[i++] = p.getClass();
            Method m = skillsSearchService.getClass().getMethod(methodName, paramClasses);
            return m.invoke(skillsSearchService, params);
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalStateException("Couldn't invoke " + methodName + ".", e);
        }

    }
}

