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


package pspdash;

import java.util.Arrays;

import DistLib.uniform;


/** Construct a confidence interval using a Monte Carlo simulation
 */
public class MonteCarloConfidenceInterval implements ConfidenceInterval
{

    protected DoubleList samples = null;
    protected double viability = NOMINAL;
    protected double acceptableError = 0.001;

    public MonteCarloConfidenceInterval() {
        samples = new DoubleList(getBaseNumSamples());
    }

    protected double getSample() { return Double.NaN; }

    protected int getBaseNumSamples() { return 100; }
    protected int getMaxNumSamples() { return 20000; }
    protected int getSampleIncrement() { return 100; }
    protected double getSampleMultiplier() { return 1.0; }

    protected double getAcceptableError() { return acceptableError; }

    protected void runSimulation() {
        int numSamples = getBaseNumSamples();
        int maxNumSamples = getMaxNumSamples();
        int increment = getSampleIncrement();
        double multiplier = getSampleMultiplier();
        double acceptableError = getAcceptableError();

        double result;
        double lastResult = Double.NaN;
        double lastError = acceptableError * 2;
        while (true) {
            // grow the samples array
            samples.ensureCapacity(numSamples);

            // generate the new samples
            for (int i = numSamples - samples.size();  i-- > 0; )
                samples.add(getSample());

            // get the current 70% LPI
            samples.sort();
            result = getQuantile(0.15);

            // if we're within an acceptable error, or we've reached the
            // maximum number of samples, return.
            double error = Math.abs(result - lastResult);
            if ((error <= acceptableError && lastError <= acceptableError) ||
                numSamples >= maxNumSamples)
                break;

            numSamples = (int) (numSamples * multiplier + increment);
            lastResult = result;
            lastError = error;
        }
    }

    public void addSample(double sample) {
        samples.add(sample);
    }

    public void samplesDone() {
        samples.sort();
    }

    public void setInput(double input) {}
    public void debugPrint(int numSamples) {
        for (int i = 0;   i < numSamples;   i++)
            System.out.println("\t"+samples.get(i));
    }

    protected double getQuantile(double percentage) {
        if (samples == null) return Double.NaN;
        if (!(percentage >= 0 && percentage <= 1)) return Double.NaN;

        double pos = percentage * (samples.size() - 1);

        int posL = (int) Math.floor(pos);
        int posR = (int) Math.ceil(pos);
        if (posL == posR || posR == samples.size())
            return samples.get(posL);

        double sampleL = samples.get(posL);
        double sampleR = samples.get(posR);
        return sampleL + (pos - posL) * (sampleR - sampleL);
    }

    public double getPrediction() {
        return getQuantile(0.5);
    }

    boolean debug = false;
    int d = 0;
    void debug() {
        d++;
    }

    public double getLPI(double percentage) {
        if (debug) debug();
        return getQuantile((1 - percentage) / 2);
    }

    public double getUPI(double percentage) {
        if (debug) debug();
        return getQuantile((1 + percentage) / 2);
    }

    public double getRandomValue(uniform u) {
        return getQuantile(u.random());
    }

    public void calcViability(double expectedValue, double cutoffPercentile) {
        int pos = samples.find(expectedValue);
        double prob = 2 * Math.abs((((double) pos) / samples.size()) - 0.5);
        if (prob > cutoffPercentile)
            // the current probability is not acceptable.
            viability = SERIOUS_PROBLEM;
        else
            // use the percentage to scale the nominal viability rating.
            viability = NOMINAL * (1 - prob);
    }

    public double getViability() {
        return viability;
    }
}
