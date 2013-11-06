package com.quanta.weathertime;

import java.util.ArrayList;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

public class WeatherWidgetProvider extends AppWidgetProvider {


    public void onReceive(Context context, Intent intent) {
		super.onReceive(context, intent);
        String action = intent.getAction();
//        Log.e("weatherWidgetProvide", "onReceive "+action);
        if (AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(action)) {
        	/*Get CONFIGURED_WIDGET_ID to update all widget*/

//	    	int appwidgetid = context.getSharedPreferences(WeatherDatabaseAction.CITY_LIST_DETAIL, 0)
//	    			.getInt(WeatherDatabaseAction.WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
    		AppWidgetManager gm = AppWidgetManager.getInstance(context);
    		ArrayList<Integer> appwidgetList = WeatherDatabaseAction.getWidgetList(context);
			for (int i = 0; i < appwidgetList.size(); i++) {
				int appwidgetid = appwidgetList.get(i);
				updateAppWidget(context, gm, appwidgetid);
			}
//                updateAppWidget(context, gm, appwidgetid);
           // }
        }
	}


    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
//    	Log.e("weatherWidgetProvide", "onUpdate");
        final int N = appWidgetIds.length;
        /*Perform this loop procedure for each App Widget that belongs to this provider*/
        for (int i=0; i<N; i++) {
            int appWidgetId = appWidgetIds[i];
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }


    public void onDeleted(Context context, int[] appWidgetIds) {
    	/*delete info from CONFIGURED_WIDGET DB when widget was removed*/
    	super.onDeleted(context, appWidgetIds);

  	    Uri myUri = WeatherContentProvider.CONTENT_SHOWCITYS_URI;
  		ContentResolver cr = context.getContentResolver();

          final int N = appWidgetIds.length;
          for (int i=0; i<N; i++) {
          	cr.delete(myUri, "AppwidgetId" + " = " + appWidgetIds[i], null);

          }

    }

	static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {
//		Log.e("WeatherWidgetProvider","updateAppWidget "+appWidgetId);

		String showcityStr = null;
		Cursor cur = context.getContentResolver().query(WeatherContentProvider.CONTENT_SHOWCITYS_URI,
    			new String[]{"showCitys"}, "AppwidgetId"+"="+appWidgetId, null, null);

    	if (cur != null) {
    	    if (cur.getCount() > 0) {
    	        cur.moveToNext();
    	        showcityStr = cur.getString(cur.getColumnIndex("showCitys"));
    	    }
    	    cur.close();
		}
		RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.main);
		Intent intent = new Intent(context, WidgetService.class);
		intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		intent.putExtra("show_citys", showcityStr);
		intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
		rv.setRemoteAdapter(R.id.appwidget_stack_view, intent);
		//rv.setRemoteAdapter(appWidgetId, R.id.appwidget_stack_view, intent);
        Intent clickIntent = new Intent(context, WeatherConditionsActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        rv.setPendingIntentTemplate(R.id.appwidget_stack_view, pendingIntent);
        rv.setEmptyView(R.id.appwidget_stack_view, R.id.empty_view);
		//appWidgetManager.updateAppWidget(new ComponentName(context, WeatherWidgetProvider.class), rv);
		appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.appwidget_stack_view);
		appWidgetManager.updateAppWidget(appWidgetId, rv);

    }

	public static class WidgetService extends RemoteViewsService{
		@Override
		public RemoteViewsFactory onGetViewFactory(Intent intent) {

			@SuppressWarnings("unused")
			int id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
	                AppWidgetManager.INVALID_APPWIDGET_ID);
			String showcitys = intent.getStringExtra("show_citys");
			return new WeatherWidget(this, showcitys);
		}
	}
}