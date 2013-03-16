package net.i2geo.index.relatives;

import net.i2geo.api.GeoSkillsConstants;
import net.i2geo.index.GSIUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.semanticweb.owl.model.OWLIndividual;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;
import java.io.*;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

/** Command line tool to download the content of definition texts from the web, at attached referenced URLs
 *  then recompute the distance between the close-by nodes along the LSA-distance and write them in the
 *  ontology.
 */
public class GSRelativesUpdater implements GeoSkillsConstants {



    public static void main(String[] args) throws Exception {
        new GSRelativesUpdater(new File(args[0]), args[1]).run();
    }


    public GSRelativesUpdater(File geoSkillsRelativesPath, String language) throws Exception {
        this.access = new GSRelativesAccess(geoSkillsRelativesPath.toURL());
        this.geoSkillsRelativesFile = geoSkillsRelativesPath;
        this.language = language;
        File baseDir = new File("/tmp/GSRelativesUpdater");
        FileUtils.deleteDirectory(baseDir);
        baseDir.mkdir();
        this.corpus = new DefinitionsCorpus(baseDir, Arrays.asList(language));;
    }

    private File geoSkillsRelativesFile;
    private String language;

    private final Log log = LogFactory.getLog(GSRelativesUpdater.class);
    private final GSRelativesAccess access;
    private DefinitionsCorpus corpus = null;
    private static long uuidCounter = System.nanoTime();

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    public void run() throws Exception {
        loadDefinitionsFromWeb();
    }

    private void loadDefinitionsFromWeb() throws Exception {
        log.info("Updating definitionTexts: Crawling through the individuals.");
        Set<OWLIndividual> nodes = access.getIndividualsOfClass(access.getNameableBitClass());
        Set<String> urisWithADef = new TreeSet<String>();
        corpus.startWriting();
        for(OWLIndividual node : nodes ) {
            // individual URI
            String uri = node.getURI().toString();
            String fragId = GSIUtil.uriToName(uri);
            log.info("Individual: " + fragId);

            Map<String, Set<String>> definitionURLs = access.getNamesOfProp(node, access.definitionURLProperty);
            for(String lang: definitionURLs.keySet()) {
                log.info("Found definition for " + fragId);
                if(!language.equals(lang)) continue;
                urisWithADef.add(fragId);
                Set<String> urls = definitionURLs.get(lang);
                if(urls.size()>1) {
                    log.error("Multiple definitionURLs found for " + fragId + ": " + urls + ". Ignoring all of them!");
                    continue;
                }

                String fetchedText = fetchTextFromWebPage(urls.iterator().next());
                // check if there's a manual definition, read it, and do not load then
                Set<OWLIndividual> definitionTexts = access.getObjectPropertyValue(node, access.definingProperty);
                OWLIndividual definitionText = null;
                if(definitionTexts!=null) for(OWLIndividual text: definitionTexts){
                    if(access.isOfType(text, access.manualDefinitionTestClass) ) {
                        definitionText = text; break;
                    }
                }

                if(definitionText==null) {
                    log.info("Adding definitionText.");
                    // remove possible earlier auto-fetched definition
                    access.removeObjectPropertiesAndObjects(node, access.definingProperty);

                    // fetch text from web-page
                    fetchedText = fetchTextFromWebPage(urls.iterator().next());
                    // missing: all three props

                    // set auto-fetched definition
                    definitionText = access.createIndividual(createURI(), access.fetchedDefinitionTextClass);
                    access.assertStringProperty(definitionText, access.languageProperty, lang);
                    access.assertStringProperty(definitionText, access.textProperty, fetchedText);
                    access.assertObjectProperty(definitionText, access.definingProperty, node);
                    //access.assertStringProperty(definitionText, access.modificationDateProperty, dateFormat.format(new Date()));

                }
                corpus.insertDefinition(fragId,language, fetchedText);
                //feedIntoCorpus(fragId, lang, fetchedText);//access.getTextProp(definitionText, access.definitionTextProperty));
            }
        }
        log.info("Finished inserting definitionTexts.");
        corpus.startReading();
        log.info("================ index dump ==============================");
        corpus.dumpLuceneIndex();
        log.info("================ rebuilding vectors ==============================");
        corpus.rebuildVectorsIndex(language);

        for(String fragId: urisWithADef) {
            log.info("================ Searching for "+fragId+" ==============================");
            List<DefinitionsCorpus.Neighbour> results = corpus.searchNeighbourDocs(fragId, language, 100);
            for(DefinitionsCorpus.Neighbour result: results) {
                String nodeUri = result.uri;
                if(nodeUri.startsWith("uri_")) nodeUri = nodeUri.substring("uri_".length());
                System.out.println(" ---- " + result.distance + ":" + nodeUri);
                String relationURI = createURI();
                OWLIndividual relation = access.createIndividual(relationURI, access.computedRelativeRelationClass);
                access.assertObjectProperty(relation, access.relFromProperty, access.getOntologyIndividualOfName(fragId));
                access.assertObjectProperty(relation, access.relToProperty, access.getOntologyIndividualOfName(nodeUri));
                access.assertFloatProperty(relation, access.relWeightProperty, result.distance);
            }
        }
        log.info("================ Saving GeoSkillsRelatives ==============================");
        File file = new File(geoSkillsRelativesFile.getParentFile(),"EnrichedGeoSkillsRelatives.owl");
        access.saveOntology(new OutputStreamWriter(new FileOutputStream(file), "utf-8"));
        log.info("================ "+ file + "properly saved ==============================");
    }

    private String createURI() {
        synchronized(this.getClass()) {
            uuidCounter++;
            return Long.toHexString(uuidCounter);
        }
    }

    private String fetchTextFromWebPage(String urlText) throws IOException {
        log.info("Fetching from " + urlText);
        final URL url = new URL(urlText);
        final boolean isWikipedia = url.getHost().endsWith(".wikipedia.org");
        String ref = url.getRef();
        if(ref!=null && ref.length()==0) ref = null;
        final String anchor = ref;
        final StringBuilder buff = new StringBuilder(1024);


        HTMLEditorKit.ParserCallback htmlListener = new HTMLEditorKit.ParserCallback() {

            boolean isRecording = false, finished = false; // we're inside if there's no ID to track
            Stack<HTML.Tag> elementsStack = new Stack<HTML.Tag>();

            public void handleStartTag(HTML.Tag t, MutableAttributeSet a, int pos) {
                String id = (String) a.getAttribute(HTML.Attribute.ID);
                if(anchor!=null && !isRecording) {
                    // evaluate if we start to go inside
                    String name = (String) a.getAttribute(HTML.Attribute.NAME);
                    if(anchor.equals(id) || anchor.equals(name)) isRecording = true;
                } else if(anchor==null && !isRecording && !isWikipedia) {
                    isRecording = t==HTML.Tag.BODY;
                } else if(!isRecording && isWikipedia && !finished) {
                    isRecording = t==HTML.Tag.P;
                } else if(isRecording && isWikipedia) {
                    if("toc".equals(id)) {
                        isRecording = false;
                        finished = true;
                    }
                }
                if(isRecording) elementsStack.push(t);
            }

            public void handleText(char[] data, int pos)
            {
                if(isRecording) {
                    buff.append(data, 0, data.length);
                }

            }

            public void handleSimpleTag(HTML.Tag t, MutableAttributeSet a, int pos)
            {
                handleStartTag(t, a, pos);
                handleEndTag(t, pos);
            }

            public void handleEndTag(HTML.Tag t, int pos) {
                if(isRecording) elementsStack.pop();
                if(elementsStack.isEmpty()) {
                    isRecording = false;
                }
            }
        };
        new ParserDelegator().parse(new InputStreamReader(url.openStream(), "utf-8"),
                htmlListener, true);



        log.info("Finished fetching from " + urlText + " yielding a text of " + buff.length());
        return buff.toString();
    }

    private void updateGSRelativesBasedOnCorpus() {
        // TODO: accumulate weights so that lsa-similarity and manual similarity grow score
    }



}
