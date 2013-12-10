package com.MIBMaverick.voiceservice;

import java.util.ArrayList;
import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.drm.DrmStore.Action;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class HelloAndroid extends Activity {
	TextView mText;
	public TextToSpeech myTTS;
	public int MY_DATA_CHECK_CODE = 0;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.getWindow().addFlags(
				WindowManager.LayoutParams.FLAG_FULLSCREEN
						| WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
						| WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
						| WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
						| WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		KeyguardManager manager = (KeyguardManager) this
				.getSystemService(Context.KEYGUARD_SERVICE);
		KeyguardLock lock = manager.newKeyguardLock("abc");
		lock.disableKeyguard();
		setContentView(R.layout.main);
		
		startVoiceService();
		ConnectivityManager cm = (ConnectivityManager) this
				.getSystemService(Activity.CONNECTIVITY_SERVICE);
		
		Button btnServiceStatus = (Button) findViewById(R.id.btnServiceStatus);
		btnServiceStatus.setBackgroundColor(0xFF00FF00);
		btnServiceStatus.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Button btn = (Button) v;
				if (btn.getText() == getResources().getString(
						R.string.btn_service_running)) {
					btn.setText(R.string.btn_service_stopped);
					btn.setBackgroundColor(0xFFFF0000);
					stopVoiceService();
				} else {
					btn.setText(R.string.btn_service_running);
					btn.setBackgroundColor(0xFF00FF00);
					startVoiceService();
				}

			}
		});

		

		if (cm.getActiveNetworkInfo() != null
				&& cm.getActiveNetworkInfo().isConnected()) {
			btnServiceStatus
					.setText("I am unable to listen as there is no internet connectivity");
		}

		Log.d("info", "HelloAndroid started");
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	
	private void startVoiceService() {
		Intent serviceIntent = new Intent(this, VoiceService.class);
		this.stopService(serviceIntent);
		this.startService(serviceIntent);
	}

	
	private void stopVoiceService() {
		Intent serviceIntent = new Intent(this, VoiceService.class);
		this.stopService(serviceIntent);
	}

	
}
