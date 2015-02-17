package com.kaolick.ioio_droid.ui;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.ActionMode;
import android.view.ActionMode.Callback;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.Toast;

import com.kaolick.ioio_droid.R;
import com.kaolick.ioio_droid.adapter.MeasurementsCursorActionModeAdapter;
import com.kaolick.ioio_droid.adapter.MeasurementsCursorAdapter;
import com.kaolick.ioio_droid.database.IodDatabaseManager;
import com.kaolick.ioio_droid.database.MeasurementSchema;
import com.kaolick.ioio_droid.database.MeasurementTable;
import com.kaolick.ioio_droid.dialog.UniversalDialogFragment;
import com.kaolick.ioio_droid.dialog.UniversalDialogFragment.UniversalDialogListener;
import com.kaolick.ioio_droid.store.Store;
import com.kaolick.ioio_droid.xively.UploadManager;

/**
 * Handles the measurements of a sensor. Implements
 * {@link UniversalDialogListener}.
 * 
 * @author kaolick
 * @see Activity
 */
public class IOIOSensorMeasurementsActivity extends FragmentActivity implements
								    UniversalDialogListener
{
    // General attributes
    private Context mContext;
    private IodDatabaseManager mDatabaseManager;
    private SharedPreferences mPrefs;
    private int mSensorID = -1;

    // ListView for displaying the sensor's measurements
    private ListView mMeasurementsListView;

    // ListView-related attributes
    private MeasurementsCursorAdapter mListViewAdapter;
    private MeasurementsCursorActionModeAdapter mActionModeListViewAdapter;
    private Cursor mListViewCursor;
    private String[] from = new String[] { MeasurementSchema.UPLOADED,
	    MeasurementSchema.TIMESTAMP, MeasurementSchema.VALUE };
    private int[] to = new int[] { R.id.uploadedStatusTV, R.id.timestampTV,
	    R.id.measuredValueTV };

    // Contextual action mode
    private ActionMode mActionMode;
    private ActionMode.Callback mActionModeCallback;

    // *************** Activity life cycle ***************

    @Override
    protected void onCreate(Bundle _savedInstanceState)
    {
	super.onCreate(_savedInstanceState);
	setContentView(R.layout.activity_ioio_sensor_measurements);

	// Initialize attributes
	mContext = this;
	mDatabaseManager = new IodDatabaseManager(mContext);
	mPrefs = SettingsActivity.getPrefs(this);
	mActionModeCallback = getActionModeCallback();

	// Get ActionBar an enable navigation
	ActionBar actionBar = getActionBar();
	actionBar.setDisplayHomeAsUpEnabled(true);

	// Get the sensorID from the intent
	Bundle extras = getIntent().getExtras();
	mSensorID = extras.getInt(Store.SENSOR_ID);

	// Set the title to the sensor's name
	setTitle(extras.getString(Store.SENSOR_NAME));

	// Initialize UI elements
	mMeasurementsListView = (ListView) findViewById(R.id.measurementsListView);
	initListView();
    }

    @Override
    protected void onResume()
    {
	super.onResume();

	initListView();
    }

    // *************** Contextual Action Mode ***************

    private ActionMode.Callback getActionModeCallback()
    {
	return new Callback()
	{
	    @Override
	    public boolean onPrepareActionMode(ActionMode _mode, Menu _menu)
	    {
		return false;
	    }

	    @Override
	    public boolean onCreateActionMode(ActionMode _mode, Menu _menu)
	    {
		// Inflate the menu for the CAB
		MenuInflater inflater = _mode.getMenuInflater();
		inflater.inflate(R.menu.menu_activity_measurements_cam, _menu);

		initActionModeListView();

		return true;
	    }

	    @Override
	    public boolean
		    onActionItemClicked(ActionMode _mode, MenuItem _item)
	    {
		switch (_item.getItemId())
		{
		case R.id.menu_measurements_activity_cam_delete_all:
		    // Remove all measurements from the database and refresh the
		    // ListView
		    showDeleteDialog();
		    return true;
		default:
		    return false;
		}
	    }

	    @Override
	    public void onDestroyActionMode(ActionMode _mode)
	    {
		mActionMode = null;

		initListView();
	    }
	};
    }

    /**
     * Initializes the {@link ListView} for the {@link ActionMode}.
     */
    public void initActionModeListView()
    {
	mListViewCursor = mDatabaseManager.getMeasurementsCursor(mSensorID);
	mActionModeListViewAdapter = new MeasurementsCursorActionModeAdapter(
		getApplicationContext(),
		R.layout.listview_item_measurements_cam, mListViewCursor, from,
		to, 0, mSensorID);
	mMeasurementsListView.setAdapter(mActionModeListViewAdapter);
    }

    /**
     * Starts the {@link ActionMode}.
     */
    public void startActionMode()
    {
	if (isMeasuring())
	{
	    Toast.makeText(mContext, R.string.toast_is_measuring,
		    Toast.LENGTH_SHORT).show();
	}
	else
	{
	    if (mActionMode == null)
	    {
		mActionMode = startActionMode(mActionModeCallback);
	    }
	}
    }

    // *************** Dialog ***************

    /**
     * Shows a <code>Dialog</code> for deleting all measurements.
     */
    public void showDeleteDialog()
    {
	String title = getResources().getString(
		R.string.dialog_delete_all_measurements_title);
	String message = getResources().getString(
		R.string.dialog_delete_all_measurements_message);

	DialogFragment fragment = UniversalDialogFragment.newInstance(
		Store.DELETE_ALL_MEASUREMENTS, title, message);
	fragment.show(getFragmentManager(), "Delete all measurements");
    }

    @Override
    public void onUniversalDialogPositiveClick(DialogFragment _dialog)
    {
	deleteMeasurements();
    }

    @Override
    public void onUniversalDialogNegativeClick(DialogFragment _dialog)
    {
	// User touched the dialog's negative button
	// Do nothing
    }

    // *************** Help ***************

    /**
     * Shows a custom {@link AlertDialog} with help content.
     */
    public void showHelp()
    {
	AlertDialog.Builder builder = new AlertDialog.Builder(this);
	builder.setTitle(R.string.menu_help);
	builder.setView(getLayoutInflater().inflate(
		R.layout.help_activity_measurements, null));
	builder.setPositiveButton(R.string.ok, null);
	builder.create().show();
    }

    // *************** Menu ***************

    @Override
    public boolean onCreateOptionsMenu(Menu _menu)
    {
	// Inflate the menu; this adds items to the action bar if it is present.
	getMenuInflater().inflate(R.menu.menu_activity_measurements, _menu);

	return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem _item)
    {
	switch (_item.getItemId())
	{
	case android.R.id.home:
	    // Return to the MainActivity when clicking on the app icon in the
	    // ActionBar
	    startActivity(new Intent(this, MainActivity.class));
	    return true;
	case R.id.menu_measurements_activity_edit_mode:
	    startActionMode();
	    return true;
	case R.id.menu_measurements_activity_refresh:
	    initListView();
	    Toast.makeText(mContext, R.string.toast_refreshed_measurements,
		    Toast.LENGTH_SHORT).show();
	    return true;
	case R.id.menu_measurements_activity_export:
	    exportMeasurements();
	    return true;
	case R.id.menu_measurements_activity_settings:
	    startSettingsActivity();
	    return true;
	case R.id.menu_measurements_activity_upload:
	    uploadMeasurements();
	    return true;
	case R.id.menu_measurements_activity_help:
	    showHelp();
	    return true;
	default:
	    return super.onOptionsItemSelected(_item);
	}
    }

    // *************** ListView ***************

    /**
     * Initializes the sensor {@link ListView} and all necessary components.
     */
    public void initListView()
    {
	mListViewCursor = mDatabaseManager.getMeasurementsCursor(mSensorID);
	mListViewAdapter = new MeasurementsCursorAdapter(mContext,
		R.layout.listview_item_measurements, mListViewCursor, from, to,
		0);
	mMeasurementsListView.setAdapter(mListViewAdapter);
    }

    // *************** Measurements ***************

    /**
     * Deletes all measurements from the according database table.
     */
    public void deleteMeasurements()
    {
	// Delete the measurements
	mDatabaseManager.getDb().delete(
		MeasurementTable.TABLE_NAME_PRE_TAG + mSensorID, null, null);

	// Refresh the ListView
	mListViewCursor = mDatabaseManager.getMeasurementsCursor(mSensorID);
	mActionModeListViewAdapter.changeCursor(mListViewCursor);

	// Finish the ActionMode
	mActionMode.finish();
    }

    /**
     * Exports the sensor's measurements table to the external storage.
     */
    public void exportMeasurements()
    {
	if (mDatabaseManager.exportMeasurements(mSensorID))
	{
	    Toast.makeText(mContext, R.string.toast_export_success,
		    Toast.LENGTH_SHORT).show();
	}
	else
	{
	    Toast.makeText(mContext, R.string.toast_export_failed,
		    Toast.LENGTH_SHORT).show();
	}
    }

    // *************** Measurement Status ***************

    /**
     * Checks if there is a running measuring process.
     * 
     * @return the <code>true</code> if there is a running measuring process,
     *         <code>false</code> otherwise.
     */
    public boolean isMeasuring()
    {
	return mPrefs.getBoolean(Store.MEASUREMENT_STATUS, false);
    }

    // *************** Settings ***************

    /**
     * Starts the {@link SettingsActivity}.
     */
    public void startSettingsActivity()
    {
	if (isMeasuring())
	{
	    Toast.makeText(mContext, R.string.toast_is_measuring,
		    Toast.LENGTH_SHORT).show();
	}
	else
	{
	    startActivity(new Intent(mContext, SettingsActivity.class));
	}
    }

    // *************** Upload ***************

    /**
     * Uploads the sensor's measurements.
     */
    public void uploadMeasurements()
    {
	UploadManager um = new UploadManager(mContext, this, true);

	Integer[] sensorIDs = { mSensorID };

	um.startUploadingMeasurements(sensorIDs);
    }
}