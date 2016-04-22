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

import com.near.chimerarevo.misc.Constants;

public class URLUtils {

	public static String getUrl() {
		return Constants.GET_POSTS_URL + "?type=post,recensioni,video&page=1";
	}
	
	public static String getUrl(int page) {
		return Constants.GET_POSTS_URL + "?type=post,recensioni,video&page=" + page;
	}
	
	public static String getUrl(int page, String cat) {
		return Constants.GET_POSTS_URL + "?page=" + page + "&category=" + cat;
	}
	
	public static String getUrl(String type) {
		return Constants.GET_POSTS_URL + "?type=" + type;
	}
	
	public static String getUrl(String type, int page) {
		return Constants.GET_POSTS_URL + "?page=" + page +"&type=" + type;
	}
	
	public static String getPostUrl(int id) {
		return Constants.GET_POST_URL + "?id=" + id;
	}
	
	public static String getPostUrl(String url) {
		return Constants.GET_POST_URL + "?url=" + url;
	}
	
	public static String getSearchUrl(String q, int page) {
		return Constants.SEARCH_URL + "?q=" + q.replace(" ", "+").replace("&", "").trim() + "&page=" + page;
	}
	
	public static String getProductsUrl(int page, String cat, String brand) {
		return Constants.PRODUCTS_URL + "?page=" + page + "&categories=" + cat + "&brands=" + brand.replaceAll(" ", "%20");
	}
	
	public static String getProductUrl(int id) {
		return Constants.PRODUCT_URL + "?id=" + id;
	}
	
	public static String getThreadIdUrl(String url) {
		return Constants.DISQUS_LIST_THREADS + "?api_key=" + Constants.DISQUS_API_KEY + "&forum=" + Constants.DISQUS_SITE_NAME + "&thread=link:" + url;
	}
	
	public static String getCommentsUrl(String id) {
		return Constants.DISQUS_LIST_COMMENTS + "?api_key=" + Constants.DISQUS_API_KEY + "&forum=" + Constants.DISQUS_SITE_NAME + "&thread=" + id;
	}
	
	public static String getDisqusAuthUrl() {
		return Constants.DISQUS_AUTH_URL + "client_id=" + Constants.DISQUS_API_KEY + "&scope=read,write&response_type=code&redirect_uri=" + Constants.SITE_URL;
	}
	
	public static String getDisqusAccessUrl(String code) {
		return Constants.DISQUS_TOKEN_URL + "grant_type=authorization_code&client_id=" + Constants.DISQUS_API_KEY 
				+ "&client_secret=" + Constants.DISQUS_API_SECRET + "&redirect_uri=" + Constants.SITE_URL + "&code=" + code;
	}
	
}
