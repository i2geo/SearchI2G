package net.i2geo.skillstextbox.client;

import net.i2geo.api.SkillItem;


/** A class to contain the information relevant to the match of a partially input
 * string and a skill considered matching this item.
 */
public class SkillItemMatch {

    /** The item that this match points to. */
    SkillItem item;

    /** The sub-string of the input that has been matched. Typically the whole input */
    String matchedSubString;

    /** The string positions where the match was made. */
    int startPos, endPos;

    /** The quality of the match, important to report if low. (e.g. below 0.2) */
    float score;

    /** If the match is chosen, the following string should be input.
     * This string should correspond to an exact input that the user could
     * do later to match that skill with score 1.0. */
    String substringToReplaceIfChosen;

    /** Where to put the cursor within the replaced string after this has been chosen.
     * Particularly important if the choice is that of an item that is incomplete
     * so that a user is invited to complete the input for a precise item. */
    String cursorPositionIfChosen;

    public SkillItem getItem() {
        return item;
    }

    public String getMatchedSubString() {
        return matchedSubString;
    }

    public int getStartPos() {
        return startPos;
    }

    public int getEndPos() {
        return endPos;
    }

    public float getScore() {
        return score;
    }

    public String getSubstringToReplaceIfChosen() {
        return substringToReplaceIfChosen;
    }

    public String getCursorPositionIfChosen() {
        return cursorPositionIfChosen;
    }
}
