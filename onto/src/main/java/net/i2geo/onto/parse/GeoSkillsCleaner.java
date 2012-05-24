package net.i2geo.onto.parse;

import org.jdom.*;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;
import org.jdom.input.SAXBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

/** Class to perform XML manipulations to clean-up some dirt of the ontology
 * that we see appear.
 */
public class GeoSkillsCleaner {

    public GeoSkillsCleaner(File inputFile, File outputFile) {
        this.inputFile = inputFile;
        this.outputFile = outputFile;
    }
    private final File inputFile, outputFile;
    private int errorCount = 0;
    private Document ontoDoc = null;
    private static List keepOutIDs = Arrays.asList("Thing","NamableBit","Competency","Topic","EducationalLevel");
    private Map<String,Element> capitalNameToElement = new HashMap<String,Element>(2000),
        nameToElement = new HashMap<String,Element>(2000);
    private static Namespace RDF_NAMESPACE = Namespace.getNamespace("rdf","http://www.w3.org/1999/02/22-rdf-syntax-ns#"),
        RDFS_NAMESPACE = Namespace.getNamespace("rdfs","http://www.w3.org/2000/01/rdf-schema#"),
        GS_NAMESPACE = Namespace.getNamespace("gs","http://www.inter2geo.eu/2008/ontology/GeoSkills#");

    private void run() throws Exception {
        parseFile();
        reportNamesAmbiguities();
        moveNamesAndDatesFromClassesToRepresentative();
        outputDoc();
    }

    private void parseFile() throws Exception {
        ontoDoc = new SAXBuilder().build(inputFile.toURL());
    }

    private void reportNamesAmbiguities() throws Exception {
        XPath xp = XPath.newInstance("*//@rdf:about");
        xp.addNamespace(RDF_NAMESPACE);

        List<Attribute> atts = new ArrayList<Attribute>((List<Attribute>) xp.selectNodes(ontoDoc));


        
        // first prefill the table
        for(Attribute att: atts) {
            String name = att.getValue();
            Element elt = att.getParent();
            Element wasThere = capitalNameToElement.get(name.toUpperCase());
            if(wasThere!=null && !keepOutIDs.contains(name) && "Description".equals(elt.getName())) {
                System.out.println("Moving labels of " + elt + " of id \"" + name + "\" to pre-existing one.");
                wasThere.addContent("\n      ");
                //wasThere.addContent(new Comment("moved from a below place"));
                wasThere.addContent("\n      ");
                for(Content child: new ArrayList<Content>((List<Content>)elt.getContent())) {
                    if(!(child instanceof Element )|| "label".equals(((Element)child).getName()) || ((Element)child).getName().endsWith("Name"))
                        wasThere.addContent(cleanUpChild(child.detach()));
                }
                //elt.detach();
                /* for(Element child: (List<Element>) elt.getChildren()) {
                    cleanUpChild(child);
                }*/
            } else {
                capitalNameToElement.put(name.toUpperCase(),elt);
            }
            //if(wasThere!=null) System.out.println(name + " is duplicate, up to case.");
            nameToElement.put(name,elt);
        }

        // now proof that each node having a _r has a class with same casing
        for(Map.Entry<String,Element> entry: nameToElement.entrySet()) {
            String name = entry.getKey();
            if(name.endsWith("_r")) {
                String className = name.substring(0,name.length()-2),
                    classNameUp = className.toUpperCase();
                if(!nameToElement.containsKey(className)) {
                    if(capitalNameToElement.containsKey(classNameUp))
                        reportError(name + " should have a class called " + className + " but it is called " +
                                capitalNameToElement.get(classNameUp).getAttributeValue("about", RDF_NAMESPACE));
                    else
                        reportError(name + " should have a class called " + className + " but I could not find it.");
                }
            }
        }

        // now order the children of root by the value of their rdf:about if any
        final List<Element> childList = new ArrayList<Element>(ontoDoc.getRootElement().getChildren());

        for(Element elt:childList) { elt.detach(); }

        Collections.sort(childList, new Comparator<Element>() {
            @Override
            public int compare(Element o1, Element o2) {
                String id1 = o1.getAttributeValue("about", RDF_NAMESPACE),
                        id2 = o2.getAttributeValue("about",RDF_NAMESPACE);
                if(id1 == null && id2 == null) {
                    return compareIndexes(o1,o2);
                }
                if(id1==null && id2 !=null) return 1;
                if(id1!=null && id2 ==null) return -1;
                // both non-null
                int r = id1.compareTo(id2);
                if(r==0) return compareIndexes(o1,o2);
                return r;
            }
            private int compareIndexes(Element o1, Element o2) {
                int p1 = childList.indexOf(o1),
                        p2 = childList.indexOf(o2);
                if(p1 < p2) return 1;
                if(p1 ==p2) return 0;
                if(p1 > p2) return -1;
                throw new IllegalStateException("can't compage " + p1 + " and " + p2);
            }
        });
        for(Element elt: childList) {
            ontoDoc.getRootElement().addContent("\n    ");
            ontoDoc.getRootElement().addContent(elt); }
    }


    private void outputDoc() throws IOException {
        FileOutputStream out = new FileOutputStream(outputFile);
        new XMLOutputter().output(ontoDoc, out);
        out.flush();out.close();
    }

    private void reportError(String errorMessage) {
        errorCount++;
        System.out.println(errorMessage);
    }


    private void moveNamesAndDatesFromClassesToRepresentative() throws JDOMException {
        XPath xp = XPath.newInstance("//gs:modified");
        xp.addNamespace(GS_NAMESPACE);
        List<Element> modifiedElements = xp.selectNodes(ontoDoc);
        for(Element modified: modifiedElements) {
            Element parent = modified.getParentElement();
            if(parent !=null && parent.getAttribute("about",RDF_NAMESPACE)!=null) {
                String id = parent.getAttributeValue("about",RDF_NAMESPACE);
                if(id == null || id.endsWith("_r")) continue;
                String id_r = id + "_r";
                Element representative = nameToElement.get(id_r);
                if(representative!=null) {
                    moveNamesAndDates(parent,representative);
                }
            }
        }
    }

    private void moveNamesAndDates(Element from, Element to) {
        tryMove(from.getChild("creator",GS_NAMESPACE),to);
        tryMove(from.getChild("created",GS_NAMESPACE),to);
        tryMove(from.getChild("modified",GS_NAMESPACE),to);
        tryMoveToCommonName(from.getChild("label",RDF_NAMESPACE),to);
    }

    private void tryMove(Element child, Element toParent) {
        if(child==null) return;
        child.detach();
        if(toParent.getChild(child.getName(),child.getNamespace())==null) {
            toParent.addContent(child);
        }
    }

    private void tryMoveToCommonName(Element elt, Element to) {
        boolean foundIt = false;
        if(elt==null) return;
        String text = elt.getTextNormalize(),
            lang = elt.getAttributeValue("lang",Namespace.XML_NAMESPACE);
        elt.detach();
        if(lang == null || text==null) return;
        for(Element ch : (List<Element>) to.getChildren()) {
            if(!ch.getName().endsWith("Name")) continue;
            if(!lang.equals(ch.getAttributeValue("lang",Namespace.XML_NAMESPACE))) continue;
            if(text.equals(ch.getTextNormalize())) {
                foundIt = true; break;
            }
        }
        if(!foundIt) {
            Element name = new Element("commonName",GS_NAMESPACE);
            name.addContent(text);
            name.setAttribute("lang",lang,Namespace.XML_NAMESPACE);
            to.addContent(name);
        }
    }

    private Content cleanUpChild(Content node) {
        if(node instanceof Element) {
            Element elt = (Element) node;
            if("label".equals(elt.getName()) || "subClassOf".equals(elt.getName()))
                elt.setNamespace(RDFS_NAMESPACE);
            return node;
        } else {
            return node;
        }
    }
    
    public static void main(String[] args) throws Throwable {
        GeoSkillsCleaner cleaner = new GeoSkillsCleaner(new File(args[0]),new File(args[1]));
        cleaner.run();
        if(cleaner.errorCount>0) System.out.println(cleaner.errorCount + " errors.");
        if(cleaner.errorCount==0) {
            cleaner.outputDoc();
        }

    }

}
