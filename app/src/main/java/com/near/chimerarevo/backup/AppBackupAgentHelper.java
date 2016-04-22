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

import android.app.backup.BackupAgentHelper;

public class AppBackupAgentHelper extends BackupAgentHelper {

    private static final String FAVORITES_BACKUP = "favorites_db";
    private static final String PREFS_BACKUP = "main_prefs";

    public void onCreate() {
        addHelper(FAVORITES_BACKUP, new FavoritesBackupAgent(this));
        addHelper(PREFS_BACKUP, new PreferencesBackupAgent(this));
    }

}
