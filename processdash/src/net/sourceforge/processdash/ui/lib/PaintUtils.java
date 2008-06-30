// Copyright (C) 2007-2008 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
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

package net.sourceforge.processdash.ui.lib;

import java.awt.Color;

public class PaintUtils {

    public static Color mixColors(Color a, Color b, double r) {
        double s = 1.0f - r;
        return new Color(
                boundColor((a.getRed()   * r + b.getRed()   * s) / 255.0),
                boundColor((a.getGreen() * r + b.getGreen() * s) / 255.0),
                boundColor((a.getBlue()  * r + b.getBlue()  * s) / 255.0));
    }

    private static float boundColor(double c) {
        if (!(c > 0))
            return 0;
        if (c > 1.0)
            return 1.0f;
        return (float) c;
    }

    public static Color[] getGlassGradient(int size, Color mid, Color light) {
        Color[] result = new Color[size];

        double midPoint = size / 2.0;

        int x = 0;
        while (x < midPoint) {
            double f = x / midPoint;
            result[x++] = mixColors(mid, light, f * f * f);
        }
        while (x < size) {
            double f = (x - midPoint) / (size - midPoint);
            result[x++] = mixColors(light, mid, f);
        }

        return result;
    }

    public static double toGray(Color c) {
        return (0.30 * c.getRed() +
                0.59 * c.getGreen() +
                0.11 * c.getBlue());
    }

    public static Color adjustForContrast(Color fg, Color bg) {
        float[] hsb = new float[3];
        Color.RGBtoHSB(bg.getRed(), bg.getGreen(), bg.getBlue(), hsb);
        float bgBright = hsb[2];
        Color.RGBtoHSB(fg.getRed(), fg.getGreen(), fg.getBlue(), hsb);
        float fgBright = hsb[2];
        float brightDelta = Math.abs(fgBright - bgBright);
        if (brightDelta > 0.1)
            return fg;

        float resultBright;
        if (bgBright <= 0.1)
            resultBright = bgBright + 0.1f;
        else if (bgBright >= 230)
            resultBright = bgBright - 0.1f;
        else if (fgBright > bgBright)
            resultBright = bgBright + 0.1f;
        else
            resultBright = bgBright - 0.1f;
        hsb[2] = resultBright;
        return Color.getHSBColor(hsb[0], hsb[1], hsb[2]);
    }

}
