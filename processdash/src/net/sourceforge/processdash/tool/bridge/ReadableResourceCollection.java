// Copyright (C) 2013 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.bridge;

import java.io.IOException;
import java.io.InputStream;

/**
 * Inteface for a resource collection which can provide access to the contents
 * of the resources it contains.
 * 
 * @since 1.15.0.3
 */
public interface ReadableResourceCollection extends ResourceCollectionInfo {

    /**
     * Return an input stream that can be used to read the contents of a given
     * resource.
     * 
     * @param resourceName
     *                the name of a resource
     * @return an <code>InputStream</code> for reading the contents of the
     *         named resource
     * @throws IOException
     *                 if the named resource does not exist, can not be read, or
     *                 if any other IO error occurs when reading the resource
     */
    public InputStream getInputStream(String resourceName) throws IOException;

}
