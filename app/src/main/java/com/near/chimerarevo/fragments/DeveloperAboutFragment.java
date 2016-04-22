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
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.near.chimerarevo.R;
import com.near.chimerarevo.activities.BaseActivity;
import com.near.chimerarevo.misc.Constants;
import com.near.chimerarevo.activities.EasterEggActivity;

public class DeveloperAboutFragment extends Fragment {

	private byte easter_egg = 0;
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.dev_about_layout, container, false);

        ((BaseActivity) getActivity()).getToolbar().setTitle(getResources().getString(R.string.action_about));
		
		((TextView) v.findViewById(R.id.dev_website)).setText(Html.fromHtml(Constants.DEV_WEBSITE));
		((TextView) v.findViewById(R.id.dev_website)).setLinksClickable(true);
		((TextView) v.findViewById(R.id.dev_website)).setMovementMethod(LinkMovementMethod.getInstance());
		
		((TextView) v.findViewById(R.id.dev_copy)).setText(Html.fromHtml(Constants.DEV_COPY));
		((TextView) v.findViewById(R.id.dev_copy)).setLinksClickable(true);
		((TextView) v.findViewById(R.id.dev_copy)).setMovementMethod(LinkMovementMethod.getInstance());
		
		v.findViewById(R.id.dev_img).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				easter_egg++;
				if(easter_egg == 5) {
					easter_egg = 0;
                    ActivityOptionsCompat opts = ActivityOptionsCompat.makeScaleUpAnimation(v, 0, 0,
                            v.getWidth(), v.getHeight());
					Intent i = new Intent(getActivity(), EasterEggActivity.class);
					i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					getActivity().startActivity(i, opts.toBundle());
				}
			}
		});
		
		v.findViewById(R.id.gplus).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent(Intent.ACTION_VIEW);
				i.setData(Uri.parse(Constants.DEV_GP));
				startActivity(i);
			}
		});
		
		v.findViewById(R.id.facebook).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent(Intent.ACTION_VIEW);
				i.setData(Uri.parse(Constants.DEV_FB));
				startActivity(i);
			}
		});
		
		v.findViewById(R.id.twitter).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent(Intent.ACTION_VIEW);
				i.setData(Uri.parse(Constants.DEV_TW));
				startActivity(i);
			}
		});
		
		String version = getResources().getString(R.string.about_version_title) + ": ";
	    try {
	    	version += getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0).versionName;
	    } catch (NameNotFoundException e) {
	    	e.printStackTrace();
	    	version += getResources().getString(R.string.error_notavailable);
	    }
	    ((TextView) v.findViewById(R.id.app_version)).setText(version);
		
		return v;
	}

}
