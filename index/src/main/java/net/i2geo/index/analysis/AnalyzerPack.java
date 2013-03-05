package net.i2geo.index.analysis;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.cn.ChineseAnalyzer;
import org.apache.lucene.analysis.es.SpanishAnalyzer;
import org.apache.lucene.analysis.ru.RussianAnalyzer;
import org.apache.lucene.analysis.cz.CzechAnalyzer;
import org.apache.lucene.analysis.de.GermanAnalyzer;
import org.apache.lucene.analysis.nl.DutchAnalyzer;
import org.apache.lucene.analysis.fr.FrenchAnalyzer;
import org.apache.lucene.analysis.snowball.SnowballAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

import java.util.*;
import java.io.StringReader;
import java.io.IOException;

import net.i2geo.index.IndexHome;
import net.i2geo.index.rsearch.RSearchContext;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.util.Attribute;
import org.apache.lucene.util.Version;

/**
 */
public class AnalyzerPack {

    private static List<String> supportedLanguages = IndexHome.supportedLanguages;

    private final static Map<String,Analyzer> simpleAnalyzersPerLanguage = createAnalyzersPerLanguage(supportedLanguages);

    public static Analyzer getAnalyzerForLanguage(String lang) {
        Analyzer a = simpleAnalyzersPerLanguage.get(lang);
        if(a == null) a = simpleAnalyzersPerLanguage.get("*");
        return a;
    }

    public static Analyzer getAnalyzerForContext(RSearchContext context) {
        Analyzer defaultA = AnalyzerPack.getAnalyzerForLanguage("*"),
            found = null;
        for(String lang:context.getAcceptedLanguages()) {
            Analyzer a = AnalyzerPack.getAnalyzerForLanguage(lang);
            if(a != defaultA) {
                found = a; break;
            }
        }
        return found;
    }


    public static Map<String,Analyzer> createAnalyzersPerLanguage(List<String> langs) {
        Map<String,Analyzer> m = new HashMap<String,Analyzer>();
        for(String lang: langs) {
            Analyzer a = null;
            if("en".equals(lang)) a = new SnowballAnalyzer(Version.LUCENE_35,"English");
            else if("es".equals(lang)) a = new SpanishAnalyzer(Version.LUCENE_35);
            else if("fr".equals(lang)) a = new FrenchAnalyzer(Version.LUCENE_35);
            else if("nl".equals(lang)) a = new DutchAnalyzer(Version.LUCENE_35);
            else if("de".equals(lang)) a = new GermanAnalyzer(Version.LUCENE_35);
            else if("cz".equals(lang)) a = new CzechAnalyzer(Version.LUCENE_35);
            else if("ru".equals(lang)) a = new RussianAnalyzer(Version.LUCENE_35);
            else if("cn".equals(lang)) a = new ChineseAnalyzer();
            else a = new StandardAnalyzer(Version.LUCENE_35);
            m.put(lang,a);
        }
        m.put("*", new StandardAnalyzer(Version.LUCENE_35));
        return m;
    }


    public static List<String> tokenizeString(Analyzer an, String fieldName, String text) {
        List<String> ls = new ArrayList<String>();
        TokenStream str = an.tokenStream(fieldName,new StringReader(text));
        TermAttribute termAtt = str.getAttribute(TermAttribute.class);
        try {
            str.reset();
            while(str.incrementToken()) {
                ls.add(termAtt.term());
            }
        } catch (IOException e) {
            throw new IllegalStateException("ImprobableException.",e);
        }
        return ls;
   }

}
