package com.kaolick.ioio_droid.location;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.location.Location;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.kaolick.ioio_droid.R;
import com.kaolick.ioio_droid.database.IodDatabaseManager;
import com.kaolick.ioio_droid.store.Store;
import com.kaolick.ioio_droid.toast.ToastHandler;
import com.kaolick.ioio_droid.ui.SettingsActivity;
import com.kaolick.ioio_droid.xively.UploadManager;

/**
 * Manages location updates.
 * 
 * @author kaolick
 */
public class IodLocationManager implements
			       GooglePlayServicesClient.ConnectionCallbacks,
			       GooglePlayServicesClient.OnConnectionFailedListener,
			       LocationListener
{
    // Logging tag
    @SuppressWarnings("unused")
    private static final String TAG = "IodLocationManager";

    // General attributes
    private Context mContext;
    private IodDatabaseManager mDatabaseManager;
    private SharedPreferences mPrefs;
    private Resources mResources;
    private ToastHandler mToastHandler;
    private UploadManager mUploadManager;

    // Location attributes
    private LocationClient mLocationClient;
    private LocationRequest mLocationRequest;

    // *************** Life Cycle ***************

    /**
     * Class constructor.
     * 
     * @param _context
     *            to use for accessing the application's database, resources,
     *            shared preferences and more...
     */
    public IodLocationManager(Context _context)
    {
	this.mContext = _context;
	this.mDatabaseManager = new IodDatabaseManager(_context);
	this.mPrefs = SettingsActivity.getPrefs((ContextWrapper) _context);
	this.mResources = _context.getResources();
	this.mToastHandler = new ToastHandler(_context);
	this.mUploadManager = new UploadManager(_context, null, false);
    }

    /**
     * Connects the {@link LocationClient}.
     */
    public void connect()
    {
	mLocationClient = new LocationClient(mContext, this, this);
	mLocationRequest = LocationRequest.create();
	mLocationRequest.setInterval(getLocationUpdatePeriod());
	mLocationRequest
		.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

	// Connect the location client
	if (!mLocationClient.isConnected())
	{
	    mLocationClient.connect();
	}
    }

    /**
     * Stops location updates and disconnects the {@link LocationClient}.
     */
    public void disconnect()
    {
	if (mLocationClient.isConnected())
	{
	    // Stop location updates and disconnect the location client
	    mLocationClient.removeLocationUpdates(this);
	    mLocationClient.disconnect();
	}
    }

    // *************** Google Play Service ***************

    @Override
    public void onConnectionFailed(ConnectionResult _connectionResult)
    {
	// Display the connection status
	mToastHandler.showToast(
		R.string.toast_location_client_connection_failed,
		Toast.LENGTH_SHORT);

	// Try to connect again
	mLocationClient.connect();
    }

    @Override
    public void onConnected(Bundle _dataBundle)
    {
	// Display the connection status
	mToastHandler.showToast(R.string.toast_location_client_connected,
		Toast.LENGTH_SHORT);

	// Start requesting location updates
	mLocationClient.requestLocationUpdates(mLocationRequest, this);
    }

    @Override
    public void onDisconnected()
    {
	// Display the connection status
	mToastHandler.showToast(R.string.toast_location_client_disconnected,
		Toast.LENGTH_SHORT);

	// Try to re-connect
	mLocationClient.connect();
    }

    // *************** Location ***************

    /**
     * Returns the location update period from the application's
     * {@link SharedPreferences}.
     * 
     * @return The location update period in milliseconds
     */
    private long getLocationUpdatePeriod()
    {
	// Get the frequency
	long freq = Long.parseLong(mPrefs.getString(mResources
		.getString(R.string.pref_location_updates_frequency_key), "1"));

	// Get the time unit
	long unit = Long.parseLong(mPrefs.getString(mResources
		.getString(R.string.pref_location_updates_time_unit_key),
		"3600000"));

	return freq * unit;
    }

    @Override
    public void onLocationChanged(Location _location)
    {
	// Save the location in the database
	mDatabaseManager.saveLocation(Store.getTimestamp(),
		_location.getAltitude(), _location.getLatitude(),
		_location.getLongitude());

	// If automatic upload is activated...
	if (mPrefs
		.getBoolean(mResources
			.getString(R.string.pref_xively_upload_automatic_key),
			false))
	{
	    // Upload location
	    mUploadManager.startUploadingLocation();
	}
    }
}