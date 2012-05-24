package net.i2geo.skillstextbox.client;

import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.Window;
import com.google.gwt.http.client.URL;
import net.i2geo.api.SKBServiceContext;

/**
 */
public class SKBSuggestButton extends Button implements ChangeListener,ClickListener {

    public SKBSuggestButton(SuggestBox box, SKBServiceContext serviceContext) {
        super("+");
        super.setStyleName("SKBSuggestButton");
        this.box = box;
        box.addChangeListener(this);
        labelBase = serviceContext.getI18n().addSuggestionInvitation();
        suggestionBody = serviceContext.getI18n().addSuggestionBody();
        super.addClickListener(this);
    }
    private final SuggestBox box;
    private final String labelBase;
    private final String suggestionBody;

    public void onChange(Widget sender) {
        if(sender!=box) return;
        super.setTitle(labelBase +' '+ box.getText());
    }

    public void onClick(Widget widget) {
        //if(widget!=this) return;
        String titleEscaped = URL.encode(box.getText());
        String suggestBodyEscaped = URL.encode(suggestionBody);
        Window.open("http://i2geo.net/xwiki/bin/view/Suggest/CreateSuggestion?title="
                +titleEscaped + "&content=" + suggestBodyEscaped,
                "gsSuggest","width=640,height=480,scrollbars=yes,status=no,toolbar=no,menubar=no,location=no,resizable=yes");
    }
}
