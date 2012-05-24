package net.i2geo.index;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
//import org.apache.solr.common.SolrInputDocument;
import org.semanticweb.owl.model.OWLNamedObject;

import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.net.URI;
import java.net.URLEncoder;
import java.io.UnsupportedEncodingException;

import net.i2geo.api.OntType;
import net.i2geo.api.SKBi18n;
import net.i2geo.api.SKBServiceContext;

/**
 */
public class GSIUtil {
    
    static final float BOOST_DEFCOMMONNAME = 7;
    static final float BOOST_COMMONNAME = 5;
    static final float BOOST_UNCOMMONNAME = 4;
    static final float BOOST_RARENAME = 3;
    static final float BOOST_FALSEFRIENDNAME = 0.5f;
    private static final String baseTermNavURLforLevels = "http://i2geo.net/ontologies/dev/individuals/",
        baseTopicsAndCompetenciesURL = "http://i2geo.net/comped/show.html?uri=";

    static void addKeywordField(Document doc, String fieldName, String fieldValue) {
        doc.add(new Field(fieldName,fieldValue,
                Field.Store.YES,Field.Index.UN_TOKENIZED));
    }

    static void addSimplyStoredField(Document doc, String fieldName, String fieldValue) {
        doc.add(new Field(fieldName,fieldValue,
                Field.Store.YES, Field.Index.NO));
    }    


    /* injects into the document the values read as property values*/
    static Map<String,String> transportNames(Map<String, Set<String>> allNames , String fragmentId, String fieldNamePrefix, float boost, boolean store, Document doc) {
        // Set<OWLConstant> props
        if(allNames == null || allNames.isEmpty()) return null;
        Map<String,String> firstNames = new HashMap<String,String>();
        for(Map.Entry<String,Set<String>> entry: allNames.entrySet()) {
            String language = entry.getKey();
            Set<String> names = entry.getValue();
            try {
                for(String n: names) {
                    if(language!=null && !firstNames.containsKey(language))
                        firstNames.put(language,n);
                    GSILogger.log("  - " + language + " : " + boost + ":" + n);
                    Field field = new Field(fieldNamePrefix + "-" + language,n,
                            store?Field.Store.YES:Field.Store.YES,Field.Index.TOKENIZED);
                    field.setBoost(boost);
                    doc.add(field);
                    // universal language as well
                    field = new Field(fieldNamePrefix + "-x-all",n,
                            store?Field.Store.YES:Field.Store.NO,Field.Index.TOKENIZED);
                    field.setBoost(boost);
                    doc.add(field);
                }
                //}
            } catch (Exception e) {
                GSILogger.log(e.toString());
            }
        }
        doc.add(new Field("name-x-all",fragmentId, Field.Store.NO, Field.Index.UN_TOKENIZED));
        return firstNames;
    }

    /* injects into the document the values read as property values*/
    /* static Map<String,String> transportNames(Map<String, Set<String>> allNames , String fragmentId, String fieldNamePrefix, float boost, boolean store, SolrInputDocument doc) {
        // Set<OWLConstant> props
        if(allNames == null || allNames.isEmpty()) return null;
        Map<String,String> firstNames = new HashMap<String,String>();
        for(Map.Entry<String,Set<String>> entry: allNames.entrySet()) {
            String language = entry.getKey();
            Set<String> names = entry.getValue();
            try {
                //if(x.isTyped() && null != x.getLiteral() && x.getLiteral().length()>0)
                //    log.info("  - DROPPED: \"" + x + "\" of type " + x.asOWLTypedConstant().getDataType());
                //else {
                String n = names.iterator().next();
                if(language!=null && !firstNames.containsKey(language))
                    firstNames.put(language,n);
                GSILogger.log("  - " + language + " : " + boost + ":" + n);
                doc.addField(fieldNamePrefix + "-" + language,n,boost);
                // universal language as well
                doc.addField(fieldNamePrefix + "-x-all",n,boost);
                //}
            } catch (Exception e) {
                GSILogger.log(e.toString());
            }
        }
        doc.addField("name-x-all",fragmentId);
        return firstNames;
    } */

    static String nameOf(OWLNamedObject o) {
        return uriToName(o.getURI());
    }

    static String uriToName(URI uri) {
        return uriToName(uri.toString());
    }

    static String uriToName(String uri) {
        return uriToName(uri,true);
    }

    public static String uriToName(String uri, boolean zap_r) {
        String[] parts = uri.split("\\#");
        if(parts.length!=2) return uri;
        String r = parts[1];
        if(r.length()==0) r = parts[2];
        if (zap_r && r.endsWith("_r")) r = r.substring(0,r.length()-2);
        return r;
    }


    static void addFieldForType(Document doc, OntType type) {
        addFieldForType(doc,type.getName());
    }
    static  void addFieldForType(Document doc, String typeName) {
        GSILogger.log("ontType: " + typeName);
        doc.add(new Field("ontType",typeName,
            Field.Store.YES,Field.Index.UN_TOKENIZED));
    }
    /* static  void addFieldForAncestorTopic(SolrInputDocument doc, String typeName) {
        GSILogger.log("ancestorTopic: " + typeName);
        doc.addField("ancestorTopic",typeName);//,Field.Store.YES,Field.Index.UN_TOKENIZED));
    }
    static void addFieldForType(SolrInputDocument doc, OntType type) {
        addFieldForType(doc,type.getName());
    }
    static  void addFieldForType(SolrInputDocument doc, String typeName) {
        GSILogger.log("ontType: " + typeName);
        doc.addField("ontType",typeName);//,Field.Store.YES,Field.Index.UN_TOKENIZED));
    } */
    static  void addFieldForAncestorTopic(Document doc, String ancestorTypeName) {
        GSILogger.log("ancestorTopic: " + ancestorTypeName);
        doc.add(new Field("ancestorTopic",ancestorTypeName,
            Field.Store.YES,Field.Index.UN_TOKENIZED));
    }


    static String urlForMoreInfo(String uri, OntType type) {
        if(type==OntType.TOPIC || type==OntType.ABSTRACTTOPIC || type==OntType.ABSTRACTTOPIC_WITH_REPRESENTATIVE || type==OntType.PURE_ABSTRACT_TOPIC ||
                type==OntType.COMPETENCY || type==OntType.COMPETENCYPROCESS)
            return renderUri(baseTopicsAndCompetenciesURL,uri,"");
        else if (type==OntType.LEVEL)
            return renderUri(baseTermNavURLforLevels,uri,".html");
        else throw new IllegalStateException("Can't make URL for type \"" + type + "\".");
    }

    private static String renderUri(String base, String uri, String suffix) {
        try {
            return base
             + URLEncoder.encode(uriToName(uri),"utf-8") + suffix;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }


    public static String makeItGSURI(String inputQuery) {
        if(inputQuery.startsWith("http")) return inputQuery;
        if(inputQuery.startsWith("#")) return GeoSkillsIndexer.ontBaseURI + inputQuery;
        else return GeoSkillsIndexer.ontBaseURI + "#" + inputQuery;
    }
}
