package net.i2geo.index.relatives;


import junit.framework.TestCase;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import pitt.search.semanticvectors.SearchResult;

import java.io.File;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
            Element definitions = new SAXBuilder().build(this.getClass().getResource("a-few-definitions.xml")).getRootElement();
            for(String lang: new String[]{"en"}) { // "fr", "de"
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


    public void testTermsSimilarity() throws Exception {
        System.out.println("Test " + getName() + " starting.");

        Set<String> relatedWords = new HashSet<String>();
        for(Object o: corpus.searchNeighbourTerms("en", 1000, "giraff")) {
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
        System.out.println("Test " + getName() + " starting.");
        List<DefinitionsCorpus.Neighbour> results = corpus.searchNeighbourDocs("uri_Girafe","en", 5);
        for(DefinitionsCorpus.Neighbour result: results) {
            System.out.println(" ---- " + result);
        }
        System.out.println("Test " + getName() + " finished.");
    }


}
