package net.i2geo.onto;

import net.i2geo.onto.tasks.GeoSkillsParser;
import net.i2geo.onto.parse.GeoSkillsParseListener;

import java.util.Set;
import java.util.Iterator;
import java.util.HashSet;

import org.apache.log4j.Logger;
import org.apache.log4j.Level;
import junit.framework.AssertionFailedError;

/** Simple stub that goes to system out.
 */
public class TestParseListening extends junit.framework.TestCase  {

    public TestParseListening(String name) { super(name); }

    private class ParseListeningSysout implements GeoSkillsParseListener {
        public void thereIsItem(String uri, NodeType type) {
            System.out.println("There is item: " + GeoSkillsUtil.shortenName(uri,ontBaseU) + " of type " + type + ".");
        }

        public void topicType(String uri, Set<String> parentTypeURIs, NamesMap names) {
            System.out.println("Topic type: " + GeoSkillsUtil.shortenName(uri,ontBaseU) + " of types " + GeoSkillsUtil.shortenName(parentTypeURIs,ontBaseU) + ": ");
            outputNames(names);
            assertNoDuplicateNames(names,uri);

            assertNoDuplicateURIs(uri);
        }

        public void topicDescription(String uri, Set<String> parentTypeURIs, NamesMap names, ModificationData mData) {
            String s = GeoSkillsUtil.shortenName(uri,ontBaseU);
            if("#Topic_r".equals(s)) baseTopicIsHere = true;
            System.out.println("Topic : " +  s + " of types " + GeoSkillsUtil.shortenName(parentTypeURIs,ontBaseU) + ": ");
            outputNames(names);
            assertNoDuplicateNames(names,uri);
            assertNoDuplicateURIs(uri);
            if(mData!=null) {
                if(mData.getModificationDate()!=null) {
                    System.out.println("Modification date: " + mData.getModificationDate());
                    foundModificationDate = true;
                    assertEquals("Modification date should be 12-12-08",11,mData.getModificationDate().getMonth());
                }
            }
        }

        public void competencyType(String uri, Set<String> parentTypeURIs, NamesMap names) {
            String s = GeoSkillsUtil.shortenName(uri,ontBaseU);
            if("#Competency".equals(s)) baseCompetencyProcessIsHere = true;
            System.out.println("Competency type: " + s + " of types " + GeoSkillsUtil.shortenName(parentTypeURIs,ontBaseU)+ ": ");
            outputNames(names);
            assertNoDuplicateNames(names,uri);

            assertNoDuplicateURIs(uri);
        }


        public void competencyDescription(String uri, Set<String> parentTypeURIs, Set<String> hasTopicsURIs, NamesMap names, ModificationData mData) {
            String s = GeoSkillsUtil.shortenName(uri,ontBaseU);
            System.out.println("Competency: " + s + " of types " + GeoSkillsUtil.shortenName(parentTypeURIs,ontBaseU) + ": ");
            if("#grouin".equals(s)) grouinIsHere = true;
            outputNames(names);
            System.out.println("Topics: " + GeoSkillsUtil.shortenName(hasTopicsURIs,ontBaseU));
            assertNoDuplicateNames(names,uri);

            assertNoDuplicateURIs(uri);
        }

    }

    private static void assertNoDuplicateNames(GeoSkillsParseListener.NamesMap map, String uri) {
        Set set = new HashSet();
        for(Iterator<String> it=map.getLanguages(); it.hasNext(); ) {
            String lang = it.next();
            set.clear();
            for(Iterator<GeoSkillsParseListener.NameWithFrequency> namesIt=map.getNames(lang); namesIt.hasNext();) {
                GeoSkillsParseListener.NameWithFrequency nwf = namesIt.next();
                String name = nwf.getName();
                if(set.contains(name)) throw new AssertionFailedError("Name \"" + name + "\" is duplicate in " + uri + ".");
            }
        }
    }

    Set<String> uris = new HashSet<String>();
    private void assertNoDuplicateURIs(String uri) {
        if(uris.contains(uri)) throw new AssertionFailedError("Uri " + uri + " is duplicate.");
        uris.add(uri);
    }

    private static void outputNames(GeoSkillsParseListener.NamesMap map) {
        for(Iterator<String> it = map.getLanguages(); it.hasNext(); ) {
            String lang = it.next();
            for(Iterator<GeoSkillsParseListener.NameWithFrequency> i=map.getNames(lang); i.hasNext(); ) {
                System.out.print(lang + " : ");
                GeoSkillsParseListener.NameWithFrequency nf = i.next();
                System.out.print(nf.getFrequency());
                System.out.print(" : ");
                System.out.println(nf.getName());
            }
        }
    }

    private boolean grouinIsHere = false, baseTopicIsHere= false, baseCompetencyProcessIsHere = false, foundModificationDate  = false;

    public void testOutputGeoSkills() throws Throwable {
        try {
            Level level = Logger.getLogger("org.mindswap.pellet.ABox").getLevel();
            Logger.getLogger("org.mindswap.pellet.ABox").setLevel(Level.ERROR);
            new GeoSkillsParser(GeoSkillsAccess.getTestOntologyUrl().toExternalForm()).runParser(new ParseListeningSysout());
            Logger.getLogger("org.mindswap.pellet.ABox").setLevel(level);

            assertTrue("There should be a comeptency grouin",grouinIsHere);
            assertTrue("There should be the base competency class",baseCompetencyProcessIsHere);
            assertTrue("There should be the base topic representative",baseTopicIsHere);
            assertTrue("There should be a modification date.", foundModificationDate);
        } catch(Exception ex) {
            ex.printStackTrace();
            throw ex;
        }
    }


    //private static GeoSkillsAccess access = new GeoSkillsAccess("file:///Users/paul/projects/intergeo/ontologies/GeoSkills.owl");
    private String ontBaseU = GeoSkillsAccess.getInstance().getBaseURI();
    private int ontBaseUL = ontBaseU.length();

}
