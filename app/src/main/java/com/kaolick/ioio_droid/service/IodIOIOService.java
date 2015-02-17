package com.kaolick.ioio_droid.service;

import ioio.lib.api.IOIO;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOService;

import java.util.ArrayList;
import java.util.List;
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
 * Handles the connection to a <code>IOIO</code> and measuring sensor data.
 * 
 * @author kaolick
 * @see IOIOService
 */
public class IodIOIOService extends IOIOService
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

    // *************** Service Life Cycle ***************

    @Override
    protected IOIOLooper createIOIOLooper()
    {
	return new BaseIOIOLooper()
	{
	    @Override
	    public void disconnected()
	    {
		super.disconnected();

		// Inform the user
		mToastHandler.showToast(R.string.toast_ioio_disconnected,
			Toast.LENGTH_SHORT);
	    }

	    @Override
	    public void incompatible()
	    {
		super.incompatible();

		// Inform the user
		mToastHandler.showToast(R.string.toast_ioio_incompatible,
			Toast.LENGTH_SHORT);
	    }

	    @Override
	    public void loop() throws ConnectionLostException,
			      InterruptedException
	    {
		super.loop();
	    }

	    @Override
	    protected void setup() throws ConnectionLostException,
				  InterruptedException
	    {
		super.setup();

		// Create the timer tasks for the sensors
		mSensorTimerTasks = createSensorTimerTasks(ioio_);

		// Start the measuring process
		startMeasuring();
	    }
	};
    }

    @Override
    public IBinder onBind(Intent intent)
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
	    // Upload latest location data
	    mUploadManager.startUploadingLocation();
	    mUploadManager.startUploadingMeasurements(mSensorIDs);
	}

	handleWakeLock();

	super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent _intent, int _flags, int _startId)
    {
	super.onStartCommand(_intent, _flags, _startId);

	// Start the Service in foreground with higher priority
	startForeground(NOTIFICATION_ID, getNotification());

	handleWakeLock();

	return Service.START_REDELIVER_INTENT;
    }

    // *************** Initializing ***************

    /**
     * Creates a list of {@link IOIOSensorTimerTask}s, one for each non-paused
     * sensor.
     * 
     * @param _ioio
     *            The connected <code>IOIO</code>
     * @return The list of <code>IOIOSensorTimerTask</code>s
     */
    private List<IOIOSensorTimerTask> createSensorTimerTasks(IOIO _ioio)
    {
	// Create a new empty list for the IOIOSensorTimerTasks
	List<IOIOSensorTimerTask> sensorTimerTasks = new ArrayList<IOIOSensorTimerTask>();

	// Create a IOIOSensorTimerTasks for each sensor in the list
	if (mSensors != null && !mSensors.isEmpty())
	{
	    for (int i = 0; i < mSensors.size(); i++)
	    {
		IOIOSensorTimerTask sensorTimerTask = new IOIOSensorTimerTask(
			mSensors.get(i), _ioio);

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

    // *************** Measuring Process ***************

    /**
     * Starts the measuring process.
     */
    public void startMeasuring()
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
     * Returns a {@link Notification} for the <code>IodIOIOService</code>. When
     * the user clicks on the <code>Notification</code>, the application's
     * {@link MainActivity} is called.
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
     * Handles measuring sensor values.
     * 
     * @author kaolick
     * @see TimerTask
     */
    class IOIOSensorTimerTask extends TimerTask
    {
	// The sensor
	private IodIOIOSensor sensor;

	// The IOIO
	private IOIO ioio;

	/**
	 * Class constructor.
	 * 
	 * @param _sensor
	 *            The {@link IodIOIOSensor} for which the instance of this
	 *            class handles the measuring of sensor data.
	 * @param _ioio
	 *            The connected <code>IOIO</code>
	 */
	public IOIOSensorTimerTask(IodIOIOSensor _sensor, IOIO _ioio)
	{
	    this.sensor = _sensor;
	    this.ioio = _ioio;
	}

	@Override
	public void run()
	{
	    // The timestamp for the measuring
	    String timestamp = Store.getTimestamp();

	    // Measure the value
	    float value = sensor.measureValue(ioio);

	    // Save the value in the database
	    sensor.saveValueInDatabase(value, timestamp, mDatabaseManager);
	}
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