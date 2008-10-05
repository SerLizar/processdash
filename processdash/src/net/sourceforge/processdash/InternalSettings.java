// Copyright (C) 2001-2006 Tuma Solutions, LLC
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

package net.sourceforge.processdash;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sourceforge.processdash.util.FileProperties;
import net.sourceforge.processdash.util.RobustFileWriter;

public class InternalSettings extends Settings {

    private static FileProperties fsettings = null;
    private static String settingsFile = null;
    private static String settingsFileRename = null;
    private static PropertyChangeSupport propSupport =
        new PropertyChangeSupport(InternalSettings.class);
    public static final String sep = System.getProperty("file.separator");
    private static boolean dirty;
    private static boolean disableChanges;

    private static final Logger logger = Logger
              .getLogger(InternalSettings.class.getName());

    public static void initialize(String settingsFilename) throws IOException {
        checkPermission("initialize");

        if (settings != null)
            return;

        String cwd  = System.getProperty("user.dir");
        String home = System.getProperty("user.home");
        homedir = home;

        InputStream in;

        // create application defaults.  First, get a set of common defaults.
        //
        defaults = defaultProperties();

        try {
            // now supplement the defaults by reading the system-wide settings file.
            // This file should be in the same directory as the Settings.class file.
            //
            in = Settings.class.getResourceAsStream("pspdash.ad");

            if (in != null) {
                Properties systemDefaults = new Properties(defaults);
                systemDefaults.load(in);
                in.close();
                filterOperatingSystemSpecificSettings(systemDefaults);
                defaults = systemDefaults;
            }

        } catch (Exception e) { e.printStackTrace(); }
        setReadOnly(readOnly);

        //
        Properties propertyComments = new Properties();
        try {
            propertyComments.load
                (Settings.class.getResourceAsStream("pspdash.ad-comments"));
        } catch (Exception e0) {}

        // finally, open the user's settings file and load those properties.  The
        // default search path for these user settings is:
        //    * the current directory
        //    * the user's home directory (specified by the system property
        //          "user.home")
        //
        // on Windows/Mac systems, this will look for a file named "pspdash.ini".
        // on all other platforms, it will look for a file named ".pspdash".
        //
        settings = fsettings = new FileProperties(defaults, propertyComments);
        fsettings.setDateStamping(false);

        String filename = getSettingsFilename();
        dirty = disableChanges = false;

        filename = checkForOldSettingsFile(cwd, filename);
        if (settingsFileRename != null)
            dirty = true;

        File settingsFile;
        if (settingsFilename != null && settingsFilename.length() != 0) {
            // if the caller has specified a particular settings file, use it.
            settingsFile = new File(settingsFilename);
        } else {
            // search for an existing settings file
            String cwdFilename = cwd + sep + filename;
            File cwdFile = new File(cwdFilename);
            String homeFilename = home + sep + filename;
            File homeFile = new File(homeFilename);
            if (cwdFile.isFile()) {
                // first, look in the current working directory.
                settingsFile = cwdFile;
                settingsFilename = cwdFilename;
                homedir = cwd;
            } else if (homeFile.isFile()) {
                // next, check the user's home directory.
                settingsFile = homeFile;
                settingsFilename = homeFilename;
                homedir = home;
            } else {
                // if no file was found, default to the current working directory.
                System.out.println("could not read user preferences file from any of");
                System.out.println("     " + cwdFilename);
                System.out.println("     " + homeFilename);
                System.out.println("...using system-wide defaults.");
                settingsFile = cwdFile;
                settingsFilename = cwdFilename;
                homedir = cwd;
            }
        }

        if (!settingsFile.isFile()) {
            // if the file doesn't exist, make a note that we need to save it.
            dirty = true;
        } else {
            try {
                in = new FileInputStream(settingsFile);
                fsettings.load(in);
                in.close();
            } catch (Exception e) {
                // the settings file exists, but we were unable to read it.  Throw
                // an exception whose message is the filename we tried to read.
                IOException ioe = new IOException(settingsFilename);
                ioe.initCause(e);
                throw ioe;
            }
        }
        InternalSettings.settingsFile = settingsFilename;
        fsettings.setHeader(PROPERTIES_FILE_HEADER);
        fsettings.setKeepingStrangeKeys(true);

    }
    private static void filterOperatingSystemSpecificSettings(Properties settings) {
        String os = "os-" + getOSPrefix() + ".";
        Map matchingValues = new HashMap();
        for (Iterator i = settings.entrySet().iterator(); i.hasNext();) {
            Map.Entry e = (Map.Entry) i.next();
            String key = (String) e.getKey();
            if (key.startsWith("os-")) {
                if (key.startsWith(os))
                    matchingValues.put(key.substring(os.length()), e.getValue());
                i.remove();
            }
        }
        settings.putAll(matchingValues);
    }
    static String getOSPrefix() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.indexOf("windows") != -1)
            return "windows";
        else if (os.startsWith("mac"))
            return "mac";
        else if (os.indexOf("linux") != -1)
            return "linux";
        else
            return "unix";
    }
    private static final String getSettingsFilename() {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.startsWith("win") || osName.startsWith("mac"))
            return "pspdash.ini";
        else
            return ".pspdash";
    }
    /**
     * On Mac OS X, we have historically used ".pspdash" for the settings file.
     * However, 'dot' files are nearly impossible for the average user to find.
     * The method above has changed the default settings file name for the Mac -
     * but if the user has a legacy file, we need to load it and migrate it to
     * the new name.  This method checks for the existence of a legacy settings
     * file, and possibly prepares for a file rename.
     * 
     * @param cwd the directory to look in
     * @param desiredName the name we'd like the file to have
     */
    private static String checkForOldSettingsFile(String cwd, String desiredName) {
        // not on a Mac? do nothing.
        if (!System.getProperty("os.name").toLowerCase().startsWith("mac"))
            return desiredName;

        // if the settings file already exists with the new name, do nothing.
        File desiredFile = new File(cwd, desiredName);
        if (desiredFile.isFile())
            return desiredName;

        // look for an older settings file.  If it exists, tell our calling code
        // to read from the older file.  But make a note of the name we prefer
        // to use, so the save logic can rename the file later.
        File oldFile = new File(cwd, ".pspdash");
        if (oldFile.isFile()) {
            settingsFileRename = desiredFile.getPath();
            return ".pspdash";
        }

        // no settings file exists with either the new or the old name.  Just
        // return the new name so our caller can use it to proceed.
        return desiredName;
    }
    public static String getSettingsFileName() {
        checkPermission("getFileName");
        return settingsFile;
    }
    private static final String PROPERTIES_FILE_HEADER =
        "User preferences for the PSP Dashboard tool " +
        "(NOTE: When specifying names of files or directories within this " +
        "file, use a forward slash as a separator.  It will be translated " +
        "into an appropriate OS-specific directory separator automatically.)";

    public static void set(String name, String value, String comment) {
        set0(name, value, comment);
    }

    public static void set(String name, String value) {
        set0(name, value, null);
    }

    private static synchronized void set0(String name, String value,
              String comment) {
        checkPermission("write."+name);

        if (disableChanges)
            return;

        String oldValue = fsettings.getProperty(name);

        if (value == null)
            fsettings.remove(name);

        else {
            fsettings.put(name, value);
            if (comment != null)
                fsettings.setComment(name, comment);
        }

        String propName = SYS_PROP_PREFIX + name;
        if (System.getProperty(propName) != null)
            System.getProperties().remove(propName);

        serializable = null;
        dirty = true;

        saveSettings();

        propSupport.firePropertyChange(name, oldValue, value);
    }

    public static String getExtendableVal(String name, String sep) {
        String result = getVal(name);
        String extra = getVal("additional." + name);
        if (extra != null) {
            if (result == null)
                result = extra;
            else
                result = result + sep + extra;
            set(name, result);
            set("additional." + name, null);
        }
        return result;
    }

    static synchronized void saveSettings() {
        if (isReadOnly())
            return;

        AccessController.doPrivileged(new PrivilegedAction() {
            public Object run() {
                if (fsettings != null) try {
                    String oldName, destName;
                    if (settingsFileRename == null) {
                        oldName = null;
                        destName = settingsFile;
                    } else {
                        oldName = settingsFile;
                        destName = settingsFileRename;
                    }

                    Writer out = new RobustFileWriter(destName);
                    fsettings.store(out);
                    out.close();

                    if (oldName != null) {
                        new File(oldName).delete();
                        settingsFile = destName;
                        settingsFileRename = null;
                    }

                    dirty = false;
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Unable to save settings file.", e);
                }
                return null;
            }});
    }

    public static synchronized boolean isDirty() {
        return dirty;
    }

    static void setReadOnly(boolean ro) {
        Settings.readOnly = ro;
        if (defaults != null) {
            if (ro)
                defaults.put(READ_ONLY, "true");
            else
                defaults.remove(READ_ONLY);
        }
    }

    static synchronized void setDisableChanges(boolean disable) {
        disableChanges = disable;
        logger.fine("Settings changes "
                + (disableChanges ? "disabled." : "enabled."));
    }

    public static void addPropertyChangeListener(PropertyChangeListener l) {
        propSupport.addPropertyChangeListener(l);
    }
    public static void addPropertyChangeListener(String propertyName, PropertyChangeListener l) {
        propSupport.addPropertyChangeListener(propertyName, l);
    }

    public static void removePropertyChangeListener(PropertyChangeListener l) {
        propSupport.removePropertyChangeListener(l);
    }
    public static void removePropertyChangeListener(String propertyName, PropertyChangeListener l) {
        propSupport.removePropertyChangeListener(propertyName, l);
    }

    static void loadLocaleSpecificDefaults(ResourceBundle resources) {
        checkPermission("initialize");
        defaults.put("dateFormat", resources.getString("Date_Format"));
        defaults.put("dateTimeFormat", resources.getString("Date_Time_Format"));
        defaults.put("http.charset", resources.getString("HTTP_charset_"));
    }

    /** This main method is used to merge settings from one or more external
     * sources into a user's settings file.  It is typically only called by
     * the dashboard installer as an optional post-installation step.
     */
    public static void main(String[] args) {
        if (args.length < 2)
            return;
        File destDir = new File(args[0]);
        if (!destDir.isDirectory())
            return;

        String filename = checkForOldSettingsFile(destDir.getPath(),
              getSettingsFilename());
        String rename = settingsFileRename;
        File settingsFile = new File(destDir, filename);
        try {
            initialize(settingsFile.getPath());
            settingsFileRename = rename;

            for (int i = args.length;  i-- > 1;)
                tryToMerge(args[i]);
        } catch (Exception e) {
        }
    }

    private static void tryToMerge(String url) {
        if ("none".equalsIgnoreCase(url))
            return;

        Properties nullProps = new Properties();
        FileProperties propsIn = new FileProperties(nullProps, nullProps);
        try {
            propsIn.load(new URL(url).openStream());
        } catch (Exception e) {}
        if (propsIn.isEmpty())
            return;

        for (Iterator i = propsIn.entrySet().iterator(); i.hasNext();) {
            Map.Entry e = (Map.Entry) i.next();
            String propKey = ((String) e.getKey()).trim();
            if (!propKey.startsWith(MERGE_PROP_PREFIX))
                continue;

            String settingName = propKey.substring(MERGE_PROP_PREFIX.length());
            if (getVal(settingName) == null) {
                String settingVal = ((String) e.getValue()).trim();
                set(settingName, settingVal);
            }
        }
    }

    private static final String MERGE_PROP_PREFIX = "pdash.";
}