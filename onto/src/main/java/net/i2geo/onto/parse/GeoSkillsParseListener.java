package net.i2geo.onto.parse;

import java.util.Iterator;
import java.util.Set;
import java.util.Date;

/** SAX-inspired API to listen to the ontology being parsed.
 * Implementing classes are given to the {@link net.i2geo.onto.tasks.GeoSkillsParser}
 * which then call the methods of this interface to build
 * a representation of GeoSkills.
 * <p>
 * The parsing starts with a set of calls to {@link #thereIsItem}
 * then each item is described in more details using
 */
public interface GeoSkillsParseListener {

    public void thereIsItem(String uri, NodeType type);

    public void topicType(String uri, Set<String> parentTypeURIs,NamesMap names);

    public void topicDescription(String uri, Set<String> parentTypeURIs, NamesMap names, ModificationData md);

    public void competencyType(String uri, Set<String> parentTypeURIs,NamesMap names);

    public void competencyDescription(String uri, Set<String> parentTypeURIs, Set<String> hasTopicsURIs, NamesMap names, ModificationData md);


    // TODO: educational levels?

    public static interface ModificationData {
        public String getModificationUserName();
        public String getCreationUserName();
        public Date getModificationDate();
        public Date getCreationDate();
    }


    public static interface NamesMap {
        public Iterator<String> getLanguages();
        public Iterator<NameWithFrequency> getNames(String language);

    }
    public static interface NameWithFrequency {
        public float getFrequency();
        public String getName();
    }

    public static class NodeType {
        public NodeType(String name) {this.name = name;}
        final String name;
        public String toString() {return name;}
    }
    public static final NodeType
            TYPE_COMPETENCY_CLASS = new NodeType("competency-class-type"),
            TYPE_COMPETENCY       = new NodeType("competency-type"),
            TYPE_TOPIC_CLASS      = new NodeType("topic-class-type"),
            TYPE_TOPIC            = new NodeType("topic-type");
}
