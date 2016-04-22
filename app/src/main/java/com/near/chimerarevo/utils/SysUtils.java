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

package com.near.chimerarevo.utils;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

import com.near.chimerarevo.R;
import com.near.chimerarevo.misc.PostsListObject;
import com.near.chimerarevo.providers.PostsLRWidgetProvider;
import com.near.chimerarevo.providers.PostsListWidgetProvider;
import com.readystatesoftware.systembartint.SystemBarTintManager;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;

public class SysUtils {

    public static void toggleAppWidgets(Context ctx, boolean isEnabled) {
        PackageManager pm = ctx.getPackageManager();
        ComponentName wid1 = new ComponentName(ctx, PostsListWidgetProvider.class);
        ComponentName wid2 = new ComponentName(ctx, PostsLRWidgetProvider.class);

        if (isEnabled) {
                pm.setComponentEnabledSetting(wid1, PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP);
                pm.setComponentEnabledSetting(wid2, PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP);
        } else {
                pm.setComponentEnabledSetting(wid1, PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);
                pm.setComponentEnabledSetting(wid2, PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);
        }

    }

    public static void writeOfflineFile(Context ctx, ArrayList<String> mJson, String fileName) {
        if(mJson == null)
            return;

        try {
            new File(ctx.getCacheDir() + "/" + fileName).delete();
            OutputStream file = new FileOutputStream(ctx.getCacheDir() + "/" + fileName);
            ObjectOutput output = new ObjectOutputStream(new BufferedOutputStream(file));
            output.writeObject(new PostsListObject(mJson));
            output.close();
        } catch (IOException | NullPointerException e) {
            e.printStackTrace();
        }
    }

    public static Bitmap getBitmapFromAsset(Context context, String filePath) {
        AssetManager assetManager = context.getAssets();

        InputStream istr;
        Bitmap bitmap = null;
        try {
            istr = assetManager.open(filePath);
            bitmap = BitmapFactory.decodeStream(istr);
        } catch (IOException e) {
            // handle exception
        }

        return bitmap;
    }

    public static int getStatusBarHeight(Activity context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }


    public static void setInsets(Activity context, View view) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT)
            return;

        if(context.getResources().getConfiguration().orientation != Configuration.ORIENTATION_PORTRAIT)
            return;

        SystemBarTintManager tintManager = new SystemBarTintManager(context);
        SystemBarTintManager.SystemBarConfig config = tintManager.getConfig();
        view.setPadding(0, config.getPixelInsetTop(true), config.getPixelInsetRight(), config.getPixelInsetBottom());
    }

    public static void setTopInsets(Activity context, View view) {
        if(context.getResources().getConfiguration().orientation != Configuration.ORIENTATION_PORTRAIT)
            return;

        view.setPadding(0, context.getResources().getDimensionPixelSize(R.dimen.actionbar_height), 0, 0);
    }

    public static void setBottomInsets(Activity context, View view) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT)
            return;

        if(context.getResources().getConfiguration().orientation != Configuration.ORIENTATION_PORTRAIT
                || ViewConfiguration.get(context).hasPermanentMenuKey())
            return;

        SystemBarTintManager tintManager = new SystemBarTintManager(context);
        SystemBarTintManager.SystemBarConfig config = tintManager.getConfig();
        view.setPadding(0, 0, 0, config.getPixelInsetBottom());
    }

    public static void setTopMargin(Activity context, View view) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT)
            return;

        if(context.getResources().getConfiguration().orientation != Configuration.ORIENTATION_PORTRAIT)
            return;

        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        params.topMargin += context.getResources().getDimensionPixelSize(R.dimen.actionbar_height);
        view.setLayoutParams(params);
    }

    public static void setBottomMargin(Activity context, View view) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT)
            return;

        if(context.getResources().getConfiguration().orientation != Configuration.ORIENTATION_PORTRAIT
                || ViewConfiguration.get(context).hasPermanentMenuKey())
            return;

        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        SystemBarTintManager tintManager = new SystemBarTintManager(context);
        SystemBarTintManager.SystemBarConfig config = tintManager.getConfig();

        params.bottomMargin += config.getPixelInsetBottom();

        view.setLayoutParams(params);
    }

    public static void setTopStatusMargin(Activity context, View view) {
        if (Build.VERSION.SDK_INT != Build.VERSION_CODES.KITKAT)
            return;

        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        params.topMargin += getStatusBarHeight(context);
        view.setLayoutParams(params);
    }

}
