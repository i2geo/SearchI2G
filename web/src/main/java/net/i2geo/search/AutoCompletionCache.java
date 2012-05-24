package net.i2geo.search;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;
import org.apache.log4j.Logger;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

class AutoCompletionCache {

    private int countAccess = 0;
    private transient Logger log = Logger.getLogger(TokenSearchCache.class);

    public AutoCompletionCache() {
        CacheManager manager = CacheManager.create();

        //Create a Cache specifying its configuration.

        cache = new Cache("autoCompletionCache",20000,true,false,3600,3600);
        manager.addCache(cache);
    }

    private Cache cache;

    private void tick() {
        countAccess++;
        if(countAccess % 10==0) log.info("Cache statistics: " + cache.getStatistics());
    }

    byte[] getMatchingResults(String queryString, String[] authorizedTypes, String acceptedLangs) {
        Object key = new AutoCompletionCacheKey(queryString,authorizedTypes, acceptedLangs);
        tick();
        Element elt = cache.get(key);
        if(elt!=null) return (byte[]) elt.getObjectValue();
            else return null;
    }

    void addMatchingResult(String queryString, String []authorizedTypes, String acceptedLangs, byte[] results) {
        Object key = new AutoCompletionCacheKey(queryString,authorizedTypes, acceptedLangs);
        Element elt = new Element(key,results);
        cache.put(elt);
    }

}


class AutoCompletionCacheKey {

    AutoCompletionCacheKey(String queryString, String[] authorizedTypes, String acceptedLangs) {
        list = Arrays.asList(queryString,Arrays.asList(authorizedTypes),acceptedLangs);
    }

    private List<Object> list;

    public boolean equals(Object o) {
        if(!(o instanceof AutoCompletionCacheKey)) return false;
        return ((AutoCompletionCacheKey)o).list.equals(this.list);
    }

    public int hashCode() {
        return list.hashCode();
    }
}