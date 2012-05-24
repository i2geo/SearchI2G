package net.i2geo.api;

/** Simple class to serve the duties of rendering to HTML some skill-items.
 */
public class SKBRenderer {

    public SKBRenderer(SKBServiceContext service) {
        this.service = service;
    }

    private SKBServiceContext service;
    private boolean withSelection = false;
    private boolean small = true;

    public void setSmall(boolean small) { this.small = small;}
    public void setWitSelection(boolean withSelection)
        { this.withSelection = withSelection;}

    public String render(String wordSet) {
        if(wordSet==null || wordSet.length()==0) return "";
        String typeIcon = service.getI18n().types_icons_text();
        String typeLabel = service.getI18n().types_labels_text();
        if(!withSelection) {
            return "<p><img src=\"" + service.basePath + typeIcon + "\" alt=\""+typeLabel+"\"> " + escapeStringForHtmlText(wordSet)+ "</p>";
        } else return "    <table cellspacing=\"0\" "+(small?"":"width=\"95%\"")+"><tr " +
                "  onmouseover=\"this.className = 'SKBEditor-Topic-Selected'\" onmouseout=\"this.className = 'SKBEditor-Topic-DeSelected'\"" +
                ">\n" +
                "        <td style=\"vertical-align:middle; width:"+(small?"1.5em":"2em")+"\"><img src=\""+service.basePath + typeIcon + "\" alt=\""+ typeLabel +"\" style=\"width:"+(small?"1.5em":"2em")+"\"/></td>\n" +
                "        <td>"+wordSet+"</td>\n" +
                "        <td style=\"width:1em\" " +
                ">x</td>\n" +
                "    </tr></table>";
    }

    public String render(SkillItem item) {
        return this.render(item,null);
    }



/* MODEL
<a target="competencyBrowsing"
  href='xx'
  onclick='window.open("xx", "competencyBrowsing","width=400,height=500,screenX=20,scrollbars=yes,status=no,toolbar=no,menubar=no,location=no,resizable=yes"); return false;'
  >
 */

    public String render(SkillItem item, String storageName) {
        String urlForNav = item.getUrlForNavigator(),
                type = item.getType(),
                readableTitle = item.getReadableTitle(),
                uri = item.getUri();

        if(uri==null) uri = "MISSING-URI";
        if(readableTitle==null) readableTitle=uri;
        StringBuffer buff = new StringBuffer("    <span class=\"static-node-list-entry\"> ");
        //if(!small) buff.append("width=\"95%\"");
        String typeIcon = service.getI18n().getString("types_icons_" + item.getType() );
        String typeLabel = service.getI18n().getString("types_labels_" + item.getType() );
        if(type!=null) {
            buff.append("<img src=\"");
            buff.append(service.basePath).append(typeIcon).append("\" alt=\"").append(typeLabel).append("\" style=\"vertical-align:middle; width:");
            if(small) buff.append("1.5em"); else buff.append("2em");
            buff.append("\"/>");
        }
        if(urlForNav!=null) {
            buff.append("<a class=\"SKBEditorTitle\" href=\"").append(urlForNav).append("\" target=\"competencyBrowsing\" title=\"")
                    .append(service.getI18n().tooltipClickToSeeMore());
            buff.append("\" onclick='window.open(\"")
                    .append(urlForNav)
                    .append("#itemTitle\", \"competencyBrowsing\",\"width=400,height=500,screenX=20,scrollbars=yes,status=no,toolbar=no,menubar=no,location=no,resizable=yes\"); return false;");
            buff.append("'>");
        }
        buff.append(readableTitle);
        if(urlForNav!=null) buff.append("</a>");

        buff.append("    </span><br/>");
        return buff.toString();
    }

    private String escapeStringForHtmlText(String w) {
        return w.replaceAll("&","&amp;")
                .replaceAll("<","&lt;").replaceAll(">","&gt;");
    }



}
