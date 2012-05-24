package net.i2geo.skillstextbox.client;

import net.i2geo.api.SkillItem;
import net.i2geo.api.SKBServiceContext;
import net.i2geo.api.SKBRenderer;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.Timer;

public class SKBSearchField extends Grid implements SKBTarget {

    public SKBSearchField(String functionNameWhenChosen, 
            String authorizedTypes,
            SKBServiceContext service, SKBConsole console, boolean debug) {
        super(1,2);
        oracle = new SKBOracle(service,service.basePath,
                authorizedTypes.split(",| "),console);
        oracle.setAlsoPlainText(true);
        WaitingSign waitingSign = new WaitingSign(service);
        box = new SuggestBox(oracle);
        oracle.setSkbTarget(this,box,waitingSign);
        super.setWidget(0,0,box);
        super.setWidget(0,1,waitingSign);
        super.setTitle(service.getI18n().tooltipToExplainSKBsearchField());
        super.setSize("25em","2em");
        this.console = console;
        this.renderer = new SKBRenderer(service);
        this.functionNameWhenChosen = functionNameWhenChosen;
        box.addFocusListener(new HereFocusListener());
        box.setStyleName("SKB_searchBox_outFocus");
        originalText = service.getI18n().searchFieldLabelInnerText();
        box.setText(originalText);
    }
    
    private final String functionNameWhenChosen;
    private String originalText;
    private final SKBOracle oracle;
    private final SKBConsole console;
    private final SuggestBox box;
    private final SKBRenderer renderer;
    

    private void log(String msg) {
        if(console!=null) console.log(msg);
    }

    String getText() {
        return box.getText();
    }

    private class HereFocusListener implements FocusListener {
        public void onFocus(Widget widget) {
            if(widget!=box) return;
            if(originalText!=null && originalText.equals(box.getText())) {
                box.setText(""); originalText = null;
            }
            box.setStyleName("SKB_searchBox_inFocus");
        }

        public void onLostFocus(Widget widget) {
            if(widget!=box) return;
            box.setStyleName("SKB_searchBox_outFocus");
        }
    }

    public void choiceIdentified(final SkillItem item) {
        if(item==null) return;
        log("Choice identified: " + item);
        super.clear();
        String h = renderer.render(item);
        if(h.endsWith("<br/>")) h = h.substring(0,h.length()-5);
        setHTML(0,1,(h + "<img src='/SearchI2G/net.i2geo.skillstextbox.SkillsTextBox/Spinning_wheel_throbber.gif'/>"));
        Timer t = new Timer() { public void run() {
            log("Activating submission.");
            String uri= item.getUri();
            if(!uri.startsWith("#") && !uri.startsWith("http://")) uri = '#' + uri;
            doNativeCallChoiceIdentified(functionNameWhenChosen,uri,item.getType());
        }};
        t.schedule(50);
    }

    private native void doNativeCallChoiceIdentified(String functionName, String uri, String type)/*-{
        var w=window.top;
        if(w[functionName]) {
            w[functionName](uri,type); // e.g. itemChosen("Circle_r")
        } else {
            alert("No such function \"" + functionName + "\".");
        }
    }-*/;

    public void insertWordSet(String wordSet) {
        doNativeCallChoiceIdentified(functionNameWhenChosen,wordSet,"text");
    }

    public Object requestAndDeleteLastInput() {
        return null;
    }

    public void insertNode(SkillItem nodeId) {
        choiceIdentified(nodeId);
    }
    
} // class SKBSearchField
