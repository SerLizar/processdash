// Copyright (C) 2013 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ev;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Element;

import net.sourceforge.processdash.tool.export.mgr.ExportManager;
import net.sourceforge.processdash.util.XMLUtils;

public class ImportedEVManager {

    public interface CachedDataCalculator {

        public Object calculateCachedData(String taskListName, Element xml);

    }


    private static final ImportedEVManager INSTANCE = new ImportedEVManager();

    public static ImportedEVManager getInstance() {
        return INSTANCE;
    }


    private Map<String, ImportedTaskList> importedTaskLists;

    private Map<Object, CachedDataCalculator> calculators;

    private ImportedEVManager() {
        importedTaskLists = Collections.synchronizedMap(new HashMap());
        calculators = Collections.synchronizedMap(new LinkedHashMap());
    }



    /**
     * Add information for an imported task list.
     * 
     * @param uniqueKey
     *            the String key for this imported task list. This value should
     *            begin with a prefix that is specific to the import file. Then
     *            it should contain a name formatted by the
     *            {@link ExportManager#exportedScheduleDataPrefix(String, String)}
     *            method.
     * @param xml
     *            the parsed XML Element corresponding to this EV schedule.
     */
    public void importTaskList(String uniqueKey, Element xml) {
        if (EVTaskList.EV_TASK_LIST_ELEMENT_NAME.equals(xml.getTagName())) {
            ImportedTaskList taskList = new ImportedTaskList(uniqueKey, xml);
            importedTaskLists.put(uniqueKey, taskList);
        } else {
            System.err.println("Attempt to import invalid EV XML "
                    + "document; ignoring");
        }
    }


    /**
     * Remove all of the imported task lists whose unique key began with a
     * particular prefix.
     * 
     * @param prefix
     *            a uniqueKey prefix, which identifies the file that the task
     *            list was imported from.
     */
    public void closeTaskLists(String prefix) {
        synchronized (importedTaskLists) {
            for (Iterator<String> i = importedTaskLists.keySet().iterator(); i
                    .hasNext();) {
                String uniqueKey = i.next();
                if (uniqueKey.startsWith(prefix))
                    i.remove();
            }
        }
    }


    /**
     * Get the taskListNames of all imported schedules.
     * 
     * @return a Set containing the task list names of all imported schedules.
     *         Task list names are generally of the form
     *         <tt>[task list ID]#XMLID[unique key]</tt>
     */
    public Set<String> getImportedTaskListNames() {
        Set<String> result = new HashSet<String>();
        synchronized (importedTaskLists) {
            for (ImportedTaskList tl : importedTaskLists.values()) {
                result.add(tl.taskListName);
            }
        }
        return result;
    }


    /**
     * Retrieve the XML fragment associated with a particular imported task
     * list.
     * 
     * @param taskListName
     *            the task list name, which can either be a uniqueKey, or (more
     *            commonly) a string of the form
     *            <tt>[task list ID]#XMLID[unique key]</tt>
     * @return the XML fragment associated with that imported task list, or null
     *         if no task list could be found with that name.
     */
    public Element getImportedTaskListXml(String taskListName) {
        ImportedTaskList tl = getImportedTaskListByName(taskListName);
        return (tl == null ? null : tl.xml);
    }


    /**
     * Find an imported task list with the given ID, and return its full task
     * list name.
     * 
     * @param taskListID
     *            a task list ID
     * @return the full name for the imported task list with that ID, or null if
     *         no such task list could be found.
     */
    public String getTaskListNameForID(String taskListID) {
        ImportedTaskList tl = getImportedTaskListByID(taskListID);
        return (tl == null ? null : tl.taskListName);
    }



    /**
     * Register an object to perform cached data calculations.
     */
    public void addCalculator(Object calculatorKey, CachedDataCalculator calc) {
        calculators.put(calculatorKey, calc);
    }


    /**
     * Find an imported task list with the given name, and retrieve the cached
     * data object that was calculated by the calculator with the given key
     * 
     * @param taskListName
     *            the name of a task list
     * @param calculatorKey
     *            the key that was previously used to register a
     *            {@link CachedDataCalculator}
     * @return the data object calculated by that calculator for this task list.
     */
    public <T> T getCachedData(String taskListName, Object calculatorKey) {
        ImportedTaskList tl = getImportedTaskListByName(taskListName);
        return (T) (tl == null ? null : tl.getCachedData(calculatorKey));
    }


    /**
     * Scan all of the imported task lists, and build a map of the cached data
     * object that was calculated for each one by the calculator with the given
     * key
     * 
     * @param calculatorKey
     *            the key that was previously used to register a
     *            {@link CachedDataCalculator}
     * @return a map whose keys are task list names, and whose value is the data
     *         object calculated by that calculator for that task list.
     */
    public <T> Map<String, T> getCachedData(Object calculatorKey) {
        ArrayList<ImportedTaskList> taskLists;
        synchronized (importedTaskLists) {
            taskLists = new ArrayList(importedTaskLists.values());
        }

        Map result = new HashMap();
        for (ImportedTaskList tl : taskLists) {
            String name = tl.taskListName;
            Object data = tl.getCachedData(calculatorKey);
            result.put(name, data);
        }
        return result;
    }


    private ImportedTaskList getImportedTaskListByName(String taskListName) {
        if (taskListName == null)
            return null;

        // parse out the task list ID value, if it is present
        String uniqueKey, taskListID;
        int pos = taskListName.indexOf(EVTaskListXML.XMLID_FLAG);
        if (pos == -1) {
            uniqueKey = taskListName;
            taskListID = null;
        } else {
            taskListID = taskListName.substring(0, pos);
            uniqueKey = taskListName.substring(pos
                    + EVTaskListXML.XMLID_FLAG.length());
        }

        // try locating the imported task list in a variety of ways
        ImportedTaskList result = importedTaskLists.get(uniqueKey);
        if (result == null)
            result = getImportedTaskListByID(taskListID);
        if (result == null)
            result = getImportedTaskListByUniqueDisplayName(taskListName);

        return result;
    }


    private ImportedTaskList getImportedTaskListByID(String taskListID) {
        if (taskListID != null) {
            synchronized (importedTaskLists) {
                for (ImportedTaskList taskList : importedTaskLists.values()) {
                    if (taskListID.equals(taskList.taskListID))
                        return taskList;
                }
            }
        }
        return null;
    }


    /**
     * "Missing task list" errors are a common problem in team dashboards. See
     * if we can mitigate these errors by following a simple heuristic: if the
     * rollup is calling for a imported plain task list with a name like
     * "Some Task List (Owner name)", and we have exactly one imported schedule
     * with that name, return it.
     */
    private ImportedTaskList getImportedTaskListByUniqueDisplayName(
            String taskListName) {
        // Retrieve the display name, and make certain it includes the embedded
        // name of a schedule owner.
        String displayName = EVTaskList.cleanupName(taskListName);
        if (displayName.lastIndexOf(')') == -1)
            return null;

        ImportedTaskList result = null;
        synchronized (importedTaskLists) {
            for (ImportedTaskList taskList : importedTaskLists.values()) {
                if (displayName.equals(taskList.displayName)) {
                    if (result == null)
                        // if this is the first schedule we've found with this
                        // display name, save it.
                        result = taskList;
                    else
                        // if we've now seen two schedules with this name,
                        // abort and return null.
                        return null;
                }
            }
        }

        return result;
    }


    private class ImportedTaskList {

        private Element xml;

        private String taskListID;

        private String taskListName;

        private String displayName;

        private Map cachedData;

        protected ImportedTaskList(String uniqueKey, Element xml) {
            this.xml = xml;
            this.taskListID = xml.getAttribute(EVTaskListXML.XMLID_ATTR);
            if (XMLUtils.hasValue(taskListID)) {
                this.taskListName = taskListID + EVTaskListXML.XMLID_FLAG
                        + uniqueKey;
            } else {
                this.taskListName = uniqueKey;
                this.taskListID = null;
            }
            this.displayName = EVTaskList.cleanupName(taskListName);
            this.cachedData = new HashMap();
        }

        private synchronized Object getCachedData(Object calculatorKey) {
            Object result = cachedData.get(calculatorKey);
            if (result == null) {
                CachedDataCalculator calc = calculators.get(calculatorKey);
                if (calc != null)
                    result = calc.calculateCachedData(taskListName, xml);
                cachedData.put(calculatorKey, result);
            }
            return result;
        }

    }

}
