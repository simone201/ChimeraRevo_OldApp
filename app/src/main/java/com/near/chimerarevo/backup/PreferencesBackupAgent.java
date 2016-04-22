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

package com.near.chimerarevo.backup;

import android.app.backup.SharedPreferencesBackupHelper;
import android.content.Context;

import com.near.chimerarevo.misc.Constants;

public class PreferencesBackupAgent extends SharedPreferencesBackupHelper {

    private static final String MAIN_PREFS_TAG = "com.near.chimerarevo_preferences";

    public PreferencesBackupAgent(Context context) {
        super(context, Constants.PREFS_TAG, MAIN_PREFS_TAG);
    }

}
