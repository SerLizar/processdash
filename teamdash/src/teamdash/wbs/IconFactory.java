
package teamdash.wbs;

import java.awt.Color;
import java.awt.Component;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageProducer;
import java.awt.image.RGBImageFilter;
import java.util.HashMap;
import java.util.Map;

import javax.swing.GrayFilter;
import javax.swing.Icon;
import javax.swing.ImageIcon;

/** Factory for icons used by the WBSEditor and its components.
 */
public class IconFactory {

    private static final Color DEFAULT_COLOR = new Color(204, 204, 255);

    // This class is a singleton.
    private IconFactory() {}



    // Icons to depict various type of nodes in the work breakdown structure

    public static Icon getProjectIcon() {
        return new ProjectIcon(DEFAULT_COLOR);
    }

    public static Icon getWorkflowIcon() {
        return new WorkflowIcon(DEFAULT_COLOR);
    }

    public static Object getComponentIcon() {
        return new ComponentIcon(DEFAULT_COLOR);
    }

    public static Object getComponentIcon(Color fill) {
        return new ComponentIcon(fill);
    }

    public static Icon getSoftwareComponentIcon() {
        return new SoftwareComponentIcon(DEFAULT_COLOR);
    }

    public static Icon getDocumentIcon(Color highlight) {
        return new DocumentIcon(highlight);
    }

    public static Icon getTaskIcon(Color fill) {
        return new TaskIcon(fill);
    }

    public static Icon getPSPTaskIcon(Color fill) {
        return new PSPTaskIcon(fill);
    }



    // Icons used in toolbars and menus

    public static Icon getUndoIcon() {
        if (UNDO_ICON == null) UNDO_ICON = loadIconResource("undo.png");
        return UNDO_ICON;
    }
    private static Icon UNDO_ICON = null;

    public static Icon getRedoIcon() {
        if (REDO_ICON == null) REDO_ICON = loadIconResource("redo.png");
        return REDO_ICON;
    }
    private static Icon REDO_ICON = null;

    public static Icon getPromoteIcon() {
        if (PROMOTE_ICON == null) PROMOTE_ICON = loadIconResource("promote.png");
        return PROMOTE_ICON;
    }
    private static Icon PROMOTE_ICON = null;

    public static Icon getDemoteIcon() {
        if (DEMOTE_ICON == null) DEMOTE_ICON = loadIconResource("demote.png");
        return DEMOTE_ICON;
    }
    private static Icon DEMOTE_ICON = null;

    public static Icon getCutIcon() {
        if (CUT_ICON == null) CUT_ICON = loadIconResource("cut.gif");
        return CUT_ICON;
    }
    private static Icon CUT_ICON = null;

    public static Icon getCopyIcon() {
        if (COPY_ICON == null) COPY_ICON = loadIconResource("copy.png");
        return COPY_ICON;
    }
    private static Icon COPY_ICON = null;

    public static Icon getPasteIcon() {
        if (PASTE_ICON == null) PASTE_ICON = loadIconResource("paste.png");
        return PASTE_ICON;
    }
    private static Icon PASTE_ICON = null;

    public static Icon getDeleteIcon() {
        if (DELETE_ICON == null) DELETE_ICON = loadIconResource("delete.png");
        return DELETE_ICON;
    }
    private static Icon DELETE_ICON = null;

    public static Icon getImportIcon() {
        if (IMPORT_ICON == null) {
            IMPORT_ICON = new ConcatenatedIcon(new Icon[] { getWorkflowIcon(),
                    getLeftArrowIcon(), getOpenIcon() });
        }
        return IMPORT_ICON;
    }
    private static Icon IMPORT_ICON = null;

    public static Icon getExportIcon() {
        if (EXPORT_ICON == null) {
            EXPORT_ICON = new ConcatenatedIcon(new Icon[] { getWorkflowIcon(),
                    getRightArrowIcon(), getOpenIcon() });
        }
        return EXPORT_ICON;
    }
    private static Icon EXPORT_ICON = null;

    public static Icon getOpenIcon() {
        if (OPEN_ICON == null) OPEN_ICON = loadIconResource("open.png");
        return OPEN_ICON;
    }
    private static Icon OPEN_ICON = null;

    public static Icon getLeftArrowIcon() {
        return getPromoteIcon();
    }
    public static Icon getRightArrowIcon() {
        return getDemoteIcon();
    }

    public static Icon getExpandIcon() {
        if (EXPAND_ICON == null) EXPAND_ICON = loadIconResource("expand.png");
        return EXPAND_ICON;
    }
    private static Icon EXPAND_ICON = null;

    public static Icon getExpandAllIcon() {
        if (EXPAND_ALL_ICON == null) EXPAND_ALL_ICON = loadIconResource("expand-all.png");
        return EXPAND_ALL_ICON;
    }
    private static Icon EXPAND_ALL_ICON = null;

    public static Icon getCollapseIcon() {
        if (COLLAPSE_ICON == null) COLLAPSE_ICON = loadIconResource("collapse.png");
        return COLLAPSE_ICON;
    }
    private static Icon COLLAPSE_ICON = null;

    /** Convenience method for mixing colors.
     * @param r the ratio to use when mixing; must be between 0.0 and 1.0 .
     *   A value of 1.0 would return a color equivalent to color a.
     *   A value of 0.0 would return a color equivalent to color b.
     *   Other values linearly mix a and b together, yielding a color
     *   equivalent to (r * a) + ((1-r) * b)
     */
    public static Color mixColors(Color a, Color b, float r) {
        float s = 1.0f - r;
        return new Color(colorComp(a.getRed()   * r + b.getRed()   * s),
                         colorComp(a.getGreen() * r + b.getGreen() * s),
                         colorComp(a.getBlue()  * r + b.getBlue()  * s));
    }
    private static final float colorComp(float f) {
        return Math.min(1f, Math.max(0f, f / 255f));
    }



    /** Simple class to buffer an icon image for quick repainting.
     */
    private static class BufferedIcon implements Icon {
        protected Image image = null;
        protected int width = 16, height = 16;

        public BufferedIcon() {}

        public BufferedIcon(Icon originalIcon) {
            width = originalIcon.getIconWidth();
            height = originalIcon.getIconHeight();
            image = new BufferedImage(width, height,
                                      BufferedImage.TYPE_INT_ARGB);
            Graphics imageG = image.getGraphics();
            originalIcon.paintIcon(null, imageG, 0, 0);
            imageG.dispose();
        }

        public int getIconWidth() { return width; }
        public int getIconHeight() { return height; }
        protected void doPaint(Component c, Graphics g) {}

        public void paintIcon(Component c, Graphics g, int x, int y) {
            if (image == null) {
                image = new BufferedImage(getIconWidth(), getIconHeight(),
                                          BufferedImage.TYPE_INT_ARGB);
                Graphics imageG = image.getGraphics();
                doPaint(c,imageG);
                imageG.dispose();
            }
            g.drawImage(image, x, y, null);
        }

        public void applyFilter(RGBImageFilter filter) {
            ImageProducer prod =
                new FilteredImageSource(image.getSource(), filter);
            this.image = Toolkit.getDefaultToolkit().createImage(prod);
        }
    }



    /** Icon image representing a project.
     *
     * This draws a large square block.
     */
    private static class ProjectIcon extends BufferedIcon {

        Color fillColor, highlight, shadow;

        public ProjectIcon(Color fill) {
            this.fillColor = fill;
            this.highlight = mixColors(fill, Color.white, 0.3f);
            this.shadow    = mixColors(fill, Color.black, 0.7f);
        }

        protected void doPaint(Component c, Graphics g) {
            g.setColor(fillColor);
            g.fillRect(3,  3,  10, 10);

            g.setColor(shadow);
            g.drawLine(13, 3,  13, 13); // right shadow
            g.drawLine(3,  13, 13, 13); // bottom shadow

            g.setColor(highlight);
            g.drawLine(2,  2,  2, 13); // left highlight
            g.drawLine(2,  2,  13, 2); // top highlight

            g.setColor(Color.black);
            g.drawRect(1, 1, 13, 13);
        }
    }



    /** Icon image representing a common workflow.
     *
     * This draws four small boxes.
     */
    private static class WorkflowIcon extends PolygonIcon {

        Color highlight, shadow;

        public WorkflowIcon(Color fill) {
            this.xPoints = new int[] { 1, 1, 7, 7 };
            this.yPoints = new int[] { 1, 7, 7, 1 };
            this.fillColor = fill;
            this.highlight = mixColors(fill, Color.white, 0.3f);
            this.shadow    = mixColors(fill, Color.black, 0.7f);
        }

        protected void doPaint(Component c, Graphics g) {
            super.doPaint(c, g);
            g.translate(6, 0);
            super.doPaint(c, g);
            g.translate(0, 6);
            super.doPaint(c, g);
            g.translate(-6, 0);
            super.doPaint(c, g);
            g.translate(0, -6);

            g.setColor(Color.black);
            g.drawLine(7, 0, 7, 0);
            g.drawLine(0, 7, 0, 7);
            g.drawLine(7, 14, 7, 14);
            g.drawLine(14, 7, 14, 7);
        }

        protected void doHighlights(Component c, Graphics g) {
            g.setColor(shadow);
            drawHighlight(g, 1,  0, -1);
            drawHighlight(g, 2, -1,  0);

            g.setColor(highlight);
            drawHighlight(g, 0, 1, 0);
            drawHighlight(g, 3, 0, 1);
        }

    }



    /** Icon image representing a project component.
     *
     * This draws a square block.
     */
    private static class ComponentIcon extends BufferedIcon {

        Color fillColor, highlight, shadow;

        public ComponentIcon(Color fill) {
            this.fillColor = fill;
            this.highlight = mixColors(fill, Color.white, 0.3f);
            this.shadow    = mixColors(fill, Color.black, 0.7f);
        }

        protected void doPaint(Component c, Graphics g) {
            if (g instanceof Graphics2D) {
                GradientPaint grad = new GradientPaint(-10, -10, Color.white,
                                                       16, 16, fillColor);
                ((Graphics2D) g).setPaint(grad);
            } else {
                g.setColor(fillColor);
            }
            g.fillRect(3,  3,  10, 10);

            g.setColor(shadow);
            g.drawLine(13, 3,  13, 13); // right shadow
            g.drawLine(3,  13, 13, 13); // bottom shadow

            g.setColor(highlight);
            g.drawLine(2,  2,  2, 13); // left highlight
            g.drawLine(2,  2,  13, 2); // top highlight

            g.setColor(Color.black);
            g.drawRect(1, 1, 13, 13);
        }
    }



    /** Icon image representing a software component.
     *
     * This draws a floppy disk.
     */
    private static class SoftwareComponentIcon extends BufferedIcon {

        Color highlight;

        public SoftwareComponentIcon(Color highlight) {
            this.highlight = highlight;
        }

        protected void doPaint(Component c, Graphics g) {
            // fill in floppy
            g.setColor(highlight);
            g.fillRect(2,2, 12,12);

            // draw outline
            g.setColor(Color.black);
            g.drawLine( 1, 1, 13, 1);
            g.drawLine(14, 2, 14,14);
            g.drawLine( 1,14, 14,14);
            g.drawLine( 1, 1,  1,14);

            // draw interior lines
            g.setColor(Color.gray);
            g.fillRect(5,2, 6,5);
            g.drawLine(4,8, 11,8);
            g.drawLine(3,9, 3,13);
            g.drawLine(12,9, 12,13);

            // draw white parts
            g.setColor(Color.white);
            g.fillRect(8,3, 2,3);
            g.fillRect(4,9, 8,5);

            // draw text on floppy label
            g.setColor(highlight);
            g.drawLine(5,10, 9,10);
            g.drawLine(5,12, 8,12);
        }
    }



    /** Icon image representing a document
     *
     * This draws a page of paper.
     */
    private static class DocumentIcon extends BufferedIcon {

        Color highlight;

        public DocumentIcon(Color highlight) { this.highlight = highlight; }

        protected void doPaint(Component c, Graphics g) {

            int right = width - 1;
            int bottom = height - 1;

            // Draw fill
            if (g instanceof Graphics2D) {
                GradientPaint grad = new GradientPaint(0, 0, Color.white,
                                                       16, 16, highlight);
                ((Graphics2D) g).setPaint(grad);
                g.fillRect( 3, 1, 9, 14 );
                g.fillRect(12, 4, 2, 11 );

            } else {
                g.setColor(Color.white);
                g.fillRect(4, 2, 9, 12 );

                // Draw highlight
                g.setColor(highlight);
                g.drawLine( 3, 1, 3, bottom - 1 );                  // left
                g.drawLine( 3, 1, right - 6, 1 );                   // top
                g.drawLine( right - 2, 7, right - 2, bottom - 1 );  // right
                g.drawLine( right - 5, 2, right - 3, 4 );           // slant
                g.drawLine( 3, bottom - 1, right - 2, bottom - 1 ); // bottom
            }

            // Draw outline
            g.setColor(Color.black);
            g.drawLine( 2, 0, 2, bottom );                 // left
            g.drawLine( 2, 0, right - 4, 0 );              // top
            g.drawLine( 2, bottom, right - 1, bottom );    // bottom
            g.drawLine( right - 1, 6, right - 1, bottom ); // right
            g.drawLine( right - 6, 2, right - 2, 6 );      // slant 1
            g.drawLine( right - 5, 1, right - 4, 1 );      // part of slant 2
            g.drawLine( right - 3, 2, right - 3, 3 );      // part of slant 2
            g.drawLine( right - 2, 4, right - 2, 5 );      // part of slant 2


        }
    }



    /** Generic icon to draw a polygon with 3D edge highlighting.
     */
    private static class PolygonIcon extends BufferedIcon {
        int[] xPoints;
        int[] yPoints;
        Color fillColor;

        protected void doPaint(Component c, Graphics g) {
            // fill shape
            g.setColor(fillColor);
            g.fillPolygon(xPoints, yPoints, yPoints.length);

            // draw custom highlights
            doHighlights(c, g);

            // draw black outline
            g.setColor(Color.black);
            g.drawPolygon(xPoints, yPoints, yPoints.length);
        }

        protected void doHighlights(Component c, Graphics g) { }
        protected void drawHighlight(Graphics g, int segment,
                                     int xDelta, int yDelta) {
            int segStart = segment;
            int segEnd = (segment + 1) % xPoints.length;

            g.drawLine(xPoints[segStart] + xDelta,
                       yPoints[segStart] + yDelta,
                       xPoints[segEnd]   + xDelta,
                       yPoints[segEnd]   + yDelta);
        }
    }



    /** Icon image representing a work breakdown structure task.
     *
     * This draws a parallelogram.
     */
    private static class TaskIcon extends PolygonIcon {

        Color highlight, shadow;

        public TaskIcon(Color fill) {
            this.xPoints = new int[] {  0, 5, 15, 10 };
            this.yPoints = new int[] { 14, 1,  1, 14 };
            this.fillColor = fill;
            this.highlight = mixColors(fill, Color.white, 0.3f);
            this.shadow    = mixColors(fill, Color.black, 0.7f);
        }

        protected void doHighlights(Component c, Graphics g) {
            g.setColor(shadow);
            drawHighlight(g, 2, -1,  0);
            drawHighlight(g, 3,  0, -1);


            g.setColor(highlight);
            drawHighlight(g, 0, 1, 0);
            drawHighlight(g, 1, 0, 1);
        }
    }



    /** Icon image representing a PSP task.
     *
     * This draws a pentagon.
     */
    private static class PSPTaskIcon extends PolygonIcon {

        Color highlight, shadow;

        public PSPTaskIcon(Color fill) {
            this.xPoints = new int[] { 7, 0,  3, 12, 15, 8 };
            this.yPoints = new int[] { 1, 7, 15, 15,  7, 1 };
            this.fillColor = fill;
            this.highlight = mixColors(fill, Color.white, 0.3f);
            this.shadow    = mixColors(fill, Color.black, 0.7f);
        }


        protected void doHighlights(Component c, Graphics g) {
            g.setColor(Color.white);
            drawHighlight(g, 1,  1, 0); // bottom left highlight
            drawHighlight(g, 4,  0, 1); // top right highlight

            g.setColor(highlight);
            drawHighlight(g, 0,  0, 1); // top left highlight
            drawHighlight(g, 0,  1, 1); // top left highlight

            g.setColor(shadow);
            drawHighlight(g, 2,  0, -1); // bottom shadow
            drawHighlight(g, 3, -1,  0); // right shadow
        }
    }


    /** Icon capable of concatenating several other icons.
     */
    private static class ConcatenatedIcon implements Icon {

        private Icon[] icons;
        int width, height;

        public ConcatenatedIcon(Icon[] icons) {
            this.icons = icons;
            this.width = this.height = 0;
            for (int i = 0; i < icons.length; i++) {
                this.width += icons[i].getIconWidth();
                this.height = Math.max(this.height, icons[i].getIconHeight());
            }
        }

        public int getIconHeight() {
            return height;
        }

        public int getIconWidth() {
            return width;
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            for (int i = 0; i < icons.length; i++) {
                icons[i].paintIcon(c, g, x, y);
                x += icons[i].getIconWidth();
            }
        }

    }




    public static final int PHANTOM_ICON = 1;
    public static final int ERROR_ICON = 2;
    public static final int DISABLED_ICON = 4;

    private static final Map[] MODIFIED_ICONS = new Map[4];
    static {
        MODIFIED_ICONS[0] = new HashMap();
        MODIFIED_ICONS[1] = new HashMap();
        MODIFIED_ICONS[2] = new HashMap();
        MODIFIED_ICONS[3] = new HashMap();
    }

    /** Create a modified version of an icon.
     *
     * @param i the icon to replicate
     * @param modifierFlags a bitwise-or of any collection of the
     * following flags:<ul>
     * <li>{@link #PHANTOM_ICON} to create a whitened-out icon, indicative
     *     of a cut operation
     * <li>{@link #ERROR_ICON} to create a reddened icon, indicative of an
     *     error condition.
     * <li>{@link #DISABLED_ICON} to create a grayed-out icon, indicative of a
     *     disabled action.  (This flag cannot be used in combination with
     *     either of the other two flags.)
     * </ul>
     * @return an icon which looks like the original, with the requested
     *  filters applied. <b>Note:</b> this method will automatically cache
     *  the icons it generates, and return a cached icon when appropriate.
     */
    public static Icon getModifiedIcon(Icon i, int modifierFlags) {
        if (modifierFlags < 1 || modifierFlags > 4)
            return i;

        Map destMap = MODIFIED_ICONS[modifierFlags - 1];
        Icon result = (Icon) destMap.get(i);
        if (result == null) {
            BufferedIcon bufIcon = new BufferedIcon(i);
            if ((modifierFlags & ERROR_ICON) > 0)
                bufIcon.applyFilter(RED_FILTER);
            if ((modifierFlags & PHANTOM_ICON) > 0)
                bufIcon.applyFilter(PHANTOM_FILTER);
            if ((modifierFlags & DISABLED_ICON) > 0)
                bufIcon.applyFilter(GRAY_FILTER);
            result = bufIcon;
            destMap.put(i, result);
        }
        return result;
    }

    // filter for creating "error" icons.  Converts to red monochrome.
    private static RedFilter RED_FILTER = new RedFilter();
    private static class RedFilter extends RGBImageFilter {
        public RedFilter() { canFilterIndexColorModel = true; }

        public int filterRGB(int x, int y, int rgb) {
            // Use NTSC conversion formula.
            int gray = (int)((0.30 * ((rgb >> 16) & 0xff) +
                              0.59 * ((rgb >> 8) & 0xff) +
                              0.11 * (rgb & 0xff)) / 3);

            if (gray < 0) gray = 0;
            if (gray > 255) gray = 255;
            return (rgb & 0xffff0000) | (gray << 8) | (gray << 0);
        }
    }

    // filter for creating "phantom" icons.  Mixes all colors
    // half-and-half with white.
    private static PhantomFilter PHANTOM_FILTER = new PhantomFilter();
    private static class PhantomFilter extends RGBImageFilter {
        public PhantomFilter() { canFilterIndexColorModel = true; }

        public int filterRGB(int x, int y, int rgb) {
            int alpha = rgb & 0xff000000;
            int red   = filt((rgb >> 16) & 0xff);
            int green = filt((rgb >> 8)  & 0xff);
            int blue  = filt((rgb >> 0)  & 0xff);

            return alpha | (red << 16) | (green << 8) | blue;
        }
        public int filt(int component) {
            return (component + 0xff) / 2;
        }
    }

    // filter for creating "disabled" icons.
    private static GrayFilter GRAY_FILTER = new GrayFilter(true, 50);



    /** Fetch an icon from a file in the classpath. */
    private static Icon loadIconResource(String name) {
        return new ImageIcon(IconFactory.class.getResource(name));
    }

}
