// Copyright (C) 2002-2010 Tuma Solutions, LLC
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

package net.sourceforge.processdash.team.mcf.editor;

import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.AbstractAction;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.templates.TemplateLoader;

public class CustomProcessEditorAction extends AbstractAction {

    private DashboardContext context;

    public CustomProcessEditorAction() {
        super("Team Metrics Framework Editor");
    }

    public void setDashboardContext(DashboardContext context) {
        this.context = context;
    }

    public void actionPerformed(ActionEvent e) {
        CustomProcessEditor editor = new CustomProcessEditor(null, context
                .getWebServer());

        File defaultTemplatesDir = TemplateLoader.getDefaultTemplatesDir();
        if (defaultTemplatesDir != null)
            defaultTemplatesDir.mkdir();
        editor.setDefaultDirectory(defaultTemplatesDir);
    }

}
