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

package com.near.chimerarevo.adapters;

import com.near.chimerarevo.R;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

public class MenuAdapter extends BaseExpandableListAdapter {

	private Context context;
    private String[] mTitle, mCategories;
    private boolean[] mCheck, mCatCheck;
    
    public MenuAdapter(Context context) {
    	this.context = context;

        mTitle = context.getResources().getStringArray(R.array.menu_items_titles);
        mCategories = context.getResources().getStringArray(R.array.menu_categories_titles);

        mCheck = new boolean[mTitle.length];
        mCatCheck = new boolean[mCategories.length];
        mCheck[0] = true;
    }
    
    public void toggleSelection(int position) {
    	mCheck[position] = !mCheck[position];
    }

    public void toggleChildSelection(int position) {
        mCatCheck[position] = !mCatCheck[position];
    }

    public void resetChildCheck() {
        mCatCheck = new boolean[mCategories.length];
    }

    @Override
    public int getGroupCount() {
        return mTitle.length;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        if(groupPosition == 1)
            return mCategories.length;
        else
            return 0;
    }

    @Override
    public Object getGroup(int groupPosition) {
        return mTitle[groupPosition];
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        if(groupPosition == 1)
            return mCategories[childPosition];
        else
            return null;
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return groupPosition * childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        if(convertView == null)
            convertView = LayoutInflater.from(context).inflate(R.layout.drawer_list_item, parent, false);

        TextView mText = (TextView) convertView.findViewById(R.id.menu_text);
        mText.setText(mTitle[groupPosition]);

        if(groupPosition != 1)
            convertView.findViewById(R.id.expand_indicator).setVisibility(View.GONE);
        else
            convertView.findViewById(R.id.expand_indicator).setVisibility(View.VISIBLE);

        if(mCheck[groupPosition])
            mText.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
        else
            mText.setTypeface(Typeface.createFromAsset(context.getAssets(), "roboto_thin.ttf"));

        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        if(convertView == null)
            convertView = LayoutInflater.from(context).inflate(R.layout.drawer_list_child, parent, false);

        TextView mText = (TextView) convertView.findViewById(R.id.child_text);
        mText.setText(mCategories[childPosition]);

        if(mCatCheck[childPosition]) {
            mText.setTextColor(getChildColor(childPosition));
        } else
            mText.setTextColor(context.getResources().getColor(android.R.color.white));

        return convertView;
    }

    public int getChildColor(int position) {
        return context.getResources().getIntArray(R.array.categories_colors)[position];
    }

}
