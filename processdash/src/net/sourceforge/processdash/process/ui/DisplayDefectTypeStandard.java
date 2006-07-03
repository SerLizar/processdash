// Copyright (C) 2003-2006 Tuma Solutions, LLC
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


package net.sourceforge.processdash.process.ui;


import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.process.DefectTypeStandard;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.HTMLUtils;


public class DisplayDefectTypeStandard extends TinyCGIBase {

    private static final String NAME = "name";
    private static final Resources resources =
        Resources.getDashBundle("Defects.Standard.Display");

    /** Generate CGI script output. */
    protected void writeContents() {

        DefectTypeStandard defectTypeStandard = null;
        if (parameters.get(NAME) != null)
            defectTypeStandard = DefectTypeStandard.getByName
                (getParameter(NAME), getDataRepository());
        if (defectTypeStandard == null)
            defectTypeStandard = DefectTypeStandard.get
                (getPrefix(), getDataRepository());

        String name = defectTypeStandard.getName();
        if (name == null) name = "";
        String title = resources.format("Title_FMT", name);

        out.println("<HTML><HEAD>" + cssLinkHTML());
        out.println("<TITLE>" + title + "</TITLE>");
        out.println("</HEAD><BODY>");
        out.println("<H1>" + title + "</H1>");
        out.print("<TABLE BORDER><TR><TD><B>");
        out.print(resources.getString("Type"));
        out.println("</B></TD>");
        out.print("    <TD><B>");
        out.print(resources.getString("Description"));
        out.println("</B></TD></TR>");

        String type, description;
        for (int i=0;  i<defectTypeStandard.options.size();  i++) {
            type = (String) defectTypeStandard.options.elementAt(i);
            description = (String) defectTypeStandard.comments.get(type);
            if (description == null) description = "&nbsp;";

            out.print("<TR><TD VALIGN=baseline>");
            out.print(HTMLUtils.escapeEntities(type));
            out.println("</TD>");
            out.print("    <TD VALIGN=baseline>");
            out.print(HTMLUtils.escapeEntities(description));
            out.println("</TD></TR>");
        }
        out.println("</TABLE>");
        if (Settings.isReadWrite())
            out.println("<p class='doNotPrint'>"+
                        "<a href='/dash/dtsEdit.class'><i>"+
                        resources.getHTML("Create_Edit_Link")+
                        "</i></a></p>");
        if ("PSP - text".equals(name) || "PSP - numbers".equals(name))
            out.println(COPYRIGHT_NOTICE);
        out.println("</BODY></HTML>");
    }

    private static final String COPYRIGHT_NOTICE =
        "<HR><FONT SIZE='-1'><I>Adapted from\n" +
        "<a href='/help/Topics/Overview/disc-eng.html'>A&nbsp;Discipline\n" +
        "for Software Engineering</a>, copyright &copy;&nbsp;1995\n" +
        "Addison-Wesley. Used by permission.</I></FONT>\n";
}
