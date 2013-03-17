package net.i2geo.index.relatives;

import net.i2geo.index.GSIUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.DefaultHttpClient;
import org.semanticweb.owl.model.OWLIndividual;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class AttemptAutoDefinitionURL {

    private static final Log log = LogFactory.getLog(AttemptAutoDefinitionURL.class);

    public static void main(String[] args) throws Exception {
        File geoSkillsRelativesPath = new File(args[0]);
        GSRelativesAccess access = new GSRelativesAccess(geoSkillsRelativesPath.toURL());
        String language = args[1];
        HttpClient httpClient = new DefaultHttpClient();

        log.info("Attempt auto-definition URLs.");
        Set<OWLIndividual> nodes = access.getIndividualsOfClass(access.getTopicClass());
        int count = 0;
        for(OWLIndividual node : nodes ) {
            log.info("Checking "+ GSIUtil.uriToName(node.getURI())+"("+count+" of " + nodes.size() + ").");
            // check if definitionURL is already there, if yes don't process
            Map<String, Set<String>> definitionURLs = access.getNamesOfProp(node, access.definitionURLProperty);
            if(definitionURLs!=null && definitionURLs.containsKey(language)) continue;

            // read commonName
            String commonName = access.getCommonName(node, new String[]{language}, false);
            if(commonName==null) {
                log.info("No commonname in language " + language + ".");
                continue;
            }
            commonName = Character.toUpperCase(commonName.charAt(0)) + commonName.substring(1);
            commonName = commonName.replaceAll(" ", "_");

            // check with wikipedia
            String wkpUrl = "http://" + language + ".wikipedia.org/wiki/" + URLEncoder.encode(commonName, "utf-8");
            log.info("--- polling " + wkpUrl);
            try {
                HttpHead get = new HttpHead(wkpUrl) ;
                HttpResponse response = httpClient.execute(get);

                if(response.getStatusLine().getStatusCode()/100==3) {
                    log.info("--- redirected to " + response.getFirstHeader("Location") + ", ignoring...");

                    continue;
                }
                if(response.getStatusLine().getStatusCode()==404) {
                    log.info("--- not found.");
                    // TODO: could employ other languages' translations if existing
                    continue;
                }
                if(response.getStatusLine().getStatusCode()!=200) {
                    log.info("--- error at fetching: " + response.getStatusLine());
                    continue;
                }
            } catch (IOException e) {
                log.info("--- failing request: " + e);
                continue;
            }
            log.info("--- success.");
            access.assertStringProperty(node, access.definitionURLProperty, wkpUrl);
        }
        File file = new File(geoSkillsRelativesPath.getParentFile(),"GeoSkillsRelativesWithAutoDefs.owl");
        log.info("Saving ontology to " + file + ".");
        access.saveOntology(new OutputStreamWriter(new FileOutputStream(file), "utf-8"));
        log.info("Finished.");

    }
}
