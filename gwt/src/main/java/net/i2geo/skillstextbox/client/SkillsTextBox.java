package net.i2geo.skillstextbox.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.http.client.UrlBuilder;
import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.*;

import java.util.Date;
import java.util.Iterator;
import java.util.Arrays;

import net.i2geo.api.*;

//imports needed for rpc services

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class SkillsTextBox implements EntryPoint {

    //SkillsSearchServiceAsync service = null;
    SKBServiceContext serviceContext = new SKBServiceContext();
    private SKBConsole console = null;
    private SkillsTextBox myself;
    private String basePath = null;
    private boolean shouldCallJSlisteners = true;
    private String acceptedLangs = null;
    private String functionNameForEditorClosing = null;

    public SkillsTextBox(String basePath) {
        this.basePath = basePath;
    }
    public SkillsTextBox() {
        this.basePath = "./"; // the document's URL
    }

    public void onModuleLoad() {
        GWT.setUncaughtExceptionHandler(new GWT.UncaughtExceptionHandler() {
            boolean haveWarned = false;
            public void onUncaughtException(final Throwable caught) {
                caught.printStackTrace();
                if(!haveWarned) {
                    haveWarned = true;
                    Window.alert("Caught:" + caught + "\nmessage[" + caught.getMessage() + "]");
                }
            }
        });
        this.myself = this;
        //console = new SKBConsole(false);
        readConfig();
        if(acceptedLangs==null || acceptedLangs.length()==0 || "undefined".equals(acceptedLangs)) {
            acceptedLangs = "en";
        }
        console = new SKBConsole(true);
        log("acceptedLangs = " + acceptedLangs);

        serviceContext.service = (SkillsSearchServiceAsync) GWT.create(SkillsSearchService.class);
        ((ServiceDefTarget) serviceContext.service).
                setServiceEntryPoint(basePath + "../search");
        serviceContext.basePath = basePath;
        if(basePath==null || basePath.length()<=2) {
            basePath = GWT.getModuleBaseURL();
            int p = basePath.indexOf('/',9);
            if(p!=-1) basePath = basePath.substring(0,p);
            basePath = basePath + "/SearchI2G/";
        }
        log("BasePath: " + basePath);
        serviceContext.acceptedLangs = acceptedLangs;
        serviceContext.chooseLanguage();
        log("Configuring i18n");
        serviceContext.getI18n();
        //alertIfMSIE();
        log("About to declare plugs.");
        declarePlugs();
    }// end of onModuleLoad method

    public void onModuleDetach() {
        
    }

    public String getBasePath() {
        return basePath;
    }

    public void skbEditOnDiv(String command) {
        this.loadOnDiv(command);
    }

    public void loadOnDiv(String command) {
        if(command==null || command.length()==0 || "undefined".equals(command)) return;
        String[] semiColonSepped = command.split(";");

        log("inspecting command " + command);
        for(int i=0; i<semiColonSepped.length; i++) {
            if(semiColonSepped[i]==null || semiColonSepped[i].length()==0
                    || "undefined".equals(semiColonSepped[i])) continue;
            String[] s = semiColonSepped[i].split("\\|");
            log("Will load div with command " + command);
            if(s==null || s.length!=4) {
                log("loadOnDiv needs to be made of a sequence of four arguments separated by \"|\", had \"" + command + "\" interpreted as "+ (s==null?"null":""+Arrays.asList(s)) +" and couldn't parse it.");
                Window.alert("loadOnDiv needs to be made of a sequence four arguments separated by \"|\", had \"" + command + "\" interpreted as "+ (s==null?"null":""+Arrays.asList(s)) +" and couldn't parse it.");
                return;
            }
            loadOnDiv(s[0],s[1],s[2],s[3]);
        }
    }

    public void loadOnDiv(String divId, String idOfStorageField, String authorizedTypes, String doDebug) {
        final RootPanel rootPanel = RootPanel.get(divId);

        deleteAllWidgets(rootPanel);
        boolean isDebug = doDebug!=null && "true".equalsIgnoreCase(doDebug);
        if(isDebug) console = new SKBConsole(true);
        log("console initted.");
        log("loading on div \"" + divId + "\".");
        //isDebug = isDebug || SKBConsole.isEnvironmentFireBugEnabled
        //        || SKBConsole.isEnvironmentHostedMode;

        final SKBOracle oracle = new SKBOracle(serviceContext,basePath,authorizedTypes.split(","),console);
        final SuggestBox autoCompleteTextBox = new SuggestBox(oracle);
        final SKBList queryList = new SKBList(serviceContext);
        final WaitingSign waitingSign = new WaitingSign(serviceContext);

        oracle.setSkbTarget(queryList,autoCompleteTextBox,waitingSign);
        log("query list initted");
        autoCompleteTextBox.setWidth("400px"); // 385px
        log("autoCompleteTextBox initted.");

        SKBSuggestButton suggestButton = new SKBSuggestButton(autoCompleteTextBox,serviceContext);
        suggestButton.setSize("20px","130%");


        queryList.setSize("400px","8em");
        if(isDebug) queryList.setConsole(console);
        queryList.setIdFieldStorage(idOfStorageField);
        queryList.init();
        if(shouldCallJSlisteners)
            queryList.addListListener(new JSListListner(queryList));

        rootPanel.add(queryList);
        if(isDebug && console.needsSpace())
            console.setSize("200px","200px");
        HorizontalPanel hop = new HorizontalPanel();
        hop.add(autoCompleteTextBox);
        hop.add(suggestButton);
        hop.add(waitingSign);
        rootPanel.add(hop);
        if(!"level".equalsIgnoreCase(authorizedTypes.trim()))
            rootPanel.add(new HTML("<p style=\"width:99%\" align=\"right\"><a target='curriculum-browsing' \n" +
                    "   href='/xwiki/bin/view/Main/CurriculumTexts?xpage=popup' \n" +
                    "   onclick='window.open(\"/xwiki/bin/view/Main/CurriculumTexts?xpage=popup\",\"curriculum-browsing\",\"width=300,height=500,screenX=20,scrollbars=yes,status=no,toolbar=no,menubar" +
                    "=no,location=no,resizable=yes\"); return false;'\n" +
                    "  >"+serviceContext.getI18n().label_curriculumTextsLink()
                    +"</a></p>"));

        //if(isDebug) autoCompleteTextBox.setConsole(console);

        //autoCompleteTextBox.setFocus(true);
        if(isDebug && console.needsSpace())
            rootPanel.add(new ScrollPanel(console));

    }
    
    public void searchFieldOnDiv(String divId, String authorizedTypes, String functionNameWhenChosen, boolean debug) {
        final RootPanel rootPanel = RootPanel.get(divId);
        console = new SKBConsole(debug);
        SKBSearchField sf = new SKBSearchField(
                functionNameWhenChosen, authorizedTypes, serviceContext,console,debug);
        rootPanel.clear();
        rootPanel.add(sf);
    }

    public void searchFieldCmd(String cmd) {
        String[] cmdTerms = cmd.split("\\|");
        if(cmdTerms.length!=4 && cmdTerms.length!=2) {
            Window.alert("Sorry, the command \"" + cmd + "\" does not have 2 or 4 terms but " + cmdTerms.length);
            return;
        }
        if(cmdTerms.length==2)
            searchFieldOnDiv(cmdTerms[0],"",cmdTerms[1],false);
        else
            searchFieldOnDiv(cmdTerms[0],cmdTerms[1],cmdTerms[2], Boolean.parseBoolean(cmdTerms[3]));
    }

    public void insertInPane(VerticalPanel vpane, String divId, String idOfStorageField, String authorizedTypes, String previousValue, String basePathForImages, String width, String height) {
        deleteAllWidgets(vpane);

        SuggestOracle oracle = new SKBOracle(serviceContext,basePath,authorizedTypes.split(","), console);
        SuggestBox autoCompleteTextBox = new SuggestBox(oracle);
        
        SKBList queryList = new SKBList(serviceContext);
        autoCompleteTextBox.setWidth(width);
        //autoCompleteTextBox.setService(serviceContext);
        //autoCompleteTextBox.setAuthorizedTypes(authorizedTypes);

        queryList.setSize(width,height);
        if(console!=null) queryList.setConsole(console);
        queryList.setIdFieldStorage(idOfStorageField);
        queryList.init();

        vpane.add(queryList);
        //autoCompleteTextBox.setSkbTarget(queryList);
        //autoCompleteTextBox.setOwnerDivId(divId);
        //autoCompleteTextBox.init();
        if(console!=null) console.setSize(width,height);
        vpane.add(autoCompleteTextBox);
        //if(console!=null) autoCompleteTextBox.setConsole(console);

        autoCompleteTextBox.setFocus(true);
        if(console!=null && console.needsSpace()) vpane.add(new ScrollPanel(console));
    }

    public void closeOnDiv(String divId) {
        final RootPanel rootPanel = RootPanel.get(divId);
        SKBList queryList = null;
        for(Iterator it=rootPanel.iterator(); it.hasNext() ; ) {
            Widget wid = (Widget) it.next();
            if(wid instanceof SKBList)
                queryList = (SKBList) wid;
        }
        if(queryList==null) Window.alert("Can't close on " + divId + ", queryList not found.");
        // clear all
        if(queryList==null) {
            deleteAllWidgets(rootPanel);
            //log("Closing without query list.");
        } else {
            queryList.log("Closing with list " + queryList.getItems());
            fillWithStaticList(rootPanel, queryList);
        }
        if(queryList!=null && functionNameForEditorClosing!=null)
            callNativeFunctionForEditorClosing(queryList.getIDsListString());
    }


    private native void callNativeFunctionForEditorClosing(String idsList) /*-{
        if($wnd.functionNameForEditorClosing && $wnd[$wnd.functionNameForEditorClosing])
            $wnd[$wnd.functionNameForEditorClosing](idsList);
        else
            alert("No such function " + functionNameForEditorClosing + " for closing editor.");
    }-*/; 

    public void fillWithStaticList(RootPanel rootPanel, SKBList queryList) {
        deleteAllWidgets(rootPanel);
        StringBuffer buff = new StringBuffer("<p>");
        SKBRenderer renderer = new SKBRenderer(serviceContext);
        renderer.setWitSelection(false); renderer.setSmall(true);
        for(Iterator it=queryList.getItems().iterator(); it.hasNext(); ) {
            SkillItem item = (SkillItem) it.next();
            buff.append(renderer.render(item));
            if(it.hasNext()) buff.append("<br/>");
        }
        buff.append("</p>");
        rootPanel.add(new HTML(buff.toString()));
    }

    public void populateWithStaticList(String divId, String idFieldStorageId) {
        final SKBList list = new SKBList(serviceContext);
        list.setIdFieldStorage(idFieldStorageId);
        list.init();
        final RootPanel rootPanel = RootPanel.get(divId);
        if(rootPanel==null) Window.alert("Div of id \"" + divId + "\" not found.");
        fillWithStaticList(rootPanel,list);
        list.addListListener(new SKBList.ListListener() {
            public void elementAdded(SkillItem item, SKBItemEntry entry) {
                fillWithStaticList(rootPanel,list);
            }

            public void elementRemoved(SkillItem item, SKBItemEntry entry) {
                fillWithStaticList(rootPanel,list);
            }
        });
    }



    private static void deleteAllWidgets(ComplexPanel rootPanel) {
        if(rootPanel==null) return;
        rootPanel.clear();
        Element r= rootPanel.getElement();
        int m= DOM.getChildCount(r);
        for(int i=0; i<m; i++) {
            DOM.removeChild(r,DOM.getChild(r,i));
        }
    }


    public void pleaseReplaceMe(String idsList) {
        if(idsList==null || idsList.length()==0 || "undefined".equals(idsList)) return;
        String[] skbAreas = idsList.split(",");
        for(int i=0, l=skbAreas.length; i<l; i++) {
            if(skbAreas[i] == null || skbAreas[i].length()==0 || "undefined".equals(idsList)) continue;
            String[] ids = skbAreas[i].split("/");
            if(ids.length != 2) {
                Window.alert("skbPleaseReplaceMe must be a comma separated list of a/b pairs, found \"" + skbAreas[i]+ "\".");
                return;
            }
            populateWithStaticList(ids[0],ids[1]);
        }
    }

    public void putWidgetAt(String command) {
        if(command==null || command.length()==0 || "undefined".equals(command)) return;
        String[] semiColonSepped = command.split(";");

        // e.g. SkillsTextBox|idsStorage|topic,level|true
        log("inspecting command " + command);
        for(int i=0; i<semiColonSepped.length; i++) {
            if(semiColonSepped[i]==null || semiColonSepped[i].length()==0
                    || "undefined".equals(semiColonSepped[i])) continue;
            String[] s = semiColonSepped[i].split("\\|");
            log("Will load div with command " + command);
            if(s==null || s.length!=4) {
                log("putAllWidgetsAt needs to be made of a sequence of four arguments separated by \"|\", had \"" + command + "\" interpreted as "+ (s==null?"null":""+Arrays.asList(s)) +" and couldn't parse it.");
                Window.alert("loadOnDiv needs to be made of a sequence four arguments separated by \"|\", had \"" + command + "\" interpreted as "+ (s==null?"null":""+Arrays.asList(s)) +" and couldn't parse it.");
                return;
            }
            RootPanel panel = RootPanel.get(s[0]);
            panel.add(new SKBListEditorWidget(s[1],"",s[2],acceptedLangs,basePath,30,5,"true".equalsIgnoreCase(s[0])));
        }
    }
    public void activeConsole() {
        if(console==null) console = new SKBConsole(true);
    }

    public native void readConfig() /*-{
        if($wnd.skbConfigBasePath!=null) {
            this.@net.i2geo.skillstextbox.client.SkillsTextBox::basePath = $wnd.skbConfigBasePath;
        } else {
            $wnd.skbConfig = function skbConfig(basePath) {
                $wnd.skillsTextBox.@net.i2geo.skillstextbox.client.SkillsTextBox::basePath = $wnd.skbConfigBasePath;
            }
        }
        if(window.top.console) window.top.console.log("browserLanguages = " + $wnd.browserLanguages);
        if(window.top.console)
            this.@net.i2geo.skillstextbox.client.SkillsTextBox::activeConsole()();
        if($wnd.browserLanguages==null)
            this.@net.i2geo.skillstextbox.client.SkillsTextBox::acceptedLangs = "en";
        else
            this.@net.i2geo.skillstextbox.client.SkillsTextBox::acceptedLangs = $wnd.browserLanguages;
        if(window.top.console) window.top.console.log("Have read config.");

        if($wnd.skbStartWithWidget) {
            this.@net.i2geo.skillstextbox.client.SkillsTextBox::putWidgetAt(Ljava/lang/String;)($wnd.skbStartWithWidget);
        }

        if($wnd.functionNameForEditorClosing) {
            this.@net.i2geo.skillstextbox.client.SkillsTextBox::functionNameForEditorClosing = $wnd.functionNameForEditorClosing;
        }
    }-*/;

    /** This method is the "post-init" which starts the population after the parameters
     * have been read.
     */
    public native void declarePlugs()  /*-{
        $wnd.skillsTextBox= this.@net.i2geo.skillstextbox.client.SkillsTextBox::myself;
        $wnd.skbEdit = function skbEdit(idOfDiv,idOfStorageField, authorizedTypes,doDebug) {
            $wnd.skillsTextBox.@net.i2geo.skillstextbox.client.SkillsTextBox::loadOnDiv(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)(idOfDiv,idOfStorageField,authorizedTypes,doDebug);
        }
        $wnd.fillWithStaticList = function fillWithStaticList(idOfDiv,idOfStorageField) {
            $wnd.skillsTextBox.@net.i2geo.skillstextbox.client.SkillsTextBox::populateWithStaticList(Ljava/lang/String;Ljava/lang/String;)(idOfDiv,idOfStorageField);
        }
        $wnd.skbDoReplaceMe= function skbDoReplaceMe(idOfDiv,idOfStorageField,width,height) {
            $wnd.skillsTextBox.@net.i2geo.skillstextbox.client.SkillsTextBox::populateWithStaticList(Ljava/lang/String;Ljava/lang/String;)(idOfDiv,idOfStorageField);
            // TODO: take size in account
        }
        $wnd.skbDoEdit = function skbDoEdit(cmd) {
            if(window.top.console) window.top.console.log("Will edit cmd " + cmd);
            $wnd.skillsTextBox.@net.i2geo.skillstextbox.client.SkillsTextBox::skbEditOnDiv(Ljava/lang/String;)(cmd);
        }
        $wnd.skbDoSearch = function skbDoSearch(cmd) {
            if(window.top.console) window.top.console.log("Will put search cmd " + cmd);
            $wnd.skillsTextBox.@net.i2geo.skillstextbox.client.SkillsTextBox::searchFieldCmd(Ljava/lang/String;)(cmd);
        }
        if($wnd.skbPleaseReplaceMe) {
            $wnd.skillsTextBox.@net.i2geo.skillstextbox.client.SkillsTextBox::pleaseReplaceMe(Ljava/lang/String;)($wnd.skbPleaseReplaceMe);
        }
        if($wnd.skbPleaseReplaceMeActive) {
            if(window.top.console) window.top.console.log("skbPleaseReplaceMeActive="+$wnd.skbPleaseReplaceMeActive)
            this.@net.i2geo.skillstextbox.client.SkillsTextBox::loadOnDiv(Ljava/lang/String;)($wnd.skbPleaseReplaceMeActive);
        }
        if($wnd.skbSearchPleaseReplaceMe) {
            // e.g. skbSearchField|Topic,Competency|choiceIdentified
            // e.g. skbSearchField| |choiceIdentified
            this.@net.i2geo.skillstextbox.client.SkillsTextBox::searchFieldCmd(Ljava/lang/String;)($wnd.skbSearchPleaseReplaceMe);
        }

    }-*/;


    public class JSListListner implements SKBList.ListListener {
        public JSListListner(SKBList list) { this.list = list; }
        SKBList list;
        public void elementAdded(SkillItem item, SKBItemEntry entry) {
            refresh(list.getIDsListString());
        }
        public void elementRemoved(SkillItem item, SKBItemEntry entry) {
            refresh(list.getIDsListString());
        }
        private native void refresh(String newVal) /*-{
            if(window.top.console) window.top.console.log("Refreshing with value " + newVal);
            if($wnd != null && $wnd.opener !=null && $wnd.opener.skbSetMyValue!=null) {
                $wnd.opener.skbSetMyValue(newVal)
            }
            if($wnd!=null && $wnd.opener!=null && $wnd.skbPanelName != null) {
                
            }
        }-*/;
    }

    private native void alertIfMSIE() /*-{
        this.@net.i2geo.skillstextbox.client.SkillsTextBox::alertIfMSIE(Ljava/lang/String;)(navigator.appName);
    }-*/;

    private void alertIfMSIE(String navName) {
    	if(navName==null|| navName.indexOf("Microsoft")>=0 && navName.indexOf("Explorer")>=0) {
		String name="hasMSIE-i2g-warned";
		String c = Cookies.getCookie(name);
		if(c==null) {
		    Cookies.setCookie(name,"true",new Date(System.currentTimeMillis()+3600*1000*24), // one day
                    "i2geo.net","/",false); // all of i2geo.net
		    Window.alert(serviceContext.getI18n().msieWarning());
                    //"You seem to be running MicroSoft Internet Explorer. I2Geo has issues with this browser. We recommend you change browser. This warning will not be displayed anymore."
		}
    	}
    }

    private void log(String message) {
        if(console!=null) console.log(message);
    }

    private native void detectAndWarnMSIEFails() /*-{
        if(Browser.IE)
            this.@net.i2geo.skillstextbox.client.SkillsTextBox::warnBecauseOfMSIE()();
    }-*/;

    private void warnBecauseOfMSIE() {
        String COOKIE_NAME="warnedMSIEFails";
        String cookie = Cookies.getCookie(COOKIE_NAME);
        if(cookie==null) {
            Window.alert(serviceContext.getI18n().msieWarning());
            Cookies.setCookie(COOKIE_NAME,"done");
        }
    }

}// end of SkillsTextBox class
