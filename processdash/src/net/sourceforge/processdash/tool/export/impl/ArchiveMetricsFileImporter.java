// Copyright (C) 2005-2013 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.export.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import net.sourceforge.processdash.data.DateData;
import net.sourceforge.processdash.data.ImmutableDoubleData;
import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.data.TagData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.data.repository.InvalidDatafileFormat;
import net.sourceforge.processdash.ev.ImportedEVManager;
import net.sourceforge.processdash.log.defects.ImportedDefectManager;
import net.sourceforge.processdash.log.time.ImportedTimeLogManager;
import net.sourceforge.processdash.util.XMLUtils;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

public class ArchiveMetricsFileImporter implements Runnable,
        ArchiveMetricsXmlConstants {

    public interface Handler {
        boolean canHandle(String type, String version);

        void handle(ArchiveMetricsFileImporter caller, InputStream in,
                String type, String version) throws Exception;
    };

    private File file;

    private DataRepository data;

    private String prefix;

    private List fileHandlers;

    public ArchiveMetricsFileImporter(DataRepository data, File file,
            String prefix) {
        this.data = data;
        this.file = file;
        this.prefix = prefix;
        this.fileHandlers = initHandlers();
    }

    private List initHandlers() {
        List result = new LinkedList();
        result.add(new EVImporterXMLv1());
        result.add(new DefectImporterXMLv1());
        result.add(new TimeLogImporterXMLv1());
        result.add(new DataImporterXMLv1());
        result.add(new MessageImporterXMLv1());
        return result;
    }

    public void run() {
        try {
            doImport();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void doImport() throws IOException {
        try {
            readAndProcessArchive();
        } catch (Exception e) {
            IOException ioe = new IOException();
            ioe.initCause(e);
            throw ioe;
        } finally {
            owner = null;
            exportTimestamp = null;
            defns = null;
            if (zipFile != null)
                try {
                    zipFile.close();
                } catch (Exception e) {}
            zipFile = null;
            if (shouldDeleteArchiveFileOnCompletion)
                file.delete();
        }
    }

    private ZipFile zipFile;

    private String owner;

    private Date exportTimestamp;

    private Map defns;

    private boolean shouldDeleteArchiveFileOnCompletion;

    public Map getDefns() {
        return defns;
    }

    public Date getExportTimestamp() {
        return exportTimestamp;
    }

    public String getOwner() {
        return owner;
    }

    public String getPrefix() {
        return prefix;
    }

    private void readAndProcessArchive() throws IOException,
            XmlPullParserException {
        shouldDeleteArchiveFileOnCompletion = false;
        zipFile = new ZipFile(file);
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        XmlPullParser parser = factory.newPullParser();

        InputStream manifestIn = openEntry(zipFile, MANIFEST_FILE_NAME);
        parser.setInput(manifestIn, ENCODING);

        parser.nextTag();
        parser.require(XmlPullParser.START_TAG, null, ARCHIVE_ELEM);
        if (!FILE_TYPE_ARCHIVE
                .equals(parser.getAttributeValue(null, TYPE_ATTR)))
            error("expected archive of type " + FILE_TYPE_ARCHIVE);

        parser.nextTag();
        parser.require(XmlPullParser.START_TAG, null, EXPORTED_TAG);
        owner = parser.getAttributeValue(null, OWNER_ATTR);
        exportTimestamp = XMLUtils.parseDate(parser.getAttributeValue(null,
                WHEN_ATTR));

        ImportedDefectManager.closeDefects(prefix);
        ImportedTimeLogManager.getInstance().closeTimeLogs(prefix);
        ImportedEVManager.getInstance().closeTaskLists(prefix);
        defns = new HashMap();
        Map<String, String> packageIDs = new HashMap<String, String>();

        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.getEventType() == XmlPullParser.START_TAG) {
                if (PACKAGE_ELEM.equals(parser.getName())) {
                    String id = parser.getAttributeValue(null, PACKAGE_ID_ATTR);
                    String version = parser.getAttributeValue(null, VERSION_ATTR);
                    packageIDs.put(id, version);
                } else if (FILE_ELEM.equals(parser.getName())) {
                    String name = parser.getAttributeValue(null, FILE_NAME_ATTR);
                    String type = parser.getAttributeValue(null, TYPE_ATTR);
                    String version = parser.getAttributeValue(null, VERSION_ATTR);
                    readFile(zipFile, name, type, version);
                }
            }
        }

        // Protect this data from being viewed via external http requests.
        defns.put("_Password_", ImmutableDoubleData.READ_ONLY_ZERO);

        // Save metadata about the import
        addImportMetadata(packageIDs);

        try {
            data.mountImportedData(prefix, defns);
        } catch (InvalidDatafileFormat idf) {
        }
    }

    private void readFile(ZipFile zipFile, String name, String type,
            String version) throws IOException {
        Handler h = getHandler(type, version);
        if (h != null) {
            InputStream in = openEntry(zipFile, name);
            try {
                h.handle(this, in, type, version);
            } catch (IOException ioe) {
                throw ioe;
            } catch (Exception e) {
                IOException ioe = new IOException();
                ioe.initCause(e);
                throw ioe;
            } finally {
                in.close();
            }
        }
    }

    private Handler getHandler(String type, String version) {
        for (Iterator iter = fileHandlers.iterator(); iter.hasNext();) {
            Handler h = (Handler) iter.next();
            if (h.canHandle(type, version))
                return h;
        }
        return null;
    }

//    private String getKey(String type, String version) {
//        return type + "_v" + version;
//    }

    private InputStream openEntry(ZipFile zipFile, String name)
            throws IOException {
        InputStream result = null;
        ZipEntry entry = zipFile.getEntry(name);
        if (entry != null)
            result = zipFile.getInputStream(entry);
        if (result == null)
            error("could not find archive entry " + name);
        return result;
    }

    private void error(String message) throws IOException {
        String filename = file.getAbsolutePath();
        throw new IOException("Unable to import '" + filename
                + "'; not a valid archive file: " + message);
    }

    private void addImportMetadata(Map<String, String> packageIDs) {
        String prefix = METADATA_DATA_NAME + "/" + EXPORTED_TAG + ".";

        defns.put(METADATA_DATA_NAME, TagData.getInstance());

        if (owner != null)
            defns.put(prefix + OWNER_ATTR, StringData.create(owner));

        if (exportTimestamp != null)
            defns.put(prefix + WHEN_ATTR, new DateData(exportTimestamp, false));

        ListData packageList = new ListData();
        String packagePrefix = prefix + PACKAGE_ELEM + "/";
        for (Map.Entry<String, String> e : packageIDs.entrySet()) {
            String id = e.getKey();
            String version = e.getValue();
            packageList.add(id);
            packageList.add(version);
            defns.put(packagePrefix + id, StringData.create(version));
        }
        defns.put(prefix + PACKAGE_ELEM, packageList);
    }

    public void deleteArchiveFileOnCompletion() {
        shouldDeleteArchiveFileOnCompletion = true;
    }

}
