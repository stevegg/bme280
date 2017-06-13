package com.wellmadesoftware.bme280.data.repository;

import com.wellmadesoftware.bme280.data.exception.DBException;
import com.wellmadesoftware.bme280.data.model.Measurement;

import java.util.List;
import java.util.UUID;

/**
 * Created by Steve Goyette on 6/12/2017.
 */
public interface MeasurementRepository {


    Measurement create(Measurement measurement) throws DBException;
    Measurement read(UUID id) throws DBException;
    Measurement update(Measurement measurement) throws DBException;
    void delete(Measurement measurement) throws DBException;

    List<Measurement> list(long start, long end);


}
