package com.quanta.weathertime;

import android.net.Uri;
import android.provider.BaseColumns;

public class WeatherColumns {
	private WeatherColumns() {}

	public static final String AUTHORITY  = "com.quanta.weather.WeatherContentProvider";
	
	private static final String	TABLE_CITY_CODE = "cityinfo";
	private static final String TABLE_CONFIGURED_WIDGET = "configuredwidget";
	private static final String TABLE_WEATHER_DETAIL_INFO = "weatherdetailinfo";

   public static final class WeatherInfo implements BaseColumns {
	    private WeatherInfo() {}
    	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);
    	public static final Uri CONTENT_CITY_CODE_URI = Uri.parse("content://" + AUTHORITY + "/" + TABLE_CITY_CODE);
    	public static final Uri CONTENT_ONFIGURED_WIDGET_URI = Uri.parse("content://" + AUTHORITY + "/" + TABLE_CONFIGURED_WIDGET);
    	public static final Uri CONTENT_WEATHER_DETAIL_INFO_URI = Uri.parse("content://" + AUTHORITY + "/" + TABLE_WEATHER_DETAIL_INFO);
        
        /*TABLE:citycode*/
        public static final String  CITYNAME  = "cityname";
        public static final String  CITYID  = "cityid";  
        
        /*TABLE:configuredwidget*/ 
        public static final String  CONFIGURED_WIDGET_ID  = "configured_widget_id";
        public static final String	CONFIGURED_CITYCODE_ID = "configured_citycode_id";
        
        /*TABLE:weatherdetailinfo*/
        public static final String  DETAIL_CITYCODE_ID = "detail_citycode_id";
        
        public static final String  UNIT_TEMPERATURE  = "unit_temperature";
        public static final String  UNIT_DISTANCE  = "unit_distance";  
        public static final String  UNIT_PRESSURE = "unit_pressure";
        public static final String  UNIT_SPEED = "unit_speed";
        
        public static final String  SPEED  = "speed";
        public static final String  HUMIDITY  = "humidity";  
        public static final String  VISIBILITY = "visibility";
        public static final String  PRESSURE = "pressure";     
        public static final String  RISING  = "rising";
        
        public static final String  SUNRISE  = "sunrise";  
        public static final String  SUNSET = "sunset";
        
        public static final String  TITLE = "title";
        
        public static final String  PUBDATE  = "pubDate";
        
        public static final String  TEXT  = "text";  
        public static final String  CODE = "code";
        public static final String  TEMP = "temp";     
        public static final String  DATE  = "date";
        
        public static final String  TOMORROW = "tomorrow";
        public static final String  TOMORROW_DATE = "tomorrow_date";      
        public static final String  TOMORROW_LOW  = "tomorrow_low";      
        public static final String  TOMORROW_HIGH  = "tomorrow_high";  
        public static final String  TOMORROW_TEXT = "tomorrow_text";
        public static final String  TOMORROW_CODE = "tomorrow_code"; 
        
        public static final String  TODAY = "today";
        public static final String  TODAY_DATE = "today_date";      
        public static final String  TODAY_LOW  = "today_low";      
        public static final String  TODAY_HIGH  = "today_high";  
        public static final String  TODAY_TEXT = "today_text";
        public static final String  TODAY_CODE = "today_code";  
        
        public static final String CITY_INDEX = "cityIndex";
        public static final String CITY_SHOW_NAME = "showName";
        public static final String CITY_NAME = "cityName";
        //payne
        public static final String CURRENT_CONDITION = "currentCondition";
        public static final String FORECAST = "forecast";
        public static final String CITY_GEOPOSITION = "geoPosition";
        public static final String CITY_DETAIL = "cityDetail";
        public static final String CITY_ID = "cityId";
        public static final String CITY_LOCAL_TIME = "cityLocalTime";
    }
}
