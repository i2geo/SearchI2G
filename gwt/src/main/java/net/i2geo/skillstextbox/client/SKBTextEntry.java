package net.i2geo.skillstextbox.client;

import com.google.gwt.user.client.ui.HTML;
import net.i2geo.api.SKBRenderer;

public class SKBTextEntry extends HTML {

    String text;

    public SKBTextEntry(String text, SKBList list) {
        super(text);
        this.text=text;
    }
}
