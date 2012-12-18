package net.i2geo.api;

import java.util.List;

public interface NamesMap {

    public List getSupportedLanguages();

    public List getDefaultCommonNamesFor(String language);

    public List getAllNamesFor(String language);

}
