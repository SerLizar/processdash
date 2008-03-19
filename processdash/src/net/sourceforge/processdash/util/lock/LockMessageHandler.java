// Copyright (C) 2008 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public License
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

package net.sourceforge.processdash.util.lock;


/**
 * Interface for indicating interest in messages sent by other processes
 * desiring this lock
 */
public interface LockMessageHandler {

    /** A message that is sent to the listener of a lock, if the lock was
     * lost and could not be reclaimed. */
    public static final String LOCK_LOST_MESSAGE = "lockWasLost";

    /**
     * Called when some other process wanted to obtain a lock, and could not
     * because we own it.
     * 
     * @param message
     *                a message from the other process (for example, describing
     *                what they wanted to do with the lock). This listener can
     *                potentially look at the message and perform some action on
     *                behalf of the other process.
     * @return our response to the message in question; can be null. The reponse
     *         must not include the carriage return or newline characters.
     * @throws Exception
     *                 if we cannot understand the message or respond to it for
     *                 some reason.
     */
    public String handleMessage(LockMessage e) throws Exception;

}
