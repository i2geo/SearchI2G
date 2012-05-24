package net.i2geo.onto.tasks;

import net.i2geo.onto.GeoSkillsAccess;
import net.i2geo.onto.GeoSkillsUtil;

import java.net.URL;
import java.net.URI;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map;
import java.util.Iterator;

import org.semanticweb.owl.model.*;

/** Little class to process the Subjects ontology outputting a velocity file
 * which defines the topics depending on the traces' topic.
 */
public class TracesTopicsTranslatorProducer extends GeoSkillsAccess {

    private TracesTopicsTranslatorProducer(URL subjectsOntU, URL gsOntU) {
        super(gsOntU);
        this.subjectsOntU = subjectsOntU;
    }
    private URL subjectsOntU;
    private OWLOntology subjectsOnt;
    private final String subjectsBaseURI = "http://inter2geo.eu/2008/ontologies/Subjects#";

    public void open() throws Exception {
        super.open();
        subjectsOnt = manager.loadOntologyFromPhysicalURI
                (URI.create(subjectsOntU.toExternalForm()));
    }

    public void run() throws Exception {
        OWLClass tracesTopicClass =
         manager.getOWLDataFactory().getOWLClass(
                 URI.create(subjectsBaseURI + "TracesSubject"));
        Set<OWLIndividual> tracesTopics =
            new TreeSet<OWLIndividual>(tracesTopicClass.getIndividuals(subjectsOnt));

        System.out.println("// have " + tracesTopics.size() + " traces topics.");
        System.out.println("if(false){");
        for(OWLIndividual tracesTopic:tracesTopics) {
            // read name
            System.out.println("} else if(traceTopic.equals(\""+
                    ttShort(tracesTopic)+"\")) {");
            // read relationships
            Set<OWLIndividual> i2gTopics =
                    tracesTopic.getObjectPropertyValues(subjectsOnt).get(
                            manager.getOWLDataFactory().getOWLObjectProperty(
                                    URI.create(subjectsBaseURI + "subjectHasTopic"))
                            );
            System.out.print("  i2gTopic =\"");
            if(i2gTopics!=null) {
                for(Iterator<OWLIndividual> it = i2gTopics.iterator(); it.hasNext(); ) {
                    OWLIndividual i2gTopic = it.next();
                    System.out.print(GeoSkillsUtil.shortenName(i2gTopic.getURI().toString(),ontBaseU));
                    if(it.hasNext()) System.out.print(",");
                }
            }
            System.out.println("\";");
        }
        System.out.println("} ");
        // TODO: make sure URI fragment-id's prefix is same as topic-name-intraces
    }

    private String ttShort(OWLIndividual i) {
        OWLDataProperty subjectNameProp = manager.getOWLDataFactory().getOWLDataProperty(
            URI.create("http://inter2geo.eu/2008/ontologies/Subjects#subjectHasName"));
                    //subjectsBaseURI+ "subjectHasName"));
        Map<OWLDataPropertyExpression,Set<OWLConstant>> m = i.getDataPropertyValues(subjectsOnt);
        Set<OWLConstant> s= m.get(subjectNameProp);
        String r = s.iterator().next().getLiteral();

                GeoSkillsUtil.shortenName(i.getURI().toString(),
                subjectsBaseURI);
        if(r.endsWith("-topic")) r = r.substring(0, r.length()-"-topic".length());
        return r;
    }

    public static void main(String[] args) throws Exception {
        TracesTopicsTranslatorProducer tt = new TracesTopicsTranslatorProducer(new URL(args[0]),new URL(args[1]));
        tt.open();
        tt.run();
    }
}
