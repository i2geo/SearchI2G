package net.i2geo.api;

import javax.servlet.ServletContext;

/** Simple map from accepted languages to skb18n.
 */
public interface SKBServiceContextProvider {

    public SKBServiceContext tryToGetOrMakeServiceForLangs(String accLangs);

}
