package com.quanta.weathertime;

import java.util.List;

import com.quanta.weathertime.WeatherWidgetConfigure.QueryCityItem;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class CityListAdapter extends BaseAdapter {

	LayoutInflater minflater;
	private List<QueryCityItem> mcityidinfo = null;
	public CityListAdapter(Context context ,List<QueryCityItem> idList ){
		minflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mcityidinfo = idList ;
	}
	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return mcityidinfo.size();
	}

	@Override
	public Object getItem(int arg0) {
		// TODO Auto-generated method stub
		return mcityidinfo.get(arg0);
	}

	@Override
	public long getItemId(int arg0) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public View getView(int arg0, View arg1, ViewGroup arg2) {
		// TODO Auto-generated method stub
		View view = null;
		ViewHolder holder = null;
		if (arg1 == null || arg1.getTag() == null) {
			view = minflater.inflate(R.layout.city_id_list, null);
			holder = new ViewHolder(view);
			view.setTag(holder);
		}else {
			view = arg1;
			holder = (ViewHolder) arg1.getTag();
		}
		QueryCityItem cityItem = (QueryCityItem) getItem(arg0);
		holder.cityname.setText(cityItem.getCityName()+", ");
		if (null == cityItem.getAdminArea() || cityItem.getAdminArea().equals("")) {
			holder.adminArea.setText(cityItem.getAdminArea());
		}else {
			holder.adminArea.setText(cityItem.getAdminArea()+", ");
		}
		holder.country.setText(cityItem.getCountry());
//		holder.cityid.setText(idinfo.getCityId());
		return view;
	}

	class ViewHolder{
		TextView cityname;
		TextView adminArea;
		TextView country;
//		TextView cityid;

		public ViewHolder(View view){
			this.cityname = (TextView) view.findViewById(R.id.city_Name);
			this.adminArea = (TextView) view.findViewById(R.id.city_Adminarea);
			this.country = (TextView) view.findViewById(R.id.city_Country);
//			this.cityid = (TextView) view.findViewById(R.id.city_Id);
		}
	}
}
