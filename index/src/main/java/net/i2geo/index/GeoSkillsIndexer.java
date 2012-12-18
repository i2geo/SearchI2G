package net.i2geo.index;


import net.i2geo.api.GeoSkillsConstants;
import net.i2geo.api.MatchingResourcesCounter;
import org.semanticweb.owl.model.*;
import org.semanticweb.owl.inference.OWLReasoner;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.Term;
import org.apache.log4j.*;

import java.lang.reflect.Constructor;
import java.net.URLClassLoader;
import java.util.*;
import java.io.*;
import java.net.URL;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;
import net.i2geo.api.SkillItem;
import net.i2geo.api.OntType;
import net.i2geo.onto.GeoSkillsAccess;


/**
 * Simple class to query an OWL ontology for given types and index their names into
 * a Lucene index.
 * */
public class GeoSkillsIndexer implements GeoSkillsConstants {

    public static final String ontBaseURI = GEOSKILLS_BASE_URI;
    private static String logFile ="work/tmp/IndexingLog.log";



    private static final Logger log = Logger.getLogger(GeoSkillsIndexer.class);
    private IndexHome indexHome;

    public static void main(String[] args) throws Exception {
        IndexHome home = null;
        try {
            new File("target/index").mkdirs();
            if(args.length==1)
                System.setProperty("net.i2geo.onto.geoSkillsDevUrl",args[0]);
            //IndexHome.getInstance("target/index");
            home = new IndexHome("target/index",false);
            new GeoSkillsIndexer(home).run();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(home!=null) home.stopWriting();
        }
    }

    GeoSkillsIndexer(IndexHome home) {
        this.indexHome = home;
    }

    static File getLogFile() {
        return new File(logFile);
                //.replaceAll("/","\\\\"));
    }




    void run() {
        GeoSkillsAccess access = null;
        try {
            GSILogger.openExtraLogFileAppender();

            // start the index
            GSILogger.log("Starting to index.");
            //indexHome = IndexHome.getInstance("target/index");
            indexHome.backItUp();
            GSILogger.log("Backup process concluded.");
            indexHome.emptyIt();
            indexHome.startWriting();
            GSILogger.log("Ready for writing.");

            // inspired from: http://owlapi.svn.sourceforge.net/viewvc/owlapi/owl1_1/trunk/examples/src/main/java/org/coode/owlapi/examples/Example8.java?view=markup




            /* OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            OWLOntology ont = manager.loadOntologyFromPhysicalURI(URI.create(ontologyURL));
            OWLReasoner reasoner = createReasoner(manager);
            log.info("Ontology loaded.");
            Set<OWLOntology> importsClosure = manager.getImportsClosure(ont);
            reasoner.loadOntologies(importsClosure); */

            //reasoner.classify();
            /* DLExpressivityChecker checker = new DLExpressivityChecker(importsClosure);
               log.info("Expressivity: " + checker.getDescriptionLogicName());
               boolean consistent = reasoner.isConsistent(ont);
               log.info("Consistent: " + consistent);
               log.info("\n"); */


            /* OWLDataProperty commonNameProp= manager.getOWLDataFactory().getOWLDataProperty(
                URI.create(ontBaseURI + "#commonName"));
            OWLDataProperty unCommonNameProp= manager.getOWLDataFactory().getOWLDataProperty(
                URI.create(ontBaseURI + "#unCommonName"));
            OWLDataProperty rareNameProp= manager.getOWLDataFactory().getOWLDataProperty(
                URI.create(ontBaseURI + "#rareName"));
            OWLDataProperty falseFriendNameProp= manager.getOWLDataFactory().getOWLDataProperty(
                URI.create(ontBaseURI + "#falseFriendName")); */

            XStream xStream = new XStream(new JettisonMappedXmlDriver());
            //todo: could be doing it all with GeoSkillsParseListener

            System.err.println("Will now access GeoSkills.");
            access = GeoSkillsAccess.getInstance();
            access.open();
            ClassLoader loader = access.getClass().getClassLoader();
            log.info("GeoSkillsAccess is " + access + " loaded with " + loader);
            if(loader instanceof URLClassLoader)
                log.info(".... with URL " + Arrays.asList(((URLClassLoader)loader).getURLs()));
            GSILogger.log("Ontology opened from  "+ access.getOntologyURL() + ".");
            Set<OWLIndividual> nodes = access.getIndividualsOfClass(access.getNameableBitClass());
            for(OWLIndividual node : nodes ) {
                Document doc = new Document();

                // individual URI
                String uri = node.getURI().toString();
                String fragmentId = GSIUtil.uriToName(uri,false);
                GSIUtil.addKeywordField(doc,"uri",uri);

                GSIUtil.addKeywordField(doc,"uri-weak", GSIUtil.uriToName(uri,true).toLowerCase());
                
                GSILogger.log("- " + GSIUtil.nameOf(node) + " (");

                SkillItem theItem = new SkillItem(null,
                        fragmentId, 0, "", fragmentId);
                String urlForMoreInfo = null;
                // types
                // first the primary types
                access.getOntologyClassOfName(fragmentId);
                if(access.isOfCompetencyType(node)) {
                    GSIUtil.addFieldForType(doc,OntType.COMPETENCY);
                    theItem.setType(OntType.COMPETENCY.getName());
                    urlForMoreInfo = GSIUtil.urlForMoreInfo(uri, OntType.COMPETENCY);
                    theItem.setUrlForNavigator(urlForMoreInfo);

                }
                if(access.isOfTopicType(node)) {
                    GSIUtil.addFieldForType(doc,OntType.TOPIC);
                    theItem.setType(OntType.TOPIC.getName());
                    System.out.println("Fragment id " + fragmentId);
                    if(fragmentId.endsWith("_r")) {
                        GSIUtil.addFieldForType(doc,OntType.ABSTRACTTOPIC);
                        GSIUtil.addFieldForType(doc,OntType.ABSTRACTTOPIC_WITH_REPRESENTATIVE);
                    } else {
                        System.out.println("Concrete topic " + fragmentId);
                        GSIUtil.addFieldForType(doc,OntType.CONCRETE_TOPIC);
                    }
                    urlForMoreInfo = GSIUtil.urlForMoreInfo(uri, OntType.TOPIC);
                    theItem.setUrlForNavigator(urlForMoreInfo);
                }
                if(access.isOfLevelType(node)) {
                    GSIUtil.addFieldForType(doc,OntType.LEVEL);
                    theItem.setType("level");
                    urlForMoreInfo = GSIUtil.urlForMoreInfo(fragmentId,OntType.LEVEL);
                    theItem.setUrlForNavigator(urlForMoreInfo);
                }
                if(urlForMoreInfo!=null)
                    GSIUtil.addKeywordField(doc,"urlForNav",urlForMoreInfo);


                Set<OWLIndividual> ancestors = null;
                if (access.isOfCompetencyType(node)) {
                    //PUT THE INCLUDED TOPICS
                    ancestors = access
                            .getPropertyValue(node,access.getHasTopicProperty());
                    if(ancestors ==null) ancestors = new HashSet<OWLIndividual>();
                    else ancestors = new HashSet<OWLIndividual>(ancestors);
                    ancestors.add(node);
                } else if (access.isOfLevelType(node)) {
                    ancestors = (HashSet<OWLIndividual>) new HashSet(Arrays.asList(node));
                    Set<OWLIndividual> pathways = access.getPropertyValue(node,access.getBelongsToEducationalPathway());
                    if(pathways!=null) for(OWLIndividual pathway:pathways) {
                        ancestors.add(pathway);
                        Set<OWLIndividual> regions = access.getPropertyValue(pathway,access.getInEducationalRegion());
                        if(regions!=null) ancestors.addAll(regions);
                    }
                } else // all other cases, topic included
                    ancestors = (HashSet<OWLIndividual>) new HashSet(Arrays.asList(node));

                /* if(uri.endsWith("verbalize_dependency_relations_in_real_world")) {
                    System.out.println("Here!");
                }*/

                if(access.isOfLevelType(node)) {
                    String value = access.getStringPropertyValue(node, access.getAge());
                    if(value!=null) GSIUtil.addKeywordField(doc, "age", value);
                }


                if(ancestors!=null) for(OWLIndividual n: ancestors) {
                    GSIUtil.addFieldForAncestorTopic(doc, GSIUtil.nameOf(n));
                        GSILogger.log(GSIUtil.nameOf(n));
                    for(Iterator<OWLClass> itt = access.getAncestorClasses(n).iterator(); itt.hasNext(); ) {
                        OWLDescription desc = itt.next();
                        GSIUtil.addFieldForAncestorTopic(doc, GSIUtil.nameOf(desc.asOWLClass()));
                        GSILogger.log(GSIUtil.nameOf(desc.asOWLClass()));
                        if(itt.hasNext()) GSILogger.log(", " );
                    }
                }

                GSILogger.log(") :");

                // name properties
                Map<String,Set<String>> defaultCommonNames = access.getNamesOfProp(node,access.getDefaultCommonNameProp());
                if(defaultCommonNames==null || defaultCommonNames.size()==0)
                    GSILogger.log("WARNING: missing defaultCommonName: " + fragmentId);
                Map<String,String>defNames  = GSIUtil.transportNames(defaultCommonNames,
                                fragmentId,"name", GSIUtil.BOOST_COMMONNAME,true,doc);
                Map<String,String> names =
                        GSIUtil.transportNames(access.getNamesOfProp(node,access.getCommonNameProp()),
                                fragmentId,"name", GSIUtil.BOOST_COMMONNAME,true,doc);
                if(names==null) names = new HashMap<String,String>();
                if(defNames!=null) for(String l:defNames.keySet()) {
                    names.put(l, defNames.get(l));
                }

                //transportNames(access.getNamesOfProp(node,access.getCommonNameProp()),
                //        //node.getDataPropertyValues(access.getOnt()).get(access.getCommonNameProp()),
                //        fragmentId,"name",5,true,doc);
                GSIUtil.transportNames(access.getNamesOfProp(node,access.getUnCommonNameProp()),
                        //node.getDataPropertyValues(access.getOnt()).get(access.getUnCommonNameProp()),
                        fragmentId,"name", GSIUtil.BOOST_UNCOMMONNAME,true,doc);
                GSIUtil.transportNames(access.getNamesOfProp(node,access.getRareNameProp()),
                        //node.getDataPropertyValues(access.getOnt()).get(access.getRareNameProp()),
                        fragmentId,"name", GSIUtil.BOOST_RARENAME,true,doc);
                GSIUtil.transportNames(access.getNamesOfProp(node,access.getFalseFriendNameProp()),
                        //node.getDataPropertyValues(access.getOnt()).get(access.getFalseFriendNameProp()),
                        fragmentId,"name", GSIUtil.BOOST_FALSEFRIENDNAME,true,doc);

                if(names!=null) {
                    for(String lang: IndexHome.supportedLanguages) {
                        String tit = names.get(lang);
                        if(tit!=null) {
                            theItem.setReadableTitle(tit);
                            // serialize
                            GSIUtil.addSimplyStoredField(doc,"skillItem-" + lang,xStream.toXML(theItem));
                        }
                        if(tit!=null) GSIUtil.addSimplyStoredField(doc,"title-" + lang,tit);
                    }

                }
                doc.add(new Field("name-x-all",fragmentId,Field.Store.NO, Field.Index.UN_TOKENIZED));

                addResourcesCountToDoc(doc,fragmentId);
                // add document to Index
                indexHome.getWriter().addDocument(doc);
            }


            // now do the same for the topic-groups (which have more than a _r) and the competency-classes
            Set<OWLClass> abstractTopics = access.getDescendantClasses(access.getTopicClass());
            // remove all the ones that just have a _r: they are "trivial topic groups" and their names has been fetched while crawling the individuals
            for(Iterator<OWLClass> it =abstractTopics.iterator(); it.hasNext(); ) {
                OWLClass topicClass = it.next();
                Set<OWLIndividual> instances = access.getIndividualsOfClass(topicClass);
                if(instances == null || instances.size()!=1) continue;
                OWLIndividual i = instances.iterator().next();
                String clsURI = topicClass.getURI().toString();
                String instanceURI = i.getURI().toString();
                if(instanceURI.equals(clsURI + "_r"))
                    it.remove();
            }

            GSILogger.log("=========== Indexing Pure Abstract topics ===================");
            for(OWLClass cls : abstractTopics) {
                Document doc = addDocForClass(xStream, access, cls,OntType.ABSTRACTTOPIC);
                indexHome.getWriter().addDocument(doc);
                GSIUtil.addFieldForType(doc,OntType.TOPIC);
                GSIUtil.addFieldForType(doc,OntType.PURE_ABSTRACT_TOPIC);
                indexHome.getWriter().addDocument(doc);
            }

            GSILogger.log("=========== Indexing Competency Processes ===================");
            for(OWLClass cls : access.getDescendantClasses(access.getCompetencyClass())) {
                Document doc = addDocForClass(xStream, access, cls,OntType.COMPETENCYPROCESS);
                indexHome.getWriter().addDocument(doc);
            }
            // delete previous modification date document
            try { indexHome.getWriter().deleteDocuments(new Term("isModifDate","yes"));
            } catch(Exception ex){ex.printStackTrace();}
            // insert modification date special document
            Document doc = new Document();
            doc.add(new Field("isModifDate","yes", Field.Store.NO, Field.Index.UN_TOKENIZED));
            doc.add(new Field("modificationDate","modified on " + new Date(), Field.Store.YES, Field.Index.UN_TOKENIZED));
            indexHome.getWriter().addDocument(doc);

            // close and flush all writes
            indexHome.stopWriting();


            // now index subjects
            new SubjectsCollector(
                    new URL(
                        GeoSkillsAccess.getInstance().getOntologyURL().toExternalForm().replace("GeoSkills.owl","Subjects.owl"))
                    ).run(indexHome);

            GSILogger.log("Have indexed.");
        } catch (Exception e) {
            log.warn(e);
            e.printStackTrace();
            //indexHome.recoverFromBackup();
            //indexHome.computeStatus(false);
            throw new IllegalStateException(this.getClass().getName() + "'s run failed.", e);
        } finally {
            GSILogger.closeExtraLogFileAppender();
            if(access!=null)
                access.forgetLoadedOntology();
        }

    }

    private Document addDocForClass(XStream xStream, GeoSkillsAccess access, OWLClass cls, OntType ontType) throws IOException {
        //System.err.println("Adding document for uri " + cls.getURI());
        Document doc = new Document();

        String uri = cls.getURI().toString();
        String fragmentId = GSIUtil.uriToName(uri);
        doc.add(new Field("uri",uri,
                Field.Store.YES,Field.Index.UN_TOKENIZED));
        GSILogger.log("- "+ ontType + ":" + fragmentId + " (");

        SkillItem theItem = new SkillItem(null,
                fragmentId, 0, "", fragmentId);

        // compute types
        doc.add(new Field("ontType",ontType.getName(),
                Field.Store.YES,Field.Index.UN_TOKENIZED));
        theItem.setType(ontType.getName());
        theItem.setUrlForNavigator(GSIUtil.urlForMoreInfo(uri,ontType));


        Map<String,Set<String>> names = access.readRDFLabels(cls);
        // create the skill-items
        for(String lang: IndexHome.supportedLanguages) {
            if(names!=null) {
                Set<String> tit = names.get(lang);
                if(tit!=null && !tit.isEmpty()) {
                    theItem.setReadableTitle(tit.iterator().next());
                    GSIUtil.addSimplyStoredField(doc,"title-" + lang,tit.iterator().next());
                }
            }
            if(theItem.getReadableTitle()==null)
                theItem.setReadableTitle(GSIUtil.nameOf(cls));
            GSIUtil.addSimplyStoredField(doc,"skillItem-" + lang,
                    xStream.toXML(theItem));
        }

        // add the matching names
        float boost=5.0f;
        if(names!=null) {
            for(String lang: IndexHome.supportedLanguages) {
                String tit = null;
                if(names.get(lang)!=null) for(String name: names.get(lang)) {
                    if(tit==null) tit = name;
                    GSILogger.log("  - " + lang + " : " + 5.0 + ":" + name);
                    Field field = new Field("name-" + lang,name,
                            Field.Store.YES,Field.Index.TOKENIZED);
                    field.setBoost(boost);
                    doc.add(field);
                    // universal language as well
                    field = new Field("name-x-all",name,
                            Field.Store.NO,Field.Index.TOKENIZED);
                    field.setBoost(boost);
                    doc.add(field);
                }
            }
            doc.add(new Field("name-x-all",fragmentId, Field.Store.YES, Field.Index.UN_TOKENIZED));
        }

        addResourcesCountToDoc(doc,fragmentId);

        return doc;
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

    private void addResourcesCountToDoc(Document doc, String fragmentId) {
        if(indexHome.getMatchingResourcesCounter()==null) return;
        int count = indexHome.getMatchingResourcesCounter().countMatchingResources(fragmentId);
        GSIUtil.addSimplyStoredField(doc,"numResources",""+count);
        if(count>0)
            doc.add(new Field("hasResources","true", Field.Store.NO, Field.Index.UN_TOKENIZED));
    }


    protected void finalize() {
        log.info("GeoSkillsIndexer " + this.toString() + " being finalized.");
    }


}


