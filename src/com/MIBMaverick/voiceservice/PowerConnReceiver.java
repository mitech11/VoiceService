package com.MIBMaverick.voiceservice;

import android.R.string;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.BatteryManager;
import android.util.Log;

public class PowerConnReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		Intent serviceIntent = new Intent(context, VoiceService.class);
		
		Log.d("VSRec", "I am here");
		Log.i("VSREC", intent.getAction());
		if (intent.getAction() == Intent.ACTION_POWER_CONNECTED) {
	        context.stopService(serviceIntent);
	        context.startService(serviceIntent);
		}
		else {
			context.stopService(serviceIntent);
		}
	}
}


