// Copyright (C) 2008 Tuma Solutions, LLC
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
import java.util.logging.Logger;

/**
 * An import directory that dynamically recomputes the location of source data.
 * 
 * There are three primary ways of accessing imported data:
 * 
 * <ol>
 * <li>Accessing the files in the target directory through the filesystem
 *     (using a {@link LocalImportDirectory})</li>
 * <li>Accessing the files through a team server (using a
 *     {@link BridgedImportDirectory})</li>
 * <li>Accessing the files on the local hard drive that were left behind by a
 *     previously operational bridged import directory (this access is provided
 *     by a {@link CachedImportDirectory})</li>
 * </ol>
 * 
 * This class attempts to switch dynamically between these three options in
 * response to changing network connectivity.
 */
public class DynamicImportDirectory implements ImportDirectory {

    /** The list of locations we can use to search for import data */
    private String[] locations;

    /** The currently active ImportDirectory <b>(never null)</b> */
    private ImportDirectory delegate;


    private static final Logger logger = Logger
            .getLogger(DynamicImportDirectory.class.getName());


    public DynamicImportDirectory(String[] locations) throws IOException {
        this.locations = locations;
        maybeUpdateDelegate();
        if (delegate == null)
            throw new IOException();
    }

    public String getDescription() {
        return delegate.getDescription();
    }

    public File getDirectory() {
        return delegate.getDirectory();
    }

    public String getRemoteLocation() {
        return delegate.getRemoteLocation();
    }

    public void update() throws IOException {
        maybeUpdateDelegate();
        delegate.update();
    }

    private void maybeUpdateDelegate() {
        if (isUpdateNeeded()) {
            ImportDirectory newDelegate = ImportDirectoryFactory.getInstance()
                    .getImpl(locations);

            if (newDelegate != null && newDelegate != delegate) {
                this.delegate = newDelegate;

                String type = delegate.getClass().getSimpleName();
                String path = delegate.getDirectory().getPath();
                logger.fine("Using " + type + " " + path);
            }
        }
    }

    private boolean isUpdateNeeded() {
        // if we have no delegate, we need to update!
        if (delegate == null)
            return true;

        // Our main preference is a BridgedImportDirectory. If we already
        // have one, stick with it. Note: the implication of this decision
        // is that we will never switch from a bridged directory back to a
        // local directory. But the need to do that is rare: it would only
        // be useful if the Team Server shut down while we were running.
        if (delegate instanceof BridgedImportDirectory)
            return false;

        // If we're using cached files on our hard drive, we always want to
        // see if a better option is available.
        if (delegate instanceof CachedImportDirectory)
            return true;

        // If we're using a LocalImportDirectory with a nonexistent target,
        // we'd prefer to update the delegate and get something useful instead.
        // But in the most common case (when no Team Server is in use),
        // rechecking can't have any useful effect. So we only consider updating
        // if (a) there is more than one location in our "locations" list,
        // leading to the possibility that a different location might better,
        // or (b) a default team server is in effect, leading to the
        // possibility that we might be able to access the location through
        // an implicitly constructed URL. Note that at this time, we don't
        // consider the possibility of transitioning to bridged mode when
        // we have a valid local directory with no prior knowledge of Team
        // Server support. That is a rare transition, and is better handled
        // elsewhere as a configuration change, rather than being checked every
        // time the ImportDirectory.update() is called.
        if (delegate instanceof LocalImportDirectory) {
            if (locations.length == 1
                    && !TeamServerSelector.isDefaultTeamServerConfigured())
                return false;
            else
                return !delegate.getDirectory().isDirectory();
        }

        return false;
    }

}
