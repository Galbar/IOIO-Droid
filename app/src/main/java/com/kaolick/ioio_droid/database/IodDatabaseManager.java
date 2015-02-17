package com.kaolick.ioio_droid.database;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.widget.Toast;

import com.kaolick.ioio_droid.R;
import com.kaolick.ioio_droid.export.CSVManager;
import com.kaolick.ioio_droid.sensor.IodIOIOSensor;
import com.kaolick.ioio_droid.store.Store;
import com.kaolick.ioio_droid.ui.SettingsActivity;

/**
 * Manages all database accesses.
 * 
 * @author kaolick
 */
public class IodDatabaseManager
{
    // General attributes
    private Context mContext;
    private CSVManager mCSVManager;
    private SharedPreferences mPrefs;

    // Database
    private IodDatabase mIodDatabase;
    private SQLiteDatabase mSQLiteDatabase;

    /**
     * Get a writable {@link SQLiteDatabase}.
     * 
     * @return The <code>SQLiteDatabase</code>
     */
    public SQLiteDatabase getDb()
    {
	return mSQLiteDatabase;
    }

    /**
     * Class constructor.
     * 
     * @param _context
     *            to use for accessing the application's database,
     *            <code>SharedPreferences</code> and more...
     */
    public IodDatabaseManager(Context _context)
    {
	this.mContext = _context;
	this.mCSVManager = new CSVManager(_context);
	this.mPrefs = SettingsActivity.getPrefs((ContextWrapper) _context);
	this.mIodDatabase = new IodDatabase(_context);
	this.mSQLiteDatabase = mIodDatabase.getWritableDatabase();
    }

    // *************** Read database ***************

    /**
     * Creates a formatted <code>JSON</code> string. Used as request body for
     * uploading locations to Xively.
     * 
     * @param _cursor
     *            A <code>Cursor</code> with the location rows
     * @return the <code>JSON</code> string.
     */
    public String createJSONMessageForLocations(Cursor _cursor)
    {
	// The cursor with the measured value
	Cursor result = _cursor;

	// The JSON message for all locations of the given cursor that have not
	// been uploaded yet
	String message = "";

	try
	{
	    // If the cursor is not empty...
	    if (result.getCount() > 0)
	    {
		// Create the JSONObject for the location attributes
		JSONObject locationAttributes = new JSONObject();

		// Put the location attributes to the JSONObject
		try
		{
		    locationAttributes.put("lon", result.getDouble(result
			    .getColumnIndex(LocationSchema.LONGITUDE)));
		    locationAttributes.put("lat", result.getDouble(result
			    .getColumnIndex(LocationSchema.LATITUDE)));
		    locationAttributes.put("ele", String.valueOf(result
			    .getDouble(result
				    .getColumnIndex(LocationSchema.ALTITUDE))));
		    locationAttributes.put("disposition", "mobile");
		}
		catch (JSONException _exception)
		{
		    throw new RuntimeException(_exception);
		}

		// Create the outer JSONObject
		JSONObject location = new JSONObject();
		try
		{
		    location.put("location", locationAttributes);
		}
		catch (JSONException _exception)
		{
		    throw new RuntimeException(_exception);
		}

		// Turn the outer JSONObject into a string
		message = location.toString();
	    }
	}
	finally
	{
	    // If the cursor is not closed yet...
	    if (result != null && !result.isClosed())
	    {
		// Close the cursor
		result.close();
	    }
	}

	return message;
    }

    /**
     * Creates a formatted <code>JSON</code> string. Used as request body for
     * uploading measurements to Xively.
     * 
     * @param _cursor
     *            A <code>Cursor</code> with the measurement rows
     * @param _datastream
     *            The datastream for which the request body will be used
     * @return the <code>JSON</code> string.
     */
    public String createJSONMessageForMeasurements(Cursor _cursor,
						   String _datastream)
    {
	// The cursor with the measured values
	Cursor result = _cursor;

	// The JSON message for all measurements of the given cursor that have
	// not been uploaded yet
	String message = "";

	try
	{
	    if (result.getCount() > 0)
	    {
		// Move to the first row
		result.moveToFirst();

		// Create a JSONArray for the datapoints
		JSONArray datapointsArray = new JSONArray();

		// Get the values from the database
		do
		{
		    // Create a new JSONObject for each row that looks like the
		    // this: {"at":"2013-06-27T20:15:00Z","value":"1.08"}
		    JSONObject datapoint = new JSONObject();

		    try
		    {
			// Add the value
			datapoint
				.put("value",
					String.valueOf(result.getDouble(result
						.getColumnIndex(MeasurementSchema.VALUE))));

			// Add the timestamp
			datapoint.put("at", result.getString(result
				.getColumnIndex(MeasurementSchema.TIMESTAMP)));
		    }
		    catch (JSONException _exception)
		    {
			throw new RuntimeException(_exception);
		    }

		    // Add a datapoint to the JSONArray
		    datapointsArray.put(datapoint);
		}
		while (result.moveToNext());

		// Create a JSONObject for a datastream
		JSONObject datastream = new JSONObject();

		// Add datapoints and further attributes to the datastream
		try
		{
		    datastream.put("datapoints", datapointsArray);
		    datastream.put("id", _datastream);
		}
		catch (JSONException _exception)
		{
		    throw new RuntimeException(_exception);
		}

		// Create a JSONArray for the datastreams
		JSONArray datastreams = new JSONArray();

		// Add the datastream to the datastreams array
		datastreams.put(datastream);

		// Create the outer JSONObject for the message
		JSONObject messageObject = new JSONObject();

		// Add the datastreams array to the message object
		try
		{
		    messageObject.put("datastreams", datastreams);
		}
		catch (JSONException _exception)
		{
		    throw new RuntimeException(_exception);
		}

		// Turn the JSONObject into a String
		message = messageObject.toString();
	    }
	}
	finally
	{
	    // If the cursor is not closed yet...
	    if (result != null && !result.isClosed())
	    {
		// Close the cursor
		result.close();
	    }
	}

	return message;
    }

    /**
     * Checks whether the given datastream is already used by another sensor.
     * 
     * @param _datastream
     * @param _sensorID
     * @return <code>true</code> if the given datastream is already used by
     *         another sensor saved in the database than the one with the given
     *         sensor ID, <code>false</code> otherwise.
     */
    public boolean datastreamInUse(String _datastream, int _sensorID)
    {
	// Return value
	boolean inUse = false;

	// The SQL query: SELECT the sensor ID for the given datastream
	String sqlQuery = "SELECT " + SensorSchema.SENSOR_ID + " FROM "
		+ SensorTable.TABLE_NAME + " WHERE " + SensorSchema.DATASTREAM
		+ "=" + "'" + _datastream + "'";

	// The query's result cursor
	Cursor result = null;

	try
	{
	    // Make the SQL query
	    result = mSQLiteDatabase.rawQuery(sqlQuery, null);

	    // If the cursor is not empty...
	    if (result.getCount() > 0)
	    {
		result.moveToFirst();

		int selectedSensorID = result.getInt(result
			.getColumnIndex(SensorSchema.SENSOR_ID));

		if (selectedSensorID != _sensorID)
		{
		    // Inform the user
		    Toast.makeText(
			    mContext,
			    mContext.getResources().getString(
				    R.string.toast_datastream_in_use)
				    + " " + selectedSensorID + "!",
			    Toast.LENGTH_LONG).show();

		    inUse = true;
		}
	    }

	    // Close the cursor
	    result.close();
	}
	finally
	{
	    // If the cursor is not closed yet...
	    if (result != null && !result.isClosed())
	    {
		// Close the cursor
		result.close();
	    }
	}

	return inUse;
    }

    /**
     * Exports all database tables to the external storage.
     */
    public void exportAllDatabaseTables()
    {
	// Prepare toast in case export fails
	Toast exportFailedToast = Toast.makeText(mContext,
		R.string.toast_export_failed, Toast.LENGTH_SHORT);

	// Export the IOIOSensor configuration table
	if (!exportIOIOSensorConfigTable())
	{
	    // If file export failed, inform the user and break
	    exportFailedToast.show();

	    return;
	}

	// Export the location data table
	if (!exportLocationData())
	{
	    // If file export failed, inform the user and break
	    exportFailedToast.show();

	    return;
	}

	// Export the measurements tables
	List<Integer> sensorIDs = IodDatabase.getIOIOSensorIDs(getDb());
	if (!sensorIDs.isEmpty())
	{
	    for (int i = 0; i < sensorIDs.size(); i++)
	    {
		// If file export failed, inform the user and break
		if (!mCSVManager.exportDatabaseTable(
			MeasurementTable.TABLE_NAME_PRE_TAG + sensorIDs.get(i),
			Store.FOLDER_FILE_PATH))
		{
		    exportFailedToast.show();

		    return;
		}
	    }
	}

	// Export was successful, inform the user
	Toast.makeText(mContext, R.string.toast_export_success,
		Toast.LENGTH_LONG).show();
    }

    /**
     * Exports the IOIOSensor configuration table to the external storage.
     * 
     * @return <code>true</code> if the file export was successful,
     *         <code>false</code> otherwise.
     */
    public boolean exportIOIOSensorConfigTable()
    {
	return mCSVManager.exportDatabaseTable(SensorTable.TABLE_NAME,
		Store.FOLDER_FILE_PATH);
    }

    /**
     * Exports the location data table to the external storage.
     * 
     * @return <code>true</code> if the file export was successful,
     *         <code>false</code> otherwise.
     */
    public boolean exportLocationData()
    {
	return mCSVManager.exportDatabaseTable(LocationTable.TABLE_NAME,
		Store.FOLDER_FILE_PATH);
    }

    /**
     * Exports the measurements table for the sensor with the given sensor ID to
     * the external storage.
     * 
     * @return <code>true</code> if the file export was successful,
     *         <code>false</code> otherwise.
     */
    public boolean exportMeasurements(int _sensorID)
    {
	return mCSVManager.exportDatabaseTable(
		MeasurementTable.TABLE_NAME_PRE_TAG + _sensorID,
		Store.FOLDER_FILE_PATH);
    }

    /**
     * Returns a {@link List} of all active {@link IodIOIOSensor}s from the
     * database.
     * 
     * @return the list of <code>IodIOIOSensor</code>s.
     */
    public List<IodIOIOSensor> getActiveSensors()
    {
	// New empty list for all active sensors
	List<IodIOIOSensor> activeSensors = new ArrayList<IodIOIOSensor>();

	// The SQL query: SELECT all active sensors saved in the database
	String sqlQuery = "SELECT * FROM " + SensorTable.TABLE_NAME + " WHERE "
		+ SensorSchema.STATE + "=" + IodIOIOSensor.STATE_ACTIVE;

	// The query's result cursor
	Cursor result = null;

	try
	{
	    // Make the SQL query
	    result = mSQLiteDatabase.rawQuery(sqlQuery, null);

	    // If the result is not empty...
	    if (result.getCount() > 0)
	    {
		// Get the sensor and add it to the list
		while (result.moveToNext())
		{
		    // Get sensor attributes from the database
		    int sensorID = result.getInt(result
			    .getColumnIndex(SensorSchema.SENSOR_ID));
		    String name = result.getString(result
			    .getColumnIndex(SensorSchema.NAME));
		    int pinNumber = result.getInt(result
			    .getColumnIndex(SensorSchema.PIN_NUMBER));
		    int frequency = result.getInt(result
			    .getColumnIndex(SensorSchema.FREQUENCY));
		    int timeUnit = result.getInt(result
			    .getColumnIndex(SensorSchema.TIME_UNIT));
		    int inputType = result.getInt(result
			    .getColumnIndex(SensorSchema.INPUT_TYPE));
		    int measurementType = result.getInt(result
			    .getColumnIndex(SensorSchema.MEASUREMENT_TYPE));
		    double threshold = result.getDouble(result
			    .getColumnIndex(SensorSchema.THRESHOLD));
		    int thresholdType = result.getInt(result
			    .getColumnIndex(SensorSchema.THRESHOLD_TYPE));
		    int state = result.getInt(result
			    .getColumnIndex(SensorSchema.STATE));
		    int useXively = result.getInt(result
			    .getColumnIndex(SensorSchema.USE_XIVELY));
		    String datastream = result.getString(result
			    .getColumnIndex(SensorSchema.DATASTREAM));

		    // Create sensor
		    IodIOIOSensor sensor = new IodIOIOSensor();

		    // Set the sensor's attributes
		    sensor.setSensorID(sensorID);
		    sensor.setName(name);
		    sensor.setPinNumber(pinNumber);
		    sensor.setFrequency(frequency);
		    sensor.setTimeUnit(timeUnit);
		    sensor.setInputType(inputType);
		    sensor.setMeasurementType(measurementType);
		    sensor.setThreshold(threshold);
		    sensor.setThresholdType(thresholdType);
		    sensor.setState(state);
		    sensor.setUseXively(useXively);
		    sensor.setDatastream(datastream);

		    // Add the sensor to the list
		    activeSensors.add(sensor);
		}
	    }

	    // Close the cursor
	    result.close();
	}
	finally
	{
	    // If the cursor is not closed yet...
	    if (result != null && !result.isClosed())
	    {
		// Close the cursor
		result.close();
	    }
	}

	return activeSensors;
    }

    /**
     * Gets the device's last known location from the datase.
     * 
     * @return a {@link Cursor} with the coordinates.
     */
    public Cursor getLastLocation()
    {
	// The SQL query: Select all location rows in reversed order, meaning
	// latest inserted first
	String sqlQuery = "SELECT * FROM " + LocationTable.TABLE_NAME
		+ " ORDER BY " + LocationSchema.ID + " DESC";

	// The query's result cursor
	Cursor result = null;

	// Make the SQL query
	result = mSQLiteDatabase.rawQuery(sqlQuery, null);

	// Move to the last inserted row
	if (result != null)
	{
	    result.moveToFirst();
	}

	return result;
    }

    /**
     * Gets a database table with all saved {@link IodIOIOSensor}s from the
     * database.
     * 
     * @return a {@link Cursor} with the database table.
     */
    public Cursor getIOIOSensorsCursor()
    {
	// The query's result cursor
	Cursor result = null;

	// The SQL query: Select * from the IOIOSensorTable
	String sqlQuery = "SELECT * FROM " + SensorTable.TABLE_NAME;

	// Make the SQL query
	result = mSQLiteDatabase.rawQuery(sqlQuery, null);

	if (result != null)
	{
	    result.moveToFirst();
	}

	return result;
    }

    /**
     * Gets the {@link IodIOIOSensor} for the given row ID.
     * 
     * @param _id
     *            The row ID
     * @return the <code>IodIOIOSensor</code>.
     */
    public IodIOIOSensor getIOIOSensorByRowID(long _id)
    {
	// Create a new sensor
	IodIOIOSensor sensor = null;

	// The SQL query: Select * from the IOIOSensorTable for the given row ID
	String sqlQuery = "SELECT * FROM " + SensorTable.TABLE_NAME + " WHERE "
		+ SensorSchema.ID + "=" + _id;

	// The query's result cursor
	Cursor result = null;

	try
	{
	    // Make the SQL query
	    result = mSQLiteDatabase.rawQuery(sqlQuery, null);

	    // If the result is not empty...
	    if (result.getCount() > 0)
	    {
		result.moveToFirst();

		// Get sensor attributes from the database
		int sensorID = result.getInt(result
			.getColumnIndex(SensorSchema.SENSOR_ID));
		String name = result.getString(result
			.getColumnIndex(SensorSchema.NAME));
		int pinNumber = result.getInt(result
			.getColumnIndex(SensorSchema.PIN_NUMBER));
		int frequency = result.getInt(result
			.getColumnIndex(SensorSchema.FREQUENCY));
		int timeUnit = result.getInt(result
			.getColumnIndex(SensorSchema.TIME_UNIT));
		int inputType = result.getInt(result
			.getColumnIndex(SensorSchema.INPUT_TYPE));
		int measurementType = result.getInt(result
			.getColumnIndex(SensorSchema.MEASUREMENT_TYPE));
		double threshold = result.getDouble(result
			.getColumnIndex(SensorSchema.THRESHOLD));
		int thresholdType = result.getInt(result
			.getColumnIndex(SensorSchema.THRESHOLD_TYPE));
		int state = result.getInt(result
			.getColumnIndex(SensorSchema.STATE));
		int useXively = result.getInt(result
			.getColumnIndex(SensorSchema.USE_XIVELY));
		String datastream = result.getString(result
			.getColumnIndex(SensorSchema.DATASTREAM));

		// Create sensor
		sensor = new IodIOIOSensor();

		// Set the sensor's attributes
		sensor.setSensorID(sensorID);
		sensor.setName(name);
		sensor.setPinNumber(pinNumber);
		sensor.setFrequency(frequency);
		sensor.setTimeUnit(timeUnit);
		sensor.setInputType(inputType);
		sensor.setMeasurementType(measurementType);
		sensor.setThreshold(threshold);
		sensor.setThresholdType(thresholdType);
		sensor.setState(state);
		sensor.setUseXively(useXively);
		sensor.setDatastream(datastream);
	    }

	    // Close the cursor
	    result.close();
	}
	finally
	{
	    // If the cursor is not closed yet...
	    if (result != null && !result.isClosed())
	    {
		// Close the cursor
		result.close();
	    }
	}

	return sensor;
    }

    /**
     * Gets the {@link IodIOIOSensor} for the given sensor ID.
     * 
     * @param _sensorID
     *            The sensor ID
     * @return the <code>IodIOIOSensor</code>.
     */
    public IodIOIOSensor getIOIOSensorBySensorID(int _sensorID)
    {
	// Create a new sensor
	IodIOIOSensor sensor = null;

	// The SQL query: Select * from the IOIOSensorTable for the given
	// sensorID
	String sqlQuery = "SELECT * FROM " + SensorTable.TABLE_NAME + " WHERE "
		+ SensorSchema.SENSOR_ID + "=" + _sensorID;

	// The query's result cursor
	Cursor result = null;

	try
	{
	    // Make the SQL query
	    result = mSQLiteDatabase.rawQuery(sqlQuery, null);

	    // If the result is not empty...
	    if (result.getCount() > 0)
	    {
		result.moveToFirst();

		// Get sensor attributes from the database
		String name = result.getString(result
			.getColumnIndex(SensorSchema.NAME));
		int pinNumber = result.getInt(result
			.getColumnIndex(SensorSchema.PIN_NUMBER));
		int frequency = result.getInt(result
			.getColumnIndex(SensorSchema.FREQUENCY));
		int timeUnit = result.getInt(result
			.getColumnIndex(SensorSchema.TIME_UNIT));
		int inputType = result.getInt(result
			.getColumnIndex(SensorSchema.INPUT_TYPE));
		int measurementType = result.getInt(result
			.getColumnIndex(SensorSchema.MEASUREMENT_TYPE));
		double threshold = result.getDouble(result
			.getColumnIndex(SensorSchema.THRESHOLD));
		int thresholdType = result.getInt(result
			.getColumnIndex(SensorSchema.THRESHOLD_TYPE));
		int state = result.getInt(result
			.getColumnIndex(SensorSchema.STATE));
		int useXively = result.getInt(result
			.getColumnIndex(SensorSchema.USE_XIVELY));
		String datastream = result.getString(result
			.getColumnIndex(SensorSchema.DATASTREAM));

		// Create the sensor
		// sensor = new IodIOIOSensor(name, pinNumber, frequency,
		// timeUnit, inputType, measurementType, threshold,
		// thresholdType, state, useXively, datastream);

		sensor = new IodIOIOSensor();

		// Set the sensor's attributes
		sensor.setSensorID(_sensorID);
		sensor.setName(name);
		sensor.setPinNumber(pinNumber);
		sensor.setFrequency(frequency);
		sensor.setTimeUnit(timeUnit);
		sensor.setInputType(inputType);
		sensor.setMeasurementType(measurementType);
		sensor.setThreshold(threshold);
		sensor.setThresholdType(thresholdType);
		sensor.setState(state);
		sensor.setUseXively(useXively);
		sensor.setDatastream(datastream);
	    }

	    // Close the cursor
	    result.close();
	}
	finally
	{
	    // If the cursor is not closed yet...
	    if (result != null && !result.isClosed())
	    {
		// Close the cursor
		result.close();
	    }
	}

	return sensor;
    }

    /**
     * Gets a database table with the measured values of the
     * {@link IodIOIOSensor} with the given sensor ID.
     * 
     * @param _sensorID
     *            The sensor ID
     * @return a {@link Cursor} with the database table.
     */
    public Cursor getMeasurementsCursor(int _sensorID)
    {
	// The query's result cursor
	Cursor result = null;

	// The SQL query: Select * from the measured values table
	String sqlQuery = "SELECT * FROM "
		+ MeasurementTable.TABLE_NAME_PRE_TAG + _sensorID
		+ " ORDER BY " + MeasurementTable.ID + " DESC";

	// Make the SQL query
	result = mSQLiteDatabase.rawQuery(sqlQuery, null);

	return result;
    }

    /**
     * Gets a database table for all measured values for the
     * {@link IodIOIOSensor} with the given sensor ID that have not been
     * uploaded yet.
     * 
     * @param _sensorID
     *            The sensor ID
     * @return the {@link Cursor} with the database table.
     */
    public Cursor getMeasurementsForUpload(int _sensorID)
    {
	// Table name of the measurements table for the given sensor
	String tableName = MeasurementTable.TABLE_NAME_PRE_TAG + _sensorID;

	// The SQL query: Select all measurement rows that have not been
	// uploaded yet
	String sqlQuery = "SELECT * FROM " + tableName + " WHERE "
		+ MeasurementSchema.UPLOADED + "= 0";

	// The query's result cursor
	Cursor result = null;

	// Make the SQL query
	result = mSQLiteDatabase.rawQuery(sqlQuery, null);

	return result;
    }

    /**
     * Gets a {@link List} of the IDs of the given not yet uploaded coordinates
     * from the database.
     * 
     * @param _coordinates
     *            The coordinates
     * @return the <code>List</code> of IDs.
     */
    public List<Integer> getIDsOfNotYetUploadedCoordinates(Cursor _coordinates)
    {
	// Create new empty list for the IDs
	List<Integer> ids = new ArrayList<Integer>();

	// Add the IDs to the list
	while (_coordinates.moveToNext())
	{
	    int id = _coordinates.getInt(_coordinates
		    .getColumnIndex(LocationTable.ID));

	    ids.add(id);
	}

	return ids;
    }

    /**
     * Gets a {@link List} of the IDs for the given {@link Cursor} of all not
     * yet uploaded values from the database.
     * 
     * @param _measurements
     *            The measurements <code>Cursor</code>
     * @return the <code>List</code> of IDs.
     */
    public List<Integer>
	    getIDsOfNotYetUploadedMeasurements(Cursor _measurements)
    {
	// Create new empty list for the IDs
	List<Integer> ids = new ArrayList<Integer>();

	// Add the IDs to the list
	while (_measurements.moveToNext())
	{
	    int id = _measurements.getInt(_measurements
		    .getColumnIndex(MeasurementTable.ID));

	    ids.add(id);
	}

	return ids;
    }

    /**
     * Gets an {@link Array} of sensor IDs of all active {@link IodIOIOSensor}s
     * that use Xively.
     * 
     * @return the <code>Array</code>.
     */
    public Integer[] getSensorIDsOfXivelyUsingActiveSensors()
    {
	// List for the sensorIDs; set to null
	List<Integer> listOfSensorIDs = new ArrayList<Integer>();

	// The SQL query: Select all sensorIDs of sensors that use Xively
	String sqlQuery = "SELECT " + SensorSchema.SENSOR_ID + " FROM "
		+ SensorTable.TABLE_NAME + " WHERE " + SensorSchema.USE_XIVELY
		+ "=" + IodIOIOSensor.USE_XIVELY_TRUE + " AND "
		+ SensorSchema.STATE + "=" + IodIOIOSensor.STATE_ACTIVE;

	// The query's result cursor
	Cursor result = null;

	try
	{
	    // Make the SQL query
	    result = mSQLiteDatabase.rawQuery(sqlQuery, null);

	    // If the Cursor is not empty...
	    if (result.getCount() > 0)
	    {
		// Get the row's sensorID and add it to the list
		while (result.moveToNext())
		{
		    int sensorID = result.getInt(result
			    .getColumnIndex(SensorSchema.SENSOR_ID));

		    listOfSensorIDs.add(sensorID);
		}
	    }

	    // Close the cursor
	    result.close();
	}
	finally
	{
	    // If the cursor is not closed yet...
	    if (result != null && !result.isClosed())
	    {
		// Close the cursor
		result.close();
	    }
	}

	// Number of found sensorIDs
	int count = listOfSensorIDs.size();

	// Array to return
	Integer[] sensorIDs = new Integer[count];

	// Add the sensorIDs to the array
	for (int i = 0; i < count; i++)
	{
	    sensorIDs[i] = listOfSensorIDs.get(i);
	}

	return sensorIDs;
    }

    /**
     * Gets an {@link Array} of sensor IDs of all {@link IodIOIOSensor}s that
     * use Xively.
     * 
     * @return the <code>Array</code>.
     */
    public Integer[] getSensorIDsOfXivelyUsingSensors()
    {
	// List for the sensorIDs; set to null
	List<Integer> listOfSensorIDs = new ArrayList<Integer>();

	// The SQL query: Select all sensorIDs of sensors that use Xively
	String sqlQuery = "SELECT " + SensorSchema.SENSOR_ID + " FROM "
		+ SensorTable.TABLE_NAME + " WHERE " + SensorSchema.USE_XIVELY
		+ "=" + IodIOIOSensor.USE_XIVELY_TRUE;

	// The query's result cursor
	Cursor result = null;

	try
	{
	    // Make the SQL query
	    result = mSQLiteDatabase.rawQuery(sqlQuery, null);

	    // If the Cursor is not empty...
	    if (result.getCount() > 0)
	    {
		// Get the row's sensorID and add it to the list
		while (result.moveToNext())
		{
		    int sensorID = result.getInt(result
			    .getColumnIndex(SensorSchema.SENSOR_ID));

		    listOfSensorIDs.add(sensorID);
		}
	    }

	    // Close the cursor
	    result.close();
	}
	finally
	{
	    // If the cursor is not closed yet...
	    if (result != null && !result.isClosed())
	    {
		// Close the cursor
		result.close();
	    }
	}

	// Number of found sensorIDs
	int count = listOfSensorIDs.size();

	// Array to return
	Integer[] sensorIDs = new Integer[count];

	// Add the sensorIDs to the array
	for (int i = 0; i < count; i++)
	{
	    sensorIDs[i] = listOfSensorIDs.get(i);
	}

	return sensorIDs;
    }

    /**
     * Checks whether the given pin number is used by another
     * {@link IodIOIOSensor}.
     * 
     * @param _pinNumber
     *            The pin number
     * @return <code>true</code> if the given pin number is already used by
     *         another <code>IodIOIOSensor</code> that is saved in the database
     *         than the one with the given sensor ID, <code>false</code>
     *         otherwise.
     */
    public boolean isPinNumberInUse(int _pinNumber, int _sensorID)
    {
	boolean inUse = false;

	// The SQL query: SELECT sensor ID for the given pin number
	String sqlQuery = "SELECT " + SensorSchema.SENSOR_ID + " FROM "
		+ SensorTable.TABLE_NAME + " WHERE " + SensorSchema.PIN_NUMBER
		+ "=" + _pinNumber;

	// The query's result cursor
	Cursor result = null;

	try
	{
	    // Make the SQL query
	    result = mSQLiteDatabase.rawQuery(sqlQuery, null);

	    // If the Cursor is not empty...
	    if (result.getCount() > 0)
	    {
		result.moveToFirst();

		int selectedSensorID = result.getInt(result
			.getColumnIndex(SensorSchema.SENSOR_ID));

		if (selectedSensorID != _sensorID)
		{
		    // Inform the user which sensor already uses the pin number
		    Toast.makeText(
			    mContext,
			    mContext.getResources().getString(
				    R.string.toast_pin_number_in_use)
				    + " " + selectedSensorID + "!",
			    Toast.LENGTH_LONG).show();

		    inUse = true;
		}
	    }

	    // Close the cursor
	    result.close();
	}
	finally
	{
	    // If the cursor is not closed yet...
	    if (result != null && !result.isClosed())
	    {
		// Close the cursor
		result.close();
	    }
	}

	return inUse;
    }

    // *************** Write database ***************

    /**
     * Adds the given {@link IodIOIOSensor} to the {@link SensorTable}.
     * 
     * @param _sensor
     *            The <code>IodIOIOSensor</code>
     */
    public void addIOIOSensor(IodIOIOSensor _sensor)
    {
	ContentValues cv = new ContentValues();
	cv.put(SensorSchema.SENSOR_ID, _sensor.getSensorID());
	cv.put(SensorSchema.NAME, _sensor.getName());
	cv.put(SensorSchema.PIN_NUMBER, _sensor.getPinNumber());
	cv.put(SensorSchema.FREQUENCY, _sensor.getFrequency());
	cv.put(SensorSchema.TIME_UNIT, _sensor.getTimeUnit());
	cv.put(SensorSchema.INPUT_TYPE, _sensor.getInputType());
	cv.put(SensorSchema.MEASUREMENT_TYPE, _sensor.getMeasurementType());
	cv.put(SensorSchema.THRESHOLD, _sensor.getThreshold());
	cv.put(SensorSchema.THRESHOLD_TYPE, _sensor.getThresholdType());
	cv.put(SensorSchema.STATE, _sensor.getState());
	cv.put(SensorSchema.USE_XIVELY, _sensor.getUseXively());
	cv.put(SensorSchema.DATASTREAM, _sensor.getDatastream());

	mSQLiteDatabase.insert(SensorTable.TABLE_NAME, null, cv);

	createIOIOMeasurementTable(_sensor.getSensorID());
    }

    /**
     * Creates a new database table for the measurements of the
     * {@link IodIOIOSensor} with the given sensor ID. The table name is the
     * sensor ID.
     * 
     * @param _sensorID
     *            The sensor ID
     */
    public void createIOIOMeasurementTable(int _sensorID)
    {
	// Create the database table
	mSQLiteDatabase.execSQL(MeasurementTable.getSQLCreate(_sensorID));
    }

    /**
     * Deletes the {@link IodIOIOSensor} for the given sensor ID from the
     * database.
     * 
     * @param _sensorID
     *            The sensor ID
     */
    public void deleteIOIOSensor(int _sensorID)
    {
	exportMeasurements(_sensorID);

	mSQLiteDatabase.delete(SensorTable.TABLE_NAME, SensorSchema.SENSOR_ID
		+ "=?", new String[] { "" + _sensorID });
    }

    /**
     * Exports the {@link LocationTable} to the external storage and then
     * deletes it from the database.
     */
    public void deleteLocationData()
    {
	boolean exportSuccessful = false;

	// New CSVManager
	CSVManager csvm = new CSVManager(mContext);

	// Export the IOIOSensorTable
	exportSuccessful = csvm.exportDatabaseTable(LocationTable.TABLE_NAME,
		Store.FOLDER_FILE_PATH);

	// Export was successful, inform the user
	if (exportSuccessful)
	{
	    Toast.makeText(mContext, R.string.toast_export_success,
		    Toast.LENGTH_LONG).show();
	}
	// Export failed; inform the user
	else
	{
	    Toast.makeText(mContext, R.string.toast_export_failed,
		    Toast.LENGTH_SHORT).show();
	}

	mSQLiteDatabase.delete(LocationTable.TABLE_NAME, null, null);
    }

    /**
     * Deletes a measurement with the given row ID from the according database
     * table for the given sensor ID.
     * 
     * @param _rowID
     *            The row ID
     * @param _sensorID
     *            The sensor ID
     */
    public void deleteMeasurement(int _rowID, int _sensorID)
    {
	mSQLiteDatabase.delete(MeasurementTable.TABLE_NAME_PRE_TAG + _sensorID,
		MeasurementSchema.ID + "=?", new String[] { "" + _rowID });
    }

    /**
     * Marks the location for the given row ID as uploaded in the according
     * database table.
     * 
     * @param _rowID
     *            The row ID
     */
    public void markCoordinatesAsUploaded(int _rowID)
    {
	ContentValues cv = new ContentValues();
	cv.put(MeasurementSchema.UPLOADED, 1);
	mSQLiteDatabase.update(LocationTable.TABLE_NAME, cv, LocationSchema.ID
		+ "=?", new String[] { "" + _rowID });
    }

    /**
     * Marks all values for the {@link IodIOIOSensor} with the given sensor ID
     * as uploaded.
     * 
     * @param _values
     *            The values
     * @param _sensorID
     *            The sensor ID
     */
    public void markMeasurementsAsUploaded(List<Integer> _ids, int _sensorID)
    {
	// Create the table name
	String tableName = MeasurementTable.TABLE_NAME_PRE_TAG + _sensorID;

	for (int i = 0; i < _ids.size(); i++)
	{
	    int id = _ids.get(i);

	    ContentValues cv = new ContentValues();
	    cv.put(MeasurementSchema.UPLOADED, 1);
	    mSQLiteDatabase.update(tableName, cv, MeasurementSchema.ID + "=?",
		    new String[] { "" + id });
	}
    }

    /**
     * Saves a location and the according timestamp in the database.
     * 
     * @param _timestamp
     *            The timestamp
     * @param _latitude
     *            The location's latitude
     * @param _longitude
     *            The location's longitude
     */
    public void saveLocation(String _timestamp,
			     double _altitude,
			     double _latitude,
			     double _longitude)
    {
	ContentValues cv = new ContentValues();
	cv.put(LocationSchema.TIMESTAMP, _timestamp);
	cv.put(LocationSchema.ALTITUDE, _altitude);
	cv.put(LocationSchema.LATITUDE, _latitude);
	cv.put(LocationSchema.LONGITUDE, _longitude);
	cv.put(LocationSchema.UPLOADED, 0);

	mSQLiteDatabase.insert(LocationTable.TABLE_NAME, null, cv);
    }

    /**
     * Saves a measured sensor value and the according timestamp in the database
     * table with the given name.
     * 
     * @param _tableName
     *            The table name
     * @param _value
     *            The measured sensor value
     * @param _timestamp
     *            The according timestamp
     */
    public void saveMeasuredSensorValue(String _tableName,
					float _value,
					String _timestamp)
    {
	// Number of decimals for the measured values as set in the settings
	int decimals = Integer.valueOf(mPrefs.getString(mContext.getResources()
		.getString(R.string.pref_general_decimals_key), "4"));

	// Round the measured value to the specified number of decimals
	BigDecimal bd = new BigDecimal(_value).setScale(decimals,
		RoundingMode.HALF_UP);
	double roundedValue = bd.doubleValue();

	ContentValues cv = new ContentValues();
	cv.put(MeasurementSchema.TIMESTAMP, _timestamp);
	cv.put(MeasurementSchema.VALUE, roundedValue);
	cv.put(MeasurementSchema.UPLOADED, 0);

	mSQLiteDatabase.insert(_tableName, null, cv);
    }

    /**
     * Updates a {@link IodIOIOSensor} in the database.
     * 
     * @param _sensor
     *            The <code>IodIOIOSensor</code>
     */
    public void updateIOIOSensor(IodIOIOSensor _sensor)
    {
	ContentValues cv = new ContentValues();
	cv.put(SensorSchema.NAME, _sensor.getName());
	cv.put(SensorSchema.PIN_NUMBER, _sensor.getPinNumber());
	cv.put(SensorSchema.FREQUENCY, _sensor.getFrequency());
	cv.put(SensorSchema.TIME_UNIT, _sensor.getTimeUnit());
	cv.put(SensorSchema.INPUT_TYPE, _sensor.getInputType());
	cv.put(SensorSchema.MEASUREMENT_TYPE, _sensor.getMeasurementType());
	cv.put(SensorSchema.THRESHOLD, _sensor.getThreshold());
	cv.put(SensorSchema.THRESHOLD_TYPE, _sensor.getThresholdType());
	cv.put(SensorSchema.STATE, _sensor.getState());
	cv.put(SensorSchema.USE_XIVELY, _sensor.getUseXively());
	cv.put(SensorSchema.DATASTREAM, _sensor.getDatastream());

	mSQLiteDatabase.update(SensorTable.TABLE_NAME, cv,
		SensorSchema.SENSOR_ID + "=?",
		new String[] { "" + _sensor.getSensorID() });
    }

    /**
     * Updates a {@link IodIOIOSensor}'s current state for the given sensor ID
     * to a new state (from active to inactive or vice versa).
     * 
     * @param _sensorID
     *            The sensorID
     * @param _stateValue
     *            The sensor's <i>new</i> state
     */
    public void updateSensorState(int _sensorID, int _stateValue)
    {
	ContentValues cv = new ContentValues();
	cv.put(SensorSchema.STATE, _stateValue);

	mSQLiteDatabase.update(SensorTable.TABLE_NAME, cv,
		SensorSchema.SENSOR_ID + "=?", new String[] { "" + _sensorID });
    }
}