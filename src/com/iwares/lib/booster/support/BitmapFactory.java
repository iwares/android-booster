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
 * @file	src/com/iwares/lib/booster/support/BitmapFactory.java
 * @author	Eric.Tsai
 *
 */

package com.iwares.lib.booster.support;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.TypedValue;

public class BitmapFactory {

	public static class Options extends android.graphics.BitmapFactory.Options {
	}

	public static final Bitmap decodeByteArray(byte[] data, int offset,
			int length) {
		return android.graphics.BitmapFactory.decodeByteArray(data, offset,
				length);
	}

	public static final Bitmap decodeByteArray(byte[] data, int offset,
			int length, Options opts) {
		return android.graphics.BitmapFactory.decodeByteArray(data, offset,
				length, opts);
	}

	public static final Bitmap decodeByteArray(byte[] data, int offset,
			int length, int minSideLength, int maxNumOfPixels) {
		Options options = new Options();
		options.inJustDecodeBounds = true;
		decodeByteArray(data, 0, data.length, options);
		int cx = options.outWidth, cy = options.outHeight;
		if (cx < 0 && cy < 0)
			return null;
		options.inSampleSize = computeSampleSize(options, minSideLength, maxNumOfPixels);
		options.inJustDecodeBounds = false;
		options.inDither = true;
		return decodeByteArray(data, offset, length, options);
	}

	public static final Bitmap decodeStream(InputStream istream) {
		return android.graphics.BitmapFactory.decodeStream(istream);
	}

	public static final Bitmap decodeStream(InputStream istream, Rect rect,
			Options opts) {
		return android.graphics.BitmapFactory.decodeStream(istream, rect, opts);
	}

	public static final Bitmap decodeStream(InputStream istream, Rect rect,
			int minSideLength, int maxNumOfPixels) {
		if (!istream.markSupported())
			return null;
		Options options = new Options();
		options.inJustDecodeBounds = true;
		istream.mark(1024 * 32);
		decodeStream(istream, null, options);
		int cx = options.outWidth, cy = options.outHeight;
		if (cx < 0 && cy < 0)
			return null;
		options.inSampleSize = computeSampleSize(options, minSideLength, maxNumOfPixels);
		options.inJustDecodeBounds = false;
		options.inDither = true;
		try {
			istream.reset();
		} catch (IOException e) {
			return null;
		}
		return decodeStream(istream, rect, options);
	}

	public static final Bitmap decodeFile(String pathName) {
		return android.graphics.BitmapFactory.decodeFile(pathName);
	}

	public static final Bitmap decodeFile(String pathName, Options opts) {
		return android.graphics.BitmapFactory.decodeFile(pathName, opts);
	}

	public static final Bitmap decodeFile(String pathName, int minSideLength, int maxNumOfPixels) {
		Options options = new Options();
		options.inJustDecodeBounds = true;
		decodeFile(pathName, options);
		int cx = options.outWidth, cy = options.outHeight;
		if (cx < 0 && cy < 0)
			return null;
		options.inSampleSize = computeSampleSize(options, minSideLength, maxNumOfPixels);
		options.inJustDecodeBounds = false;
		options.inDither = true;
		return decodeFile(pathName, options);
	}

	public static final Bitmap decodeFileDescriptor(FileDescriptor fd) {
		return android.graphics.BitmapFactory.decodeFileDescriptor(fd);
	}

	public static final Bitmap decodeFileDescriptor(FileDescriptor fd,
			Rect rect, Options opts) {
		return android.graphics.BitmapFactory.decodeFileDescriptor(fd, rect,
				opts);
	}

	public static final Bitmap decodeResource(Resources res, int id) {
		return android.graphics.BitmapFactory.decodeResource(res, id);
	}

	public static final Bitmap decodeResource(Resources res, int id,
			Options opts) {
		return android.graphics.BitmapFactory.decodeResource(res, id, opts);
	}

	public static final Bitmap decodeResource(Resources res, int id,
			int minSideLength, int maxNumOfPixels) {
		Options options = new Options();
		options.inJustDecodeBounds = true;
		decodeResource(res, id, options);
		int cx = options.outWidth, cy = options.outHeight;
		if (cx < 0 && cy < 0)
			return null;
		options.inSampleSize = computeSampleSize(options, minSideLength, maxNumOfPixels);
		options.inJustDecodeBounds = false;
		options.inDither = true;
		return decodeResource(res, id, options);
	}

	public static final Bitmap decodeResourceStream(Resources res,
			TypedValue value, InputStream is, Rect pad, Options opts) {
		return android.graphics.BitmapFactory.decodeResourceStream(res, value,
				is, pad, opts);
	}

	public static int computeSampleSize(Options options, int minSideLength,
			int maxNumOfPixels) {
		int initialSize = computeInitialSampleSize(options, minSideLength,
				maxNumOfPixels);

		int roundedSize;
		if (initialSize <= 8) {
			roundedSize = 1;
			while (roundedSize < initialSize) {
				roundedSize <<= 1;
			}
		} else {
			roundedSize = (initialSize + 7) / 8 * 8;
		}
		return roundedSize;
	}

	public static int computeInitialSampleSize(Options options,
			int minSideLength, int maxNumOfPixels) {
		double w = options.outWidth;
		double h = options.outHeight;
		int lowerBound = (maxNumOfPixels == -1) ? 1 : (int) Math.ceil(Math
				.sqrt(w * h / maxNumOfPixels));
		int upperBound = (minSideLength == -1) ? 128 : (int) Math.min(
				Math.floor(w / minSideLength), Math.floor(h / minSideLength));

		if (upperBound < lowerBound) {
			// return the larger one when there is no overlapping zone.
			return lowerBound;
		}

		if ((maxNumOfPixels == -1) && (minSideLength == -1)) {
			return 1;
		} else if (minSideLength == -1) {
			return lowerBound;
		} else {
			return upperBound;
		}
	}

}
