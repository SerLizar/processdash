// Copyright (C) 2006 Tuma Solutions, LLC
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

package net.sourceforge.processdash.net.cms;

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.Set;

/** Page assembler that can render HTML for viewing a page of CMS content.
 */
public class ViewSinglePageAssembler extends AbstractViewPageAssembler {

    protected void writePage(Writer out, Set headerItems, PageContentTO page)
            throws IOException {

        //out.write(HTML_TRANSITIONAL_DOCTYPE);
        out.write("<html>\n");
        writeHead(out, headerItems, page);
        out.write("<body>\n");
        out.write("<div><form>\n\n");

        PageSectionHelper.hideInvalidSections(page);
        for (Iterator i = page.getSnippets().iterator(); i.hasNext();) {
            SnippetInstanceTO snip = (SnippetInstanceTO) i.next();
            writeSnippet(out, snip);
        }

        out.write("</form></div>\n\n");
        out.write("<hr>\n");
        out.write("<script src='/data.js?showExcelLink' type='text/javascript'> </script>\n");
        out.write("</body>\n");
        out.write("</html>\n");
    }

    protected void addPageSpecificHeaderItems(Set headerItems) {
        super.addPageSpecificHeaderItems(headerItems);
        addScript(headerItems, "/lib/fixSlash.js");
    }


}
