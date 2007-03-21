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
// OO-ALC/TISHD
// Attn: PSP Dashboard Group
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net


import pspdash.*;
import pspdash.data.DataRepository;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.text.NumberFormat;

public class r4 extends TinyCGIBase implements DefectAnalyzer.Task {

    // In the following map, keys are defect types (e.g. "Syntax") and
    // values are arrays of integers.
    protected Map defectCounts;
    protected int[] totals;
    protected boolean strict = true;

    public static final int INJ_DESIGN  = 0;
    public static final int INJ_CODE    = 1;
    public static final int REM_COMPILE = 2;
    public static final int REM_TEST    = 3;
    public static final int PRESENT_AT_COMP_ENTRY = 4;
    public static final int FOUND_IN_COMPILE = 5;


    private static final String [] FILTERS = {
        "inj=Design", "inj=Code", "rem=Compile", "rem=Test" };

    private static final String HEADER_TEXT =
        "<HTML><HEAD><TITLE>Report R4</TITLE>%css%\n" +
        "<STYLE>\n" +
        "    TABLE { empty-cells: show }\n" +
        "    TD { text-align:center; vertical-align: baseline }\n" +
        "    .header { font-weight: bold; vertical-align:bottom }\n" +
        "    .footnote { font-size: small; font-style:italic }\n" +
        "    @media print { .doNotPrint { display: none } }\n" +
        "</STYLE></HEAD>\n" +
        "<BODY><H1>%path%</H1><H2>Report R4</H2>";

    /** Generate CGI script output. */
    protected void writeContents() {

        String path = getPrefix();
        strict = (parameters.get("strict") != null);

        initValues();
        DefectAnalyzer.run(getPSPProperties(), getDataRepository(),
                           path, parameters, this);

        String header = HEADER_TEXT;
        header = StringUtils.findAndReplace(header, "%css%", cssLinkHTML());
        header = StringUtils.findAndReplace(header, "%path%", path);
        out.println(header);

        out.println("<H3>Table D23</H3>");
        out.println("<TABLE NAME=D23 BORDER><TR class=header><TD></TD>");
        out.println("<TD colspan=2>Number Injected</TD>");
        out.println("<TD colspan=2>Percentage Injected</TD>");
        out.println("<TD colspan=2>Number Removed</TD>");
        out.println("<TD colspan=2>Percentage Removed</TD></TR>");

        out.println("<TR><TD>Type</TD>");
        out.println("<TD>Design</TD><TD>Code</TD>");
        out.println("<TD>Design</TD><TD>Code</TD>");
        out.println("<TD>Compile</TD><TD>Test</TD>");
        out.println("<TD>Compile</TD><TD>Test</TD></TR>");

        String defectLogParam = (String) env.get("QUERY_STRING");
        if (defectLogParam == null)
            defectLogParam = "";
        else
            defectLogParam = StringUtils.findAndReplace
                (defectLogParam, "qf=../", "qf=");

        Iterator defectTypes = defectCounts.keySet().iterator();
        String defectType;
        int [] row;
        while (defectTypes.hasNext()) {
            defectType = (String) defectTypes.next();
            row = (int[]) defectCounts.get(defectType);
            printD23(defectLogParam, defectType, row);
        }
        printD23(defectLogParam, "Total", totals);

        out.println("</TABLE>");


        out.println("<H3>Table D24</H3>");
        out.println("<TABLE NAME=D24 BORDER>");
        out.println("<TR class=header><TD>Defect Type</TD>");
        out.print("<TD VALIGN=bottom>Number of defects at Compile Entry</TD>");
        out.print("<TD VALIGN=bottom>Number of defects found in Compile</TD>");
        out.print("<TD VALIGN=bottom>Percentage of Type found by the " +
                  "Compiler</TD></TR>\n");

        defectTypes = defectCounts.keySet().iterator();
        while (defectTypes.hasNext()) {
            defectType = (String) defectTypes.next();
            row = (int[]) defectCounts.get(defectType);
            printD24(defectType, row);
        }
        printD24("Total", totals);

        out.println("</TABLE>");
        out.println("<P class='doNotPrint'><A HREF=\"../excel.iqy\"><I>Export"+
                    " to Excel</I></A></P>");
        if (strict) {
            String query = (String) env.get("QUERY_STRING");
            query = StringUtils.findAndReplace(query, "strict", "notstrict");
            out.println("<P><HR>" +
                        StringUtils.findAndReplace(FOOTNOTE, "%qs%", query));
        }
        out.println("</BODY></HTML>");
    }
    protected static final String FOOTNOTE =
        "<P class=footnote><span class=doNotPrint>To reduce clutter, "+
        "and omit completely empty rows from the tables above, " +
        "<A HREF='r4.class?%qs%'>click here</A>.</span></P>";

    protected void printD23(String param, String label, int [] row) {
        String dt = param;
        if (!label.startsWith("Total")) {
            dt += ("&type=" + URLEncoder.encode(label));
            if (!strict && 0 == (row[INJ_DESIGN] + row[INJ_CODE] +
                                 row[REM_COMPILE] + row[REM_TEST]))
                return;
        }
        out.println("<TR><TD><A HREF=\"../defectlog.class?" + dt +"\">" +
                    label + "</A></TD>");
        out.println("<TD>" + fc(dt, row, INJ_DESIGN) + "</TD>");
        out.println("<TD>" + fc(dt, row, INJ_CODE) + "</TD>");
        out.println("<TD>" + fp(row, INJ_DESIGN) + "</TD>");
        out.println("<TD>" + fp(row, INJ_CODE) + "</TD>");
        out.println("<TD>" + fc(dt, row, REM_COMPILE) + "</TD>");
        out.println("<TD>" + fc(dt, row, REM_TEST) + "</TD>");
        out.println("<TD>" + fp(row, REM_COMPILE) + "</TD>");
        out.println("<TD>" + fp(row, REM_TEST) + "</TD></TR>");
    }
    /** format a count, found in slot col of array row. */
    protected String fc(String dt, int [] row, int col) {
        if (row[col] == 0) return NA;
        if (dt == null) return Integer.toString(row[col]);
        return "<A HREF=\"../defectlog.class?" + FILTERS[col] + "&" + dt
            +"\">" + row[col] + "</A>";
    }
    /** format a percentage, calculated by dividing item n of row by item d */
    protected String fp(int num, int denom) {
        if (num == 0 || denom == 0) return NA;
        return PERCENT_FORMAT.format(((double) num) / denom);
        //int val = (100 * num) / denom;
        //return Integer.toString(val) + "%";
    }
    protected String fp(int [] row, int n) { return fp(row[n], totals[n]); }
    protected String fp(int [] row, int n, int d) { return fp(row[n],row[d]); }
    private static final String NA = "-";
    private static final NumberFormat PERCENT_FORMAT =
        NumberFormat.getPercentInstance();
    static {
        PERCENT_FORMAT.setMaximumFractionDigits(0);
    }


    protected void printD24(String label, int [] row) {
        if (!strict && 0 == (row[PRESENT_AT_COMP_ENTRY] +
                             row[FOUND_IN_COMPILE]))
            return;

        out.println("<TR><TD>" + label + "</TD>");
        out.println("<TD>" + fc(null, row, PRESENT_AT_COMP_ENTRY) + "</TD>");
        out.println("<TD>" + fc(null, row, FOUND_IN_COMPILE) + "</TD>");
        out.println("<TD>" + fp(row, FOUND_IN_COMPILE,
                                PRESENT_AT_COMP_ENTRY) + "</TD></TR>");
    }

    /** Generate an empty row of the appropriate size */
    private int[] emptyRow() {
        int [] result = new int[6];
        for (int i=0;  i<6;  i++) result[i] = 0;
        return result;
    }
    /** Initialize internal data structures to zero */
    private void initValues() {
        totals = emptyRow();
        defectCounts = new TreeMap();
        if (strict) {
            DefectTypeStandard dts =
                DefectTypeStandard.get(getPrefix(), getDataRepository());
            for (int i=dts.options.size();  i-->0; )
                getRow((String) dts.options.elementAt(i));
        }
    }

    /** Lookup the row for a defect type - create it if it doesn't exist. */
    private int[] getRow(String defectType) {
        int [] result = (int[]) defectCounts.get(defectType);
        if (result == null)
            defectCounts.put(defectType, result = emptyRow());
        return result;
    }
    /** Increment a defect count for a particular defect type */
    protected void increment(int [] row, int type) {
        totals[type]++;
        row[type]++;
    }

    protected boolean test(String defPhase, String cmpPhase) {
        return defPhase.equals(cmpPhase) || defPhase.endsWith("/"+cmpPhase);
    }

    public void analyze(String path, Defect d) {
        int [] row = getRow(d.defect_type);

        if (test(d.phase_injected, "Design")) increment(row, INJ_DESIGN);
        else if (test(d.phase_injected, "Code")) increment(row, INJ_CODE);

        if (test(d.phase_removed, "Compile")) increment(row, REM_COMPILE);
        else if (test(d.phase_removed, "Test")) increment(row, REM_TEST);

        if (!test(d.phase_injected, "Compile") &&
            !test(d.phase_injected, "Test") &&
            !test(d.phase_injected, "Reassessment") &&
            !test(d.phase_injected, "Postmortem"))
            if (d.phase_removed.endsWith("Compile")) {
                increment(row, PRESENT_AT_COMP_ENTRY);
                increment(row, FOUND_IN_COMPILE);
            } else if (d.phase_removed.endsWith("Test") ||
                       d.phase_removed.endsWith("Reassessment") ||
                       d.phase_removed.endsWith("Postmortem"))
                increment(row, PRESENT_AT_COMP_ENTRY);
    }
}