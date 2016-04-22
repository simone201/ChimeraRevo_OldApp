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

package com.near.chimerarevo.misc;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.near.chimerarevo.R;
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

public class ListRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    private ArrayList<HashMap<String, String>> arrayList;
    private Context mContext;

    public ListRemoteViewsFactory(Context context) {
        mContext = context;
    }

    public void onCreate() {
        arrayList = new ArrayList<>();
    }

    @Override
    public void onDataSetChanged() {
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
            arrayList.clear();
            for(int i = 0; i < mPostsArray.length(); i++) {
                try {
                    addItem(mPostsArray.getJSONObject(i));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        arrayList.clear();
    }

    @Override
    public int getCount() {
        return arrayList.size();
    }

    @Override
    public RemoteViews getViewAt(int position) {
        RemoteViews rv = new RemoteViews(mContext.getPackageName(), R.layout.widget_list_item);

        Bundle args = new Bundle();
        args.putInt(Constants.KEY_ID, Integer.parseInt(arrayList.get(position).get(Constants.KEY_ID)));
        args.putString(Constants.KEY_TITLE, arrayList.get(position).get(Constants.KEY_TITLE));
        args.putString(Constants.KEY_IMG, arrayList.get(position).get(Constants.KEY_IMG));
        args.putString(Constants.KEY_TYPE, arrayList.get(position).get(Constants.KEY_TYPE));
        args.putString(Constants.KEY_DATE, arrayList.get(position).get(Constants.KEY_DATE));
        args.putString(Constants.KEY_URL, arrayList.get(position).get(Constants.KEY_URL));

        Intent i = new Intent();
        i.putExtras(args);
        rv.setOnClickFillInIntent(R.id.post_widget_container, i);

        rv.setTextViewText(R.id.post_title, arrayList.get(position).get(Constants.KEY_TITLE));
        rv.setTextViewText(R.id.post_date_cat, arrayList.get(position).get(Constants.KEY_DATE));

        return rv;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    private void addItem(final JSONObject jObject) throws JSONException {
        HashMap<String,String> map = new HashMap<>();
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
        arrayList.add(map);
    }

}
