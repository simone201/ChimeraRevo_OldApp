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

package com.near.chimerarevo.fragments;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.fscz.util.TextViewEx;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubeThumbnailLoader;
import com.google.android.youtube.player.YouTubeThumbnailView;
import com.google.android.youtube.player.YouTubeThumbnailView.OnInitializedListener;
import com.melnykov.fab.FloatingActionButton;
import com.near.chimerarevo.R;
import com.near.chimerarevo.activities.BaseActivity;
import com.near.chimerarevo.activities.PostContainerActivity;
import com.near.chimerarevo.activities.YoutubeActivity;
import com.near.chimerarevo.misc.Constants;
import com.near.chimerarevo.widget.NotifyingScrollView;
import com.near.chimerarevo.widget.TouchImageView;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;

public class PostFragment extends Fragment implements NotifyingScrollView.OnScrollChangedListener, OnClickListener {

    private NotifyingScrollView mParent;
	private LinearLayout lay;
    private FrameLayout img_container, video_lay;
    private CardView video_card;
	private TextView title, subtitle, author;
	private ImageView img;
	private YouTubeThumbnailView thumb;
    private View mShadow;

	private boolean hasTitle = false, isLandscapeLarge = false;
	private byte numGalleries = 0;
    private int curAlpha = 0;
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v;

        if(getArguments().containsKey("isLandscapeLarge"))
            isLandscapeLarge = getArguments().getBoolean("isLandscapeLarge");

        hasTitle = getArguments().containsKey("hasTitle") && getArguments().getBoolean("hasTitle");

		if(hasTitle)
			v = inflater.inflate(R.layout.post_layout, container, false);
		else
			v = inflater.inflate(R.layout.post_page_layout, container, false);

        mParent = (NotifyingScrollView) v.findViewById(R.id.post_parent);

        LinearLayout post_layout = (LinearLayout) v.findViewById(R.id.post_layout);
		lay = (LinearLayout) v.findViewById(R.id.post_body);
        img_container = (FrameLayout) v.findViewById(R.id.post_img_container);

        if(getArguments().containsKey("isLandscapeLarge") && getArguments().getBoolean("isLandscapeLarge"))
            v.findViewById(R.id.goto_comments_container).setVisibility(View.GONE);
        else {
            FloatingActionButton fab = (FloatingActionButton) v.findViewById(R.id.comments_fab);
            fab.setOnClickListener(this);
        }

		if(hasTitle) {
			title = (TextView) v.findViewById(R.id.title);
			author = (TextView) v.findViewById(R.id.author);
			img = (ImageView) v.findViewById(R.id.img);
            subtitle = (TextView) v.findViewById(R.id.subtitle);
            video_card = (CardView) v.findViewById(R.id.video_card);

			if(!getArguments().containsKey(Constants.KEY_TYPE)
					|| !getArguments().getString(Constants.KEY_TYPE).equals(Constants.VIDEO)
					&& !getArguments().getString(Constants.KEY_TYPE).equals(Constants.RECENSIONI))
                video_card.setVisibility(View.GONE);
			else if (getArguments().getString(Constants.KEY_TYPE).equals(Constants.VIDEO)) {
                video_lay = (FrameLayout) v.findViewById(R.id.video_layout);
                subtitle.setVisibility(View.GONE);
                thumb = (YouTubeThumbnailView) v.findViewById(R.id.video_thumb);
                ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) post_layout.getLayoutParams();
                params.topMargin += 120;
                post_layout.setLayoutParams(params);
            } else if (getArguments().getString(Constants.KEY_TYPE).equals(Constants.RECENSIONI)) {
                subtitle.setVisibility(View.GONE);
                video_card.setVisibility(View.GONE);
                ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) post_layout.getLayoutParams();
                params.topMargin += 120;
                post_layout.setLayoutParams(params);
            }
		}
		
		return v;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
        ((BaseActivity) getActivity()).getToolbar().setTitle(getResources().getString(R.string.app_name));

        if(!isLandscapeLarge) {
            mShadow = ((PostContainerActivity) getActivity()).getDropShadow();
            if(mShadow != null)
                mShadow.setAlpha(0);
            ((BaseActivity) getActivity()).getToolbar().getBackground().setAlpha(curAlpha);
        }
        mParent.setOnScrollChangedListener(this);

        if(hasTitle)
			setTitle();
		
		parseHTML(getArguments().getString("html"));
	}

    @Override
    public void onScrollChanged(ScrollView who, int l, int t, int oldl, int oldt) {
        img_container.setTranslationY(mParent.getScrollY() * 0.5f);

        if(!isLandscapeLarge) {
            final int headerHeight = (int) (((float) img.getHeight()) * 1.5) - ((BaseActivity) getActivity()).getToolbar().getHeight();
            final float ratio = (float) Math.min(Math.max(t, 0), headerHeight) / headerHeight;
            curAlpha = (int) (ratio * 255);
            ((BaseActivity) getActivity()).getToolbar().getBackground().setAlpha(curAlpha);
            if(mShadow != null) {
                if(curAlpha > 250)
                    mShadow.setAlpha(curAlpha);
                else
                    mShadow.setAlpha(0);
            }
        }
    }

    @Override
    public void onClick(View view) {
        if(view.getId() == R.id.comments_fab)
            ((PostContainerActivity) getActivity()).scrollViewPager(1);
    }

    public int getCurAlpha() {
        return curAlpha;
    }

	private void setTitle() {
		if(getArguments().containsKey(Constants.KEY_IMG)) {
			DisplayImageOptions options = new DisplayImageOptions.Builder()
				.cacheOnDisk(false)
				.cacheInMemory(true)
				.showImageOnLoading(R.drawable.empty_cr)
				.bitmapConfig(Bitmap.Config.RGB_565)
				.imageScaleType(ImageScaleType.IN_SAMPLE_INT)
				.delayBeforeLoading(100)
				.build();
			ImageLoader.getInstance().displayImage(getArguments().getString(Constants.KEY_IMG), img, options);
		} else
            img.setImageResource(R.drawable.empty_cr);
		
		title.setText(getArguments().getString(Constants.KEY_POST_TITLE));
		author.setText("by " + getArguments().getString(Constants.KEY_POST_AUTHOR));
		
		if(getArguments().containsKey(Constants.KEY_TYPE)) {
			if(!getArguments().getString(Constants.KEY_TYPE).equals(Constants.VIDEO)
					&& !getArguments().getString(Constants.KEY_TYPE).equals(Constants.RECENSIONI)) {
				subtitle.setTypeface(subtitle.getTypeface(), Typeface.ITALIC);
				subtitle.setText(getArguments().getString(Constants.KEY_POST_SUBTITLE));
			} else if (getArguments().getString(Constants.KEY_TYPE).equals(Constants.VIDEO)) {
				final String yturl;
				if(getArguments().getString(Constants.KEY_VIDEO_URL).contains("youtu.be"))
					yturl = getArguments().getString(Constants.KEY_VIDEO_URL).split("youtu.be/")[1];
                else if(getArguments().getString(Constants.KEY_VIDEO_URL).contains("www.youtube.com"))
                    yturl = getArguments().getString(Constants.KEY_VIDEO_URL).split("v=")[1];
				else {
                    video_card.setVisibility(View.GONE);
                    return;
                }
			
				try {
					thumb.initialize(Constants.YOUTUBE_API_TOKEN, new OnInitializedListener() {
						@Override
						public void onInitializationFailure(YouTubeThumbnailView thumbView, YouTubeInitializationResult error) {
							error.getErrorDialog(getActivity(), 0).show();
							video_card.setVisibility(View.GONE);
						}
						@Override
						public void onInitializationSuccess(YouTubeThumbnailView thumbView, YouTubeThumbnailLoader thumbLoader) {
							thumbLoader.setVideo(yturl);
						}
					});
				
					video_lay.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							Intent i = new Intent(getActivity(), YoutubeActivity.class);
							i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							i.putExtra(Constants.KEY_VIDEO_URL, yturl);
							startActivity(i);
						}
					});
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} else {
			subtitle.setTypeface(subtitle.getTypeface(), Typeface.ITALIC);
			subtitle.setText(getArguments().getString(Constants.KEY_POST_SUBTITLE));
		}
	}
	
	private void parseHTML(String html) {
		Document doc = Jsoup.parse(html);
		Elements el = doc.body().children();

		for(Element e : el) {
			if(e.getElementsByTag("h1").size() > 0)
				parseTitles(e.getElementsByTag("h1"), 1);
			
			if(e.getElementsByTag("h2").size() > 0)
				parseTitles(e.getElementsByTag("h2"), 2);
			
			if(e.getElementsByTag("h3").size() > 0)
				parseTitles(e.getElementsByTag("h3"), 3);
			
			if(e.getElementsByTag("h4").size() > 0)
				parseTitles(e.getElementsByTag("h4"), 4);

            if(e.getElementsByTag("h5").size() > 0)
                parseTitles(e.getElementsByTag("h5"), 5);
			
			if(e.getElementsByTag("p").size() > 0)
				parseParagraphs(e.getElementsByTag("p"));
			
			if(e.getElementsByTag("img").size() > 0)
				parseNormalImages(e.getElementsByTag("img"));
			
			if(e.getElementsByTag("a").size() > 0)
				parseLinkedImages(e.getElementsByTag("a"));
			
			if(e.getElementsByTag("iframe").size() > 0)
				parseYoutubeVideos(e.getElementsByTag("iframe"));
				
			if(e.getElementsByTag("ul").size() > 0)
				parseBulletedLists(e.getElementsByTag("ul"));
			
			if(e.getElementsByTag("ol").size() > 0)
				parseOrderedLists(e.getElementsByTag("ol"));
			
			if(e.getElementsByTag("pre").size() > 0)
				parseCodeText(e.getElementsByTag("pre"));
			
			if(e.getElementsByTag("tr").size() > 0)
				parseTables(e.getElementsByTag("tr"));
		}

        ((PostContainerActivity) getActivity()).setIsLoading(false);
	}

	private void parseTitles(Elements tils, int type) {
		for(Element ti : tils)
			addTitle(ti.text().trim(), type);
	}
	
	private void parseParagraphs(Elements ps) {
		for(Element p : ps) {
			if(!p.html().startsWith("&") 
					&& !p.html().startsWith("<iframe")
					&& !p.html().startsWith("<!")
					&& !p.html().contains("<h")
					&& !p.html().contains("<ol")
					&& !p.html().contains("<ul")
					&& !p.html().contains("<pre")
					&& !p.html().contains("<tr")) {
				parseNormalImages(p.select("img"));
				p.select("img").remove();
				
				Elements lnks = p.getElementsByTag("a");
				for(Element lnk : lnks) {
					if(lnk.attr("href").startsWith("#"))
						lnk.removeAttr("href");
				}
				
				String txt = p.html().replace("<br />", "").replace("\n", "").trim();
				if(txt.length() > 0)
					addText(txt, true, Typeface.DEFAULT);
			}
		}
	}
	
	private void parseNormalImages(Elements ims) {
		if(PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("load_imgs_pref", true))
			for(Element im : ims) {
				if(im.attr("src").startsWith("http")) {
					if(!im.attr("src").contains("www.gstatic.com"))
						addImage(im.attr("src"));
					else if(im.hasAttr("pagespeed_lazy_src"))
						addImage(im.attr("pagespeed_lazy_src"));
				}
			}
	}
	
	private void parseLinkedImages(Elements lnk) {
		if(PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("load_imgs_pref", true)) {
			String[] urls = new String[lnk.size()];
			for(int i = 0; i < lnk.size(); i++) {
				Elements ims = lnk.get(i).getElementsByTag("img");
				for(Element im : ims) {
                    if(im.hasAttr("data-original"))
					    urls[i] = im.attr("data-original");
                    else if(im.hasAttr("ng-src"))
                        urls[i] = im.attr("ng-src");
                    else
                        urls[i] = "#";
				}
			}
			if(urls.length == 1)
				addImage(urls[0]);
			else
				addGallery(urls);
		}
	}
	
	private void parseYoutubeVideos(Elements vids) {
        for(Element vid : vids) {
            if (vid.hasAttr("src")) {
                if (vid.attr("src").contains("youtube")) {
                    if (PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("load_vid_pref", true))
                        addYoutubeVideo(vid.attr("src"));
                } else
                    addWebObject(vid.attr("width"), vid.attr("height"), vid.attr("src"));
            }
        }
	}
	
	private void parseBulletedLists(Elements itms) {		
		String bld = "";
		for(Element itm : itms) {
			Elements str = itm.getElementsByTag("li");
			for(Element itm2 : str) {
				if(itm2.children().size() >= 1) {
					Elements ch = itm2.getElementsByTag("a");
					for(Element c : ch) {
						if(c.attr("href").contains("#"))
							c.removeAttr("href");
					}
				}
				bld += ("\u2022 " + itm2.outerHtml() + "<br />");
			}
		}
		addText(bld, true, Typeface.DEFAULT);
	}
	
	private void parseOrderedLists(Elements itms) {
		String bld = "";
		for(Element itm : itms) {
			Elements str = itm.getElementsByTag("li");
			for(int j = 0; j < str.size(); j++) {
				Element itm2 = str.get(j);
				bld += ("<b>" + (j+1) + ")</b> <i>" + itm2.outerHtml() + "</i><br />");
			}
		}
		addText(bld, true, Typeface.DEFAULT);
	}
	
	private void parseCodeText(Elements cds) {
		for(Element cd : cds)
			addText(cd.text().trim(), false, Typeface.MONOSPACE);
	}
	
	private void parseTables(Elements tbls) {
		TableLayout tl = new TableLayout(getActivity());
		LayoutParams tl_prms = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		tl_prms.gravity = Gravity.CENTER_HORIZONTAL;
		tl_prms.setMargins(10, 10, 10, 0);
		tl.setLayoutParams(tl_prms);

		for(Element tbl : tbls) {
			Elements rws = tbl.getElementsByTag("td");
			TableRow row = new TableRow(getActivity());
			for(Element rw : rws) {
				TextView txt = new TextView(getActivity());
				txt.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT));
				txt.setText(rw.text());
				row.addView(txt);
			}
			tl.addView(row);
		}
		lay.addView(tl);
	}
	
	private void addTitle(String text, int type) {
		TextView txt = new TextView(getActivity());
		txt.setText(text);
		txt.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
		LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		params.setMargins(10, 10, 10, 0);
		txt.setLayoutParams(params);
        txt.setTextIsSelectable(true);
		
		switch(type) {
			case 1:
				txt.setTextSize(26);
				break;
			case 2:
				txt.setTextSize(24);
				break;
			case 3:
				txt.setTextSize(22);
				break;
			case 4:
				txt.setTextSize(20);
				break;
            case 5:
                txt.setTextSize(18);
                break;
			default:
				break;
		}
		
		lay.addView(txt);
	}
	
	private void addText(String text, boolean isHTML, Typeface tf) {
		TextViewEx txt = new TextViewEx(getActivity());
		boolean justifyText = PreferenceManager.getDefaultSharedPreferences(getActivity())
				.getBoolean("justify_text_pref", false);
		if(isHTML) {
			txt.setText(Html.fromHtml(text), justifyText);
			txt.setLinksClickable(true);
            Linkify.addLinks(txt, Linkify.WEB_URLS);
            txt.setTextIsSelectable(true);
            txt.setMovementMethod(LinkMovementMethod.getInstance());
		} else {
            txt.setText(text, justifyText);
            txt.setTextIsSelectable(true);
        }
		int textSize = PreferenceManager.getDefaultSharedPreferences(getActivity())
				.getInt("text_size_pref", 16);
		txt.setTextSize(textSize);
		txt.setTypeface(tf);
		LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		params.setMargins(15, 10, 15, 0);
		txt.setLayoutParams(params);
		lay.addView(txt);
	}
	
	private void addImage(final String imgUrl) {
		final ImageView img = new ImageView(getActivity());
		LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		params.setMargins(15, 0, 15, 0);
		params.gravity = Gravity.CENTER_HORIZONTAL;
		img.setLayoutParams(params);
		img.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
		lay.addView(img);

		final DisplayImageOptions options = new DisplayImageOptions.Builder()
			.cacheOnDisk(false)
			.cacheInMemory(true)
			.showImageOnLoading(R.drawable.empty_cr)
			.bitmapConfig(Bitmap.Config.RGB_565)
			.imageScaleType(ImageScaleType.EXACTLY)
			.delayBeforeLoading(150)
			.build();
		ImageLoader.getInstance().displayImage(imgUrl, img, options);
		
		img.setClickable(true);
		img.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				final Dialog dialog = new Dialog(getActivity());
				dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
				dialog.setContentView(R.layout.img_dialog_layout);
				ImageLoader.getInstance().displayImage(imgUrl.split("\\?resize=")[0], ((TouchImageView) dialog.findViewById(R.id.dialog_image)), options);
				dialog.setCancelable(true);
				dialog.show();
			}
		});
	}
	
	private void addGallery(final String[] imgUrls) {
		numGalleries++;
		if(!PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("load_imgs_pref", true) ||
				numGalleries > Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(getActivity()).getString("gallery_num_pref", "20")))
			return;
		HorizontalScrollView hsv = new HorizontalScrollView(getActivity());
		LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		params.gravity = Gravity.CENTER_HORIZONTAL;
		params.setMargins(10, 10, 10, 0);
		hsv.setLayoutParams(params);
		
		LinearLayout container = new LinearLayout(getActivity());
		container.setOrientation(LinearLayout.HORIZONTAL);
		for(int i = 0; i < imgUrls.length; i++) {
			final ImageView img = new ImageView(getActivity());
			LayoutParams imgPar = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			if(i == 0)
				imgPar.setMargins(5, 10, 0, 10);
			else
				imgPar.setMargins(10, 10, 0, 10);
			img.setLayoutParams(imgPar);
			img.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
			container.addView(img);

            final DisplayImageOptions options = new DisplayImageOptions.Builder()
				.cacheOnDisk(false)
				.cacheInMemory(true)
				.showImageOnLoading(R.drawable.empty_cr)
				.bitmapConfig(Bitmap.Config.RGB_565)
				.imageScaleType(ImageScaleType.EXACTLY)
				.delayBeforeLoading(200)
				.build();
			ImageLoader.getInstance().displayImage(imgUrls[i], img, options);
			
			final int k = i;
			img.setClickable(true);
			img.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					final Dialog dialog = new Dialog(getActivity());
					dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
					dialog.setContentView(R.layout.img_dialog_layout);
					ImageLoader.getInstance().displayImage(imgUrls[k].split("\\?resize=")[0], ((TouchImageView) dialog.findViewById(R.id.dialog_image)), options);
					dialog.setCancelable(true);
					dialog.show();
				}
			});
		}
		hsv.addView(container);
		lay.addView(hsv);
	}
	
	private void addYoutubeVideo(String url) {
		final String yturl;
		if(url.contains("embed")) {
			String temp = url.split("embed/")[1];
			if(url.contains("feature")) {
				temp = temp.split("feature=")[0];
				yturl = temp.substring(0, temp.length() - 1);
			} else
				yturl = temp;
		} else if(url.contains("youtu.be")) {
			yturl = url.split("youtu.be/")[1];
		} else
			return;

		final RelativeLayout rl = new RelativeLayout(getActivity());
		YouTubeThumbnailView yt = new YouTubeThumbnailView(getActivity());
		ImageView icon = new ImageView(getActivity());

        try {
            yt.setTag(yturl);
            yt.initialize(Constants.YOUTUBE_API_TOKEN, new OnInitializedListener() {
                @Override
                public void onInitializationFailure(YouTubeThumbnailView thumbView, YouTubeInitializationResult error) {
					rl.setVisibility(View.GONE);
                }
                @Override
                public void onInitializationSuccess(YouTubeThumbnailView thumbView, YouTubeThumbnailLoader thumbLoader) {
                    thumbLoader.setVideo(yturl);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

		RelativeLayout.LayoutParams obj_params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		obj_params.addRule(RelativeLayout.CENTER_HORIZONTAL);
		obj_params.addRule(RelativeLayout.CENTER_VERTICAL);
		yt.setLayoutParams(obj_params);
		
		icon.setImageResource(R.drawable.yt_play_button);
		icon.setLayoutParams(obj_params);
		
		RelativeLayout.LayoutParams rl_params = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		rl_params.setMargins(0, 10, 0, 0);
		rl.setLayoutParams(rl_params);
		rl.setGravity(Gravity.CENTER_HORIZONTAL);
		rl.setClickable(true);
		
		rl.addView(yt);
		rl.addView(icon);
		
		rl.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent(getActivity(), YoutubeActivity.class);
				i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				i.putExtra(Constants.KEY_VIDEO_URL, yturl);
				startActivity(i);
			}
		});
		
		lay.addView(rl);
	}
	
	@SuppressLint("SetJavaScriptEnabled")
	private void addWebObject(String width, String height, String url) {
		WebView wv = new WebView(getActivity());
		LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		
		wv.setLayoutParams(params);
		wv.getSettings().setUseWideViewPort(true);
        wv.getSettings().setLoadWithOverviewMode(true);
		wv.getSettings().setSupportZoom(false);
		wv.setVerticalScrollBarEnabled(true);
        wv.setHorizontalScrollBarEnabled(true);
		wv.getSettings().setJavaScriptEnabled(true);
		wv.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        wv.setWebViewClient(new WebViewClient());

		String frame = "<iframe width=\"" + width + "\" height=\"" + height + "\" frameborder=\"0\" src=\"" + url + "\"/>";
        wv.loadDataWithBaseURL(url, frame, "html/plain", "UTF-8", null);
		lay.addView(wv);
	}

}
