package com.wellmadesoftware.bme280.data.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Created by Steve Goyette on 6/13/2017.
 */
public class AveragingMeasurement extends Measurement {

    private static final Logger logger = LoggerFactory.getLogger(AveragingMeasurement.class);

    private int count = 0;

    public void addTemperature(float temperature) {
        setTemperature(count>0?((getTemperature() * count) + temperature) / (count + 1) : temperature );
        /*
        Calendar cal = GregorianCalendar.getInstance();
        cal.setTimeInMillis(getTimestamp());
        logger.debug("Average Temperature for {} set to {}", cal.get(Calendar.HOUR_OF_DAY), getTemperature());
        */
    }

    public void addHumidity(float humidity) {
        setHumidity(count>0?((getHumidity() * count) + humidity) / (count + 1) : humidity );
    }

    public void addPressure(float pressure) {
        setPressure(count>0?((getPressure() * count) + pressure) / (count + 1) : pressure );
    }

    public void addCpuCoreVoltage(float cpuCoreVoltage) {
        setCpuCoreVoltage(count>0?((getCpuCoreVoltage() * count) + cpuCoreVoltage) / (count + 1) : cpuCoreVoltage );
    }

    public void addCpuTemp(float cpuTemp) {
        setCpuTemp(count>0?((getCpuTemp() * count) + cpuTemp) / (count + 1) : cpuTemp );
    }

    public void incrementCount() {
        count ++;
    }

}
