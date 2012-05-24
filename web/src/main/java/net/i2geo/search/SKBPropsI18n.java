package net.i2geo.search;

import net.i2geo.api.SKBi18n;

import java.util.Properties;
import java.util.Map;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.IOException;
import java.io.InputStream;

/** Implementation of SKBi18n based on properties file loaded from class-loader, meant
 * for server storage.
 */
public class SKBPropsI18n implements SKBi18n {

    public SKBPropsI18n(String lang) throws IOException {
        // first load basic
        Reader in = new InputStreamReader(SKBi18n.class.getResourceAsStream("SKBi18nPhrases.properties"),"utf-8");
        props.load(in);
        InputStream inStream = SKBi18n.class.getResourceAsStream("SKBi18nPhrases_"+ lang +".properties");
        if(inStream!=null)
            props.load(new InputStreamReader(inStream,"utf-8"));
    }

    Properties props = new Properties();

    public String types_labels_competency() {
        return props.getProperty("types_labels_competency");
    }

    public String types_labels_topic() {
        return props.getProperty("types_labels_topic");
    }

    public String types_labels_level() {
        return props.getProperty("types_labels_level");
    }

    public String types_labels_competencyProcess() {
        return props.getProperty("types_labels_competencyProcess");
    }

    public String types_labels_abstractTopic() {
        return props.getProperty("types_labels_abstractTopic");
    }

    public String types_labels_abstractTopicWithRepresentative() {
        return props.getProperty("types_labels_abstractTopicWithRepresentative");
    }

    public String types_labels_pureAbstractTopic() {
        return props.getProperty("types_labels_pureAbstractTopic");
    }
    public String types_labels_concreteTopic() {
        return props.getProperty("types_labels_concreteTopic");
    }

    public String types_icons_competency() {
        return props.getProperty("types_icons_competency");
    }

    public String types_icons_topic() {
        return props.getProperty("types_icons_topic");
    }

    public String types_icons_level() {
        return props.getProperty("types_icons_level");
    }

    public String types_icons_competencyProcess() {
        return props.getProperty("types_icons_competencyProcess");
    }

    public String types_icons_abstractTopic() {return props.getProperty("types_icons_abstractTopic");}
    public String types_icons_abstractTopicWithRepresentative() {return props.getProperty("types_icons_abstractTopicWithRepresentative");}
    public String types_icons_pureAbstractTopic() {return props.getProperty("types_icons_pureAbstractTopic");}
    public String types_icons_concreteTopic() {return props.getProperty("types_icons_concreteTopic");}

    public String buttonsErase() {
        return props.getProperty("buttonsErase");
    }

    public String tooltipClickToSeeMore() {
        return props.getProperty("tooltipClickToSeeMore");
    }

    public String tooltipToExplainSKBqueryField() {
        return props.getProperty("tooltipToExplainSKBqueryField");
    }

    public String tooltipToExplainSKBsearchField() {
        return props.getProperty("tooltipToExplainSKBsearchField");
    }

    public String emptyListLabel() {
        return props.getProperty("emptyListLabel");
    }

    public String addSuggestionInvitation() {
        return props.getProperty("addSuggestionInvitation");
    }

    public boolean getBoolean(String methodName) {
        String v = props.getProperty(methodName);
        if(v==null) throw new IllegalArgumentException();
        if("true".equalsIgnoreCase(v.trim())
                || "yes".equalsIgnoreCase(v.trim()))
            return true;
        if("false".equalsIgnoreCase(v.trim())
                || "no".equalsIgnoreCase(v.trim()))
            return true;
        throw new IllegalArgumentException();
    }

    public double getDouble(String methodName) {
        String v = props.getProperty(methodName);
        return Double.parseDouble(v);
    }

    public float getFloat(String methodName) {
        String v = props.getProperty(methodName);
        return Float.parseFloat(v);
    }

    public int getInt(String methodName) {
        String v = props.getProperty(methodName);
        return Integer.parseInt(v);
    }

    public Map getMap(String methodName) {
        throw new IllegalArgumentException("No map implemented.");
    }

    public String getString(String methodName) {
        String v = props.getProperty(methodName);
        return v;
    }

    public String[] getStringArray(String methodName) {
        throw new IllegalArgumentException("No map implemented.");
    }

    public String types_labels_text() {
        return props.getProperty("types_labels_text");
    }

    public String types_icons_text() {
        return props.getProperty("types_icons_text");
    }

    public String tooltipShortDesc() {
        return props.getProperty("tooltipShortDesc");
    }

    public String editorTitle(){
        return props.getProperty("editorTitle");
    }

    public String addSuggestionBody() {
        return props.getProperty("addSuggestionBody");
    }

    public String label_curriculumTextsLink() {
        return props.getProperty("label_curriculumTextsLink");
    }

    public String searchFieldLabelInnerText() {
        return props.getProperty("searchFieldLabelInnerText");
    }

    public String searchWithTextLabel() {
        return props.getProperty("searchWithTextLabel");
    }

    public String searchWithTextTooltip() {
        return props.getProperty("searchWithTextTooltip");
    }

    public String waitingWheelToolTip() {
        return props.getProperty("waitingWheelToolTip");
    }

    public String msieWarning() {
        return props.getProperty("msieWarning");
    }
}
