package com.gothenburg.tax.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gothenburg.tax.config.TaxRuleLoader;
import com.gothenburg.tax.model.TaxRuleConfig;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.ObjectMapper;

class TollFreeDateServiceTest {

    private TollFreeDateService service;
    private TaxRuleConfig rules;

    @BeforeEach
    void setUp() throws Exception {
        service = new TollFreeDateService();
        var objectMapper = new ObjectMapper();
        var ruleLoader = new TaxRuleLoader(objectMapper);
        var field = TaxRuleLoader.class.getDeclaredField("defaultRulesResource");
        field.setAccessible(true);
        field.set(ruleLoader, new ClassPathResource("data/gothenburg-tax-rules.json"));
        ruleLoader.init();
        rules = ruleLoader.getDefaultRules();
    }

    @Test
    @DisplayName("Saturday is toll-free")
    void saturday() {
        // 2013-01-12 is Saturday
        assertTrue(service.isTollFreeDate(LocalDateTime.of(2013, 1, 12, 10, 0), rules));
    }

    @Test
    @DisplayName("Sunday is toll-free")
    void sunday() {
        // 2013-01-13 is Sunday
        assertTrue(service.isTollFreeDate(LocalDateTime.of(2013, 1, 13, 10, 0), rules));
    }

    @Test
    @DisplayName("Monday is not toll-free (unless holiday)")
    void monday() {
        // 2013-01-14 is Monday, not a holiday
        assertFalse(service.isTollFreeDate(LocalDateTime.of(2013, 1, 14, 10, 0), rules));
    }

    @Test
    @DisplayName("New Year's Day is toll-free")
    void newYearsDay() {
        assertTrue(service.isTollFreeDate(LocalDateTime.of(2013, 1, 1, 10, 0), rules));
    }

    @Test
    @DisplayName("New Year's Eve (day before holiday) is toll-free")
    void newYearsEve() {
        // Dec 31 is itself a holiday in our config
        assertTrue(service.isTollFreeDate(LocalDateTime.of(2013, 12, 31, 10, 0), rules));
    }

    @Test
    @DisplayName("Day before Maundy Thursday is toll-free")
    void dayBeforeMaundyThursday() {
        // 2013-03-28 is Maundy Thursday (holiday), so 2013-03-27 should be free
        assertTrue(service.isTollFreeDate(LocalDateTime.of(2013, 3, 27, 10, 0), rules));
    }

    @Test
    @DisplayName("July is toll-free")
    void july() {
        assertTrue(service.isTollFreeDate(LocalDateTime.of(2013, 7, 1, 10, 0), rules));
        assertTrue(service.isTollFreeDate(LocalDateTime.of(2013, 7, 15, 10, 0), rules));
        assertTrue(service.isTollFreeDate(LocalDateTime.of(2013, 7, 31, 10, 0), rules));
    }

    @Test
    @DisplayName("Regular weekday in February is not toll-free")
    void regularWeekday() {
        // 2013-02-04 is Monday
        assertFalse(service.isTollFreeDate(LocalDateTime.of(2013, 2, 4, 10, 0), rules));
    }
}
