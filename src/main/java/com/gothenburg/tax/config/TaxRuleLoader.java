package com.gothenburg.tax.config;

import com.gothenburg.tax.model.TaxRuleConfig;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Loads tax rule configurations from external JSON files.
 * Supports multiple cities — each city has its own rule file.
 *
 * For the bonus scenario, this could be ex * For the bonus scenario, this could be extended to load from a database,tended to load from a database,
 * remote API, or file system path outside the application.
 */
@Component
public class TaxRuleLoader {

    private static final Logger log = LoggerFactory.getLogger(TaxRuleLoader.class);

    private final ObjectMapper objectMapper;
    private final Map<String, TaxRuleConfig> rulesByCity = new ConcurrentHashMap<>();

    @Value("${tax.rules.path:classpath:data/gothenburg-tax-rules.json}")
    private Resource defaultRulesResource;

    public TaxRuleLoader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() throws IOException {
        loadRules(defaultRulesResource);
        log.info("Loaded tax rules for {} city/cities: {}", rulesByCity.size(), rulesByCity.keySet());
    }

    /**
     * Load rules from a Spring Resource (classpath or file).
     */
    public void loadRules(Resource resource) throws IOException {
        try (InputStream is = resource.getInputStream()) {
            TaxRuleConfig config = objectMapper.readValue(is, TaxRuleConfig.class);
            rulesByCity.put(config.getCity().toLowerCase(), config);
            log.info("Loaded tax rules for city: {}", config.getCity());
        }
    }

    /**
     * Get the tax rules for a given city.
     *
     * @param city city name (case-insensitive)
     * @return the rule config, or null if not found
     */
    public TaxRuleConfig getRules(String city) {
        return rulesByCity.get(city.toLowerCase());
    }

    /**
     * Get the default (Gothenburg) rules.
     */
    public TaxRuleConfig getDefaultRules() {
        return rulesByCity.get("gothenburg");
    }
}
