package com.quanta.weathertime;

import java.util.ArrayList;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.util.Log;

public class WeatherSettings extends PreferenceActivity //SherlockPreferenceActivity
	implements SharedPreferences.OnSharedPreferenceChangeListener{
	private String TAG = "settings";
	private ListPreference mAutoRefresh;
	public static final String SETTING_INFOS = "SETTING_Infos";
	@SuppressWarnings("deprecation")
	@Override
	    protected void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);

	        getPreferenceManager().setSharedPreferencesName(SETTING_INFOS);
	        addPreferencesFromResource(R.xml.weather_settings);
	        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	        mAutoRefresh = (ListPreference) findPreference(getString(R.string.auto_refresh_key));
	    }
	 
	@Override
	public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
		// TODO Auto-generated method stub
		Log.e(TAG, "key === " + getString(R.string.auto_refresh_key));
		if (key == getString(R.string.auto_refresh_key)) {
			Log.e(TAG, "KEY == auto_refresh ...");
			String update_interval = mAutoRefresh.getValue();
			Log.e(TAG, "update—interval === " + update_interval);
			
			Intent intent = new Intent(WeatherSettings.this, WeatherService.class);
			intent.putExtra("UPDATE_INTERVAL_CHANGED", update_interval);
			startService(intent);
		}
		
		if (key == getString(R.string.temperature_unit_key)) {
			Log.e(TAG, "KEY == temperature_unit ...");
			AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(WeatherSettings.this);
			ArrayList<Integer> appwidgetList = WeatherDatabaseAction.getWidgetList(WeatherSettings.this);
			for (int i = 0; i < appwidgetList.size(); i++) {
				int appwidgetid = appwidgetList.get(i);
				WeatherWidgetProvider.updateAppWidget(WeatherSettings.this, appWidgetManager, appwidgetid);
			}
		}
	}
}







