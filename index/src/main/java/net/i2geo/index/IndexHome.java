package net.i2geo.index;

import net.i2geo.api.GeoSkillsConstants;
import net.i2geo.api.MatchingResourcesCounter;
import net.i2geo.index.analysis.AnalyzerPack;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.index.*;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.highlight.*;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Arrays;

import net.i2geo.index.analysis.SKBAnalyzer;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

/** Simple class to configure the index and use it
 */
public class IndexHome {

    private static IndexHome singleInstance = null;
    private boolean shouldUpdateReader = false;
    private long lastIndexed = System.currentTimeMillis();
    private MatchingResourcesCounter matchingResourcesCounter = null;
    Log log = LogFactory.getLog(IndexHome.class);
    public static final List<String> supportedLanguages = Arrays.asList(new String[] {"en","ch","cs","de","es","eu","fr","mk","nl","pt","ru","zh"});

    public static IndexHome getInstance() {
        return singleInstance;
    }

    public static synchronized IndexHome getInstance(String pathToIndex) {
        return getInstance(pathToIndex,true);
    }
    public static synchronized IndexHome getInstance(String pathToIndex, boolean indexIfNeedBe) {
        if(singleInstance!=null) return singleInstance;
        singleInstance = new IndexHome(pathToIndex,indexIfNeedBe);
        return singleInstance;
    }


    public IndexHome(final String pathToIndex) {
        this(pathToIndex,true);
    }

    public IndexHome(String pathToIndex, boolean doIndexIfNeedBe) {
        this.pathToIndex = pathToIndex;
        open(doIndexIfNeedBe);
    }

    public void open(boolean doIndexIfNeedBe) {
        try {
            startReading(doIndexIfNeedBe);
            currentStatus = Status.READ_READY;
            BooleanQuery.setMaxClauseCount(65536);
        } catch(Exception ex) {
            currentStatus = Status.NEEDS_INDEXING;
            log.warn(ex);
        }
        if(currentStatus == Status.NEEDS_INDEXING && doIndexIfNeedBe) {
            startIndexingProcess(false);
        }
    }

    void computeStatus(boolean doIndexIfNeedBe) {
        try {
            startReading(doIndexIfNeedBe);
            if(doIndexIfNeedBe) // an index has ben created 
                currentStatus = Status.READ_READY;
        } catch(Exception ex) {
            currentStatus = Status.NEEDS_INDEXING;
            log.warn(ex);
        }
    }

    private IndexWriter writer = null;
    private IndexReader reader;
    private IndexSearcher searcher;

    private GeoSkillsIndexer currentIndexer = null;
    private Status currentStatus = Status.NEEDS_CHECKING;

    private boolean currentlyWriting = false;
    private Analyzer analyzer = createAnalyzer(Arrays.asList(languages));

    // configuration constants
    private String pathToIndex = "target/index";
    private File pathToBackup = null;
    private File logFile = null;
    private static String[] languages = new String[] {"en","fr","de","es","nl"};

    boolean hasNoFiles() {
        File f = new File(pathToIndex);
        if(!f.exists()) return true;
        String[] files = f.list();
        if(files==null) return true;
        if(files.length==0) return true;
        return false;
    }

    void backItUp() {
        if(pathToBackup==null)
            pathToBackup = new File(new File(pathToIndex).getParentFile(),"index-backup");
        if(!pathToBackup.isDirectory() || pathToBackup.list()==null || pathToBackup.list().length==0) {
            log.warn("Index directory empty, resigning backup.");
            return;
        }

        log.info("Will back-up index : to " + pathToBackup);
        try {
            boolean backupWasThere = pathToBackup.isDirectory();
            if(!backupWasThere) {
                log.info("Creating backup directory.");
                if(pathToBackup.mkdirs()==false)
                    throw new IllegalStateException("Can't make backup, can't create directory \"" + pathToBackup + "\".");
            }
            Exception ex = null;
            try { startReading(false);} catch(Exception e) { e.printStackTrace(); ex=e;}
            if(ex==null 
                    && new File(pathToIndex).isDirectory() && new File(pathToIndex).listFiles().length>0) {
                stopWriting(); reader.close();
                FileUtils.cleanDirectory(pathToBackup);
                FileUtils.copyDirectory(new File(pathToIndex),pathToBackup,true);
                startReading(false);
            } else {
                log.info("No backup would be worth doing.");
            }
        } catch(Exception ex) {throw new IllegalStateException("couldn't backup",ex);}
        log.info("end of backup routine.");
    }

    public void recoverFromBackup() {
        if(pathToBackup==null)
            pathToBackup = new File(new File(pathToIndex).getParentFile(),"index-backup");
        log.info("Recovering from backup.");
        System.err.println("Recovering from backup.");
        try { FileUtils.copyDirectory(pathToBackup,new File(pathToIndex)); }
            catch(Exception ex) {
                throw new IllegalStateException("couldn't recover backup \"" + pathToBackup + "\"",ex);
            }
        log.info("Recovered from backup.");
        System.err.println("Recovered from backup.");
    }

    public void emptyIt() {
        if(writer!=null) {
            try{writer.close(); writer = null;}catch(Exception ex) {log.warn(ex);}
        }
        if(new File(pathToIndex).listFiles().length==0)
            return;
        try {
        reader = IndexReader.open(FSDirectory.open(new File(pathToIndex)));
            for(int i=0; i<reader.maxDoc(); i++) {
                try {
                    reader.deleteDocument(i);
                } catch (IOException e) {
                    log.info("Deletion failed: " + e);
                }
            }
            reader.close();
            reader = IndexReader.open(FSDirectory.open(new File(pathToIndex)));
        } catch (Exception e) {
            log.warn("Deletion failed: ",e);
        }
    }

    public String computeMatchedField(int docNum, Document doc, Analyzer analyzer, Query query) throws IOException {
        //System.out.println("----- computing matched field for query " + query + " on document " + doc.get("uri"));
        query = query.rewrite(this.reader);
        String found = null;
        float maxScore = 0;
        String text = null;
        try {
            for(Fieldable f: doc.getFields()) {
                QueryScorer scorer = new QueryScorer(query,reader,f.name());
                if(!f.name().startsWith("name-")) continue;
                //System.out.println("Measuring field " + f.name() + ": " + f.stringValue());
                text = f.stringValue();
                TokenStream tokenStream = analyzer.tokenStream(f.name(),new StringReader(text));
                        //TokenSources.getAnyTokenStream(reader,docNum, f.name(), doc, analyzer);
                SimpleHTMLFormatter htmlFormatter = new SimpleHTMLFormatter();
                Highlighter highlighter = new Highlighter(htmlFormatter, scorer);
                TextFragment[] frags = highlighter.getBestTextFragments(tokenStream, text, false, 1);
                if(frags==null || frags.length==0) continue;
                float score = frags[0].getScore();
                //System.out.println("Score: " + score);
                if(score > maxScore) {
                    maxScore = score;
                    found = frags[0].toString();
                }
            }
        } catch(Exception ex) {System.err.println("for text \"" + text + "\"." ); ex.printStackTrace();}
        return found;
    }

    public void setMatchingResourcesCounter(MatchingResourcesCounter counter) {
        this.matchingResourcesCounter = counter;
    }

    public MatchingResourcesCounter getMatchingResourcesCounter() {
        return this.matchingResourcesCounter;
    }


    // read and write cycles
    // the index can only be in one of the positions: currentlyWriting true or false


    public static class Status {
        public static final Status
            CURRENTLY_INDEXING = new Status("Currently indexing."),
            BROKEN = new Status("Index is broken."),
            READ_READY = new Status("Read ready."),
            NEEDS_INDEXING = new Status("Needs indexing"),
            CLOSED = new Status("Closed."),
            NEEDS_CHECKING = new Status("Needs checking.");

        private Status(String name) {this.name = name; }
        private final String name;
        public String toString() {
            return super.toString() + ": " + name;
        } 
    }



    public synchronized void startWriting() {
        File indexDir = new File(pathToIndex);
        boolean didExist = indexDir.isDirectory() && indexDir.list().length >0;
        if(!didExist) indexDir.mkdirs();
        startWriting(!didExist);
    }


    private synchronized void startIndexingProcessAndWaitForIt(boolean withBackups) {
        doIndexingProcess(withBackups);
        lastIndexed = System.currentTimeMillis();        
    }

    public long getLastIndexed() { return lastIndexed; }

    public Thread startIndexingProcess() {
        return startIndexingProcess(true);
    }
    public Thread startIndexingProcess(final boolean withBackups) {
        if(Status.CURRENTLY_INDEXING == currentStatus)
            throw new IllegalStateException("Can't re-index!");
        System.err.println("Launching indexing process.");
        currentStatus = Status.CURRENTLY_INDEXING;
        if(currentIndexer != null)
            throw new IllegalStateException("An index is already running, please see log.");
        Thread worker = new Thread("IndexHome Starter") { public void run() {
            doIndexingProcess(withBackups);
            lastIndexed = System.currentTimeMillis();
        }};
        worker.start();
        return worker;

    }

    private void doIndexingProcess(boolean withBackups) {
        try {new File(pathToIndex).mkdirs();} catch(Exception ex) {ex.printStackTrace();}
        System.err.println("Starting indexing process.");
        currentIndexer = new GeoSkillsIndexer(this);
        logFile = currentIndexer.getLogFile();
        try {
            Thread.sleep(2000);
            currentIndexer.run();
            startReading();
            currentStatus = Status.READ_READY;
        } catch(Throwable ex) {
            System.err.println("doIndexingProcess failed.");
            ex.printStackTrace();
            try {
                try {stopWriting();} catch(Exception x) {}
                recoverFromBackup();
                startReading(false);
                currentStatus = Status.READ_READY;
            } catch(Exception e ) {
                e.printStackTrace();
                currentStatus = Status.BROKEN;
            }
            currentlyWriting = false;
        }
        finally {
            System.err.println("Indexing process finished, notifying.");
            currentIndexer = null;
            // currentStatus = Status.READ_READY;
            //IndexHome.this.startReading();
            synchronized(this) { IndexHome.this.notify(); }
        }
        System.err.println("Finished doIndexing.");

    }

    public synchronized void startWriting(boolean create) {
        try {
            if(currentlyWriting && writer!=null) return;
            currentlyWriting = true;
            //if( reader!=null) reader.close();
            //if( searcher!=null ) searcher.close(); TENTATIVE NO CLOSE READER
            IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_35, analyzer);
            writer = new IndexWriter(FSDirectory.open(new File(pathToIndex)),config);
            currentlyWriting = true;
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalStateException(e);
        }
    }

    public synchronized void stopWriting() {
        //new Exception("Where am I from?").printStackTrace();
        try {
            if(!currentlyWriting) return;
            if(writer!=null) {
                //writer.flush();
                writer.close();
                writer = null;
            }
            currentlyWriting = false;
            currentStatus = Status.NEEDS_CHECKING;
            shouldUpdateReader = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private synchronized void startReading() {
        startReading(true);
    }
    private synchronized void startReading(boolean doIndexIfNeedBe) {
        try {
            boolean anIndexWasThere = new File(pathToIndex).isDirectory()
                    && new File(new File(pathToIndex),"segments.gen").exists();
            if(!anIndexWasThere) {
                currentStatus = Status.NEEDS_INDEXING;
                if(!doIndexIfNeedBe) return;
            } else {
                // check status
                reader = IndexReader.open(FSDirectory.open(new File(pathToIndex)));
                searcher = new IndexSearcher(reader);
                int number = reader.numDocs();
                if(number>10) currentStatus = Status.READ_READY;
            }
            if(currentStatus != Status.READ_READY) {
                if(doIndexIfNeedBe) {
                    if(currentIndexer!=null)
                        while(currentStatus == Status.CURRENTLY_INDEXING && currentIndexer!=null) {
                            try { synchronized(this){this.wait(200);} }
                            catch(InterruptedException ex) {ex.printStackTrace(); }
                        }
                    else startIndexingProcessAndWaitForIt(false);

                    //try{ writer.flush();} catch(Exception ex) { }
                    //try{ writer.close();} catch(Exception ex) {}
                    currentlyWriting = false;
                } else {
                    //startWriting(true);
                    //try{ writer.flush();} catch(Exception ex) {log.warn("can't flush: " + ex); }
                    //try{ writer.close();} catch(Exception ex) {log.warn("can't flush: " + ex); }
                }
            }
            if( reader!=null)  try {reader.close();}
                catch (IOException e) {e.printStackTrace();}
            if( searcher!=null ) try {searcher.close();}
                catch (IOException e) {e.printStackTrace();}
            FSDirectory directory = FSDirectory.open(new File(pathToIndex));
            reader = IndexReader.open(directory);
            searcher = new IndexSearcher(reader);
            currentStatus = Status.READ_READY;
            if(reader.numDocs()<10) {
                currentStatus = Status.NEEDS_INDEXING;
                throw new IllegalStateException("Index has only " + reader.numDocs() + " documents. That's too small it needs to be recreated.");
            } else {
                System.out.println(reportStatus());
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalStateException(e);
        }
    }

    public void commitDeletions() throws Exception {
        updateReader();
    }

    public Analyzer createAnalyzer(List<String> languages) {
        return new SKBAnalyzer(false,languages);
    }

    // report methods

    public String reportStatus() {
        StringBuffer buff = new StringBuffer();
        buff.append("==== INDEX STATUS =====\n");
        buff.append("Path: ").append(pathToIndex).append("\n");
        if(currentStatus == Status.READ_READY) {
            buff.append("Index is read ready.\n");
            buff.append(this.getReader().numDocs()).append(" documents.\n");
            String modificationDate = "unknown";
            try {   TermDocs docs = reader.termDocs(new Term("isModifDate","yes"));
                    docs.next();
                    if(!reader.isDeleted(docs.doc()))
                        modificationDate = reader.document(docs.doc()).get("modificationDate");}
                catch(Exception ex) {ex.printStackTrace();System.err.println("Ignored last exception.");}
            buff.append("Index has " + reader.numDocs() + " document and was last updated on " + modificationDate);
        } else if (currentStatus == Status.CURRENTLY_INDEXING) {
            buff.append("Currently indexing, please see <a href=\"log\">log</a> ("+currentIndexer.getLogFile().length()+" bytes).");

        } else if (currentStatus == Status.NEEDS_INDEXING) {
            buff.append("The indexing needs to be done.");
        } else if (currentStatus == Status.NEEDS_CHECKING) {
            buff.append("It should be checked if indexing is needed.");
        } else if (currentStatus == Status.CLOSED) {
            buff.append("Index is closed.");
        } else {
            buff.append("Index Status unknown: " + currentStatus);
        }
        return buff.toString();
    }

    private synchronized void updateReader() {
        try {
            if(reader!=null) {
                try {
                    log.info("Updating reader and searcher ("+ reader.numDocs()+" docs).");
                } catch (Exception e) {}
                reader.close();
                searcher.close();
            }
            reader = IndexReader.open(FSDirectory.open(new File(pathToIndex)));
            searcher = new IndexSearcher(reader);
            int number = reader.numDocs();
            log.info("Updated reader and searcher ("+ number+" docs).");
            if(number>10) currentStatus = Status.READ_READY;
        } catch (IOException e) {
            e.printStackTrace();
        }
        shouldUpdateReader = false;
    }



    // =================== Index access =====================
    public IndexSearcher getSearcher() {
        //if(currentlyWriting) throw new IllegalStateException("Currently writing.");
        /* TENTATIVE while(currentStatus == Status.CURRENTLY_INDEXING) {
            synchronized(this) {
                try {
                    this.wait(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }*/
        if(currentStatus == Status.BROKEN)
            throw new IllegalStateException("Sorry can't read, index is broken.");
        if(searcher==null || shouldUpdateReader) updateReader();
        return searcher;
    }
    public IndexReader getReader() {
        if(shouldUpdateReader) updateReader();
        //if(currentlyWriting) throw new IllegalStateException("Currently writing.");
        return reader;
    }


    public Document getDocForUri(String uri) {
        try {
            if(uri!=null && uri.startsWith("#")) uri= GeoSkillsConstants.GEOSKILLS_BASE_URI + uri;
            Term term = new Term("uri",uri);
            TermDocs termDocs = reader.termDocs(term);
            boolean hasNext =termDocs.next();
            if(!hasNext) {
                termDocs = reader.termDocs(new Term("uri-weak", GSIUtil.uriToName(uri,false)));
                hasNext = termDocs.next();
            }
            if(!hasNext) return null;
            return reader.document(termDocs.doc());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public IndexWriter getWriter() {
        if(!currentlyWriting) throw new IllegalStateException("Not currently writing.");
        return writer;
    }

    public File getLogFile() {
        return logFile;
    }

    public Status getCurrentStatus() {
        if(currentStatus == Status.NEEDS_CHECKING) {
            computeStatus(false);
        }
        return currentStatus;
    }

    protected void finalize() {
        try {
            if(currentlyWriting) {
                currentStatus = Status.CURRENTLY_INDEXING;
                while(currentIndexer!=null) {
                    try { synchronized(this){this.wait(200);} }
                        catch(InterruptedException ex) {ex.printStackTrace(); }
                }
                GSILogger.log("Closing indexHome " + this + " and writer " + writer);
                //writer.flush();
                writer.close();
                currentlyWriting = false;
            } else {
                reader.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close() throws Exception {
        //new Exception("Now closing IndexHome").printStackTrace();
        if(writer!=null) {
            try {writer.close(true); } catch(Exception ex) {}
        } else {
            try {reader.close();
            searcher.close();} catch(Exception ex) {}
        }
        currentStatus = Status.CLOSED;
    }


}
