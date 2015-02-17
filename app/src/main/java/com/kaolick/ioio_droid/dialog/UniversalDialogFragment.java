package com.kaolick.ioio_droid.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

import com.kaolick.ioio_droid.R;

/**
 * A universal {@link DialogFragment} that can be used for multiple purposes.
 * 
 * @author kaolick
 */
public class UniversalDialogFragment extends DialogFragment
{
    /*
     * The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks. Each method
     * passes the DialogFragment in case the host needs to query it.
     */
    public interface UniversalDialogListener
    {
	public void onUniversalDialogPositiveClick(DialogFragment _dialog);

	public void onUniversalDialogNegativeClick(DialogFragment _dialog);
    }

    // Bundle keys
    public static final String KEY = "key";
    public static final String MESSAGE = "message";
    public static final String TITLE = "title";

    // Use this instance of the interface to deliver action events
    private UniversalDialogListener dialogListener;

    /**
     * Creates a new instance of {@link UniversalDialogFragment}, providing
     * <i>title</i> and <i>message</i> as arguments for the <code>Dialog</code>.
     * 
     * @param _key
     *            A key to identify the <code>Dialog</code> type
     * @param _title
     *            The <code>Dialog's</code> title
     * @param _message
     *            The <code>Dialog's</code> message
     * @return The <code>IodDialogFragment</code> instance
     * 
     * @see {@link Dialog}
     */
    public static UniversalDialogFragment newInstance(int _key,
						String _title,
						String _message)
    {
	// Create a new UniversalDialogFragment
	UniversalDialogFragment fragment = new UniversalDialogFragment();

	// Supply title and message as arguments
	Bundle args = new Bundle();
	args.putInt(KEY, _key);
	args.putString(TITLE, _title);
	args.putString(MESSAGE, _message);
	fragment.setArguments(args);

	return fragment;
    }

    /*
     * Override the Fragment.onAttach() method to instantiate the
     * UniversalDialogListener
     */
    @Override
    public void onAttach(Activity _activity)
    {
	super.onAttach(_activity);

	// Verify that the host activity implements the callback interface
	try
	{
	    // Instantiate the UniversalDialogListener so that events can be sent to
	    // the host
	    dialogListener = (UniversalDialogListener) _activity;
	}
	catch (ClassCastException _exception)
	{
	    // The activity doesn't implement the interface, throw exception
	    throw new ClassCastException(_activity.toString()
		    + " must implement UniversalDialogListener");
	}
    }

    @Override
    public Dialog onCreateDialog(Bundle _savedInstanceState)
    {
	// Build the dialog and set up the button click handlers
	AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
	builder.setPositiveButton(R.string.ok,
		new DialogInterface.OnClickListener()
		{
		    public void onClick(DialogInterface dialog, int id)
		    {
			// Send the positive button event back to the host
			// activity
			dialogListener
				.onUniversalDialogPositiveClick(UniversalDialogFragment.this);
		    }
		});
	builder.setNegativeButton(R.string.cancel,
		new DialogInterface.OnClickListener()
		{
		    public void onClick(DialogInterface dialog, int id)
		    {
			// Send the negative button event back to the host
			// activity
			dialogListener
				.onUniversalDialogNegativeClick(UniversalDialogFragment.this);
		    }
		});

	// Set the icon
	builder.setIcon(R.drawable.alerts_and_states_warning);

	// Set the title and message
	builder.setTitle(getArguments().getString(TITLE));
	builder.setMessage(getArguments().getString(MESSAGE));

	// Create the AlertDialog object and return it
	return builder.create();
    }
}