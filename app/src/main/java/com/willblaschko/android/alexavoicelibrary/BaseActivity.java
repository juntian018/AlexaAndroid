package com.willblaschko.android.alexavoicelibrary;

import android.app.Instrumentation;
import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import com.willblaschko.android.alexa.AlexaManager;
import com.willblaschko.android.alexa.audioplayer.AlexaAudioPlayer;
import com.willblaschko.android.alexa.callbacks.AsyncCallback;
import com.willblaschko.android.alexa.interfaces.AvsItem;
import com.willblaschko.android.alexa.interfaces.AvsResponse;
import com.willblaschko.android.alexa.interfaces.audioplayer.AvsPlayContentItem;
import com.willblaschko.android.alexa.interfaces.audioplayer.AvsPlayRemoteItem;
import com.willblaschko.android.alexa.interfaces.errors.AvsResponseException;
import com.willblaschko.android.alexa.interfaces.playbackcontrol.AvsMediaNextCommandItem;
import com.willblaschko.android.alexa.interfaces.playbackcontrol.AvsMediaPauseCommandItem;
import com.willblaschko.android.alexa.interfaces.playbackcontrol.AvsMediaPlayCommandItem;
import com.willblaschko.android.alexa.interfaces.playbackcontrol.AvsMediaPreviousCommandItem;
import com.willblaschko.android.alexa.interfaces.playbackcontrol.AvsReplaceAllItem;
import com.willblaschko.android.alexa.interfaces.playbackcontrol.AvsReplaceEnqueuedItem;
import com.willblaschko.android.alexa.interfaces.playbackcontrol.AvsStopItem;
import com.willblaschko.android.alexa.interfaces.speaker.AvsAdjustVolumeItem;
import com.willblaschko.android.alexa.interfaces.speaker.AvsSetMuteItem;
import com.willblaschko.android.alexa.interfaces.speaker.AvsSetVolumeItem;
import com.willblaschko.android.alexa.interfaces.speechrecognizer.AvsExpectSpeechItem;
import com.willblaschko.android.alexa.interfaces.speechsynthesizer.AvsSpeakItem;
import com.willblaschko.android.alexavoicelibrary.actions.BaseListenerFragment;

import java.util.ArrayList;
import java.util.List;

import static com.willblaschko.android.alexavoicelibrary.global.Constants.PRODUCT_ID;
import android.content.SharedPreferences.Editor;
import com.willblaschko.android.alexa.utility.Util;

/**
 * @author will on 5/30/2016.
 */

public abstract class BaseActivity extends AppCompatActivity implements BaseListenerFragment.AvsListenerInterface{

    private final static String TAG = "BaseActivity";

    private final static int STATE_LISTENING = 1;
    private final static int STATE_PROCESSING = 2;
    private final static int STATE_SPEAKING = 3;
    private final static int STATE_PROMPTING = 4;
    private final static int STATE_FINISHED = 0;

    private AlexaManager alexaManager;
    private AlexaAudioPlayer audioPlayer;
    private List<AvsItem> avsQueue = new ArrayList<>();

    private long startTime = 0;

    public void SaveToken(String refresh_token, String access_token, long expires_in) {
        String REFRESH_TOKEN = refresh_token;
        String ACCESS_TOKEN = access_token;
        Editor preferences = Util.getPreferences(this.getApplicationContext()).edit();
        preferences.putString("access_token", ACCESS_TOKEN);
        preferences.putString("refresh_token", REFRESH_TOKEN);
        preferences.putLong("token_expires", System.currentTimeMillis() + expires_in * 1000L);
        preferences.commit();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        String access_token = "Atza|IwEBIK7GFgdg1jp2biexRuZJ2UMchZ0ZOfbYuYbKSWaAei9zfZWfb332l6F7ULrY8phC4nkiFU0lyQWv9m8anFzQ-2JX_juHaaPVsE058ICZnorDHinJcuRXgtegAzUCSpl6WWq1WYZIWDXq6axI6ysOKCl6HIs2VznhQiwHc45n0QZcMo4p8BDIdXNpYCEPjGE13UPFU8_6AryU1qPgRF1CXIS7I_rLqJ7mpSb5vA40xdJhCAtySeQF2UHshJZO2GZf91MbOji7sQwoA4jxzWZmYLYDKWZgVAmKTgG8b_Cny3-fpucLDDU_8G1ulHy22yS7P4jhdz78A3sjobZXbfrOtOyc15EahNHWluNTQPDNn4ccsJAY7qMW_ZZ39TE-h91IIX9Qge2wY5BtQEXrZ9fJ1949FjhK071Yl0XUs-GeQQY8OxslzGvbSlAly0EMikz5mg0OyOSKh4nsIJsBDzpMIWWPspxj0VsHH9Tw2c7L_zK0DQHEC1G2ESNaqxAxzY8LswM";
//        String refresh_token = "Atzr|IwEBIHkFg2Ef3u73z1cmfE7qkwOWXPR_2ZZCNeZS1Js61W625nE2p0gb5K7v-DT4PRPrnuRrgY9PLRCZh7MnDRzAh5XEO1mKABKrTLRer0gDCPZ_l7Otje0D3fGSjYbMmP-WGxqx5AQbf-6rJYL0kJvuorb4dlkzByi89L8EMyaU8qDktGAGoaH92ii3vY3prDBOeN_tk1fxGZ27m5V01bK0aUN7OmStQQ_zypZ0i4Cfjb5MwD-u8UM_w63elovvrRGU1pKNH1TwmI4m-m2Pamw9WXUxDJO9CKfTkCOyJRQHpTmAawcPN6-eMe5DM983BUUH8qK2V-ULaT4aFs9IimaClQNfcFtMv8nVsCHy58U3EJ-Ifw8Yh7pWUIL61MlI4Fnvb0TOSKUMFhaE3wMb0oCw86nTurvK3I6G7UfbdGs0HnVUA_pDvJjzUDtJq8uLbjGJqVvn-ip0cWtB1xssVk2osnyVtK91EvsNaiaiIDE1HahkBo4TJzHOD2ojq9z8yKb5gN-CoxC3XHU9ZxCDFfr6EJQO";
//
//        SaveToken(refresh_token, access_token, 0);

        initAlexaAndroid();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(audioPlayer != null){
            audioPlayer.stop();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(audioPlayer != null){
            //remove callback to avoid memory leaks
            audioPlayer.removeCallback(alexaAudioPlayerCallback);
            audioPlayer.release();
        }
    }


    @Override
    public AsyncCallback<AvsResponse, Exception> getRequestCallback() {
        return requestCallback;
    }





    private void initAlexaAndroid(){
        //get our AlexaManager instance for convenience
        alexaManager = AlexaManager.getInstance(this, PRODUCT_ID);

        //instantiate our audio player
        audioPlayer = AlexaAudioPlayer.getInstance(this);

        //Remove the current item and check for more items once we've finished playing
        audioPlayer.addCallback(alexaAudioPlayerCallback);

        //open our downchannel
        alexaManager.sendOpenDownchannelDirective();

        //synchronize our device
        alexaManager.sendSynchronizeStateEvent(requestCallback);
    }

    //Our callback that deals with removing played items in our media player and then checking to see if more items exist
    private AlexaAudioPlayer.Callback alexaAudioPlayerCallback = new AlexaAudioPlayer.Callback() {

        private boolean almostDoneFired = false;

        @Override
        public void playerPrepared(AvsItem pendingItem) {
            almostDoneFired = false;
            sendPlaybackStartedEvent(pendingItem);

        }

        @Override
        public void playerProgress(AvsItem item, long offsetInMilliseconds, float percent) {
            Log.i(TAG, "Player percent: "+percent);
            if(percent > .5f && !almostDoneFired){
                almostDoneFired = true;
                sendPlaybackNearlyFinishedEvent(item, offsetInMilliseconds);
            }
        }

        @Override
        public void itemComplete(AvsItem completedItem) {
            almostDoneFired = false;
            sendPlaybackFinishedEvent(completedItem);
            avsQueue.remove(completedItem);
            checkQueue();
        }

        @Override
        public boolean playerError(AvsItem item, int what, int extra) {
            sendPlaybackNearlyFinishedEvent(item, 500);
            itemComplete(item);
            return false;
        }

        @Override
        public void dataError(AvsItem item, Exception e) {
            e.printStackTrace();
            sendPlaybackNearlyFinishedEvent(item, 500);
            itemComplete(item);
        }
    };

    /**
     * Send an event back to Alexa that we're nearly done with our current playback event, this should supply us with the next item
     * https://developer.amazon.com/public/solutions/alexa/alexa-voice-service/reference/audioplayer#PlaybackNearlyFinished Event
     */
    private void sendPlaybackNearlyFinishedEvent(AvsItem item, long offsetInMilliseconds){
        if (item != null) {
            alexaManager.sendPlaybackNearlyFinishedEvent(item, offsetInMilliseconds, requestCallback);
            Log.i(TAG, "Sending PlaybackNearlyFinishedEvent");
        }
    }

    /**
     * Send an event back to Alexa that we're starting a speech event
     * https://developer.amazon.com/public/solutions/alexa/alexa-voice-service/reference/audioplayer#PlaybackNearlyFinished Event
     */
    private void sendPlaybackStartedEvent(AvsItem item){
        alexaManager.sendPlaybackStartedEvent(item, null);
        Log.i(TAG, "Sending SpeechStartedEvent");
    }

    /**
     * Send an event back to Alexa that we're done with our current speech event, this should supply us with the next item
     * https://developer.amazon.com/public/solutions/alexa/alexa-voice-service/reference/audioplayer#PlaybackNearlyFinished Event
     */
    private void sendPlaybackFinishedEvent(AvsItem item){
        if (item != null) {
            alexaManager.sendPlaybackFinishedEvent(item, requestCallback);
            Log.i(TAG, "Sending SpeechFinishedEvent");
        }
    }

    //async callback for commands sent to Alexa Voice
    private AsyncCallback<AvsResponse, Exception> requestCallback = new AsyncCallback<AvsResponse, Exception>() {
        @Override
        public void start() {
            startTime = System.currentTimeMillis();
            Log.i(TAG, "Event Start");
            setState(STATE_PROCESSING);
        }

        @Override
        public void success(AvsResponse result) {
            Log.i(TAG, "Event Success");
            handleResponse(result);
        }

        @Override
        public void failure(Exception error) {
            error.printStackTrace();
            Log.i(TAG, "Event Error");
            setState(STATE_FINISHED);
        }

        @Override
        public void complete() {
            Log.i(TAG, "Event Complete");
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    long totalTime = System.currentTimeMillis() - startTime;
                    Toast.makeText(BaseActivity.this, "Total request time: "+totalTime+" miliseconds", Toast.LENGTH_LONG).show();
                    Log.i(TAG, "Total request time: "+totalTime+" miliseconds");
                }
            });
        }
    };

    /**
     * Handle the response sent back from Alexa's parsing of the Intent, these can be any of the AvsItem types (play, speak, stop, clear, listen)
     * @param response a List<AvsItem> returned from the mAlexaManager.sendTextRequest() call in sendVoiceToAlexa()
     */
    private void handleResponse(AvsResponse response){
        boolean checkAfter = (avsQueue.size() == 0);
        if(response != null){
            //if we have a clear queue item in the list, we need to clear the current queue before proceeding
            //iterate backwards to avoid changing our array positions and getting all the nasty errors that come
            //from doing that
            for(int i = response.size() - 1; i >= 0; i--){
                if(response.get(i) instanceof AvsReplaceAllItem || response.get(i) instanceof AvsReplaceEnqueuedItem){
                    //clear our queue
                    avsQueue.clear();
                    //remove item
                    response.remove(i);
                }
            }
            Log.i(TAG, "Adding "+response.size()+" items to our queue");
            avsQueue.addAll(response);
        }
        if(checkAfter) {
            checkQueue();
        }
    }


    /**
     * Check our current queue of items, and if we have more to parse (once we've reached a play or listen callback) then proceed to the
     * next item in our list.
     *
     * We're handling the AvsReplaceAllItem in handleResponse() because it needs to clear everything currently in the queue, before
     * the new items are added to the list, it should have no function here.
     */
    private void checkQueue() {

        //if we're out of things, hang up the phone and move on
        if (avsQueue.size() == 0) {
            setState(STATE_FINISHED);
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    long totalTime = System.currentTimeMillis() - startTime;
                    Toast.makeText(BaseActivity.this, "Total interaction time: "+totalTime+" miliseconds", Toast.LENGTH_LONG).show();
                    Log.i(TAG, "Total interaction time: "+totalTime+" miliseconds");
                }
            });
            return;
        }

        final AvsItem current = avsQueue.get(0);

        Log.i(TAG, "Item type " + current.getClass().getName());

        if (current instanceof AvsPlayRemoteItem) {
            //play a URL
            if (!audioPlayer.isPlaying()) {
                audioPlayer.playItem((AvsPlayRemoteItem) current);
            }
        } else if (current instanceof AvsPlayContentItem) {
            //play a URL
            if (!audioPlayer.isPlaying()) {
                audioPlayer.playItem((AvsPlayContentItem) current);
            }
        } else if (current instanceof AvsSpeakItem) {
            //play a sound file
            if (!audioPlayer.isPlaying()) {
                audioPlayer.playItem((AvsSpeakItem) current);
            }
            setState(STATE_SPEAKING);
        } else if (current instanceof AvsStopItem) {
            //stop our play
            audioPlayer.stop();
            avsQueue.remove(current);
        } else if (current instanceof AvsReplaceAllItem) {
            //clear all items
            //mAvsItemQueue.clear();
            audioPlayer.stop();
            avsQueue.remove(current);
        } else if (current instanceof AvsReplaceEnqueuedItem) {
            //clear all items
            //mAvsItemQueue.clear();
            avsQueue.remove(current);
        } else if (current instanceof AvsExpectSpeechItem) {

            //listen for user input
            audioPlayer.stop();
            avsQueue.clear();
            startListening();
        } else if (current instanceof AvsSetVolumeItem) {
            //set our volume
            setVolume(((AvsSetVolumeItem) current).getVolume());
            avsQueue.remove(current);
        } else if(current instanceof AvsAdjustVolumeItem){
            //adjust the volume
            adjustVolume(((AvsAdjustVolumeItem) current).getAdjustment());
            avsQueue.remove(current);
        } else if(current instanceof AvsSetMuteItem){
            //mute/unmute the device
            setMute(((AvsSetMuteItem) current).isMute());
            avsQueue.remove(current);
        }else if(current instanceof AvsMediaPlayCommandItem){
            //fake a hardware "play" press
            sendMediaButton(this, KeyEvent.KEYCODE_MEDIA_PLAY);
            Log.i(TAG, "Media play command issued");
            avsQueue.remove(current);
        }else if(current instanceof AvsMediaPauseCommandItem){
            //fake a hardware "pause" press
            sendMediaButton(this, KeyEvent.KEYCODE_MEDIA_PAUSE);
            Log.i(TAG, "Media pause command issued");
            avsQueue.remove(current);
        }else if(current instanceof AvsMediaNextCommandItem){
            //fake a hardware "next" press
            sendMediaButton(this, KeyEvent.KEYCODE_MEDIA_NEXT);
            Log.i(TAG, "Media next command issued");
            avsQueue.remove(current);
        }else if(current instanceof AvsMediaPreviousCommandItem){
            //fake a hardware "previous" press
            sendMediaButton(this, KeyEvent.KEYCODE_MEDIA_PREVIOUS);
            Log.i(TAG, "Media previous command issued");
            avsQueue.remove(current);
        }else if(current instanceof AvsResponseException){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    new AlertDialog.Builder(BaseActivity.this)
                            .setTitle("Error")
                            .setMessage(((AvsResponseException) current).getDirective().getPayload().getCode() + ": " + ((AvsResponseException) current).getDirective().getPayload().getDescription())
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                }
            });

            avsQueue.remove(current);
            checkQueue();
        }
    }

    protected abstract void startListening();

    private void adjustVolume(long adjust){
        setVolume(adjust, true);
    }
    private void setVolume(long volume){
        setVolume(volume, false);
    }
    private void setVolume(final long volume, final boolean adjust){
        AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
        final int max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        long vol= am.getStreamVolume(AudioManager.STREAM_MUSIC);
        if(adjust){
            vol += volume * max / 100;
        }else{
            vol = volume * max / 100;
        }
        am.setStreamVolume(AudioManager.STREAM_MUSIC, (int) vol, AudioManager.FLAG_VIBRATE);

        alexaManager.sendVolumeChangedEvent(volume, vol == 0, requestCallback);

        Log.i(TAG, "Volume set to : " + vol +"/"+max+" ("+volume+")");

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if(adjust) {
                    Toast.makeText(BaseActivity.this, "Volume adjusted.", Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(BaseActivity.this, "Volume set to: " + (volume / 10), Toast.LENGTH_SHORT).show();
                }
            }
        });

    }
    private void setMute(final boolean isMute){
        AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
        am.setStreamMute(AudioManager.STREAM_MUSIC, isMute);

        alexaManager.sendMutedEvent(isMute, requestCallback);

        Log.i(TAG, "Mute set to : "+isMute);

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(BaseActivity.this, "Volume " + (isMute ? "muted" : "unmuted"), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Force the device to think that a hardware button has been pressed, this is used for Play/Pause/Previous/Next Media commands
     * @param context
     * @param keyCode keycode for the hardware button we're emulating
     */
    private static void sendMediaButton(Context context, int keyCode) {
        Instrumentation inst = new Instrumentation();
        inst.sendKeyDownUpSync(keyCode);
    }

    private void setState(final int state){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch (state){
                    case(STATE_LISTENING):
                        stateListening();
                        break;
                    case(STATE_PROCESSING):
                        stateProcessing();
                        break;
                    case(STATE_SPEAKING):
                        stateSpeaking();
                        break;
                    case(STATE_FINISHED):
                        stateFinished();
                        break;
                    case(STATE_PROMPTING):
                        statePrompting();
                        break;
                    default:
                        stateNone();
                        break;
                }
            }
        });
    }

    protected abstract void stateListening();
    protected abstract void stateProcessing();
    protected abstract void stateSpeaking();
    protected abstract void stateFinished();
    protected abstract void statePrompting();
    protected abstract void stateNone();

}
