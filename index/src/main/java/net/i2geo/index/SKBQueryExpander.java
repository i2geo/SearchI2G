package net.i2geo.index;

import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.search.*;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.WhitespaceAnalyzer;

import java.util.*;
import java.io.StringReader;
import java.io.IOException;

import net.i2geo.index.analysis.SKBAnalyzer;

/**
 */
public class SKBQueryExpander {


    public SKBQueryExpander(List<String> supportedLanguages) {
        this.supportedLanguages = supportedLanguages;
        this.analyzerEndToEnd = new SKBAnalyzer(true,supportedLanguages);
        this.analyzerSimple = new SKBAnalyzer(false,supportedLanguages);
    }

    private Analyzer analyzerEndToEnd,analyzerSimple;
    private List<String> supportedLanguages;


    private Query varyQueryLanguage(Query query, String language) {
        Query q = null;
        if(query instanceof TermQuery) {
            String field = ((TermQuery) query).getTerm().field();
            if("name".equals(field))
                q = new TermQuery(new Term("name-" + language,((TermQuery)query).getTerm().text()));
            else
                System.err.println("Can't vary query on field " + field + ", dropping variation.");
        } else if (query instanceof FuzzyQuery) {
            FuzzyQuery fq = (FuzzyQuery) query;
            if("name".equals(fq.getTerm().field()))
                q = new FuzzyQuery(new Term("name-" + language,fq.getTerm().text()),fq.getMinSimilarity(),fq.getPrefixLength());
        } else if (query instanceof PrefixQuery) {
                    PrefixQuery fq = (PrefixQuery) query;
                    if("name".equals(fq.getPrefix().field()))
                        q = new PrefixQuery(new Term("name-" + language,fq.getPrefix().text()));
        } else if (query instanceof PhraseQuery) {
            PhraseQuery fq = (PhraseQuery) query;
            for(int i=0, l=fq.getTerms().length; i<l; i++) {
                if("name".equals(fq.getTerms()[i].field())) {
                    fq.getTerms()[i] = new Term("name-"+language, fq.getTerms()[i].text());
                }
            }
            q =  fq;
        }  else if (query instanceof BooleanQuery) {
            BooleanQuery newQ = new BooleanQuery();
            for(BooleanClause c: (List<BooleanClause>) (((BooleanQuery)query).clauses())) {
                BooleanClause newC = new BooleanClause(this.varyQueryLanguage(c.getQuery(),language),c.getOccur());
                newQ.add(newC);
            }
            q = newQ;
        }
        if(q==null) q = query;
        q.setBoost(query.getBoost());
        return q;
    }
    /* public Query expandQuery(String inputQuery, String[] authorizedTypes, String[] languages) throws ParseException {
        BooleanQuery bq = new BooleanQuery();
        Query q = this.expandQuery(inputQuery, authorizedTypes, languages,1);
        q.setBoost(1.0f);
        bq.add(new BooleanClause(q,BooleanClause.Occur.SHOULD));
        q = this.expandQuery(inputQuery, authorizedTypes, languages,0);
        q.setBoost(0.5f);
        bq.add(q,BooleanClause.Occur.SHOULD);
        return bq;
    }*/

    public Query expandQuery(String inputQuery, String[] authorizedTypes, List<String> languages, boolean withPrefix) throws ParseException {
        List<String> langs = repackLanguages(languages);
        String mainField = "name";
        BooleanQuery fullQuery = new BooleanQuery();

        // highest: the URI alone
        Query q = new TermQuery(new Term("uri",GSIUtil.makeItGSURI(inputQuery)));
        q.setBoost(1000);
        fullQuery.add(q, BooleanClause.Occur.SHOULD);
        
        // high: phrase query end-to-end in own language with no stemming (degrades with holes in it)
        List<String> words = extractWords(inputQuery, "x-all",new WhitespaceAnalyzer());
        SpanTermQuery[] spq = new SpanTermQuery[words.size()];
        Iterator<String> it = words.iterator();
        for(int i=0,l=words.size(); i<l; i++) {
            spq[i] = new SpanTermQuery(new Term(mainField + "-" + langs.get(0),it.next()));
        }
        q = new SpanNearQuery(spq,20,true);
        q.setBoost(5.0f);
        fullQuery.add(q, BooleanClause.Occur.SHOULD);


        // then full-sentence in all languages with stemming
        int i=0;
        if(!words.isEmpty()) for(String lang: langs) {
            words = extractWords(inputQuery, lang,analyzerEndToEnd);
            PhraseQuery pq = new PhraseQuery();
            for(String word : words) {
                pq.add(new Term(mainField + "-" + lang,word));
            }
            pq.setBoost(2.5f+ 0.5f/(i +1));
            fullQuery.add(pq,BooleanClause.Occur.SHOULD);
            i++;
        }

        // then prefixed-words in all languages
        i=0;
        for(String lang: langs) {
            for(String word : words) {
                words = extractWords(inputQuery, lang,analyzerSimple);
                Query pq;
                if(withPrefix)
                    pq = new PrefixQuery(new Term(mainField,word));
                else
                    pq = new TermQuery(new Term(mainField,word));
                fullQuery.add(varyQueryLanguage(pq,lang),BooleanClause.Occur.SHOULD);
                pq.setBoost(1.2f+ 0.5f/(i++ +1));
            }
        }

        // then words approximations in all languages
        i=0;
        for(String lang: langs) {
            for(String word : words) {
                words = extractWords(inputQuery, langs.get(0),analyzerSimple);
                FuzzyQuery fq = new FuzzyQuery(new Term(mainField,word),0.8f);
                fullQuery.add(varyQueryLanguage(fq,lang),BooleanClause.Occur.SHOULD);
                fq.setBoost(1.0f+ 0.5f/(i++ +1));
            }
        }


        BooleanQuery query = new BooleanQuery();
        query.add(new BooleanClause(fullQuery, BooleanClause.Occur.MUST));
        if(authorizedTypes!=null) {
            boolean wasSomeType = false;
            BooleanQuery typesQ = new BooleanQuery();
            int l;
            for(i=0,l=authorizedTypes.length; i<l; i++) {
                String type = authorizedTypes[i];
                if(type==null || type.length()==0) continue;
                type = type.trim();
                if(type.length()==0) continue;
                wasSomeType = true;
                typesQ.add(new TermQuery(new Term("ontType",type)), BooleanClause.Occur.SHOULD);
            }
            if(wasSomeType) query.add(typesQ, BooleanClause.Occur.MUST);
        }
        return query;
    }

    private List<String> extractWords(String input, String language, Analyzer a) {
        List<String> s = new ArrayList<String>(8);
        try {
            TokenStream ts = a.tokenStream("name-" + language, new StringReader(input));
            TermAttribute termAttribute = ts.getAttribute(TermAttribute.class);
            while(ts.incrementToken()) {
                String term  = termAttribute.term();
                boolean hasNonZero = false;
                for(int i=0; i< term.length(); i++)
                    if(term.charAt(i)!=0) { hasNonZero= true; break; }
                if(!hasNonZero) continue;
                s.add(term);
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalStateException("impossible to have this exception",e);
        }
        return s;
    }

    private List<String> repackLanguages(List<String> l) {
        int length=0;
        List<String> r = new ArrayList<String>(5);
        for(Iterator<String> it = l.iterator(); it.hasNext() && length<4; ) {
            String s = it.next();
            if(s==null) { continue; }
            s = s.trim();
            if(s.length()>2 && !"x-all".equals(s)) s=s.substring(0,2);
            if(!supportedLanguages.contains(s)) continue;
            r.add(s);
            length++;
        }

        if(r.isEmpty()) r.add("en");
        r.add("x-all");
        return r;
    }
}
