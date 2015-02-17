package com.kaolick.ioio_droid.database;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * The application's database.
 * 
 * @author kaolick
 */
public class IodDatabase extends SQLiteOpenHelper
{
    // Database name & version
    public static final String DB_NAME = "iod.db";
    private static final int DB_VERSION = 1;

    /**
     * Class constructor.
     * 
     * @param _context
     */
    public IodDatabase(Context _context)
    {
	super(_context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase _db)
    {
	// Create table for sensors
	_db.execSQL(SensorTable.SQL_CREATE);

	// Create table for locations
	_db.execSQL(LocationTable.SQL_CREATE);

	// Create a table for measurements for each sensor
	List<Integer> sensorIDs = getIOIOSensorIDs(_db);
	if (!sensorIDs.isEmpty())
	{
	    for (int i = 0; i < sensorIDs.size(); i++)
	    {
		// The current sensorID
		int sensorID = sensorIDs.get(i);

		// Create the database table for the current sensorID
		_db.execSQL(MeasurementTable.getSQLCreate(sensorID));
	    }
	}
    }

    @Override
    public void onUpgrade(SQLiteDatabase _db, int _oldVersion, int _newVersion)
    {
	// Drop table for sensors
	_db.execSQL(SensorTable.SQL_DROP);

	// Drop table for locations
	_db.execSQL(LocationTable.SQL_DROP);

	// Drop all measurement tables
	List<Integer> sensorIDs = getIOIOSensorIDs(_db);
	if (!sensorIDs.isEmpty())
	{
	    for (int i = 0; i < sensorIDs.size(); i++)
	    {
		// The current sensorID
		int sensorID = sensorIDs.get(i);

		// Drop the database table for the current sensorID
		_db.execSQL(MeasurementTable.getSQLDrop(sensorID));
	    }
	}

	// Re-create tables
	onCreate(_db);
    }

    /**
     * Gets a list of all sensor IDs of all sensors that are saved in the
     * database.
     * 
     * @return The list of sensor IDs
     */
    public static List<Integer> getIOIOSensorIDs(SQLiteDatabase _db)
    {
	// Create a new list for the sensorIDs
	List<Integer> sensorIDs = new ArrayList<Integer>();

	// The SQL query: Select all sensorIDs from the IOIOSensorTable
	String sqlQuery = "SELECT " + SensorSchema.SENSOR_ID + " FROM "
		+ SensorTable.TABLE_NAME;

	// The query's result cursor
	Cursor result = null;

	try
	{
	    // Make the SQL query
	    result = _db.rawQuery(sqlQuery, null);

	    // If the result is not empty...
	    if (result.getCount() > 0)
	    {
		while (result.moveToNext())
		{
		    // Get the row's sensorID and add it to the list
		    sensorIDs.add(result.getInt(result
			    .getColumnIndex(SensorSchema.SENSOR_ID)));
		}
	    }

	    // Close the Cursor
	    result.close();
	}
	finally
	{
	    // If the Cursor is not closed yet...
	    if (!result.isClosed())
	    {
		// Close the Cursor
		result.close();
	    }
	}

	return sensorIDs;
    }
}