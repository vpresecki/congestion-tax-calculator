package com.gothenburg.tax.controller;

import com.gothenburg.tax.model.TaxRequest;
import com.gothenburg.tax.model.TaxResponse;
import com.gothenburg.tax.service.CongestionTaxCalculator;
import com.gothenburg.tax.service.CongestionTaxCalculator.TaxResult;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for congestion tax calculations.
 *
 * POST /api/tax/calculate
 *   - Body: { "vehicleType": "CAR", "dates": ["2013-02-08 06:27:00", ...] }
 *   - Optional query param: ?city=gothenburg (defaults to gothenburg)
 *
 * Returns the total tax and a per-date breakdown.
 */
@RestController
@RequestMapping("/api/tax")
public class CongestionTaxController {

    private final CongestionTaxCalculator calculator;

    public CongestionTaxController(CongestionTaxCalculator calculator) {
        this.calculator = calculator;
    }

    @PostMapping("/calculate")
    public ResponseEntity<TaxResponse> calculateTax(
            @Valid @RequestBody TaxRequest request,
            @RequestParam(defaultValue = "gothenburg") String city) {

        TaxResult result = calculator.calculate(request.vehicleType(), request.dates(), city);

        TaxResponse response = new TaxResponse(
                request.vehicleType(),
                result.totalTax(),
                result.taxByDate(),
                result.tollFree()
        );

        return ResponseEntity.ok(response);
    }
}
