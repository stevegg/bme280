package com.wellmadesoftware.bme280.utils;

import org.rrd4j.ConsolFun;
import org.rrd4j.DsType;
import org.rrd4j.core.*;
import org.rrd4j.graph.RrdGraph;
import org.rrd4j.graph.RrdGraphDef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.GregorianCalendar;

import static org.rrd4j.DsType.*;
import static org.rrd4j.ConsolFun.*;

/**
 * Created by Steve Goyette on 6/7/2017.
 */
public class RRDatabase {

    private static final Logger logger = LoggerFactory.getLogger(RRDatabase.class);

    private static final String RRD_PATH = "/home/sgoyette/data";

    private static final long HOUR_MILLIS = 60 * 60 * 1000;
    private static final long DAY_MILLIS = HOUR_MILLIS * 24;
    private static final long MONTH_MILLIS = DAY_MILLIS * 30;

    private RrdDef rrdDef;
    private RrdDb rrdDb;
    private String datasourceName;
    private String databaseName;

    public RRDatabase( String databaseName, String datasourceName, double minValue, double maxValue ) {
        logger.debug("Creating RRD Database {}/{} with datasource name {}", RRD_PATH, databaseName, datasourceName );
        this.datasourceName = datasourceName;
        this.databaseName = databaseName;
        // We'll record a value every 60 seconds
        rrdDef = new RrdDef(String.format("%s/%s", RRD_PATH, databaseName), 60);
        // Starting time is June 1, 2017
        rrdDef.setStartTime(new GregorianCalendar(2017, 5, 1));
        // Our Datasource name is Temperature and it's a GAUGE type.  If we don't
        // get a value within 24 seconds of the 60 second measurement interval then
        // we'll use a non-measured value.  Valid values from -30 to +40
        rrdDef.addDatasource(datasourceName, DsType.GAUGE, 24, minValue, maxValue);
        // Store 1 Value every minute for 60 minutes.  This will give us our min and max over the last hour
        rrdDef.addArchive(AVERAGE, 0.5, 1, 60);
        // Store 1 Value every hour for 24 hours.  This will give us our min and max over the last 24 hours
        rrdDef.addArchive(AVERAGE, 0.5, 60, 24);
        // Store 1 Value every day for 30 days.  This will give us our min and max over the last day
        rrdDef.addArchive(AVERAGE, 0.5, 1440, 30);

    }

    public void open() throws DBException {

        try {
            rrdDb = new RrdDb(rrdDef);
        } catch (IOException e) {
            e.printStackTrace();
            throw new DBException(e);
        }
    }

    public void close() throws DBException {
        if ( rrdDb != null && !rrdDb.isClosed() ) {
            try {
                rrdDb.close();
            } catch (IOException e) {
                e.printStackTrace();
                throw new DBException(e);
            }
        }
    }

    public void writeValue(double value) throws DBException {
        try {
            if ( rrdDb == null || rrdDb.isClosed()) {
                open();
            }
            try {
                Sample sample = rrdDb.createSample();
                sample.setValues(value);
            } catch (IOException e) {
                e.printStackTrace();
                throw new DBException(e);
            }

        } finally {
            close();
        }
    }

    public void createGraphs(String path, Color lineColor) throws DBException {

        logger.debug("Graph creation start...");
        RrdGraphDef graphDef = new RrdGraphDef();

        try {
            logger.debug("Creating last hour graph...");
            // Last Hour
            long end = System.currentTimeMillis();
            long start = end - HOUR_MILLIS;
            graphDef.setTimeSpan(start, end);
            graphDef.datasource(datasourceName, String.format("%s/%s", RRD_PATH, databaseName), datasourceName, ConsolFun.AVERAGE);
            graphDef.line(datasourceName, lineColor, null, 2);
            graphDef.setFilename(String.format("%s/%s_hourl.gif", path, datasourceName));
            RrdGraph graph = new RrdGraph(graphDef);
            BufferedImage bi = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
            graph.render(bi.getGraphics());
            
        } catch (IOException e) {
            e.printStackTrace();
            throw new DBException(e);
        }

        logger.debug("Graph creation complete.");
    }

}
