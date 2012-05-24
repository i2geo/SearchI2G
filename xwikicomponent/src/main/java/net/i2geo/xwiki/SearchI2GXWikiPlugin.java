package net.i2geo.xwiki;

import com.xpn.xwiki.plugin.XWikiDefaultPlugin;
import com.xpn.xwiki.plugin.XWikiPluginInterface;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Api;
import net.i2geo.xwiki.impl.DefaultSearchI2GXWiki;
import org.xwiki.component.phase.InitializationException;

/**
 */
public class SearchI2GXWikiPlugin extends XWikiDefaultPlugin {

    public SearchI2GXWikiPlugin(String name, String className, XWikiContext context) {
        super("searchi2g",SearchI2GXWikiPlugin.class.getName(),context);
    }

    public String getName() {
        return "searchi2g";
    }

    private SearchI2GXWiki component;

    public void init(XWikiContext context) {
        try {
            this.component = new DefaultSearchI2GXWiki(this,context);
            this.component.initialize();
        } catch (InitializationException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
    public Api getPluginApi(XWikiPluginInterface plugin, XWikiContext context) {
        return (Api) component;
    }

    public SearchI2GXWiki getImplementation() {
        return component;
    }

}
