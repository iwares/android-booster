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
 * @file	src/com/iwares/lib/booster/support/DataCache.java
 * @author	Eric.Tsai
 *
 */

package com.iwares.lib.booster.support;

public interface DataCache {

	public static class CacheData {

		private long mLastModified, mExpires;

		private byte[] mData;

		public CacheData() {
			mLastModified = mExpires = 0;
			mData = null;
		}

		public CacheData(long lastModified, long expires, byte[] data) {
			mLastModified = lastModified;
			mExpires = expires;
			mData = data;
		}

		public long lastModified() {
			return mLastModified;
		}

		public void setLastModified(long lastModified) {
			mLastModified = lastModified;
		}

		public long expires() {
			return mExpires;
		}

		public void setExpires(long expires) {
			mExpires = expires;
		}

		public byte[] data() {
			return mData;
		}

		public void setData(byte[] data) {
			mData = data;
		}

		public boolean isExpired() {
			return mExpires <= System.currentTimeMillis();
		}

	}

	public void setCache(String resName, CacheData cacheData);

	public CacheData getCache(String resName);

	public void deleteCache(String resName);

}
