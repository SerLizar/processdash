// Copyright (C) 2006 Tuma Solutions, LLC
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

package net.sourceforge.processdash.net.cms;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import net.sourceforge.processdash.templates.TemplateLoader;

/** Implementation of a persistence service that can load page content from
 * files in the dashboard template search path.
 */
public class TemplatePersistenceService implements PersistenceService {

    public InputStream open(String filename) throws IOException {
        URL url = TemplateLoader.resolveURL("cms/" + filename);
        if (url != null)
            return url.openStream();

        url = TemplateLoader.resolveURL(filename);
        if (url != null)
            return url.openStream();

        return null;
    }

    public OutputStream save(String filename) throws IOException {
        return null;
    }

}
