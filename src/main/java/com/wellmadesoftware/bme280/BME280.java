package com.wellmadesoftware.bme280;

import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;

import com.pi4j.system.SystemInfo;
import com.wellmadesoftware.bme280.data.exception.DBException;
import com.wellmadesoftware.bme280.data.model.AveragingMeasurement;
import com.wellmadesoftware.bme280.data.model.Measurement;
import com.wellmadesoftware.bme280.data.repository.MeasurementRepository;
import com.wellmadesoftware.bme280.data.repository.MeasurementRepositoryImpl;
import com.wellmadesoftware.bme280.utils.EndianReaders;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.RectangleInsets;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.io.IOException;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;

public class BME280 {

	private static final Logger logger = LoggerFactory.getLogger(BME280.class);

	private static final long SLEEP_TIME = 60000; // Amount of time in milliseconds to sleep between readings

	private static final boolean verboseOutput = true;

	private final static EndianReaders.Endianness BME280_ENDIANNESS = EndianReaders.Endianness.LITTLE_ENDIAN;
	/*
	Prompt> sudo i2cdetect -y 1
			 0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f
	00:          -- -- -- -- -- -- -- -- -- -- -- -- --
	10: -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
	20: -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
	30: -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
	40: -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
	50: -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
	60: -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
	70: -- -- -- -- -- -- -- 77
	 */
	// This next addresses is returned by "sudo i2cdetect -y 1", see above.
	public final static int BME280_I2CADDR = 0x77;

	// Operating Modes
	public final static int BME280_OSAMPLE_1 = 1;
	public final static int BME280_OSAMPLE_2 = 2;
	public final static int BME280_OSAMPLE_4 = 3;
	public final static int BME280_OSAMPLE_8 = 4;
	public final static int BME280_OSAMPLE_16 = 5;

	// BME280 Registers
	public final static int BME280_REGISTER_DIG_T1 = 0x88;  // Trimming parameter registers
	public final static int BME280_REGISTER_DIG_T2 = 0x8A;
	public final static int BME280_REGISTER_DIG_T3 = 0x8C;

	public final static int BME280_REGISTER_DIG_P1 = 0x8E;
	public final static int BME280_REGISTER_DIG_P2 = 0x90;
	public final static int BME280_REGISTER_DIG_P3 = 0x92;
	public final static int BME280_REGISTER_DIG_P4 = 0x94;
	public final static int BME280_REGISTER_DIG_P5 = 0x96;
	public final static int BME280_REGISTER_DIG_P6 = 0x98;
	public final static int BME280_REGISTER_DIG_P7 = 0x9A;
	public final static int BME280_REGISTER_DIG_P8 = 0x9C;
	public final static int BME280_REGISTER_DIG_P9 = 0x9E;

	public final static int BME280_REGISTER_DIG_H1 = 0xA1;
	public final static int BME280_REGISTER_DIG_H2 = 0xE1;
	public final static int BME280_REGISTER_DIG_H3 = 0xE3;
	public final static int BME280_REGISTER_DIG_H4 = 0xE4;
	public final static int BME280_REGISTER_DIG_H5 = 0xE5;
	public final static int BME280_REGISTER_DIG_H6 = 0xE6;
	public final static int BME280_REGISTER_DIG_H7 = 0xE7;

	public final static int BME280_REGISTER_CHIPID = 0xD0;
	public final static int BME280_REGISTER_VERSION = 0xD1;
	public final static int BME280_REGISTER_SOFTRESET = 0xE0;

	public final static int BME280_REGISTER_CONTROL_HUM = 0xF2;
	public final static int BME280_REGISTER_CONTROL = 0xF4;
	public final static int BME280_REGISTER_CONFIG = 0xF5;
	public final static int BME280_REGISTER_PRESSURE_DATA = 0xF7;
	public final static int BME280_REGISTER_TEMP_DATA = 0xFA;
	public final static int BME280_REGISTER_HUMIDITY_DATA = 0xFD;

	private int dig_T1 = 0;
	private int dig_T2 = 0;
	private int dig_T3 = 0;

	private int dig_P1 = 0;
	private int dig_P2 = 0;
	private int dig_P3 = 0;
	private int dig_P4 = 0;
	private int dig_P5 = 0;
	private int dig_P6 = 0;
	private int dig_P7 = 0;
	private int dig_P8 = 0;
	private int dig_P9 = 0;

	private int dig_H1 = 0;
	private int dig_H2 = 0;
	private int dig_H3 = 0;
	private int dig_H4 = 0;
	private int dig_H5 = 0;
	private int dig_H6 = 0;

	private float tFine = 0F;

	private static boolean verbose = "true".equals(System.getProperty("bme280.debug", "false"));

	private I2CBus bus;
	private I2CDevice bme280;
	private int mode = BME280_OSAMPLE_8;

	public BME280() throws I2CFactory.UnsupportedBusNumberException {
		this(BME280_I2CADDR);
	}

	public BME280(int address) throws I2CFactory.UnsupportedBusNumberException {
		try {
			// Get i2c bus
			bus = I2CFactory.getInstance(I2CBus.BUS_1); // Depends onthe RasPI version
			if (verbose)
				System.out.println("Connected to bus. OK.");

			// Get device itself
			bme280 = bus.getDevice(address);
			if (verbose)
				System.out.println("Connected to device. OK.");

			try {
				this.readCalibrationData();
			} catch (Exception ex) {
				ex.printStackTrace();
			}

			bme280.write(BME280_REGISTER_CONTROL, (byte) 0x3F);
			tFine = 0.0f;
		} catch (IOException e) {
			System.err.println(e.getMessage());
			throw new RuntimeException(e);
		}
	}

	private int readU8(int register) throws Exception {
		return EndianReaders.readU8(this.bme280, BME280_I2CADDR, register, verbose);
	}

	private int readS8(int register) throws Exception {
		return EndianReaders.readS8(this.bme280, BME280_I2CADDR, register, verbose);
	}

	private int readU16LE(int register) throws Exception {
		return EndianReaders.readU16LE(this.bme280, BME280_I2CADDR, register, verbose);
	}

	private int readS16LE(int register) throws Exception {
		return EndianReaders.readS16LE(this.bme280, BME280_I2CADDR, register, verbose);
	}

	public void readCalibrationData() throws Exception {
		// Reads the calibration data from the IC
		dig_T1 = readU16LE(BME280_REGISTER_DIG_T1);
		dig_T2 = readS16LE(BME280_REGISTER_DIG_T2);
		dig_T3 = readS16LE(BME280_REGISTER_DIG_T3);

		dig_P1 = readU16LE(BME280_REGISTER_DIG_P1);
		dig_P2 = readS16LE(BME280_REGISTER_DIG_P2);
		dig_P3 = readS16LE(BME280_REGISTER_DIG_P3);
		dig_P4 = readS16LE(BME280_REGISTER_DIG_P4);
		dig_P5 = readS16LE(BME280_REGISTER_DIG_P5);
		dig_P6 = readS16LE(BME280_REGISTER_DIG_P6);
		dig_P7 = readS16LE(BME280_REGISTER_DIG_P7);
		dig_P8 = readS16LE(BME280_REGISTER_DIG_P8);
		dig_P9 = readS16LE(BME280_REGISTER_DIG_P9);

		dig_H1 = readU8(BME280_REGISTER_DIG_H1);
		dig_H2 = readS16LE(BME280_REGISTER_DIG_H2);
		dig_H3 = readU8(BME280_REGISTER_DIG_H3);
		dig_H6 = readS8(BME280_REGISTER_DIG_H7);

		int h4 = readS8(BME280_REGISTER_DIG_H4);
		h4 = (h4 << 24) >> 20;
		dig_H4 = h4 | (readU8(BME280_REGISTER_DIG_H5) & 0x0F);

		int h5 = readS8(BME280_REGISTER_DIG_H6);
		h5 = (h5 << 24) >> 20;
		dig_H5 = h5 | (readU8(BME280_REGISTER_DIG_H5) >> 4 & 0x0F);

		if (verbose)
			showCalibrationData();
	}

	private String displayRegister(int reg) {
		return String.format("0x%s (%d)", lpad(Integer.toHexString(reg & 0xFFFF).toUpperCase(), "0", 4), reg);
	}

	private void showCalibrationData() {
		// Displays the calibration values for debugging purposes
		System.out.println("======================");
		System.out.println("DBG: T1 = " + displayRegister(dig_T1));
		System.out.println("DBG: T2 = " + displayRegister(dig_T2));
		System.out.println("DBG: T3 = " + displayRegister(dig_T3));
		System.out.println("----------------------");
		System.out.println("DBG: P1 = " + displayRegister(dig_P1));
		System.out.println("DBG: P2 = " + displayRegister(dig_P2));
		System.out.println("DBG: P3 = " + displayRegister(dig_P3));
		System.out.println("DBG: P4 = " + displayRegister(dig_P4));
		System.out.println("DBG: P5 = " + displayRegister(dig_P5));
		System.out.println("DBG: P6 = " + displayRegister(dig_P6));
		System.out.println("DBG: P7 = " + displayRegister(dig_P7));
		System.out.println("DBG: P8 = " + displayRegister(dig_P8));
		System.out.println("DBG: P9 = " + displayRegister(dig_P9));
		System.out.println("----------------------");
		System.out.println("DBG: H1 = " + displayRegister(dig_H1));
		System.out.println("DBG: H2 = " + displayRegister(dig_H2));
		System.out.println("DBG: H3 = " + displayRegister(dig_H3));
		System.out.println("DBG: H4 = " + displayRegister(dig_H4));
		System.out.println("DBG: H5 = " + displayRegister(dig_H5));
		System.out.println("DBG: H6 = " + displayRegister(dig_H6));
		System.out.println("======================");
	}

	private int readRawTemp() throws Exception {
		// Reads the raw (uncompensated) temperature from the sensor
		int meas = mode;
		if (verbose)
			System.out.println(String.format("readRawTemp: 1 - meas=%d", meas));
		bme280.write(BME280_REGISTER_CONTROL_HUM, (byte) meas); // HUM ?
		meas = mode << 5 | mode << 2 | 1;
		if (verbose)
			System.out.println(String.format("readRawTemp: 2 - meas=%d", meas));
		bme280.write(BME280_REGISTER_CONTROL, (byte) meas);

		double sleepTime = 0.00125 + 0.0023 * (1 << mode);
		sleepTime = sleepTime + 0.0023 * (1 << mode) + 0.000575;
		sleepTime = sleepTime + 0.0023 * (1 << mode) + 0.000575;
		waitfor((long) (sleepTime * 1_000L));
		int msb = readU8(BME280_REGISTER_TEMP_DATA);
		int lsb = readU8(BME280_REGISTER_TEMP_DATA + 1);
		int xlsb = readU8(BME280_REGISTER_TEMP_DATA + 2);
		int raw = ((msb << 16) | (lsb << 8) | xlsb) >> 4;
		if (verbose)
			System.out.println("DBG: Raw Temp: " + (raw & 0xFFFF) + ", " + raw + String.format(", msb: 0x%04X lsb: 0x%04X xlsb: 0x%04X", msb, lsb, xlsb));
		return raw;
	}

	private int readRawPressure() throws Exception {
		// Reads the raw (uncompensated) pressure level from the sensor
		int msb = readU8(BME280_REGISTER_PRESSURE_DATA);
		int lsb = readU8(BME280_REGISTER_PRESSURE_DATA + 1);
		int xlsb = readU8(BME280_REGISTER_PRESSURE_DATA + 2);
		int raw = ((msb << 16) | (lsb << 8) | xlsb) >> 4;
		return raw;
	}

	private int readRawHumidity() throws Exception {
		int msb = readU8(BME280_REGISTER_HUMIDITY_DATA);
		int lsb = readU8(BME280_REGISTER_HUMIDITY_DATA + 1);
		int raw = (msb << 8) | lsb;
		return raw;
	}

	public float readTemperature() throws Exception {
		// Gets the compensated temperature in degrees celcius
		float UT = readRawTemp();
		float var1 = 0;
		float var2 = 0;
		float temp = 0.0f;

		// Read raw temp before aligning it with the calibration values
		var1 = (UT / 16384.0f - dig_T1 / 1024.0f) * (float) dig_T2;
		var2 = ((UT / 131072.0f - dig_T1 / 8192.0f) * (UT / 131072.0f - dig_T1 / 8192.0f)) * (float) dig_T3;
		tFine = (int) (var1 + var2);
		temp = (var1 + var2) / 5120.0f;
		if (verbose)
			System.out.println("DBG: Calibrated temperature = " + temp + " C");
		return temp;
	}

	public float readPressure() throws Exception {
		// Gets the compensated pressure in pascal
		int adc = readRawPressure();
		if (verbose)
			System.out.println("ADC:" + adc + ", tFine:" + tFine);
		float var1 = (tFine / 2.0f) - 64000.0f;
		float var2 = var1 * var1 * (dig_P6 / 32768.0f);
		var2 = var2 + var1 * dig_P5 * 2.0f;
		var2 = (var2 / 4.0f) + (dig_P4 * 65536.0f);
		var1 = (dig_P3 * var1 * var1 / 524288.0f + dig_P2 * var1) / 524288.0f;
		var1 = (1.0f + var1 / 32768.0f) * dig_P1;
		if (var1 == 0f)
			return 0f;
		float p = 1048576.0f - adc;
		p = ((p - var2 / 4096.0f) * 6250.0f) / var1;
		var1 = dig_P9 * p * p / 2147483648.0f;
		var2 = p * dig_P8 / 32768.0f;
		p = p + (var1 + var2 + dig_P7) / 16.0f;
		if (verbose)
			System.out.println("DBG: Pressure = " + p + " Pa");
		return p;
	}

	public float readHumidity() throws Exception {
		int adc = readRawHumidity();
		float h = tFine - 76800.0f;
		h = (adc - (dig_H4 * 64.0f + dig_H5 / 16384.8f * h)) *
						(dig_H2 / 65536.0f * (1.0f + dig_H6 / 67108864.0f * h * (1.0f + dig_H3 / 67108864.0f * h)));
		h = h * (1.0f - dig_H1 * h / 524288.0f);
		if (h > 100)
			h = 100;
		else if (h < 0)
			h = 0;
		if (verbose)
			System.out.println("DBG: Humidity = " + h);
		return h;
	}

	private int standardSeaLevelPressure = 101325;

	public void setStandardSeaLevelPressure(int standardSeaLevelPressure) {
		this.standardSeaLevelPressure = standardSeaLevelPressure;
	}

	public double readAltitude() throws Exception {
		// "Calculates the altitude in meters"
		double altitude = 0.0;
		float pressure = readPressure();
		altitude = 44330.0 * (1.0 - Math.pow(pressure / standardSeaLevelPressure, 0.1903));
		if (verbose)
			System.out.println("DBG: Altitude = " + altitude);
		return altitude;
	}

	protected static void waitfor(long howMuch) {
		try {
			Thread.sleep(howMuch);
		} catch (InterruptedException ie) {
			ie.printStackTrace();
		}
	}

	private static String lpad(String s, String with, int len) {
		String str = s;
		while (str.length() < len)
			str = with + str;
		return str;
	}

	public static void main(String[] args) throws I2CFactory.UnsupportedBusNumberException {
		final NumberFormat NF = new DecimalFormat("##00.00");
		BME280 sensor = new BME280();

		logger.debug("Creating databases...");
		MeasurementRepository repository = new MeasurementRepositoryImpl();

		while (true) {
			Measurement measurement = new Measurement();
			try {
				measurement.setTemperature(sensor.readTemperature());
			} catch (Exception ex) {
				logger.error("Failed to read temperature : {}", ex.getMessage());
				ex.printStackTrace();
			}

			try {
				measurement.setPressure(sensor.readPressure());
			} catch (Exception ex) {
				logger.error("Failed to read pressure : {}", ex.getMessage());
				ex.printStackTrace();
			}

			try {
				measurement.setHumidity(sensor.readHumidity());
			} catch (Exception ex) {
				logger.error("Failed to read humidity : {}", ex.getMessage());
				ex.printStackTrace();
			}

			try {
				measurement.setCpuTemp(SystemInfo.getCpuTemperature());
				measurement.setCpuCoreVoltage(SystemInfo.getCpuVoltage());
			} catch (InterruptedException | IOException ie) {
				logger.error("Failed to read CPU Temperature/Voltage : {}", ie.getMessage());
				ie.printStackTrace();
			}

			try {
				repository.create(measurement);
			} catch ( DBException ioe ) {
				logger.error("Failed to write measurement...");
				ioe.printStackTrace();
			}

			if ( verboseOutput ) {
				logger.debug("Temperature: {} C", NF.format(measurement.getTemperature()));
				logger.debug("Pressure   : {} hpa", NF.format(measurement.getPressure() / 100));
				//  System.out.println("Altitude   : " + NF.format(alt) + " m");
				logger.debug("Humidity   : {} %", NF.format(measurement.getHumidity()));
				// Bonus : CPU Temperature
				logger.debug("CPU Temperature   :  {}", measurement.getCpuTemp());
				logger.debug("CPU Core Voltage  :  {}", measurement.getCpuCoreVoltage());
			}

			try {
				createCharts(repository);
			} catch (IOException e) {
				logger.error("Failed to create charts : {}", e.getMessage());
				e.printStackTrace();
			}


			try {
				Thread.sleep(SLEEP_TIME);
			} catch (InterruptedException e) {
				System.err.println(e.getMessage());
			}
		}
	}

	private static void createCharts( MeasurementRepository repository) throws IOException {

		List<Measurement> data = repository.list(System.currentTimeMillis() - 3600000, System.currentTimeMillis());
		createChart(data, "Last Hour", "hourly.jpg");

		// Get data for the last 24 hours
		data = repository.list(System.currentTimeMillis() - 86400000, System.currentTimeMillis());
		// Now go through and summarize that data into hourly buckets
		Map<String, AveragingMeasurement> hourBuckets = new HashMap<>(24);
		DateTime current = new DateTime();
		for ( int i = 0; i < 24; i ++ ) {

			int currentHour = current.getHourOfDay();
			int currentDay = current.getDayOfMonth();
			int currentMonth = current.getMonthOfYear();
			int currentYear = current.getYear();
			String key = String.format("%d:%d:%d:%d", currentYear, currentMonth, currentDay, currentHour);
			hourBuckets.put(key, new AveragingMeasurement());
			current = current.minusHours(1);
		}

		for ( Measurement measurement : data ) {

			current = new DateTime(measurement.getTimestamp());
			int currentHour = current.getHourOfDay();
			int currentDay = current.getDayOfMonth();
			int currentMonth = current.getMonthOfYear();
			int currentYear = current.getYear();
			String key = String.format("%d:%d:%d:%d", currentYear, currentMonth, currentDay, currentHour);
			AveragingMeasurement bucket = hourBuckets.get(key);
			assert( bucket != null );

			bucket.addTemperature(measurement.getTemperature());
			bucket.addHumidity(measurement.getHumidity());
			bucket.addPressure(measurement.getPressure());
			bucket.addCpuTemp(measurement.getCpuTemp());
			bucket.addCpuCoreVoltage(measurement.getCpuCoreVoltage());
			bucket.incrementCount();
		}

		// Now use this to get the 24 hour chart
		data = new ArrayList<>(24);
		for ( String key : hourBuckets.keySet() ) {
			data.add(hourBuckets.get(key));
		}

		createChart(data, "Last 24 Hours", "last24hours.jpg");
	}

	private static void createChart(List<Measurement> data, String title, String filename) throws IOException {

		// Get data

		final TimeSeries series = new TimeSeries( "Temperature" );
		for ( Measurement measurement : data ) {
			Second timestamp = new Second(new Date(measurement.getTimestamp()));
			series.addOrUpdate(timestamp, measurement.getTemperature());
		}
		final XYDataset temperature=(XYDataset)new TimeSeriesCollection(series, TimeZone.getDefault());

		JFreeChart timechart = ChartFactory.createTimeSeriesChart(
				title,
				"Timestamp",
				"Value",
				temperature,
				false,
				false,
				false);

		timechart.setBackgroundPaint(Color.white);
		XYPlot plot = (XYPlot) timechart.getPlot();
		plot.setBackgroundPaint(Color.lightGray);
		plot.setDomainGridlinePaint(Color.white);
		plot.setRangeGridlinePaint(Color.white);
		plot.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 5.0));

		int width = 640;   /* Width of the image */
		int height = 480;  /* Height of the image */
		File timeChart = new File( String.format("/var/www/html/graphs/%s", filename ));
		ChartUtilities.saveChartAsJPEG( timeChart, timechart, width, height );
	}

}
