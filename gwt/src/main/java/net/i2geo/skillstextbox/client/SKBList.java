package net.i2geo.skillstextbox.client;

import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.rpc.AsyncCallback;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import net.i2geo.api.SkillItem;
import net.i2geo.api.SKBServiceContext;

/** A component to collect a list of Skills-text-box items being input through the line.
 */
public class SKBList extends VerticalPanel implements SKBTarget {

    private List skillEntries = new ArrayList();
    private SKBConsole console = null;
    private SKBServiceContext service;
    private String idFieldStorage = null;
    private Widget emptyness = null;
    private TextBox inputBox = null;


    public SKBList(SKBServiceContext service) {
        super.setStyleName("SKBEditor");
        this.service = service;
        fillWithEmptyness();
    }


    public void setConsole(SKBConsole console) {
        this.console = console;
    }

    public void setIdFieldStorage(String nm) {
        this.idFieldStorage = nm;
    }

    public void setTextBox(TextBox textBox) {
        this.inputBox = textBox;
    }

    /** Workaround to enable display of the list even if empty. */
    private void fillWithEmptyness() {
        emptyness = new HTML("<span class=\"skb-empty-label\">"+service.getI18n().emptyListLabel()+"</span>");
        super.add(emptyness);
    }

    private void zapEmptyness() {
        if(emptyness!=null)
            super.remove(emptyness);
    }

    public void insertWordSet(String wordSet) {
        if(emptyness!=null) { zapEmptyness(); }
        Widget widget = new SKBTextEntry(wordSet,this);
        super.add(widget);
        skillEntries.add(widget);
        refreshIDField();
    }

    // used?
    public Object requestAndDeleteLastInput() {
        Object last = skillEntries.get(skillEntries.size()-1);
        refreshIDField();
        return last;
    }

    public void insertNode(SkillItem skillItem) {
        this.insertNode(skillItem, true);
    }
    public void insertNode(SkillItem skillItem, boolean fireEvents) {
        if(skillItem==null) return;
        if(skillItem.getUri()==null || skillItem.getUri().indexOf("#")<0) {
            skillItem.setUri("#" + skillItem.getUri());
        }

        // proof that it's not there yet
        if(skillEntries==null) skillEntries = new ArrayList();
        for(Iterator it=skillEntries.iterator(); it.hasNext();) {
            Object obj = it.next();
            if(obj instanceof SKBItemEntry) {
                if(((SKBItemEntry) obj).item.getUri().equals(skillItem.getUri())) {
                    log("Refusing to add duplicate.");
                    return;
                }
            }
        }

        if(emptyness!=null) { zapEmptyness(); }
        log("Adding SkillItem " + skillItem);
        SKBItemEntry entry = new SKBItemEntry(skillItem, service, this);
        entry.init();
        skillEntries.add(entry);
        super.add(entry);
        refreshIDField();
        if(fireEvents) for(Iterator it=listListeners.iterator(); it.hasNext(); ) {
            ((ListListener)it.next()).elementAdded(skillItem,entry);
        }
    }




    public void deleteNode(String uri) {
        log("Deleting uri " + uri);
        if(uri==null || uri.length()==0) return;
        if(uri.indexOf("#")==-1) uri= "#" + uri;
        List widgetsToRemove = new ArrayList();
        for(Iterator it=skillEntries.iterator(); it.hasNext(); ) {
            Object obj = it.next();
            if(obj instanceof SKBItemEntry) {
                if(uri.equals(((SKBItemEntry)obj).item.getUri())) {
                    widgetsToRemove.add(obj);
                }
            } else if(obj instanceof SKBTextEntry) {
                if(uri.equals(((SKBTextEntry)obj).text)) {
                    widgetsToRemove.add(obj);
                }
            } else {
                log("Bizarre entry: " + obj);
            }
        }


        // first put emptyness if all are to be removed
        if(widgetsToRemove.size() == super.getWidgetCount()) {
            fillWithEmptyness();
        }

        log("Deleting " + widgetsToRemove);
        log("Will fire for " + listListeners.size() + " listeners.");
        
        for(Iterator it=widgetsToRemove.iterator(); it.hasNext(); ) {
            Widget wid = (Widget) it.next();
            super.remove(wid);
            skillEntries.remove(wid);

            if(wid instanceof SKBItemEntry) {
                log("Firing for " + wid);
                SKBItemEntry entry = (SKBItemEntry) wid;
                for(Iterator i=listListeners.iterator(); i.hasNext(); ) {
                    ((ListListener)i.next()).elementRemoved(entry.item,entry);
                }
            }
        }



        refreshIDField();
        log("Now field is \"" + getIDFieldContent() + "\".");
    }


    public void log(String msg) {
        if(console!=null)
            console.log(msg);
    }

    void removeSkillEntry(SKBItemEntry obj) {
        log("Removing it "+ obj.item);
        deleteNode(obj.item.getUri());
    }


    public String toString() {
        return "SKBList: " + super.toString();
    }

    public List getItems() {
        ArrayList l = new ArrayList(skillEntries.size());
        for(Iterator it=skillEntries.iterator(); it.hasNext(); ) {
            Object obj = it.next();
            if(obj instanceof SKBItemEntry) {
                l.add(obj);
            }
        }
        return l;
    }


    public String getIDsListString() {
        StringBuffer buff = new StringBuffer();
        for(Iterator it= skillEntries.iterator(); it.hasNext(); ) {
            Object obj = it.next();
            if(obj instanceof SKBItemEntry)
                buff.append(((SKBItemEntry) obj).item.getUri());
            if(it.hasNext()) buff.append(',');
        }
        return buff.toString();
    }
    private void refreshIDField() {
        if(inputBox==null)
            setIDFieldContent(getIDsListString());
        else {
            log("Setting value to " + getIDsListString());
            inputBox.setText(getIDsListString());
        }
    }

    public native void setIDFieldContent(String content) /*-{
        name = this.@net.i2geo.skillstextbox.client.SKBList::idFieldStorage;
        tf = window.top.document.getElementById(name);
        if(tf==null) tf = window.top.document.getElementById(name+"_");
        if(tf==null) return;
        if(tf.nodeName == "INPUT")
            tf.value = content;
        else
            tf.data = content;
    }-*/;


    public native String getIDFieldContent() /*-{
        name = this.@net.i2geo.skillstextbox.client.SKBList::idFieldStorage;
        tf = window.top.document.getElementById(name);
        if(tf==null) tf = window.top.document.getElementById(name+"_");
        if(tf==null) return "";
        if(tf.nodeName == "INPUT")
            return tf.value;
        else
            return tf.data;
    }-*/;

    /** Reads builds the list of values as provided by the text-field getIDFieldContent */
    void init() {
        log("Searching for field of name " + idFieldStorage);
        String potentialInit = getIDFieldContent();
        log("potentialInit0 = " + potentialInit);

        if(potentialInit!=null && potentialInit.trim().length()>0) {
            log("potentialInit1 is " + potentialInit);
            if(potentialInit.charAt(0) =='[' && potentialInit.charAt(potentialInit.length()-1)==']') {
                if(potentialInit.length()==2) potentialInit="";
                else potentialInit = potentialInit.substring(1,potentialInit.length()-1);
            }
            log("potentialInit2 is " + potentialInit);
            final String sentPotentialInit = potentialInit;
            service.service.getSkillItem(potentialInit, service.acceptedLangs,
                    new AsyncCallback() {
                public void onFailure(Throwable caught) {
                    log("Failure at building skill item \"" + sentPotentialInit + "\"." + caught);
                    try {
                        SKBList.this.insertWordSet(sentPotentialInit);
                    } catch (Exception e) {
                        log(""+e);
                    }
                }

                public void onSuccess(Object result) {
                    try {
                        SkillItem[] items = (SkillItem[]) result;
                        for(int i=0, l=items.length; i<l; i++) {
                            insertNode(items[i],false);
                        }
                    } catch (ClassCastException e) {
                        log("Not skill items !" + result);
                    }
                }
            });
        }
    }

    private List listListeners = new ArrayList();

    void addListListener(ListListener l) {
        listListeners.add(l);
    }

    public static interface ListListener {
        public void elementAdded(SkillItem item, SKBItemEntry entry);
        public void elementRemoved(SkillItem item, SKBItemEntry entry);
    }

}
