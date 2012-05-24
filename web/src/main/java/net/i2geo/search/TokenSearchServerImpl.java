package net.i2geo.search;

//needed java imports
import java.util.*;
import java.io.IOException;

//lucene imports
import net.i2geo.index.SubjectsCollector;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.search.*;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.*;

//GWT imports
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

//XStream imports
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;

import javax.servlet.ServletException;

import net.i2geo.api.*;
import net.i2geo.api.search.UserQuery;
import net.i2geo.api.search.QueryExpander;
import net.i2geo.api.search.QueryExpansionResult;
import net.i2geo.index.IndexHome;
import net.i2geo.index.GeoSkillsIndexer;
import net.i2geo.index.SKBQueryExpander;
import net.i2geo.index.rsearch.RSearchQueryExpander;
import net.i2geo.index.rsearch.RSearchContext;
import net.i2geo.index.rsearch.RSOntologyAccess;
import net.i2geo.onto.GeoSkillsAccess;

public class TokenSearchServerImpl
        extends RemoteServiceServlet
        implements SkillsSearchService, QueryExpander {

    // the search modes
    private final static int MAX_SEARCH_RESULT = 30;
    private static TokenSearchServerImpl firstInstance = null;

    private transient IndexHome indexHome = IndexHome.getInstance();
    private transient Logger log = Logger.getLogger(TokenSearchServerImpl.class);
    private transient XStream xStream = new XStream(new JettisonMappedXmlDriver());
    private transient SKBQueryExpander skbExpander = new SKBQueryExpander(IndexHome.supportedLanguages);
    private transient RSOntologyAccess rSearchOnto = new RSOntologyAccess(GeoSkillsAccess.getInstance(),indexHome);
    private transient TokenSearchCache cache = null;
    private static final float SCORE_THRESHOLD = 0.01f;
    private static final int COUNT_THRESHOLD_FROM = 5;

    public void init() throws ServletException {
        try {
            xStream.alias("SkillItem", SkillItem.class);
            indexHome = I2GSearchWebappCenter.init(getServletContext()).getIndexHome();
            getServletContext().setAttribute(SkillsSearchService.class.getName(),this);
            getServletContext().setAttribute(QueryExpander.class.getName(),this);
            ConsoleAppender appender = new ConsoleAppender(new PatternLayout("%d %-5p %c{2} %x - %m\\n"));
            appender.setThreshold(Priority.INFO);
            //log.setLevel(Level.INFO);
            log.addAppender(appender);
            firstInstance = this;

            cache = (TokenSearchCache) getServletContext().getAttribute("cache");
            if(cache==null) {
                cache = new TokenSearchCache();
                getServletContext().setAttribute("cache",cache);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServletException(e);
        }
    }

    public static TokenSearchServerImpl getFirstInstance() {
        return firstInstance;
    }

    public void destroy() {
        firstInstance = null;
        indexHome = null;
    }



    private void searchIndex(String inputQuery, String[] authorizedTypes, List<String> languages, List<SkillItem> matchList){
        try{
            Query query = skbExpander.expandQuery(inputQuery,authorizedTypes,languages,true);
            TopDocs topDocs = indexHome.getSearcher().search(query,30);
            int numberOfMatches = topDocs.totalHits;
            if(log.isInfoEnabled()) log.info("search for \"" + inputQuery + "\" has expanded to \"" + query +"\".");
            if(log.isInfoEnabled()) log.info("got: "+numberOfMatches+" items.");


            Document doc;
            int amountPut = 0;
            if (numberOfMatches!=0){
                ScoreDoc[] hits = topDocs.scoreDocs;
                for(int i=0; i<numberOfMatches && amountPut<MAX_SEARCH_RESULT; i++,amountPut++){
                    if(i> COUNT_THRESHOLD_FROM && hits[i].score< SCORE_THRESHOLD) continue;
                    doc = indexHome.getReader().document(hits[i].doc);
                    SkillItem item = readSkillItemFromDoc(doc,languages);
                    if(item!=null) {
                        item.setShortDescription(indexHome.computeMatchedField(hits[i].doc,doc,indexHome.createAnalyzer(languages),query));
                        matchList.add(item);
                    }
                }
            } else {
                log.info("[SkillsSearch] - search delivered no matches for queryString \""+inputQuery + "\".");
            }
        } catch (Exception e){
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }// end of searchIndex method

    public SkillItem[] searchSkillItem(String queryString, String[] authorizedTypes, String acceptedLangs) {
        SkillItem[] skillItemArray = cache.getMatchingSkillsItems(queryString,authorizedTypes, acceptedLangs);
        if(skillItemArray!=null) {
            return skillItemArray;
        }
        log.info("searchSkillItem \"" + queryString + "\".");

        try{
            List<SkillItem> results = new ArrayList<SkillItem>(20);
            List<String> acceptedLanguages = I2GSWebUtil.readAcceptedLangs(acceptedLangs);
            searchIndex(queryString,authorizedTypes, acceptedLanguages,results);
            // drop all null ones
            for(Iterator it=results.iterator(); it.hasNext();) {if (it.next()==null) it.remove();}
            skillItemArray = new SkillItem[results.size()];
            int skillCounter=0;
            for(SkillItem item: results) {
                skillItemArray[skillCounter++] = item;
            }
            // concluded, now add to cache
            cache.addMatchingSkillsItems(queryString,authorizedTypes,acceptedLangs,skillItemArray);
        } catch(Exception e){
            e.printStackTrace();
            throw new IllegalStateException("Can't search items : "+ e);
        } catch(Error e){
            e.printStackTrace();
            System.out.println("org.apache.lucene.analysis.Token:" + org.apache.lucene.analysis.Token.class.getResource("Token.class"));
            throw e;
        } catch(Throwable x) {
            x.printStackTrace();
            throw new IllegalStateException("Can't search items : "+ x);

        }
        return skillItemArray;

    }

    public SkillItem[] getSkillItem(String uris, String acceptedLangs) {
        return getSkillItem(uris, I2GSWebUtil.readAcceptedLangs(acceptedLangs));
    }
    public SkillItem[] getSkillItem(String uris, List<String> acceptedLangs) {
        String[] textArray = uris.split(",| ");
        List<String> urisRequested = new ArrayList<String>(textArray.length);
        try {
            for(String text:textArray) {
                if(!text.contains("#") && !text.startsWith("http://"))
                    text = "#"+text;
                if(!text.startsWith("http://"))
                    text = GeoSkillsIndexer.ontBaseURI + text;
                urisRequested.add(text);
            }

            List<SkillItem> items = new ArrayList<SkillItem>();
            for(String uri:urisRequested) {
                SkillItem item = renderItem(uri,acceptedLangs);
                if(item!=null) items.add(item);
            }

            // case for weak-uris??

            SkillItem[] skillItems = new SkillItem[items.size()];
            int i=0;
            for(SkillItem skillItem: items) skillItems[i++] = skillItem;
            return skillItems;



            /* BooleanQuery query = new BooleanQuery();
            query.add(new BooleanClause(new TermQuery(new Term("uri",text)),
                    BooleanClause.Occur.SHOULD));

            System.out.println("getSkillItem searching for " + query);
            Hits hits = indexHome.getSearcher().search(query);
            if(hits.length()==0 && textArrayList.size()==1) {
                String uri = (String) textArrayList.iterator().next();
                TermQuery q = null;
                q = new TermQuery(new Term("uri-weak",uri.toLowerCase()));
                if(q!=null)
                    hits = indexHome.getSearcher().search(q);
            }
            log.info("Rendering found " + hits.length() + " items.");
            SkillItem[] items = new SkillItem[hits.length()];
            int c=0;
            for(int i=0, l=hits.length(); i<l; i++) {
                SkillItem item = readSkillItemFromDoc(hits.doc(i),acceptedLangs);
                if(item!=null) {
                    items[c++] = item;
                } else
                    log.warn("No skill-item found for " + hits.doc(i).get("uri"));
            }
            if(c<items.length) {
                SkillItem[] r = new SkillItem[c];
                System.arraycopy(items,0,r,0,c);
                return r;
            }
            return items;*/
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Couldn't getSkillItem: " + e.toString(),e);
        }
    }// end of getSkillItem method


    private SkillItem readSkillItemFromDoc(Document doc, List<String> acceptedLangs) {
        for(String lang: acceptedLangs) {
            String json = doc.get("skillItem-" +lang);
            if(json!=null) {
                SkillItem item = (SkillItem) xStream.fromXML(json);
                return item;
            }
        }
        // nothing found? try the first one then
        for(Object o:doc.getFields()) {
            Field f= (Field)o;
            if(f.name().startsWith("skillItem-"))
                return (SkillItem) xStream.fromXML(doc.get(f.name()));
        }
        return null; // there was none to find
    }

    public SkillItem renderItem(String uri, String acceptedLangs) {
        List<String> langs = I2GSWebUtil.readAcceptedLangs(acceptedLangs);
        return renderItem(uri,langs);
    }

    private SkillItem renderItem(String uri, List<String> langs) {
        SkillItem item = cache.getSkillItemRendering(uri,langs);
        if(item!=null) return item;
        log.info("renderItem " + uri);
        try {
            if(uri==null) return null;
            if(!uri.startsWith("http://")) {
                if(uri.startsWith("#"))
                    uri = GeoSkillsIndexer.ontBaseURI + uri;
                else
                    uri = GeoSkillsIndexer.ontBaseURI + "#" + uri;
            }
            Query query = new TermQuery(new Term("uri",uri));
            TopDocs topdocs = indexHome.getSearcher().search(query,20);
            ScoreDoc[] docs;
            if(topdocs.totalHits == 0 ) {
                TermQuery q;
                if(!uri.contains("#") && !uri.startsWith("http://"))
                    uri= "#"+uri;
                if(!uri.startsWith("http://"))
                    uri= GeoSkillsIndexer.ontBaseURI + uri;
                if(uri.endsWith("_r")) {
                    // try without _r
                    q = new TermQuery(new Term("uri",uri.substring(0,uri.length()-2)));
                } else {
                    // try with _r
                    q = new TermQuery(new Term("uri",uri + "_r"));
                }

                // also try URI-weak?
                topdocs = indexHome.getSearcher().search(q,20);
            }
            if(topdocs.totalHits==0) return null;
            docs = topdocs.scoreDocs;
            if(topdocs.totalHits>1) log.warn("Ambiguous match for uri \"" + uri + "\".");
            item = readSkillItemFromDoc(indexHome.getReader().document(docs[0].doc),langs);
            cache.addSkillItemRendering(uri,langs,item);
            return item;
        } catch (IOException e) {
            log.warn("Can't renderItem", e);
            throw new IllegalStateException("Can't render item: " + e);
        }

    }

    public QueryExpansionResult expandUserQuery(UserQuery query) {
        RSearchContext searchContext = new RSearchContext(query);
        RSearchQueryExpander expander = new RSearchQueryExpander(searchContext,indexHome,rSearchOnto);
        QueryExpansionResult result = new QueryExpansionResult();
        result.setMessages(new LinkedList<String>());
        expander.setMessagesList(result.getMessages());
        try {
            result.setQuery(expander.expandToLuceneQuery(query));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return result;
    }

    @Override
    public QueryExpansionResult expandSubjectQuery(String query) {
        QueryExpansionResult result = new QueryExpansionResult();
        try {
            result.setQuery(SubjectsCollector.getQuery(indexHome,query));
        } catch (IOException e) {
            result.setMessages(Arrays.asList(
                    "Trouble at expressing subject \"" + query + "\".",
                    e.toString()));
            e.printStackTrace();
        }
        return result;
    }

    public String[] getNodeParents(String uri) {
        Document doc = indexHome.getDocForUri(uri);
        log.info("getNodeParents of "+ uri + " yields doc " + doc );
        if(doc!=null) return doc.getValues("ancestorTopic");
        else return new String[] {};
    }

    Log weblog = LogFactory.getLog("fromweb");
    public void log(String msg) {
        weblog.info(msg);
    }

}
