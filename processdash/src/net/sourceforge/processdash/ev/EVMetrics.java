// Copyright (C) 2003-2006 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ev;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.TreeMap;

import javax.swing.event.EventListenerList;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.ev.ci.AbstractConfidenceInterval;
import net.sourceforge.processdash.ev.ci.ConfidenceInterval;
import net.sourceforge.processdash.ev.ci.TargetedConfidenceInterval;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.util.FormatUtil;

import org.w3c.dom.Element;


public class EVMetrics implements TableModel {

    /** The total planned time for all tasks in an EVModel, in minutes.
     * (aka Budget At Completion, or BAC) */
    protected double totalPlanTime = 0.0;

    /** The total planned time for all <b>completed</b> tasks in an EVModel,
     * in minutes.  (aka Budgeted Cost for Work Performed, or BCWP) */
    protected double earnedValueTime = 0.0;

    /** The total actual time for all <b>completed</b> tasks in an
     *  EVModel, in minutes.  (aka Actual Cost for Work Performed, or ACWP) */
    protected double actualTime = 0.0;

    /** The total planned time for all tasks that were planned to be
     * completed as of today, in minutes.  (aka Budgeted Cost for Work
     * Scheduled, or BCWS) */
    protected double planTime = 0.0;

    /** The start date of this EVModel */
    protected Date startDate = null;

    /** The current date (or effective date) */
    protected Date currentDate = null;

    /** The planned completion date */
    protected Date planDate = null;

    /** The forecast completion date. */
    protected Date forecastDate = null;

    /** What % complete is the current period, in terms of elapsed time? */
    double periodPercent = 0.0;

    /** The total planned time to date for completed schedule periods. */
    double totalSchedulePlanTime = 0.0;

    /** The total actual time to date for completed schedule periods. */
    double totalScheduleActualTime = 0.0;

    /** The total actual time spent on indirect tasks */
    double indirectTime = 0.0;

    /** The end of the current time period in the schedule */
    protected Date periodEnd = null;

    /** A confidence interval predicting the total cost of the tasks
     * which have not yet been completed (note that this <b>includes</b>
     * time spent so far on the incomplete tasks). */
    protected ConfidenceInterval costInterval = null;

    /** A confidence interval predicting the ratio of actual direct time
     * to planned direct time */
    protected ConfidenceInterval timeErrInterval = null;

    /** A confidence interval predicting the forecast completion date. */
    protected ConfidenceInterval completionDateInterval = null;

    /** A list of warnings/errors associated with this EVModel.
     *  the keys are the error messages; they may map to an EVTask
     *  where the error was located. */
    protected Map errors = null;

    /** a string that can be prepended to error messages to identify
     * that they came from this EVMetrics object.
     */
    protected String errorQualifier = null;

    public EVMetrics() {
        this(true);
    }

    public EVMetrics(boolean supportFormatting) {
        if (supportFormatting)
            metrics = buildFormatters();
        else
            metrics = Collections.EMPTY_LIST;
    }

    public void reset(Date start, Date current,
                      Date periodStart, Date periodEnd) {
        totalPlanTime = earnedValueTime = actualTime = planTime =
            indirectTime = totalScheduleActualTime = totalSchedulePlanTime = 0.0;
        startDate = start;
        currentDate = current;
        planDate = forecastDate = null;
        this.periodEnd = periodEnd;
        errors = null;
        if (periodStart != null && periodEnd != null) {
            long periodElapsed = currentDate.getTime() - periodStart.getTime();
            long periodLength = periodEnd.getTime() - periodStart.getTime();
            periodPercent = (double) periodElapsed / (double) periodLength;
            if (periodPercent < 0.0) periodPercent = 0.0;
            if (periodPercent > 1.0) periodPercent = 1.0;
        } else {
            periodEnd = current;
            periodPercent = 0.0;
        }
    }
    public void addTask(double planTime, double actualTime,
                        Date planDate, Date actualDate) {
        this.totalPlanTime += planTime;

        // has the task been completed?
        if (actualDate != null) {
            this.earnedValueTime += planTime;
            this.actualTime += actualTime;
        }

        // did we plan to have the task completed by now?
        if (planDate != null) {
            if (planDate.before(currentDate))
                this.planTime += planTime;
            else if (!periodEnd.before(planDate))
                this.planTime += planTime * periodPercent;
        }

        this.planDate = EVCalculator.maxPlanDate(this.planDate, planDate);
    }
    public void addIndirectTime(double indirectTime) {
        this.indirectTime += indirectTime;
    }
    public void addError(String message, EVTask node) {
        if (errors == null) errors = new TreeMap();
        errors.put(message, node);
    }
    public void setErrorQualifier(String qualifier) {
        errorQualifier = qualifier;
    }
    public String getErrorQualifier() { return errorQualifier; }

    public void recalcComplete(EVSchedule s) {
        recalcForecastDate(s);
        recalcViability(s);
        recalcMetricsFormatters();
    }
    private static boolean RETARGET_INTERVALS =
        !Settings.getBool("ev.disableRetarget", false);
    protected void retargetViability(EVSchedule s,
                                      TargetedConfidenceInterval t,
                                      double target) {
        if (RETARGET_INTERVALS)
            t.calcViability(target, 0.7);
    }
    protected void retargetViability(EVSchedule s,
                                      TargetedConfidenceInterval t,
                                      Date target) {
        if (RETARGET_INTERVALS) {
            double v = -1;
            if (target != null) v = target.getTime();
            t.calcViability(v, 0.7);
        }
    }
    protected boolean unviable(ConfidenceInterval ci) {
        if (ci == null) return false;
        if (ci.getViability() <= ConfidenceInterval.ACCEPTABLE) return true;
        return false;
    }
    protected void recalcViability(EVSchedule s) {
        if (costInterval instanceof TargetedConfidenceInterval)
            retargetViability(s, (TargetedConfidenceInterval) costInterval,
                              independentForecastCost() - actual());
        if (unviable(costInterval)) {
            // System.out.println("cost interval is not viable");
            costInterval = null;
            timeErrInterval = null;
            completionDateInterval = null;
        }

        /*maybeCalcViability(s, timeErrInterval, 1.0);
        if (unviable(timeErrInterval)) {
            // System.out.println("time err interval is not viable");
            timeErrInterval = null;
            completionDateInterval = null;
        }*/

        if (completionDateInterval instanceof TargetedConfidenceInterval)
            retargetViability
                (s, (TargetedConfidenceInterval)completionDateInterval,
                 independentForecastDate());
        if (unviable(completionDateInterval)) {
            // System.out.println("completion date interval is not viable");
            completionDateInterval = null;
        }
    }
    protected void recalcMetricsFormatters() {
        validMetrics.clear();
        Iterator i = metrics.iterator();
        while (i.hasNext()) {
            MetricFormatter f = (MetricFormatter) i.next();
            f.recalc();
            if (f.isValid())
                validMetrics.add(f);
        }
        fireTableChanged(new TableModelEvent(this));
    }
    protected void recalcScheduleTime(EVSchedule s) {
        totalSchedulePlanTime = s.getScheduledPlanTime(currentDate);
        totalScheduleActualTime = s.getScheduledActualTime(currentDate);
    }
    public void setForecastDate(Date d) {
        if (d == EVSchedule.NEVER)
            forecastDate = null;
        else
            forecastDate = d;
    }
    protected void recalcForecastDate(EVSchedule s) {
        // Do nothing by default; our calculator will normally handle this.
        // Subclasses may override if a particular technique is required.
    }
    public void setCostConfidenceInterval(ConfidenceInterval incompleteTaskCost) {
        costInterval = incompleteTaskCost;
    }
    public ConfidenceInterval getCostConfidenceInterval() {
        return costInterval;
    }
    public void setTimeErrConfidenceInterval(ConfidenceInterval timeError) {
        timeErrInterval = timeError;
    }
    public ConfidenceInterval getTimeErrConfidenceInterval() {
        return timeErrInterval;
    }
    public void setDateConfidenceInterval(ConfidenceInterval completionDate) {
        completionDateInterval = completionDate;
    }


    /** BCWP */
    public double earnedValue() { return earnedValueTime; }
    /** ACWP */
    public double actual()      { return actualTime;      }
    /** BCWS */
    public double plan()        { return planTime;        }
    /** BAC */
    public double totalPlan()   { return totalPlanTime;   }
    public Date startDate()     { return startDate;       }
    public Date currentDate()   { return currentDate;     }
    public Date planDate()      { return planDate;        }



    public double costVariance() {
        return earnedValue() - actual();
    }
    public double scheduleVariance() {
        return earnedValue() - plan();
    }
    public double costVariancePercentage() {
        return costVariance() / earnedValue();
    }
    public double timeEstimatingError() {
        return  - costVariancePercentage();
    }
    public double scheduleVariancePercentage() {
        return scheduleVariance() / plan();
    }
    public double costPerformanceIndex() {
        return earnedValue() / actual();
    }
    public double schedulePerformanceIndex() {
        return earnedValue() / plan();
    }
    public double directTimePerformanceIndex() {
        return totalSchedulePlanTime / totalScheduleActualTime;
    }
    public double percentComplete() {
        return earnedValue() / totalPlan();
    }
    public double earnedValuePercentage() {
        return percentComplete();
    }
    public double percentSpent() {
        return actual() / totalPlan();
    }
    public double toCompletePerformanceIndex() {
        return (totalPlan() - earnedValue()) / (totalPlan() - actual());
    }
    public double improvementRatio() {
        return (toCompletePerformanceIndex() / costPerformanceIndex()) - 1.0;
    }
    public double independentForecastCost() {
        return totalPlan() / costPerformanceIndex();
    }
    public double independentForecastCostLPI() {
        if (costInterval == null)
            return Double.NaN;
        else
//          return Math.max(actualTime + costInterval.getLPI(0.70),
//                          totalScheduleActualTime);
            return actualTime + costInterval.getLPI(0.70);
    }
    public double independentForecastCostUPI() {
        if (costInterval == null)
            return Double.NaN;
        else
            return actualTime + costInterval.getUPI(0.70);
    }

    /** the number of minutes that have elapsed since the beginning of
         the project. */
    public double elapsed() {
        Date s, c;
        if ((s = startDate()) == null) return Double.NaN;
        if ((c = currentDate()) == null) return Double.NaN;
        return (c.getTime() - s.getTime()) / (double) MINUTE_MILLIS;
    }

    public double scheduleVarianceDuration() {
        return scheduleVariance() * elapsed() / earnedValue();
    }
    public double independentForecastDuration() {
        return calcDuration(startDate(), forecastDate);
    }
    public Date independentForecastDate() {
        return forecastDate;
    }
    public Date independentForecastDateLPI() {
        if (completionDateInterval == null)
            return null;
        else
            return convertToDate(completionDateInterval.getLPI(0.70));
    }
    public Date independentForecastDateUPI() {
        if (completionDateInterval == null)
            return null;
        else
            return convertToDate(completionDateInterval.getUPI(0.70));
    }

    protected Date convertToDate(double when) {
        if (Double.isNaN(when) || Double.isInfinite(when))
            return null;
        else if (when == EVSchedule.NEVER.getTime())
            return null;
        else
            return new Date((long) when);
    }

    protected double calcDuration(Date s, Date e) {
        if (s == null || e == null) return Double.NaN;
        return (e.getTime() - s.getTime()) / (double) MINUTE_MILLIS;
    }

    public Map getErrors() {
        if (errors == null)
            return null;
        else
            return Collections.unmodifiableMap(errors);
    }






    static Resources resources = Resources.getDashBundle("EV");
    static Map formatCaches = Collections.synchronizedMap(new HashMap());

    protected String getResourcePrefix() { return null; }

    protected static class FormatResources implements Cloneable {
        String metricName;
        MessageFormat shortFormat, medFormat, fullFormat;
        public Object clone() {
            try {
                return super.clone();
            } catch (CloneNotSupportedException e) {
                return null;
            }
        }
        public void loadResources(String resourcePrefix, String key) {
            if (metricName == null)
                metricName = resources.getString("Metrics." + key + ".Name");
            if (resourcePrefix != null)
                key = resourcePrefix + key;
            shortFormat = getFmt("Metrics." + key + ".Short_FMT", shortFormat);
            medFormat   = getFmt("Metrics." + key + ".Medium_FMT", medFormat);
            fullFormat  = getFmt("Metrics." + key + ".Full_FMT",   fullFormat);
        }
        private MessageFormat getFmt(String key, MessageFormat defVal)
        {
            try {
                return new MessageFormat(resources.getString(key));
            } catch (MissingResourceException mre) {
                if (defVal != null) return defVal;
                throw mre;
            }
        }
    }
    protected FormatResources getFormatResources(String key) {
        return getFormatResources(getResourcePrefix(), key);
    }
    protected static FormatResources getFormatResources(String prefix,
                                                        String key)
    {
        String resourcePrefix = prefix;

        Map cache = (Map) formatCaches.get(resourcePrefix);
        if (cache == null) {
            cache = Collections.synchronizedMap(new HashMap());
            formatCaches.put(resourcePrefix, cache);
        }

        FormatResources result = (FormatResources) cache.get(key);
        if (result == null) {
            result = makeFormatter(prefix, key);
            cache.put(key, result);
        }
        return result;
    }

    protected static FormatResources makeFormatter(String prefix, String key)
    {
        FormatResources result = null;
        if (prefix == null) {
            result = new FormatResources();
        } else {
            FormatResources base = getFormatResources(null, key);
            result = (FormatResources) base.clone();
        }
        result.loadResources(prefix, key);
        return result;
    }


    protected abstract class MetricFormatter {
        String key;
        String metricName;
        MessageFormat shortFormat, medFormat, fullFormat;
        Object[] args = null;

        public MetricFormatter(String key) {
            this.key = key;
            FormatResources fmt = getFormatResources(key);
            metricName = fmt.metricName;
            shortFormat = fmt.shortFormat;
            medFormat = fmt.medFormat;
            fullFormat = fmt.fullFormat;
        }

        protected boolean isValid() { return args != null; }

        public String getName() { return metricName; }
        public String getShort() { return shortFormat.format(args); }
        public String getMed() { return medFormat.format(args); }
        public String getFull() { return fullFormat.format(args); }
        public String getKey() { return key; }

        protected abstract void recalc();
    }

    protected abstract class AbsMetricFormatter extends MetricFormatter {
        public AbsMetricFormatter(String key) { super(key); }
        abstract double val();
        protected void recalc() {
            double d = val();
            if (badDouble(d))
                args = null;
            else
                args = new Object[] { new Double(d),
                                      new Double(Math.abs(d)) };
        }
    }

    protected abstract class CostMetricFormatter extends MetricFormatter {
        public CostMetricFormatter(String key) { super(key); }
        abstract double val();
        protected void recalc() {
            double d = val();
            if (badDouble(d))
                args = null;
            else
                args = new Object[] { new Double(d/HOUR_MINUTES),
                                      formatDuration(d, HOUR_MINUTES) };
        }
    }

    protected abstract class DateMetricFormatter extends MetricFormatter {
        public DateMetricFormatter(String key) { super(key); }
        abstract Date val();
        protected void recalc() {
            Date d = val();
            if (d == null)
                args = null;
            else
                args = new Object[] { d };
        }
    }

    protected abstract class DblMetricFormatter extends MetricFormatter {
        public DblMetricFormatter(String key) { super(key); }
        abstract double val();
        protected void recalc() {
            double d = val();
            if (badDouble(d))
                args = null;
            else
                args = new Object[] { new Double(d) };
        }
    }

    protected abstract class DurationMetricFormatter extends MetricFormatter {
        public DurationMetricFormatter(String key) { super(key); }
        abstract double val();
        protected void recalc() {
            double d = val();
            if (badDouble(d))
                args = null;
            else
                args = new Object[] { new Double(d/DAY_MINUTES),
                                      formatDuration(d) };
        }
    }

    protected abstract class CostRangeMetricFormatter extends MetricFormatter {
        public CostRangeMetricFormatter(String key) { super(key); }
        abstract double lpi();
        abstract double upi();
        protected void recalc() {
            double lpi = lpi();
            double upi = upi();
            if (badDouble(lpi) || badDouble(upi))
                args = null;
            else
                args = new Object[] { new Double(lpi/HOUR_MINUTES),
                                      formatDuration(lpi, HOUR_MINUTES),
                                      new Double(upi/HOUR_MINUTES),
                                      formatDuration(upi, HOUR_MINUTES) };
        }
    }

    protected abstract class DateRangeMetricFormatter extends MetricFormatter {
        public DateRangeMetricFormatter(String key) { super(key); }
        abstract Date lpi();
        abstract Date upi();
        protected void recalc() {
            Date lpi = lpi();
            Date upi = upi();
            if (lpi == null || upi == null)
                args = null;
            else
                args = new Object[] { lpi, upi };
        }
    }

    protected List buildFormatters() {
        ArrayList result = new ArrayList();
        result.add(new DateMetricFormatter("Plan_Date") {
            Date val() { return planDate(); } } );
        result.add(new CostMetricFormatter("Cost_Variance") {
                double val() { return costVariance(); } } );
        result.add(new AbsMetricFormatter("Cost_Variance_Percent") {
                double val() { return costVariancePercentage(); } } );
        result.add(new DblMetricFormatter("Cost_Performance_Index") {
                double val() { return costPerformanceIndex(); } } );
        result.add(new CostMetricFormatter("Schedule_Variance") {
                double val() { return scheduleVariance(); } } );
        result.add(new AbsMetricFormatter("Schedule_Variance_Percent") {
                double val() { return scheduleVariancePercentage(); } } );
        result.add(new DurationMetricFormatter
            ("Schedule_Variance_Duration") {
                double val() { return scheduleVarianceDuration(); } } );
        result.add(new DblMetricFormatter("Schedule_Performance_Index") {
                double val() { return schedulePerformanceIndex(); } } );
        result.add(new DblMetricFormatter("Percent_Complete") {
                double val() { return percentComplete(); } } );
        result.add(new DblMetricFormatter("Percent_Spent") {
                double val() { return percentSpent(); } } );
        result.add(new DblMetricFormatter("To_Complete_Index") {
                double val() { return toCompletePerformanceIndex(); } } );
        result.add(new AbsMetricFormatter("Improvement_Ratio") {
                double val() { return improvementRatio(); } } );
        result.add(new CostMetricFormatter("Forecast_Cost") {
                double val() { return independentForecastCost(); } } );
        result.add(new CostRangeMetricFormatter("Forecast_Cost_Range") {
                double lpi() { return independentForecastCostLPI(); }
                double upi() { return independentForecastCostUPI(); } } );
        result.add(new DurationMetricFormatter("Forecast_Duration") {
                double val() { return independentForecastDuration(); } } );
        result.add(new DateMetricFormatter("Forecast_Date") {
                Date val() { return independentForecastDate(); } } );
        result.add(new DateRangeMetricFormatter("Forecast_Date_Range") {
                Date lpi() { return independentForecastDateLPI(); }
                Date upi() { return independentForecastDateUPI(); } } );
        return result;
    }

    private static String ONE_YEAR = resources.getString("Metrics.One_Year");
    private static MessageFormat YEARS_FMT =
        new MessageFormat(resources.getString("Metrics.Years_FMT"));
    private static String ONE_MONTH = resources.getString("Metrics.One_Month");
    private static MessageFormat MONTHS_FMT =
        new MessageFormat(resources.getString("Metrics.Months_FMT"));
    private static String ONE_WEEK = resources.getString("Metrics.One_Week");
    private static MessageFormat WEEKS_FMT =
        new MessageFormat(resources.getString("Metrics.Weeks_FMT"));
    private static String ONE_DAY = resources.getString("Metrics.One_Day");
    private static MessageFormat DAYS_FMT =
        new MessageFormat(resources.getString("Metrics.Days_FMT"));
    private static String ONE_HOUR = resources.getString("Metrics.One_Hour");
    private static MessageFormat HOURS_FMT =
        new MessageFormat(resources.getString("Metrics.Hours_FMT"));
    private static String ONE_MINUTE = resources.getString("Metrics.One_Minute");
    private static MessageFormat MINUTES_FMT =
        new MessageFormat(resources.getString("Metrics.Minutes_FMT"));

    static final int MINUTE_MILLIS = 60 /*seconds*/ * 1000 /*millis*/;
    static final int MINUTE        = 1;
    static final int HOUR_MINUTES  = 60;
    static final int DAY_MINUTES   = 24 /*hours*/ * HOUR_MINUTES;
    static final int WEEK_MINUTES  = 7  /*days*/  * DAY_MINUTES;
    static final int MONTH_MINUTES = 30 /*days*/  * DAY_MINUTES;
    static final int YEAR_MINUTES  = 365 /*days*/ * DAY_MINUTES;


    private static int[] DURATION_UNITS = {
        YEAR_MINUTES, MONTH_MINUTES, WEEK_MINUTES,
        DAY_MINUTES, HOUR_MINUTES, MINUTE };
    private static MessageFormat[] DURATION_FORMATS = {
        YEARS_FMT, MONTHS_FMT, WEEKS_FMT, DAYS_FMT, HOURS_FMT, MINUTES_FMT };
    private static String[] DURATION_SINGLES = {
        ONE_YEAR, ONE_MONTH, ONE_WEEK, ONE_DAY, ONE_HOUR, ONE_MINUTE };



    public static String formatDuration(double duration) {
        return formatDuration(duration, YEAR_MINUTES);
    }
    public static String formatDuration(double duration, int maxUnits) {
        if (badDouble(duration))
            return null;
        if (duration < 0) duration = -duration;

        if (maxUnits < MINUTE) maxUnits = MINUTE;

        for (int i = 0;   i < DURATION_UNITS.length;   i++) {
            if (DURATION_UNITS[i] > maxUnits) continue;

            if (duration > DURATION_UNITS[i])
                return formatNumber(DURATION_FORMATS[i],
                                    duration / DURATION_UNITS[i]);
            if (duration == DURATION_UNITS[i])
                return DURATION_SINGLES[i];
        }

        return formatNumber(MINUTES_FMT, duration);
    }
    public static String formatNumber(MessageFormat f, double number) {
        return f.format(new Object[] { formatNumber(number) } );
    }
    public static String formatNumber(double number) {
        return FormatUtil.formatNumber(number);
    }


    public static boolean badDouble(double d) {
        return Double.isNaN(d) || Double.isInfinite(d);
    }
    public static Object[] args(double d) {
        Object[] result = { new Double(d) };
        return result;
    }
    public static Object[] args(Object o) {
        Object[] result = { o };
        return result;
    }
    public static Object[] args(double d, Object o) {
        Object[] result = { new Double(d), o };
        return result;
    }
    public static Object[] durationArgs(double d) {
        return args(d/DAY_MINUTES, formatDuration(d));
    }
    public static Object[] costArgs(double d) {
        return args(d/HOUR_MINUTES, formatDuration(d, HOUR_MINUTES));
    }
    public static Object[] absArgs(double d) {
        Object[] result = { new Double(d), new Double(Math.abs(d)) };
        return result;
    }



    /*
     *TableModel Interface
     */

    /** display the name of the metric. */
    public static final int NAME = 0;

    /** display a value as a number only, possibly preceeded by a
     * negative sign and possibly followed by a percentage sign or
     * other unit qualifier. Examples: 1.09, -65% */
    public static final int SHORT  = 1;

    /** display a value succintly, but rather than using a numeric sign,
     * interpret the value.  Examples: 3 days behind schedule, 45% over
     * budget */
    public static final int MEDIUM = 2;

    /** return a complete sentence explaining and interpreting the value.
     * Examples: 40% of the total work has been accomplished. */
    public static final int FULL   = 3;

    /** Return the String ID for the metric */
    public static final int METRIC_ID = -1;


    private static String NAME_HEADING =
        resources.getString("Metrics.Column_Heading.Name");
    private static String SHORT_HEADING =
        resources.getString("Metrics.Column_Heading.Short");
    private static String MEDIUM_HEADING =
        resources.getString("Metrics.Column_Heading.Medium");
    private static String FULL_HEADING =
        resources.getString("Metrics.Column_Heading.Full");

    public String getColumnName(int columnIndex) {
        switch (columnIndex) {
        case NAME:   return NAME_HEADING;
        case SHORT:  return SHORT_HEADING;
        case MEDIUM: return MEDIUM_HEADING;
        case FULL: default: return FULL_HEADING;
        }
    }
    public int getColumnCount() { return 4; }

    protected List metrics;
    protected ArrayList validMetrics = new ArrayList();

    public Object getValueAt(int row, int col) {
        if (row < 0 || row >= validMetrics.size()) return null;
        MetricFormatter f = (MetricFormatter) validMetrics.get(row);
        switch (col) {
        case NAME: return f.getName();
        case SHORT: return f.getShort();
        case MEDIUM: return f.getMed();
        case FULL: return f.getFull();
        case METRIC_ID: return f.getKey();
        }
        return null;
    }
    public int getRowCount() {
        return validMetrics.size();
    }


    public Class getColumnClass(int columnIndex) { return String.class; }
    public boolean isCellEditable(int row, int col) { return false; }
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {}

    EventListenerList listenerList = new EventListenerList();
    public void addTableModelListener(TableModelListener l) {
        listenerList.add(TableModelListener.class, l);
    }
    public void removeTableModelListener(TableModelListener l) {
        listenerList.remove(TableModelListener.class, l);
    }
    public void fireTableChanged(TableModelEvent e) {
        Object [] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==TableModelListener.class) {
                if (e == null)
                    e = new TableModelEvent(this);
                ((TableModelListener)listeners[i+1]).tableChanged(e);
            }
        }
    }


    public void saveToXML(StringBuffer result) {
        result
            .append( " tpt='").append(totalPlanTime)
            .append("' evt='").append(earnedValueTime)
            .append("' at='").append(actualTime)
            .append("' pt='").append(planTime)
            .append("' it='").append(indirectTime)
            .append("' start='").append(EVSchedule.saveDate(startDate))
            .append("' eff='").append(EVSchedule.saveDate(currentDate))
            .append("'");
    }
    public void saveIntervalsToXML(StringBuffer result) {
        saveIntervalsToXML(result, false);
    }
    public void saveIntervalsToXML(StringBuffer result, boolean whitespace) {
        saveOneIntervalToXml(costInterval, "costInterval", result, whitespace);
        saveOneIntervalToXml(timeErrInterval, "timeErrInterval", result, whitespace);
    }
    private void saveOneIntervalToXml(ConfidenceInterval interval,
            String intervalName, StringBuffer result, boolean whitespace) {
        if (interval instanceof AbstractConfidenceInterval) {
            if (whitespace)
                result.append("    ");
            ((AbstractConfidenceInterval) interval).saveToXML(intervalName,
                    result);
            if (whitespace)
                result.append("\n");
        }
    }

    public void loadFromXML(Element e) {
        totalPlanTime   = EVSchedule.getXMLNum(e, "tpt");
        earnedValueTime = EVSchedule.getXMLNum(e, "evt");
        actualTime      = EVSchedule.getXMLNum(e, "at");
        planTime        = EVSchedule.getXMLNum(e, "pt");
        indirectTime    = EVSchedule.getXMLNum(e, "it");
        startDate       = EVSchedule.getXMLDate(e, "start");
        currentDate     = EVSchedule.getXMLDate(e, "eff");

        costInterval = AbstractConfidenceInterval.readFromXML
            (e.getElementsByTagName("costInterval"));
        if (costInterval != null)
            costInterval.setInput(totalPlanTime - earnedValueTime);
        timeErrInterval = AbstractConfidenceInterval.readFromXML
            (e.getElementsByTagName("timeErrInterval"));
    }
}
