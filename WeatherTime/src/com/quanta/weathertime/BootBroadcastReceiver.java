package com.quanta.weathertime;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.Context;

public class BootBroadcastReceiver extends BroadcastReceiver {
	/**
	 * @see android.content.BroadcastReceiver#onReceive(Context,Intent)
	 */
	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Put your code here
		if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
			Intent intent_service = new Intent(context,WeatherService.class);
			intent_service.putExtra("REQUERY_ALL", "");
			context.startService(intent_service);
		}
	}
}
