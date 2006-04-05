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

package net.sourceforge.processdash;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.tools.ant.BuildException;

/** Zip up all the files in a directory that came from SVN.
 */
public class SVNZip extends CVSZip {

    public void execute() throws BuildException {
        setIncludecvsdirs(false);
        this.shouldWriteFilesRecurse = false;
        super.execute();
    }

    protected List getVersionedFiles(File dir) throws IOException {
        String[] cmd = new String[] { "svn", "status", "-v" };
        Process p = Runtime.getRuntime().exec(cmd, null, dir);
        InputStream inStr = p.getInputStream();
        BufferedReader in = new BufferedReader(new InputStreamReader(inStr));

        List result = new LinkedList();
        String line;
        while ((line = in.readLine()) != null) {
            Matcher m = SVN_LINE_PATTERN.matcher(line);
            if (!m.matches())
                continue;
            String filename = m.group(1);
            result.add(filename);
        }

        return result;
    }

    private static final Pattern SVN_LINE_PATTERN = Pattern
            .compile("......\\s*\\d+\\s+\\d+\\s+\\S+\\s+(.*)");

}
