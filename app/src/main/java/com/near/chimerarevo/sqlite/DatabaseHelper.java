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

package com.near.chimerarevo.sqlite;

import java.text.MessageFormat;

import com.near.chimerarevo.R;
import com.near.chimerarevo.sqlite.tables.FavoritesTable;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.Toast;

public class DatabaseHelper extends SQLiteOpenHelper {

	private static final String DATABASE_NAME = "chimerarevo.db";
	private static final int SCHEMA_VERSION = 2;

    private Context ctx;

	public DatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, SCHEMA_VERSION);
        this.ctx = context;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		String sql = "CREATE TABLE  IF NOT EXISTS {0} ({1} INTEGER PRIMARY KEY AUTOINCREMENT," +
				" {2} TEXT NOT NULL, {3} TEXT NOT NULL, {4} TEXT NOT NULL, {5} TEXT NOT NULL, {6} TEXT NOT NULL, {7} TEXT NOT NULL);";
		db.execSQL(MessageFormat.format(sql, FavoritesTable.TABLE_NAME, FavoritesTable._ID,
				FavoritesTable.POST_ID, FavoritesTable.POST_TITLE, FavoritesTable.POST_IMG,
                FavoritesTable.POST_DATE, FavoritesTable.POST_TYPE, FavoritesTable.POST_URL));
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if(oldVersion == 1 && newVersion == 2) {
            db.execSQL("DROP TABLE IF EXISTS " + FavoritesTable.TABLE_NAME);
            onCreate(db);
            Toast.makeText(ctx, ctx.getResources().getString(R.string.text_favorites_resetted), Toast.LENGTH_LONG).show();
        }
	}

	public long insertFavouritePost(int id, String title, String img, String date, String type, String url) {
		ContentValues v = new ContentValues();
		v.put(FavoritesTable.POST_ID, String.valueOf(id));
		v.put(FavoritesTable.POST_TITLE, title);
		v.put(FavoritesTable.POST_IMG, img);
		v.put(FavoritesTable.POST_DATE, date);
		v.put(FavoritesTable.POST_TYPE, type);
        v.put(FavoritesTable.POST_URL, url);
		return getWritableDatabase().insert(FavoritesTable.TABLE_NAME, null, v);
	}
	
	public Cursor getFavourites() {
		return (getReadableDatabase().query(
				FavoritesTable.TABLE_NAME,
				FavoritesTable.COLUMNS,
				null,
				null,
				null, 
				null, 
				FavoritesTable.POST_ID));
	}
	
	public boolean hasFavourite(int id) {
		Cursor c = getReadableDatabase().query(
				FavoritesTable.TABLE_NAME,
				FavoritesTable.COLUMNS,
				FavoritesTable.POST_ID + "=" + id,
				null,
				null, 
				null, 
				FavoritesTable.POST_ID);
		
		return c.moveToNext();
	}
	
	public boolean removeFavourite(int id) {
		return getWritableDatabase().delete(FavoritesTable.TABLE_NAME, FavoritesTable.POST_ID + "=" + id, null) > 0;
	}
	
}
