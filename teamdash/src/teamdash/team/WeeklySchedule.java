package teamdash.team;

import java.io.IOException;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TimeZone;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import teamdash.XMLUtils;


public class WeeklySchedule implements EffortCalendar {

    public static final int NO_END = Integer.MAX_VALUE;

    Date zeroDay;

    int startWeek;

    int endWeek;

    WeekData hoursPerWeek;

    Map exceptions;

    int maxExceptionWeek;


    public WeeklySchedule(Date zeroDay, int startWeek) {
        this.zeroDay = zeroDay;
        this.startWeek = startWeek;
        this.endWeek = NO_END;
        setHoursPerWeek(20);
        this.exceptions = new HashMap();
        this.maxExceptionWeek = Integer.MIN_VALUE;
    }

    public WeeklySchedule(WeeklySchedule that) {
        this.zeroDay = that.zeroDay;
        this.startWeek = that.startWeek;
        this.endWeek = that.endWeek;
        this.hoursPerWeek = that.hoursPerWeek;
        this.exceptions = new HashMap(that.exceptions);
        this.maxExceptionWeek = that.maxExceptionWeek;
    }

    public WeeklySchedule(Element xml, Date zeroDay) {
        this.zeroDay = zeroDay;

        if (xml.hasAttribute(START_WEEK_ATTR))
            setStartWeek(XMLUtils.getXMLInt(xml, START_WEEK_ATTR));
        else if (xml.hasAttribute(START_DATE_ATTR))
            setStartDate(XMLUtils.getXMLDate(xml, START_DATE_ATTR));
        else
            setStartWeek(0);

        if (xml.hasAttribute(END_WEEK_ATTR))
            setEndWeek(XMLUtils.getXMLInt(xml, END_WEEK_ATTR));
        else
            setEndWeek(NO_END);

        String attr = xml.getAttribute(HOURS_PER_WEEK_ATTR);
        double hours = 20;
        if (XMLUtils.hasValue(attr))
            try {
                hours = Double.parseDouble(attr);
            } catch (NumberFormatException nfe) {}
        setHoursPerWeek(hours);

        this.exceptions = new HashMap();
        this.maxExceptionWeek = Integer.MIN_VALUE;
        NodeList exceptionNodes = xml.getElementsByTagName(EXCEPTION_TAG);
        for (int i = 0; i < exceptionNodes.getLength(); i++) {
            Element e = (Element) exceptionNodes.item(i);
            int weekNum = XMLUtils.getXMLInt(e, WEEK_ATTR);
            double time = XMLUtils.getXMLNum(e, HOURS_ATTR);
            addException(weekNum, time);
        }
    }


    public int getStartWeek() {
        return startWeek;
    }

    public void setStartWeek(int startWeek) {
        this.startWeek = startWeek;
    }

    public Date getStartDate() {
        return weekValueToDate(startWeek);
    }

    public void setStartDate(Date d) {
        // note - this will forcefully align the start date to the nearest
        // integer week boundary.
        this.startWeek = dateToWeekValue(d);
    }

    public void setZeroDay(Date zeroDay) {
        this.zeroDay = zeroDay;
    }

    public int getEndWeek() {
        return endWeek;
    }

    public void setEndWeek(int endWeek) {
        this.endWeek = endWeek;
    }

    public Date getEndDate() {
        if (endWeek == NO_END)
            return null;
        else
            return weekValueToDate(endWeek);
    }

    public void setEndDate(Date endDate) {
        if (endDate == null)
            endWeek = NO_END;
        else
            // note - this will forcefully align the end date to the nearest
            // integer week boundary.
            endWeek = dateToWeekValue(endDate);
    }

    public double getHoursPerWeek() {
        return hoursPerWeek.getHours();
    }

    public void setHoursPerWeek(double hours) {
        hours = Math.max(hours, 0);  // disallow negative hours
        this.hoursPerWeek = new WeekData(hours, WeekData.TYPE_DEFAULT);
    }

    public boolean hasExceptions() {
        return (exceptions != null && !exceptions.isEmpty());
    }

    /** Return a week number such that all subsequent weeks are guaranteed to
     * contain the same number of hours as the given week.
     */
    public int getMaintenanceStartWeek() {
        int result = Math.max(startWeek, maxExceptionWeek+1);
        if (endWeek != NO_END)
            result = Math.max(result, endWeek);
        return result;
    }


    public WeekData getWeekData(int week) {
        if (week < startWeek) {
            if (week == startWeek - 1)
                return WeekData.WEEK_START;
            else
                return WeekData.WEEK_OUTSIDE_SCHEDULE;
        }

        if (week == endWeek)
            return WeekData.WEEK_END;
        if (week > endWeek)
            return WeekData.WEEK_OUTSIDE_SCHEDULE;

        WeekData exception = (WeekData) exceptions.get(week);
        if (exception != null)
            return exception;

        return hoursPerWeek;
    }

    public void setWeekData(int week, Object value) {
        int type = getTypeOfWeek(value);
        if (type == WeekData.TYPE_START)
            startWeek = week + 1;

        else if (type == WeekData.TYPE_END)
            endWeek = week;

        else if (value == null || "".equals(value)) {
            if (exceptions.containsKey(week))
                removeException(week);
            else
                addException(week, 0);

        } else if (value instanceof Number) {
            double hours = ((Number) value).doubleValue();
            if (eq(hours, hoursPerWeek.getHours()))
                removeException(week);
            else if (hours >= 0)
                addException(week, hours);
        }
    }

    private int getTypeOfWeek(Object obj) {
        if (obj instanceof WeekData) {
            WeekData wd = (WeekData) obj;
            return wd.getType();
        }
        return -1;
    }

    public double getEffortForDate(Date d) {
        if (d == null || d.before(zeroDay))
            return 0;

        double week = dateToDoubleWeekValue(zeroDay, d);
        int finalWeekNum = (int) week;

        double result = 0;
        for (int i = startWeek;  i < finalWeekNum;  i++)
            result += getWeekData(i).getHours();

        double weekFraction = week - finalWeekNum;
        result += getWeekData(finalWeekNum).getHours() * weekFraction;
        return result;
    }

    public Date getDateForEffort(double hours) {

        int week = getStartWeek();
        double defaultHoursPerWeek = getHoursPerWeek();

        while (hours > 0) {
            if (week > maxExceptionWeek) {
                if (defaultHoursPerWeek > 0) {
                    double remainingWeeks = hours / defaultHoursPerWeek;
                    return weekValueToDate(week + remainingWeeks);
                } else {
                    return null;
                }
            }

            double hoursThisWeek;
            WeekData exception = (WeekData) exceptions.get(week);
            if (exception != null)
                hoursThisWeek = exception.getHours();
            else
                hoursThisWeek = defaultHoursPerWeek;

            if (hours < hoursThisWeek) {
                double fractionalWeek = hours / hoursThisWeek;
                return weekValueToDate(week + fractionalWeek);
            }

            hours = hours - hoursThisWeek;
            week++;
        }

        return weekValueToDate(week);
    }

    public void addException(int weekNum, double time) {
        exceptions.put(weekNum, new WeekData(time, WeekData.TYPE_EXCEPTION));
        maxExceptionWeek = Math.max(maxExceptionWeek, weekNum);
    }

    public void removeException(int week) {
        exceptions.remove(week);
    }


    public void writeAttributes(Writer out, boolean dumpMode) throws IOException {
        // write the start date.  This is used by the dump/sync, and also used
        // by legacy WBS Editor code (which doesn't use week numbers)
        out.write(" " + START_DATE_ATTR + "='");
        Date startDate = getStartDate();
        out.write(XMLUtils.saveDate(truncToGMT(startDate)));
        if (dumpMode) {
            // write out the calendar start date
            out.write("' " + START_CALENDAR_ATTR + "='");
            out.write(CALENDAR_DATE_FMT.format(startDate));
        } else {
            // write out the schedule start week
            out.write("' " + START_WEEK_ATTR + "='");
            out.write(Integer.toString(startWeek));
        }

        // write the end week, if applicable
        if (endWeek != NO_END) {
            int week = endWeek;
            if (dumpMode) week = week - startWeek;
            out.write("' " + END_WEEK_ATTR + "='");
            out.write(Integer.toString(week));
        }

        // write the default hours per week
        out.write("' " + HOURS_PER_WEEK_ATTR + "='");
        out.write(Double.toString(hoursPerWeek.getHours()));
        out.write("'");
    }

    public void writeExceptions(Writer out, boolean dumpMode) throws IOException {
        for (Iterator i = exceptions.entrySet().iterator(); i.hasNext();) {
            Map.Entry e = (Map.Entry) i.next();
            int week = ((Integer) e.getKey()).intValue();
            if (dumpMode) {
                if (week < startWeek || week >= endWeek)
                    continue;
                week = week - startWeek;
            }

            out.write("    <" + EXCEPTION_TAG + " " + WEEK_ATTR + "='");
            out.write(Integer.toString(week));
            out.write("' " + HOURS_ATTR + "='");
            WeekData wd = (WeekData) e.getValue();
            out.write(Double.toString(wd.getHours()));
            out.write("'/>\n");
        }
    }


    private boolean eq(double a, double b) {
        return Math.abs(a - b) < 0.01;
    }

    public int dateToWeekValue(Date d) {
        return dateToWeekValue(zeroDay, d);
    }

    public static int dateToWeekValue(Date zero, Date d) {
        double weekDelta = dateToDoubleWeekValue(zero, d);
        return (int) Math.round(weekDelta);
    }

    private static double dateToDoubleWeekValue(Date zero, Date d) {
        double delta = d.getTime() - zero.getTime();
        return delta / WEEK_MILLIS;
    }

    public Date weekValueToDate(double week) {
        return weekValueToDate(zeroDay, week);
    }

    public static Date weekValueToDate(Date zero, double week) {
        return new Date(zero.getTime() + (long) (week * WEEK_MILLIS));
    }

    private static final long WEEK_MILLIS = 7l * 24 * 60 * 60 * 1000;

    private static Date truncToGMT(Date d) {
        // begin by truncating the time to midnight in the local time zone.
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0);
        // now, add the effective time zone offset to convert to GMT.
        int offset = TimeZone.getDefault().getOffset(c.getTimeInMillis());
        c.add(Calendar.MILLISECOND, offset);
        return c.getTime();
    }

    private static final DateFormat CALENDAR_DATE_FMT =
        new SimpleDateFormat("yyyy-MM-dd");


    private static final String START_DATE_ATTR = "startDate";

    private static final String START_CALENDAR_ATTR = "startCalendarDate";

    private static final String START_WEEK_ATTR = "startWeek";

    private static final String END_WEEK_ATTR = "endWeek";

    private static final String HOURS_PER_WEEK_ATTR = "hoursPerWeek";

    private static final String EXCEPTION_TAG = "scheduleException";

    private static final String WEEK_ATTR = "week";

    private static final String HOURS_ATTR = "hours";

}