package net.i2geo.index.rsearch;

import org.apache.lucene.search.*;
import org.apache.lucene.index.Term;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.queryParser.ParseException;

import java.util.*;
import java.io.StringReader;
import java.io.IOException;

import net.i2geo.index.analysis.AnalyzerPack;
import net.i2geo.index.IndexHome;
import net.i2geo.api.search.UserQuery;
import net.i2geo.api.OntType;

/** Class to expand a user-side query into a server-side query.
 */
public class RSearchQueryExpander implements RSConstants {

    public RSearchQueryExpander(RSearchContext context, IndexHome termIndex, RSOntologyAccess onto) {
        this.context = context;
        this.onto = onto;
        this.indexHome = termIndex;
    }

    private final RSearchContext context;
    private final RSOntologyAccess onto;
    private List<String> messages = null;
    private final IndexHome indexHome;

    private static final BooleanClause.Occur
        MUST = BooleanClause.Occur.MUST,
        SHOULD= BooleanClause.Occur.SHOULD,
        MUST_NOT = BooleanClause.Occur.MUST_NOT;

    private String fieldNameEduLevel = RSConstants.EDULEVEL_FIELDNM,
        fieldNameTrainedTopicsAndCompetencies = RSConstants.FIELDNM_TOPICS_AND_COMPETENCIES;

    public void setFieldNames(String eduLevelFN, String trainedTopcFN) {
        this.fieldNameEduLevel = eduLevelFN;
        this.fieldNameTrainedTopicsAndCompetencies = trainedTopcFN;
    }

    public void setMessagesList(List<String> messages) {
        this.messages = messages;
    }
    public Query expandToLuceneQuery(UserQuery userQuery) throws ParseException {
        return expandToLuceneQuery(peelTerms(userQuery.getTerms()));
        // TODO: use userQuery's expansionLevel
    }

    /** A form of query parsing from plain-text tokenizing through commas then
     * differentiating identifiers from word search terms and expanding
     * word-sets to the possible alternatives. */
    public BooleanQuery peelTerms(String plainText) throws ParseException {
        if(plainText==null || plainText.length()==0) return new BooleanQuery();
        QueryParser parser = new QueryParser("text", new WhitespaceAnalyzer());
        Query parsed = parser.parse(plainText);
        BooleanQuery query;
        if(! (parsed instanceof BooleanQuery)) {
            query = new BooleanQuery();
            query.add(parsed,BooleanClause.Occur.MUST);
        } else {
            query = (BooleanQuery) parsed;
        }
        adjustTermQueries(query);

        return query;
    }

    private Query adjustTermQueries(Query query) {
        if(query instanceof BooleanQuery ) {
            BooleanQuery bq = (BooleanQuery) query;
            // then recognize queries for gs-nodes in words
            for(BooleanClause clause: bq.getClauses()) {
                if(clause==null) continue;
                Query q = clause.getQuery();
                clause.setQuery(adjustTermQueries(q));
            }
            return bq;
        } else if (query instanceof TermQuery) {
            TermQuery tq = (TermQuery) query;
            String txt = tq.getTerm().text();
            if(txt.startsWith("#") || txt.startsWith("http://i2geo.net"))
                tq = new TermQuery(new Term("gs",txt));
            else {
                tq = new TermQuery(new Term("text",txt));
            }
            return tq;
        } else {
            return query;
        }
    }

    public void recognizeGSNodesInTextQueries(List<TermQuery> tqs) {
        for(ListIterator<TermQuery> listIt = tqs.listIterator(); listIt.hasNext(); ) {
            TermQuery q = listIt.next();
            if("text".equals(q.getTerm().field())) {
                for(TermQuery tq:makeGSNodeQueriesForApproaching(q.getTerm().text())) {
                    listIt.add(tq);
                }
            }
        }
    }




    public Query expandToLuceneQuery(List<TermQuery> inputQueries) {
        // do entity-recognition
        recognizeGSNodesInTextQueries(inputQueries);
        // create disjunction of the queries
        BooleanQuery superficialQuery = new BooleanQuery();
        for(TermQuery bit: inputQueries) {
            superficialQuery.add(bit, SHOULD);
        }

        return expandToLuceneQuery(superficialQuery);
    }


    public Query expandToLuceneQuery(BooleanQuery userSideQuery) {
        Iterator<BooleanClause> it = ((List<BooleanClause>)userSideQuery.clauses()).iterator();
        while(it.hasNext()) {
            BooleanClause clause = it.next();
            if(clause.getQuery() instanceof TermQuery) {
                TermQuery tq = (TermQuery) clause.getQuery();
                Query expQ = expandUserTermQuery(tq);
                if(expQ instanceof BooleanQuery)
                    expQ = removeEmptyQueries((BooleanQuery) expQ);
                if(expQ !=null)
                    clause.setQuery(expQ);
                else
                    it.remove();
            } else if (clause.getQuery() instanceof BooleanQuery){
                clause.setQuery(expandToLuceneQuery((BooleanQuery)clause.getQuery()));
            } else if (clause.getQuery() instanceof PhraseQuery) {
                PhraseQuery pq = (PhraseQuery) clause.getQuery();
                Term[] termsSource = pq.getTerms();
                PhraseQuery targetPQ = new PhraseQuery();
                try {
                    for(int i=0, l=termsSource.length; i<l; i++) {
                        if("text".equals(termsSource[i].field())) {
                            // now run the analyzer to split in unstemmed tokens (using standard-analyzer for fancy stuffs)
                            StandardAnalyzer an = new StandardAnalyzer();
                            TokenStream stream = an.tokenStream("ft",new StringReader(termsSource[i].text()));
                            for(Token tok = stream.next(); tok!=null; tok = stream.next()) {
                                targetPQ.add(new Term("ft",tok.term()));
                            }
                        } // TODO: would need to expand to title etc... similarly to #makeTextQuery
                    }
                } catch (IOException e) {
                    e.printStackTrace(); // an IOException will not happen with StringReader
                }
                clause.setQuery(targetPQ);
            } else if (clause.getQuery() instanceof FuzzyQuery) {
                FuzzyQuery fq = (FuzzyQuery) clause.getQuery();
                FuzzyQuery fq2 = new FuzzyQuery(fq.getTerm(),fq.getMinSimilarity(),fq.getPrefixLength());
                fq2.setBoost(fq.getBoost());
                clause.setQuery(fq2);
            }
            // TODO: probably other query types, in particular RangeQueries.
        }
        return userSideQuery;
    }

    private Query removeEmptyQueries(Query q) {
        if(q==null) return null;
        if(!(q instanceof BooleanQuery)) return q;
        BooleanQuery bq = (BooleanQuery) q;
        Iterator<BooleanClause> it = ((List<BooleanClause>)bq.clauses()).iterator();
        while(it.hasNext()) {
            BooleanClause clause = it.next();
            Query qq = removeEmptyQueries(clause.getQuery());
            if(qq==null) it.remove();
        }
        if(bq.clauses().isEmpty()) return null;
        return bq;
    }

    private Query expandUserTermQuery(TermQuery userQuery) {
        Term t = userQuery.getTerm();
        String f = t.field();
        if("".equals(t.text()) || null==t.text()) return null;
        if("text".equals(f)) {
            Query textQuery = makeTextQuery(t.text(), context.getAcceptedLanguages());
            textQuery.setBoost(userQuery.getBoost());
            return textQuery;
        } else if(GSNODES_FIELDNMS.contains(f) || fieldNameEduLevel.equals(f) || fieldNameTrainedTopicsAndCompetencies.equals(f) || "gs".equals(f)) {
            String uri = t.text();
            BooleanQuery q = new BooleanQuery();
            // recognize uri type
            // first check if _r could be added... if yes, use that, if no, fine
            if(onto.doesNodeExist(uri + "_r"))
                uri = uri + "_r";
            String type = onto.getOntType(uri);
            if(onto.isCompetencyNode(type)) {
                q.add(new GSTermQuery(fieldNameTrainedTopicsAndCompetencies,uri,OntType.COMPETENCY),SHOULD);

                try {
                    // ===== for included topics ====
                    // example: apply intercept-theorem => add => proportionality, thales-config, ratio
                    Set<String> included = onto.getIncludedTopics(uri);
                    BooleanQuery subQ = new BooleanQuery();
                    subQ.setBoost(0.5f);
                    q.add(subQ,SHOULD);
                    for(String i:included) {
                        subQ.add(new GSTermQuery(RSConstants.FIELDNM_ANCESTORS,i,OntType.TOPIC),SHOULD);
                    }
                    // add verbs
                    for(String cls: onto.getOntoClasses(uri)) {
                        q.add(new GSTermQuery(RSConstants.FIELDNM_ANCESTORS,cls,OntType.TOPIC),SHOULD);
                    }
                } catch(Exception ex) {
                    ex.printStackTrace();
                    if(messages!=null) messages.add("Expansion issue: " + ex);
                }
                // (query competencies doing that?)
            } else if(onto.isTopicNode(type)) {
                try {
                    q.add(new GSTermQuery(fieldNameTrainedTopicsAndCompetencies,uri,OntType.TOPIC),SHOULD);
                    //q.add(new GSTermQuery(fieldNameTrainedTopicsAndCompetencies,uri,OntType.TOPIC),SHOULD);
                    String u = uri;
                    if(u.endsWith("_r")) u = u.substring(0,u.length()-2);
                    Query parentQ = new GSTermQuery(RSConstants.FIELDNM_ANCESTORS,u,OntType.TOPIC);
                    parentQ.setBoost(0.8f);
                    q.add(parentQ,SHOULD);

                } catch (Exception e) {
                    e.printStackTrace();
                    if(messages!=null) messages.add("Expansion issue " + e);
                }

            } else if (onto.isLevelNode(type)) {
                // TODO: parent levels as well ? Relatives
                return new GSTermQuery(fieldNameEduLevel,uri, OntType.LEVEL);
                // ?? ... for text with the names in the nodes?
            } else throw new IllegalArgumentException("Don't understand uri \""+uri+"\".");

            q.setBoost(userQuery.getBoost());
            return q;
        }
        else throw new IllegalArgumentException("Can't process field-name " + userQuery);
    }

    private BooleanQuery makeTextQuery(String text, List<String> langs) {

        BooleanQuery q = new BooleanQuery();
        int count=0;
        for(String lang: langs) {
            count++;
            if(count>3) break;
            Query qInLang = makeTextQuery(text,lang);
            qInLang.setBoost(0.5f+count*(0.5f/3));
            q.add(qInLang,SHOULD);
        }
        Query qInLang = makeTextQuery(text,"x-all");
        qInLang.setBoost(0.5f);
        q.add(qInLang,SHOULD);
        return q;
    }

    private BooleanQuery makeTextQuery(String text, String lang) {
        Analyzer an = AnalyzerPack.getAnalyzerForContext(context);
        String lang3Letters = null;
        if(!"x-all".equals(lang)) lang3Letters = new Locale(lang).getISO3Language();
        String textFld = "ft",
            titleFld = "title";

        List<String> stemmedTokens = AnalyzerPack.tokenizeString(an,textFld, text),
            unstemmedTokens = AnalyzerPack.tokenizeString(new StandardAnalyzer(),"title-x-all", text);

        BooleanQuery bq = new BooleanQuery();
        // first with phrase match
        // (text:x -> title:x^2 or text-langs:x or text-x-all:x^0.5)

        // phrase match with high boost
        if(!unstemmedTokens.isEmpty()) {
            PhraseQuery pq = new PhraseQuery();
            for(String w:unstemmedTokens)
                pq.add(new Term(titleFld,w));
            pq.setBoost(2.0f);
            bq.add(pq,SHOULD);
            pq = new PhraseQuery();
            for(String w:unstemmedTokens) pq.add(new Term(textFld,w));
            pq.setBoost(1.5f);
            bq.add(pq,SHOULD);
        }


        // then text match (title, text) stemmed
        for(String w:stemmedTokens) {
            TermQuery tq = new TermQuery(new Term(titleFld + ".stemmed",w));
            tq.setBoost(1.2f);
            bq.add(tq,SHOULD);
        }
        for(String w:stemmedTokens) {
            TermQuery tq = new TermQuery(new Term(textFld + ".stemmed",w));
            tq.setBoost(1.0f);
            bq.add(tq,SHOULD);
        }
        BooleanQuery r;
        if(!bq.clauses().isEmpty() && lang3Letters!=null) {  // this query only for this language
            r = new BooleanQuery();
            r.add(bq,BooleanClause.Occur.MUST);
            r.add(new TermQuery(new Term("CurrikiCode.AssetClass.language",lang3Letters)),BooleanClause.Occur.MUST);
        } else r = bq;
        return r;
    }


    /** Expands the queries for a text phrase to a query for a few matching geoskills node.
     *
     * @param text the text to be looked in within the various names of the geoskills' nodes
     * @return a query of geoskills nodes
     */
    private List<TermQuery> makeGSNodeQueriesForApproaching(String text) {
        // fetch nodes
        List<BoostedText> uris = onto.fetchNodesMatchingApprox(text, context.getAcceptedLanguages(), 3,false);
        List<TermQuery> r = new ArrayList<TermQuery>(uris.size());
        for(BoostedText bt: uris) {
            TermQuery q = new TermQuery(new Term("gs",bt.text));
            q.setBoost(bt.boost);
            r.add(q);
        }
        return r;
    }

    public String shortenURIForPlatform(String u) {
        if(u.startsWith(RSOntologyAccess.ontBaseU))
            u = u.substring(RSOntologyAccess.ontBaseU.length());
        if(!u.startsWith("#")) u = "#" + u;
        return u;
    }

}
