package net.i2geo.api;

import java.util.*;

/** An enumeration of possible ont-types used both on the client and on the server;
 * ont-types are basic class of the ontology according to 
 */
public class OntType {

    public static final OntType
            COMPETENCY = new OntType("competency"),
            TOPIC = new OntType("topic"),
            LEVEL = new OntType("level"),
            COMPETENCYPROCESS = new OntType("competencyProcess"),
            CONCRETE_TOPIC = new OntType("concreteTopic"),
            ABSTRACTTOPIC = new OntType("abstractTopic"),
            PURE_ABSTRACT_TOPIC = new OntType("pureAbstractTopic"),
            ABSTRACTTOPIC_WITH_REPRESENTATIVE = new OntType("abstractTopicWithRepresentative")

    ;

    private OntType(String name) {this.name = name; }
    private final String name;
    public String getName() { return name; }

    public static final Set<String> topicTypeNames = (Set<String>)
            Collections.unmodifiableSet(new HashSet((Collection<String>) Arrays.asList(
                    TOPIC.getName(), CONCRETE_TOPIC.getName(),
                    ABSTRACTTOPIC.getName(),
                    ABSTRACTTOPIC_WITH_REPRESENTATIVE.getName(),
                    PURE_ABSTRACT_TOPIC.getName())));

    public static final Set<String> competencyTypeNames = (Set<String>)
            Collections.unmodifiableSet(new HashSet((Collection<String>) Arrays.asList(
                    COMPETENCY.getName(), COMPETENCYPROCESS.getName())));


    public static final Set<String> topicOrCompetencyTypeNames = (Set<String>)
            Collections.unmodifiableSet(new HashSet((Collection<String>) Arrays.asList(
                    COMPETENCY.getName(), COMPETENCYPROCESS.getName(),
                    TOPIC.getName(), CONCRETE_TOPIC.getName(),
                    ABSTRACTTOPIC.getName(),
                    ABSTRACTTOPIC_WITH_REPRESENTATIVE.getName(),
                    PURE_ABSTRACT_TOPIC.getName())));

    public String toString() { return getName();}
}
