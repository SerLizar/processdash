package teamdash.wbs;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import net.sourceforge.processdash.tool.bridge.client.BridgedWorkingDirectory;
import net.sourceforge.processdash.tool.bridge.client.WorkingDirectory;
import net.sourceforge.processdash.tool.bridge.client.WorkingDirectoryFactory;
import net.sourceforge.processdash.tool.export.mgr.ExternalLocationMapper;
import net.sourceforge.processdash.ui.macosx.MacGUIUtils;
import net.sourceforge.processdash.util.DashboardBackupFactory;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.PreferencesUtils;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.lock.AlreadyLockedException;
import net.sourceforge.processdash.util.lock.LockFailureException;
import net.sourceforge.processdash.util.lock.LockMessage;
import net.sourceforge.processdash.util.lock.LockMessageHandler;
import net.sourceforge.processdash.util.lock.LockUncertainException;
import net.sourceforge.processdash.util.lock.ReadOnlyLockFailureException;
import net.sourceforge.processdash.util.lock.SentLockMessageException;
import teamdash.SaveListener;
import teamdash.team.TeamMemberListEditor;
import teamdash.wbs.WBSTabPanel.LoadTabsException;
import teamdash.wbs.columns.PercentCompleteColumn;
import teamdash.wbs.columns.PercentSpentColumn;
import teamdash.wbs.columns.SizeAccountingColumnSet;
import teamdash.wbs.columns.TeamActualTimeColumn;
import teamdash.wbs.columns.TeamCompletionDateColumn;
import teamdash.wbs.columns.TeamTimeColumn;
import teamdash.wbs.columns.UnassignedTimeColumn;

public class WBSEditor implements WindowListener, SaveListener,
        LockMessageHandler {

    public static final String INTENT_WBS_EDITOR = "showWbsEditor";
    public static final String INTENT_TEAM_EDITOR = "showTeamListEditor";

    WorkingDirectory workingDirectory;
    TeamProject teamProject;
    MilestonesDataModel milestonesModel;
    JFrame frame;
    WBSTabPanel tabPanel;
    TeamTimePanel teamTimePanel;
    WBSDataWriter dataWriter;
    File dataDumpFile;
    WBSDataWriter workflowWriter;
    File workflowDumpFile;
    WBSSynchronizer reverseSynchronizer;
    File customTabsFile;
    private String owner;
    private int mode;
    boolean showActualData = false;
    boolean readOnly = false;
    boolean exitOnClose = false;
    String syncURL = null;
    boolean disposed = false;

    private TeamMemberListEditor teamListEditor = null;
    private WorkflowEditor workflowEditor = null;
    private MilestonesEditor milestonesEditor = null;

    private static final int MODE_PLAIN = 1;
    private static final int MODE_HAS_MASTER = 2;
    private static final int MODE_MASTER = 4;
    private static final int MODE_BOTTOM_UP = 8;

    private static Preferences preferences = Preferences.userNodeForPackage(WBSEditor.class);
    private static final String EXPANDED_NODES_KEY_SUFFIX = "_EXPANDEDNODES";
    private static final String EXPANDED_NODES_DELIMITER = Character.toString('\u0001');
    private static final String DATA_DUMP_FILE = "projDump.xml";
    private static final String WORKFLOW_DUMP_FILE = "workflowDump.xml";
    private static final String CUSTOM_TABS_FILE = "tabs.xml";

    public WBSEditor(WorkingDirectory workingDirectory,
            TeamProject teamProject, String owner) throws LockFailureException {

        this.workingDirectory = workingDirectory;
        this.teamProject = teamProject;
        acquireLock(owner);

        MacGUIUtils.tweakLookAndFeel();

        File storageDir = teamProject.getStorageDirectory();
        this.dataDumpFile = new File(storageDir, DATA_DUMP_FILE);
        this.workflowDumpFile = new File(storageDir, WORKFLOW_DUMP_FILE);
        this.customTabsFile = new File(storageDir, CUSTOM_TABS_FILE);
        this.readOnly = teamProject.isReadOnly();

        setMode(teamProject);

        if (isMode(MODE_HAS_MASTER) && !readOnly) {
            MasterWBSUtil.mergeFromMaster(teamProject);
        }

        WBSModel model = teamProject.getWBS();

        // set expanded nodes on model based on saved user preferences
        Set expandedNodes = getExpandedNodesPref(teamProject.getProjectID());
        if (expandedNodes != null) {
            model.setExpandedNodeIDs(expandedNodes);
        }

        TaskDependencySource taskDependencySource = getTaskDependencySource();
        DataTableModel data = new DataTableModel
            (model, teamProject.getTeamMemberList(),
             teamProject.getTeamProcess(), teamProject.getMilestones(),
             taskDependencySource, owner);

        milestonesModel = new MilestonesDataModel(teamProject.getMilestones());

        if (isMode(MODE_PLAIN)) {
            reverseSynchronizer = new WBSSynchronizer(teamProject, data);
            reverseSynchronizer.run();
            showActualData = reverseSynchronizer.getFoundActualData();
        }

        dataWriter = new WBSDataWriter(model, data,
                teamProject.getTeamProcess(), teamProject.getProjectID(),
                teamProject.getTeamMemberList());
        workflowWriter = new WBSDataWriter(teamProject.getWorkflows(), null,
                teamProject.getTeamProcess(), teamProject.getProjectID(), null);
        if (!readOnly && workingDirectory != null) {
            try {
                workingDirectory.doBackup("startup");
            } catch (IOException e) {}
        }
        this.owner = owner;

        tabPanel = new WBSTabPanel(model, data, teamProject.getTeamProcess(),
                taskDependencySource);
        tabPanel.setReadOnly(readOnly);
        teamProject.getTeamMemberList().addInitialsListener(tabPanel);

        String[] sizeMetrics = teamProject.getTeamProcess().getSizeMetrics();
        String[] sizeTabColIDs = new String[sizeMetrics.length+2];
        String[] sizeTabColNames = new String[sizeMetrics.length+2];
        sizeTabColIDs[0] = "Size";       sizeTabColNames[0] = "Size";
        sizeTabColIDs[1] = "Size-Units"; sizeTabColNames[1] = "Units";
        for (int i = 0; i < sizeMetrics.length; i++) {
            sizeTabColIDs[i+2] = SizeAccountingColumnSet.getNCID(sizeMetrics[i]);
            sizeTabColNames[i+2] = sizeMetrics[i];
        }
        tabPanel.addTab("Size", sizeTabColIDs, sizeTabColNames);

        tabPanel.addTab("Size Accounting",
                     new String[] { "Size-Units", "Base", "Deleted", "Modified", "Added",
                                    "Reused", "N&C", "Total" },
                     new String[] { "Units",  "Base", "Deleted", "Modified", "Added",
                                    "Reused", "N&C", "Total" });

        if (!isMode(MODE_MASTER))
            tabPanel.addTab((showActualData ? "Planned Time" : "Time"),
                     new String[] { TeamTimeColumn.COLUMN_ID,
                                    WBSTabPanel.TEAM_MEMBER_PLAN_TIMES_ID,
                                    UnassignedTimeColumn.COLUMN_ID },
                     new String[] { "Team", "", "Unassigned" });

        tabPanel.addTab("Task Time",
                new String[] { "Phase", "Task Size", "Task Size Units", "Rate",
                        ifMode(MODE_PLAIN, "Hrs/Indiv"),
                        ifMode(MODE_PLAIN, "# People"),
                        (isMode(MODE_MASTER) ? "TimeNoErr" : "Time"),
                        ifNotMode(MODE_MASTER, "Assigned To"),
                        (showActualData ? TeamCompletionDateColumn.COLUMN_ID : null),
                        (showActualData ? PercentCompleteColumn.COLUMN_ID : null),
                        (showActualData ? PercentSpentColumn.COLUMN_ID : null),
                        (showActualData ? TeamActualTimeColumn.COLUMN_ID : null) },
                new String[] { "Phase/Type", "Task Size", "Units", "Rate",
                        "Hrs/Indiv", "# People", "Time", "Assigned To",
                        "Completed", "%C", "%S", "Actual Time" });

        tabPanel.addTab("Task Details",
                new String[] { "Milestone", "Labels", "Dependencies", "Notes" },
                new String[] { "Milestone", "Task Labels", "Task Dependencies", "Notes" });

        if (showActualData)
            tabPanel.addTab("Actual Time",
                new String[] { TeamActualTimeColumn.COLUMN_ID,
                               WBSTabPanel.TEAM_MEMBER_ACTUAL_TIMES_ID },
                new String[] { "Team", "" });

        //String[] s = new String[] { "P", "O", "N", "M", "L", "K", "J", "I", "H", "G", "F" };
        //table.addTab("Defects", s, s);

        // read in custom tabs file
        try {
            tabPanel.loadTabs(customTabsFile);
        } catch (LoadTabsException e) {
        }

        teamTimePanel =
            new TeamTimePanel(teamProject.getTeamMemberList(), data,
                milestonesModel);
        teamTimePanel.setVisible(isMode(MODE_BOTTOM_UP));
        if (isMode(MODE_BOTTOM_UP))
            teamTimePanel.setShowBalancedBar(false);
        teamTimePanel.setShowRemainingWork(showActualData == true);

        try {
            new MacOSXWBSHelper(this);
        } catch (Throwable t) {}

        frame = new JFrame(teamProject.getProjectName()
                + " - Work Breakdown Structure"
                + (teamProject.isReadOnly() ? " (Read-Only)" : ""));
        frame.setJMenuBar(buildMenuBar(tabPanel, teamProject.getWorkflows(),
            teamProject.getMilestones()));
        frame.getContentPane().add(tabPanel);
        frame.getContentPane().add(teamTimePanel, BorderLayout.SOUTH);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(this);
        frame.pack();
    }

    private void acquireLock(String owner) throws LockFailureException {
        if (teamProject.isReadOnly() || workingDirectory == null)
            return;

        try {
            workingDirectory.acquireWriteLock(this, owner);
        } catch (ReadOnlyLockFailureException e) {
            if (showFilesAreReadOnlyMessage(teamProject, workingDirectory
                    .getDescription()) == false)
                throw e;
        } catch (LockFailureException e) {
            String otherOwner = null;
            if (e instanceof AlreadyLockedException)
                otherOwner = ((AlreadyLockedException) e).getExtraInfo();
            if (otherOwner == null)
                otherOwner = "someone on another machine";
            CONCURRENCY_MESSAGE[1] += (otherOwner + ".");

            int userResponse = JOptionPane.showConfirmDialog(null,
                    CONCURRENCY_MESSAGE, "Open Project in Read-Only Mode",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (userResponse == JOptionPane.YES_OPTION)
                teamProject.setReadOnly(true);
            else
                throw e;
        }
    }
    private static final String[] CONCURRENCY_MESSAGE = {
        "The Work Breakdown Structure for this project is currently",
        "open for editing by ",
        " ",
        "Would you like to open the project anyway, in read-only mode?"
    };

    private void setMode(TeamProject teamProject) {
        if (teamProject instanceof TeamProjectBottomUp)
            this.mode = MODE_BOTTOM_UP;
        else if (teamProject.isMasterProject())
            this.mode = MODE_MASTER;
        else {
            this.mode = MODE_PLAIN;
            if (teamProject.getMasterProjectDirectory() != null)
                this.mode |= MODE_HAS_MASTER;
        }
    }

    private String ifMode(int m, String id) {
        return (isMode(m) ? id : null);
    }
    private String ifNotMode(int m, String id) {
        return (isMode(m) ? null : id);
    }
    private boolean isMode(int m) {
        return ((mode & m) == m);
    }

    private TaskDependencySource getTaskDependencySource() {
        if (isMode(MODE_PLAIN + MODE_HAS_MASTER))
            return new TaskDependencySourceMaster(teamProject);
        else
            return new TaskDependencySourceSimple(teamProject);
    }

    public void setExitOnClose(boolean exitOnClose) {
        this.exitOnClose = exitOnClose;
    }

    private void setSyncURL(String syncURL) {
        this.syncURL = syncURL;
    }

    public boolean isDisposed() {
        return disposed;
    }

    public void show() {
        frame.setVisible(true);
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
    }

    public String handleMessage(LockMessage lockMessage) {
        String message = lockMessage.getMessage();
        if (INTENT_TEAM_EDITOR.equals(message)) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    showTeamListEditor();
                }});
            return "OK";
        }
        if (INTENT_WBS_EDITOR.equals(message)) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    raiseWindow();
                }});
            return "OK";
        }
        if (LockMessage.LOCK_LOST_MESSAGE.equals(message)) {
            if (readOnly == false) {
                readOnly = true;
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        showLostLockMessage();
                    }});
            }
            return "OK";
        }
        throw new IllegalArgumentException();
    }

    public void raiseWindow() {
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.setVisible(true);
        frame.toFront();
    }

    public void showTeamListEditor() {
        if (teamListEditor != null)
            teamListEditor.show();
        else {
            teamListEditor = new TeamMemberListEditor
                (teamProject.getProjectName(), teamProject.getTeamMemberList());
            teamListEditor.addSaveListener(this);
        }
    }

    public void showLostLockMessage() {
        readOnly = true;
        tabPanel.setReadOnly(true);
        saveAction.setEnabled(false);
        importFromCsvAction.setEnabled(false);

        JOptionPane.showMessageDialog(frame, LOST_LOCK_MESSAGE,
                "Network Connectivity Problems", JOptionPane.ERROR_MESSAGE);
    }
    private static final String[] LOST_LOCK_MESSAGE = {
        "Although you opened the work breakdown structure in read-write",
        "mode, your connection to the network was broken, and your lock",
        "on the WBS was lost.  In the meantime, another individual has",
        "opened the work breakdown structure for editing, so your lock",
        "could not be reclaimed.",
        " ",
        "As a result, you will no longer be able to save changes to the",
        "work breakdown structure.  You are strongly encouraged to close",
        "and reopen the work breakdown structure editor."
    };

    private void showWorkflowEditor() {
        if (workflowEditor != null)
            workflowEditor.show();
        else {
            workflowEditor = new WorkflowEditor(teamProject);
            //workflowEditor.addSaveListener(this);
        }
    }

    private void showMilestonesEditor() {
        if (milestonesEditor != null)
            milestonesEditor.show();
        else {
            milestonesEditor = new MilestonesEditor(teamProject, milestonesModel);
        }
        milestonesEditor.show();
    }

    private JMenuBar buildMenuBar(WBSTabPanel tabPanel, WBSModel workflows,
            WBSModel milestones) {
        JMenuBar result = new JMenuBar();

        result.add(buildFileMenu(tabPanel.getFileActions()));
        result.add(buildEditMenu(tabPanel.getEditingActions()));
        result.add(buildTabMenu(tabPanel.getTabActions()));
        if (!isMode(MODE_BOTTOM_UP))
            result.add(buildWorkflowMenu
                (workflows, tabPanel.getInsertWorkflowAction(workflows)));
        result.add(buildMilestonesMenu(milestones));
        if (isMode(MODE_HAS_MASTER)
                && "true".equals(teamProject.getUserSetting("showMasterMenu")))
            result.add(buildMasterMenu(tabPanel.getMasterActions(teamProject)));
        if (!isMode(MODE_MASTER))
            result.add(buildTeamMenu());

        return result;
    }
    private Action saveAction, importFromCsvAction;
    private JMenu buildFileMenu(Action[] fileActions) {
        JMenu result = new JMenu("File");
        result.setMnemonic('F');
        result.add(saveAction = new SaveAction());
        if (!isMode(MODE_BOTTOM_UP))
            result.add(importFromCsvAction = new ImportFromCsvAction());
        for (int i = 0; i < fileActions.length; i++) {
            result.add(fileActions[i]);
        }
        result.addSeparator();
        result.add(new CloseAction());
        return result;
    }
    private JMenu buildEditMenu(Action[] editingActions) {
        JMenu result = new JMenu("Edit");
        result.setMnemonic('E');
        for (int i = 0;   i < editingActions.length;   i++) {
            result.add(editingActions[i]);
            if (i == 1) result.addSeparator();
        }

        return result;
    }
    private JMenu buildTabMenu(Action[] tabActions) {
        JMenu result = new JMenu("Tabs");
        result.setMnemonic('A');
        for (int i = 0; i < tabActions.length; i++) {
            result.add(tabActions[i]);
            if (i == 2 || i == 4) result.addSeparator();
        }

        return result;
    }
    private JMenu buildWorkflowMenu(WBSModel workflows,
                                    Action insertWorkflowAction) {
        JMenu result = new JMenu("Workflow");
        result.setMnemonic('W');
        result.add(new WorkflowEditorAction());
        // result.add(new DefineWorkflowAction());
        result.addSeparator();
        new WorkflowMenuBuilder(result, workflows, insertWorkflowAction);
        return result;
    }
    private JMenu buildMilestonesMenu(WBSModel milestones) {
        JMenu result = new JMenu("Milestones");
        result.setMnemonic('M');
        result.add(new MilestonesEditorAction());
        if (!isMode(MODE_MASTER)) {
            result.addSeparator();
            result.add(new ShowCommitDatesMenuItem());
            result.add(new ShowMilestoneMarksMenuItem());
            new BalanceMilestoneMenuBuilder(result, milestones);
        }
        return result;
    }
    private JMenu buildMasterMenu(Action[] masterActions) {
        JMenu result = new JMenu("Master");
        result.setMnemonic('S');
        for (int i = 0;   i < masterActions.length;   i++)
            result.add(masterActions[i]);
        return result;
    }
    private JMenu buildTeamMenu() {
        JMenu result = new JMenu("Team");
        result.setMnemonic('T');
        if (isMode(MODE_PLAIN))
            result.add(new ShowTeamMemberListEditorMenuItem());
        result.add(new ShowTeamTimePanelMenuItem());
        if (showActualData) {
            ButtonGroup g = new ButtonGroup();
            result.add(new BottomUpShowReplanMenuItem(g));
            result.add(new BottomUpShowPlanMenuItem(g));
        }
        result.add(new BottomUpIncludeUnassignedMenuItem());
        return result;
    }

    private boolean save() {
        if (readOnly)
            return true;

        tabPanel.stopCellEditing();

        JDialog dialog = createWaitDialog(frame, "Saving Data...");
        SaveThread saver = new SaveThread(dialog);
        saver.start();
        dialog.setVisible(true);
        return saver.saveResult;
    }

    private static JDialog createWaitDialog(JFrame frame, String message) {
        JDialog dialog = new JDialog(frame, "Please Wait", true);
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialog.getContentPane().add(buildWaitContents(message));
        dialog.pack();
        dialog.setLocationRelativeTo(frame);
        return dialog;
    }

    private static JFrame createWaitFrame(String message) {
        JFrame result = new JFrame("Please Wait");
        result.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        result.getContentPane().add(buildWaitContents(message));
        result.pack();
        result.setLocationRelativeTo(null);
        return result;
    }

    private static JPanel buildWaitContents(String message) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(7, 7, 7, 7));

        JLabel label = new JLabel(message);
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 7, 10));
        panel.add(label, BorderLayout.NORTH);

        JProgressBar bar = new JProgressBar();
        bar.setIndeterminate(true);
        panel.add(bar, BorderLayout.CENTER);

        panel.add(Box.createHorizontalStrut(200), BorderLayout.SOUTH);
        return panel;
    }

    private class SaveThread extends Thread {
        JDialog saveDialog;
        boolean saveResult;

        public SaveThread(JDialog saveDialog) {
            this.saveDialog = saveDialog;
        }

        public void run() {
            saveResult = saveImpl();
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() {
                        JPanel panel = (JPanel) saveDialog.getContentPane()
                                .getComponent(0);
                        for (int i = 0;  i < panel.getComponentCount(); i++) {
                            Component c = panel.getComponent(i);
                            if (c instanceof JLabel) {
                                JLabel label = (JLabel) c;
                                label.setText("Data Saved.");
                            } else if (c instanceof JProgressBar) {
                                JProgressBar bar = (JProgressBar) c;
                                bar.setIndeterminate(false);
                                bar.setValue(bar.getMaximum());
                            }
                        }
                        saveDialog.setTitle("Data Saved");
                    }});
                Thread.sleep(750);
            } catch (Exception e) {}

            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    saveDialog.dispose();
                }});
        }

    }

    private boolean saveImpl() {
        if (readOnly)
            return true;

        try {
            if (saveData()) {
                maybeTriggerSyncOperation();
                return true;
            }

        } catch (LockUncertainException lue) {
        } catch (LockFailureException fe) {
            showLostLockMessage();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
        }

        showSaveErrorMessage();
        return false;
    }

    private boolean saveData() throws LockFailureException, IOException {
        if (readOnly || workingDirectory == null)
            return true;

        workingDirectory.assertWriteLock();

        if (teamProject.save() == false)
            return false;

        dataWriter.write(dataDumpFile);
        workflowWriter.write(workflowDumpFile);

        // write out custom tabs file
        tabPanel.saveTabs(customTabsFile);

        if (workingDirectory.flushData() == false)
            return false;

        String qualifier = "saved";
        if (owner != null && owner.trim().length() > 0)
            qualifier = "saved_by_" + FileUtils.makeSafe(owner.trim());
        workingDirectory.doBackup(qualifier);

        return true;
    }

    private void showSaveErrorMessage() {
        if (workingDirectory instanceof BridgedWorkingDirectory) {
            SAVE_ERROR_MSG[3] = BRIDGED_ADVICE;
            SAVE_ERROR_MSG[4] = "";
        } else {
            SAVE_ERROR_MSG[3] = LOCAL_ADVICE;
            SAVE_ERROR_MSG[4] = "      " + workingDirectory.getDescription();
        }

        JOptionPane.showMessageDialog(frame, SAVE_ERROR_MSG,
                "Unable to Save", JOptionPane.ERROR_MESSAGE);
    }

    private static final String[] SAVE_ERROR_MSG = {
        "The Work Breakdown Structure Editor encountered an unexpected error",
        "and was unable to save data. This problem might have been caused by",
        "poor network connectivity, or by read-only file permissions. Please",
        "",
        "",
        " ",
        "Then, try saving again. If you shut down the Work Breakdown Structure",
        "Editor without resolving this problem, any changes you have made will",
        "be lost."
    };
    private static final String LOCAL_ADVICE =
        "check to ensure that you can write to the following location:";
    private static final String BRIDGED_ADVICE =
        "doublecheck your network connection.";

    private void maybeTriggerSyncOperation() {
        if (syncURL != null)
            new SyncTriggerThread().start();
    }

    private class SyncTriggerThread extends Thread {
        public void run() {
            try {
                URL u = new URL(syncURL);
                URLConnection conn = u.openConnection();
                InputStream in = new BufferedInputStream(conn
                        .getInputStream());
                while (in.read() != -1)
                    ;
                in.close();
            } catch (Exception e) {}
        }
    }

    /** Give the user a chance to save data before the window closes.
     * 
     * @return false if the user selects cancel, true otherwise
     */
    private boolean maybeSave(boolean showCancel) {
        if (readOnly)
            return true;

        int buttons =
            (showCancel
                ? JOptionPane.YES_NO_CANCEL_OPTION
                : JOptionPane.YES_NO_OPTION);
        int result = JOptionPane.showConfirmDialog
            (frame, "Would you like to save changes?",
             "Save Changes?", buttons);
        switch (result) {
            case JOptionPane.CANCEL_OPTION:
            case JOptionPane.CLOSED_OPTION:
                return false;

            case JOptionPane.YES_OPTION:
                if (save() == false)
                    return false;
                break;
        }

        return true;
    }

    protected void maybeClose() {
        tabPanel.stopCellEditing();
        if (maybeSave(true)) {
            // Set expanded nodes preference
            Set expandedNodes = teamProject.getWBS().getExpandedNodeIDs();
            setExpandedNodesPref(teamProject.getProjectID(), expandedNodes);

            if (workingDirectory != null)
                workingDirectory.releaseLocks();

            if (exitOnClose)
                System.exit(0);
            else {
                if (teamListEditor != null) teamListEditor.hide();
                if (workflowEditor != null) workflowEditor.hide();
                if (milestonesEditor != null) milestonesEditor.hide();
                frame.dispose();
                disposed = true;
            }
        }
    }

    public void windowOpened(WindowEvent e) {}
    public void windowClosing(WindowEvent e) {
        maybeClose();
    }
    public void windowClosed(WindowEvent e) {}
    public void windowIconified(WindowEvent e) {}
    public void windowDeiconified(WindowEvent e) {}
    public void windowActivated(WindowEvent e) {}
    public void windowDeactivated(WindowEvent e) {}

    public static WBSEditor createAndShowEditor(String location,
            boolean bottomUp, boolean showTeamList, String syncURL,
            boolean exitOnClose, boolean forceReadOnly, String owner) {

        String message = (showTeamList
                ? "Opening Team Member List..."
                : "Opening Work Breakdown Structure...");
        JFrame waitFrame = createWaitFrame(message);
        waitFrame.setVisible(true);

        LockMessageDispatcher dispatch;
        WorkingDirectory workingDirectory;
        File dir;
        TeamProject proj;

        if (bottomUp)
        {
            proj = new TeamProjectBottomUp(location, "Team Project");
            dir = proj.getStorageDirectory();
            if (!dir.isDirectory()) {
                waitFrame.dispose();
                showBadFilenameError(location);
                return null;
            }
            workingDirectory = null;
            dispatch = null;
        }
        else // if not bottom up
        {
            String intent = showTeamList ? INTENT_TEAM_EDITOR : INTENT_WBS_EDITOR;
            dispatch = new LockMessageDispatcher();
            workingDirectory = configureWorkingDirectory(location, intent,
                dispatch);
            if (workingDirectory == null) {
                waitFrame.dispose();
                return null;
            }
            dir = workingDirectory.getDirectory();
            proj = new TeamProject(dir, "Team Project");
        }

        if (forceReadOnly)
            proj.setReadOnly(true);

        if (owner == null && !forceReadOnly)
            owner = getOwnerName();

        try {
            WBSEditor w = new WBSEditor(workingDirectory, proj, owner);
            w.setExitOnClose(exitOnClose);
            w.setSyncURL(syncURL);
            if (showTeamList)
                w.showTeamListEditor();
            else
                w.show();

            if (dispatch != null)
                dispatch.setEditor(w);
            waitFrame.dispose();
            return w;
        } catch (LockFailureException e) {
            workingDirectory.releaseLocks();
            if (exitOnClose)
                System.exit(0);
            waitFrame.dispose();
            return null;
        }
    }

    private static WorkingDirectory configureWorkingDirectory(String location,
            String intent, LockMessageHandler handler) {
        DashboardBackupFactory.setKeepBackupsNumDays(30);
        WorkingDirectory workingDirectory = WorkingDirectoryFactory
                .getInstance().get(location, WorkingDirectoryFactory.PURPOSE_WBS);
        String locationDescr = workingDirectory.getDescription();

        try {
            workingDirectory.acquireProcessLock(intent, handler);
        } catch (SentLockMessageException s) {
            // another WBS Editor is running, and it handled the request for us.
            return null;
        } catch (LockFailureException e) {
            e.printStackTrace();
            showLockFailureError();
            return null;
        }

        boolean workingDirIsGood = false;
        try {
            workingDirectory.prepare();
            File dir = workingDirectory.getDirectory();
            workingDirIsGood = dir.isDirectory();
        } catch (IOException e) {
            // do nothing.  An exception means that "workingDirIsGood" will
            // remain false, so we will display an error message below.
        }

        if (workingDirIsGood) {
            return workingDirectory;
        }
        else {
            if (workingDirectory instanceof BridgedWorkingDirectory) {
                showBadServerError(locationDescr);
            } else {
                showBadFilenameError(locationDescr);
            }
            return null;
        }
    }

    private static void showLockFailureError() {
        String[] message = new String[] {
                "The Work Breakdown Structure Editor encountered an",
                "unexpected error during startup: another process",
                "on this computer seems to be locking the WBS data",
                "for this project.  To prevent data corruption, this",
                "program must exit.",
                " ",
                "Try launching the WBS Editor again.  If you still",
                "receive this message, it restarting your computer may",
                "be necessary."
        };
        JOptionPane.showMessageDialog(null, message, "Data Locking Error",
            JOptionPane.ERROR_MESSAGE);
    }

    private static void showBadServerError(String url) {
        String[] message = new String[] {
                "The Work Breakdown Structure Editor attempted to read",
                "project data from the following location:",
                "        " + url,
                "Unfortunately, this server could not be contacted.  Make",
                "certain you are connected to the network and try again." };
        JOptionPane.showMessageDialog(null, message,
                "Could not Open Project Files", JOptionPane.ERROR_MESSAGE);
    }

    private static void showBadFilenameError(String filename) {
        String[] message = new String[] {
                "The Work Breakdown Structure Editor attempted to open",
                "project data located in the directory:",
                "        " + filename,
                "Unfortunately, this directory could not be found.  You",
                "may need to map a network drive to edit this data." };
        JOptionPane.showMessageDialog(null, message,
                "Could not Open Project Files", JOptionPane.ERROR_MESSAGE);
    }

    private static class LockMessageDispatcher implements LockMessageHandler {

        private List<LockMessage> missedMessages = new ArrayList<LockMessage>();

        private WBSEditor editor = null;

        public synchronized void setEditor(WBSEditor editor) {
            this.editor = editor;
            for (LockMessage msg : missedMessages) {
                editor.handleMessage(msg);
            }
        }

        public synchronized String handleMessage(LockMessage e) throws Exception {
            if (editor != null) {
                return editor.handleMessage(e);
            } else {
                missedMessages.add(e);
                return "OK";
            }
        }

    }

    private static boolean showFilesAreReadOnlyMessage(TeamProject teamProject,
            String location) {
        READ_ONLY_FILES_MESSAGE[2] = "      " + location;
        int userResponse = JOptionPane.showConfirmDialog(null,
                READ_ONLY_FILES_MESSAGE, "Open Project in Read-Only Mode",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (userResponse == JOptionPane.YES_OPTION) {
            teamProject.setReadOnly(true);
            return true;
        }
        return false;
    }
    private static final String[] READ_ONLY_FILES_MESSAGE = {
        "The Work Breakdown Structure Editor stores data for this project",
        "into XML files located at:",
        "",
        " ",
        "Unfortunately, the current filesystem permissions do not allow",
        "you to modify those files.  Would you like to open the project",
        "anyway, in read-only mode?"
    };

    private static String getOwnerName() {
        String result = preferences.get("ownerName", null);
        if (result == null) {
            result = JOptionPane.showInputDialog(null, INPUT_NAME_MESSAGE,
                    "Enter Your Name", JOptionPane.PLAIN_MESSAGE);
            if (result != null)
                preferences.put("ownerName", result);
        }
        return result;
    }
    private static final String INPUT_NAME_MESSAGE =
        "To open the Work Breakdown Structure, please enter your name:";

    private String getExpandedNodesKey(String projectId) {
        return projectId + EXPANDED_NODES_KEY_SUFFIX;
    }

    private Set getExpandedNodesPref(String projectId) {
        String value = PreferencesUtils.getCLOB(preferences,
                getExpandedNodesKey(projectId), null);
        if (value == null)
            return null;

        String[] nodesArray = value.split(EXPANDED_NODES_DELIMITER);
        Set nodesToExpand = new HashSet(Arrays.asList(nodesArray));

        return nodesToExpand;
    }

    private void setExpandedNodesPref(String projectId, Set value) {
        PreferencesUtils.putCLOB(preferences, getExpandedNodesKey(projectId),
                StringUtils.join(value, EXPANDED_NODES_DELIMITER));
    }

    public static void main(String args[]) {
        ExternalLocationMapper.getInstance().loadDefaultMappings();

        String location = null;
        if (args.length > 0)
            location = args[0];

        boolean bottomUp = Boolean.getBoolean("teamdash.wbs.bottomUp");
        boolean showTeam = Boolean.getBoolean("teamdash.wbs.showTeamMemberList");
        boolean readOnly = Boolean.getBoolean("teamdash.wbs.readOnly");
        String syncURL = System.getProperty("teamdash.wbs.syncURL");
        String owner = System.getProperty("teamdash.wbs.owner");
        createAndShowEditor(location, bottomUp, showTeam, syncURL, true,
            readOnly, owner);
    }


    private class SaveAction extends AbstractAction {
        public SaveAction() {
            super("Save");
            putValue(MNEMONIC_KEY, new Integer('S'));
            setEnabled(!readOnly);
        }
        public void actionPerformed(ActionEvent e) {
            save();
        }
    }

    private class ImportFromCsvAction extends AbstractAction {
        public ImportFromCsvAction() {
            super("Import from MS Project CSV file...");
            putValue(MNEMONIC_KEY, new Integer('I'));
            setEnabled(readOnly == false);
        }

        public void actionPerformed(ActionEvent e) {
            CsvNodeDataImporterUI ui = new CsvNodeDataImporterUI();
            ui.run(tabPanel.wbsTable, teamProject.getTeamMemberList());
        }
    }

    private class CloseAction extends AbstractAction {
        public CloseAction() {
            super("Close");
            putValue(MNEMONIC_KEY, new Integer('C'));
        }

        public void actionPerformed(ActionEvent e) {
            maybeClose();
        }
    }

    private class WorkflowEditorAction extends AbstractAction {
        public WorkflowEditorAction() {
            super("Edit Workflows");
            putValue(MNEMONIC_KEY, new Integer('E'));
        }
        public void actionPerformed(ActionEvent e) {
            showWorkflowEditor();
        }
    }

    private class WorkflowMenuBuilder implements TableModelListener {
        private JMenu menu;
        private int initialMenuLength;
        private WBSModel workflows;
        private Action insertWorkflowAction;
        private ArrayList itemList;

        public WorkflowMenuBuilder(JMenu menu, WBSModel workflows,
                                   Action insertWorkflowAction) {
            this.menu = menu;
            this.initialMenuLength = menu.getItemCount();
            this.workflows = workflows;
            this.insertWorkflowAction = insertWorkflowAction;
            this.itemList = new ArrayList();
            rebuildMenu();
            workflows.addTableModelListener(this);
        }

        private void rebuildMenu() {
            ArrayList newList = new ArrayList();
            WBSNode[] workflowItems =
                workflows.getChildren(workflows.getRoot());
            for (int i = 0;   i < workflowItems.length;   i++) {
                String workflowName = workflowItems[i].getName();
                if (!newList.contains(workflowName))
                    newList.add(workflowName);
            }

            synchronized (menu) {
                if (newList.equals(itemList)) return;

                while (menu.getItemCount() > initialMenuLength)
                    menu.remove(initialMenuLength);
                Iterator i = newList.iterator();
                while (i.hasNext()) {
                    String workflowItemName = (String) i.next();
                    JMenuItem menuItem = new JMenuItem(insertWorkflowAction);
                    menuItem.setActionCommand(workflowItemName);
                    menuItem.setText(workflowItemName);
                    menu.add(menuItem);
                }

                itemList = newList;
            }
        }

        public void tableChanged(TableModelEvent e) {
            rebuildMenu();
        }
    }


    private class MilestonesEditorAction extends AbstractAction {
        public MilestonesEditorAction() {
            super("Edit Milestones");
            putValue(MNEMONIC_KEY, new Integer('E'));
        }

        public void actionPerformed(ActionEvent e) {
            showMilestonesEditor();
        }
    }

    private class ShowCommitDatesMenuItem extends JCheckBoxMenuItem implements
            ChangeListener {
        public ShowCommitDatesMenuItem() {
            super("Show Commit Dates on Balancing Panel");
            setSelected(true);
            addChangeListener(this);
        }

        public void stateChanged(ChangeEvent e) {
            teamTimePanel.setShowCommitDates(getState());
        }
    }

    private class ShowMilestoneMarksMenuItem extends JCheckBoxMenuItem
            implements ChangeListener {
        public ShowMilestoneMarksMenuItem() {
            super("Show Milestone Marks for Team Members");
            setSelected(true);
            addChangeListener(this);
        }

        public void stateChanged(ChangeEvent e) {
            teamTimePanel.setShowMilestoneMarks(getState());
        }
    }

    private class BalanceMilestoneMenuBuilder implements TableModelListener,
            ActionListener {
        private JMenu menu;
        private int initialMenuLength;
        private WBSModel milestonesWbs;
        private int selectedMilestoneID;
        private ButtonGroup group;
        private Border indentBorder;

        public BalanceMilestoneMenuBuilder(JMenu menu, WBSModel milestones) {
            this.menu = menu;
            menu.add(new JMenuItem("Balance Work Through:"));
            this.initialMenuLength = menu.getItemCount();
            this.milestonesWbs = milestones;
            this.selectedMilestoneID = -1;
            rebuildMenu();
            milestones.addTableModelListener(this);

        }

        private void rebuildMenu() {
            WBSNode[] milestones =
                milestonesWbs.getChildren(milestonesWbs.getRoot());

            synchronized (menu) {
                while (menu.getItemCount() > initialMenuLength)
                    menu.remove(initialMenuLength);

                group = new ButtonGroup();
                addMenuItem("( Entire WBS )", -1);
                for (WBSNode milestone : milestones) {
                    String name = milestone.getName();
                    int uniqueID = milestone.getUniqueID();
                    if (name != null && name.trim().length() > 0)
                        addMenuItem(name, uniqueID);
                }
            }
        }

        private void addMenuItem(String name, int uniqueID) {
            JMenuItem menuItem = new JRadioButtonMenuItem(name);
            menuItem.setActionCommand(Integer.toString(uniqueID));
            group.add(menuItem);
            if (uniqueID == selectedMilestoneID || uniqueID == -1)
                menuItem.setSelected(true);

            if (indentBorder == null)
                indentBorder = BorderFactory.createCompoundBorder(
                    menuItem.getBorder(), new EmptyBorder(0, 15, 0, 0));
            menuItem.setBorder(indentBorder);

            if (uniqueID == -1)
                menuItem.setFont(menuItem.getFont().deriveFont(
                    Font.ITALIC + Font.BOLD));

            menuItem.addActionListener(this);
            menu.add(menuItem);
        }


        public void tableChanged(TableModelEvent e) {
            rebuildMenu();
        }

        public void actionPerformed(ActionEvent e) {
            try {
                String newSelection = e.getActionCommand();
                selectedMilestoneID = Integer.parseInt(newSelection);
                teamTimePanel.setBalanceThroughMilestone(selectedMilestoneID);
            } catch (Exception ex) {}
        }
    }



    private class ShowTeamMemberListEditorMenuItem extends AbstractAction {
        public ShowTeamMemberListEditorMenuItem() {
            super("Edit Team Member List");
            putValue(MNEMONIC_KEY, new Integer('E'));
        }
        public void actionPerformed(ActionEvent e) {
            showTeamListEditor();
        }
    }


    private class ShowTeamTimePanelMenuItem extends JCheckBoxMenuItem
    implements ChangeListener {
        public ShowTeamTimePanelMenuItem() {
            super("Show Bottom Up Time Panel");
            setMnemonic('B');
            setSelected(teamTimePanel.isVisible());
            addChangeListener(this);
        }
        public void stateChanged(ChangeEvent e) {
            teamTimePanel.setVisible(getState());
            frame.invalidate();
        }
    }

    private class BottomUpShowReplanMenuItem extends JRadioButtonMenuItem
    implements ChangeListener {
        public BottomUpShowReplanMenuItem (ButtonGroup buttonGroup) {
            super("Colored Bars Show Remaining Work (Replan)");
            setMnemonic('R');
            setDisplayedMnemonicIndex(getText().indexOf('R'));
            setSelected(true);
            setBorder(BorderFactory.createCompoundBorder(getBorder(),
                new EmptyBorder(0, 15, 0, 0)));
            buttonGroup.add(this);
            addChangeListener(this);
        }

        public void stateChanged(ChangeEvent e) {
            teamTimePanel.setShowRemainingWork(isSelected());
        }
    }

    private class BottomUpShowPlanMenuItem extends JRadioButtonMenuItem {
        public BottomUpShowPlanMenuItem (ButtonGroup buttonGroup) {
            super("Colored Bars Show End-to-End Plan");
            setMnemonic('P');
            setBorder(BorderFactory.createCompoundBorder(getBorder(),
                new EmptyBorder(0, 15, 0, 0)));
            buttonGroup.add(this);
        }
    }

    private class BottomUpIncludeUnassignedMenuItem extends JCheckBoxMenuItem
            implements ChangeListener {
        public BottomUpIncludeUnassignedMenuItem() {
            super("Include Unassigned Effort in Balanced Team Calculation");
            setSelected(true);
            setBorder(BorderFactory.createCompoundBorder(getBorder(),
                new EmptyBorder(0, 15, 0, 0)));
            addChangeListener(this);
        }

        public void stateChanged(ChangeEvent e) {
            teamTimePanel.setIncludeUnassigned(isSelected());
        }
    }

    public void itemSaved(Object item) {
        if (item == teamListEditor) {
            if (!frame.isVisible()) {
                // The frame containing the WBSEditor itself is not visible.
                // Thus, the user must have just opened the team member list
                // only, edited, and clicked the save button. It will fall to us
                // to save files on the team member list's behalf.  Since they
                // might have edited initials (which alters the data model),
                // we need to save the entire team project, not just the team
                // member list.  It is safe to do this without asking, because
                // the WBS and workflows have never been displayed, so they must
                // not have been otherwise altered. Finally, since no GUI
                // windows will be visible anymore, we should exit.
                save();
                if (exitOnClose)
                    System.exit(0);
            }
        }
    }

    public void itemCancelled(Object item) {
        if (item == teamListEditor) {
            if (exitOnClose && !frame.isVisible())
                System.exit(0);
        }
    }

}