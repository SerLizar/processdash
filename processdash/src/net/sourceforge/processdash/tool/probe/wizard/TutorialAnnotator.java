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

package net.sourceforge.processdash.tool.probe.wizard;

import java.util.Comparator;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class TutorialAnnotator {

    private TreeMap annotations;

    public TutorialAnnotator() {
        annotations = new TreeMap(SORT_ORDER);
    }

    public void addAnnotation(String text, String replacement) {
        annotations.put(text, new Annotation(text, replacement));
    }

    public String markup(String text) {
        Iterator i = annotations.values().iterator();
        while (i.hasNext()) {
            Annotation a = (Annotation) i.next();
            text = a.markup(text);
        }
        return text;
    }

    private static final class Annotation {
        private Pattern pattern1;
        private Pattern pattern2;
        private String replacement;

        public Annotation(String text, String replacement) {
            this.pattern1 = Pattern.compile
            ("\\b\\Q" + text + "\\E\\b", Pattern.UNICODE_CASE);
            this.pattern2 = Pattern.compile
            ("\\b\\Q" + text + "\\E ", Pattern.UNICODE_CASE);
            this.replacement = replacement;
        }
        public String markup(String text) {
            Matcher m = pattern1.matcher(text);
            text = m.replaceAll(replacement);
            m = pattern2.matcher(text);
            text = m.replaceAll(replacement+" ");
            return text;
        }
    }

    private static final class SortOrder implements Comparator {
        public int compare(Object o1, Object o2) {
            return String.CASE_INSENSITIVE_ORDER.compare((String) o2, (String) o1);
        }
    }
    private static final Comparator SORT_ORDER = new SortOrder();
}
