package com.quanta.weathertime;

import java.util.ArrayList;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.util.Log;


public class WeatherDatabaseAction{
	private WeatherDatabaseAction(){}

	public static String SAVE_CITY_INFO_DB = "save_city_info_db";
	public static String SAVE_CONFIGURED_INFO_DB = "save_configured_info_db";
	public static String SAVE_DETAIL_INFO_DB = "save_detail_info_db";

	public static String CITY_LIST_DETAIL = "city_detail_list";
	public static String CITY_NAMES = "stored_location_name";
	public static String CITY_NAME_INDEX = "stored_location_index";
	public static String CITYS_SHOWIN_WIDGET = "citys_showin_widget";
	public static final String SHOW_CITY = "show_city";
	public static final String CURRENT = "current";
	public static final String WIDGET_ID = "widget_id";
	public static final String CITY_INDEX = "city_index";
	public static final String CITY_ID = "city_id";
	//payne:
	public static final String CITY_DETAIL = "cityDetail";
	public static final String CITY_LOCAL_TIME = "cityLocalTime";

	public static class Cityinfo{
		String cityname = "";
		String citycode = "";
		int citycode_id = 0;
		String language = "";
	}

	public static class Configuredinfo{
		int citycode_id = 0;
		int widgetid = 0;
	}
	public static class Detailinfo{
		String citycode_id = "";
		String unit_temperature = "";
		String unit_distance = "";
		String unit_pressure = "";
		String unit_speed = "";

		String speed = "";
		//String humidity = "";
		String visibility = "";
		String pressure = "";
		String rising = "";
		String sunrise = "";
		String sunset = "";

		String title = "";
	    String pubDate = "";
		String text = "";
		String code = "";
		String temp = "";
		String date = "";

		// new param
		String geoPosition = "";
		String city_detail = "";
		String city_name = "";
		String city_latitude = "";
		String city_longitude = "";
		String current_datetime = "";
		String unit_system = "";
		//payne new param
		String city_local_time = "";
		String day_night = "";

		String condition = "";
		String temp_f = "";
		String temp_c = "";
		String humidity = "";
		String icon_data = "";
		String wind_condition = "";
		String wind_speed = "";
		int index;
	}

	public static class Forecase_conditions{
		String date = "";
		String low = "";
		String high = "";
		String icon = "";
		String condition = "";
	}

	public static Detailinfo mDetailinfo;
	public static ArrayList<Forecase_conditions> conditions = new ArrayList<WeatherDatabaseAction.Forecase_conditions>();
	public static void saveConditions(Context context, Detailinfo info, ArrayList<Forecase_conditions> conditionlist){
		mDetailinfo = info;
		conditions = conditionlist;

		SharedPreferences citySettings = context.getSharedPreferences(CITY_LIST_DETAIL, 0);
		String citys = citySettings.getString(CITY_NAMES, null);
		String showcity = citySettings.getString(SHOW_CITY, null);
		if (citys != null) {
			boolean same = false;
			String[] tempString =citys.split("/");
			for (int i = 0; i < tempString.length; i++) {
				if (tempString[i].equals(mDetailinfo.city_detail)) {
					same = true;
					break;
				}
			}
			if (!same) {
				citys = citys+"/"+mDetailinfo.city_detail;
			}

		}else {
			citys = mDetailinfo.city_detail;
			showcity = mDetailinfo.city_name;
		}

		Editor editor = citySettings.edit();
		editor.putString(CITY_NAMES, citys);
		editor.putString(SHOW_CITY, showcity);
		editor.commit();
	}
	
    @SuppressWarnings("unused")
	public static ArrayList<Integer> getWidgetList (Context context){ 
    	ArrayList<Integer> idList = new ArrayList<Integer>();    	
    	
    	idList.clear();
    	
        final ContentResolver contentResolver = context.getContentResolver();
        final Cursor c = contentResolver.query(
                WeatherContentProvider.CONTENT_SHOWCITYS_URI, null, null, null, null);

        if (c != null) {
        	try {
            	final int appWidgetIdIndex = c.getColumnIndexOrThrow("AppwidgetId");
            	final int cityshowNameIndex = c.getColumnIndexOrThrow("showCitys");                       

                while (c.moveToNext()) {
                    try {                	
                    	int id = c.getInt(appWidgetIdIndex);
                    	String showcitys = c.getString(cityshowNameIndex);               	                   
                    	
                    	idList.add(id);
                    } catch (Exception e) {
                        Log.w("WeatherDatabaseAction", " items loading interrupted:", e);
                    }
                   
                }
            } finally {
            	if (c != null) {
            		 c.close();
    			}           
            }
		}       

        return idList;
    }

}