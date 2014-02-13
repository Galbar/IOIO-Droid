package com.kaolick.ioio_droid.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import com.kaolick.ioio_droid.R;
import com.kaolick.ioio_droid.database.IodDatabaseManager;
import com.kaolick.ioio_droid.sensor.IodIOIOSensor;
import com.kaolick.ioio_droid.sensor.IodIOIOSensorValidator;
import com.kaolick.ioio_droid.store.Store;

/**
 * Handles the configuration of a sensor.
 * 
 * @author kaolick
 * @see Activity
 */
public class IOIOSensorConfigActivity extends Activity
{
    // General attributes
    private Context mContext;
    private IodDatabaseManager mDatabaseManager;
    private boolean isNewSensor = true;

    // UI elements
    private EditText mNameEditText, mPinEditText, mFreqEditText,
	    mThresholdEditText, mDatastreamEditText;
    private Spinner mTimeUnitSpinner, mInputTypeSpinner,
	    mMeasurementTypeSpinner, mThresholdTypeSpinner;
    private Switch mXivelySwitch;

    // Entered values
    int enteredTimeUnit, enteredInputType, enteredMeasurementType,
	    enteredThresholdType, enteredUseXively;

    // The sensor that gets configured
    private IodIOIOSensor mSensor;

    // *************** Activity life cycle ***************

    @Override
    protected void onCreate(Bundle _savedInstanceState)
    {
	super.onCreate(_savedInstanceState);
	setContentView(R.layout.activity_ioio_sensor_config);

	// Enable navigation via ActionBar
	getActionBar().setDisplayHomeAsUpEnabled(true);

	// Initialize general attributes
	mContext = this;
	mDatabaseManager = new IodDatabaseManager(mContext);

	// Initialize UI elements
	initUIElements();

	// Get data from intent
	Bundle extras = getIntent().getExtras();
	isNewSensor = extras.getBoolean(Store.IS_NEW_SENSOR);

	// Set the values to default like in the standard constructor of a IOIO
	// sensor
	if (isNewSensor)
	{
	    enteredTimeUnit = IodIOIOSensor.TIME_UNIT_SECONDS;
	    enteredInputType = IodIOIOSensor.INPUT_TYPE_ANALOG;
	    enteredMeasurementType = IodIOIOSensor.MEASUREMENT_TYPE_ABSOLUTE;
	    enteredThresholdType = IodIOIOSensor.THRESHOLD_TYPE_LOWER;
	    enteredUseXively = IodIOIOSensor.USE_XIVELY_FALSE;
	}
	// Get the selected sensor's configuration from the database and fill
	// the UI elements with its attributes
	else
	{
	    long rowID = extras.getLong(Store.ROW_ID);

	    mSensor = mDatabaseManager.getIOIOSensorByRowID(rowID);

	    enteredTimeUnit = mSensor.getTimeUnit();
	    enteredInputType = mSensor.getInputType();
	    enteredMeasurementType = mSensor.getMeasurementType();
	    enteredThresholdType = mSensor.getThresholdType();
	    enteredUseXively = mSensor.getUseXively();

	    // Fill the UI elements with the sensor's data.
	    fillUIElements();
	}
    }

    /**
     * Resets the entered values and UI elements to default.
     */
    private void resetActivity()
    {
	// Reset entered values
	enteredTimeUnit = IodIOIOSensor.TIME_UNIT_SECONDS;
	enteredInputType = IodIOIOSensor.INPUT_TYPE_ANALOG;
	enteredMeasurementType = IodIOIOSensor.MEASUREMENT_TYPE_ABSOLUTE;
	enteredThresholdType = IodIOIOSensor.THRESHOLD_TYPE_LOWER;
	enteredUseXively = IodIOIOSensor.USE_XIVELY_FALSE;

	// Reset UI elements
	mNameEditText.setText("");
	mPinEditText.setText("");
	mFreqEditText.setText("");
	mTimeUnitSpinner.setSelection(0);
	mInputTypeSpinner.setSelection(0);
	mMeasurementTypeSpinner.setSelection(0);
	mThresholdEditText.setText("");
	mThresholdTypeSpinner.setSelection(0);
	mXivelySwitch.setChecked(false);
	mDatastreamEditText.setText("");
	useXively(false);
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
		R.layout.help_activity_ioio_sensor_config, null));
	builder.setPositiveButton(R.string.ok, null);
	builder.create().show();
    }

    // *************** Menu ***************

    @Override
    public boolean onCreateOptionsMenu(Menu _menu)
    {
	// Inflate the menu; this adds items to the action bar if it is present.
	getMenuInflater().inflate(R.menu.menu_activity_config_ioio_sensor,
		_menu);

	return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem _item)
    {
	switch (_item.getItemId())
	{
	case android.R.id.home:
	    startActivity(new Intent(this, MainActivity.class));
	    return true;
	case R.id.menu_config_sensor_activity_save_sensor:
	    if (sensorIsValid())
	    {
		saveSensor();
	    }
	    mNameEditText.requestFocus();
	    return true;
	case R.id.menu_config_sensor_activity_reset_ui:
	    resetActivity();
	    return true;
	case R.id.menu_config_sensor_activity_help:
	    showHelp();
	    return true;
	default:
	    return super.onOptionsItemSelected(_item);
	}
    }

    // *************** Sensor ***************

    /**
     * Checks if the sensor attributes are valid. Informs the user in case he
     * entered an invalid value.
     * 
     * @return <code>true</code> if the sensor configuration is valid,
     *         <code>false</code> otherwise.
     */
    private boolean sensorIsValid()
    {
	IodIOIOSensorValidator validator = new IodIOIOSensorValidator(mContext);

	// Get entered values
	String enteredName = mNameEditText.getText().toString();

	int enteredPinNumber = 0;
	if (!mPinEditText.getText().toString().equals(""))
	{
	    enteredPinNumber = Integer.valueOf(mPinEditText.getText()
		    .toString());
	}

	int enteredFrequency = 0;
	if (!mFreqEditText.getText().toString().equals(""))
	{
	    enteredFrequency = Integer.valueOf(mFreqEditText.getText()
		    .toString());
	}

	double enteredThreshold = 0;
	if (!mThresholdEditText.getText().toString().equals(""))
	{
	    enteredThreshold = Double.valueOf(mThresholdEditText.getText()
		    .toString());
	}

	String enteredDatastream = mDatastreamEditText.getText().toString();

	// Create a new sensor with the entered values
	if (isNewSensor)
	{
	    mSensor = new IodIOIOSensor();
	}

	// Set the sensor values with the entered values
	mSensor.setName(enteredName);
	mSensor.setPinNumber(enteredPinNumber);
	mSensor.setFrequency(enteredFrequency);
	mSensor.setTimeUnit(enteredTimeUnit);
	mSensor.setInputType(enteredInputType);
	mSensor.setMeasurementType(enteredMeasurementType);
	mSensor.setThreshold(enteredThreshold);
	mSensor.setThresholdType(enteredThresholdType);
	mSensor.setUseXively(enteredUseXively);
	mSensor.setDatastream(enteredDatastream);

	// If sensor is valid
	if (validator.validateSensor(mSensor))
	{
	    return true;
	}
	// If sensor is not valid
	else
	{
	    return false;
	}
    }

    /**
     * Save the sensor in the application's database.
     */
    private void saveSensor()
    {
	// Save sensor configuration
	if (isNewSensor)
	{
	    mDatabaseManager.addIOIOSensor(mSensor);

	    // Inform the user
	    Toast.makeText(mContext, R.string.toast_sensor_saved,
		    Toast.LENGTH_SHORT).show();

	    resetActivity();
	}
	// Update sensor configuration
	else
	{
	    mDatabaseManager.updateIOIOSensor(mSensor);

	    // Inform the user
	    Toast.makeText(mContext, R.string.toast_sensor_updated,
		    Toast.LENGTH_SHORT).show();
	}
    }

    // *************** Spinners ***************

    /**
     * Gets an {@link ArrayAdapter} with the given resources and default layout.
     * 
     * @param _textArrayResId
     *            The resources ID
     * @return the <code>ArrayAdapter</code>
     */
    private ArrayAdapter<CharSequence> getArrayAdapter(int _textArrayResId)
    {
	// Create an ArrayAdapter using the string array and a default spinner
	// layout
	ArrayAdapter<CharSequence> adapter = ArrayAdapter
		.createFromResource(mContext, _textArrayResId,
			android.R.layout.simple_spinner_item);

	// Specify the layout to use when the list of choices appears
	adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

	return adapter;
    }

    /**
     * Initializes the input type {@link Spinner}.
     */
    private void initInputTypeSpinner()
    {
	// Apply an adapter to the spinner
	mInputTypeSpinner.setAdapter(getArrayAdapter(R.array.inputTypes));

	// Apply a listener to the spinner
	mInputTypeSpinner
		.setOnItemSelectedListener(new OnItemSelectedListener()
		{
		    @Override
		    public void onItemSelected(AdapterView<?> _parent,
					       View _view,
					       int _pos,
					       long _id)
		    {
			// Set the entered input type to the selected type
			enteredInputType = _pos;

			// Input type = analog
			if (_pos == IodIOIOSensor.INPUT_TYPE_ANALOG)
			{
			    // Apply an adapter to the measurement spinner
			    mMeasurementTypeSpinner
				    .setAdapter(getArrayAdapter(R.array.measurementTypesAnalog));

			    switch (enteredMeasurementType)
			    {
			    case IodIOIOSensor.MEASUREMENT_TYPE_ABSOLUTE:
				mMeasurementTypeSpinner.setSelection(0);
				break;
			    case IodIOIOSensor.MEASUREMENT_TYPE_RELATIVE:
				mMeasurementTypeSpinner.setSelection(1);
				break;
			    }
			}
			// Input type = digital
			else
			{
			    // Apply an adapter to the measurement spinner
			    mMeasurementTypeSpinner
				    .setAdapter(getArrayAdapter(R.array.measurementTypesDigital));

			    switch (enteredMeasurementType)
			    {
			    case IodIOIOSensor.MEASUREMENT_TYPE_FLOATING:
				mMeasurementTypeSpinner.setSelection(0);
				break;
			    case IodIOIOSensor.MEASUREMENT_TYPE_PULL_DOWN:
				mMeasurementTypeSpinner.setSelection(1);
			    case IodIOIOSensor.MEASUREMENT_TYPE_PULL_UP:
				mMeasurementTypeSpinner.setSelection(2);
			    }
			}
		    }

		    @Override
		    public void onNothingSelected(AdapterView<?> _parent)
		    {
			// Do nothing
		    }
		});
    }

    /**
     * Initializes the measurement type {@link Spinner}.
     */
    private void initMeasurementTypeSpinner()
    {
	// Apply an adapter to the spinner
	mMeasurementTypeSpinner
		.setAdapter(getArrayAdapter(R.array.measurementTypesAnalog));

	// Apply a listener to the spinner
	mMeasurementTypeSpinner
		.setOnItemSelectedListener(new OnItemSelectedListener()
		{
		    @Override
		    public void onItemSelected(AdapterView<?> _parent,
					       View _view,
					       int _pos,
					       long _id)
		    {
			// Input type analog
			if (enteredInputType == IodIOIOSensor.INPUT_TYPE_ANALOG)
			{
			    enteredMeasurementType = _pos;
			}
			// Input type digital
			else
			{
			    // Values for measurement types for digital input
			    // type start at 2; therefore add 2 to position
			    enteredMeasurementType = _pos + 2;
			}
		    }

		    @Override
		    public void onNothingSelected(AdapterView<?> _parent)
		    {
			// Do nothing
		    }
		});
    }

    /**
     * Initializes the threshold type {@link Spinner}.
     */
    private void initThresholdTypeSpinner()
    {
	mThresholdTypeSpinner
		.setAdapter(getArrayAdapter(R.array.thresholdTypes));

	mThresholdTypeSpinner
		.setOnItemSelectedListener(new OnItemSelectedListener()
		{

		    @Override
		    public void onItemSelected(AdapterView<?> _parent,
					       View _view,
					       int _pos,
					       long _id)
		    {
			enteredThresholdType = _pos;
		    }

		    @Override
		    public void onNothingSelected(AdapterView<?> _parent)
		    {
			// Do nothing
		    }
		});
    }

    /**
     * Initializes the time unit {@link Spinner}.
     */
    private void initTimeUnitSpinner()
    {
	// Apply an adapter to the spinner
	mTimeUnitSpinner.setAdapter(getArrayAdapter(R.array.timeUnits));

	// Apply a listener to the spinner
	mTimeUnitSpinner.setOnItemSelectedListener(new OnItemSelectedListener()
	{
	    @Override
	    public void onItemSelected(AdapterView<?> _parent,
				       View _view,
				       int _pos,
				       long _id)
	    {
		switch (_pos)
		{
		case 0:
		    enteredTimeUnit = IodIOIOSensor.TIME_UNIT_SECONDS;
		    break;
		case 1:
		    enteredTimeUnit = IodIOIOSensor.TIME_UNIT_MINUTES;
		    break;
		case 2:
		    enteredTimeUnit = IodIOIOSensor.TIME_UNIT_HOURS;
		    break;
		default:
		    break;
		}
	    }

	    @Override
	    public void onNothingSelected(AdapterView<?> _parent)
	    {
		// Do nothing
	    }
	});
    }

    // *************** Switch ***************

    /**
     * Called when the Xively switch was clicked.
     * 
     * @param _view
     */
    public void onXivelySwitchClicked(View _view)
    {
	// Is the Switch on?
	boolean on = ((Switch) _view).isChecked();

	useXively(on);
    }

    /**
     * Handles click on Xively switch. Sets the according sensor value and
     * changes the appearance of Xively-related UI elements.
     * 
     * @param _useXively
     */
    public void useXively(boolean _useXively)
    {
	if (_useXively)
	{
	    // Set selected value
	    enteredUseXively = IodIOIOSensor.USE_XIVELY_TRUE;

	    // Activate UI elements
	    mDatastreamEditText.setEnabled(true);
	    mDatastreamEditText.setHintTextColor(getResources().getColor(
		    R.color.hint_darker_gray));
	}
	else
	{
	    // Set selected value
	    enteredUseXively = IodIOIOSensor.USE_XIVELY_FALSE;

	    // De-activate UI elements
	    mDatastreamEditText.setEnabled(false);
	    mDatastreamEditText.setHintTextColor(getResources().getColor(
		    R.color.hint_lighter_gray));
	}
    }

    // *************** UI elements ***************

    /**
     * Sets the UI elements with the sensor attributes. This is only used when an
     * already existing sensor gets edited.
     */
    private void fillUIElements()
    {
	// Set sensor name
	mNameEditText.setText(mSensor.getName());

	// Set pin number
	mPinEditText.setText(String.valueOf(mSensor.getPinNumber()));

	// Set frequency
	mFreqEditText.setText(String.valueOf(mSensor.getFrequency()));

	// Set time unit
	switch (mSensor.getTimeUnit())
	{
	case IodIOIOSensor.TIME_UNIT_SECONDS:
	    mTimeUnitSpinner.setSelection(0);
	    break;
	case IodIOIOSensor.TIME_UNIT_MINUTES:
	    mTimeUnitSpinner.setSelection(1);
	    break;
	case IodIOIOSensor.TIME_UNIT_HOURS:
	    mTimeUnitSpinner.setSelection(2);
	    break;
	}

	// Set input type
	switch (mSensor.getInputType())
	{
	case IodIOIOSensor.INPUT_TYPE_ANALOG:
	    mInputTypeSpinner.setSelection(0);

	    // Set the measurement type spinner according to the input type
	    mMeasurementTypeSpinner
		    .setAdapter(getArrayAdapter(R.array.measurementTypesAnalog));
	    break;
	case IodIOIOSensor.INPUT_TYPE_DIGITAL:
	    mInputTypeSpinner.setSelection(1);

	    // Set the measurement type spinner according to the input type
	    mMeasurementTypeSpinner
		    .setAdapter(getArrayAdapter(R.array.measurementTypesDigital));
	    break;
	}

	// Set measurement type
	switch (mSensor.getMeasurementType())
	{
	case IodIOIOSensor.MEASUREMENT_TYPE_ABSOLUTE:
	    mMeasurementTypeSpinner.setSelection(0);
	    break;
	case IodIOIOSensor.MEASUREMENT_TYPE_RELATIVE:
	    mMeasurementTypeSpinner.setSelection(1);
	    break;
	case IodIOIOSensor.MEASUREMENT_TYPE_FLOATING:
	    mMeasurementTypeSpinner.setSelection(0);
	    break;
	case IodIOIOSensor.MEASUREMENT_TYPE_PULL_DOWN:
	    mMeasurementTypeSpinner.setSelection(1);
	    break;
	case IodIOIOSensor.MEASUREMENT_TYPE_PULL_UP:
	    mMeasurementTypeSpinner.setSelection(2);
	    break;
	}

	// Set threshold
	mThresholdEditText.setText(String.valueOf(mSensor.getThreshold()));

	// Set threshold type
	switch (mSensor.getThresholdType())
	{
	case IodIOIOSensor.THRESHOLD_TYPE_LOWER:
	    mThresholdTypeSpinner.setSelection(0);
	    break;
	case IodIOIOSensor.THRESHOLD_TYPE_UPPER:
	    mThresholdTypeSpinner.setSelection(1);
	    break;
	}

	// Set Xively switch
	switch (mSensor.getUseXively())
	{
	case IodIOIOSensor.USE_XIVELY_TRUE:
	    mXivelySwitch.setChecked(true);
	    useXively(true);
	    break;
	case IodIOIOSensor.USE_XIVELY_FALSE:
	    mXivelySwitch.setChecked(false);
	    useXively(false);
	}

	// Set datastream
	mDatastreamEditText.setText(mSensor.getDatastream());
    }

    /**
     * Initializes all UI elements.
     */
    private void initUIElements()
    {
	// Find UI elements by ID
	mNameEditText = (EditText) findViewById(R.id.sensorNameEditText);
	mPinEditText = (EditText) findViewById(R.id.sensorPinEditText);
	mFreqEditText = (EditText) findViewById(R.id.sensorFreqEditText);
	mTimeUnitSpinner = (Spinner) findViewById(R.id.timeUnitSpinner);
	mInputTypeSpinner = (Spinner) findViewById(R.id.inputTypeSpinner);
	mMeasurementTypeSpinner = (Spinner) findViewById(R.id.measurementTypeSpinner);
	mThresholdEditText = (EditText) findViewById(R.id.thresholdEditText);
	mThresholdTypeSpinner = (Spinner) findViewById(R.id.thresholdTypeSpinner);
	mXivelySwitch = (Switch) findViewById(R.id.xivelySwitch);
	mDatastreamEditText = (EditText) findViewById(R.id.datastreamEditText);

	// Initialize UI elements
	initTimeUnitSpinner();
	initInputTypeSpinner();
	initMeasurementTypeSpinner();
	initThresholdTypeSpinner();
	mXivelySwitch.setSelected(false);
    }
}