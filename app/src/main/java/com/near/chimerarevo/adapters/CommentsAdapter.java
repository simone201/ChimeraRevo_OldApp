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

package com.near.chimerarevo.adapters;

import java.util.ArrayList;
import java.util.HashMap;

import org.jsoup.Jsoup;

import com.near.chimerarevo.R;
import com.near.chimerarevo.misc.Constants;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;

import de.hdodenhof.circleimageview.CircleImageView;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class CommentsAdapter extends BaseAdapter {

	private ArrayList<HashMap<String,String>> arrayList;
	
	public CommentsAdapter(ArrayList<HashMap<String,String>> arrayList) {
		super();
		this.arrayList = arrayList;
	}
	
	@Override
	public int getCount() {
		return arrayList.size();
	}

	@Override
	public Object getItem(int position) {
		return position;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}
	
	public int getCommentId(int position) {
		return Integer.parseInt(arrayList.get(position).get(Constants.KEY_ID));
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		TextView user, date, text;
		CircleImageView img;
		
		if (convertView == null)
			convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.comment_item_layout, parent, false);
		
		user = (TextView) convertView.findViewById(R.id.comment_user);
		date = (TextView) convertView.findViewById(R.id.comment_date);
		text = (TextView) convertView.findViewById(R.id.comment_text);
		img = (CircleImageView) convertView.findViewById(R.id.comment_img);
		
		HashMap<String,String> map = arrayList.get(position);
		user.setText(map.get(Constants.KEY_NAME));
		date.setText(map.get(Constants.KEY_DATE));
		text.setText(Jsoup.parse(map.get(Constants.KEY_MESSAGE)).body().text());
		
		DisplayImageOptions options = new DisplayImageOptions.Builder()
			.cacheOnDisk(true)
			.cacheInMemory(false)
			.imageScaleType(ImageScaleType.IN_SAMPLE_INT)
			.bitmapConfig(Bitmap.Config.RGB_565)
			.delayBeforeLoading(100)
			.build();
		ImageLoader.getInstance().displayImage(map.get(Constants.KEY_IMG), img, options);
		
		return convertView;
	}
	
}
