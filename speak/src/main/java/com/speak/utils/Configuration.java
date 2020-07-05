package com.speak.utils;

public class Configuration {

    private String seedValue;
    private Integer samplingRate;
    // TODO check with codec for android devices
    private Double bitDuration;
    private Integer spreadingFactor;
    private Double codeBitDuration;
    private Integer carrierFrequency;
    private Integer samplesPerDataBit;
    private Integer samplesPerCodeBit;

    public Configuration() {
        this.seedValue = "10000011";
        this.samplingRate = 44100;
        this.bitDuration = 0.2;
        this.spreadingFactor = 60;
        this.carrierFrequency = 18000;
        setDependentData();
    }

    public void setSeedValue(String seedValue) {
        this.seedValue = seedValue;
    }

    public void setCarrierFrequency(Integer carrierFrequency) {
        this.carrierFrequency = carrierFrequency;
    }

    void setDependentData() {
        this.codeBitDuration = this.bitDuration / this.spreadingFactor;
        this.samplesPerDataBit = (int) (bitDuration * samplingRate);
        this.samplesPerCodeBit = (int) (codeBitDuration * samplingRate);
    }

    public String getSeedValue() {
        return seedValue;
    }

    public Integer getSamplingRate() {
        return samplingRate;
    }

    public Double getBitDuration() {
        return bitDuration;
    }

    public Integer getSpreadingFactor() {
        return spreadingFactor;
    }

    public Double getCodeBitDuration() {
        return codeBitDuration;
    }

    public Integer getCarrierFrequency() {
        return carrierFrequency;
    }

    public Integer getSamplesPerDataBit() {
        return samplesPerDataBit;
    }

    public Integer getSamplesPerCodeBit() {
        return samplesPerCodeBit;
    }
}
