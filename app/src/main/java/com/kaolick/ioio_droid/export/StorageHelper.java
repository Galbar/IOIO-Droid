package com.kaolick.ioio_droid.export;

import android.os.Environment;

/**
 * Checks the state of the external storage of the device.
 * 
 * @author kaolick
 */
public class StorageHelper
{
    // Storage states
    private boolean externalStorageAvailable, externalStorageWriteable;

    private void checkStorage()
    {
	// Get the external storage's state
	String state = Environment.getExternalStorageState();

	if (state.equals(Environment.MEDIA_MOUNTED))
	{
	    // Storage is available and writeable
	    externalStorageAvailable = externalStorageWriteable = true;
	}
	else if (state.equals(Environment.MEDIA_MOUNTED_READ_ONLY))
	{
	    // Storage is only readable
	    externalStorageAvailable = true;
	    externalStorageWriteable = false;
	}
	else
	{
	    // Storage is neither readable nor writeable
	    externalStorageAvailable = externalStorageWriteable = false;
	}
    }

    /**
     * Checks the state of the external storage.
     * 
     * @return <code>true</code> if the external storage is available,
     *         <code>false</code> otherwise.
     */
    public boolean isExternalStorageAvailable()
    {
	checkStorage();

	return externalStorageAvailable;
    }

    /**
     * Checks the state of the external storage.
     * 
     * @return <code>true</code> if the external storage is writeable,
     *         <code>false</code> otherwise.
     */
    public boolean isExternalStorageWriteable()
    {
	checkStorage();

	return externalStorageWriteable;
    }

    /**
     * Checks the state of the external storage.
     * 
     * @return <code>true</code> if the external storage is available and
     *         writeable, <code>false</code> otherwise.
     */
    public boolean isExternalStorageAvailableAndWriteable()
    {
	checkStorage();

	if (!externalStorageAvailable)
	{
	    return false;
	}
	else if (!externalStorageWriteable)
	{
	    return false;
	}
	else
	{
	    return true;
	}
    }
}