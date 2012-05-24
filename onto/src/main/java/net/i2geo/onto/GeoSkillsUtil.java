package net.i2geo.onto;

import java.util.Set;
import java.util.HashSet;

/**
 */
public class GeoSkillsUtil {
    public static String shortenName(String maybeFullURI, String ontBaseU) {
        if(maybeFullURI==null) return null;
        if(maybeFullURI.startsWith(ontBaseU))
            return maybeFullURI.substring(ontBaseU.length());
        return maybeFullURI;
    }
    public static Set<String> shortenName(Set<String> maybeFullURIs, String ontBaseU) {
        if(maybeFullURIs==null) return null;
        Set<String> r = new HashSet<String>(maybeFullURIs.size());
        for(String s: maybeFullURIs) {
            r.add(shortenName(s,ontBaseU));
        }
        return r;
    }}
