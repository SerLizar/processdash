// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2003 Software Process Dashboard Initiative
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

package net.sourceforge.processdash.data.applet;


import pspdash.Settings;

import java.applet.Applet;
import java.applet.AudioClip;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLEncoder;

import net.sourceforge.processdash.data.repository.*;


public class DataApplet extends Applet implements RepositoryClientListener {

    protected volatile boolean isRunning = false;
    RepositoryClient data = null;
    String dataPrefix = null;
    String requiredTag = null;
    static int ieVersion = -1;
    static int nsVersion = -1;
    String errorMsg = null;
    protected HTMLFieldManager mgr = null;
    AudioClip dataStoredSound = null;
    public static boolean debug = false;


    public DataApplet() {}


    protected void printError(Exception e) {
        System.err.println("Exception: " + e);
        e.printStackTrace(System.err);
    }


    protected void debug(String msg) {
        if (debug)
            System.out.println("DataApplet: " + msg);
    }

    public void repositoryClientStopping() {
        debug("RepositoryClientListener stopping applet...");
        stop();
        debug("RepositoryClientListener starting applet...");
        start();
    }

    public void start() {
        debug("starting...");
        avoidMicrosoftJVM();
        readParameters();
        isRunning = true;

        if (createFieldManager()) {
            initData();
            initSound();
            if (isRunning && mgr != null)
                mgr.initialize(data, (data == null ? errorMsg : dataPrefix));
        } else
            isRunning = false;
    }




    protected void readParameters() {
        requiredTag = getParameter("requiredTag");
        ieVersion = getIntParameter("ieVersion");
        nsVersion = getIntParameter("nsVersion");
        debug = getBoolParameter("debug", false);
        debug("url="+getDocumentBase()+", requiredTag="+requiredTag);
    }

    protected void initData() {
        this.data = null;
        try {
            RepositoryClient data =
                new RepositoryClient(getDocumentBase(), requiredTag);
            dataPrefix = data.getDataPath();
            data.addRepositoryClientListener(this);
            this.data = data;

        } catch (RemoteException e) {
            debug("got remote exception.");
            errorMsg = "NO CONNECTION";
        } catch (ForbiddenException e) {
            debug("got forbidden exception.");
            errorMsg = "No such project OR project/process mismatch";
        } catch (Exception e) {
            printError(e);
        }
    }

    protected void initSound() {
        if (Settings.getBool("dataApplet.quiet", false)) return;

        String soundFile =
            (System.getProperty("java.version").startsWith("1.1")
             ? "/dataStored.au" : "/dataStored.wav");
        dataStoredSound = getAudioClip(getCodeBase(), soundFile);
    }

    protected boolean createFieldManager() {
        try {
            if (mgr == null) maybeCreateDOMManager();
            if (mgr == null) createNSManager();
            return (mgr != null);
        } catch (Throwable t) {
            // creating or initializing the HTMLFieldManager in an unsupported
            // browser could cause various exceptions or errors to be thrown.
            System.out.println
                ("Your current browser configuration appears to be incapable\n"
                 + "of supporting the dashboard. The error encountered was:");
            System.out.println(t);
            t.printStackTrace();

            redirectToProblemURL(t);
            return false;
        }
    }

    protected void maybeCreateDOMManager() {
        // if the user disabled use of the DOMFieldManager, return.
        if (getBoolParameter("disableDOM", false)) return;

        // The DOM support only works in 1.4.2 and higher.
        String javaVer = System.getProperty("java.version");
        if (javaVer.compareTo("1.4.2") < 0) return;

        try {
            // Try to create a DOMFieldManager.
            createFieldManager("dom.DOMFieldManager");
        } catch (Throwable e) {}
    }

    protected void createNSManager() throws Exception {
        createFieldManager("ns.NSFieldManager");
    }

    protected void createFieldManager(String className) throws Exception {
        Class clz = Class.forName
            ("net.sourceforge.processdash.data.applet." + className);
        Constructor cstr = clz.getConstructor(new Class[] {DataApplet.class});
        mgr = (HTMLFieldManager) cstr.newInstance(new Object[] { this });
    }


    public void stop() {
        isRunning = false;
        try {
            if (mgr != null) {
                mgr.dispose(false);
                mgr = null;
            }
            if (data != null) {
                debug("data is not null; unregistering listener...");
                data.removeRepositoryClientListener(this);
                debug("quitting data...");
                data.quit();
                data = null;
            }
        } catch (Exception e) { printError(e); }

        dataPrefix = errorMsg = requiredTag = null;
        debug("stop complete.");
    }


    public void repositoryClientStoredData(String name) {
        if (dataStoredSound != null)
            dataStoredSound.play();
    }


    public int getIntParameter(String name) {
        try {
            return Integer.parseInt(getParameter(name));
        } catch (Exception e) {
            return -1;
        }
    }

    public boolean getBoolParameter(String name, boolean defaultValue) {
        String value = getParameter(name);
        if (value == null || value.length() == 0) return defaultValue;
        switch (value.charAt(0)) {
        case 't': case 'T': case 'y': case 'Y': return true;
        case 'f': case 'F': case 'n': case 'N': return false;
        }
        return defaultValue;
    }

    public static int ieVersion() { return ieVersion; }

    public static int nsVersion() { return nsVersion; }

    public boolean unlocked() { return getBoolParameter("unlock", false); }

    public void refreshPage() {
        try {
            String urlStr = getParameter("docURL");
            if (urlStr == null || urlStr.length() == 0)
                urlStr = getDocumentBase().toString();
            if (urlStr.indexOf('?') == -1)
                urlStr = urlStr + "?reload";
            else
                urlStr = urlStr + "&reload";

            URL url = new URL(urlStr);
            stop();
            getAppletContext().showDocument(url, "_self");
        } catch (Exception ioe) {
            printError(ioe);
        }
    }


    public void notifyListener(Object id) {
        debug("NSDataApplet.notifyListener("+id+")");
        if (isRunning && mgr != null)
            mgr.notifyListener(id);
    }

    private static final String PROBLEM_URL = "/help/Topics/Troubleshooting/DataApplet/OtherBrowser.htm";


    protected void redirectToProblemURL(Throwable t) {
        try {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            PrintWriter w = new PrintWriter(buf);
            t.printStackTrace(w);
            w.flush();
            String javaVersion = System.getProperty("java.vendor") +
                " JRE " + System.getProperty("java.version") +
                "; " + System.getProperty("os.name");
            // DO NOT fix the deprecated statements on the next lines!
            // This code needs to run in Java 1.1 JVMs.
            String urlStr = PROBLEM_URL +
                "?JAVA_VERSION=" + URLEncoder.encode(javaVersion) +
                "&ERROR_MESSAGE=" + URLEncoder.encode(buf.toString());
            URL url = new URL(getDocumentBase(), urlStr);
            getAppletContext().showDocument(url, "_top");
        } catch (IOException ioe) {}
    }


    private void avoidMicrosoftJVM() {
        String javaVendor = System.getProperty("java.vendor").toLowerCase();
        if (javaVendor.indexOf("microsoft") != -1)
            try {
                String urlStr = getParameter("docURL");
                if (urlStr == null || urlStr.length() == 0)
                    urlStr = getDocumentBase().toString();
                if (urlStr.indexOf('?') == -1)
                    urlStr = urlStr + "?ForceJavaPlugIn";
                else
                    urlStr = urlStr + "&ForceJavaPlugIn";

                URL url = new URL(urlStr);
                getAppletContext().showDocument(url, "_self");
            } catch (IOException ioe) {
                System.out.println(ioe);
            }
    }
}
