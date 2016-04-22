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

package com.near.chimerarevo.receivers;

import com.android.vending.billing.util.IabException;
import com.android.vending.billing.util.IabHelper;
import com.android.vending.billing.util.IabResult;
import com.android.vending.billing.util.Inventory;
import com.near.chimerarevo.misc.Constants;
import com.near.chimerarevo.services.NewsService;
import com.near.chimerarevo.utils.SysUtils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.preference.PreferenceManager;

public class BootReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(final Context ctx, Intent i) {
        try {
            final IabHelper mHelper = new IabHelper(ctx, Constants.LICENSE_KEY);
            mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
                public void onIabSetupFinished(IabResult result) {
                    if(result.isSuccess()) {
                        try {
                            Inventory inv = mHelper.queryInventory(false, null);
                            if(inv.hasPurchase(Constants.PREMIUM_ITEM_SKU)) {
                                SysUtils.toggleAppWidgets(ctx, true);
                            } else
                                SysUtils.toggleAppWidgets(ctx, false);
                        } catch (IabException e) {
                            e.printStackTrace();
                        }
                    } else
                        SysUtils.toggleAppWidgets(ctx, false);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

		if(PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean("news_search_pref", true)) {
    		Intent intent = new Intent(ctx.getApplicationContext(), NewsService.class);
    		PendingIntent pintent = PendingIntent.getService(ctx.getApplicationContext(), 0, intent, 0);
    		
    		long delay;
    		int sel = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(ctx).getString("notification_delay_pref", "0"));
    		
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
    		
    		AlarmManager alarm = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
    		alarm.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.uptimeMillis(), delay, pintent);
    	}
	}

}
