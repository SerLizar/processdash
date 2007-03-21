// Copyright (C) 2003-2007 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// The author(s) may be contacted at:
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash;

import java.awt.Frame;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.ev.EVTaskList;
import net.sourceforge.processdash.ev.ui.TaskScheduleChooser;
import net.sourceforge.processdash.hier.HierarchyAlterer;
import net.sourceforge.processdash.hier.Prop;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.log.time.DashboardTimeLog;
import net.sourceforge.processdash.net.http.WebServer;
import net.sourceforge.processdash.process.ui.TriggerURI;
import net.sourceforge.processdash.security.DashboardPermission;
import net.sourceforge.processdash.templates.ui.ImportTemplatePermissionDialog;
import net.sourceforge.processdash.tool.export.mgr.AbstractInstruction;
import net.sourceforge.processdash.tool.export.mgr.CompletionStatus;
import net.sourceforge.processdash.tool.export.mgr.ExportManager;
import net.sourceforge.processdash.tool.export.mgr.ImportDirectoryInstruction;
import net.sourceforge.processdash.tool.export.mgr.ImportManager;


public class DashController {

    static ProcessDashboard dash = null;
    private static String localAddress = "127.0.0.1";
    private static DashboardPermission PERMISSION =
        new DashboardPermission("dashController");
    private static final Logger logger = Logger.getLogger(DashController.class
            .getName());

    public static void setDashboard(ProcessDashboard dashboard) {
        dash = dashboard;
        try {
            localAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (IOException ioe) {}
    }

    public static void checkIP(Object remoteAddress) throws IOException {
        PERMISSION.checkPermission();
        if (!"127.0.0.1".equals(remoteAddress) &&
            !localAddress.equals(remoteAddress))
            throw new IOException("Connection not accepted from: " + remoteAddress);
    }

    public static void raiseWindow() {
        SwingUtilities.invokeLater(new Runnable() {
                public void run() { raiseWindowImpl(); } } );
    }

    private static void raiseWindowImpl() {
        if (dash.getState() == Frame.ICONIFIED)
            dash.setState(Frame.NORMAL);
        dash.show();
        dash.toFront();
    }

    public static void showTaskSchedule(final String path) {
        SwingUtilities.invokeLater(new Runnable() {
                public void run() { showTaskScheduleImpl(path); } } );
    }
    private static void showTaskScheduleImpl(String path) {

        // if no path was given, just display a chooser dialog to the user.
        if (path == null || path.length() == 0) {
            raiseWindowImpl();
            new TaskScheduleChooser(dash);
            return;
        }

        List taskLists = EVTaskList.getTaskListNamesForPath(dash.data,
                path);

        raiseWindow();
        if (taskLists.size() == 1)
            TaskScheduleChooser.open(dash, (String) taskLists.get(0));
        else
            new TaskScheduleChooser
                (dash, (String[]) taskLists.toArray(new String[0]));
    }

    public static void exportData(String prefix) {
        exportDataForPrefix(prefix);
    }
    public static CompletionStatus exportDataForPrefix(String prefix) {
        String dataName = DataRepository.createDataName
            (prefix, ExportManager.EXPORT_DATANAME);
        AbstractInstruction instr = ExportManager.getInstance()
                .getExportInstructionFromData(dataName);
        Runnable task = null;
        if (instr != null)
            task = ExportManager.getInstance().getExporter(instr);
        if (task == null)
            return new CompletionStatus(CompletionStatus.NO_WORK_NEEDED,
                    null, null);

        task.run();

        if (task instanceof CompletionStatus.Capable) {
            CompletionStatus result = ((CompletionStatus.Capable) task)
                    .getCompletionStatus();
            if (result != null && result.getException() != null)
                logger.log(Level.WARNING, "Error exporting data for '" + prefix
                        + "'", result.getException());
            return result;
        }

        return new CompletionStatus(CompletionStatus.SUCCESS, null, null);
    }

    public static void exportAllData() {
        ExportManager.getInstance().exportAll(dash, dash);
    }

    public static File backupData() {
        List unsavedData = dash.saveAllData();
        if (!unsavedData.isEmpty())
            return null;

        return FileBackupManager.run(dash.property_directory,
                FileBackupManager.RUNNING, null);
    }

    public static void startTiming() {
        SwingUtilities.invokeLater(new Runnable() {
                public void run() { startTimingImpl(); } } );
    }
    private static void startTimingImpl() {
        DashboardTimeLog tl = (DashboardTimeLog) dash.getTimeLog();
        tl.getTimeLoggingModel().startTiming();
    }

    public static void stopTiming()  {
        SwingUtilities.invokeLater(new Runnable() {
                public void run() { stopTimingImpl(); } } );
    }
    private static void stopTimingImpl()  {
        DashboardTimeLog tl = (DashboardTimeLog) dash.getTimeLog();
        tl.getTimeLoggingModel().stopTiming();
    }


    private static boolean setPathSuccessful;
    public static boolean setPath(final String path) {
        if (SwingUtilities.isEventDispatchThread())
            return setPathImpl(path);
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() { setPathSuccessful = setPathImpl(path); }});
        } catch (Exception e) {
            setPathSuccessful = false;
        }
        return setPathSuccessful;
    }
    private static boolean setPathImpl(String path) {
        return dash.pause_button.setPath(path);
    }

    private static boolean setPhaseSuccessful;
    public static boolean setPhase(final String phase) {
        if (SwingUtilities.isEventDispatchThread())
            return setPhaseImpl(phase);
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() { setPhaseSuccessful = setPhaseImpl(phase); }});
        } catch (Exception e) {
            setPhaseSuccessful = false;
        }
        return setPhaseSuccessful;
    }
    private static boolean setPhaseImpl(String phase) {
        return dash.pause_button.setPhase(phase);
    }

    public static void printNullDocument(PrintWriter out) {
        out.println(TriggerURI.NULL_DOCUMENT_MARKER);
        out.println("<HTML><HEAD><SCRIPT>");
        out.println("history.back();");
        out.println("</SCRIPT></HEAD><BODY></BODY></HTML>");
    }

    public static Map getTemplates() {
        Prop templates = dash.templates.pget(PropertyKey.ROOT);
        TreeMap result = new TreeMap();
        for (int i = templates.getNumChildren();   i-- > 0; ) {
            PropertyKey childKey = templates.getChild(i);
            Prop child = dash.templates.pget(childKey);
            result.put(child.getID(), Prop.unqualifiedName(childKey.name()));
        }
        return result;
    }

    public static boolean isHierarchyEditorOpen() {
        return dash.configure_button.isHierarchyEditorOpen();
    }

    public static void addTemplateDirToPath(String templateDir,
                                            boolean create) {
        if (ImportTemplatePermissionDialog.askUserForPermission
            (dash, null, templateDir, create) == true)
            addTemplateDirToTemplatePath(templateDir);
    }

    private static void addTemplateDirToTemplatePath(String templateDir) {
        if (templateDir == null) return;
        templateDir = templateDir.replace('\\', '/');

        String templatePath = Settings.getVal(TEMPLATE_PATH);
        if (templatePath == null)
            InternalSettings.set(TEMPLATE_PATH, templateDir);

        else if (!templateSettingContainsDir(templatePath, templateDir)) {
            templatePath = templateDir + ";" + templatePath;
            InternalSettings.set(TEMPLATE_PATH, templatePath);
        }
    }
    private static final String TEMPLATE_PATH = "templates.directory";
    private static boolean templateSettingContainsDir(String setting,
                                                      String dir)
    {
        setting = ";" + setting + ";";
        dir     = ";" + dir     + ";";
        return setting.indexOf(dir) != -1;
    }

    public static boolean alterTemplateID(String prefix,
                                          String oldID,
                                          String newID) {
        if (Settings.isReadOnly())
            return false;

        PropertyKey key = dash.props.findExistingKey(prefix);
        String actualID = dash.props.getID(key);
        if (oldID == actualID || // handles "null == null" case
            (oldID != null && oldID.equals(actualID))) try {
            HierarchyAlterer a = new HierarchyAlterer(dash);
            a.addTemplate(prefix, newID);
            return true;
        } catch (Exception e) {}

        return false;
    }

    public static boolean loadNewTemplate(String jarfileName,
                                          String templateDir,
                                          boolean create)
    {
        if (ImportTemplatePermissionDialog.askUserForPermission
            (dash, jarfileName, templateDir, create) == false)
            return false;

        if (dash.addTemplateJar(jarfileName) == false)
            return false;

        addTemplateDirToTemplatePath(templateDir);
        return true;
    }

    public static HierarchyAlterer getHierarchyAlterer() {
        return new HierarchyAlterer(dash);
    }
    public static String getSettingsFileName() {
        return InternalSettings.getSettingsFileName();
    }
    public static void addImportSetting(String prefix, String importDir) {
        ImportDirectoryInstruction instr = new ImportDirectoryInstruction(
                importDir, prefix);
        ImportManager.getInstance().addInstruction(instr);
    }

    public static void enableTeamSettings() {
        if (Settings.isReadOnly())
            return;

        // enable earned value rollups.
        InternalSettings.set(EV_ROLLUP, "true");

        // export more often.
        InternalSettings.set(EXPORT_TIMES, "*");

        // listen on any address, if we aren't already.
        InternalSettings.set(WebServer.HTTP_ALLOWREMOTE_SETTING, "true");

        // listen on a repeatable port.
        String port = Settings.getVal(HTTP_PORT);
        if (port == null) {
            int portNum = getAvailablePort();
            InternalSettings.set(HTTP_PORT, Integer.toString(portNum));
        }
    }

    private static int getAvailablePort() {
        for (int i = 0;   i < PORT_PATTERNS.length;   i++)
            for (int j = 3;   j < 10;   j++)
                if (isPortAvailable(PORT_PATTERNS[i]*j))
                    return PORT_PATTERNS[i]*j;
        return 3000;
    }

    private static boolean isPortAvailable(int port) {
        if (port < 1024) return false;
        boolean successful = false;
        try {
            ServerSocket a = new ServerSocket(port-1);
            ServerSocket b = new ServerSocket(port);
            successful = true;
            a.close();
            b.close();
        } catch (IOException ioe) {}
        return successful;
    }

    private static final String EV_ROLLUP = "ev.enableRollup";
    private static final String EXPORT_TIMES =
        ExportManager.EXPORT_TIMES_SETTING;
    private static final String HTTP_PORT = ProcessDashboard.HTTP_PORT_SETTING;
    private static final int[] PORT_PATTERNS = {
        1000, 1111, 1001, 1010, 1100, 1011, 1101, 1110 };

}