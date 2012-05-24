package net.i2geo.xwiki;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.plugin.lucene.LucenePluginApi;
import net.i2geo.api.MatchingResourcesCounter;

public class MatchingResourcesCounterImpl implements MatchingResourcesCounter {

    public MatchingResourcesCounterImpl(LucenePluginApi luPlug, XWikiContext context) {
        this.luPlug = luPlug;
        this.xwikiContext = context;
    }

    private final LucenePluginApi luPlug;
    private final XWikiContext xwikiContext;

    public int countMatchingResources(String q) {
        int count =-1;
        try {
            count = luPlug.count("+object:currikicode.assetclass  -CurrikiCode.AssetClass.hidden_from_search:1" +
                    " +(+(CurrikiCode.AssetClass.trainedTopicsAndCompetencies:"+q+"))", xwikiContext);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return count;
    }
}
