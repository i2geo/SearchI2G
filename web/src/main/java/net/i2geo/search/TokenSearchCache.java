package net.i2geo.search;

import net.i2geo.api.SkillItem;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;
import org.apache.log4j.Logger;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

class TokenSearchCache {

    private int countAccess = 0;
    private transient Logger log = Logger.getLogger(TokenSearchCache.class);

    public TokenSearchCache() {
        CacheManager manager = CacheManager.create();

        //Create a Cache specifying its configuration.

        cache = new Cache("tokenSearchCache",20000,false,false,3600,3600);
        manager.addCache(cache);
    }

    private Cache cache;

    private void tick() {
        countAccess++;
        if(countAccess % 10==0) log.info("Cache statistics: " + cache.getStatistics());
    }

    SkillItem getSkillItemRendering(String uri, List<String> acceptedLangs) {
        SkillItemRenderingKey key = new SkillItemRenderingKey(uri,acceptedLangs);
        Element elt = cache.get(key);
        if(elt!=null) return (SkillItem) elt.getObjectValue();
        else return null;
    }

    void addSkillItemRendering(String uri, List<String> acceptedLangs, SkillItem item) {
        Element elt = new Element(new SkillItemRenderingKey(uri,acceptedLangs), item);
        cache.put(elt);
    }



    SkillItem[] getMatchingSkillsItems(String queryString, String[] authorizedTypes, String acceptedLangs) {
        Object key = new MatchingSkillItemKey(queryString,authorizedTypes, acceptedLangs);
        tick();
        Element elt = cache.get(key);
        if(elt!=null) return (SkillItem[]) elt.getObjectValue();
            else return null;
    }

    void addMatchingSkillsItems(String queryString, String []authorizedTypes, String acceptedLangs, SkillItem[] items) {
        Object key = new MatchingSkillItemKey(queryString,authorizedTypes, acceptedLangs);
        Element elt = new Element(key,items);
        cache.put(elt);
    }

}

class SkillItemRenderingKey {
    SkillItemRenderingKey(String uri, List<String> langs) {
        this.list = Arrays.asList(uri,langs);
    }
    final List<Object> list;
    public boolean equals(Object o) {
        return (o instanceof SkillItemRenderingKey) && this.list.equals(((SkillItemRenderingKey) o).list); 
    }

    public int hashCode() {
        return list.hashCode();
    }
}

class MatchingSkillItemKey {

    MatchingSkillItemKey(String queryString, String[] authorizedTypes, String acceptedLangs) {
        list = Arrays.asList(queryString,Arrays.asList(authorizedTypes),acceptedLangs);
    }

    private List<Object> list;

    public boolean equals(Object o) {
        if(!(o instanceof MatchingSkillItemKey)) return false;
        return ((MatchingSkillItemKey)o).list.equals(this.list);
    }

    public int hashCode() {
        return list.hashCode();
    }
}