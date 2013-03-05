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
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;

import java.io.Reader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

/** Configurable analyzer for indexing and queries.
 */
public final class SKBAnalyzer extends Analyzer {

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
            this.tokenStream = input;
            this.termAttribute = input.getAttribute(TermAttribute.class);
            this.positionIncrementAttribute = input.getAttribute(PositionIncrementAttribute.class);
            this.offsetAttribute = input.getAttribute((OffsetAttribute.class));
        }
        boolean hasStarted = false, hasFinished = false;
        private TokenStream tokenStream;
        TermAttribute termAttribute;
        OffsetAttribute offsetAttribute;
        PositionIncrementAttribute positionIncrementAttribute;

        private void setToken(Token token) {
            // is this breaking the underlying TokenStream?
            termAttribute.setTermBuffer(token.term());
            offsetAttribute.setOffset(token.startOffset(), token.endOffset());
            positionIncrementAttribute.setPositionIncrement(token.getPositionIncrement());
        }

        @Override
        public boolean incrementToken() throws IOException {
            if(!hasStarted) {
                // set start token
                hasStarted = true;
                setToken(START_MARKER_TOKEN);
                return true;
            } else if(hasFinished) {
                return false;
            } else {
                hasFinished = tokenStream.incrementToken();
                if(hasFinished) {
                    setToken(END_MARKER_TOKEN);
                    return true;
                    // TODO: change offsets?
                } else {
                    // input tokenStream is working
                    // in principle all values are set (oddly enough)
                    return true;
                }
            }
        }
    }

    public static Token START_MARKER_TOKEN = new Token("[start]",0,0,"-start-end-marker-"),
        END_MARKER_TOKEN = new Token("[end]",0,0,"-start-end-marker-");

    public static void main(String[] args) throws Throwable {
        SKBAnalyzer a = new SKBAnalyzer(Boolean.parseBoolean(args[2]), Arrays.asList(args[1].split(",")));
        TokenStream t =a.tokenStream(args[0], new StringReader(args[3]));
        TermAttribute termAttribute = t.getAttribute(TermAttribute.class);
        while(t.incrementToken()) System.out.println("-- " + termAttribute.term());
    }

}
