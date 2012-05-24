package net.i2geo.onto;

import org.semanticweb.owl.model.OWLClass;
import org.semanticweb.owl.model.OWLIndividual;
import org.semanticweb.owl.model.OWLProperty;
import org.semanticweb.owl.model.OWLNamedObject;

import java.io.File;
import java.io.Writer;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
import java.util.*;
import java.net.URI;

/**
 * Class to create a set of linked HTML pages for every appropriate
 * node of the ontology.
 */
public class GeoSkillsHtmlGenerator {

    public GeoSkillsHtmlGenerator(File targetDir) {
        targetDir.mkdirs();
        this.targetDir = targetDir;
    }

    private File targetDir;
    private GeoSkillsAccess gs =
            new GeoSkillsAccess("file:///Users/paul/projects/intergeo/ontologies/GeoSkills.owl");// GeoSkillsAccess.geoSkillsDevUrl);
    private String baseURL = "./";

    public void run() {
        try {
            gs.open();

            // for classes: Competency, Topic, EducationalRegion, EducationalPathWay, EducationalProgramme
            // - make a class-page with list instances (of itself and subclasses)
            // - for each instance, list *appropriate* properties

            String[] langs = new String[] {"fr","es","en","de"};


            // first list Educational Programmes
            OWLClass educP = gs.getOntologyClassOfName("EducationalProgram");
            Set<OWLIndividual> educPs = gs.getIndividualsOfClass(educP);
            outputClassWithFlatInstances(educP,educPs,"Class","Educational Programmes","Competencies", langs);

            // create a set of individuals related to individual educational programmes
            // can only do so by going through all of them for now
            OWLProperty belongsToCurriculum = gs.getOntologyPropertyOfName("belongsToCurriculum");
            for(OWLIndividual educationalProgramme: educPs) {
                //OWLIndividual educationalProgramme = gs.getOntologyIndividualOfName("Programme_de_Maths_6eme");
                Set<OWLIndividual> s= new HashSet<OWLIndividual>();
                for(OWLIndividual i : gs.getIndividualsOfClass(gs.getOntologyClassOfName("Competency"))) {
                    Set<OWLIndividual> programmesHere = gs.getPropertyValues(i).get(belongsToCurriculum);
                    if(programmesHere!=null && programmesHere.contains(educationalProgramme)){
                        System.out.println(i);
                        s.add(i);
                    }
                }
                for(OWLIndividual i : gs.getIndividualsOfClass(gs.getOntologyClassOfName("Topic"))) {
                    Set<OWLIndividual> programmesHere = gs.getPropertyValues(i).get(belongsToCurriculum);
                    if(programmesHere!=null && programmesHere.contains(educationalProgramme)){
                        System.out.println(i);
                        s.add(i);
                    }
                }
                outputClassWithFlatInstances(educationalProgramme,s,"Competencies", gs.getCommonName(educationalProgramme,langs,true),"Indiv",langs);
            }


            // create an individual view per competency
            OWLProperty hasTopicProperty = gs.getOntologyPropertyOfName("hasTopic");
            for(OWLIndividual competency: gs.getIndividualsOfClass(gs.getOntologyClassOfName("Competency"))) {
                //OWLIndividual educationalProgramme = gs.getOntologyIndividualOfName("Programme_de_Maths_6eme");
                //Set<OWLIndividual> s= new HashSet<OWLIndividual>();
                Set<OWLIndividual> topics = gs.getPropertyValues(competency).get(hasTopicProperty);
                outputClassWithFlatInstances(competency,topics,"Indiv", gs.getCommonName(competency,langs,true),"Indiv",langs);
            }


        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalStateException(e);
        }

    }


    private String createViewHref(URI uri, String viewType) {
        return baseURL + viewType +"_" + uri.getFragment() + ".html";
    }

    private void outputClassWithFlatInstances(OWLNamedObject namedObject, Set<OWLIndividual> individuals, String viewTitle, String title, String targetViewType, String[] langs) throws Exception {
        String fileName = viewTitle + "_" + namedObject.getURI().getFragment() + ".html";
        System.out.println("Outputting " + fileName);

        Writer out = new OutputStreamWriter(new FileOutputStream(new File(targetDir,fileName)),"utf-8");
        out.write("<html><head><title>"+title+"</title>" +
                "<meta http-equiv=\"Content-Type\" content=\"text/html;charset=utf-8\" /></head><body>" +
                "<h1>"+title+"</h1>");
        out.write("<ul>");

        // first order the instances by name in indicated language
        SortedMap<String,OWLIndividual> tm = new TreeMap<String,OWLIndividual>(String.CASE_INSENSITIVE_ORDER);

        if(individuals!=null) {
            for(OWLIndividual ep : individuals) {
                String name = gs.getCommonName(ep,langs,true);
                tm.put(name,ep);
            }


            for(Map.Entry<String,OWLIndividual> entry: tm.entrySet()) {
                out.write("<li><a href=\"");
                out.write(createViewHref(entry.getValue().getURI(),targetViewType));
                out.write("\">");
                out.write(entry.getKey());
                out.write("</li>");
            }
        }

        out.write("</ul>");
        out.write("</body></html>");

        out.flush();
        out.close();
    }



    public static void main(String[] args) throws Exception {
        File baseDir = new File("target/output");
        baseDir.mkdirs();
        new GeoSkillsHtmlGenerator(baseDir).run();
    }
}
