// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2003 Software Process Dashboard Initiative
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


import java.io.IOException;

import net.sourceforge.processdash.DashController;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.process.DefectTypeStandard;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringUtils;



/** CGI script for editing defect type standards.
 */
public class EditDefectTypeStandards extends TinyCGIBase {

    protected static final String ACTION = "action";
    protected static final String CONFIRM = "confirm";
    protected static final String CREATE = "create";
    protected static final String NAME = "name";
    protected static final String SAVE = "save";
    protected static final String SET_DEFAULT = "setDefault";
    protected static final String CONTENTS = "contents";

    protected static final String[] OPTIONS = {
        "View", "Edit", "Delete", "Copy", "Default" };

    private static final int VIEW = 0;
    private static final int EDIT = 1;
    private static final int DELETE = 2;
    private static final int COPY = 3;
    private static final int DEFAULT = 4;

    private static int uniqueNumber = 0;

    protected void doPost() throws IOException {
        DashController.checkIP(env.get("REMOTE_ADDR"));
        parseFormData();
        if (parameters.containsKey(SAVE)) {
            save(getParameter(NAME), getParameter(CONTENTS));
        } else if (parameters.containsKey(SET_DEFAULT)) {
            saveDefault(getParameter(NAME));
        }

        out.print("Location: dtsEdit.class?"+uniqueNumber+"\r\n\r\n");
        uniqueNumber++;
    }

    /** Generate CGI script output. */
    protected void doGet() throws IOException {
        writeHeader();

        String action = getParameter(ACTION);
        String name = getParameter(NAME);
        if (CREATE.equals(action))
            createNew();
        else if (OPTIONS[VIEW].equals(action))
            showStandard(name);
        else if (OPTIONS[EDIT].equals(action))
            editExisting(name);
        else if (OPTIONS[DELETE].equals(action))
            deleteExisting(name);
        else if (OPTIONS[COPY].equals(action))
            copyExisting(name);
        else if (OPTIONS[DEFAULT].equals(action))
            setDefault(name);
        else
            showListOfDefinedStandards();
    }

    private static final Resources resources =
        Resources.getDashBundle("Defects");

    protected String getHTML(String key) {
        return HTMLUtils.escapeEntities(resources.getString(key));
    }

    protected void writeHTMLHeader() {
        String title = resources.getString("Page_Title");
        out.print("<html><head><title>");
        out.print(title);
        out.print("</title>\n"+
                  "<link rel=stylesheet type='text/css' href='/style.css'>\n"+
                  "<style>\n"+
                  "  TD { padding-right: 0.3cm }\n"+
                  "</style>\n"+
                  "</head><body><h1>");
        out.print(title);
        out.print("</h1>\n");
    }

    protected void showStandard(String name) throws IOException {
        out.print("<html><head><meta http-equiv='Refresh' "+
                  "CONTENT='0;URL=../reports/dts.class?name=");
        out.print(HTMLUtils.urlEncode(name));
        out.print("'></head><body>&nbsp;</body></html>");
    }


    protected void showListOfDefinedStandards() throws IOException {
        DataRepository data = getDataRepository();
        String[] standards = DefectTypeStandard.getDefinedStandards(data);

        String defaultName = DefectTypeStandard.get("", data).getName();

        writeHTMLHeader();
        out.print("<p>");
        out.println(getHTML("Welcome_Prompt"));
        out.println("<ul>");
        out.print("<li><a href=\"dtsEdit.class?"+ACTION+"="+CREATE+"\">");
        out.print(getHTML("Create_Option"));
        out.println("</a></li>");

        if (standards.length > 0) {
            out.print("<li>");
            out.print(getHTML("Manage_Option"));
            out.println("<table>");

            for (int i = 0;   i < standards.length;   i++) {
                String htmlName = HTMLUtils.escapeEntities(standards[i]);
                String urlName = HTMLUtils.urlEncode(standards[i]);

                out.print("<tr><td><ul><li>&quot;<b>");
                out.print(htmlName);
                out.print("</b>&quot;</li></ul></td>");

                for (int o = 0;   o < OPTIONS.length;   o++) {
                    if (o == DEFAULT && standards[i].equals(defaultName)) {
                        out.print("<td><i>(default)</i></td>");
                    } else {
                        String opt = OPTIONS[o];
                        out.print("<td><a href='dtsEdit.class?"+ACTION+"="+opt+
                                  "&"+NAME+"="+urlName+"'>");
                        out.print(getHTML(opt));
                        out.print("</a></td>");
                    }
                }
                out.println("</tr>");
            }
        }

        out.print("</table></li></ul></body></html>");
    }

    protected void showName(String standardName, boolean editable)
        throws IOException
    {
        out.print("<p><b>");
        out.print(getHTML("Name_Prompt"));
        out.print("</b>&nbsp;");

        if (!editable)
            out.print(HTMLUtils.escapeEntities(standardName));

        out.print("<input type=");
        out.print(editable ? "text size=40" : "hidden");
        out.print(" name='"+NAME+"' value='");
        out.print(HTMLUtils.escapeEntities(standardName));
        out.print("'>");
    }

    protected void showEditBox(String standardName) throws IOException {
        DefectTypeStandard defectTypeStandard = null;
        if (standardName != null)
            defectTypeStandard = DefectTypeStandard.getByName
                (standardName, getDataRepository());

        out.print("<p>");
        out.println(getHTML("Edit_Instructions"));
        out.print("<br><textarea name='"+CONTENTS+"' rows=12 cols=80>");

        if (defectTypeStandard == null) {
            out.print(getHTML("Sample_Defect_Type"));
        } else {
            String type, description;
            for (int i=0;  i<defectTypeStandard.options.size();  i++) {
                type = (String) defectTypeStandard.options.elementAt(i);
                description = (String) defectTypeStandard.comments.get(type);
                out.print(HTMLUtils.escapeEntities(type));
                if (description != null && description.length() > 0) {
                    out.print(" (");
                    out.print(HTMLUtils.escapeEntities(description));
                    out.print(")");
                }
                out.println();
            }
        }
        out.println("</textarea>");
    }

    protected void drawForm(String headerKey,
                            String nameToDisplay,
                            boolean nameEditable,
                            String realName) throws IOException {

        writeHTMLHeader();
        out.print("<h2>");
        out.print(getHTML(headerKey));
        out.println("</h2>");
        out.println("<form action='dtsEdit.class' method='POST'>");
        showName(nameToDisplay, nameEditable);
        showEditBox(realName);

        out.print("<p><input type=submit name='"+SAVE+"' value='");
        out.print(HTMLUtils.escapeEntities(resources.getString("Save")));
        out.print("'>&nbsp;");

        out.print("<input type=submit name='cancel' value='");
        out.print(HTMLUtils.escapeEntities(resources.getString("Cancel")));
        out.print("'>");
        out.print("</form></body></html>");
    }

    protected void editExisting(String standardName) throws IOException {
        drawForm("Edit_Existing", standardName, false, standardName);
    }

    protected void createNew() throws IOException {
        drawForm("Create_New", "Enter Name", true, null);
    }

    protected void copyExisting(String standardName) throws IOException {
        drawForm("Copy_Existing", "Enter New Name", true, standardName);
    }


    protected void save(String standardName, String contents) {
        String[] types = null;
        if (contents != null)
            types = StringUtils.split(contents, "\n");
        DefectTypeStandard.save
            (standardName, getDataRepository(), types,
             resources.getString("Sample_Defect_Type"));
    }

    protected void deleteExisting(String standardName) throws IOException {
        writeHTMLHeader();
        out.print("<h2>");
        out.print(getHTML("Delete_Existing"));
        out.println("</h2><p>");
        out.print(getHTML("Delete_Existing_Prompt"));
        out.println("<form action='dtsEdit.class' method='POST'>");
        showName(standardName, false);

        out.print("<p><input type=submit name='"+SAVE+"' value='");
        out.print(HTMLUtils.escapeEntities(resources.getString("OK")));
        out.print("'>&nbsp;");

        out.print("<input type=submit name='cancel' value='");
        out.print(HTMLUtils.escapeEntities(resources.getString("Cancel")));
        out.print("'>");
        out.print("</form></body></html>");
    }

    protected void setDefault(String standardName) throws IOException {
        writeHTMLHeader();
        out.print("<h2>");
        out.print(getHTML("Set_As_Default"));
        out.println("</h2><p>");
        out.print(getHTML("Set_As_Default_Prompt"));
        out.println("<form action='dtsEdit.class' method='POST'>");
        showName(standardName, false);

        out.print("<p><input type=submit name='"+SET_DEFAULT+"' value='");
        out.print(HTMLUtils.escapeEntities(resources.getString("OK")));
        out.print("'>&nbsp;");

        out.print("<input type=submit name='cancel' value='");
        out.print(HTMLUtils.escapeEntities(resources.getString("Cancel")));
        out.print("'>");
        out.print("</form></body></html>");
    }

    protected void saveDefault(String standardName) {
        DefectTypeStandard.saveDefault(getDataRepository(), "", standardName);
    }
}
