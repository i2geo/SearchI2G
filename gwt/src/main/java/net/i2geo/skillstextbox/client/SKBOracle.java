package net.i2geo.skillstextbox.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.http.client.*;
import com.google.gwt.user.client.ui.SuggestOracle;
import com.google.gwt.user.client.ui.SuggestionHandler;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.SuggestionEvent;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.Timer;

import java.util.*;

import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.Element;
import com.google.gwt.xml.client.NodeList;
import com.google.gwt.xml.client.XMLParser;
import net.i2geo.api.SkillItem;
import net.i2geo.api.SKBServiceContext;

/**
 */
public class SKBOracle extends SuggestOracle implements SuggestionHandler {

    public SKBOracle(SKBServiceContext service, String basePath, String[] authorizedTypes, SKBConsole console) {
        this.service = service;
        this.basePath = basePath;
        this.console = console;
        this.authorizedTypes = authorizedTypes;
    }

    private SKBServiceContext service;
    private String basePath;
    private SKBConsole console;
    private String[] authorizedTypes;
    private SKBTarget target = null;
    private SuggestBox box = null;
    private boolean alsoPlainText = false;
    private SKBOracleWorkListener workListener = null;
    private Timer zapMeInCase = null;

    private static final int SUGGESTION_CACHE_SIZE = 20;
    private Map<String,List<SKBCompletion>> suggestionCache = new HashMap<String,List<SKBCompletion>>();
    private List<SKBCompletion> suggestionCacheList = new LinkedList<SKBCompletion>();


    private DelayedQuerier lastQuerier = null;

    public boolean isDisplayStringHTML() {
        return true;
    }

    void setWorkListener(SKBOracleWorkListener workListener) {
        this.workListener = workListener;
    }

    public void setAlsoPlainText(boolean alsoPlainText) {
        this.alsoPlainText = alsoPlainText;
    }

    public class DelayedQuerier extends Timer {
        private static final int DELAY_BEFORE_LAUNCH_QUERY = 500;

        public DelayedQuerier(Request request, Callback callback) {
            this.callback = callback;
            this.request = request;
        }

        private final Request request;
        private final Callback callback;
        private boolean cancelled = false;

        public String toString() {
            return super.toString() + " for \"" + request.getQuery() + "\"";
        }

        public void run() {
            if(cancelled) {
                console.log("Delayed querier " + toString() + " cancelled.");
                return;
            }
            console.log("requesting suggestion for \"" + request.getQuery() + "\".");
            StringBuffer urlB = new StringBuffer(service.basePath).append("../getAutoCompletions?l=")
                    .append(URL.encodeComponent(service.acceptedLangs))
                    .append("&q=").append(URL.encodeComponent(request.getQuery()))
                    .append("&t=");
            for(String type: authorizedTypes) {
                urlB.append(URL.encodeComponent(type));
                urlB.append(URL.encodeComponent(","));
            }
            String url = urlB.toString();
            //String url = "http://127.0.0.1:8080/SearchI2G/getAutoCompletions?l=" + URL.encodeComponent(service.acceptedLangs)
            //        + "&q=" + URL.encodeComponent(request.getQuery());
            //console.log("Requesting to " + url);
            RequestBuilder builder = new RequestBuilder(RequestBuilder.GET,url);
            try {
                builder.sendRequest(null,new RequestCallback() {
                    public void onResponseReceived(com.google.gwt.http.client.Request httpRequest, com.google.gwt.http.client.Response httpResponse) {
                        if(cancelled) return;
                        if(httpResponse.getStatusCode() != 200) {
                            if(console!=null) console.log("received http status " + httpResponse.getStatusCode() + " : " +
                               httpResponse.getStatusText());
                            return;
                        }

                        try {
                            String xmlString = httpResponse.getText();
                            Document xmlDoc = XMLParser.parse(xmlString);
                            //console.log("Receiving xml " + xmlString);
                            NodeList list = xmlDoc.getElementsByTagName("autoCompletion");
                            //console.log("Receiving suggestions of length " + list.getLength()+ " for " + DelayedQuerier.this);
                            List<SKBCompletion> suggestions = new ArrayList<SKBCompletion>(list.getLength());
                            for(int i=0, l = list.getLength(); i<l; i++) {
                                Element elt = (Element) list.item(i);
                                SkillItem item = new SkillItem(
                                        elt.getAttribute("title"),
                                        elt.getAttribute("shortDesc"),
                                        elt.getAttribute("num")!=null ?Integer.parseInt(elt.getAttribute("num")) : -1 ,
                                        elt.getAttribute("urlForNav"),
                                        elt.getAttribute("uri")
                                );
                                item.setType(elt.getAttribute("type"));
                                suggestions.add(new SKBCompletion(service,renderSKIforPopup(item, basePath),
                                        item));
                            }
                            Response oracleResponse = new Response();
                            if(alsoPlainText)
                                suggestions.add(0,new SKBCompletion(service,request.getQuery(),null));
                            addToCache(request.getQuery(),suggestions);
                            oracleResponse.setSuggestions(suggestions);
                            callback.onSuggestionsReady(request,oracleResponse);
                            if(lastQuerier==DelayedQuerier.this) {
                                console.log("Last querier was me.");
                                lastQuerier = null;
                                if(workListener!=null) workListener.nowFinishedRequest();
                            } else console.log("Last querier was not me.");
                        } catch (Exception e) {
                            e.printStackTrace();
                            if(console!=null) console.log("Trouble at executing request: " + e);
                        }
                    }

                    public void onError(com.google.gwt.http.client.Request httpRequest, Throwable throwable) {
                        if(console!=null) {
                            console.log(throwable.toString());
                        }
                        if(!cancelled) {
                            List<SKBCompletion> responses = new ArrayList<SKBCompletion>();
                            Response response = new Response();
                            if(alsoPlainText)
                                responses.add(new SKBCompletion(service,request.getQuery(),null));
                            response.setSuggestions(responses);
                            callback.onSuggestionsReady(request,response);
                            if(console==null)
                                Window.alert(throwable.toString());
                        }
                    }
                });
            } catch (RequestException e) {
                e.printStackTrace();
                console.log("Trouble at formulating request: " + e);
            }
            if(workListener!=null) workListener.nowLaunchedRequest();
        }

        public void start() {
            if(workListener!=null) workListener.willLaunchRequest(DELAY_BEFORE_LAUNCH_QUERY);
            if(lastQuerier!=null) {
                lastQuerier.cancelled = true;
                lastQuerier.cancel();
                console.log("Querier " + lastQuerier + " cancelled.");
            }
            lastQuerier = this;
            this.schedule(DELAY_BEFORE_LAUNCH_QUERY);
            console.log("Delayed querier " + toString() + " launched.");
        }

    }

    private void addToCache(String query, List<SKBCompletion> suggestions) {
        if(suggestionCache.size()> SUGGESTION_CACHE_SIZE) {
            SKBCompletion extraSuggestion = suggestionCacheList.remove(0);
            suggestionCache.remove(extraSuggestion);
        }

        if(console!=null) console.log("Adding suggestion to cache.");
        suggestionCache.put(query,suggestions);        
    }

    public void requestSuggestions(final Request request, final Callback callback) {
        if(suggestionCache.containsKey(request.getQuery())) {
            if(console!=null)console.log("Suggestion already cached.");
            Response response = new Response();
            response.setSuggestions(suggestionCache.get(request.getQuery()));
            callback.onSuggestionsReady(request,response);
        } else {
            DelayedQuerier q = new DelayedQuerier(request,callback);
            q.start();
        }
    }

    public void setSkbTarget(final SKBTarget target, final SuggestBox box, final WaitingSign waitingSign) {
        this.target = target;
        try {
            box.removeEventHandler(this);
        } catch (Exception e) {
            // do nothing
        }
        box.addEventHandler(this);

        box.addKeyDownHandler(new KeyDownHandler() {
            public void onKeyDown(KeyDownEvent keyDownEvent) {
                if(keyDownEvent.getNativeKeyCode() == KeyCodes.KEY_ESCAPE) {
                    box.hideSuggestionList();
                }
                if(keyDownEvent.getNativeKeyCode() == KeyCodes.KEY_ENTER
                        && target instanceof SKBSearchField && !box.isSuggestionListShowing()) {
                    zapMeInCase = new Timer() { public void run() {
                        String text = ((SKBSearchField)target).getText();
                        if(text!=null && text.length()>0)
                            chooseText(text);
                    } };
                    zapMeInCase.schedule(200);
                }
            }
        });

        this.box = box;
        new ChooseNodeAgent(box,this);
        console.log("Oracle " + this + " has target " + target);

        this.setWorkListener(waitingSign);
    }

    public void onSuggestionSelected(SuggestionEvent event) {
        if(target!=null && box!=null && event.getSelectedSuggestion() instanceof SKBCompletion) {
            if(zapMeInCase!=null) zapMeInCase.cancel();
            SKBCompletion suggestion = (SKBCompletion) event.getSelectedSuggestion();
            if(suggestion.getItem()!=null)
                chooseNode(suggestion.getItem());
            else
                chooseText(suggestion.getReplacementString());
        } else {
            console.log("No target.");
        }
    }

    public void chooseNode(SkillItem item) {
        target.insertNode(item);
        box.setText("");
    }


    public boolean isTypeAuthorized(SkillItem item) {
        if(authorizedTypes==null || authorizedTypes.length==0) return true;
        String type = item.getType();
        for(String t: authorizedTypes) {
            if(type.equalsIgnoreCase(t)) return true;
        }
        return false;
    }

    public void chooseText(String txt) {
        target.insertWordSet(txt);
        box.setText(txt);
    }

    public void chooseNode(final String uri) {
        console.log("Sending request to chooseNode from " + this);
        AsyncCallback cb = new AsyncCallback() {
            boolean tryAdding_r = false;
            public void onFailure(Throwable caught) {
                if(!tryAgain()) console.log("Sorry, can't choose node " + uri + " : " + caught);
            }

            /** @return true if it did try again */
            private boolean tryAgain() {

                if(tryAdding_r == false && !uri.endsWith("_r")) { // try again with _r
                    tryAdding_r = true;
                    service.service.getSkillItem(uri + "_r",service.acceptedLangs,this);
                    return true;
                }
                return false;
            }
            public void onSuccess(Object result) {
                console.log("successfully received.");
                SkillItem[] items = (SkillItem[]) result;
                if(items.length<1) {
                    if(!tryAgain())
                        console.log("received empty result! ");
                    return;
                } else if(items.length>1) {
                    console.log("too many items received, taking first.");
                }
                SkillItem item = items[0];
                console.log("Received result \"" + item.getReadableTitle()+ "\".");
                if(isTypeAuthorized(item)) {
                    chooseNode(item);
                    console.log("Have called chooseNode.");
                } else {
                    console.log("Type \""+ item.getType()+"\" is not authorized.");
                }
            }
        };
        service.service.getSkillItem(uri,service.acceptedLangs,cb);
    }

    public static String renderSKIforPopup(SkillItem item, String basePath) {
        return "    <table class=\"SKBItemSuggest\" cellspacing=\"0\" width=\"95%\">\n" +
                "    <tr>" +
                "        <td style=\"width:2em\" rowspan=\"2\"><img src=\""+basePath+"type-"+item.getType()+".png\" alt=\""+item.getType()+"\" style=\"width:2em\"/></td>\n"+
                "        <td style=\"width:95%\" style=\"font-size:bigger\">"+item.getReadableTitle() +"</td>\n" +
                "        <td title=\"Number of occurences found\" ><!-- ["+item.getNumberOfMatchInStore()+"]--></td>" +
                "    </tr><tr>" +
                "" +
                "        <td title=\"Short Description\" style=\"font-size:smaller\">"+item.getShortDescription()+"</td>"+
                "        <td title=\"more about this\">" +
                "<a target=\"competencyBrowsing\"\n" +
                "  href='"+item.getUrlForNavigator()+"'\n" +
                "  onclick='window.open(\""+item.getUrlForNavigator()+"#itemTitle\", \"competencyBrowsing\",\"width=400,height=500,screenX=20,scrollbars=yes,status=no,toolbar=no,menubar=no,location=no,resizable=yes\"); return false;'\n" +
                "  ><img src=\""+basePath+"book.gif\" border=\"0\"></a></td></tr></table>";
    }
/* MODEL
<a target="competencyBrowsing"
  href='xx'
  onclick='window.open("xx#itemTitle", "competencyBrowsing","width=400,height=500,screenX=20,scrollbars=yes,status=no,toolbar=no,menubar=no,location=no,resizable=yes"); return false;'
  >
 */


    /** Interface to allow UIs to show that a request is being sent hence let users be patient. */
    public static interface SKBOracleWorkListener {

        public void willLaunchRequest(int inMillis);

        public void nowFinishedRequest();

        public void nowLaunchedRequest();
    }


}
