package net.i2geo.api;

import com.google.gwt.user.client.rpc.IsSerializable;

//import rocket.json.client.JsonSerializable;
/** A class to denote a token that is displayed as potential match
 * within the completion suggested menu. This object should encompass
 * all necessary information to provide a rich view at a skill that would be
 * chosen.
 */
public class SkillItem implements IsSerializable{

    public SkillItem(){
      //default constructor
    }

    public SkillItem(String title, String shortDesc, int num, String urlForNav, String uri) {
      super();
      this.readableTitle = title;
      this.shortDescription = shortDesc;
      this.numberOfMatchInStore = num;
      this.urlForNavigator = urlForNav;
      this.uri = uri;
    }
    /** The title to be presented to the user. 
     * @jsonSerialization-javascriptPropertyName readableTitle
     */
    String readableTitle;

    /** The number of resources linked to this token in some way. 
     * @jsonSerialization-javascriptPropertyName numberOfMatchInStore
     */
    int numberOfMatchInStore;

    /** A 2-3 sentence description of the token. 
     * @jsonSerialization-javascriptPropertyName shortDescription
     */
    String shortDescription;

    /** The URL to browse to for a description of this item as well as a navigator
     * to the tokens related to this one. 
     * @jsonSerialization-javascriptPropertyName urlForNavigator
     */
    String urlForNavigator;

    /** The identifier within the ontology either with absolute URI or as fragment identifier (with or without #)
     * @jsonSerialization-javascriptPropertyName uri
     * */
    String uri;

    /** The type-name of this item, allows to attach an icon. 
     * @jsonSerialization-javascriptPropertyName type
     */
    String type;

    /** Indicates whether this skill is considered complete or to require more
     * information in order to denote a single skill. 
     * @jsonSerialization-javascriptPropertyName complete
     */
    boolean complete;

    public String getReadableTitle() {
        return readableTitle;
    }

    public int getNumberOfMatchInStore() {
        return numberOfMatchInStore;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDesc) {
        this.shortDescription = shortDesc;
    }

    public String getType() {
        return type;
    }

    public boolean isComplete() {
        return complete;
    }
    
    public void setReadableTitle(String newTitle){
	this.readableTitle = newTitle;
    }

    public String toString() {
        return "[" + type + ": " + readableTitle + ", " + uri+ "]";
    }

    public void setUri(String uri) {
        this.uri = uri;
    }
    public String getUri(){
        return uri;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setUrlForNavigator(String urlForNavigator) {
        this.urlForNavigator = urlForNavigator;
    }
    public String getUrlForNavigator() { return urlForNavigator; }

}
