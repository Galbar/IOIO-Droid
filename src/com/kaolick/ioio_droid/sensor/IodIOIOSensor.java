package com.kaolick.ioio_droid.sensor;

import ioio.lib.api.AnalogInput;
import ioio.lib.api.DigitalInput;
import ioio.lib.api.IOIO;
import ioio.lib.api.exception.ConnectionLostException;

import java.io.Serializable;
import java.util.Random;

import com.kaolick.ioio_droid.database.IodDatabaseManager;
import com.kaolick.ioio_droid.database.MeasurementTable;

/**
 * A physical sensor that can be connected to a IOIO board. A sensor has certain
 * attributes which are described in the following:
 * <p>
 * <b>sensor ID</b>: A unique identifier. Set randomly.
 * <p>
 * <b>name</b>: The sensor's name, for example <i>Temperature sensor</i>.
 * Default: <b><i>empty string</i></b>
 * <p>
 * <b>pin number</b>: The number of the pin on the IOIO board which the sensor
 * is connected to. Must be an <code>int</code> between 1 and 48 (IOIO V1) or 1
 * and 46 (IOIO OTG). Default: <b>0</b>
 * <p>
 * <b>frequency</b>: The frequency at which the sensor's value is measured (in
 * milliseconds). Default: <b>0</b>
 * <p>
 * <b>time unit</b>: The time unit for measuring sensor values. Use only
 * <i>TIME_UNIT_SECONDS</i>, <i>TIME_UNIT_MINUTES</i> or <i>TIME_UNIT_HOURS</i>.
 * Default: <b>TIME_UNIT_SECONDS</b>
 * <p>
 * <b>input type</b>: The sensor's input type. 0 = <i>analog</i>; 1 =
 * <i>digital</i>. Default: <b>0</b>
 * <p>
 * <b>measurement type</b>: The sensor's measurement type. 0 = <i>absolute</i>
 * (voltage is measured between 0 and 3.3V); 1 = <i>relative</i> (voltage is
 * scaled between 0 and 1); 2 = <i>floating</i> (default for digital sensors); 3
 * = <i>pull-down</i> (pin is pulled gently to 0V); 4 = <i>pull-up</i> (pin is
 * pulled gently to 3.3V). Default: <b>0</b>
 * <p>
 * <b>threshold</b>: The sensor's threshold for measured values. Handling
 * depends on the <i>threshold type</i>. Default: <b>0</b>
 * <p>
 * <b>threshold type</b>: The sensor's threshold type. 0 = <i>lower</i>: only
 * measured values >= the threshold will be saved in the database; 1 =
 * <i>upper</i>: only measured values <= the threshold will be saved in the
 * database. Default: <b>0</b>
 * <p>
 * <b>state</b>: The sensor's state during a measurement process. 0 =
 * <i>inactive</i>; 1 = <i>active</i>. Default: <b>1</b>
 * <p>
 * <b>use Xively</b>: Whether the sensor uses Xively or not. 0 = <i>false</i>; 1
 * = <i>true</i>. Default: <b>0</b>
 * <p>
 * <b>datastream</b>: The sensor's <i>datastream</i> or <i>channel</i> on
 * Xively. Default: <b><i>empty string</i></b>
 * 
 * @author kaolick
 */
@SuppressWarnings("serial")
public class IodIOIOSensor implements Serializable
{
    // Logging tag
    @SuppressWarnings("unused")
    private static final String TAG = "IodIOIOSensor";

    // Sensor attributes
    private int sensorID, frequency, timeUnit, pinNumber, inputType,
	    measurementType, thresholdType, state, useXively;
    private double threshold;
    private String name, datastream;

    // Static values for database handling etc.
    public static final int INPUT_TYPE_ANALOG = 0;
    public static final int INPUT_TYPE_DIGITAL = 1;
    public static final int MEASUREMENT_TYPE_ABSOLUTE = 0;
    public static final int MEASUREMENT_TYPE_RELATIVE = 1;
    public static final int MEASUREMENT_TYPE_FLOATING = 2;
    public static final int MEASUREMENT_TYPE_PULL_DOWN = 3;
    public static final int MEASUREMENT_TYPE_PULL_UP = 4;
    public static final int STATE_INACTIVE = 0;
    public static final int STATE_ACTIVE = 1;
    public static final int THRESHOLD_TYPE_LOWER = 0;
    public static final int THRESHOLD_TYPE_UPPER = 1;
    public static final int TIME_UNIT_SECONDS = 1000;
    public static final int TIME_UNIT_MINUTES = 60 * TIME_UNIT_SECONDS;
    public static final int TIME_UNIT_HOURS = 60 * TIME_UNIT_MINUTES;
    public static final int UPLOADED_FALSE = 0;
    public static final int UPLOADED_TRUE = 1;
    public static final int USE_XIVELY_FALSE = 0;
    public static final int USE_XIVELY_TRUE = 1;

    /**
     * Class constructor with default values.
     * 
     * @see IodIOIOSensor
     */
    public IodIOIOSensor()
    {
	this.sensorID = getRandomInt(1000000000, 2000000000);
	this.name = "";
	this.pinNumber = 0;
	this.frequency = 0;
	this.timeUnit = TIME_UNIT_SECONDS;
	this.inputType = INPUT_TYPE_ANALOG;
	this.measurementType = MEASUREMENT_TYPE_ABSOLUTE;
	this.threshold = 0;
	this.thresholdType = THRESHOLD_TYPE_LOWER;
	this.state = STATE_ACTIVE;
	this.useXively = USE_XIVELY_FALSE;
	this.datastream = "";
    }

    /**
     * Creates a random <code>int</code> between a minimum and maximum value.
     * Used for creating the sensor's ID.
     * 
     * @param _min
     *            The minimum value
     * @param _max
     *            The maximum value
     * @return The random <code>int</code>
     */
    public int getRandomInt(int _min, int _max)
    {
	Random random = new Random();

	return random.nextInt(_max - _min) + _min;
    }

    // *************** Database ***************

    /**
     * Saves the given <i>value</i> and <i>timestamp</i> in the according
     * database table.
     * 
     * @param _value
     *            The measured sensor value
     * @param _timestamp
     *            The timestamp of the measurement
     * @param _databaseManager
     *            A {@link IodDatabaseManager} to access the application's
     *            database
     * @return <code>true</code> if saving was successful, <code>false</code>
     *         otherwise.
     */
    public boolean saveValueInDatabase(float _value,
				       String _timestamp,
				       IodDatabaseManager _databaseManager)
    {
	boolean success = false;

	// If measuring did not fail...
	if (_value != -1)
	{
	    // Create the table name
	    String tableName = MeasurementTable.TABLE_NAME_PRE_TAG
		    + getSensorID();

	    // Save according to the threshold type
	    switch (getThresholdType())
	    {
	    case THRESHOLD_TYPE_LOWER:
		// If the measured value equals or is higher than the sensor's
		// configured threshold, save the value in the database
		if (_value >= getThreshold())
		{
		    // Save value in the according database table
		    _databaseManager.saveMeasuredSensorValue(tableName, _value,
			    _timestamp);

		    success = true;
		}
		return success;
	    case THRESHOLD_TYPE_UPPER:
		// If the measured value equals or is lower than the sensor's
		// configured threshold, save the value in the database
		if (_value <= getThreshold())
		{
		    // Save value in the according database table
		    _databaseManager.saveMeasuredSensorValue(tableName, _value,
			    _timestamp);

		    success = true;

		}
		return success;
	    default:
		return false;
	    }
	}
	else
	{
	    return false;
	}
    }

    // *************** Measuring ***************

    /**
     * Measures the sensor's value from an {@link AnalogInput} according to the
     * sensor's configuration.
     * 
     * @param _ioio
     *            The connected {@link IOIO}
     * @return The measured value; -1 if measuring failed.
     */
    private float readAnalogInput(IOIO _ioio)
    {
	// Return value; set to -1 before measuring
	float value = -1;

	// Create analog input
	AnalogInput analogInput = null;

	// Try to open analog input
	try
	{
	    analogInput = _ioio.openAnalogInput(getPinNumber());
	}
	catch (ConnectionLostException _exception)
	{
	    _exception.printStackTrace();
	}

	// Measure value
	if (analogInput != null)
	{
	    try
	    {
		switch (getMeasurementType())
		{
		case MEASUREMENT_TYPE_ABSOLUTE:
		    value = analogInput.getVoltage();
		    break;
		case MEASUREMENT_TYPE_RELATIVE:
		    value = analogInput.read();
		    break;
		}
	    }
	    catch (InterruptedException _exception)
	    {
		_exception.printStackTrace();
	    }
	    catch (ConnectionLostException _exception)
	    {
		_exception.printStackTrace();
	    }

	    // Close analog input
	    analogInput.close();
	}

	return value;
    }

    /**
     * Measures the sensor's value from a {@link DigitalInput} according to the
     * sensor's configuration.
     * 
     * @param _ioio
     *            The connected {@link IOIO}
     * @return 0 for <i>LOW</i> (0V); 1 for <i>HIGH</i> (3.3V or 5V); -1 if
     *         measuring failed.
     */
    private float readDigitalInput(IOIO _ioio)
    {
	// Return value; set to -1
	float value = -1;

	// Create analog input
	DigitalInput digitalInput = null;

	// Try to open digital input according to the measurement type
	try
	{
	    switch (getMeasurementType())
	    {
	    case MEASUREMENT_TYPE_FLOATING:
		digitalInput = _ioio.openDigitalInput(getPinNumber());
		break;
	    case MEASUREMENT_TYPE_PULL_DOWN:
		digitalInput = _ioio.openDigitalInput(getPinNumber(),
			DigitalInput.Spec.Mode.PULL_DOWN);
		break;
	    case MEASUREMENT_TYPE_PULL_UP:
		digitalInput = _ioio.openDigitalInput(getPinNumber(),
			DigitalInput.Spec.Mode.PULL_UP);
		break;
	    }
	}
	catch (ConnectionLostException _exception)
	{
	    _exception.printStackTrace();
	}

	// Measure value
	if (digitalInput != null)
	{
	    try
	    {
		boolean digitalValue = digitalInput.read();

		// Sensor returns TRUE for HIGH
		if (digitalValue)
		{
		    value = 1;
		}
		// Sensor returns FALSE for LOW
		else
		{
		    value = 0;
		}
	    }
	    catch (InterruptedException _exception)
	    {
		_exception.printStackTrace();
	    }
	    catch (ConnectionLostException _exception)
	    {
		_exception.printStackTrace();
	    }

	    // Close digital input
	    digitalInput.close();
	}

	return value;
    }

    /**
     * Measures the sensor's value according to the sensor's configuration.
     * 
     * @param _ioio
     *            The connected {@link IOIO}
     * @return The measured value if using {@link AnalogInput}, 0 or 1 if using
     *         {@link DigitalInput}, or -1 if measuring failed.
     */
    public float measureValue(IOIO _ioio)
    {
	// Return value; set to -1
	float value = -1;

	switch (getInputType())
	{
	case INPUT_TYPE_ANALOG:
	    value = readAnalogInput(_ioio);
	    break;
	case INPUT_TYPE_DIGITAL:
	    value = readDigitalInput(_ioio);
	default:
	    break;
	}

	return value;
    }

    // *************** Getters & Setters ***************

    public int getSensorID()
    {
	return sensorID;
    }

    public void setSensorID(int _sensorID)
    {
	this.sensorID = _sensorID;
    }

    public String getName()
    {
	return name;
    }

    public void setName(String _name)
    {
	this.name = _name;
    }

    public int getPinNumber()
    {
	return pinNumber;
    }

    public void setPinNumber(int _pinNumber)
    {
	this.pinNumber = _pinNumber;
    }

    public int getFrequency()
    {
	return frequency;
    }

    public void setFrequency(int _frequency)
    {
	this.frequency = _frequency;
    }

    public int getTimeUnit()
    {
	return timeUnit;
    }

    public void setTimeUnit(int _timeUnit)
    {
	this.timeUnit = _timeUnit;
    }

    public int getInputType()
    {
	return inputType;
    }

    public void setInputType(int _inputType)
    {
	this.inputType = _inputType;
    }

    public int getMeasurementType()
    {
	return measurementType;
    }

    public void setMeasurementType(int _measurementType)
    {
	this.measurementType = _measurementType;
    }

    public double getThreshold()
    {
	return threshold;
    }

    public void setThreshold(double _threshold)
    {
	this.threshold = _threshold;
    }

    public int getThresholdType()
    {
	return thresholdType;
    }

    public void setThresholdType(int _thresholdType)
    {
	this.thresholdType = _thresholdType;
    }

    public int getState()
    {
	return state;
    }

    public void setState(int state)
    {
	this.state = state;
    }

    public int getUseXively()
    {
	return useXively;
    }

    public void setUseXively(int _useXively)
    {
	this.useXively = _useXively;
    }

    public String getDatastream()
    {
	return datastream;
    }

    public void setDatastream(String _datastream)
    {
	this.datastream = _datastream;
    }
}