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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v7.graphics.Palette;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import com.near.chimerarevo.R;
import com.near.chimerarevo.misc.Constants;
import com.near.chimerarevo.widget.ScrollControlViewPager;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.viewpagerindicator.CirclePageIndicator;

public class TutorialActivity extends Activity implements View.OnClickListener {

    private static final int NUM_PAGES = 6;

    private ScrollControlViewPager pager;
    private Button prev, next;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tutorial_layout);

        pager = (ScrollControlViewPager) findViewById(R.id.view_pager);
        pager.setPagingEnabled(false);

        CirclePageIndicator ind = (CirclePageIndicator) findViewById(R.id.indicator);
        pager.setAdapter(new TutorialAdapter(this));
        ind.setViewPager(pager);

        prev = (Button) findViewById(R.id.prev_button);
        next = (Button) findViewById(R.id.next_button);

        prev.setOnClickListener(this);
        next.setOnClickListener(this);

        prev.setVisibility(View.GONE);
    }

    @SuppressLint("NewApi")
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (hasFocus) {
                getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            }
        }
        super.onWindowFocusChanged(hasFocus);
    }

    @Override
    public void onBackPressed() {
        // do nothing
    }

    @Override
    public void onClick(View v) {
        int curView = pager.getCurrentItem();

        if(v.getId() == R.id.next_button) {
            if ((curView + 1) < NUM_PAGES) {
                curView++;
                prev.setVisibility(View.VISIBLE);
                if(curView == (NUM_PAGES - 1))
                    next.setText(R.string.text_begin);
                else
                    next.setText(R.string.text_next);
            } else {
                getSharedPreferences(Constants.PREFS_TAG, Context.MODE_PRIVATE).edit()
                        .putBoolean("hasTutorialShown", true).apply();
                finish();
                overridePendingTransition(R.anim.hold, R.anim.push_down_exit);
            }
        } else if(v.getId() == R.id.prev_button) {
            if ((curView - 1) >= 0) {
                curView--;
                if(curView == 0)
                    prev.setVisibility(View.GONE);
            }
        }

        if(curView < NUM_PAGES && curView >= 0)
            pager.setCurrentItem(curView);
    }

    private class TutorialAdapter extends PagerAdapter {

        private Context ctx;

        public TutorialAdapter(Context ctx) {
            this.ctx = ctx;
        }

        @Override
        public int getCount() {
            return NUM_PAGES;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            ImageView img = new ImageView(ctx);

            DisplayImageOptions options = new DisplayImageOptions.Builder()
                    .cacheOnDisk(true)
                    .cacheInMemory(false)
                    .showImageOnLoading(R.drawable.empty_cr)
                    .bitmapConfig(Bitmap.Config.RGB_565)
                    .imageScaleType(ImageScaleType.IN_SAMPLE_INT)
                    .delayBeforeLoading(0)
                    .build();

            Bitmap btm;

            switch (position) {
                case 0:
                    btm = ImageLoader.getInstance().loadImageSync("assets://tutorial_1.jpg", options);
                    break;
                case 1:
                    btm = ImageLoader.getInstance().loadImageSync("assets://tutorial_2.jpg", options);
                    break;
                case 2:
                    btm = ImageLoader.getInstance().loadImageSync("assets://tutorial_3.jpg", options);
                    break;
                case 3:
                    btm = ImageLoader.getInstance().loadImageSync("assets://tutorial_4.jpg", options);
                    break;
                case 4:
                    btm = ImageLoader.getInstance().loadImageSync("assets://tutorial_5.jpg", options);
                    break;
                case 5:
                    btm = ImageLoader.getInstance().loadImageSync("assets://tutorial_6.jpg", options);
                    break;
                default:
                    btm = ImageLoader.getInstance().loadImageSync("assets://tutorial_1.jpg", options);
            }

            img.setImageBitmap(btm);
            new Palette.Builder(btm).generate(
                new Palette.PaletteAsyncListener() {
                    @Override
                    public void onGenerated(Palette palette) {
                        findViewById(R.id.tutorial_btns_container).setBackgroundColor(palette.getDarkMutedColor(getResources().getColor(android.R.color.black)));
                    }
                }
            );

            img.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            img.setScaleType(ImageView.ScaleType.FIT_XY);
            img.setFitsSystemWindows(true);

            container.addView(img);

            return img;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }

    }

}
