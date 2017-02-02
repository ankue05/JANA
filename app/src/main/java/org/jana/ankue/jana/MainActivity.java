package org.jana.ankue.jana;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;

import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.microsoft.cognitiveservices.luis.clientlibrary.LUISClient;
import com.microsoft.cognitiveservices.luis.clientlibrary.LUISDialog;
import com.microsoft.cognitiveservices.luis.clientlibrary.LUISEntity;
import com.microsoft.cognitiveservices.luis.clientlibrary.LUISIntent;
import com.microsoft.cognitiveservices.luis.clientlibrary.LUISResponse;
import com.microsoft.cognitiveservices.luis.clientlibrary.LUISResponseHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;


public class MainActivity extends Activity {

    private static final String TAG = MainActivity.class.getSimpleName();

    // Required API Keys
    private final String LUIS_APP_ID = "LUIS_APP_ID";
    private final String LUIS_USER_ID = "LUIS_USER_ID";
    private final String OW_APP_ID = "OW_APP_ID";

    //Access points for used API's
    private final String radioUri = "http://1live.akacast.akamaistream.net/7/706/119434/v1/gnl.akacast.akamaistream.net/1live";
    private final String weatherUri = "http://api.openweathermap.org/data/2.5/weather?q=%s&APPID="+ OW_APP_ID +"&lang=de";

    private LUISClient luisClient = new LUISClient(LUIS_APP_ID,LUIS_USER_ID,true);
    private SpeechRecognizer speechRecognizer;
    private TextToSpeech tts;
    private MediaPlayer mediaPlayer = new MediaPlayer();
    private Button listen;
    private TextView speech_results;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        listen = (Button)findViewById(R.id.btn_sr_listen);
        speech_results = (TextView)findViewById(R.id.tv_speechResults);
        listen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                speechRecognizer.listen();
            }
        });
        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                tts.setLanguage(Locale.GERMAN);
            }
        });
        speechRecognizer = new SpeechRecognizer(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
            speechRecognizer.addListener(this);
            speechRecognizer.listen();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        speechRecognizer.cleanUp();
    }

    private void doPrediction(String query){

        try{

            luisClient.predict(query, new LUISResponseHandler() {
                @Override
                public void onSuccess(LUISResponse response) {
                    processResponse(response);
                }
                @Override
                public void onFailure(Exception e) {
                }
            });

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void onResultRecieving(String res){
        doPrediction(res);
    }
    public void onPartialResultRecieving(String res){
        speech_results.setText(res);
    }

    public void processResponse(LUISResponse response) {

        debugPrint(response);

        LUISIntent topIntent = response.getTopIntent();

        if(topIntent.getName().contains("ChangeMusic")){
            handleMusicRequest(response.getEntities());
        }
        else if(topIntent.getName().contains("GetRoute")){
            handleRouteRequest(response.getEntities());
        }
        else if(topIntent.getName().contains("GetWeather")){
            handleWeatherRequest(response.getEntities());
        }
        else if(topIntent.getName().contains("GetTime")){
            handleTimeRequest();
        }
        else{
            tts.speak("Tut mir leid, das habe ich nicht verstanden",TextToSpeech.QUEUE_FLUSH,null,"id");
        }
    }

    private void handleMusicRequest(List<LUISEntity> entities){
        boolean play = false;
        for(int i=0;i<entities.size();i++){
            if(entities.get(i).getType().equals("Music::musicOn"))
                play = true;

        }
        Uri uri = Uri.parse(radioUri);
        if(play){
            if(!mediaPlayer.isPlaying()){
                try {
                    mediaPlayer.setDataSource(this, uri);
                    mediaPlayer.prepare();
                    mediaPlayer.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }else{
            mediaPlayer.stop();
        }

    }
    private void handleRouteRequest(List<LUISEntity> entities){

        String fromLocation="",toLocation="";

        for(int i=0;i<entities.size();i++){
            if(entities.get(i).getType().equals("Location::toLocation"))
                toLocation = entities.get(i).getName();
            if(entities.get(i).getType().equals("Location::fromLocation"))
                fromLocation = entities.get(i).getName();
        }
        String uri = String.format(Locale.GERMAN, "http://maps.google.com/maps?saddr=%s&daddr=%s",fromLocation,toLocation);
        Uri gmmIntentUri =  Uri.parse(uri);
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");

        startActivity(mapIntent);
    }
    private void handleWeatherRequest(List<LUISEntity> entities){
        String location = "";
        for (int i = 0; i < entities.size(); i++) {
            if (entities.get(i).getType().equals("Location"))
                location = entities.get(i).getName();
        }
        final String loc = location;
        String uri = String.format(weatherUri,loc);
        Log.i("WeatherUri", uri);
        RequestQueue queue = Volley.newRequestQueue(this);
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, uri, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.i("Response", response.toString());
                try{
                    JSONArray a = response.getJSONArray("weather");
                    JSONObject o = a.getJSONObject(0);
                    String allgemein = o.get("description").toString();

                    o = response.getJSONObject("main");
                    String temp = o.get("temp").toString(); //kelvin
                    temp =  String.valueOf(Math.round(Double.valueOf(temp) - 273.15));
                    o = response.getJSONObject("wind");
                    String windspeed = o.get("speed").toString();

                    o = response.getJSONObject("clouds");
                    String clouds = o.get("all").toString();

                    String w = "Wetter in" + loc  + "\n ist " +allgemein + " bei " + temp + " Grad Celsius";
                    w+= " Die Windbeschwindigkeit beträgt " +windspeed + " und eine Trübheit von "+clouds+" Prozent.";
                    tts.speak(w,TextToSpeech.QUEUE_FLUSH,null,"id");

                }catch (JSONException e){
                    Log.e(TAG,e.toString());
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.i("Response", error.toString());
            }
        });
        queue.add(request);

    }

    private void handleTimeRequest(){

        Calendar c = GregorianCalendar.getInstance(Locale.GERMANY);
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int min = c.get(Calendar.MINUTE);

        String time = "Es ist " + String.valueOf(hour) + " Uhr " + String.valueOf(min);
        tts.speak(time,TextToSpeech.QUEUE_FLUSH,null,"id");
    }

    private void debugPrint(LUISResponse response){
        LUISResponse previousResponse;
        printToResponse("-------- debugPrint -----------");
        previousResponse = response;
        printToResponse(response.getQuery());
        LUISIntent topIntent = response.getTopIntent();
        printToResponse("Top Intent: " + topIntent.getName());
        printToResponse("Entities:");
        List<LUISEntity> entities = response.getEntities();
        for (int i = 0; i < entities.size(); i++) {
            printToResponse(String.valueOf(i+1)+ " - " + entities.get(i).getName());
        }
        LUISDialog dialog = response.getDialog();
        if (dialog != null) {
            printToResponse("Dialog Status: " + dialog.getStatus());
            if (!dialog.isFinished()) {
                printToResponse("Dialog prompt: " + dialog.getPrompt());
            }
        }
    }
    private void printToResponse(String txt){
        Log.i(TAG, txt);
    }
}
