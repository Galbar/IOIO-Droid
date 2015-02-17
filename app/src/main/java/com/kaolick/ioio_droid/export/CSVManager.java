package com.kaolick.ioio_droid.export;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import android.content.Context;
import android.database.Cursor;
import android.os.Environment;
import android.widget.Toast;
import au.com.bytecode.opencsv.CSVWriter;

import com.kaolick.ioio_droid.R;
import com.kaolick.ioio_droid.database.IodDatabaseManager;

/**
 * Exports database tables as <code>*.csv</code> files to the external storage.
 * 
 * @author kaolick
 */
public class CSVManager
{
    // General attributes
    private Context mContext;
    private StorageHelper mStorageHelper;

    /**
     * Class constructor.
     * 
     * @param _context
     *            to make a {@link Toast} in case the file export failed
     */
    public CSVManager(Context _context)
    {
	this.mContext = _context;
	this.mStorageHelper = new StorageHelper();
    }

    /**
     * Saves a database table as a <code>*.csv</code> file to the external
     * storage.
     * 
     * @param _tableName
     *            The name of the database table
     * @param _filepath
     *            The file path where the file is exported to
     * @return <code>true</code> if the file export was successful,
     *         <code>false</code> otherwise.
     */
    public boolean exportDatabaseTable(String _tableName, String _filepath)
    {
	// If the external storage is writeable
	if (mStorageHelper.isExternalStorageAvailableAndWriteable())
	{
	    // Directory on external storage
	    File exportDir = new File(
		    Environment.getExternalStorageDirectory(), _filepath);

	    // In case the directory does not exist
	    if (!exportDir.exists())
	    {
		// Create directory
		exportDir.mkdirs();
	    }

	    // Create new .csv file
	    File file = new File(exportDir, _tableName + ".csv");

	    try
	    {
		// Create a new file
		file.createNewFile();

		// Create a new CSVWriter
		CSVWriter csvWriter = new CSVWriter(new FileWriter(file));

		// Cursor for the complete table with the given table name
		Cursor cursor = new IodDatabaseManager(mContext).getDb()
			.rawQuery("SELECT * FROM " + _tableName, null);

		// Write the column names
		csvWriter.writeNext(cursor.getColumnNames());

		// Write each row
		while (cursor.moveToNext())
		{
		    int numberOfColumns = cursor.getColumnCount();

		    String[] rowEntries = new String[numberOfColumns];

		    for (int i = 0; i < numberOfColumns; i++)
		    {
			rowEntries[i] = cursor.getString(i);
		    }

		    csvWriter.writeNext(rowEntries);
		}

		// Close the CSVWriter and the Cursor
		csvWriter.close();
		cursor.close();

		// Export successful
		return true;
	    }
	    catch (IOException e)
	    {
		e.printStackTrace();

		// Export failed
		return false;
	    }
	}
	else
	{
	    // Inform the user that the external storage is not available
	    Toast.makeText(mContext, R.string.toast_storage_not_available,
		    Toast.LENGTH_SHORT).show();

	    return false;
	}
    }
}