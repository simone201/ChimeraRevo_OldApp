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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.melnykov.fab.FloatingActionButton;
import com.near.chimerarevo.R;
import com.near.chimerarevo.activities.BaseActivity;
import com.near.chimerarevo.adapters.PostsRecyclerAdapter;
import com.near.chimerarevo.misc.Constants;
import com.near.chimerarevo.misc.PostsListObject;
import com.near.chimerarevo.utils.JSONUtils;
import com.near.chimerarevo.utils.OkHttpUtils;
import com.near.chimerarevo.utils.SnackbarUtils;
import com.near.chimerarevo.utils.SysUtils;
import com.near.chimerarevo.utils.URLUtils;
import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.listeners.EventListener;
import com.pnikosis.materialishprogress.ProgressWheel;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import at.markushi.ui.CircleButton;

public class SearchFragment extends Fragment implements View.OnClickListener, EventListener {

    private static final String FRAGMENT_TAG = "SearchFragment";

    private Handler mHandler = new Handler();

    private FloatingActionButton mFab;
    private ProgressWheel mLoading;

    private RecyclerView mListView;
    private PostsRecyclerAdapter mAdapter;

	private EditText mText;
	private CircleButton mBtn;
	
	private static byte page = 1;
	private byte counter = 1;
	private int clickCounter = 0, numEntries = 0, mScrollThreshold = 0;
	private boolean shouldAddToStack, shouldSmoothScroll;
	
	private ArrayList<String> mJson;
    private ArrayList<HashMap<String,String>> arrayList;
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.search_layout, container, false);

        mListView = (RecyclerView) v.findViewById(R.id.cardList);
        mListView.setOverScrollMode(View.OVER_SCROLL_NEVER);

        mFab = (FloatingActionButton) v.findViewById(R.id.floating_action);
        mFab.setEnabled(false);

		mText = (EditText) v.findViewById(R.id.search_text);
		mBtn = (CircleButton) v.findViewById(R.id.search_btn);

		return v;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
        ((BaseActivity) getActivity()).getToolbar().setTitle(getResources().getString(R.string.action_search));

        mJson = new ArrayList<>();
        arrayList = new ArrayList<>();
        mAdapter = new PostsRecyclerAdapter(getActivity(), arrayList, false);

        mListView.setHasFixedSize(false);
        final LinearLayoutManager llm = new LinearLayoutManager(getActivity());
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        mListView.setLayoutManager(llm);
        mListView.setAdapter(mAdapter);

        mFab.attachToRecyclerView(mListView);
        mFab.setOnClickListener(this);

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
            }
        });

		if(readOfflineFile()) {
			clickCounter++;
			mFab.setEnabled(true);
		}
		
		mBtn.setOnClickListener(this);
		
		mText.setOnEditorActionListener(new OnEditorActionListener() {
        	@Override
		    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		        if (actionId == EditorInfo.IME_ACTION_SEARCH) {
	                if(mText.getText().toString().length() > 0) {
                        InputMethodManager in = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                        in.hideSoftInputFromWindow(mText.getApplicationWindowToken(),InputMethodManager.HIDE_NOT_ALWAYS);

	                	mText.setError(null);
                        mFab.setEnabled(true);
                        performUpdate();
	                } else {
	                	mText.setError(getResources().getString(R.string.error_field_required));
	                	mText.requestFocus();
	                }
		        }
		        return true;
        	}
        });
	}
	
	@Override
	public void onPause() {
		super.onPause();
        SysUtils.writeOfflineFile(getActivity(), mJson, "search.ser");
	}
	
	@Override
	public void onDestroy() {
        new File(getActivity().getCacheDir() + "/search.ser").delete();
        super.onDestroy();
	}

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.progress, menu);
        super.onCreateOptionsMenu(menu, inflater);
        mLoading = (ProgressWheel) MenuItemCompat.getActionView(menu.findItem(R.id.refresh_view)).findViewById(R.id.progressBar);
    }

    @Override
    public void onClick(View view) {
        if(view instanceof FloatingActionButton)
            addPage();
        else if(view instanceof CircleButton) {
            clickCounter++;

            if(clickCounter == 1)
                mFab.setEnabled(true);

            if(mText.getText().toString().length() > 0) {
                InputMethodManager in = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                in.hideSoftInputFromWindow(mText.getApplicationWindowToken(),InputMethodManager.HIDE_NOT_ALWAYS);

                mText.setError(null);
                performUpdate();
            } else {
                mText.setError(getResources().getString(R.string.error_field_required));
                mText.requestFocus();
            }
        }
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
        page = 1;
        counter = 1;
        numEntries = 0;
        shouldAddToStack = false;
        shouldSmoothScroll = false;

        mLoading.setVisibility(View.VISIBLE);

        OkHttpUtils.getInstance().newCall(new Request.Builder()
                .url(URLUtils.getSearchUrl(mText.getText().toString(), page))
                .tag(FRAGMENT_TAG)
                .build()).enqueue(new GetPostsCallback());
    }

    private void addPage() {
        page++;
        counter = page;
        shouldAddToStack = true;
        shouldSmoothScroll = true;

        mLoading.setVisibility(View.VISIBLE);

        OkHttpUtils.getInstance().newCall(new Request.Builder()
                .url(URLUtils.getSearchUrl(mText.getText().toString(), page))
                .tag(FRAGMENT_TAG)
                .build()).enqueue(new GetPostsCallback());
    }
	
	private boolean readOfflineFile() {
		try {
			if(new File(getActivity().getCacheDir() + "/search.ser").exists()) {
				InputStream file = new FileInputStream(getActivity().getCacheDir() + "/search.ser");
			    InputStream buffer = new BufferedInputStream(file);
			    ObjectInput input = new ObjectInputStream(buffer);
			    mJson = ((PostsListObject) input.readObject()).getJSONs();
                input.close();
                shouldSmoothScroll = false;
			    try {
			    	for(String json : mJson)
			    		setItems(JSONUtils.getJSONArray(json, Constants.KEY_POSTS));
			    } catch (Exception e) {
			    	e.printStackTrace();
			    	return false;
			    }
			    return true;
			} else
				return false;
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
        return false;
	}
	
	private void addItem(final JSONObject jObject) throws JSONException {
		HashMap<String,String> map = new HashMap<>();
		map.put(Constants.KEY_ID, String.valueOf(jObject.getInt(Constants.KEY_ID)));
		map.put(Constants.KEY_TITLE, jObject.getString(Constants.KEY_TITLE));

        String cat = JSONUtils.getCategory(jObject);
        if(cat.trim().equalsIgnoreCase("") || cat.length() < 1)
            if(getArguments().containsKey(Constants.KEY_TYPE))
                cat = getArguments().getString(Constants.KEY_TYPE);

        map.put(Constants.KEY_DATE, jObject.getString(Constants.KEY_DATE) + " | " + cat);
		map.put(Constants.KEY_IMG, jObject.getString(Constants.KEY_IMG));
        map.put(Constants.KEY_TYPE, jObject.getString(Constants.KEY_TYPE));
        map.put(Constants.KEY_URL, jObject.getString(Constants.KEY_URL));
        arrayList.add(map);
        mAdapter.notifyItemInserted(numEntries);
    }
	
	private void setItems(JSONArray jArray) {
		for(int i = 0; i < jArray.length(); i++) {
			try {
				final JSONObject jObject = jArray.getJSONObject(i);
                addItem(jObject);
                numEntries++;

                if(shouldSmoothScroll)
                    mListView.smoothScrollToPosition(numEntries);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
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

            if(arrayList.size() < 1)
                mFab.setEnabled(false);

            if(showSnackbar)
                SnackbarUtils.showMultiShortSnackbar(getActivity(),
                        error, SearchFragment.this).show(getActivity());
        }
    }

    private class GetPostsCallback implements Callback {

        @Override
        public void onFailure(Request request, IOException e) {
            e.printStackTrace();

            if(page > counter)
                page = counter;

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
            }

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        setItems(JSONUtils.getJSONArray(responseBody, Constants.KEY_POSTS));
                        mAdapter.notifyDataSetChanged();
                        mJson.add(responseBody);
                    } catch (Exception e) {
                        e.printStackTrace();

                        SnackbarUtils.showMultiShortSnackbar(getActivity(),
                                getResources().getString(R.string.error_occurred) + ": " + e.getMessage(),
                                SearchFragment.this).show(getActivity());
                        mLoading.setVisibility(View.GONE);
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
            }
        }
    }
	
}
