package net.i2geo.index.rsearch;

import net.i2geo.index.IndexHome;
import net.i2geo.api.SKBServiceContext;
import net.i2geo.onto.GeoSkillsAccess;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.index.Term;

/** Little command-line utility to ask for the query and expand it under the eyes
 */
public class RSQueryExpanderPlayer {

    public static void main(String[] args) throws Exception {
        IndexHome home = IndexHome.getInstance("target/index");
        RSearchContext context = new RSearchContext(Arrays.asList(System.getProperty("user.language")));
        RSOntologyAccess onto = new RSOntologyAccess(GeoSkillsAccess.getInstance(),home);
        onto.open();
        RSearchQueryExpander exp = new RSearchQueryExpander(context,home,onto);
        List<TermQuery> qs = new ArrayList<TermQuery>();
        for(int i=0; i<args.length; i++) {
            System.out.println("Received : \"" + args[i] + "\".");
            qs.add(new TermQuery(new Term("text",args[i])));
        }
        Query q = exp.expandToLuceneQuery(qs);
        System.out.println("Expanded to " + q);
    }

}
