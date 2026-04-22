package com.val.envmonitoring;

import com.val.envmonitoring.dto.ForecastResponse;
import com.val.envmonitoring.model.EnvironmentData;
import com.val.envmonitoring.service.DataSimulationService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class EnvMonitoringBackendApplicationTests {

    @Autowired
    private DataSimulationService dataSimulationService;

    @Test
    void contextLoads() {
    }

    @Test
    void storesHistoryAndBuildsForecast() {
        dataSimulationService.collectAndStoreData("beijing");
        dataSimulationService.collectAndStoreData("beijing");

        List<EnvironmentData> history = dataSimulationService.getHistory("beijing");
        ForecastResponse forecast = dataSimulationService.getForecast("beijing", 4);

        assertFalse(history.isEmpty());
        assertNotNull(forecast);
        assertFalse(forecast.getForecast().isEmpty());
    }

}
