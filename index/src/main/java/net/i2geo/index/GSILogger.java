package net.i2geo.index;

import org.apache.log4j.*;

import java.io.*;

/**
 */
public class GSILogger {

    static void log(String msg) {
        System.err.println(msg);
        if(logFileOut!=null) {
            try {
                logFileOut.write(msg);
                logFileOut.write('\n');
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    static void openExtraLogFileAppender() throws IOException {
        GeoSkillsIndexer.getLogFile().getParentFile().mkdirs();
        logFileOut = new OutputStreamWriter(new FileOutputStream(GeoSkillsIndexer.getLogFile()),"utf-8");
        /*appender = new WriterAppender(new PatternLayout("%d %-5p %c{2} %x - %m\n"),logFileOut);
        System.err.println("Indexing process logging to " + GeoSkillsIndexer.getLogFile()+ ".");
        System.err.println("with appender " + appender);
        //log.setLevel(Level.INFO);
        Logger.getLogger("org.mindswap.pellet").removeAllAppenders();
        Logger.getLogger("org.mindswap.pellet").addAppender(appender);
        //Logger.getLogger("org.mindswap.pellet").addAppender(sysout);
        Logger.getLogger("net.i2geo.index").setLevel(Level.INFO);

        Logger.getLogger("net.i2geo.index").removeAllAppenders();
        Logger.getLogger("net.i2geo.index").addAppender(appender);
        //Logger.getLogger("net.i2geo.index").addAppender(sysout);
        Logger.getLogger("net.i2geo.index").setLevel(Level.INFO); */
    }

    static void closeExtraLogFileAppender() {
        try {
            try{logFileOut.flush();logFileOut.close();} catch(Exception ex) {}
            logFileOut = null;

            /* Logger.getLogger("org.mindswap.pellet").removeAllAppenders();
            Logger.getLogger("net.i2geo.index").removeAllAppenders();
            appender = null;
            System.err.println("Closed appender " + appender);

            Appender sysout = new WriterAppender(new PatternLayout("%d %-5p %c{2} %x - %m\n"),new OutputStreamWriter(System.out,"utf-8"));
            Logger.getLogger("org.mindswap.pellet").addAppender(sysout);
            Logger.getLogger("net.i2geo.index").addAppender(sysout); */

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //private static Appender appender = null;
    private static Writer logFileOut;

}
