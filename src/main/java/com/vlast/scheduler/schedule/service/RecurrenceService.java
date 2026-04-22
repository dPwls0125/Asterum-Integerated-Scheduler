package com.vlast.scheduler.schedule.service;

import com.vlast.scheduler.schedule.entity.EndType;
import com.vlast.scheduler.schedule.entity.RecurrenceGroup;
import com.vlast.scheduler.schedule.entity.RecurrenceType;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

/**
 * Dynamically expands RecurrenceGroup rules into concrete instance dates
 * for a given date range. This avoids storing individual rows per occurrence.
 */
@Service
public class RecurrenceService {

    /**
     * Generate all instance dates for a recurrence group within [rangeStart, rangeEnd].
     */
    public List<LocalDate> expandInstances(RecurrenceGroup group, LocalDate rangeStart, LocalDate rangeEnd) {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate cursor = group.getStartDate();
        int count = 0;
        int maxCount = group.getEndType() == EndType.COUNT && group.getEndCount() != null
                ? group.getEndCount() : Integer.MAX_VALUE;
        LocalDate hardEnd = group.getEndType() == EndType.UNTIL_DATE && group.getEndDate() != null
                ? group.getEndDate() : LocalDate.of(2099, 12, 31);

        // For WEEKLY, align cursor to the correct day of week
        if (group.getRecurrenceType() == RecurrenceType.WEEKLY && group.getDayOfWeek() != null) {
            DayOfWeek targetDay = DayOfWeek.of(group.getDayOfWeek());
            if (cursor.getDayOfWeek() != targetDay) {
                cursor = cursor.with(TemporalAdjusters.nextOrSame(targetDay));
            }
        }

        while (!cursor.isAfter(hardEnd) && !cursor.isAfter(rangeEnd) && count < maxCount) {
            LocalDate instanceDate = cursor;

            // For MONTHLY, handle months with fewer days
            if (group.getRecurrenceType() == RecurrenceType.MONTHLY && group.getDayOfMonth() != null) {
                int targetDay = group.getDayOfMonth();
                int lastDay = cursor.lengthOfMonth();
                instanceDate = cursor.withDayOfMonth(Math.min(targetDay, lastDay));
            }

            if (!instanceDate.isBefore(rangeStart) && !instanceDate.isAfter(rangeEnd)) {
                dates.add(instanceDate);
            }

            count++;
            cursor = advanceCursor(cursor, group);

            // Safety: break if cursor didn't advance (shouldn't happen)
            if (cursor.equals(instanceDate) || cursor.isBefore(instanceDate)) {
                break;
            }
        }

        return dates;
    }

    private LocalDate advanceCursor(LocalDate current, RecurrenceGroup group) {
        return switch (group.getRecurrenceType()) {
            case DAILY -> current.plusDays(1);
            case WEEKLY -> current.plusWeeks(1);
            case MONTHLY -> current.plusMonths(1);
        };
    }
}
