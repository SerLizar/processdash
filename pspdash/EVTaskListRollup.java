// PSP Dashboard - Data Automation Tool for PSP-like processes
// Copyright (C) 1999  United States Air Force
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
// OO-ALC/TISHD
// Attn: PSP Dashboard Group
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  ken.raisor@hill.af.mil


package pspdash;

import java.awt.event.*;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.*;
import javax.swing.*;
import javax.swing.Timer;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.*;
import javax.swing.tree.TreePath;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import pspdash.data.DataRepository;
import pspdash.data.DataComparator;
import pspdash.data.DoubleData;
import pspdash.data.StringData;
import pspdash.data.SimpleData;
import pspdash.data.ListData;

public class EVTaskListRollup extends EVTaskList {

    public static final String TASK_LISTS_DATA_NAME = "Task Lists";

    protected DataRepository data;
    protected Vector evTaskLists;

    public EVTaskListRollup(String taskListName,
                            DataRepository data,
                            PSPProperties hierarchy,
                            ObjectCache cache) {
        super(taskListName, taskListName, false);
        this.data = data;

        evTaskLists = new Vector();
        addTaskListsFromData(data, hierarchy, cache, taskListName);
        schedule = new EVScheduleRollup(evTaskLists);
    }


    private void addTaskListsFromData(DataRepository data,
                                      PSPProperties hierarchy,
                                      ObjectCache cache,
                                      String taskListName) {
        String globalPrefix = MAIN_DATA_PREFIX + taskListName;
        String dataName =
            data.createDataName(globalPrefix, TASK_LISTS_DATA_NAME);
        SimpleData listVal = data.getSimpleValue(dataName);
        ListData list = null;
        if (listVal instanceof ListData)
            list = (ListData) listVal;
        else if (listVal instanceof StringData)
            list = ((StringData) listVal).asList();

        if (list == null) return;
        for (int i = 0;   i < list.size();   i++) {
            taskListName = (String) list.get(i);
            EVTaskList taskList = EVTaskList.openExisting
                (taskListName, data, hierarchy, cache, false);
            if (taskList == null) {
                if (EVTaskListXML.validName(taskListName))
                    taskList = new EVTaskListXML(taskListName, data);
                else
                    continue;
            }

            ((EVTask) root).add((EVTask) taskList.root);
            evTaskLists.add(taskList);
        }
    }
    public boolean isEditable() { return true; }

    public void save(String newName) {
        String dataName;

        // First, erase the data element that used to hold the list of
        // task lists.
        if (!taskListName.equals(newName)) {
            dataName = data.createDataName(MAIN_DATA_PREFIX + taskListName,
                                           TASK_LISTS_DATA_NAME);
            data.putValue(dataName, null);
        }

        // Now, save the rollup to the repository with the new name.
        if (newName != null) {
            dataName = data.createDataName(MAIN_DATA_PREFIX + newName,
                                           TASK_LISTS_DATA_NAME);
            ListData list = new ListData();
            Iterator i = evTaskLists.iterator();
            while (i.hasNext())
                list.add(((EVTaskList) i.next()).taskListName);

            data.putValue(dataName, list);
            taskListName = newName;
        }
    }

    public void recalc() {
        EVTaskList taskList;

        // Recalculate all the subschedules.
        for (int i = evTaskLists.size();   i-- > 0; ) {
            taskList = (EVTaskList) evTaskLists.get(i);
            taskList.recalc();

            // Some types of task lists perform a recalc by completely
            // replacing their root task and schedule. Give them the
            // benefit of the doubt and make certain that we are using
            // the correct root and schedule
            ((EVTask) root).replace(i, (EVTask) taskList.root);
            ((EVScheduleRollup) schedule).replaceSchedule(i, taskList);
        }

        // Recalculate the root node.
        ((EVTask) root).recalcRollupNode();

        // Recalculate the rollup schedule.
        ((EVScheduleRollup) schedule).recalc();

        super.recalc();
    }

    public EVTask createAndAddTask(String path,
                                   DataRepository data,
                                   PSPProperties hierarchy,
                                   ObjectCache cache,
                                   boolean willNeedChangeNotification) {

        EVTaskList taskList = EVTaskList.openExisting
            (path, data, hierarchy, cache, false);
        if (taskList == null) return null;

        EVTask newTask = (EVTask) taskList.root;
        if (((EVTask) root).add(newTask)) {
            evTaskLists.add(taskList);
            ((EVScheduleRollup) schedule).addSchedule(taskList);
            return newTask;
        } else
            return null;
    }

    protected void finishRemovingTask(int pos) {
        EVTaskList taskList = (EVTaskList) evTaskLists.remove(pos);
        ((EVScheduleRollup) schedule).removeSchedule(taskList.schedule);
    }

    protected void finishMovingTaskUp(int pos) {
        Object taskList = evTaskLists.remove(pos);
        evTaskLists.insertElementAt(taskList, pos-1);
    }



    public static boolean validName(String taskListName) {
        return (taskListName != null &&
                taskListName.length() > 0 &&
                taskListName.indexOf('/') == -1);
    }
    public static boolean exists(DataRepository data, String taskListName) {
        String dataName = data.createDataName(MAIN_DATA_PREFIX + taskListName,
                                              TASK_LISTS_DATA_NAME);
        return data.getSimpleValue(dataName) != null;
    }
    public static String taskListNameFromDataElement(String dataName) {
        if (dataName == null ||
            !dataName.startsWith(MAIN_DATA_PREFIX) ||
            !dataName.endsWith("/" + TASK_LISTS_DATA_NAME))
            return null;

        return dataName.substring
            (MAIN_DATA_PREFIX.length(),
             dataName.length() - TASK_LISTS_DATA_NAME.length() - 1);
    }

}
