package net.i2geo.index;

import org.apache.lucene.search.*;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import junit.framework.TestCase;

/** Simle test taken from LuceneBook which indicates well how to do multisearching which might
 * turn out to be useful 
 */
public class TestMultiSearcher extends TestCase {
  private IndexSearcher[] searchers;

    // TODO: try with one in RAM and one on disk.
  public void setUp() throws Exception {
    String[] animals = { "aardvark", "beaver", "coati",
                       "dog", "elephant", "frog", "gila monster",
                       "horse", "iguana", "javelina", "kangaroo",
                       "lemur", "moose", "nematode", "orca",
                       "python", "quokka", "rat", "scorpion",
                       "tarantula", "uromastyx", "vicuna",
                       "walrus", "xiphias", "yak", "zebra"};
      Analyzer analyzer = new WhitespaceAnalyzer();
         Directory aTOmDirectory = new RAMDirectory();
         Directory nTOzDirectory = new RAMDirectory();
         IndexWriter aTOmWriter = new IndexWriter(aTOmDirectory,
                                                 analyzer, true);
         IndexWriter nTOzWriter = new IndexWriter(nTOzDirectory,
                                                 analyzer, true);
         for (int i=0; i < animals.length; i++) {
           Document doc = new Document();
           String animal = animals[i];
           doc.add(new Field("animal", animal, Field.Store.YES, Field.Index.TOKENIZED));
           if (animal.compareToIgnoreCase("n") < 0) {
             aTOmWriter.addDocument(doc);
           } else {
             nTOzWriter.addDocument(doc);
           }
         }
         aTOmWriter.close();
         nTOzWriter.close();
         searchers = new IndexSearcher[2];
         searchers[0] = new IndexSearcher(aTOmDirectory);
         searchers[1] = new IndexSearcher(nTOzDirectory);
       }
       public void testMulti() throws Exception {
         MultiSearcher searcher = new MultiSearcher(searchers);
         Query query = new TermQuery(new Term("animal","dog"));
         Hits hits = searcher.search(query);
         assertEquals("dogs' there",1,hits.length());
         query = new RangeQuery(new Term("animal", "h"),
                        new Term("animal", "t"), true);
           hits = searcher.search(query);
         assertEquals("12 matches accross two indexes", 13, hits.length());
       }
}

