// Copyright (C) 2008-2009 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.bridge.client;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import net.sourceforge.processdash.tool.bridge.impl.DashboardInstanceStrategy;
import net.sourceforge.processdash.tool.bridge.impl.FileResourceCollectionStrategy;
import net.sourceforge.processdash.tool.bridge.impl.TeamDataDirStrategy;

public class WorkingDirectoryFactory {

    public static final int PURPOSE_DASHBOARD = 1;

    public static final int PURPOSE_WBS = 2;

    static final Logger logger = Logger.getLogger(WorkingDirectory.class
            .getName());

    private static final WorkingDirectoryFactory INSTANCE =
            new WorkingDirectoryFactory();

    public static WorkingDirectoryFactory getInstance() {
        return INSTANCE;
    }

    private WorkingDirectoryFactory() {}

    public WorkingDirectory get(String location, int purpose) {
        return get(purpose, location);
    }

    public WorkingDirectory get(int purpose, String... locations_) {
        List<String> locations = filterLocations(locations_);
        File fileLocation = findFirstFileLocation(locations);

        // look through each location until we find one that works.
        for (String loc : locations) {
            if (TeamServerSelector.isUrlFormat(loc)) {
                // test URL locations to see if a team server is actually
                // present
                URL u = TeamServerSelector.testServerURL(loc);
                if (u != null)
                    return get(fileLocation, loc, purpose);

            } else {
                // test "regular" locations to see if the directory exists
                File dir = new File(loc);
                if (dir.isDirectory())
                    return get(dir, purpose);
            }
        }

        // at this point, we believe that none of the locations were viable.
        // return *something* pointing to a broken location, so our client
        // will have a WorkingDirectory object to complain about to the user.
        if (fileLocation != null)
            return get(fileLocation, null, purpose);
        else
            return get(null, locations.get(locations.size() - 1), purpose);
    }

    private List<String> filterLocations(String[] locations) {
        List<String> result = new ArrayList<String>();

        // add all non-null entries from "locations" to the list
        if (locations != null) {
            for (String loc : locations)
                if (loc != null && loc.length() > 0)
                    result.add(loc);
        }

        // no locations provided? use default of "current working dir"
        if (result.isEmpty())
            result.add(System.getProperty("user.dir"));

        return result;
    }

    private File findFirstFileLocation(List<String> locations) {
        for (String loc : locations) {
            if (!TeamServerSelector.isUrlFormat(loc))
                return new File(loc);
        }
        return null;
    }

    public WorkingDirectory get(File targetDirectory, int purpose) {
        URL serverURL = TeamServerSelector.getServerURL(targetDirectory);
        String serverUrlStr = (serverURL == null ? null : serverURL.toString());
        return get(targetDirectory, serverUrlStr, purpose);
    }

    public WorkingDirectory get(URL url, int purpose) {
        return get(null, url.toString(), purpose);
    }

    private WorkingDirectory get(File targetDirectory, String remoteURL,
            int purpose) {

        FileResourceCollectionStrategy strategy = getStrategy(purpose);

        if (targetDirectory != null) {
            try {
                targetDirectory = targetDirectory.getCanonicalFile();
            } catch (IOException e) {
                targetDirectory = targetDirectory.getAbsoluteFile();
            }
        }

        if (remoteURL != null) {
            logger.info("Using bridged working directory via URL " + remoteURL);
            return new BridgedWorkingDirectory(targetDirectory, remoteURL,
                    strategy, DirectoryPreferences.getMasterWorkingDirectory());

        } else if (targetDirectory != null) {
            logger.info("Using local working directory "
                    + targetDirectory.getPath());
            return new LocalWorkingDirectory(targetDirectory, strategy,
                    DirectoryPreferences.getMasterWorkingDirectory());

        } else {
            throw new NullPointerException();
        }
    }

    private FileResourceCollectionStrategy getStrategy(int purpose) {
        if (purpose == PURPOSE_DASHBOARD)
            return DashboardInstanceStrategy.INSTANCE;
        else if (purpose == PURPOSE_WBS)
            return TeamDataDirStrategy.INSTANCE;
        else
            throw new IllegalArgumentException(
                    "Unrecognized working directory strategy");
    }

}