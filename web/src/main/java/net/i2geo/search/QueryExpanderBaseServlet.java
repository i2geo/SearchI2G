package net.i2geo.search;

import net.i2geo.index.IndexHome;
import net.i2geo.index.rsearch.RSearchContext;
import net.i2geo.index.rsearch.RSOntologyAccess;
import net.i2geo.index.rsearch.RSearchQueryExpander;
import net.i2geo.onto.GeoSkillsAccess;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.*;

import org.apache.lucene.search.Query;

/**
 */
public abstract class QueryExpanderBaseServlet extends HttpServlet {


    protected IndexHome indexHome;
    protected RSOntologyAccess onto;
    protected Map<List<String>,RSearchContext> searchContexts
             = new HashMap<List<String>,RSearchContext>();

    @Override
    public void init() throws ServletException {
        try {
            I2GSearchWebappCenter center = I2GSearchWebappCenter.init(getServletContext());
            indexHome = center.getIndexHome();
            String geoSkillsUrl = getServletContext().getInitParameter("geoSkills-URL");
            if(System.getProperty("net.i2geo.onto.geoSkillsDevUrl")!=null)
                geoSkillsUrl = System.getProperty("net.i2geo.onto.geoSkillsDevUrl");
            onto = RSOntologyAccess.boot(geoSkillsUrl,indexHome);
            //new RSOntologyAccess(access);
            onto.open();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }




    protected RSearchContext getSearchContext(HttpServletRequest req) {
        List<String> langs = I2GSWebUtil.readAcceptedLangs(req);
        RSearchContext r = searchContexts.get(langs);
        if(r!=null) return r;
        r = new RSearchContext(langs);
        searchContexts.put(langs,r);
        return r;
    }
}
