package com.kaolick.ioio_droid.adapter;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.kaolick.ioio_droid.R;
import com.kaolick.ioio_droid.database.IodDatabaseManager;
import com.kaolick.ioio_droid.database.SensorSchema;
import com.kaolick.ioio_droid.sensor.IodIOIOSensor;
import com.kaolick.ioio_droid.store.Store;
import com.kaolick.ioio_droid.ui.SettingsActivity;

/**
 * A custom {@link SimpleCursorAdapter} for displaying a {@link IodIOIOSensor}
 * in a {@link ListView}.
 * 
 * @author kaolick
 */
public class IOIOSensorCursorAdapter extends SimpleCursorAdapter
{
    // Inner class for the ListView item's UI elements
    static class ViewHolder
    {
	ImageView mStateIV, mMeasurementsIV;
	TextView mNameTV, mPinNumberTV, mSensorIDTV, mFreqTV;
    }

    // General attributes
    private Context mContext;
    private Cursor mCursor;
    private IodDatabaseManager mDatabaseManager;
    private SharedPreferences mPrefs;

    /**
     * Class constructor.
     * 
     * @param _context
     * @param _layout
     * @param _cursor
     * @param _from
     * @param _to
     * @param _flags
     * 
     * @see SimpleCursorAdapter#SimpleCursorAdapter(Context, int, Cursor,
     *      String[], int[], int)
     */
    public IOIOSensorCursorAdapter(Context _context,
				   int _layout,
				   Cursor _cursor,
				   String[] _from,
				   int[] _to,
				   int _flags)
    {
	super(_context, _layout, _cursor, _from, _to, _flags);

	// Initialize attributes
	this.mContext = _context;
	this.mCursor = _cursor;
	this.mDatabaseManager = new IodDatabaseManager(_context);
	this.mPrefs = SettingsActivity.getPrefs((ContextWrapper) _context);
    }

    @Override
    public View getView(final int _position,
			View _convertView,
			ViewGroup _parent)
    {
	// The ViewHolder for the ListView item
	ViewHolder holder = null;

	// Get a LayoutInflater
	LayoutInflater inflater = (LayoutInflater) mContext
		.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

	// Create a new View by inflating an XML layout
	if (_convertView == null)
	{
	    // Inflate a layout
	    _convertView = inflater
		    .inflate(R.layout.listview_item_sensor, null);

	    // Initialize the ViewHolder and its UI elements
	    holder = new ViewHolder();
	    holder.mStateIV = (ImageView) _convertView
		    .findViewById(R.id.stateImageView);
	    holder.mNameTV = (TextView) _convertView
		    .findViewById(R.id.sensorNameTextView);
	    holder.mPinNumberTV = (TextView) _convertView
		    .findViewById(R.id.sensorPinNumberTextView);
	    holder.mSensorIDTV = (TextView) _convertView
		    .findViewById(R.id.sensorSensorIDTextView);
	    holder.mFreqTV = (TextView) _convertView
		    .findViewById(R.id.sensorFrequencyTextView);
	    // holder.mMeasurementsIV = (ImageView) _convertView
	    // .findViewById(R.id.measurementsImageView);

	    // Set the ViewHolder as tag for recycling the View
	    _convertView.setTag(holder);
	}
	// Recycle a View that already exists
	else
	{
	    holder = (ViewHolder) _convertView.getTag();
	}

	// Set an OnClickListener to the "State ImageView"
	holder.mStateIV.setOnClickListener(new OnClickListener()
	{
	    @Override
	    public void onClick(View _view)
	    {
		// If the measuring process is running...
		if (mPrefs.getBoolean(Store.MEASUREMENT_STATUS, false))
		{
		    // Do nothing; inform the user
		    Toast.makeText(mContext, R.string.toast_is_measuring,
			    Toast.LENGTH_SHORT).show();
		}
		else
		{
		    // Move the Cursor to the according ListView item
		    mCursor.moveToPosition(_position);

		    // Get the sensor's sensorID and state
		    int sensorID = mCursor.getInt(mCursor
			    .getColumnIndex(SensorSchema.SENSOR_ID));
		    int state = mCursor.getInt(mCursor
			    .getColumnIndex(SensorSchema.STATE));

		    // State == inactive
		    if (state == IodIOIOSensor.STATE_INACTIVE)
		    {
			mDatabaseManager.updateSensorState(sensorID,
				IodIOIOSensor.STATE_ACTIVE);
		    }
		    // State == active
		    else
		    {
			mDatabaseManager.updateSensorState(sensorID,
				IodIOIOSensor.STATE_INACTIVE);
		    }

		    // Refresh the ListView
		    mCursor = mDatabaseManager.getIOIOSensorsCursor();
		    swapCursor(mCursor);
		    notifyDataSetChanged();
		}
	    }
	});

	// // Set an OnClickListener to the "Measurements Icon" (the arrow on
	// the
	// // right)
	// holder.mMeasurementsIV.setOnClickListener(new OnClickListener()
	// {
	// @Override
	// public void onClick(View _view)
	// {
	// // Move the Cursor to the according ListView item
	// mCursor.moveToPosition(_position);
	//
	// // Get the sensor's sensorID
	// int sensorID = mCursor.getInt(mCursor
	// .getColumnIndex(SensorSchema.SENSOR_ID));
	//
	// String sensorName = mCursor.getString(mCursor
	// .getColumnIndex(SensorSchema.NAME));
	//
	// // Open the MeasurementsActivity for the according sensor
	// Intent intent = new Intent(mContext,
	// IOIOSensorMeasurementsActivity.class);
	// intent.putExtra(Store.SENSOR_ID, sensorID);
	// intent.putExtra(Store.SENSOR_NAME, sensorName);
	// intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	// mContext.startActivity(intent);
	// }
	// });

	// Move the Cursor to the according ListView item
	mCursor.moveToPosition(_position);

	// If the ListView is not empty...
	if (mCursor.getCount() > 0)
	{
	    // Get the sensor's state
	    int state = mCursor.getInt(mCursor
		    .getColumnIndex(SensorSchema.STATE));

	    // State == inactive
	    if (state == IodIOIOSensor.STATE_INACTIVE)
	    {
		// Gray out the sensor's appeareance
		holder.mStateIV.setImageResource(R.drawable.av_play_over_video);
		holder.mStateIV.setColorFilter(mContext.getResources()
			.getColor(R.color.hint_lighter_gray));
		holder.mNameTV.setTextColor(mContext.getResources().getColor(
			R.color.hint_darker_gray));
	    }
	    // State == active
	    else
	    {
		// Colorize the sensor's appeareance
		holder.mStateIV
			.setImageResource(R.drawable.av_pause_over_video);
		holder.mStateIV.setColorFilter(mContext.getResources()
			.getColor(R.color.holo_green_mat));
		holder.mNameTV.setTextColor(mContext.getResources().getColor(
			android.R.color.black));
	    }

	    // Set the sensor's name to the according TextView
	    holder.mNameTV.setText(mCursor.getString(mCursor
		    .getColumnIndex(SensorSchema.NAME)));

	    // Set the sensor's pin number to the according TextView
	    holder.mPinNumberTV.setText(String.valueOf(mCursor.getInt(mCursor
		    .getColumnIndex(SensorSchema.PIN_NUMBER))));

	    // Set the sensor's SensorID to the according TextView
	    holder.mSensorIDTV.setText(String.valueOf(mCursor.getInt(mCursor
		    .getColumnIndex(SensorSchema.SENSOR_ID))));

	    // Set the sensor's frequency to the according TextView
	    int frequency = mCursor.getInt(mCursor
		    .getColumnIndex(SensorSchema.FREQUENCY));
	    int timeUnit = mCursor.getInt(mCursor
		    .getColumnIndex(SensorSchema.TIME_UNIT));
	    String freqTVText = "";
	    switch (timeUnit)
	    {
	    case IodIOIOSensor.TIME_UNIT_MINUTES:
		freqTVText = frequency + " min";
		break;
	    case IodIOIOSensor.TIME_UNIT_HOURS:
		freqTVText = frequency + " h";
		break;
	    case IodIOIOSensor.TIME_UNIT_SECONDS:
		freqTVText = frequency + " sec";
		break;
	    default:
		break;
	    }
	    holder.mFreqTV.setText(freqTVText);
	}

	return _convertView;
    }
}