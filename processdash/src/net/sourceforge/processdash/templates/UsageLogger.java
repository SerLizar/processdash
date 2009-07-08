// Copyright (C) 2009 Tuma Solutions, LLC
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

package net.sourceforge.processdash.templates;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.util.StringUtils;

public class UsageLogger implements Runnable {

    private static final String FILE_SETTING = "usageLogger.fileList";

    public void run() {
        String logFileList = Settings.getFile(FILE_SETTING);
        if (!StringUtils.hasValue(logFileList))
            return;

        String[] filenames = logFileList.split("!!!");
        for (String filename : filenames)
            logToFile(new File(filename));
    }

    private void logToFile(File file) {
        try {
            FileWriter out = new FileWriter(file);
            out.write(Long.toString(System.currentTimeMillis()));
            out.close();
        } catch (IOException e) {
        }
    }

}
