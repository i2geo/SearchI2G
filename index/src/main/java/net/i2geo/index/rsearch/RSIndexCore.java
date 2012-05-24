package net.i2geo.index.rsearch;

import org.apache.lucene.analysis.Analyzer;

/** Connection to the resource-index.
 */
public class RSIndexCore {

    public static RSIndexCore getInstance() {
        if(instance==null) {
            instance = new RSIndexCore();
        }
        return instance;
    }

    private static RSIndexCore instance = null;

}
