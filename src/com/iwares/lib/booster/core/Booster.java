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
 * @file	src/com/iwares/lib/booster/core/Booster.java
 * @author	Eric.Tsai
 *
 */

package com.iwares.lib.booster.core;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import android.accounts.AccountManager;
import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.DownloadManager;
import android.app.KeyguardManager;
import android.app.NotificationManager;
import android.app.SearchManager;
import android.app.UiModeManager;
import android.app.admin.DevicePolicyManager;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.hardware.SensorManager;
import android.hardware.display.DisplayManager;
import android.hardware.input.InputManager;
import android.hardware.usb.UsbManager;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.MediaRouter;
import android.net.ConnectivityManager;
import android.net.nsd.NsdManager;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.nfc.NfcManager;
import android.os.Build;
import android.os.DropBoxManager;
import android.os.PowerManager;
import android.os.UserManager;
import android.os.Vibrator;
import android.os.storage.StorageManager;
import android.service.wallpaper.WallpaperService;
import android.telephony.TelephonyManager;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityManager;
import android.view.inputmethod.InputMethodManager;
import android.view.textservice.TextServicesManager;
import android.widget.AdapterView;

import com.iwares.lib.booster.annotation.IntentExtra;
import com.iwares.lib.booster.annotation.OnClick;
import com.iwares.lib.booster.annotation.OnFocusChanged;
import com.iwares.lib.booster.annotation.OnItemClick;
import com.iwares.lib.booster.annotation.OnItemLongClick;
import com.iwares.lib.booster.annotation.OnItemSelected;
import com.iwares.lib.booster.annotation.OnLongClick;
import com.iwares.lib.booster.annotation.OnTouch;
import com.iwares.lib.booster.annotation.SystemService;
import com.iwares.lib.booster.annotation.ViewById;
import com.iwares.lib.booster.annotation.ViewFromLayout;

public class Booster {

	/**
	 * This method inject all fields with {@link ViewById} annotation in 'target' with
	 * corresponding {@link View}s find from 'source'. To make this method working
	 * correctly, the 'source' must provides a {@code void findViewById(int id)} method.
	 * 
	 * @param target An object who's fields will be injected.
	 * @param source An object which contains corresponding {@link View}s.
	 * 
	 * @see {@link ViewById}
	 * 
	 */
	public static final void injectViews(Object target, Object source) {
		try {
			// Prepare findViewById method for injecting @ViewById fields.
			Method findViewById = null;
			findViewById = source.getClass().getMethod("findViewById", int.class);

			// Get all fields form target object.
			Field[] fields = target.getClass().getDeclaredFields();
			for (int i = 0, c = fields.length; i < c; ++i) {
				Field field = fields[i];
				// Process @ViewById annotation.
				if (findViewById != null && field.isAnnotationPresent(ViewById.class)) {
					int id = field.getAnnotation(ViewById.class).value();
					View view = (View)findViewById.invoke(source, id);
					boolean isAccessible = field.isAccessible();
					field.setAccessible(true);
					field.set(target, view);
					field.setAccessible(isAccessible);
				}
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to inject Views", e);
		}
	}

	/**
	 * Is equivalent to calling {@link #injectViews(object, object)}
	 * 
	 * @param object The object which wants to inject {@link View}s to.
	 * 
	 * @see {@link #injectViews(Object target, Object source)}
	 * @see {@link ViewById}
	 * 
	 */
	public static final void injectViews(Object object) {
		injectViews(object, object);
	}

	/**
	 * This method inflates {@link View}s according to {@link ViewFormLayout}
	 * annotations of 'object' and assigns them to corresponding fields. To make this
	 * method working correctly, the 'source' object must be a Context or provides a
	 * {@code Context getContext()} method.
	 * 
	 * @param target An object who's fields will be set to inflated {@link View}s.
	 * @param source An {@link Context} object or an an object which provides a
	 *        {@code Context getContext()} method.
	 * 
	 * @see {@link ViewFromLayout}
	 * 
	 */
	public static final void inflateLayouts(Object target, Object source) {
		try {
			// Prepare Context object for inflating @ViewFromLayout fields.
			Context context = null;
			try {
				Method getContext = source.getClass().getMethod("getContext");
				Object result = getContext.invoke(source);
				if (result == null || !(result instanceof Context))
					throw new NullPointerException();
				context = (Context)result;
			} catch (Exception e) {
				if (source instanceof Context)
					context = (Context)source;
			}
			if (context == null)
				throw new RuntimeException("Context not found.");

			// Get all fields form target object.
			Field[] fields = target.getClass().getDeclaredFields();
			for (int i = 0, c = fields.length; i < c; ++i) {
				Field field = fields[i];
				// Process @ViewFromLayout annotation.
				if (field.isAnnotationPresent(ViewFromLayout.class)) {
					int id = field.getAnnotation(ViewFromLayout.class).value();
					View view = View.inflate(context, id, null);
					boolean isAccessible = field.isAccessible();
					field.setAccessible(true);
					field.set(target, view);
					field.setAccessible(isAccessible);
				}
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to inflate layouts", e);
		}
	}

	/**
	 * Is equivalent to calling {@link #inflateLayouts(object, object)}
	 * 
	 * @param object The object which wants to inflate layouts to.
	 * 
	 * @see {@link #inflateLayouts(Object target, Object source)}
	 * @see {@link ViewFromLayout}
	 * 
	 */
	public static final void inflateLayouts(Object object) {
		inflateLayouts(object, object);
	}

	private static class ViewOnClickListener implements View.OnClickListener {

		public final WeakReference<Object> mReceiverRef;

		public final Method mMethod;

		public ViewOnClickListener(Object receiver, Method method) {
			mReceiverRef = new WeakReference<Object>(receiver);
			mMethod = method;
		}

		@Override
		public void onClick(View v) {
			try {
				Object receiver = mReceiverRef.get();
				if (receiver == null)
					return;
				Method method = mMethod;
				boolean isAccessible = method.isAccessible();
				method.setAccessible(true);
				method.invoke(receiver, v);
				method.setAccessible(isAccessible);
			} catch (Exception e) {
				throw new RuntimeException("Faild to invoke" + mMethod.getName(), e);
			}
		}

	}

	private static class ViewOnLongClickListener implements View.OnLongClickListener {

		public final WeakReference<Object> mReceiverRef;

		public final Method mMethod;

		public ViewOnLongClickListener(Object receiver, Method method) {
			mReceiverRef = new WeakReference<Object>(receiver);
			mMethod = method;
		}

		@Override
		public boolean onLongClick(View v) {
			try {
				Object receiver = mReceiverRef.get();
				if (receiver == null)
					return false;
				Method method = mMethod;
				boolean isAccessible = method.isAccessible();
				method.setAccessible(true);
				boolean result = (Boolean)mMethod.invoke(receiver, v);
				method.setAccessible(isAccessible);
				return result;
			} catch (Exception e) {
				throw new RuntimeException("Faild to invoke" + mMethod.getName(), e);
			}
		}

	}

	private static class AdapterViewOnItemClickListener implements AdapterView.OnItemClickListener {

		public final WeakReference<Object> mReceiverRef;

		public final Method mMethod;

		public AdapterViewOnItemClickListener(Object receiver, Method method) {
			mReceiverRef = new WeakReference<Object>(receiver);
			mMethod = method;
		}

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			try {
				Object receiver = mReceiverRef.get();
				if (receiver == null)
					return;
				Method method = mMethod;
				boolean isAccessible = method.isAccessible();
				method.setAccessible(true);
				mMethod.invoke(receiver, parent, view, position, id);
				method.setAccessible(isAccessible);
			} catch (Exception e) {
				throw new RuntimeException("Faild to invoke" + mMethod.getName(), e);
			}
		}

	}

	private static class AdapterViewOnItemLongClickListener implements AdapterView.OnItemLongClickListener {

		public final WeakReference<Object> mReceiverRef;

		public final Method mMethod;

		public AdapterViewOnItemLongClickListener(Object receiver, Method method) {
			mReceiverRef = new WeakReference<Object>(receiver);
			mMethod = method;
		}

		@Override
		public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
			try {
				Object receiver = mReceiverRef.get();
				if (receiver == null)
					return false;
				Method method = mMethod;
				boolean isAccessible = method.isAccessible();
				method.setAccessible(true);
				boolean result = (Boolean)mMethod.invoke(receiver, parent, view, position, id);
				method.setAccessible(isAccessible);
				return result;
			} catch (Exception e) {
				throw new RuntimeException("Faild to invoke" + mMethod.getName(), e);
			}
		}

	}

	private static class AdapterViewOnItemSelectedListener implements AdapterView.OnItemSelectedListener {

		public final WeakReference<Object> mReceiverRef;

		public final Method mMethod;

		public AdapterViewOnItemSelectedListener(Object receiver, Method method) {
			mReceiverRef = new WeakReference<Object>(receiver);
			mMethod = method;
		}

		@Override
		public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
			try {
				Object receiver = mReceiverRef.get();
				if (receiver == null)
					return;
				Method method = mMethod;
				boolean isAccessible = method.isAccessible();
				method.setAccessible(true);
				mMethod.invoke(receiver, parent, view, position, id);
				method.setAccessible(isAccessible);
			} catch (Exception e) {
				throw new RuntimeException("Faild to invoke" + mMethod.getName(), e);
			}
		}

		@Override
		public void onNothingSelected(AdapterView<?> parent) {
			onItemSelected(parent, null, 0, 0);
		}

	}

	private static class ViewOnTouchListener implements View.OnTouchListener {

		public final WeakReference<Object> mReceiverRef;

		public final Method mMethod;

		public ViewOnTouchListener(Object receiver, Method method) {
			mReceiverRef = new WeakReference<Object>(receiver);
			mMethod = method;
		}

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			try {
				Object receiver = mReceiverRef.get();
				if (receiver == null)
					return false;
				Method method = mMethod;
				boolean isAccessible = method.isAccessible();
				method.setAccessible(true);
				boolean result = (Boolean)mMethod.invoke(receiver, v, event);
				method.setAccessible(isAccessible);
				return result;
			} catch (Exception e) {
				throw new RuntimeException("Faild to invoke" + mMethod.getName(), e);
			}
		}

	}

	private static class ViewOnFocusChangedListener implements View.OnFocusChangeListener {

		public final WeakReference<Object> mReceiverRef;

		public final Method mMethod;

		public ViewOnFocusChangedListener(Object receiver, Method method) {
			mReceiverRef = new WeakReference<Object>(receiver);
			mMethod = method;
		}

		@Override
		public void onFocusChange(View v, boolean hasFocus) {
			try {
				Object receiver = mReceiverRef.get();
				if (receiver == null)
					return;
				Method method = mMethod;
				boolean isAccessible = method.isAccessible();
				method.setAccessible(true);
				method.invoke(receiver, v, hasFocus);
				method.setAccessible(isAccessible);
			} catch (Exception e) {
				throw new RuntimeException("Faild to invoke" + mMethod.getName(), e);
			}
		}

	}

	/**
	 * This method registers all annotated methods of 'object' to corresponding
	 * {@link View}s that find form 'target'. To make this method working correctly,
	 * the 'source' must provides a {@code void findViewById(int id)} method.
	 * 
	 * @param target An object which contains annotated methods.
	 * @param source An object which contains corresponding {@link View}s.
	 * 
	 */
	public static final void registerListeners(Object target, Object source) {
		try {
			// Prepare findViewById method for register listeners
			Method findViewById = source.getClass().getMethod("findViewById", int.class);
			Method[] methods = target.getClass().getDeclaredMethods();
			for (int i = 0, c = methods.length; i < c; ++i) {
				Method method = methods[i];
				// Register View.OnClickListener
				if (method.isAnnotationPresent(OnClick.class)) {
					int[] ids = method.getAnnotation(OnClick.class).value();
					for (int j = 0, d = ids.length; j < d; ++j) {
						View view = (View)findViewById.invoke(source, ids[j]);
						ViewOnClickListener listener = new ViewOnClickListener(target, method);
						view.setOnClickListener(listener);
					}
				}
				// Register View.OnLongClickListener
				if (method.isAnnotationPresent(OnLongClick.class)) {
					int[] ids = method.getAnnotation(OnLongClick.class).value();
					for (int j = 0, d = ids.length; j < d; ++j) {
						View view = (View)findViewById.invoke(source, ids[j]);
						ViewOnLongClickListener listener = new ViewOnLongClickListener(target, method);
						view.setOnLongClickListener(listener);
					}
				}
				// Register AdapterView.OnItemClickListener
				if (method.isAnnotationPresent(OnItemClick.class)) {
					int[] ids = method.getAnnotation(OnItemClick.class).value();
					for (int j = 0, d = ids.length; j < d; ++j) {
						AdapterView<?> view = (AdapterView<?>)findViewById.invoke(source, ids[j]);
						AdapterViewOnItemClickListener listener = new AdapterViewOnItemClickListener(target, method);
						view.setOnItemClickListener(listener);
					}
				}
				// Register AdapterView.OnItemLongClickListener
				if (method.isAnnotationPresent(OnItemLongClick.class)) {
					int[] ids = method.getAnnotation(OnItemLongClick.class).value();
					for (int j = 0, d = ids.length; j < d; ++j) {
						AdapterView<?> view = (AdapterView<?>)findViewById.invoke(source, ids[j]);
						AdapterViewOnItemLongClickListener listener = new AdapterViewOnItemLongClickListener(target, method);
						view.setOnItemLongClickListener(listener);
					}
				}
				// Register AdapterView.OnItemSelectedListener
				if (method.isAnnotationPresent(OnItemSelected.class)) {
					int[] ids = method.getAnnotation(OnItemSelected.class).value();
					for (int j = 0, d = ids.length; j < d; ++j) {
						AdapterView<?> view = (AdapterView<?>)findViewById.invoke(source, ids[j]);
						AdapterViewOnItemSelectedListener listener = new AdapterViewOnItemSelectedListener(target, method);
						view.setOnItemSelectedListener(listener);
					}
				}
				// Register View.OnTouchListener
				if (method.isAnnotationPresent(OnTouch.class)) {
					int[] ids = method.getAnnotation(OnTouch.class).value();
					for (int j = 0, d = ids.length; j < d; ++j) {
						View view = (View)findViewById.invoke(source, ids[j]);
						ViewOnTouchListener listener = new ViewOnTouchListener(target, method);
						view.setOnTouchListener(listener);
					}
				}
				// Register View.OnFocusChangedListener
				if (method.isAnnotationPresent(OnFocusChanged.class)) {
					int[] ids = method.getAnnotation(OnFocusChanged.class).value();
					for (int j = 0, d = ids.length; j < d; ++j) {
						View view = (View)findViewById.invoke(source, ids[j]);
						ViewOnFocusChangedListener listener = new ViewOnFocusChangedListener(target, method);
						view.setOnFocusChangeListener(listener);
					}
				}
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to register listeners", e);
		}
	}

	/**
	 * Is equivalent to calling {@link #registerListeners(object, object)}
	 * 
	 * @param object The object which wants to register listeners to.
	 * 
	 * @see {@link #registerListeners(Object target, Object source)}
	 * 
	 */
	public static final void registerListeners(Object object) {
		registerListeners(object, object);
	}

	/**
	 * This method will initial all fields with {@link SystemService} annotation in
	 * 'object' with corresponding system service object. To make this method working
	 * correctly, the 'source' object must be a Context or provides a
	 * {@code Context getContext()} method.
	 * 
	 * @param target An object who's fields will be initialized.
	 * @param source An {@link Context} object or an an object which provides a
	 *        {@code Context getContext()} method.
	 * 
	 * @see {@link SystemService}
	 * 
	 */
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
	@SuppressWarnings("deprecation")
	public static final void bindSystemServices(Object target, Object source) {
		try {
			// Prepare Context object for binding @SystemService fields.
			Context context = null;
			try {
				Method getContext = source.getClass().getMethod("getContext");
				Object result = getContext.invoke(source);
				if (result == null || !(result instanceof Context))
					throw new NullPointerException();
				context = (Context)result;
			} catch (Exception e) {
				if (source instanceof Context)
					context = (Context)source;
			}
			if (context == null)
				throw new RuntimeException("Context not found.");

			// Get all fields form target.
			Field[] fields = target.getClass().getDeclaredFields();
			for (int i = 0, c = fields.length; i < c; ++i) {
				Field field = fields[i];
				if (!field.isAnnotationPresent(SystemService.class))
					continue;
				boolean isAccessible = field.isAccessible();
				field.setAccessible(true);
				Class<?> clazz = field.getType();
				if (android.text.ClipboardManager.class.equals(clazz)) {
					field.set(target, context.getSystemService(Context.CLIPBOARD_SERVICE));
				} else if (WindowManager.class.equals(clazz)) {
					field.set(target, context.getSystemService(Context.WINDOW_SERVICE));
				} else if (LayoutInflater.class.equals(clazz)) {
					field.set(target, context.getSystemService(Context.LAYOUT_INFLATER_SERVICE));
				} else if (ActivityManager.class.equals(clazz)) {
					field.set(target, context.getSystemService(Context.ACTIVITY_SERVICE));
				} else if (PowerManager.class.equals(clazz)) {
					field.set(target, context.getSystemService(Context.POWER_SERVICE));
				} else if (AlarmManager.class.equals(clazz)) {
					field.set(target, context.getSystemService(Context.ALARM_SERVICE));
				} else if (NotificationManager.class.equals(clazz)) {
					field.set(target, context.getSystemService(Context.NOTIFICATION_SERVICE));
				} else if (KeyguardManager.class.equals(clazz)) {
					field.set(target, context.getSystemService(Context.KEYGUARD_SERVICE));
				} else if (LocationManager.class.equals(clazz)) {
					field.set(target, context.getSystemService(Context.LOCATION_SERVICE));
				} else if (SearchManager.class.equals(clazz)) {
					field.set(target, context.getSystemService(Context.SEARCH_SERVICE));
				} else if (SensorManager.class.equals(clazz)) {
					field.set(target, context.getSystemService(Context.SENSOR_SERVICE));
				} else if (Vibrator.class.equals(clazz)) {
					field.set(target, context.getSystemService(Context.VIBRATOR_SERVICE));
				} else if (ConnectivityManager.class.equals(clazz)) {
					field.set(target, context.getSystemService(Context.CONNECTIVITY_SERVICE));
				} else if (WifiManager.class.equals(clazz)) {
					field.set(target, context.getSystemService(Context.WIFI_SERVICE));
				} else if (AudioManager.class.equals(clazz)) {
					field.set(target, context.getSystemService(Context.AUDIO_SERVICE));
				} else if (TelephonyManager.class.equals(clazz)) {
					field.set(target, context.getSystemService(Context.TELEPHONY_SERVICE));
				} else if (InputMethodManager.class.equals(clazz)) {
					field.set(target, context.getSystemService(Context.INPUT_METHOD_SERVICE));
				} else if (AccessibilityManager.class.equals(clazz)) {
					field.set(target, context.getSystemService(Context.ACCESSIBILITY_SERVICE));
				} else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ECLAIR) {
					throw new RuntimeException("Corresponding system service not found.");
				} else if (AccountManager.class.equals(clazz)) {
					field.set(target, context.getSystemService(Context.ACCOUNT_SERVICE));
				} else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ECLAIR_MR1) {
					throw new RuntimeException("Corresponding system service not found.");
				} else if (WallpaperService.class.equals(clazz)) {
					field.set(target, context.getSystemService(Context.WALLPAPER_SERVICE));
				} else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) {
					throw new RuntimeException("Corresponding system service not found.");
				} else if (UiModeManager.class.equals(clazz)) {
					field.set(target, context.getSystemService(Context.UI_MODE_SERVICE));
				} else if (DropBoxManager.class.equals(clazz)) {
					field.set(target, context.getSystemService(Context.DROPBOX_SERVICE));
				} else if (DevicePolicyManager.class.equals(clazz)) {
					field.set(target, context.getSystemService(Context.DEVICE_POLICY_SERVICE));
				} else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD) {
					throw new RuntimeException("Corresponding system service not found.");
				} else if (StorageManager.class.equals(clazz)) {
					field.set(target, context.getSystemService(Context.STORAGE_SERVICE));
				} else if (DownloadManager.class.equals(clazz)) {
					field.set(target, context.getSystemService(Context.DOWNLOAD_SERVICE));
				} else if (NfcManager.class.equals(clazz)) {
					field.set(target, context.getSystemService(Context.NFC_SERVICE));
				} else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
					throw new RuntimeException("Corresponding system service not found.");
				} else if (ClipboardManager.class.equals(clazz)) {
					field.set(target, context.getSystemService(Context.CLIPBOARD_SERVICE));
				} else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB_MR1) {
					throw new RuntimeException("Corresponding system service not found.");
				} else if (UsbManager.class.equals(clazz)) {
					field.set(target, context.getSystemService(Context.USB_SERVICE));
				} else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB_MR2) {
					throw new RuntimeException("Corresponding system service not found.");
				} else if (WifiP2pManager.class.equals(clazz)) {
					field.set(target, context.getSystemService(Context.WIFI_P2P_SERVICE));
				} else if (TextServicesManager.class.equals(clazz)) {
					field.set(target, context.getSystemService(Context.TEXT_SERVICES_MANAGER_SERVICE));
				} else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
					throw new RuntimeException("Corresponding system service not found.");
				} else if (NsdManager.class.equals(clazz)) {
					field.set(target, context.getSystemService(Context.NSD_SERVICE));
				} else if (MediaRouter.class.equals(clazz)) {
					field.set(target, context.getSystemService(Context.MEDIA_ROUTER_SERVICE));
				} else if (InputManager.class.equals(clazz)) {
					field.set(target, context.getSystemService(Context.INPUT_SERVICE));
				} else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
					throw new RuntimeException("Corresponding system service not found.");
				} else if (DisplayManager.class.equals(clazz)) {
					field.set(target, context.getSystemService(Context.DISPLAY_SERVICE));
				} else if (UserManager.class.equals(clazz)) {
					field.set(target, context.getSystemService(Context.USER_SERVICE));
				} else {
					throw new RuntimeException("Corresponding system service not found.");
				}
				field.setAccessible(isAccessible);
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to bind system services", e);
		}
	}

	/**
	 * Is equivalent to calling {@link #bindSystemServices(object, object)}
	 * 
	 * @param object The object which wants to bind system services to.
	 * 
	 * @see {@link #bindSystemServices(Object target, Object source)}
	 * @see {@link SystemService}
	 * 
	 */
	public static final void bindSystemServices(Object object) {
		bindSystemServices(object, object);
	}

	public static final void obtainIntentExtras(Object target, Object source) {
		try {
			// Prepare Intent object for injecting @IntentExtra fields.
			Intent intent = null;
			try {
				Method getContext = source.getClass().getMethod("getIntent");
				Object result = getContext.invoke(source);
				if (result == null || !(result instanceof Intent))
					throw new NullPointerException();
				intent = (Intent)result;
			} catch (Exception e) {
				if (source instanceof Intent)
					intent = (Intent)source;
			}
			if (intent == null)
				throw new RuntimeException("Intent not found.");
	
			// Get all fields form target object.
			Field[] fields = target.getClass().getDeclaredFields();
			for (int i = 0, c = fields.length; i < c; ++i) {
				Field field = fields[i];
				// Process @IntentExtra annotation.
				if (!field.isAnnotationPresent(IntentExtra.class))
					continue;
				String name = field.getAnnotation(IntentExtra.class).value();
				if (name.length() == 0)
					name = field.getName();
				boolean isAccessible = field.isAccessible();
				field.setAccessible(true);
				Class<?> clazz = field.getType();
				if (clazz.equals(boolean.class)) {
					boolean value = field.getBoolean(target);
					field.setBoolean(target, intent.getBooleanExtra(name, value));
				} else if (clazz.equals(boolean[].class)) {
					boolean[] value = intent.getBooleanArrayExtra(name);
					if (value != null) field.set(target, value);
				} else if (clazz.equals(byte.class)) {
					byte value = field.getByte(target);
					field.setByte(target, intent.getByteExtra(name, value));
				} else if (clazz.equals(byte[].class)) {
					byte[] value = intent.getByteArrayExtra(name);
					if (value != null) field.set(target, value);
				} else if (clazz.equals(char.class)) {
					char value = field.getChar(target);
					field.setChar(target, intent.getCharExtra(name, value));
				} else if (clazz.equals(char[].class)) {
					char[] value = intent.getCharArrayExtra(name);
					if (value != null) field.set(target, value);
				} else if (clazz.equals(short.class)) {
					short value = field.getShort(target);
					field.setShort(target, intent.getShortExtra(name, value));
				} else if (clazz.equals(short[].class)) {
					short[] value = intent.getShortArrayExtra(name);
					if (value != null) field.set(target, value);
				} else if (clazz.equals(int.class)) {
					int value = field.getInt(target);
					field.setInt(target, intent.getIntExtra(name, value));
				} else if (clazz.equals(int[].class)) {
					int[] value = intent.getIntArrayExtra(name);
					if (value != null) field.set(target, value);
				} else if (clazz.equals(long.class)) {
					long value = field.getLong(target);
					field.setLong(target, intent.getLongExtra(name, value));
				} else if (clazz.equals(long[].class)) {
					long[] value = intent.getLongArrayExtra(name);
					if (value != null) field.set(target, value);
				} else if (clazz.equals(float.class)) {
					float value = field.getFloat(target);
					field.setFloat(target, intent.getFloatExtra(name, value));
				} else if (clazz.equals(float[].class)) {
					float[] value = intent.getFloatArrayExtra(name);
					if (value != null) field.set(target, value);
				} else if (clazz.equals(double.class)) {
					double value = field.getDouble(target);
					field.setDouble(target, intent.getDoubleExtra(name, value));
				} else if (clazz.equals(double[].class)) {
					double[] value = intent.getDoubleArrayExtra(name);
					if (value != null) field.set(target, value);
				} else if (clazz.equals(String.class)) {
					String value = intent.getStringExtra(name);
					if (value != null) field.set(target, value);
				} else if (clazz.equals(String[].class)) {
					String[] value = intent.getStringArrayExtra(name);
					if (value != null) field.set(target, value);
				} else {
					throw new RuntimeException("Unexpected type: " + clazz.getName());
				}
				field.setAccessible(isAccessible);
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to inject intent extras", e);
		}
	}

	public static final void obtainIntentExtras(Object object) {
		obtainIntentExtras(object, object);
	}

}
