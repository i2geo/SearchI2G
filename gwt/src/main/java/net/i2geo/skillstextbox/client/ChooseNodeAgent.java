package net.i2geo.skillstextbox.client;

import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.FocusListener;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.Window;
import com.google.gwt.core.client.GWT;

/** tiny class to choose node
 */
public class ChooseNodeAgent {

    public ChooseNodeAgent(SuggestBox box, SKBOracle oracle) {
        box.addFocusListener(new HereFocusListener());
        this.oracle = oracle;
        try {
            declareChooseNode();
        } catch(Exception ex) {
            // nothing thus far
        }
    }

    private final SKBOracle oracle;


    private class HereFocusListener implements FocusListener {
        public void onFocus(Widget sender) {
            declareChooseNode();
        }

        public void onLostFocus(Widget sender) {}
    }

    public static SKBOracle currentOracle = null;

    public void declareChooseNode() {
        currentOracle = oracle;
        declareChooseNodeNative();
    }

    private native void declareChooseNodeNative() /*-{
        var w = $wnd;
        while(w) {
            w.chooseNode = function chooseNode(uri) {
                if(window.console) window.console.log("Choosing \"" + uri + "\".");
                @net.i2geo.skillstextbox.client.ChooseNodeAgent::chooseNode(Ljava/lang/String;)(uri);
                if(window.console) window.console.log("Have chosen \"" + uri + "\".");
            };
            var previous = w;
            w = w.opener;
            if(w = previous) break;
        }
    }-*/;


    private void unDeclareChooseNode() {
        currentOracle = null;
        unDeclareChooseNodeNative();
    }

    public native void unDeclareChooseNodeNative() /*-{
        $wnd.chooseNode = null;
    }-*/;



    public static void chooseNode(String uri) {
        if(currentOracle==null) {
            Window.alert("Can't choose node... no query-field or annotation-field to send it to.");
        } else {
            currentOracle.chooseNode(uri);
        }
    }
}
