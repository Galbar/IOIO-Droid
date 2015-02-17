package com.kaolick.ioio_droid.database;

/**
 * Database table for locations. Implements {@link LocationSchema}.
 * 
 * @author kaolick
 */
public class LocationTable implements LocationSchema
{
    public static final String TABLE_NAME = "location";

    public static final String SQL_CREATE = "CREATE TABLE " + TABLE_NAME + " ("
	    + ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + TIMESTAMP
	    + " TEXT NOT NULL, " + ALTITUDE + " REAL, " + LATITUDE + " REAL, "
	    + LONGITUDE + " REAL, " + UPLOADED + " INTEGER" + ");";

    public static final String SQL_DROP = "DROP TABLE IF EXISTS " + TABLE_NAME;
}