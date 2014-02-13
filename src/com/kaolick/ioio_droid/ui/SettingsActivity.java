package com.kaolick.ioio_droid.ui;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.widget.Toast;

import com.kaolick.ioio_droid.R;
import com.kaolick.ioio_droid.database.IodDatabase;
import com.kaolick.ioio_droid.database.IodDatabaseManager;
import com.kaolick.ioio_droid.dialog.UniversalDialogFragment;
import com.kaolick.ioio_droid.dialog.UniversalDialogFragment.UniversalDialogListener;
import com.kaolick.ioio_droid.store.Store;

/**
 * Handles the application settings. Implements {@link UniversalDialogListener}.
 * 
 * @author kaolick
 * @see Activity
 */
public class SettingsActivity extends Activity implements
					      UniversalDialogListener
{
    // General attributes
    private Context mContext;
    private IodDatabaseManager mDatabaseManager;

    // *************** Activity life cycle ***************

    @Override
    protected void onCreate(Bundle _savedInstanceState)
    {
	super.onCreate(_savedInstanceState);

	// Initialize attributes
	mContext = this;
	mDatabaseManager = new IodDatabaseManager(this);

	// Display the fragment as the main content
	getFragmentManager().beginTransaction()
		.replace(android.R.id.content, new SettingsFragment()).commit();

	// This is a Work-Around to display additional Views (here: a line
	// between the ActionBar and the Preferences)
	setContentView(R.layout.activity_settings);

	// Get ActionBar an enable navigation
	getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    // *************** Dialog ***************

    @Override
    public void onUniversalDialogPositiveClick(DialogFragment _dialog)
    {
	// User touched the dialog's positive button
	switch (_dialog.getArguments().getInt(UniversalDialogFragment.KEY))
	{
	case Store.DELETE_ALL_DATABASE_TABLES:
	    deleteDatabase(IodDatabase.DB_NAME);
	    Toast.makeText(mContext, R.string.toast_database_deleted,
		    Toast.LENGTH_SHORT).show();
	    break;
	case Store.DELETE_LOCATION_DATA:
	    mDatabaseManager.deleteLocationData();
	    Toast.makeText(mContext, R.string.toast_location_data_deleted,
		    Toast.LENGTH_SHORT).show();
	    break;
	default:
	    break;
	}
    }

    @Override
    public void onUniversalDialogNegativeClick(DialogFragment _dialog)
    {
	// User touched the dialog's negative button
	// Do nothing
    }

    /**
     * Shows a <code>Dialog</code> for deleting the database.
     */
    public void showDeleteDatabaseDialog()
    {
	// Set the Dialog's title and message
	String title = getResources().getString(
		R.string.dialog_delete_database_title);
	String message = getResources().getString(
		R.string.dialog_delete_database_message);

	// Create and show the Dialog
	DialogFragment fragment = UniversalDialogFragment.newInstance(
		Store.DELETE_ALL_DATABASE_TABLES, title, message);
	fragment.show(getFragmentManager(), "Delete all database tables");
    }

    /**
     * Shows a <code>Dialog</code> for deleting all loaction data.
     */
    public void showDeleteLocationDataDialog()
    {
	// Set the Dialog's title and message
	String title = getResources().getString(
		R.string.dialog_delete_location_data_title);
	String message = getResources().getString(
		R.string.dialog_delete_location_data_message);

	// Create and show the Dialog
	DialogFragment fragment = UniversalDialogFragment.newInstance(
		Store.DELETE_LOCATION_DATA, title, message);
	fragment.show(getFragmentManager(), "Delete all location data");
    }

    // *************** Menu ***************

    @Override
    public boolean onOptionsItemSelected(MenuItem _item)
    {
	switch (_item.getItemId())
	{
	case android.R.id.home:
	    // Return to the last Activity
	    dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,
		    KeyEvent.KEYCODE_BACK));
	    finish();
	    return true;
	default:
	    return super.onOptionsItemSelected(_item);
	}
    }

    // *************** Shared Preferences ***************

    /**
     * Returns the application's {@link SharedPreferences}.
     * 
     * @param _ctxWrapper
     * @return the application's <code>SharedPreferences</code>.
     */
    public static final SharedPreferences
	    getPrefs(final ContextWrapper _ctxWrapper)
    {
	return _ctxWrapper.getSharedPreferences(_ctxWrapper.getPackageName()
		+ "_preferences", Context.MODE_PRIVATE);
    }
}