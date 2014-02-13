package com.kaolick.ioio_droid.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MenuItem;

import com.kaolick.ioio_droid.R;

/**
 * Contains information like version, author etc. about the application.
 * 
 * @author kaolick
 */
public class AboutActivity extends Activity
{
    // *************** Activity life cycle ***************

    @Override
    protected void onCreate(Bundle _savedInstanceState)
    {
	super.onCreate(_savedInstanceState);
	setTheme(android.R.style.Theme_Holo);
	setContentView(R.layout.activity_about);

	// Get ActionBar an enable navigation
	getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    // *************** Menu ***************

    @Override
    public boolean onOptionsItemSelected(MenuItem _item)
    {
	switch (_item.getItemId())
	{
	case android.R.id.home:
	    // Return to the SettingsActivity when clicking on the app icon in
	    // the ActionBar
	    dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,
		    KeyEvent.KEYCODE_BACK));
	    finish();
	    return true;
	default:
	    return super.onOptionsItemSelected(_item);
	}
    }
}