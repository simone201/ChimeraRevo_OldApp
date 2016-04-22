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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.near.chimerarevo.misc.Constants;

public class JSONUtils {

	public static JSONObject getJSONObject(String str, String key) {
		JSONObject jObject = null;
		
		try {
			jObject = (new JSONObject(str)).getJSONObject(key);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		return jObject;
	}
	
	public static JSONArray getJSONArray(String str, String key) {
		JSONObject jObject;
		JSONArray jArray = null;
		
		try {
			jObject = new JSONObject(str);
			jArray = jObject.getJSONArray(key);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		return jArray;
	}
	
	public static String getCategory(JSONObject jObject) {
		try {
			JSONArray jArray = jObject.getJSONArray(Constants.KEY_CAT);
			JSONObject temp = jArray.getJSONObject(0);
			return temp.getString(Constants.KEY_NAME);
		} catch (JSONException e) {
			return "";
		}
	}
	
}
