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

import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentTransaction;
import android.view.View;

import com.google.analytics.tracking.android.EasyTracker;
import com.near.chimerarevo.R;
import com.near.chimerarevo.fragments.ProductFragment;
import com.near.chimerarevo.utils.SysUtils;

public class ProductActivity extends BaseActivity implements View.OnClickListener {

    @Override
    public int getLayoutResource() {
        return R.layout.activity_main;
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

        getToolbar().setNavigationIcon(getResources().getDrawable(R.drawable.ic_action_arrow_back));
        getToolbar().setNavigationOnClickListener(this);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            SysUtils.setTopStatusMargin(this, findViewById(R.id.layout_container));

        if(savedInstanceState == null) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);

            ProductFragment frag = new ProductFragment();
            frag.setArguments(getIntent().getExtras());

            ft.replace(R.id.content_frame, frag);
            ft.commit();
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

}
