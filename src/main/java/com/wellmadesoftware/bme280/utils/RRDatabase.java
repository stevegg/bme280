package com.wellmadesoftware.bme280.utils;

import org.rrd4j.DsType;
import org.rrd4j.core.*;
import static org.rrd4j.DsType.*;
import static org.rrd4j.ConsolFun.*;

/**
 * Created by Steve Goyette on 6/7/2017.
 */
public class RRDatabase {

    private static final String RRD_PATH = "/home/sgoyette/data/data.rrd";

    public RRDatabase() {
        RrdDef rrdDef = new RrdDef(RRD_PATH, 60);
        rrdDef.addDatasource("Temperature", DsType.GAUGE, 24, Double.NaN, Double.NaN);
        rrdDef.addArchive(AVERAGE, 0.5, 60, 600); // 1 step, 600 rows
        rrdDef.addArchive(AVERAGE, 0.5, 6, 700); // 6 steps, 700 rows
        rrdDef.addArchive(MAX, 0.5, 1, 600);
    }

}
