package net.i2geo.api;

import net.i2geo.api.SkillsSearchServiceAsync;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.Arrays;
import java.util.List;
import java.util.Collections;
import java.io.Serializable;

/** Used to hold recurring parameters to the skills-text-box-service
 */
public class SKBServiceContext {


    public transient SkillsSearchServiceAsync service;

    public String acceptedLangs = null;

    public String basePath;


    private String language = null;
    public static final List platformSupportedLanguages = (Arrays.asList(new String[] {"cs","de","en","es","fr","nl","pt","ru"}));
    private SKBi18n skbi18n = null;

    public void chooseLanguage() {
        if(acceptedLangs==null || acceptedLangs.length()==0) acceptedLangs = "en";
        String[] langs = acceptedLangs.split(",| ");
        for(int i=0; i<langs.length; i++) {
            String l = langs[i].trim();
            if(l.length()>2) l = l.substring(0,2);
            if(platformSupportedLanguages.contains(l)) {
                language = langs[i];
                break;
            }
        }
        if(language == null)
            this.language = "en";
    }

    public void setSkbi18n(SKBi18n i18n) {
        this.skbi18n = i18n;
    }

    public SKBi18n getI18n() {
        if(language==null) chooseLanguage();
        if(this.skbi18n!=null) return this.skbi18n;
        // if were are here, we simply use GWT (this requires the locale javascript property)
        return new SKBi18nThroughJStrans();
        /*if(this.skbi18n==null) {
            this.skbi18n = (SKBi18n) GWT.create(SKBi18n.class);
        return skbi18n;
        }*/
    }

}
