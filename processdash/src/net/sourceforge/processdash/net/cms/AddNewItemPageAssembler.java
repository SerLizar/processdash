// Copyright (C) 2006 Tuma Solutions, LLC
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

package net.sourceforge.processdash.net.cms;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringUtils;

/** Specialized handler for adding items to a page.
 * 
 * This class reuses the functionality of the {@link EditSinglePageAssembler}
 * to write an editing div for a <u>single</u> snippet, which will be sent back
 * over an AJAX request for addition to an in-progress editing page.
 */
public class AddNewItemPageAssembler extends EditSinglePageAssembler {


    public void service(Writer out, PageContentTO page) throws IOException {
        // turn on the "defaults" parameter (which will get propagated to
        // the snippet).  This allows the snippet to generate content
        // based on reasonable defaults, instead of persisted text.
        parameters.put("defaults", "true");

        // reset the mode to "edit", so snippets will render correctly
        // (previously, mode is "addNew" if this assembler was chosen)
        parameters.put("mode", "edit");

        super.service(out, page);
    }


    protected void addPageSpecificHeaderItems(Set headerItems) {
        // add nothing
    }


    protected void setSnippetNamespace(SnippetInstanceTO snippet,
            String namespace) {
        // ignore the namespace parameter passed in, and read from the
        // parameter instead.
        snippet.setNamespace((String) parameters.get("ns"));
    }

    protected void writePage(Writer out, Set headerItems, PageContentTO page)
            throws IOException {

        // if this snippet editor requires scripts or stylesheets, generate
        // javascript to add them to the page.
        if (headerItems != null && !headerItems.isEmpty())
            writeHeaderItemScript(out, headerItems);

        // write the HTML fragment for the single snippet editor.
        SnippetInstanceTO snip = (SnippetInstanceTO) page
                .getContentSnippets().get(0);
        writeSnippet(out, snip, false);

        // write javascript to notify the page that an item has been added.
        out.write("<script type='text/javascript'>");
        out.write("DashCMS.afterItemAdded();");
        out.write("</script>");
    }

    /** When we add a snippet editor to a page-in-progress, it may require
     * new scripts or stylesheets that weren't anticipated when we initially
     * constructed the page.  This method writes javascript enumerating these
     * additions, and leaves the rest up to the DashCMS javascript object.
     * 
     * @param out output stream for writing to the client
     * @param headerItems items that the snippet editor needs
     * @throws IOException if a problem is encountered.
     */
    private void writeHeaderItemScript(Writer out, Set headerItems)
            throws IOException {
        List scripts = new ArrayList();
        List styles = new ArrayList();
        for (Iterator iter = headerItems.iterator(); iter.hasNext();) {
            String item = (String) iter.next();
            Matcher m = SCRIPT_PATTERN.matcher(item);
            if (m.find())
                scripts.add(HTMLUtils.unescapeEntities(m.group(1)));
            m = STYLE_SHEET_PATTERN.matcher(item);
            if (m.find())
                styles.add(HTMLUtils.unescapeEntities(m.group(1)));
        }

        out.write("<script type='text/javascript'>\n");
        for (Iterator i = styles.iterator(); i.hasNext();) {
            String styleURI = (String) i.next();
            out.write("DashCMS.requireStyleSheet('"
                    + StringUtils.javaEncode(styleURI) + "');\n");
        }
        for (Iterator i = scripts.iterator(); i.hasNext();) {
            String styleURI = (String) i.next();
            out.write("DashCMS.requireScript('"
                    + StringUtils.javaEncode(styleURI) + "');\n");
        }
        out.write("</script>\n");
    }

    private static final Pattern SCRIPT_PATTERN = Pattern
            .compile("<script.*src=\"([^\"]+)\"");

    private static final Pattern STYLE_SHEET_PATTERN = Pattern
            .compile("<link.*href=\"([^\"]+)\"");

}
