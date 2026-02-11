package com.gothenburg.tax.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

/**
 * Represents the full set of congestion tax rules for a city/year,
 * loaded from an external data store (JSON file).
 */
public class TaxRuleConfig {

    private String city;
    private int year;
    private String currency;
    private int maxDailyTax;
    private int singleChargeWindowMinutes;
    private List<String> tollFreeVehicleTypes;
    private List<Integer> tollFreeMonths;
    private List<TimeRange> timeRanges;
    private List<String> publicHolidays;

    private Set<VehicleType> tollFreeVehicleSet;
    private Set<LocalDate> publicHolidaySet;

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public int getMaxDailyTax() { return maxDailyTax; }
    public void setMaxDailyTax(int maxDailyTax) { this.maxDailyTax = maxDailyTax; }

    public int getSingleChargeWindowMinutes() { return singleChargeWindowMinutes; }
    public void setSingleChargeWindowMinutes(int singleChargeWindowMinutes) {
        this.singleChargeWindowMinutes = singleChargeWindowMinutes;
    }

    public List<String> getTollFreeVehicleTypes() { return tollFreeVehicleTypes; }
    public void setTollFreeVehicleTypes(List<String> tollFreeVehicleTypes) {
        this.tollFreeVehicleTypes = tollFreeVehicleTypes;
        this.tollFreeVehicleSet = null; // invalidate cache
    }

    public List<Integer> getTollFreeMonths() { return tollFreeMonths; }
    public void setTollFreeMonths(List<Integer> tollFreeMonths) { this.tollFreeMonths = tollFreeMonths; }

    public List<TimeRange> getTimeRanges() { return timeRanges; }
    public void setTimeRanges(List<TimeRange> timeRanges) { this.timeRanges = timeRanges; }

    public List<String> getPublicHolidays() { return publicHolidays; }
    public void setPublicHolidays(List<String> publicHolidays) {
        this.publicHolidays = publicHolidays;
        this.publicHolidaySet = null; // invalidate cache
    }

    public Set<VehicleType> getTollFreeVehicleSet() {
        if (tollFreeVehicleSet == null && tollFreeVehicleTypes != null) {
            tollFreeVehicleSet = new java.util.HashSet<>();
            for (String type : tollFreeVehicleTypes) {
                tollFreeVehicleSet.add(VehicleType.valueOf(type));
            }
        }
        return tollFreeVehicleSet;
    }

    public Set<LocalDate> getPublicHolidaySet() {
        if (publicHolidaySet == null && publicHolidays != null) {
            publicHolidaySet = new java.util.HashSet<>();
            for (String date : publicHolidays) {
                publicHolidaySet.add(LocalDate.parse(date));
            }
        }
        return publicHolidaySet;
    }

    /**
     * A time range with its associated tax amount.
     */
    public static class TimeRange {
        private String from;
        private String to;
        private int amount;

        public String getFrom() { return from; }
        public void setFrom(String from) { this.from = from; }

        public String getTo() { return to; }
        public void setTo(String to) { this.to = to; }

        public int getAmount() { return amount; }
        public void setAmount(int amount) { this.amount = amount; }

        public LocalTime getFromTime() { return LocalTime.parse(from); }
        public LocalTime getToTime() { return LocalTime.parse(to); }
    }
}
