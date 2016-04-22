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

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mikepenz.aboutlibraries.Libs;
import com.mikepenz.aboutlibraries.ui.LibsFragment;
import com.near.chimerarevo.R;
import com.near.chimerarevo.misc.Constants;

public class AboutContainerFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.about_container_layout, container, false);

        boolean isLandscapeLarge = false;

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if ((getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK)
                    >= Configuration.SCREENLAYOUT_SIZE_LARGE)
                isLandscapeLarge = true;
        } else
            isLandscapeLarge = false;

        if(!isLandscapeLarge)
            v.setPadding(0, getResources().getDimensionPixelSize(R.dimen.actionbar_height), 0, 0);

        ViewPager pager = (ViewPager) v.findViewById(R.id.view_pager);
        pager.setAdapter(new AboutPagerAdapter());

        return v;
    }

    private class AboutPagerAdapter extends FragmentPagerAdapter {
        private final static int PAGE_COUNT = 2;

        public AboutPagerAdapter() {
            super(getChildFragmentManager());
        }

        @Override
        public Fragment getItem(int position) {
            switch(position) {
                case 0:
                    return new Libs.Builder()
                            .withFields(R.string.class.getFields())
                            .withAboutAppName(getResources().getString(R.string.app_name))
                            .withAboutIconShown(true)
                            .withAboutDescription(Constants.APP_INFO)
                            .withLibraries(new String[]{
                                    "Jsoup", "ldialogs", "systembartint", "OkHttp"
                            })
                            .fragment();
                case 1:
                    return new DeveloperAboutFragment();
                default:
                    return new Fragment();
            }
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch(position) {
                case 0:
                    return Constants.APP_INFO_TITLE;
                case 1:
                    return Constants.APP_DEV_TITLE;
                default:
                    return "";
            }
        }

        @Override
        public int getCount() {
            return PAGE_COUNT;
        }

    }

}
