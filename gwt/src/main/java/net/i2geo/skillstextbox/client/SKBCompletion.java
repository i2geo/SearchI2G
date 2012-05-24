package net.i2geo.skillstextbox.client;

import com.google.gwt.user.client.ui.SuggestOracle;
import net.i2geo.api.SKBServiceContext;
import net.i2geo.api.SkillItem;

/**
 */
public class SKBCompletion implements SuggestOracle.Suggestion {
    public SKBCompletion(SKBServiceContext service, String display, SkillItem item) {
        if(item!=null) {
            this.display = display;
            this.replacement = item.getShortDescription();
            this.uri = item.getUri();
            this.item = item;
        } else {
            this.display = "<table class=\"SKBItemSuggest\" cellspacing=\"0\" width=\"95%\">\n" +
                    "  <tbody> <tr> <td style=\"width: 2em;\" rowspan=\"2\"><img src=\""+ service.basePath+"/waiting-blank.gif\" style=\"width: 2em;\"></td> \n" +
                    "  <td style=\"width: 95%;\" title=\""+service.getI18n().searchWithTextTooltip()+"\"\n" +
                    "         >"+service.getI18n().searchWithTextLabel()+": <i>" + display + "</i></td><td></td>\n" +
                    "      </tr><tr> <td style=\"font-size: smaller;\">&nbsp;</td><td title=\""+service.getI18n().searchWithTextTooltip()+"\"><img src=\""+ service.basePath+"/waiting-blank.gif\" border=\"0\"></td></tr></tbody></table>";
            this.replacement = display;
            this.uri = null;
            this.item = null;
        }
    }
    private final String display, replacement, uri;
    private final SkillItem item;

    public String getDisplayString() {
        return display;
    }

    public String getReplacementString() {
        return replacement;
    }

    public String getUri() {
        return uri;
    }

    public SkillItem getItem() {
        return item;
    }
}

