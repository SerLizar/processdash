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

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import pspdash.EVSchedule.Period;

import DistLib.uniform;

public class EVScheduleRandom extends EVSchedule
    implements EVScheduleConfidenceIntervals.Randomizable
{

    protected Vector origPeriods = new Vector();
    protected ConfidenceInterval cost;
    protected ConfidenceInterval time;
    protected double origDefaultPlanDirectTime;
    protected double origDefaultPlanTotalTime;
    protected long lastPeriodEnd;
    protected long defaultPeriodLen;
    protected double lastPeriodCumPlanTime;

    public EVScheduleRandom(EVSchedule s) {
        super(s);
        setEffectiveDate(s.getEffectiveDate());
        this.cost = s.getMetrics().getCostConfidenceInterval();
        this.time = s.getMetrics().getTimeErrConfidenceInterval();
        this.metrics = new EVMetricsRandom(s.getMetrics());

        preparePeriods(s.periods);
        origDefaultPlanDirectTime = defaultPlanDirectTime;
        origDefaultPlanTotalTime = defaultPlanTotalTime;
    }

    public EVSchedule copy() { return this; }

    protected synchronized void preparePeriods(List histPeriods) {
        cleanUp();

        Date effDate = getEffectiveDate();
        Period p;

        // grow the schedule until the final schedule period falls
        // after the effective date.
        if (defaultPlanDirectTime != 0)
            while (true) {
                p = get(periods.size()-1);
                if (p.getBeginDate().compareTo(effDate) >= 0)
                    break;

                double newCumTime =
                    p.cumPlanDirectTime + defaultPlanDirectTime;
                super.getPlannedCompletionDate(newCumTime, newCumTime);
            }

        p = getLast();
        lastPeriodEnd = p.endDate.getTime();
        long lastPeriodStart = p.getBeginDate().getTime();
        defaultPeriodLen = lastPeriodEnd - lastPeriodStart;

        splitAt(effDate);
        rewriteHistory(effDate, histPeriods);
        recalcCumPlanTimes();
        simplifyPeriods(effDate);
        addAllPeriods(periods, origPeriods);
    }

    /** split the schedule at a given date.
     */
    protected void splitAt(Date effDate) {
        // find the period that contains the given date.
        Period r = get(effDate);
        int pos = periods.indexOf(r);

        // if the date already begins or ends with the given date, we
        // don't need to do anything.
        if (r.endDate.equals(effDate) || r.getBeginDate().equals(effDate))
            return;

        // calculate what percentage of the period lies on each side
        // of the given date.
        long eff = effDate.getTime();
        long start = r.getBeginDate().getTime();
        long end = r.endDate.getTime();
        double duration = end - start;
        double left = (eff - start) / duration;
        double right = (end - eff) / duration;

        // create a new period that ends on the given date; insert it
        // before the existing period; adjust "previous" pointers
        Period l = new Period(effDate, 0.0);
        l.previous = r.previous;
        r.previous = l;
        periods.add(pos, l);

        // proportionally distribute plan total time
        double data = r.planTotalTime;
        l.planTotalTime = left * data;   r.planTotalTime = right * data;

        // proportionally distribute plan direct time
        data = r.planDirectTime;
        l.planDirectTime = left * data;  r.planDirectTime = right * data;

        // calc cumPlanDirectTime at the given date
        l.cumPlanDirectTime = r.cumPlanDirectTime - r.planDirectTime;

        // calc cumPlanValue at the given date (optimistic)
        l.cumPlanValue = r.cumPlanValue;

        // allocate actual direct time to the left period
        l.actualDirectTime = r.actualDirectTime;
        r.actualDirectTime = 0;

        // allocate actual indirect time to the left period
        l.actualIndirectTime = r.actualIndirectTime;
        r.actualIndirectTime = 0;

        // allocate actual data to the left period
        l.cumActualDirectTime = r.cumActualDirectTime;
        l.cumEarnedValue = r.cumEarnedValue;
    }

    private void rewriteHistory(Date effDate, List histPeriods) {
        for (int i = 1;  i < periods.size() && i < histPeriods.size();  i++) {
            Period p = get(i);
            Period h = (Period) histPeriods.get(i);
            if (p.endDate.compareTo(effDate) <= 0) {
                p.planTotalTime = h.actualDirectTime + h.actualIndirectTime;
                p.planDirectTime = h.actualDirectTime;
                p.cumPlanDirectTime = h.cumActualDirectTime;
                p.cumPlanValue = h.cumEarnedValue;
            }
            p.automatic = false;
        }
    }


    private void simplifyPeriods(Date effectiveDate) {
        Period p = getLast();
        while (p != null) {
            maybeSimplifyPeriod(p, effectiveDate);
            p = p.previous;
        }
    }


    private void maybeSimplifyPeriod(Period b, Date effectiveDate) {
        int bPos = periods.indexOf(b);
        if (bPos < 2) return;
        Period a = b.previous;
        int aPos = bPos - 1;

        boolean aIsPast = a.getBeginDate().before(effectiveDate);
        boolean bIsPast = b.getBeginDate().before(effectiveDate);
        if (aIsPast != bIsPast) return;

        if (!aIsPast) {
            double aSpeed = calcPeriodSpeed(a);
            double bSpeed = calcPeriodSpeed(b);
            if (aSpeed != bSpeed) return;
        }

        b.planTotalTime += a.planTotalTime;
        b.planDirectTime += a.planDirectTime;
        b.previous = a.previous;
        periods.remove(aPos);
    }

    private double calcPeriodSpeed(Period b) {
        long start = b.getBeginDate().getTime();
        long end = b.endDate.getTime();
        double duration = end - start;

        return b.planDirectTime / duration;
    }

    public void randomize(uniform random) {
        addAllPeriods(origPeriods, periods);
        double timeErrRatio = time.getRandomValue(random);
        rewriteFuture(timeErrRatio);
        ((EVMetricsRandom) metrics).randomize(this, random);
    }

    private void rewriteFuture(double timeErrRatio) {
        Date effDate = getEffectiveDate();
        double cumPlanDirectTime = 0;

        Iterator i = periods.iterator();
        Date endDate = null;
        while (i.hasNext()) {
            Period p = (Period) i.next();
            if (p.endDate.compareTo(effDate) > 0) {
                p.planDirectTime = p.planDirectTime * timeErrRatio;
                p.cumPlanDirectTime = cumPlanDirectTime + p.planDirectTime;
            }
            cumPlanDirectTime = p.cumPlanDirectTime;
            endDate = p.endDate;
        }
        lastPeriodCumPlanTime = cumPlanDirectTime;
        lastPeriodEnd = endDate.getTime();
        defaultPlanDirectTime = origDefaultPlanDirectTime * timeErrRatio;
        defaultPlanTotalTime = origDefaultPlanTotalTime * timeErrRatio;
    }

    /** Return the date that the schedule would reach the given cumulative
     * plan time. Perform a "what-if" calculation - don't modify the
     * current schedule.
     */
    public Date getHypotheticalDate(double cumPlanTime) {
        if (Double.isNaN(cumPlanTime) || Double.isInfinite(cumPlanTime))
            return NEVER;

        if (cumPlanTime < lastPeriodCumPlanTime)
            return extrapolateWithinSchedule(cumPlanTime);
        else if (defaultPlanDirectTime > 0) {
            double additionalTime = cumPlanTime - lastPeriodCumPlanTime;
            double additionalPeriods = additionalTime / defaultPlanDirectTime;
            long additionalMillis =
                (long) (additionalPeriods * defaultPeriodLen);
            return new Date(lastPeriodEnd + additionalMillis);
        } else
            return NEVER;
    }

    public synchronized Date getPlannedCompletionDate(double cumPlanTime,
                                                      double cumPlanValue) {
        Date result = getHypotheticalDate(cumPlanTime);
        if (NEVER.equals(result) || (result.getTime() <= lastPeriodEnd))
            return result;

        else {
            Period p = new Period(result, cumPlanTime - lastPeriodCumPlanTime);
            p.previous = getLast();
            periods.add(p);
            p.cumPlanDirectTime = lastPeriodCumPlanTime = cumPlanTime;
            lastPeriodEnd = result.getTime();
            return result;
        }
    }

}
