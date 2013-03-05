package net.i2geo.index;

import junit.framework.TestCase;
import net.i2geo.index.analysis.AnalyzerPack;
import net.i2geo.onto.GeoSkillsAccess;
import net.i2geo.api.OntType;
import net.i2geo.index.analysis.SKBAnalyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.highlight.*;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.index.Term;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.commons.io.FileUtils;
import org.apache.lucene.util.Version;

import java.util.*;
import java.io.File;
import java.net.URLClassLoader;

/** A batch of test-queries directly operated on the current GeoSkills. This still assumes the index is built before.
 */
public class TestAFewIndexQueries extends TestCase {

    private static boolean doRebuild = false;

    private static GeoSkillsAccess access =  GeoSkillsAccess.getTestInstance();
    static {
        try {
            new File("target/index").mkdirs();
            if(doRebuild) FileUtils.cleanDirectory(new File("target/index"));
        }catch(Exception ex) { throw new IllegalStateException("can't start.",ex);}
    }
    private static IndexHome indexHome = IndexHome.getInstance("target/index",true);
    private static SKBQueryExpander queryExpander = new SKBQueryExpander(Arrays.asList(new String[]{"en","es","de"}));



    protected void setUp() throws Exception {
        System.out.println("Status : " + indexHome.getCurrentStatus() + " for " + super.toString() );
        if(indexHome.getCurrentStatus() != IndexHome.Status.READ_READY)
            indexHome.open(true);
    }

    protected void tearDown() throws Exception {
        indexHome.close();
    }

    public TestAFewIndexQueries(String name) { super(name); }

    public void testCompetencyProcessIsHere() throws Exception {
        Query query = new TermQuery(new Term("ontType", OntType.COMPETENCYPROCESS.getName()));
        IndexSearcher searcher = indexHome.getSearcher();
        System.out.println("Searcher is " + searcher + " for status " + indexHome.getCurrentStatus());
        TopDocs hits = searcher.search(query, 100);
        System.out.println("Found " + hits.totalHits + " documents of type competency-process");
        assertTrue("There should be many competency processes.", hits.totalHits>10);
        for(int i=0; i< Math.min(hits.totalHits, 100); i++) {
            System.out.println(" - " + indexHome.getReader().document(hits.scoreDocs[i].doc).get("uri"));
        }
    }

    public void testAbstractTopicsAreHere() throws Exception {
        IndexSearcher searcher = indexHome.getSearcher();
        Query query = new TermQuery(new Term("ontType",OntType.ABSTRACTTOPIC.getName()));
        TopDocs hits = searcher.search(query, 100);
        System.out.println("Found " + hits.totalHits + " documents of type abstractTopic");
        assertTrue("There should be several abstractTopics.", hits.totalHits>1);
        for(int i=0; i< Math.min(hits.totalHits, 100); i++) {
            System.out.println(" - " + indexHome.getReader().document(hits.scoreDocs[i].doc));
        }

        // there should be several concrete topics
        query = new TermQuery(new Term("ontType",OntType.CONCRETE_TOPIC.getName()));
        hits = searcher.search(query, 100);
        System.out.println("Found " + hits.totalHits + " documents of type concreteTopic");
        assertTrue("There should be several concreteTopic.", hits.totalHits>1);

        // there should be several pure abstract topics
        query = new TermQuery(new Term("ontType",OntType.PURE_ABSTRACT_TOPIC.getName()));
        hits = searcher.search(query, 100);
        System.out.println("Found " + hits.totalHits + " documents of type pureAbstractTopic");
        assertTrue("There should be several pureAbstractTopic.", hits.totalHits>1);

        // there should be several pure abstract topics
        query = new TermQuery(new Term("ontType",OntType.ABSTRACTTOPIC_WITH_REPRESENTATIVE.getName()));
        hits = searcher.search(query, 100);
        System.out.println("Found " + hits.totalHits + " documents of type abstractTopicWithRepresentative");
        assertTrue("There should be several abstractTopicWithRepresentative.", hits.totalHits>1);
    }

    public void testBasicQueryExpansion() throws Exception {
        Query query = queryExpander.expandQuery("blip blop",new String[]{"someType"},Arrays.asList(new String[]{"en"}),true);
        System.out.println("Expanded query \"blip blop\" to " + query);
        // verify there's enough terms
        Set<String> fields = new HashSet<String>(), texts = new HashSet<String>();
        collectTermQueries(query,null,fields,texts);
        assertTrue("There should be English",fields.contains("name-en"));
        assertTrue("There should be x-all", fields.contains("name-x-all"));
        System.out.println(texts);
        assertEquals(
                "blip, blop, uri, and someType should be the terms",
                new HashSet<String>(Arrays.asList(//"[start]","[end]",
                        "http://www.inter2geo.eu/2008/ontology/GeoSkills#blip blop",
                        "blop","blip","someType")),
                texts);
    }

    public void testMultipleCommonNamesUsed() throws Exception {

    }

    private void collectTermQueries(Query q, Set<Query> termQueries, Set<String> fields, Set<String> texts) {
        if(q instanceof TermQuery) {
            TermQuery tq = (TermQuery) q;
            if(termQueries!=null) termQueries.add(tq);
            if(fields!=null) fields.add(tq.getTerm().field());
            if(texts!=null) texts.add(tq.getTerm().text());
        } else if(q instanceof PrefixQuery) {
            PrefixQuery pq = (PrefixQuery) q;
            if(termQueries!=null) termQueries.add(new TermQuery(pq.getPrefix()));
            if(fields!=null) fields.add(pq.getPrefix().field());
            if(texts!=null) texts.add(pq.getPrefix().text());
        } else if(q instanceof PhraseQuery) {
            PhraseQuery pq = (PhraseQuery) q;
            Term[] terms = pq.getTerms();
            for(int i=0, l = terms.length; i<l; i++) {
                if(termQueries!=null) termQueries.add(new TermQuery(terms[i]));
                if(texts!=null) texts.add(terms[i].text());
            }
        } else if(q instanceof SpanTermQuery) {
            SpanTermQuery spq = (SpanTermQuery) q;
            if(termQueries!=null) termQueries.add(new SpanTermQuery(spq.getTerm()));
            if(fields!=null) fields.add(spq.getTerm().field());
            if(texts!=null) texts.add(spq.getTerm().text());
        } else if(q instanceof FuzzyQuery) {
            FuzzyQuery fq = (FuzzyQuery) q;
            if(termQueries!=null) termQueries.add(new TermQuery(fq.getTerm()));
            if(fields!=null) fields.add(fq.getTerm().field());
            if(texts!=null) texts.add(fq.getTerm().text());
        } else if(q instanceof BooleanQuery) {
            for(BooleanClause clause: (List <BooleanClause>) (((BooleanQuery) q).clauses())) {
                collectTermQueries(clause.getQuery(),termQueries, fields, texts);
            }
        } else if(q instanceof SpanNearQuery) {
            for(SpanQuery spq: (((SpanNearQuery) q).getClauses())) {
                collectTermQueries(spq,termQueries, fields, texts);
            }
        } else throw new IllegalArgumentException("Unknown query type " + q.getClass() + " : " + q);
    }

    public void testNonDefaultCommonNameMatch() throws Exception {
        IndexSearcher searcher = indexHome.getSearcher();
        List<String> langs = Arrays.asList(new String[]{"nl"});
        queryExpander = new SKBQueryExpander(langs);
        Query query = queryExpander.expandQuery("eerste klas VMBO",new String[]{},langs,true);
        System.out.println("Query: " + query);
        TopDocs hits = searcher.search(query, 10);
        System.out.println("Found " + hits.totalHits + " with \"erste klas VMBO\" inside");

        Set<String> s = new HashSet<String>();
        for(int i=0; i<hits.totalHits; i++) {
            System.out.println("- match: " + indexHome.getReader().document(hits.scoreDocs[i].doc));
            s.add(indexHome.getReader().document(hits.scoreDocs[i].doc).get("uri"));
        }
        assertTrue("VMBO_1 should be a match",s.contains("http://www.inter2geo.eu/2008/ontology/GeoSkills#VMBO_1"));
        System.err.println(indexHome.getSearcher().explain(query, hits.scoreDocs[1].doc).toString());

        query = queryExpander.expandQuery("brugklas",new String[]{},langs,true);
        for(int i=0; i<hits.totalHits; i++) {
            Document doc = indexHome.getReader().document(hits.scoreDocs[i].doc);
            System.out.println("- match: " + doc.get("uri"));
            s.add(doc.get("uri"));
        }
        assertTrue("VMBO_1 should be a match brugklass",s.contains("http://www.inter2geo.eu/2008/ontology/GeoSkills#VMBO_1"));
    }

    public void testHighlighter() throws Exception {
        QueryParser parser = new QueryParser(Version.LUCENE_35, "name-nl", AnalyzerPack.getAnalyzerForLanguage("nl"));
        Query query = parser.parse("klas");
        IndexSearcher searcher = indexHome.getSearcher();
        TopDocs hits = indexHome.getSearcher().search(query, 10);
        SimpleHTMLFormatter htmlFormatter = new SimpleHTMLFormatter();
        Highlighter highlighter = new Highlighter(htmlFormatter, new QueryScorer(query));
        for (int i = 0; i < Math.min(hits.totalHits,10); i++) {
          int id = hits.scoreDocs[i].doc;
          Document doc = searcher.doc(id);
          System.out.println(" - " + doc.get("uri"));
            StringBuffer b = new StringBuffer();
            for(String text: doc.getValues("name-nl")) {
                b.append(text); b.append("\n");
            }
          String text = toString() ;
          TokenStream tokenStream = TokenSources.getAnyTokenStream(searcher.getIndexReader(), id, "name-nl", AnalyzerPack.getAnalyzerForLanguage("nl"));
          TextFragment[] frag = highlighter.getBestTextFragments(tokenStream, text, false, 1);
          for (int j = 0; j < frag.length; j++) {
            if ((frag[j] != null) && (frag[j].getScore() > 0)) {
              System.out.println(frag[j].getScore() + ": "+(frag[j].toString()));
            }
          }
          //Term vector
          /* text = doc.get("tv");
          tokenStream = TokenSources.getAnyTokenStream(searcher.getIndexReader(), hits.scoreDocs[i].doc, "tv", analyzer);
          frag = highlighter.getBestTextFragments(tokenStream, text, false, 10);
          for (int j = 0; j < frag.length; j++) {
            if ((frag[j] != null) && (frag[j].getScore() > 0)) {
              System.out.println((frag[j].toString()));
            }
          }
          System.out.println("-------------");
          */
        }
    }


    public void testNeunteKlasseComesOut() throws Exception {
        IndexSearcher searcher = indexHome.getSearcher();
        List<String> langs = Arrays.asList(new String[]{"en"});
        queryExpander = new SKBQueryExpander(langs);
        Query query = queryExpander.expandQuery("Strecke",new String[]{},langs,true);
        TopDocs hits = searcher.search(query, 100);
        System.out.println("Found " + hits.totalHits + " with strecke inside");
        int n = hits.totalHits;
        for(ScoreDoc sd: hits.scoreDocs) {
           System.out.println("- " + sd.score + " : " + indexHome.getReader().document(sd.doc).get("uri"));
        }
        assertTrue("There should be some documents with Strecke inside.", hits.totalHits>=1);


        Analyzer analyzer = new SKBAnalyzer(true,IndexHome.supportedLanguages);

        for(ScoreDoc sd: hits.scoreDocs) {
            String matchedField = indexHome.computeMatchedField(sd.doc, indexHome.getReader().document(sd.doc),analyzer,query);
            if(matchedField==null) continue;
            System.out.println("matchedField: " + matchedField);
            assertEquals("Matching field is different then preferred title: matched Strecke with English preferred.",
                    "<B>Strecke</B>",matchedField);
        }
    }

    public void testUriMatch() throws Exception {
        IndexSearcher searcher = indexHome.getSearcher();
        List<String> langs = Arrays.asList(new String[]{"en"});
        queryExpander = new SKBQueryExpander(langs);
        Query query = queryExpander.expandQuery("Bachillerato_Ciencias_y_Tecnologia_2",new String[]{},langs,true);
        System.out.println("Query: " + query);
        TopDocs hits = searcher.search(query, 100);
        System.out.println("Found " + hits.totalHits + " with Bachillerato_Ciencias inside");
        for(ScoreDoc sd: hits.scoreDocs) {
            System.out.println("- " + sd.score + " : " + indexHome.getReader().document(sd.doc).get("uri"));
            //System.out.println(indexHome.getSearcher().explain(query,hit.getId()));
        }
        assertTrue("There should be some documents with Bachillerato_Ciencias_y_Tecnologia_2 inside.", hits.totalHits>1);

        Analyzer analyzer = new SKBAnalyzer(true,IndexHome.supportedLanguages);
        String matchedField = indexHome.computeMatchedField(hits.scoreDocs[0].doc, indexHome.getReader().document(hits.scoreDocs[0].doc),analyzer,query);
        System.out.println("Matched Field is \"" + matchedField + "\".");
        //assertTrue("Matching field is uri-value.", matchedField.contains("Bachillerato_Ciencias_y_Tecnologia_2"));
    }

    // TODO: test URI is queryable

    // TODO: test neunte klasse is queryable

    public void testDumbSpanQ() throws Exception {
        indexHome.startWriting();
        Document doc = new Document();
        doc.add(new Field("xx","aa bb cc", Field.Store.YES, Field.Index.ANALYZED));
        indexHome.getWriter().addDocument(doc);
        indexHome.stopWriting();
        SpanTermQuery[] queries = new SpanTermQuery[] {
            new SpanTermQuery(new Term("xx","aa")),
                new SpanTermQuery(new Term("xx","bb")),
                new SpanTermQuery(new Term("xx","cc")), 
        };
        Query q = new SpanNearQuery(queries,20,false);
        TopDocs h = indexHome.getSearcher().search(q, 100);
        assertTrue("Need to find it with a correct query: aa bb cc",h.totalHits>0);

        queries = new SpanTermQuery[] {
            new SpanTermQuery(new Term("xx","aa")),
                new SpanTermQuery(new Term("xx","dd")),
                new SpanTermQuery(new Term("xx","cc")),
        };
        q = new SpanNearQuery(queries,20,false);
        h = indexHome.getSearcher().search(q, 100);
        assertFalse("Should not find it without a proper term.", h.totalHits>0);
    }


    public void testSubjectHasSomething() throws Exception {
        BooleanQuery q = SubjectsCollector.getQuery(indexHome, "http://inter2geo.eu/2008/ontologies/Subjects#Geometry");
        BooleanQuery.setMaxClauseCount(10196);
        assertNotNull("The subject Geometry yields a query.",q);
        System.out.println("Clauses: " + q.clauses());
        assertTrue("There should be some nodes in Geometry",!q.clauses().isEmpty());
    }

    public void testCompetencyAncestor() throws Exception {
        System.out.println("For verbalize_dependency_relations_in_real_world");
        List<String> parents = Arrays.asList(indexHome.getDocForUri("http://www.inter2geo.eu/2008/ontology/GeoSkills#verbalize_dependency_relations_in_real_world").getValues("ancestorTopic"));
        for(String p : parents) {
            System.out.println("- parent: " + p);
        }
        assertTrue("verbalize should be parent", parents.contains("Verbalize"));
        assertTrue("dependency relation should be parent", parents.contains("Dependency_relation"));
    }
}
