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

import java.io.IOException;
import java.util.Iterator;

import org.json.JSONException;
import org.json.JSONObject;

import com.fscz.util.TextViewEx;
import com.near.chimerarevo.R;
import com.near.chimerarevo.misc.Constants;
import com.near.chimerarevo.utils.JSONUtils;
import com.near.chimerarevo.utils.OkHttpUtils;
import com.near.chimerarevo.utils.SnackbarUtils;
import com.near.chimerarevo.utils.URLUtils;
import com.near.chimerarevo.widget.MaterialShareActionProvider;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.pnikosis.materialishprogress.ProgressWheel;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TextView;

public class ProductFragment extends Fragment {

	private static final String FRAGMENT_TAG = "ProductFragment";

	private MaterialShareActionProvider mShareActionProvider;
    private ProgressWheel mLoading;

	private Handler mHandler = new Handler();

    private View descr_container, specs_container;
	private LinearLayout lay;
	private TextViewEx descr;
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		setHasOptionsMenu(true);
		View v = inflater.inflate(R.layout.product_layout, container, false);

        ImageView img = (ImageView) v.findViewById(R.id.prod_img);
        TextView name = (TextView) v.findViewById(R.id.prod_name);
        descr_container = v.findViewById(R.id.prod_descr_container);
        specs_container = v.findViewById(R.id.prod_specs_container);
		lay = (LinearLayout) v.findViewById(R.id.prod_specs);
		descr = (TextViewEx) v.findViewById(R.id.prod_descr);
        mLoading = (ProgressWheel) v.findViewById(R.id.product_progress);
		
		if(getArguments().containsKey(Constants.KEY_IMG)) {
			DisplayImageOptions options = new DisplayImageOptions.Builder()
				.cacheOnDisk(false)
				.cacheInMemory(true)
				.showImageOnLoading(R.drawable.empty_cr)
				.bitmapConfig(Bitmap.Config.RGB_565)
				.imageScaleType(ImageScaleType.IN_SAMPLE_INT)
				.delayBeforeLoading(100)
				.build();
			ImageLoader.getInstance().displayImage(getArguments().getString(Constants.KEY_IMG), img, options);
		}
		
		if(getArguments().containsKey(Constants.KEY_NAME))
			name.setText(getArguments().getString(Constants.KEY_NAME));
		
		return v;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
	
		if(getArguments().containsKey(Constants.KEY_ID)) {
			Request request = new Request.Builder()
					.url(URLUtils.getProductUrl(getArguments().getInt(Constants.KEY_ID)))
					.tag(FRAGMENT_TAG)
					.build();

			OkHttpUtils.getInstance().newCall(request).enqueue(new GetProductCallback());
		}
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.post, menu);
		super.onCreateOptionsMenu(menu, inflater);
        menu.removeItem(R.id.action_favorite);
		mShareActionProvider = (MaterialShareActionProvider) MenuItemCompat.getActionProvider(menu.findItem(R.id.action_share));
	}
	
	@SuppressWarnings("unchecked")
	private void parseProduct(String result) {
		JSONObject specs;
		try {
			descr.setText(Html.fromHtml(
					(new JSONObject(result))
							.getJSONObject(Constants.KEY_POST)
							.getString(Constants.KEY_POST_CONTENT)), true);
			specs = (new JSONObject(result))
					.getJSONObject(Constants.KEY_POST)
					.getJSONObject(Constants.KEY_PROPS);
		} catch (JSONException e) {
			e.printStackTrace();

			SnackbarUtils.showShortSnackbar(getActivity(),
					getResources().getString(R.string.error_occurred)).show(getActivity());
			mLoading.setVisibility(View.GONE);

			return;
		}
		
		Iterator<String> par = specs.keys();
		while(par.hasNext()) {
			String tag = par.next();
			addTitle(tag);

			try {
				int i = 0;
				JSONObject jObj = specs.getJSONObject(tag);
				Iterator<String> vals = jObj.keys();
				
				while(vals.hasNext()) {
					i++;
					String key = vals.next();
					String val = jObj.getString(key);
					addRow(key, val, i);
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}

		descr_container.setVisibility(View.VISIBLE);
		specs_container.setVisibility(View.VISIBLE);
		mLoading.setVisibility(View.GONE);
	}
	
	private void addTitle(String text) {
		TextView title = new TextView(getActivity());
		title.setBackgroundColor(getResources().getColor(R.color.prod_title_color));
		title.setTextColor(getResources().getColor(android.R.color.white));
		title.setTypeface(Typeface.createFromAsset(getActivity().getAssets(), "roboto_light.ttf"));
		title.setTextSize(22);
		title.setPadding(15, 10, 15, 10);
		
		LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		title.setLayoutParams(params);
		
		title.setText(text);
		
		View div = new View(getActivity());
		div.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, 2));
		div.setBackgroundColor(getResources().getColor(R.color.prod_title_color));
		
		lay.addView(title);
		lay.addView(div);
	}
	
	private void addRow(String key, String val, int i) {
		LinearLayout ll = new LinearLayout(getActivity());
		ll.setOrientation(LinearLayout.HORIZONTAL);
		ll.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		if(i % 2 == 1)
			ll.setBackgroundColor(getResources().getColor(android.R.color.white));
		
		TextView tv1 = new TextView(getActivity());
		TextView tv2 = new TextView(getActivity());
		tv1.setLayoutParams(new TableLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1f));
		tv2.setLayoutParams(new TableLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1f));
		tv1.setPadding(20, 10, 0, 10);
		tv2.setPadding(0, 10, 20, 10);
		tv1.setTypeface(Typeface.createFromAsset(getActivity().getAssets(), "roboto_condensed.ttf"));
		tv2.setTypeface(Typeface.createFromAsset(getActivity().getAssets(), "roboto_light.ttf"));
		tv1.setTextSize(16);
		tv2.setTextSize(15);
		
		tv1.setText(key);
		tv2.setText(val);
		
		ll.addView(tv1);
		ll.addView(tv2);
		lay.addView(ll);
	}

	private class GetProductErrorRunnable implements Runnable {

		private String error;
		private boolean showSnackbar;

		public GetProductErrorRunnable(String error, boolean showSnackbar) {
			this.error = error;
			this.showSnackbar = showSnackbar;
		}

		@Override
		public void run() {
			mLoading.setVisibility(View.GONE);

			if(showSnackbar)
				SnackbarUtils.showMultiShortSnackbar(getActivity(), error).show(getActivity());
		}

	}

	private class GetProductCallback implements Callback {

		@Override
		public void onFailure(Request request, IOException e) {
			e.printStackTrace();

			mHandler.post(new GetProductErrorRunnable(e.getMessage(), true));
		}

		@Override
		public void onResponse(Response response) throws IOException {
			if(response == null) {
				mHandler.post(new GetProductErrorRunnable(
						getResources().getString(R.string.error_occurred), true));
				return;
			}

			if(!response.isSuccessful()) {
				mHandler.post(new GetProductErrorRunnable(
						response.message() + " - " + response.code(), true));
				return;
			}

			final String responseBody = response.body().string().trim();
			final String responseMessage = response.message();

			try {
				JSONObject jObject =
						JSONUtils.getJSONObject(responseBody, Constants.KEY_POST);

				final Intent shareIntent = new Intent(Intent.ACTION_SEND);
				shareIntent.setType("text/plain");
				shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "");
				shareIntent.putExtra(android.content.Intent.EXTRA_TEXT,
						jObject.getString(Constants.KEY_POST_TITLE)
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

			} catch (Exception e) {
				e.printStackTrace();
			}

			mHandler.post(new Runnable() {
				@Override
				public void run() {
					try {
						parseProduct(responseBody);
					} catch (Exception e) {
						e.printStackTrace();

						SnackbarUtils.showMultiShortSnackbar(getActivity(), responseMessage)
								.show(getActivity());
						mLoading.setVisibility(View.GONE);
					}

				}
			});
		}
	}

}
