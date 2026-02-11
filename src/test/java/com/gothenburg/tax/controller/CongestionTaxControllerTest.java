package com.gothenburg.tax.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gothenburg.tax.model.TaxRequest;
import com.gothenburg.tax.model.VehicleType;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class CongestionTaxControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	@DisplayName("POST /api/tax/calculate returns correct tax for a car")
	void calculateTaxForCar() throws Exception {
		TaxRequest request = new TaxRequest(
				VehicleType.CAR,
				List.of("2013-02-04 07:30:00")
		);

		mockMvc.perform(post("/api/tax/calculate")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.vehicleType").value("CAR"))
				.andExpect(jsonPath("$.totalTax").value(18))
				.andExpect(jsonPath("$.tollFree").value(false));
	}

	@Test
	@DisplayName("POST /api/tax/calculate returns 0 for toll-free vehicle")
	void calculateTaxForEmergencyVehicle() throws Exception {
		TaxRequest request = new TaxRequest(
				VehicleType.EMERGENCY,
				List.of("2013-02-04 07:30:00")
		);

		mockMvc.perform(post("/api/tax/calculate")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalTax").value(0))
				.andExpect(jsonPath("$.tollFree").value(true));
	}

	@Test
	@DisplayName("POST /api/tax/calculate returns 400 for missing vehicleType")
	void missingVehicleType() throws Exception {
		String json = """
				{
				    "dates": ["2013-02-04 07:30:00"]
				}
				""";

		mockMvc.perform(post("/api/tax/calculate")
						.contentType(MediaType.APPLICATION_JSON)
						.content(json))
				.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("POST /api/tax/calculate returns 400 for empty dates")
	void emptyDates() throws Exception {
		TaxRequest request = new TaxRequest(VehicleType.CAR, List.of());

		mockMvc.perform(post("/api/tax/calculate")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("POST /api/tax/calculate with city param")
	void withCityParam() throws Exception {
		TaxRequest request = new TaxRequest(
				VehicleType.CAR,
				List.of("2013-02-04 07:30:00")
		);

		mockMvc.perform(post("/api/tax/calculate?city=gothenburg")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalTax").value(18));
	}

	@Test
	@DisplayName("POST /api/tax/calculate with unknown city returns 400")
	void unknownCity() throws Exception {
		TaxRequest request = new TaxRequest(
				VehicleType.CAR,
				List.of("2013-02-04 07:30:00")
		);

		mockMvc.perform(post("/api/tax/calculate?city=stockholm")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("No tax rules found for city: stockholm"));
	}
}
