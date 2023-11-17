package raf.sk_schedule.model.schedule;

import raf.sk_schedule.api.Constants.WeekDay;
import raf.sk_schedule.exception.ScheduleException;
import raf.sk_schedule.model.location.RoomProperties;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static raf.sk_schedule.util.exporter.ScheduleExporterJSON.serializeObject;
import static raf.sk_schedule.util.format.DateTimeFormatter.*;

/**
 * This class represents a universal time slot within the scheduling component.
 */
public class ScheduleSlot {

    /**
     * Date of the time slot starts.
     */
    private Date date;

    /**
     * Time of the day when the time slot starts.
     */
    protected String startTime;

    /**
     * Time of the day when the time slot ends.
     */
    protected String endTime;

    /**
     * Duration of the time slot in minutes.
     */
    protected int duration;

    /**
     * Location (room) where the time slot is scheduled.
     */
    protected RoomProperties location;

    /**
     * Additional attributes associated with the time slot.
     */
    protected Map<String, Object> attributes;


    private ScheduleSlot(Date date, String startTime, String endTime, int duration, RoomProperties location, Map<String, Object> attributes) {
        this.date = date;

        if (startTime == null)
            throw new ScheduleException("Starting time of ScheduleSlot not defined!");

        this.startTime = startTime;

        if (duration > 0) {
            if (endTime == null) {

                this.endTime =
                        formatTime(
                                new Date(
                                        parseDateTime(
                                                formatDateTime(date) + " " + startTime).getTime()
                                                + (long) duration * 1000 * 60
                                )
                        );

            } else {
                long startTimeMills = parseDateTime(formatDate(date) + " " + this.startTime).getTime();
                long endTimeMills = parseDateTime(formatDate(date) + " " + endTime).getTime();
                if (duration == (int) (endTimeMills - startTimeMills / (1000 * 60))) {
                    this.duration = duration;
                    this.endTime = endTime;
                } else
                    throw new ScheduleException(
                            "End time and duration does not match. Based of off end time the calculated duration would be "
                                    + (int) (endTimeMills - startTimeMills / (1000 * 60)) + " minutes.");

            }
        } else if (endTime != null) {
            this.endTime = endTime;
            long startTimeMills = parseDateTime(formatDate(date) + " " + this.startTime).getTime();
            long endTimeMills = parseDateTime(formatDate(date) + " " + endTime).getTime();
            this.duration = (int) (endTimeMills - startTimeMills / (1000 * 60));
        } else
            throw new ScheduleException("End time and duration are both not defined thus the be occupied time couldn't be calculated!");


        this.location = location;
        this.attributes = attributes;
    }

    public long getStartTimeInMillis() {
        return parseDateTime(formatDateTime(this.date) + " " + startTime).getTime();
    }

    public long getEndTimeInMillis() {
        return /*|start_in_ms|*/getStartTimeInMillis()
                + /*|millSec|*/1000
                * /*|minutes|*/60
                * /*|dur_in_minutes|*/(long) duration;
    }

    /**
     * Checks if this time slot is colliding with another time slot.
     *
     * @param otherSlot The other time slot to check for collisions.
     * @return True if there's a collision, false otherwise.
     * @throws ParseException If there is an issue parsing the time slots.
     */
    public boolean isCollidingWith(ScheduleSlot otherSlot) throws ParseException {
        long start_1 = getStartTimeInMillis();
        long end_1 = getEndTimeInMillis();
        long start_2 = otherSlot.getStartTimeInMillis();
        long end_2 = otherSlot.getEndTimeInMillis();

        //  collision cases for [1] {2}
        //  1.start >= 2.start >= 2.end >= 1.end // [ {\\\\} ]
        //  1.start >= 2.start >= 1.end >= 2.end // [ {\\\\] }
        //  2.start >= 1.start >= 1.end >= 2.end // { [\\\\] }
        //  2.start >= 1.start >= 2.end >= 1.end // { [\\\\} ]

        return (start_1 <= start_2 && start_2 < end_1) || (start_2 <= start_1 && start_1 < end_2);
    }


    public void setDate(Date date) {
        this.date = date;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
        updateDuration();
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
        updateDuration();
    }

    /**
     * Since the duration value is dependent to changes in startTime and endTime field it is recalculated dynamically,
     * according to changes in time fields.
     */
    public void updateDuration() {
        long intervalStart = parseDateTime(formatDate(this.date) + " " + this.startTime).getTime();
        long intervalEnd = parseDateTime(formatDate(this.date) + " " + this.endTime).getTime();
        if (intervalStart > intervalEnd)
            throw new ScheduleException("The start time of a repetitive schedule mapper can not be after ending time!");
        this.duration = (int) ((intervalEnd - intervalStart) / (1000 * 60));
    }

    /**
     * When the duration is set to new value the end time gets recalculated based on that new value,
     * and start time remains unchanged
     */
    public void setDuration(int duration) {
        this.duration = duration;
        long intervalStart = parseDateTime(formatDate(this.date) + " " + this.startTime).getTime();
        endTime = formatTime(new Date(this.date.getTime() + (long) duration * 1000 * 60));
    }

    public void setLocation(RoomProperties location) {
        this.location = location;
    }

    public ScheduleSlot setAttribute(String attributeName, String attributeValue) {
        attributes.put(attributeName, attributeValue);
        return this;
    }


    public Date getDate() {
        return date;
    }

    public String getEndTime() {
        return endTime;
    }

    public String getStartTime() {
        return startTime;
    }

    public int getDuration() {
        return duration;
    }

    public RoomProperties getLocation() {
        return location;
    }

    public Object getAttribute(String attributesName) {
        return attributes.get(attributesName);
    }

    public boolean hasAttribute(String attributeName) {
        return attributes.containsKey(attributeName);
    }

    public Map<String, Object> getAttributes() {
        return new HashMap<>(attributes);
    }

    public WeekDay getDayOfWeek() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int dayOfWeekIndex = calendar.get(Calendar.DAY_OF_WEEK);
        return WeekDay.values()[dayOfWeekIndex];
    }


    @Override
    public String toString() {
        return "<on day:" + date
                + "> <starts at: " + startTime
                + "> <ends_at: " + endTime
                + "> <location: " + location.getName()
                + "> <properties: " + serializeObject(attributes) + ">";
    }

    public static class Builder {
        private Date date;
        String startTime;
        String endTime;
        private int duration;
        private RoomProperties location;
        private Map<String, Object> attributes;

        public Builder() {
            date = null;
            startTime = null;
            endTime = null;
            duration = 0;
            location = null;
            attributes = new HashMap<>();
        }

        public Builder setDate(Date date) {
            this.date = date;
            return this;
        }

        public Builder setStartTime(String startTime) {
            this.startTime = startTime;
            return this;
        }


        public Builder setEndTime(String endTime) {
            this.endTime = endTime;
            return this;
        }


        public Builder setDuration(int duration) {
            this.duration = duration;
            return this;
        }


        public Builder setLocation(RoomProperties location) {
            this.location = location;
            return this;
        }

        public Builder setAttribute(String attributeName, Object attributeValue) {
            attributes.put(attributeName, attributeValue);
            return this;
        }

        public Builder setAttributes(Map<String, Object> attributes) {
            this.attributes = attributes;
            return this;
        }

        public ScheduleSlot build() {
            return new ScheduleSlot(this.date, this.startTime, this.endTime, this.duration, this.location, this.attributes);
        }
    }

}