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

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.near.chimerarevo.R;
import com.near.chimerarevo.activities.PostContainerActivity;
import com.near.chimerarevo.misc.Constants;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.pnikosis.materialishprogress.ProgressWheel;

import java.util.ArrayList;
import java.util.HashMap;

public class PostsRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private ArrayList<HashMap<String,String>> arrayList;
    private Context ctx;

    private boolean hasHeader;

    public PostsRecyclerAdapter(Context context, ArrayList<HashMap<String,String>> arrayList, boolean hasHeader) {
        super();
        this.ctx = context;
        this.arrayList = arrayList;
        this.hasHeader = hasHeader;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
        if(hasHeader) {
            if (viewType == 0)
                return new HeaderViewHolder(inflater.inflate(R.layout.posts_header_overlay, viewGroup, false), ctx);
            else
                return new PostViewHolder(inflater.inflate(R.layout.posts_card_layout, viewGroup, false), ctx);
        } else
            return new PostViewHolder(inflater.inflate(R.layout.posts_card_layout, viewGroup, false), ctx);
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        if(viewHolder instanceof PostViewHolder) {
            final PostViewHolder postViewHolder = (PostViewHolder) viewHolder;
            postViewHolder.setMap(arrayList.get(position));
            postViewHolder.title.setText(Html.fromHtml(arrayList.get(position).get(Constants.KEY_TITLE)));
            postViewHolder.sub.setText(arrayList.get(position).get(Constants.KEY_DATE));
            postViewHolder.container.setBackgroundColor(getBackgroundColor(arrayList.get(position).get(Constants.KEY_DATE).split("[\\x7C]")[1].trim()));

            DisplayImageOptions options = new DisplayImageOptions.Builder()
                    .cacheOnDisk(true)
                    .cacheInMemory(false)
                    .imageScaleType(ImageScaleType.IN_SAMPLE_INT)
                    .bitmapConfig(Bitmap.Config.RGB_565)
                    .delayBeforeLoading(150)
                    .build();
            ImageLoader.getInstance().displayImage(arrayList.get(position).get(Constants.KEY_IMG), postViewHolder.img,
                    options, new ImageLoadingListener() {

                        @Override
                        public void onLoadingStarted(String imageUri, View view) {
                            postViewHolder.progress.setVisibility(View.VISIBLE);
                        }

                        @Override
                        public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
                            postViewHolder.progress.setVisibility(View.GONE);
                        }

                        @Override
                        public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                            postViewHolder.progress.setVisibility(View.GONE);
                        }

                        @Override
                        public void onLoadingCancelled(String imageUri, View view) {
                            postViewHolder.progress.setVisibility(View.GONE);
                        }
                    });
        } else if(viewHolder instanceof HeaderViewHolder) {
            HeaderViewHolder headerViewHolder = (HeaderViewHolder) viewHolder;
            headerViewHolder.setMap(arrayList.get(position));
        }
    }

    @Override
    public int getItemCount() {
        return arrayList.size();
    }

    private int getBackgroundColor(String mCat) {
        int[] colors = ctx.getResources().getIntArray(R.array.categories_colors);
        String[] cats = ctx.getResources().getStringArray(R.array.menu_categories_titles_italian);

        for(int i = 0; i < cats.length; i++) {
            if(mCat.equalsIgnoreCase(cats[i]))
                return colors[i];
        }

        return ctx.getResources().getColor(android.R.color.black);
    }

    public static class HeaderViewHolder extends BaseViewHolder {
        public HeaderViewHolder(View v, Context ctx) {
            super(v, ctx);
            v.setOnClickListener(this);
        }
    }

    public static class PostViewHolder extends BaseViewHolder {
        protected ImageView img;
        protected TextView title, sub;
        protected View container;
        protected ProgressWheel progress;

        public PostViewHolder(View v, Context ctx) {
            super(v, ctx);
            v.setOnClickListener(this);

            img =  (ImageView) v.findViewById(R.id.post_img);
            title = (TextView)  v.findViewById(R.id.post_title);
            sub = (TextView)  v.findViewById(R.id.post_date_cat);
            container = v.findViewById(R.id.post_info_container);
            progress = (ProgressWheel) v.findViewById(R.id.post_img_progress);
        }
    }

    public static class BaseViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        protected HashMap<String,String> map;
        private Context ctx;

        public BaseViewHolder(View v, Context ctx) {
            super(v);
            this.ctx = ctx;
        }

        @Override
        public void onClick(View view) {
            Bundle args = new Bundle();
            args.putInt(Constants.KEY_ID, getPostId());
            args.putString(Constants.KEY_TITLE, getPostTitle());
            args.putString(Constants.KEY_IMG, getImgUrl());
            args.putString(Constants.KEY_TYPE, getPostType());
            args.putString(Constants.KEY_DATE, getPostDate());
            args.putString(Constants.KEY_URL, getPostUrl());

            ActivityOptionsCompat opts = ActivityOptionsCompat.makeScaleUpAnimation(view, 0, 0,
                    view.getWidth(), view.getHeight());
            Intent i = new Intent(ctx, PostContainerActivity.class);
            i.putExtras(args);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                ctx.startActivity(i, opts.toBundle());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void setMap(HashMap<String,String> map) {
            this.map = map;
        }

        private int getPostId() {
            return Integer.parseInt(map.get(Constants.KEY_ID));
        }

        private String getPostTitle() {
            return map.get(Constants.KEY_TITLE);
        }

        private String getImgUrl() {
            return map.get(Constants.KEY_IMG);
        }

        private String getPostType() {
            return map.get(Constants.KEY_TYPE);
        }

        private String getPostDate() {
            return map.get(Constants.KEY_DATE);
        }

        private String getPostUrl() {
            return map.get(Constants.KEY_URL);
        }
    }

}
