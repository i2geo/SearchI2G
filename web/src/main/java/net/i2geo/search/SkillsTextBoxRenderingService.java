package net.i2geo.search;

import net.i2geo.api.SKBRenderer;
import net.i2geo.api.SkillsSearchService;
import net.i2geo.api.SkillItem;
import net.i2geo.api.SKBServiceContext;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.Writer;
import java.io.OutputStreamWriter;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

/** Servlet for the edition of a row of ontology nodes.
 */
public class SkillsTextBoxRenderingService extends HttpServlet {


    private Map<String, SKBServiceContext> contextsPerLanguage = new HashMap<String, SKBServiceContext>();

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String[] uris = request.getParameterValues("uri");
        if(uris==null || uris.length==0) {
            response.setContentLength(0);
            response.getOutputStream();
            return;
        }
        boolean small = true;
        if(request.getParameter("small")!=null) {
            small = Boolean.parseBoolean(request.getParameter("small"));
        }
        boolean withSelection = false;
        if(request.getParameter("selection")!=null) {
            withSelection = Boolean.parseBoolean(request.getParameter("small"));
        }

        SKBServiceContext serviceContext = null;
        String languageParam = request.getParameter("language");
        if(languageParam==null) languageParam = request.getParameter("lang");
        if(languageParam==null) languageParam = request.getParameter("langs");
        String accLangs = request.getHeader("Accept-Language");
        accLangs = (languageParam!=null?languageParam+ ",":"") +
                (accLangs!=null?accLangs:"");
        if(accLangs.length()==0) accLangs = "en";
        List<String> acceptedLangs = I2GSWebUtil.readAcceptedLangs(accLangs);
        serviceContext = I2GSearchWebappCenter.getInstance().tryToGetOrMakeServiceForLangs(acceptedLangs);
        SKBRenderer renderer = new SKBRenderer(serviceContext);
        renderer.setSmall(small);
        renderer.setWitSelection(withSelection);
        TokenSearchServerImpl service = TokenSearchServerImpl.getFirstInstance();

        //ByteArrayOutputStream buff = new ByteArrayOutputStream(512);

        response.setContentType("text/html;charset=utf-8");
        Writer out = response.getWriter();//new OutputStreamWriter(buff,"utf-8");
        for(int i=0, l=uris.length; i<l; i++) {
            if(uris[i]==null || uris[i].trim().length()==0) continue;
            SkillItem[] founds = service.getSkillItem(uris[i],acceptedLangs);
            for(int j=0,k=founds.length; j<k; j++)
                out.write(renderer.render(founds[j]));
        }
        out.flush();
        //response.setContentLength(buff.size());
        //response.getOutputStream().write(buff.toByteArray());
    }

}
