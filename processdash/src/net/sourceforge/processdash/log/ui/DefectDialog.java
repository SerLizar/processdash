// Copyright (C) 2000-2012 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 3
// of the License, or (at your option) any later version.
//
// Additional permissions also apply; see the README-license.txt
// file in the project root directory for more information.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net


package net.sourceforge.processdash.log.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.toedter.calendar.JDateChooser;

import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.log.defects.Defect;
import net.sourceforge.processdash.log.defects.DefectLog;
import net.sourceforge.processdash.log.defects.DefectUtil;
import net.sourceforge.processdash.log.time.TimeLoggingModel;
import net.sourceforge.processdash.process.DefectTypeStandard;
import net.sourceforge.processdash.ui.DashboardIconFactory;
import net.sourceforge.processdash.ui.help.PCSH;
import net.sourceforge.processdash.ui.lib.BoxUtils;
import net.sourceforge.processdash.ui.lib.DecimalField;
import net.sourceforge.processdash.ui.lib.DropDownLabel;
import net.sourceforge.processdash.ui.macosx.MacGUIUtils;
import net.sourceforge.processdash.util.FormatUtil;
import net.sourceforge.processdash.util.Stopwatch;
import net.sourceforge.processdash.util.StringUtils;


public class DefectDialog extends JDialog
    implements ActionListener, DocumentListener, WindowListener
{
    ProcessDashboard parent;
    String defectFilename;
    PropertyKey defectPath;
    DefectLog defectLog = null;
    Stopwatch stopwatch = null;
    StopwatchSynchronizer stopwatchSynchronizer;
    StartStopButtons defectTimerButton;
    Date date = null;
    JButton OKButton, CancelButton, fixDefectButton;
    String defectNumber = null;
    JLabel number;
    JDateChooser fix_date;
    JTextField fix_defect;
    DecimalField fix_time, fix_count;
    JTextArea description;
    JComboBox defect_type, phase_injected, phase_removed;
    PendingSelector pendingSelector;
    Map<String, String> extra_attrs;
    boolean isDirty = false, autoCreated = false;

    /** A stack of the defect dialogs that have been interrupted. */
    private static Stack interruptedDialogs = new Stack();
    /** The defect dialog which was timing most recently. */
    private static DefectDialog activeDialog = null;
    /** A list of all open defect dialogs. */
    private static Hashtable defectDialogs = new Hashtable();
    /** A timer object for refreshing the fix time field. */
    private javax.swing.Timer activeRefreshTimer = null;

    Resources resources = Resources.getDashBundle("Defects.Editor");


    DefectDialog(ProcessDashboard dash, String defectFilename,
                 PropertyKey defectPath) {
        this(dash, defectFilename, defectPath, true);
    }

    DefectDialog(ProcessDashboard dash, String defectFilename,
                 PropertyKey defectPath, boolean guessDefaults) {
        super(dash);
        setTitle(resources.getString("Window_Title"));
        PCSH.enableHelpKey(this, "EnteringDefects");

        parent = dash;
        this.defectFilename = defectFilename;
        this.defectPath = defectPath;
        defectLog = new DefectLog(defectFilename, defectPath.path(),
                                  dash.getData());
        date = new Date();
        stopwatch = new Stopwatch(false);
        stopwatch.setMultiplier(Settings.getVal("timer.multiplier"));
        stopwatchSynchronizer = new StopwatchSynchronizer(dash
                .getTimeLoggingModel());

        JPanel panel = new JPanel();
        GridBagLayout layout = new GridBagLayout();
        GridBagConstraints g = new GridBagConstraints();
        JComponent c;
        Insets bottom_margin = new Insets(1, 1, 8, 1);
        Insets bottom_right_margin = new Insets(1, 1, 8, 10);
        Insets small_margin = new Insets(1, 1, 1, 1);
        panel.setBorder(new EmptyBorder(5,5,5,5));
        panel.setLayout(layout);

                                // first row
        g.gridy = 0;
        g.insets = small_margin;
        g.anchor = GridBagConstraints.WEST;

        number = new JLabel();
        g.gridx = 0;   layout.setConstraints(number, g);
        panel.add(number);

        c = new JLabel(resources.getString("Fix_Date_Label"));
        g.gridx = 1;   layout.setConstraints(c, g);
        panel.add(c);

                                // second row
        g.gridy = 1;
        g.insets = bottom_right_margin;
        g.anchor = GridBagConstraints.NORTHWEST;
        g.fill = GridBagConstraints.HORIZONTAL;
        defect_type = DefectTypeStandard.get
            (defectPath.path(), dash.getData()).getAsComboBox();
        defect_type.insertItemAt(resources.getString("Defect_Type_Prompt"), 0);
        defect_type.setMaximumRowCount(20);
        defect_type.setSelectedIndex(0);
        defect_type.addActionListener(this);

        g.gridx = 0;   layout.setConstraints(defect_type, g);
        panel.add(defect_type);

        fix_date = new JDateChooser(date);
        fix_date.getDateEditor().getUiComponent().setToolTipText(
            resources.getString("Fix_Date_Tooltip"));
        g.insets = bottom_margin;
        g.fill = GridBagConstraints.BOTH;
        g.gridx = 1;   layout.setConstraints(fix_date, g);
        panel.add(fix_date);

                                // third row
        g.gridy = 2;
        g.insets = small_margin;
        g.anchor = GridBagConstraints.WEST;
        g.weightx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;

        c = new JLabel(resources.getString("Injected_Label"));
        g.gridx = 0;   layout.setConstraints(c, g);
        panel.add(c);

        c = pendingSelector = new PendingSelector();
        g.fill = GridBagConstraints.NONE;
        g.gridx = 1;   layout.setConstraints(c, g);
        panel.add(c);

                                // fourth row
        g.gridy = 3;
        g.insets = bottom_right_margin;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.NORTHWEST;

        List defectPhases = DefectUtil.getDefectPhases(defectPath.path(),
                parent);

        String defaultRemovalPhase = null;
        if (guessDefaults)
            defaultRemovalPhase = guessRemovalPhase(defectPath);
        phase_removed = phaseComboBox(defectPhases, defaultRemovalPhase);
        phase_removed.setToolTipText(resources.getString("Removed_Tooltip"));

        String defaultInjectionPhase = null;
        if (guessDefaults && defaultRemovalPhase != null)
            defaultInjectionPhase = DefectUtil.guessInjectionPhase(defectPhases,
                    defaultRemovalPhase);
        phase_injected = phaseComboBox(defectPhases, defaultInjectionPhase);
        phase_injected.setToolTipText(resources.getString("Injected_Tooltip"));

        phase_injected.insertItemAt("Before Development", 0);
        phase_injected.addActionListener(this);
        g.gridx = 0;   layout.setConstraints(phase_injected, g);
        panel.add(phase_injected);

        phase_removed.addItem("After Development");
        phase_removed.addActionListener(this);
        g.insets = bottom_margin;
        g.gridx = 1; layout.setConstraints(phase_removed, g);
        panel.add(phase_removed);

                                // fifth row

        // create a subpanel to allow more even spacing for the three
        // elements that appear on these two rows
        GridBagLayout fixLayout = new GridBagLayout();
        JPanel fixPanel = new JPanel(fixLayout);

        g.gridy = 0;
        g.insets = small_margin;
        g.fill = GridBagConstraints.VERTICAL;
        g.anchor = GridBagConstraints.WEST;

        c = new JLabel(resources.getString("Fix_Time_Label"));
        g.gridx = 0;   fixLayout.setConstraints(c, g);
        fixPanel.add(c);

        c = new JLabel(resources.getString("Fix_Count_Label"));
        g.gridx = 1;   fixLayout.setConstraints(c, g);
        fixPanel.add(c);

        c = new JLabel(resources.getString("Fix_Defect_Label"));
        g.gridx = 2;   fixLayout.setConstraints(c, g);
        fixPanel.add(c);

                                // sixth row
        g.gridy = 1;
        g.insets = bottom_right_margin;

        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setMaximumFractionDigits(1);
        fix_time = new DecimalField(0.0, 4, nf);
        setTextFieldMinSize(fix_time);
        fix_time.setToolTipText(resources.getString("Fix_Time_Tooltip"));
        defectTimerButton = new StartStopButtons();
        c = BoxUtils.hbox(fix_time, 1, defectTimerButton.stopButton, 1,
            defectTimerButton.startButton);
        g.fill = GridBagConstraints.VERTICAL;
        g.anchor = GridBagConstraints.NORTHWEST;
        g.gridx = 0;   fixLayout.setConstraints(c, g);
        fixPanel.add(c);
        fix_time.getDocument().addDocumentListener(this);

        nf = NumberFormat.getIntegerInstance();
        fix_count = new DecimalField(1, 4, nf);
        setTextFieldMinSize(fix_count);
        fix_count.setToolTipText(resources.getString("Fix_Count_Tooltip"));
        g.gridx = 1;   fixLayout.setConstraints(fix_count, g);
        fixPanel.add(fix_count);
        fix_count.getDocument().addDocumentListener(this);

        fix_defect = new JTextField(3);
        fix_defect.setToolTipText(resources.getString("Fix_Defect_Tooltip"));
        fix_defect.getDocument().addDocumentListener(this);
        setTextFieldMinSize(fix_defect);

        fixDefectButton = new JButton();
        fixDefectButton.setIcon(DashboardIconFactory.getDefectIcon());
        fixDefectButton.setMargin(new Insets(1, 2, 1, 2));
        fixDefectButton.setToolTipText(resources
                .getString("Fix_Defect_Button_Tooltip"));
        fixDefectButton.addActionListener(this);

        c = BoxUtils.hbox(fix_defect, 1, fixDefectButton);
        g.insets = bottom_margin;
        g.fill = GridBagConstraints.BOTH;
        g.gridx = 2;   fixLayout.setConstraints(c, g);
        fixPanel.add(c);

        g.gridx = 0;   g.gridy = 4;  g.gridwidth = 2;
        g.insets = new Insets(0, 0, 0, 0);
        layout.setConstraints(fixPanel, g);
        panel.add(fixPanel);

                                // seventh row
        g.gridy = 6;
        g.insets = small_margin;
        g.anchor = GridBagConstraints.WEST;
        g.gridwidth = 1;
        c = new JLabel(resources.getString("Description_Label"));
        g.gridx = 0;   layout.setConstraints(c, g);
        panel.add(c);

                                // eighth row
        g.gridy = 7;
        g.insets = bottom_margin;
        g.fill = GridBagConstraints.BOTH;
        description = new JTextArea();
        description.getDocument().addDocumentListener(this);
        description.setLineWrap(true);
        description.setWrapStyleWord(true);

        JScrollPane scroller = new
            JScrollPane(description, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                        JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroller.setPreferredSize(new Dimension(100, 100));

        JPanel textWrapper = new JPanel(new BorderLayout());
//      textWrapper.setAlignmentX(LEFT_ALIGNMENT);
        textWrapper.setBorder(new BevelBorder(BevelBorder.LOWERED));
        textWrapper.add("Center", scroller);

        g.weighty = 100;
        g.gridwidth = 2;   layout.setConstraints(textWrapper, g);
        panel.add(textWrapper);
        g.gridwidth = 1;
        g.weighty = 0;

                                // ninth row
        g.gridy = 8;
        g.insets = small_margin;
        g.anchor = GridBagConstraints.CENTER;
        g.fill = GridBagConstraints.NONE;

        Action okAction = new OKButtonAction();
        OKButton = new JButton(okAction);
        g.gridx = 0;   layout.setConstraints(OKButton, g);
        panel.add(OKButton);

        Action cancelAction = new CancelButtonAction();
        CancelButton = new JButton(cancelAction);
        g.gridx = 1; layout.setConstraints(CancelButton, g);
        panel.add(CancelButton);

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(this);

        InputMap inputMap = panel
                .getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, MacGUIUtils
                .getCtrlModifier()), "okButtonAction");
        panel.getActionMap().put("okButtonAction", okAction);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            "cancelButtonAction");
        panel.getActionMap().put("cancelButtonAction", cancelAction);

        getContentPane().add(panel);
        pack();
        panel.setMinimumSize(panel.getPreferredSize());
        setVisible(true);

        if ("true".equalsIgnoreCase(Settings.getVal("defectDialog.autostart")))
            startTimingDefect();
        setDirty(false);
    }

    private void setTextFieldMinSize(JTextField tf) {
        Dimension d = tf.getPreferredSize();
        d.width /= 2;
        tf.setMinimumSize(d);
    }

    private DefectDialog(ProcessDashboard dash, String defectFilename,
                         PropertyKey defectPath, Defect defect) {
        this(dash, defectFilename, defectPath, false);
        stopTimingDefect();
        setValues(defect);
        setDirty(false);
    }

    public static DefectDialog getDialogForDefect
        (ProcessDashboard dash, String defectFilename,
         PropertyKey defectPath, Defect defect, boolean create)
    {
        DefectDialog result = null;

        String comparisonKey = defectFilename + defect.number;
        result = (DefectDialog) defectDialogs.get(comparisonKey);
        if (result != null && result.isDisplayable()) return result;
        if (!create) return null;

        result = new DefectDialog(dash, defectFilename, defectPath, defect);
        result.saveDialogInCache();

        return result;
    }

    private String comparisonKey() {
        return defectLog.getDefectLogFilename() + defectNumber;
    }
    private void saveDialogInCache() {
        defectDialogs.put(comparisonKey(), this);
    }

    public void setDirty(boolean dirty) {
        isDirty = dirty;
        MacGUIUtils.setDirty(this, isDirty);
        //OKButton.setEnabled(dirty);
    }

    public void save() {
        refreshFixTimeFromStopwatch();

        Defect d = new Defect();
        d.date = fix_date.getDate();
        if (d.date == null)
            d.date = date;
        d.number = defectNumber;
        d.defect_type = (String)defect_type.getSelectedItem();
        d.phase_injected = (String)phase_injected.getSelectedItem();
        d.phase_removed = (String)phase_removed.getSelectedItem();
        d.fix_time = fix_time.getText();
        try {
            d.fix_count = (int) FormatUtil.parseNumber(fix_count.getText());
        } catch (ParseException nfe) {
            d.fix_count = 1;
        }
        d.fix_defect = fix_defect.getText();
        d.fix_pending = pendingSelector.isPending();
        d.description = description.getText();
        d.extra_attrs = extra_attrs;

        defectLog.writeDefect(d);

        defectNumber = d.number;
        number.setText(formatDefectNum(d.number));
        setDirty(false);
    }
    private String formatDefectNum(String number) {
        return resources.format("Defect_Number_FMT", number);
    }

    public void startTimingDefect() {
        stopwatch.start();
        defectTimerButton.setRunning(true);
        if (activeRefreshTimer == null)
            activeRefreshTimer = new javax.swing.Timer(6000, this);
        activeRefreshTimer.start();

        if (activeDialog != this) synchronized (interruptedDialogs) {
            if (activeDialog != null && activeDialog.stopwatch.isRunning()) {
                interruptedDialogs.push(activeDialog);
                activeDialog.stopTimingDefect();
            }

            interruptedDialogs.remove(this); // it might not be there, that's OK
            activeDialog = this;
        }
    }

    public void stopTimingDefect() {
        stopwatch.stop();
        defectTimerButton.setRunning(false);
        if (activeRefreshTimer != null)
            activeRefreshTimer.stop();

        refreshFixTimeFromStopwatch();
    }

    private void maybePopDialog() {
        if (activeDialog == this)
            if (interruptedDialogs.empty())
                activeDialog = null;
            else {
                activeDialog = (DefectDialog) interruptedDialogs.pop();
                activeDialog.toFront();

                if (stopwatch.isRunning())
                    activeDialog.startTimingDefect();
            }
    }

    private void maybeDeleteAutocreatedDefect() {
        if (autoCreated)
            defectLog.deleteDefect(defectNumber);
    }

    private void comboSelect(JComboBox cb, String item) {
        int i = cb.getItemCount();
        while (i != 0)
            if (item.equals(cb.getItemAt(--i))) {
                cb.setSelectedIndex(i);
                return;
            }
        if (StringUtils.hasValue(item)) {
            cb.addItem(item);
            cb.setSelectedItem(item);
        }
    }

    private JComboBox phaseComboBox(List phases, String selectedChild) {
        JComboBox result = new JComboBox();

        for (Iterator i = phases.iterator(); i.hasNext();) {
            String phase = (String) i.next();
            result.addItem(phase);
            if (phase.equals(selectedChild))
                result.setSelectedItem(phase);
        }

        return result;
    }


    /** Make an educated guess about which removal phase might correspond to
     * the current dashboard state.
     */
    private String guessRemovalPhase(PropertyKey defectPath) {
        String phasePath = parent.getCurrentPhase().path();
        return DefectUtil.guessRemovalPhase(defectPath.path(), phasePath,
                parent);
    }


    private void hide_popups() {
        defect_type.hidePopup();
        phase_injected.hidePopup();
        phase_removed.hidePopup();
    }

    private volatile boolean programmaticallyChangingFixTime = false;

    private void fixTimeChanged() {
        if (programmaticallyChangingFixTime) return;
        setDirty(true);
        stopwatch.setElapsed((long) (fix_time.getValue() * 60.0));
    }

    private void refreshFixTimeFromStopwatch() {
        programmaticallyChangingFixTime = true;
        fix_time.setValue(stopwatch.minutesElapsedDouble());
        programmaticallyChangingFixTime = false;
    }

    private void openFixDefectDialog() {
        if (defectNumber == null) {
            save();
            setDirty(true);
            saveDialogInCache();
            autoCreated = true;
        }
        DefectDialog d = new DefectDialog(parent, defectFilename, defectPath);
        d.fix_defect.setText(defectNumber);
        comboSelect(d.phase_injected, (String)phase_removed.getSelectedItem());
        d.setDirty(false);
    }

    public void setValues(Defect d) {
        date = d.date;
        fix_date.setDate(date);
        defectNumber = d.number;
        number.setText(formatDefectNum(d.number));
        comboSelect(defect_type, d.defect_type);
        comboSelect(phase_injected, d.phase_injected);
        comboSelect(phase_removed, d.phase_removed);
        fix_time.setText(d.getLocalizedFixTime()); // will trigger fixTimeChanged
        fix_count.setText(FormatUtil.formatNumber(d.fix_count));
        fix_defect.setText(d.fix_defect);
        pendingSelector.setPending(d.fix_pending);
        description.setText(d.description);
        description.setCaretPosition(0);
        extra_attrs = d.extra_attrs;
    }

    public void dispose() {
        hide_popups();
        if (activeRefreshTimer != null) {
            activeRefreshTimer.stop();
            activeRefreshTimer.removeActionListener(this);
            activeRefreshTimer = null;
        }
        stopwatchSynchronizer.dispose();
        interruptedDialogs.remove(this); // it might not be there, that's OK
        defectDialogs.remove(comparisonKey());
        super.dispose();
    }


    /** Check to see if the removal phase is before the injection phase.
     *
     * If they are out of order, display an error message to the user and
     * return false; otherwise return true.
     */
    private boolean checkSequence() {
        if ("false".equalsIgnoreCase
            (Settings.getVal("defectDialog.restrictSequence")))
            return true;

        // Ensure that the user isn't removing a defect before it is
        // injected.
        String injected = (String)phase_injected.getSelectedItem();
        String removed  = (String)phase_removed.getSelectedItem();

        int numOptions = phase_injected.getItemCount();
        String option;
        for (int i = 0;  i < numOptions;  i++) {
            option = (String) phase_injected.getItemAt(i);
            if (option.equalsIgnoreCase(injected))
                return true;
            if (option.equalsIgnoreCase(removed)) {
                JOptionPane.showMessageDialog
                    (this,
                     resources.getStrings("Sequence_Error_Message"),
                     resources.getString("Sequence_Error_Title"),
                     JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        // We shouldn't get here...
        return true;
    }


    /** Check to ensure that the user has selected a defect type.
     *
     * If they have not, display an error message to the user and
     * return false; otherwise return true.
     */
    private boolean checkValidType() {
        if (defect_type.getSelectedIndex() > 0)
            return true;

        JOptionPane.showMessageDialog
            (this, resources.getString("Choose_Defect_Type_Message"),
             resources.getString("Choose_Defect_Type_Title"),
             JOptionPane.ERROR_MESSAGE);

        return false;
    }


    /** Check to see if the defect has been modified, prior to a "Cancel"
     * operation.
     *
     * If the defect has not been modified, return true.  If the
     * defect HAS been modified, ask the user if they really want to
     * discard their changes.  If they do, return true; otherwise
     * returns false.
     */
    private boolean checkDirty() {
        return (!isDirty ||
                JOptionPane.showConfirmDialog
                (this, resources.getString("Confirm_Cancel_Message"),
                 resources.getString("Confirm_Cancel_Title"),
                 JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION);
    }

    /** Logic supporting a click of the OK button */
    private void okButtonAction() {
        if (checkSequence() && checkValidType()) {
            maybePopDialog();
            save();
            dispose();
        }
    }

    /** Logic supporting a click of the Cancel button */
    private void cancelButtonAction() {
        if (checkDirty()) {
            maybeDeleteAutocreatedDefect();
            maybePopDialog();
            dispose();
        }
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == activeRefreshTimer)
            refreshFixTimeFromStopwatch();
        else if (e.getSource() == fixDefectButton)
            openFixDefectDialog();
        else
            // this event must be a notification of a change to one of the
            // JComboBoxes on the form.
            setDirty(true);
    }

    // Implementation of the DocumentListener interface

    private void handleDocumentEvent(DocumentEvent e) {
        if (e.getDocument() == fix_time.getDocument())
            // If the user edited the "Fix Time" field, perform the
            // necessary recalculations.
            fixTimeChanged();

        else
            // The user changed one of the other text fields on the form
            // (for example, the Fix Defect or the Description).
            setDirty(true);
    }

    public void changedUpdate(DocumentEvent e) {}
    public void insertUpdate(DocumentEvent e)  { handleDocumentEvent(e); }
    public void removeUpdate(DocumentEvent e)  { handleDocumentEvent(e); }

    // Implementation of the WindowListener interface

    public void windowOpened(WindowEvent e) {}
    public void windowClosing(WindowEvent e) { cancelButtonAction(); }
    public void windowClosed(WindowEvent e) {}
    public void windowIconified(WindowEvent e) {}
    public void windowDeiconified(WindowEvent e) {}
    public void windowActivated(WindowEvent e) {}
    public void windowDeactivated(WindowEvent e) {}

    private class StopwatchSynchronizer implements PropertyChangeListener {

        TimeLoggingModel timeLoggingModel;
        boolean pausedByTimeLoggingModel = false;

        public StopwatchSynchronizer(TimeLoggingModel timeLoggingModel) {
            this.timeLoggingModel = timeLoggingModel;
            timeLoggingModel.addPropertyChangeListener(this);
        }

        public void dispose() {
            timeLoggingModel.removePropertyChangeListener(this);
        }

        public void userToggledDefectTimer() {
            pausedByTimeLoggingModel = false;
        }

        public void propertyChange(PropertyChangeEvent evt) {
            if (TimeLoggingModel.PAUSED_PROPERTY.equals(evt.getPropertyName())) {
                boolean mainTimerIsPaused = timeLoggingModel.isPaused();
                if (mainTimerIsPaused && stopwatch.isRunning()) {
                    stopTimingDefect();
                    pausedByTimeLoggingModel = true;
                }
                if (!mainTimerIsPaused && pausedByTimeLoggingModel
                        && activeDialog == DefectDialog.this) {
                    startTimingDefect();
                    pausedByTimeLoggingModel = false;
                }
            }
        }
    }

    private class PendingSelector extends DropDownLabel {
        private boolean pending;
        private String removedLabel, pendingLabel;
        public PendingSelector() {
            removedLabel = resources.getString("Removed_Label");
            pendingLabel = resources.getString("Found_Label");
            setPending(false);
            getMenu().add(new PendingMenuOption(false));
            getMenu().add(new PendingMenuOption(true));
        }
        private boolean isPending() {
            return pending;
        }
        private void setPending(boolean fix_pending) {
            this.pending = fix_pending;
            setText(fix_pending ? pendingLabel : removedLabel);
        }
        private class PendingMenuOption extends AbstractAction {
            boolean pending;
            public PendingMenuOption(boolean pending) {
                this.pending = pending;
                String resKey = pending ? "Found_Option" : "Removed_Option";
                putValue(Action.NAME, resources.getString(resKey));
            }
            public void actionPerformed(ActionEvent e) {
                PendingSelector.this.setPending(pending);
            }
        }
    }

    private class StartStopButtons implements ActionListener {
        boolean running;
        JButton stopButton, startButton;
        public StartStopButtons() {
            stopButton = makeButton();
            startButton = makeButton();
            setRunning(false);
        }
        private JButton makeButton() {
            JButton result = new JButton();
            result.setMargin(new Insets(0,0,0,0));
            result.setFocusPainted(false);
            result.addActionListener(StartStopButtons.this);
            return result;
        }
        public void actionPerformed(ActionEvent e) {
            boolean shouldBeRunning = (e.getSource() == startButton);
            if (running != shouldBeRunning) {
                stopwatchSynchronizer.userToggledDefectTimer();
                if (shouldBeRunning)
                    startTimingDefect();
                else
                    stopTimingDefect();
                setDirty(true);
            }
        }
        private void setRunning(boolean running) {
            this.running = running;
            if (running) {
                startButton.setIcon(DashboardIconFactory.getPlayGlowingIcon());
                startButton.setToolTipText(resources.getString("Timing.Started"));
                stopButton.setIcon(DashboardIconFactory.getPauseBlackIcon());
                stopButton.setToolTipText(resources.getString("Timing.Pause"));
            } else {
                stopButton.setIcon(DashboardIconFactory.getPauseGlowingIcon());
                stopButton.setToolTipText(resources.getString("Timing.Paused"));
                startButton.setIcon(DashboardIconFactory.getPlayBlackIcon());
                startButton.setToolTipText(resources.getString("Timing.Start"));
            }
        }
    }

    private class OKButtonAction extends AbstractAction {
        public OKButtonAction() {
            super(resources.getString("OK"));
        }
        public void actionPerformed(ActionEvent e) {
            okButtonAction();
        }
    }
    private class CancelButtonAction extends AbstractAction {
        public CancelButtonAction() {
            super(resources.getString("Cancel"));
        }
        public void actionPerformed(ActionEvent e) {
            cancelButtonAction();
        }
    }
}
