// Copyright (C) 2007 Tuma Solutions, LLC
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.StringTokenizer;

import javax.swing.JOptionPane;

/**
 * This class verifies that JRE version is supported before reflectively
 * delegating startup to <code>ProcessDashboard.main()</code> class. Reflection is
 * used to eliminate hard coded dependency on any ProcessDashboard class. The
 * reason is that this class must be compiled with lower target level in order to
 * support previous JRE, such as 1.4 and 1.3. Even if underlying JRE is not supported,
 * the application should be able to display meaningfull message without crashing at startup.
 * 
 * @author Max Agapov <magapov@gmail.com>
 * 
 */
public class Main {

    /**
     * Ensure that underlying JRE is supported. Exit if not.
     */
    private static void ensureJRE() {
        Version jre = new Version(System.getProperty("java.version"));
        Version req = new Version("1.5");
        if (jre.compareTo(req) < 0) {
            ResourceBundle res = ResourceBundle
                    .getBundle("Templates.resources.ProcessDashboard");
            String vendorURL = System.getProperty("java.vendor.url");

            String titleFmt = res.getString("Errors.JRE_Requirement_Title_FMT");
            String title = MessageFormat.format(titleFmt,
                new Object[] { req.toString() });

            String msgFmt = res.getString("Errors.JRE_Requirement_Message_FMT");
            String message = MessageFormat.format(msgFmt,
                new Object[] { jre.toString(), vendorURL, req.toString() });

            JOptionPane.showMessageDialog(null, message, title,
                JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
    }

    /**
     * Reflectively invoke <code>ProcessDashboard.main()</code>
     * 
     * @param args,
     *            command line arguments
     */
    private static void invokeProcessDash(String[] args) {
        try {
            // check for class existence
            // this may throw ClassNotFoundException
            Class clazz = Class
                    .forName("net.sourceforge.processdash.ProcessDashboard");
            // reflectively invoke main method to
            Method main = clazz.getMethod("main",
                new Class[] { String[].class });
            main.invoke(clazz, new Object[] { args });
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * The main entry point.
     * 
     * @param args,
     *            command line arguments
     */
    public static void main(String[] args) {
        ensureJRE();
        invokeProcessDash(args);
    }

    /**
     * Utility class for version comparison. The version is expected to be in
     * this format:<br>
     * <b>major.minor[.other]</b><br>
     * Only major and minor parts of the version name are taken into account.<br>
     * This class implements its own parsing in order to minimize dependency<br>
     * on API that might not be available in earlier versions of JRE.
     */
    public static class Version implements Comparable {

        private static final char DOT = '.';

        private int major = 0, minor = 0;

        private String version = "";

        private Version() {}

        /**
         * @param version,
         *            like "1.2"
         */
        public Version(String version) {
            this.version = version;
            List split = splitString(DOT, "0" + version);
            if (!split.isEmpty()) {
                try {
                    major = Integer.parseInt((String) split.get(0));
                } catch (NumberFormatException e) {
                    // do nothing
                }
            }
            if (split.size() > 1) {
                try {
                    minor = Integer.parseInt((String) split.get(1));
                } catch (NumberFormatException e) {
                    // do nothing
                }
            }
        }

        /**
         * @param delimiter,
         *            the character delimiter, like '.'
         * @param version,
         *            i.e "1.2"
         * @return list of substrings
         */
        private List splitString(char delimiter, String version) {
            List split = new ArrayList();
            StringTokenizer st = new StringTokenizer(version, String.valueOf(delimiter));
            while (st.hasMoreTokens()) {
                split.add(st.nextElement());
            }
            return split;
        }

        public int compareTo(Object o) {
            Version other = (Version) o;
            if (major != other.getMajor()) {
                return major - other.getMajor();
            }
            if (minor != other.getMinor()) {
                return minor - other.getMinor();
            }
            return 0;
        }

        public int getMajor() {
            return major;
        }

        public int getMinor() {
            return minor;
        }

        public String toString() {
            return version;
        }
    }
}
