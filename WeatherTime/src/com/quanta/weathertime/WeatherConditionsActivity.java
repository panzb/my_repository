
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

import android.app.Activity;
import android.app.Fragment;
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
import android.net.Uri;
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
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.CursorAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.quanta.weathertime.WeatherDatabaseAction.Forecase_conditions;
import com.quanta.weathertime.location.Coordinates;
import com.quanta.weathertime.location.LocationGetter;

public class WeatherConditionsActivity extends Activity implements OnItemClickListener, OnInitListener, OnUtteranceCompletedListener{
    private static final String TAG = "WeatherConditiosActivity";
    private boolean debug = false;

    public static final String SETTING_INFOS = "SETTING_Infos";
    public static final String UPDATE_INTERVAL_IN_SECONDS = "update_interval";
    public static final String ACTION_UPDATE = "com.quanta.weather.ACTION_UPDATE_WEATHER";
    // public static final String SHOW_CITY = "show_city";

    private static final int CONTEXTMENU_DELETE = 0;
    @SuppressWarnings("unused")
	private static final int CONTEXTMENU_VIEW = 1;

    public static final int MSG = 1;
    public static final int UPDATE = 0;
    public static final int SETTINGS = 1;
    public static final int ADD = 2;
    public static final int REMOVEALL = 3;
    public static final int LOCATION = 4;
    public static final int UPDATE_ALL = 5;

    private ViewPager viewPager;
    private viewPagerAdapter pagerAdapter;
    private List<View> viewPagerList;
    private View detailView;
    
    private Fragment detailFragment;
    private MenuItem mRefreshIcon;
    private MenuItem mSettingsIcon;
    private MenuItem mSpeechIcon;
    private Boolean Is_Loading = false;
    private ListView mCityListView;
    private TextView mCityNameView;
    @SuppressWarnings("unused")
	private TextView mMarkView;
    private TextView mTimeView;
    private TextView mTempView;
    private TextView mConditionView;
    private TextView mHumView;
    private TextView mWindView;
    private ImageView mWeatherIconView;
    private GridView mExtendForeCast;

    private TextView mExtendDateView;
    private TextView mLowView;
    private TextView mHighView;
    private ImageView mExtendIconView;
    private TextView nullDataView;
    private View mLoadingView;
    private CityItemAdapter mAdapter;
    public static Cursor mCursor;
    private Boolean addFromCityListUI = false;

    private WeatherDatabaseAction.Detailinfo mDetailinfo = null;
    private ArrayList<Forecase_conditions> mForecastConditons = new ArrayList<WeatherDatabaseAction.Forecase_conditions>();
    private SharedPreferences mSettings;
    private SharedPreferences citySettings;
    // private String storeLocations;
    private String unit;
    private String current_temp;
    //private String showCityName;
    private String mCurrentLoc;
    private String mCurrentSLoc;
    private int showCityIndex;
    private String cityId;
    private String mCurrentLocationStr;

    private LocationGetter mLocationGetter;

    public static double longitude;
    public static double latitude;

    private ArrayList<CityItemInfo> locations = new ArrayList<CityItemInfo>();

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
        	if (debug) {
        		Log.v(TAG, "BroadcastReceiver:onReceive " + intent.getAction());
			}
            if (intent.getAction().equals("com.quanta.weather.ACTION_UPDATE_WEATHER_INFO")) {
            	if (debug) {
            		Log.v(TAG, "BroadcastReceiver:onReceive "+showCityIndex);
				}

                getCityList();
                /*
                Cursor cursor = getContentResolver().query(WeatherContentProvider.CONTENT_URI, null, null, null, null);
                mCityListView.setAdapter(new CityItemAdapter(context, cursor));
                */
                if (detailFragment == null) {
        		}else {
        			setViewValue(showCityIndex);
        			viewPager.setCurrentItem(showCityIndex);
        		}

                citySettings.edit().putInt(WeatherDatabaseAction.SHOW_CITY, showCityIndex)
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        mLocationGetter = new LocationGetter(this);
        setContentView(R.layout.city_weather_layout);
        detailFragment = this.getFragmentManager().findFragmentById(R.id.detail_fragment);
        mDetailinfo = new WeatherDatabaseAction.Detailinfo();
        
        addFromCityListUI = true;
        mCurrentLocationStr = getResources().getString(R.string.current_location);
        Cursor cur = getContentResolver().query(WeatherContentProvider.CONTENT_URI, null, null, null, null);
        if (cur.getCount() == 0) {
        	addInfoToDatabase(0, "Current Location", "Current Location");
        }
        cur.close();
        mCursor = getContentResolver().query(WeatherContentProvider.CONTENT_URI, null, null, null, null);
        mAdapter = new CityItemAdapter(this, mCursor);
        mSettings = getSharedPreferences(SETTING_INFOS, 0);
        citySettings = getSharedPreferences(WeatherDatabaseAction.CITY_LIST_DETAIL, 0);
        mCurrentLoc = citySettings.getString(WeatherDatabaseAction.CURRENT, null);
        getCityList();
        getShowCity();
        findComponent();
        mtts = new TextToSpeech(getApplicationContext(), this);
//        mtts.setOnUtteranceCompletedListener(this);
    }

    protected void onResume() {
        super.onResume();
        Log.e(TAG, "onResume  ......");
        getCityList();
        findComponent();
        if (addFromCityListUI) {
        	
        }else {
        	if (detailFragment == null) {
        		showCityIndex = WeatherInfoDetail.showcity;
        	}
        }
        addFromCityListUI = false;
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
    	mtts.shutdown();
    }

    @SuppressWarnings("deprecation")
	private void updateUI(){
    	Log.e(TAG, "updateUI ......");
        if (citySettings.getString(WeatherDatabaseAction.CURRENT, null) == null) {
        	updateInfoToDatabase(0,"Current Location",null);
		}

        detailFragment = this.getFragmentManager().findFragmentById(R.id.detail_fragment);
        mCursor.requery();
		unit = mSettings.getString("Temperature Unit", "C");
		if (detailFragment == null) {

		}else {
			if (Is_Loading) {
				mLoadingView.setVisibility(View.GONE);
				nullDataView.setVisibility(View.VISIBLE);
			}else {
				setViewValue(showCityIndex);
				viewPager.setCurrentItem(showCityIndex);
			}
		}
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
    	mMarkView = (TextView) view.findViewById(R.id.mark);
    }

    private void findComponent() {
        mCityListView = (ListView) findViewById(R.id.city_list);
        mCityListView.setAdapter(mAdapter);
        mCityListView.setOnItemClickListener(this);
        if (detailFragment == null) {
        	
        }else {
        	viewPager = (ViewPager) findViewById(R.id.pager);
        	viewPagerList = new ArrayList<View>();
        	for (int i = 0; i < locations.size(); i++) {
        		View view = getLayoutInflater().inflate(R.layout.detail_fragement, null);
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
        			if (oldView != null) {
        				oldView.setBackgroundDrawable(null);
        			}
        			mCityListView.getFirstVisiblePosition();
        			View view = mCityListView.getChildAt(position-mCityListView.getFirstVisiblePosition());
        			if (debug) Log.e(TAG, "view =" + mCityListView.getChildAt(position-mCityListView.getFirstVisiblePosition()));
        			if (null != view) {
        				view.setBackgroundResource(R.drawable.list_selected_holo_dark);
        			}
        			oldView = view;
        			showCityIndex = position;
        			citySettings.edit().putInt(WeatherDatabaseAction.SHOW_CITY, showCityIndex).commit();
        			setViewValue(showCityIndex);
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
        
        mCityListView.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
            public void onCreateContextMenu(ContextMenu conMenu, View view, ContextMenuInfo info) {
            	if (debug) {
            		Log.v(TAG, "onCreateContextMenu");
				}
                AdapterView.AdapterContextMenuInfo menuinfo = (AdapterView.AdapterContextMenuInfo) info;
                if (menuinfo.position == 0) {
					return;
				}
                conMenu.setHeaderIcon(R.drawable.delete);
                conMenu.setHeaderTitle(R.string.app_name);
                conMenu.add(0, CONTEXTMENU_DELETE, 0, R.string.delete);
            }
        });
    }
    
    private void addInfoToDatabase(int index, String showname, String cityname) {
    	
        final ContentValues values = new ContentValues();
        final ContentResolver cr = getContentResolver();
        values.put("cityIndex", index);
        values.put("showName", showname);
        values.put("cityName", cityname);
        @SuppressWarnings("unused")
		Uri result = cr.insert(WeatherContentProvider.CONTENT_URI, values);
    }

    private void getCityList() {
        locations.clear();
        locations = getItemInfoList();
    }

    private void getShowCity() {
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
//            showCityIndex = getIntent().getIntExtra("CITYCODE_ID", "0");
        	for (int i = 0; i < locations.size(); i++) {
				if (locations.get(i).cityindex == getIntent().getIntExtra("CITY_INDEX", 0)) {
					showCityIndex = i;
				}
			}
//            showCityIndex = getIntent().getIntExtra("CITY_INDEX.", 0);
//            showCityIndex = getIntent().getIntExtra("data", 0);
            citySettings.edit().putInt(WeatherDatabaseAction.SHOW_CITY, showCityIndex).commit();
        } else {
            showCityIndex = citySettings.getInt(WeatherDatabaseAction.SHOW_CITY, 0);
        }
    }

    private void setViewValue(int index) {
    	findViewItem(index);
    	locations = getItemInfoList();
//    	String show_name = null;
    	String city_name = null;
    	String currentCondition = null;
    	String forecast = null;
        Cursor cur = getContentResolver().query(WeatherContentProvider.CONTENT_URI,
                new String[] { WeatherColumns.WeatherInfo.CITY_SHOW_NAME, WeatherColumns.WeatherInfo.CITY_NAME,
        			WeatherColumns.WeatherInfo.CURRENT_CONDITION, WeatherColumns.WeatherInfo.FORECAST},
//                WeatherColumns.WeatherInfo.CITY_INDEX + "=" + index,
                WeatherColumns.WeatherInfo.CITY_INDEX + "=" + locations.get(index).cityindex,
                null, null);

        if (cur != null) {
            if (cur.getCount() > 0) {
                cur.moveToNext();
//                show_name = cur.getString(cur.getColumnIndex(WeatherColumns.WeatherInfo.CITY_SHOW_NAME));
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
                mLoadingView.setVisibility(View.INVISIBLE);
                return;
            } else {
            	if (null != currentCondition && null != forecast) {
					parseJSON(currentCondition, forecast);
            	}else {
//            		mDetailinfo = null;
            	}
            }
        } catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        if ((currentCondition == null || forecast == null) && Is_Loading == false) {
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

    private CharSequence convertTempUnit(String temp) {
        String style = this.getString(R.string.deg);
        String format = null;
        format = String.format(style, temp, unit);
        CharSequence text = Html.fromHtml(format);
        return text;
    }

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
            getCityList();
            if (index == locations.get(showCityIndex).cityindex) {
            	if (oldView != null) {
    				oldView.setBackgroundDrawable(null);
    			}
            	view.setBackgroundResource(R.drawable.list_selected_holo_dark);
            	oldView = view;
			}
            view.setTag(index);
            if (index == 0) {
            	if (!name.equalsIgnoreCase("Current Location")) {
            		name = mCurrentLocationStr + "-" + name;
				}else {
					name = mCurrentLocationStr;
				}
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
            convertView = LayoutInflater.from(WeatherConditionsActivity.this).inflate(
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
            } else if (unit.equals("F")) {   //&& mDetailinfo.unit_system.equals("SI")
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
    
    @SuppressWarnings("deprecation")
	public boolean onContextItemSelected(MenuItem aItem) {
    	if (debug) {
    		Log.v(TAG, "onContextItemSelected");
		}
        AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) aItem.getMenuInfo();

        Cursor cur = (Cursor) mCityListView.getItemAtPosition(menuInfo.position);
//        String cityname = cur.getString(cur.getColumnIndex(WeatherColumns.WeatherInfo.CITY_SHOW_NAME));
        int index = cur.getInt(cur.getColumnIndex(WeatherColumns.WeatherInfo.CITY_INDEX));
        if (menuInfo.position == 0) {
            return false;
        }
        switch (aItem.getItemId()) {
            case CONTEXTMENU_DELETE:
            	deleteInfoFromDatabase(index);
//            	if (detailFragment == null) {
//            		
//            	}else {
//            		viewPagerList.remove(menuInfo.position);
//            		viewPager.setAdapter(pagerAdapter);
//				}
//                if (locations.get(showCityIndex).cityindex == index) {
            	if (menuInfo.position == showCityIndex) {
                	showCityIndex = 0;
                    citySettings.edit().putInt(WeatherDatabaseAction.SHOW_CITY, showCityIndex)
                            .commit();
                }else if (menuInfo.position < showCityIndex) {
                	showCityIndex = showCityIndex-1;
				}
            	
            	if (detailFragment == null) {
            		
            	}else {
            		viewPagerList.remove(menuInfo.position);
            		viewPager.setAdapter(pagerAdapter);
            		setViewValue(showCityIndex);
            		viewPager.setCurrentItem(showCityIndex);
            	}
                getCityList();
                mCursor.requery();
                /*
                Cursor cursor = getContentResolver().query(WeatherContentProvider.CONTENT_URI, null, null, null, null);
                mCityListView.setAdapter(new CityItemAdapter(this, cursor));
                */

                ArrayList<Integer> idList = WeatherDatabaseAction.getWidgetList(this);
                for (int i = 0; i < idList.size(); i++) {
                	Cursor idCursor = getContentResolver().query(WeatherContentProvider.CONTENT_SHOWCITYS_URI,
                			new String[]{"showCitys"}, "AppwidgetId" + "=" + idList.get(i), null, null);
                	String tmp = null;
                	if (idCursor != null) {
                	    if (idCursor.getCount() > 0) {
                	        idCursor.moveToNext();
                	        tmp = idCursor.getString(idCursor.getColumnIndex("showCitys"));
                	    }
                	    idCursor.close();
            		}

                    if (tmp != null) {
                        String[] citysShowIn = tmp.split("/");
                        ArrayList<String> citysShowList = new ArrayList<String>();
                        for (int j = 0; j < citysShowIn.length; j++) {
                            if (!Integer.toString(index).equals(citysShowIn[j])) {
                                citysShowList.add(citysShowIn[j]);
                            }
                        }
                        String citysShowInStr = null;
                        for (int j = 0; j < citysShowList.size(); j++) {
                            if (j == 0) {
                                citysShowInStr = citysShowList.get(j);
                            } else {
                                citysShowInStr = citysShowInStr + "/" + citysShowList.get(j);
                            }
                        }
                        //in activity,delete all citys shown in widget,we can show current location in widget if needed
//                        if (null == citysShowInStr || citysShowInStr.equals("")) {
//							citysShowInStr = "0";
//						}
                        updateShowCitysToDatabase(idList.get(i), citysShowInStr);
                    }
				}

                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
				for (int i = 0; i < idList.size(); i++) {
					int appwidgetid = idList.get(i);
					WeatherWidgetProvider.updateAppWidget(this, appWidgetManager, appwidgetid);
				}
                break;
        }
        return false;
    }

	private void deleteInfoFromDatabase(int index) {
        final ContentResolver cr = getContentResolver();
        String where = "cityIndex" + "=" + index;
        cr.delete(WeatherContentProvider.CONTENT_URI, where, null);
    }

    private void updateInfoToDatabase(int index, String cityname, String geoPosition) {

        final ContentValues values = new ContentValues();
        final ContentResolver cr = getContentResolver();

        values.put("cityName", cityname);
        values.put("geoPosition", geoPosition);
        Log.e(TAG, "cityname and geoPosition " + cityname +" "+geoPosition);
        String where = "cityIndex" + "=" + index;
        cr.update(WeatherContentProvider.CONTENT_URI, values, where, null);
    }

    private void updateShowCitysToDatabase(int widgetid, String showcitys) {

        final ContentValues values = new ContentValues();
        final ContentResolver cr = getContentResolver();

        values.put("showCitys", showcitys);

        String where = "AppwidgetId" + "=" + widgetid;
        cr.update(WeatherContentProvider.CONTENT_SHOWCITYS_URI, values, where, null);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.weather_conditions_options, menu);
        return true;
    }

    public boolean onPrepareOptionsMenu(Menu menu) {
        mRefreshIcon = menu.findItem(R.id.update);
        mSettingsIcon = menu.findItem(R.id.settings);
        mSpeechIcon = menu.findItem(R.id.speech);
        if (detailFragment == null) {
			mRefreshIcon.setVisible(false);
			mSettingsIcon.setVisible(false);
		}
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
    private TextToSpeech mtts;
    public void Speech_weather(MenuItem item){
    	if (mtts.isSpeaking()) {
			mtts.stop();
			item.setIcon(R.drawable.title_voice);
    	}else {
    		String wind_direction = mDetailinfo.wind_condition;
    		int w_lenth = wind_direction.length();
    		String direction_1 = "";
    		String direction_2 = "";
    		String direction_3 = "";
    		Log.e(TAG, "wind_direction.charat(0) === " + wind_direction.charAt(0));
    		if (w_lenth >= 1&&(Locale.getDefault()==Locale.US)) {
    			switch (mDetailinfo.wind_condition.charAt(0)) {
    			case 'N':
    				direction_1 = "north";
    				break;
    			case 'S':
    				direction_1 = "south";
    				break;
    			case 'E':
    				direction_1 = "east";
    				break;
    			case 'W':
    				direction_1 = "west";
    				break;
    			case 'C':
    				direction_1 = "no wind";
    				break;
    			default:
    				break;
    			}
    			wind_direction = direction_1;
    		}
    		if (w_lenth >= 2&&(Locale.getDefault()==Locale.US)) {
    			Log.e(TAG, "w_lenth >=2......");
    			switch (mDetailinfo.wind_condition.charAt(1)) {
    			case 'N':
    				direction_2 = "north";
    				break;
    			case 'S':
    				direction_2 = "south";
    				break;
    			case 'E':
    				direction_2 = "east";
    				break;
    			case 'W':
    				direction_2 = "west";
    				break;
    			default:
    				break;
    			}
    			wind_direction = direction_1 + direction_2;
    		}
    		if (w_lenth >= 3&&(Locale.getDefault()==Locale.US)) {
    			Log.e(TAG, "w_lenth >=3......");
    			switch (mDetailinfo.wind_condition.charAt(2)) {
    			case 'N':
    				direction_3 = "north";
    				break;
    			case 'S':
    				direction_3 = "south";
    				break;
    			case 'E':
    				direction_3 = "east";
    				break;
    			case 'W':
    				direction_3 = "west";
    				break;
    			default:
    				break;
    			}
    			wind_direction = direction_1 + " by " + direction_2 + direction_3;
    		}
    		Log.e(TAG, "wind_direction ===== " + wind_direction);
    		
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
//			String text = "天气为您播报"+ mCityNameView.getText() + "," +DayNightStr+","+ mConditionView.getText() + "," + "温度"
//			+ mForecastConditons.get(0).low + "度到" + mForecastConditons.get(0).high + "度" + "," + "风速 ，"+mDetailinfo.wind_speed;
    		
    		String text = "Weather report,"+mCityNameView.getText()+","+DayNightStr +","+mConditionView.getText()
    				+","+" temperature "+mForecastConditons.get(0).low+" degree to "+mForecastConditons.get(0).high+" degree,"
    				+"wind direction,"+wind_direction+ "," + "wind speed," + mDetailinfo.wind_speed;
//					+wind_direction+" wind "+" grade 3 to 4";
    		Log.e("WeatherConditionsActivity", "text === " + text);
    		item.setIcon(R.drawable.title_voice_stop);
    		HashMap<String, String> myHashAlarm = new HashMap<String, String>();
    		myHashAlarm.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(AudioManager.STREAM_NOTIFICATION));
    		myHashAlarm.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,
    				"utteranceId");
    		mtts.speak(text, TextToSpeech.QUEUE_FLUSH, myHashAlarm);
    	}
    }
    
	@Override
	public void onInit(int status) {
		   // status can be either TextToSpeech.SUCCESS or TextToSpeech.ERROR.
        if (status == TextToSpeech.SUCCESS) {
            // Set preferred language to US english.
            // Note that a language may not be available, and the result will indicate this.
            int result = mtts.setLanguage(Locale.getDefault());
            // Try this someday for some interesting results.
            if (result == TextToSpeech.LANG_MISSING_DATA ||
                result == TextToSpeech.LANG_NOT_SUPPORTED) {
               // Lanuage data is missing or the language is not supported.
//                mtts.setLanguage(Locale.US);
                Log.e("MessageViewFragment", "Language is not available.");
            } else {
                // Check the documentation for other possible result codes.
                // For example, the language may be available for the locale,
                // but not for the specified country and variant.

                // The TTS engine has been successfully initialized.
            }
            /**
             * There are a number of issues you must overcome to get it work nicely.
             * They are:
             * 1.  Always set the UtteranceId (or else OnUtteranceCompleted will not be called)
             * 2.  setting OnUtteranceCompleted listener (oytnly after the speech system is properly initialized)
             */
            mtts.setOnUtteranceCompletedListener(this);  //must be registered after init text-to-speech
        } else {
            // Initialization failed.
            Log.e("WeatherConditionActivity", "Could not initialize TextToSpeech.");
        }
	}
	
	@Override
	public void onUtteranceCompleted(String utteranceId) {
		// TODO Auto-generated method stub
		runOnUiThread(new Runnable() {  //must change UI in UiThread
			@Override
			public void run() {
				//UI changes
				mSpeechIcon.setIcon(R.drawable.title_voice);
			}
		});
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
                    CityItemInfo cityInfo = locations.get(i);
                    if (cityInfo.cityindex == 0){
                        getCurrentLocation();
                    }  
                }
                startWeatherService(showCityIndex, cityId, "REQUERY_ALL", mCurrentLoc, mCurrentSLoc);
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
        addFromCityListUI = true;
        Intent intent = new Intent(this, WeatherWidgetConfigure.class);
        intent.putExtra("FROM_CITYWEATHERLIST", "");
        startActivityForResult(intent, MSG);
    }

    @SuppressWarnings("unused")
	private void ClickMenuItem_removeall() {
    	if (debug) {
    		Log.v(TAG, "ClickMenuItem_removeall");
		}
    }

    private void getCurrentLocation() {
    	InitLocation();
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
    	
    	if (debug) {
    		Log.e(TAG, " long & lat " + longitude + " " + latitude);
    	}
    	String geoPosition = latitude+","+longitude;
//    	String geoPosition = "25.028373,121.36528";  //龟山乡
//    	String geoPosition = "25.056989,121.375237"; //长庚村  显示dahua(大华村)
//    	String geoPosition = "25.056678,121.374893"; //QCI
    	if (geoPosition != null) {
    		Log.e(TAG, "do this part .......");
    		updateInfoToDatabase(0, "Current Location", geoPosition);
    	}
    }

    private void startWeatherService(int cityindex, String cityid, String key, String current, String currentS) {
    	String cityname = null;
    	locations = getItemInfoList();
    	 for (int i = 0; i < locations.size(); i++) {
    		 if (locations.get(i).cityindex == cityindex) {  //此处cityindex为数据库中的index
//    		 if (i == cityindex) {   
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

    @SuppressWarnings({ "deprecation", "unused" })
	protected void onActivityResult(int requestCode, int resultCode,
            Intent intent) {
        switch (requestCode) {
            case MSG: {
                if (resultCode == RESULT_OK) {
                	Is_Loading = true;
//                	mLoadingView.setVisibility(View.VISIBLE);
                	if (debug) {
                		Log.e(TAG, "onActivityResult " + intent.getIntExtra("data", 0));
					}
                    String city_name = null;//intent.getCharSequenceExtra("data").toString();
                    int cityindex = intent.getIntExtra("data", 0);
                    showCityIndex = cityindex;
                    cityId = intent.getStringExtra("city_id");
                    getCityList();
//                    Cursor cursor = getContentResolver().query(WeatherContentProvider.CONTENT_URI, null, null, null, null);
//                    mCityListView.setAdapter(new CityItemAdapter(this, cursor));
                    mCursor.requery();
                    if (null == detailFragment) {
                    	
                    }else {
                    	View view = getLayoutInflater().inflate(R.layout.detail_fragement, null);
                    	viewPagerList.add(view);
                    	viewPager.setCurrentItem(pagerAdapter.getCount()-1);
//                    	viewPagerList.get(pagerAdapter.getCount()-1).findViewById(R.id.loading).setVisibility(View.VISIBLE);
					}
                    //mCityListView.setAdapter(mAdapter);
                    startWeatherService(showCityIndex, cityId, "REQUERY_SPECIFIED_CITY", null, null);
                }
                break;
            }
        }
    }

    void parseJSON(String currentCondition, String forecast) throws JSONException {
    	// TODO: switch to sax
    	mForecastConditons.clear();
//    	mDetailinfo = new WeatherDatabaseAction.Detailinfo();
    	WeatherDatabaseAction.Forecase_conditions condition = null;
    	JSONArray jsonArray = new JSONArray(currentCondition);
    	for (int i = 0; i < jsonArray.length(); i++) {
    		mDetailinfo.condition=jsonArray.getJSONObject(i).getString("WeatherText");
    		mDetailinfo.icon_data=jsonArray.getJSONObject(i).getString("WeatherIcon");
    		mDetailinfo.day_night=jsonArray.getJSONObject(i).getString("IsDayTime");
    		mDetailinfo.temp_c=jsonArray.getJSONObject(i).getJSONObject("Temperature").getJSONObject("Metric").getString("Value");
    		mDetailinfo.temp_f=jsonArray.getJSONObject(i).getJSONObject("Temperature").getJSONObject("Imperial").getString("Value");
    		mDetailinfo.wind_condition=jsonArray.getJSONObject(i).getJSONObject("Wind").getJSONObject("Direction").getString("Localized");
    		mDetailinfo.wind_speed=jsonArray.getJSONObject(i).getJSONObject("Speed").getJSONObject("Metric").getString("Value");
//    		mDetailinfo.wind_condition=getString(R.string.wind_direction)+jsonArray.getJSONObject(i).getJSONObject("Wind").getJSONObject("Direction").getString("Localized");
    		mDetailinfo.humidity=getString(R.string.humidity)+jsonArray.getJSONObject(i).getString("RelativeHumidity");
    		mDetailinfo.current_datetime=jsonArray.getJSONObject(i).getString("LocalObservationDateTime");
    		String temps_1 = mDetailinfo.current_datetime.substring(0, 10);
    		String temps_2 = mDetailinfo.current_datetime.substring(11,19);
    		mDetailinfo.current_datetime = getString(R.string.update_time) + " " + temps_1 + " " + temps_2;
    	}
    	JSONObject jsonObject = new JSONObject(forecast);
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

    @SuppressWarnings("unused")
	private String getIconName(String path) {
        String[] str = path.split("/");
        int len = str.length;
        String temp = str[len - 1];
        String name = temp.substring(0, temp.indexOf("."));
        return name;
    }

    View oldView;

    @Override
    public void onItemClick(AdapterView<?> arg0, View view, int position,
            long id) {
//        Cursor cursor = (Cursor) arg0.getItemAtPosition(position);
        if (oldView != null) {
            // oldView.findViewById(R.id.check_select).setVisibility(View.INVISIBLE);
            oldView.setBackgroundDrawable(null);
        }
        oldView = view;
        view.setBackgroundResource(R.drawable.list_selected_holo_dark);

        showCityIndex = position;
//        showCityIndex = cursor.getInt(cursor.getColumnIndex(WeatherColumns.WeatherInfo.CITY_INDEX));
        citySettings.edit().putInt(WeatherDatabaseAction.SHOW_CITY, showCityIndex).commit();
        if (detailFragment == null) {
            Intent intent = new Intent(this,WeatherInfoDetail.class);
            if (debug) {
            	Log.e(TAG, "showcityindex = " + showCityIndex);
			}
            intent.putExtra("showcity", showCityIndex);
            intent.putExtra("cityid", cityId);
            startActivity(intent);
		}else {
			setViewValue(showCityIndex);
			viewPager.setCurrentItem(position);
		}
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

    @SuppressWarnings("deprecation")
	@Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        setContentView(R.layout.city_weather_layout);
        findComponent();
        if (locations.size() > 0) {
        	mCursor.requery();
            showCityIndex = citySettings.getInt(WeatherDatabaseAction.SHOW_CITY, 0);
            if (detailFragment == null) {

    		}else {
    			setViewValue(showCityIndex);
    			viewPager.setCurrentItem(showCityIndex);
    		}
        }
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
}
