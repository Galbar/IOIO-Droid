package com.kaolick.ioio_droid.adapter;

import android.content.Context;
import android.database.Cursor;
import android.view.ActionMode;
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
import com.kaolick.ioio_droid.database.MeasurementSchema;
import com.kaolick.ioio_droid.sensor.IodIOIOSensor;

/**
 * A custom {@link SimpleCursorAdapter} for displaying measured sensor values in
 * a {@link ListView} in contextual {@link ActionMode}.
 * 
 * @author kaolick
 */
public class MeasurementsCursorActionModeAdapter extends SimpleCursorAdapter
{
    // Inner class for the ListView item's UI elements
    static class ViewHolder
    {
	ImageView mRemoveIV;
	TextView mCountTV, mMeasuredValueTV, mTimestampTV, mUploadedStatusTV;
    }

    // General attributes
    private Context mContext;
    private Cursor mCursor;
    private IodDatabaseManager mDatabaseManager;
    private Toast mToast;
    private int sensorID;

    /**
     * Class constructor.
     * 
     * @param _context
     * @param _layout
     * @param _cursor
     * @param _from
     * @param _to
     * @param _flags
     * @param _sensorID
     *            The sensor's ID
     * 
     * @see SimpleCursorAdapter#SimpleCursorAdapter(Context, int, Cursor,
     *      String[], int[], int)
     */
    public MeasurementsCursorActionModeAdapter(Context _context,
					       int _layout,
					       Cursor _cursor,
					       String[] _from,
					       int[] _to,
					       int _flags,
					       int _sensorID)
    {
	super(_context, _layout, _cursor, _from, _to, _flags);

	// Initialize attributes
	this.mContext = _context;
	this.mCursor = _cursor;
	this.mDatabaseManager = new IodDatabaseManager(_context);
	this.sensorID = _sensorID;
	initToast();
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
	    // Inflate the layout
	    _convertView = inflater.inflate(
		    R.layout.listview_item_measurements_cam, null);

	    // Initialize the ViewHolder and its UI elements
	    holder = new ViewHolder();
	    holder.mRemoveIV = (ImageView) _convertView
		    .findViewById(R.id.removeMeasurementImageView);
	    holder.mCountTV = (TextView) _convertView
		    .findViewById(R.id.countTV);
	    holder.mUploadedStatusTV = (TextView) _convertView
		    .findViewById(R.id.uploadedStatusTV);
	    holder.mTimestampTV = (TextView) _convertView
		    .findViewById(R.id.timestampTV);
	    holder.mMeasuredValueTV = (TextView) _convertView
		    .findViewById(R.id.measuredValueTV);

	    // Set the ViewHolder as tag for recycling the View
	    _convertView.setTag(holder);
	}
	// Recycle a View that already exists
	else
	{
	    holder = (ViewHolder) _convertView.getTag();
	}

	// Set an OnClickListener to the "Delete Icon"
	holder.mRemoveIV.setOnClickListener(new OnClickListener()
	{
	    @Override
	    public void onClick(View _view)
	    {
		// Move the Cursor to the according ListView item
		mCursor.moveToPosition(_position);

		// Delete the sensor from database
		mDatabaseManager.deleteMeasurement(mCursor.getInt(mCursor
			.getColumnIndex(MeasurementSchema.ID)), sensorID);

		// Inform the user; this way showing too many toasts in a row
		// can be avoided in case the user deletes several measurements
		// in a row manually
		mToast.cancel();
		initToast();
		mToast.show();

		// Refresh the ListView
		mCursor = mDatabaseManager.getMeasurementsCursor(sensorID);
		swapCursor(mCursor);
		notifyDataSetChanged();
	    }
	});

	// Move the Cursor to the according ListView item
	mCursor.moveToPosition(_position);

	// Set the measurement count to the according TextView
	holder.mCountTV.setText(String.valueOf(mCursor.getCount()
		- mCursor.getPosition()));

	// Set uploaded status to the according TextView
	if (mCursor.getInt(mCursor.getColumnIndex(MeasurementSchema.UPLOADED)) == IodIOIOSensor.UPLOADED_FALSE)
	{
	    holder.mUploadedStatusTV.setText(R.string.no);
	}
	else
	{
	    holder.mUploadedStatusTV.setText(R.string.yes);
	}

	// Set the timestamp to the according TextView
	holder.mTimestampTV.setText(mCursor.getString(mCursor
		.getColumnIndex(MeasurementSchema.TIMESTAMP)));

	// Set the value to the according TextView
	holder.mMeasuredValueTV.setText(String.valueOf(mCursor
		.getDouble(mCursor.getColumnIndex(MeasurementSchema.VALUE))));

	return _convertView;
    }

    /**
     * Initializes a {@link Toast} that is shown after a measurement is deleted.
     */
    public void initToast()
    {
	mToast = Toast.makeText(mContext, R.string.toast_measurement_deleted,
		Toast.LENGTH_SHORT);
    }
}