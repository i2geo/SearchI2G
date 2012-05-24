package net.i2geo.skillstextbox.client;

import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.core.client.GWT;
import net.i2geo.api.SKBServiceContext;
import net.i2geo.api.SkillsSearchServiceAsync;
import net.i2geo.api.SkillsSearchService;

/** Widget class to encapsulate all functionalities of the SKBList as an editor (list, textfield, input-field)
 * 
 */
public class SKBListEditorWidget extends VerticalPanel {

    public SKBListEditorWidget(String inputFieldName, String value, String types, String acceptedLangs, String basePath, float widthInEm, float heightInEm, boolean logToConsole) {
        this.console = new SKBConsole(logToConsole);
        this.service = new SKBServiceContext();
        service.acceptedLangs = acceptedLangs;
        service.basePath = basePath;
        service.chooseLanguage();
        service.service = (SkillsSearchServiceAsync) GWT.create(SkillsSearchService.class);
        ((ServiceDefTarget) service.service).
                setServiceEntryPoint("/SearchI2G/search");

        this.list = new SKBList(service);
        list.setIdFieldStorage(inputFieldName);
        list.setConsole(console);

        SKBOracle oracle = new SKBOracle(service,basePath,types.split(","),console);
        field = new SuggestBox(oracle);
        //field.setConsole(console);
        WaitingSign waitingSign = new WaitingSign(service);
        /// TODO: display waitingSign
        oracle.setSkbTarget(list,field,waitingSign);
        Grid grid = new Grid(1,1);
        grid.setWidget(0,0,field);
        field.setWidth("100%");

        // put in
        super.add(list);
        super.add(field);
        if(console.isEnvironmentBrowserOnly) super.add(console);

        // sizes
        if(widthInEm!=-1) {
            list.setWidth(""+widthInEm+"em");
            grid.setWidth(""+widthInEm+"em");
        }
        if(heightInEm>2.4f) {
            list.setHeight(""+(heightInEm-1.5f)+"em");
            grid.setHeight("1.5em");
        }

        // storage
        input = new TextBox();
        input.setName(inputFieldName);
        input.setText(value);
        input.setVisible(true);
        list.setTextBox(input);
        super.add(input);
    }


    private SKBServiceContext service;
    private SKBList list;
    private SuggestBox field;
    private SKBConsole console;
    private TextBox input;

    public String getValue() {
        return list.getIDsListString();
    }

    public String getName() {
        return input.getName();
    }
}
