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
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayer.Provider;
import com.google.android.youtube.player.YouTubePlayerView;
import com.near.chimerarevo.R;
import com.near.chimerarevo.misc.Constants;
import com.near.chimerarevo.receivers.ScreenReceiver;

public class YoutubeActivity extends YouTubeFailureRecoveryActivity {
	
	private YouTubePlayer player;
	private BroadcastReceiver mReceiver;
	private YouTubePlayerView playerView;
	
	@Override
    public void onStart() {
    	super.onStart();
        if(PreferenceManager.getDefaultSharedPreferences(this).getBoolean("analytics_pref", true))
            EasyTracker.getInstance(this).activityStart(this);
    	
    	final IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        mReceiver = new ScreenReceiver();
        registerReceiver(mReceiver, filter);
    }
    
    @Override
    public void onStop() {
    	super.onStop();
        if(PreferenceManager.getDefaultSharedPreferences(this).getBoolean("analytics_pref", true))
            EasyTracker.getInstance(this).activityStop(this);
    	unregisterReceiver(mReceiver);
    }
	
    @Override
    protected void onPause() {
        if (ScreenReceiver.wasScreenOn) {
        	if(player != null && player.isPlaying())
        		player.pause();
        }
        super.onPause();
    }

    @SuppressLint("NewApi")
	@Override
    public void onWindowFocusChanged(boolean hasFocus) {
    	if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {  
    		if (hasFocus) {
    			getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    		}
    	} else {
            if (hasFocus) {
                getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN);
            }
        }
    	super.onWindowFocusChanged(hasFocus);
    }
    
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.youtube_layout);
        
        playerView = (YouTubePlayerView) findViewById(R.id.youtube_video);
        playerView.initialize(Constants.YOUTUBE_API_TOKEN, this);
	}

	@Override
	public void onInitializationFailure(Provider provider, YouTubeInitializationResult error) {
        error.getErrorDialog(this, 0).show();
	}

	@Override
	public void onInitializationSuccess(Provider provider, YouTubePlayer player, boolean wasRestored) {
		this.player = player;
		player.setFullscreen(true);
		
	    if (!wasRestored)
			player.cueVideo(getIntent().getExtras().getString(Constants.KEY_VIDEO_URL));
	}
	
	@Override
	protected YouTubePlayer.Provider getYouTubePlayerProvider() {
		return playerView;
	}
	
}
