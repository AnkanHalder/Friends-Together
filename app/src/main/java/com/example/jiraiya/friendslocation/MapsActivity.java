package com.example.jiraiya.friendslocation;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import android.content.pm.PackageManager;


import android.graphics.Color;
import android.location.Location;
import com.google.android.gms.location.LocationListener;

import android.os.AsyncTask;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener
        {

            private static final int REQUEST_CODE = 2216;
            private static final int PLAY_SERVICES_REQUEST = 1996;

            private LocationRequest mLocationRequest;
            private GoogleApiClient mGoogleApiClient;
            private Location mLastLocation;
            private Marker myMarker;

            private static int UPDATE_INTERVAL = 5000;
            private static int FASTEST_INTERVAL = 3000;
            private static int DISPLACEMENT = 10;

            private AlertDialog.Builder builder;
            private GoogleMap mMap;

            @Override
            protected void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);
                setContentView(R.layout.activity_maps);
                // Obtain the SupportMapFragment and get notified when the map is ready to be used.
                SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.map);
                mapFragment.getMapAsync(this);

                setupMapLocation();
            }

            @Override
            public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
                switch (requestCode)
                {
                    case REQUEST_CODE:
                        if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                        {
                            if (checkPlayServices())
                            {
                                buildGoogleApiClient();
                                createLocationRequest();
                                displayLocation();
                            }
                        }
                        break;
                }
            }

            private void setupMapLocation() {

                if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)!= PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(
                                this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED)
                {
                    ActivityCompat.requestPermissions(this,new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },REQUEST_CODE);
                }
                else
                    {
                        if (checkPlayServices())
                        {
                            buildGoogleApiClient();
                            createLocationRequest();
                            displayLocation();
                        }
                    }
            }

            private void displayLocation() {

                if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)!= PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(
                        this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED)
                {
                    return;
                }
                mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                if(mLastLocation != null)
                {
                    double latitude = mLastLocation.getLatitude();
                    double longitude = mLastLocation.getLongitude();

                    if(myMarker != null)
                        myMarker.remove();

                    LatLng myloc = new LatLng(latitude, longitude);
                    LatLng desti = new LatLng(latitude+0.003, longitude+0.008);
                    mMap.addMarker(new MarkerOptions().position(desti).title("Destination"));
                    mMap.addMarker(new MarkerOptions().position(myloc).title("You"));
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myloc,11.0f));

                    String url = getDirectionsUrl(myloc, desti);

                    DownloadTask downloadTask = new DownloadTask();

                    downloadTask.execute(url);

                    Log.d("LOCATION","Your Location Lat"+latitude+" ,Longi "+longitude);
                }
            }

            private void turnGPSOn(){
                String provider = Settings.Secure.getString(getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);

                if(!provider.contains("gps")){ //if gps is disabled
                    alert(this);
                }
            }

            private void createLocationRequest() {

                mLocationRequest = new LocationRequest();
                mLocationRequest.setInterval(UPDATE_INTERVAL);
                mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
                mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                mLocationRequest.setSmallestDisplacement(DISPLACEMENT);
            }

            private void buildGoogleApiClient() {
                mGoogleApiClient =new GoogleApiClient.Builder(this)
                        .addConnectionCallbacks(this)
                        .addOnConnectionFailedListener(this)
                        .addApi(LocationServices.API)
                        .build();
                mGoogleApiClient.connect();
            }

            private boolean checkPlayServices() {

                int result = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

                if(result != ConnectionResult.SUCCESS)
                {
                    if(GooglePlayServicesUtil.isUserRecoverableError(result)){
                        GooglePlayServicesUtil.getErrorDialog(result,this,PLAY_SERVICES_REQUEST).show();

                    }
                    else
                    {
                        Toast.makeText(this, "The Device is not Supported ", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                    return false;
                }
                return true;
            }


            private String getDirectionsUrl(LatLng origin, LatLng dest) {

                // Origin of route
                String str_origin = "origin=" + origin.latitude + "," + origin.longitude;

                // Destination of route
                String str_dest = "destination=" + dest.latitude + "," + dest.longitude;

                // Sensor enabled
                String sensor = "sensor=false";
                String mode = "mode=driving";

                // Building the parameters to the web service
                String parameters = str_origin + "&" + str_dest + "&" + sensor + "&" + mode;

                // Output format
                String output = "json";

                // Building the url to the web service
                String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters;


                return url;
            }

            private void alert(Context context){
                AlertDialog.Builder builder;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    builder = new AlertDialog.Builder(context, android.R.style.Theme_Material_Dialog_Alert);
                } else {
                    builder = new AlertDialog.Builder(context);
                }
                builder.setTitle("GPS is not active")
                        .setMessage("GPS required for your current location.\n Please activate your device GPS")
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                turnGPSOn();
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
            }

            private class DownloadTask extends AsyncTask<String, Void, String> {

                @Override
                protected String doInBackground(String... url) {

                    String data = "";

                    try {
                        data = downloadUrl(url[0]);
                    } catch (Exception e) {
                        Log.d("Background Task", e.toString());
                    }
                    return data;
                }

                @Override
                protected void onPostExecute(String result) {
                    super.onPostExecute(result);

                    ParserTask parserTask = new ParserTask();


                    parserTask.execute(result);

                }
            }

            private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {

                // Parsing the data in non-ui thread
                @Override
                protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

                    JSONObject jObject;
                    List<List<HashMap<String, String>>> routes = null;

                    try {
                        jObject = new JSONObject(jsonData[0]);
                        DirectionsJSONParser parser = new DirectionsJSONParser();

                        routes = parser.parse(jObject);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return routes;
                }

                @Override
                protected void onPostExecute(List<List<HashMap<String, String>>> result) {
                    ArrayList points = null;
                    PolylineOptions lineOptions = null;
                    MarkerOptions markerOptions = new MarkerOptions();

                    for (int i = 0; i < result.size(); i++) {
                        points = new ArrayList();
                        lineOptions = new PolylineOptions();

                        List<HashMap<String, String>> path = result.get(i);

                        for (int j = 0; j < path.size(); j++) {
                            HashMap<String, String> point = path.get(j);

                            double lat = Double.parseDouble(point.get("lat"));
                            double lng = Double.parseDouble(point.get("lng"));
                            LatLng position = new LatLng(lat, lng);

                            points.add(position);
                        }

                        lineOptions.addAll(points);
                        lineOptions.width(12);
                        lineOptions.color(Color.RED);
                        lineOptions.geodesic(true);

                    }

                    mMap.addPolyline(lineOptions);
                }
            }


            private String downloadUrl(String strUrl) throws IOException {
                String data = "";
                InputStream iStream = null;
                HttpURLConnection urlConnection = null;
                try {
                    URL url = new URL(strUrl);

                    urlConnection = (HttpURLConnection) url.openConnection();

                    urlConnection.connect();

                    iStream = urlConnection.getInputStream();

                    BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

                    StringBuffer sb = new StringBuffer();

                    String line = "";
                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                    }

                    data = sb.toString();

                    br.close();

                } catch (Exception e) {
                    Log.d("Exception", e.toString());
                } finally {
                    iStream.close();
                    urlConnection.disconnect();
                }
                return data;
            }



            @Override
            public void onMapReady(GoogleMap googleMap) {
                mMap = googleMap;

                turnGPSOn();
//
//                 //Add a marker in Sydney and move the camera
//                LatLng sydney = new LatLng(-34, 151);
//                mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
//
//                LatLng sydney2 = new LatLng(22.6459807, 88.4148934);
//                mMap.addMarker(new MarkerOptions().position(sydney2).title("Harami"));
//
//                mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));

            }

            @Override
            public void onLocationChanged(Location location) {
                mLastLocation = location;
                displayLocation();
                Toast.makeText(this, "Location Changed", Toast.LENGTH_SHORT).show();
            }


            @Override
            public void onConnected(@Nullable Bundle bundle) {

                displayLocation();
                startLocationUpdates();
            }

            private void startLocationUpdates() {

                if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)!= PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(
                        this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED)
                {
                    return;
                }

                LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,mLocationRequest,this);

            }

            @Override
            public void onConnectionSuspended(int i) {
                mGoogleApiClient.connect();
            }

            @Override
            public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

            }
        }
