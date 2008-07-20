// Copyright (C) 2002-2003 Tuma Solutions, LLC
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

import java.util.*;
import javax.swing.*;

import net.sourceforge.processdash.util.*;


/** This class provides a generally reusable feature for displaying a
 *  collected list of error messages to the user. To use this class,
 *  <ol>
 *    <li>Create an instance of ErrorReporter.
 *    <li>Possibly log one or more error messages using the
 *        <code>logError</code> method.
 *    <li>Call <code>done</code>.
 *  </ol>
 *  If any error messages have been logged when you call the done() method,
 *  they will be displayed to the user in a dialog box. If no errors were
 *  reported, the done() method will do nothing and return.
 */
public class ErrorReporter {

    String title;
    Object preMessage, postMessage;
    Vector errors;

    /** Create a new ErrorReporter.
     *
     * The parameters control the apppearance of the error dialog that
     * this object will display if needed.
     * @param title the title to display
     * @param preMessage the object(s) to display above the error list. See
     *     JOptionPane for valid choices.
     * @param postMessage the object(s) to display below the error list. See
     *     JOptionPane for valid choices.
     */
    public ErrorReporter(String title, Object preMessage, Object postMessage) {
        this.title = title;
        this.preMessage = (preMessage == null ? "" : preMessage);
        this.postMessage = (postMessage == null ? "" : postMessage);
        this.errors = new Vector();
    }

    /** Add an error to this error reporter.
     *
     * If the error message is a duplicate of one already logged for
     * this ErrorReporter, this method call will have no effect.
     */
    public synchronized void logError(String errorMessage) {
        if (!errors.contains(errorMessage))
            errors.add(errorMessage);
    }

    public void done() {
        if (errors.isEmpty()) return;

        StringBuffer errorList = new StringBuffer();
        errorList.append("<html><body><table border=0>\n");
        Iterator i = errors.iterator();
        while (i.hasNext()) {
            errorList.append("<tr><td valign=top><b>-</b></td><td>");
            errorList.append(HTMLUtils.escapeEntities((String) i.next()));
            errorList.append("</td></tr>\n");
        }
        errorList.append("</table></body></html>");
        JEditorPane jep = new JEditorPane("text/html", errorList.toString());
        jep.setEditable(false);
        jep.setCaretPosition(0);
        JScrollPane scrollPane = new JScrollPane(jep);
        scrollPane.setPreferredSize(new java.awt.Dimension(200, 200));

        Object [] dialogComponents = new Object[3];
        dialogComponents[0] = preMessage;
        dialogComponents[1] = scrollPane;
        dialogComponents[2] = postMessage;

        JOptionPane.showMessageDialog(null, dialogComponents, title,
                                      JOptionPane.ERROR_MESSAGE);
    }


}
