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
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.melnykov.fab.FloatingActionButton;
import com.near.chimerarevo.R;
import com.near.chimerarevo.adapters.CommentsAdapter;
import com.near.chimerarevo.misc.Constants;
import com.near.chimerarevo.utils.JSONUtils;
import com.near.chimerarevo.utils.OkHttpUtils;
import com.near.chimerarevo.utils.ProgressDialogUtils;
import com.near.chimerarevo.utils.SnackbarUtils;
import com.near.chimerarevo.utils.URLUtils;
import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.listeners.EventListener;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.http.SslError;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AnimationUtils;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import uk.me.lewisdeane.ldialogs.BaseDialog;
import uk.me.lewisdeane.ldialogs.CustomDialog;

public class CommentsFragment extends ListFragment implements OnClickListener, EventListener {

    private static final String FRAGMENT_TAG = "CommentsFragment";

    private final Object attachingActivityLock = new Object();
    private boolean syncVariable = false;

    private Handler mHandler = new Handler();

    private ProgressDialog mDialog;
	
	private CommentsAdapter mAdapter;
	
	private ArrayList<HashMap<String,String>> arrayList;

    private FloatingActionButton mFab;
    private TextView mEmptyText;
    private View mProgressContainer;
    private View mListContainer;

	private String thread_id, access_token;
	private int parentId = -1;
    private boolean isDialogOpen = false;
    private boolean mListShown = false;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        synchronized(attachingActivityLock) {
            syncVariable = true;
            attachingActivityLock.notifyAll();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.comment_list_layout, container, false);

        if(getArguments().containsKey("isLandscapeLarge") && !getArguments().getBoolean("isLandscapeLarge"))
            v.setPadding(0, getResources().getDimensionPixelSize(R.dimen.actionbar_height), 0, 0);

        mFab = (FloatingActionButton) v.findViewById(R.id.floating_action);
        mProgressContainer = v.findViewById(R.id.progressContainer);
        mListContainer = v.findViewById(R.id.listContainer);

        mEmptyText = (TextView) v.findViewById(android.R.id.empty);
        mEmptyText.setText(R.string.text_nocomments);

        return v;
    }

	@Override
    public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		arrayList = new ArrayList<>();
		mAdapter = new CommentsAdapter(arrayList);
        setListAdapter(mAdapter);

        mFab.attachToListView(getListView());
        mFab.setOnClickListener(this);

        if(getArguments().containsKey(Constants.KEY_URL))
        	performUpdate();
        else {
            setListShown(true);
            mFab.setVisibility(View.GONE);
        }
	}
	
	@Override
    public void onListItemClick(ListView l, View v, int position, long id) {
		parentId = mAdapter.getCommentId(position);
		if(access_token == null)
			loadDisqusOAuth();
		else
			createCommentDialog(parentId);
	}

    @Override
    public void onClick(View view) {
        if(!isDialogOpen) {
            mFab.hide();
            parentId = -1;
            if(access_token == null)
                loadDisqusOAuth();
            else
                createCommentDialog(parentId);
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

    public void setListShown(boolean shown, boolean animate){
        if (mListShown == shown)
            return;

        mListShown = shown;
        if (shown) {
            if (animate) {
                try {
                    mProgressContainer.startAnimation(AnimationUtils.loadAnimation(
                            getActivity(), android.R.anim.fade_out));
                    mListContainer.startAnimation(AnimationUtils.loadAnimation(
                            getActivity(), android.R.anim.fade_in));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            mProgressContainer.setVisibility(View.GONE);
            mListContainer.setVisibility(View.VISIBLE);
        } else {
            if (animate) {
                try {
                    mProgressContainer.startAnimation(AnimationUtils.loadAnimation(
                            getActivity(), android.R.anim.fade_in));
                    mListContainer.startAnimation(AnimationUtils.loadAnimation(
                            getActivity(), android.R.anim.fade_out));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            mProgressContainer.setVisibility(View.VISIBLE);
            mListContainer.setVisibility(View.INVISIBLE);
        }
    }

    public void setListShown(boolean shown){
        setListShown(shown, true);
    }

	public void performUpdate() {
        arrayList.clear();

        synchronized (attachingActivityLock) {
            while(!syncVariable){
                try {
                    attachingActivityLock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        Request request = new Request.Builder()
                .url(URLUtils.getThreadIdUrl(getArguments().getString(Constants.KEY_URL)))
                .tag(FRAGMENT_TAG)
                .build();
        OkHttpUtils.getInstance().newCall(request).enqueue(new ThreadIdCallback());
	}
	
	@SuppressLint("SetJavaScriptEnabled")
	private void loadDisqusOAuth() {
		if(getActivity().getSharedPreferences(Constants.PREFS_TAG, Context.MODE_PRIVATE)
                .getString(Constants.KEY_REFRESH_TOKEN, "").length() > 1) {

            RequestBody formBody = new FormEncodingBuilder()
                    .add("grant_type", "refresh_token")
                    .add("client_id", Constants.DISQUS_API_KEY)
                    .add("client_secret", Constants.DISQUS_API_SECRET)
                    .add("refresh_token", getActivity().getSharedPreferences(Constants.PREFS_TAG,
                            Context.MODE_PRIVATE).getString(Constants.KEY_REFRESH_TOKEN, ""))
                    .build();

            Request request = new Request.Builder()
                    .url(Constants.DISQUS_TOKEN_URL)
                    .post(formBody)
                    .tag(FRAGMENT_TAG)
                    .build();

            if(mDialog == null)
                mDialog = ProgressDialogUtils.getInstance(getActivity(), R.string.text_login);
            else
                mDialog = ProgressDialogUtils.modifyInstance(mDialog, R.string.text_login);

            mDialog.show();
            OkHttpUtils.getInstance().newCall(request).enqueue(new PostAccessTokenCallback());

			return;
		}
		
		final Dialog dialog = new Dialog(getActivity());
		
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setContentView(R.layout.webview_layout);
		dialog.setCancelable(true);
		
		dialog.setOnCancelListener(new OnCancelListener() {
			@Override
			public void onCancel(DialogInterface arg0) {
                isDialogOpen = false;
                mFab.show();
			}
		});
		dialog.setOnDismissListener(new OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface arg0) {
                isDialogOpen = false;
                mFab.show();
			}
		});

        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT)
            CookieManager.getInstance().removeAllCookies(null);
        else {
            CookieSyncManager.createInstance(getActivity());
            CookieManager.getInstance().removeAllCookie();
        }
		
		WebView wv = (WebView) dialog.findViewById(R.id.webview);
		wv.getSettings().setJavaScriptEnabled(true);
		wv.getSettings().setSaveFormData(false);
		wv.setWebViewClient(new WebViewClient() {
			
			@Override
			public void onPageFinished(WebView view, String url) {
				super.onPageFinished(view, url);
                dialog.findViewById(R.id.progressContainer).setVisibility(View.GONE);
                dialog.findViewById(R.id.webViewContainer).setVisibility(View.VISIBLE);
			}
			
			@Override
			public void onPageStarted (WebView view, String url, Bitmap favicon) {
				super.onPageStarted(view, url, favicon);
                dialog.findViewById(R.id.progressContainer).setVisibility(View.VISIBLE);
                dialog.findViewById(R.id.webViewContainer).setVisibility(View.GONE);
			}
			
			@Override
		    public boolean shouldOverrideUrlLoading(WebView view, String url) {
				boolean state = super.shouldOverrideUrlLoading(view, url);
				if(url.contains(Constants.SITE_URL)) {
					String code = url.split("code=")[1];

                    RequestBody formBody = new FormEncodingBuilder()
                            .add("grant_type", "authorization_code")
                            .add("client_id", Constants.DISQUS_API_KEY)
                            .add("client_secret", Constants.DISQUS_API_SECRET)
                            .add("redirect_uri", Constants.SITE_URL)
                            .add("code", code)
                            .build();

                    Request request = new Request.Builder()
                            .url(Constants.DISQUS_TOKEN_URL)
                            .post(formBody)
                            .tag(FRAGMENT_TAG)
                            .build();

                    if(mDialog == null)
                        mDialog = ProgressDialogUtils.getInstance(getActivity(), R.string.text_login);
                    else
                        mDialog = ProgressDialogUtils.modifyInstance(mDialog, R.string.text_login);

                    dialog.dismiss();
                    mDialog.show();

                    OkHttpUtils.getInstance().newCall(request).enqueue(new PostAccessTokenCallback());
				}
				return state;
			}
			
	        @Override
	        public void onReceivedSslError(WebView view, @NonNull SslErrorHandler handler, SslError error) {
	            handler.proceed();
	        }
			
		});
		wv.loadUrl(URLUtils.getDisqusAuthUrl());

        isDialogOpen = true;
        mFab.hide();
		dialog.show();
	}
	
	private void createCommentDialog(final int parent) {
        CustomDialog.Builder builder;

        if(parentId == -1)
            builder = new CustomDialog.Builder(getActivity(),
                    getResources().getString(R.string.text_addcomment),
                    getResources().getString(R.string.text_sendcomment));
        else
            builder = new CustomDialog.Builder(getActivity(),
                    getResources().getString(R.string.text_replycomment),
                    getResources().getString(R.string.text_sendcomment));

        builder.positiveColorRes(android.R.color.holo_blue_bright);
        builder.titleColorRes(android.R.color.holo_red_light);
        builder.titleAlignment(BaseDialog.Alignment.CENTER);
        builder.negativeText(getResources().getString(R.string.text_cancel));
        builder.negativeColorRes(android.R.color.holo_red_dark);

		final CustomDialog dialog = builder.build();
		dialog.setCustomView(LayoutInflater.from(getActivity())
                .inflate(R.layout.dialog_comment_layout, null, false));
		dialog.setCancelable(false);

        dialog.setClickListener(new CustomDialog.ClickListener() {
            @Override
            public void onConfirmClick() {
                EditText text = (EditText) dialog.findViewById(R.id.comment_edittext);

                isDialogOpen = false;

                if(checkEditText(text, getResources())) {
                    SnackbarUtils.showMultiShortSnackbar(getActivity(),
                            getResources().getString(R.string.text_textnotempty),
                            CommentsFragment.this)
                            .dismissOnActionClicked(true)
                            .actionLabel(getResources().getString(R.string.text_close))
                            .actionColor(getResources().getColor(android.R.color.holo_red_dark))
                            .show(getActivity());

                    return;
                }

                String commentText = text.getText().toString() + "\n\n"
                        + getResources().getString(R.string.text_sentfromapp);

                FormEncodingBuilder feb = new FormEncodingBuilder()
                        .add("access_token", access_token)
                        .add("api_key", Constants.DISQUS_API_KEY)
                        .add("api_secret", Constants.DISQUS_API_SECRET)
                        .add("message", commentText)
                        .add("thread", thread_id);

                if(parent != -1)
                    feb.add("parent", String.valueOf(parent));

                Request request = new Request.Builder()
                        .url(Constants.DISQUS_POST_COMMENT)
                        .post(feb.build())
                        .tag(FRAGMENT_TAG)
                        .build();

                if(mDialog == null)
                    mDialog = ProgressDialogUtils.getInstance(getActivity(), R.string.text_sending_comment);
                else
                    mDialog = ProgressDialogUtils.modifyInstance(mDialog, R.string.text_sending_comment);

                mDialog.show();

                OkHttpUtils.getInstance().newCall(request).enqueue(new PostCommentCallback());
            }

            @Override
            public void onCancelClick() {
                isDialogOpen = false;
                mFab.show();
            }
        });

        isDialogOpen = true;
        mFab.hide();
		dialog.show();
	}
	
	private static boolean checkEditText(EditText ed, Resources res) {
		if(ed.getText().toString().trim().equals("")) {
			ed.setError(res.getString(R.string.error_field_required));
			ed.requestFocus();
			return true;
		} else {
			ed.setError(null);
			return false;
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
		map.put(Constants.KEY_ID, jObject.getString(Constants.KEY_ID));
		map.put(Constants.KEY_NAME, jObject.getJSONObject(Constants.KEY_AUTHOR).getString(Constants.KEY_NAME));
		map.put(Constants.KEY_DATE, jObject.getString(Constants.KEY_CREATEDAT).replaceAll("T", " "));
		map.put(Constants.KEY_MESSAGE, jObject.getString(Constants.KEY_MESSAGE));
		map.put(Constants.KEY_IMG, jObject.getJSONObject(Constants.KEY_AUTHOR)
                .getJSONObject(Constants.KEY_AVATAR).getString(Constants.KEY_URL));
		arrayList.add(map);
	}

    private class ThreadIdCallback implements Callback {

        @Override
        public void onFailure(Request request, final IOException e) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    SnackbarUtils.showShortSnackbar(getActivity(),
                            e.getMessage(), CommentsFragment.this).show(getActivity());
                }
            });
        }

        @Override
        public void onResponse(final Response response) throws IOException {
            if(response.code() == 200)
                new CheckThreadIdTask().execute(response.body().string());
            else
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        SnackbarUtils.showShortSnackbar(getActivity(),
                                response.message(), CommentsFragment.this).show(getActivity());
                    }
                });
        }
    }

    private class CheckThreadIdTask extends AsyncTask<String,Void,Void> {

        @Override
        protected Void doInBackground(String... params) {

            if(!isOnline())
                return null;

            try {
                JSONArray jArray = JSONUtils.getJSONArray(params[0], Constants.KEY_RESPONSE);

                for(int i = 0; i < jArray.length(); i++) {
                    JSONObject obj = jArray.getJSONObject(i);

                    try {
                        URL curUrl = new URL(obj.getString(Constants.DISQUS_LINK_TAG));
                        HttpURLConnection ucon = (HttpURLConnection) curUrl.openConnection();
                        ucon.setInstanceFollowRedirects(true);
                        String secondUrl = ucon.getHeaderField("Location");

                        if(secondUrl == null)
                            secondUrl = obj.getString(Constants.DISQUS_LINK_TAG);

                        if(secondUrl.equals(getArguments().getString(Constants.KEY_URL))) {
                            thread_id = obj.getString(Constants.KEY_ID);
                        }
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }

                }

            } catch (JSONException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if(thread_id == null) {
                mEmptyText.setText(R.string.error_disqus);
                mFab.setVisibility(View.GONE);
                setListShown(true);
            } else {
                Request request = new Request.Builder()
                        .url(URLUtils.getCommentsUrl(thread_id))
                        .tag(FRAGMENT_TAG)
                        .build();
                OkHttpUtils.getInstance().newCall(request).enqueue(new GetCommentsCallback());
            }
        }

        public boolean isOnline() {
            try {
                ConnectivityManager cm =
                        (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo netInfo = cm.getActiveNetworkInfo();
                return netInfo != null && netInfo.isConnectedOrConnecting();
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

    }

    private class PostAccessTokenCallback implements Callback {

        @Override
        public void onFailure(Request request, final IOException e) {
            if(mDialog.isShowing())
                mDialog.dismiss();

            getActivity().getSharedPreferences(Constants.PREFS_TAG, Context.MODE_PRIVATE)
                    .edit().remove(Constants.KEY_REFRESH_TOKEN).commit();

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    SnackbarUtils.showShortSnackbar(getActivity(),
                            e.getMessage(), CommentsFragment.this).show(getActivity());
                    loadDisqusOAuth();
                }
            });
        }

        @Override
        public void onResponse(final Response response) throws IOException {
            if(mDialog.isShowing())
                mDialog.dismiss();

            if(response.code() != 200) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        SnackbarUtils.showShortSnackbar(getActivity(),
                                response.message(), CommentsFragment.this).show(getActivity());
                    }
                });

                return;
            }

            try {
                JSONObject jObject = new JSONObject(response.body().string());
                access_token = jObject.getString(Constants.KEY_ACCESS_TOKEN);
                getActivity().getSharedPreferences(Constants.PREFS_TAG, Context.MODE_PRIVATE).edit()
                        .putString(Constants.KEY_REFRESH_TOKEN, jObject.getString(Constants.KEY_REFRESH_TOKEN)).apply();

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        createCommentDialog(parentId);
                    }
                });
            } catch (JSONException e) {
                e.printStackTrace();
                getActivity().getSharedPreferences(Constants.PREFS_TAG, Context.MODE_PRIVATE).edit()
                        .remove(Constants.KEY_REFRESH_TOKEN).apply();

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        loadDisqusOAuth();
                    }
                });
            }
        }
    }

    private class GetCommentsCallback implements Callback {

        @Override
        public void onFailure(Request request, final IOException e) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    SnackbarUtils.showShortSnackbar(getActivity(),
                            e.getMessage(), CommentsFragment.this).show(getActivity());
                }
            });
        }

        @Override
        public void onResponse(final Response response) throws IOException {
            if(response.code() != 200) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        SnackbarUtils.showShortSnackbar(getActivity(),
                                response.message(), CommentsFragment.this).show(getActivity());
                    }
                });

                return;
            }

            final String body = response.body().string();

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        setItems(JSONUtils.getJSONArray(body, Constants.KEY_RESPONSE));
                        setListShown(true);
                    } catch (Exception e) {
                        e.printStackTrace();
                        setListShown(true);
                    }
                }
            });
        }
    }

    private class PostCommentCallback implements Callback {

        @Override
        public void onFailure(Request request, final IOException e) {
            if(mDialog.isShowing())
                mDialog.dismiss();

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    SnackbarUtils.showShortSnackbar(getActivity(),
                            e.getMessage(), CommentsFragment.this).show(getActivity());
                }
            });
        }

        @Override
        public void onResponse(Response response) throws IOException {
            if(mDialog.isShowing())
                mDialog.dismiss();

            if(response == null || !response.isSuccessful()) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        SnackbarUtils.showShortSnackbar(getActivity(),
                                getResources().getString(R.string.error_occurred),
                                CommentsFragment.this).show(getActivity());
                    }
                });
            } else {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        SnackbarUtils.showShortSnackbar(getActivity(),
                                getResources().getString(R.string.text_commentsent),
                                CommentsFragment.this).show(getActivity());
                    }
                });

                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        performUpdate();
                    }
                }, 4000);
            }
        }
    }

}
