package com.quanta.weathertime;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.quanta.weathertime.WeatherDatabaseAction.Forecase_conditions;
import com.quanta.weathertime.location.Coordinates;
import com.quanta.weathertime.location.LocationGetter;

import android.app.Activity;
import android.app.ProgressDialog;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Parcelable;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CursorAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

public class WeatherInfoDetail extends Activity implements OnInitListener, OnUtteranceCompletedListener {
	private final String TAG = "WeatherInfoDetail";
	private boolean debug = false;
	public static final String SETTING_INFOS = "SETTING_Infos";
	
	private ViewPager viewPager;
    private viewPagerAdapter pagerAdapter;
    private List<View> viewPagerList;
	
	private TextView mCityNameView;
	private TextView mTimeView;
	private TextView mTempView;
	private TextView mConditionView;
	private TextView mHumView;
	private TextView mWindView;
	private ImageView mWeatherIconView;
	private GridView mExtendForeCast;
	private TextView nullDataView;
	private View mLoadingView;
	private TextView mExtendDateView;
	private TextView mLowView;
	private TextView mHighView;
	private ImageView mExtendIconView;
	private WeatherDatabaseAction.Detailinfo mDetailinfo = null;
	private ArrayList<Forecase_conditions> mForecastConditons = new ArrayList<WeatherDatabaseAction.Forecase_conditions>();
	private String unit;
	private String mCurrentLocationStr;
	private Boolean Is_Loading = false;
	private String current_temp;
	private SharedPreferences citySettings;
	public static int showcity;
	private SharedPreferences mSettings;
	private MenuItem mRefreshIcon;
	private MenuItem mSpeechIcon;
	private String mCurrentLoc;
	private String mCurrentSLoc;
	private ArrayList<CityItemInfo> locations = new ArrayList<CityItemInfo>();
	private double longitude;
	private double latitude;
	private LocationGetter mLocationGetter;
	private String cityId;
	public static final int MSG = 1;
	
	private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
		
		public void onReceive(Context context, Intent intent) {
			if (debug) {
				Log.v(TAG, "BroadcastReceiver:onReceive " + intent.getAction());
			}
			if (intent.getAction().equals("com.quanta.weather.ACTION_UPDATE_WEATHER_INFO")) {
//				if (debug) {
					Log.v(TAG, "BroadcastReceiver:onReceive "+ showcity);
//				}
				
//                getCityList();
				/*
                Cursor cursor = getContentResolver().query(WeatherContentProvider.CONTENT_URI, null, null, null, null);
                mCityListView.setAdapter(new CityItemAdapter(context, cursor));
				 */
				
				setViewValue(showcity);
				viewPager.setCurrentItem(showcity);
				citySettings.edit().putInt(WeatherDatabaseAction.SHOW_CITY, showcity)
				.commit();
				
				if (mRefreshIcon != null) {
					mRefreshIcon.setActionView(null);
				}
				
			} else if (intent.getAction().equals("com.quanta.weather.ACTION_UPDATE_FINISH")) {
				if (debug) {
					Log.v(TAG, "BroadcastReceiver:onReceive1 " + intent.getAction());
				}
				if (progressDialog != null && progressDialog.isShowing()) {
					progressDialog.dismiss();
				}
			}
		}
	};
	
	private void getCityList() {
		locations.clear();
		locations = getItemInfoList();
	}
	
	private TextToSpeech mTts;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Put your code here
		super.onCreate(savedInstanceState);
		mLocationGetter = new LocationGetter(this);
//		setContentView(R.layout.weather_info_detail);
		setContentView(R.layout.weather_detail);
		mDetailinfo = new WeatherDatabaseAction.Detailinfo();
		mCurrentLocationStr = getResources().getString(R.string.current_location);
		mSettings = getSharedPreferences(SETTING_INFOS, 0);
		citySettings = getSharedPreferences(WeatherDatabaseAction.CITY_LIST_DETAIL, 0);
		mCurrentLoc = citySettings.getString(WeatherDatabaseAction.CURRENT, null);
		if (debug) {
			Log.e(TAG, "mCurrentLoc = " + mCurrentLoc);
		} 
		showcity = getIntent().getIntExtra("showcity", 0);
		cityId = getIntent().getStringExtra("cityid");
		getCityList();
		Findcomponent();
		mTts = new TextToSpeech(getApplicationContext(), this);
//		setViewValue(showcity);
	}
	
	 private void findViewItem(int viewId){
	    	View view = viewPagerList.get(viewId);
	    	mCityNameView = (TextView) view.findViewById(R.id.city_name);
	    	mTimeView = (TextView) view.findViewById(R.id.current_time);
	    	mTempView = (TextView) view.findViewById(R.id.temp);
	    	mConditionView = (TextView) view.findViewById(R.id.condition);
	    	mHumView = (TextView) view.findViewById(R.id.hum);
	    	mWindView = (TextView) view.findViewById(R.id.wind);
	    	mWeatherIconView = (ImageView) view.findViewById(R.id.icon);
	    	mExtendForeCast = (GridView) view.findViewById(R.id.extend_forecast);
	    	nullDataView = (TextView) view.findViewById(R.id.null_data);
	    	mLoadingView = view.findViewById(R.id.loading);
//	    	mMarkView = (TextView) view.findViewById(R.id.mark);
	    }
	
	private void Findcomponent(){
//		mCityNameView = (TextView) findViewById(R.id.city_name);
//		mTimeView = (TextView) findViewById(R.id.current_time);
//		mTempView = (TextView) findViewById(R.id.temp);
//		mConditionView = (TextView) findViewById(R.id.condition);
//		mHumView = (TextView) findViewById(R.id.hum);
//		mWindView = (TextView) findViewById(R.id.wind);
//		mWeatherIconView = (ImageView) findViewById(R.id.icon);
//		mExtendForeCast = (GridView) findViewById(R.id.extend_forecast);
//		nullDataView = (TextView) findViewById(R.id.null_data);
//		mLoadingView = findViewById(R.id.loading);
		
		viewPager = (ViewPager) findViewById(R.id.pager);
		viewPagerList = new ArrayList<View>();
		for (int i = 0; i < locations.size(); i++) {
			View view = getLayoutInflater().inflate(R.layout.weather_info_detail, null);
			viewPagerList.add(view);
		}
		pagerAdapter = new viewPagerAdapter();
		viewPager.setAdapter(pagerAdapter);
		for (int j = 0; j < viewPagerList.size(); j++) {
			setViewValue(j);
		}
		
		viewPager.setOnPageChangeListener(new OnPageChangeListener() {
			
			@Override
			public void onPageSelected(int position) {
				// TODO Auto-generated method stub
//				if (oldView != null) {
//					oldView.setBackgroundDrawable(null);
//				}
//				Log.e(TAG, "on pageselected page === " + positon);
//				mCityListView.getFirstVisiblePosition();
//				View view = mCityListView.getChildAt(positon-mCityListView.getFirstVisiblePosition());
//				Log.e(TAG, "view =" + mCityListView.getChildAt(positon-mCityListView.getFirstVisiblePosition()));
//				if (null != view) {
//					view.setBackgroundResource(R.drawable.list_selected_holo_dark);
//				}
//				oldView = view;
//				showCityIndex = positon;
//				citySettings.edit().putInt(WeatherDatabaseAction.SHOW_CITY, showCityIndex).commit();
//				setViewValue(showCityIndex);
				showcity = position;
				citySettings.edit().putInt(WeatherDatabaseAction.SHOW_CITY, showcity).commit();
				setViewValue(showcity);
			}
			
			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {
				// TODO Auto-generated method stub
			}
			
			@Override
			public void onPageScrollStateChanged(int arg0) {
				// TODO Auto-generated method stub
			}
		});
	}
	
	private void setViewValue(int index) {
		findViewItem(index);
		locations = getItemInfoList();
//		String show_name = null;
		String city_name = null;
		String currentCondition = null;
    	String forecast = null;
		Cursor cur = getContentResolver().query(WeatherContentProvider.CONTENT_URI,
				new String[] { WeatherColumns.WeatherInfo.CITY_SHOW_NAME, WeatherColumns.WeatherInfo.CITY_NAME, 
				WeatherColumns.WeatherInfo.CURRENT_CONDITION, WeatherColumns.WeatherInfo.FORECAST },
				WeatherColumns.WeatherInfo.CITY_INDEX + "=" + locations.get(index).cityindex,
				null, null);
		
		if (cur != null) {
			if (cur.getCount() > 0) {
				cur.moveToNext();
//				show_name = cur.getString(cur.getColumnIndex(WeatherColumns.WeatherInfo.CITY_SHOW_NAME));
				city_name = cur.getString(cur.getColumnIndex(WeatherColumns.WeatherInfo.CITY_NAME));
				currentCondition = cur.getString(cur.getColumnIndex(WeatherColumns.WeatherInfo.CURRENT_CONDITION));
                forecast = cur.getString(cur.getColumnIndex(WeatherColumns.WeatherInfo.FORECAST));
			}
			cur.close();
		}
		try {
			if (index == 0 && city_name.equals(mCurrentLocationStr)) {
				nullDataView.setVisibility(View.VISIBLE);
				nullDataView.setText(R.string.update_current_location);
				return;
			} else {
				if (null != currentCondition && null != forecast) {
					parseJSON(currentCondition, forecast);
				}else {
//					mDetailinfo = null;
				}
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if ((currentCondition == null || forecast == null)&& Is_Loading == false) {
			if (debug) {
				Log.e(TAG, "set nullview");
			}
			nullDataView.setVisibility(View.VISIBLE);
			nullDataView.setText(R.string.city_invalid);
			mLoadingView.setVisibility(View.GONE);
			return;
		}
		unit = mSettings.getString("Temperature Unit", "C");
		if (unit.equals("C")) {
			current_temp = mDetailinfo.temp_c;
		} else if (unit.equals("F")) {
			current_temp = mDetailinfo.temp_f;
		}
		
		nullDataView.setVisibility(View.GONE);
		mLoadingView.setVisibility(View.GONE);
		mCityNameView.setText(city_name);
		mTimeView.setText(mDetailinfo.current_datetime);
		mTempView.setText(convertTempUnit(current_temp));
		mConditionView.setText(mDetailinfo.condition);
		mHumView.setText(mDetailinfo.humidity+"%");
		mWindView.setText(getString(R.string.wind_direction)+mDetailinfo.wind_condition);
		mWeatherIconView.setImageResource(WeatherService.selectedPicture(mDetailinfo.icon_data));
		mExtendForeCast.setAdapter(new ExtendForecastAdapter());
		citySettings.edit().putInt(WeatherDatabaseAction.SHOW_CITY, index).commit();
	}
	
	
	void parseJSON(String currentCondition, String forecat) throws JSONException {
		// TODO: switch to sax
		if (debug) {
			Log.i(TAG, "activity_parserss  ");
		}
		mForecastConditons.clear();
//		mDetailinfo = new WeatherDatabaseAction.Detailinfo();
		WeatherDatabaseAction.Forecase_conditions condition = null;
		
		JSONArray jsonArray = new JSONArray(currentCondition);
		for (int i = 0; i < jsonArray.length(); i++) {
			mDetailinfo.condition=jsonArray.getJSONObject(i).getString("WeatherText");
			mDetailinfo.icon_data=jsonArray.getJSONObject(i).getString("WeatherIcon");
			mDetailinfo.day_night=jsonArray.getJSONObject(i).getString("IsDayTime");
			mDetailinfo.temp_c=jsonArray.getJSONObject(i).getJSONObject("Temperature").getJSONObject("Metric").getString("Value");
			mDetailinfo.temp_f=jsonArray.getJSONObject(i).getJSONObject("Temperature").getJSONObject("Imperial").getString("Value");
			mDetailinfo.wind_condition=jsonArray.getJSONObject(i).getJSONObject("Wind").getJSONObject("Direction").getString("Localized");
//			mDetailinfo.wind_condition=getString(R.string.wind_direction)+jsonArray.getJSONObject(i).getJSONObject("Wind").getJSONObject("Direction").getString("Localized");
			mDetailinfo.humidity=getString(R.string.humidity)+jsonArray.getJSONObject(i).getString("RelativeHumidity");
			mDetailinfo.current_datetime=jsonArray.getJSONObject(i).getString("LocalObservationDateTime");
			String temps_1 = mDetailinfo.current_datetime.substring(0, 10);
			String temps_2 = mDetailinfo.current_datetime.substring(11,19);
			mDetailinfo.current_datetime = getString(R.string.update_time) + " " + temps_1 + " " + temps_2;
		}
		
		JSONObject jsonObject = new JSONObject(forecat);
		for (int i = 0; i < jsonObject.getJSONArray("DailyForecasts").length()-1; i++) {
			condition = new WeatherDatabaseAction.Forecase_conditions();
			JSONObject item = jsonObject.getJSONArray("DailyForecasts").getJSONObject(i);
			condition.date = item.getString("Date").substring(0, 10);
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			try {
				java.util.Date date = sdf.parse(condition.date);
				SimpleDateFormat df = new SimpleDateFormat("EEEE");
				condition.date = df.format(date);
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (debug) {
				System.out.println(condition.date);
			}
			condition.low = String.valueOf(item.getJSONObject("Temperature").getJSONObject("Minimum").getInt("Value"));
			condition.high = String.valueOf(item.getJSONObject("Temperature").getJSONObject("Maximum").getInt("Value"));
			if (mDetailinfo.day_night.equals("true")) {
				condition.icon = item.getJSONObject("Day").getString("Icon");
				condition.condition = item.getJSONObject("Day").getString("ShortPhrase");
			}else {
				condition.icon = item.getJSONObject("Night").getString("Icon");
				condition.condition = item.getJSONObject("Night").getString("ShortPhrase");
			}
			mForecastConditons.add(condition);
		}
	}
	
	public boolean onCreateOptionsMenu(Menu menu) {
		final MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.weather_conditions_options, menu);
		return true;
	}
	
	public boolean onPrepareOptionsMenu(Menu menu) {
		mRefreshIcon = menu.findItem(R.id.update);
		mSpeechIcon = menu.findItem(R.id.speech);
		return true;
	}
	
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.speech:
			Speech_weather(item);
			return true;
		case R.id.update:
			ClickMenuItem_updateall();
			return true;
		case R.id.settings:
			pop_weatherSettings();
			return true;
		case R.id.add:
			ClickMenuItem_addcity();
			return true;
		}
		return false;
	}
	
	 public void Speech_weather(MenuItem item){
	    	if (mTts.isSpeaking()) {
				mTts.stop();
				item.setIcon(R.drawable.title_voice);
			}else {
//				String text = "天气为您播报"+ mCityNameView.getText() + "," + "今天白天" + mConditionView.getText() + "," + "温度"
//						+ mForecastConditons.get(0).low + "度到" + mForecastConditons.get(0).high + "度" + "," + "北风3到4级。";
//				String text = ""+ mCityNameView.getText() + mTimeView.getText().toString().substring(0,9) + mConditionView.getText()+mTempView.getText()
//						+mExtendDateView.getText()+mHighView.getText()+mLowView.getText()+mForecastConditons.get(0).low
//						+mDetailinfo.city_name;
				String wind_direction = mDetailinfo.wind_condition;
				if (wind_direction.equals("N")) {
					wind_direction = "north";
				}
				if (wind_direction.equals("S")) {
					wind_direction = "south";
				}
				if (wind_direction.equals("N")) {
					wind_direction = "north";
				}
				if (wind_direction.equals("W")) {
					wind_direction = "west";
				}
				if (wind_direction.equals("E")) {
					wind_direction = "east";
				}
				if (wind_direction.equals("NE")) {
					wind_direction = "north east";
				}
				if (wind_direction.equals("ES")) {
					wind_direction = "east south";
				}
				if (wind_direction.equals("WS")) {
					wind_direction = "west south";
				}
				if (wind_direction.equals("WN")) {
					wind_direction = "west north";
				}
				if (wind_direction.equals("EN")) {
					wind_direction = "east north";
				}
				if (wind_direction.equals("WS")) {
					wind_direction = "west south";
				}
				String DayNightStr = "今天白天";
				if(mDetailinfo.day_night.equals("true")){
					if (Locale.getDefault().equals(Locale.CHINA)) {
						DayNightStr = "今天白天";
					}else {
						DayNightStr = "During the day today";
					}
				}else{
					if (Locale.getDefault().equals(Locale.CHINA)) {
						DayNightStr = "今天夜间";
					}else {
						DayNightStr = "Today night";
					}
				}
				String text = "To Broadcast the weather for you,"+mCityNameView.getText()+","+DayNightStr+mConditionView.getText()
						+","+" temperature "+mForecastConditons.get(0).low+" degree to "+mForecastConditons.get(0).high+" degree,"
						+wind_direction+" wind "+" grade 3 to 4";
				Log.e("WeatherConditionsActivity", "text === " + text);
//				text = "天气为您播报，上海，今天白天到夜间多云转阴，温度16到23度，东风3到4级";
				item.setIcon(R.drawable.title_voice_stop);
				HashMap<String, String> myHashAlarm = new HashMap<String, String>();
				myHashAlarm.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(AudioManager.STREAM_NOTIFICATION));
				myHashAlarm.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,
						"utteranceId");
				mTts.speak(text, TextToSpeech.QUEUE_FLUSH, myHashAlarm);
			}
	    }
	
	private ProgressDialog progressDialog;
	private void ClickMenuItem_updateall() {
		progressDialog = ProgressDialog.show(this,
				getResources().getString(R.string.load),
				getResources().getString(R.string.please_wait), true, false);
		
		new Thread()
		{
			public void run()
			{
				locations = getItemInfoList();
				for (int i = 0; i < locations.size(); i++) {
//					String city = null;
					CityItemInfo cityInfo = locations.get(i);
					if (cityInfo.cityindex == 0){//city.equals(getResources().getString(R.string.current_location))) {
						getCurrentLocation();
//						city = mCurrentLocationStr;
					}
				}
				startWeatherService(showcity, cityId, "REQUERY_ALL", mCurrentLoc, mCurrentSLoc);
			}
		}.start();
	}
	
	private void pop_weatherSettings() {
		Intent intent = new Intent(this, WeatherSettings.class);
		startActivity(intent);
	}
	
	private void ClickMenuItem_addcity() {
		if (debug) {
			Log.v(TAG, "ClickMenuItem_addcity");
		}
		Intent intent = new Intent(this, WeatherWidgetConfigure.class);
		intent.putExtra("FROM_CITYWEATHERLIST", "");
		startActivityForResult(intent, MSG);
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
			
			while (c.moveToNext()) {
				try {
					CityItemInfo info = new CityItemInfo();
					info.cityindex = c.getInt(cityindexIndex);
					info.cityshowName = c.getString(cityshowNameIndex);
					info.cityName = c.getString(cityNameIndex);
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
	
	private class CityItemInfo{
		int cityindex;
		String cityshowName;
		@SuppressWarnings("unused")
		String cityName;
	}
	
	private void getCurrentLocation() {
		InitLocation();
//		String strlocation = null;
//	        try {
		// qci position for test
		/*
	            longitude = 121.3764188;
	            latitude = 25.049439;
		 */
		
		// exception for test
		/*
	            longitude = 0.0;
	            latitude = 0.0;
		 */
		
//		Log.e(TAG, " long & lat " + longitude + " " + latitude);
//	            Geocoder gcd = new Geocoder(this, Locale.getDefault());
//	            List<Address> addresses = gcd.getFromLocation(latitude, longitude, 1);
		
//	            if (addresses.size() > 0) {
//	                strlocation = String.valueOf(addresses.get(0).getLocality());
//	                mCurrentLoc = addresses.get(0).getAdminArea();
//	                mCurrentLoc = strlocation;//.toLowerCase();
//	            }
		String geoPosition = latitude+","+longitude;
		if (geoPosition != null) {
			updateInfoToDatabase(0, mCurrentLocationStr, geoPosition);
		}
	}
	
	private void updateInfoToDatabase(int index, String cityname, String geoPosition) {
		
		final ContentValues values = new ContentValues();
		final ContentResolver cr = getContentResolver();
		
		values.put("cityName", cityname);
		values.put("geoPosition", geoPosition);
		
		String where = "cityIndex" + "=" + index;
		cr.update(WeatherContentProvider.CONTENT_URI, values, where, null);
	}
	
	private void InitLocation() {
		/*
	        Criteria criteria = new Criteria();
	        criteria.setAccuracy(Criteria.ACCURACY_FINE);

	        criteria.setAltitudeRequired(false);
	        criteria.setBearingRequired(false);
	        criteria.setCostAllowed(true);
	        criteria.setPowerRequirement(Criteria.POWER_LOW);

	        lm = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
	        // lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,1l,1l,this);
	        // String provider = lm.getBestProvider(criteria, true);
	        Location location = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
	        if (location != null) {
	            longitude = location.getLongitude();
	            latitude = location.getLatitude();
	        }
	        // updataGpsWidthLocation(location);
		 */
		
		Coordinates coordinates = mLocationGetter.getLocation(10000, 2000);
		longitude = coordinates.longitude;
		latitude = coordinates.latitude;
	}
	
	private void startWeatherService(int cityindex, String cityid, String key, String current, String currentS) {
		String cityname = null;
		locations = getItemInfoList();
		for (int i = 0; i < locations.size(); i++) {
			if (locations.get(i).cityindex == cityindex) {
				cityname = locations.get(i).cityshowName;
			}
		}
		int id = citySettings.getInt(WeatherDatabaseAction.WIDGET_ID,
				AppWidgetManager.INVALID_APPWIDGET_ID);
		Intent intent = new Intent(this, WeatherService.class);
		Bundle bundle = new Bundle();
		bundle.putString("city_name", cityname);
		bundle.putString("city_id", cityid);
		bundle.putString("current", current);
		bundle.putString("current_admin", currentS);
		bundle.putString(key, "");
		bundle.putInt("appwidget_id", id);
		intent.putExtras(bundle);
		
		startService(intent);
	}
	
	protected void onResume(){
		super.onResume();
		updateUI();
		Is_Loading = false;
		IntentFilter filter = new IntentFilter();
		filter.addAction("com.quanta.weather.ACTION_UPDATE_WEATHER_INFO");
		filter.addAction("com.quanta.weather.ACTION_UPDATE_FINISH");
		this.registerReceiver(mIntentReceiver, filter);
	}
	
	public void onPause() {
		super.onPause();
		if (mIntentReceiver != null) {
			this.unregisterReceiver(mIntentReceiver);
		}
		if (progressDialog != null && progressDialog.isShowing()) {
			progressDialog.dismiss();
		}
	}
	
	public void onDestroy(){
		super.onDestroy();
		mTts.shutdown();
	}
	
	@SuppressWarnings("deprecation")
	private void updateUI(){
		if (citySettings.getString(WeatherDatabaseAction.CURRENT, null) == null) {
			updateInfoToDatabase(0, mCurrentLocationStr, null);
		}
		WeatherConditionsActivity.mCursor.requery();
		unit = mSettings.getString("Temperature Unit", "C");
		if (debug) {
			Log.e(TAG, "showcity in updateUI = " + showcity);
		}
		if (Is_Loading) {
			mLoadingView.setVisibility(View.VISIBLE);
			nullDataView.setVisibility(View.GONE);
		}else {
			if (debug) {
				Log.e(TAG, "showcity in updateUI = " + showcity);
			}
			viewPager.setCurrentItem(showcity);
			setViewValue(showcity);
		}
	}
	
	@SuppressWarnings({ "deprecation", "unused" })
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		switch (requestCode) {
		case MSG: {
			if (resultCode == RESULT_OK) {
				Is_Loading = true;
				if (debug) {
					Log.e(TAG, "onActivityResult " + intent.getIntExtra("data", 0));
				}
				String city_name = null;//intent.getCharSequenceExtra("data").toString();
				int cityindex = intent.getIntExtra("data", 0);
				showcity = cityindex;
				cityId = intent.getStringExtra("city_id");
				getCityList();
				WeatherConditionsActivity.mCursor.requery();
				 View view = getLayoutInflater().inflate(R.layout.weather_info_detail, null);
				viewPagerList.add(view);
                viewPager.setCurrentItem(pagerAdapter.getCount()-1);
				startWeatherService(showcity, cityId, "REQUERY_SPECIFIED_CITY", null, null);
			}
			break;
		}
		}
	}
	
	View oldView;
	@SuppressWarnings("unused")
	private class CityItemAdapter extends CursorAdapter{
		private LayoutInflater mInflater;
		
		@SuppressWarnings("deprecation")
		public CityItemAdapter(Context context, Cursor c) {
			super(context, c);
			mInflater = LayoutInflater.from(context);
		}
		
		public void bindView(View view, Context context, Cursor cursor) {
			ViewHolder holder;
			holder = new ViewHolder();
			holder.citynameView = (TextView) view.findViewById(R.id.city);
			holder.cityDetailView = (TextView) view.findViewById(R.id.city_detail);
			//view.setTag(holder);
			String name = cursor.getString(cursor.getColumnIndex(WeatherColumns.WeatherInfo.CITY_NAME));
			String detail = cursor.getString(cursor.getColumnIndex(WeatherColumns.WeatherInfo.CITY_DETAIL));
			
			int index = cursor.getInt(cursor.getColumnIndex(WeatherColumns.WeatherInfo.CITY_INDEX));
			if (index == 0) {
				holder.cityDetailView.setVisibility(View.GONE);
			}else {
				holder.cityDetailView.setVisibility(View.VISIBLE);
			}
			if (index == showcity) {
				if (oldView != null) {
					oldView.setBackgroundDrawable(null);
				}
				view.setBackgroundResource(R.drawable.list_selected_holo_dark);
				oldView = view;
			}
			view.setTag(index);
			if (index == 0 && !name.equalsIgnoreCase(mCurrentLocationStr)) {
				name = mCurrentLocationStr + "-" + name;
			}
			holder.citynameView.setText(name);
			holder.cityDetailView.setText(detail);
		}
		
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			if (debug) {
				Log.v(TAG, "newView");
			}
			View convertView = mInflater.inflate(R.layout.stored_city_list, null);
			
			return convertView;
		}
		
		class ViewHolder {
			TextView citynameView;
			TextView cityDetailView;
		}
	}
	
	private class ExtendForecastAdapter extends BaseAdapter {
		
		public ExtendForecastAdapter() {
			
		}
		
		@Override
		public int getCount() {
			return mForecastConditons.size();
		}
		
		@Override
		public Object getItem(int position) {
			return mForecastConditons.get(position);
		}
		
		@Override
		public long getItemId(int position) {
			return position;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			WeatherDatabaseAction.Forecase_conditions condition = mForecastConditons.get(position);
			convertView = LayoutInflater.from(WeatherInfoDetail.this).inflate(
					R.layout.extend_forecast_info, null);
			mExtendDateView = (TextView) convertView.findViewById(R.id.date);
			mExtendIconView = (ImageView) convertView.findViewById(R.id.icon);
			mHighView = (TextView) convertView.findViewById(R.id.high);
			mLowView = (TextView) convertView.findViewById(R.id.low);
			
			int hightem = Integer.parseInt(condition.high);
			int lowtem = Integer.parseInt(condition.low);
			if (unit.equals("C")) {         // && mDetailinfo.unit_system.equals("US")
				hightem = (hightem - 32) * 5 / 9;
				lowtem = (lowtem - 32) * 5 / 9;
			} else if (unit.equals("F") && mDetailinfo.unit_system.equals("SI")) {   //&& mDetailinfo.unit_system.equals("SI")
				hightem = hightem * 9 / 5 + 32;
				lowtem = lowtem * 9 / 5 + 32;
			}
			mExtendDateView.setText(condition.date);
			mExtendIconView.setImageResource(WeatherService.selectedPicture(condition.icon));
			mHighView.setText(convertTempUnit(String.valueOf(hightem)));
			mLowView.setText(convertTempUnit(String.valueOf(lowtem)));
			return convertView;
		}
		
	}
	private CharSequence convertTempUnit(String temp) {
		String style = this.getString(R.string.deg);
		String format = null;
		format = String.format(style, temp, unit);
		CharSequence text = Html.fromHtml(format);
		return text;
	}
	
	 private class viewPagerAdapter extends PagerAdapter{
	    	
	    	@Override
	    	public int getCount(){
	    		return viewPagerList.size();
	    	}

	    	@Override
	    	public boolean isViewFromObject(View view, Object object) {
	    		// TODO Auto-generated method stub
	    		return view == object;
	    	}
	    	
	    	@Override  
	    	public int getItemPosition(Object object) {  
	    		// TODO Auto-generated method stub
	    		return super.getItemPosition(object);  
	    	}
	    	
	    	@Override
	    	public void destroyItem (View view, int viewId, Object object) {
	    		if (viewId<viewPagerList.size()) {
	    			((ViewPager) view).removeView(viewPagerList.get(viewId));
				}
	    	}
			
	    	@Override
	    	public Object instantiateItem (View view, int viewId) {
	    		((ViewPager) view).addView(viewPagerList.get(viewId), 0);
	    		return viewPagerList.get(viewId);
	    	}
	    	
	    	@Override  
	    	public void restoreState(Parcelable arg0, ClassLoader arg1) {  
	    		// TODO Auto-generated method stub  
	    	}  
	    	
	    	@Override  
	    	public Parcelable saveState() {  
	    		// TODO Auto-generated method stub  
	    		return null;  
	    	}  
	    	
	    	@Override  
	    	public void startUpdate(View arg0) {  
	    		// TODO Auto-generated method stub  
	    	}  
	    	
	    	@Override  
	    	public void finishUpdate(View arg0) {  
	    		// TODO Auto-generated method stub  
	    	}
	    }

	@Override
	public void onInit(int status) {
		   // status can be either TextToSpeech.SUCCESS or TextToSpeech.ERROR.
     if (status == TextToSpeech.SUCCESS) {
         // Note that a language may not be available, and the result will indicate this.
         int result = mTts.setLanguage(Locale.getDefault());
         if (result == TextToSpeech.LANG_MISSING_DATA ||
             result == TextToSpeech.LANG_NOT_SUPPORTED) {
            // Lanuage data is missing or the language is not supported.
             mTts.setLanguage(Locale.US);
             Log.e("MessageViewFragment", "Language is not available.");
         } else {
         }
         /**
          * There are a number of issues you must overcome to get it work nicely.
          * They are:
          * 1.  Always set the UtteranceId (or else OnUtteranceCompleted will not be called)
          * 2.  setting OnUtteranceCompleted listener (oytnly after the speech system is properly initialized)
          */
         mTts.setOnUtteranceCompletedListener(this);  //must be registered after init text-to-speech
     } else {
         // Initialization failed.
         Log.e("WeatherConditionActivity", "Could not initialize TextToSpeech.");
     }
	}

	@Override
	public void onUtteranceCompleted(String utteranceId) {
		runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				mSpeechIcon.setIcon(R.drawable.title_voice);
			}
		});
	}
}
