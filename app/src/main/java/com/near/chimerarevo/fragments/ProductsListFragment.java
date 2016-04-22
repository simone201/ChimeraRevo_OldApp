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
import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.near.chimerarevo.R;
import com.near.chimerarevo.activities.ProductActivity;
import com.near.chimerarevo.adapters.ProductsAdapter;
import com.near.chimerarevo.misc.Constants;
import com.near.chimerarevo.utils.JSONUtils;
import com.near.chimerarevo.utils.OkHttpUtils;
import com.near.chimerarevo.utils.SnackbarUtils;
import com.near.chimerarevo.utils.URLUtils;
import com.pnikosis.materialishprogress.ProgressWheel;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.Spinner;

public class ProductsListFragment extends Fragment implements AdapterView.OnItemSelectedListener, AbsListView.OnScrollListener,
        AdapterView.OnItemClickListener {

    private static final String FRAGMENT_TAG = "ProductsListFragment";

    private Handler mHandler = new Handler();

    private ProductsAdapter mAdapter;

	private Spinner mCat, mBrand;
    private ProgressWheel mLoading, mLoadingToolbar;
	
	private static byte page = 1;
	private byte counter = 1;
    private boolean isTaskRunning;
	
	private ArrayList<String> brandsList;
	private ArrayList<String> catList;
	private ArrayList<Integer> catIdList;

    private ArrayList<HashMap<String,String>> arrayList;
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.products_grid_layout, container, false);

        boolean isLandscapeLarge = false;

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if ((getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK)
                    >= Configuration.SCREENLAYOUT_SIZE_LARGE)
                isLandscapeLarge = true;
        } else
            isLandscapeLarge = false;

        if(!isLandscapeLarge)
            v.setPadding(0, getResources().getDimensionPixelSize(R.dimen.actionbar_height), 0, 0);

        GridView mGrid = (GridView) v.findViewById(R.id.products_grid);
		mCat = (Spinner) v.findViewById(R.id.category_spinner);
		mBrand = (Spinner) v.findViewById(R.id.brand_spinner);
		mLoading = (ProgressWheel) v.findViewById(R.id.product_progress);

		brandsList = new ArrayList<>();
		catList = new ArrayList<>();
		catIdList = new ArrayList<>();

		arrayList = new ArrayList<>();
		
		mAdapter = new ProductsAdapter(arrayList);
        if (mGrid != null) {
            mGrid.setAdapter(mAdapter);
            mGrid.setOnScrollListener(this);
            mGrid.setOnItemClickListener(this);
        }

		return v;
	}

	@Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);

        SharedPreferences prefs = getActivity().getSharedPreferences("chimerarevo", Context.MODE_PRIVATE);
        
        if(prefs.getInt("timesProductsOpened", 0) > 5 || !prefs.getBoolean("firstProductsOpening", false)) {
            OkHttpUtils.getInstance().newCall(new Request.Builder()
                    .tag(FRAGMENT_TAG)
                    .url(Constants.CATEGORIES_URL).build())
                    .enqueue(new GetSpinnerCallback(mCat));

        	prefs.edit().putInt("timesProductsOpened", 0).apply();
        	prefs.edit().putBoolean("firstProductsOpening", true).apply();
        } else {
        	prefs.edit().putInt("timesProductsOpened", prefs.getInt("timesProductsOpened", 0) + 1).apply();
        	try {
                setSpinnerValue(mCat, (new JSONObject(prefs.getString("category_array", ""))).getJSONArray(Constants.KEY_CAT));
                setSpinnerValue(mBrand, (new JSONObject(prefs.getString("brands_array", ""))).getJSONArray(Constants.KEY_BRANDS));
                onItemSelected(null, null, 0, 0);
            } catch (JSONException e) {
                OkHttpUtils.getInstance().newCall(new Request.Builder()
                        .tag(FRAGMENT_TAG)
                        .url(Constants.CATEGORIES_URL).build())
                        .enqueue(new GetSpinnerCallback(mCat));
            }
        }
        
        mCat.setOnItemSelectedListener(this);
        mBrand.setOnItemSelectedListener(this);
	}
	
	@Override
	public void onDestroy() {
		page = 1;
		super.onDestroy();
	}

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.progress, menu);
        super.onCreateOptionsMenu(menu, inflater);

        mLoadingToolbar = (ProgressWheel) MenuItemCompat.getActionView(menu.findItem(R.id.refresh_view)).findViewById(R.id.progressBar);
    }

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        arrayList.clear();
        counter = 1;
        page = 1;

        Request.Builder rb = new Request.Builder()
                .tag(FRAGMENT_TAG);

        if(mCat.getSelectedItemPosition() > 0 && mBrand.getSelectedItemPosition() > 0)
            rb.url(URLUtils.getProductsUrl(counter,
                    String.valueOf(catIdList.get(mCat.getSelectedItemPosition() - 1)),
                    brandsList.get(mBrand.getSelectedItemPosition())));
        else if(mCat.getSelectedItemPosition() > 0)
            rb.url(URLUtils.getProductsUrl(counter,
                    String.valueOf(catIdList.get(mCat.getSelectedItemPosition() - 1)), ""));
        else if(mBrand.getSelectedItemPosition() > 0)
            rb.url(URLUtils.getProductsUrl(counter, "",
                    brandsList.get(mBrand.getSelectedItemPosition())));
        else
            rb.url(URLUtils.getProductsUrl(counter, "", ""));

        isTaskRunning = true;
        OkHttpUtils.getInstance().newCall(rb.build()).enqueue(new GetProductsCallback());

        if(mLoadingToolbar != null)
            mLoadingToolbar.setVisibility(View.VISIBLE);
	}
	
	@Override
	public void onNothingSelected(AdapterView<?> parent) {
		// do nothing
	}

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        // do nothing
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        if(totalItemCount < 10)
            return;

        byte visibleThreshold = 1;

        if (!isTaskRunning
                && (totalItemCount - visibleItemCount) <= (firstVisibleItem + visibleThreshold)) {

            page++;
            counter = page;

            Request.Builder rb = new Request.Builder()
                    .tag(FRAGMENT_TAG);

            if(mCat.getSelectedItemPosition() > 0 && mBrand.getSelectedItemPosition() > 0)
                rb.url(URLUtils.getProductsUrl(counter,
                        String.valueOf(catIdList.get(mCat.getSelectedItemPosition() - 1)),
                        brandsList.get(mBrand.getSelectedItemPosition())));
            else if(mCat.getSelectedItemPosition() > 0)
                rb.url(URLUtils.getProductsUrl(counter,
                        String.valueOf(catIdList.get(mCat.getSelectedItemPosition() - 1)), ""));
            else if(mBrand.getSelectedItemPosition() > 0)
                rb.url(URLUtils.getProductsUrl(counter, "",
                        brandsList.get(mBrand.getSelectedItemPosition())));
            else
                rb.url(URLUtils.getProductsUrl(counter, "", ""));

            isTaskRunning = true;
            OkHttpUtils.getInstance().newCall(rb.build()).enqueue(new GetProductsCallback());

            if(mLoadingToolbar != null)
                mLoadingToolbar.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Bundle args = new Bundle();
        args.putInt(Constants.KEY_ID, mAdapter.getProductId(position));
        args.putString(Constants.KEY_NAME, mAdapter.getProductName(position));
        args.putString(Constants.KEY_IMG, mAdapter.getImgUrl(position));

        ActivityOptionsCompat opts = ActivityOptionsCompat.makeScaleUpAnimation(view, 0, 0,
                view.getWidth(), view.getHeight());
        Intent i = new Intent(getActivity(), ProductActivity.class);
        i.putExtras(args);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getActivity().startActivity(i, opts.toBundle());
    }

	private void setSpinnerValue(Spinner sp, JSONArray jArr) {
		
		if(sp.getId() == R.id.brand_spinner) {
			brandsList.add(getResources().getString(R.string.text_brands_all));
			for(int i = 0; i < jArr.length(); i++) {
				try {
					brandsList.add(jArr.getString(i));
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
			
			sp.setAdapter(new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_dropdown_item, brandsList));
		} else if(sp.getId() == R.id.category_spinner) {
			catList.add(getResources().getString(R.string.text_categories_all));
			for(int i = 0; i < jArr.length(); i++) {
				JSONObject jCat;
				try {
					jCat = jArr.getJSONObject(i);
					catList.add(jCat.getString(Constants.KEY_NAME));
					catIdList.add(jCat.getInt(Constants.KEY_ID));
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
			
			sp.setAdapter(new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_dropdown_item, catList));
		}
	}
	
	private void setSpinnerValues(Spinner sp, String result) {
		SharedPreferences prefs = getActivity().getSharedPreferences("chimerarevo", Context.MODE_PRIVATE);
		
		if(sp.getId() == R.id.brand_spinner) {
			try {
				JSONObject jObj = new JSONObject(result);
				JSONArray jArr = jObj.getJSONArray(Constants.KEY_BRANDS);
				prefs.edit().putString("brands_array", result).apply();
				
				brandsList.add(getResources().getString(R.string.text_brands_all));
				for(int i = 0; i < jArr.length(); i++) {
					brandsList.add(jArr.getString(i));
				}

				sp.setAdapter(new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_dropdown_item, brandsList));
			} catch (JSONException e) {
				e.printStackTrace();
			}
		} else if(sp.getId() == R.id.category_spinner) {
			try {
				JSONObject jObj = new JSONObject(result);
				JSONArray jArr = jObj.getJSONArray(Constants.KEY_CAT);
				prefs.edit().putString("category_array", result).apply();
				
				catList.add(getResources().getString(R.string.text_categories_all));
				for(int i = 0; i < jArr.length(); i++) {
					JSONObject jCat = jArr.getJSONObject(i);
					catList.add(jCat.getString(Constants.KEY_NAME));
					catIdList.add(jCat.getInt(Constants.KEY_ID));
				}
				
				sp.setAdapter(new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_dropdown_item, catList));

                OkHttpUtils.getInstance().newCall(new Request.Builder()
                        .tag(FRAGMENT_TAG)
                        .url(Constants.BRANDS_URL).build())
                        .enqueue(new GetSpinnerCallback(mBrand));
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}

	}

    private class GetSpinnerCallback implements Callback {

        private Spinner sp;

        public GetSpinnerCallback(Spinner sp) {
            super();
            this.sp = sp;
        }

        @Override
        public void onFailure(Request request, final IOException e) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if(mLoadingToolbar != null)
                        mLoadingToolbar.setVisibility(View.GONE);

                    SnackbarUtils.showMultiShortSnackbar(getActivity(), e.getMessage())
                            .show(getActivity());
                }
            });
        }

        @Override
        public void onResponse(Response response) throws IOException {

            final String responseBody = response.body().string().trim();

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    setSpinnerValues(sp, responseBody);

                    if(mLoadingToolbar != null)
                        mLoadingToolbar.setVisibility(View.GONE);
                }
            });
        }
    }
	
	private void setItems(JSONArray jArray) {
		for(int i = 0; i < jArray.length(); i++) {
			try {
				final JSONObject jObject = jArray.getJSONObject(i);
				addItem(jObject);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
        mAdapter.notifyDataSetChanged();
	}
	
	private void addItem(final JSONObject jObject) throws JSONException {
		HashMap<String,String> map = new HashMap<>();
		map.put(Constants.KEY_ID, String.valueOf(jObject.getInt(Constants.KEY_ID)));
		map.put(Constants.KEY_NAME, jObject.getString(Constants.KEY_NAME));
		map.put(Constants.KEY_BRAND, jObject.getString(Constants.KEY_BRAND));
		map.put(Constants.KEY_IMG, jObject.getString(Constants.KEY_IMG));
		arrayList.add(map);
	}

    private class GetProductsCallback implements Callback {

        @Override
        public void onFailure(Request request, final IOException e) {
            isTaskRunning = false;

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if(mLoadingToolbar != null)
                        mLoadingToolbar.setVisibility(View.GONE);
                    mLoading.setVisibility(View.GONE);

                    SnackbarUtils.showMultiShortSnackbar(getActivity(), e.getMessage())
                            .show(getActivity());
                }
            });
        }

        @Override
        public void onResponse(Response response) throws IOException {
            if(response == null || !response.isSuccessful()) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if(mLoadingToolbar != null)
                            mLoadingToolbar.setVisibility(View.GONE);
                        mLoading.setVisibility(View.GONE);
                    }
                });

                isTaskRunning = false;

                return;
            }

            final String responseBody = response.body().string().trim();

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        setItems(JSONUtils.getJSONArray(responseBody, Constants.KEY_POSTS));
                    } catch (Exception e) {
                        SnackbarUtils.showShortSnackbar(getActivity(), responseBody)
                                .show(getActivity());
                        e.printStackTrace();
                        counter = page;
                    }

                    if(mLoadingToolbar != null)
                        mLoadingToolbar.setVisibility(View.GONE);
                    mLoading.setVisibility(View.GONE);
                }
            });


            if (counter < page) {
                counter++;

                Request.Builder rb = new Request.Builder()
                        .tag(FRAGMENT_TAG);

                if(mCat.getSelectedItemPosition() != 0 && mBrand.getSelectedItemPosition() != 0)
                    rb.url(URLUtils.getProductsUrl(counter,
                            String.valueOf(catIdList.get(mCat.getSelectedItemPosition() - 1)),
                                    brandsList.get(mBrand.getSelectedItemPosition())));
                else if(mCat.getSelectedItemPosition() != 0)
                    rb.url(URLUtils.getProductsUrl(counter,
                            String.valueOf(catIdList.get(mCat.getSelectedItemPosition() - 1)), ""));
                else if(mBrand.getSelectedItemPosition() != 0)
                    rb.url(URLUtils.getProductsUrl(counter, "",
                            brandsList.get(mBrand.getSelectedItemPosition())));
                else
                    rb.url(URLUtils.getProductsUrl(counter, "", ""));

                OkHttpUtils.getInstance().newCall(rb.build()).enqueue(this);
            } else {
                counter = 1;
                isTaskRunning = false;
            }
        }
    }
	
}
