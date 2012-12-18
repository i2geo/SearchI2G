package net.i2geo.index.analysis;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.cn.ChineseAnalyzer;
import org.apache.lucene.analysis.ru.RussianAnalyzer;
import org.apache.lucene.analysis.cz.CzechAnalyzer;
import org.apache.lucene.analysis.de.GermanAnalyzer;
import org.apache.lucene.analysis.nl.DutchAnalyzer;
import org.apache.lucene.analysis.fr.FrenchAnalyzer;
import org.apache.lucene.analysis.snowball.SnowballAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

import java.io.Reader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

/** Configurable analyzer for indexing and queries.
 */
public class SKBAnalyzer extends Analyzer {

    public SKBAnalyzer(boolean withStartAndEndMarkers, List<String> supportedLangs) {
        this.withStartAndEndMarkers = false;//withStartAndEndMarkers;
    }
    private final boolean withStartAndEndMarkers;


    public TokenStream tokenStream(String fieldName, Reader reader) {
        String language = fieldName.substring(fieldName.length()-2);
        Analyzer a = AnalyzerPack.getAnalyzerForLanguage(language);
        if(withStartAndEndMarkers) {
            return new StartAndEndWrapper(a.tokenStream(fieldName,reader));
        } else {
            return a.tokenStream(fieldName,reader);
        }
    }

    private class StartAndEndWrapper extends TokenFilter {
        protected StartAndEndWrapper(TokenStream input) {
            super(input);
        }
        boolean hasStarted = false, hasFinished = false;

        @Override
        public Token next(Token last) throws IOException {
            if(!hasStarted) {
                hasStarted = true;
                return START_MARKER_TOKEN;
            }
            Token n = input.next(last);
            if(n==null) {
                if(!hasFinished) {
                    hasFinished = true;
                    return END_MARKER_TOKEN;
                } else {
                    return null;
                }
            } else return n;
        }
    }

    public static Token START_MARKER_TOKEN = new Token("[start]",0,0,"-start-end-marker-"),
        END_MARKER_TOKEN = new Token("[end]",0,0,"-start-end-marker-");

    public static void main(String[] args) throws Throwable {
        SKBAnalyzer a = new SKBAnalyzer(Boolean.parseBoolean(args[2]), Arrays.asList(args[1].split(",")));
        TokenStream t =a.tokenStream(args[0], new StringReader(args[3]));
        Token tok;
        while((tok=t.next())!=null) System.out.println("-- " +tok);
    }

}
