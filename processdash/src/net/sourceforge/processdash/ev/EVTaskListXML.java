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


package net.sourceforge.processdash.ev;

import java.awt.event.*;
import java.net.*;
import java.util.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.tree.TreePath;

import pspdash.data.DataRepository;
import pspdash.data.StringData;
import pspdash.data.SimpleData;

public class EVTaskListXML extends EVTaskListXMLAbstract {

    public static final String XML_DATA_NAME = "XML Task List";
    public static final String XMLID_FLAG = "#XMLID";
    private static final String XMLID_ATTR = "tlid";
    protected DataRepository data;

    public EVTaskListXML(String taskListName, DataRepository data) {
        super(taskListName, null, false);
        this.data = data;

        if (!openXML(data, taskListName))
            createErrorRootNode(cleanupName(taskListName),
                                resources.getString("Task_List_Missing"));
    }

    private boolean openXML(DataRepository data, String taskListName) {
        String xmlDoc = getXMLString(data, taskListName);
        if (xmlDoc == null) return false;

        return openXML(xmlDoc, cleanupName(taskListName));
    }

    public void recalc() {
        if (!openXML(data, taskListName))
            createErrorRootNode(cleanupName(taskListName),
                                resources.getString("Task_List_Missing"));
        super.recalc();
    }

    public static boolean validName(String taskListName) {
        return (taskListName != null &&
                taskListName.indexOf(MAIN_DATA_PREFIX) != -1);
    }

    public static boolean exists(DataRepository data, String taskListName) {
        String xmlDoc = getXMLString(data, taskListName);
        return xmlDoc != null;
    }

    private static final Map ID_MAP = new Hashtable();

    public static String taskListNameFromDataElement(DataRepository data,
                                                     String dataName) {
        if (dataName == null ||
            dataName.indexOf(MAIN_DATA_PREFIX) == -1 ||
            !dataName.endsWith("/" + XML_DATA_NAME))
            return null;

        String result = dataName.substring
            (0, dataName.length() - XML_DATA_NAME.length() - 1);

        // retrieve the ID for the task list we found.
        String taskListID = getIDForDataName(data, dataName);
        if (taskListID != null)
            // if this task list has an ID, prepend it to the task list name.
            result = taskListID + XMLID_FLAG + result;

        return result;
    }

    public static String getDataNameForID(DataRepository data,
                                          String taskListID)
    {
        if (taskListID == null) return null;

        // check the cache to see if we have a mapping for this id
        String result = (String) ID_MAP.get(taskListID);
        if (result != null) {
            // if the mapping is still valid, return it.
            String actualID = getIDForDataName(data, result);
            if (taskListID.equals(actualID))
                return result;
            else
                ID_MAP.remove(taskListID);
        }

        // scan all the data elements in the repository.
        Iterator i = data.getKeys();
        String dataName;
        result = null;
        while (i.hasNext()) {
            dataName = (String) i.next();

            // only examine elements that look like an XML EV task list.
            if (!dataName.endsWith(XML_DATA_NAME)) continue;

            // if this is an XML EV task list, get its ID.
            String id = getIDForDataName(data, dataName);
            if (id != null && id.equals(taskListID))
                // if we've found a match, remember the corresponding
                // data name.  (We'll go ahead and finish the scan,
                // since this will make future calls to this method
                // much more efficient)
                result = dataName;
        }

        return result;
    }

    private static String getXMLString(DataRepository data,
                                       String taskListName)
    {
        // If the taskListName appears to contain an XMLID, extract that ID.
        String taskListID = null;
        int pos = taskListName.indexOf(XMLID_FLAG);
        if (pos != -1) {
            taskListID = taskListName.substring(0, pos);
            taskListName = taskListName.substring(pos+XMLID_FLAG.length());
        }

        String dataName = data.createDataName(taskListName, XML_DATA_NAME);
        SimpleData value = data.getSimpleValue(dataName);
        if (!(value instanceof StringData) && taskListID != null) {
            dataName = getDataNameForID(data, taskListID);
            value = data.getSimpleValue(dataName);
        }

        if (value instanceof StringData)
            return value.format();
        else
            return null;
    }

    private static String getIDForDataName(DataRepository data,
                                              String dataName) {
        SimpleData value = data.getSimpleValue(dataName);
        if (!(value instanceof StringData)) return null;

        String xmlDoc = value.format();

        String pattern = " "+XMLID_ATTR+"='";
        int beg = xmlDoc.indexOf(pattern);
        if (beg == -1) return null;
        beg += pattern.length();

        int end = xmlDoc.indexOf('\'', beg);
        if (end == -1 || end == beg) return null;

        String taskListID = xmlDoc.substring(beg, end);

        // keep a cache mapping IDs to data names.
        ID_MAP.put(taskListID, dataName);

        return taskListID;
    }

}
