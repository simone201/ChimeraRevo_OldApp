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

import com.android.vending.billing.util.IabHelper;
import com.android.vending.billing.util.IabResult;
import com.android.vending.billing.util.Inventory;
import com.android.vending.billing.util.Purchase;
import com.google.analytics.tracking.android.EasyTracker;
import com.near.chimerarevo.R;
import com.near.chimerarevo.adapters.MenuAdapter;
import com.near.chimerarevo.fragments.AboutContainerFragment;
import com.near.chimerarevo.fragments.FavoritesFragment;
import com.near.chimerarevo.fragments.PostsRecyclerFragment;
import com.near.chimerarevo.fragments.ProductsListFragment;
import com.near.chimerarevo.fragments.SettingsFragment;
import com.near.chimerarevo.fragments.WebFragment;
import com.near.chimerarevo.misc.Constants;
import com.near.chimerarevo.services.NewsService;
import com.near.chimerarevo.utils.SysUtils;
import com.nispok.snackbar.Snackbar;
import com.nostra13.universalimageloader.core.ImageLoader;

import android.app.AlarmManager;
import android.app.FragmentManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Gravity;
import android.view.View;
import android.widget.ExpandableListView;

import uk.me.lewisdeane.ldialogs.BaseDialog;
import uk.me.lewisdeane.ldialogs.CustomDialog;

public class MainActivity extends BaseActivity implements ExpandableListView.OnChildClickListener,
        ExpandableListView.OnGroupClickListener, ExpandableListView.OnGroupExpandListener, View.OnClickListener,
        IabHelper.OnIabPurchaseFinishedListener, IabHelper.QueryInventoryFinishedListener {

    private IabHelper mHelper;
    private Bundle instanceState;

    private DrawerLayout mDrawerLayout;
    private ExpandableListView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;
    private MenuAdapter mMenuAdapter;
    private View mFooter, mShadow;

    private Fragment curFragment;

    private byte prevSelection = -1;
    private byte prevChildSelection = -1;
    private boolean isLandscapeLarge = false;
    private boolean isLicensed = false;

    @Override
    public int getLayoutResource() {
        return R.layout.drawer_main;
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

        instanceState = savedInstanceState;

        if(!getSharedPreferences(Constants.PREFS_TAG, Context.MODE_PRIVATE)
                .getBoolean("hasTutorialShown", false))
            startActivity(new Intent(this, TutorialActivity.class));

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if ((getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK)
                    >= Configuration.SCREENLAYOUT_SIZE_LARGE)
                isLandscapeLarge = true;
        } else
            isLandscapeLarge = false;

        mDrawerList = (ExpandableListView) findViewById(R.id.left_drawer_list);

        mFooter = LayoutInflater.from(this).inflate(R.layout.drawer_list_footer, mDrawerList, false);
        mDrawerList.addFooterView(mFooter);
        mShadow = findViewById(R.id.drop_shadow);

        if (!isLandscapeLarge) {
            mDrawerList.addHeaderView(LayoutInflater.from(this).inflate(R.layout.drawer_list_header, mDrawerList, false), null, false);
            mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
            mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, Gravity.START);
            mDrawerLayout.setStatusBarBackgroundColor(getResources().getColor(R.color.colorPrimaryDark));

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                SysUtils.setTopStatusMargin(this, mDrawerLayout);
        }

        mDrawerList.setGroupIndicator(null);
        mDrawerList.setOnGroupClickListener(this);
        mDrawerList.setOnChildClickListener(this);
        mDrawerList.setOnGroupExpandListener(this);

        mMenuAdapter = new MenuAdapter(this);
        mDrawerList.setAdapter(mMenuAdapter);

        if (!isLandscapeLarge) {
            mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, getToolbar(), R.string.drawer_open, R.string.drawer_close);
            mDrawerLayout.setDrawerListener(mDrawerToggle);
        }

        mFooter.findViewById(R.id.unlock_premium_btn).setVisibility(View.GONE);
        try {
            mHelper = new IabHelper(this, Constants.LICENSE_KEY);
            mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
                public void onIabSetupFinished(IabResult result) {
                    isLicensed = result.isSuccess();
                    if(isLicensed)
                        mHelper.queryInventoryAsync(MainActivity.this);
                    else
                        SysUtils.toggleAppWidgets(MainActivity.this, false);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (savedInstanceState != null) {
            prevSelection = savedInstanceState.getByte("prevSelection");

            mMenuAdapter.toggleSelection(0);
            if(prevSelection != 2)
                mMenuAdapter.toggleSelection(prevSelection);

            selectMenuItem(prevSelection);
        } else
            selectMenuItem(0);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if(ImageLoader.getInstance().isInited()) {
			ImageLoader.getInstance().clearDiskCache();
			ImageLoader.getInstance().clearMemoryCache();
		}
        if (mHelper != null)
            mHelper.dispose();

        if(PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean("news_search_pref", true)) {
            Intent intent = new Intent(getApplicationContext(), NewsService.class);
            PendingIntent pintent = PendingIntent.getService(getApplicationContext(), 0, intent, 0);
            AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

            alarm.cancel(pintent);

            long delay;
            int sel = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(this)
                    .getString("notification_delay_pref", "0"));

            switch(sel) {
                case 0:
                    delay = AlarmManager.INTERVAL_FIFTEEN_MINUTES;
                    break;
                case 1:
                    delay = AlarmManager.INTERVAL_HALF_HOUR;
                    break;
                case 2:
                    delay = AlarmManager.INTERVAL_HOUR;
                    break;
                case 3:
                    delay = 2 * AlarmManager.INTERVAL_HOUR;
                    break;
                case 4:
                    delay = 3 * AlarmManager.INTERVAL_HOUR;
                    break;
                case 5:
                    delay = 6 * AlarmManager.INTERVAL_HOUR;
                    break;
                case 6:
                    delay = AlarmManager.INTERVAL_HALF_DAY;
                    break;
                case 7:
                    delay = AlarmManager.INTERVAL_DAY;
                    break;
                default:
                    delay = AlarmManager.INTERVAL_HOUR;
                    break;
            }

            alarm.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.uptimeMillis(), delay, pintent);
        }
	}

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putByte("prevSelection", prevSelection);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if(mDrawerToggle != null)
            mDrawerToggle.syncState();
    }

	@Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if((newConfig.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK)
                >= Configuration.SCREENLAYOUT_SIZE_LARGE) {
            if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE)
                isLandscapeLarge = true;
            else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT)
                isLandscapeLarge = false;
        }
        if (mDrawerLayout != null)
        	mDrawerToggle.onConfigurationChanged(newConfig);
    }
	
    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
        menu.clear();
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
    
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
        	case R.id.action_search:
        		Intent i = new Intent(MainActivity.this, SearchActivity.class);
				i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(i);
                overridePendingTransition(R.anim.push_up_enter, R.anim.hold);
        		return true;
        	default:
        		return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
        if(prevSelection != -1 && prevSelection != 1)
            mMenuAdapter.toggleSelection(prevSelection);
        if(prevChildSelection == -1)
            mMenuAdapter.toggleSelection(groupPosition);
        prevSelection = (byte) groupPosition;

        if(prevChildSelection != -1)
            mMenuAdapter.toggleChildSelection(prevChildSelection);
        mMenuAdapter.toggleChildSelection(childPosition);
        mMenuAdapter.notifyDataSetChanged();

        if(mDrawerLayout != null)
            mDrawerLayout.closeDrawers();

        if(mShadow != null)
            mShadow.setVisibility(View.VISIBLE);
        else
            getToolbar().setElevation(
                    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics()));
        selectCategoryItem(childPosition);

        return true;
    }

    @Override
    public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
        int len = mMenuAdapter.getGroupCount();
        for (int i = 0; i < len; i++) {
            if (i != groupPosition) {
                if(mDrawerList.isGroupExpanded(i))
                    mDrawerList.collapseGroup(i);
            }
        }

        if(prevSelection != -1 && groupPosition != 1)
            mMenuAdapter.toggleSelection(prevSelection);
        if(groupPosition != 1)
            mMenuAdapter.toggleSelection(groupPosition);
        mMenuAdapter.notifyDataSetChanged();

        prevChildSelection = -1;
        if(groupPosition == 1)
            return false;

        if(mDrawerLayout != null)
            mDrawerLayout.closeDrawers();

        if(mShadow != null)
            mShadow.setVisibility(View.VISIBLE);
        else
            getToolbar().setElevation(
                    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics()));
        selectMenuItem(groupPosition);

        return true;
    }

    @Override
    public void onGroupExpand(int groupPosition) {
        mMenuAdapter.resetChildCheck();
        mMenuAdapter.notifyDataSetChanged();
    }

    @Override
    public void onClick(View view) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

        switch(view.getId()) {
            case R.id.unlock_premium_btn:
                CustomDialog.Builder builder = new CustomDialog.Builder(this,
                        getResources().getString(R.string.action_unlock_premium),
                        getResources().getString(R.string.text_continue));
                builder.content(getResources().getString(R.string.text_premium_list));
                builder.contentTextSize(15);
                builder.negativeText(getResources().getString(R.string.text_decline));
                builder.titleAlignment(BaseDialog.Alignment.LEFT);
                builder.titleColorRes(R.color.red_light);
                builder.positiveColorRes(R.color.green_light);

                final CustomDialog dialog = builder.build();
                dialog.setClickListener(new CustomDialog.ClickListener() {
                    @Override
                    public void onConfirmClick() {
                        mHelper.launchPurchaseFlow(MainActivity.this, Constants.PREMIUM_ITEM_SKU, 10001, MainActivity.this);
                    }

                    @Override
                    public void onCancelClick() {
                        // do nothing
                    }
                });

                dialog.show();
                break;
            case R.id.favorite_btn:
                if(mShadow != null)
                    mShadow.setVisibility(View.VISIBLE);
                else
                    getToolbar().setElevation(
                            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics()));
                curFragment = new FavoritesFragment();
                break;
            case R.id.settings_btn:
                if(mShadow != null)
                    mShadow.setVisibility(View.VISIBLE);
                else
                    getToolbar().setElevation(
                            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics()));
                curFragment = new SettingsFragment();
                break;
            case R.id.about_btn:
                if(mShadow != null)
                    mShadow.setVisibility(View.GONE);
                else
                    getToolbar().setElevation(0);
                curFragment = new AboutContainerFragment();
                break;
            default:
                break;
        }

        if(view.getId() != R.id.unlock_premium_btn) {
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            ft.replace(R.id.content_frame, curFragment);
            ft.commit();
            setToolbarStatusColor(-1);
        }

        if(mDrawerLayout != null)
            mDrawerLayout.closeDrawers();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (!mHelper.handleActivityResult(requestCode, resultCode, data))
            super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onIabPurchaseFinished(IabResult result, Purchase info) {
        if(result.isSuccess())
            mHelper.queryInventoryAsync(this);
        else
            Snackbar.with(this).text(result.getMessage()).show(this);
    }

    @Override
    public void onQueryInventoryFinished(IabResult result, Inventory inv) {
        if(result.isSuccess()) {
            if (inv.hasPurchase(Constants.PREMIUM_ITEM_SKU)) {
                mFooter.findViewById(R.id.unlock_premium_btn).setVisibility(View.GONE);
                SysUtils.toggleAppWidgets(MainActivity.this, true);
            } else {
                mFooter.findViewById(R.id.unlock_premium_btn).setVisibility(View.VISIBLE);
                SysUtils.toggleAppWidgets(MainActivity.this, false);
            }
        } else if(result.getResponse() == IabHelper.BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE)
            SysUtils.toggleAppWidgets(MainActivity.this, false);
        else
            Snackbar.with(this).text(result.getMessage()).show(this);
    }

    @Override
    public void onBackPressed() {
        if(curFragment instanceof WebFragment) {
            if(mDrawerLayout == null || !mDrawerLayout.isDrawerOpen(Gravity.START)) {
                WebFragment web = (WebFragment) curFragment;
                if (web.canGoBack()) {
                    web.goBack();
                    return;
                }
            }
        }

        if(mDrawerLayout != null) {
            if(mDrawerLayout.isDrawerOpen(Gravity.START))
                mDrawerLayout.closeDrawers();
            else
                super.onBackPressed();
        } else
            super.onBackPressed();
    }

    public View getDropShadow() {
        return mShadow;
    }

    private void selectMenuItem(int position) {
        if(prevSelection == position && position != 0)
            return;

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);

        Bundle args = new Bundle();
        if(position == 0 || position == 2 || position == 3)
            curFragment = new PostsRecyclerFragment();

        FragmentManager fm = getFragmentManager();
        for(int i = 0; i < fm.getBackStackEntryCount(); ++i) {
            fm.popBackStack();
        }

        prevSelection = (byte) position;

        switch(position) {
            case 2:
                args.putString(Constants.KEY_TYPE, Constants.RECENSIONI);
                break;
            case 3:
                args.putString(Constants.KEY_TYPE, Constants.VIDEO);
                break;
            case 4:
                curFragment = new ProductsListFragment();
                break;
        }

        if(position != 1) {
            if(position != 4)
                curFragment.setArguments(args);
            ft.replace(R.id.content_frame, curFragment);
        }
        ft.commit();
        setToolbarStatusColor(-1);
    }

	private void selectCategoryItem(int position) {
		if(prevChildSelection == position)
			return;
		
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        
		Bundle args = new Bundle();
        curFragment = new PostsRecyclerFragment();
		
		FragmentManager fm = getFragmentManager();
		for(int i = 0; i < fm.getBackStackEntryCount(); ++i) {
		    fm.popBackStack();
		}

        prevChildSelection = (byte) position;
		
		switch(position) {
			case 0:
				args.putString(Constants.KEY_CAT, Constants.ANDROID);
				break;
			case 1:
				args.putString(Constants.KEY_CAT, Constants.APPLE);
				break;
			case 2:
				args.putString(Constants.KEY_CAT, Constants.GIOCHI);
				break;
			case 3:
				args.putString(Constants.KEY_CAT, Constants.HARDWARE);
				break;
			case 4:
				args.putString(Constants.KEY_CAT, Constants.INTERNET);
				break;
			case 5:
				args.putString(Constants.KEY_CAT, Constants.LINUX);
				break;
			case 6:
				args.putString(Constants.KEY_CAT, Constants.SMARTPHONE);
				break;
			case 7:
				args.putString(Constants.KEY_CAT, Constants.TABLET);
				break;
			case 8:
				args.putString(Constants.KEY_CAT, Constants.WINDOWS);
				break;
			default:
				break;
		}

        curFragment.setArguments(args);
        ft.replace(R.id.content_frame, curFragment);
        ft.commit();
        setToolbarStatusColor(position);
	}

    private void setToolbarStatusColor(int childPosition) {
        int colorPrimary, colorDark;

        if(childPosition == -1) {
            colorPrimary = getResources().getColor(R.color.colorPrimary);
            colorDark = getResources().getColor(R.color.colorPrimaryDark);
        } else {
            int[] colorsPrimary = getResources().getIntArray(R.array.categories_colors);
            int[] colorsDark = getResources().getIntArray(R.array.categories_colors_dark);

            colorPrimary = colorsPrimary[childPosition];
            colorDark = colorsDark[childPosition];
        }

        getToolbar().setBackgroundColor(colorPrimary);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if(mDrawerLayout != null)
                mDrawerLayout.setStatusBarBackgroundColor(colorDark);
            else
                getWindow().setStatusBarColor(colorDark);
        }
    }

}
