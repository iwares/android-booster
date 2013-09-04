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
 * @file	src/com/iwares/lib/booster/support/SQLiteCache.java
 * @author	Eric.Tsai
 *
 */

package com.iwares.lib.booster.support;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class SQLiteCache implements DataCache {

	private static final int DATABASE_VERSION = 0x01000000;

	private static final String TABLE_CACHETABLE = "CacheTable";

	private static final String FIELD_RESNAME = "_ResName";

	private static final String FIELD_LAST_MODIFIED = "_LastModified";

	private static final String FIELD_EXPIRES = "_Expires";

	private static final String FIELD_RESDATA = "_ResData";

	private static final String SQL_CREATE_CACHETABLE = "" +
		"CREATE TABLE " + TABLE_CACHETABLE + "(" +
			FIELD_RESNAME +			" VARCHAR PRIMARY KEY," +
			FIELD_LAST_MODIFIED +	" INTEGER(64) NOT NULL," +
			FIELD_EXPIRES +			" INTEGER(64) NOT NULL," +
			FIELD_RESDATA +			" BLOB NOT NULL" +
			");" +
		"";

	private final SQLiteDatabase mDatabase;

	public SQLiteCache(String pathName) {
		SQLiteDatabase database = null;
		try {
			if (pathName == null) {
				database = SQLiteDatabase.create(null);
			} else {
				database = SQLiteDatabase.openDatabase(
					pathName, null,
					SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.CREATE_IF_NECESSARY
					);
			}
			int version = database.getVersion();
			if (version == 0)
				createCacheTable(database);
			database.setVersion(DATABASE_VERSION);
		} catch (Exception e) {
			database = null;
		}
		mDatabase = database;
	}

	private static void createCacheTable(SQLiteDatabase database) {
		database.execSQL(SQL_CREATE_CACHETABLE);
	}

	@Override
	public void setCache(String resName, CacheData cacheData) {
		final SQLiteDatabase database = mDatabase;
		if (database == null)
			return;
		try {
			ContentValues contentValues = new ContentValues(4);
			contentValues.put(FIELD_RESNAME, resName);
			contentValues.put(FIELD_RESDATA, cacheData.data());
			contentValues.put(FIELD_LAST_MODIFIED, cacheData.lastModified());
			contentValues.put(FIELD_EXPIRES, cacheData.expires());
			database.insertWithOnConflict(
				TABLE_CACHETABLE, null, contentValues,
				SQLiteDatabase.CONFLICT_REPLACE
				);
		} catch (Exception e) { }
	}

	@Override
	public CacheData getCache(String resName) {
		final SQLiteDatabase database = mDatabase;
		if (database == null)
			return null;
		try {
			final String[] colums = { FIELD_LAST_MODIFIED, FIELD_EXPIRES, FIELD_RESDATA };
			final String selection = FIELD_RESNAME + "=?";
			final String[] args = { resName };
			Cursor cursor = database.query(
				TABLE_CACHETABLE, colums, selection, args, null, null, null
				);
			CacheData cachedData = null;
			if (cursor != null) {
				cursor.moveToFirst();
				if (cursor.getCount() > 0) {
					long lastModified = cursor.getLong(0);
					long expires = cursor.getLong(1);
					byte[] data = cursor.getBlob(2);
					cachedData = new CacheData(lastModified, expires, data);
				}
				cursor.close();
			}
			return cachedData;
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public void deleteCache(String resName) {
		final SQLiteDatabase database = mDatabase;
		if (database == null)
			return;
		try {
			final String whereClause = FIELD_RESNAME + "=?";
			final String[] whereArgs = { resName };
			database.delete(TABLE_CACHETABLE, whereClause, whereArgs);
		} catch (Exception e) { }
	}

}
