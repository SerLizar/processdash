// Copyright (C) 2005-2007 Tuma Solutions, LLC
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

package net.sourceforge.processdash.log.time;

import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.table.AbstractTableModel;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.log.ChangeFlagged;
import net.sourceforge.processdash.util.FormatUtil;

public class TimeLogTableModel extends AbstractTableModel implements
        TimeLogListener {

    private ModifiableTimeLog timeLog;

    private TimeLoggingApprover approver;

    private ArrayList filteredEntries;

    private String filterPath;

    private Date filterStart;

    private Date filterEnd;

    private boolean isEditable = true;

    public static final int COL_ID = -1;

    public static final int COL_PATH = 0;

    public static final int COL_START_TIME = 1;

    public static final int COL_ELAPSED = 2;

    public static final int COL_INTERRUPT = 3;

    public static final int COL_COMMENT = 4;

    private static Resources resources = Resources.getDashBundle("Time");

    private static final String[] COLUMN_KEYS = { "Logged_To", "Start_Time",
            "Delta_Time", "Interrupt_Time", "Comment" };

    private static final String[] COLUMN_NAMES = resources.getStrings(
            "Columns.", COLUMN_KEYS, ".Name");

    public static final int[] COLUMN_WIDTHS = resources.getInts("Columns.",
            COLUMN_KEYS, ".Width_");

    public static final String[] COLUMN_TOOLTIPS = resources.getStrings(
            "Columns.", COLUMN_KEYS, ".Tooltip");

    public TimeLogTableModel() {
        this.filteredEntries = new ArrayList();

    }

    public void setTimeLog(ModifiableTimeLog newTimeLog) {
        if (newTimeLog != timeLog) {
            if (timeLog != null)
                timeLog.removeTimeLogListener(this);
            timeLog = newTimeLog;
            if (timeLog != null)
                timeLog.addTimeLogListener(this);
            refreshFilteredEntries();
        }
    }

    public void setApprover(TimeLoggingApprover approver) {
        this.approver = approver;
    }

    public void setFilter(String path, Date from, Date to) {
        filterPath = path;
        filterStart = from;
        filterEnd = to;
        refreshFilteredEntries();
    }

    public boolean isEditable() {
        return isEditable;
    }

    public void setEditable(boolean isEditable) {
        this.isEditable = isEditable;
    }

    protected void refreshFilteredEntries() {
        ArrayList newList = new ArrayList();
        if (timeLog != null)
            try {
                Iterator iter = timeLog.filter(filterPath, filterStart,
                        filterEnd);
                while (iter.hasNext())
                    newList.add(iter.next());
            } catch (Exception e) {
                // FIXME: what do we do here? This could be an IOException or
                // an IONoSuchElementException
            }
        Collections.sort(newList);
        filteredEntries = newList;
        fireTableDataChanged();
    }

    public int getColumnCount() {
        return COLUMN_KEYS.length;
    }

    public String getColumnName(int column) {
        return COLUMN_NAMES[column];
    }

    public int getRowCount() {
        return filteredEntries.size();
    }

    private boolean noSuchRow(int row) {
        return (row < 0 || row >= filteredEntries.size() || timeLog == null);
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        if (noSuchRow(rowIndex))
            return null;

        TimeLogEntry tle = (TimeLogEntry) filteredEntries.get(rowIndex);
        switch (columnIndex) {
        case COL_PATH:
            return tle.getPath();
        case COL_START_TIME:
            return FormatUtil.formatDateTime(tle.getStartTime());
        case COL_ELAPSED:
            return FormatUtil.formatTime(tle.getElapsedTime());
        case COL_INTERRUPT:
            return FormatUtil.formatTime(tle.getInterruptTime());
        case COL_COMMENT:
            return tle.getComment();
        case COL_ID:
            return new Long(tle.getID());
        }

        return null;
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return isEditable;
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (noSuchRow(rowIndex))
            return;

        String newValue = (aValue == null ? null : String.valueOf(aValue));
        String oldValue = (String) getValueAt(rowIndex, columnIndex);
        if (oldValue == newValue
                || (oldValue != null && oldValue.equals(newValue)))
            return; // no changes to make

        TimeLogEntry tle = (TimeLogEntry) filteredEntries.get(rowIndex);
        ChangeFlaggedTimeLogEntry diff = null;
        switch (columnIndex) {

        case COL_PATH:
            if (approver == null || approver.isTimeLoggingAllowed(newValue))
                diff = new TimeLogEntryVO(tle.getID(), newValue, null, 0, 0,
                        null, ChangeFlagged.MODIFIED);
            break;

        case COL_START_TIME:
            Date startTime = FormatUtil.parseDateTime(newValue);
            if (startTime != null)
                diff = new TimeLogEntryVO(tle.getID(), null, startTime, 0, 0,
                        null, ChangeFlagged.MODIFIED);
            break;

        case COL_ELAPSED:
            long newElapsed = FormatUtil.parseTime(newValue);
            if (newElapsed != -1) {
                long elapsedDiff = newElapsed - tle.getElapsedTime();
                if (elapsedDiff != 0)
                    diff = new TimeLogEntryVO(tle.getID(), null, null,
                            elapsedDiff, 0, null, ChangeFlagged.MODIFIED);
            }
            break;

        case COL_INTERRUPT:
            long newInterrupt = FormatUtil.parseTime(newValue);
            if (newInterrupt != -1) {
                long interruptDiff = newInterrupt - tle.getInterruptTime();
                if (interruptDiff != 0) {
                    long elapsedDiff = Math.min(interruptDiff,
                            tle.getElapsedTime());
                    diff = new TimeLogEntryVO(tle.getID(), null, null,
                            -elapsedDiff, interruptDiff, null,
                            ChangeFlagged.MODIFIED);
                }
            }
            break;

        case COL_COMMENT:
            if (newValue == null)
                newValue = "";   // user deleted the existing comment
            diff = new TimeLogEntryVO(tle.getID(), null, null, 0, 0, newValue,
                    ChangeFlagged.MODIFIED);
            break;
        }

        if (diff != null)
            timeLog.addModification(diff);
    }

    public void deleteRow(int row) {
        if (noSuchRow(row))
            return;

        TimeLogEntry tle = (TimeLogEntry) filteredEntries.get(row);
        ChangeFlaggedTimeLogEntry deletion = new TimeLogEntryVO(tle.getID(), null, null, 0,
                0, null, ChangeFlagged.DELETED);
        timeLog.addModification(deletion);
    }

    public void duplicateRow(int row) {
        if (noSuchRow(row))
            return;

        TimeLogEntry tle = (TimeLogEntry) filteredEntries.get(row);
        long nextID = timeLog.getNextID();
        ChangeFlaggedTimeLogEntry addition = new TimeLogEntryVO(nextID, tle.getPath(), tle
                .getStartTime(), tle.getElapsedTime(), tle.getInterruptTime(),
                null, ChangeFlagged.ADDED);
        timeLog.addModification(addition);
    }

    public void addRow(String path) {
        if (timeLog == null)
            return;

        long nextID = timeLog.getNextID();
        ChangeFlaggedTimeLogEntry addition = new TimeLogEntryVO(nextID, path, new Date(), 0,
                0, null, ChangeFlagged.ADDED);
        timeLog.addModification(addition);
    }

    public void summarizeRows() {
        if (filteredEntries.size() < 2 || timeLog == null)
            return;

        List modifications = new LinkedList();
        Map baseEntries = new HashMap();
        Map mergedComments = new HashMap();
        for (Iterator iter = filteredEntries.iterator(); iter.hasNext();) {
            TimeLogEntry newEntry = (TimeLogEntry) iter.next();
            String path = newEntry.getPath();
            TimeLogEntry origEntry = (TimeLogEntry) baseEntries.get(path);
            if (origEntry == null) {
                baseEntries.put(path, newEntry);
                mergedComments.put(path, newEntry.getComment());
            } else {
                String origComment = (String) mergedComments.get(path);
                String mergedCommentDiff = createMergedCommentDiff(
                        origComment, newEntry.getComment());
                if (mergedCommentDiff != null)
                    mergedComments.put(path, mergedCommentDiff);
                TimeLogEntry mod = new TimeLogEntryVO(origEntry.getID(),
                        null, null, newEntry.getElapsedTime(),
                        newEntry.getInterruptTime(), mergedCommentDiff,
                        ChangeFlagged.MODIFIED);
                modifications.add(mod);
                TimeLogEntry del = new TimeLogEntryVO(newEntry.getID(),
                        null, null, 0, 0, null, ChangeFlagged.DELETED);
                modifications.add(del);
            }
        }

        if (!modifications.isEmpty())
            timeLog.addModifications(modifications.iterator());
    }

    private String createMergedCommentDiff(String origComment, String newComment) {
        if (newComment == null || newComment.length() == 0)
            return null;  // no changes needed
        else if (origComment == null || origComment.length() == 0)
            return newComment;  // use new comment
        else
            return origComment + "\n" + newComment;  // merge comments
    }

    public Transferable getTransferrable(int[] rows) {
        StringBuffer result = new StringBuffer();
        result.append(COLUMN_NAMES[COL_PATH]).append('\t');
        result.append(COLUMN_NAMES[COL_START_TIME]).append('\t');
        result.append(COLUMN_NAMES[COL_ELAPSED]).append('\t');
        result.append(COLUMN_NAMES[COL_INTERRUPT]).append('\t');
        result.append(COLUMN_NAMES[COL_COMMENT]).append('\n');

        for (int i = 0; i < rows.length; i++) {
            TimeLogEntry tle = (TimeLogEntry) filteredEntries.get(rows[i]);
            result.append(tle.getPath()).append("\t");
            result.append(FormatUtil.formatDateTime(tle.getStartTime())).append("\t");
            result.append(tle.getElapsedTime()).append("\t");
            result.append(tle.getInterruptTime()).append("\t");
            String comment = tle.getComment();
            if (comment != null)
                result.append(comment.replace('\t', ' ').replace('\r', ' ')
                        .replace('\n', ' '));
            result.append("\n");
        }
        return new StringSelection(result.toString());
    }

    public void timeLogChanged(TimeLogEvent e) {
        ChangeFlaggedTimeLogEntry tle = e.getTimeLogEntry();

        if (tle == null)
            refreshFilteredEntries();

        else {
            ChangeFlagged cf = (ChangeFlagged) tle;
            if (cf.getChangeFlag() == ChangeFlagged.ADDED)
                maybeAddTimeLogEntry(tle);
            else if (cf.getChangeFlag() == ChangeFlagged.MODIFIED)
                maybeModifyTimeLogEntry(tle);
            else if (cf.getChangeFlag() == ChangeFlagged.DELETED)
                maybeDeleteTimeLogEntry(tle);
        }
    }

    private void maybeAddTimeLogEntry(TimeLogEntry added) {
        int pos = findTimeLogEntryInFilteredList(added.getID());
        if (pos != -1) {
            filteredEntries.set(pos, added);
            fireTableRowsUpdated(pos, pos);
        } else if (TimeLogIteratorFilter.matches(added, filterPath,
                filterStart, filterEnd, true)) {
            pos = filteredEntries.size();
            filteredEntries.add(added);
            fireTableRowsInserted(pos, pos);
        }
    }

    private void maybeModifyTimeLogEntry(TimeLogEntry diff) {
        int pos = findTimeLogEntryInFilteredList(diff.getID());
        if (pos != -1) {
            TimeLogEntry orig = (TimeLogEntry) filteredEntries.get(pos);
            TimeLogEntry newTle = TimeLogEntryVO.applyChanges(orig, diff,
                    false);
            filteredEntries.set(pos, newTle);
            fireTableRowsUpdated(pos, pos);
        }
    }

    private void maybeDeleteTimeLogEntry(TimeLogEntry deleted) {
        int pos = findTimeLogEntryInFilteredList(deleted.getID());
        if (pos != -1) {
            filteredEntries.remove(pos);
            fireTableRowsDeleted(pos, pos);
        }
    }

    private int findTimeLogEntryInFilteredList(long id) {
        for (int i = 0; i < filteredEntries.size(); i++) {
            TimeLogEntry tle = (TimeLogEntry) filteredEntries.get(i);
            if (tle.getID() == id)
                return i;
        }
        return -1;
    }
}
