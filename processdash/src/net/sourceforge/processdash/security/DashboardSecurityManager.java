// Copyright (C) 2007 Tuma Solutions, LLC
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

package net.sourceforge.processdash.security;

import java.security.AllPermission;
import java.security.Permission;


public class DashboardSecurityManager extends SecurityManager {

    // Efficiency-related overrides. For all File-based operations, the built-in
    // Java SecurityManager creates a FilePermission for the file.  This
    // triggers a step to resolve the canonical filename of the file, which
    // can take an extremely long time for some VPN/network drive combinations.
    // If the caller has been granted "All Permission" (the most common
    // scenario for dashboard code), these steps are unnecessary and can be
    // skipped.


    public void checkRead(String file, Object context) {
        if (hasAllPermission())
            return;
        super.checkRead(file, context);
    }

    public void checkRead(String file) {
        if (hasAllPermission())
            return;
        super.checkRead(file);
    }

    public void checkWrite(String file) {
        if (hasAllPermission())
            return;
        super.checkWrite(file);
    }

    public void checkDelete(String file) {
        if (hasAllPermission())
            return;
        super.checkDelete(file);
    }



    private static final Permission ALL_PERMISSION = new AllPermission();

    private boolean hasAllPermission() {
        try {
            checkPermission(ALL_PERMISSION);
            return true;
        } catch (SecurityException se) {
            return false;
        }
    }
}
