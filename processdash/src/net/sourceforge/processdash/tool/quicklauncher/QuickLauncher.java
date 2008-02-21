// Copyright (C) 2006-2008 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
//
// The author(s) may be contacted at:
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC: processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.tool.quicklauncher;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Frame;
import java.beans.EventHandler;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableColumnModel;

import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.templates.DashPackage;
import net.sourceforge.processdash.ui.DashboardIconFactory;
import net.sourceforge.processdash.ui.macosx.MacGUIUtils;
import net.sourceforge.processdash.util.ConcurrencyLock;
import net.sourceforge.processdash.util.FallbackObjectFactory;
import net.sourceforge.processdash.util.Initializable;


public class QuickLauncher {

    static Resources resources = Resources.getDashBundle("QuickLauncher");

    public static void main(String[] args) {
        String fileToLaunch = null;
        if (args.length > 0)
            fileToLaunch = args[0];

        new QuickLauncher(fileToLaunch);
    }

    ConcurrencyLock lock;

    boolean launchInSameVm;

    DashboardProcessFactory processFactory;

    InstanceLauncherFactory launcherFactory;

    InstanceList instanceList;

    InstanceActionHandler actionHandler;

    JFrame frame;

    public QuickLauncher(String fileToLaunch) {
        launchInSameVm = Boolean.getBoolean("quickLauncher.sameJVM");

        if (!launchInSameVm)
            try {
                getLock(fileToLaunch);
            } catch (ConcurrencyLock.SentMessageException clsme) {
                System.exit(0);
            } catch (Exception e) {
            }

        CompressedInstanceLauncher.cleanupOldDirectories();

        MacGUIUtils.tweakLookAndFeel();

        try {
            if (launchInSameVm)
                processFactory = new DashboardProcessFactorySameJVM();
            else
                processFactory = new DashboardProcessFactoryForking();

            processFactory.addVmArg("-D" + Settings.SYS_PROP_PREFIX
                    + "backup.extraDirectories=");

            String userLang = System.getProperty("user.language");
            if (userLang != null)
                processFactory.addVmArg("-Duser.language=" + userLang);

            String maxMem = System.getProperty("maxMemory");
            if (maxMem != null)
                processFactory.addVmArg("-Xmx" + maxMem);

            instanceList = new InstanceList();
            processFactory.addVmArg("-D"
                    + ProcessDashboard.NOTIFY_ON_OPEN_PORT_PROPERTY + "="
                    + instanceList.getNotificationPort());

            buildUI();

            if (fileToLaunch != null)
                launchFilename(fileToLaunch);

            new FallbackObjectFactory<Initializable>(Initializable.class)
                .add(MAC_HELPER_CLASS).get().initialize(this);

        } catch (Exception e) {
            abortWithError(e.getMessage());
        }
    }

    private void getLock(String fileToLaunch) throws IOException,
            ConcurrencyLock.FailureException {
        File tempFile = File.createTempFile("foo", "bar");
        tempFile.delete();
        File tempDir = tempFile.getParentFile();
        String lockFileName = "pdash-quicklaunch-lock-"
                + System.getProperty("user.name") + "-"
                + InetAddress.getLocalHost().getHostAddress().replace('.', '-');
        File lockFile = new File(tempDir, lockFileName);

        LockListener listener = new LockListener();

        lock = new ConcurrencyLock(lockFile,
                listener.getStartupCommand(fileToLaunch), listener);
    }

    private void buildUI() throws Exception {
        String windowTitle;
        String versionNumber = getVersionNumber();
        if (versionNumber == null)
            windowTitle = resources.getString("Window_Title");
        else
            windowTitle = resources.format("Window_Title2_FMT", versionNumber);
        frame = new JFrame(windowTitle);
        DashboardIconFactory.setLauncherWindowIcon(frame);

        JPanel contents = new JPanel();
        contents.setLayout(new BoxLayout(contents, BoxLayout.Y_AXIS));
        launcherFactory = new InstanceLauncherFactory();
        DropTransferHandler th = new DropTransferHandler(this, launcherFactory);
        contents.setTransferHandler(th);

        contents.add(new JLabel(resources.getString("Window_Prompt")));

        contents.add(new OptionCheckbox("Read_Only", "-DreadOnly=true", null,
                processFactory, false));
        contents.add(new OptionCheckbox("Disable_Export", "-D"
                + Settings.SYS_PROP_PREFIX + "export.disableAutoExport=true",
                null, processFactory, true));
        contents.add(new OptionCheckbox("Use_External_Templates", null, "-D"
                + Settings.SYS_PROP_PREFIX + "templates.directory=",
                processFactory, false));

        JTable table = new JTable(instanceList);
        TableColumnModel cols = table.getColumnModel();
        for (int i = 0;  i < InstanceList.COLUMN_WIDTHS.length;  i++) {
            int width = InstanceList.COLUMN_WIDTHS[i];
            cols.getColumn(i).setPreferredWidth(width);
        }
        table.getSelectionModel().setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION);
        table.setTransferHandler(th);
        table.setPreferredScrollableViewportSize(new Dimension(380, 100));
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBackground(Color.WHITE);
        contents.add(scrollPane);

        actionHandler = new InstanceActionHandler(table, instanceList);

        instanceList.addTableModelListener((TableModelListener) EventHandler
                .create(TableModelListener.class, this, "updateCursor"));

        frame.getContentPane().add(contents);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    private static String getVersionNumber() {
        try {
            return QuickLauncher.class.getPackage().getImplementationVersion();
        } catch (Exception e) {
            return null;
        }
    }

    public void updateCursor() {
        frame.setCursor(instanceList.hasLaunching()
                ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : null);
    }

    static void showError(Object message) {
        JOptionPane.showMessageDialog(null, message, resources
                .getString("Errors.Dialog_Title"), JOptionPane.ERROR_MESSAGE);
    }

    private void abortWithError(Object message) {
        showError(message);
        System.exit(1);
    }

    public void quit() {
        if (!launchInSameVm || frame.isVisible())
            System.exit(0);
    }

    public boolean useDashboardJarFile(File f) {
        if (!(processFactory instanceof DashboardProcessFactoryForking)) {
            JOptionPane.showMessageDialog(frame,
                    resources.getStrings("Errors.SameJVMCannotSwitch"),
                    resources.getString("Errors.Dialog_Title"),
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }

        String packageID = null;
        String version = null;
        String mainClass = null;
        try {
            JarFile jar = new JarFile(f);
            Manifest mf = jar.getManifest();
            Attributes attrs = mf.getMainAttributes();
            packageID = attrs.getValue(DashPackage.ID_ATTRIBUTE);
            version = attrs.getValue(DashPackage.VERSION_ATTRIBUTE);
            mainClass = attrs.getValue("Main-Class");
            jar.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (!"pspdash".equals(packageID) || version == null || mainClass == null) {
            JOptionPane.showMessageDialog(frame,
                    resources.format("Errors.Not_Pspdash_Jar_FMT",
                            f.getAbsolutePath()),
                    resources.getString("Errors.Dialog_Title"),
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }

        DashboardProcessFactoryForking pf =
            (DashboardProcessFactoryForking) processFactory;
        pf.setClasspath(f.getAbsolutePath());
        pf.setMainClassName(mainClass);

        String windowTitle = resources.format("Window_Title3_FMT", version, f
                .getParent());
        frame.setTitle(windowTitle);
        return true;
    }

    public void launchInstances(Collection launchers) {
        for (Iterator i = launchers.iterator(); i.hasNext();) {
            DashboardInstance inst = (DashboardInstance) i.next();
            int pos = instanceList.indexOfSimilar(inst);
            if (pos == -1)
                new LaunchThread(inst).start();
            else
                actionHandler.bringToFront(pos);
        }
    }

    public void launchFilename(String filename) {
        File f = new File(filename);
        DashboardInstance inst = launcherFactory.getLauncher(
                frame.getContentPane(), f);
        if (inst != null)
            launchInstances(Collections.singleton(inst));
    }


    private class LaunchThread extends Thread {

        DashboardInstance instance;

        public LaunchThread(DashboardInstance instance) {
            super();
            this.instance = instance;
        }

        public void run() {
            try {
                String display = instance.getDisplay();
                if (display == null)
                    instance.setDisplay(resources.getString("Launcher.Display"));

                instanceList.addInstance(instance);
                instance.launch(processFactory);

                if (launchInSameVm)
                    frame.dispose();
                else
                    instance.waitForCompletion();
            } catch (LaunchException le) {
                le.printStackTrace();
                showError(le.getMessage());
            } finally {
                instanceList.removeInstance(instance);
            }
        }

    }

    private class LockListener implements ConcurrencyLock.Listener {

        private static final String COMMAND_OPEN = "OPEN:";
        private static final String COMMAND_RAISE = "RAISE";

        public String handleMessage(String message) throws Exception {
            if (COMMAND_RAISE.equals(message)) {
                raiseWindow();
                return "OK";

            } else if (message != null && message.startsWith(COMMAND_OPEN)) {
                String filename = message.substring(COMMAND_OPEN.length());
                raiseWindow();
                launchFilename(filename);
                return "OK";

            } else {
                return "ERROR";
            }
        }

        private void raiseWindow() {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() { raiseWindowImpl(); } } );
        }

        private void raiseWindowImpl() {
            if (frame.getState() == Frame.ICONIFIED)
                frame.setState(Frame.NORMAL);
            frame.setVisible(true);
            frame.toFront();
        }

        public String getStartupCommand(String filename) {
            if (filename == null)
                return COMMAND_RAISE;
            else
                return COMMAND_OPEN + filename;
        }

    }

    private static final String MAC_HELPER_CLASS =
        "net.sourceforge.processdash.ui.macosx.LauncherMacOSXHelper";

}
