package net.i2geo.skillstextbox.client;

import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.ui.Image;
import net.i2geo.api.SkillItem;
import net.i2geo.api.SKBServiceContext;

/**
*/
public class SKBItemEntry extends HorizontalPanel {


    public SKBItemEntry(SkillItem item, SKBServiceContext context, SKBList list) {
        super();
        this.item = item;
        this.context = context;
        this.list = list;
        super.setWidth("99%");
    }

    SkillItem item;
    Image image;
    Label deleteButton;
    SKBServiceContext context;
    SKBList list;

    public void init() {

        super.setWidth("100%");
        super.setStyleName("SKBEditor");

        String iconPictureName = "missing-icon.png";
        try {
            iconPictureName = context.getI18n().getString("types_labels_" + item.getType());
        } catch(Exception ex) {
            list.log("Can't parse iconPicture of type " + item.getType());
            list.log("Please provide phrase types_labels_\"" + item.getType());
        }
        image = new Image(context.basePath +
                context.getI18n().getString("types_icons_"+ item.getType()));
        image.setTitle(iconPictureName);
        image.setWidth("2em");
        image.setHeight("2em");
        super.add(image);

        // title of the node
        HTML text = new HTML(nodeAnchorElement());
        super.add(text);
        list.log("added text.");
        super.setCellVerticalAlignment(text,ALIGN_MIDDLE);
        super.setCellHorizontalAlignment(text,ALIGN_LEFT);
        super.setCellWidth(text,"95%");

        // delete button: a label
        deleteButton = new Label("x");
        deleteButton.addClickListener(new ClickListener() {
            public void onClick(Widget wid) {
                list.removeSkillEntry(SKBItemEntry.this);
            }});
        /* deleteButton.addMouseListener(new MouseListenerAdapter() {
            public void onMouseEnter(Widget wid) {
                deleteButton.setStyleName("SKBEditor-DeleteButton-Over");
            }
            public void onMouseLeave(Widget wid) {
                deleteButton.setStyleName("SKBEditor-DeleteButton-Out");
            }}); */

        deleteButton.setTitle(context.getI18n().buttonsErase());
        deleteButton.setHeight("2em");
        super.add(deleteButton);
        super.setCellHorizontalAlignment(deleteButton,HorizontalPanel.ALIGN_RIGHT);
        super.setCellVerticalAlignment(deleteButton,ALIGN_MIDDLE);
        //deleteButton.setVisible(false);


        // roll-overs to indicate the currently selected topic
        super.setStyleName("SKBEditor-Topic-DeSelected");
        MouseListener nodeCursor = new MouseListenerAdapter() {
            public void onMouseEnter(Widget wid) {
                SKBItemEntry.super.setStyleName("SKBEditor-Topic-Selected");
                deleteButton.setStyleName("SKBEditor-DeleteButton-Over");
                //deleteButton.setVisible(true);
            }
            public void onMouseLeave(Widget wid) {
                SKBItemEntry.super.setStyleName("SKBEditor-Topic-DeSelected");
                deleteButton.setStyleName("SKBEditor-DeleteButton-Out");
                //deleteButton.setVisible(false);
            }};
        deleteButton.addMouseListener(nodeCursor);
        image.addMouseListener(nodeCursor);
        text.addMouseListener(nodeCursor);
    }


  /*  a target="curriculum-browsing"
  href='/ontologies/current/CurriculumExtracts/Schroedel-Mathematik-neue-Wege-9-Lergenmuueller-Schmidt.html'
  onclick='window.open("/ontologies/current/CurriculumExtracts/Schroedel-Mathematik-neue-Wege-9-Lergenmuueller-Schmidt.html", "curriculumBrowsing","width=300,height=500,screenX=20,scrollbars=yes,status=no,toolbar=no,menubar=no,location=no,resizable=yes"); return false;'
  >*/
    public String nodeAnchorElement() {
        StringBuffer buff = new StringBuffer();
        buff.append("<a target=\"competencyBrowsing\" class=\"SKBEditorTitle\" href=\"");
        buff.append(item.getUrlForNavigator());
        buff.append("\" title=\"").append(context.getI18n().tooltipClickToSeeMore());
        buff.append("\" onclick='window.open(\"").append(item.getUrlForNavigator())
                .append("#itemTitle\", \"competencyBrowsing\",\"width=400,height=500,screenX=20,scrollbars=yes,status=no,toolbar=no,menubar=no,location=no,resizable=yes\"); return false;''");
        buff.append("\">");
        buff.append(item.getReadableTitle());
        buff.append("</a>");
        return buff.toString();
    }

    public String toString() {
        return "SKBItemEntry(" + (item!=null? item.getUri(): " no item") + ") ";
    }
}