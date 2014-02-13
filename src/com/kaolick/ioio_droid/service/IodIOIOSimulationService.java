package com.kaolick.ioio_droid.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import com.kaolick.ioio_droid.R;
import com.kaolick.ioio_droid.database.IodDatabaseManager;
import com.kaolick.ioio_droid.location.IodLocationManager;
import com.kaolick.ioio_droid.sensor.IodIOIOSensor;
import com.kaolick.ioio_droid.store.Store;
import com.kaolick.ioio_droid.toast.ToastHandler;
import com.kaolick.ioio_droid.ui.MainActivity;
import com.kaolick.ioio_droid.ui.SettingsActivity;
import com.kaolick.ioio_droid.xively.UploadManager;

/**
 * Simulates the connection to a <code>IOIO</code> and measuring sensor data.
 * 
 * @author kaolick
 * @see Service
 */
public class IodIOIOSimulationService extends Service
{
    // General attributes
    private Context mContext;
    private IodDatabaseManager mDatabaseManager;
    private IodLocationManager mLocationManager;
    private PowerManager mPowerManager;
    private PowerManager.WakeLock mWakeLock;
    private SharedPreferences mPrefs;
    private Resources mResources;
    private ToastHandler mToastHandler;
    private UploadManager mUploadManager;
    private boolean usePositioning;

    // Notification ID
    private static final int NOTIFICATION_ID = 123;

    // Sensor attributes
    private List<IodIOIOSensor> mSensors;
    private Integer[] mSensorIDs;
    private List<IOIOSensorTimerTask> mSensorTimerTasks;
    private List<Timer> mTimers;

    // Upload attributes
    private Timer mUploadTimer;
    private UploadTimerTask mUploadTimerTask;
    private boolean useAutomaticUpload;

    // *************** Service life cycle ***************

    @Override
    public IBinder onBind(Intent _intent)
    {
	// Binding is not used
	return null;
    }

    @Override
    public void onCreate()
    {
	super.onCreate();

	// Initialize attributes
	mContext = getApplicationContext();
	mDatabaseManager = new IodDatabaseManager(mContext);
	mLocationManager = new IodLocationManager(mContext);
	mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
	mWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
		getClass().getName());
	mPrefs = SettingsActivity.getPrefs((ContextWrapper) mContext);
	mResources = mContext.getResources();
	mToastHandler = new ToastHandler(mContext);
	mUploadManager = new UploadManager(mContext, null, false);
	mSensors = mDatabaseManager.getActiveSensors();
	mSensorIDs = mDatabaseManager.getSensorIDsOfXivelyUsingActiveSensors();
	mSensorTimerTasks = createSensorTimerTasks();
	mTimers = createTimers();
	usePositioning = mPrefs.getBoolean(mResources
		.getString(R.string.pref_location_location_service_key), false);
	useAutomaticUpload = mPrefs
		.getBoolean(mResources
			.getString(R.string.pref_xively_upload_automatic_key),
			false);
    }

    @Override
    public void onDestroy()
    {
	// Stop location updates
	if (usePositioning)
	{
	    mLocationManager.disconnect();
	}

	// Cancel all SensorTimerTasks
	if (mSensorTimerTasks != null)
	{
	    for (int i = 0; i < mSensorTimerTasks.size(); i++)
	    {
		mSensorTimerTasks.get(i).cancel();
	    }
	}

	// Cancel UploadTimerTask
	if (mUploadTimerTask != null)
	{
	    mUploadTimerTask.cancel();
	}

	// If automatic upload is activated...
	if (useAutomaticUpload)
	{
	    // Upload latest data
	    mUploadManager.startUploadingLocation();
	    mUploadManager.startUploadingMeasurements(mSensorIDs);
	}

	handleWakeLock();

	super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
	// Start the Service in foreground with higher priority
	startForeground(NOTIFICATION_ID, getNotification());

	handleWakeLock();

	startMeasuringSimulation();

	return Service.START_REDELIVER_INTENT;
    }

    // *************** Initializing ***************

    /**
     * Creates a list of {@link IOIOSensorTimerTask}s, one for each non-paused
     * sensor.
     * 
     * @return The list of <code>IOIOSensorTimerTask</code>s
     */
    private List<IOIOSensorTimerTask> createSensorTimerTasks()
    {
	// Create a new empty list for the IOIOSensorTimerTasks
	List<IOIOSensorTimerTask> sensorTimerTasks = new ArrayList<IOIOSensorTimerTask>();

	if (mSensors != null && !mSensors.isEmpty())
	{
	    for (int i = 0; i < mSensors.size(); i++)
	    {
		// Create a IOIOSensorTimerTasks for each sensor in the list
		IOIOSensorTimerTask sensorTimerTask = new IOIOSensorTimerTask(
			mSensors.get(i));

		// Add it to the list
		sensorTimerTasks.add(sensorTimerTask);
	    }
	}

	return sensorTimerTasks;
    }

    /**
     * Creates a list of {@link Timer}s, one for each sensor in the given sensor
     * list.
     * 
     * @return The list of <code>Timer</code>s
     */
    private List<Timer> createTimers()
    {
	// Create a new empty list of timers
	List<Timer> timers = new ArrayList<Timer>();

	// Create a timer for each sensor
	for (int i = 0; i < mSensors.size(); i++)
	{
	    timers.add(new Timer());
	}

	return timers;
    }

    /**
     * Initializes the automatic upload if activated by the user.
     */
    private void initUpload()
    {
	// If automatic upload is activated in the application's settings...
	if (useAutomaticUpload)
	{
	    // If the array is not empty...
	    if (mSensorIDs.length > 0)
	    {
		// Initialize Timer and UploadTimerTask
		mUploadTimer = new Timer();
		mUploadTimerTask = new UploadTimerTask(mSensorIDs);

		// Get the upload period
		long freq = Long.parseLong(mPrefs.getString((mResources
			.getString(R.string.pref_xively_upload_frequency_key)),
			"1"));
		long timeUnit = Long.parseLong(mPrefs.getString(mResources
			.getString(R.string.pref_xively_upload_time_unit_key),
			"3600000"));
		long period = freq * timeUnit;

		// Start the UploadTimerTask
		mUploadTimer.scheduleAtFixedRate(mUploadTimerTask, period,
			period);
	    }
	}
    }

    // *************** Measuring Simulation Process ***************

    /**
     * Starts the simulation of the measuring process.
     */
    public void startMeasuringSimulation()
    {
	// Inform the user
	mToastHandler.showToast(R.string.toast_measuring_started,
		Toast.LENGTH_SHORT);

	// Initialize upload
	initUpload();

	// Start location updates
	if (usePositioning)
	{
	    mLocationManager.connect();
	}

	// Start a timer task for each sensor
	for (int i = 0; i < mSensorTimerTasks.size(); i++)
	{
	    // Get the current IOIOSensorTimerTask
	    IOIOSensorTimerTask currentTask = mSensorTimerTasks.get(i);

	    // Get the according timer
	    Timer timer = mTimers.get(i);

	    // Create the measuring period
	    int freq = currentTask.sensor.getFrequency();
	    int timeUnit = currentTask.sensor.getTimeUnit();
	    long period = (long) (freq * timeUnit);

	    // Start the IOIOSensorTimerTasks
	    timer.scheduleAtFixedRate(currentTask, 0, period);
	}
    }

    // *************** Notification ***************

    /**
     * Returns a {@link Notification} for the
     * <code>IodIOIOSimulationService</code>. When the user clicks on the
     * <code>Notification</code>, the application's {@link MainActivity} is
     * called.
     * 
     * @return the <code>Notification</code>
     */
    public Notification getNotification()
    {
	// Build the Notification
	NotificationCompat.Builder builder = new NotificationCompat.Builder(
		this)
		.setSmallIcon(R.drawable.notification_icon)
		.setContentTitle(
			getResources().getString(R.string.notification_title))
		.setContentText(
			getResources().getString(R.string.notification_text));

	// Create a PendingIntent for opening the MainActivity
	Intent notificationIntent = new Intent(this, MainActivity.class);
	PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
		notificationIntent, 0);
	builder.setContentIntent(pendingIntent);

	// Return the Notification
	return builder.build();
    }

    // *************** Power Management ***************

    /**
     * Handles wakelock behaviour according to the application settings.
     */
    @SuppressLint("Wakelock")
    public void handleWakeLock()
    {
	// If the user has activated "Keep CPU awake"...
	if (mPrefs
		.getBoolean(mResources
			.getString(R.string.pref_general_wakelock_key), false))
	{
	    if (!mWakeLock.isHeld())
	    {
		mWakeLock.acquire();
	    }
	    else
	    {
		mWakeLock.release();
	    }
	}
    }

    // *************** SensorTimerTask ***************

    /**
     * Handles the simulation of measuring sensor values.
     * 
     * @author kaolick
     * @see TimerTask
     */
    class IOIOSensorTimerTask extends TimerTask
    {
	// The sensor
	private IodIOIOSensor sensor;

	/**
	 * Class constructor.
	 * 
	 * @param _sensor
	 *            The {@link IodIOIOSensor} for which the instance of this
	 *            class handles the simulation of measuring.
	 */
	public IOIOSensorTimerTask(IodIOIOSensor _sensor)
	{
	    this.sensor = _sensor;
	}

	@Override
	public void run()
	{
	    // The sensor's value; set to -1 before the value gets measured
	    float value = -1;

	    // The timestamp for the measuring
	    String timestamp = Store.getTimestamp();

	    // If the sensor's input type is analog...
	    if (sensor.getInputType() == IodIOIOSensor.INPUT_TYPE_ANALOG)
	    {
		// Simulate getting the sensor's absolute voltage value
		if (sensor.getMeasurementType() == IodIOIOSensor.MEASUREMENT_TYPE_ABSOLUTE)
		{
		    // Get a value between 0 and 3.3
		    value = (float) getRandomDouble(0, 3.3);
		}
		// Simulate getting the sensor's relative voltage value
		else if (sensor.getMeasurementType() == IodIOIOSensor.MEASUREMENT_TYPE_RELATIVE)
		{
		    // Get a value between 0 and 1; this range covers the entire
		    // range of permitted voltage values
		    value = (float) getRandomDouble(0, 1);
		}
	    }
	    // ...the sensor's input type is digital
	    else
	    {
		// Get a random value between 0 and 1
		double random = getRandomDouble(0, 1);

		// Sensor returns TRUE for HIGH
		if (random < 0.5)
		{
		    value = 0;
		}
		// Sensor returns FALSE for LOW
		else
		{
		    value = 1;
		}
	    }

	    // Save the value in the database
	    sensor.saveValueInDatabase(value, timestamp, mDatabaseManager);
	}
    }

    // *************** Simulation ***************

    /**
     * Creates a random <code>double</code> value between a minimum and maximum
     * value.
     * 
     * @param _min
     *            The minimum value
     * @param _max
     *            The maximum value
     * @return the random <code>double</code> value
     */
    public double getRandomDouble(double _min, double _max)
    {
	Random random = new Random();

	return _min + (_max - _min) * random.nextDouble();
    }

    // *************** UploadTimerTask ***************

    /**
     * Handles the automatic upload of measured sensor values.
     * 
     * @author kaolick
     * @see TimerTask
     */
    class UploadTimerTask extends TimerTask
    {
	private Integer[] sensorIDs;

	/**
	 * Class constructor.
	 * 
	 * @param _sensorIDs
	 *            An {@link Array} of sensor IDs of {@link IodIOIOSensor}s
	 *            that upload data automatically to <i>xively.com</i>
	 */
	public UploadTimerTask(Integer[] _sensorIDs)
	{
	    this.sensorIDs = _sensorIDs;
	}

	@Override
	public void run()
	{
	    mUploadManager.startUploadingMeasurements(sensorIDs);
	    mUploadManager.startUploadingLocation();
	}
    }
}