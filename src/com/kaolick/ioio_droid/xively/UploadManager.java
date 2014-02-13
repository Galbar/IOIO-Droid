package com.kaolick.ioio_droid.xively;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ListView;
import android.widget.Toast;

import com.kaolick.ioio_droid.R;
import com.kaolick.ioio_droid.database.IodDatabaseManager;
import com.kaolick.ioio_droid.database.LocationSchema;
import com.kaolick.ioio_droid.sensor.IodIOIOSensor;
import com.kaolick.ioio_droid.toast.ToastHandler;
import com.kaolick.ioio_droid.ui.IOIOSensorMeasurementsActivity;
import com.kaolick.ioio_droid.ui.SettingsActivity;

/**
 * Handles the data upload to <i>xively.com</i> via {@link AsyncTask}.
 * 
 * @author kaolick
 */
public class UploadManager
{
    // General attributes
    private IOIOSensorMeasurementsActivity mActivity;
    private Context mContext;
    private IodDatabaseManager mDatabaseManager;
    private SharedPreferences mPrefs;
    private ToastHandler mToastHandler;

    // URL attributes
    public final String URL_BASE = "https://api.xively.com/v2/feeds/";
    public final String DATASTREAMS = "/datastreams/";
    public final String DATAPOINTS = "/datapoints";

    /**
     * Class constructor.
     * 
     * @param _context
     *            to access the application's database,
     *            {@link SharedPreferences} and more...
     * @param _activity
     *            The {@link Activity} an instance of this class gets created in
     * @param _createdInMeasurementsActivity
     *            <b>CAUTION:</b> Set this parameter to <code>true</code> only
     *            if an instance of this class is created in the
     *            {@link IOIOSensorMeasurementsActivity}. This way the
     *            {@link ListView} with the measurements can be updated after a
     *            successful upload.
     */
    public UploadManager(Context _context,
			 Activity _activity,
			 boolean _createdInMeasurementsActivity)
    {
	this.mContext = _context;
	this.mDatabaseManager = new IodDatabaseManager(_context);
	this.mPrefs = SettingsActivity.getPrefs((ContextWrapper) _context);
	this.mToastHandler = new ToastHandler(_context);

	if (_createdInMeasurementsActivity)
	{
	    this.mActivity = (IOIOSensorMeasurementsActivity) _activity;
	}
	else
	{
	    this.mActivity = null;
	}
    }

    // *************** Checking ***************

    /**
     * Checks if there is an API key saved in the database.
     * 
     * @return <code>true</code> if there is no API key saved in the
     *         {@link SharedPreferences}, <code>false</code> otherwise.
     */
    public boolean isAPIKeyEmpty()
    {
	if (mPrefs.getString(
		mContext.getResources().getString(
			R.string.pref_xively_api_key_key), "").equals(""))
	{
	    // Inform the user
	    mToastHandler.showToast(R.string.toast_no_api_key,
		    Toast.LENGTH_SHORT);

	    return true;
	}
	else
	{
	    return false;
	}
    }

    /**
     * Checks if there is a feed ID saved in the database.
     * 
     * @return <code>true</code> if there is no feed ID saved in the
     *         {@link SharedPreferences}, <code>false</code> otherwise.
     */
    public boolean isFeedIDEmpty()
    {
	if (mPrefs.getString(
		mContext.getResources().getString(
			R.string.pref_xively_feed_id_key), "-1").equals("-1"))
	{
	    // Inform the user
	    mToastHandler.showToast(R.string.toast_no_feed_id,
		    Toast.LENGTH_SHORT);

	    return true;
	}
	else
	{
	    return false;
	}
    }

    /**
     * Checks if the device has internet connection.
     * 
     * @return <code>true</code> if the device has internet connection,
     *         <code>false</code> otherwise.
     */
    public boolean isOnline()
    {
	ConnectivityManager cm = (ConnectivityManager) mContext
		.getSystemService(Context.CONNECTIVITY_SERVICE);

	NetworkInfo netInfo = cm.getActiveNetworkInfo();

	if (netInfo != null && netInfo.isConnected())
	{
	    return true;
	}
	else
	{
	    // Inform the user that there is no internet connection
	    mToastHandler.showToast(R.string.toast_no_internet_connection,
		    Toast.LENGTH_SHORT);

	    return false;
	}
    }

    // *************** Upload ***************

    /**
     * Uploads location data from the database to <i>xively.com</i>.
     * 
     * @return <code>true</code> if the upload was successful,
     *         <code>false</code> otherwise.
     */
    public boolean uploadLoaction()
    {
	// Return value; set to false
	boolean requestSuccessful = false;

	// Get the cursor with the coordinates to upload
	Cursor coordinates = mDatabaseManager.getLastLocation();

	// Check if the coordinates have already been uploaded
	if (coordinates.getInt(coordinates
		.getColumnIndex(LocationSchema.UPLOADED)) == 1)
	{
	    // Inform the user
	    mToastHandler.showToast(R.string.toast_location_already_uploaded,
		    Toast.LENGTH_SHORT);

	    // Stop the upload
	    return false;
	}

	// Row id of the location
	int id = -1;

	if (coordinates.getCount() > 0)
	{
	    id = coordinates.getInt(coordinates
		    .getColumnIndex(LocationSchema.ID));
	}

	// No values; no request necessary
	if (id == -1)
	{
	    // Inform the user
	    mToastHandler.showToast(R.string.toast_upload_no_values,
		    Toast.LENGTH_SHORT);

	    // Stop the upload
	    return false;
	}

	// Create the request body
	String requestBody = mDatabaseManager
		.createJSONMessageForLocations(coordinates);

	// Close the cursor
	coordinates.close();

	// Get the API key from the preferences
	String apiKey = mPrefs.getString(
		mContext.getResources().getString(
			R.string.pref_xively_api_key_key), "");

	// Get the feed ID
	int feedID = Integer.parseInt(mPrefs.getString(mContext.getResources()
		.getString(R.string.pref_xively_feed_id_key), "-1"));

	// Create the Uri; Example: //
	// https://api.xively.com/v2/feeds/THE_FEED_ID
	String url = URL_BASE + feedID;

	// Create a default HttpClient
	HttpClient httpClient = new DefaultHttpClient();

	// Create an HttpPut with the URL
	HttpPut httpPut = new HttpPut(url);

	// Create an HttpResponse
	HttpResponse httpResponse = null;

	// Make the request
	try
	{
	    // Set the StringEntity with the request body
	    httpPut.setEntity(new StringEntity(requestBody));

	    // Add headers
	    httpPut.addHeader("X-ApiKey", apiKey);
	    httpPut.addHeader(HTTP.CONTENT_TYPE, "application/json");

	    // Execute Request
	    httpResponse = httpClient.execute(httpPut);

	    // Try updating the database depending on the response
	    if (httpResponse != null)
	    {
		// Status code of the response
		int statusCode = httpResponse.getStatusLine().getStatusCode();
		Log.d("status code", "" + statusCode);

		// If request was successful...
		if (statusCode == HttpURLConnection.HTTP_OK)
		{
		    // Mark values as uploaded
		    mDatabaseManager.markCoordinatesAsUploaded(id);

		    requestSuccessful = true;
		}
		else
		{
		    // Inform the user
		    mToastHandler.showToast(R.string.toast_upload_failed,
			    Toast.LENGTH_SHORT);
		}
	    }
	}
	catch (UnsupportedEncodingException _exception)
	{
	    _exception.printStackTrace();
	}
	catch (ClientProtocolException _exception)
	{
	    _exception.printStackTrace();
	}
	catch (IOException _exception)
	{
	    _exception.printStackTrace();
	}

	return requestSuccessful;
    }

    /**
     * Uploads measured values from the database for the {@link IodIOIOSensor}
     * with the given sensor ID to <i>xively.com</i>.
     * 
     * @param _sensorID
     *            The sensor ID
     * @return <code>true</code> if the upload was successful,
     *         <code>false</code> otherwise.
     */
    public boolean uploadMeasurements(int _sensorID)
    {
	// Return value; set to false
	boolean requestSuccessful = false;

	// Get the cursor with the measurements to upload
	Cursor measurements = mDatabaseManager
		.getMeasurementsForUpload(_sensorID);

	// Get the according sensor
	IodIOIOSensor sensor = mDatabaseManager
		.getIOIOSensorBySensorID(_sensorID);

	// If the sensor does not use Xively...
	if (sensor.getUseXively() == IodIOIOSensor.USE_XIVELY_FALSE)
	{
	    // Inform the user
	    mToastHandler.showToast(R.string.toast_sensor_not_using_xively,
		    Toast.LENGTH_SHORT);

	    // Stop the upload
	    return false;
	}

	// List with all row IDs of measurements that have not been uploaded yet
	List<Integer> ids = mDatabaseManager
		.getIDsOfNotYetUploadedMeasurements(measurements);

	// No values; no request necessary
	if (ids.isEmpty())
	{
	    mToastHandler.showToast(R.string.toast_upload_no_values,
		    Toast.LENGTH_SHORT);

	    // Stop the upload
	    return false;
	}

	// Create the request body
	String requestBody = mDatabaseManager.createJSONMessageForMeasurements(
		measurements, sensor.getDatastream());

	// Close the cursor
	measurements.close();

	// Get the API key from the preferences
	String apiKey = mPrefs.getString(
		mContext.getResources().getString(
			R.string.pref_xively_api_key_key), "");

	// Get the feed ID
	int feedID = Integer.parseInt(mPrefs.getString(mContext.getResources()
		.getString(R.string.pref_xively_feed_id_key), "-1"));

	// Create the Uri; Example:
	// https://api.xively.com/v2/feeds/THE_FEED_ID/datastreams/THE_DATASTREAM/datapoints
	// String url = URL_BASE + feedID + DATASTREAMS + sensor.getDatastream()
	// + DATAPOINTS;
	String url = URL_BASE + feedID;

	// Create a default HttpClient
	HttpClient httpClient = new DefaultHttpClient();

	// Create an HttpPut with the URL
	HttpPut httpPut = new HttpPut(url);

	// Create an HttpResponse
	HttpResponse httpResponse = null;

	// Make the request
	try
	{
	    // Set the StringEntity with the request body
	    httpPut.setEntity(new StringEntity(requestBody));

	    // Add headers
	    httpPut.addHeader("X-ApiKey", apiKey);
	    httpPut.addHeader(HTTP.CONTENT_TYPE, "application/json");

	    // Execute Request
	    httpResponse = httpClient.execute(httpPut);

	    // Try updating the database depending on the response
	    if (httpResponse != null)
	    {
		// Status code of the response
		int statusCode = httpResponse.getStatusLine().getStatusCode();

		// If request was successful...
		if (statusCode == HttpURLConnection.HTTP_OK)
		{
		    // Mark values as uploaded
		    mDatabaseManager.markMeasurementsAsUploaded(ids, _sensorID);

		    requestSuccessful = true;
		}
		else
		{
		    // Inform the user
		    mToastHandler.showToast(R.string.toast_upload_failed,
			    Toast.LENGTH_SHORT);
		}
	    }
	}
	catch (UnsupportedEncodingException _exception)
	{
	    _exception.printStackTrace();
	}
	catch (ClientProtocolException _exception)
	{
	    _exception.printStackTrace();
	}
	catch (IOException _exception)
	{
	    _exception.printStackTrace();
	}

	return requestSuccessful;
    }

    /**
     * Starts the {@link UploadLocationTask}.
     */
    public void startUploadingLocation()
    {
	if (!isAPIKeyEmpty() && !isFeedIDEmpty() && isOnline())
	{
	    new UploadLocationTask().execute();
	}
    }

    /**
     * Starts the {@link UploadMeasurementsTask} for the {@link IodIOIOSensor}s
     * with the given sensor IDs.
     * 
     * @param _sensorIDs
     *            The sensor IDs
     */
    public void startUploadingMeasurements(Integer[] _sensorIDs)
    {
	if (!isAPIKeyEmpty() && !isFeedIDEmpty() && isOnline())
	{
	    new UploadMeasurementsTask().execute(_sensorIDs);
	}
    }

    // *************** Upload Tasks ***************

    /**
     * Handles uploading location data from the database to <i>xively.com</i>.
     * 
     * @author kaolick
     * @see AsyncTask
     */
    public class UploadLocationTask extends AsyncTask<Void, Void, Boolean>
    {
	@Override
	protected void onPreExecute()
	{
	    super.onPreExecute();

	    // Inform the user that the app is trying to upload the values
	    mToastHandler.showToast(R.string.toast_upload_coordinates_started,
		    Toast.LENGTH_SHORT);
	}

	@Override
	protected Boolean doInBackground(Void... params)
	{
	    return uploadLoaction();
	}

	@Override
	protected void onPostExecute(Boolean _success)
	{
	    if (_success)
	    {
		// Inform the user that the upload succeeded
		mToastHandler.showToast(R.string.toast_upload_successful,
			Toast.LENGTH_SHORT);
	    }
	    else
	    {
		// Inform the user that the upload failed
		mToastHandler.showToast(R.string.toast_upload_failed,
			Toast.LENGTH_SHORT);
	    }
	}
    }

    /**
     * Handles uploading measurements from the database to <i>xively.com</i>.
     * 
     * @author kaolick
     * @see AsyncTask
     */
    public class UploadMeasurementsTask extends
				       AsyncTask<Integer, Void, Boolean>
    {
	@Override
	protected void onPreExecute()
	{
	    super.onPreExecute();

	    // Inform the user that the app is trying to upload the values
	    mToastHandler.showToast(R.string.toast_upload_measurements_started,
		    Toast.LENGTH_SHORT);
	}

	@Override
	protected Boolean doInBackground(Integer... _sensorIDs)
	{
	    boolean success = true;

	    // Make a request for each sensor
	    for (int i = 0; i < _sensorIDs.length; i++)
	    {
		// The current sensorID
		int sensorID = _sensorIDs[i];

		// In case one of the requests was not successful
		if (!uploadMeasurements(sensorID))
		{
		    success = false;
		}
	    }

	    return success;
	}

	@Override
	protected void onPostExecute(Boolean _success)
	{
	    if (_success)
	    {
		// Inform the user that the upload succeeded
		mToastHandler.showToast(R.string.toast_upload_successful,
			Toast.LENGTH_SHORT);

		// Update the measurements ListView
		if (mActivity != null)
		{
		    mActivity.initListView();
		}
	    }
	    else
	    {
		// Inform the user that the upload failed
		mToastHandler.showToast(R.string.toast_upload_failed,
			Toast.LENGTH_SHORT);
	    }
	}
    }
}