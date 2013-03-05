package net.i2geo.onto;

import net.i2geo.api.GeoSkillsConstants;
import org.semanticweb.owl.apibinding.OWLManager;
import org.semanticweb.owl.io.OWLOntologyInputSource;
import org.semanticweb.owl.model.*;
import org.semanticweb.owl.inference.OWLReasoner;
import org.semanticweb.owl.inference.OWLReasonerException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.URL;
import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.lang.reflect.Constructor;

/** Utility class to convert the Subjects' ontology to a list of queries that can be
 * run over the index providing "topic browsing".
 */
public class SubjectsOntologyToGSNodesList implements GeoSkillsConstants {

    private static Log LOG = LogFactory.getLog(SubjectsOntologyToGSNodesList.class);
    private static boolean DEBUG = LOG.isDebugEnabled();

    public SubjectsOntologyToGSNodesList(URL geoskillsOntologyURL, URL subjectsOntologyURL) throws Exception {
        System.out.println("Loading GeoSkills from " + geoskillsOntologyURL);
        manager = OWLManager.createOWLOntologyManager();
        manager.loadOntologyFromPhysicalURI(geoskillsOntologyURL.toURI());
        System.out.println("Loading Subjects from " + subjectsOntologyURL);
        manager.loadOntologyFromPhysicalURI(subjectsOntologyURL.toURI());
        subjectsOntology = manager.getOntology(SubjectsOntoURI);
        String reasonerClassName = "org.mindswap.pellet.owlapi.Reasoner";
        Class reasonerClass = Class.forName(reasonerClassName);
        Constructor<OWLReasoner> con = reasonerClass.getConstructor(OWLOntologyManager.class);
        reasoner = (OWLReasoner) con.newInstance(manager);

        Set<OWLOntology> importsClosure = manager.getImportsClosure(subjectsOntology);
        reasoner.loadOntologies(importsClosure);
        reasoner.classify();
    }

    public static final URI SubjectsOntoURI = URI.create(SUBJECTS_BASE_URI),
        subjectsRootClassURI = URI.create("http://inter2geo.eu/2008/ontologies/Subjects#CurrikiSubjects");

    final OWLOntologyManager manager;
    final OWLOntology subjectsOntology;
    final OWLReasoner reasoner;

    public Set<URI> listSubjects() throws OWLReasonerException {
        OWLClass clz = manager.getOWLDataFactory().getOWLClass(subjectsRootClassURI);
        Set owlClasses = reasoner.getSubClasses(clz);
        return nodesToURIs(owlClasses);
    }

    public Set<URI> getSubjectIndividuals(URI subjectURI) throws OWLReasonerException {
        Set individuals =
                reasoner.getIndividuals(manager.getOWLDataFactory().getOWLClass(subjectURI),false);
        return nodesToURIs(individuals);
    }

    private Set<URI> nodesToURIs(Set nodes) {
        Set<URI> s = new HashSet<URI> (nodes.size());
        for(Object obj: nodes) {
            if(obj instanceof OWLNamedObject) {
                s.add(((OWLNamedObject)obj).getURI());
            } else if (obj instanceof Set) {
                for(Object o: ((Set)obj))
                    if(o instanceof OWLNamedObject)
                        s.add(((OWLNamedObject)o).getURI());
                    else
                        LOG.info("Warning dropping node " + o);
            } else
                LOG.info("Warning, dropping node " + obj);
        }
        return s;
    }

    public static void main(String[] args) throws Throwable {
        LOG.info("Loading ontology: " + args[0]);

        SubjectsOntologyToGSNodesList conv = new SubjectsOntologyToGSNodesList(new URL(args[1]),new URL(args[0]));
        System.out.println("Ontology loaded.");
        for(URI cls: conv.listSubjects()) {
            System.out.println("==========================");
            System.out.println("=== " + cls);
            for(URI node: conv.getSubjectIndividuals(cls))
                System.out.println(" - " + node);
        }
    }

}
