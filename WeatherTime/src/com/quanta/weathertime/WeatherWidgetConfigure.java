package com.quanta.weathertime;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

public class WeatherWidgetConfigure extends Activity { //implements Runnable
	public static final String TAG = "WeatherWidgetConfigure";
	private boolean debug = false;
    private static final String SAVE_CITY_CODE_DB= "save_city_code_db";
	int mAppWidgetId = -1;//AppWidgetManager.INVALID_APPWIDGET_ID;

    private Looper mlooper;
    @SuppressWarnings("unused")
	private InputStream stream;
    private static final int REPEAT_DIALOG = 0;
    private static final int SELECT_DIALOG = 1;
    @SuppressWarnings("unused")
	private boolean instead_db;
    private ProgressDialog mProgressDialog;

    private SharedPreferences citysSettings;
    private ArrayList<Integer> showcitys = new ArrayList<Integer>();

    private ArrayList<CityItemInfo> mCityInfoList = new ArrayList<WeatherWidgetConfigure.CityItemInfo>();
    private List<QueryCityItem> mQueryCityLists = null;
    private String cityName;
    private String cityShowName;
    private int currentIndex;

    private ListView mListView;
    private TextView nullCityView;
    private View loadView;
    private Bundle extras;
    @SuppressWarnings("unused")
	private String mCurrent;
    private String currentStr;

    private static int SHOW_CITYLIST = 0;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == SHOW_CITYLIST) {
            	CityListAdapter idListAdapter = null;
             	idListAdapter = new CityListAdapter(getApplicationContext(),mQueryCityLists);
             	mListView.setAdapter(idListAdapter);
             	if (mQueryCityLists.size() != 0) {
             		loadView.setVisibility(View.GONE);
             		mListView.setVisibility(View.VISIBLE);
             		nullCityView.setVisibility(View.GONE);
				}else {
					loadView.setVisibility(View.GONE);
					nullCityView.setVisibility(View.VISIBLE);
					if (cityName.length() == 0) {
						nullCityView.setText(null);
					}else {
						nullCityView.setText(R.string.city_invalid);
					}
				}
             	mListView.setOnItemClickListener(new OnItemClickListener() {
             		@Override
             		public void onItemClick(
             				AdapterView<?> arg0, View arg1,
             				int arg2, long arg3) {
             			// TODO Auto-generated method stub
             			String cityDetail;
             			mListView.setVisibility(View.GONE);
             			String showcityid = mQueryCityLists.get(arg2).getCityId();
             			if (mQueryCityLists.get(arg2).getAdminArea().length() == 0) {
             				cityDetail = mQueryCityLists.get(arg2).getCountry();
						}else {
							cityDetail = mQueryCityLists.get(arg2).getAdminArea() + ", " + mQueryCityLists.get(arg2).getCountry();
						}
             			cityShowName = mQueryCityLists.get(arg2).getCityName() + ", " + cityDetail;
             			int showcity = citysSettings.getInt(WeatherDatabaseAction.SHOW_CITY, 0);
             			ArrayList<CityItemInfo> cityinfoList = getItemInfoList();
             			boolean same = false;
             			for (int i = 1; i < cityinfoList.size(); i++) {
             				if (mQueryCityLists.get(arg2).getCityName().equalsIgnoreCase(cityinfoList.get(i).cityName) &&
             						cityDetail.equalsIgnoreCase(cityinfoList.get(i).cityDetail)) {
             					same = true;
             					Toast.makeText(WeatherWidgetConfigure.this, R.string.name_exist, Toast.LENGTH_SHORT).show();
             				}
             			}
	                    if (!same) {
	                    	currentIndex++;
	                    	showcity = currentIndex;
	                    	Editor editor = citysSettings.edit();
	                    	editor.putInt(WeatherDatabaseAction.SHOW_CITY, showcity);
	                    	editor.putInt(WeatherDatabaseAction.CITY_INDEX, currentIndex);
	                    	editor.putString(WeatherDatabaseAction.CITY_ID, showcityid);
	                    	editor.putString(WeatherDatabaseAction.CITY_DETAIL, cityDetail);
	                    	editor.putString(WeatherDatabaseAction.CITYS_SHOWIN_WIDGET, Integer.toString(currentIndex));
	                    	editor.commit();
//	                    	mAppWidgetId = currentIndex;
	                    	addInfoToDatabase(currentIndex, showcityid, cityShowName, mQueryCityLists.get(arg2).getCityName(),
	                    			cityDetail, mQueryCityLists.get(arg2).getGeoPosition());
                 			getWeatherInfo(cityShowName, showcityid, cityDetail, Integer.toString(currentIndex), false);
                 		 }
             		}
             	});
            }
        }
    };

	private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {

        @SuppressWarnings("unused")
		public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("com.quanta.weather.ACTION_UPDATE_CITY_CODE")) {
            	if (debug) {
            		Log.v(TAG,"BroadcastReceiver:onReceive");
				}
                String citydetail = intent.getStringExtra("citydetail");

                if (extras != null && !extras.containsKey("FROM_CITYWEATHERLIST")){
                	AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                	WeatherWidgetProvider.updateAppWidget(context, appWidgetManager,
                			mAppWidgetId);
                	
                	Intent resultValue = new Intent();
                	resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
                	WeatherWidgetConfigure.this.setResult(RESULT_OK, resultValue);
                }
			       finish();
            }
        }
    };

    @SuppressWarnings("deprecation")
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(this.getString(R.string.city_name));
        setResult(RESULT_CANCELED);

        /* Get city-code data from DB and display in Listview */
        setContentView(R.layout.weatherwidget_configure);

        mQueryCityLists = new ArrayList<QueryCityItem>();
        mListView = (ListView) findViewById(R.id.cityIdListView);
        nullCityView = (TextView) findViewById(R.id.null_city_data);
        loadView = findViewById(R.id.cityloading);

        currentStr = getResources().getString(R.string.current_location);
        Cursor cursor = getContentResolver().query(WeatherContentProvider.CONTENT_URI, null, null, null, null);
        if (cursor.getCount() == 0) {
			addInfoToDatabase(0, null, currentStr, currentStr, null, null);
		}
        cursor.close();

        citysSettings = getSharedPreferences(WeatherDatabaseAction.CITY_LIST_DETAIL, 0);

        currentIndex = citysSettings.getInt(WeatherDatabaseAction.CITY_INDEX, 0);
        mCityInfoList = getItemInfoList();

        /* Find the widget id from the intent */
        Intent intent = getIntent();
        extras = intent.getExtras();
        if (extras != null && !extras.containsKey("FROM_CITYWEATHERLIST")) {
        	add_Widget = true;
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
            citysSettings.edit().putInt(WeatherDatabaseAction.WIDGET_ID, mAppWidgetId).commit();
            if (mCityInfoList.size() > 0){//(citys != null && citys.length > 0) {
            	showcitys.clear();
            	showDialog(SELECT_DIALOG);

            }
        }
        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
        }

        final EditText editText = (EditText) findViewById(R.id.city_name);
        editText.setOnEditorActionListener(
        		new OnEditorActionListener() {
        			@Override
        			public boolean onEditorAction(TextView cityNameView, int actionId, KeyEvent event) {
        				if (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
        					mListView.setVisibility(View.GONE);
        					nullCityView.setVisibility(View.GONE);
        					loadView.setVisibility(View.VISIBLE);
        					cityName = cityNameView.getText().toString().trim();
        					new Thread(new Runnable() {
        						@Override
        						public void run() {
        							// TODO Auto-generated method stub
        							String cityid_url_prefix_1 =
        									"http://api.accuweather.com/locations/v1/search?q=";
        							String cityid_url_prefix2 = "&apikey=a115f94ccb7f488faa4e40a1d3c03532";
        							String code = WeatherService.getCode();
        							saveCityid(cityName, cityid_url_prefix_1, cityid_url_prefix2, code);
        						}
        					}).start();
        					InputMethodManager imm = (InputMethodManager) cityNameView.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        					if (imm.isActive()) {
        						imm.hideSoftInputFromWindow(cityNameView.getApplicationWindowToken(), 0);
        					}
        					return false;
        				}
        				return false;
        			}
        		}
        		);
    }

	public void getcityIdList(String quercityname) throws JSONException, IOException{
		File file = new File(getFilesDir() + "/" + quercityname + "cityId.json");
		if (file.exists()) {
			BufferedReader bfr = new BufferedReader(new FileReader(file));
			String tempString = null;
			String jsonString = "";
			while ((tempString = bfr.readLine())!=null) {
				jsonString = jsonString + tempString;
			}
			JSONArray jsonArray = new JSONArray(jsonString);
			mQueryCityLists.clear();
			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject item = jsonArray.getJSONObject(i);
				String cityName = item.getString("LocalizedName");
				String country = item.getJSONObject("Country").getString("LocalizedName");
				String adminarea= item.getJSONObject("AdministrativeArea").getString("LocalizedName");
				String cityid = item.getString("Key");
				String geoPosition = item.getJSONObject("GeoPosition").getString("Latitude")+","
						+item.getJSONObject("GeoPosition").getString("Longitude");
				QueryCityItem cityItemInfo = new QueryCityItem();
				cityItemInfo.setCityName(cityName);
				cityItemInfo.setAdminArea(adminarea);
				cityItemInfo.setCountry(country);
				cityItemInfo.setCityId(cityid);
				cityItemInfo.setGeoPosition(geoPosition);
				mQueryCityLists.add(cityItemInfo);
			}
			bfr.close();
		}
	}

	class QueryCityItem{
		String cityname;
		String adminArea;
		String country;
		String cityid;
		String geoPosition;
		public QueryCityItem(){

		}
		public void setAdminArea(String adminArea){
			this.adminArea = adminArea;
		}
		public String getAdminArea(){
			return adminArea;
		}
		public void setCityId(String cityid){
			this.cityid = cityid;
		}
		public String getCityId(){
			return cityid;
		}
		public void setCityName(String cityname){
			this.cityname = cityname;
		}
		public String getCityName(){
			return cityname;
		}
		public void setCountry(String country){
			this.country = country;
		}
		public String getCountry(){
			return country;
		}
		public void setGeoPosition(String geoPosition){
			this.geoPosition = geoPosition;
		}
		public String getGeoPosition(){
			return geoPosition;
		}
	}
	public void saveCityid(String cityNameStr, String url_prefix_1,String url_prefix_2, String code){
		RequestQueue queue = Volley.newRequestQueue(this);
		mQueryCityLists.clear();
		String cityurl;
		if (!cityNameStr.equalsIgnoreCase("")) {
			String name = null;
			try {
				name = URLEncoder.encode(cityNameStr, "UTF_8");
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			cityurl = url_prefix_1 + name + url_prefix_2 + code;
			JsonArrayRequest jsArrayRequest = new JsonArrayRequest(cityurl, new Response.Listener<JSONArray>() {
				
				@Override
				public void onResponse(JSONArray response) {
					// TODO Auto-generated method stub
					for (int i = 0; i < response.length(); i++) {
						try {
							JSONObject item = response.getJSONObject(i);
							String cityName = item.getString("LocalizedName");
							String country = item.getJSONObject("Country").getString("LocalizedName");
							String adminarea= item.getJSONObject("AdministrativeArea").getString("LocalizedName");
							String cityid = item.getString("Key");
							String geoPosition = item.getJSONObject("GeoPosition").getString("Latitude")+","
									+item.getJSONObject("GeoPosition").getString("Longitude");
							QueryCityItem cityItemInfo = new QueryCityItem();
							cityItemInfo.setCityName(cityName);
							cityItemInfo.setAdminArea(adminarea);
							cityItemInfo.setCountry(country);
							cityItemInfo.setCityId(cityid);
							cityItemInfo.setGeoPosition(geoPosition);
							mQueryCityLists.add(cityItemInfo);
						} catch (JSONException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					mHandler.sendEmptyMessage(SHOW_CITYLIST);
				}
			}, new Response.ErrorListener() {
				
				@Override
				public void onErrorResponse(VolleyError error) {
					// TODO Auto-generated method stub
					mHandler.sendEmptyMessage(SHOW_CITYLIST);
				}
			});
			queue.add(jsArrayRequest);
		}
	}

    private void addInfoToDatabase(int index, String cityid, String showname, String cityname, String citydetail, String geoPositon) {

        final ContentValues values = new ContentValues();
        final ContentResolver cr = getContentResolver();
        values.put("cityIndex", index);
        values.put("showName", showname);
        values.put("cityName", cityname);
        values.put("cityId", cityid);
        values.put("cityDetail", citydetail);
        values.put("geoPosition", geoPositon);

        @SuppressWarnings("unused")
		Uri result = cr.insert(WeatherContentProvider.CONTENT_URI, values);
    }

    private void addShowCitysToDatabase(int widgetid, String showcitys) {

        final ContentValues values = new ContentValues();
        final ContentResolver cr = getContentResolver();
        values.put("AppwidgetId", widgetid);
        values.put("showCitys", showcitys);

        @SuppressWarnings("unused")
		Uri result = cr.insert(WeatherContentProvider.CONTENT_SHOWCITYS_URI, values);
    }

    private ArrayList<CityItemInfo> getItemInfoList (){
    	ArrayList<CityItemInfo> itemInfos = new ArrayList<CityItemInfo>();
    	@SuppressWarnings("unused")
		Map<Integer, String> cityMap = new HashMap<Integer, String>();

    	itemInfos.clear();

        final ContentResolver contentResolver = this.getContentResolver();
        final Cursor c = contentResolver.query(
                WeatherContentProvider.CONTENT_URI, null, null, null, null);

        try {
        	final int cityindexIndex = c.getColumnIndexOrThrow("cityIndex");
        	final int cityshowNameIndex = c.getColumnIndexOrThrow("showName");
            final int cityNameIndex = c.getColumnIndexOrThrow("cityName");
            final int cityDetailIndex = c.getColumnIndexOrThrow("cityDetail");

            while (c.moveToNext()) {
                try {
                	CityItemInfo info = new CityItemInfo();
                	info.cityindex = c.getInt(cityindexIndex);
                	info.cityshowName = c.getString(cityshowNameIndex);
                	info.cityName = c.getString(cityNameIndex);
                	info.cityDetail = c.getString(cityDetailIndex);

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

     class CityItemInfo{
    	int cityindex;
    	String cityshowName;
    	String cityName;
    	String cityDetail;
    }

    protected void onResume() {
        super.onResume();
        if (debug) {
        	Log.v(TAG,"onResume()");
		}
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.quanta.weather.ACTION_UPDATE_CITY_CODE");
        this.registerReceiver(mIntentReceiver, filter);
    }

    public void onPause(){
    	super.onPause();
    	if (debug) {
    		Log.v(TAG,"onPause()");
		}
    	if (mIntentReceiver != null) {
            this.unregisterReceiver(mIntentReceiver);
        }
    }

    public void onStop(){
    	super.onStop();
    	if (debug) {
    		Log.v(TAG,"onStop()");
		}
    }

	public void onDestroy() {
		super.onDestroy();
		if(mlooper != null)
			mlooper.quit();
	}

    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case REPEAT_DIALOG: {
                mProgressDialog = new ProgressDialog(this);
                @SuppressWarnings("unused")
				Dialog dialog = new Dialog(this);
                //dialog.setTitle("Indeterminate");
                mProgressDialog.setMessage("Please wait while loading...");
                mProgressDialog.setIndeterminate(true);
                mProgressDialog.setCancelable(true);
                return mProgressDialog;
            }
            case SELECT_DIALOG:
            	return selectDialog();
        }
        return null;
    }

    @SuppressWarnings("deprecation")
	@Override
	protected void onPrepareDialog(int id, final Dialog dialog) {
		switch (id) {
		case SELECT_DIALOG:
			break;

		default:
			break;
		}
		super.onPrepareDialog(id, dialog);
	}

    boolean isSelectAll = false;
    boolean fromListAdapter = false;
    CheckBox selectAll;
    private Dialog selectDialog(){
    	Dialog selectDialog;
    	if (nullCityView.getTextSize()==18) {
    		selectDialog = new Dialog(this, R.style.Dialog_Fullscreen);
		}else {
			selectDialog = new Dialog(this);
		}
    	selectDialog.setTitle(R.string.stored_locations);
    	selectDialog.setContentView(R.layout.select_citys_dialog);

    	final ListView listView = (ListView) selectDialog.findViewById(R.id.city_list);
    	final selectCityAdapter adapter = new selectCityAdapter();
    	listView.setAdapter(adapter);
    	selectAll = (CheckBox) selectDialog.findViewById(R.id.select_all);
    	selectAll.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					isSelectAll = true;
					listView.setAdapter(adapter);
					fromListAdapter = false;
				}else {
					isSelectAll = false;
					if (!fromListAdapter) {
						listView.setAdapter(adapter);
					}

				}
			}
		});
    	selectDialog.findViewById(R.id.ok).setOnClickListener(new OnClickListener() {
    		
			@Override
			public void onClick(View v) {
				String tempcitysStr = null;
				String tempcityIndexsStr = null;
				String city_id = null;

				for (int i = 0; i < showcitys.size(); i++) {
				    Cursor cur = getContentResolver().query(WeatherContentProvider.CONTENT_URI,
				    			new String[]{WeatherColumns.WeatherInfo.CITY_ID}, 
				    			WeatherColumns.WeatherInfo.CITY_INDEX + "=" + showcitys.get(i), null, null);

				    if (cur != null) {
				    	cur.moveToNext();
						city_id = cur.getString(cur.getColumnIndex(WeatherColumns.WeatherInfo.CITY_ID));
					}
				    cur.close();

					if (i == 0) {
						tempcityIndexsStr = "" + showcitys.get(i);
						tempcitysStr = "" + city_id;
					}else {
						tempcityIndexsStr = tempcityIndexsStr+"/"+showcitys.get(i);
						tempcitysStr = tempcitysStr + "/" + city_id;
					}
				}
				/**
				 *if we did't choose any city, do nothing or show current location instead of empty layout if needed
				 */
				if (null == tempcityIndexsStr && null == tempcitysStr) {
					finish();   //do nothing
				}
				getWeatherInfo(tempcitysStr, tempcitysStr, null, tempcityIndexsStr, true);
				citysSettings.edit().putString(WeatherDatabaseAction.CITYS_SHOWIN_WIDGET, tempcityIndexsStr).commit();
			}
		});

        return selectDialog;
    }

	private class selectCityAdapter extends BaseAdapter{

		@Override
		public int getCount() {
			return mCityInfoList.size();
		}

		@Override
		public Object getItem(int position) {
			return mCityInfoList.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			if (position == 0) {
				showcitys.clear();
			}
			if (convertView == null) {
				convertView = LayoutInflater.from(WeatherWidgetConfigure.this).inflate(R.layout.check_select_all, null);
			}
			CityItemInfo itemInfo = mCityInfoList.get(position);

			TextView textView = (TextView) convertView.findViewById(R.id.name);
			if (position == 0) {
				if (!itemInfo.cityName.equals("Current Location")) {
					textView.setText(currentStr+"("+itemInfo.cityName+")");
				}else {
					textView.setText(currentStr);
				}
			}else {
				textView.setText(itemInfo.cityshowName);  
			}
			CheckBox checkBox = (CheckBox) convertView.findViewById(R.id.select_cb);
			//checkBox.setText(citys[position]);
			checkBox.setChecked(isSelectAll);
			if (checkBox.isChecked()) {
				if (!showcitys.contains(itemInfo.cityindex)) {
					showcitys.add(itemInfo.cityindex);
				}

			}else {
				if (showcitys.contains(itemInfo.cityindex)) {
					showcitys.remove(itemInfo.cityindex);
				}

			}
			checkBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {

				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

					if (isChecked) {

						if (!showcitys.contains(mCityInfoList.get(position).cityindex)) {
							showcitys.add(mCityInfoList.get(position).cityindex);
						}
						if (showcitys.size() == mCityInfoList.size()) {
							isSelectAll = true;
							selectAll.setChecked(isSelectAll);
						}
					}else {

						if (selectAll.isChecked()) {
							isSelectAll = false;
							fromListAdapter = true;
							selectAll.setChecked(isChecked);
						}

						if (showcitys.contains(mCityInfoList.get(position).cityindex)) {

							showcitys.remove(showcitys.indexOf(mCityInfoList.get(position).cityindex));
						}
					}
				}
			});
			return convertView;
		}

    }

	private boolean add_Widget = false;
	protected void getWeatherInfo(String cityname, String cityid, String citydetail, String cityindexs, boolean isList) {
		/*Start WeatherService*/
		if (add_Widget) {
			addShowCitysToDatabase(mAppWidgetId, cityindexs);
		}
		add_Widget = false;
		Bundle extras = getIntent().getExtras();
		if (extras != null && !extras.containsKey("FROM_CITYWEATHERLIST")){
			if (debug) {
				Log.v(TAG,"extras != null && !extras.containsKeyFROM_CITYWEATHERLIST");
			}
//			AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
//			
//			WeatherWidgetProvider.updateAppWidget(this, appWidgetManager,
//					mAppWidgetId);
			
			Intent intent = new Intent(WeatherWidgetConfigure.this, WeatherService.class);
			Bundle bundle = new Bundle();
			bundle.putString("city_name", cityname);
			bundle.putString("city_id", cityid);
			bundle.putString("city_detail", citydetail);
			bundle.putString("show_in", "true");
			bundle.putString("REQUERY_SPECIFIED_CITY", "");
			bundle.putInt("appwidget_id", mAppWidgetId);
			bundle.putBoolean("city_list", isList);
			intent.putExtras(bundle);
			startService(intent);
		}
		
		if (extras != null && extras.containsKey("FROM_CITYWEATHERLIST")){
			
			if (mCityInfoList.size() > 0) {
				for (int i=0; i < mCityInfoList.size(); i++){
					if (mCityInfoList.get(i).cityName.equalsIgnoreCase(cityname)) {
						AlertDialog.Builder builder = new AlertDialog.Builder(this);
						builder.setMessage(R.string.name_exist);
						builder.setTitle(R.string.remind);
						
						builder.create().show();
						//return;
					}
				}
			}
		}
		final Context context = WeatherWidgetConfigure.this;
		
		SharedPreferences settings = this.getSharedPreferences(SAVE_CITY_CODE_DB, 1);
		@SuppressWarnings("unused")
		int ncitycode_id = settings.getInt("configured_id", 1);
		
		/*Save configured city info*/
		
		if (extras != null && !extras.containsKey("FROM_CITYWEATHERLIST")){
			AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
			
			WeatherWidgetProvider.updateAppWidget(context, appWidgetManager,
					mAppWidgetId);
			
			Intent resultValue = new Intent();
			resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
			setResult(RESULT_OK, resultValue);
			finish();
		}
		
		if (extras != null && extras.containsKey("FROM_CITYWEATHERLIST")){
			Bundle bundle = new Bundle();
			//bundle.putString(key, value);
			bundle.putInt("data", currentIndex);
			bundle.putString("city_id", cityid);
			
			Intent intent = new Intent();
			intent.putExtras(bundle);
			setResult(RESULT_OK,intent);
			finish();
		}
	}

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
    	super.onConfigurationChanged(newConfig);
    }

}