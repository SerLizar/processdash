// Copyright (C) 2001-2011 Tuma Solutions, LLC
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


package net.sourceforge.processdash.process;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.DoubleData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.ui.OptionList;

public class DefectTypeStandard extends OptionList {

    private String defectTypeName = null;

    private DefectTypeStandard(String s) { super(s); }

    public String getName() { return defectTypeName; }

    private static final String DATA_PREFIX = "/Defect Type Standard/";
    private static final String PRIORITY_PREFIX = "/Defect Type Priority/";
    private static final String SETTING_DATA_NAME = "Defect Type Standard";
    private static final String CONTENTS_DATA_NAME =
        "Defect Type Standard Contents";
    private static final String TITLE_DELIMITER = ":::";

    /** Get the defect type standard for the named project/task */
    public static DefectTypeStandard get(String path, DataRepository r) {
        data = r;

        // get the defect type standard for this project.
        if (path == null) path = "";

        while (path != null) {
            // first, check for a setting pointing to a defined standard.
            String settingDataName = DataRepository.createDataName(path,
                    SETTING_DATA_NAME);
            SimpleData d = data.getSimpleValue(settingDataName);
            if (d != null && d.test()) {
                String defectTypeName = d.format();
                if (defectTypeName.trim().length() > 0)
                    return getByName(defectTypeName, data);
            }

            // next, check for an ad-hoc standard, specified directly for this
            // particular path
            String contentDataName = DataRepository.createDataName(path,
                    CONTENTS_DATA_NAME);
            d = data.getSimpleValue(contentDataName);
            if (d != null && d.test())
                return getFromContents(d.format());

            // no luck - move up the hierarchy
            path = DataRepository.chopPath(path);
        }

        // No setting was found in the hierarchy for this project.  Look in
        // the user settings for a global default.
        String defectTypeName = Settings.getVal("defectTypeStandard");

        // If the user didn't have a global default set, find the installed
        // defect type standard with the highest preference/priority.
        if (defectTypeName == null)
            defectTypeName = getNameOfMostPreferredStandard();

        return getByName(defectTypeName, r);
    }

    /** Construct a standard on-the-fly, from custom-supplied contents */
    private static DefectTypeStandard getFromContents(String contents) {
        String defectTypeName = null;
        int colonPos = contents.indexOf(TITLE_DELIMITER);
        if (colonPos != -1) {
            defectTypeName = contents.substring(0, colonPos);
            contents = contents.substring(colonPos + TITLE_DELIMITER.length());
        }

        DefectTypeStandard result = new DefectTypeStandard(contents);
        result.defectTypeName = defectTypeName;
        return result;
    }

    private static String getNameOfMostPreferredStandard() {
        if (mostPreferredName != null)
            return mostPreferredName;

        String bestName = DEFAULT_NAME;
        double bestPriority = -1;
        for (String oneName : getDefinedStandards(data)) {
            SimpleData sd = data.getSimpleValue(PRIORITY_PREFIX + oneName);
            if (sd instanceof DoubleData) {
                double onePriority = ((DoubleData) sd).getDouble();
                if (onePriority > bestPriority) {
                    bestName = oneName;
                    bestPriority = onePriority;
                }
            }
        }

        mostPreferredName = bestName;
        return mostPreferredName;
    }

    /** Get the named defect type standard. */
    public static DefectTypeStandard getByName(String defectTypeName,
                                               DataRepository r)
    {
        data = r;

        DefectTypeStandard result =
            (DefectTypeStandard) cache.get(defectTypeName);
        if (result == null) {
            String defectTypes = null;
            try {
                defectTypes = data.getSimpleValue
                    (DATA_PREFIX + defectTypeName).format();
            } catch (NullPointerException npe) {
                defectTypes = Settings.getVal("defectType." + defectTypeName);
                if (defectTypes == null) {
                    System.err.println("Could not find a defect type called " +
                                       defectTypeName);
                    defectTypes = DEFAULT_DEFECT_TYPES;
                }
            }

            // create a JComboBox with those defect types.
            result = new DefectTypeStandard(defectTypes);
            result.defectTypeName = defectTypeName;
            cache.put(defectTypeName, result);
        }
        return result;
    }

    public static String[] getDefinedStandards(DataRepository r) {
        LinkedList names = new LinkedList();
        data = r;
        Iterator k = r.getKeys();
        while (k.hasNext()) {
            String name = (String) k.next();
            if (name.startsWith(DATA_PREFIX))
                names.add(name.substring(DATA_PREFIX.length()));
        }
        if (names.isEmpty())
            names.add(DEFAULT_NAME);
        String[] result = new String[names.size()];
        result = (String[]) names.toArray(result);
        Arrays.sort(result, String.CASE_INSENSITIVE_ORDER);
        return result;
    }

    public static void save(String defectTypeName,
                            DataRepository r,
                            String[] types,
                            String ignorableType) {

        StringBuffer buf = new StringBuffer();
        if (types != null)
            for (int i = 0;  i < types.length;   i++) {
                if (types[i] == null) continue;
                String t = types[i].trim();
                if (t.length() == 0) continue;
                if (t.equals(ignorableType)) continue;
                buf.append("|").append(t);
            }
        String saveValue = buf.toString();
        data = r;

        if (saveValue.length() == 0)
            data.putValue(DATA_PREFIX + defectTypeName, null);
        else
            data.putValue(DATA_PREFIX + defectTypeName,
                          StringData.create(saveValue.substring(1)));
        cache.remove(defectTypeName);
    }


    public static void saveDefault(DataRepository r, String path,
                                   String defectTypeName) {
        data = r;

        // if an empty string was passed in for the defect type, use
        // null instead.
        if (defectTypeName != null && defectTypeName.length() == 0)
            defectTypeName = null;

        // if a nonexistent defect type was named, do nothing.
        if (defectTypeName != null &&
            data.getSimpleValue(DATA_PREFIX + defectTypeName) == null &&
            Settings.getVal("defectType." + defectTypeName) == null)
            return;

        if (path == null) path = "";

        String dataName = DataRepository.createDataName(path, SETTING_DATA_NAME);
        data.putValue(dataName,
                      (defectTypeName == null ? null
                       : StringData.create(defectTypeName)));
    }

    public static String getSetting(DataRepository r, String path) {
        data = r;
        if (path == null) path = "";
        String dataName = DataRepository.createDataName(path, SETTING_DATA_NAME);
        SimpleData defectSetting = data.getSimpleValue(dataName);
        String result = null;
        if (defectSetting != null) result = defectSetting.format();
        if (result != null && result.trim().length() == 0) result = null;
        return result;
    }


    private static DataRepository data = null;


    /** Cache of previously created defect types. */
    private static Hashtable cache = new Hashtable();

    /** The name of the most preferred defect type standard. */
    private static String mostPreferredName = null;

    /** Defect standard to use if everything else fails */
    private static final String DEFAULT_NAME = "Generic";
    private static final String DEFAULT_DEFECT_TYPES =
        "10|20|30|40|50|60|70|80|90|100";
}
