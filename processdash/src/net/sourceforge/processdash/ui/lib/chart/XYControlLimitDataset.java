// Copyright (C) 2008 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui.lib.chart;

import net.sourceforge.processdash.i18n.Resources;

import org.jfree.data.general.DatasetChangeEvent;
import org.jfree.data.general.DatasetChangeListener;
import org.jfree.data.xy.AbstractXYDataset;
import org.jfree.data.xy.XYDataset;

/**
 * Class used to analyze an underlying XYDataset that contains XY data and then
 *  produce control limit lines.
 */
public class XYControlLimitDataset extends AbstractXYDataset
        implements DatasetChangeListener {
    /** The resources keys */
    public static final String[] SERIES_RES_KEYS =
        new String[] { "Mean", "UCL", "LCL" };

    private final Resources resources =
        Resources.getDashBundle("Analysis.Snippet.Charts.Control_limit");

    /** The number of valid series in the dataset */
    private int seriesCount;

    /** The array indexes where data related to different series are stored */
    public static final int MEAN_POS = 0;
    public static final int UCL_POS = 1;
    public static final int LCL_POS = 2;

    /** The different series keys */
    private String[] seriesKey = new String[3];

    /** Since we are drawing horizontal lines, the Y value  of a series is always the same,
        no matter what's the item count. This array contains those Y values */
    private double values[] = new double[3];

    /** The XYDataset that we are adding control limit to. */
    protected XYDataset source;

    /** The series number of the underlying dataset for which we calculate the control
        limits */
    private int seriesNum;

    /** The smallest and biggest X value of the source dataset */
    private double minX;
    private double maxX;

    public XYControlLimitDataset(XYDataset source, int seriesNum) {
        this.source = source;
        this.source.addChangeListener(this);
        this.seriesNum = seriesNum;

        updateData();

        seriesKey[MEAN_POS] = resources.getString(SERIES_RES_KEYS[MEAN_POS]);
        seriesKey[UCL_POS] = resources.getString(SERIES_RES_KEYS[UCL_POS]);
        seriesKey[LCL_POS] = resources.getString(SERIES_RES_KEYS[LCL_POS]);
    }

    private void updateData() {
        // The sum of the natural log of all data point values
        double sumOfLog = 0;

        // The sum of the square of the natural log of all data point values
        double squareSumOfLog = 0;

        minX = Double.MAX_VALUE;
        maxX = Double.MIN_VALUE;

        int n = 0;
        if (source.getSeriesCount() > seriesNum)
            n = source.getItemCount(seriesNum);

        // Since need to go through all the source dataset value, we might as well calculate
        //  all the values that we need in one pass.
        double logOfValue;
        double xValue;
        for (int i = 0; i < n; ++i) {
            logOfValue = Math.log(source.getYValue(seriesNum, i));
            sumOfLog += logOfValue;
            squareSumOfLog += Math.pow(logOfValue, 2);

            xValue = source.getXValue(seriesNum, i);
            if (xValue < minX) { minX = xValue; }
            if (xValue > maxX) { maxX = xValue; }
        }

        // The mean of the natural log of all data point values
        double logMean = sumOfLog / n;

        // The standard deviation of the natural log of all data point values
        double logStd = Math.sqrt((squareSumOfLog - n * Math.pow(logMean, 2)) / (n-1));

        if (Double.isNaN(logMean) || Double.isInfinite(logMean)) {
            this.seriesCount = 0;
        }
        else {
            values[MEAN_POS] = Math.exp(logMean);

            if (Double.isNaN(logStd) || logStd <= 0 || Double.isInfinite(logStd)) {
                this.seriesCount = 1;
            }
            else {
                this.seriesCount = 3;
                values[UCL_POS] = Math.exp(logMean + 3 * logStd);
                values[LCL_POS] = Math.exp(logMean - 3 * logStd);
            }
        }
    }

    @Override
    public int getSeriesCount() {
        return this.seriesCount;
    }

    @Override
    public Comparable getSeriesKey(int series) {
        if (0 <= series && series < seriesKey.length)
            return seriesKey[series];
        else
            return null;
    }

    /**
     * Since we are only drawing straight lines made of 2 points, we know
     *  for a fact that there will be 2 items in all series
     */
    public int getItemCount(int series) {
        if (0 <= series && series < this.seriesCount)
            return 2;
        else
            return 0;
    }

    public Number getX(int series, int item) {
        if (0 <= series && series < this.seriesCount)
            return item == 0 ? minX : maxX;
        else
            return null;
    }

    public Number getY(int series, int item) {
        if (0 <= series && series < values.length)
            return values[series];
        else
            return null;
    }

    public void datasetChanged(DatasetChangeEvent event) {
        updateData();
    }

    public static XYDataset getControlLimit(XYDataset source) {
        return getControlLimit(source, 0);
    }
    public static XYDataset getControlLimit(XYDataset source, int seriesNum) {
        return new XYControlLimitDataset(source, seriesNum);
    }

}
