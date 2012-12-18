package net.i2geo.index;

import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexWriter;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;

import net.i2geo.api.OntType;
import net.i2geo.api.SkillItem;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;

public class SKBUpdater extends Thread {

    private static long LAZINESS_TIME = 5000;
    private static Namespace ontoUpNS = Namespace.getNamespace("gs","http://www.inter2geo.eu/2008/ontology/ontoUpdates.owl#"),
        rdfNS = Namespace.getNamespace("rdf","http://www.w3.org/1999/02/22-rdf-syntax-ns#"),
        gsNS =  Namespace.getNamespace("gs","http://www.inter2geo.eu/2008/ontology/GeoSkills#"),
        rdfsNS = Namespace.getNamespace("rdfs","http://www.w3.org/2000/01/rdf-schema#"),
        owlNS = Namespace.getNamespace("owl","http://www.w3.org/2002/07/owl#");


    public SKBUpdater(SKBUpdateQueue queue, IndexHome indexHome) {
        super("SKBUpdater");
        this.queue = queue;
        this.indexHome = indexHome;
    }
    private final SKBUpdateQueue queue;
    private final IndexHome indexHome;
    private long lastDone = -1;
    private XStream xstream = new XStream(new JettisonMappedXmlDriver());

    public void run() {
        try {
            while(true) {
                synchronized(queue) {queue.wait(1000);}
                if(System.currentTimeMillis()-lastDone< LAZINESS_TIME) continue;
                File[] files = queue.listFilesToProcess();
                if(files.length>0) {
                    processFiles(files);
                    lastDone = System.currentTimeMillis();
                }
            }
        } catch (InterruptedException e) {
            System.err.println(super.toString() + ": interrupted, quitting.");
        }
        System.err.println("SKBUpdater has quit.");
    }

    private void processFiles(File[] files) {
        indexHome.startWriting();
        IndexWriter writer = indexHome.getWriter();
        for (int i=0; i<files.length; i++) {
            try {
                System.out.println("Processing file " + files[i]);
                Element root = new SAXBuilder().build(files[i]).getRootElement();
                List<Element> additions = root.getChildren("Additions", ontoUpNS),
                        updates = root.getChildren("Updates", ontoUpNS),
                        deletions = root.getChildren("Deletions", ontoUpNS);
                for(Element addition:additions) { for(Element elt:(List<Element>) addition.getChildren()) {
                    processAddition(writer,elt);
                }}
                for(Element update:updates) { for(Element elt:(List<Element>) update.getChildren()) {
                    processUpdate(writer,elt);
                }}
                for(Element deletion:deletions) { for(Element elt:(List<Element>) deletion.getChildren()) {
                    processDeletion(writer,elt);
                }}
            } catch(Exception ex) {
                ex.printStackTrace();
                try{FileUtils.copyFileToDirectory(files[i],queue.rejectsDir);}
                    catch(Exception x){x.printStackTrace();}
            } finally {
                System.out.println("Finished update for " + files[i]);
                indexHome.stopWriting();
                files[i].delete();
            }
        }
    }

    private void processAddition(IndexWriter writer, Element addition) throws Exception {
        processUpdate(writer,addition);
    }
    private void processUpdate(IndexWriter writer, Element update) throws Exception {
        String uri = readURI(update);
        System.out.println("Updating uri " + uri);
        Document doc = createDocBasic(update);
        writer.deleteDocuments(new Term("uri",uri));
        writer.addDocument(doc);
    }
    private void processDeletion(IndexWriter writer, Element deletion) throws Exception {
        writer.deleteDocuments(new Term("uri",readURI(deletion)));
    }

    private String readURI(Element m) {
        return m.getAttributeValue("ID",rdfNS);
    }

    private Document createDocBasic(Element elt) {
        Document doc = new Document();
        String uri=readURI(elt);
        // put URI
        GSIUtil.addKeywordField(doc,"uri",uri);
        doc.add(new Field("name-x-all",uri, Field.Store.NO, Field.Index.UN_TOKENIZED));

        // all forms of names
        Map<String,String> firstNames = new HashMap<String,String>();
        
        for(Element nm:(List<Element>) elt.getChildren("label",rdfsNS)) {
            addLangField(doc,nm,"name-",GSIUtil.BOOST_COMMONNAME,firstNames);}
        for(Element nm:(List<Element>) elt.getChildren("defaultCommonName",gsNS)) {
            addLangField(doc,nm,"name-",GSIUtil.BOOST_COMMONNAME,firstNames);}
        for(Element nm:(List<Element>) elt.getChildren("commonName",gsNS)) {
            addLangField(doc,nm,"name-",GSIUtil.BOOST_COMMONNAME,firstNames);}
        for(Element nm:(List<Element>) elt.getChildren("unCommonName",gsNS)) {
            addLangField(doc,nm,"name-",GSIUtil.BOOST_UNCOMMONNAME,firstNames);}
        for(Element nm:(List<Element>) elt.getChildren("rareName",gsNS)) {
            addLangField(doc,nm,"name-",GSIUtil.BOOST_RARENAME,firstNames);}
        for(Element nm:(List<Element>) elt.getChildren("falseFriendName",gsNS)) {
            addLangField(doc,nm,"name-",GSIUtil.BOOST_FALSEFRIENDNAME,firstNames);}

        // ontTypes
        OntType basicType = null;
        if(elt.getNamespace() == gsNS) {// an individual in GeoSkills
            if("Competency".equals(elt.getName())) {
                basicType = OntType.COMPETENCY;
            } else if("Topic".equals(elt.getName())) {
                basicType = OntType.TOPIC;
                if(uri.endsWith("_r")) {
                    GSIUtil.addFieldForType(doc,OntType.ABSTRACTTOPIC_WITH_REPRESENTATIVE);
                } else {
                    GSIUtil.addFieldForType(doc,OntType.CONCRETE_TOPIC);
                }
            }
        } else if (elt.getNamespace() == owlNS) { // a class: should be an pure-abstract-toic or a competency-process 
            basicType = OntType.COMPETENCYPROCESS;
            // fixme... how to detect a pure-abstract-topic? for now we don't
        }
        if(basicType==null) basicType= OntType.TOPIC;
        GSIUtil.addFieldForType(doc,basicType);

        // skill-item
        for(String lang:firstNames.keySet()) {
            SkillItem item = new SkillItem(firstNames.get(lang),uri, 0,
                    GSIUtil.urlForMoreInfo(uri,basicType),uri);
            item.setType(basicType.getName());
            // TODO: better url-to-navigate maps
            GSIUtil.addSimplyStoredField(doc,"skillItem-" + lang,xstream.toXML(item));
        }

        return doc;
    }

    private void addLangField(Document doc, Element nm, String fieldNamePrefix, float boost, Map<String,String> firstNames) {
        String lang = nm.getAttributeValue("lang",Namespace.XML_NAMESPACE);
        if(lang==null) lang = "en";
        Field field = new Field(fieldNamePrefix + lang,nm.getText(),Field.Store.YES, Field.Index.TOKENIZED);
        if(firstNames.get(lang)==null) firstNames.put(lang,nm.getText());
        field.setBoost(boost);
        doc.add(field);
    }

}
