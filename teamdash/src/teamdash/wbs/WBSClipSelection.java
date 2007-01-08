package teamdash.wbs;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WBSClipSelection implements Transferable, ClipboardOwner {

    private static class WBSListDataFlavor extends DataFlavor {
        public WBSListDataFlavor() {
            // by subclassing DataFlavor, we help it to determine the
            // appropriate classloader that should be used to load the
            // WBSClipData class, named in the mime type below.
            super(DataFlavor.javaJVMLocalObjectMimeType + "; class="
                    + WBSClipData.class.getName(),
                    "Work Breakdown Structure Data");
        }
    }

    public static final DataFlavor WBS_LIST_FLAVOR = new WBSListDataFlavor();

    public static final DataFlavor WBS_FLAVOR = new DataFlavor(
            WBSClipData.class, "Work Breakdown Structure Data");

    private static final DataFlavor[] FLAVORS = { WBS_LIST_FLAVOR, WBS_FLAVOR,
            DataFlavor.stringFlavor };

    private WBSClipData clipData;

    private String textData;


    public WBSClipSelection(List wbsNodes) {
        this.clipData = new WBSClipData(wbsNodes);
        this.textData = getTextDataForClipboard(wbsNodes);
    }

    public DataFlavor[] getTransferDataFlavors() {
        return (DataFlavor[]) FLAVORS.clone();
    }

    public boolean isDataFlavorSupported(DataFlavor flavor) {
        for (int i = 0; i < FLAVORS.length; i++) {
            if (flavor.equals(FLAVORS[i]))
                return true;
        }
        return false;
    }

    public Object getTransferData(DataFlavor flavor)
            throws UnsupportedFlavorException, IOException {
        if (flavor.equals(WBS_LIST_FLAVOR) || flavor.equals(WBS_FLAVOR))
            return clipData;
        else if (flavor.equals(DataFlavor.stringFlavor))
            return textData;
        else
            throw new UnsupportedFlavorException(flavor);
    }

    public void lostOwnership(Clipboard clipboard, Transferable contents) {
    }


    /** Interact with the system clipboard, and place a list of WBSNodes there.
     * 
     * @param wbsNodes the list of WBSNodes to share
     * @param o an owner who would like to be notified when the nodes are no
     *   longer on the system clipboard; can be null.
     */
    public static void putNodeListOnClipboard(List wbsNodes, ClipboardOwner o) {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        WBSClipSelection t = new WBSClipSelection(wbsNodes);
        clipboard.setContents(t, (o == null ? t : o));
    }


    /** Return a plain-text representation of a list of WBSNodes.
     * 
     * This should ideally be in the same format as the text parsed by the
     * {@link #getNodeListFromClipboard(WBSNode)} method below, so users can
     * see what text format we're expecting to receive.
     */
    private static String getTextDataForClipboard(List wbsNodes) {
        StringBuffer buf = new StringBuffer();
        for (Iterator i = wbsNodes.iterator(); i.hasNext();) {
            WBSNode node = (WBSNode) i.next();
            for (int j = node.getIndentLevel(); j-- > 0;)
                buf.append("   ");
            buf.append(node.getName()).append("\n");
        }
        return buf.toString();
    }


    /** Interact with the system clipboard to retrieve a list of WBSNodes.
     * 
     * @param prototype if the system clipboard contains plain text, a list
     *   of nodes modeled after this prototype will be returned.
     */
    public static List getNodeListFromClipboard(WBSNode prototype) {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable t = clipboard.getContents(prototype);

        // first, try to retrieve a local java object (the exact one we placed
        // on the clipboard).
        try {
            Object d = t.getTransferData(WBS_LIST_FLAVOR);
            List result = ((WBSClipData) d).getWBSNodes(prototype.getWbsModel());
            if (result != null)
                return result;
        } catch (Exception e) {
        }

        // if that failed, try to retrieve a WBSClipData object placed on the
        // clipboard by some other WBS Editor.
        try {
            Object d = t.getTransferData(WBS_FLAVOR);
            List result = ((WBSClipData) d).getWBSNodes(prototype.getWbsModel());
            if (result != null)
                return result;
        } catch (Exception e) {
        }

        // if that fails, interpret the string on the clipboard as a list of
        // names, and create some ultra-simple nodes with those names.
        try {
            String plainText = (String) t
                    .getTransferData(DataFlavor.stringFlavor);
            String[] lines = plainText.split("\n");

            List result = new ArrayList();
            for (int i = 0; i < lines.length; i++) {
                Matcher m = PLAINTEXT_PATTERN.matcher(lines[i]);
                if (!m.find())
                    continue;

                WBSNode node = (WBSNode) prototype.clone();
                node.setReadOnly(false);
                node.setName(m.group(2).replace('/', ','));
                node.setType(WBSNode.UNKNOWN_TYPE);
                int indent = Math.max(1, node.getIndentLevel());
                if (m.group(1) != null && m.group(1).length() > 0) {
                    Matcher mi = INDENT_PATTERN.matcher(m.group(1));
                    while (mi.find())
                        indent++;
                }
                node.setIndentLevel(indent);
                result.add(node);
            }
            return result;
        } catch (Exception e) {
        }

        // if all else fails, return null to indicate failure.
        return null;
    }

    private static final Pattern INDENT_PATTERN = Pattern.compile("   |\t");

    private static final Pattern PLAINTEXT_PATTERN = Pattern
            .compile("^([ \t]*)([^\t]+)");

}
