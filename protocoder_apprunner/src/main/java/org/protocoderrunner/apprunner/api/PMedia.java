/*
 * Protocoder 
 * A prototyping platform for Android devices 
 * 
 * Victor Diaz Barrales victormdb@gmail.com
 *
 * Copyright (C) 2014 Victor Diaz
 * Copyright (C) 2013 Motorola Mobility LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software
 * is furnished to do so, subject to the following conditions: 
 * 
 * The above copyright notice and this permission notice shall be included in all 
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN 
 * THE SOFTWARE.
 * 
 */

package org.protocoderrunner.apprunner.api;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import org.protocoderrunner.apidoc.annotation.ProtoMethod;
import org.protocoderrunner.apidoc.annotation.ProtoMethodParam;
import org.protocoderrunner.apprunner.AppRunnerSettings;
import org.protocoderrunner.apprunner.PInterface;
import org.protocoderrunner.apprunner.api.media.PAudioPlayer;
import org.protocoderrunner.apprunner.api.media.PAudioRecorder;
import org.protocoderrunner.apprunner.api.media.PMidi;
import org.protocoderrunner.apprunner.api.media.PPureData;
import org.protocoderrunner.apprunner.api.other.WhatIsRunning;
import org.protocoderrunner.media.Audio;
import org.protocoderrunner.media.AudioServicePd;
import org.protocoderrunner.utils.AndroidUtils;
import org.protocoderrunner.utils.MLog;

import java.util.ArrayList;
import java.util.Locale;

public class PMedia extends PInterface {

    String TAG = "PMedia";

    private HeadSetReceiver headsetPluggedReceiver;
    private MicPluggedCB headsetCallbackfn;
    PAudioRecorder rec;

    public PMedia(Context c) {
        super(c);
        rec = new PAudioRecorder(getContext());

        WhatIsRunning.getInstance().add(this);
    }


    //public PAudioPlayer loadSound(String url, PAudioPlayer.LoadSoundCB callbackfn) {
    //	return loadPlayer(url, callbackfn);
    //}

    PAudioPlayer player;


    @ProtoMethod(description = "Play a sound file giving its filename", example = "media.playSound(fileName);")
    @ProtoMethodParam(params = {"fileName"})
    public PAudioPlayer playSound(String url) {
        PAudioPlayer pAudioPlayer = new PAudioPlayer(url);

        return pAudioPlayer;
    }

    @ProtoMethod(description = "Set the main volume", example = "")
    @ProtoMethodParam(params = {"volume"})
    public void volume(int volume) {
        AndroidUtils.setVolume(getContext(), volume);
    }


    @ProtoMethod(description = "Routes the audio through the speakers", example = "media.playSound(fileName);")
    @ProtoMethodParam(params = {""})
    public void audioOnSpeakers(boolean b) {
        AudioManager audioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        audioManager.setMode(AudioManager.MODE_IN_CALL);
        audioManager.setSpeakerphoneOn(!b);
    }


    @ProtoMethod(description = "Enable sounds effects (default false)", example = "")
    @ProtoMethodParam(params = {"boolean"})
    public void enableSoundEffects(boolean b) {
        AndroidUtils.setEnableSoundEffects(getContext(), b);
    }


    @ProtoMethod(description = "Loads and initializes a PureData patch http://www.puredata.info using libpd", example = "")
    @ProtoMethodParam(params = {"fileName", "micChannels", "outputChannels", "sampleRate", "buffer"})
    public PPureData loadPdPatch(String fileName, int micChannels, int outputChannels, int sampleRate, int buffer) {
        AudioServicePd.settingsSampleRate = sampleRate;
        AudioServicePd.settingsMicChannels = micChannels;
        AudioServicePd.settingsOutputChannels = outputChannels;
        AudioServicePd.settingsBuffer = buffer;

        return this.loadPdPatch(fileName);
    }

    @ProtoMethod(description = "Loads and initializes a PureData patch http://www.puredata.info using libpd", example = "")
    @ProtoMethodParam(params = {"fileName"})
    public PPureData loadPdPatch(String fileName) {
        PPureData pPureData = new PPureData(getActivity());
        pPureData.initPatch(fileName);

        return pPureData;
    }

    boolean recording = false;


    @ProtoMethod(description = "Record a sound with the microphone", example = "")
    @ProtoMethodParam(params = {"fileName", "showProgressBoolean"})
    public void audioRecord(String fileName, boolean showProgress) {
        if (!recording) {
            recording = true;
            if (AppRunnerSettings.get().hasUi) {
                rec.startRecordingWithUi(getActivity());
            }

            rec.startRecording(fileName, showProgress & AppRunnerSettings.get().hasUi);
        }
    }


    @ProtoMethod(description = "Record a sound with the microphone", example = "")
    @ProtoMethodParam(params = {"fileName", "showProgressBoolean"})
    public void stopAudioRecord() {
        if (recording) {
            rec.stopRecording();
            recording = false;
        }

    }


    @ProtoMethod(description = "Says a text with voice", example = "media.textToSpeech('hello world');")
    @ProtoMethodParam(params = {"text"})
    public void textToSpeech(String text) {
        Audio.speak(getContext(), text, Locale.getDefault());
    }


    @ProtoMethod(description = "Says a text with voice using a defined locale", example = "media.textToSpeech('hello world');")
    @ProtoMethodParam(params = {"text", "Locale"})
    public void textToSpeech(String text, Locale locale) {
        Audio.speak(getContext(), text, locale);
    }


    // --------- startVoiceRecognition ---------//
    interface StartVoiceRecognitionCB {
        void event(String responseString);
    }


    @ProtoMethod(description = "Fires the voice recognition and returns the best match", example = "media.startVoiceRecognition(function(text) { console.log(text) } );")
    @ProtoMethodParam(params = {"function(recognizedText)"})
    public void voiceRecognition(final StartVoiceRecognitionCB callbackfn) {
        if (getActivity() == null) return;

        (getActivity()).addVoiceRecognitionListener(new onVoiceRecognitionListener() {

            @Override
            public void onNewResult(String text) {
                MLog.d(TAG, "" + text);
                callbackfn.event(text);
            }

        });

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Tell me something!");
        getActivity().startActivityForResult(intent, getActivity().VOICE_RECOGNITION_REQUEST_CODE);
    }

    // --------- startVoiceRecognition ---------//
    interface StartVoiceRecognition2CB {
        void event(String status, String responseString);
    }

    public SpeechRecognizer startVoiceRecognition2(final StartVoiceRecognition2CB callbackfn) {

        String langMode = "en-US";
        langMode = RecognizerIntent.LANGUAGE_MODEL_FREE_FORM;
        SpeechRecognizer sr = SpeechRecognizer.createSpeechRecognizer(getContext());

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, langMode);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, this.getClass().getPackage().getName());
        sr.startListening(intent);

        sr.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {

            }

            @Override
            public void onBeginningOfSpeech() {

            }

            @Override
            public void onRmsChanged(float rmsdB) {

            }

            @Override
            public void onBufferReceived(byte[] buffer) {

            }

            @Override
            public void onEndOfSpeech() {
                callbackfn.event("ended", "");
            }

            @Override
            public void onError(int error) {
                callbackfn.event("error", "");
            }

            @Override
            public void onResults(Bundle results) {

                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

                for (int i = 0; i < matches.size(); i++) {
                    MLog.d(TAG, "result " + i + " " + matches.get(i));
                }

                callbackfn.event("result", matches.get(matches.size()));
            }

            @Override
            public void onPartialResults(Bundle partialResults) {

                ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

                for (int i = 0; i < matches.size(); i++) {
                    MLog.d(TAG, "partialResult " + i + " " + matches.get(i));
                }
            }

            @Override
            public void onEvent(int eventType, Bundle params) {

            }
        });

        return sr;

        // sr.stopListening();
    }

    public interface onVoiceRecognitionListener {
        public void onNewResult(String text);
    }

    public void stop() {
        getContext().unregisterReceiver(headsetPluggedReceiver);
    }


    @ProtoMethod(description = "Start a connected midi device", example = "media.startVoiceRecognition(function(text) { console.log(text) } );")
    @ProtoMethodParam(params = {"function(recognizedText)"})
    public PMidi connectMidiDevice() {
        PMidi pMidi = new PMidi(getContext());

        return pMidi;
    }

    public boolean isHeadsetPlugged() {
        AudioManager audioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        return audioManager.isWiredHeadsetOn();
    }

    interface MicPluggedCB {
        void event(boolean b);
    }

    public void headsetListener(MicPluggedCB callbackfn) {
        WhatIsRunning.getInstance().add(this);
        headsetCallbackfn = callbackfn;

        IntentFilter filter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        headsetPluggedReceiver = new HeadSetReceiver();
        getContext().registerReceiver(headsetPluggedReceiver, filter);
    }

    private class HeadSetReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
                int state = intent.getIntExtra("state", -1);
                switch (state) {
                    case 0:
                        Log.d(TAG, "Headset unplugged");
                        headsetCallbackfn.event(false);
                        break;
                    case 1:
                        Log.d(TAG, "Headset plugged");
                        headsetCallbackfn.event(true);
                        break;
                }
            }
        }
    }

}

