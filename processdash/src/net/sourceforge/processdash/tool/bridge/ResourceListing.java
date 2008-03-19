// Copyright (C) 2008 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.bridge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResourceListing implements ResourceCollectionInfo {

    List<String> resourceNames;

    Map<String, Long> modTimes;

    Map<String, Long> checksums;

    public ResourceListing() {
        resourceNames = new ArrayList<String>();
        modTimes = new HashMap<String, Long>();
        checksums = new HashMap<String, Long>();
    }

    public ResourceListing(ResourceCollection collection) {
        this(collection, collection.listResourceNames());
    }

    public ResourceListing(ResourceCollection collection, ResourceFilter f) {
        this(collection, f.filterCollection(collection));
    }

    protected ResourceListing(ResourceCollection collection,
            List<String> names) {
        this();
        for (String name : names) {
            long lastMod = collection.getLastModified(name);
            Long checksum = collection.getChecksum(name);
            addResource(name, lastMod, checksum);
        }
    }

    public Long getChecksum(String resourceName) {
        return checksums.get(resourceName);
    }

    public long getLastModified(String resourceName) {
        return modTimes.get(resourceName);
    }

    public List<String> listResourceNames() {
        return resourceNames;
    }

    public void addResource(String name, Long mod, Long checksum) {
        resourceNames.add(name);
        modTimes.put(name, mod);
        checksums.put(name, checksum);
    }

    public void setModTime(String name, long mod) {
        modTimes.put(name, mod);
    }

    public void setChecksum(String name, Long checksum) {
        checksums.put(name, checksum);
    }

}
