package com.wellmadesoftware.bme280.data.repository;

import com.wellmadesoftware.bme280.data.exception.DBException;
import com.wellmadesoftware.bme280.data.model.Measurement;
import org.dizitart.no2.Nitrite;
import org.dizitart.no2.objects.Cursor;
import org.dizitart.no2.objects.ObjectRepository;

import java.util.List;
import java.util.UUID;

import static org.dizitart.no2.objects.filters.ObjectFilters.*;

/**
 * Created by Steve Goyette on 6/12/2017.
 */
public class MeasurementRepositoryImpl implements MeasurementRepository {

    private static final String DB_PATH = "/home/sgoyette/data";

    private Nitrite db;
    private ObjectRepository<Measurement> repository;

    public MeasurementRepositoryImpl()  {

        db = Nitrite.builder()
                .compressed()
                .filePath(String.format("%s/measurements.db", DB_PATH))
                .openOrCreate();

        repository = db.getRepository(Measurement.class);
    }

    @Override
    public Measurement create(Measurement measurement) throws DBException {
        repository.insert(measurement);

        db.commit();
        return measurement;
    }

    @Override
    public Measurement read(UUID id) throws DBException {
        Cursor<Measurement> cursor = repository.find(eq("id", id));
        return cursor.firstOrDefault();
    }

    @Override
    public Measurement update(Measurement measurement) throws DBException {
        return measurement;
    }

    @Override
    public void delete(Measurement measurement) throws DBException {
        if ( measurement == null || measurement.getId() == null ) {
            throw new DBException("Invalid parameter.  Measurement must not be null and must have a valid id");
        }
    }

    @Override
    public List<Measurement> list(long start, long end) {

        Cursor<Measurement> cursor = repository.find(and(
                gte("timestamp", start),
                lt("timestamp", end)
        ));

        List<Measurement> results = cursor.toList();

        return results;
    }
}
