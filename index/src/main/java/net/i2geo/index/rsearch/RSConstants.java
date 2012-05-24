package net.i2geo.index.rsearch;

import java.util.*;

/**
 */
public interface RSConstants {

    public static final String

            FIELDNM_TOPICS_AND_COMPETENCIES = "trainedTopicsAndCompetencies",

            EDULEVEL_FIELDNM = "eduLevelFine",

            FIELDNM_TEXT = "text",

            FIELDNM_ANCESTORS = "i2geo.ancestorTopics"

    ;


    public static final Set<String> GSNODES_FIELDNMS = Collections.unmodifiableSet(
            new HashSet<String>(Arrays.asList(FIELDNM_TOPICS_AND_COMPETENCIES,EDULEVEL_FIELDNM))
    );
}
