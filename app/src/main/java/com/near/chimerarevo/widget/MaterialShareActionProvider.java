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

package com.near.chimerarevo.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.ShareActionProvider;
import android.view.View;

import com.near.chimerarevo.R;

import java.lang.reflect.Method;

public class MaterialShareActionProvider extends ShareActionProvider {

    private final Context mContext;

    public MaterialShareActionProvider(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    public View onCreateActionView() {

        View view = super.onCreateActionView();

        try {
            Drawable icon = mContext.getResources().getDrawable(R.drawable.ic_action_share);
            Method method = view.getClass().getMethod("setExpandActivityOverflowButtonDrawable", Drawable.class);
            method.invoke(view, icon);
        }
        catch (Exception e) { }

        return view;
    }

}
