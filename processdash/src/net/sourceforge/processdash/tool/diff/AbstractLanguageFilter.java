// Copyright (C) 2001-2007 Tuma Solutions, LLC
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


package net.sourceforge.processdash.tool.diff;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.StringTokenizer;

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.util.StringUtils;


/** No-op implementation of the LanguageFilter interface.
 */
public class AbstractLanguageFilter implements LanguageFilter {

    public interface NamedFilter {
        public String getFilterName();
    }


    static final Resources resources = Resources.getDashBundle("LOCDiff");

    protected static final String COMMENT_START_STR =
        String.valueOf(COMMENT_START);
    protected static final String COMMENT_END_STR =
        String.valueOf(COMMENT_END);

    protected String LANG_OPTION, LANG_OPTION_SPACE;
    private String charset = "iso-8859-1";

    public AbstractLanguageFilter() {
        setLangName(getFilterName(this));
    }

    protected void setLangName(String name) {
        if (name != null) {
            LANG_OPTION = "-lang=" + name.toLowerCase();
            LANG_OPTION_SPACE = LANG_OPTION + " ";
        }
    }

    /** Judge whether this filter is capable of acting as a filter for a
     *  particular file.
     *
     * The default implementation gives the file 100 points if its filename
     * matches, and 30 points if it begins with a matching comment. If
     * the options include the string "-lang=Foo" (where Foo is the first
     * part of the name of this class), returns 1000.
     */
    public int languageMatches(String filename, String contents,
                               String options) {
        int result = 0;

        if (filename != null && filename.length() > 0) {
            String[] filenameEndings = getFilenameEndings();
            if (filenameEndings != null)
                for (int i = filenameEndings.length;   i-- > 0; )
                    if (endsWithIgnoreCase(filename, filenameEndings[i])) {
                        result += 100;
                        break;
                    }
        }

        if (contents != null && contents.length() > 0) {
            contents = contents.trim();     // skip initial whitespace.

            String[] commentStarters = getCommentStarters();
            if (commentStarters != null) {
                for (int i = commentStarters.length;   i-- > 0; )
                    if (startsWithIgnoreCase(contents, commentStarters[i])) {
                        result += 30;
                        break;
                    }
            }

            result = doubleCheckFileContents(contents, result);
        }

        if (options != null) {
            String opts = options.trim().toLowerCase();
            if (opts.endsWith(LANG_OPTION) ||
                opts.indexOf(LANG_OPTION_SPACE) != -1)
                result = 1000;
        }

        if (result > 0)
            setOptions(options == null ? "" : options);

        return result;
    }

    protected int doubleCheckFileContents(String contents, int match) {
        return match;
    }

    protected static boolean endsWithIgnoreCase(String s, String e) {
        if (s == null || e == null) return false;
        return s.regionMatches(true, s.length()-e.length(), e, 0, e.length());
    }

    protected static boolean startsWithIgnoreCase(String s, String e) {
        if (s == null || e == null) return false;
        return s.regionMatches(true, 0, e, 0, e.length());
    }

    protected void setOptions(String options) { }



    /** Insert flags in a file to highlight the syntax of the language. */
    public void highlightSyntax(StringBuffer file) {
        flagComments(file, getCommentStarters(), getCommentEnders());
    }



    /** Test a line of code to see if it is countable.
     */
    public boolean isSignificant(String line) {
        return (line.trim().length() > 0); // count nonblank lines.
    }



    /** Display HTML describing the caveats of operation for this
     *  language filter.
     */
    public void service(InputStream in, OutputStream out, Map env)
        throws IOException
    {
        out.write(CAVEAT_HTML.getBytes(charset));
    }

    protected static String CAVEAT_HTML = resources.getString("Caveat_HTML");


    public String[][] getOptions() { return null; }



    protected void flagComments(StringBuffer file,
                                String[] commentStarters,
                                String[] commentEnders) {
        if (commentStarters == null || commentEnders == null) return;

        int begin, b, end, i, selectedStyle, pos = 0;
        String beginPattern, endPattern;
        while (true) {
            begin = Integer.MAX_VALUE;
            selectedStyle = -1;
            for (i = commentStarters.length;   i-- > 0; ) {
                b = StringUtils.indexOf(file, commentStarters[i], pos);
                if (b != -1 && b < begin) {
                    begin = b;
                    selectedStyle = i;
                }
            }

            // no comments found? we're done.
            if (selectedStyle == -1) break;

            // retrieve the strings that describe the comment style we found.
            beginPattern = commentStarters[selectedStyle];
            endPattern   = commentEnders  [selectedStyle];

            // search for the end of the comment.
            end = StringUtils.indexOf
                (file, endPattern, begin + beginPattern.length());
            if (end == -1)
                end = file.length();
            else
                end += endPattern.length();

            // insert comment indicators before and after the string.
            file.insert(end,   COMMENT_END_STR);
            file.insert(begin, COMMENT_START_STR);

            pos = end+2;
        }
        StringUtils.findAndReplace(file, "\n" + COMMENT_END,
                                   COMMENT_END + "\n");
    }

    protected String[] getCommentStarters() { return null; }
    protected String[] getCommentEnders()   { return null; }
    protected String[] getDefaultFilenameEndings() { return null; }

    protected String[] getFilenameEndings() {
        String settingName = "pspdiff.suffix." +
            getFilterName(this).toLowerCase();
        return buildArrayFromUserSetting
            (settingName, " \t", getDefaultFilenameEndings());
    }



    public static String getFilterName(Object filter) {
        if (filter instanceof NamedFilter) {
            return ((NamedFilter) filter).getFilterName();
        }

        String className = filter.getClass().getName();
        if (endsWithIgnoreCase(className, "filter"))
            className = className.substring(0, className.length() - 6);
        int pos = className.lastIndexOf('.');
        if (pos != -1) className = className.substring(pos+1);
        return className;
    }



    protected String[] buildArrayFromUserSetting(String setting,
                                                 String delimiters,
                                                 String[] extra) {
        setting = Settings.getVal(setting);
        if (setting == null || setting.length() == 0) return extra;

        StringTokenizer tok = new StringTokenizer(setting, delimiters);
        int count = tok.countTokens() + (extra == null ? 0 : extra.length);
        String[] result = new String[count];

        // copy user-specified tokens into the result array.
        count = 0;
        while (tok.hasMoreTokens())
            result[count++] = tok.nextToken();

        // copy options in the extra parameter into the result array
        if (extra != null)
            for (int i = 0;   i < extra.length;   i++)
                result[count++] = extra[i];

        return result;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

}