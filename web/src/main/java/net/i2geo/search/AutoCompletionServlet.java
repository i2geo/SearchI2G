package net.i2geo.search;

import net.i2geo.api.SkillItem;
import net.i2geo.index.IndexHome;
import net.i2geo.index.SKBQueryExpander;
import net.i2geo.index.GSIUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.Arrays;
import java.util.List;

/** Servlet to return the auto-completions as an XML document.
 */
public class AutoCompletionServlet extends HttpServlet {

    private transient SKBQueryExpander skbExpander = null;
    private transient IndexHome indexHome = null;
    private transient AutoCompletionCache cache = null;
    private static int MAX_SEARCH_RESULT = 30,
        COUNT_THRESHOLD_FROM = 5;
    private static final float SCORE_THRESHOLD = 0.01f;


    protected static Log log = LogFactory.getLog(AutoCompletionServlet.class.getName());

    public void init() throws ServletException {
        log.info("Servlet AutoCompletionServlet " + super.getServletName() + " initting." );
        super.init();
        skbExpander = new SKBQueryExpander(IndexHome.supportedLanguages);
        indexHome = IndexHome.getInstance();
        cache = new AutoCompletionCache();
        log.info("Servlet AutoCompletionServlet " + super.getServletName() + " initted." );
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        throw new UnsupportedOperationException("Please use the GET method with the parameters l and q.");
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String query = request.getParameter("q");
        String languages = request.getParameter("l");
        if(languages==null) languages = "en";
        List<String> langs  = SKBUtils.parseLanguages(languages,5);
        long ifModif = request.getDateHeader("If-Modified-Since");
        if(ifModif != -1 && ifModif > indexHome.getLastIndexed()) {
            response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            if(log.isDebugEnabled()) log.debug("Not modified (" + query + " in " + languages + ").");
            return;
        }

        String types = request.getParameter("t");
        if(types==null) types = "";
        String[] typesA = types.split(",| ");

        response.setContentType("text/xml?charset=utf-8");
        response.setCharacterEncoding("utf-8");
        response.setDateHeader("Expires",System.currentTimeMillis()+24*3600*1000);
        response.setDateHeader("Date",indexHome.getLastIndexed());
        // E-Tags??
        byte[] results = cache.getMatchingResults(query,typesA,languages);
        if(results==null) {
            if(log.isDebugEnabled()) log.debug("Computing autocompletion (" + query + " in " + languages + ").");
            ByteArrayOutputStream bOut = new ByteArrayOutputStream(1024);
            Writer out = new OutputStreamWriter(bOut,"utf-8");
            try{
                Query luceneQuery= skbExpander.expandQuery(query,typesA,langs,true);
                TopDocs topDocs = indexHome.getSearcher().search(luceneQuery,MAX_SEARCH_RESULT);
                int numberOfMatches = topDocs.totalHits;
                if(log.isInfoEnabled()) log.info("search for \"" + query + "\" has expanded to \"" + luceneQuery +"\".");
                if(log.isInfoEnabled()) log.info("got: "+numberOfMatches+" items.");


                out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
                out.write("<autoCompletions query='");
                out.write(query);
                out.write("' languages='"); outputEscapedAttValue(languages,out); out.write("' ");
                out.write("types='"); outputEscapedAttValue(types,out); out.write("'>\n");

                int amountPut = 0;
                if (numberOfMatches!=0){
                    ScoreDoc[] hits = topDocs.scoreDocs;
                    for(int i=0; i<numberOfMatches && amountPut<MAX_SEARCH_RESULT; i++,amountPut++){
                        if(i> COUNT_THRESHOLD_FROM && hits[i].score< SCORE_THRESHOLD) continue;
                        out.write("  <autoCompletion ");
                        Document doc = indexHome.getReader().document(hits[i].doc);
                        String tit = null;
                        for(String lang: langs) {
                            tit = doc.get("title-" + lang);
                            if(tit != null) break;
                        }

                        if(tit==null) {
                            for(Field f : (List<Field>) doc.getFields()) {
                                if(f.name().startsWith("title-")) {
                                    tit = f.stringValue();
                                    break;
                                }
                            }
                        }
                        if(tit==null) tit = doc.get("uri-weak");
                        if(tit==null) tit= "-missing-title-";
                        outputAttr(tit,"title","title",out);
                        outputAttr(doc, "ontType","type",out);
                        String uri = doc.get("uri");
                        uri = GSIUtil.uriToName(uri,false);
                        outputAttr(uri,"uri","uri",out);
                        outputAttr(doc,"urlForNav","urlForNav",out);
                        String matchField = indexHome.computeMatchedField(hits[i].doc, doc,indexHome.createAnalyzer(langs),luceneQuery);
                        outputAttr(matchField, "","shortDesc",out);
                        outputAttr(doc,"numResources","num",out);
                        out.write("/>\n");
                    }
                } else {
                    log.info("[SkillsSearch] - search delivered no matches for queryString \""+query + "\".");
                }
                out.write("</autoCompletions>");
                out.flush();
            } catch (Exception e){
                e.printStackTrace();
                e.printStackTrace(new PrintWriter(out));
                throw new RuntimeException(e);
            }
            results = bOut.toByteArray();
        } else {
            if(log.isDebugEnabled()) log.debug("Using cached result (" + query + " in " + languages + ").");
        }

        // now do the output
        response.setIntHeader("Content-Length", results.length);
        OutputStream out = response.getOutputStream();
        out.write(results,0,results.length);

    }// end of searchIndex method


    private void outputAttr(Document doc, String fieldName, String attName, Writer out) throws IOException{
        String fieldValue = doc.get(fieldName);
        outputAttr(fieldValue, fieldName, attName, out);
    }
    private void outputAttr(String fieldValue, String fieldName, String attName, Writer out) throws IOException {
        if(fieldValue!=null) {
            out.write(" ");
            out.write(attName);
            out.write("='");
            outputEscapedAttValue(fieldValue,out);
            out.write("'");
        }
    }

    private void outputEscapedAttValue(String t, Writer out) throws IOException {
        for(int i=0,l=t.length(); i<l; i++) {
            char c = t.charAt(i);
            switch (c) {
                case '\n':
                    out.write("&#xA;"); break;
                case '\r':
                    out.write("&#xD;"); break;
                case '\'':
                    out.write("&apos;"); break;
                case '"':
                    out.write("&quot;"); break;
                case '&':
                    out.write("&amp;"); break;
                case '<': out.write("&lt;"); break;
                case '>': out.write("&gt;"); break;
                default: out.write(c); break;
            }
        }
    }
}
