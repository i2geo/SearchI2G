package net.i2geo.index.analysis;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.cn.ChineseAnalyzer;
import org.apache.lucene.analysis.ru.RussianAnalyzer;
import org.apache.lucene.analysis.cz.CzechAnalyzer;
import org.apache.lucene.analysis.de.GermanAnalyzer;
import org.apache.lucene.analysis.nl.DutchAnalyzer;
import org.apache.lucene.analysis.fr.FrenchAnalyzer;
import org.apache.lucene.analysis.snowball.SnowballAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.io.StringReader;
import java.io.IOException;

import net.i2geo.index.IndexHome;
import net.i2geo.index.rsearch.RSearchContext;
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
            if("en".equals(lang)) a = new SnowballAnalyzer(Version.LUCENE_29,"English");
            else if("es".equals(lang)) a = new SnowballAnalyzer(Version.LUCENE_29,"Spanish");
            else if("fr".equals(lang)) a = new FrenchAnalyzer(Version.LUCENE_29);
            else if("nl".equals(lang)) a = new DutchAnalyzer(Version.LUCENE_29);
            else if("de".equals(lang)) a = new GermanAnalyzer(Version.LUCENE_29);
            else if("cz".equals(lang)) a = new CzechAnalyzer(Version.LUCENE_29);
            else if("ru".equals(lang)) a = new RussianAnalyzer(Version.LUCENE_29);
            else if("cn".equals(lang)) a = new ChineseAnalyzer();
            else a = new StandardAnalyzer(Version.LUCENE_29);
            m.put(lang,a);
        }
        m.put("*", new StandardAnalyzer(Version.LUCENE_29));
        return m;
    }


    public static List<String> tokenizeString(Analyzer an, String fieldName, String text) {
        List<String> ls = new ArrayList<String>();
        TokenStream str = an.tokenStream(fieldName,new StringReader(text));
        Token tok = new Token();
        try {
            while((tok=str.next(tok))!=null) {
                ls.add(tok.termText());
            }
        } catch (IOException e) {
            throw new IllegalStateException("ImprobableException.",e);
        }
        return ls;
   }

}
