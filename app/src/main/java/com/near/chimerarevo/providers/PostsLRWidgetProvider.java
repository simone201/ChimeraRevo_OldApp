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

package com.near.chimerarevo.providers;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.widget.RemoteViews;

import com.near.chimerarevo.R;
import com.near.chimerarevo.activities.PostContainerActivity;
import com.near.chimerarevo.misc.Constants;
import com.near.chimerarevo.misc.PostsListObject;
import com.near.chimerarevo.services.NewsService;
import com.near.chimerarevo.utils.JSONUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;

public class PostsLRWidgetProvider extends AppWidgetProvider {

    public static final String REFRESH_VIEWS_ACTION = "com.near.chimerarevo.providers.REFRESH_VIEWS_LR";
    public static final String OPEN_POST_ACTION = "com.near.chimerarevo.providers.LR_OPEN_POST";
    public static final String NEXT_POST_ACTION = "com.near.chimerarevo.providers.LR_NEXT_POST";
    public static final String PREV_POST_ACTION = "com.near.chimerarevo.providers.LR_PREV_POST";
    public static final String REFRESH_ACTION = "com.near.chimerarevo.providers.REFRESH_LR";

    private HashMap<String, String> map;

    private int position = 0;

    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {
        SharedPreferences prefs = context.getSharedPreferences(Constants.PREFS_TAG, Context.MODE_PRIVATE);
        position = prefs.getInt("lr_widget_position", 0);

        if(intent.getAction().equals(PostsLRWidgetProvider.REFRESH_VIEWS_ACTION)) {
            position = 0;
            prefs.edit().putInt("lr_widget_position", position).apply();

            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, this.getClass()));
            onUpdate(context, appWidgetManager, appWidgetIds);
        } else if(intent.getAction().equals(PostsLRWidgetProvider.REFRESH_ACTION)) {
            Intent i = new Intent(context, NewsService.class);
            i.putExtra("shouldNotCreateNotification", true);
            context.startService(i);
        } else if(intent.getAction().equals(PostsLRWidgetProvider.NEXT_POST_ACTION)) {
            position++;
            if(position > 9)
                position = 0;

            parseData(context);
            updateWidgetView(context, intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 0),
                    AppWidgetManager.getInstance(context));
        } else if(intent.getAction().equals(PostsLRWidgetProvider.PREV_POST_ACTION)) {
            position--;
            if(position < 0)
                position = 9;

            parseData(context);
            updateWidgetView(context, intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 0),
                    AppWidgetManager.getInstance(context));
        } else if(intent.getAction().equals(PostsLRWidgetProvider.OPEN_POST_ACTION)) {
            parseData(context);
            Bundle args = new Bundle();
            args.putInt(Constants.KEY_ID, Integer.parseInt(map.get(Constants.KEY_ID)));
            args.putString(Constants.KEY_TITLE, map.get(Constants.KEY_TITLE));
            args.putString(Constants.KEY_IMG, map.get(Constants.KEY_IMG));
            args.putString(Constants.KEY_TYPE, map.get(Constants.KEY_TYPE));
            args.putString(Constants.KEY_DATE, map.get(Constants.KEY_DATE));
            args.putString(Constants.KEY_URL, map.get(Constants.KEY_URL));

            Intent i = new Intent(context, PostContainerActivity.class);
            i.putExtras(args);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
        } else {
            position = 0;
            prefs.edit().putInt("lr_widget_position", position).apply();
            super.onReceive(context, intent);
            return;
        }

        prefs.edit().putInt("lr_widget_position", position).apply();
    }

    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        parseData(context);

        for (int appWidgetId : appWidgetIds)
            updateWidgetView(context, appWidgetId, appWidgetManager);

        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    private void updateWidgetView(Context context, int appWidgetId, AppWidgetManager appWidgetManager) {
        RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget_leftright_layout);

        if(map != null) {
            rv.setTextViewText(R.id.post_title , map.get(Constants.KEY_TITLE));
            rv.setTextViewText(R.id.post_date_cat, map.get(Constants.KEY_DATE));

            Intent i = new Intent(context, PostsLRWidgetProvider.class);
            i.setAction(PostsLRWidgetProvider.PREV_POST_ACTION);
            i.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            i.setData(Uri.parse(i.toUri(Intent.URI_INTENT_SCHEME)));
            PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
            rv.setOnClickPendingIntent(R.id.widget_navigate_back, pi);

            i = new Intent(context, PostsLRWidgetProvider.class);
            i.setAction(PostsLRWidgetProvider.NEXT_POST_ACTION);
            i.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            i.setData(Uri.parse(i.toUri(Intent.URI_INTENT_SCHEME)));
            pi = PendingIntent.getBroadcast(context, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
            rv.setOnClickPendingIntent(R.id.widget_navigate_forward, pi);

            i = new Intent(context, PostsLRWidgetProvider.class);
            i.setAction(PostsLRWidgetProvider.OPEN_POST_ACTION);
            i.setData(Uri.parse(i.toUri(Intent.URI_INTENT_SCHEME)));
            pi = PendingIntent.getBroadcast(context, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
            rv.setOnClickPendingIntent(R.id.widget_post_info, pi);

            i = new Intent(context, PostsLRWidgetProvider.class);
            i.setAction(PostsLRWidgetProvider.REFRESH_ACTION);
            i.setData(Uri.parse(i.toUri(Intent.URI_INTENT_SCHEME)));
            pi = PendingIntent.getBroadcast(context, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
            rv.setOnClickPendingIntent(R.id.widget_refresh_btn, pi);
        }

        appWidgetManager.updateAppWidget(appWidgetId, rv);
    }

    private void parseData(Context mContext) {
        JSONArray mPostsArray = null;

        try {
            if(new File(mContext.getCacheDir() + "/posts.ser").exists()) {
                InputStream file = new FileInputStream(mContext.getCacheDir() + "/posts.ser");
                ObjectInput input = new ObjectInputStream(new BufferedInputStream(file));
                ArrayList<String> mJson = ((PostsListObject) input.readObject()).getJSONs();
                input.close();

                if(mJson != null && mJson.size() > 0)
                    mPostsArray = JSONUtils.getJSONArray(mJson.get(0), Constants.KEY_POSTS);
            }
        } catch (IOException | ClassNotFoundException | ClassCastException e) {
            e.printStackTrace();
        }

        if(mPostsArray != null) {
            try {
                addItem(mPostsArray.getJSONObject(position));
            } catch (JSONException | ArrayIndexOutOfBoundsException e) {
                e.printStackTrace();
            }
        }
    }

    private void addItem(final JSONObject jObject) throws JSONException {
        map = new HashMap<>();
        map.put(Constants.KEY_ID, String.valueOf(jObject.getInt(Constants.KEY_ID)));
        map.put(Constants.KEY_TITLE, jObject.getString(Constants.KEY_TITLE));

        String cat = JSONUtils.getCategory(jObject);
        if(cat.trim().equalsIgnoreCase("") || cat.length() < 1)
            cat = Character.toString(jObject.getString(Constants.KEY_TYPE).charAt(0)).toUpperCase()
                    + jObject.getString(Constants.KEY_TYPE).substring(1);

        map.put(Constants.KEY_DATE, jObject.getString(Constants.KEY_DATE) + " | " + cat);
        map.put(Constants.KEY_IMG, jObject.getString(Constants.KEY_IMG));
        map.put(Constants.KEY_TYPE, jObject.getString(Constants.KEY_TYPE));
        map.put(Constants.KEY_URL, jObject.getString(Constants.KEY_URL));
    }

}
