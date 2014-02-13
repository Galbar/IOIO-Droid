package com.kaolick.ioio_droid.adapter;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.kaolick.ioio_droid.R;
import com.kaolick.ioio_droid.database.MeasurementSchema;
import com.kaolick.ioio_droid.sensor.IodIOIOSensor;

/**
 * A custom {@link SimpleCursorAdapter} for displaying measured sensor values in
 * a {@link ListView}.
 * 
 * @author kaolick
 */
public class MeasurementsCursorAdapter extends SimpleCursorAdapter
{
    // Inner class for the ListView item's UI elements
    static class ViewHolder
    {
	TextView mCountTV, mMeasuredValueTV, mTimestampTV, mUploadedStatusTV;
    }

    // General attributes
    private Context mContext;
    private Cursor mCursor;

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
    public MeasurementsCursorAdapter(Context _context,
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
    }

    @Override
    public View getView(int _position, View _convertView, ViewGroup _parent)
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
		    R.layout.listview_item_measurements, null);

	    // Initialize the ViewHolder and its UI elements
	    holder = new ViewHolder();
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
}