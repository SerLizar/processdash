// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2005 Software Process Dashboard Initiative
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

package net.sourceforge.processdash.log.time;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Date;

public class MutableTimeLogEntryVO extends TimeLogEntryVO implements MutableTimeLogEntry {

    PropertyChangeSupport propertyChangeSupport = null;

    public MutableTimeLogEntryVO(TimeLogEntry tle) {
        super(tle);
    }

    public MutableTimeLogEntryVO(long id, String path, Date startTime,
            long elapsedTime, long interruptTime, String comment, int flag) {
        super(id, path, startTime, elapsedTime, interruptTime, comment, flag);
    }

    public void setPath(String path) {
        String oldVal = this.path;
        this.path = path;
        firePropertyChange("path", oldVal, path);
    }

    public void setStartTime(Date startTime) {
        Date oldVal = this.startTime;
        this.startTime = startTime;
        firePropertyChange("startTime", oldVal, startTime);
    }

    public void setElapsedTime(long elapsedTime) {
        long oldVal = this.elapsedTime;
        this.elapsedTime = elapsedTime;
        firePropertyChange("elapsedTime", oldVal, elapsedTime);
    }

    public void setInterruptTime(long interruptTime) {
        long oldVal = this.interruptTime;
        this.interruptTime = interruptTime;
        firePropertyChange("interruptTime", oldVal, interruptTime);
    }

    public void setComment(String comment) {
        String oldComment = this.comment;
        this.comment = comment;
        firePropertyChange("comment", oldComment, comment);
    }

    public void setChangeFlag(int flag) {
        int oldVal = this.flag;
        this.flag = flag;
        firePropertyChange("changeFlag", oldVal, flag);
    }

    public synchronized void addPropertyChangeListener(PropertyChangeListener l) {
        if (propertyChangeSupport == null)
            propertyChangeSupport = new PropertyChangeSupport(this);
        propertyChangeSupport.addPropertyChangeListener(l);
    }

    public synchronized void removePropertyChangeListener(PropertyChangeListener l) {
        if (propertyChangeSupport != null) {
            propertyChangeSupport.removePropertyChangeListener(l);
        }
    }

    protected void firePropertyChange(String propName, Object oldVal, Object newVal) {
        if (propertyChangeSupport != null)
            propertyChangeSupport.firePropertyChange(propName, oldVal, newVal);
    }

    protected void firePropertyChange(String propName, long oldVal, long newVal) {
        if (propertyChangeSupport != null)
            propertyChangeSupport.firePropertyChange(propName, (int) oldVal, (int) newVal);
    }

}
