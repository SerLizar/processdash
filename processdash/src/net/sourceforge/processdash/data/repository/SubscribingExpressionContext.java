// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2006 Software Process Dashboard Initiative
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

package net.sourceforge.processdash.data.repository;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.data.compiler.ExpressionContext;

/** This context looks up data on behalf of a DataListener.  Along the way,
 * it registers that DataListener for all items that were looked up.
 */
public class SubscribingExpressionContext implements ExpressionContext {

    private DataRepository data;

    private String prefix;

    private String listenerName;

    private DataListener listener;

    private Set currentSubscriptions;

    private Set namesSeen;

    public SubscribingExpressionContext(DataRepository data, String prefix,
            DataListener listener, String listenerName, Set currentSubscriptions) {
        this.data = data;
        this.prefix = prefix;
        this.listener = listener;
        this.listenerName = listenerName;
        this.currentSubscriptions = currentSubscriptions;
        this.namesSeen = new HashSet();
    }

    public SimpleData get(String dataName) {
        if (PREFIXVAR_NAME.equals(dataName))
            return StringData.create(prefix);

        dataName = resolveName(dataName);
        namesSeen.add(dataName);

        if (!currentSubscriptions.contains(dataName)) {
            data.addActiveDataListener(dataName, listener, listenerName, false);
            currentSubscriptions.add(dataName);
        }

        return data.getSimpleValue(dataName);
    }

    public String resolveName(String dataName) {
        return DataRepository.createDataName(prefix, dataName);
    }

    public void removeOldSubscriptions() {
        synchronized (currentSubscriptions) {
            Set s = new HashSet(currentSubscriptions);
            s.removeAll(namesSeen);
            for (Iterator i = s.iterator(); i.hasNext();) {
                String dataName = (String) i.next();
                data.removeDataListener(dataName, listener);
                currentSubscriptions.remove(dataName);
            }
        }
    }

}
