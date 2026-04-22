package com.val.envmonitoring.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.val.envmonitoring.dto.ForecastPoint;
import com.val.envmonitoring.dto.ForecastResponse;
import com.val.envmonitoring.model.EnvironmentData;
import com.val.envmonitoring.repository.EnvironmentDataRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

@Service
public class DataSimulationService {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final Random random = new Random();
    private final EnvironmentDataRepository repository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String openWeatherApiKey;
    private final RestTemplate restTemplate = new RestTemplate();

    public DataSimulationService(
            EnvironmentDataRepository repository,
            @Value("${openweather.api.key:}") String openWeatherApiKey
    ) {
        this.repository = repository;
        this.openWeatherApiKey = openWeatherApiKey;
    }

    public double fetchPollutionFromAPI(String city) {
        String normalizedCity = normalizeCity(city);

        try {
            if (openWeatherApiKey == null || openWeatherApiKey.isBlank()) {
                throw new IllegalStateException("OpenWeather API key is not configured");
            }

            Coordinates coordinates = getCoordinates(normalizedCity);

            String url = "http://api.openweathermap.org/data/2.5/air_pollution"
                    + "?lat=" + coordinates.lat()
                    + "&lon=" + coordinates.lon()
                    + "&appid=" + openWeatherApiKey;

            Map response = restTemplate.getForObject(url, Map.class);
            Map main = (Map) ((Map) ((java.util.List) response.get("list")).get(0)).get("main");
            int aqi = (int) main.get("aqi");

            return aqi * 25.0 + (Math.random() * 10 - 5);
        } catch (Exception e) {
            return 50 + random.nextDouble() * 50;
        }
    }

    @Transactional
    public EnvironmentData collectAndStoreData(String city) {
        String normalizedCity = normalizeCity(city);
        double basePollution = fetchPollutionFromAPI(normalizedCity);
        WeatherSnapshot weatherSnapshot = fetchWeatherSnapshot(normalizedCity);
        double pollution = basePollution + (random.nextDouble() * 10 - 5);
        double temperature = weatherSnapshot.temperature();
        double humidity = weatherSnapshot.humidity();
        double prediction = callPythonPointModel(pollution, temperature, humidity);

        EnvironmentData data = new EnvironmentData();
        data.setCity(normalizedCity);
        data.setPollution((float) Math.max(0, pollution));
        data.setTemperature((float) temperature);
        data.setHumidity((float) humidity);
        data.setPrediction(prediction);
        data.setTimestamp(LocalDateTime.now());

        return repository.save(data);
    }

    @Transactional
    public EnvironmentData getLatestData(String city) {
        return repository.findTopByCityOrderByTimestampDesc(normalizeCity(city))
                .orElseGet(() -> collectAndStoreData(city));
    }

    @Transactional(readOnly = true)
    public List<EnvironmentData> getHistory(String city) {
        return repository.findByCityOrderByTimestampAsc(normalizeCity(city));
    }

    @Transactional
    public double predictPollution(String city) {
        ForecastResponse forecast = getForecast(city, 6);
        if (forecast.getForecast().isEmpty()) {
            return getLatestData(city).getPrediction();
        }
        return forecast.getForecast().get(0).getValue();
    }

    @Transactional
    public ForecastResponse getForecast(String city, int horizon) {
        String normalizedCity = normalizeCity(city);
        int normalizedHorizon = Math.max(1, Math.min(horizon, 12));

        EnvironmentData latest = getLatestData(normalizedCity);
        List<EnvironmentData> recentHistory = repository.findTop48ByCityOrderByTimestampDesc(normalizedCity);
        recentHistory.sort(Comparator.comparing(EnvironmentData::getTimestamp));

        List<Double> pollutionSeries = new ArrayList<>();
        for (EnvironmentData item : recentHistory) {
            pollutionSeries.add((double) item.getPollution());
        }

        List<Double> forecastValues = callPythonForecastModel(pollutionSeries, normalizedHorizon);
        List<ForecastPoint> points = buildForecastPoints(latest.getTimestamp(), forecastValues);

        ForecastResponse response = new ForecastResponse();
        response.setCity(normalizedCity);
        response.setSourceSeries(pollutionSeries);
        response.setForecast(points);
        response.setCurrentPollution(latest.getPollution());
        response.setAverageForecast(points.stream().mapToDouble(ForecastPoint::getValue).average().orElse(latest.getPollution()));
        response.setPeakForecast(points.stream().mapToDouble(ForecastPoint::getValue).max().orElse(latest.getPollution()));

        String warningLevel = evaluateWarningLevel(response.getPeakForecast());
        response.setWarningLevel(warningLevel);
        response.setWarningMessage(buildWarningMessage(normalizedCity, warningLevel, response.getPeakForecast()));

        return response;
    }

    @Scheduled(initialDelay = 15000, fixedRate = 300000)
    public void collectScheduledData() {
        for (String city : List.of("beijing", "amsterdam", "newyork")) {
            collectAndStoreData(city);
        }
    }

    public double callPythonPointModel(double pollution, double temperature, double humidity) {
        try {
            String jsonInput = String.format(
                    Locale.US,
                    "{\"mode\":\"point\",\"pollution\":%.2f,\"temperature\":%.2f,\"humidity\":%.2f}",
                    pollution,
                    temperature,
                    humidity
            );

            String result = executePython(jsonInput);
            if (result == null || result.isBlank()) {
                return pollution;
            }

            JsonNode root = objectMapper.readTree(result);
            if (root.has("prediction")) {
                return root.get("prediction").asDouble(pollution);
            }
            return pollution;
        } catch (Exception e) {
            return pollution;
        }
    }

    public List<Double> callPythonForecastModel(List<Double> series, int horizon) {
        try {
            String jsonInput = objectMapper.writeValueAsString(Map.of(
                    "mode", "forecast",
                    "series", series,
                    "horizon", horizon
            ));

            String result = executePython(jsonInput);
            if (result == null || result.isBlank()) {
                return fallbackForecast(series, horizon);
            }

            JsonNode root = objectMapper.readTree(result);
            JsonNode forecastNode = root.get("forecast");
            if (forecastNode == null || !forecastNode.isArray()) {
                return fallbackForecast(series, horizon);
            }

            List<Double> values = new ArrayList<>();
            for (JsonNode node : forecastNode) {
                values.add(Math.max(0, node.asDouble()));
            }

            if (values.isEmpty()) {
                return fallbackForecast(series, horizon);
            }
            return values;
        } catch (Exception e) {
            return fallbackForecast(series, horizon);
        }
    }

    private String executePython(String jsonInput) throws Exception {
        ProcessBuilder pb = new ProcessBuilder("python", "predict.py", jsonInput);
        pb.redirectErrorStream(true);

        Process process = pb.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

        String line;
        String result = "";
        while ((line = reader.readLine()) != null) {
            result = line;
        }

        process.waitFor();
        return result;
    }

    private List<Double> fallbackForecast(List<Double> series, int horizon) {
        if (series.isEmpty()) {
            return Collections.nCopies(horizon, 50.0);
        }

        double last = series.get(series.size() - 1);
        double slope = 0;
        if (series.size() > 1) {
            slope = (series.get(series.size() - 1) - series.get(series.size() - 2)) * 0.6;
        }

        List<Double> values = new ArrayList<>();
        for (int i = 1; i <= horizon; i++) {
            values.add(Math.max(0, last + slope * i));
        }
        return values;
    }

    private List<ForecastPoint> buildForecastPoints(LocalDateTime latestTimestamp, List<Double> values) {
        LocalDateTime baseTime = latestTimestamp != null ? latestTimestamp : LocalDateTime.now();
        List<ForecastPoint> points = new ArrayList<>();

        for (int i = 0; i < values.size(); i++) {
            LocalDateTime pointTime = baseTime.plusHours(i + 1L);
            points.add(new ForecastPoint(TIMESTAMP_FORMAT.format(pointTime), values.get(i)));
        }

        return points;
    }

    private String evaluateWarningLevel(double peakForecast) {
        if (peakForecast >= 120) {
            return "HIGH";
        }
        if (peakForecast >= 80) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private String buildWarningMessage(String city, String warningLevel, double peakForecast) {
        String formattedCity = city.substring(0, 1).toUpperCase(Locale.ROOT) + city.substring(1);
        return switch (warningLevel) {
            case "HIGH" -> formattedCity + " is likely to face unhealthy pollution levels soon. Peak forecast: "
                    + String.format(Locale.US, "%.2f", peakForecast);
            case "MEDIUM" -> formattedCity + " may experience elevated pollution later today. Peak forecast: "
                    + String.format(Locale.US, "%.2f", peakForecast);
            default -> formattedCity + " is forecast to remain within a lower-risk pollution range.";
        };
    }

    private String normalizeCity(String city) {
        if (city == null || city.isBlank()) {
            return "amsterdam";
        }
        return city.trim().toLowerCase(Locale.ROOT).replace(" ", "");
    }

    private WeatherSnapshot fetchWeatherSnapshot(String city) {
        try {
            if (openWeatherApiKey == null || openWeatherApiKey.isBlank()) {
                throw new IllegalStateException("OpenWeather API key is not configured");
            }

            Coordinates coordinates = getCoordinates(city);
            String url = "https://api.openweathermap.org/data/2.5/weather"
                    + "?lat=" + coordinates.lat()
                    + "&lon=" + coordinates.lon()
                    + "&appid=" + openWeatherApiKey
                    + "&units=metric";

            Map response = restTemplate.getForObject(url, Map.class);
            Map main = (Map) response.get("main");

            double temperature = ((Number) main.get("temp")).doubleValue();
            double humidity = ((Number) main.get("humidity")).doubleValue();
            return new WeatherSnapshot(temperature, humidity);
        } catch (Exception e) {
            return new WeatherSnapshot(15 + random.nextDouble() * 15, 30 + random.nextDouble() * 50);
        }
    }

    private Coordinates getCoordinates(String city) {
        return switch (city) {
            case "beijing" -> new Coordinates(39.90, 116.40);
            case "newyork" -> new Coordinates(40.71, -74.00);
            default -> new Coordinates(52.37, 4.90);
        };
    }

    private record Coordinates(double lat, double lon) {
    }

    private record WeatherSnapshot(double temperature, double humidity) {
    }
}
