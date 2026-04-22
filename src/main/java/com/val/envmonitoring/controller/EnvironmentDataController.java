package com.val.envmonitoring.controller;

import com.val.envmonitoring.dto.ForecastResponse;
import com.val.envmonitoring.model.EnvironmentData;
import com.val.envmonitoring.service.DataSimulationService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/data")
@CrossOrigin(origins = "*")
public class EnvironmentDataController {

    private final DataSimulationService service;

    public EnvironmentDataController(DataSimulationService service) {
        this.service = service;
    }

    @GetMapping
    public EnvironmentData getData(@RequestParam String city) {
        return service.collectAndStoreData(city);
    }

    @PostMapping("/collect")
    public EnvironmentData collectData(@RequestParam String city) {
        return service.collectAndStoreData(city);
    }

    @GetMapping("/latest")
    public EnvironmentData getLatest(@RequestParam String city) {
        return service.getLatestData(city);
    }

    @GetMapping("/history")
    public List<EnvironmentData> getHistory(@RequestParam String city) {
        return service.getHistory(city);
    }

    @GetMapping("/forecast")
    public ForecastResponse getForecast(
            @RequestParam(defaultValue = "amsterdam") String city,
            @RequestParam(defaultValue = "6") int horizon
    ) {
        return service.getForecast(city, horizon);
    }

    @GetMapping("/predict")
    public double predictPollution(@RequestParam(defaultValue = "amsterdam") String city) {
        return service.predictPollution(city);
    }
}
