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

package com.near.chimerarevo.services;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.util.ArrayList;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.near.chimerarevo.R;
import com.near.chimerarevo.activities.MainActivity;
import com.near.chimerarevo.misc.PostsListObject;
import com.near.chimerarevo.providers.PostsLRWidgetProvider;
import com.near.chimerarevo.providers.PostsListWidgetProvider;
import com.near.chimerarevo.utils.OkHttpUtils;
import com.near.chimerarevo.utils.SysUtils;
import com.near.chimerarevo.utils.URLUtils;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.widget.Toast;

public class NewsService extends IntentService {
	
	private NetworkInfo mWifi, mMobile;
    private Handler mHandler;
	
	public NewsService() {
		super("NewsService");
        mHandler = new Handler();
	}

	@Override
	protected void onHandleIntent(Intent i) {
		ConnectivityManager mConnManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        if(mConnManager != null) {
            mWifi = mConnManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            mMobile = mConnManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

            if(!isMobileConnected() && !isWiFiConnected())
                return;
        }

        if(i.getBooleanExtra("shouldNotCreateNotification", false))
            mHandler.post(new DisplayToast(this, getResources().getString(R.string.text_loading)));

        Request request = new Request.Builder()
                .url(URLUtils.getUrl())
                .build();

		try {
            Response response = OkHttpUtils.getInstance()
                    .newCall(request).execute();

            String body = response.body().string().trim();

	        if(!body.isEmpty())
	        	if(readOfflineFile(body)) {
                    ArrayList<String> mJson = new ArrayList<>();
                    mJson.add((new JsonParser()).parse(body).toString());
                    SysUtils.writeOfflineFile(this, mJson, "posts.ser");

                    Intent update = new Intent(this, PostsListWidgetProvider.class);
                    update.setAction(PostsListWidgetProvider.REFRESH_VIEWS_ACTION);
                    sendBroadcast(update);

                    update = new Intent(this, PostsLRWidgetProvider.class);
                    update.setAction(PostsLRWidgetProvider.REFRESH_VIEWS_ACTION);
                    sendBroadcast(update);

                    if(!i.getBooleanExtra("shouldNotCreateNotification", false))
                        createNotification();
                }
		} catch(Exception e) {
			e.printStackTrace();
	    }
	}
	
	private boolean isMobileConnected() {
		try {
			return mMobile.isConnected();
		} catch (Exception e) {
			return false;
		}
	}
	
	private boolean isWiFiConnected() {
		try {
			return mWifi.isConnected();
		} catch (Exception e) {
			return false;
		}
	}
	
	private void createNotification() {
        if(!PreferenceManager.getDefaultSharedPreferences(this).getBoolean("notification_pref", true))
            return;

		Intent intent = new Intent(this, MainActivity.class);
		PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent, 0);

        NotificationCompat.BigTextStyle bigStyle = new NotificationCompat.BigTextStyle();
        bigStyle.bigText(getResources().getString(R.string.text_newposts_summary));
        bigStyle.setBigContentTitle(getResources().getString(R.string.app_name));

        NotificationCompat.WearableExtender wearableExtender =
            new NotificationCompat.WearableExtender()
                    .setContentIcon(R.drawable.ic_logo_cr_mini)
                    .setHintHideIcon(true);

        Notification n  = new NotificationCompat.Builder(this)
	        .setContentTitle(getResources().getString(R.string.app_name))
	        .setContentText(getResources().getString(R.string.text_newposts_summary))
	        .setSmallIcon(R.drawable.ic_logo_cr_mini)
	        .setContentIntent(pIntent)
            .extend(wearableExtender)
            .setAutoCancel(true)
            .build();
		
		if(PreferenceManager.getDefaultSharedPreferences(this).getBoolean("notification_vibrate_pref", true))
			n.defaults |= Notification.DEFAULT_VIBRATE;
		if(PreferenceManager.getDefaultSharedPreferences(this).getBoolean("notification_sound_pref", true))
			n.defaults |= Notification.DEFAULT_SOUND;
		if(PreferenceManager.getDefaultSharedPreferences(this).getBoolean("notification_light_pref", true)) {
			n.ledARGB = getLEDColor();
			n.ledOnMS = 300;
			n.ledOffMS = 1000;
			n.flags |= Notification.FLAG_SHOW_LIGHTS;
		}

        NotificationManagerCompat notificationManager =
                NotificationManagerCompat.from(this);
        notificationManager.notify(0, n);
	}
	
	private boolean readOfflineFile(String result) {
		try {
			if(new File(getCacheDir() + "/posts.ser").exists()) {
                InputStream file = new FileInputStream(getCacheDir() + "/posts.ser");
                ObjectInput input = new ObjectInputStream(new BufferedInputStream(file));
                ArrayList<String> mJson = ((PostsListObject) input.readObject()).getJSONs();
                input.close();
                if(mJson != null && mJson.size() > 0) {
                    try {
                        JsonParser parser = new JsonParser();
                        JsonElement o1 = parser.parse(result);
                        JsonElement o2 = parser.parse(mJson.get(0));
                        return !o1.equals(o2);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
			}
		} catch (IOException | ClassNotFoundException | ClassCastException e) {
			e.printStackTrace();
		}
        return false;
	}

    private int getLEDColor() {
        int index = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(this).getString("notification_light_color_pref", "4"));

        switch(index) {
            case 0:
                return Color.BLUE;
            case 1:
                return Color.CYAN;
            case 2:
                return Color.GREEN;
            case 3:
                return Color.MAGENTA;
            case 4:
                return Color.RED;
            case 5:
                return Color.WHITE;
            case 6:
                return Color.YELLOW;
            default:
                return Color.RED;
        }
    }

    private class DisplayToast implements Runnable {
        private final Context mContext;
        private String mText;

        public DisplayToast(Context mContext, String text) {
            this.mContext = mContext;
            mText = text;
        }

        public void run() {
            Toast.makeText(mContext, mText, Toast.LENGTH_LONG).show();
        }
    }

}
