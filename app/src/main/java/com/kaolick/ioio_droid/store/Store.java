package com.kaolick.ioio_droid.store;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Stores global values and methods.
 * 
 * @author kaolick
 */
public class Store
{
    // *************** Dialog keys ***************

    public static final int DELETE_ALL_SENSORS = 1;
    public static final int DELETE_ALL_MEASUREMENTS = 2;
    public static final int DELETE_ALL_DATABASE_TABLES = 3;
    public static final int DELETE_LOCATION_DATA = 4;
    public static final int EXPORT_ALL_DATABASE_TABLES = 5;

    // *************** File path ***************

    public static final String FOLDER_FILE_PATH = "IOIO-Droid";

    // *************** Intent keys ***************

    public static final String IS_NEW_SENSOR = "isNewSensor";
    public static final String ROW_ID = "rowID";
    public static final String SENSOR = "sensor";
    public static final String SENSOR_ID = "sensorID";
    public static final String SENSOR_NAME = "sensorName";

    // *************** Logging ***************

    public static final String APP_NAME = "IOIO-Droid";

    // *************** SharedPreferences keys ***************

    public static final String MEASUREMENT_STATUS = "measurement_status";

    // *************** Timestamp ***************

    /**
     * Gets the current system time and returns it as a formatted timestamp as
     * needed for Xively, f.e. 2013-07-24T13:11:25+0200
     * 
     * @return The timestamp as string
     */
    public static String getTimestamp()
    {
	SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
		"yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault());

	return simpleDateFormat.format(System.currentTimeMillis());
    }
}