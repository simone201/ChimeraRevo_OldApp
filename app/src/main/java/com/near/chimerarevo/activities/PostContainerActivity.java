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

package com.near.chimerarevo.activities;

import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.analytics.tracking.android.EasyTracker;
import com.near.chimerarevo.R;
import com.near.chimerarevo.fragments.CommentsFragment;
import com.near.chimerarevo.fragments.PostFragment;
import com.near.chimerarevo.interfaces.LoadingViewCallback;
import com.near.chimerarevo.interfaces.ViewPagerScrollCallback;
import com.near.chimerarevo.misc.Constants;
import com.near.chimerarevo.sqlite.DatabaseHelper;
import com.near.chimerarevo.utils.JSONUtils;
import com.near.chimerarevo.utils.OkHttpUtils;
import com.near.chimerarevo.utils.SnackbarUtils;
import com.near.chimerarevo.utils.URLUtils;
import com.near.chimerarevo.widget.MaterialShareActionProvider;
import com.pnikosis.materialishprogress.ProgressWheel;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

public class PostContainerActivity extends BaseActivity implements View.OnClickListener,
        ViewPagerScrollCallback, LoadingViewCallback, ViewPager.OnPageChangeListener {

    private static final String ACTIVITY_TAG = "PostContainerActivity";

    private Handler mHandler = new Handler();

    private ViewPager pager;
    private MaterialShareActionProvider mShareActionProvider;
    private MenuItem mFavoriteItem;
    private ProgressWheel mLoading;
    private View mShadow;

    private Bundle args, frag_args;

    private int oldAlpha;
    private boolean isSaved = false;
    private boolean isLandscapeLarge = false;

    @Override
    public int getLayoutResource() {
        return R.layout.post_container_layout;
    }

    @Override
    public void onStart() {
        super.onStart();
        if(PreferenceManager.getDefaultSharedPreferences(this).getBoolean("analytics_pref", true))
            EasyTracker.getInstance(this).activityStart(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        if(PreferenceManager.getDefaultSharedPreferences(this).getBoolean("analytics_pref", true))
            EasyTracker.getInstance(this).activityStop(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if ((getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK)
                    >= Configuration.SCREENLAYOUT_SIZE_LARGE)
                isLandscapeLarge = true;
        } else
            isLandscapeLarge = false;

        getToolbar().setNavigationIcon(getResources().getDrawable(R.drawable.ic_action_arrow_back));
        getToolbar().setNavigationOnClickListener(this);

        if(!isLandscapeLarge) {
            pager = (ViewPager) findViewById(R.id.view_pager);
            pager.setOnPageChangeListener(this);
        }
        mLoading = (ProgressWheel) findViewById(R.id.post_progress);
        mShadow = findViewById(R.id.drop_shadow);

        args = getIntent().getExtras();

        if(savedInstanceState != null)
            args = savedInstanceState.getBundle("arguments");

        if(args.containsKey(Constants.KEY_DATE))
            setToolbarStatusColor(args.getString(Constants.KEY_DATE).split("[\\x7C]")[1].trim());

        if(getIntent().getData() != null) {
            List<String> params = getIntent().getData().getPathSegments();
            if(params != null && params.size() > 0) {
                String ext_url = Constants.SITE_URL;
                for(String p : params) {
                    ext_url += p + "/";
                }

                if(ext_url.contains("/" + Constants.PRODOTTI + "/")) {
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse(ext_url));
                    startActivity(i);
                    finish();
                } else {
                    if(ext_url.contains("/" + Constants.RECENSIONI + "/"))
                        args.putString(Constants.KEY_TYPE, Constants.RECENSIONI);
                    else if(ext_url.contains("/" + Constants.VIDEO + "/"))
                        args.putString(Constants.KEY_TYPE, Constants.VIDEO);

                    OkHttpUtils.getInstance()
                            .newCall(new Request.Builder()
                                    .url(URLUtils.getPostUrl(ext_url))
                                    .tag(ACTIVITY_TAG)
                                    .build()).enqueue(new GetPostCallback());
                }
            }
        }

        if(args != null) {
            if(!args.containsKey("isLandscapeLarge"))
                args.putBoolean("isLandscapeLarge", isLandscapeLarge);

            if(args.containsKey(Constants.KEY_ID)) {
                int post_id = args.getInt(Constants.KEY_ID);
                OkHttpUtils.getInstance()
                        .newCall(new Request.Builder()
                                .url(URLUtils.getPostUrl(post_id))
                                .tag(ACTIVITY_TAG)
                                .build()).enqueue(new GetPostCallback());
            } else if(args.containsKey(Constants.KEY_URL)) {
                String post_url = args.getString(Constants.KEY_URL);
                OkHttpUtils.getInstance()
                        .newCall(new Request.Builder()
                                .url(URLUtils.getPostUrl(post_url))
                                .tag(ACTIVITY_TAG)
                                .build()).enqueue(new GetPostCallback());
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBundle("arguments", args);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        args = savedInstanceState.getBundle("arguments");
        if(args.containsKey("isLandscapeLarge"))
            isLandscapeLarge = args.getBoolean("isLandscapeLarge", false);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if ((newConfig.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE) {
            if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE)
                isLandscapeLarge = true;
            else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT)
                isLandscapeLarge = false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.post, menu);

        mShareActionProvider = (MaterialShareActionProvider) MenuItemCompat.getActionProvider(menu.findItem(R.id.action_share));

        if(args == null)
            return true;

        if(args.containsKey(Constants.KEY_URL) && !args.containsKey(Constants.KEY_ID))
            menu.removeItem(R.id.action_favorite);
        else if(args.containsKey(Constants.KEY_ID)) {
            mFavoriteItem = menu.findItem(R.id.action_favorite);

            DatabaseHelper db = new DatabaseHelper(this);
            isSaved = db.hasFavourite(args.getInt(Constants.KEY_ID));
            if(isSaved) {
                mFavoriteItem.setTitle(getResources().getString(R.string.action_remove_favorite));
                mFavoriteItem.setIcon(getResources().getDrawable(R.drawable.ic_action_favorite));
            }
            db.close();
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_favorite:
                DatabaseHelper db = new DatabaseHelper(this);
                if(isSaved) {
                    if(db.removeFavourite(args.getInt(Constants.KEY_ID))) {
                        mFavoriteItem.setTitle(getResources().getString(R.string.action_favorite));
                        mFavoriteItem.setIcon(getResources().getDrawable(R.drawable.ic_action_favorite_outline));
                    }
                } else {
                    if(db.insertFavouritePost(args.getInt(Constants.KEY_ID), args.getString(Constants.KEY_TITLE),
                            args.getString(Constants.KEY_IMG), args.getString(Constants.KEY_DATE),
                            args.getString(Constants.KEY_TYPE), args.getString(Constants.KEY_URL)) != -1) {
                        mFavoriteItem.setTitle(getResources().getString(R.string.action_remove_favorite));
                        mFavoriteItem.setIcon(getResources().getDrawable(R.drawable.ic_action_favorite));
                    }
                }
                isSaved = !isSaved;
                db.close();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.hold, R.anim.push_right_exit);
    }

    @Override
    public void onClick(View view) {
        finish();
        overridePendingTransition(R.anim.hold, R.anim.push_right_exit);
    }

    @Override
    public void scrollViewPager(int position) {
        if(pager != null)
            pager.setCurrentItem(position);
    }

    @Override
    public void setIsLoading(boolean isLoading) {
        if(mLoading != null) {
            if(isLoading)
                mLoading.setVisibility(View.VISIBLE);
            else
                mLoading.setVisibility(View.GONE);
        }
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        // do nothing
    }

    @Override
    public void onPageSelected(int position) {
        if(position == 1) {
            oldAlpha = ((PostFragment) ((PostPagerAdapter) pager.getAdapter()).getItem(0)).getCurAlpha();
            getToolbar().getBackground().setAlpha(255);
        } else if(position == 0)
            getToolbar().getBackground().setAlpha(oldAlpha);
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        // do nothing
    }

    public View getDropShadow() {
        return mShadow;
    }

    private void setToolbarStatusColor(String mCat) {
        int colorPrimary = getResources().getColor(R.color.colorPrimary);
        int colorDark = getResources().getColor(R.color.colorPrimaryDark);
        int[] colorsPrimary = getResources().getIntArray(R.array.categories_colors);
        int[] colorsDark = getResources().getIntArray(R.array.categories_colors_dark);
        String[] cats = getResources().getStringArray(R.array.menu_categories_titles_italian);

        for(int i = 0; i < cats.length; i++) {
            if(mCat.equalsIgnoreCase(cats[i])) {
                colorPrimary = colorsPrimary[i];
                colorDark = colorsDark[i];
            }
        }

        getToolbar().setBackgroundColor(colorPrimary);

       if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            getWindow().setStatusBarColor(colorDark);
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
            mLoading.setVisibility(View.GONE);

            if(showSnackbar)
                SnackbarUtils.showShortSnackbar(PostContainerActivity.this,
                        error).show(PostContainerActivity.this);
        }
    }

    private class GetPostCallback implements Callback {

        @Override
        public void onFailure(Request request, final IOException e) {
            e.printStackTrace();

            mHandler.post(new GetPostsErrorRunnable(e.getMessage(), true));
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

            try {
                JSONObject jObject = JSONUtils
                        .getJSONObject(responseBody, Constants.KEY_POST);

                final Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "");
                shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, jObject.getString(Constants.KEY_POST_TITLE)
                        + " - " + jObject.getString(Constants.KEY_POST_URL) + " via @chimerarevo");

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            mShareActionProvider.setShareIntent(shareIntent);
                        } catch (NullPointerException e) {
                            e.printStackTrace();
                        }
                    }
                });

                StringBuilder sb = new StringBuilder();
                sb.append("<html><head>");
                sb.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">");
                sb.append("</head><body>");
                sb.append(jObject.getString(Constants.KEY_POST_CONTENT));
                sb.append("</body></html>");

                frag_args = new Bundle();
                frag_args.putBoolean("isLandscapeLarge", isLandscapeLarge);
                frag_args.putString("html", sb.toString());
                if (args.containsKey(Constants.KEY_TYPE))
                    frag_args.putString(Constants.KEY_TYPE, args.getString(Constants.KEY_TYPE));
                frag_args.putBoolean("hasTitle", true);
                if (!args.containsKey(Constants.KEY_URL))
                    args.putString(Constants.KEY_URL, jObject.getString(Constants.KEY_URL));
                frag_args.putString(Constants.KEY_POST_TITLE, jObject.getString(Constants.KEY_POST_TITLE));
                frag_args.putString(Constants.KEY_POST_SUBTITLE, jObject.getString(Constants.KEY_POST_SUBTITLE));
                if (args.containsKey(Constants.KEY_IMG))
                    frag_args.putString(Constants.KEY_IMG, args.getString(Constants.KEY_IMG));
                else
                    frag_args.putString(Constants.KEY_IMG, jObject.getJSONArray(Constants.KEY_IMG).getString(0));
                frag_args.putString(Constants.KEY_POST_AUTHOR,
                        JSONUtils.getJSONObject(responseBody, Constants.KEY_AUTHOR)
                                .getString(Constants.KEY_POST_AUTHOR));
                if (jObject.has(Constants.KEY_VIDEO_URL))
                    frag_args.putString(Constants.KEY_VIDEO_URL, jObject.getString(Constants.KEY_VIDEO_URL));
                frag_args.putBoolean("isLandscapeLarge", isLandscapeLarge);

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if(pager != null)
                            pager.setAdapter(new PostPagerAdapter(getSupportFragmentManager()));
                        else {
                            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);

                            Fragment frag = new PostFragment();
                            frag.setArguments(frag_args);
                            ft.replace(R.id.post_fragment, frag);

                            frag = new CommentsFragment();
                            frag.setArguments(args);
                            ft.replace(R.id.comments_fragment, frag);

                            ft.commit();
                        }

                        mLoading.setVisibility(View.GONE);
                    }
                });
            } catch (JSONException | NullPointerException | ExceptionInInitializerError e) {
                e.printStackTrace();
                mHandler.post(new GetPostsErrorRunnable(
                        getResources().getString(R.string.error_occurred) + ": " + e.getMessage()
                        , true));
            }
        }
    }

    private class PostPagerAdapter extends FragmentPagerAdapter {

        private static final int NUM_FRAGS = 2;
        private PostFragment frag0;
        private CommentsFragment frag1;

        public PostPagerAdapter(FragmentManager fm) {
            super(fm);
            frag0 = new PostFragment();
            frag1 = new CommentsFragment();

            frag0.setArguments(frag_args);
            frag1.setArguments(args);
        }

        @Override
        public Fragment getItem(int position) {
            switch(position) {
                case 0:
                    return frag0;
                case 1:
                    return frag1;
                default:
                    return new Fragment();

            }
        }

        @Override
        public int getCount() {
            return NUM_FRAGS;
        }

    }

}
