package com.kaolick.ioio_droid.ui;

import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.widget.Toast;

import com.kaolick.ioio_droid.R;
import com.kaolick.ioio_droid.database.IodDatabase;
import com.kaolick.ioio_droid.database.IodDatabaseManager;
import com.kaolick.ioio_droid.database.LocationSchema;
import com.kaolick.ioio_droid.database.LocationTable;
import com.kaolick.ioio_droid.database.MeasurementTable;
import com.kaolick.ioio_droid.database.SensorTable;
import com.kaolick.ioio_droid.export.CSVManager;
import com.kaolick.ioio_droid.store.Store;
import com.kaolick.ioio_droid.xively.UploadManager;

/**
 * {@link PreferenceFragment} for the {@link SettingsActivity}.
 * 
 * @author kaolick
 */
public class SettingsFragment extends PreferenceFragment
{
    // General attributes
    private Context mContext;
    private CSVManager mCSVManager;
    private IodDatabaseManager mDatabaseManager;
    private SettingsActivity mSettingsActivity;
    private UploadManager mUploadManager;

    // Preferences
    private Preference mAboutPref, mDeleteDatabasePref, mDeleteLocationsPref,
	    mExportDatabasePref, mExportLocationsPref, mShowLocationPref,
	    mUploadMeasurementsPref, mUploadLocationPref;

    @Override
    public void onCreate(Bundle _savedInstanceState)
    {
	super.onCreate(_savedInstanceState);

	// Initialize attributes
	mSettingsActivity = (SettingsActivity) getActivity();
	mContext = mSettingsActivity;
	mCSVManager = new CSVManager(mContext);
	mDatabaseManager = new IodDatabaseManager(mContext);
	mUploadManager = new UploadManager(mContext, mSettingsActivity, false);

	// Load the preferences from an XML resource
	addPreferencesFromResource(R.xml.preferences);

	// Initialize Preferences
	initPrefs();
    }

    // *************** Database ***************

    /**
     * Exports all database tables to the external storage.
     */
    public void exportAllDatabaseTables()
    {
	boolean exportSuccessful = true;

	// Export the IOIOSensorTable
	exportSuccessful = mCSVManager.exportDatabaseTable(
		SensorTable.TABLE_NAME, Store.FOLDER_FILE_PATH);

	// If file export failed, inform the user and break
	if (!exportSuccessful)
	{
	    Toast.makeText(mContext, R.string.toast_export_failed,
		    Toast.LENGTH_SHORT).show();

	    return;
	}

	// Export location table
	exportSuccessful = mCSVManager.exportDatabaseTable(
		LocationTable.TABLE_NAME, Store.FOLDER_FILE_PATH);

	// If file export failed, inform the user and break
	if (!exportSuccessful)
	{
	    Toast.makeText(mContext, R.string.toast_export_failed,
		    Toast.LENGTH_SHORT).show();

	    return;
	}

	// Export each sensor's measurement table
	List<Integer> sensorIDs = IodDatabase.getIOIOSensorIDs(mDatabaseManager
		.getDb());
	if (!sensorIDs.isEmpty())
	{
	    for (int i = 0; i < sensorIDs.size(); i++)
	    {
		exportSuccessful = mCSVManager.exportDatabaseTable(
			MeasurementTable.TABLE_NAME_PRE_TAG + sensorIDs.get(i),
			Store.FOLDER_FILE_PATH);

		// If file export failed, inform the user and break
		if (!exportSuccessful)
		{
		    Toast.makeText(mContext, R.string.toast_export_failed,
			    Toast.LENGTH_SHORT).show();

		    return;
		}
	    }
	}

	// Export was successful, inform the user
	if (exportSuccessful)
	{
	    Toast.makeText(mContext, R.string.toast_export_success,
		    Toast.LENGTH_LONG).show();
	}
    }

    // *************** Preferences ***************

    /**
     * Initializes the Preferences.
     */
    public void initPrefs()
    {
	Resources res = getResources();

	// Find Preferences
	mAboutPref = (Preference) findPreference(res
		.getString(R.string.pref_general_about_key));
	mDeleteDatabasePref = (Preference) findPreference(res
		.getString(R.string.pref_database_delete_key));
	mDeleteLocationsPref = (Preference) findPreference(res
		.getString(R.string.pref_location_delete_key));
	mExportDatabasePref = (Preference) findPreference(res
		.getString(R.string.pref_database_export_key));
	mExportLocationsPref = (Preference) findPreference(res
		.getString(R.string.pref_location_export_key));
	mShowLocationPref = (Preference) findPreference(res
		.getString(R.string.pref_location_show_last_location_key));
	mUploadMeasurementsPref = (Preference) findPreference(res
		.getString(R.string.pref_xively_upload_measurements_key));
	mUploadLocationPref = (Preference) findPreference(res
		.getString(R.string.pref_xively_upload_location_key));

	// Set OnClickListener to Preferences
	mAboutPref.setOnPreferenceClickListener(new OnPreferenceClickListener()
	{
	    @Override
	    public boolean onPreferenceClick(Preference preference)
	    {
		startActivity(new Intent(mContext, AboutActivity.class));

		return true;
	    }
	});
	mDeleteDatabasePref
		.setOnPreferenceClickListener(new OnPreferenceClickListener()
		{
		    @Override
		    public boolean onPreferenceClick(Preference preference)
		    {
			mSettingsActivity.showDeleteDatabaseDialog();

			return true;
		    }
		});
	mDeleteLocationsPref
		.setOnPreferenceClickListener(new OnPreferenceClickListener()
		{
		    @Override
		    public boolean onPreferenceClick(Preference preference)
		    {
			mSettingsActivity.showDeleteLocationDataDialog();

			return true;
		    }
		});
	mExportDatabasePref
		.setOnPreferenceClickListener(new OnPreferenceClickListener()
		{
		    @Override
		    public boolean onPreferenceClick(Preference preference)
		    {
			mDatabaseManager.exportAllDatabaseTables();

			return true;
		    }
		});
	mExportLocationsPref
		.setOnPreferenceClickListener(new OnPreferenceClickListener()
		{
		    @Override
		    public boolean onPreferenceClick(Preference _preference)
		    {
			// Export was successful; inform the user
			if (mDatabaseManager.exportLocationData())
			{
			    Toast.makeText(mContext,
				    R.string.toast_export_success,
				    Toast.LENGTH_LONG).show();
			}
			// Export failed; inform the user
			else
			{
			    Toast.makeText(mContext,
				    R.string.toast_export_failed,
				    Toast.LENGTH_SHORT).show();
			}

			return true;
		    }
		});
	mShowLocationPref
		.setOnPreferenceClickListener(new OnPreferenceClickListener()
		{
		    @Override
		    public boolean onPreferenceClick(Preference preference)
		    {
			Cursor locationCursor = mDatabaseManager
				.getLastLocation();

			if (locationCursor.getCount() < 1)
			{
			    Toast.makeText(mContext,
				    R.string.toast_no_last_location,
				    Toast.LENGTH_SHORT).show();
			}
			else
			{
			    // Get latitude & longitude from the cursor
			    double latitude = locationCursor.getDouble(locationCursor
				    .getColumnIndex(LocationSchema.LATITUDE));
			    double longitude = locationCursor.getDouble(locationCursor
				    .getColumnIndex(LocationSchema.LONGITUDE));

			    // Create the URI and send the intent
			    String uriBegin = String.format(Locale.ENGLISH,
				    "geo:%f,%f", latitude, longitude);
			    String query = String.format(Locale.ENGLISH,
				    "%f,%f", latitude, longitude);
			    String uriString = uriBegin + "?q=" + query;
			    Uri uri = Uri.parse(uriString);
			    startActivity(new Intent(Intent.ACTION_VIEW, uri));
			}

			return true;
		    }
		});
	mUploadMeasurementsPref
		.setOnPreferenceClickListener(new OnPreferenceClickListener()
		{
		    @Override
		    public boolean onPreferenceClick(Preference preference)
		    {
			Integer[] sensorIDs = mDatabaseManager
				.getSensorIDsOfXivelyUsingSensors();

			mUploadManager.startUploadingMeasurements(sensorIDs);

			return true;
		    }
		});
	mUploadLocationPref
		.setOnPreferenceClickListener(new OnPreferenceClickListener()
		{
		    @Override
		    public boolean onPreferenceClick(Preference preference)
		    {
			// Upload location data here
			mUploadManager.startUploadingLocation();

			return true;
		    }
		});
    }
}