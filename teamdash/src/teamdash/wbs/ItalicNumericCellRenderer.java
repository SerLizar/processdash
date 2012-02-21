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

package teamdash.wbs;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.JTable;

/** Special cell renderer for numbers that can italicize displayed values.
 * 
 * The presence of a certain error message is interpreted as a flag that the
 * value should be displayed in italics (rather than in a bold colored font,
 * like the regular {@link DataTableCellNumericRenderer} would do).
 */
public class ItalicNumericCellRenderer extends DataTableCellNumericRenderer {


    private Font italic = null;
    private String messageToItalicize;

    public ItalicNumericCellRenderer(String messageToItalicize) {
        this.messageToItalicize = messageToItalicize;
    }


    public Component getTableCellRendererComponent(JTable table,
                                                   Object value,
                                                   boolean isSelected,
                                                   boolean hasFocus,
                                                   int row,
                                                   int column) {

        Component result = super.getTableCellRendererComponent
            (table, value, isSelected, hasFocus, row, column);

        NumericDataValue num = (NumericDataValue) value;
        if (num != null && num.errorMessage != null &&
            num.errorMessage.equals(messageToItalicize)) {
            result.setForeground(Color.black);
            result.setFont(getItalicFont(result));
        }

        return result;
    }

    /** Create and cache an appropriate italic font. */
    protected Font getItalicFont(Component c) {
        if (italic == null) {
            Font regular = super.getFont(false, c);
            if (regular != null)
                 italic = regular.deriveFont(Font.ITALIC);
        }

        return italic;
    }
}
