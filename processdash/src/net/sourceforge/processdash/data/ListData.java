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

package net.sourceforge.processdash.data;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

public class ListData implements SimpleData {

    public static final String PREFERRED_DELIMITERS = ",;|\n";

    private Vector list = new Vector();
    private String stringVersion = null;
    private boolean immutable = false;

    public ListData() {}
    public ListData(ListData l) {
        this.list = (Vector) l.list.clone();
    }

    public ListData(String l) {
        if (l == null) return;

        int delim = -1;
        if (l.length() > 1) {
            char first = l.charAt(0);
            char last = l.charAt(l.length() - 1);
            if (first == last && !Character.isLetterOrDigit(first))
                delim = first;
        }

        if (delim == -1)
            add(l);
        else {
            l = l.substring(1);
            int pos;
            while (l.length() > 0) {
                pos = l.indexOf(delim);
                add(l.substring(0, pos));
                l = l.substring(pos+1);
            }
        }
    }

    // If you call either of the following methods, it is up to you to
    // call DataRepository.putValue() to notify the data repository
    // that the value has changed.
    public synchronized void clear() {
        if (immutable) throw new IllegalStateException();
        list.removeAllElements(); stringVersion = null; }
    public synchronized void insert(Object o, int pos) {
        if (immutable) throw new IllegalStateException();
        list.insertElementAt(o, pos); stringVersion = null; }
    public synchronized void add(Object o) {
        if (immutable) throw new IllegalStateException();
        list.addElement(o); stringVersion = null; }
    public synchronized void addAll(Object o) {
        if (immutable) throw new IllegalStateException();
        if (o == null) return;
        if (o instanceof SaveableData && !(o instanceof SimpleData))
            o = ((SaveableData) o).getSimpleValue();
        if (o instanceof StringData)
            o = ((StringData) o).asList();
        if (o instanceof ListData) {
            ListData l = (ListData) o;
            for (int i=0;  i<l.size();  i++) add(l.get(i));
        } else
            add(o);
    }
    public boolean contains(Object o) {
        return list.contains(o); }
    public synchronized boolean remove(Object o) {
        if (immutable) throw new IllegalStateException();
        // We cannot use the Vector.remove() method because it was
        // introduced in JDK 1.2, and this class must run inside IE
        // which only supports 1.1
        if (o == null) return false;
        for (int i=0;  i<list.size();  i++)
            if (o.equals(list.elementAt(i))) {
                list.removeElementAt(i);
                stringVersion = null;
                return true;
            }
        return false;
    }

    public synchronized boolean setAdd(Object o) {
        if (immutable) throw new IllegalStateException();
        if (list.contains(o)) return false;
        add(o); return true;
    }

    public int size()           { return list.size();          }
    public Object get(int item) { return list.elementAt(item); }

    // The following methods implement the SaveableData interface.

                                // editable lists are not yet supported.
    public boolean isEditable() { return false; }
    public void setEditable(boolean e) {}
                                // undefined lists are not yet supported.
    public boolean isDefined() { return true; }
    public void setDefined(boolean d) {}

    public String saveString() {
        return StringData.create(format()).saveString();
    }

    public SimpleData getSimpleValue() {
        if (immutable)
            return this;
        else
            return new ListData(this);
    }

    public void dispose() {
        // do nothing!  MANY ListData items are shared by many data
        // elements.  just because one element no longer needs it, we
        // don't have the right to unilaterally destroy the contents.
    }

    public void setImmutable() { immutable = true; }

    public List asList() {
        return Collections.unmodifiableList(list);
    }
    public void sortContents(Comparator c) {
        if (immutable) throw new IllegalStateException();
        Collections.sort(list, c);
    }

    // The following methods implement the SimpleData interface.

    public String toString() { return format(); }
    private static final char DEFAULT_DELIM = '\u0002';
    //private static final char DEFAULT_DELIM = ';';
    public synchronized String format() {
        if (stringVersion != null) return stringVersion;

        if (list.size() == 0)
            stringVersion = "";
        else {
            StringBuffer result = new StringBuffer();
            result.append(DEFAULT_DELIM);
            for (int i=0;  i < list.size();  i++)
                result.append(list.elementAt(i)).append(DEFAULT_DELIM);
            stringVersion = result.toString();
        }

        return stringVersion;
    }

    public SimpleData parse(String val) {
        return (val == null || val.length() == 0) ? null : new ListData(val);
    }


    public boolean equals(SimpleData val) {
        return (val != null && format().equals(val.format()));
    }
                                // ordering for lists doesn't make sense.
    public boolean lessThan(SimpleData val) { return false; }
    public boolean greaterThan(SimpleData val) { return false; }

    public boolean test() { return !list.isEmpty(); }

    // since editable lists are not supported, there is no need to
    // create a different version of this list.
    public SaveableData getEditable(boolean editable) { return this; }
}
