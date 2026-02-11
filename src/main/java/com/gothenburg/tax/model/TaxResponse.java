package com.gothenburg.tax.model;

import java.util.Map;

/**
 * Response payload for congestion tax calculation.
 *
 * @param vehicleType   the vehicle type used in calculation
 * @param totalTax      total tax across all days (in SEK)
 * @param taxByDate     breakdown of tax per date (date string -> SEK)
 * @param tollFree      whether the vehicle type is toll-free
 */
public record TaxResponse(
        VehicleType vehicleType,
        int totalTax,
        Map<String, Integer> taxByDate,
        boolean tollFree
) {}
