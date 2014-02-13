package com.kaolick.ioio_droid.ui;

import android.content.Context;
import android.preference.SwitchPreference;
import android.util.AttributeSet;

/**
 * This is a work-around for the known bug in <code>Android</code> when too many
 * {@link SwitchPreference}s are used in a {@link PreferenceFragment}.
 * 
 * @author kaolick
 */
public class IodSwitchPreference extends SwitchPreference
{
    public IodSwitchPreference(Context _context)
    {
	super(_context);
    }

    public IodSwitchPreference(Context _context, AttributeSet _attrs)
    {
	super(_context, _attrs);
    }

    public IodSwitchPreference(Context _context,
			       AttributeSet _attrs,
			       int _defStyle)
    {
	super(_context, _attrs, _defStyle);
    }
}