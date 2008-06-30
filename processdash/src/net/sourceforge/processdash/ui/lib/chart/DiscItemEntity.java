// Copyright (C) 2008 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
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

package net.sourceforge.processdash.ui.lib.chart;

import java.awt.Shape;

import org.jfree.chart.entity.ChartEntity;
import org.jfree.data.general.PieDataset;

public class DiscItemEntity extends ChartEntity {

    private PieDataset dataset;

    private int discIndex;

    private Comparable discKey;


    public DiscItemEntity(Shape area, PieDataset dataset, int discIndex,
            Comparable discKey, String toolTipText, String urlText) {
        super(area, toolTipText, urlText);
        this.dataset = dataset;
        this.discIndex = discIndex;
        this.discKey = discKey;
    }

    /**
     * @return Returns the dataset.
     */
    public PieDataset getDataset() {
        return dataset;
    }

    /**
     * @param dataset
     *                The dataset to set.
     */
    public void setDataset(PieDataset dataset) {
        this.dataset = dataset;
    }

    /**
     * @return Returns the discIndex.
     */
    public int getDiscIndex() {
        return discIndex;
    }

    /**
     * @param discIndex
     *                The discIndex to set.
     */
    public void setDiscIndex(int discIndex) {
        this.discIndex = discIndex;
    }

    /**
     * @return Returns the discKey.
     */
    public Comparable getDiscKey() {
        return discKey;
    }

    /**
     * @param discKey
     *                The discKey to set.
     */
    public void setDiscKey(Comparable discKey) {
        this.discKey = discKey;
    }

}
