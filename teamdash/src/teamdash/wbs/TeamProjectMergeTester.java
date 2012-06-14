// Copyright (C) 2012 Tuma Solutions, LLC
// Team Functionality Add-ons for the Process Dashboard
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

package teamdash.wbs;

import java.io.File;

import teamdash.wbs.TeamProjectMergeCoordinator.QuickTeamProject;

public class TeamProjectMergeTester {

    public static void main(String[] args) {
        File mainDir, baseDir, incomingDir;
        if (args.length == 3) {
            baseDir = new File(args[0]);
            mainDir = new File(args[1]);
            incomingDir = new File(args[2]);
        } else {
            String rootPath = args[0];
            baseDir = new File(rootPath, "base");
            mainDir = new File(rootPath, "main");
            incomingDir = new File(rootPath, "incoming");
        }
        TeamProject base = new QuickTeamProject(baseDir, "base");
        TeamProject main = new QuickTeamProject(mainDir, "main");
        TeamProject incoming = new QuickTeamProject(incomingDir, "incoming");

        try {
            TeamProjectMerger merger = new TeamProjectMerger(base, main,
                    incoming);
            merger.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
