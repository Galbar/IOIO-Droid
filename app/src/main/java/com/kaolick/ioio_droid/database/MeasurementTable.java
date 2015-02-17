package com.kaolick.ioio_droid.database;

/**
 * Database table for measurements. Implements {@link MeasurementSchema}.
 * 
 * @author kaolick
 */
public class MeasurementTable implements MeasurementSchema
{
    public static final String TABLE_NAME_PRE_TAG = "sensor_";

    public static final String getSQLCreate(int _sensorID)
    {
	return "CREATE TABLE " + TABLE_NAME_PRE_TAG + _sensorID + " (" + ID
		+ " INTEGER PRIMARY KEY AUTOINCREMENT, " + TIMESTAMP
		+ " TEXT NOT NULL, " + VALUE + " REAL, " + UPLOADED
		+ " INTEGER" + ");";
    }

    public static final String getSQLDrop(int _sensorID)
    {
	return "DROP TABLE IF EXISTS " + TABLE_NAME_PRE_TAG + _sensorID;
    }
}