package com.kaolick.ioio_droid.database;

/**
 * Database table for sensors. Implements {@link SensorSchema}.
 * 
 * @author kaolick
 */
public class SensorTable implements SensorSchema
{
    public static final String TABLE_NAME = "ioio_sensors";

    public static final String SQL_CREATE = "CREATE TABLE " + TABLE_NAME + " ("
	    + ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + SENSOR_ID
	    + " INTEGER, " + NAME + " TEXT NOT NULL, " + PIN_NUMBER
	    + " INTEGER, " + FREQUENCY + " INTEGER, " + TIME_UNIT
	    + " INTEGER, " + INPUT_TYPE + " INTEGER, " + MEASUREMENT_TYPE
	    + " INTEGER, " + THRESHOLD + " REAL, " + THRESHOLD_TYPE
	    + " INTEGER, " + STATE + " INTEGER, " + USE_XIVELY + " INTEGER, "
	    + DATASTREAM + " TEXT NOT NULL" + ");";

    public static final String SQL_DROP = "DROP TABLE IF EXISTS " + TABLE_NAME;
}