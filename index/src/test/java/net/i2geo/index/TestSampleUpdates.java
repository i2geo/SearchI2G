package net.i2geo.index;

import net.i2geo.index.analysis.AnalyzerPack;
import net.i2geo.index.analysis.SKBAnalyzer;
import net.i2geo.onto.GeoSkillsAccess;
import net.i2geo.api.OntType;

import java.io.File;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.index.Term;
import junit.framework.TestCase;

/** tests the application of the SampleUpdate*.xml files in this directory.
 */
public class TestSampleUpdates extends TestCase {

    private static GeoSkillsAccess access =  GeoSkillsAccess.getTestInstance();
    static {
        try {
            new File("target/index").mkdirs();
            FileUtils.cleanDirectory(new File("target/index"));
        }catch(Exception ex) { throw new IllegalStateException("can't start.",ex);}
    }
    private static IndexHome indexHome = IndexHome.getInstance("target/index",true);
    private static SKBQueryExpander queryExpander = new SKBQueryExpander(Arrays.asList(new String[]{"en","es","de"}));
    private SKBUpdateQueue updaterQueue = null;
    private SKBUpdater updater = null;

    protected void setUp() throws Exception {
        System.out.println("Status : " + indexHome.getCurrentStatus() + " for " + super.toString() );
        if(indexHome.getCurrentStatus() != IndexHome.Status.READ_READY)
            indexHome.open(true);
        this.updaterQueue = new SKBUpdateQueue(new File("target/updaterQueue"),new File("target/updaterRejects"));
        this.updater = new SKBUpdater(updaterQueue,indexHome);
        this.updater.start();
    }

    public void testApplySampleUpdates() throws Exception {
        TermQuery q;
        BooleanQuery bq;
        int numHits;

        System.out.println("Adding Nancy.");
        updaterQueue.receiveUpdate("",getClass().getResourceAsStream("/Update-addNancy.xml"));
        updaterQueue.waitTillQueueIsEmpty();

        String nancyAnalyzed = AnalyzerPack.tokenizeString(new SKBAnalyzer(false, Arrays.asList("en")), "name-en", "Nancy").get(0);
        q= new TermQuery(new Term("name-en", nancyAnalyzed));
        numHits = indexHome.getSearcher().search(q, 10).totalHits;
        assertTrue("There should be Nancy in english names.", numHits > 0);

        System.out.println("Adding Grenoble competency.");
        updaterQueue.receiveUpdate("",getClass().getResourceAsStream("/Update-addGrenobleCompetency.xml"));
        updaterQueue.waitTillQueueIsEmpty();
        bq=new BooleanQuery();
        String grenobleAnalyzed = AnalyzerPack.tokenizeString(new SKBAnalyzer(false, Arrays.asList("en")), "name-en", "Grenoble").get(0);
        bq.add(new TermQuery(new Term("name-en", grenobleAnalyzed )), BooleanClause.Occur.MUST);
        bq.add(new TermQuery(new Term("ontType", OntType.COMPETENCY.getName())), BooleanClause.Occur.MUST);
        numHits = indexHome.getSearcher().search(bq, 10).totalHits;
        assertTrue("There should be Grenoble in english names.",numHits==1);

        // add Bretagne topic 4
        System.out.println("Adding Bretagne topic.");
        updaterQueue.receiveUpdate("", getClass().getResourceAsStream("/Update-addBretagneTopic.xml"));
        updaterQueue.waitTillQueueIsEmpty();
        bq=new BooleanQuery();
        String bretagneAnalyzed = AnalyzerPack.tokenizeString(new SKBAnalyzer(false, Arrays.asList("en")), "name-en", "Bretagne").get(0);
        bq.add(new TermQuery(new Term("name-en", bretagneAnalyzed)), BooleanClause.Occur.MUST);
        bq.add(new TermQuery(new Term("ontType", OntType.TOPIC.getName())), BooleanClause.Occur.MUST);
        numHits = indexHome.getSearcher().search(bq, 10).totalHits;
        assertTrue("There should be bretagne in english names.",numHits==1);


        // add Forbach topic (! test topic !): disqualified now, would need to walk the hierarchy
        /* System.out.println("Adding Forbach topic.");
        updaterQueue.receiveUpdate("",getClass().getResourceAsStream("/Update-addForbachTopic.xml"));
        updaterQueue.waitTillQueueIsEmpty();
        bq=new BooleanQuery();
        bq.add(new TermQuery(new Term("name-en","forbach")), BooleanClause.Occur.MUST);
        bq.add(new TermQuery(new Term("ontType", OntType.TOPIC.getName())), BooleanClause.Occur.MUST);
        numHits = indexHome.getSearcher().search(bq).length();
        assertTrue("There should be forbach in english names.",numHits==1); */

        // remove Grenoble (6)
        System.out.println("Removing competency Grenoble.");
        updaterQueue.receiveUpdate("",getClass().getResourceAsStream("/Update-removeGrenoble.xml"));
        updaterQueue.waitTillQueueIsEmpty();
        bq=new BooleanQuery();
        bq.add(new TermQuery(new Term("name-en", grenobleAnalyzed)), BooleanClause.Occur.MUST);
        numHits = indexHome.getSearcher().search(bq, 10).totalHits;
        assertTrue("Grenoble should be gone.",numHits==0);

        // delete Forbach and Forbach_r
        System.out.println("Removing topic Forbach.");
        String forbachAnalyzed = AnalyzerPack.tokenizeString(new SKBAnalyzer(false, Arrays.asList("en")), "name-en", "Forbach").get(0);
        updaterQueue.receiveUpdate("",getClass().getResourceAsStream("/Update-removeForbach.xml"));
        updaterQueue.waitTillQueueIsEmpty();
        bq=new BooleanQuery();
        bq.add(new TermQuery(new Term("name-en", forbachAnalyzed)), BooleanClause.Occur.MUST);
        numHits = indexHome.getSearcher().search(bq,10).totalHits;
        assertTrue("Forbach should be gone.",numHits==0);
    }

    protected void tearDown() throws Exception {
        indexHome.close();
        this.updater.stop();
    }

    


}
