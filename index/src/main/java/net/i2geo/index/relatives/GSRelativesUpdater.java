package net.i2geo.index.relatives;

import net.i2geo.api.GeoSkillsConstants;
import net.i2geo.index.GSIUtil;
import net.i2geo.index.IndexHome;
import net.i2geo.onto.GeoSkillsAccess;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.semanticweb.owl.model.OWLIndividual;

import java.net.URLClassLoader;
import java.text.SimpleDateFormat;
import java.util.*;

/** Command line tool to download the content of definition texts from the web, at attached referenced URLs
 *  then recompute the distance between the close-by nodes along the LSA-distance and write them in the
 *  ontology.
 */
public class GSRelativesUpdater implements GeoSkillsConstants {



    public static void main(String[] args) {

    }


    private List<String> languages = IndexHome.supportedLanguages;
    private Log log = LogFactory.getLog(GSRelativesUpdater.class);
    private GSRelativesAccess access;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    private GeoSkillsAccess loadOntology() throws Exception {
        GeoSkillsAccess access = GeoSkillsAccess.getInstance();
        access.open();
        ClassLoader loader = access.getClass().getClassLoader();
        log.info("GeoSkillsAccess is " + access + " loaded with " + loader);
        // TODO: need GSRelatives in here!
        if(loader instanceof URLClassLoader)
            log.info(".... with URL " + Arrays.asList(((URLClassLoader) loader).getURLs()));
        log.info("Ontology opened from  " + access.getOntologyURL() + ".");


        return access;
    }

    private void loadDefinitionsFromWeb() {
        log.info("Updating definitionTexts: Crawling through the individuals.");
        Set<OWLIndividual> nodes = access.getIndividualsOfClass(access.getNameableBitClass());
        for(OWLIndividual node : nodes ) {
            Document doc = new Document();

            // individual URI
            String uri = node.getURI().toString();
            String fragmentId = GSIUtil.uriToName(uri, false);

            Map<String, Set<String>> definitionURLs =
                    access.getNamesOfProp(node, access.definitionURLProperty);
            for(String lang: definitionURLs.keySet()) {
                if(!languages.contains(lang)) continue;
                Set<String> urls = definitionURLs.get(lang);
                if(urls.size()>1) {
                    log.error("Multiple definitionURLs... ignoring all of them!");
                    continue;
                }
                // check if there's a manual definition, read it, and do not load then
                Set<OWLIndividual> definitionTexts =
                        access.getObjectPropertyValue(node, access.definitionTextProperty);
                OWLIndividual definitionText = null;
                for(OWLIndividual text: definitionTexts){
                    if(access.isOfType(text, access.manualDefinitionClass) ) { // TODO: && xx
                        definitionText = text; break;
                    }
                }

                if(definitionText==null) {
                    // remove possible earlier auto-fetched definition
                    access.removeObjectPropertiesAndObjects(node, access.definitionTextProperty);

                    // fetch text from web-page
                    String fetchedText = fetchTextFromWebPage(urls.iterator().next());

                    // set auto-fetched definition
                    definitionText = access.createIndividual(createURI(), access.automaticDefinitionClass);
                    access.assertStringProperty(definitionText, access.languageProperty, lang);
                    access.assertI18nStringProperty(definitionText, access.definitionTextContentProperty, lang, fetchedText);
                    access.assertStringProperty(definitionText, access.modificationDateProperty, dateFormat.format(new Date()));

                }
                feedIntoCorpus(uri, lang, access.getTextProp(definitionText, access.definitionTextContentProperty));
            }
        }
        log.info("Finished updating definitionTexts.");

    }

    private String createURI() {
        return null;  // TODO: implement
    }

    private String fetchTextFromWebPage(String url) {
        return null;  //TODO: implement
    }

    private void feedIntoCorpus(String uri, String lang, String definitionText) {

    }

    private void updateGSRelativesBasedOnCorpus() {
        // TODO: accumulate weights so that lsa-similarity and manual similarity grow score
    }



}
