package com.MIBMaverick.voiceservice;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Locale;

import android.*;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.text.format.Time;
import android.text.method.DateTimeKeyListener;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

public class VoiceService extends Service implements SensorEventListener,
		OnInitListener {
	protected AudioManager mAudioManager;
	protected SpeechRecognizer mSpeechRecognizer;
	protected Intent mSpeechRecognizerIntent;
	protected final Messenger mServerMessenger = new Messenger(
			new IncomingHandler(this));

	protected boolean mIsListening;
	protected boolean mMusicMute;
	protected boolean mSpeechReady;
	protected volatile boolean mIsCountDownOn;
	protected boolean mHasResultsClicked;

	static final int MSG_RECOGNIZER_START_LISTENING = 1;
	static final int MSG_RECOGNIZER_CANCEL = 2;
	private static final String TAG = "info";
	private SensorManager mSensorManager;
	private Sensor mOrientation;
	private long mLastSensed;
	ConnectivityManager cm;
	TextToSpeech myTTS;

	@Override
	public void onCreate() {
		super.onCreate();

		mIsListening = false;
		mSpeechReady = false;
		mMusicMute = false;
		mLastSensed = 0;

		mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		initializeSpeechRecognizer();
		initializeSensor();
		IntentFilter filter = new IntentFilter(
				"android.net.conn.CONNECTIVITY_CHANGE");
		this.registerReceiver(mConnectivityCheckReceiver, filter);

		myTTS = new TextToSpeech(this, this);

		Message message = Message.obtain(null, MSG_RECOGNIZER_START_LISTENING);
		try {
			mServerMessenger.send(message);
		} catch (RemoteException e) {

		}
		speakIfImListening();
	}

	private final BroadcastReceiver mConnectivityCheckReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();

			boolean noConnectivity = intent.getBooleanExtra(
					ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
			String reason = intent
					.getStringExtra(ConnectivityManager.EXTRA_REASON);
			boolean isFailover = intent.getBooleanExtra(
					ConnectivityManager.EXTRA_IS_FAILOVER, false);

			if (noConnectivity)
				speakTTS("No Connectivity");
			else
				speakTTS("Connected");

		}

	};

	private void initializeSensor() {
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		mOrientation = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
		mSensorManager.registerListener(this, mOrientation,
				SensorManager.SENSOR_DELAY_NORMAL);
	}

	private void initializeSpeechRecognizer() {
		mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
		mSpeechRecognizer.setRecognitionListener(new SpeechRecognitionListener(
				this));
		mSpeechRecognizerIntent = new Intent(
				RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
				RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
		mSpeechRecognizerIntent.putExtra(
				RecognizerIntent.EXTRA_CALLING_PACKAGE, this.getPackageName());
	}

	public String getCurActivity() {
		ActivityManager mActivityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		List<ActivityManager.RunningTaskInfo> RunningTask = mActivityManager
				.getRunningTasks(1);
		ActivityManager.RunningTaskInfo ar = RunningTask.get(0);
		return ar.baseActivity.getClassName().toString();
	}

	public boolean isInternetConnected() {
		cm = (ConnectivityManager) this
				.getSystemService(Activity.CONNECTIVITY_SERVICE);
		return cm.getActiveNetworkInfo() != null
				&& cm.getActiveNetworkInfo().isConnected();
	}

	protected static class IncomingHandler extends Handler {
		private static final String TAG = "info";
		private WeakReference<VoiceService> mtarget;

		IncomingHandler(VoiceService target) {
			mtarget = new WeakReference<VoiceService>(target);
		}

		@Override
		public void handleMessage(Message msg) {
			final VoiceService target = mtarget.get();

			switch (msg.what) {
			case MSG_RECOGNIZER_START_LISTENING:

				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
					// turn off beep sound
					target.mAudioManager.setStreamMute(
							AudioManager.STREAM_SYSTEM, true);
				}
				if (!target.mIsListening) {
					if (target.isInternetConnected()) {
						if (!target.mAudioManager.isMusicActive()) {
							startListening(target); //$NON-NLS-1$
						} else if (target.mMusicMute) {
							startListening(target);
						}
					}
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
						target.mIsCountDownOn = true;
						target.mNoSpeechCountDown.start();

					}
				}
				break;

			case MSG_RECOGNIZER_CANCEL:
				target.mSpeechRecognizer.cancel();
				target.mIsListening = false;
				Log.d(TAG, "message canceled recognizer"); //$NON-NLS-1$
				break;
			}
		}

		private void startListening(final VoiceService target) {
			target.mSpeechReady = false;
			target.mSpeechRecognizer
					.startListening(target.mSpeechRecognizerIntent);
			target.mIsListening = true;
			Log.d(TAG, "message start listening");
		}
	}

	// Count down timer for Jelly Bean work around
	protected CountDownTimer mNoSpeechCountDown = new CountDownTimer(15000,
			15000) {

		@Override
		public void onTick(long millisUntilFinished) {
			// TODO Auto-generated method stub
			if (mIsListening && !mSpeechReady) {
				mIsCountDownOn = false;
				Message message = Message.obtain(null, MSG_RECOGNIZER_CANCEL);
				try {
					mServerMessenger.send(message);
					message = Message.obtain(null,
							MSG_RECOGNIZER_START_LISTENING);
					mServerMessenger.send(message);
				} catch (RemoteException e) {

				}

			}
		}

		@Override
		public void onFinish() {
			mIsCountDownOn = false;
			Message message = Message.obtain(null, MSG_RECOGNIZER_CANCEL);
			try {
				mServerMessenger.send(message);
				message = Message.obtain(null, MSG_RECOGNIZER_START_LISTENING);
				mServerMessenger.send(message);
			} catch (RemoteException e) {

			}
		}
	};

	@Override
	public void onDestroy() {
		super.onDestroy();
		speakTTS("Shutting down");
		if (mIsCountDownOn) {
			mNoSpeechCountDown.cancel();
		}
		if (mSpeechRecognizer != null) {
			if (mMusicMute) {
				mMusicMute = false;
				mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
			}
			mSpeechRecognizer.stopListening();
			mSpeechRecognizer.destroy();
			Log.d(TAG, "recog destroyed");
		}
		if (mSensorManager != null) {
			mSensorManager.unregisterListener(this);
		}
	}

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onSensorChanged(SensorEvent se) {
		// TODO Auto-generated method stub

		// Log.d(TAG, se.toString());
		long curTime = System.currentTimeMillis();
		Log.d(TAG, Long.toString(curTime - mLastSensed));
		if (curTime - mLastSensed > 1000) {
			mLastSensed = curTime;
			if (se.values[0] < 2) {
				// Unmute music if mute
				if (mMusicMute) {
					Log.d(TAG, "Sensor hit unmute");
					mMusicMute = false;
					mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC,
							false);
					mSpeechRecognizer.stopListening();
				} else {
					Log.d(TAG, "Sensor hit");
					mIsListening = false;
					mAudioManager
							.setStreamMute(AudioManager.STREAM_MUSIC, true);
					mMusicMute = true;
					Message message = Message.obtain(null,
							MSG_RECOGNIZER_START_LISTENING);
					try {
						mServerMessenger.send(message);
					} catch (RemoteException e) {

					}
				}
			}
		}
	}

	@Override
	public void onInit(int initStatus) {
		// TODO Auto-generated method stub
		if (initStatus == TextToSpeech.SUCCESS) {
			if (myTTS.isLanguageAvailable(Locale.US) == TextToSpeech.LANG_AVAILABLE) {
				myTTS.setLanguage(Locale.US);
				Toast.makeText(this, "myTTS initialized", Toast.LENGTH_LONG)
						.show();
				Log.i(TAG,"Checking and speaking internet");
				speakIfImListening();
			}
		} else if (initStatus == TextToSpeech.ERROR) {
			Toast.makeText(this, "Sorry! Text To Speech failed...",
					Toast.LENGTH_LONG).show();
		}
	}

	private void speakIfImListening() {
		// TODO Auto-generated method stub
		if (isInternetConnected()) {
			Log.i(TAG, "Internet connected, speaking");
			speakTTS("I am listening");
		}
		else {
			Log.i(TAG, "Internet not connected, speaking");
			speakTTS("I am dumb");
		}
			
	}

	private void speakTTS(String sayThis) {
		// TODO Auto-generated method stub
		if (myTTS != null)
			myTTS.speak(sayThis, TextToSpeech.QUEUE_ADD, null);
	}
}