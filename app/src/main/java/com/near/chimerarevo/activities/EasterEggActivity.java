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
import android.widget.ImageView;
import android.view.View;
import android.view.View.OnClickListener;

import com.near.chimerarevo.R;
import com.near.chimerarevo.utils.SysUtils;
import com.nispok.snackbar.Snackbar;

public class EasterEggActivity extends BaseActivity implements View.OnClickListener {

    @Override
    public int getLayoutResource() {
        return R.layout.easter_egg_layout;
    }

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getToolbar().setNavigationIcon(getResources().getDrawable(R.drawable.ic_action_arrow_back));
        getToolbar().setNavigationOnClickListener(this);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            SysUtils.setTopStatusMargin(this, getToolbar());

        ImageView sp = (ImageView) findViewById(R.id.steve_pap);
        sp.setClickable(true);
        sp.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
                Snackbar alert = Snackbar.with(EasterEggActivity.this)
                        .text(getResources().getString(R.string.text_steve_watching));
                alert.show(EasterEggActivity.this);
			}
             });
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
