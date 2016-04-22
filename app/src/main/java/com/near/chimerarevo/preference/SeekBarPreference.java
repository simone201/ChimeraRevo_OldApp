/*
* Copyright (C) 2013-2015 Simone Renzo.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.near.chimerarevo.preference;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import com.near.chimerarevo.R;

public class SeekBarPreference extends DialogPreference implements SeekBar.OnSeekBarChangeListener {
	
	// Namespaces to read attributes
	protected static final String PREFERENCE_NS = "http://schemas.android.com/apk/res/com.near.chimerarevo";
	protected static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";

	// Attribute names
	protected static final String ATTR_DEFAULT_VALUE = "defaultValue";
	protected static final String ATTR_MIN_VALUE = "minValue";
	protected static final String ATTR_MAX_VALUE = "maxValue";

	// Default values for defaults
	protected static final int DEFAULT_CURRENT_VALUE = 50;
	protected static final int DEFAULT_MIN_VALUE = 0;
	protected static final int DEFAULT_MAX_VALUE = 100;

	// Real defaults
	protected int mDefaultValue;
	protected int mMaxValue;
	protected int mMinValue;
	    
	// Current value
	protected int mCurrentValue, mTempValue;
	    
	// Parameters
	protected int mStep = 0;
	protected String mSuffix = "";
    protected int mCorrectFactor = 0;
	    
    // View elements
    protected SeekBar mSeekBar;
    protected TextView mValueText;
    
    protected Context ctx;

    public SeekBarPreference(Context context, AttributeSet attrs) {
    	super(context, attrs);
    	ctx = context;
	    // Read parameters from attributes
		mMinValue = attrs.getAttributeIntValue(PREFERENCE_NS, ATTR_MIN_VALUE, DEFAULT_MIN_VALUE);
		mMaxValue = attrs.getAttributeIntValue(PREFERENCE_NS, ATTR_MAX_VALUE, DEFAULT_MAX_VALUE);
		mDefaultValue = attrs.getAttributeIntValue(ANDROID_NS, ATTR_DEFAULT_VALUE, DEFAULT_CURRENT_VALUE);
    }

    @Override
    protected View onCreateDialogView() {
		// Inflate layout
		LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.dialog_slider, null);
		
		// Setup SeekBar
		mSeekBar = (SeekBar) view.findViewById(R.id.seek_bar);
		mSeekBar.setMax(mMaxValue - mMinValue);
		mSeekBar.setOnSeekBarChangeListener(this);

		// Setup text label for current value
		mValueText = (TextView) view.findViewById(R.id.current_value);
		mValueText.setTextColor(ctx.getResources().getColor(android.R.color.black));
		mValueText.setText(Integer.toString(mCurrentValue) + mSuffix);
		
		if(mStep > 0) {
			mSeekBar.setProgress((mCurrentValue - mCorrectFactor) / mStep);
			((TextView) view.findViewById(R.id.min_value)).setText(Integer.toString((mMinValue * mStep) + mCorrectFactor));
			((TextView) view.findViewById(R.id.max_value)).setText(Integer.toString((mMaxValue * mStep) + mCorrectFactor));
		} else {
			mSeekBar.setProgress(mCurrentValue - mMinValue);
			((TextView) view.findViewById(R.id.min_value)).setText(Integer.toString(mMinValue));
			((TextView) view.findViewById(R.id.max_value)).setText(Integer.toString(mMaxValue));
		}
			
		return view;
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
    	super.onDialogClosed(positiveResult);

    	// Return if change was cancelled
    	if (!positiveResult) {
    		return;
    	}

    	mCurrentValue = mTempValue;
	    	
    	// Persist current value
        persistInt(mCurrentValue);

    	// Notify activity about changes (to update preference summary line)
    	notifyChanged();
    }
	    
    public void onProgressChanged(SeekBar seek, int value, boolean fromTouch) {
    	if(fromTouch) {
    		// Update current value
    		if(mStep > 0)
    			mTempValue = value * mStep + mCorrectFactor;
    		else
    			mTempValue = value + mMinValue;
        	// Update label with current value
    		if(mSuffix.equals(""))
    			mValueText.setText(Integer.toString(mTempValue));
    		else
    			mValueText.setText(Integer.toString(mTempValue) + mSuffix);
    	}
    }

    public void onStartTrackingTouch(SeekBar seek) {
    	// Not used
    }

    public void onStopTrackingTouch(SeekBar seek) {
    	// Not used
    }
	    
    public void setParameters(String type, int step, int defVal, int factor) {
    	mSuffix = type;
    	mStep = step;
    	mCurrentValue = defVal;
    	mCorrectFactor = factor;
    	mTempValue = mCurrentValue;
    }

}
