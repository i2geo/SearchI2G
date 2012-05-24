package net.i2geo.search;

import net.i2geo.api.SKBServiceContext;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Collections;
import java.util.ArrayList;

/**
 */
public class I2GSWebUtil {

    
    public static List<String> readAcceptedLangs(HttpServletRequest req) {
        return readAcceptedLangs(req.getParameter("language")
                + "," + req.getHeader("Accept-Language"));
    }
    
    public static List<String> readAcceptedLangs(String s) {
        if(s==null || s.length()==0) s="";
        // count the number of commas
        int count=0;
        for(int p=0; p<s.length() && p>=0; count++)
            p = s.indexOf(",",p+1);
        List<String> r = new ArrayList(count+1);
        int lastP = -1;
        for(int i=0,p=0; p>lastP && p<s.length(); i++, p=s.indexOf(",",p)+1) {
            lastP=p;
            int q=s.indexOf(";",p);
            int t=s.indexOf(",",p);
            if(t!=-1 && t<q) q=t;
            if(q==-1) q = s.indexOf(",",p);
            if(q==-1) q = s.length();
            //System.out.println("p="+p + ", q="+q + ": ");
            String lang = s.substring(p,q).trim();
            if(!"x-all".equals(lang) && lang.length()>2) lang = lang.substring(0,2); 
            r.add(lang);
            //System.out.println("r["+i+"]="+r[i]);
        }
        r.add("x-all");
        return r;
    }

    public static SKBServiceContext createSKBServiceContext(String lang, String basePath) {
        SKBServiceContext context;
        if(!SKBServiceContext.platformSupportedLanguages.contains(lang)) return null;

        try {
            context = new SKBServiceContext();
            context.basePath = basePath;
            context.setSkbi18n(new SKBPropsI18n(lang));
            return context;
        } catch(Exception ex) {
            ex.printStackTrace();
            return null;
        }

    }

}
