package net.i2geo.index.relatives;

import net.i2geo.onto.GeoSkillsAccess;
import org.semanticweb.owl.model.OWLClass;
import org.semanticweb.owl.model.OWLIndividual;
import org.semanticweb.owl.model.OWLProperty;

import java.io.IOException;
import java.net.URL;

/** Utility class to access the members of the GeoSkillsRelatives.owl ontology, extending that of GeoSkills.
 */
class GSRelativesAccess extends GeoSkillsAccess {

    OWLProperty definitionTextProperty;
    OWLProperty languageProperty;
    OWLProperty modificationDateProperty;
    OWLProperty definitionTextContentProperty;
    OWLProperty definitionURLProperty;
    OWLClass manualDefinitionClass;
    OWLClass automaticDefinitionClass;

    GSRelativesAccess(URL relativesURL) throws IOException {
        super(new URL(relativesURL, "GeoSkillsRelatives.owl"));
    }




    public void removeObjectPropertiesAndObjects(OWLIndividual node, OWLProperty definitionTextProperty) {
        //TODO: implement
    }

    public OWLIndividual createIndividual(String uri, OWLClass automaticDefinitionClass) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    public void assertI18nStringProperty(OWLIndividual node, OWLProperty prop, String language, String value) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    public void assertStringProperty(OWLIndividual node, OWLProperty property, String value) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }
}
