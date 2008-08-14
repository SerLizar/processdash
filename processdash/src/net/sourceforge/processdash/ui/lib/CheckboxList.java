// Copyright (C) 2008 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui.lib;

import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;

public class CheckboxList extends JTable {

    private Map<TableCellRenderer, TableCellRenderer> rendererMap;

    public CheckboxList(Object[] listData) {
        super(new CheckmarkTableModel(listData));
        setTableHeader(null);
        setShowGrid(false);
        setIntercellSpacing(new Dimension(0, 1));
        getColumnModel().getColumn(0).setMaxWidth(30);
        setFillsViewportHeight(true);
        rendererMap = new HashMap<TableCellRenderer, TableCellRenderer>();
    }

    public void setChecked(int row, boolean checked) {
        getModel().setValueAt(Boolean.valueOf(checked), row, 0);
    }

    public boolean getChecked(int row) {
        return Boolean.TRUE == getModel().getValueAt(row, 0);
    }

    public void setAllChecked(boolean checked) {
        ((CheckmarkTableModel) getModel()).setAllChecked(checked);
    }

    public int[] getCheckedIndexes() {
        List<Integer> rows = getCheckedRowNumbers();
        int[] result = new int[rows.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = rows.get(i);
        }
        return result;
    }

    public void setCheckedIndexes(int[] indexes) {
        setAllChecked(false);
        for (int i = 0; i < indexes.length; i++)
            setChecked(indexes[i], true);
    }

    public Object[] getCheckedItems() {
        List<Integer> rows = getCheckedRowNumbers();
        Object[] result = new Object[rows.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = getModel().getValueAt(rows.get(i), 1);
        }
        return result;
    }

    public void setCheckedItems(Iterable items) {
        setAllChecked(false);
        for (Object item : items)
            ((CheckmarkTableModel) getModel()).setChecked(item, true);
    }

    private List<Integer> getCheckedRowNumbers() {
        List<Integer> result = new ArrayList<Integer>();
        for (int i = 0; i < getRowCount(); i++) {
            if (getModel().getValueAt(i, 0) == Boolean.TRUE)
                result.add(i);
        }
        return result;
    }

    @Override
    public TableCellRenderer getCellRenderer(int row, int column) {
        TableCellRenderer baseRenderer = super.getCellRenderer(row, column);
        TableCellRenderer result = rendererMap.get(baseRenderer);
        if (result == null) {
            result = new NoBorderCellRenderer(baseRenderer);
            rendererMap.put(baseRenderer, result);
        }
        return result;
    }



    private static class CheckmarkTableModel extends AbstractTableModel {

        private Object[] data;

        private boolean[] checked;

        public CheckmarkTableModel(Object[] data) {
            this.data = data;
            this.checked = new boolean[data.length];
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 0)
                return Boolean.class;
            else
                return Object.class;
        }

        public int getColumnCount() {
            return 2;
        }

        public int getRowCount() {
            return data.length;
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            switch (columnIndex) {
            case 0:
                return checked[rowIndex];
            case 1:
                return data[rowIndex];
            default:
                return null;
            }
        }

        @Override
        public void setValueAt(Object value, int rowIndex, int columnIndex) {
            if (columnIndex == 0) {
                checked[rowIndex] = (value == Boolean.TRUE);
                fireTableCellUpdated(rowIndex, columnIndex);
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return (columnIndex == 0);
        }

        public void setAllChecked(boolean check) {
            Arrays.fill(checked, check);
            fireTableDataChanged();
        }

        public void setChecked(Object item, boolean check) {
            for (int i = 0; i < data.length; i++) {
                if (data[i] == item) {
                    checked[i] = check;
                    fireTableCellUpdated(i, 0);
                    return;
                }
            }
        }

    }

    private static class NoBorderCellRenderer implements TableCellRenderer {
        TableCellRenderer delegate;

        private NoBorderCellRenderer(TableCellRenderer delegate) {
            this.delegate = delegate;
        }

        public Component getTableCellRendererComponent(JTable table,
                Object value, boolean isSelected, boolean hasFocus, int row,
                int column) {
            Component result = delegate.getTableCellRendererComponent(table,
                value, isSelected, hasFocus, row, column);
            if (result instanceof JComponent)
                ((JComponent) result).setBorder(null);
            return result;
        }

    }

}
