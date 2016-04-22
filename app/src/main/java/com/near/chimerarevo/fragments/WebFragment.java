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

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.near.chimerarevo.R;
import com.near.chimerarevo.activities.BaseActivity;
import com.near.chimerarevo.misc.Constants;

public class WebFragment extends Fragment {

    private WebView mWebView;
    private View mProgressContainer;
    private View mWebContainer;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.webview_layout, container, false);

        boolean isLandscapeLarge = false;

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if ((getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK)
                    >= Configuration.SCREENLAYOUT_SIZE_LARGE)
                isLandscapeLarge = true;
        } else
            isLandscapeLarge = false;

        if(!isLandscapeLarge)
            v.setPadding(0, getResources().getDimensionPixelSize(R.dimen.actionbar_height), 0, 0);

        mWebView = (WebView) v.findViewById(R.id.webview);
        mProgressContainer = v.findViewById(R.id.progressContainer);
        mWebContainer = v.findViewById(R.id.webViewContainer);

        return v;
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ((BaseActivity) getActivity()).getToolbar().setTitle(getResources().getString(R.string.app_name));

        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        mWebView.setHorizontalScrollBarEnabled(true);
        mWebView.setHorizontalScrollbarOverlay(true);
        mWebView.setVerticalScrollBarEnabled(true);
        mWebView.setVerticalScrollbarOverlay(true);

        mWebView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (Uri.parse(url).getHost().contains(Uri.parse(getArguments().getString(Constants.KEY_URL)).getHost()))
                    return false;

                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
                return true;
            }


            @Override
            public void onPageStarted (WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                mProgressContainer.setVisibility(View.VISIBLE);
                mWebContainer.setVisibility(View.GONE);
            }

            @Override
            public void onPageFinished (WebView view, String url) {
                super.onPageFinished(view, url);
                mProgressContainer.setVisibility(View.GONE);
                mWebContainer.setVisibility(View.VISIBLE);
            }

        });

        if(getArguments().containsKey(Constants.KEY_URL))
            mWebView.loadUrl(getArguments().getString(Constants.KEY_URL));
    }

    public boolean canGoBack() {
        return this.mWebView != null && this.mWebView.canGoBack();
    }

    public void goBack() {
        if(this.mWebView != null)
            this.mWebView.goBack();
    }

}
