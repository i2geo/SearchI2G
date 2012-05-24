package net.i2geo.index;

import org.xml.sax.InputSource;

import java.io.*;

/**
 */
public class SKBUpdateQueue {

    

    public SKBUpdateQueue(File queueDir, File rejectsDir) {
        this.queueDir = queueDir;
        queueDir.mkdirs();
        this.rejectsDir = rejectsDir;
        rejectsDir.mkdirs();
    }

    final File queueDir, rejectsDir;


    public synchronized File receiveUpdate(String mimeType, InputStream in) throws IOException {
        // read if charset in mime-type
        boolean hasCharSet = mimeType.toLowerCase().indexOf("charset=")!=-1;
        Reader reader = null;
        if(hasCharSet) {
            int l = "charset=".length();
            int p = mimeType.toLowerCase().indexOf("charset=");
            int q = mimeType.toLowerCase().indexOf(";",p);
            if(q==-1) q= mimeType.length();
            if(p+l < q) {
                reader = new InputStreamReader(in,mimeType.substring(p+l,q));
            }
        }
        if(reader==null) {
            reader = new InputStreamReader(in,"utf-8");
        }
        File f = getANewFileName();
        Writer out = new OutputStreamWriter(new FileOutputStream(f),"utf-8");
        char[] buff = new char[128]; int l=0;
        while((l=reader.read(buff,0,128))!=-1) {out.write(buff,0,l);}
        out.flush(); out.close();
        return f;
    }

    public void dumpRemainingChanges(OutputStream out) throws IOException {
        File[] files = listFilesToProcess();
        byte[] b = new byte[128]; int r = 0;
        for(int i=0,l=files.length; i<l;) {
            FileInputStream in = new FileInputStream(files[i]);
            while((r=in.read(b,0,128))!=-1) {
                out.write(b,0,r);
            }
        }
        out.flush();
    }

    File[] listFilesToProcess() {
        synchronized(this) {
            return queueDir.listFiles();
        }
    }

    public File getANewFileName() {
        for(int i=0; i<16384; i++) {
            File tentative = new File(queueDir,"request-" + i + ".xml");
            if(!tentative.isFile()) return tentative;
        }
        throw new IllegalStateException("Sorry, can't have more than 16'000 files.");
    }

    public void waitTillQueueIsEmpty() throws InterruptedException {
        synchronized(this) {
            while(queueDir.list().length>0) {
                this.wait(100);
            }
        }
    }
}
