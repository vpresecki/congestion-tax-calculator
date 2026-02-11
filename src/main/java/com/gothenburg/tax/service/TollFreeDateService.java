package com.gothenburg.tax.service;

import com.gothenburg.tax.model.TaxRuleConfig;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Determines whether a given date/time is toll-free.
 *
 * A date is toll-free if it is:
 * - A Saturday or Sunday
 * - A public holiday (as defined in the rule config)
 * - The day before a public holiday
 * - During a toll-free month (e.g. July)
 */
@Service
public class TollFreeDateService {

    /**
     * Check if the given timestamp falls on a toll-free date.
     */
    public boolean isTollFreeDate(LocalDateTime dateTime, TaxRuleConfig rules) {
        LocalDate date = dateTime.toLocalDate();

        // Weekend check
        DayOfWeek day = date.getDayOfWeek();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            return true;
        }

        // Toll-free month (e.g. July)
        if (rules.getTollFreeMonths().contains(date.getMonthValue())) {
            return true;
        }

        // Public holiday
        if (rules.getPublicHolidaySet().contains(date)) {
            return true;
        }

        // Day before a public holiday
        LocalDate nextDay = date.plusDays(1);
        if (rules.getPublicHolidaySet().contains(nextDay)) {
            return true;
        }

        return false;
    }
}
