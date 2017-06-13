package com.wellmadesoftware.bme280.data.model;

import org.dizitart.no2.IndexType;
import org.dizitart.no2.objects.Id;
import org.dizitart.no2.objects.Index;
import org.dizitart.no2.objects.Indices;

import java.util.UUID;

/**
 * Created by Steve Goyette on 6/12/2017.
 */

// provides index information for ObjectRepository
@Indices({
        @Index(value = "id", type = IndexType.Unique)
})
public class Measurement {

    @Id
    private UUID id = UUID.randomUUID();
    private long timestamp = System.currentTimeMillis();
    private float temperature;
    private float humidity;
    private float pressure;
    private float cpuTemp;
    private float cpuCoreVoltage;

    public Measurement() {

    }

    public Measurement(Measurement other) {
        this.id = other.id;
        this.timestamp = other.timestamp;
        this.temperature = other.temperature;
        this.humidity = other.humidity;
        this.pressure = other.pressure;
        this.cpuTemp = other.cpuTemp;
        this.cpuCoreVoltage = other.cpuCoreVoltage;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public float getTemperature() {
        return temperature;
    }

    public void setTemperature(float temperature) {
        this.temperature = temperature;
    }

    public float getHumidity() {
        return humidity;
    }

    public void setHumidity(float humidity) {
        this.humidity = humidity;
    }

    public float getPressure() {
        return pressure;
    }

    public void setPressure(float pressure) {
        this.pressure = pressure;
    }

    public float getCpuTemp() {
        return cpuTemp;
    }

    public void setCpuTemp(float cpuTemp) {
        this.cpuTemp = cpuTemp;
    }

    public float getCpuCoreVoltage() {
        return cpuCoreVoltage;
    }

    public void setCpuCoreVoltage(float cpuCoreVoltage) {
        this.cpuCoreVoltage = cpuCoreVoltage;
    }
}
