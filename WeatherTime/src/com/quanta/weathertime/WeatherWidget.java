package com.quanta.weathertime;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONException;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

public class WeatherWidget implements RemoteViewsService.RemoteViewsFactory{
	private static final String TAG = "WeatherWidget";
	private boolean debug = false;
	private Context mContext;
	
	private String unit;
	private String showInCitys;
//	private int updateCount;
	private SharedPreferences mSettings;
	@SuppressWarnings("unused")
	private SharedPreferences citySettings;
	private ArrayList<WeatherDatabaseAction.Detailinfo> detailinfos = new ArrayList<WeatherDatabaseAction.Detailinfo>();
	
	public WeatherWidget(Context context, String showcitys) {
		mContext = context;
		showInCitys = showcitys;
		mSettings = mContext.getSharedPreferences(WeatherConditionsActivity.SETTING_INFOS, 0);
		citySettings = mContext.getSharedPreferences(WeatherDatabaseAction.CITY_LIST_DETAIL, 0);
	}
	
	@Override
	public int getCount() {
		return detailinfos.size();
	}
	
	@Override
	public long getItemId(int position) {
		return position;
	}
	
	@Override
	public RemoteViews getLoadingView() {
		return null;
	}
	
	@Override
	public RemoteViews getViewAt(int position) {
		RemoteViews views = new RemoteViews(mContext.getPackageName(), R.layout.weather_appwidget);
		if (detailinfos.size()>0) {
			WeatherDatabaseAction.Detailinfo detailinfo = detailinfos.get(position);
			if (detailinfo != null) {
				if (detailinfo.day_night.equalsIgnoreCase("true")) {
					views.setImageViewResource(R.id.day_night, R.drawable.day_bg);
				}else {
					views.setImageViewResource(R.id.day_night, R.drawable.night_bg);
				}
				String widget_cityString = detailinfo.city_name;
				views.setTextViewText(R.id.widget_city, widget_cityString);
				CharSequence city_temperature = null;
				if (unit.equals("C")) {
					city_temperature = detailinfo.temp_c;
				}else if(unit.equals("F")){
					city_temperature = detailinfo.temp_f;
				}
				int i = WeatherService.selectedPicture(detailinfo.icon_data);
				
				String style = mContext.getString (R.string.deg);
				String format = null;
				format = String.format(style, city_temperature,unit);
				CharSequence text = Html.fromHtml(format);
				
				views.setTextViewText(R.id.widget_temperature, text);
				
				views.setImageViewResource(R.id.widget_picture, i);
				views.setTextViewText(R.id.widget_weather, detailinfo.condition);
				views.setTextViewText(R.id.widget_updated_time, detailinfo.current_datetime);
				//payne:set local time
				Cursor cursor = mContext.getContentResolver().query(WeatherContentProvider.CONTENT_URI,
						new String[] { WeatherColumns.WeatherInfo.CITY_LOCAL_TIME },
						WeatherColumns.WeatherInfo.CITY_INDEX + "=" + detailinfo.index,
						null, null);
				if (cursor != null && cursor.moveToNext()) {
					String localtime = cursor.getString(cursor.getColumnIndex(WeatherColumns.WeatherInfo.CITY_LOCAL_TIME));
					if (localtime != null && localtime.length() != 0) {
						views.setTextViewText(R.id.widget_local_time, localtime.substring(11));
						String year_month = localtime.substring(0,10);
						if (debug) {
							Log.i(TAG, "localtime = " + year_month);
						}
						SimpleDateFormat df1 = new SimpleDateFormat("yyyy-MM-dd");
						java.text.DateFormat df2 = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault());
						java.util.Date date2;
						try {
							date2 = df1.parse(year_month);
							String date3 = df2.format(date2);
							year_month = date3.toString();
							if (debug) {
								Log.i(TAG, "date = "+ date3.toString());
							}
						} catch (ParseException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						views.setTextViewText(R.id.year_month, year_month);
					}
				}
				cursor.close();
				if (detailinfo.citycode_id == null || detailinfo.citycode_id.equals("")) {
					views.setViewVisibility(R.id.widget_local_time, View.INVISIBLE);
					views.setViewVisibility(R.id.year_month, View.INVISIBLE);
					views.setViewVisibility(R.id.week, View.INVISIBLE);
					views.setViewVisibility(R.id.widget_temperature, View.INVISIBLE);
					views.setViewVisibility(R.id.current_layout_divider, View.INVISIBLE);
					views.setViewVisibility(R.id.widget_mark, View.INVISIBLE);
					Cursor cur = mContext.getContentResolver().query(WeatherContentProvider.CONTENT_URI,
							new String[] { WeatherColumns.WeatherInfo.CITY_NAME },
							WeatherColumns.WeatherInfo.CITY_INDEX + "=" + detailinfo.index,
							null, null);
					if (cur != null && cur.moveToNext()) {
						try {
							String name = cur.getString(cur.getColumnIndex(WeatherColumns.WeatherInfo.CITY_NAME));
							views.setTextViewText(R.id.widget_city, name);
						} catch (Exception e) {
							// TODO: handle exception
						}finally{
							cur.close(); 
						}
					}
				}
				Intent fillInIntent = new Intent(mContext, WeatherConditionsActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				Bundle bundle = new Bundle();
				bundle.putInt("CITY_INDEX", detailinfo.index);
				fillInIntent.putExtras(bundle);
				views.setOnClickFillInIntent(R.id.widget_layout, fillInIntent);
			}
		}
		return views;
	}
	
	@Override
	public int getViewTypeCount() {
		return 1;
	}
	
	@Override
	public boolean hasStableIds() {
		return true;
	}
	
	@Override
	public void onCreate() {
		
	}
	
	public String[] tempStrings = null;
	@Override
	public void onDataSetChanged() {
		if (debug) {
			Log.i(TAG, "onDataSetChanged");
		}
			detailinfos.clear();
			int cityindex = 0;
			String currentCondition = null;
			@SuppressWarnings("unused")
			String currentCity = null;
			
			unit = mSettings.getString("Temperature Unit", "C");
			if (showInCitys == null) {
				if (true) {
					Log.e(TAG, "showInCitys is null");
				}
			}else {
				tempStrings = showInCitys.split("/");
				detailinfos.clear();
				for (int i = 0; i < tempStrings.length; i++) {
					cityindex = Integer.parseInt(tempStrings[i]);
					Cursor cur = mContext.getContentResolver().query(WeatherContentProvider.CONTENT_URI,
							new String[]{WeatherColumns.WeatherInfo.CITY_NAME, WeatherColumns.WeatherInfo.CURRENT_CONDITION },
							WeatherColumns.WeatherInfo.CITY_INDEX+ "=" + cityindex, null, null);
					if (cur != null && cur.getCount() > 0) {
						cur.moveToNext();
						currentCity = cur.getString(cur.getColumnIndex(WeatherColumns.WeatherInfo.CITY_NAME));
						currentCondition = cur.getString(cur.getColumnIndex(WeatherColumns.WeatherInfo.CURRENT_CONDITION));
					}
					cur.close();
					
					try {
						if (null != currentCondition && !currentCondition.equals("")) { 
							detailinfos.add(parseJson(cityindex));
						}else {
							WeatherDatabaseAction.Detailinfo detailinfo = new WeatherDatabaseAction.Detailinfo();
							detailinfo.city_name = "";
							detailinfo.citycode_id = "";
							detailinfo.day_night = "true";
							detailinfo.index = cityindex;
							detailinfo.condition = "";
							detailinfo.temp_c = "";
							detailinfo.temp_f = "";
							detailinfo.icon_data = "";
							detailinfo.current_datetime = "";
							detailinfos.add(detailinfo);
						}
					} catch (JSONException e) {
//						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
	}
	
	@Override
	public void onDestroy() {
		detailinfos.clear();
		detailinfos = null;
	}
	
	public WeatherDatabaseAction.Detailinfo parseJson(int index) throws JSONException {
		String currentCondition = null;
		WeatherDatabaseAction.Detailinfo mDetailinfo = new WeatherDatabaseAction.Detailinfo();
		Cursor cursor = mContext.getContentResolver().query(WeatherContentProvider.CONTENT_URI,
				new String[] { WeatherColumns.WeatherInfo.CITY_ID, WeatherColumns.WeatherInfo.CITY_NAME,
				WeatherColumns.WeatherInfo.CITY_GEOPOSITION, WeatherColumns.WeatherInfo.CURRENT_CONDITION },
				WeatherColumns.WeatherInfo.CITY_INDEX + "='" + index + "'",
				null, null);
		if (cursor != null && cursor.moveToFirst()) {
			mDetailinfo.citycode_id = cursor.getString(cursor.getColumnIndex(WeatherColumns.WeatherInfo.CITY_ID));
			mDetailinfo.city_name = cursor.getString(cursor.getColumnIndex(WeatherColumns.WeatherInfo.CITY_NAME));
			mDetailinfo.geoPosition = cursor.getString(cursor.getColumnIndex(WeatherColumns.WeatherInfo.CITY_GEOPOSITION));
			currentCondition = cursor.getString(cursor.getColumnIndex(WeatherColumns.WeatherInfo.CURRENT_CONDITION));
		}
		cursor.close();
		if (null != currentCondition) {
			JSONArray jsonArray = new JSONArray(currentCondition);
			for (int i = 0; i < jsonArray.length(); i++) {
				mDetailinfo.index = index;
				mDetailinfo.condition=jsonArray.getJSONObject(i).getString("WeatherText");
				mDetailinfo.icon_data=jsonArray.getJSONObject(i).getString("WeatherIcon");
				mDetailinfo.day_night=jsonArray.getJSONObject(i).getString("IsDayTime");
				mDetailinfo.temp_c=jsonArray.getJSONObject(i).getJSONObject("Temperature").getJSONObject("Metric").getString("Value");
				mDetailinfo.temp_f=jsonArray.getJSONObject(i).getJSONObject("Temperature").getJSONObject("Imperial").getString("Value");
				mDetailinfo.wind_condition=mContext.getString(R.string.wind_direction)+jsonArray.getJSONObject(i).getJSONObject("Wind").getJSONObject("Direction").getString("Localized");
				mDetailinfo.humidity=mContext.getString(R.string.humidity)+jsonArray.getJSONObject(i).getString("RelativeHumidity");
				mDetailinfo.current_datetime=jsonArray.getJSONObject(i).getString("LocalObservationDateTime");
				String temps_1 = mDetailinfo.current_datetime.substring(0, 10);
				String temps_2 = mDetailinfo.current_datetime.substring(11,19);
				mDetailinfo.current_datetime = mContext.getString(R.string.update_time) + " " + temps_1 + " " + temps_2;
			}
		}
		return mDetailinfo;
	}
	
	@SuppressWarnings("unused")
	private String getIconName(String path){
		String[] str = path.split("/");
		int len = str.length;
		String temp = str[len-1];
		String name = temp.substring(0,temp.indexOf("."));
		return name;
	}
	
	@SuppressWarnings("unused")
	private ArrayList<CityShowNameInfo> getCityShowName(){
		ArrayList<CityShowNameInfo> ShowNameInfos = new ArrayList<CityShowNameInfo>();
		ShowNameInfos.clear();
		final ContentResolver contentResolver = mContext.getContentResolver();
		final Cursor c = contentResolver.query(
				WeatherContentProvider.CONTENT_URI, null, null, null, null);
		try {
			final int cityshowNameIndex = c.getColumnIndexOrThrow("showName");
			
			while (c.moveToNext()) {
				try {
					CityShowNameInfo info = new CityShowNameInfo();
					info.cityshowName = c.getString(cityshowNameIndex);
					ShowNameInfos.add(info);
				} catch (Exception e) {
					Log.w(TAG, " items loading interrupted:", e);
				}
			}
		} finally {
			c.close();
		}
		return ShowNameInfos;
	}
	
	private class CityShowNameInfo{
		@SuppressWarnings("unused")
		String cityshowName;
	}
}
