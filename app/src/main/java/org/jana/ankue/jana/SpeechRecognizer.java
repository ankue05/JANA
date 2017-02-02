package org.jana.ankue.jana;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.util.Log;

import java.util.ArrayList;

public class SpeechRecognizer implements RecognitionListener {


    private android.speech.SpeechRecognizer googleSpeechRecognizer;
    private final String TAG = SpeechRecognizer.class.getSimpleName();
    private MainActivity listener = null;
    private Intent startSR;

        public SpeechRecognizer(Context con) {
        googleSpeechRecognizer = android.speech.SpeechRecognizer.createSpeechRecognizer(con);
        googleSpeechRecognizer.setRecognitionListener(this);
    }

    public void listen(){

        Log.d(TAG, "start listening");
        //Festlegen wofür der Intent spezifiziert ist
        startSR = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        //Setzen des Sprachmodels
        startSR.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        //Zwischenergebnisse zulassen
        startSR.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        //Sich auf das beste Ergebnis beschränken
        startSR.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,1);
        //Sprache festlegen
        startSR.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "DE-de");
        //erkennung starten
        googleSpeechRecognizer.startListening(startSR);
    }

    public void onReadyForSpeech(Bundle params)
    {
        Log.d(TAG, "onReadyForSpeech");
    }

    public void onBeginningOfSpeech()
    {
        Log.d(TAG, "onBeginningOfSpeech");
    }

    public void onRmsChanged(float rmsdB)
    {
        Log.d(TAG, "onRmsChanged");
    }

    public void onBufferReceived(byte[] buffer)
    {
        Log.d(TAG, "onBufferReceived");
    }

    public void onEndOfSpeech() {
        Log.d(TAG, "onEndofSpeech");
    }

    public void onError(int error)
    {
        Log.d(TAG,  "error " +  error);
    }

    public void onResults(Bundle results)
    {
        String str = new String();
        ArrayList data = results.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION);
        float [] data_conf = results.getFloatArray(android.speech.SpeechRecognizer.CONFIDENCE_SCORES);

        if (data.size()> 0) {
            str = data.get(0).toString();
        }else{
            //error
        }
        Log.d(TAG, "onResults: " + data.get(0).toString().toLowerCase());

        if(listener != null)
            listener.onResultRecieving(str);
    }

    @Override
    public void onEvent(int i, Bundle bundle) {}

    @Override
    public void onPartialResults(Bundle results) {
        String str = new String();
        ArrayList data = results.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION);

        if (data.size()> 0)
            str = data.get(0).toString();
        else {return;}

        listener.onPartialResultRecieving(str);
    }


    public void addListener(MainActivity a){
        listener = a;
    }

    public void cleanUp(){
        googleSpeechRecognizer.destroy();
    }

}
