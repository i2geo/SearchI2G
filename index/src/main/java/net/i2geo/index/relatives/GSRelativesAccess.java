package net.i2geo.index.relatives;

import net.i2geo.onto.GeoSkillsAccess;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.semanticweb.owl.inference.OWLReasonerException;
import org.semanticweb.owl.io.WriterOutputTarget;
import org.semanticweb.owl.model.*;

import java.io.Writer;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

/** Utility class to access the members of the GeoSkillsRelatives.owl ontology, extending that of GeoSkills.
 */
class GSRelativesAccess extends GeoSkillsAccess {

    OWLObjectProperty definingProperty;
    OWLDataProperty languageProperty;
    OWLDataProperty modificationDateProperty;
    OWLDataProperty definitionURLProperty;
    OWLDataProperty relWeightProperty;
    OWLClass manualDefinitionTestClass;
    OWLClass fetchedDefinitionTextClass;
    OWLObjectProperty relFromProperty, relToProperty;

    private final Log log = LogFactory.getLog(GSRelativesUpdater.class);
    public static final String gsRelativesBaseU = "http://www.inter2geo.eu/2008/ontology/GeoSkillsRelatives";
    OWLClass computedRelativeRelationClass;
    OWLDataProperty textProperty;


    GSRelativesAccess(URL relativesURL) throws Exception {
        super(new URL(relativesURL, "GeoSkills.owl"), relativesURL);
        super.open();
        ClassLoader loader = this.getClass().getClassLoader();
        log.info("GSRelativesAccess class is " + getClass() + " loaded with " + loader);
        if(loader instanceof URLClassLoader)
            log.info(".... with URL " + Arrays.asList(((URLClassLoader) loader).getURLs()));
        log.info("GeoSkills Ontology opened from  " + super.getOntologyURL() + ".");

        definingProperty = manager.getOWLDataFactory().getOWLObjectProperty(
                URI.create(gsRelativesBaseU + "#defining"));

        computedRelativeRelationClass = manager.getOWLDataFactory()
                .getOWLClass(URI.create(gsRelativesBaseU + "#computedRelativeRelations"));

        // THINKME: consider a model with ReferenceTexts class so that weaker updates can happen
        //OWLProperty modificationDateProperty;public
        relFromProperty= manager.getOWLDataFactory().getOWLObjectProperty(
                URI.create(gsRelativesBaseU + "#rel_from"));
        relToProperty= manager.getOWLDataFactory().getOWLObjectProperty(
                URI.create(gsRelativesBaseU + "#rel_to"));
        definitionURLProperty = manager.getOWLDataFactory().getOWLDataProperty(
                URI.create(gsRelativesBaseU+ "#definitionURL"));
        relWeightProperty = manager.getOWLDataFactory().getOWLDataProperty(
                URI.create(gsRelativesBaseU+ "#rel_weight"));
        textProperty = manager.getOWLDataFactory().getOWLDataProperty(
                URI.create(gsRelativesBaseU+"#text"));


        definingProperty = manager.getOWLDataFactory().getOWLObjectProperty(
                URI.create(gsRelativesBaseU+ "#defining"));
        languageProperty= manager.getOWLDataFactory().getOWLDataProperty(
                URI.create(gsRelativesBaseU+ "#language"));
        modificationDateProperty= manager.getOWLDataFactory().getOWLDataProperty(
                URI.create(ontBaseU+ "#modificationDate"));
        manualDefinitionTestClass = manager.getOWLDataFactory().getOWLClass(
                URI.create(gsRelativesBaseU+ "#ManualDefinitionText"));
        fetchedDefinitionTextClass = manager.getOWLDataFactory().getOWLClass(
                        URI.create(gsRelativesBaseU+ "#FetchedDefinitionText"));
    }



    public Set<OWLIndividual> getIndividualsOfClass(OWLClass cls) {
        try {
            Set<OWLIndividual> s = new HashSet<OWLIndividual>(reasoner.getIndividuals(cls,false));
            s.addAll(cls.getIndividuals(anotherOnt));
            return s;
        } catch (OWLReasonerException e) {
            e.printStackTrace();
            throw new IllegalStateException(e);
        }
    }

    public Map<String,Set<String>> getNamesOfProp(OWLIndividual i, OWLProperty nameProp) {
        if(i==null) {
            return Collections.emptyMap();
        } else if(i.getDataPropertyValues(ont) == null) {
            return Collections.emptyMap();
        } else {
            Map<String,Set<String>> r = new HashMap<String,Set<String>>();
            Set<OWLConstant> set = null;
            try {
                set = reasoner.getDataPropertyRelationships(i).get(nameProp);
            } catch (OWLReasonerException e) {
                e.printStackTrace();
                throw new IllegalStateException("",e);
            }
            if(set==null) return Collections.emptyMap();
            for(OWLConstant x : set) {
                String lang = null;
                //System.out.println("Lang = " + lang);
                if(x.isTyped())
                    lang=x.asOWLTypedConstant().getLiteral();
                else
                    lang= x.asOWLUntypedConstant().getLang();
                String value = x.getLiteral();
                if(nameProp.getURI().toString().endsWith("definitionURL") && value!=null && value.startsWith("http://") && value.contains("wikipedia.org")) {
                    String langFromURL = value.substring("http://".length(), "http://".length()+2);
                    if(!langFromURL.equals(lang)) {
                        log.warn("Wrong language for definitionURL at URI: " + i.getURI() + ": value " + value + " should have language " + langFromURL + ". Now correcting.");
                        lang = langFromURL;
                    }
                }
                Set<String> forLang = r.get(lang);
                if(forLang==null) {
                    forLang = new HashSet<String>();
                    r.put(lang,forLang);
                }
                forLang.add(x.getLiteral());
            }
            return r;
        }
    }


    public void saveOntology(Writer out) throws OWLOntologyStorageException {
        manager.saveOntology(anotherOnt,new WriterOutputTarget(out));
    }



    public void removeObjectPropertiesAndObjects(OWLIndividual node, OWLProperty definitionTextProperty) {
        //TODO: implement
    }

    public OWLIndividual createIndividual(String uri, OWLClass clz) throws Exception {
        OWLIndividual individual = manager.getOWLDataFactory().getOWLIndividual(new URI(uri));
        OWLClassAssertionAxiom classAssert = manager.getOWLDataFactory().getOWLClassAssertionAxiom(individual, clz);
        manager.addAxiom(anotherOnt, classAssert);
        return individual;
    }

    public void assertStringProperty(OWLIndividual node, OWLDataProperty property, String value) throws Exception {
        OWLDataPropertyAssertionAxiom assertion = manager.getOWLDataFactory().getOWLDataPropertyAssertionAxiom(node, property, value);
        manager.addAxiom(anotherOnt, assertion);
    }
    public void assertObjectProperty(OWLIndividual node, OWLObjectProperty property, OWLIndividual value) throws Exception {
        OWLObjectPropertyAssertionAxiom assertion = manager.getOWLDataFactory()
                .getOWLObjectPropertyAssertionAxiom(node, property, value);
        manager.addAxiom(anotherOnt, assertion);
    }

    public void assertFloatProperty(OWLIndividual individual, OWLDataProperty property, float value) throws Exception {
        OWLDataPropertyAssertionAxiom assertion = manager.getOWLDataFactory()
                .getOWLDataPropertyAssertionAxiom(individual, property, value);
        manager.addAxiom(anotherOnt, assertion);
    }
}
