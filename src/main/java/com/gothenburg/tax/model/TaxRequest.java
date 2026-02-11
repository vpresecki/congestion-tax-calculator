package com.gothenburg.tax.model;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Request payload for calculating congestion tax.
 *
 * @param vehicleType the type of vehicle
 * @param dates       list of passage timestamps in ISO format (e.g. "2013-02-08 06:27:00")
 */
public record TaxRequest(
        @NotNull(message = "vehicleType is required")
        VehicleType vehicleType,

        @NotEmpty(message = "dates must contain at least one entry")
        List<String> dates
) {}
