package net.i2geo.api;

import com.google.gwt.i18n.client.ConstantsWithLookup;

/** access to values of properties
 */
public interface SKBi18n {
    
    public String getString(String key);

    public String types_labels_competency       ();
    public String types_labels_topic            ();
    public String types_labels_level            ();
    public String types_labels_competencyProcess();
    public String types_labels_pureAbstractTopic();
    public String types_labels_abstractTopicWithRepresentative();
    public String types_labels_abstractTopic();
    public String types_labels_text();

    public String types_icons_competency        ();
    public String types_icons_topic             ();
    public String types_icons_level             ();
    public String types_icons_competencyProcess ();
    public String types_icons_abstractTopic();
    public String types_icons_pureAbstractTopic();
    public String types_icons_abstractTopicWithRepresentative();
    public String types_icons_text();

    public String buttonsErase();

    public String tooltipClickToSeeMore();
    public String tooltipShortDesc();

    public String tooltipToExplainSKBqueryField();
    public String tooltipToExplainSKBsearchField();

    public String emptyListLabel();

    public String editorTitle();

    public String addSuggestionInvitation();
    public String addSuggestionBody();
    public String label_curriculumTextsLink();

    public String searchFieldLabelInnerText();

    public String searchWithTextLabel();
    public String searchWithTextTooltip();

    public String waitingWheelToolTip();

    String msieWarning();
}
