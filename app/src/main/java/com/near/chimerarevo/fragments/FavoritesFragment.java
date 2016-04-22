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

import java.util.ArrayList;
import java.util.HashMap;

import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import com.near.chimerarevo.R;
import com.near.chimerarevo.activities.BaseActivity;
import com.near.chimerarevo.adapters.PostsRecyclerAdapter;
import com.near.chimerarevo.misc.Constants;
import com.near.chimerarevo.sqlite.DatabaseHelper;
import com.near.chimerarevo.sqlite.tables.FavoritesTable;

public class FavoritesFragment extends Fragment {

    private RecyclerView mListView;

    private View mProgressContainer;
    private View mListContainer;
    private View mEmptyText;

    private boolean mListShown = false;

    private ArrayList<HashMap<String,String>> arrayList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.recyclerview_layout, container, false);

        boolean isLandscapeLarge = false;

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if ((getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK)
                    >= Configuration.SCREENLAYOUT_SIZE_LARGE)
                isLandscapeLarge = true;
        } else
            isLandscapeLarge = false;

        if(!isLandscapeLarge)
            v.setPadding(0, getResources().getDimensionPixelSize(R.dimen.actionbar_height), 0, 0);

        mListView = (RecyclerView) v.findViewById(R.id.cardList);

        mProgressContainer = v.findViewById(R.id.progressContainer);
        mListContainer = v.findViewById(R.id.listContainer);
        mEmptyText = v.findViewById(android.R.id.empty);

        ((TextView) v.findViewById(android.R.id.empty)).setText(R.string.text_nofavorites);

        return v;
    }

	@Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ((BaseActivity) getActivity()).getToolbar().setTitle(getResources().getString(R.string.action_favoriteslist));

        arrayList = new ArrayList<>();
        PostsRecyclerAdapter mAdapter = new PostsRecyclerAdapter(getActivity(), arrayList, false);

        mListView.setHasFixedSize(true);
        LinearLayoutManager llm = new LinearLayoutManager(getActivity());
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        mListView.setLayoutManager(llm);
        
        DatabaseHelper db = new DatabaseHelper(getActivity());
        setItems(db.getFavourites());
        mListView.setAdapter(mAdapter);
        setListShown(true);
        if(arrayList.size() > 0)
            mEmptyText.setVisibility(View.GONE);
	}

    public void setListShown(boolean shown, boolean animate){
        if (mListShown == shown)
            return;

        mListShown = shown;
        if (shown) {
            if (animate) {
                mProgressContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity(), android.R.anim.fade_out));
                mListContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity(), android.R.anim.fade_in));
            }
            mProgressContainer.setVisibility(View.GONE);
            mListContainer.setVisibility(View.VISIBLE);
        } else {
            if (animate) {
                mProgressContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity(), android.R.anim.fade_in));
                mListContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity(), android.R.anim.fade_out));
            }
            mProgressContainer.setVisibility(View.VISIBLE);
            mListContainer.setVisibility(View.INVISIBLE);
        }
    }

    public void setListShown(boolean shown){
        setListShown(shown, true);
    }

	private void setItems(Cursor c) {
		if(c.moveToFirst()) {
			do {
				HashMap<String,String> map = new HashMap<String,String>();
				map.put(Constants.KEY_ID, c.getString(c.getColumnIndex(FavoritesTable.POST_ID)));
				map.put(Constants.KEY_TITLE, c.getString(c.getColumnIndex(FavoritesTable.POST_TITLE)));
				map.put(Constants.KEY_DATE, c.getString(c.getColumnIndex(FavoritesTable.POST_DATE)));
				map.put(Constants.KEY_IMG, c.getString(c.getColumnIndex(FavoritesTable.POST_IMG)));
				map.put(Constants.KEY_TYPE, c.getString(c.getColumnIndex(FavoritesTable.POST_TYPE)));
                map.put(Constants.KEY_URL, c.getString(c.getColumnIndex(FavoritesTable.POST_URL)));
				arrayList.add(map);
			} while(c.moveToNext());
		}
		c.close();
	}
	
}
