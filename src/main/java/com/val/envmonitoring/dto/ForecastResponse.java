package com.val.envmonitoring.dto;

import java.util.List;

public class ForecastResponse {

    private String city;
    private List<Double> sourceSeries;
    private List<ForecastPoint> forecast;
    private double currentPollution;
    private double averageForecast;
    private double peakForecast;
    private String warningLevel;
    private String warningMessage;

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public List<Double> getSourceSeries() {
        return sourceSeries;
    }

    public void setSourceSeries(List<Double> sourceSeries) {
        this.sourceSeries = sourceSeries;
    }

    public List<ForecastPoint> getForecast() {
        return forecast;
    }

    public void setForecast(List<ForecastPoint> forecast) {
        this.forecast = forecast;
    }

    public double getCurrentPollution() {
        return currentPollution;
    }

    public void setCurrentPollution(double currentPollution) {
        this.currentPollution = currentPollution;
    }

    public double getAverageForecast() {
        return averageForecast;
    }

    public void setAverageForecast(double averageForecast) {
        this.averageForecast = averageForecast;
    }

    public double getPeakForecast() {
        return peakForecast;
    }

    public void setPeakForecast(double peakForecast) {
        this.peakForecast = peakForecast;
    }

    public String getWarningLevel() {
        return warningLevel;
    }

    public void setWarningLevel(String warningLevel) {
        this.warningLevel = warningLevel;
    }

    public String getWarningMessage() {
        return warningMessage;
    }

    public void setWarningMessage(String warningMessage) {
        this.warningMessage = warningMessage;
    }
}
