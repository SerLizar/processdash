// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2005 Software Process Dashboard Initiative
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
//
// The author(s) may be contacted at:
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC: processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.tool.export.ui.wizard;

import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.text.Document;

public class FileChooserComponent extends JPanel implements ActionListener {

    JTextField textField;

    JButton chooseButton;

    JFileChooser fileChooser;

    String suffix;

    public FileChooserComponent(String startingDirectoryName) {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        textField = new JTextField(startingDirectoryName);
        add(textField);

        add(Box.createHorizontalStrut(5));

        String browseButtonLabel = Wizard.resources.getString("Browse_Button");
        chooseButton = new JButton(browseButtonLabel);
        chooseButton.setMargin(new Insets(1, 5, 1, 5));
        chooseButton.addActionListener(this);
        add(chooseButton);
    }

    public Document getDocument() {
        return textField.getDocument();
    }

    public String getSelectedFile() {
        return textField.getText();
    }

    protected String getSuffix() {
        return suffix;
    }

    protected void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public void actionPerformed(ActionEvent e) {
        if (fileChooser == null)
            fileChooser = createFileChooser();
        int returnVal = fileChooser.showDialog(this, getApproveButtonLabel());
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File result = fileChooser.getSelectedFile();
            if (result != null) {
                String path = result.getPath();
                if (path != null && suffix != null
                        && !path.toLowerCase().endsWith(suffix.toLowerCase()))
                    path = path + suffix;
                textField.setText(path);
            }
        }
    }

    protected String getApproveButtonLabel() {
        return Wizard.resources.getString("OK");
    }

    protected JFileChooser createFileChooser() {
        String filePath = textField.getText();

        JFileChooser result;
        if (filePath == null || filePath.trim().length() == 0)
            result = new JFileChooser();
        else
            result = new JFileChooser(filePath);

        return result;
    }

}
