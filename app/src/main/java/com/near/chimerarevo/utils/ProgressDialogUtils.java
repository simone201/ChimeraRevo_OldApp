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

import android.app.ProgressDialog;
import android.content.Context;

public class ProgressDialogUtils {

    public static ProgressDialog getInstance(Context ctx, int dialogText) {
        return getInstance(ctx, dialogText, false);
    }

    public static ProgressDialog getInstance(Context ctx, int dialogText, boolean cancelable) {
        ProgressDialog mProgressDialog = new ProgressDialog(ctx);
        mProgressDialog.setMessage(ctx.getResources().getString(dialogText));
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setCancelable(cancelable);

        return mProgressDialog;
    }

    public static ProgressDialog modifyInstance(ProgressDialog progressDialog, int dialogText) {
        return modifyInstance(progressDialog, dialogText, false);
    }

    public static ProgressDialog modifyInstance(ProgressDialog progressDialog, int dialogText, boolean cancelable) {
        progressDialog.setMessage(progressDialog.getContext().getResources().getString(dialogText));
        progressDialog.setCancelable(cancelable);

        return progressDialog;
    }

}
