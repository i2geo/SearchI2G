package net.i2geo.search;

import net.i2geo.index.IndexHome;
import net.i2geo.api.SKBi18n;
import net.i2geo.api.SKBServiceContext;
import net.i2geo.api.SKBServiceContextProvider;

import javax.servlet.ServletContext;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

/**
 */
public class I2GSearchWebappCenter implements SKBServiceContextProvider {

    private IndexHome indexHome;
    private Map<String,SKBServiceContext> contextsPerLanguage = new HashMap<String,SKBServiceContext>();
    private static I2GSearchWebappCenter firstInstance;
    public static final String basePath = "/SearchI2G/net.i2geo.skillstextbox.SkillsTextBox/";

    public static I2GSearchWebappCenter init(ServletContext context) {
        I2GSearchWebappCenter center = (I2GSearchWebappCenter)
                context.getAttribute(I2GSearchWebappCenter.class.getName());
        if(center == null) {
            center = new I2GSearchWebappCenter(context);
            if(firstInstance == null) firstInstance = center;
        }
        return center;
    }


    public static I2GSearchWebappCenter getInstance() {
        return firstInstance;
    }

    public I2GSearchWebappCenter(ServletContext context) {
        indexHome = IndexHome.getInstance(context.getInitParameter("indexPath"));
        context.setAttribute(I2GSearchWebappCenter.class.getName(),this);
        context.setAttribute(SKBServiceContextProvider.class.getName(),this);
    }

    public IndexHome getIndexHome() {
        return indexHome;
    }

    public SKBServiceContext tryToGetOrMakeServiceForLangs(String accLangs) {
        List<String> acceptedLangs = I2GSWebUtil.readAcceptedLangs(accLangs);
        return tryToGetOrMakeServiceForLangs(acceptedLangs);
    }
    public SKBServiceContext tryToGetOrMakeServiceForLangs(List<String> acceptedLangs) {
        SKBServiceContext serviceContext = null;
        for(String lan:acceptedLangs) {
            if(lan.length()>2) lan=lan.substring(2);
            if(lan.length()!=2) continue;
            serviceContext = I2GSearchWebappCenter.getInstance().tryToGetOrMakeServiceForLang(lan);
            if(serviceContext!=null) break;
        }
        if(serviceContext == null)
            serviceContext = I2GSearchWebappCenter.getInstance().tryToGetOrMakeServiceForLang("en");
        return serviceContext;
    }

    public SKBServiceContext tryToGetOrMakeServiceForLang(String lang) {
        if(lang==null) return null;
        SKBServiceContext context = contextsPerLanguage.get(lang);
        if(context!=null) return context;
        context = I2GSWebUtil.createSKBServiceContext(lang,I2GSearchWebappCenter.basePath);
        contextsPerLanguage.put(lang,context);
        return context;
    }


}
