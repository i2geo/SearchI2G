package net.i2geo.search;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

class SKBUtils {

    public static List<String> parseLanguages(String langs, int max) {
        StringTokenizer stok = new StringTokenizer(langs,", ",false);
        List<String> l = new ArrayList<String>(max);
        while(stok.hasMoreTokens()) {
            String lang = stok.nextToken();
            if(lang.length()>3 || lang.contains("-")) {
                int p = lang.indexOf('-');
                if(p>-1) lang = lang.substring(0,p);
            }
            if(! l.contains(lang)) l.add(lang);
        }
        return l;
    }

}
