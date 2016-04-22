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

import android.graphics.Bitmap;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.near.chimerarevo.R;
import com.near.chimerarevo.misc.Constants;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.pnikosis.materialishprogress.ProgressWheel;

import java.util.ArrayList;
import java.util.HashMap;

public class ProductsAdapter extends BaseAdapter {

    private ArrayList<HashMap<String,String>> arrayList;

    public ProductsAdapter(ArrayList<HashMap<String,String>> arrayList) {
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

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView img;
        TextView name, brand;
        final ProgressWheel wheel;

        if(convertView == null)
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.products_grid_item_layout, parent, false);

        img = (ImageView) convertView.findViewById(R.id.prod_list_img);
        name = (TextView) convertView.findViewById(R.id.prod_list_name);
        brand = (TextView) convertView.findViewById(R.id.prod_list_brand);
        wheel = (ProgressWheel) convertView.findViewById(R.id.prod_list_img_progress);

        name.setText(Html.fromHtml(arrayList.get(position).get(Constants.KEY_NAME)));
        brand.setText(arrayList.get(position).get(Constants.KEY_BRAND));

        DisplayImageOptions options = new DisplayImageOptions.Builder()
                .cacheOnDisk(true)
                .cacheInMemory(false)
                .imageScaleType(ImageScaleType.IN_SAMPLE_INT)
                .bitmapConfig(Bitmap.Config.RGB_565)
                .delayBeforeLoading(150)
                .build();
        ImageLoader.getInstance().displayImage(arrayList.get(position).get(Constants.KEY_IMG), img,
                options, new ImageLoadingListener() {

            @Override
            public void onLoadingStarted(String imageUri, View view) {
                wheel.setVisibility(View.VISIBLE);
            }

            @Override
            public void onLoadingFailed(String imageUri, View view, FailReason
            failReason) {
                wheel.setVisibility(View.GONE);
            }

            @Override
            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                wheel.setVisibility(View.GONE);
            }

            @Override
            public void onLoadingCancelled(String imageUri, View view) {
                wheel.setVisibility(View.GONE);
            }
        });

        return convertView;
    }

    public int getProductId(int position) {
        return Integer.parseInt(arrayList.get(position).get(Constants.KEY_ID));
    }

    public String getProductName(int position) {
        return arrayList.get(position).get(Constants.KEY_NAME);
    }

    public String getImgUrl(int position) {
        return arrayList.get(position).get(Constants.KEY_IMG);
    }
}
