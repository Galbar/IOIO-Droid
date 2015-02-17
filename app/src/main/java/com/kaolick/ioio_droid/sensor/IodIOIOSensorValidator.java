package com.kaolick.ioio_droid.sensor;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.widget.Toast;

import com.kaolick.ioio_droid.R;
import com.kaolick.ioio_droid.database.IodDatabaseManager;
import com.kaolick.ioio_droid.ui.SettingsActivity;

/**
 * A validator for a {@link IodIOIOSensor}. Validates the sensor's
 * configuration.
 * 
 * @author kaolick
 */
public class IodIOIOSensorValidator
{
    // General attributes
    private Context mContext;
    private Resources mResources;
    private SharedPreferences mPrefs;

    // IOIO version strings
    private String IOIO_V1;
    private String ioioVersion;

    /**
     * Class constructor.
     * 
     * @param _context
     *            to use for accessing the application's {@link Resources} and
     *            {@link SharedPreferences}
     */
    public IodIOIOSensorValidator(Context _context)
    {
	this.mContext = _context;
	this.mPrefs = SettingsActivity.getPrefs((ContextWrapper) mContext);
	this.mResources = _context.getResources();
	this.IOIO_V1 = mResources.getString(R.string.ioio_v1_entry_value);
	this.ioioVersion = mPrefs.getString(
		mResources.getString(R.string.pref_general_ioio_version_key),
		IOIO_V1);
    }

    // *************** Private methods ***************

    private boolean validateDatastream(int _useXively,
				       String _datastream,
				       int _sensorID)
    {
	// Check the entered datastream; must not be ""
	if (_useXively == IodIOIOSensor.USE_XIVELY_TRUE
		&& _datastream.equals(""))
	{
	    // Inform the user
	    Toast.makeText(mContext, R.string.toast_sensor_invalid_datastream,
		    Toast.LENGTH_LONG).show();

	    return false;
	}

	/*
	 * Check if the datastream is already used by another sensor with the
	 * same feed ID saved in the database
	 */
	IodDatabaseManager dbm = new IodDatabaseManager(mContext);
	if (_useXively == IodIOIOSensor.USE_XIVELY_TRUE
		&& dbm.datastreamInUse(_datastream, _sensorID))
	{
	    return false;
	}

	return true;
    }

    private boolean validateFrequency(int _frequency)
    {
	if (_frequency == 0)
	{
	    // Inform the user
	    Toast.makeText(mContext, R.string.toast_sensor_invalid_freq,
		    Toast.LENGTH_LONG).show();

	    return false;
	}

	return true;
    }

    private boolean validateName(String _name)
    {
	if (_name.equals(""))
	{
	    // Inform the user
	    Toast.makeText(mContext, R.string.toast_sensor_invalid_name,
		    Toast.LENGTH_LONG).show();

	    return false;
	}

	return true;
    }

    private boolean validatePinNumber(int _pinNumber,
				      int _inputType,
				      int _sensorID)
    {
	// Check for IOIO V1
	if (ioioVersion.equals(IOIO_V1))
	{
	    // Check if pin number is between 1 and 48
	    if (_pinNumber < 1 || _pinNumber > 48)
	    {
		// Inform the user
		Toast.makeText(mContext,
			R.string.toast_sensor_invalid_pin_number_v1,
			Toast.LENGTH_LONG).show();

		return false;
	    }
	}
	// Check for IOIO OTG
	else
	{
	    // Check if pin number is between 1 and 46
	    if (_pinNumber < 1 || _pinNumber > 46)
	    {
		// Inform the user
		Toast.makeText(mContext,
			R.string.toast_sensor_invalid_pin_number_otg,
			Toast.LENGTH_LONG).show();

		return false;
	    }
	}

	/*
	 * Check if pin number is between 31 and 46 in case the sensor's input
	 * type is analog
	 */
	if (_inputType == IodIOIOSensor.INPUT_TYPE_ANALOG)
	{
	    if (_pinNumber < 31 || _pinNumber > 46)
	    {
		// Inform the user
		Toast.makeText(
			mContext,
			R.string.toast_sensor_invalid_pin_number_for_analog_input,
			Toast.LENGTH_LONG).show();

		return false;
	    }
	}

	/*
	 * Check if the pin number is already used by another sensor saved in
	 * the database
	 */
	IodDatabaseManager dbm = new IodDatabaseManager(mContext);
	if (dbm.isPinNumberInUse(_pinNumber, _sensorID))
	{
	    return false;
	}

	return true;
    }

    private boolean validateThreshold(int _measurementType, double _threshold)
    {
	if (_measurementType == IodIOIOSensor.MEASUREMENT_TYPE_ABSOLUTE
		&& _threshold > 3.3)
	{
	    // Inform the user
	    Toast.makeText(mContext, R.string.toast_sensor_threshold_absolute,
		    Toast.LENGTH_LONG).show();

	    return false;
	}

	if (_measurementType != IodIOIOSensor.MEASUREMENT_TYPE_ABSOLUTE
		&& _threshold >= 1)
	{
	    // Inform the user
	    Toast.makeText(mContext, R.string.toast_sensor_threshold_relative,
		    Toast.LENGTH_LONG).show();

	    return false;
	}

	return true;
    }

    // *************** Public methods ***************

    /**
     * Validates the configuration of the given sensor.
     * 
     * @param _sensor
     *            The sensor to validate
     * @return <code>true</code> if the sensor's configuration is valid,
     *         <code>false</code> otherwise.
     */
    public boolean validateSensor(IodIOIOSensor _sensor)
    {
	// Check name; must not be ""
	if (!validateName(_sensor.getName()))
	{
	    return false;
	}
	/*
	 * Check pin number; must be between 1 and 48 (V1) or 1 and 46 (OTG);
	 * must be between 31 and 46 if input type is analog
	 */
	if (!validatePinNumber(_sensor.getPinNumber(), _sensor.getInputType(),
		_sensor.getSensorID()))
	{
	    return false;
	}
	// Check frequency; must not be 0
	if (!validateFrequency(_sensor.getFrequency()))
	{
	    return false;
	}
	/*
	 * Check threshold; must be < 3.3 for measurement type absolute or < 1
	 * otherwise
	 */
	if (!validateThreshold(_sensor.getMeasurementType(),
		_sensor.getThreshold()))
	{
	    return false;
	}
	// Check datastream; must not be ""
	if (!validateDatastream(_sensor.getUseXively(),
		_sensor.getDatastream(), _sensor.getSensorID()))
	{
	    return false;
	}

	// Sensor is valid
	return true;
    }
}