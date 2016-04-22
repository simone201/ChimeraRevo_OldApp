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

package com.near.chimerarevo.fragments;

import com.near.chimerarevo.R;
import com.near.chimerarevo.activities.BaseActivity;
import com.near.chimerarevo.activities.TutorialActivity;
import com.near.chimerarevo.misc.Constants;
import com.near.chimerarevo.preference.SeekBarPreference;
import com.near.chimerarevo.services.NewsService;
import com.near.chimerarevo.utils.SnackbarUtils;
import com.nispok.snackbar.Snackbar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.support.v4.preference.PreferenceFragment;

public class SettingsFragment extends PreferenceFragment {
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            ((BaseActivity) getActivity()).getToolbar().setTitle(getResources().getString(R.string.action_settings));
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        addPreferencesFromResource(R.xml.prefs);

        findPreference("gallery_num_pref").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
        	@Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
        		int num = Integer.parseInt(newValue.toString());
        		if(num < 0 || num > 999) {
					SnackbarUtils.showShortSnackbar(getActivity(),
							getResources().getString(R.string.error_value_notvalid))
							.show(getActivity());
        			return false;
        		} else
        			return true;
               }
		});
        
        findPreference("comments_reset_pref").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
                getActivity().getSharedPreferences(Constants.PREFS_TAG, Context.MODE_PRIVATE)
						.edit().remove(Constants.KEY_REFRESH_TOKEN).commit();
				SnackbarUtils.showLongSnackbar(getActivity(),
						getResources().getString(R.string.comments_reset_toast))
						.show(getActivity());
				return true;
			}
             });

        findPreference("show_tutorial_pref").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(getActivity(), TutorialActivity.class));
                return true;
            }
        });

        ((SeekBarPreference) findPreference("text_size_pref")).setParameters(" sp", 1,
        		PreferenceManager.getDefaultSharedPreferences(getActivity())
				.getInt("text_size_pref", 16), 10);
        
        findPreference("notification_delay_pref").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				setAlarm(Integer.parseInt((String) newValue), true);
				return true;
        	}
        });
        
        findPreference("news_search_pref").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				boolean isChecked = (Boolean) newValue;
				setAlarm(Integer.parseInt(PreferenceManager
						.getDefaultSharedPreferences(getActivity())
						.getString("notification_delay_pref", "0")), isChecked);
				return true;
			}
             });

	}

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        boolean isLandscapeLarge = false;

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if ((getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK)
                    >= Configuration.SCREENLAYOUT_SIZE_LARGE)
                isLandscapeLarge = true;
        } else
            isLandscapeLarge = false;

        if(!isLandscapeLarge)
            getListView().setPadding(0, getResources().getDimensionPixelSize(R.dimen.actionbar_height), 0, 0);
    }

	private void setAlarm(int sel, boolean isEnabled) {
		Intent intent = new Intent(getActivity().getApplicationContext(), NewsService.class);
		PendingIntent pintent = PendingIntent.getService(getActivity().getApplicationContext(), 0, intent, 0);
		AlarmManager alarm = (AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE);
		
		alarm.cancel(pintent);
		
		if(isEnabled) {
			long delay;
			switch(sel) {
				case 0:
					delay = AlarmManager.INTERVAL_FIFTEEN_MINUTES;
					break;
	    		case 1:
	    			delay = AlarmManager.INTERVAL_HALF_HOUR;
	    			break;
	    		case 2:
	    			delay = AlarmManager.INTERVAL_HOUR;
	    			break;
	    		case 3:
	    			delay = 2 * AlarmManager.INTERVAL_HOUR;
	    			break;
	    		case 4:
	    			delay = 3 * AlarmManager.INTERVAL_HOUR;
	    			break;
	    		case 5:
	    			delay = 6 * AlarmManager.INTERVAL_HOUR;
	    			break;
	    		case 6:
	    			delay = AlarmManager.INTERVAL_HALF_DAY;
	    			break;
	    		case 7:
	    			delay = AlarmManager.INTERVAL_DAY;
	    			break;
	    		default:
	    			delay = AlarmManager.INTERVAL_HOUR;
	    			break;
			}
			
			alarm.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.uptimeMillis(), delay, pintent);
		}
	}
	
}
