package net.i2geo.skillstextbox.client;

import net.i2geo.api.SkillItem;

/** Methods of a class that listens to the input of the user using skills-text-box.
 * Events include insertion of a node, of a set-of-words, or their deletions.
 * The idea is that Skills-text-box can be used both as a query editor
 * and as a metadata editor: the methods of this interface are
 * expected to be implemented by both.
 */
public interface SKBTarget {

    public void insertWordSet(String wordSet);

    /** Requests the listener to cancel the last word set entry
     * so that he or she can edit it again.
     * @return the text that was given by insertWordSet of class {@link String} or {@link SkillItem}
     */
    public Object requestAndDeleteLastInput();

    public void insertNode(SkillItem nodeId);

}
