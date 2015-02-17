package com.kaolick.ioio_droid.toast;

import android.content.Context;
import android.os.Handler;
import android.widget.Toast;

/**
 * A class for showing a {@link Toast} from background processes using a
 * {@link Handler}.
 * 
 * @author kaolick
 */
public class ToastHandler
{
    // General attributes
    private Context mContext;
    private Handler mHandler;

    /**
     * Class constructor.
     * 
     * @param _context
     *            The {@link Context} for showing the <code>Toast</code>
     */
    public ToastHandler(Context _context)
    {
	this.mContext = _context;
	this.mHandler = new Handler();
    }

    /**
     * Runs the {@link Runnable} in a separate {@link Thread}.
     * 
     * @param _runnable
     *            The <code>Runnable</code> containing the <code>Toast</code>
     */
    private void runRunnable(final Runnable _runnable)
    {
	Thread thread = new Thread()
	{
	    public void run()
	    {
		mHandler.post(_runnable);
	    }
	};

	thread.start();
	thread.interrupt();
	thread = null;
    }

    /**
     * Shows a {@link Toast}. Can be used from background processes.
     * 
     * @param _resID
     *            The resource id of the string resource to use. Can be
     *            formatted text.
     * @param _duration
     *            How long to display the message. Only use <i>LENGTH_LONG</i>
     *            or <i>LENGTH_SHORT</i> from {@link Toast}.
     */
    public void showToast(final int _resID, final int _duration)
    {
	final Runnable runnable = new Runnable()
	{
	    @Override
	    public void run()
	    {
		// Get the text for the given resource ID
		String text = mContext.getResources().getString(_resID);

		Toast.makeText(mContext, text, _duration).show();
	    }
	};

	runRunnable(runnable);
    }

    /**
     * Shows a {@link Toast}. Can be used from background processes.
     * 
     * @param _text
     *            The text to show. Can be formatted text.
     * @param _duration
     *            How long to display the message. Only use <i>LENGTH_LONG</i>
     *            or <i>LENGTH_SHORT</i> from {@link Toast}.
     */
    public void showToast(final CharSequence _text, final int _duration)
    {
	final Runnable runnable = new Runnable()
	{
	    @Override
	    public void run()
	    {
		Toast.makeText(mContext, _text, _duration).show();
	    }
	};

	runRunnable(runnable);
    }
}