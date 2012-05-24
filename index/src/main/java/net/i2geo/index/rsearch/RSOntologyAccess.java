package net.i2geo.index.rsearch;

import net.i2geo.onto.GeoSkillsAccess;
import net.i2geo.index.IndexHome;
import net.i2geo.index.SKBQueryExpander;
import net.i2geo.api.OntType;

import java.util.*;
import java.io.IOException;

import org.apache.lucene.search.*;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.Term;
import org.semanticweb.owl.model.OWLClass;
import org.semanticweb.owl.model.OWLDescription;
import org.semanticweb.owl.model.OWLIndividual;

/** Encapsulation of the usage of the ontology for the purposes of the search tool.
 * Is currently implemented with owl-api but should move to a remote call or an index
 * query.
 */
public class RSOntologyAccess {

    public static RSOntologyAccess boot(String url, IndexHome indexHome) {
        System.out.println("GeoSkills URL: " + url);
        GeoSkillsAccess access = new GeoSkillsAccess(url);
        return new RSOntologyAccess(access,indexHome);
    }

    private GeoSkillsAccess gsAccess;
    public static final String ontBaseU  = GeoSkillsAccess.ontBaseU;
    public static final float THRESHOLD_SCORE = 0.05f;
    private IndexHome indexHome;


    public RSOntologyAccess(GeoSkillsAccess access, IndexHome indexHome) {
        this.gsAccess = access;
        if(!access.isOpened()) {
            try {
                access.open();
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
        this.indexHome = indexHome;
    }

    public void open() throws Exception {
        gsAccess.open();
    }
    
    String canonicalizeUri(String uri) {
        if(uri.toLowerCase().startsWith("http"))
            return uri;
        if(!uri.startsWith("#")) uri = "#" + uri;
        return GeoSkillsAccess.ontBaseU + uri;
    }


    boolean doesNodeExist(String uri) {
        try {
            String u = this.canonicalizeUri(uri);
            TermDocs d = indexHome.getReader().termDocs(new Term("uri",u));
            if(!d.next()) {
                return false;
            } else {
                if(d.next())
                    System.err.println("[warn]: Ambiguous URI \"" + u + "\".");
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalStateException("Can't read index.",e);
        }
    }


    String getOntType(String uri) {
        try {
            String u = this.canonicalizeUri(uri);
            TermDocs d = indexHome.getReader().termDocs(new Term("uri",u));
            String type=null;
            if(d.next()) {
                int n = d.doc();
                type = indexHome.getReader().document(n).get("ontType");
            } else { // try with _r
                d = indexHome.getReader().termDocs(new Term("uri",u + "_r"));
                if(d.next()) {
                    type = indexHome.getReader().document(d.doc()).get("ontType");
                }
            }
            return type;
        } catch(Exception ex) {
            throw new RuntimeException("can't get ont type of uri \"" + uri + "\".",ex);
        }
    }

    boolean isCompetencyOrTopicNode(String ontType) {
        return OntType.topicOrCompetencyTypeNames.contains(ontType);
    }

    boolean isCompetencyNode(String ontType) {
        return OntType.competencyTypeNames.contains(ontType);
    }
    boolean isTopicNode(String ontType) {
        return OntType.topicTypeNames.contains(ontType);
    }
    boolean isLevelNode(String ontType) {
        return OntType.LEVEL.getName().equals(ontType);        
    }

    public List<BoostedText> fetchNodesMatchingApprox(String text,
            List<String> acceptedLanguages, int maxDocs, boolean withPrefix) {
        List<BoostedText> r = new ArrayList<BoostedText>(10);
        try {
            IndexHome home = IndexHome.getInstance();
            Query q = new SKBQueryExpander(IndexHome.supportedLanguages).expandQuery(text,new String[]{"topic","competency"},acceptedLanguages,withPrefix);
            TopDocs d = home.getSearcher().search(q,(Filter) null,maxDocs);
            int count=0;
            for(ScoreDoc s: d.scoreDocs) {
                Document doc = home.getReader().document(s.doc);
                count++;
                if(s.score< THRESHOLD_SCORE) continue;
                BoostedText bt = new BoostedText(doc.getField("uri").stringValue(),s.score);
                r.add(bt);
                if(count>10) break;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return r;
    }

    public Set<String> getIncludedTopics(String competencyUri) {
        if(competencyUri==null) return null;
        return 
                gsAccess.getTopicsOfCompetencies(competencyUri);
    }

    public String findRepresentativeIfPossible(String basicURI) {
        if(gsAccess.isOwlClassUri(basicURI)) {
            // try to see if there's a _r
            String repUri = basicURI + "_r";
            if(gsAccess.isOwlIndividualUri(repUri)) return repUri;
        }
        return basicURI;
    }

    public List<Set<String>> findAncestorClasses(String uri) {
        LinkedList<Set<String>> l = new LinkedList<Set<String>>();
        feedAncestorClasses(uri, l,3);
        return l;
    }
    private void feedAncestorClasses(String uri, List<Set<String>> classes, int maxLevel) {
        uri = canonicalizeUri(uri);
        if(gsAccess.isOwlIndividualUri(uri)) {
            Set<String> us = new HashSet<String>(); classes.add(us);
            for(OWLDescription desc: gsAccess.getOntologyIndividualOfName(uri).getTypes(gsAccess.getOnt())) {
                if(!desc.isAnonymous()) {
                    us.add(desc.asOWLClass().getURI().toASCIIString());
                }
            }
            // create grand-parents' list and request it
            Set<String> grandParentsURIs = new HashSet<String>();
            classes.add(grandParentsURIs);
            for(String u:us) { feedParentURIsofClass(u,grandParentsURIs,maxLevel-1);}
        } else { // must be a class
            Set<String> uris = new HashSet<String>();
            feedParentURIsofClass(uri,uris,maxLevel-1);
        }
    }

    private void feedParentURIsofClass(String uri, Set<String> parentUrisList, int maxLevel) {
        if(maxLevel==0) return;
        if(uri.endsWith("Thing")) return; // TODO: correct?
        OWLClass c = gsAccess.getOntologyClassOfName(uri);
        for(OWLDescription desc: c.getSuperClasses(gsAccess.getOnt())) {
            String u = desc.asOWLClass().getURI().toASCIIString();
            if(u.endsWith("#Topic_r") || u.endsWith("#Topic") || u.endsWith("#Competency") || u.endsWith("#Thing"))
                continue;
            parentUrisList.add(u);
        }
    }

    public List<Set<String>> findDescendentClassesAndIndividuals(String uri) {
        // if an individual not a representative: nothing
        uri = canonicalizeUri(uri);
        if(gsAccess.isOwlIndividualUri(uri)) {
            if(uri.endsWith("_")) uri = uri.substring(0, uri.length()-2);
        }
        ArrayList<Set<String>> r = new ArrayList<Set<String>>();
        if(gsAccess.isOwlIndividualUri(uri)) return r;

        Set<String> level1 = new HashSet<String>();
        r.add(level1);
        for(OWLIndividual i : gsAccess.getIndividualsOfClass(gsAccess.getOntologyClassOfName(uri))) {
            String u = i.getURI().toASCIIString();
            if(u.endsWith("_r")) continue;
            level1.add(u);
        }
        // TODO: continue to children classes etc..

        // if an individual representative: the subclasses and sub-individuals
        // if a class, the subclasses and instances (but not the representatives)
        return r;
    }

    public Set<String> getOntoClasses(String uri) {
        List<String> d = new ArrayList<String>(gsAccess.getOntologyClassesOfIndividual(uri));
        for(ListIterator<String> it = d.listIterator(); it.hasNext(); ) {
            String s = it.next();
            it.set(canonicalizeUri(s));
        }
        return new HashSet<String>(d);
    }
}
