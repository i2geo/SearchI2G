package net.i2geo.index.relatives;

import net.i2geo.index.analysis.SKBAnalyzer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.index.*;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import pitt.search.semanticvectors.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/** Simple class to access the corpus of definitions and its LSA measures.
 * Most parts are adapted from SemanticVectors' BuildIndex, TermVectorsFromLucene, and Search
 */
public class DefinitionsCorpus {

    public DefinitionsCorpus(File indexPath, List<String> supportedLanguages) throws IOException {
        this.indexPath = indexPath.getPath();
        analyzer = new SKBAnalyzer(false, supportedLanguages);
        dir = FSDirectory.open(indexPath);
        iwc = new IndexWriterConfig(Version.LUCENE_35, analyzer);
        iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);

        this.supportedLanguages = supportedLanguages;
        configs = new ArrayList<FlagConfig>(supportedLanguages.size());
        for(String lang: supportedLanguages)
            configs.add(FlagConfig.getFlagConfig(new String[]{
                    "-contentsfields", "contents_" + lang + ",uri",
                    "-luceneindexpath", this.indexPath,
                    "-termvectorsfile", "termVectors-" + lang,
                    "-queryvectorfile", "termVectors-" + lang,
                    "-docvectorsfile", "docVectors-" + lang,
                    "-trainingcycles", "5"}));
    }

    private static Log LOG = LogFactory.getLog(DefinitionsCorpus.class);

    private List<String> supportedLanguages;

    // --- lucene
    private Directory dir;
    private final String indexPath;
    private final Analyzer analyzer;

    private IndexWriterConfig iwc;
    private IndexWriter writer = null;
    private IndexReader reader = null;
    private IndexSearcher searcher = null;

    // --- semantic vectors
    List<FlagConfig> configs;


    public void clearAll() throws Exception {
        if(writer==null) startWriting();
        writer.deleteAll();
        writer.commit();
    }

    public void insertDefinition(String uri, String language, String definitionText) throws Exception {
        // make a new, empty document
        Document doc = new Document();

        Field uriField = new Field("uri", "uri_" + uri, Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS);
        uriField.setIndexOptions(FieldInfo.IndexOptions.DOCS_ONLY);
        doc.add( uriField);

        doc.add(new Field("contents_" + language, definitionText, Field.Store.YES, Field.Index.ANALYZED));
        writer.updateDocument(new Term("uri", uri), doc);
    }

    public void startWriting() throws IOException {
        if(reader!=null) reader.close();
        reader = null;
        if(writer==null) {
            dir = FSDirectory.open(new File(indexPath));
            writer = new IndexWriter(dir, iwc);
        }
    }

    public void startReading() throws IOException {
        if(writer!=null) writer.close(true);
        writer = null;
        if(reader==null) reader = IndexReader.open(dir);
    }


    public List<SearchResult> searchNeighbourTerms(String lang, int maxResults, String query) throws IOException, ZeroVectorException {
        FlagConfig config = configs.get(supportedLanguages.indexOf(lang));
        CloseableVectorStore queryVecReader = VectorStoreReader.openVectorStore(config.queryvectorfile(), config);
        LuceneUtils luceneUtils = new LuceneUtils(config);
        VectorSearcher vecSearcher = null;
        LOG.info("Searching term vectors, searchtype " + config.searchtype() + "\n");
        vecSearcher = new VectorSearcher.VectorSearcherCosine(
                queryVecReader, queryVecReader, luceneUtils, config, query.split(" |\t|\n|\r"));
        LinkedList<SearchResult> results = vecSearcher.getNearestNeighbors(maxResults);
        return results;
    }

    public List<Neighbour> searchNeighbourDocs(String uri, String language, int maxResults) throws IOException, ZeroVectorException{
        FlagConfig config = configs.get(supportedLanguages.indexOf(language));
        CloseableVectorStore queryVecReader = VectorStoreReader.openVectorStore(config.termvectorsfile(), config),
            resultsVecReader = VectorStoreReader.openVectorStore(config.docvectorsfile(), config);
        LuceneUtils luceneUtils = new LuceneUtils(config);
        LOG.info("Searching term vectors, searchtype " + config.searchtype() + "\n");
        VectorSearcher  vecSearcher = new VectorSearcher.VectorSearcherCosine(
                queryVecReader, resultsVecReader, luceneUtils, config, new String[] {uri}); // "uri:" +
        LinkedList<SearchResult> results = vecSearcher.getNearestNeighbors(maxResults);

        return searchResultsToNeighbours(results);
    }


    private List<Neighbour> searchResultsToNeighbours(List<SearchResult> internal) {
        ArrayList<Neighbour> l = new ArrayList<Neighbour>(internal.size());
        for(SearchResult r: internal) {
            Neighbour nb = new Neighbour();
            nb.distance = (float) r.getScore();
            nb.uri = r.getObjectVector().getObject().toString();
            l.add(nb);
        }
        return l;
    }

    public void rebuildVectorsIndex(String language) throws IOException {
        int i=supportedLanguages.indexOf(language);
        if(i==-1) throw new IllegalArgumentException("Not a supported language \"" + language + "\".");
        FlagConfig config = configs.get(i);

        System.out.println("======================================================================================================");
        LOG.info("\nCreating term vectors ("+language+")...");
        LOG.info("Seedlength: " + config.seedlength()
                + ", Dimension: " + config.dimension()
                + ", Vector type: " + config.vectortype()
                + ", Minimum frequency: " + config.minfrequency()
                + ", Maximum frequency: " + config.maxfrequency()
                + ", Number non-alphabet characters: " + config.maxnonalphabetchars()
                + ", Contents fields are: " + Arrays.toString(config.contentsfields()) + "\n");
        String termFile = config.termvectorsfile();
        LOG.info("Termfile ("+language+"): " + termFile);
        String docFile = config.docvectorsfile();
        LOG.info("Docfile ("+language+"): " + docFile);
        TermVectorsFromLucene vecStore;
        if (!config.initialtermvectors().isEmpty()) {
            LOG.info("Creating term vectors ... \n");
            vecStore = TermVectorsFromLucene.createTermBasedRRIVectors(config);
        } else {
            LOG.info("Creating elemental document vectors ... \n");
            vecStore = TermVectorsFromLucene.createTermVectorsFromLucene(config, null);
        }

        // Create doc vectors and write vectors to disk.
        switch (config.docindexing()) {
            case INCREMENTAL:
                VectorStoreWriter.writeVectors(termFile, config, vecStore);
                IncrementalDocVectors.createIncrementalDocVectors(
                        vecStore, config, indexPath, "incremental_"+docFile);
                IncrementalTermVectors itermVectors = null;

                for (int cycle = 1; cycle < config.trainingcycles(); ++cycle) {
                    itermVectors = new IncrementalTermVectors(config,
                            indexPath, docFile);

                    VectorStoreWriter.writeVectors(
                            "incremental_termvectors"+ config.trainingcycles()+".bin", config, itermVectors);

                    if (cycle == config.trainingcycles() - 1)
                        docFile = "docvectors" + config.trainingcycles() + ".bin";

                    IncrementalDocVectors.createIncrementalDocVectors(
                            itermVectors, config, indexPath, "incremental_"+docFile);
                }
            case INMEMORY:
                DocVectors docVectors = new DocVectors(vecStore, config);
                for (int cycle = 1; cycle < config.trainingcycles(); ++cycle) {
                    LOG.info("\nRetraining with learned document vectors (cycle "+cycle+")...");
                    vecStore = TermVectorsFromLucene.createTermVectorsFromLucene(config, docVectors);
                    docVectors = new DocVectors(vecStore, config);
                }
                VectorStore writeableDocVectors = docVectors.makeWriteableVectorStore();

                if (config.trainingcycles() > 1) {
                    termFile = "termvectors" + config.trainingcycles() + ".bin";
                    docFile = "docvectors" + config.trainingcycles() + ".bin";
                }
                LOG.info("Writing term vectors to " + termFile + "\n");
                VectorStoreWriter.writeVectors(termFile, config, vecStore);
                LOG.info("Writing doc vectors to " + docFile + "\n");
                VectorStoreWriter.writeVectors(docFile, config, writeableDocVectors);
                break;
            case NONE:
                // Write term vectors to disk even if there are no docvectors to output.
                LOG.info("Writing term vectors to " + termFile + "\n");
                VectorStoreWriter.writeVectors(termFile, config, vecStore);
                break;
            default:
                throw new IllegalStateException(
                        "No procedure defined for -docindexing " + config.docindexing());
        }

        if(!(new File(termFile).equals(new File(config.termvectorsfile() + ".bin")))) {
            LOG.info("Moving "+termFile+" to " + config.termvectorsfile() + ".bin");
            new File(config.termvectorsfile() + ".bin").delete();
            new File(termFile).renameTo(new File(config.termvectorsfile() + ".bin"));
        }
        if(!(new File(termFile).equals(new File(config.docvectorsfile() + ".bin")))) {
            LOG.info("Moving "+docFile+" to " + config.docvectorsfile() + ".bin");
            new File(config.docvectorsfile() + ".bin").delete();
            new File(docFile).renameTo(new File(config.docvectorsfile() + ".bin"));
        }

        LOG.info("Finished rebuilding vectors (" + language + ").");
    }

    public void dumpLuceneIndex() throws Exception {
        startReading();
        for(int i=0; i<reader.maxDoc(); i++) {
            System.out.println(" ---------------- document " + i + " ----------------------------");
            Document doc = reader.document(i);
            if(doc==null) continue;
            for(Fieldable field: doc.getFields()) {
                if(field==null) continue;
                System.out.println(field.name() + ": " + field.stringValue());
            }
        }
    }

    public class Neighbour {
        String uri;
        float distance;

        public String toString() {
            return distance + ":" + uri;
        }
    }

}
