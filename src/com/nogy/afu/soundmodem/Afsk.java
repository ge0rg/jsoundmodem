/*
 * AFSK modulator
 * 
 * This class helps sending out AFSK modulated data packets for e.g. hamradio use
 * 
 * 08/27/2010
 * Bastian Mueller
 * 
 */

package com.nogy.afu.soundmodem;

import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.ConsoleHandler;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.provider.MediaStore.Audio.AudioColumns;
import android.test.IsolatedContext;
import android.widget.TextView;
import android.os.Handler;

public class Afsk implements AudioRecord.OnRecordPositionUpdateListener
{
	public short[] recordData;
	
	private AudioTrack a;
	private boolean isPlaying;
	private	AudioRecord ar;

	private float volume;
	public short max;
	
	private Thread postProcessor;
	private Runnable postProcess;
	
	private int pos;
	private boolean run;
	
	public TextView debug;
	
	private Handler uiHandler;
	private Runnable updateTextView;
	
	public Afsk()
	{
		run = true;
		uiHandler = new Handler();
		debug = null;
		volume = AudioTrack.getMaxVolume()/2;	
		max = 0;
		
		updateTextView = new Runnable() {
			
			@Override
			public void run() {
				debug.setText("sps:"+ Integer.toString(pos)+"/"+Integer.toString(recordData.length)+ " - max: "+Integer.toString(max));
				debug.invalidate();
			}
		};
				
		postProcess = new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
				int i=0;
				int read = 0;
				int k=0;
				long time = 0;
				while (run)
				{
					/*if (max<Short.MAX_VALUE)
						max++;
					else
						max = Short.MIN_VALUE;
					*/
					time = android.os.SystemClock.elapsedRealtime();
					read = ar.read(recordData, 0, 2048);
					pos = read;
					max = 0;
					for (i=0; i<read; i++)
						if ((recordData[i]>max)||(recordData[i] < -1*max))
							max = recordData[i];
					time = android.os.SystemClock.elapsedRealtime()-time;
					if (time < 30)
					{					
						uiHandler.postAtFrontOfQueue(updateTextView);
						android.os.SystemClock.sleep(10);	
					}
				}
			}
		};

		postProcessor = new Thread(postProcess);
//		postProcessor.setPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
		postProcessor.setDaemon(true);
	}
	
	//
	public void release()
	{
		run = false;
		try {
			postProcessor.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void setVolume(float vol)	
	{
		this.volume = vol;
	}
	
	public void sendMessage(Message m)
	{
		// stop playback if not finished with last one
		if (isPlaying) {
			android.util.Log.e("Afsk", "Skipping packet; last one is still playing.");
			return;
		}

		sendPCM(AfskEncoder.encodeMessagePCM(m));
	}
	
	public void sendPCM(short[] pcmData)
	{
		a = new AudioTrack(
				AudioManager.STREAM_RING,
				AfskEncoder.samplerate,
				AudioFormat.CHANNEL_CONFIGURATION_MONO,
				AudioFormat.ENCODING_PCM_16BIT,
				pcmData.length*2,
				AudioTrack.MODE_STREAM
				);
		a.write(pcmData, 0, pcmData.length);
		a.setStereoVolume(this.volume, this.volume);
		isPlaying = true;
		// implement self-destruction
		a.setNotificationMarkerPosition(pcmData.length);
		a.setPlaybackPositionUpdateListener(new AudioTrack.OnPlaybackPositionUpdateListener() {
			public void onMarkerReached(AudioTrack track) {
				track.release();
				isPlaying = false;
			}
			public void onPeriodicNotification(AudioTrack track) {
				// no-op
			}
		}, uiHandler);
		a.play();
	}
	
	public void readPCM()
	{
		if (ar != null)
		{
			run = false;
			try {
				postProcessor.join();
			} catch (Exception e) {
				// TODO: handle exception
			}
			if (ar.getRecordingState() == (AudioRecord.RECORDSTATE_RECORDING))
				ar.stop();
			ar.release();
		}
		else
		{
			//android.Manifest.permission.RECORD_AUDIO;
			int encoding = AudioFormat.ENCODING_PCM_16BIT;
			int format = AudioFormat.CHANNEL_CONFIGURATION_MONO;
			int bs = (AudioRecord.getMinBufferSize(AfskEncoder.samplerate, format, encoding));
			
			recordData = new short[bs];
			
			//android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
			ar = new AudioRecord(
						MediaRecorder.AudioSource.MIC,
						AfskEncoder.samplerate,
						format,
						encoding,
						bs);
			ar.setRecordPositionUpdateListener(this);
			ar.setPositionNotificationPeriod(10);
			
			ar.startRecording();
			run = true;
			postProcessor.start();
			long time;
			/*while (run)
			{
				time = android.os.SystemClock.elapsedRealtime();
				pos = ar.read(recordData, 0, 4096);
				time = android.os.SystemClock.elapsedRealtime()-time;
				//uiHandler.post(updateTextView);
				android.os.SystemClock.sleep(1);
			}
			*/
		}
	}

	@Override
	public void onMarkerReached(AudioRecord recorder) {
		// TODO Auto-generated method stub
		// UNUSED
	}

	@Override
	public void onPeriodicNotification(AudioRecord recorder) {
		// TODO Auto-generated method stub
	}

}
