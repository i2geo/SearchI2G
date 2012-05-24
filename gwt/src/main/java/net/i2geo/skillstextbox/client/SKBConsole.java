package net.i2geo.skillstextbox.client;

import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.Window;
import com.google.gwt.core.client.GWT;

/** Simple panel to act as a console
 */
public class SKBConsole extends VerticalPanel {

    boolean isEnvironmentHostedMode = false;
    boolean isEnvironmentFireBugEnabled = false;
    boolean isEnvironmentBrowserOnly = false;

    private boolean doLog = true;


    /** @deprecated don't use this one... make the doLog flag explicit. */
    public SKBConsole() {
        this(true);
    }
    public SKBConsole(boolean doLog) {
        super();
        this.doLog = doLog;
        checkEnvironment();
        //super.setWidth("80%");
        //super.setHeight("100px");
        super.setStyleName("console-with-frame");
        if(isEnvironmentBrowserOnly && doLog) {
            super.add(new HTML("<h4>Console</h4>"));
        }
    }


    public boolean needsSpace() {
        return isEnvironmentBrowserOnly;
    }

    private void checkEnvironment() {
        if(!GWT.isScript()) {
            // hosted mode
            isEnvironmentHostedMode = true;
            isEnvironmentFireBugEnabled = false;
            isEnvironmentBrowserOnly = false;
        } else if(windowHasConsole()) {
            isEnvironmentFireBugEnabled = true;
            isEnvironmentHostedMode = false;
            isEnvironmentBrowserOnly = false;
        } else {
            isEnvironmentFireBugEnabled = false;
            isEnvironmentHostedMode = false;
            isEnvironmentBrowserOnly = true;
        }
    }

    private native boolean windowHasConsole() /*-{
        return !!(window.top.console);
    }-*/;


    private boolean hasBeenAttached = false;

    protected void onAttach() {
        super.onAttach();
        hasBeenAttached = true;
    }

    public void log(String msg) {
        if(!doLog) return;
        if(isEnvironmentBrowserOnly) {
            try {
                if(hasBeenAttached)
                    super.insert(new HTML("<p>"+msg+"</p>"),0); // <![CDATA[xxx]]>
            } catch(Exception ex) {
                Window.alert(msg + "(" + ex + ")");
            }
        } else if (isEnvironmentFireBugEnabled) {
            consoleObjLog(msg);
        } else if (isEnvironmentHostedMode) {
            GWT.log(msg,null);
        }
    }
    private native void consoleObjLog(String msg) /*-{
        window.top.console.log(msg);
    }-*/;

}
