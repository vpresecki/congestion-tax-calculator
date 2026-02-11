package com.gothenburg.tax.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gothenburg.tax.config.TaxRuleLoader;
import com.gothenburg.tax.model.VehicleType;
import com.gothenburg.tax.service.CongestionTaxCalculator.TaxResult;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.ObjectMapper;

class CongestionTaxCalculatorTest {

	private CongestionTaxCalculator calculator;

	@BeforeEach
	void setUp() throws Exception {
		var objectMapper = new ObjectMapper();
		var ruleLoader = new TaxRuleLoader(objectMapper);
		// Use reflection to set the resource field, then init
		var field = TaxRuleLoader.class.getDeclaredField("defaultRulesResource");
		field.setAccessible(true);
		field.set(ruleLoader, new ClassPathResource("data/gothenburg-tax-rules.json"));
		ruleLoader.init();

		var tollFreeDateService = new TollFreeDateService();
		calculator = new CongestionTaxCalculator(ruleLoader, tollFreeDateService);
	}

	@Nested
	@DisplayName("Toll-free vehicle types")
	class TollFreeVehicles {

		@Test
		@DisplayName("Emergency vehicles should not be charged")
		void emergencyVehicle() {
			TaxResult result = calculator.calculate(
					VehicleType.EMERGENCY,
					List.of("2013-02-08 07:30:00"),
					"gothenburg"
			);
			assertEquals(0, result.totalTax());
			assertTrue(result.tollFree());
		}

		@Test
		@DisplayName("Motorcycles should not be charged")
		void motorcycle() {
			TaxResult result = calculator.calculate(
					VehicleType.MOTORCYCLE,
					List.of("2013-02-08 07:30:00"),
					"gothenburg"
			);
			assertEquals(0, result.totalTax());
			assertTrue(result.tollFree());
		}

		@Test
		@DisplayName("Buses should not be charged")
		void bus() {
			TaxResult result = calculator.calculate(
					VehicleType.BUS,
					List.of("2013-02-08 07:30:00"),
					"gothenburg"
			);
			assertEquals(0, result.totalTax());
			assertTrue(result.tollFree());
		}
	}

	@Nested
	@DisplayName("Toll-free dates")
	class TollFreeDates {

		@Test
		@DisplayName("Weekends should be toll-free")
		void weekend() {
			// 2013-01-12 is a Saturday
			TaxResult result = calculator.calculate(
					VehicleType.CAR,
					List.of("2013-01-12 08:00:00"),
					"gothenburg"
			);
			assertEquals(0, result.totalTax());
		}

		@Test
		@DisplayName("Public holidays should be toll-free")
		void publicHoliday() {
			// 2013-01-01 is New Year's Day
			TaxResult result = calculator.calculate(
					VehicleType.CAR,
					List.of("2013-01-01 08:00:00"),
					"gothenburg"
			);
			assertEquals(0, result.totalTax());
		}

		@Test
		@DisplayName("Days before public holidays should be toll-free")
		void dayBeforeHoliday() {
			// 2013-03-28 is a public holiday (Maundy Thursday), so 2013-03-27 should be free
			TaxResult result = calculator.calculate(
					VehicleType.CAR,
					List.of("2013-03-27 08:00:00"),
					"gothenburg"
			);
			assertEquals(0, result.totalTax());
		}

		@Test
		@DisplayName("July should be toll-free")
		void july() {
			TaxResult result = calculator.calculate(
					VehicleType.CAR,
					List.of("2013-07-15 08:00:00"),
					"gothenburg"
			);
			assertEquals(0, result.totalTax());
		}
	}

	@Nested
	@DisplayName("Time-based fee lookup")
	class TimeFees {

		@Test
		@DisplayName("06:00-06:29 should cost 8 SEK")
		void earlyMorning() {
			TaxResult result = calculator.calculate(
					VehicleType.CAR,
					List.of("2013-02-04 06:15:00"), // Monday
					"gothenburg"
			);
			assertEquals(8, result.totalTax());
		}

		@Test
		@DisplayName("07:00-07:59 should cost 18 SEK (peak)")
		void morningPeak() {
			TaxResult result = calculator.calculate(
					VehicleType.CAR,
					List.of("2013-02-04 07:30:00"),
					"gothenburg"
			);
			assertEquals(18, result.totalTax());
		}

		@Test
		@DisplayName("15:30-16:59 should cost 18 SEK (afternoon peak)")
		void afternoonPeak() {
			TaxResult result = calculator.calculate(
					VehicleType.CAR,
					List.of("2013-02-04 16:00:00"),
					"gothenburg"
			);
			assertEquals(18, result.totalTax());
		}

		@Test
		@DisplayName("Night time (18:30-05:59) should cost 0 SEK")
		void nightTime() {
			TaxResult result = calculator.calculate(
					VehicleType.CAR,
					List.of("2013-02-04 21:00:00"),
					"gothenburg"
			);
			assertEquals(0, result.totalTax());
		}
	}

	@Nested
	@DisplayName("Single charge rule (60-minute window)")
	class SingleChargeRule {

		@Test
		@DisplayName("Two passages within 60 minutes should charge only the highest fee")
		void twoPassagesWithin60Min() {
			TaxResult result = calculator.calculate(
					VehicleType.CAR,
					List.of(
							"2013-02-04 06:20:00",  // 8 SEK
							"2013-02-04 06:45:00"   // 13 SEK
					),
					"gothenburg"
			);
			// Within 60 min -> max(8, 13) = 13
			assertEquals(13, result.totalTax());
		}

		@Test
		@DisplayName("Three passages within 60 minutes should charge only the highest")
		void threePassagesWithin60Min() {
			TaxResult result = calculator.calculate(
					VehicleType.CAR,
					List.of(
							"2013-02-04 06:20:00",  // 8 SEK
							"2013-02-04 06:45:00",  // 13 SEK
							"2013-02-04 07:10:00"   // 18 SEK
					),
					"gothenburg"
			);
			// All within 60 min of 06:20 -> max(8, 13, 18) = 18
			assertEquals(18, result.totalTax());
		}

		@Test
		@DisplayName("Passages more than 60 minutes apart should be charged separately")
		void passagesOver60MinApart() {
			TaxResult result = calculator.calculate(
					VehicleType.CAR,
					List.of(
							"2013-02-04 06:20:00",  // 8 SEK
							"2013-02-04 08:00:00"   // 13 SEK
					),
					"gothenburg"
			);
			// >60 min apart -> 8 + 13 = 21
			assertEquals(21, result.totalTax());
		}
	}

	@Nested
	@DisplayName("Daily cap")
	class DailyCap {

		@Test
		@DisplayName("Total tax for one day should not exceed 60 SEK")
		void dailyCap() {
			TaxResult result = calculator.calculate(
					VehicleType.CAR,
					List.of(
							"2013-02-04 06:00:00",  // 8 SEK
							"2013-02-04 07:15:00",  // 18 SEK
							"2013-02-04 08:30:00",  // 8 SEK
							"2013-02-04 15:00:00",  // 13 SEK
							"2013-02-04 16:00:00",  // 18 SEK
							"2013-02-04 17:30:00"   // 13 SEK
					),
					"gothenburg"
			);
			// 8 + 18 + 8 + 13 + 18 + 13 = 78, capped at 60
			assertEquals(60, result.totalTax());
		}
	}

	@Nested
	@DisplayName("Post-it test dates from assignment")
	class PostItDates {

		@Test
		@DisplayName("Full post-it scenario should produce correct total")
		void fullPostItScenario() {
			TaxResult result = calculator.calculate(
					VehicleType.CAR,
					List.of(
							"2013-01-14 21:00:00",
							"2013-01-15 21:00:00",
							"2013-02-07 06:23:27",
							"2013-02-07 15:27:00",
							"2013-02-08 06:27:00",
							"2013-02-08 06:20:27",
							"2013-02-08 14:35:00",
							"2013-02-08 15:29:00",
							"2013-02-08 15:47:00",
							"2013-02-08 16:01:00",
							"2013-02-08 16:48:00",
							"2013-02-08 17:49:00",
							"2013-02-08 18:29:00",
							"2013-02-08 18:35:00",
							"2013-03-26 14:25:00",
							"2013-03-28 14:07:27"
					),
					"gothenburg"
			);

			assertFalse(result.tollFree());

			// 2013-01-14: 21:00 = 0 SEK
			assertEquals(0, result.taxByDate().get("2013-01-14"));

			// 2013-01-15: 21:00 = 0 SEK
			assertEquals(0, result.taxByDate().get("2013-01-15"));

			// 2013-02-07: 06:23 (8 SEK) + 15:27 (13 SEK) = 21 SEK
			assertEquals(21, result.taxByDate().get("2013-02-07"));

			// 2013-02-08: Complex day with single charge rule
			// Sorted: 06:20:27(8), 06:27:00(8), 14:35:00(8), 15:29:00(13),
			//         15:47:00(18), 16:01:00(18), 16:48:00(18), 17:49:00(13),
			//         18:29:00(8), 18:35:00(0)
			// Window1: 06:20:27 start, 06:27:00 (6min) within  -> max(8,8) = 8
			// Window2: 14:35:00 start, 15:29:00 (54min) within -> max(8,13) = 13
			// Window3: 15:47:00 start, 16:01:00 (14min) within, 16:48:00 (61min) NEW WINDOW -> max(18,18) = 18
			// Window4: 16:48:00 start, 17:49:00 (61min) NEW WINDOW -> 18
			// Window5: 17:49:00 start, 18:29:00 (40min) within, 18:35:00 (46min) within -> max(13,8,0) = 13
			// Total: 8 + 13 + 18 + 18 + 13 = 70, capped at 60
			assertEquals(60, result.taxByDate().get("2013-02-08"));

			// 2013-03-26: 14:25 = 8 SEK (Tuesday)
			assertEquals(8, result.taxByDate().get("2013-03-26"));

			// 2013-03-28: Public holiday (Maundy Thursday) = 0 SEK
			assertEquals(0, result.taxByDate().get("2013-03-28"));

			// Total: 0 + 0 + 21 + 60 + 8 + 0 = 89
			assertEquals(89, result.totalTax());
		}
	}
}
