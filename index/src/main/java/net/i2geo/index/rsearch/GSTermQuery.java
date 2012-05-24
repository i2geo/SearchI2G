package net.i2geo.index.rsearch;

import org.apache.lucene.search.TermQuery;
import org.apache.lucene.index.Term;
import net.i2geo.onto.GeoSkillsAccess;
import net.i2geo.api.OntType;

/** Simple subclass of TermQuery to concentrate treatement of the most common output of query expansion.
 */
public class GSTermQuery extends TermQuery {

    private static String baseURI = GeoSkillsAccess.ontBaseU;

    public GSTermQuery(String fld, String uri, OntType ontType) {
        super(new Term(correctFldName(fld),uriToName(uri)));
        this.ontType= ontType;
    }

    private OntType ontType = null;

    public OntType getOntType() {
        return ontType;
    }

    private static  String uriToName(String uri) {
        if(uri.startsWith(baseURI)) {
            uri = uri.substring(baseURI.length());
            //if(uri.endsWith("_r")) {
            //    uri = uri.substring(0,uri.length()-2);
            //}
        }
        //if(uri.startsWith("#"))
        //    uri = uri.substring(1);
        if(!uri.startsWith("http://") && !uri.startsWith("#")) uri = '#' + uri;
        return uri;
    }

    private static String correctFldName(String fld) {
        if(RSConstants.EDULEVEL_FIELDNM.equals(fld))
            return  "CurrikiCode.AssetClass." +RSConstants.EDULEVEL_FIELDNM;
        if(RSConstants.FIELDNM_TOPICS_AND_COMPETENCIES.equals(fld))
            return  "CurrikiCode.AssetClass." +RSConstants.FIELDNM_TOPICS_AND_COMPETENCIES;
        if("title".equals(fld))
            return  "CurrikiCode.AssetClass.title";
        if("text".equals(fld))
            return "ft";
        return fld;
    }


}
