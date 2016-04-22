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
import android.net.Uri;
import android.support.annotation.NonNull;
import android.widget.RemoteViews;

import com.near.chimerarevo.R;
import com.near.chimerarevo.activities.PostContainerActivity;
import com.near.chimerarevo.services.ListWidgetService;
import com.near.chimerarevo.services.NewsService;

public class PostsListWidgetProvider extends AppWidgetProvider {

    public static final String REFRESH_VIEWS_ACTION = "com.near.chimerarevo.providers.REFRESH_VIEWS_LIST";
    public static final String OPEN_POST_ACTION = "com.near.chimerarevo.providers.LIST_OPEN_POST";
    public static final String REFRESH_ACTION = "com.near.chimerarevo.providers.REFRESH_LIST";

    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {

        if(intent.getAction().equals(PostsListWidgetProvider.REFRESH_VIEWS_ACTION)) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, this.getClass()));
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.posts_list);
        } else if(intent.getAction().equals(PostsListWidgetProvider.OPEN_POST_ACTION)) {
            Intent i = new Intent(context, PostContainerActivity.class);
            i.putExtras(intent.getExtras());
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
        } else if(intent.getAction().equals(PostsListWidgetProvider.REFRESH_ACTION)) {
            Intent i = new Intent(context, NewsService.class);
            i.putExtra("shouldNotCreateNotification", true);
            context.startService(i);
        }

        super.onReceive(context, intent);
    }

    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        for (int appWidgetId : appWidgetIds) {
            Intent intent = new Intent(context, ListWidgetService.class);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));

            RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget_list_layout);
            rv.setRemoteAdapter(R.id.posts_list, intent);
            rv.setEmptyView(R.id.posts_list, R.id.empty_view);

            Intent i = new Intent(context, PostsListWidgetProvider.class);
            i.setAction(PostsListWidgetProvider.OPEN_POST_ACTION);
            i.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            i.setData(Uri.parse(i.toUri(Intent.URI_INTENT_SCHEME)));
            PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
            rv.setPendingIntentTemplate(R.id.posts_list, pi);

            i = new Intent(context, PostsListWidgetProvider.class);
            i.setAction(PostsListWidgetProvider.REFRESH_ACTION);
            i.setData(Uri.parse(i.toUri(Intent.URI_INTENT_SCHEME)));
            pi = PendingIntent.getBroadcast(context, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
            rv.setOnClickPendingIntent(R.id.widget_refresh_btn, pi);

            appWidgetManager.updateAppWidget(appWidgetId, rv);
        }

        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

}
