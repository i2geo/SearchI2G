package net.i2geo.onto;

import org.semanticweb.owl.apibinding.OWLManager;
import org.semanticweb.owl.model.*;
import org.semanticweb.owl.io.OWLXMLOntologyFormat;
import org.jdom.input.SAXBuilder;
import org.jdom.Document;
import org.jdom.Text;
import org.jdom.filter.Filter;
import org.jdom.xpath.XPath;

import java.net.URI;
import java.io.*;
import java.util.HashSet;
import java.util.Set;
import java.util.Iterator;

import junit.framework.TestCase;
import junit.framework.AssertionFailedError;

/** tests the save and load of an ontology that contains fancy characters
 */
public class TestInternationalizedOntology extends TestCase {

    public static final String lambda = "\u03bb",
        divided = "\u00f7", mu = "\u03bc",
       desiredName= "a string with lambda and mu: λ÷μ.";

    public void testLoadAndSaveOntologyWithLambda() throws Exception {
        // A simple example of how to load and save an ontology
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        URI uri = URI.create("http://blip/blop");
        OWLOntology ontology = manager.createOntology(uri);
        OWLDataFactory factory = manager.getOWLDataFactory();
        OWLClass clsA = factory.getOWLClass(URI.create(uri + "#"+lambda+mu));
        OWLClass clsB = factory.getOWLClass(URI.create(uri + "#x"));
        OWLAxiom axiom = factory.getOWLSubClassAxiom(clsA, clsB);
        AddAxiom addAxiom = new AddAxiom(ontology, axiom);
        manager.applyChange(addAxiom);
        OWLDataProperty hasName = factory.getOWLDataProperty(URI.create(uri+ "#name"));
        manager.applyChange(new AddAxiom(ontology,factory.getOWLFunctionalDataPropertyAxiom(hasName)));

        OWLIndividual individual = factory.getOWLIndividual(URI.create(uri+"#blop"));
        individual.getTypes(ontology).add(clsB);
        manager.applyChange(new AddAxiom(ontology, factory.getOWLDeclarationAxiom(individual)));
        manager.applyChange(new AddAxiom(ontology, factory.getOWLDataPropertyAssertionAxiom(individual,hasName,
                factory.getOWLTypedConstant(desiredName))));
        


        Set<String> thingsNames = new HashSet<String>();
        Set<OWLNamedObject> things = new HashSet<OWLNamedObject>(ontology.getReferencedClasses());
        things.addAll(ontology.getReferencedIndividuals());

        for(OWLNamedObject cls : things) {
            System.out.println("Referenced name: " + cls);
            thingsNames.add(cls.toString());
        }
        File file = File.createTempFile("blop",".owl");
        URI physicalURI = file.toURI();
        System.out.println("Outputting ontology to " + file);
        manager.saveOntology(ontology, new OWLXMLOntologyFormat(), physicalURI);
        manager.removeOntology(ontology.getURI());
        ontology = null;
        System.gc();


        OWLOntology o2 = manager.loadOntology(file.toURI());
        boolean failed = false;
        things = new HashSet<OWLNamedObject>(o2.getReferencedClasses());
        things.addAll(o2.getReferencedIndividuals());
        for(OWLNamedObject object : things) {
            System.out.println("Referenced object: " + object);
            if(!thingsNames.contains(object.toString())) {
                System.out.println("-- newly inserted object " + object);
                failed = true;
            }
            thingsNames.remove(object.toString());
        }
        if(!thingsNames.isEmpty()) {
            System.out.println("Extra object(s) found: " + thingsNames);
            failed = true;
        }
        if(failed) {
            throw new AssertionFailedError("Object names comparison failed. See above.");
        }

        OWLConstant s = factory.getOWLIndividual(URI.create(uri+ "#blop")).getDataPropertyValues(o2).
                get(hasName).iterator().next();
        assertEquals(s.asOWLTypedConstant().getLiteral(),desiredName);


        // now test that the caracters are in the file

        Document xmlDoc = new SAXBuilder().build(file.toURL());
        Iterator<Text> texts = (Iterator<Text>) xmlDoc.getDescendants(new Filter() {
            public boolean matches(Object o) {
                return o instanceof Text;
            }
        });
        boolean found = false;
        while(texts.hasNext()) {
            String t = texts.next().getTextNormalize();
            if( t==null ) break;
            if(t.length()==0) continue;
            System.out.println("Text: " + t);
            if(desiredName.equals(t)) {
                found = true;
                break;
            }
        }
        assertTrue("should find: \""+desiredName+"\"",found);
        System.out.println("Have found.");

        char[] buff = new char[(int) file.length()];
        int p=0, l=0;
        Reader in = new InputStreamReader(new FileInputStream(file),"utf-8");
        while( (l=in.read(buff,p,buff.length-p))>0) { p += l; }
        in.close();

        StringBuffer content = new StringBuffer(buff.length);
        for(int i=0; i<p; i++) {
            char c= buff[i];
            if(c!='&') content.append(c);
            if(i+2<l && buff[i+1]=='#') {
                StringBuffer entity = new StringBuffer(8);
                entity.append(buff[i]);
            }
        }
        System.out.println("Have found desired string: " + content);
    }

    
}
