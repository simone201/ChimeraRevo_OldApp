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

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.flaviofaria.kenburnsview.KenBurnsView;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.melnykov.fab.FloatingActionButton;
import com.near.chimerarevo.R;
import com.near.chimerarevo.activities.BaseActivity;
import com.near.chimerarevo.activities.MainActivity;
import com.near.chimerarevo.adapters.PostsRecyclerAdapter;
import com.near.chimerarevo.misc.Constants;
import com.near.chimerarevo.misc.PostsListObject;
import com.near.chimerarevo.providers.PostsLRWidgetProvider;
import com.near.chimerarevo.providers.PostsListWidgetProvider;
import com.near.chimerarevo.utils.JSONUtils;
import com.near.chimerarevo.utils.OkHttpUtils;
import com.near.chimerarevo.utils.SnackbarUtils;
import com.near.chimerarevo.utils.SysUtils;
import com.near.chimerarevo.utils.URLUtils;
import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.listeners.EventListener;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.pnikosis.materialishprogress.ProgressWheel;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

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

public class PostsRecyclerFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener, View.OnClickListener, EventListener {

    private static final String FRAGMENT_TAG = "PostsRecyclerFragment";

    private Handler mHandler = new Handler();

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private FloatingActionButton mFab;
    private ProgressWheel mLoading, mLoadingBar;

    private RecyclerView mListView;
    private PostsRecyclerAdapter mAdapter;

    private KenBurnsView mHeaderImg;
    private TextView mHeaderText;
    private View mHeader, mShadow;

    private static byte page = 1;
    private byte counter = 1;
    private int scrollY = 0, numEntries = 0, mScrollThreshold = 0;
    private boolean shouldAddToStack, shouldSmoothScroll, isLandscapeLarge;

    private ArrayList<String> mJson;
    private ArrayList<HashMap<String,String>> arrayList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.posts_list_layout, container, false);

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if ((getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK)
                    >= Configuration.SCREENLAYOUT_SIZE_LARGE)
                isLandscapeLarge = true;
        } else
            isLandscapeLarge = false;

        if(!isLandscapeLarge)
            ((BaseActivity) getActivity()).getToolbar().getBackground().setAlpha(0);

        mListView = (RecyclerView) v.findViewById(R.id.cardList);
        mListView.setOverScrollMode(View.OVER_SCROLL_NEVER);

        mLoadingBar = (ProgressWheel) v.findViewById(R.id.loading_bar);
        mFab = (FloatingActionButton) v.findViewById(R.id.floating_action);
        mFab.setEnabled(false);

        mHeader = v.findViewById(R.id.header);
        mHeaderText = (TextView) mHeader.findViewById(R.id.header_title);
        mHeaderImg = (KenBurnsView) mHeader.findViewById(R.id.header_img);

        mSwipeRefreshLayout = new SwipeRefreshLayout(container.getContext());
        mSwipeRefreshLayout.addView(v, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        mSwipeRefreshLayout.setLayoutParams(
                new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.blue_light, R.color.red_light,
                R.color.green_light, R.color.orange_light);

        return mSwipeRefreshLayout;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
        ((BaseActivity) getActivity()).getToolbar().setTitle(getResources().getString(R.string.app_name));

        mJson = new ArrayList<>();
        arrayList = new ArrayList<>();
        mAdapter = new PostsRecyclerAdapter(getActivity(), arrayList, true);

        mListView.setHasFixedSize(false);
        final LinearLayoutManager llm = new LinearLayoutManager(getActivity());
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        mListView.setLayoutManager(llm);
        mListView.setAdapter(mAdapter);

        mScrollThreshold = getResources().getDimensionPixelOffset(R.dimen.fab_scroll_threshold);

        mFab.attachToRecyclerView(mListView);
        mFab.setOnClickListener(this);

        mShadow = ((MainActivity) getActivity()).getDropShadow();
        if(mShadow != null && !isLandscapeLarge)
            mShadow.setAlpha(0);

        mListView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                int visibleItemCount = llm.getChildCount();
                int totalItemCount = llm.getItemCount();
                int pastVisiblesItems = llm.findFirstVisibleItemPosition();

                if ((visibleItemCount + pastVisiblesItems) < totalItemCount) {
                    if (Math.abs(dy) > mScrollThreshold) {
                        if (dy > 0)
                            mFab.hide();
                        else
                            mFab.show();
                    }
                } else
                    mFab.show();

                scrollY += dy;
                mHeader.setTranslationY(-scrollY * 0.6f);
                if (scrollY == 0)
                    mSwipeRefreshLayout.setEnabled(true);
                else
                    mSwipeRefreshLayout.setEnabled(false);

                if (!isLandscapeLarge) {
                    float offset = scrollY * 0.6f;
                    float ratio = Math.min(1, ((offset) / (mHeader.getHeight() * 0.6f)));
                    int alpha = (int) (ratio * 255);
                    ((BaseActivity) getActivity()).getToolbar().getBackground().setAlpha(alpha);
                    if (mShadow != null) {
                        if (alpha > 250)
                            mShadow.setAlpha(alpha);
                        else
                            mShadow.setAlpha(0);
                    }
                }
            }
        });

        if(!getArguments().containsKey(Constants.KEY_CAT) & !getArguments().containsKey(Constants.KEY_TYPE))
            readOfflineFile();
        if(savedInstanceState == null)
            performUpdate();
    }

    @Override
    public void onDestroy() {
        page = 1;

        if(!getArguments().containsKey(Constants.KEY_CAT) && !getArguments().containsKey(Constants.KEY_TYPE)) {
            Intent update = new Intent(getActivity(), PostsListWidgetProvider.class);
            update.setAction(PostsListWidgetProvider.REFRESH_VIEWS_ACTION);
            getActivity().sendBroadcast(update);

            update = new Intent(getActivity(), PostsLRWidgetProvider.class);
            update.setAction(PostsLRWidgetProvider.REFRESH_VIEWS_ACTION);
            getActivity().sendBroadcast(update);
        }

        super.onDestroy();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.progress, menu);
        super.onCreateOptionsMenu(menu, inflater);
        mLoading = (ProgressWheel) MenuItemCompat.getActionView(menu.findItem(R.id.refresh_view)).findViewById(R.id.progressBar);
    }

    @Override
    public void onRefresh() {
        performUpdate();
    }

    @Override
    public void onClick(View view) {
        if(view instanceof FloatingActionButton)
            addPage();
    }

    @Override
    public void onShow(Snackbar snackbar) {
        if(mFab != null)
            mFab.hide();
    }

    @Override
    public void onShowByReplace(Snackbar snackbar) {
        // do nothing
    }

    @Override
    public void onShown(Snackbar snackbar) {
        // do nothing
    }

    @Override
    public void onDismiss(Snackbar snackbar) {
        // do nothing
    }

    @Override
    public void onDismissByReplace(Snackbar snackbar) {
        // do nothing
    }

    @Override
    public void onDismissed(Snackbar snackbar) {
        if(mFab != null)
            mFab.show();
    }

    private void performUpdate() {
        mJson = new ArrayList<>();
        counter = 1;
        numEntries = 0;
        OkHttpUtils.getInstance().cancel(FRAGMENT_TAG);
        performTask();
    }

    private void performTask() {
        shouldAddToStack = false;
        shouldSmoothScroll = false;

        Bundle args = getArguments();

        Request.Builder rb = new Request.Builder()
                .tag(FRAGMENT_TAG);

        if(args != null) {
            if(args.containsKey(Constants.KEY_CAT))
                rb.url(URLUtils.getUrl(1, args.getString(Constants.KEY_CAT)));
            else if(args.containsKey(Constants.KEY_TYPE))
                rb.url(URLUtils.getUrl(args.getString(Constants.KEY_TYPE)));
            else
                rb.url(URLUtils.getUrl());
        } else
            rb.url(URLUtils.getUrl());

        OkHttpUtils.getInstance().newCall(rb.build()).enqueue(new GetPostsCallback());
    }

    private void addPage() {
        page++;
        counter = page;
        shouldAddToStack = true;
        shouldSmoothScroll = true;

        mLoading.setVisibility(View.VISIBLE);

        Bundle args = getArguments();

        Request.Builder rb = new Request.Builder()
                .tag(FRAGMENT_TAG);

        if(args != null) {
            if(args.containsKey(Constants.KEY_CAT))
                rb.url(URLUtils.getUrl(page, args.getString(Constants.KEY_CAT)));
            else if(args.containsKey(Constants.KEY_TYPE))
                rb.url(URLUtils.getUrl(args.getString(Constants.KEY_TYPE), page));
            else
                rb.url(URLUtils.getUrl(page));
        } else
            rb.url(URLUtils.getUrl(page));

        OkHttpUtils.getInstance().newCall(rb.build()).enqueue(new GetPostsCallback());
    }

    private boolean readOfflineFile() {
        try {
            if(new File(getActivity().getCacheDir() + "/posts.ser").exists()) {
                mLoadingBar.setVisibility(View.VISIBLE);
                mSwipeRefreshLayout.setRefreshing(true);

                InputStream file = new FileInputStream(getActivity().getCacheDir() + "/posts.ser");
                ObjectInput input = new ObjectInputStream(new BufferedInputStream(file));
                mJson = ((PostsListObject) input.readObject()).getJSONs();
                input.close();
                shouldAddToStack = false;
                shouldSmoothScroll = false;

                try {
                    for(String json : mJson) {
                        setItems(JSONUtils.getJSONArray(json, Constants.KEY_POSTS));
                        shouldAddToStack = true;
                    }
                    mSwipeRefreshLayout.setRefreshing(false);
                    mLoadingBar.setVisibility(View.GONE);
                    mFab.setEnabled(true);
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException | ClassCastException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        mSwipeRefreshLayout.setRefreshing(false);
        mLoadingBar.setVisibility(View.GONE);

        return false;
    }

    private void setItems(JSONArray jArray) {
        for(int i = 0; i < jArray.length(); i++) {
            try {
                final JSONObject jObject = jArray.getJSONObject(i);
                addItem(jObject);
                numEntries++;

                if (i == 0) {
                    if (!shouldAddToStack) {
                        DisplayImageOptions options = new DisplayImageOptions.Builder()
                                .cacheOnDisk(false)
                                .cacheInMemory(true)
                                .imageScaleType(ImageScaleType.IN_SAMPLE_POWER_OF_2)
                                .delayBeforeLoading(100)
                                .build();
                        ImageLoader.getInstance().displayImage(jObject.getString(Constants.KEY_IMG), mHeaderImg, options);
                        mHeaderText.setText(Html.fromHtml(jObject.getString(Constants.KEY_TITLE)));
                    } else if (shouldSmoothScroll)
                        mListView.smoothScrollToPosition(numEntries);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
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
        mAdapter.notifyItemInserted(numEntries);
    }

    private class GetPostsErrorRunnable implements Runnable {

        private String error;
        private boolean showSnackbar;

        public GetPostsErrorRunnable(String error, boolean showSnackbar) {
            this.error = error;
            this.showSnackbar = showSnackbar;
        }

        @Override
        public void run() {
            if(mLoading != null)
                mLoading.setVisibility(View.GONE);
            mLoadingBar.setVisibility(View.GONE);
            mSwipeRefreshLayout.setRefreshing(false);

            if(showSnackbar)
                SnackbarUtils.showMultiShortSnackbar(getActivity(),
                        error, PostsRecyclerFragment.this).show(getActivity());
        }
    }

    private class GetPostsCallback implements Callback {

        @Override
        public void onFailure(Request request, IOException e) {
            e.printStackTrace();

            if(page > counter)
                page = counter;

            if(arrayList.size() < 1)
                mFab.setEnabled(false);

            mHandler.post(new GetPostsErrorRunnable(
                    e.getMessage(), true));
        }

        @Override
        public void onResponse(Response response) throws IOException {
            if(response == null) {
                mHandler.post(new GetPostsErrorRunnable(
                        getResources().getString(R.string.error_occurred), true));
                return;
            }

            if(!response.isSuccessful()) {
                mHandler.post(new GetPostsErrorRunnable(
                        response.message() + " - " + response.code(), true));
                return;
            }

            final String responseBody = response.body().string().trim();

            if(!shouldAddToStack) {
                arrayList.clear();
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mAdapter.notifyDataSetChanged();
                    }
                });

                if (mJson != null && mJson.size() > 0) {

                    JsonParser parser = new JsonParser();
                    JsonElement o1 = parser.parse(response.toString());
                    JsonElement o2 = parser.parse(mJson.get(0));

                    if (!o1.equals(o2)) {
                        mJson = new ArrayList<>();
                        SnackbarUtils.showMultiShortSnackbar(getActivity(),
                                getResources().getString(R.string.text_newfeed),
                                PostsRecyclerFragment.this).show(getActivity());
                    } else {
                        mLoading.setVisibility(View.GONE);
                        mSwipeRefreshLayout.setRefreshing(false);
                        return;
                    }
                }
            }


            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        setItems(JSONUtils.getJSONArray(responseBody, Constants.KEY_POSTS));
                        mAdapter.notifyDataSetChanged();
                        mJson.add(responseBody);
                        mFab.setEnabled(true);
                    } catch (Exception e) {
                        e.printStackTrace();

                        SnackbarUtils.showMultiShortSnackbar(getActivity(),
                                getResources().getString(R.string.error_occurred) + ": " + e.getMessage(),
                                PostsRecyclerFragment.this).show(getActivity());
                        counter = page;
                    }

                }
            });

            if(counter < page) {
                counter++;
                shouldAddToStack = true;
                Bundle args = getArguments();

                Request.Builder rb = new Request.Builder()
                        .tag(FRAGMENT_TAG);

                if(args != null) {
                    if(args.containsKey(Constants.KEY_CAT))
                        rb.url(URLUtils.getUrl(counter, args.getString(Constants.KEY_CAT)));
                    else if(args.containsKey(Constants.KEY_TYPE))
                        rb.url(URLUtils.getUrl(args.getString(Constants.KEY_TYPE), counter));
                    else
                        rb.url(URLUtils.getUrl(counter));
                } else
                    rb.url(URLUtils.getUrl(counter));

                OkHttpUtils.getInstance().newCall(rb.build()).enqueue(this);
            } else {
                counter = 1;
                mHandler.post(new GetPostsErrorRunnable(null, false));

                if(!getArguments().containsKey(Constants.KEY_CAT) && !getArguments().containsKey(Constants.KEY_TYPE))
                    if(mJson != null && mJson.size() > 0)
                        SysUtils.writeOfflineFile(getActivity(), mJson, "posts.ser");
            }
        }
    }

}
