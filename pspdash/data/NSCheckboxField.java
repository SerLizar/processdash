// PSP Dashboard - Data Automation Tool for PSP-like processes
// Copyright (C) 2003 Software Process Dashboard Initiative
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
// OO-ALC/TISHD
// Attn: PSP Dashboard Group
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net


package pspdash.data;


import netscape.javascript.JSObject;


class NSCheckboxField extends NSField {

    // The values of these literals are an unending source of iritation.
    // Each version of Netscape seems to want different values for true
    // and false...Netscape 4.0 wanted Double(1) and Double(0), for example.
    // As of 4.75, it appears that getMember("checked") will return
    // Boolean.TRUE or Boolean.FALSE.  But setMember("checked", Boolean.FALSE)
    // causes the box to become checked again...?!?  setMember("checked", null)
    // unchecks the box.  So after much trial and error, these are the values
    // that are most likely to work...
    private static Object HTML_TRUE = Boolean.TRUE;
    private static Object HTML_FALSE = null;

    // ..and here are other values we can try if they don't.
    private static Object[] HTML_TRUE_VALUES = {
        Boolean.TRUE, new Double(1.0), "true", "checked" };
    private static Object[] HTML_FALSE_VALUES = {
        null, Boolean.FALSE, new Double(0.0), "false", "unchecked" };


    public NSCheckboxField(JSObject element, Repository data, String dataPath) {
        super(element, data, dataPath);
    }


    public void fetch() {
        variantValue = i.getBoolean();
    }

    public void paint() {
        boolean desiredValue = Boolean.TRUE.equals(variantValue);
        element.setMember("checked", (desiredValue ? HTML_TRUE : HTML_FALSE));

        // now check to see if our changes "took."
        if (desiredValue == isChecked(element.getMember("checked"))) return;

        // our changes didn't take. Try other values.
        Object[] trialValues =
            (desiredValue ? HTML_TRUE_VALUES : HTML_FALSE_VALUES);
        for (int i = 0;   i < trialValues.length;   i++) {
            element.setMember("checked", trialValues[i]);
            // if this trial value appeared to work, save it into the
            // appropriate static field
            if (desiredValue == isChecked(element.getMember("checked"))) {
                debug("changing "+desiredValue+" constant to '"+trialValues[i]+"'");
                if (desiredValue)
                    HTML_TRUE = trialValues[i];
                else
                    HTML_FALSE = trialValues[i];
                return;
            }
        }
    }

    public void parse() {
        Object checked = element.getMember("checked");
        variantValue = new Boolean(isChecked(checked));
    }

    private boolean isChecked(Object checked) {
        if (checked == null) return false;
        if (checked instanceof Boolean) return ((Boolean) checked).booleanValue();
        if (checked instanceof Number) return ((Number) checked).intValue() > 0;
        String s = checked.toString();
        if (s == null || s.length() == 0) return false;
        return ("1yYtTcC".indexOf(s.charAt(0)) != -1);
    }

}