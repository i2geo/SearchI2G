package net.i2geo.index;

import net.i2geo.api.GeoSkillsConstants;
import net.i2geo.onto.GeoSkillsAccess;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.document.*;

import java.io.IOException;
import java.util.*;
import java.net.URI;
import java.net.URL;

import net.i2geo.index.rsearch.RSConstants;
import net.i2geo.onto.SubjectsOntologyToGSNodesList;
import org.apache.lucene.util.Version;

/** Simple class that uses the Subjects ontology to assemble sets of queries
 * the enable the rendering of resources of this subject.
 */
public class SubjectsCollector {

    public SubjectsCollector(URL subjectOntURL) {
        this.subjectOntURL = subjectOntURL;
        System.out.println("Loading subjects from " + subjectOntURL);
    }

    private static final String 
        subjectURIFieldName = "subjectURI",
        nodeURIFieldName = "nodeURI";
    private URL subjectOntURL = null;

    private static final Field typeField =
            new Field("type-of-record","subject-content", Field.Store.NO, Field.Index.NOT_ANALYZED);


    public void run(IndexHome index) throws Exception {
        // delete older records
        index.getReader().deleteDocuments(
                new Term(typeField.name(),typeField.stringValue()));
        index.commitDeletions();

        try {
// now write the subjects
            GSILogger.log("=================================");
            GSILogger.log("Starting subjects processing.");
            synchronized(SubjectsCollector.class) {
                index.startWriting();
                IndexWriter writer = index.getWriter();
                URL gsURL = new URL(GeoSkillsAccess.geoSkillsDevUrl);
                if(subjectOntURL.getFile().startsWith("ABitOf"))
                    gsURL = new URL(subjectOntURL,"ABitOfGeoSkills.owl");
                SubjectsOntologyToGSNodesList stgnl = new SubjectsOntologyToGSNodesList(gsURL,
                        subjectOntURL);
                Set<URI> subjects = stgnl.listSubjects();
                System.out.println("Found " + subjects.size() + " subjects.");
                for(URI subj: subjects) {
                    String subjS = subj.toURL().toExternalForm();
                    GSILogger.log("subject: " + subjS);
                    for(URI node: stgnl.getSubjectIndividuals(subj)) {
                        String nodeS = node.toURL().toExternalForm();
                        if(nodeS.startsWith(GeoSkillsConstants.SUBJECTS_BASE_URI)) continue;
                        GSILogger.log("- " + nodeS);
                        Document doc = new Document();
                        doc.add(new Field(subjectURIFieldName,subjS, Field.Store.NO,  Field.Index.NOT_ANALYZED));
                        doc.add(new Field(nodeURIFieldName,nodeS,    Field.Store.YES, Field.Index.NO));
                        doc.add(typeField);
                        writer.addDocument(doc);
                    }
                }
                queryCache.clear();
            }
        } catch (Exception e) {
            e.printStackTrace();
            GSILogger.log("Error at Subjects Processing: " + e);
        }
        index.stopWriting();
        GSILogger.log("=================================");
        GSILogger.log("Finished subjects processing.");

    }

    private static Map<String,BooleanQuery> queryCache = new HashMap<String,BooleanQuery>();

    public static BooleanQuery getQuery(final IndexHome index, final String subjectID) throws IOException {
        BooleanQuery q = queryCache.get(subjectID);
        if(q==null) {
            q = makeQuery(index,subjectID);
            queryCache.put(subjectID,q);
            System.out.println("Caching query for subject \"" + subjectID + "\" yields: ");
            System.out.println(q);
            System.out.println("==== queryCache now has " + queryCache.size() + " entries.");
        }
        return q;
    }

    public static BooleanQuery makeQuery(final IndexHome index, final String subjectID) throws IOException {
        try {
            final BooleanQuery query = new BooleanQuery();
            synchronized (SubjectsCollector.class) {
                Query q = new TermQuery(new Term(subjectURIFieldName,subjectID));

                final List<Integer> l = new ArrayList<Integer>(32);
                index.getSearcher().search(q,new Collector() {
                    int n=0;

                    IndexReader reader;
                    Scorer scorer;

                    @Override
                    public void setScorer(Scorer scorer) throws IOException {
                       this.scorer = scorer;
                    }

                    @Override
                    public void setNextReader(IndexReader reader, int docBase) throws IOException {
                        this.reader = reader;
                    }

                    @Override
                    public boolean acceptsDocsOutOfOrder() {
                        return false;
                    }

                    @Override
                    public void collect(int i) {
                        l.add(i);
                        n++;
                        if(n>200) return;
                        if(l.size()>31) {
                            for(Integer in:l) {
                                try {
                                    addQuery(index,query, in);
                                } catch (IOException e) {
                                    throw new IllegalStateException("Can't expand query to subject \"" + subjectID + "\".");
                                }
                            }
                        }
                    }
                });
                if(!l.isEmpty()) {
                    for(Integer in:l) {
                        try {
                            addQuery(index,query, in);
                        } catch (IOException e) {
                            throw new IllegalStateException("Can't expand query to subject \"" + subjectID + "\".");
                        }
                    }
                }
            }

            QueryParser parser = new QueryParser(Version.LUCENE_35, "ft",new WhitespaceAnalyzer(Version.LUCENE_35));
            BooleanQuery bq = new SubjectBooleanQuery();
            // at least all the topics
            bq.add(query, BooleanClause.Occur.MUST);
            // only resources
            bq.add(parser.
                    parse("+object:CurrikiCode.AssetClass "),BooleanClause.Occur.MUST);
            // exclude the wrong ones
            bq.add(parser.parse("CurrikiCode.TextAssetClass.type:2 " +
            "name:Favorites web:AssetTemp web:\"Coll_Templates\" " +
            "name:WebHome name:WebPreferences name:MyCollections " +
            "name:SpaceIndex  CurrikiCode.AssetClass.hidden_from_search:1"),BooleanClause.Occur.MUST_NOT);

            return bq;
        } catch (Exception e) {
            e.printStackTrace();
            if(e instanceof IllegalStateException) throw (IllegalStateException) e;
            throw new IllegalStateException("Can't expand query to subject \"" + subjectID + "\".");
        }
    }

    private static void addQuery(IndexHome index, BooleanQuery query, int docId) throws IOException {
        Document doc = index.getReader().document(docId);
        String nodeURI = doc.get(nodeURIFieldName);
        if(nodeURI==null) return;

        if(nodeURI.startsWith(GeoSkillsConstants.GEOSKILLS_BASE_URI))
            nodeURI = nodeURI.substring(GeoSkillsConstants.GEOSKILLS_BASE_URI.length());
        if(nodeURI!=null && nodeURI.length()>0 && ! nodeURI.startsWith(GeoSkillsConstants.SUBJECTS_BASE_URI)) {
            query.add(new TermQuery(new Term("CurrikiCode.AssetClass."+RSConstants.FIELDNM_TOPICS_AND_COMPETENCIES,nodeURI)),
                    BooleanClause.Occur.SHOULD);
            //query.add(new TermQuery(new Term(RSConstants.EDULEVEL_FIELDNM,nodeURI)),
            //        BooleanClause.Occur.SHOULD);
            //TermQuery tq = new TermQuery(new Term(RSConstants.FIELDNM_ANCESTORS,nodeURI));
            //tq.setBoost(0.5f);
            //query.add(tq, BooleanClause.Occur.SHOULD);
        }
    }
}

class SubjectBooleanQuery extends BooleanQuery {
    public SubjectBooleanQuery() {
        super();
    }
}
