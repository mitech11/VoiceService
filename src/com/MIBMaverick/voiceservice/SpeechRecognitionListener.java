package com.MIBMaverick.voiceservice;

import java.util.ArrayList;

import android.content.ComponentName;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.os.RemoteException;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.util.Log;

class SpeechRecognitionListener implements RecognitionListener {

	/**
	 * 
	 */
	private final VoiceService voiceService;

	/**
	 * @param voiceService
	 */
	SpeechRecognitionListener(VoiceService voiceService) {
		this.voiceService = voiceService;
	}

	private static final String TAG = "info";

	@Override
	public void onBeginningOfSpeech() {
		// speech input will be processed, so there is no need for count
		// down anymore
		if (this.voiceService.mIsCountDownOn) {
			this.voiceService.mIsCountDownOn = false;
			this.voiceService.mNoSpeechCountDown.cancel();
		}
		Log.d(TAG, "onBeginingOfSpeech"); //$NON-NLS-1$
	}

	@Override
	public void onBufferReceived(byte[] buffer) {

	}

	@Override
	public void onEndOfSpeech() {
		Log.d(TAG, "onEndOfSpeech"); //$NON-NLS-1$
	}

	@Override
	public void onError(int error) {
		if (this.voiceService.mIsCountDownOn) {
			this.voiceService.mIsCountDownOn = false;
			this.voiceService.mNoSpeechCountDown.cancel();
		}
		this.voiceService.mIsListening = false;
		Message message = Message.obtain(null,
				VoiceService.MSG_RECOGNIZER_START_LISTENING);
		try {
			this.voiceService.mServerMessenger.send(message);
		} catch (RemoteException e) {

		}
		Log.d(TAG, "error = " + error); //$NON-NLS-1$
	}

	@Override
	public void onReadyForSpeech(Bundle params) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			this.voiceService.mAudioManager.setStreamMute(AudioManager.STREAM_SYSTEM, false);
		}
		this.voiceService.mSpeechReady = true;
		Log.d(TAG, "onReadyForSpeech"); //$NON-NLS-1$
	}

	@Override
	public void onRmsChanged(float rmsdB) {

	}

	@Override
	public void onEvent(int eventType, Bundle params) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onPartialResults(Bundle partialResults) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onResults(Bundle results) {
		// TODO Auto-generated method stub
		String str = new String();
		Log.d(TAG, "onResults " + results);
		ArrayList data = results
				.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
		for (int i = 0; i < data.size(); i++) {
			Log.d(TAG, "result " + data.get(i));
			str += data.get(i);
		}
		// mText.setText("results: "+String.valueOf(data.size()));
		// mText.setText("results: "+ data.toString());
		if (str.contains("google")) {
			this.voiceService.mSpeechRecognizer.cancel();

			Intent intent = new Intent(Intent.ACTION_MAIN);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			intent.setComponent(new ComponentName(
					"com.google.android.googlequicksearchbox",
					"com.google.android.googlequicksearchbox.VoiceSearchActivity"));
			this.voiceService.startActivity(intent);

			this.voiceService.mNoSpeechCountDown.start();

		} else {
			this.voiceService.mIsListening = false;
			Message message = Message.obtain(null,
					VoiceService.MSG_RECOGNIZER_START_LISTENING);
			try {
				this.voiceService.mServerMessenger.send(message);
			} catch (RemoteException e) {

			}

		}
	}

}