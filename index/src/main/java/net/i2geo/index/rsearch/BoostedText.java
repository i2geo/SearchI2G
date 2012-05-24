package net.i2geo.index.rsearch;

/** Simple class to denote a string and a boost factor
 */
class BoostedText {

    public BoostedText(String text, float boost) {
        this.text = text;
        this.boost = boost;
    }
    final float boost;
    final String text;
}
