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
import java.util.Map;

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringUtils;


public abstract class AbstractViewPageAssembler extends AbstractPageAssembler {


    protected String insertEditLink(String html) {
        if (html == null || html.length() == 0) return "";
        if (parameters.containsKey("EXPORT") || Settings.isReadOnly())
            return html;

        int pos = html.indexOf("<!-- editLink");
        if (pos == -1) return html;
        int end = html.indexOf("-->", pos);
        if (end == -1) return html;

        StringBuffer link = new StringBuffer();
        link.append("<span class='cmsToolbar'><a href='");
        link.append(HTMLUtils.escapeEntities(getEditURI(environment)));
        String target = getEditLinkTarget();
        if (target != null)
            link.append("' target='").append(target);
        link.append("' title='");
        link.append(resources.getHTML("Edit_Hyperlink.Description"));
        link.append("'>");
        link.append(EDIT_ICON_IMG_HTML);
        link.append("</a></span>\n");

        int size=32;
        if (html.lastIndexOf("Med", end) > pos) size = 22;
        else if (html.lastIndexOf("Small", end) > pos) size = 14;
        StringUtils.findAndReplace(link, "##", Integer.toString(size));

        return html.substring(0, pos) + link + html.substring(end+3);
    }
    private static final String EDIT_ICON_IMG_HTML =
        "<img src='/Images/edit##.gif' width='##' height='##' border='0'/>";

    protected String getEditURI(Map env) {
        return CmsContentDispatcher.getSimpleSelfUri(env, true) + "?mode=edit";
    }

    protected String getEditLinkTarget() {
        return "_top";
    }

    protected void writeSnippet(Writer out, SnippetInstanceTO snippet)
            throws IOException {

        int status = snippet.getStatus();
        if (status < SnippetInvoker.STATUS_OK)
            ; // Do nothing.

        else if (status == SnippetInvoker.STATUS_OK) {
            String generatedContent = snippet.getGeneratedContent();
            if (snippet.getPageRegion() == PageContentTO.REGION_HEADER)
                generatedContent = insertEditLink(generatedContent);
            out.write(generatedContent);

        } else {
            String key = "View_Page.Errors."
                    + SnippetInvoker.STATUS_RESOURCE_KEYS[status] + "_FMT";
            String id = snippet.getSnippetID();
            String errDump = StringUtils.getStackTrace(snippet
                    .getInvocationException());
            out.write("<!-- ");
            out.write(esc(resources.format(key, id)));
            out.write(esc(errDump));
            out.write(" -->\n");
        }
    }

    protected String getPageTitle(PageContentTO page) {
        if (StringUtils.hasValue(page.getPageTitle()))
            return esc(page.getPageTitle());
        else
            return resources.getHTML("Page_Title.Empty");
    }
}