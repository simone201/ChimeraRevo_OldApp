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

import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.enums.SnackbarType;
import com.nispok.snackbar.listeners.EventListener;

public class SnackbarUtils {

    public static Snackbar showShortSnackbar(Activity activity, String text) {
        return showSnackbar(activity, Snackbar.SnackbarDuration.LENGTH_SHORT,
                SnackbarType.SINGLE_LINE, text, null);
    }

    public static Snackbar showShortSnackbar(Activity activity, String text, EventListener listener) {
        return showSnackbar(activity, Snackbar.SnackbarDuration.LENGTH_SHORT,
                SnackbarType.SINGLE_LINE, text, listener);
    }

    public static Snackbar showLongSnackbar(Activity activity, String text) {
        return showSnackbar(activity, Snackbar.SnackbarDuration.LENGTH_LONG,
                SnackbarType.SINGLE_LINE, text, null);
    }

    public static Snackbar showLongSnackbar(Activity activity, String text, EventListener listener) {
        return showSnackbar(activity, Snackbar.SnackbarDuration.LENGTH_LONG,
                SnackbarType.SINGLE_LINE, text, listener);
    }

    public static Snackbar showMultiShortSnackbar(Activity activity, String text) {
        return showSnackbar(activity, Snackbar.SnackbarDuration.LENGTH_SHORT,
                SnackbarType.MULTI_LINE, text, null);
    }

    public static Snackbar showMultiShortSnackbar(Activity activity, String text, EventListener listener) {
        return showSnackbar(activity, Snackbar.SnackbarDuration.LENGTH_SHORT,
                SnackbarType.MULTI_LINE, text, listener);
    }

    public static Snackbar showMultiLongSnackbar(Activity activity, String text) {
        return showSnackbar(activity, Snackbar.SnackbarDuration.LENGTH_LONG,
                SnackbarType.MULTI_LINE, text, null);
    }

    public static Snackbar showMultiLongSnackbar(Activity activity, String text, EventListener listener) {
        return showSnackbar(activity, Snackbar.SnackbarDuration.LENGTH_LONG,
                SnackbarType.MULTI_LINE, text, listener);
    }

    private static Snackbar showSnackbar(Activity activity, Snackbar.SnackbarDuration duration,
                                     SnackbarType type, String text, EventListener listener) {
        return Snackbar.with(activity)
                .duration(duration)
                .type(type)
                .text(text)
                .eventListener(listener);
    }

}
