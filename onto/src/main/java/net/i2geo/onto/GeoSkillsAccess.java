package net.i2geo.onto;

import net.i2geo.api.GeoSkillsConstants;
import org.semanticweb.owl.model.*;
import org.semanticweb.owl.apibinding.OWLManager;
import org.semanticweb.owl.inference.OWLReasoner;
import org.semanticweb.owl.inference.OWLReasonerException;
import org.semanticweb.owl.io.WriterOutputTarget;
import org.semanticweb.owl.io.OWLOntologyInputSource;
import org.semanticweb.owl.vocab.OWLRDFVocabulary;

import java.net.URI;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.*;
import java.lang.reflect.Constructor;
import java.io.*;

import com.sun.msv.datatype.xsd.datetime.ISO8601Parser;

/** Commidity class to access the GeoSkills ontology
 */
public class GeoSkillsAccess implements GeoSkillsConstants {

    public static final String ontBaseU = GEOSKILLS_BASE_URI;
    public static final String geoSkillsDevUrl= "http://i2geo.net/ontologies/dev/GeoSkills.owl";
    //"file:///Users/paul/projects/intergeo/ontologies/GeoSkills.owl";
            //"http://i2geo.net/ontologies/dev/GeoSkills.owl";

    private static GeoSkillsAccess theInstance = null;
    public static GeoSkillsAccess getInstance() {
        String url = geoSkillsDevUrl;
        if(theInstance == null) {
            if(System.getProperty("net.i2geo.onto.geoSkillsDevUrl")!=null)
                url = System.getProperty("net.i2geo.onto.geoSkillsDevUrl");
            theInstance = new GeoSkillsAccess(url);
        }
        return theInstance;
    }

    public static URL getTestOntologyUrl() {
        return GeoSkillsAccess.class.getResource("ABitOfGeoSkills.owl");
    }

    public static GeoSkillsAccess getTestInstance() {
        if(theInstance == null)
            theInstance = new GeoSkillsAccess(getTestOntologyUrl());
        return theInstance;
    }

    private boolean opened = false;

    protected OWLOntologyManager manager;
    protected OWLOntology ont;
    private OWLReasoner reasoner;

    protected URL ontologyURL;
    private OWLDataProperty commonNameProp;
    private OWLDataProperty defaultCommonNameProp;
    private OWLDataProperty unCommonNameProp;
    private OWLDataProperty rareNameProp;
    private OWLDataProperty falseFriendNameProp;
    private OWLObjectProperty hasTopicProp;
    private OWLObjectProperty belongsToEducationalPathway;
    private OWLObjectProperty inEducationalRegion;
    private OWLDataProperty age;

    private OWLDataProperty creationDateProp, modificationDateProp;
    private OWLDataProperty creationUserProp, modificationUserProp;

    private OWLClass competencyClass;
    private OWLClass nameableBitClass;
    private OWLClass topicClass;
    private OWLClass levelClass;


    public OWLClass getTopicClass() {
        return topicClass;
    }
    public OWLClass getNameableBitClass() {
        return nameableBitClass;
    }
    public OWLClass getLevelClass() {
        return levelClass;
    }


    public GeoSkillsAccess(String ontologyURL) {
        try {
            this.ontologyURL = new URL(ontologyURL);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            throw new IllegalStateException(e);
        }
    }

    public GeoSkillsAccess(URL ontologyURL) {
        this.ontologyURL = ontologyURL;
    }

    public URL getOntologyURL() {
        return ontologyURL;
    }

    public void forgetLoadedOntology() {
        if(manager!=null) {
            manager.removeOntology(URI.create(ontBaseU));
            System.out.println("Dropped ontology.");
        }
    }

    public boolean isOpened() {
        return opened;
    }

    public void open() throws Exception {
        opened = true;
        manager = OWLManager.createOWLOntologyManager();
        OWLOntologyInputSource source = new OWLOntologyInputSource() {
            public boolean isReaderAvailable() {
                return true;
            }

            public Reader getReader() {
                try {
                    return new InputStreamReader(ontologyURL.openStream(),"utf-8");
                } catch(IOException ex) {
                    throw new IllegalStateException("Can't open GeoSkills URL.",ex);
                }
            }

            public boolean isInputStreamAvailable() {
                return false;
            }

            public InputStream getInputStream() {
                return null;
            }

            public URI getPhysicalURI() {
                return URI.create(ontologyURL.toExternalForm());
            }
        };
        
        ont = manager.loadOntology(source);
        System.out.println("Ontology loaded from " + ontologyURL.toExternalForm());
        reasoner = createReasoner(manager);
        Set<OWLOntology> importsClosure = manager.getImportsClosure(ont);
        reasoner.loadOntologies(importsClosure);


        competencyClass = manager.getOWLDataFactory().getOWLClass(URI.create(ontBaseU + "#Competency"));
        topicClass = manager.getOWLDataFactory().getOWLClass(URI.create( ontBaseU + "#Topic"));
        nameableBitClass = manager.getOWLDataFactory().getOWLClass(URI.create(ontBaseU + "#NamableBit"));
        levelClass = manager.getOWLDataFactory().getOWLClass(URI.create(ontBaseU + "#EducationalLevel"));
        commonNameProp = manager.getOWLDataFactory().getOWLDataProperty(
            URI.create(ontBaseU + "#commonName"));
        defaultCommonNameProp = manager.getOWLDataFactory().getOWLDataProperty(
            URI.create(ontBaseU + "#defaultCommonName"));
        unCommonNameProp = manager.getOWLDataFactory().getOWLDataProperty(
            URI.create(ontBaseU + "#unCommonName"));
        rareNameProp = manager.getOWLDataFactory().getOWLDataProperty(
            URI.create(ontBaseU + "#rareName"));
        falseFriendNameProp = manager.getOWLDataFactory().getOWLDataProperty(
            URI.create(ontBaseU + "#falseFriendName"));
        creationDateProp = manager.getOWLDataFactory().getOWLDataProperty(
            URI.create(ontBaseU + "#creationDate"));
        modificationDateProp = manager.getOWLDataFactory().getOWLDataProperty(
            URI.create(ontBaseU + "#modificationDate"));
        creationUserProp = manager.getOWLDataFactory().getOWLDataProperty(
            URI.create(ontBaseU + "#creationUserName"));
        modificationUserProp = manager.getOWLDataFactory().getOWLDataProperty(
            URI.create(ontBaseU + "#modificationUserName"));
        hasTopicProp = manager.getOWLDataFactory().getOWLObjectProperty(
            URI.create(ontBaseU + "#hasTopic"));
        belongsToEducationalPathway = manager.getOWLDataFactory().getOWLObjectProperty(
            URI.create(ontBaseU + "#belongsToEducationalPathway"));
        inEducationalRegion = manager.getOWLDataFactory().getOWLObjectProperty(
            URI.create(ontBaseU + "#inEducationalRegion"));
        age = manager.getOWLDataFactory().getOWLDataProperty(
            URI.create(ontBaseU + "#age"));

        Set<OWLClass> inconsistentClasses = reasoner.getInconsistentClasses();
        if(inconsistentClasses!=null && !inconsistentClasses.isEmpty()) {
            System.out.println("Inconsistent classes " + inconsistentClasses);
        } else {
            System.out.println("Ontology is consistent.");
        }
        reasoner.classify();
    }



    public OWLProperty getHasTopicProperty() {
        return hasTopicProp;
    }

    private static OWLReasoner createReasoner(OWLOntologyManager man) {
           try {
               // Where the full class name for Reasoner is org.mindswap.pellet.owlapi.Reasoner
               // Pellet requires the Pellet libraries  (pellet.jar, aterm-java-x.x.jar) and the
               // XSD libraries that are bundled with pellet: xsdlib.jar and relaxngDatatype.jar
               String reasonerClassName = "org.mindswap.pellet.owlapi.Reasoner";
               Class reasonerClass = Class.forName(reasonerClassName);
               Constructor<OWLReasoner> con = reasonerClass.getConstructor(OWLOntologyManager.class);
               return con.newInstance(man);
           }
           catch (Exception e) {
               throw new RuntimeException(e);
        }
    }


    public OWLOntology getOnt() {
        return ont;
    }

    public String getBaseURI() {
        return ontBaseU;
    }

    public OWLReasoner getReasoner() {
        return reasoner;
    }

    public OWLDataProperty getCommonNameProp() {
        return commonNameProp;
    }


    public OWLDataProperty getDefaultCommonNameProp() {
        return defaultCommonNameProp;
    }

    public OWLDataProperty getUnCommonNameProp() {
        return unCommonNameProp;
    }

    public OWLDataProperty getRareNameProp() {
        return rareNameProp;
    }

    public OWLDataProperty getFalseFriendNameProp() {
        return falseFriendNameProp;
    }

    public OWLClass getCompetencyClass() {
        return competencyClass;
    }

    public Set<String> getOntologyClassesOfIndividual(String uri) {
        Set<String> s = new HashSet<String>();
        OWLIndividual i = getOntologyIndividualOfName(uri);
        if(i==null) return null;
        for(OWLDescription desc: i.getTypes(ont)) {
            OWLClass cls = desc.asOWLClass();
            if(cls!=null) s.add(cls.getURI().toASCIIString());
        }
        return s;
    }


    public OWLClass getOntologyClassOfName(String shortName) {
        return manager.getOWLDataFactory().getOWLClass(craftURI(shortName));
    }

    public OWLIndividual getOntologyIndividualOfName(String shortName) {
        URI uri = craftURI(shortName);
        if(uri==null) return null;
        return manager.getOWLDataFactory().getOWLIndividual(uri);
    }

    public URI craftURI(String shortNameOrUri) {
        if(shortNameOrUri == null || shortNameOrUri.length()==0) return null;
        URI uri;
        if(shortNameOrUri.startsWith("http://")) uri = URI.create(shortNameOrUri);
        else uri = URI.create(ontBaseU +
           (shortNameOrUri.startsWith("#") ? "": "#")
           + shortNameOrUri);
        return uri;
    }

    public boolean isOwlClassUri(String shortNameOrUri) {
        URI uri = craftURI(shortNameOrUri);
        if(uri==null) return false;
        return ont.containsClassReference(uri);
    }
    public boolean isOwlIndividualUri(String shortNameOrUri) {
        URI uri = craftURI(shortNameOrUri);
        if(shortNameOrUri==null) return false;
        return ont.containsIndividualReference(uri);
    }

    public OWLObjectProperty getOntologyPropertyOfName(String shortName) {
        return manager.getOWLDataFactory().getOWLObjectProperty(
                craftURI(shortName));
    }

    public Set<OWLIndividual> getObjectPropertyValue(OWLIndividual i, OWLProperty prop) {
        return i.getObjectPropertyValues(ont).get(prop);
    }

    public Set<String> getTopicsOfCompetencies(String sourceUri) {
        Set<OWLIndividual> indivs = getObjectPropertyValue(getOntologyIndividualOfName(sourceUri),
                hasTopicProp);
        if(indivs == null) return new HashSet<String>();
        Set<String> s = new HashSet<String>(indivs.size());
        for(OWLIndividual i:indivs) {
            s.add(i.getURI().toASCIIString());
        }
        return s;
    }

    public Map<OWLObjectPropertyExpression,Set<OWLIndividual>>
            getPropertyValues(OWLIndividual i) {
        return i.getObjectPropertyValues(ont);
    }

    public String getFirstCommonName(OWLIndividual i) {
        String c = null;
        try {
            c = reasoner.getDataPropertyRelationships(i).get(commonNameProp).iterator().next().getLiteral();
        } catch (OWLReasonerException e) {
            e.printStackTrace();
        }
        return c;
        //return i.getDataPropertyValues(ont).get(commonNameProp)
        //        .iterator().next().getLiteral().toString();
    }

    public String getStringPropertyValue(OWLIndividual i, OWLProperty prop) {
        try {
            Map<OWLDataPropertyExpression,Set<OWLConstant>> m = i.getDataPropertyValues(ont);
            Set<OWLConstant> s = reasoner.getDataPropertyRelationships(i).get(prop);
            if(s==null) return null;
            OWLConstant c = s.iterator().next();
            if(c==null) return null;
            return c.getLiteral();
        } catch (OWLReasonerException e) {
            throw new IllegalStateException(e);
        }
    }

    public Date getModificationDate(OWLIndividual i) {
        return getDateProp(i,modificationDateProp);
    }
    public Date getCreationDate(OWLIndividual i) {
        return getDateProp(i,creationDateProp);
    }
    public String getModificationUserName(OWLIndividual i) {
        return getTextProp(i,modificationUserProp);
    }
    public String getCreationUserName(OWLIndividual i) {
        return getTextProp(i,creationUserProp);
    }

    public Date getDateProp(OWLIndividual i, OWLProperty p) {
        String t = getTextProp(i,p);
        if(t==null) return null;
        try {
            return new ISO8601Parser(new StringReader(t)).dateTimeTypeV().toCalendar().getTime();
        } catch (com.sun.msv.datatype.xsd.datetime.ParseException e) {
            throw new IllegalStateException(e);
        }
    }

    public String getTextProp(OWLIndividual i, OWLProperty p) {
        try {
            Map<OWLDataPropertyExpression,Set<OWLConstant>> m = i.getDataPropertyValues(ont);
            Set<OWLConstant> s = reasoner.getDataPropertyRelationships(i).get(p);
            //m.get(p.asOWLDataProperty());
            if(s==null || !s.iterator().hasNext()) return null;
            OWLConstant c = s.iterator().next();
            if(c.isTyped())
                return c.asOWLTypedConstant().getLiteral();
            else
                return c.asOWLUntypedConstant().getLiteral();
        } catch (OWLReasonerException e) {
            throw new IllegalStateException(e);
        }
    }

    public Map<String,Set<String>> getCommonNames(OWLIndividual i) {
        return getNamesOfProp(i,commonNameProp);
    }

    public Map<String,Set<String>> getDefaultCommonNames(OWLIndividual i) {
        return getNamesOfProp(i,defaultCommonNameProp);
    }

    public Map<String,Set<String>> getUnCommonNames(OWLIndividual i) {
        return getNamesOfProp(i,unCommonNameProp);
    }
    public Map<String,Set<String>> getRareNames(OWLIndividual i) {
        return getNamesOfProp(i,rareNameProp);
    }
    public Map<String,Set<String>> getFalseFriendNames(OWLIndividual i) {
        return getNamesOfProp(i,falseFriendNameProp);
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


    public String getCommonName(OWLIndividual i, String[] langs, boolean firstOrIdIfFail) {
        // first name in given language
        if(i==null || i.getDataPropertyValues(ont)==null || i.getDataPropertyValues(ont).get(commonNameProp)==null)
            return firstOrIdIfFail && i!=null ? i.getURI().getFragment() : null;
        for(String lang:langs) {
            for(OWLConstant x : i.getDataPropertyValues(ont).get(commonNameProp)) {
                String l = null;
                if(x.isTyped())
                    l=x.asOWLTypedConstant().getLiteral();
                else
                    l= x.asOWLUntypedConstant().getLang();
                if(lang.equals(l))
                    return x.getLiteral();
            }
        }
        // otherwise first name without language
        for(OWLConstant x : i.getDataPropertyValues(ont).get(commonNameProp)) {
            if(!x.isTyped() && !x.asOWLUntypedConstant().hasLang())
                return x.getLiteral();
        }
        if(!firstOrIdIfFail) return null;

        // otherwise the first name
        for(OWLConstant x : i.getDataPropertyValues(ont).get(commonNameProp)) {
            return x.getLiteral(); // fancy way to take the first of an iterator if there's one!
        }

        // otherwise the identifier's fragment
        return i.getURI().getFragment();

    }

    public Set<OWLIndividual> getIndividualsOfClass(OWLClass cls) {
        try {
            Set<OWLIndividual> s = new HashSet<OWLIndividual>(reasoner.getIndividuals(cls,false));
            s.addAll(cls.getIndividuals(ont));
            return s;
        } catch (OWLReasonerException e) {
            e.printStackTrace();
            throw new IllegalStateException(e);
        }
    }

    public Set<OWLClass> getDescendantClasses(OWLClass cls) {
        try {
            Set<Set<OWLClass>> subs = reasoner.getDescendantClasses(cls);
            Set<OWLClass> r = new HashSet<OWLClass>();
            for(Set<OWLClass> s: subs) {
                r.addAll(s);
            }
            r.add(cls);
            return r;
        } catch (OWLReasonerException e) {
            e.printStackTrace();
            throw new IllegalStateException(e);
        }
    }

    public Set<String> toURIsListIndiv(Set<OWLIndividual> desc) {
        if(desc==null || desc.size()==0) return Collections.EMPTY_SET;
        Set<String> s= new HashSet<String>(desc.size());
        for(OWLIndividual i:desc) {
            s.add(i.getURI().toASCIIString());
        }
        return s;
    }
    public Set<String> toURIsList(Set<OWLDescription> desc) {
        Set<String> s= new HashSet<String>(desc.size());
        for(OWLDescription obj:desc) {
            if(obj instanceof OWLNamedObject )
                s.add(((OWLNamedObject)obj).getURI().toASCIIString());
        }
        return s;
    }

    public boolean isOfType(OWLIndividual node, OWLClass cls) {
        for(OWLDescription desc: node.getTypes(ont)) {
            if(isSubclassOf(desc.asOWLClass(), cls)) return true;
        }
        return false;
    }

    public boolean isOfLevelType(OWLIndividual node) {
        return isOfType(node, levelClass);
    }

    public boolean isOfTopicType(OWLIndividual node) {
        return isOfType(node, topicClass);
    }
    public boolean isOfCompetencyType(OWLIndividual node) {
        return isOfType(node, competencyClass);
    }

    public Set<OWLClass> getAncestorClasses(OWLIndividual node) {
        Set<OWLDescription> types = node.getTypes(ont);
        HashSet<OWLClass> r = new HashSet<OWLClass>(types.size());
        for(OWLDescription desc: types) {
            try {
                OWLClass c=desc.asOWLClass();
                if(c!=null) r.add(c);
                for(Set<OWLClass> clzs: reasoner.getAncestorClasses(desc)) {
                    r.addAll(clzs);
                }
            } catch (OWLReasonerException e) {
                e.printStackTrace();
                r.add(desc.asOWLClass());
            }
        }
        return r;
    }

    public boolean isSubclassOf(OWLClass cls, OWLClass expectedSup) {
        if(cls==null) return false;
        if(cls.equals(expectedSup)) return true;
        for(OWLDescription sup: cls.getSuperClasses(ont)) {
            if(sup.isAnonymous()) continue;
            if(isSubclassOf(sup.asOWLClass(),expectedSup)) return true;
        }
        return false;
    }

    public Map<String,Set<String>> readRDFLabels(OWLEntity desc) {
        Map<String,Set<String>> map = new HashMap<String,Set<String>>();
        for(OWLAnnotation annotation: desc.getAnnotations(getOnt())) {
            if(!OWLRDFVocabulary.RDFS_LABEL.getURI().equals(annotation.getAnnotationURI()))
                continue;
            OWLConstant constant = annotation.getAnnotationValueAsConstant();
            if(!constant.isTyped()) {
                OWLUntypedConstant label = constant.asOWLUntypedConstant();
                Set<String> names = map.get(label.getLang());
                if(names==null) {
                    names = new HashSet<String>();
                    map.put(label.getLang(),names);
                }
                names.add(label.getLiteral());
            }
        }
        return map;
    }

    public void saveOntology(Writer out) throws OWLOntologyStorageException {
        manager.saveOntology(getOnt(),new WriterOutputTarget(out));
    }


    // IG-252: protect against sub-languages, i.e. consider them language
    // until IG-251 is ready
    
    public static String protectAgainstSubLanguage(String lang) {
        if(lang==null) return null;
        if(lang.length()<=2) return lang;
        else return lang.substring(0,2);
    }


    public static void main(String[] args) throws Throwable{
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology ont = manager.loadOntologyFromPhysicalURI(URI.create(new URL(args[0]).toExternalForm()));
        System.out.println("Ontology loaded.");
        OWLReasoner reasoner = createReasoner(manager);
        Set<OWLOntology> importsClosure = manager.getImportsClosure(ont);
        reasoner.loadOntologies(importsClosure);
        //GeoSkillsAccess access = new GeoSkillsAccess(args[0]);
        System.out.println("Consistent ? " + reasoner.isConsistent(ont));
        Set<OWLClass> inconsistentClasses = reasoner.getInconsistentClasses();
        System.out.println(inconsistentClasses.size() + " inconsistent classes.");
        for(OWLClass c: inconsistentClasses) {
            System.out.println("Inconsistent class: " +
                    GeoSkillsUtil.shortenName(c.getURI().toString(),GeoSkillsAccess.ontBaseU));
        }

    }

    public OWLProperty getBelongsToEducationalPathway() {
        return belongsToEducationalPathway;
    }

    public OWLObjectProperty getInEducationalRegion() {
        return inEducationalRegion;
    }

    public OWLDataProperty getAge() {
        return age;
    }
}