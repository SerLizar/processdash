// Copyright (C) 2001-2011 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.diff.engine;

import static net.sourceforge.processdash.tool.diff.engine.AccountingType.Added;
import static net.sourceforge.processdash.tool.diff.engine.AccountingType.Base;
import static net.sourceforge.processdash.tool.diff.engine.AccountingType.Deleted;
import static net.sourceforge.processdash.tool.diff.engine.AccountingType.Modified;
import static net.sourceforge.processdash.tool.diff.engine.AccountingType.Total;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import net.sourceforge.processdash.tool.diff.LanguageFilter;
import net.sourceforge.processdash.util.StringUtils;

public class LocDiffUtils {

    static final char COMMENT_START = LanguageFilter.COMMENT_START;
    static final String COMMENT_START_STR = String.valueOf(COMMENT_START);

    static final char COMMENT_END = LanguageFilter.COMMENT_END;
    static final String COMMENT_END_STR = String.valueOf(COMMENT_END);

    static final String LINE_ENDING = "\n";



    /**
     * Read content from an input stream; mark the comments in the resulting
     * text, and return an array of the resulting lines in the file.
     * 
     * @param in
     *            the InputStream containing the file content.
     * @param filter
     *            the language filter to use for highlighting comments
     * @param cs
     *            the character encoding to use when reading the input stream
     * @return an array of WhitespaceCompareString objects representing the
     *         lines in the file.
     * @throws IOException
     *             if an error is encountered while reading the file.
     */
    public static WhitespaceCompareString[] getFileContent(InputStream in,
            LanguageFilter filter, Charset cs) throws IOException {
        if (in == null)
            return new WhitespaceCompareString[0];

        BufferedReader r = new BufferedReader(new InputStreamReader(in, cs));
        StringBuffer buf = new StringBuffer();
        String line;
        while ((line = r.readLine()) != null)
            buf.append(line).append(LINE_ENDING);

        filter.highlightSyntax(buf);

        List<String> lines = breakLines(buf.toString());

        WhitespaceCompareString[] result =
            new WhitespaceCompareString[lines.size()];
        int idx = 0;
        for (Iterator i = lines.iterator(); i.hasNext();) {
            result[idx++] = new WhitespaceCompareString((String) i.next());
        }

        return result;
    }

    protected static List<String> breakLines(String s) {
        StringTokenizer tok = new StringTokenizer(s, LINE_ENDING, true);
        String line = "", token;
        List<String> result = new LinkedList<String>();
        boolean inComment = false;

        while (tok.hasMoreTokens()) {
            token = tok.nextToken();
            if (token.equals(LINE_ENDING)) {
                result.add(line);
                line = "";
            } else {
                line = token;
                if (inComment)
                    line = COMMENT_START_STR + line;

                int commentStart = line.lastIndexOf(COMMENT_START);
                int commentEnd = line.lastIndexOf(COMMENT_END);
                if (commentStart < commentEnd)
                    inComment = false;
                if (commentStart > commentEnd)
                    inComment = true;

                if (inComment)
                    line = line + COMMENT_END_STR;
            }
        }
        if (line.length() > 0)
            result.add(line);

        return result;
    }



    /**
     * Determine the appropriate change type for a file.
     * 
     * @param macroType
     *            the apparent type of the file, selected at a high level by
     *            checking to see whether the file version history began with an
     *            insertion or ended with a deletion
     * @param locCounts
     *            the number of lines of various types that were counted during
     *            a diff analysis
     * @return the most appropriate change type to use for the file.
     */
    public static AccountingType getFileChangeType(AccountingType macroType,
            int[] locCounts) {

        if (macroType == Added && isZero(locCounts, Base))
            return Added;

        else if (macroType == Deleted && isZero(locCounts, Total))
            return Deleted;

        else if (isZero(locCounts, Deleted, Modified, Added))
            return Base;

        else
            return Modified;
    }

    protected static boolean isZero(int[] locCounts, AccountingType... types) {
        for (AccountingType type : types)
            if (locCounts[type.ordinal()] != 0)
                return false;
        return true;
    }



    /** Count the significant lines of code in an array. */
    public static int countLines(Object[] lines, LanguageFilter filter) {
        return countLines(Arrays.asList(lines), filter);
    }

    /** Count the significant lines of code in a portion of an array. */
    public static int countLines(Object[] lines, int pos, int len,
            LanguageFilter filter) {
        return countLines(Arrays.asList(lines).subList(pos, pos + len), filter);
    }

    /** Count the significant lines of code in a list. */
    public static int countLines(List lines, LanguageFilter filter) {
        int result = 0;
        if (lines != null) {
            for (Object oneLine : lines) {
                String text = oneLine.toString();
                text = stripComments(text);
                if (filter.isSignificant(text))
                    result++;
            }
        }
        return result;
    }



    /**
     * Remove comments from a list of lines, and convert them to
     * {@link WhitespaceCompareString} objects.
     */
    public static WhitespaceCompareString[] stripComments(List lines) {
        WhitespaceCompareString[] result = new WhitespaceCompareString[lines
                .size()];
        for (int i = 0; i < result.length; i++) {
            String line = lines.get(i).toString();
            String noComments = stripComments(line);
            result[i] = new WhitespaceCompareString(noComments);
        }
        return result;
    }

    /** Remove comments from a piece of code */
    public static String stripComments(String str) {
        if (str.indexOf(COMMENT_START) == -1)
            // efficiently handle degenerate case: if there are no
            // comments in the string, just return it without change.
            return str;

        StringBuffer buf = new StringBuffer(str);
        stripComments(buf);
        return buf.toString();
    }

    /** Remove comments from a piece of code */
    public static void stripComments(StringBuffer buf) {
        int beg, end;
        while ((beg = StringUtils.indexOf(buf, COMMENT_START_STR)) != -1) {
            end = StringUtils.indexOf(buf, COMMENT_END_STR, beg);
            if (end == -1)
                return;

            buf.delete(beg, end + 1);
        }
    }



    /**
     * Look through an option string for the value associated with a particular
     * option.
     * 
     * @return if the option string contains either "[tag] [value]" or
     *         "[tag]=[value]", that value will be returned. Otherwise,
     *         returns null.
     */
    public static String getOption(String options, String tag) {
        if (options == null)
            return null;

        int tagPos = options.lastIndexOf(tag);
        if (tagPos == -1)
            return null;

        int valPos = tagPos + tag.length() + 1;
        if (valPos >= options.length())
            return null;

        int spacePos = options.indexOf(' ', valPos);
        if (spacePos == -1)
            return options.substring(valPos);
        else
            return options.substring(valPos, spacePos);
    }



    /**
     * Create a line of HTML code that can be used to display a particular line
     * of code.
     * 
     * @param line
     *            a single line of code to display
     * @param tabWidth
     *            the number of space characters that are equivalent to a single
     *            tab character
     * @return a line of HTML code
     */
    public static String fixupLineForHtml(String line, int tabWidth) {
        StringBuffer buf = new StringBuffer(line);

        // convert tabs to spaces.
        tabsToSpaces(buf, tabWidth);

        // escape HTML entities.
        StringUtils.findAndReplace(buf, "&", "&amp;");
        StringUtils.findAndReplace(buf, "<", "&lt;");
        StringUtils.findAndReplace(buf, ">", "&gt;");
        StringUtils.findAndReplace(buf, "\"", "&quot;");

        // highlight comments.
        StringUtils.findAndReplace(buf, COMMENT_START_STR,
            "<span class='comment'>");
        StringUtils.findAndReplace(buf, COMMENT_END_STR, "</span>");

        return buf.toString();
    }


    /**
     * Convert tabs to spaces in a single line of text.
     * 
     * @param line
     *            the line of text to convert, which could potentially contain
     *            the {@link LanguageFilter#COMMENT_START} or
     *            {@link LanguageFilter#COMMENT_END} marking characters
     * @param tabWidth
     *            the number of spaces that are equivalent to a single tab
     *            character
     */
    public static void tabsToSpaces(StringBuffer line, int tabWidth) {
        int tabPos = StringUtils.indexOf(line, "\t"), spacesNeeded;
        while (tabPos != -1) {
            spacesNeeded = tabWidth
                    - (tabPos - countInvisibleChars(line, tabPos)) % tabWidth;
            line.replace(tabPos, tabPos + 1, "        ".substring(0,
                spacesNeeded));
            tabPos = StringUtils.indexOf(line, "\t", tabPos);
        }
    }

    private static int countInvisibleChars(StringBuffer s, int endPos) {
        int result = 0;
        while (endPos-- > 0)
            switch (s.charAt(endPos)) {
            case COMMENT_START:
            case COMMENT_END:
                result++;
            }
        return result;
    }


    /**
     * Sort a list of files by filename
     *
     * @param files the list of files to sort
     * @param comp the comparator that should be used to compare the filenames
     */
    public static <T extends FileToAnalyze> void sortFiles(List<T> files,
            FilenameComparator comp) {
        List<SortableFileToAnalyze> sortables = new ArrayList(files.size());
        for (FileToAnalyze file : files)
            sortables.add(new SortableFileToAnalyze(file));
        Collections.sort(sortables, comp);
        for (int i = files.size(); i-- > 0;)
            files.set(i, (T) sortables.get(i).getFileToAnalyze());
    }

    private static class SortableFileToAnalyze extends FilenameComparable {
        private FileToAnalyze file;

        public SortableFileToAnalyze(FileToAnalyze file) {
            super(file.getFilename());
            this.file = file;
        }

        public FileToAnalyze getFileToAnalyze() { return file; }
    }

}
