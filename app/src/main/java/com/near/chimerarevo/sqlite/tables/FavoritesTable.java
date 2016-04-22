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

package com.near.chimerarevo.sqlite.tables;

import android.provider.BaseColumns;

public class FavoritesTable implements BaseColumns {

	public static String TABLE_NAME = "favorites";
	 
	public static String POST_ID = "postid";
	public static String POST_TITLE = "title";
	public static String POST_IMG = "img";
	public static String POST_DATE = "date";
	public static String POST_TYPE = "type";
    public static String POST_URL = "url";
 
	public static String[] COLUMNS = new String[]
			{ _ID, POST_ID, POST_TITLE, POST_IMG, POST_DATE, POST_TYPE, POST_URL };
	
}
