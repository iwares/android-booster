/*
 * Copyright (C) 2013 iWARES Solution Provider
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

/**
 * @file	src/com/iwares/lib/booster/core/BitmapResolver.java
 * @author	Eric.Tsai
 *
 */

package com.iwares.lib.booster.core;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.WeakHashMap;
import java.util.concurrent.Executor;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;

import com.iwares.lib.booster.support.AsyncTask;
import com.iwares.lib.booster.support.AsyncTask.Status;
import com.iwares.lib.booster.support.BitmapFactory;
import com.iwares.lib.booster.support.DataCache;
import com.iwares.lib.booster.support.DataCache.CacheData;
import com.iwares.lib.booster.support.SQLiteCache;

public class BitmapResolver {

	private static final WeakHashMap<String, BitmapResolver> mResolverMap = new WeakHashMap<String, BitmapResolver>();

	private static final String ANDROID_ASSET = "/android_asset/";

	public static BitmapResolver obtain(Context appContext, String pathName) {
		BitmapResolver resolver = mResolverMap.get(pathName);
		if (resolver == null)
			resolver = new BitmapResolver(appContext, pathName);
		if (resolver.mContext != appContext)
			throw new IllegalArgumentException("Context mismatch");
		mResolverMap.put(pathName, resolver);
		return resolver;
	}

	private static final String SCHEME_HTTP = "http";

	private static class BitmapReference extends SoftReference<Bitmap> {

		private final String mName;

		private final long mExpires;

		public BitmapReference(String n, Bitmap r, long expires, ReferenceQueue<Bitmap> q) {
			super(r, q);
			mName = n;
			mExpires = expires;
		}

		public final String name() {
			return mName;
		}

		public final boolean isExpired() {
			return mExpires <= System.currentTimeMillis();
		}
		
	}

	private static final SimpleDateFormat mDateHeaderFormat = new SimpleDateFormat(
		"EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US
		);

	private static final long DEFAULT_MAX_AGE = 5 * 60 * 1000;

	private final Context mContext;

	private final HashMap<String, BitmapReference> mMemoryCache;

	private final ReferenceQueue<Bitmap> mLapsedQueue;

	private final DataCache mStorageCache;

	private final WeakHashMap<String, HttpResolveTask> mHttpResolveTasks;

	private int mMinSideLength, mMaxNumOfPixels;

	private Executor mAsyncTaskExecutor;

	private void cacheBitmap(String name, Bitmap bitmap, long expires) {
		ReferenceQueue<Bitmap> queue = mLapsedQueue;
		for (Object ref = queue.poll(); ref != null; ref = queue.poll())
			mMemoryCache.remove(((BitmapReference)ref).name());
		BitmapReference ref = new BitmapReference(name, bitmap, expires, queue);
		mMemoryCache.put(name, ref);
	}

	private BitmapResolver(Context context, String pathName) {
		mMemoryCache = new HashMap<String, BitmapReference>();
		mLapsedQueue = new ReferenceQueue<Bitmap>();
		mContext = context;
		mStorageCache = new SQLiteCache(pathName);
		mHttpResolveTasks = new WeakHashMap<String, HttpResolveTask>();
		mMinSideLength = 0;
		mMaxNumOfPixels = Integer.MAX_VALUE;
		mAsyncTaskExecutor = AsyncTask.SERIAL_EXECUTOR;
	}

	public void setMinSideLength(int minSideLength) {
		mMinSideLength = minSideLength;
	}

	public void setMaxNumOfPixels(int maxNumOfPixels) {
		mMaxNumOfPixels = maxNumOfPixels;
	}

	public void setAsynTaskExecutor(Executor executor) {
		mAsyncTaskExecutor = executor;
	}

	public interface Callback {
		public void onBitmapResloved(Uri uri, Bitmap bitmap, boolean async);
	}

	private class HttpResolveTask extends AsyncTask<Void, Void, CacheData> {

		private final ArrayList<Callback> mCallbacks;

		private final Uri mUri;

		public HttpResolveTask(Uri uri) {
			mCallbacks = new ArrayList<Callback>();
			mUri = uri;
		}

		private CacheData refreshHttpCacheData(String url, CacheData cacheData) throws ClientProtocolException, IOException {
			if (cacheData == null)
				cacheData = new CacheData();
			HttpGet httpRequest = new HttpGet(url);
			String ifModifiedSince = mDateHeaderFormat.format(new Date(cacheData.lastModified()));
			httpRequest.addHeader("If-Modified-Since", ifModifiedSince);
			DefaultHttpClient httpClient = new DefaultHttpClient();
			HttpParams httpParams = httpClient.getParams();
			HttpConnectionParams.setConnectionTimeout(httpParams, 20 * 1000);
			HttpConnectionParams.setSoTimeout(httpParams, 45 * 1000);
			httpClient.setParams(httpParams);
			HttpResponse response = httpClient.execute(httpRequest);
			int status = response.getStatusLine().getStatusCode();
			if (status == HttpStatus.SC_OK) {
				cacheData.setData(EntityUtils.toByteArray(response.getEntity()));
			} else if (status == HttpStatus.SC_NOT_MODIFIED) {
				// Do nothing.
			} else {
				throw new IOException("Unexpected HTTP status code: " + status);
			}
			final long currentTimeMillis = System.currentTimeMillis();
			long lastModified = currentTimeMillis;
			try {
				String temp = response.getLastHeader("Last-Modified").getValue();
				lastModified = mDateHeaderFormat.parse(temp).getTime();
			} catch (Exception e) { }
			long expires = currentTimeMillis + DEFAULT_MAX_AGE;
			try {
				String temp = response.getLastHeader("Cache-Control").getValue();
				if (!temp.startsWith("max-age="))
					throw new IOException("Unsupported Cache-Control");
				long maxAge = Long.parseLong(temp.substring(8));
				if (maxAge > 0)
					expires = currentTimeMillis + maxAge;
			} catch (Exception e) { }
			try {
				String temp = response.getLastHeader("Expires").getValue();
				long millis = mDateHeaderFormat.parse(temp).getTime();
				if (millis > expires)
					expires = millis;
			} catch (Exception e) { }
			cacheData.setExpires(expires);
			cacheData.setLastModified(lastModified);
			return cacheData;
		}

		@Override
		protected CacheData doInBackground(Void... params) {
			final Uri uri = mUri;
			final String name = uri.toString();
			final DataCache dataCache = mStorageCache;

			try {
				CacheData cacheData = null;
				synchronized (dataCache) {
					cacheData = dataCache.getCache(name);
				}
				// Cache not exist or expired, download of refresh it.
				if (cacheData == null || cacheData.isExpired()) {
					cacheData = refreshHttpCacheData(name, cacheData);
					if (cacheData == null || cacheData.data() == null)
						return null;
					synchronized (dataCache) {						
						dataCache.setCache(name, cacheData);
					}
				}
				return cacheData;
			} catch (Throwable e) {
				return null;
			}
		}

		protected void onPostExecute(CacheData cacheData) {
			if (cacheData != null) {
				final byte[] data = cacheData.data();
				final String name = mUri.toString();
				Bitmap bitmap = BitmapFactory.decodeByteArray(
					data, 0, data.length, mMinSideLength, mMaxNumOfPixels
					);
				if (bitmap != null)
					cacheBitmap(name, bitmap, cacheData.expires());
				cacheData.setData(null);
				invokeCallbacks(bitmap);
			} else {
				invokeCallbacks(null);
			}
		}

		private void invokeCallbacks(Bitmap bitmap) {
			final ArrayList<Callback> callbacks = mCallbacks;
			for (Callback callback : callbacks)
				callback.onBitmapResloved(mUri, bitmap, true);
		}

		public void addCallback(Callback callback) {
			mCallbacks.add(callback);
		}

		public void removeCallback(Callback callback) {
			mCallbacks.remove(callback);
			if (!mCallbacks.isEmpty())
				return;
			cancel(false);
		}

	}

	public boolean resloveUri(Uri uri, Callback callback, boolean memoryCacheOnly) {
		// Do nothing if callback is null.
		if (callback == null)
			return false;

		// URI is null, just resolve as null.
		if (uri == null) {
			callback.onBitmapResloved(uri, null, false);
			return false;
		}

		final String name = uri.toString();
		final String scheme = uri.getScheme();
		Bitmap bitmap = null;

		// Attempt to find out bitmap object form memory cache.
		BitmapReference bitmapRef = mMemoryCache.get(name);
		if (bitmapRef != null && !bitmapRef.isExpired() && (bitmap = bitmapRef.get()) != null) {
			callback.onBitmapResloved(uri, bitmap, false);
			return true;
		}

		// If only resolve with memory cache, notify with null and return false.
		if (memoryCacheOnly) {
			callback.onBitmapResloved(uri, null, false);
			return false;
		}

		// HTTP URI.
		if (SCHEME_HTTP.equalsIgnoreCase(scheme)) {
			HttpResolveTask task = mHttpResolveTasks.get(name);
			if (task == null || task.isCancelled() || task.getStatus() == Status.FINISHED) {
				// No task for this URI is running, create a new one.
				task = new HttpResolveTask(uri);
				mHttpResolveTasks.put(name, task);
				task.addCallback(callback);
				task.executeOnExecutor(mAsyncTaskExecutor);
			} else {
				task.addCallback(callback);
			}
			return false;
		}

		// Local URI.
		try {
			if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
				InputStream istream = mContext.getContentResolver().openInputStream(uri);
				bitmap = BitmapFactory.decodeStream(istream, null, mMinSideLength, mMaxNumOfPixels);
			} else {
				String pathName = uri.toString();
				if (ContentResolver.SCHEME_FILE.equals(scheme))
					pathName = uri.getPath();
				InputStream istream = null;
				if (pathName.startsWith(ANDROID_ASSET))
					istream = mContext.getAssets().open(pathName.substring(ANDROID_ASSET.length()));
				else
					istream = new FileInputStream(pathName);
				bitmap = BitmapFactory.decodeStream(istream, null, mMinSideLength, mMaxNumOfPixels);
			}
		} catch (Exception e) {
			bitmap = null;
		}

		// Cache bitmap
		if (bitmap != null)
			cacheBitmap(name, bitmap, Long.MAX_VALUE);

		callback.onBitmapResloved(uri, bitmap, false);

		return (bitmap != null);
	}

	public void resloveUri(Uri uri, Callback callback) {
		resloveUri(uri, callback, false);
	}

	public void cancelResolveUri(Uri uri, Callback callback) {
		HttpResolveTask task = mHttpResolveTasks.get(uri.toString());
		if (task == null)
			return;
		task.removeCallback(callback);
	}

}
