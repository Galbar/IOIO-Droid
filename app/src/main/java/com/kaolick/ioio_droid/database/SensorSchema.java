package com.kaolick.ioio_droid.database;

/**
 * Database schema for a sensor table.
 * 
 * @author kaolick
 */
public interface SensorSchema
{
    String ID = "_id";
    String SENSOR_ID = "sensor_id";
    String NAME = "name";
    String PIN_NUMBER = "pin_number";
    String FREQUENCY = "frequency";
    String TIME_UNIT = "time_unit";
    String INPUT_TYPE = "input_type";
    String MEASUREMENT_TYPE = "measurement_type";
    String THRESHOLD = "threshold";
    String THRESHOLD_TYPE = "threshold_type";
    String STATE = "state";
    String USE_XIVELY = "use_xively";
    String DATASTREAM = "datastream";
}