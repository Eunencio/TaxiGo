package com.example.eunenciotovele.taxigo;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.eunenciotovele.taxigo.Common.common;
import com.example.eunenciotovele.taxigo.Helper.DirectionJSONParser;
import com.example.eunenciotovele.taxigo.Model.DataMessage;
import com.example.eunenciotovele.taxigo.Model.FCMResponse;
import com.example.eunenciotovele.taxigo.Model.Token;
import com.example.eunenciotovele.taxigo.Remote.IFCMService;
import com.example.eunenciotovele.taxigo.Remote.IGoogleAPI;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class DriverTraking extends FragmentActivity implements OnMapReadyCallback ,
        GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks,
        LocationListener{

    private GoogleMap mMap;

    String riderLat, riderLng;

    String customerId;

    private static final int PLAY_SERVICE_RES_REQUEST = 7001;

    private LocationRequest mlocationRequest;
    private GoogleApiClient mgoogleApiClient;


    private static int UPDATE_INTEVAL = 5000;
    private static int FATEST_INTERVAL = 3000;
    private static int DISPLACEMENT = 10;

    private Circle riderMarker;
    private Marker driverMarker;

    private Polyline direction;

    IGoogleAPI mService;
    IFCMService mFCMService;

    GeoFire geoFire;

    Button btnStartTrip;

    Location pickupLocation;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_traking);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        if(getIntent() != null)
        {
            riderLat= getIntent().getStringExtra("lat");
            riderLng= getIntent().getStringExtra("lng");
            customerId = getIntent().getStringExtra("customerId");
        }

        mService = common.getGoogleAPI();
        mFCMService = common.getFCMService();

        setUpLocation();

        btnStartTrip = (Button)findViewById(R.id.btnStartTrip);
        btnStartTrip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(btnStartTrip.getText().equals("COMECAR A VIAGEM"))
                {
                    pickupLocation = common.mLastLocation;
                    btnStartTrip.setText("DROP OFF HERE");
                }
                else if(btnStartTrip.getText().equals("DROP OFF HERE"))
                {
                    calculateCashfee(pickupLocation, common.mLastLocation);
                }

            }
        });

    }

    private void calculateCashfee(final Location pickupLocation, Location mLastLocation) {


        String requestAPI = null;
        try{
            requestAPI = "https://maps.googleapis.com/maps/api/directions/json?"+
                    "mode=driving&"
                    +"transit_routing_preference=less_driving&"+
                    "origin="+pickupLocation.getLatitude()+","+pickupLocation.getLongitude()+"&"+
                    "destination="+mLastLocation.getLatitude()+","+mLastLocation.getLongitude()+"&"+
                    "key="+getResources().getString(R.string.google_direction_api);

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

                                //get distance
                                JSONObject distance = legsObject.getJSONObject("distance");
                                String distance_text = distance.getString("text");
                                Double distance_value = Double.parseDouble(distance_text.replaceAll("[^0-9\\\\.]+", ""));

                                JSONObject timeObject = legsObject.getJSONObject("duration");
                                String time_text = distance.getString("text");
                                Double time_value = Double.parseDouble(time_text.replaceAll("[^0-9\\\\.]+", ""));


                                sendDropOffNotification(customerId);

                                Intent intent = new Intent(DriverTraking.this, TripDetail.class);
                                intent.putExtra("start_address", legsObject.getString("start_address"));
                                intent.putExtra("end_address", legsObject.getString("end_address"));
                                intent.putExtra("time", String.valueOf(time_value));
                                intent.putExtra("distance", String.valueOf(distance_value));
                                intent.putExtra("total", common.formulaprice(distance_value, time_value));
                                intent.putExtra("location_start", String.format("%f,%f", pickupLocation.getLatitude(), pickupLocation.getLongitude()));
                                intent.putExtra("location_end", String.format("%f,%f", common.mLastLocation.getLatitude(), common.mLastLocation.getLongitude()));

                                startActivity(intent);
                                finish();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onFailure(Call<String> call, Throwable t) {
                            Toast.makeText(DriverTraking.this, ""+t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });

        }catch (Exception e)
        {
            e.printStackTrace();
        }
    }


    private void setUpLocation() {

        if(checkPlayServices()){
            buildGoogleApiClient();
            createLocationRequest();
            displayLocation();
        }

    }


    private void createLocationRequest() {
        mlocationRequest = new LocationRequest();
        mlocationRequest.setInterval(UPDATE_INTEVAL);
        mlocationRequest.setFastestInterval(FATEST_INTERVAL);
        mlocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mlocationRequest.setSmallestDisplacement(DISPLACEMENT);

    }

    private void buildGoogleApiClient() {

        mgoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mgoogleApiClient.connect();
    }

    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if(resultCode != ConnectionResult.SUCCESS)
        {
            if(GooglePlayServicesUtil.isUserRecoverableError(resultCode))
                GooglePlayServicesUtil.getErrorDialog(resultCode, this, PLAY_SERVICE_RES_REQUEST);
            else
            {
                Toast.makeText(this, "Disposetivo nao suportado", Toast.LENGTH_SHORT).show();
                finish();
            }
            return false;
        }
        return true;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {


        try{
            boolean isSuccess = googleMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(this, R.raw.uber_style_map)
            );
            if(isSuccess)
                Log.e("ERROR","Map Style load failed");
        }catch (Resources.NotFoundException ex){
            ex.printStackTrace();
        }


        mMap = googleMap;

        riderMarker = mMap.addCircle(new CircleOptions()
                .center(new LatLng(Double.parseDouble(riderLat), Double.parseDouble(riderLng)))
                .radius(50)
                .strokeColor(Color.BLUE)
                .fillColor(0x220000FF)
                .strokeWidth(5.0f));

        geoFire = new GeoFire(FirebaseDatabase.getInstance().getReference(common.driver_tbl)
        .child(common.correntUberDriver.getCarType()));
        GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(Double.parseDouble(riderLat), Double.parseDouble(riderLng)), 0.05f);
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                sendArrivedNotification(customerId);
                btnStartTrip.setEnabled(true);
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });

    }

    private void sendArrivedNotification(String customerId) {
        Token token = new Token(customerId);
      /*  Notification notification = new Notification("Chegada", String.format("O Transporte chegou na sua localizacao!", common.correntUberDriver.getNome()));
        Sender sender = new Sender(token.getToken(), notification);*/

        Map<String, String> content = new HashMap<>();
        content.put("title", "Chegou");
        content.put("message", String.format("O Transporte chegou na sua localizacao!", common.correntUberDriver.getName()));
        DataMessage dataMessage = new DataMessage(token.getToken(), content);

        mFCMService.sendMessage(dataMessage).enqueue(new Callback<FCMResponse>() {
            @Override
            public void onResponse(Call<FCMResponse> call, Response<FCMResponse> response) {
                if(response.body().success!=1)
                {
                    Toast.makeText(DriverTraking.this, "Failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<FCMResponse> call, Throwable t) {

            }
        });
    }

    private void sendDropOffNotification(String customerId) {
        Token token = new Token(customerId);
        /*Notification notification = new Notification("DropOff", customerId);
        Sender sender = new Sender(token.getToken(), notification);
*/
        Map<String, String> content = new HashMap<>();
        content.put("title", "DropOff");
        content.put("message", customerId);
        DataMessage dataMessage = new DataMessage(token.getToken(), content);

        mFCMService.sendMessage(dataMessage).enqueue(new Callback<FCMResponse>() {
            @Override
            public void onResponse(Call<FCMResponse> call, Response<FCMResponse> response) {
                if(response.body().success!=1)
                {
                    Toast.makeText(DriverTraking.this, "Failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<FCMResponse> call, Throwable t) {

            }
        });
    }

    private void displayLocation() {
        if(ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION)!= PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED ){
            return;
        }
        common.mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mgoogleApiClient);
        if(common.mLastLocation != null)
        {

            final double latitude = common.mLastLocation.getLatitude();
            final double longetude = common.mLastLocation.getLongitude();

            if(driverMarker != null)
                driverMarker.remove();
            driverMarker = mMap.addMarker(new MarkerOptions().position(new LatLng(latitude, longetude))
                    .title("Voce")
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker)));

            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longetude), 17.0f));

            if(direction != null)
                direction.remove();
            getDirection();

        }
        else
        {
            Log.d("ERRO", "Nao foi possivel obter a Localizacao");
        }

    }

    private void getDirection() {
        LatLng currentPosition = new LatLng(common.mLastLocation.getLatitude(), common.mLastLocation.getLongitude());

        String requestAPI = null;
        try{
            requestAPI = "https://maps.googleapis.com/maps/api/directions/json?"+
                    "mode=driving&"
                    +"transit_routing_preference=less_driving&"
                    +"origin="+currentPosition.latitude+","+currentPosition.longitude+"&"
                    +"destination="+riderLat+","+riderLng+"&"
                    +"key="+getResources().getString(R.string.google_direction_api);

            Log.d("Eunencio", requestAPI);
            mService.getPath(requestAPI)
                    .enqueue(new Callback<String>() {
                        @Override
                        public void onResponse(Call<String> call, Response<String> response) {
                            try {

                                new parserTask().execute(response.body().toString());


                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onFailure(Call<String> call, Throwable t) {
                            Toast.makeText(DriverTraking.this, ""+t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });

        }catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void startLocationUpdate() {
        if(ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION)!= PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED ){
            return;
        }

        LocationServices.FusedLocationApi.requestLocationUpdates(mgoogleApiClient, mlocationRequest, this);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {


        displayLocation();
        startLocationUpdate();
    }

    @Override
    public void onConnectionSuspended(int i) {
        mgoogleApiClient.connect();

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        common.mLastLocation = location;
        displayLocation();

    }

    private class parserTask extends AsyncTask<String, Integer,List<List<HashMap<String, String>>>>
    {
        ProgressDialog mDialog = new ProgressDialog(DriverTraking.this);

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mDialog.setMessage("Aguarde por favor...");
            mDialog.show();
        }

        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... strings) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try{

                jObject = new JSONObject(strings[0]);
                DirectionJSONParser parser = new DirectionJSONParser();
                routes=parser.parse(jObject);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return routes;
        }

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> lists) {
            mDialog.dismiss();

            ArrayList<LatLng> points = new ArrayList<LatLng>();
            PolylineOptions polylinesOptions = new PolylineOptions();

            for(int i=0; i<lists.size(); i++)
            {
                points = new ArrayList();
                polylinesOptions = new PolylineOptions();

                List<HashMap<String, String>> path = lists.get(i);

                for(int j=0; j<path.size(); j++)
                {
                    HashMap<String, String> point = path.get(j);

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);

                    points.add(position);
                }
                polylinesOptions.addAll(points);
                polylinesOptions.width(10);
                polylinesOptions.color(Color.RED);
                polylinesOptions.geodesic(true);
            }
            if(points.size()!=0)
            direction = mMap.addPolyline(polylinesOptions);
        }
    }
}
