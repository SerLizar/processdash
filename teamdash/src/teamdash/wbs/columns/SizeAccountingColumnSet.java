// Copyright (C) 2002-2012 Tuma Solutions, LLC
// Team Functionality Add-ons for the Process Dashboard
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

package teamdash.wbs.columns;

import net.sourceforge.processdash.util.PatternList;

import teamdash.merge.ui.MergeConflictNotification;
import teamdash.wbs.CalculatedDataColumn;
import teamdash.wbs.ConflictCapableDataColumn;
import teamdash.wbs.DataTableModel;
import teamdash.wbs.NumericDataValue;
import teamdash.wbs.WBSModel;
import teamdash.wbs.WBSNode;

public class SizeAccountingColumnSet {

    public static void create(DataTableModel model, String id, Pruner p,
                              String editableType)
    {
        createColumn(model, getBaseID(id), p, editableType);     // create "Base" column
        createColumn(model, getDeletedID(id), p, editableType);  // create "Deleted" column
        createColumn(model, getModifiedID(id), p, editableType); // create "Modified" column
        createColumn(model, getReusedID(id), p, editableType);   // create "Reused" column

        // create "Added" column
        CalculatedDataColumn c = new AddedSizeColumn(model, getAddedID(id), p);
        if (editableType != null) c = new NodeTypeColumnFilter(c, editableType);
        model.addDataColumn(c);

        // create "New & Changed" column
        c = new NewChangedSizeColumn(model, id);
        if (editableType != null) c = new NodeTypeColumnFilter(c, editableType);
        model.addDataColumn(c);

        // create "Total" column
        c = new TotalSizeColumn(model, id);
        if (editableType != null) c = new NodeTypeColumnFilter(c, editableType);
        model.addDataColumn(c);
    }

    private static void createColumn(DataTableModel m, String name, Pruner p,
                                     String editableType) {
        TopDownBottomUpColumn column =
            new TopDownBottomUpColumn(m, name, name, p);
        column.setHideInheritedValues(true);
        if (editableType == null)
            m.addDataColumn(column);
        else
            m.addDataColumn(new NodeTypeColumnFilter(column, editableType));
        column.setConflictAttributeName(column.topDownAttrName);
    }

    public static String getBaseID(String id)     { return "Base-"     + id; }
    public static String getDeletedID(String id)  { return "Deleted-"  + id; }
    public static String getModifiedID(String id) { return "Modified-" + id; }
    public static String getAddedID(String id)    { return "Added-"    + id; }
    public static String getReusedID(String id)   { return "Reused-"   + id; }
    public static String getNCID(String id)       { return "N&C-"      + id; }
    public static String getTotalID(String id)    { return "Total-"    + id; }



    private static class AddedSizeColumn extends TopDownBottomUpColumn {

        public AddedSizeColumn(DataTableModel m, String name, Pruner p) {
            super(m, name, name, p);
            setHideInheritedValues(true);
            setConflictAttributeName(topDownAttrName);
        }

        @Override
        protected boolean attemptToRepairTopDownBottomUpMismatch(WBSNode node,
                double topDownValue, double bottomUpValue, WBSNode[] children,
                int numToInclude) {
            // the goal of this method is to improve the user experience when
            // a workflow has been inserted.  If the workflow contains exactly
            // one child of a particular size type, we will copy the parent
            // size into that single child.  To detect this scenario, we use
            // the following criteria:
            //   * The node in question has a "workflow source" atribute
            //   * the bottom-up value must be zero (as it would be if a
            //     workflow had just been inserted)
            //   * we have more than one child.  This filters out the scenario
            //     during normal editing when the first child is created
            //     underneath a particular parent.
            //   * Only one of our children is "size" related.
            if (node.getAttribute(WBSModel.WORKFLOW_SOURCE_IDS_ATTR) != null
                    && bottomUpValue == 0 && children.length > 1
                    && numToInclude == 1) {
                WBSNode delegate = getSingleLeafForNode(node, false);
                if (delegate != null) {
                    userChangingValue(delegate, topDownValue);
                    delegate.setNumericAttribute(topDownAttrName, topDownValue);
                    return true;
                }
            }
            return false;
        }
    }

    private static class NewChangedSizeColumn extends AbstractNumericColumn
        implements CalculatedDataColumn
    {
        DataTableModel dataModel;
        String addID, modID;
        int addedColumn, modifiedColumn;

        public NewChangedSizeColumn(DataTableModel m, String id) {
            this.dataModel = m;
            this.columnName = this.columnID = getNCID(id);
            this.addID = getAddedID(id);
            this.modID = getModifiedID(id);
            this.dependentColumns = new String[] { addID, modID };
        }

        public boolean recalculate() { return true; }

        public void storeDependentColumn(String ID, int columnNumber) {
            if (addID.equals(ID)) addedColumn = columnNumber;
            else if (modID.equals(ID)) modifiedColumn = columnNumber;
        }

        public boolean isCellEditable(WBSNode node) {
            return dataModel.isCellEditable(node,addedColumn);
        }


        public Object getValueAt(WBSNode node) {
            NumericDataValue added =
                (NumericDataValue) dataModel.getValueAt(node, addedColumn);
            NumericDataValue modified =
                (NumericDataValue) dataModel.getValueAt(node, modifiedColumn);

            double value = safe(added.value) + safe(modified.value);
            double bottomUp =
                safe(added.expectedValue) + safe(modified.expectedValue);

            String errMsg = null;
            if (added.errorMessage != null || modified.errorMessage != null)
                errMsg = "top-down/bottom-up mismatch (bottom-up = " +
                    NumericDataValue.format(bottomUp) + ")";

            boolean editable = added.isEditable;
            boolean invisible = added.isInvisible;

            return new NumericDataValue
                (value, editable, invisible, errMsg, bottomUp);
        }
        private double safe(double v) {
            return (Double.isNaN(v) ? 0 : v);
        }

        protected void setValueForNode(double value, WBSNode node) {
            if (Double.isNaN(value) || value < 0) return;

            double nc = value, added, modified;
            added = NumericDataValue.parse
                (dataModel.getValueAt(node, addedColumn));
            modified = NumericDataValue.parse
                (dataModel.getValueAt(node, modifiedColumn));

            if (nc > modified) {
                // when the user edit "new & changed", try to accomodate them
                // by altering the "added" value.
                added = nc - modified;
                dataModel.setValueAt(new Double(added), node, addedColumn);
            } else {
                // if the strategy above would result in a negative value for
                // "added", then zero out added and decrease the modified value
                dataModel.setValueAt(new Double(0),  node, addedColumn);
                dataModel.setValueAt(new Double(nc), node, modifiedColumn);
            }
        }
        public void resetDependentColumns() {
            addedColumn = modifiedColumn = -1;
        }
    }

    private static class TotalSizeColumn extends AbstractNumericColumn
        implements CalculatedDataColumn
    {
        DataTableModel dataModel;
        String baseID;    int baseColumn;
        String deletedID; int deletedColumn;
        String addedID;   int addedColumn;
        String reusedID;  int reusedColumn;

        public TotalSizeColumn(DataTableModel m, String id) {
            this.dataModel = m;
            this.columnName = this.columnID = getTotalID(id);
            baseID    = getBaseID(id);
            deletedID = getDeletedID(id);
            addedID   = getAddedID(id);
            reusedID  = getReusedID(id);
            this.dependentColumns = new String[] {
                baseID, deletedID, addedID, reusedID };
        }

        public boolean recalculate() { return true; }

        public void storeDependentColumn(String ID, int columnNumber) {
            if (ID == null) return;
            if      (ID.equals(baseID))     baseColumn    = columnNumber;
            else if (ID.equals(deletedID))  deletedColumn = columnNumber;
            else if (ID.equals(addedID))    addedColumn   = columnNumber;
            else if (ID.equals(reusedID))   reusedColumn  = columnNumber;
        }

        public boolean isCellEditable(WBSNode node) { return false; }

        private double getVal(WBSNode node, int column) {
            return NumericDataValue.parse(dataModel.getValueAt(node, column));
        }

        public Object getValueAt(WBSNode node) {
            double base    = getVal(node, baseColumn);
            double deleted = getVal(node, deletedColumn);
            double added   = getVal(node, addedColumn);
            double reused  = getVal(node, reusedColumn);
            double total   = base - deleted + added + reused;

            return new NumericDataValue(total, false);
        }
        public void resetDependentColumns() {
            baseColumn = deletedColumn = addedColumn = reusedColumn = -1;
        }
    }

    /** Constrains user input, only allowing users to enter top-down sizes
     * on a particular type of WBS node.
     * 
     * This is necessary because the logic which writes a dump of WBS data
     * ({@link teamdash.wbs.WBSDataWriter}) and the class which synchronizes
     * the hierarchy ({@link teamdash.templates.setup.HierarchySynchronizer})
     * only expect to see one top-down size per node.  This class effectively
     * helps to enforce that constraint;  for each custom size metric, top-down
     * metrics can only be entered on a node with the corresponding type.
     * (Of course, LOC are a different story altogether, since LOC estimates
     * will only be written and synched for PSP tasks.)
     */
    private static class NodeTypeColumnFilter implements CalculatedDataColumn,
            ConflictCapableDataColumn {

        CalculatedDataColumn column;
        String editableType;

        public NodeTypeColumnFilter(CalculatedDataColumn c,
                                    String editableType) {
            this.column = c;
            this.editableType = editableType;
        }

        public boolean recalculate() {
            return column.recalculate();
        }
        public String[] getDependentColumnIDs() {
            return column.getDependentColumnIDs();
        }
        public String[] getAffectedColumnIDs() {
            return column.getAffectedColumnIDs();
        }
        public void storeDependentColumn(String ID, int columnNumber) {
            column.storeDependentColumn(ID, columnNumber);
        }
        public String getColumnID() {
            return column.getColumnID();
        }
        public String getColumnName() {
            return column.getColumnName();
        }
        public Class getColumnClass() {
            return column.getColumnClass();
        }
        public int getPreferredWidth() {
            return column.getPreferredWidth();
        }

        protected boolean isFiltered(WBSNode node) {
            return !editableType.equals(node.getType());
        }
        public boolean isCellEditable(WBSNode node) {
            // if our delegate says the column isn't editable, they're right.
            if (!column.isCellEditable(node)) return false;
            // if we aren't filtering this node, then it's editable.
            if (!isFiltered(node)) return true;
            // if this node has a non-erroneous zero or NaN value, it isn't
            // editable.
            Object value = column.getValueAt(node);
            if (value instanceof NumericDataValue &&
                ((NumericDataValue) value).errorMessage != null) return true;
            return NumericDataValue.parse(value) > 0;
        }

        public Object getValueAt(WBSNode node) {
            NumericDataValue result =
                (NumericDataValue) column.getValueAt(node);
            if (result == null) return result;
            if (isFiltered(node) && !(result.value > 0) &&
                result.errorMessage == null) {
                result.isEditable = false;
                result.isInvisible = true;
            }
            return result;
        }
        public void setValueAt(Object aValue, WBSNode node) {
            column.setValueAt(aValue, node);
        }
        public void resetDependentColumns() {}

        public PatternList getConflictAttributeNamePattern() {
            return ccdc().getConflictAttributeNamePattern();
        }
        public Object getConflictDisplayValue(String value, WBSNode node) {
            return ccdc().getConflictDisplayValue(value, node);
        }
        public void adjustConflictNotification(MergeConflictNotification mcn) {
            ccdc().adjustConflictNotification(mcn);
        }
        public void storeConflictResolutionValue(Object value, WBSNode node) {
            ccdc().storeConflictResolutionValue(value, node);
        }
        private ConflictCapableDataColumn ccdc() {
            return (ConflictCapableDataColumn) column;
        }
    }
}
