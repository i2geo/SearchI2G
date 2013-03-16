package net.i2geo.index.relatives;


import junit.framework.TestCase;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import pitt.search.semanticvectors.SearchResult;

import java.io.File;
import java.text.DecimalFormat;
import java.util.*;

public class TestDefinitionCorpus extends TestCase {

    public TestDefinitionCorpus(String name) {
        super(name);
    }

    public void setUp() throws Exception {

        if(corpus==null) {
            List<String> languages = Arrays.asList("en", "de", "fr");
            decimalFormat = new DecimalFormat("########0.0000000000");
            corpus = new DefinitionsCorpus(new File(new File("target"), "corpusIndex"), languages);

            // populate it with the definitions in the resources
            Element definitions = new SAXBuilder().build(this.getClass().getResource("a-few-math-definitions.xml")).getRootElement();
            for(String lang: new String[]{language}) { // "fr", "de"
                corpus.startWriting();
                for(Element term: definitions.getChildren("term")) {
                    for(Element def: term.getChildren("definition")) {
                        if(lang.equals(def.getAttributeValue("lang", Namespace.XML_NAMESPACE)))
                            corpus.insertDefinition(term.getAttributeValue("id"),
                                    lang,
                                    def.getTextNormalize());
                    }
                }
                corpus.startReading();
                corpus.dumpLuceneIndex();
                corpus.rebuildVectorsIndex(lang);
            }
        }
    }

    static DefinitionsCorpus corpus = null;
    static DecimalFormat decimalFormat;
    static String language = "de";


    public void testTermsSimilarity() throws Exception {
        System.out.println("Test " + getName() + " starting.");

        Set<String> relatedWords = new HashSet<String>();
        List<SearchResult> results = corpus.searchNeighbourTerms(language, 1000, "giraff");
        assertTrue("There should be something related to girafe.", ! results.isEmpty());
        for(Object o: results) {
            SearchResult result = (SearchResult) o;
            String word = (String) result.getObjectVector().getObject();
            relatedWords.add(word);
            System.out.println("-- " + decimalFormat.format(result.getScore()) + " " + word);
        }
        assertTrue("Neck is related to girafe", relatedWords.contains("neck"));
        assertTrue("Okapi is related to girafe", relatedWords.contains("okapi"));
        System.out.println("Test " + getName() + " finished.");
    }

    public void testDocumentsSimilarity() throws Exception {
        System.out.flush(); System.err.flush();
        System.out.println("Test " + getName() + " starting.");
        System.out.println("================ finding documents similar to girafe ===========================");
        List<DefinitionsCorpus.Neighbour> results = corpus.searchNeighbourDocs("Girafe",language, 5);
        for(DefinitionsCorpus.Neighbour result: results) {
            System.out.println(" ---- " + result);
        }

        System.out.println("================ finding documents similar to elephant ===========================");
        results = corpus.searchNeighbourDocs("Disc_r",language, 10);
        assertTrue("There should be documented related to elephant.", !results.isEmpty());
        List<String> uris = new LinkedList<String>();
        for(DefinitionsCorpus.Neighbour result: results) {
            System.out.println(" ---- " + result);
            String uri = result.uri;
            if(uri.startsWith("uri_")) uri = uri.substring("uri_".length());
            uris.add(uri);
        }
        assertTrue("Lion is more similar to elephant than falcon.", uris.indexOf("Lion")<uris.indexOf("Peregrine_falcon"));

        System.out.println("Test " + getName() + " finished.");
    }

}
