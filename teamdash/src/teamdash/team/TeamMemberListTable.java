package teamdash.team;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.beans.EventHandler;
import java.text.DateFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Calendar;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultCellEditor;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.text.JTextComponent;

import net.sourceforge.processdash.util.StringUtils;



/**
 * This class provides a highly customized table for editing the list of team
 * members and their weekly schedules
 */
public class TeamMemberListTable extends JTable {

    /**
     * Object to support the dragging of "Start" and "End" tokens in the
     * individual weekly schedules
     */
    TokenDragHandler tokenDragHandler;

    /** A button allowing the user to view earlier dates in the schedule */
    JButton scrollDatesEarlierButton;

    /** A button allowing the user to view later dates in the schedule */
    JButton scrollDatesLaterButton;

    /** a hyperlink allowing the user to customize team schedule parameters */
    JLabel customizationHyperlink;


    /**
     * Create a table for editing the given {@link TeamMemberList}
     * 
     * @param teamList
     *                the list of team members to edit
     */
    public TeamMemberListTable(TeamMemberList teamList) {
        super(teamList);

        // Set up renderer and editor for the Color column.
        ColorCellRenderer.setUpColorRenderer(this);
        ColorCellEditor.setUpColorEditor(this);

        // Set up renderer and editor for the weekly columns.
        NumberFormat hoursFormatter = createHoursFormatter();
        setDefaultRenderer(WeekData.class, new WeekDataRenderer(hoursFormatter));
        setDefaultEditor(WeekData.class, new WeekDataEditor(hoursFormatter));

        tokenDragHandler = new TokenDragHandler();
        customizationHyperlink = createCustomizationHyperlink();
        createButtons();
        setupTableColumnHeader();
        setupResizeHandler();
    }



    /** Return the team member list that this table is displaying/editing */
    protected TeamMemberList getTeamMemberList() {
        return (TeamMemberList) getModel();
    }

    /**
     * If the cell underneath point is displaying weekly data, return the
     * WeekData type. otherwise return -1.
     */
    protected int getCellType(Point point) {
        if (point == null)
            return -1;
        int row = rowAtPoint(point);
        int viewCol = columnAtPoint(point);
        if (row == -1 || viewCol == -1)
            return -1;
        int col = convertColumnIndexToModel(viewCol);
        return getCellType(row, col);
    }

    /**
     * If the cell at (row,col) is displaying weekly data, return the WeekData
     * type. otherwise return -1.
     */
    protected int getCellType(int row, int col) {
        Object val = getValueAt(row, col);
        return getWeekType(val);
    }


    /** If the parameter is WeekData, return its type.  Otherwise return -1 */
    protected int getWeekType(Object val) {
        if (val instanceof WeekData) {
            WeekData wd = (WeekData) val;
            return wd.getType();
        }
        return -1;
    }

    /** Select all text when the user begins editing a cell */
    public Component prepareEditor(TableCellEditor editor, int row, int column) {
        Component result = super.prepareEditor(editor, row, column);

        if (result instanceof JTextComponent)
            ((JTextComponent) result).selectAll();

        return result;
    }



    /** Create the number formatter for displaying "hours in a week" data */
    private NumberFormat createHoursFormatter() {
        NumberFormat result = NumberFormat.getInstance();
        result.setGroupingUsed(false);
        result.setMinimumFractionDigits(0);
        return result;
    }

    /** Create the hyperlink for customizing the team schedule */
    private JLabel createCustomizationHyperlink() {
        JLabel result = new JLabel();
        result.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        result.setBackground(getBackground());
        result.setOpaque(true);
        result.setToolTipText("Click to for additional schedule options");
        result.setHorizontalAlignment(SwingConstants.CENTER);
        result.setVerticalAlignment(SwingConstants.BOTTOM);
        result.setSize(WEEK_COL_WIDTH, getRowHeight());
        result.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                showCustomizationWindow();
            }
        });

        getTableHeader().add(result);
        return result;
    }

    /** Create the buttons for scrolling the visible schedule columns */
    private void createButtons() {
        scrollDatesEarlierButton = createScrollButton();
        scrollDatesEarlierButton.setToolTipText("View earlier dates");
        scrollDatesEarlierButton
                .addActionListener((ActionListener) EventHandler.create(
                    ActionListener.class, this, "scrollDatesEarlier"));

        scrollDatesLaterButton = createScrollButton();
        scrollDatesLaterButton.setToolTipText("View later dates");
        scrollDatesLaterButton.addActionListener((ActionListener) EventHandler
                .create(ActionListener.class, this, "scrollDatesLater"));
    }

    /** Create a single scroller button (but don't set its icon yet) */
    private JButton createScrollButton() {
        JButton result = new JButton();
        result.setMargin(new Insets(0, 0, 0, 0));
        result.setSize(result.getMinimumSize());
        result.setFocusPainted(false);
        result.setCursor(Cursor.getDefaultCursor());

        getTableHeader().add(result);
        return result;
    }

    /** Create and configure the column header for the table */
    private void setupTableColumnHeader() {
        // do not allow columns to be reordered. (That wouldn't make sense for
        // our weekly data columns, which appear in chronological order.
        getTableHeader().setReorderingAllowed(false);

        // set the first few "fixed" columns to use a default header renderer,
        // and set their size to our predefined column widths.
        TableCellRenderer plainRenderer = getTableHeader().getDefaultRenderer();
        for (int i = 0; i < COL_WIDTHS.length; i++) {
            getColumnModel().getColumn(i).setHeaderRenderer(plainRenderer);
            fixWidth(getColumnModel().getColumn(i), COL_WIDTHS[i]);
        }
        getColumnModel().getColumn(0).setMinWidth(10);
        getColumnModel().getColumn(0).setMaxWidth(Integer.MAX_VALUE);

        // install a DateHeaderRenderer for all remaining columns, and
        // configure the height of the regular renderer to match.
        DateHeaderRenderer dateHeaderRenderer = new DateHeaderRenderer(this);
        getTableHeader().setDefaultRenderer(dateHeaderRenderer);
        if (plainRenderer instanceof JComponent) {
            JComponent jc = (JComponent) plainRenderer;
            jc.setPreferredSize(dateHeaderRenderer.getPreferredSize());
        }
    }

    /**
     * Install a handler that will reconfigure the number of weekly data columns
     * whenever the size of the table changes.
     */
    private void setupResizeHandler() {
        addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                recreateWeekColumnsToFit();
            }
        });
    }

    /**
     * Recreate the columns in the table model, to display data for as many
     * weeks as possible (within the constraints of the width of the table)
     */
    private void recreateWeekColumnsToFit() {
        TeamMemberList teamList = getTeamMemberList();
        TableColumnModel tcm = getColumnModel();

        // compute the amount of space allocated to the weekly columns.
        // first, get the width of the entire table
        int tableWidth = getWidth();
        int width = tableWidth;
        // next, subtract out the width of the initial, fixed columns.
        for (int i = COL_WIDTHS.length; i-- > 0;)
            width = width - COL_WIDTHS[i];
        // then, subtract out the width of the grippy column.
        width = width - GRIPPY_WIDTH - getIntercellSpacing().width;

        int numWeekColumns = Math.max(0, width / WEEK_COL_WIDTH);
        int extraWidth = width - (numWeekColumns * WEEK_COL_WIDTH);
        TableColumn firstCol = tcm.getColumn(0);
        firstCol.setWidth(COL_WIDTHS[0] + extraWidth);

        int existingNumWeekColumns = teamList.getNumWeekColumns();
        if (existingNumWeekColumns != numWeekColumns) {
            // remove the grippy column.
            removeLastColumn(tcm);
            // remove any weekly columns that are no longer needed
            for (int i = numWeekColumns; i < existingNumWeekColumns; i++)
                removeLastColumn(tcm);
            // reconfigure the table for the desired number of weekly columns
            teamList.setNumWeekColumns(numWeekColumns);
            // add any weekly columns that are now needed
            int colIdx = tcm.getColumnCount();
            for (int i = existingNumWeekColumns; i < numWeekColumns; i++)
                tcm.addColumn(createWeekColumn(colIdx++));
            // add a grippy column at the end.
            tcm.addColumn(createGrippyColumn(colIdx));
        }

        repositionHeaderDecorations(tableWidth, numWeekColumns);
    }

    /** convenience method: remove the last column in a table column model */
    private void removeLastColumn(TableColumnModel tcm) {
        TableColumn lastColumn = tcm.getColumn(tcm.getColumnCount() - 1);
        tcm.removeColumn(lastColumn);
    }

    /** Create a table column to display weekly data */
    private TableColumn createWeekColumn(int idx) {
        TableColumn result = new TableColumn(idx);
        fixWidth(result, WEEK_COL_WIDTH);
        return result;
    }

    /** Create a table column for the final "grippy" column */
    private TableColumn createGrippyColumn(int idx) {
        TableColumn result = new TableColumn(idx);
        fixWidth(result, GRIPPY_WIDTH + getIntercellSpacing().width);
        result.setCellRenderer(new GrippyRenderer());
        return result;
    }

    /** Force a table column to be unresizable, with a specific width */
    private void fixWidth(TableColumn column, int width) {
        column.setMaxWidth(width);
        column.setMinWidth(width);
        column.setPreferredWidth(width);
        column.setWidth(width);
        column.setResizable(false);
    }

    /**
     * Place the scroller buttons and the customization hyperlink in the correct
     * places on the table header
     */
    private void repositionHeaderDecorations(int tableWidth, int numWeekColumns) {
        // if the icons haven't been created for the buttons yet, do it now.
        if (scrollDatesEarlierButton.getIcon() == null)
            createScrollerButtonIcons();
        // if the text hasn't been set on the customization link, do it now
        if (!StringUtils.hasValue(customizationHyperlink.getText()))
            updateCustomizationHyperlinkText();

        if (numWeekColumns == 0) {
            // if we aren't showing any week columns, hide all decorations.
            scrollDatesEarlierButton.setVisible(false);
            scrollDatesLaterButton.setVisible(false);
            customizationHyperlink.setVisible(false);
        } else {
            // position the scroll later button over the last week column.
            int x = tableWidth - GRIPPY_WIDTH - getIntercellSpacing().width;
            scrollDatesLaterButton.setVisible(true);
            scrollDatesLaterButton.setLocation(x - SCROLL_BUTTON_PADDING
                    - scrollDatesLaterButton.getWidth(), SCROLL_BUTTON_PADDING);

            // position the "scroll earlier button" over the first week column.
            x = x - numWeekColumns * WEEK_COL_WIDTH;
            scrollDatesEarlierButton.setVisible(true);
            scrollDatesEarlierButton.setLocation(x + SCROLL_BUTTON_PADDING,
                SCROLL_BUTTON_PADDING);

            // position the customization hyperlink over the first weekly
            // dividing line, if one exists.
            if (numWeekColumns < 2) {
                customizationHyperlink.setVisible(false);
            } else {
                customizationHyperlink.setVisible(true);
                customizationHyperlink.setLocation(x + WEEK_COL_WIDTH / 2, 1);
            }
        }
    }

    /**
     * Create icons for the scroller buttons, so they will fit perfectly in the
     * space alloted to the table header.
     */
    private void createScrollerButtonIcons() {
        int headerHeight = getTableHeader().getHeight();
        if (headerHeight < 10)
            return;

        Insets border = scrollDatesEarlierButton.getBorder().getBorderInsets(
            scrollDatesEarlierButton);
        int iconHeight = headerHeight - border.top - border.bottom
                - SCROLL_BUTTON_PADDING * 2 - 1;
        int iconWidth = (WEEK_COL_WIDTH - SCROLL_BUTTON_PADDING * 3 - 1) / 2
                - border.left - border.right;

        scrollDatesEarlierButton.setIcon(new TriangleIcon(true, iconWidth,
                iconHeight));
        scrollDatesEarlierButton.setSize(scrollDatesEarlierButton
                .getMinimumSize());

        scrollDatesLaterButton.setIcon(new TriangleIcon(false, iconWidth,
                iconHeight));
        scrollDatesLaterButton.setSize(scrollDatesLaterButton.getMinimumSize());
    }

    /**
     * Set the customization hyperlink to have text matching the first weekly
     * date column label
     */
    private void updateCustomizationHyperlinkText() {
        String text = getColumnName(TeamMemberList.FIRST_WEEK_COLUMN + 1);
        String html = "<html><font color='blue'><u>" + text
                + "</u></font></html>";
        customizationHyperlink.setText(html);
    }



    /** Scroll the weekly columns to show earlier dates */
    public void scrollDatesEarlier() {
        scrollDates(true);
    }

    /** Scroll the weekly columns to show later dates */
    public void scrollDatesLater() {
        scrollDates(false);
    }

    /** Scroll the weekly columns to show different dates */
    private void scrollDates(boolean earlier) {
        TeamMemberList teamList = getTeamMemberList();
        int numWeekCols = teamList.getNumWeekColumns();
        int scrollWeeks = Math.max(1, numWeekCols >> 1);
        if (earlier)
            scrollWeeks = -scrollWeeks;
        int weekOffset = teamList.getWeekOffset();
        teamList.setWeekOffset(weekOffset + scrollWeeks);
        updateCustomizationHyperlinkText();
        getTableHeader().repaint();
    }


    /**
     * Display a dialog window, allowing the user to customize aspects of the
     * team schedule. If they click OK, apply the changes they have made.
     */
    private void showCustomizationWindow() {
        TeamMemberList tml = getTeamMemberList();

        int currentDOW = tml.getStartOnDayOfWeek();
        JComboBox weekSelector = new JComboBox();
        String[] dayNames = new DateFormatSymbols().getWeekdays();
        for (int i = 0; i < DAYS_OF_THE_WEEK.length; i++) {
            int dow = DAYS_OF_THE_WEEK[i];
            weekSelector.addItem(dayNames[dow]);
            if (dow == currentDOW)
                weekSelector.setSelectedItem(dayNames[dow]);
        }
        Box wb = Box.createHorizontalBox();
        wb.add(Box.createHorizontalStrut(25));
        wb.add(weekSelector);

        Object[] contents = new Object[] { "Weekly schedule starts on:", wb };

        if (JOptionPane.OK_OPTION == JOptionPane.showConfirmDialog(this,
            contents, "Customize Team Schedule", JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE)) {
            Object selectedDay = weekSelector.getSelectedItem();
            for (int i = 0; i < dayNames.length; i++) {
                if (selectedDay.equals(dayNames[i]))
                    tml.setStartOnDayOfWeek(i);
            }
            updateCustomizationHyperlinkText();
            getTableHeader().repaint();
        }
    }

    private static final int[] DAYS_OF_THE_WEEK = { Calendar.SUNDAY,
            Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
            Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY };


    /** Creates a component that looks like something the user can drag */
    private JComponent makeGrippy() {
        JPanel result = new JPanel();
        result.setMinimumSize(new Dimension(GRIPPY_WIDTH, 0));
        result.setPreferredSize(new Dimension(GRIPPY_WIDTH, 10));
        result.setMaximumSize(new Dimension(GRIPPY_WIDTH, Integer.MAX_VALUE));
        result.setBorder(BorderFactory.createRaisedBevelBorder());
        return result;
    }


    private class WeekDataRenderer extends DefaultTableCellRenderer {

        NumberFormat hoursFormat;

        JComponent outsideSchedule;

        JComponent startCell;

        JComponent endCell;

        Font regularFont;

        Font italicFont;

        public WeekDataRenderer(NumberFormat hoursFormat) {
            this.hoursFormat = hoursFormat;
            setHorizontalAlignment(CENTER);

            Color background = UIManager.getColor("control");
            regularFont = TeamMemberListTable.this.getFont();
            italicFont = regularFont.deriveFont(Font.ITALIC);
            Font smallFont = regularFont
                    .deriveFont(regularFont.getSize2D() - 2);

            outsideSchedule = new JPanel();

            JLabel startLabel = new JLabel("START", new ArrowIcon(false), RIGHT);
            startLabel.setHorizontalTextPosition(LEFT);
            startLabel.setVerticalTextPosition(CENTER);
            startLabel.setIconTextGap(0);
            startLabel.setBackground(background);
            startLabel.setOpaque(true);
            startLabel.setFont(smallFont);
            startCell = new JPanel(new BorderLayout());
            startCell.add(startLabel, BorderLayout.CENTER);
            startCell.add(makeGrippy(), BorderLayout.EAST);
            startCell.setToolTipText("Drag to set schedule start date");

            JLabel endLabel = new JLabel("END", new ArrowIcon(true), LEFT);
            endLabel.setIconTextGap(0);
            endLabel.setBackground(background);
            endLabel.setOpaque(true);
            endLabel.setFont(smallFont);
            endCell = new JPanel(new BorderLayout());
            endCell.add(endLabel, BorderLayout.CENTER);
            endCell.add(makeGrippy(), BorderLayout.WEST);
            endCell.setToolTipText("<html>Drag to set schedule end date.<br>"
                    + "Drag all the way to the right to remove "
                    + "end date</html>");
        }


        public Component getTableCellRendererComponent(JTable table,
                Object value, boolean isSelected, boolean hasFocus, int row,
                int column) {
            WeekData week = (WeekData) value;
            switch (week.getType()) {
            case WeekData.TYPE_OUTSIDE_SCHEDULE:
                return outsideSchedule;
            case WeekData.TYPE_START:
                return startCell;
            case WeekData.TYPE_END:
                return endCell;
            }

            String display = hoursFormat.format(week.getHours());
            if (tokenDragHandler.isDragging())
                isSelected = hasFocus = false;
            super.getTableCellRendererComponent(table, display, isSelected,
                hasFocus, row, column);

            setFont(week.getType() == WeekData.TYPE_DEFAULT ? italicFont
                    : regularFont);

            return this;
        }
    }

    private class TokenDragHandler extends MouseAdapter implements
            ListSelectionListener, Runnable, MouseMotionListener {

        int activeRow = -1;

        private Object draggedValue;


        public TokenDragHandler() {
            // configure permissions and listeners for row & column selection
            setColumnSelectionAllowed(true);
            setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
            getSelectionModel().addListSelectionListener(this);
            getColumnModel().getSelectionModel().addListSelectionListener(this);

            addMouseListener(this);
            addMouseMotionListener(this);
        }

        public boolean isDragging() {
            return draggedValue != null;
        }

        public void mousePressed(MouseEvent e) {
            activeRow = -1;
            draggedValue = null;
            // when the mouse is pressed, we may want to start a drag operation.
            // but we only want to do this if the press has caused a single
            // START or END cell to be selected.  We need to ensure that the
            // built-in Swing MouseListener gets a chance to handle the event,
            // and update the selection.  So we'll request to be notified (in
            // our run method) after that occurs.
            SwingUtilities.invokeLater(this);
        }


        public void mouseReleased(MouseEvent e) {
            if (isDragging())
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        clearSelection();
                    }
                });

            activeRow = -1;
            draggedValue = null;
            setCursor(null);
        }

        public void valueChanged(ListSelectionEvent e) {
            if (isDragging()) {
                int draggedToCol = getColumnModel().getSelectionModel()
                        .getLeadSelectionIndex();
                if (getColumnClass(draggedToCol) == WeekData.class)
                    setValueAt(draggedValue, activeRow, draggedToCol);
            }
        }

        public void run() {
            // (this method is invoked a short time after the mouse is pressed)
            if (getSelectedRowCount() == 1 && getSelectedColumnCount() == 1) {
                int row = getSelectedRow();
                int col = getSelectedColumn();
                Object value = getValueAt(row, col);
                int type = getWeekType(value);
                if (type == WeekData.TYPE_START || type == WeekData.TYPE_END) {
                    activeRow = row;
                    draggedValue = value;
                }
            }
        }

        public void mouseDragged(MouseEvent e) {}

        public void mouseMoved(MouseEvent e) {
            setCursor(getCursorForPoint(e.getPoint()));
        }

        private Cursor getCursorForPoint(Point p) {
            switch (getCellType(p)) {
            case WeekData.TYPE_START:
                return Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR);

            case WeekData.TYPE_END:
                return Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR);

            default:
                return null;
            }
        }

    }

    private class DateHeaderRenderer extends JPanel implements
            TableCellRenderer {

        private Dimension prefSize = new Dimension(10, 30);

        private JTable table;

        private Font font;

        private FontMetrics fontMetrics;

        private int column;

        public DateHeaderRenderer(JTable t) {
            this.table = t;
            this.font = t.getFont();
            this.font = font.deriveFont(font.getSize() - 2.0f);
            this.fontMetrics = t.getFontMetrics(font);
            this.prefSize = new Dimension(10, (int) (t.getRowHeight() * 1.5));

            setBackground(t.getBackground());
            customizationHyperlink.setFont(font);
        }

        public Component getTableCellRendererComponent(JTable table,
                Object value, boolean isSelected, boolean hasFocus, int row,
                int column) {
            this.column = column;
            return this;
        }

        public Dimension getPreferredSize() {
            return prefSize;
        }

        public void paint(Graphics g) {
            super.paint(g);
            boolean isFirstWeek = (column == TeamMemberList.FIRST_WEEK_COLUMN);
            boolean isLastWeek = (column + 2 == getColumnCount());
            boolean isGrippy = (column + 1 == getColumnCount());

            // the area alloted to us is over the column in question; the
            // grid line that separates cells is the rightmost pixel of that
            // rectangle.
            int width = getWidth();
            int height = getHeight();

            g.setColor(table.getBackground());
            g.fillRect(0, 0, width, height);
            g.setColor(table.getGridColor());
            g.drawLine(0, height - 1, width, height - 1);
            if (!isLastWeek && !isGrippy)
                g.drawLine(width - 1, table.getRowHeight(), width - 1, height);

            g.setColor(table.getForeground());
            g.setFont(font);
            if (!isFirstWeek && !isGrippy)
                drawLabelAt(g, 0, getColumnName(column));
            if (!isLastWeek && !isGrippy)
                drawLabelAt(g, width, getColumnName(column + 1));
        }

        private void drawLabelAt(Graphics g, int x, String text) {
            int width = SwingUtilities.computeStringWidth(fontMetrics, text);
            g.drawString(text, x - (width / 2), table.getRowHeight() - 2);
        }

    }

    private class WeekDataEditor extends DefaultCellEditor {

        private NumberFormat hoursFormatter;

        public WeekDataEditor(NumberFormat hoursFormatter) {
            super(new JTextField());
            this.hoursFormatter = hoursFormatter;
        }

        public Object getCellEditorValue() {
            JTextField tf = (JTextField) getEditorComponent();
            String text = tf.getText();
            if (text == null || text.trim().length() == 0)
                return null;

            try {
                return hoursFormatter.parse(text.trim());
            } catch (ParseException pe) {
                return "ERROR";
            }
        }

        public Component getTableCellEditorComponent(JTable table,
                Object value, boolean isSelected, int row, int column) {

            WeekData data = (WeekData) value;
            String display = hoursFormatter.format(data.getHours());
            return super.getTableCellEditorComponent(table, display,
                isSelected, row, column);
        }

    }


    /**
     * This class paints the final column in the table. That column shows a
     * grippy that the user can drag to set the end date.
     */
    private class GrippyRenderer implements TableCellRenderer {

        private JComponent empty;

        private JComponent grippy;

        public GrippyRenderer() {
            empty = new JPanel();

            grippy = makeGrippy();
            grippy.setToolTipText("Drag to set schedule end date");
        }

        public Component getTableCellRendererComponent(JTable table,
                Object value, boolean isSelected, boolean hasFocus, int row,
                int column) {
            int type = getCellType(row, column - 1);
            if (WeekData.isInsideSchedule(type))
                return grippy;
            else
                return empty;
        }
    }


    /** This class draws a tiny arrow pointing left or right */
    private class ArrowIcon implements Icon {

        private boolean left;

        public ArrowIcon(boolean left) {
            this.left = left;
        }

        public int getIconHeight() {
            return 5;
        }

        public int getIconWidth() {
            return 5;
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(Color.black);
            g.drawLine(x + 0, y + 2, x + 4, y + 2);
            g.drawLine(x + 2, y + 0, x + 2, y + 0);
            g.drawLine(x + 2, y + 4, x + 2, y + 4);
            int l = (left ? 1 : 3);
            g.drawLine(x + l, y + 1, x + l, y + 3);
        }

    }

    /** This class draws an isosceles triangle pointing left or right */
    private class TriangleIcon implements Icon {

        private boolean left;

        private int width;

        private int height;


        public TriangleIcon(boolean left, int width, int height) {
            this.left = left;
            this.width = width;
            this.height = height;
        }

        public int getIconHeight() {
            return height;
        }

        public int getIconWidth() {
            return width;
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            Polygon p = new Polygon();
            int r = x + width - 1;
            if (left) {
                p.addPoint(x, y + height / 2);
                p.addPoint(r, y);
                p.addPoint(r, y + height);
            } else {
                p.addPoint(x, y);
                p.addPoint(r, y + height / 2);
                p.addPoint(x, y + height);
            }

            Graphics2D g2 = (Graphics2D) g;
            Object hint = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Color.black);
            g2.fill(p);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, hint);
        }

    }


    // Various values to tweak the UI display


    /** How many pixels of padding to display around the scroller buttons */
    private static final int SCROLL_BUTTON_PADDING = 3;

    /** Preferred widths for the initial, fixed columns */
    private static final int[] COL_WIDTHS = { 150, 55, 55, 65 };

    /** The width of each Weekly data column */
    private static final int WEEK_COL_WIDTH = 50;

    /** The width of the "grippy" which is displayed for draggable tokens */
    private static final int GRIPPY_WIDTH = 6;

}
