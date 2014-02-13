package com.kaolick.ioio_droid.ui;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.ActionMode;
import android.view.ActionMode.Callback;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.kaolick.ioio_droid.R;
import com.kaolick.ioio_droid.adapter.IOIOSensorCursorActionModeAdapter;
import com.kaolick.ioio_droid.adapter.IOIOSensorCursorAdapter;
import com.kaolick.ioio_droid.database.IodDatabaseManager;
import com.kaolick.ioio_droid.database.SensorSchema;
import com.kaolick.ioio_droid.database.SensorTable;
import com.kaolick.ioio_droid.dialog.UniversalDialogFragment;
import com.kaolick.ioio_droid.dialog.UniversalDialogFragment.UniversalDialogListener;
import com.kaolick.ioio_droid.export.CSVManager;
import com.kaolick.ioio_droid.service.IodIOIOService;
import com.kaolick.ioio_droid.service.IodIOIOSimulationService;
import com.kaolick.ioio_droid.store.Store;

/**
 * The application's main screen. Measuring processes get started and stopped
 * here. Implements {@link UniversalDialogListener}.
 * 
 * @author kaolick
 * @see FragmentActivity
 */
public class MainActivity extends FragmentActivity implements
						  UniversalDialogListener
{
    // General attributes
    private Context mContext;
    private IodDatabaseManager mDatabaseManager;
    private SharedPreferences mPrefs;
    private Editor mEditor;

    // UI elements
    private ListView mSensorListView;
    private View mSeparatorView;
    private Button mMeasurementButton;

    // ListView-related attributes
    private IOIOSensorCursorAdapter mListViewAdapter;
    private IOIOSensorCursorActionModeAdapter mActionModeListViewAdapter;
    private Cursor mListViewCursor;
    private String[] from = new String[] { SensorSchema.NAME,
	    SensorSchema.PIN_NUMBER, SensorSchema.SENSOR_ID };
    private int[] to = new int[] { R.id.sensorNameTextView,
	    R.id.sensorPinNumberTextView, R.id.sensorSensorIDTextView };

    // Contextual action mode
    private ActionMode mActionMode;
    private ActionMode.Callback mActionModeCallback;

    // *************** Activity life cycle ***************

    @SuppressLint("CommitPrefEdits")
    @Override
    protected void onCreate(Bundle _savedInstanceState)
    {
	super.onCreate(_savedInstanceState);
	setContentView(R.layout.activity_main);

	// Initialize attributes
	mContext = this;
	mDatabaseManager = new IodDatabaseManager(mContext);
	mPrefs = SettingsActivity.getPrefs(this);
	mEditor = mPrefs.edit();
	mActionModeCallback = getActionModeCallback();

	// Initialize UI elements
	mSensorListView = (ListView) findViewById(R.id.sensorListView);
	initListView();
	mSeparatorView = (View) findViewById(R.id.separator);
	mMeasurementButton = (Button) findViewById(R.id.measurementButton);
	mMeasurementButton
		.setOnClickListener(getMeasurementButtonOnClickListener());
	setButtonLayout();
    }

    // *************** Button ***************

    /**
     * Gets a custom {@link OnClickListener} for the measurement button.
     * 
     * @return the custom <code>OnClickListener</code>
     */
    public OnClickListener getMeasurementButtonOnClickListener()
    {
	return new OnClickListener()
	{
	    @Override
	    public void onClick(View _view)
	    {
		onStartStopButtonPressed();
	    }
	};
    }

    /**
     * Starts or stops a measuring process and handles all depending operations.
     */
    public void onStartStopButtonPressed()
    {
	// Check if there are any active sensor's at all
	if (mDatabaseManager.getActiveSensors().size() == 0)
	{
	    // There are no active sensors; inform the user
	    Toast.makeText(mContext, R.string.toast_no_active_sensors,
		    Toast.LENGTH_SHORT).show();
	}
	else
	{
	    // Check Google Play services
	    if (mPrefs
		    .getBoolean(
			    getResources()
				    .getString(
					    R.string.pref_location_location_service_key),
			    false)
		    && !servicesConnected())
	    {
		// The user gets informed automatically via servicesConnected()
	    }
	    else
	    {
		// Set the measurement status in the SharedPreferences
		setMeasurementStatus(!isMeasuring());

		// Change the measurement button's layout
		setButtonLayout();

		// User pressed START
		if (isMeasuring())
		{
		    // If simulation mode is turned on
		    if (mPrefs.getBoolean(
			    getResources().getString(
				    R.string.pref_general_simulation_mode_key),
			    true))
		    {
			// Start Test
			startService(new Intent(mContext,
				IodIOIOSimulationService.class));
		    }
		    else
		    {
			// Start measuring with IOIO
			startService(new Intent(mContext, IodIOIOService.class));
		    }
		}
		// User pressed STOP
		else
		{
		    // If simulation mode is turned on
		    if (mPrefs.getBoolean(
			    getResources().getString(
				    R.string.pref_general_simulation_mode_key),
			    true))
		    {
			// Stop Test
			stopService(new Intent(mContext,
				IodIOIOSimulationService.class));
		    }
		    else
		    {
			// Stop measuring with IOIO
			stopService(new Intent(mContext, IodIOIOService.class));
		    }
		}
	    }
	}
    }

    /**
     * Sets the measurement button's layout depending on the application's
     * measurement status.
     */
    public void setButtonLayout()
    {
	if (isMeasuring())
	{
	    mMeasurementButton.setText(R.string.button_stop);
	    mMeasurementButton.setBackgroundColor(getResources().getColor(
		    android.R.color.holo_red_dark));
	    mSeparatorView.setBackgroundColor(getResources().getColor(
		    R.color.darker_red));
	}
	else
	{
	    mMeasurementButton.setText(R.string.button_start);
	    mMeasurementButton.setBackgroundColor(getResources().getColor(
		    R.color.holo_green_mat));
	    mSeparatorView.setBackgroundColor(getResources().getColor(
		    android.R.color.holo_green_dark));
	}
    }

    // *************** Contextual Action Mode ***************

    private ActionMode.Callback getActionModeCallback()
    {
	return new Callback()
	{
	    // Called each time the action mode is shown. Always called after
	    // onCreateActionMode, but may be called multiple times if the mode
	    // is invalidated.
	    @Override
	    public boolean onPrepareActionMode(ActionMode _mode, Menu _menu)
	    {
		// Here you can perform updates to the CAB due to an
		// invalidate() request

		return false;
	    }

	    // Called when the action mode is created; startActionMode() was
	    // called
	    @Override
	    public boolean onCreateActionMode(ActionMode _mode, Menu _menu)
	    {
		// Inflate the menu for the CAB
		MenuInflater inflater = _mode.getMenuInflater();
		inflater.inflate(R.menu.menu_activity_main_cam, _menu);

		initActionModeListView();

		return true;
	    }

	    // Called when the user selects a contextual menu item
	    @Override
	    public boolean
		    onActionItemClicked(ActionMode _mode, MenuItem _item)
	    {
		switch (_item.getItemId())
		{
		case R.id.menu_main_activity_cam_delete_all:
		    // Remove all sensors from the database and refresh the
		    // ListView
		    showDeleteSensorsDialog();
		    return true;
		default:
		    return false;
		}
	    }

	    // Called when the user exits the action mode
	    @Override
	    public void onDestroyActionMode(ActionMode mode)
	    {
		mActionMode = null;

		initListView();
	    }
	};
    }

    /**
     * Initializes the sensor {@link ListView} and all necessary components for
     * the {@link ActionMode}.
     */
    public void initActionModeListView()
    {
	mListViewCursor = mDatabaseManager.getIOIOSensorsCursor();
	mActionModeListViewAdapter = new IOIOSensorCursorActionModeAdapter(
		getApplicationContext(), R.layout.listview_item_sensor_cam,
		mListViewCursor, from, to, 0);
	mSensorListView.setAdapter(mActionModeListViewAdapter);
	mSensorListView.setOnItemLongClickListener(null);
    }

    /**
     * Starts the {@link ActionMode}.
     */
    public void startContextualActionMode()
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

    // *************** Dialogs ***************

    @Override
    public void onUniversalDialogPositiveClick(DialogFragment _dialog)
    {
	// User touched the dialog's positive button
	switch (_dialog.getArguments().getInt(UniversalDialogFragment.KEY))
	{
	case Store.DELETE_ALL_SENSORS:
	    removeAllSensors();
	    break;

	default:
	    break;
	}
    }

    @Override
    public void onUniversalDialogNegativeClick(DialogFragment _dialog)
    {
	// User touched the dialog's negative button
	// Do nothing
    }

    /**
     * Shows a <code>Dialog</code> for deleting all sensors.
     */
    public void showDeleteSensorsDialog()
    {
	String title = getResources().getString(
		R.string.dialog_delete_all_sensors_title);
	String message = getResources().getString(
		R.string.dialog_delete_all_sensors_message);

	DialogFragment fragment = UniversalDialogFragment.newInstance(
		Store.DELETE_ALL_SENSORS, title, message);
	fragment.show(getFragmentManager(), "Delete all IOIO sensors");
    }

    // *************** Export ***************

    /**
     * Exports all database tables to the external storage.
     */
    public void exportSensorTable()
    {
	boolean exportSuccessful = false;

	// New CSVManager
	CSVManager csvm = new CSVManager(mContext);

	// Export the IOIOSensorTable
	exportSuccessful = csvm.exportDatabaseTable(SensorTable.TABLE_NAME,
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
    }

    // *************** Google Play Services ***************

    /**
     * Checks if the Google Play Services are available.
     * 
     * @return <code>true</code> if the Google Play Services are available,
     *         <code>false</code> otherwise
     */
    private boolean servicesConnected()
    {
	int errorCode = GooglePlayServicesUtil
		.isGooglePlayServicesAvailable(this);

	if (errorCode != ConnectionResult.SUCCESS)
	{
	    GooglePlayServicesUtil.getErrorDialog(errorCode, this, 0).show();

	    return false;
	}
	else
	{
	    return true;
	}
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
		R.layout.help_activity_main, null));
	builder.setPositiveButton(R.string.ok, null);
	builder.create().show();
    }

    // *************** Menu ***************

    @Override
    public boolean onCreateOptionsMenu(Menu _menu)
    {
	// Inflate the menu; this adds items to the action bar if it is present.
	getMenuInflater().inflate(R.menu.menu_activity_main, _menu);
	return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem _item)
    {
	switch (_item.getItemId())
	{
	case R.id.menu_main_activity_edit_mode:
	    startContextualActionMode();
	    return true;
	case R.id.menu_main_activity_add_sensor:
	    addNewSensor();
	    return true;
	case R.id.menu_main_activity_settings:
	    startSettingsActivity();
	    return true;
	case R.id.menu_main_activity_export:
	    exportSensorTable();
	    return true;
	case R.id.menu_main_activity_help:
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
	mListViewCursor = mDatabaseManager.getIOIOSensorsCursor();
	mListViewAdapter = new IOIOSensorCursorAdapter(getApplicationContext(),
		R.layout.listview_item_sensor, mListViewCursor, from, to, 0);
	mSensorListView.setAdapter(mListViewAdapter);
	mSensorListView
		.setOnItemLongClickListener(new OnItemLongClickListener()
		{
		    @Override
		    public boolean onItemLongClick(AdapterView<?> _parent,
						   View _view,
						   int _position,
						   long _id)
		    {
			if (isMeasuring())
			{
			    Toast.makeText(mContext,
				    R.string.toast_is_measuring,
				    Toast.LENGTH_SHORT).show();
			}
			else
			{
			    editExistingSensor(_id);
			}

			return true;
		    }
		});
	mSensorListView.setOnItemClickListener(new OnItemClickListener()
	{

	    @Override
	    public void onItemClick(AdapterView<?> _parent,
				    View _view,
				    int _position,
				    long _id)
	    {
		// Move the Cursor to the according ListView item
		mListViewCursor.moveToPosition(_position);

		// Get the sensor's sensorID
		int sensorID = mListViewCursor.getInt(mListViewCursor
			.getColumnIndex(SensorSchema.SENSOR_ID));

		String sensorName = mListViewCursor.getString(mListViewCursor
			.getColumnIndex(SensorSchema.NAME));

		// Open the MeasurementsActivity for the according sensor
		Intent intent = new Intent(mContext,
			IOIOSensorMeasurementsActivity.class);
		intent.putExtra(Store.SENSOR_ID, sensorID);
		intent.putExtra(Store.SENSOR_NAME, sensorName);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		mContext.startActivity(intent);
	    }
	});
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

    /**
     * Sets the application's measurement status depending on its current
     * status.
     */
    public void setMeasurementStatus(boolean _status)
    {
	mEditor.putBoolean(Store.MEASUREMENT_STATUS, _status).commit();
    }

    // *************** Sensor ***************

    /**
     * Creates a new default {@link IodIOIOSensor} and passes it to the
     * {@link IOIOSensorConfigActivity}.
     */
    private void addNewSensor()
    {
	if (isMeasuring())
	{
	    Toast.makeText(mContext, R.string.toast_is_measuring,
		    Toast.LENGTH_SHORT).show();
	}
	else
	{
	    Intent intent = new Intent(mContext, IOIOSensorConfigActivity.class);
	    intent.putExtra(Store.IS_NEW_SENSOR, true);
	    startActivity(intent);
	}
    }

    /**
     * Called when the user does a long click on a ListView item in the sensor
     * {@link ListView}. Starts the {@link IOIOSensorConfigActivity} with the
     * selected sensor's attributes.
     * 
     * @param _rowID
     *            the ListView item's row ID
     */
    private void editExistingSensor(long _rowID)
    {
	Intent intent = new Intent(mContext, IOIOSensorConfigActivity.class);
	intent.putExtra(Store.ROW_ID, _rowID);
	intent.putExtra(Store.IS_NEW_SENSOR, false);
	startActivity(intent);
    }

    /**
     * Removes all sensors from the database and refreshes the sensor
     * {@link ListView}.
     */
    private void removeAllSensors()
    {
	// Drop table
	mDatabaseManager.getDb().execSQL(SensorTable.SQL_DROP);

	// Re-create table
	mDatabaseManager.getDb().execSQL(SensorTable.SQL_CREATE);

	// Refresh the ListView
	mListViewCursor = mDatabaseManager.getIOIOSensorsCursor();
	mActionModeListViewAdapter.changeCursor(mListViewCursor);

	// Finish the ActionMode
	mActionMode.finish();
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
}