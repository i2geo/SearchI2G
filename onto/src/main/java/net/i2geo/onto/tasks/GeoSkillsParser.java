package net.i2geo.onto.tasks;

import net.i2geo.onto.GeoSkillsAccess;
import net.i2geo.onto.parse.GeoSkillsParseListener;
import org.semanticweb.owl.model.*;
import org.semanticweb.owl.vocab.OWLRDFVocabulary;

import java.util.*;
import java.net.URI;

/**
 */
public class GeoSkillsParser {



    public GeoSkillsParser() {
        this(null);
    }

    public GeoSkillsParser(String urlToOntology) {
        if(urlToOntology==null)
            urlToOntology = GeoSkillsAccess.geoSkillsDevUrl;
        this.urlToOntology = urlToOntology;
    }

    public void setUrlToOntology(String url) {
        this.urlToOntology = url;
    }

    private String urlToOntology = null;
    GeoSkillsAccess access;

    public void runParser(GeoSkillsParseListener listener) throws Exception {

        this.access = new GeoSkillsAccess(urlToOntology);
        this.access.open();

        // first notify of IDs

        for(OWLClass cls: access.getDescendantClasses(access.getTopicClass())) {
            listener.thereIsItem(cls.getURI().toASCIIString(),
                    GeoSkillsParseListener.TYPE_TOPIC_CLASS);
        }
        for(OWLClass cls: access.getDescendantClasses(access.getCompetencyClass())) {
            listener.thereIsItem(cls.getURI().toASCIIString(),
                    GeoSkillsParseListener.TYPE_COMPETENCY_CLASS);
        }
        for(OWLIndividual i: access.getIndividualsOfClass(access.getTopicClass())) {
            listener.thereIsItem(i.getURI().toASCIIString(),
                    GeoSkillsParseListener.TYPE_TOPIC);
        }
        for(OWLIndividual i: access.getIndividualsOfClass(access.getCompetencyClass())) {
            listener.thereIsItem(i.getURI().toASCIIString(),
                    GeoSkillsParseListener.TYPE_COMPETENCY);
        }

        // then describe types

        Set<URI> alreadyDones = new HashSet<URI>();

        for(OWLClass i: access.getDescendantClasses(access.getTopicClass())) {
            if(alreadyDones.contains(i.getURI())) { System.err.println("Duplicate URI " + i.getURI()); continue; }
            alreadyDones.add(i.getURI());
            Set<String> parentTypes =
                access.toURIsList(i.getSuperClasses(access.getOnt()));
            listener.topicType(i.getURI().toASCIIString(), parentTypes,
                    readRDFLabels(access,i));
        }
        for(OWLClass i: access.getDescendantClasses(access.getCompetencyClass())) {
            if(alreadyDones.contains(i.getURI())) { System.err.println("Duplicate URI " + i.getURI()); continue; }
            alreadyDones.add(i.getURI());
            Set<String> parentTypes = access.toURIsList(i.getSuperClasses(access.getOnt()));
            listener.competencyType(i.getURI().toASCIIString(), parentTypes,
                    readRDFLabels(access,i));
        }

        // then describe individuals
        for(OWLIndividual i: access.getIndividualsOfClass(access.getTopicClass())) {
            if(alreadyDones.contains(i.getURI())) { System.err.println("Duplicate URI " + i.getURI()); continue; }
            alreadyDones.add(i.getURI());
            String uri = i.getURI().toASCIIString();
            Set<String> parentUris = access.toURIsList(i.getTypes(access.getOnt()));
            listener.topicDescription(uri,parentUris,makeNamesMap(i),new HereModificationData(i));
        }
        for(OWLIndividual i: access.getIndividualsOfClass(access.getCompetencyClass())) {
            if(alreadyDones.contains(i.getURI())) { System.err.println("Duplicate URI " + i.getURI()); continue; }
            alreadyDones.add(i.getURI());
            String uri = i.getURI().toASCIIString();
            Set<String> parentUris = access.toURIsList(i.getTypes(access.getOnt()));
            Set<OWLIndividual> s = i.getObjectPropertyValues(access.getOnt()).get(access.getOntologyPropertyOfName("hasTopic"));
            listener.competencyDescription(uri,parentUris,access.toURIsListIndiv(s),makeNamesMap(i),new HereModificationData(i));
        }
    }

    private NamesMap makeNamesMap(OWLIndividual i) {
        NamesMap namesMap = new NamesMap();
        Set<NameWithLanguage> alreadyDones = new HashSet<NameWithLanguage>();
        feedNamesInMap(access.getDefaultCommonNames(i),namesMap,alreadyDones,1.0f);
        feedNamesInMap(access.getCommonNames(i),namesMap,alreadyDones,0.9f);
        feedNamesInMap(access.getUnCommonNames(i),namesMap,alreadyDones,0.7f);
        feedNamesInMap(access.getRareNames(i),namesMap,alreadyDones,0.3f);
        feedNamesInMap(access.getFalseFriendNames(i),namesMap,alreadyDones,-1.0f);
        return namesMap;
    }

    private void feedNamesInMap(Map<String,Set<String>> omap, NamesMap namesMap, Set<NameWithLanguage> alreadyDones, float score) {
        for(Map.Entry<String,Set<String>> entry: omap.entrySet()) {
            if(entry==null) continue;
            String lang = GeoSkillsAccess.protectAgainstSubLanguage(entry.getKey());
            Set<GeoSkillsParseListener.NameWithFrequency> namesForLanguage = namesMap.map.get(lang);
            if(namesForLanguage==null) {
                namesForLanguage = new HashSet<GeoSkillsParseListener.NameWithFrequency>();
                namesMap.map.put(lang,namesForLanguage);
            }
            for(String s:entry.getValue()) {
                NameWithLanguage k = new NameWithLanguage(s,lang);
                if(alreadyDones.contains(k)) {// System.out.println("Removing duplicate name \"" + k.language + ": " + k.name);
                        continue;}
                alreadyDones.add(k);
                NameWithFrequencyImpl n = new NameWithFrequencyImpl();
                n.frequency = score;
                n.string = s;
                namesForLanguage.add(n);
            }
        }
    }

    private class HereModificationData implements GeoSkillsParseListener.ModificationData {
        HereModificationData(OWLIndividual i) {
            this.modificationDate = access.getModificationDate(i);
            this.creationDate = access.getCreationDate(i);
            this.modificationUserName = access.getModificationUserName(i);
            this.creationUserName = access.getCreationUserName(i);
        }
        private final Date modificationDate, creationDate;
        private final String modificationUserName, creationUserName;

        public Date getModificationDate() {
            return modificationDate;
        }

        public Date getCreationDate() {
            return creationDate;
        }

        public String getModificationUserName() {
            return modificationUserName;
        }

        public String getCreationUserName() {
            return creationUserName;
        }
    }


    public static class NamesMap implements GeoSkillsParseListener.NamesMap {
        public NamesMap(){}
        private Map<String,Set<GeoSkillsParseListener.NameWithFrequency>> map = new HashMap<String,Set<GeoSkillsParseListener.NameWithFrequency>>();
        public Iterator<String> getLanguages(){return map.keySet().iterator();}
        public Iterator<GeoSkillsParseListener.NameWithFrequency> getNames(String language)
            {return map.get(language).iterator(); }

    }

    private static class NameWithLanguage {
        private final String name;
        private final String language;
        private NameWithLanguage(String name, String language) {
            this.name = name;
            this.language = language;
        }

        public int hashCode() {
            return name.hashCode() + language.hashCode();
        }

        public boolean equals(Object o) {
            if(!(o instanceof NameWithLanguage)) return false;
            NameWithLanguage other = (NameWithLanguage) o;
            return other.language.equals(this.language) && other.name.equals(this.name);
        }
    }

    public static class NameWithFrequencyImpl implements GeoSkillsParseListener.NameWithFrequency {

        float frequency;
        String string;

        public float getFrequency() {return frequency;}
        public String getName() { return string; }
    }

    public static NamesMap readRDFLabels(GeoSkillsAccess access, OWLClass desc) {
        // TODO: this duplicates readRDFLabels(OWLEntity desc)
        NamesMap map = new NamesMap();
        for(OWLAnnotation annotation: desc.getAnnotations(access.getOnt())) {
            if(!OWLRDFVocabulary.RDFS_LABEL.getURI().equals(annotation.getAnnotationURI()))
                continue;
            OWLConstant constant = annotation.getAnnotationValueAsConstant();
            if(!constant.isTyped()) {
                OWLUntypedConstant label = constant.asOWLUntypedConstant();
                Set<GeoSkillsParseListener.NameWithFrequency> names = map.map.get(label.getLang());
                if(names==null) {
                    names = new HashSet<GeoSkillsParseListener.NameWithFrequency>();
                    map.map.put(label.getLang(),names);
                }
                NameWithFrequencyImpl name = new NameWithFrequencyImpl();
                name.frequency = 1.0f;
                name.string = label.getLiteral();
                names.add(name);
            }
        }
        return map;
    }
}
