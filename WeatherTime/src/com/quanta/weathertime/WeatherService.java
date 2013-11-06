package com.quanta.weathertime;

import java.io.IOException;
import java.io.StringReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Process;
import android.os.SystemClock;
import android.util.Log;
import android.util.Xml;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.quanta.weathertime.location.Coordinates;
import com.quanta.weathertime.location.LocationGetter;

public class WeatherService extends Service implements Runnable {
	public static final String TAG = "WeatherService";
	private boolean debug = false;
//	public static final String SETTING_INFOS_TEMPERATURE_UNIT = "SETTING_Infos_Temperature_unit";
	private static final String ACTION_UPDATE_WEATHER = "com.quanta.weather.ACTION_UPDATE_WEATHER";
	
	public static final String SAVE_DETAIL_CITY_CODE_DB = "save_detail_city_code_db";
	
	public static final String SETTING_INFOS = "SETTING_Infos";
	private int mUpdateIntervalInSeconds;
	private final String AUTO_UPDATE_INTERVAL_IN_SECONDS = "Auto-Refresh Interval";
	private final String DEF_UPDATE_INTERVAL_IN_SECONDS = "3600";
	private static final int TYPE_QUERY_ALL = 0;
	private static final int TYPE_QUERY_NONE = 1;
	private static final int TYPE_QUERY_ONE = 2;
	private int mQueryType;
	
	public static final String REQUERY_ALL = "REQUERY_ALL";
	public static final String UPDATE_INTERVAL_CHANGED = "UPDATE_INTERVAL_CHANGED";
	public static final String REQUERY_SPECIFIED_CITY  = "REQUERY_SPECIFIED_CITY";
	
	private Handler mHandler;
	private Looper mlooper;
	RequestQueue queue = null;
	
	public void onCreate() {
		getUpdateInterval();
		HandlerThread mHandlerThread = new HandlerThread("weather_service_thread",Process.THREAD_PRIORITY_BACKGROUND);
		mHandlerThread.start();
		mlooper = mHandlerThread.getLooper();
		mHandler = new Handler(mlooper);
		queue = Volley.newRequestQueue(this);
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_TIME_TICK);
		this.registerReceiver(mDateChangedReceiver, filter);
	}
	
	private int mWidgetID;
	private String cityname;
	private String citydetal;
	private String cityidString;
	private String language;
	private String unit;
	private String showin;
	private String current;
	private String currentAdmin;
	private boolean isList = false;
	@SuppressWarnings("deprecation")
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		Log.e(TAG, "onStart");
		Bundle arguments = null;
		if (intent == null) {
			if (debug) {
				Log.e(TAG, "intent is null");
			}
		}else {
			arguments = intent.getExtras();
			mWidgetID = arguments.getInt("appwidget_id");
			cityname = arguments.getString("city_name");
			citydetal = arguments.getString("city_detail");
			cityidString = arguments.getString("city_id");
			showin = arguments.getString("show_in");
			current = arguments.getString("current");
			currentAdmin = arguments.getString("current_admin");
			isList = arguments.getBoolean("city_list", false);
			if (debug) {
				Log.e(TAG, "onStart "+current+" "+currentAdmin);
			}
		}

		SharedPreferences mSettings = getSharedPreferences(SETTING_INFOS, 0);
		unit = mSettings.getString("Temperature Unit", "c");
		if (arguments != null) {
			//city_name_from_weather_detail = arguments.getString("WeatherDetailCityName");
			if (arguments.containsKey(REQUERY_ALL)) {
				if (debug) {
					Log.v(TAG,"arguments.containsKey(REQUERY_ALL)");
				}
				mQueryType = TYPE_QUERY_ALL;
				mHandler.post(this);
				reschedule();
			}else if (arguments.containsKey(REQUERY_SPECIFIED_CITY)) {
				if (debug) {
					Log.v(TAG,"arguments.containsKey(REQUERY_SPECIFIED_CITY)");
				}
				mQueryType = TYPE_QUERY_ONE;
				mHandler.post(this);
			}
			else if (arguments.containsKey(UPDATE_INTERVAL_CHANGED)) {
				getUpdateInterval();
				reschedule();
			}
		}
    }

	void getUpdateInterval() {
		SharedPreferences pref = getSharedPreferences(SETTING_INFOS, 0);
		mUpdateIntervalInSeconds = Integer.parseInt(pref.getString(AUTO_UPDATE_INTERVAL_IN_SECONDS, DEF_UPDATE_INTERVAL_IN_SECONDS));
		if (mUpdateIntervalInSeconds > 0) {
			mQueryType = TYPE_QUERY_ALL;
		}
		else {
			mQueryType = TYPE_QUERY_NONE;
		}
	}

	@SuppressWarnings("unused")
	private String getIconName(String path){
		String[] str = path.split("/");
		int len = str.length;
		String temp = str[len-1];
		String name = temp.substring(0,temp.indexOf("."));
		return name;
	}

	private final BroadcastReceiver mDateChangedReceiver = new BroadcastReceiver(){
		
		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			if (intent.getAction().equals(Intent.ACTION_TIME_TICK)) {
				ArrayList<CityItemInfo> cityList = getItemInfoList();
				if (cityList != null) {
					for (int i = 0; i < cityList.size(); i++) {
						Cursor cursor = getContentResolver().query(WeatherContentProvider.CONTENT_URI,
								new String[] { WeatherColumns.WeatherInfo.CITY_LOCAL_TIME },
								WeatherColumns.WeatherInfo.CITY_INDEX + "=" + cityList.get(i).cityindex,
								null, null);
						String localtime = null;
						if (cursor != null && cursor.moveToFirst()) {
							localtime = cursor.getString(cursor.getColumnIndex(WeatherColumns.WeatherInfo.CITY_LOCAL_TIME));
						}
						cursor.close();
						if (localtime != null && localtime.length() != 0) {  //&& !localtime.isEmpty()
							if (debug) {
								Log.i(TAG, "localtime = " + localtime.substring(0,10));
							}
							String year_Month = localtime.substring(0, 10);
							localtime = localtime.substring(11);
							String hourTemp = localtime.substring(0,2);
							String minuteTemp = localtime.substring(3,5);
							int hour = Integer.parseInt(hourTemp);
							int minute  =  Integer.parseInt(minuteTemp);
							String mtimer;
							if(minute >58 && hour < 23){
								hour = hour + 1;
							}else if(hour > 22 && minute > 58){
								hour = 0;
							}
							if(minute < 59){
								minute = minute + 1;
							}else{
								minute = 0;
							}
							String mhour = String.valueOf(hour);
							String mminute = String.valueOf(minute);
							mtimer = mhour + ":" + mminute;
							SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
							java.util.Date date = null;
							try {
								date = sdf.parse(mtimer);
							} catch (ParseException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							String timeShow = date.toString();
							timeShow = timeShow.substring(11,16);
							localtime = year_Month + " " + timeShow;
							if (debug) {
								Log.i(TAG, "final show_time = "+localtime);
							}
							updateTimetoDatabase(String.valueOf(cityList.get(i).cityindex),localtime);
						}
					}
					AppWidgetManager gm = AppWidgetManager.getInstance(WeatherService.this);
					ArrayList<Integer> appwidgetList = WeatherDatabaseAction.getWidgetList(WeatherService.this);
					for (int i = 0; i < appwidgetList.size(); i++) {
						int appwidgetid = appwidgetList.get(i);
						WeatherWidgetProvider.updateAppWidget(WeatherService.this, gm, appwidgetid);
					}
				}
			}
		}
	};
	
	private void updateInfoToDatabase(int index, String cityname, String cityId) {
		
		final ContentValues values = new ContentValues();
		final ContentResolver cr = getContentResolver();
		values.put("cityName", cityname);
		values.put("cityId", cityId);
		
		String where = "cityIndex" + "=" + index;
		cr.update(WeatherContentProvider.CONTENT_URI, values, where, null);
	}
	
	static int selectedPicture(String code){
		int picture_id = 0;
		if (code == null || "".equals(code)){
			//Log.v(TAG,"code is null");
			return picture_id = R.drawable.w3200;
		}
		if (code.equals("1")) {                        //sunny
			picture_id = R.drawable.a1_256;
		}else if (code.equals("2")) {                  //mostly sunny
			picture_id = R.drawable.a2_256;
		}else if (code.equals("3")) {                  //partly sunny
			picture_id = R.drawable.a3_256;
		}else if (code.equals("4")) {					//Intermittent Clouds
			picture_id = R.drawable.a4_256;
		}else if (code.equals("5")){                   //Hazy Sunshine
			picture_id = R.drawable.a5_256;
		}else if (code.equals("6")) {                  //Mostly Cloudy
			picture_id = R.drawable.a6_256;
		}else if (code.equals("7")) {                  //Cloudy
			picture_id = R.drawable.a7_256;
		}else if (code.equals("8")) {                  //Dreary
			picture_id = R.drawable.a8_256;
		}else if (code.equals("9")) {                  //retired  null
			
		}else if (code.equals("10")){                   //retired  null
			
		}else if (code.equals("11")) {                  //fog
			picture_id = R.drawable.a11_256;
		}else if (code.equals("12")){                   //showers
			picture_id = R.drawable.a12_256;
		}else if (code.equals("13")){                   //Mostly Cloudy with Showers
			picture_id = R.drawable.a13_256;
		}else if (code.equals("14")) {                  //Partly Sunny with Showers
			picture_id = R.drawable.a14_256;
		}else if (code.equals("15")){                   //Thunderstorms
			picture_id = R.drawable.a15_256;
		}else if (code.equals("16")) {                  //Mostly Cloudy with Thunder Showers
			picture_id = R.drawable.a16_256;
		}else if (code.equals("17")) {                  //Partly Sunnty with Thunder Showers
			picture_id = R.drawable.a17_256;
		}else if (code.equals("18")) {                  //Rain
			picture_id = R.drawable.a18_256;
		}else if (code.equals("19")) {                  //Flurries
			picture_id = R.drawable.a19_256;
		}else if (code.equals("20")) {                  //Mostly Cloudy with Flurries
			picture_id = R.drawable.a20_256;
		}else if (code.equals("21")) {                  //Partly Sunny with Flurries
			picture_id = R.drawable.a21_256;
		}else if (code.equals("22")) {                  //Snow
			picture_id = R.drawable.a22_256;
		}else if (code.equals("23")) {                  //Mostly Cloudy with Snow
			picture_id = R.drawable.a23_256;
		}else if (code.equals("24")) {                  //Ice
			picture_id = R.drawable.a24_256;
		}else if (code.equals("25")) {                  //Sleet
			picture_id = R.drawable.a25_256;
		}else if (code.equals("26")) {                  //Freezing Rain
			picture_id = R.drawable.a26_256;
		}else if (code.equals("27")) {                  //retired null
			
		}else if (code.equals("28")) {                  //retired null
			
		}else if (code.equals("29")) {                  //Rain and Snow Mixed
			picture_id = R.drawable.a29_256;
		}else if (code.equals("30")) {                  //hot
			picture_id = R.drawable.a30_256;
		}else if (code.equals("31")) {                  //cold
			picture_id = R.drawable.a31_256;
		}else if (code.equals("32")) {                  //windy
			picture_id = R.drawable.a32_256;
		}else if (code.equals("33")) {                //night only  moon
			picture_id = R.drawable.a33_256;
		}else if (code.equals("34")) {
			picture_id = R.drawable.a34_256;
		}else if (code.equals("35")) {
			picture_id = R.drawable.a35_256;
		}else if (code.equals("36")) {
			picture_id = R.drawable.a36_256;
		}else if (code.equals("37")) {
			picture_id = R.drawable.a37_256;
		}else if (code.equals("38")) {
			picture_id = R.drawable.a38_256;
		}else if (code.equals("39")) {
			picture_id = R.drawable.a39_256;
		}else if (code.equals("40")) {
			picture_id = R.drawable.a40_256;
		}else if (code.equals("41")) {
			picture_id = R.drawable.a41_256;
		}else if (code.equals("42")) {
			picture_id = R.drawable.a42_256;
		}else if (code.equals("43")) {
			picture_id = R.drawable.a43_256;
		}else if (code.equals("44")) {
			picture_id = R.drawable.a44_256;
		}
		return picture_id;
	}
	
	static String getStateOfBarometricPressure(String rising){
		String state = null;
		if (rising == null){
			return state = "no state";
		}
		switch(Integer.parseInt(rising)){
		case 0:
			state = "steady";
			break;
		case 1:
			state = "rising";
			break;
		case 2:
			state = "falling";
			break;
		default:
			state = "no state";
		}
		return state;
	}
	
	public void onDestroy() {
		super.onDestroy();
		if (mlooper != null)
			mlooper.quit();
		if (mDateChangedReceiver!=null) {
			this.unregisterReceiver(mDateChangedReceiver);
		}
	}
	
	private void reschedule() {
		AlarmManager alarmMgr = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
		Intent i = new Intent();
		i.setClassName("com.quanta.weathertime", "com.quanta.weathertime.WeatherService");
		i.setAction(ACTION_UPDATE_WEATHER);
		i.putExtra(REQUERY_ALL, "");
		PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
		
		if (mUpdateIntervalInSeconds <= 0) {
			alarmMgr.cancel(pi);
		}
		else {
			alarmMgr.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime()
					+ (mUpdateIntervalInSeconds * 1000), pi);
		}
	}
	
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	public static String getCode(){
		String lan = Locale.getDefault().getLanguage();
		String country = Locale.getDefault().getCountry();
		String code = null;
		if (lan.equals("zh")) {
			if (country.equalsIgnoreCase("TW")) {
				code = "&language=zh-tw&details=true";         //Traditional Chinese tw
			}else if (country.equalsIgnoreCase("HK")) {
				code = "&language=zh-hk&details=true";        //Traditional Chinese hk
			}else {
				code = "&language=zh-cn&details=true";	    //Simplified Chinese
			}
		}
		else if (lan.equals("ja")) {        //Japanese
			code = "&language=ja&details=true";
		}
		else if (lan.equals("fr")) {		//French
			code = "&language=fr&details=true";
		}
		else if (lan.equals("ko")) {		//Korean
			code = "&language=ko&details=true";
		}
		else if (lan.equals("ru")) {		//Russian
			code = "&language=ru&details=true";
		}
		else if (lan.equals("de")) {		//German
			code = "&language=de&details=true";
		}
		else if (lan.equals("es")) {		//spanish language
			code = "&language=es&details=true";	            //or 15
		}
		else if (lan.equals("it")) {		//Italian
			code = "&language=it&details=true";
		}
		else if (lan.equals("sl")) {		//Slovak (language)
			code = "&language=sl&details=true";
		}
		else if (lan.equals("da")) {		//Danish
			code = "&language=da&details=true";
		}
		else if (lan.equals("fi")) {		//Finnish (language)
			code = "&language=fi&details=true";
		}
		else if (lan.equals("nl")) {		//Dutch
			code = "&language=nl&details=true";
		}
		else if (lan.equals("el")) {		//greek language
			code = "&language=el&details=true";
		}
		else if (lan.equals("hu")) {		//Turkish
			code = "&language=hu&details=true";
		}
		else if (lan.equals("sl")) {		//Slovene language
			code = "&language=sl&details=true";
		}
		else if (lan.equals("pl")) {		//Polish
			code = "&language=pl&details=true";
		}
		else if (lan.equals("nl")) {		//Arabic
			code = "&language=nl&details=true";
		}
		else {
//			code = "&langid=1";				//English
			code = "&language=en&details=true";
		}
		return code;
	}
	public void queryIfPeriodicRefreshRequired() {
		if (cityidString == null) {
			Intent update_weather_info = new Intent("com.quanta.weather.ACTION_UPDATE_WEATHER_INFO");
			update_weather_info.putExtra("show_location", cityname);
			sendBroadcast(update_weather_info);
		}
		ArrayList<CityItemInfo> cityList;
		if (mQueryType == TYPE_QUERY_ONE && cityidString != null) {
			if (isList) {
				String[] temp = cityidString.split("/");
				for (int i = 0; i < temp.length; i++) {
					if (i == 0) {
						saveCurrentGeo();
						cityList = getItemInfoList();
						saveCityId(cityList.get(i).cityGeoPosition, i, cityList.size());
					}else {
						saveAndParseData(temp[i], i, temp.length);
					}
				}
			}else {
				saveAndParseData(cityidString, 0, 0);
			}
		}
		/*Update all city weather info*/
		if (mQueryType == TYPE_QUERY_ALL) {
			saveCurrentGeo();
			cityList = getItemInfoList();
			int i;
			for (i = 0; i < cityList.size(); i++){
				if (i == 0) {
					saveCityId(cityList.get(i).cityGeoPosition, i, cityList.size());
				}else {
					saveAndParseData(cityList.get(i).cityId, i, cityList.size());
				}
			}
		}
	}
	
	/**
	 * Function:Get current location and update it to database when add widget of current location
	 */
	private void saveCurrentGeo(){
		LocationGetter mLocationGetter = new LocationGetter(this);;
		Coordinates coordinates = mLocationGetter.getLocation(10000, 2000);
		double longitude = coordinates.longitude;
		double latitude = coordinates.latitude;
		
    	String geoPosition = latitude+","+longitude;
//		String geoPosition = "25.056678,121.374893"; //QCI
    	if (geoPosition != null) {
    		final ContentValues values = new ContentValues();
    		final ContentResolver cr = getContentResolver();
    		
    		values.put("cityName", "Current Location");
    		values.put("geoPosition", geoPosition);
    		
    		String where = "cityIndex" + "=" + 0;
    		cr.update(WeatherContentProvider.CONTENT_URI, values, where, null);
    	}
	}
	
	public void saveCityId(String cityGeoPosition, int count, int size){
		final int queryCityCount = count;
		final int cityListSize = size;
		Log.e(TAG, "geoposition === " + cityGeoPosition);
		if (cityGeoPosition!=null) {
			String cityurl;
			cityurl = "http://api.accuweather.com/locations/v1/cities/geoposition/search.json?q="
					+ cityGeoPosition + "&apikey=a115f94ccb7f488faa4e40a1d3c03532" + getCode();
			if (debug) {
				Log.i(TAG, "#saveAndParseCityCode---: url=" + cityurl);
			}
			JsonObjectRequest jsObjRequest = new JsonObjectRequest(Request.Method.GET, cityurl, null, new Response.Listener<JSONObject>() {
				
				@Override
				public void onResponse(JSONObject response) {
					// TODO Auto-generated method stub
					String cityId = null;
					String currentLoc = null;
					try {
						cityId = response.getString("Key");
						currentLoc = response.getString("LocalizedName");
						if (currentLoc.equals("")) {
							if (response.getJSONObject("AdministrativeArea").getString("LocalizedName").equals("")) {
								currentLoc = response.getString("EnglishName");
							}else {
								currentLoc = response.getJSONObject("AdministrativeArea").getString("LocalizedName");
							}
						}
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					getSharedPreferences(WeatherDatabaseAction.CITY_LIST_DETAIL, 0).edit()
					.putString(WeatherDatabaseAction.CURRENT, currentLoc).commit();
					updateInfoToDatabase(0, currentLoc, cityId);
					saveAndParseData(cityId, queryCityCount, cityListSize);
				}
			}, new Response.ErrorListener() {
				
				@Override
				public void onErrorResponse(VolleyError error) {
					// TODO Auto-generated method stub
					ContentValues values = new ContentValues();
					values.put("currentCondition", "");
					values.put("forecast", "");
					getContentResolver().update(WeatherContentProvider.CONTENT_URI, values,
							WeatherColumns.WeatherInfo.CITY_INDEX + "='" + 0 + "'", null);
					
					Intent intent = new Intent("com.quanta.weather.ACTION_UPDATE_FINISH");
					sendBroadcast(intent);
					
					final Context context = WeatherService.this;
					AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
					ArrayList<Integer> appwidgetList = WeatherDatabaseAction.getWidgetList(context);
					for (int i = 0; i < appwidgetList.size(); i++) {
						int appwidgetid = appwidgetList.get(i);
						WeatherWidgetProvider.updateAppWidget(context, appWidgetManager, appwidgetid);
					}
				}
			});
			queue.add(jsObjRequest);
		}
	}
	
	private void saveAndParseData(String cityId, int count, int size){
		final String queryId = cityId;
		String geoPosition = null;
		String[] geoTemp = null;
		Cursor cur = this.getContentResolver().query(WeatherContentProvider.CONTENT_URI,
				new String[] { WeatherColumns.WeatherInfo.CITY_GEOPOSITION },
				WeatherColumns.WeatherInfo.CITY_ID + "='" + queryId + "'",
				null, null);
		if (cur != null && cur.moveToFirst()) {
			geoPosition = cur.getString(cur.getColumnIndex(WeatherColumns.WeatherInfo.CITY_GEOPOSITION));
		}
		cur.close();
		if (null != geoPosition)
			geoTemp = geoPosition.split(",");
		final int queryCityCount = count;
		final int cityListSize = size;
		final String current_url = "http://api.accuweather.com/currentconditions/v1/"
				+ queryId + ".json?apikey=a115f94ccb7f488faa4e40a1d3c03532" + getCode();
		final String forecast_url = "http://api.accuweather.com/forecasts/v1/daily/5day/"
				+ queryId +"?apikey=a115f94ccb7f488faa4e40a1d3c03532" + getCode();
		final String time_cityurl = "http://ws.geonames.org/timezone?lat="
				+ geoTemp[0] + "&lng=" + geoTemp[1];
		/*
		 * Worldweatheronline API 
		 */
//		final String time_cityurl = "http://api.worldweatheronline.com/free/v1/tz.ashx?q="
//				+ geoPosition
//				+ "&format=xml"
//				+ "&key=t4ukrxepz88pnv744bzgt3c9";
		
//		Log.e(TAG, "geoPosition = " + geoPosition + " " + queryId + cityId);
//		Log.i(TAG, "#saveAndParseData: url=" + current_url);
		JsonArrayRequest jsArrayRequest = new JsonArrayRequest(current_url, new Response.Listener<JSONArray>() {
			@Override
			public void onResponse(JSONArray response) {
				// TODO Auto-generated method stub
				ContentValues values = new ContentValues();
				values.put("currentCondition", response.toString());
				getContentResolver().update(WeatherContentProvider.CONTENT_URI, values,
						WeatherColumns.WeatherInfo.CITY_ID + "='" + queryId + "'", null);
				
				queue.add(new JsonObjectRequest(Request.Method.GET, forecast_url, null, new Response.Listener<JSONObject>() {
					@Override
					public void onResponse(JSONObject response) {
						// TODO Auto-generated method stub
						ContentValues values = new ContentValues();
						values.put("forecast", response.toString());
						getContentResolver().update(WeatherContentProvider.CONTENT_URI, values,
								WeatherColumns.WeatherInfo.CITY_ID + "='" + queryId + "'", null);
						
						queue.add(new StringRequest(time_cityurl, new Response.Listener<String>() {
							@Override
							public void onResponse(String response) {
								// TODO Auto-generated method stub
								XmlPullParser xpp = Xml.newPullParser();
								try {
									xpp.setInput( new StringReader ( response.toString() ) );
									int eventType = xpp.getEventType();
									while (eventType != XmlPullParser.END_DOCUMENT) {
										String nodeName = xpp.getName();
										switch (eventType) {
										case XmlPullParser.START_DOCUMENT:
											break;
										case XmlPullParser.START_TAG:
											if (nodeName.equals("time")) {
												String Local_time = xpp.nextText();
												Log.e(TAG, Local_time + " " + queryId);
												ContentValues values = new ContentValues();
												values.put("cityLocalTime", Local_time);
												getContentResolver().update(WeatherContentProvider.CONTENT_URI, values,
														WeatherColumns.WeatherInfo.CITY_ID + "='" + queryId + "'", null);
											}
											/**
											 * Worldweatheronline API 
											 */
//											if(nodeName.equals("time_zone")) {
//												xpp.nextTag();
//												if (eventType == XmlPullParser.START_TAG) {
//													if (xpp.getName().equals("localtime")) {
//														String Local_time = xpp.nextText();
//														Log.e(TAG, Local_time + " " + queryId);
//														ContentValues values = new ContentValues();
//														values.put("cityLocalTime", Local_time);
//														getContentResolver().update(WeatherContentProvider.CONTENT_URI, values,
//																WeatherColumns.WeatherInfo.CITY_ID + "='" + queryId + "'", null);
//													}
//												}
//												eventType = xpp.next();
//										    }
											break;
										case XmlPullParser.END_TAG:
											break;
										default:
											break;
										}
										eventType = xpp.next();
									}
								} catch (XmlPullParserException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								
								Intent addwidgetintent = new Intent("com.quanta.weather.ACTION_UPDATE_CITY_CODE");
								addwidgetintent.putExtra("citydetail", cityname);
								sendBroadcast(addwidgetintent);
								
								Intent update_weather_info = new Intent("com.quanta.weather.ACTION_UPDATE_WEATHER_INFO");
								update_weather_info.putExtra("show_location", cityname);
								sendBroadcast(update_weather_info);
								
								if (queryCityCount+1 >= cityListSize) {
									Intent intent = new Intent("com.quanta.weather.ACTION_UPDATE_FINISH");
									sendBroadcast(intent);
								}
								final Context context = WeatherService.this;
								AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
								ArrayList<Integer> appwidgetList = WeatherDatabaseAction.getWidgetList(context);
								for (int i = 0; i < appwidgetList.size(); i++) {
									int appwidgetid = appwidgetList.get(i);
									WeatherWidgetProvider.updateAppWidget(context, appWidgetManager, appwidgetid);
								}
							}
						}, new Response.ErrorListener() {
							
							@Override
							public void onErrorResponse(VolleyError error) {
								// TODO Auto-generated method stub
								Intent intent = new Intent("com.quanta.weather.ACTION_UPDATE_FINISH");
								sendBroadcast(intent);
							}
						}));
					}
				}, new Response.ErrorListener() {
					
					@Override
					public void onErrorResponse(VolleyError error) {
						// TODO Auto-generated method stub
//						Log.e(TAG, "onErrorResponse ___ forecast weather");
						Intent intent = new Intent("com.quanta.weather.ACTION_UPDATE_FINISH");
						sendBroadcast(intent);
					}
				}));
			}
		}, new Response.ErrorListener() {
			
			@Override
			public void onErrorResponse(VolleyError error) {
				// TODO Auto-generated method stub
//				Log.e(TAG, "onErrorResponse ___ today's weather");
				Intent intent = new Intent("com.quanta.weather.ACTION_UPDATE_FINISH");
				sendBroadcast(intent);
			}
		});	
		queue.add(jsArrayRequest);
		queue.start();
	}
	
	@SuppressWarnings("unused")
	private void updateTimetoDatabase(String cityid, String localtime){
		final ContentValues values = new ContentValues();
		final ContentResolver cr = getContentResolver();
		values.put("cityLocalTime", localtime);
		int result = cr.update(WeatherContentProvider.CONTENT_URI, values, WeatherColumns.WeatherInfo.CITY_INDEX + "='" + cityid + "'", null);
	}
	
	private ArrayList<CityItemInfo> getItemInfoList (){
		ArrayList<CityItemInfo> itemInfos = new ArrayList<CityItemInfo>();
		
		itemInfos.clear();
		
		final ContentResolver contentResolver = this.getContentResolver();
		final Cursor c = contentResolver.query(
				WeatherContentProvider.CONTENT_URI, null, null, null, null);
		
		try {
			final int cityindexIndex = c.getColumnIndexOrThrow("cityIndex");
			final int cityshowNameIndex = c.getColumnIndexOrThrow("showName");
			final int cityNameIndex = c.getColumnIndexOrThrow("cityName");
			final int cityIdIndex = c.getColumnIndexOrThrow("cityId");
			final int cityDetailIndex = c.getColumnIndexOrThrow("cityDetail");
			final int cityGeoPositionIndex = c.getColumnIndexOrThrow("geoPosition");
			
			while (c.moveToNext()) {
				try {
					CityItemInfo info = new CityItemInfo();
					info.cityindex = c.getInt(cityindexIndex);
					info.cityshowName = c.getString(cityshowNameIndex);
					info.cityName = c.getString(cityNameIndex);
					info.cityId = c.getString(cityIdIndex);
					info.cityDetail = c.getString(cityDetailIndex);
					info.cityGeoPosition = c.getString(cityGeoPositionIndex);
					itemInfos.add(info);
				} catch (Exception e) {
					Log.w(TAG, " items loading interrupted:", e);
				}
			}
		} finally {
			c.close();
		}
		return itemInfos;
	}
	
	@SuppressWarnings("unused")
	private class CityItemInfo{
		int cityindex;
		String cityshowName;
		String cityName;
		String cityId;
		String cityDetail;
		String cityGeoPosition;
		
		public void setCityName(String cityname){
			this.cityName = cityname;
		}
	}
	
	public void run() {
		// TODO Auto-generated method stub
		queryIfPeriodicRefreshRequired();
	}
}
