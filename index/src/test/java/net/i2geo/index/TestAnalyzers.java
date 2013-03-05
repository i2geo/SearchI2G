package net.i2geo.index;

import junit.framework.TestCase;
import net.i2geo.index.analysis.AnalyzerPack;
import net.i2geo.onto.GeoSkillsAccess;
import net.i2geo.api.OntType;
import net.i2geo.index.analysis.SKBAnalyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.highlight.*;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.index.Term;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.commons.io.FileUtils;
import org.apache.lucene.util.Version;

import java.util.*;
import java.io.File;
import java.net.URLClassLoader;

/** A batch of test-queries directly operated on the current GeoSkills. This still assumes the index is built before.
 */
public class TestAnalyzers extends TestCase {

    public TestAnalyzers(String name) { super(name); }

    @Override
    protected void setUp() throws Exception {

    }

    public void testStartAndEndAnalyzer() {
        String aPhrase = "I draw circles";
        List<String> tokens = AnalyzerPack.tokenizeString(new SKBAnalyzer(true, Arrays.asList("en")), "name-en", aPhrase);
        assertEquals("The End should be found.",   SKBAnalyzer.END_MARKER_TOKEN,   tokens.get(tokens.size()-1));
        assertEquals("The Start should be found.", SKBAnalyzer.START_MARKER_TOKEN, tokens.get(0));
        assertEquals("SKBAnalyzer with 3 words should give 5 tokens.", tokens.size(), 3);
    }
}
