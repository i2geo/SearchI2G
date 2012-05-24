import com.xpn.xwiki.XWiki
import com.xpn.xwiki.XWikiContext
import org.apache.lucene.queryParser.QueryParser
import org.apache.lucene.search.Query
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.BooleanClause
import org.apache.lucene.search.TermQuery
import org.apache.lucene.search.Hits
import org.apache.lucene.index.Term
import com.xpn.xwiki.plugin.lucene.LucenePluginApi
import org.curriki.xwiki.plugin.lucene.CurrikiAnalyzer
import org.apache.commons.logging.LogFactory
import org.apache.commons.logging.Log
import javax.servlet.http.HttpServletRequest
import org.apache.lucene.analysis.Analyzer
import org.curriki.xwiki.plugin.curriki.CurrikiPluginApi
import com.xpn.xwiki.web.XWikiMessageTool
import com.xpn.xwiki.plugin.lucene.I2GLuceneProfile

public class GroovyCursor implements Iterator {

  static Log LOG = LogFactory.getLog("pages.AdvancedSearch.GroovyCursor");

  public GroovyCursor() {}

  public void init(XWiki xwiki, XWikiContext context, LucenePluginApi plugin, XWikiMessageTool msg) {
    this.plugin = plugin
    this.xwiki = xwiki;
    this.context = context;
    this.msg = msg;
    this.analyzer = CurrikiAnalyzer.getInstance(context.getLanguage(),context, I2GLuceneProfile.getInstance());
  }

  private XWiki xwiki;
  private XWikiContext context;
  private LucenePluginApi plugin;
  private XWikiMessageTool msg;
  private Analyzer analyzer;

  private String sortFields = "relevance";


  private ArrayList warnMessages = new ArrayList();
  private QueryParser parser;
  private Hits hits;
  private int pos = 0;
  private int start = 0, max = Integer.MAX_VALUE;
  private int maxPageLength = 20;
  private static must = BooleanClause.Occur.MUST, mustNot = BooleanClause.Occur.MUST_NOT, should= BooleanClause.Occur.SHOULD;

  public Set getSetFromParams(HttpServletRequest request,String name) {
    if(name==null || request==null) return Collections.EMPTY_SET;
    String[] values = request.getParameterValues(name);
    if(values==null || values.length==0) return Collections.EMPTY_SET;
    return new HashSet(Arrays.asList(request.getParameterValues(name)));
  }

  public List getICTPossibleValues(CurrikiPluginApi currikiPlugin) {
    List r = currikiPlugin.getValues("CurrikiCode.AssetClass","instructional_component");
    //r.add(0,"-");
    return r;
  }

  public List getLicensePossibleValues(CurrikiPluginApi currikiPlugin) {
    return currikiPlugin.getValues("CurrikiCode.AssetLicenseClass","licenseType");
  }

  public Query createPagesQuery(String text) {
    if(text==null || text.length()==0) return null;
    QueryParser parser = new QueryParser("ft", analyzer),
      parser2 = new QueryParser("ft.stemmed", analyzer);
    Query q = parser.parse(text), q2 = parser2.parse(text);
    BooleanQuery bq = new BooleanQuery(), bqMain = new BooleanQuery();
    bqMain.add(q,should); bqMain.add(q2,should);
    bq.add(bqMain,must);
    // add exclusions: no review, no blog, no resource
    bq.add(new TermQuery(new Term("object","QF.ReviewClass")),mustNot);
    bq.add(new TermQuery(new Term("object","XWiki.ArticleClass")),mustNot);
    bq.add(new TermQuery(new Term("object","XWiki.XWikiUsers")),mustNot);
    bq.add(new TermQuery(new Term("object","CurrikiCode.AssetClass")),mustNot);
    bq.add(new TermQuery(new Term("name","WebPreferences")),mustNot);
    return bq;
  }

  public Query createBlogsQuery(String text, String author, String keywords) {
    BooleanQuery bq = new BooleanQuery();
    bq.add(new QueryParser("object",analyzer).parse("XWiki.ArticleClass"),must);//new TermQuery(new Term("object","XWiki.ArticleClass")),must);
    boolean does = false;
    BooleanQuery bq2 = new BooleanQuery();
    does = addMustQueryIfUseful(bq2,"ft",text,analyzer,true,true) || does ;
    does = addAuthorQuery(bq2,"author",author,analyzer) || does;
    // TODO: this doesn't search the author-names.. but the user-id... one would need to search authors by name (or... all fields?) first
    does = addMustQueryIfUseful(bq2,"keywords",keywords,analyzer,true) || does;
    if(!does) return null;
    bq.add(bq2,must);
    return bq;
  }



  public Query createMembersQuery(String text, String level, String trained, String username, String firstname, String lastname, String language, String country, String city, String allMembers) {
    // XWikiUsers are not (thus far?) analyzed with a real stemming, use the english stemming
    analyzer = new CurrikiAnalyzer("en",I2GLuceneProfile.getInstance(), context);
    BooleanQuery bq = new BooleanQuery();
    bq.add(new QueryParser("object",analyzer).parse("XWiki.XWikiUsers"),must);
    if(!"true".equals(allMembers)) {
      BooleanQuery bq2 = new BooleanQuery();
      boolean does = false;
      does = addMustQueryIfUseful(bq2,"ft",text,analyzer,true)||does;
      does=addGeoSkillsQueryIfUseful(bq2,"XWiki.XWikiUsers.asset.trainedTopicsAndCompetencies","ancestorTopics",trained,analyzer)||does;
      does=addMustQueryIfUseful(bq2,"XWiki.XWikiUsers.eduLevelFine",level,analyzer,true)||does;
      does=addMustQueryIfUseful(bq2,"name",username,analyzer,true)||does;
      if(firstname!=null && firstname.length()>0)
        does=addMustQueryIfUseful(bq2,"XWiki.XWikiUsers.first_name",firstname + " XWiki.XWikiUsers.first_name.untokenized:(" + firstname + ")",analyzer,false)||does;
      if(lastname!=null && lastname.length()>0)
        does=addMustQueryIfUseful(bq2,"XWiki.XWikiUsers.last_name",lastname + " XWiki.XWikiUsers.last_name.untokenized:(" + lastname + ")",analyzer,false)||does;
      if(language!=null && language.length()>0 && !"*".equals(language.trim()))
        does=addMustQueryIfUseful(bq2,"XWiki.XWikiUsers.default_language",language + " " + lang3toLang2(language),analyzer,false)||does;
      does=addMustQueryIfUseful(bq2,"XWiki.XWikiUsers.country",country,analyzer,true)||does;
      if(city!=null && city.length()>0)
        does=addMustQueryIfUseful(bq2,"XWiki.XWikiUsers.city",city + " XWiki.XWikiUsers.city.untokenized:(" + city + ")",analyzer,true,true)||does;
      if(!does) return null;
      bq.add(bq2,must);
    }
    sortFields = "-date,XWiki.XWikiUsers.first_name,XWiki.XWikiUsers.last_name";
    maxPageLength = 30;
    return bq;
  }


  public Query createGroupsQuery (String text, String level, String trained, String creator, String groupname, String language) {
    BooleanQuery bq = new BooleanQuery();
    bq.add(new QueryParser("object",analyzer).parse("XWiki.CurrikiSpaceClass"),must);
    BooleanQuery bq2 = new BooleanQuery();
    boolean does = false;
    does = addMustQueryIfUseful(bq2,"ft",text,analyzer,true,true)||does;

    does = addAuthorQuery(bq2,"creator",creator,analyzer) || does;
    addGeoSkillsQueryIfUseful(bq2,"XWiki.SpaceClass.trainedTopicsAndCompetencies","ancestorTopics",trained,analyzer)||does;
    does=addMustQueryIfUseful(bq2,"XWiki.SpaceClass.eduLevelFine",level,analyzer,true)||does;
    does=addMustQueryIfUseful(bq2,"XWiki.SpaceClass.displayTitle",groupname,analyzer,true,true)||does;
    if(language!=null && !"*".equals(language) && !(language.length()==0))
      does=addMustQueryIfUseful(bq2,"XWiki.CurrikiSpaceClass.language",language + " " + lang3toLang2(language), analyzer,false)||does;
    if(!does) return null;
    bq.add(bq2,must);
    return bq;
  }

  //text,level,trained,title,author,RightsHolder,filetype,ict,searchlang,review,license
  public Query createResourcesQuery(String text, String level, String trained, String title, String author,
                                    String rightsHolder, String filetype, String ict, String language, String review, String license) {
    try {
      BooleanQuery bq = new BooleanQuery();
      bq.add(new QueryParser("object",analyzer).parse("CurrikiCode.AssetClass"),must);
      BooleanQuery bq2 = new BooleanQuery();
      boolean does = false;
      does =addMustQueryIfUseful(bq2,"ft",text,analyzer,true,true)||does;
      does =addMustQueryIfUseful(bq2,"CurrikiCode.AssetClass.eduLevelFine",level,analyzer,true)||does;
      does = addGeoSkillsQueryIfUseful(bq2,"CurrikiCode.AssetClass.trainedTopicsAndCompetencies","i2geo.ancestorTopics",trained,analyzer) || does;
      does =addMustQueryIfUseful(bq2,"title",title,analyzer,true,true)||does;
      does =addAuthorQuery(bq2,"author",author,analyzer) || does;
      does =addMustQueryIfUseful(bq2,"CurrikiCode.AssetLicenseClass.rightsHolder",rightsHolder,analyzer,true,true)||does;

      if("URL".equalsIgnoreCase(filetype))
        does = addMustQueryIfUseful(bq2,"assetType","External",analyzer,true)||does;
      else
        does =addMustQueryIfUseful(bq2,"CurrikiCode.AttachmentAssetClass.file_type",remapFileTypes(filetype),analyzer,false)||does;
      does=addMustQueryIfUseful(bq2,"CurrikiCode.AssetClass.instructional_component.key",ict,analyzer,false)||does;
      if(language!=null && language.length()>0 && !"*".equals(language)) {
        does = addMustQueryIfUseful(bq2,"CurrikiCode.AssetClass.language",language,analyzer,false);
      }
      
      if(!("-".equals(review) || null==review || "*".equals(review) || "".equals(review))) {
        int minReview = Integer.parseInt(review);
        BooleanQuery bq3 = new BooleanQuery();
        for(int i in minReview..4)
          bq3.add(new TermQuery(new Term("i2geo.reviewOverallRanking","" + i)),should);
        bq2.add(bq3, must);
        does=true;
      }
      does=addMustQueryIfUseful(bq2,"CurrikiCode.AssetLicenseClass.licenseType",license,analyzer,true)||does;
      LOG.warn("CreateResourcesQuery " + does + " : " + bq2);
      if(!does) return null;
      bq.add(bq2,must);
      return bq;
    } catch(Exception ex) { ex.printStackTrace(); throw ex;}
  }


  private boolean addMustQueryIfUseful(BooleanQuery bq, String fieldname, String value, Analyzer analyzer, boolean allMatch) {
    addMustQueryIfUseful(bq, fieldname, value, analyzer, allMatch, false);
  }
  private boolean addMustQueryIfUseful(BooleanQuery bq, String fieldname, String value, Analyzer analyzer, boolean allMatch, boolean alsoStemmed) {
    if(value==null || value.length()==0 || "-".equals(value) || "-<Del>".equals(value) || "*".equals(value)) return false;
    Query q = new QueryParser(fieldname, analyzer).parse(value),
      q2 = new QueryParser(fieldname + ".stemmed", analyzer).parse(value);
    if(q==null) return false;
    BooleanQuery qq = new BooleanQuery();
    qq.add(q, should);
    qq.add(q2, should);
    bq.add(qq,must);
    return true;
    /* if(q!=null) {
      if(q instanceof BooleanQuery) {
        for(BooleanClause bc: ((BooleanQuery)q).getClauses()) {
          if(bc.getOccur()== should && allMatch) {
            qq.add(bc.getQuery(),must);
            LOG.warn("influencing: " + bc.getQuery());
          } else {
            LOG.warn("cloning " + bc);
            qq.add((BooleanClause) bc);
          }
        }
        q = qq;
      } else {
        //bq.add(q,must);
      }
      //
      if(alsoStemmed) {
        BooleanQuery alt = new BooleanQuery();
        alt.add(q, should);
        Query stemmed = changeTermQueryFields(((Query) q.clone()), fieldname + ".stemmed");
        stemmed.setBoost(0.5f);
        alt.add(stemmed,should);
        q = alt;
      bq.add(q,must);
      return true;
    }
    return false;
      }*/

  }

  /* private Query changeTermQueryFields(Query q, String fieldname) {
    if(q instanceof TermQuery) {
      TermQuery tq = (TermQuery) q;
      return new TermQuery(new Term(fieldname, tq.term.text));
    } else if ((q instanceof BooleanQuery)) {
      BooleanQuery bq = (BooleanQuery) q;
      for(clause in bq.clauses) {
        clause.setQuery(changeTermQueryFields(clause.query, fieldname));
      }
      return bq;
    }
  }*/

  public boolean addGeoSkillsQueryIfUseful(BooleanQuery bq, String fieldName, String ancestorFieldName, String nodeFragIds, Analyzer analyzer) {
    if(nodeFragIds==null || nodeFragIds.trim().length()==0) return false;
    Query q = new QueryParser(fieldName, analyzer).parse(nodeFragIds);
    if(null==q) return false;
    if(ancestorFieldName != null) {
      if(! (q instanceof BooleanQuery)) {
        BooleanQuery qq = new BooleanQuery();
        qq.add(q,must);
        q = qq;
      }
      Query q2 = new BooleanQuery();
      for(i in 0..(q.clauses.length-1)) {
        Query tq = q.clauses[i].query;
        if(tq instanceof TermQuery && fieldName==tq.term.field()) {
          String fragId = tq.term.text();
          def newTermQuery = new TermQuery(new Term(ancestorFieldName,fragId));
          newTermQuery.setBoost(tq.boost);
          q2.add(newTermQuery, q.clauses[i].occur);
          if(fragId.endsWith("_r") && fragId.length()>2) {
            newTermQuery = new TermQuery(new Term(ancestorFieldName,fragId.substring(0,fragId.length()-2)));
            newTermQuery.setBoost(tq.boost);
            q2.add(newTermQuery, q.clauses[i].occur);
          }
        }
      }
      BooleanQuery ret = new BooleanQuery();
      ret.add(q,should); ret.add(q2,should);
      bq.add(ret,must);
      return true;
    } else {
      bq.add(q,must);
      return true;
    }
  }

  private boolean addAuthorQuery(BooleanQuery bq, String fieldName, String authorQ, Analyzer analyzer) {
    if(authorQ==null || "".equals(authorQ.trim())) return false;
    BooleanQuery q = getQueryForAuthorsLike(authorQ, fieldName, analyzer);
    if(q==null || q.getClauses().length==0) {
      warnMessages.add(msg.get("search.form.no-author-found"));
      return false
    }
    bq.add(q, must);
    return true;
  }

  private BooleanQuery getQueryForAuthorsLike(String like, String fieldname, Analyzer analyzer) {
    BooleanQuery bq = new BooleanQuery();
    Query q = new QueryParser("XWiki.XWikiUsers.first_name",analyzer).parse(like); bq.add(q, should);
    q= new QueryParser("XWiki.XWikiUsers.first_name.stemmed",analyzer).parse(like); q.setBoost(0.5f); bq.add(q, should);
    q=new QueryParser("XWiki.XWikiUsers.last_name",analyzer).parse(like); bq.add(q, should);
    q=new QueryParser("XWiki.XWikiUsers.last_name.stemmed",analyzer).parse(like); q.setBoost(0.5f); bq.add(q, should);
    bq.add(new QueryParser("XWiki.XWikiUsers.email",analyzer).parse(like), should);
    bq.add(new QueryParser("XWiki.XWikiUsers.affiliation",analyzer).parse(like), should);
    q=new QueryParser("XWiki.XWikiUsers.affiliation.stemmed",analyzer).parse(like); q.setBoost(0.5f); bq.add(q, should);
    Query ftQ = new QueryParser("ft",analyzer).parse(like);
    ftQ.setBoost(0.5f); bq.add(ftQ,should);
    BooleanQuery bq2 = new BooleanQuery();
    bq2.add(bq,must); bq2.add(new TermQuery(new Term("object","XWiki.XWikiUsers")),must);
    LOG.warn("author query " + bq2);
    BooleanQuery r = new BooleanQuery();
    Hits h = plugin.getLuceneHits(bq2,null);
    LOG.warn("obtained "  + h.length() + " hits.");
    if(h.length()==0) return null;
    int max = Math.min(h.length(),500);
    for(int i in 0..(max-1)) {
      String t = h.doc(i).get("fullname");
      r.add(new TermQuery(new Term(fieldname,t)),should);
    }
    if(r.getClauses().length==0) return null;
    else return r;
  }

  // ============================================================================================
  public boolean executeQuery(Query q, String start, String max) {
    if(q==null) return false;
    LOG.warn("Launching query " + q);
    this.hits = plugin.getLuceneHits(q,sortFields.split(","));
    LOG.warn("Hits " + this.hits + " of size " + hits.length());
    if(start!=null && start.length()>0)
      pos = Integer.parseInt(start);
    if(max!=null && max.length()>0)
      this.max = Integer.parseInt(max);
    else
      this.max = pos+maxPageLength;
    if(pos<0 || pos>=hits.length()) pos = 0;
    this.start = pos;
    this.max = Math.min(this.max, hits.length())
    return hits.length()>0;
  }

  public String cleanUp(String s) {
    if(s==null) return "";
    return s.replaceAll("<[^>]+>","")
  }

  public Object next() {
    Object r=null;
    if(pos>=0 && pos<max)
      r= hits.doc(pos);
    pos++;
    return r;
  }

  public int getPos() { return pos;}

  public int getStart() { return start;}
  public int getMax() { return max; }

  public int getNumHits() { return hits.length(); }

  public boolean hasNext() { return hits!=null && pos<max;}

  public float getScore() {
    return hits.score(pos);
  }

  public boolean hasNextPage() {
    return max < hits.length();
  }

  public boolean hasPreviousPage() {
    return start>0;
  }

  public int getNextPageStart() {
    return start+maxPageLength;
  }
  public int getPreviousPageStart() {
    return start- maxPageLength;
  }

  public String getURLtoHereWithoutStart(HttpServletRequest request) {
    String queryString = request.getQueryString();
    if(queryString==null) queryString = "";
    queryString = queryString.replaceAll("&start=[0-9]+","");
    return request.getContextPath() + "/" + request.getServletPath() + request.getPathInfo()+ "?" + queryString;
  }


  public void remove() { throw new UnsupportedOperationException(); }

  private String lang3toLang2(String lang3) {
    return msg.get("languages3_2." + lang3);
  }

  private String remapFileTypes(String type) {
    if(type==null || type.length()==0) return null;
    if("cabri".equals(type)) {
      return "cabri2plusfile cabri2plusFile cabri2File cabri2file fig";
    } else if("g2w".equals(type)) return "g2w";
    else if ("wiris".equals(type)) return "wirishtml"
    else if("txt".equals(type)) return "txt tracenpocheFile"
    else return type;
  }

  public ArrayList getWarnMessages() { return warnMessages; }


  private void trash() {
    def doc = xwiki.getDocument("XWiki.SouryLavergne",context);
    println("doc is " + doc);
    println();
    def ddoc = doc.getDocument();
    println("ddoc is " + ddoc + " of class " + ddoc.getClass());
    println();

    for (className in ddoc.getxWikiObjects().keySet()) {
      for (obj in ddoc.getObjects(className)) {
        println ""
        println ("<hr/>")
        println " obj (" + className + "): " + obj;

        StringBuffer contentText = new StringBuffer();
        for(baseProperty in obj.getProperties()) {
          if (baseProperty.getValue() != null) {
            contentText.append(baseProperty.getValue().toString());
            contentText.append(" ");
          }
        }
        println "text: " + contentText;


      }
    }


  }

}