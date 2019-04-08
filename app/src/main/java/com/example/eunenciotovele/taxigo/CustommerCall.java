package com.example.eunenciotovele.taxigo;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.eunenciotovele.taxigo.Common.common;
import com.example.eunenciotovele.taxigo.Model.DataMessage;
import com.example.eunenciotovele.taxigo.Model.FCMResponse;
import com.example.eunenciotovele.taxigo.Model.Token;
import com.example.eunenciotovele.taxigo.Remote.IFCMService;
import com.example.eunenciotovele.taxigo.Remote.IGoogleAPI;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class CustommerCall extends AppCompatActivity {

    TextView txtTime, txtDistance, txtAddress, txtCountDown;
    Button btnAccept, btnCancel;

    MediaPlayer mediaPlayer;

    IGoogleAPI mService;
    IFCMService mFCMService;

    String customerId;

    String lat, lng;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custommer_call);

        mService = common.getGoogleAPI();
        mFCMService = common.getFCMService();

        txtAddress = (TextView) findViewById(R.id.txtAddress);
        txtDistance = (TextView) findViewById(R.id.txtDistance);
        txtTime = (TextView) findViewById(R.id.txtTime);
        txtCountDown = (TextView) findViewById(R.id.txt_count_down);

        btnAccept = (Button) findViewById(R.id.btnAccept);
        btnCancel = (Button) findViewById(R.id.btnDecline);

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!TextUtils.isEmpty(customerId))
                    cancelBooking(customerId);
            }
        });

        btnAccept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CustommerCall.this, DriverTraking.class);
                intent.putExtra("lat", lat);
                intent.putExtra("lng", lng);
                intent.putExtra("customerId", customerId);

                startActivity(intent);
                finish();
            }
        });

        mediaPlayer = MediaPlayer.create(this, R.raw.ringtone);
        mediaPlayer.setLooping(true);
        mediaPlayer.start();

        if(getIntent() !=null)
        {
            lat = getIntent().getStringExtra("lat");
            lng = getIntent().getStringExtra("lng");
            customerId = getIntent().getStringExtra("customer");

            getDirection(lat, lng);
        }
        startTimer();
    }

    private void startTimer() {
        CountDownTimer countDownTimer = new CountDownTimer(30000, 1000) {
            @Override
            public void onTick(long l) {

                txtCountDown.setText(String.valueOf(l/1000));
            }

            @Override
            public void onFinish() {

                if(!TextUtils.isEmpty(customerId))
                    cancelBooking(customerId);
                else
                    Toast.makeText(CustommerCall.this, "Customer Id must be not null", Toast.LENGTH_SHORT).show();
            }
        }.start();
    }

    private void cancelBooking(String customerId) {
        Token token = new Token(customerId);

       /* Notification notification = new Notification("Cancel", "A sua Solicitacao foi rejeitada!");
        Sender sender = new Sender(token.getToken(), notification);
*/

        Map<String, String> content = new HashMap<>();
        content.put("title", "Cancel");
        content.put("message", "A sua Solicitacao foi rejeitada!");
        DataMessage dataMessage = new DataMessage(token.getToken(), content);

        mFCMService.sendMessage(dataMessage)
                .enqueue(new Callback<FCMResponse>() {
                    @Override
                    public void onResponse(Call<FCMResponse> call, Response<FCMResponse> response) {
                        if(response.body().success == 1)
                        {
                            Toast.makeText(CustommerCall.this, "Cancelado",Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }

                    @Override
                    public void onFailure(Call<FCMResponse> call, Throwable t) {

                    }
                });

    }

    private void getDirection(String lat, String lng) {

        String requestAPI = null;
        try{
            requestAPI = "https://maps.googleapis.com/maps/api/directions/json?"
                    +"mode=driving&"+"transit_routing_preference=less_driving&"
                    +"origin="+ common.mLastLocation.getLatitude()+","+common.mLastLocation.getLongitude()+"&"
                    +"destination="+lat+","+lng+"&"
                    +"key="+getResources().getString(R.string.google_direction_api);

            Log.d("Eunencio", requestAPI);
            mService.getPath(requestAPI)
                    .enqueue(new Callback<String>() {
                        @Override
                        public void onResponse(Call<String> call, Response<String> response) {
                            try {
                                JSONObject jsonObject = new JSONObject(response.body().toString());

                                JSONArray routes = jsonObject.getJSONArray("routes");

                                JSONObject object = routes.getJSONObject(0);

                                JSONArray legs = object.getJSONArray("legs");

                                JSONObject legsObject = legs.getJSONObject(0);

                                JSONObject distance = legsObject.getJSONObject("distance");
                                txtDistance.setText(distance.getString("text"));

                                JSONObject time = legsObject.getJSONObject("duration");
                                txtTime.setText(time.getString("text"));

                                String address = legsObject.getString("end_address");
                                txtAddress.setText(address);

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onFailure(Call<String> call, Throwable t) {
                            Toast.makeText(CustommerCall.this, ""+t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });

        }catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    protected void onStop() {
        if(mediaPlayer.isPlaying())
        mediaPlayer.release();
        super.onStop();
    }

    @Override
    protected void onPause() {
        if(mediaPlayer.isPlaying())
        {
            mediaPlayer.pause();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(mediaPlayer != null && !mediaPlayer.isPlaying())
        mediaPlayer.start();
    }
}

