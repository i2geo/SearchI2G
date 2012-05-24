package net.i2geo.api;

import net.i2geo.api.SKBi18n;

/** A class to use the javascript big object i18n  which associates key-names to phrases which are pulled from
 * the phrases. Typically this is loaded by /static/JStrans-xx.js (where xx is a language).*/
public class SKBi18nThroughJStrans implements SKBi18n {

    @Override
    public String getString(String key) {
        return getMsgFromJStrans(key);
    }

    public String types_labels_competency() {
        return getMsgFromJStrans("types_labels_competency");
    }

    public String types_labels_topic() {
        return getMsgFromJStrans("types_labels_topic");
    }

    public String types_labels_level() {
        return getMsgFromJStrans("types_labels_level");
    }

    public String types_labels_competencyProcess() {
        return getMsgFromJStrans("types_labels_competencyProcess");
    }

    public String types_labels_abstractTopic() {
        return getMsgFromJStrans("types_labels_abstractTopic");
    }

    public String types_labels_abstractTopicWithRepresentative() {
        return getMsgFromJStrans("types_labels_abstractTopicWithRepresentative");
    }

    public String types_labels_pureAbstractTopic() {
        return getMsgFromJStrans("types_labels_pureAbstractTopic");
    }
    public String types_labels_concreteTopic() {
        return getMsgFromJStrans("types_labels_concreteTopic");
    }

    public String types_icons_competency() {
        return getMsgFromJStrans("types_icons_competency");
    }

    public String types_icons_topic() {
        return getMsgFromJStrans("types_icons_topic");
    }

    public String types_icons_level() {
        return getMsgFromJStrans("types_icons_level");
    }

    public String types_icons_competencyProcess() {
        return getMsgFromJStrans("types_icons_competencyProcess");
    }

    public String types_icons_abstractTopic() {return getMsgFromJStrans("types_icons_abstractTopic");}
    public String types_icons_abstractTopicWithRepresentative() {return getMsgFromJStrans("types_icons_abstractTopicWithRepresentative");}
    public String types_icons_pureAbstractTopic() {return getMsgFromJStrans("types_icons_pureAbstractTopic");}
    public String types_icons_concreteTopic() {return getMsgFromJStrans("types_icons_concreteTopic");}

    public String buttonsErase() {
        return getMsgFromJStrans("buttonsErase");
    }

    public String tooltipClickToSeeMore() {
        return getMsgFromJStrans("tooltipClickToSeeMore");
    }

    public String tooltipToExplainSKBqueryField() {
        return getMsgFromJStrans("tooltipToExplainSKBqueryField");
    }

    public String tooltipToExplainSKBsearchField() {
        return getMsgFromJStrans("tooltipToExplainSKBsearchField");
    }

    public String emptyListLabel() {
        return getMsgFromJStrans("emptyListLabel");
    }

    public String addSuggestionInvitation() {
        return getMsgFromJStrans("addSuggestionInvitation");
    }

    public String types_labels_text() {
        return getMsgFromJStrans("types_labels_text");
    }

    public String types_icons_text() {
        return getMsgFromJStrans("types_icons_text");
    }

    public String tooltipShortDesc() {
        return getMsgFromJStrans("tooltipShortDesc");
    }

    public String editorTitle(){
        return getMsgFromJStrans("editorTitle");
    }

    public String addSuggestionBody() {
        return getMsgFromJStrans("addSuggestionBody");
    }

    public String label_curriculumTextsLink() {
        return getMsgFromJStrans("label_curriculumTextsLink");
    }

    public String searchFieldLabelInnerText() {
        return getMsgFromJStrans("searchFieldLabelInnerText");
    }

    public String searchWithTextLabel() {
        return getMsgFromJStrans("searchWithTextLabel");
    }

    public String searchWithTextTooltip() {
        return getMsgFromJStrans("searchWithTextTooltip");
    }

    public String waitingWheelToolTip() {
        return getMsgFromJStrans("waitingWheelToolTip");
    }

    public String msieWarning() {
        return getMsgFromJStrans("msieWarning");
    }

    // ============== the core part ======================
    private native String getMsgFromJStransNative(String key) /*-{
        if(typeof($wnd.i18nDict)=="undefined")
            return "undefined-i18n";
        else return $wnd.i18nDict[key];
    }-*/;

    private String getMsgFromJStrans(String key) {
        if(key==null) return "-null-";
        String msg = getMsgFromJStransNative(key);
        if(msg == null) msg = key;
        return msg;
    }
}
