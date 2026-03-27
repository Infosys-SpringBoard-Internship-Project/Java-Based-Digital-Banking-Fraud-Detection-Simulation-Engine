package com.example.infosys_project.dto;

import java.util.ArrayList;
import java.util.List;

public class SimulationRequest {
    private Integer volume = 100;
    private Integer fraudPercentage = 13;
    private Integer normalPercentage = 75;
    private Integer mediumPercentage = 12;
    private List<String> scenarios = new ArrayList<>();
    private Double amountMin = 500.0;
    private Double amountMax = 100000.0;
    private String executionMode = "INSTANT";
    private Integer durationMinutes = 1;

    public Integer getVolume() { return volume; }
    public void setVolume(Integer volume) { this.volume = volume; }
    public Integer getFraudPercentage() { return fraudPercentage; }
    public void setFraudPercentage(Integer fraudPercentage) { this.fraudPercentage = fraudPercentage; }
    public Integer getNormalPercentage() { return normalPercentage; }
    public void setNormalPercentage(Integer normalPercentage) { this.normalPercentage = normalPercentage; }
    public Integer getMediumPercentage() { return mediumPercentage; }
    public void setMediumPercentage(Integer mediumPercentage) { this.mediumPercentage = mediumPercentage; }
    public List<String> getScenarios() { return scenarios; }
    public void setScenarios(List<String> scenarios) { this.scenarios = scenarios; }
    public Double getAmountMin() { return amountMin; }
    public void setAmountMin(Double amountMin) { this.amountMin = amountMin; }
    public Double getAmountMax() { return amountMax; }
    public void setAmountMax(Double amountMax) { this.amountMax = amountMax; }
    public String getExecutionMode() { return executionMode; }
    public void setExecutionMode(String executionMode) { this.executionMode = executionMode; }
    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
}
