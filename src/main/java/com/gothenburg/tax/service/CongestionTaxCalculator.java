package com.gothenburg.tax.service;

import com.gothenburg.tax.config.TaxRuleLoader;
import com.gothenburg.tax.model.TaxRuleConfig;
import com.gothenburg.tax.model.TaxRuleConfig.TimeRange;
import com.gothenburg.tax.model.VehicleType;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Core congestion tax calculation service.
 *
 * Responsibilities:
 * - Look up the fee for a given time of day
 * - Apply the single charge rule (60-minute window)
 * - Cap the daily total at the configured maximum
 * - Determine if a vehicle type is toll-free
 */
@Service
public class CongestionTaxCalculator {

    private static final DateTimeFormatter DATETIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final TaxRuleLoader ruleLoader;
    private final TollFreeDateService tollFreeDateService;

    public CongestionTaxCalculator(TaxRuleLoader ruleLoader, TollFreeDateService tollFreeDateService) {
        this.ruleLoader = ruleLoader;
        this.tollFreeDateService = tollFreeDateService;
    }

    /**
     * Calculate the total congestion tax for a vehicle given a list of passage timestamps.
     *
     * @param vehicleType the type of vehicle
     * @param dateStrings list of passage timestamps (format: "yyyy-MM-dd HH:mm:ss")
     * @param city        the city whose rules to apply (defaults to "gothenburg")
     * @return map with "totalTax" and "taxByDate" breakdown
     */
    public TaxResult calculate(VehicleType vehicleType, List<String> dateStrings, String city) {
        TaxRuleConfig rules = ruleLoader.getRules(city);
        if (rules == null) {
            throw new IllegalArgumentException("No tax rules found for city: " + city);
        }

        // Check if vehicle is toll-free
        if (rules.getTollFreeVehicleSet().contains(vehicleType)) {
            return new TaxResult(0, Map.of(), true);
        }

        // Parse and sort all timestamps
        List<LocalDateTime> passages = dateStrings.stream()
                .map(s -> LocalDateTime.parse(s.trim(), DATETIME_FORMAT))
                .sorted()
                .toList();

        // Group passages by date
        Map<LocalDate, List<LocalDateTime>> byDate = passages.stream()
                .collect(Collectors.groupingBy(LocalDateTime::toLocalDate, TreeMap::new, Collectors.toList()));

        int totalTax = 0;
        Map<String, Integer> taxByDate = new LinkedHashMap<>();

        for (Map.Entry<LocalDate, List<LocalDateTime>> entry : byDate.entrySet()) {
            List<LocalDateTime> dayPassages = entry.getValue();

            // Check if the date itself is toll-free (using first passage of the day)
            if (tollFreeDateService.isTollFreeDate(dayPassages.get(0), rules)) {
                taxByDate.put(entry.getKey().toString(), 0);
                continue;
            }

            int dailyTax = calculateDailyTax(dayPassages, rules);
            taxByDate.put(entry.getKey().toString(), dailyTax);
            totalTax += dailyTax;
        }

        return new TaxResult(totalTax, taxByDate, false);
    }

    /**
     * Calculate the tax for a single day, applying the single charge rule and daily cap.
     */
    int calculateDailyTax(List<LocalDateTime> sortedPassages, TaxRuleConfig rules) {
        int dailyTotal = 0;
        int windowMaxFee = 0;
        LocalDateTime windowStart = null;

        for (LocalDateTime passage : sortedPassages) {
            int fee = getTollFee(passage.toLocalTime(), rules);

            if (windowStart == null) {
                // Start first window
                windowStart = passage;
                windowMaxFee = fee;
            } else {
                long minutesDiff = ChronoUnit.MINUTES.between(windowStart, passage);

                if (minutesDiff <= rules.getSingleChargeWindowMinutes()) {
                    // Within the same window — track the highest fee
                    windowMaxFee = Math.max(windowMaxFee, fee);
                } else {
                    // Window has ended — add the max fee from the previous window
                    dailyTotal += windowMaxFee;

                    // Start a new window
                    windowStart = passage;
                    windowMaxFee = fee;
                }
            }
        }

        // Don't forget the last window
        dailyTotal += windowMaxFee;

        // Apply daily cap
        return Math.min(dailyTotal, rules.getMaxDailyTax());
    }

    /**
     * Look up the toll fee for a given time of day based on configured time ranges.
     */
    int getTollFee(LocalTime time, TaxRuleConfig rules) {
        for (TimeRange range : rules.getTimeRanges()) {
            LocalTime from = range.getFromTime();
            LocalTime to = range.getToTime();

            if (to.isBefore(from)) {
                // Wraps midnight (e.g. 18:30 - 05:59)
                if (!time.isBefore(from) || !time.isAfter(to)) {
                    return range.getAmount();
                }
            } else {
                if (!time.isBefore(from) && !time.isAfter(to)) {
                    return range.getAmount();
                }
            }
        }
        return 0;
    }

    /**
     * Holds the result of a tax calculation.
     */
    public record TaxResult(int totalTax, Map<String, Integer> taxByDate, boolean tollFree) {}
}
