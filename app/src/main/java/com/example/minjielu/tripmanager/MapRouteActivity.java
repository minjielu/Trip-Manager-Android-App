/* This file maps daily route on a google map */

package com.example.minjielu.tripmanager;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;

import static java.util.Arrays.asList;

public class MapRouteActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private String title; // Title of the trip.
    private String date; // Date of the trip.
    private String currentDate;
    private String currentTime;

    protected void addMarker(LatLng userLocation, String leaveTime, String location) {
        /* If an event is completed, it's marked red. Otherwise, it's marked blue. */
        if(date.compareTo(currentDate) < 0 || (leaveTime.compareTo(currentTime) <= 0 && date.compareTo(currentDate) == 0)) {

            mMap.addMarker(new MarkerOptions().position(userLocation).title(location).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

        }
        else {

            mMap.addMarker(new MarkerOptions().position(userLocation).title(location).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));

        }

    }

    protected class DrawRoute extends AsyncTask<String, Integer, String> {
        /* Internet access to GoogleMap API happens in the background asynchronously. */

        @Override
        protected String doInBackground(String... strings) {

            /* Connect to the GoogleMap API to get a json object containing route information */
            String responseString = "";
            InputStream inputStream = null;
            HttpsURLConnection httpsURLConnection = null;
            try {

                URL url = new URL(strings[0]);
                httpsURLConnection = (HttpsURLConnection) url.openConnection();
                inputStream = httpsURLConnection.getInputStream();
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                StringBuffer stringBuffer = new StringBuffer();
                String line = bufferedReader.readLine();
                while(line != null) {

                    stringBuffer.append(line);
                    line = bufferedReader.readLine();

                }

                responseString = stringBuffer.toString();
                bufferedReader.close();
                inputStreamReader.close();

            }
            catch(Exception e) {

                e.printStackTrace();

            }
            finally {

                if(inputStream != null) {

                    try {

                        inputStream.close();


                    } catch (IOException e) {

                        e.printStackTrace();

                    }

                }

                if (httpsURLConnection != null) {

                    httpsURLConnection.disconnect();

                }

            }

            return strings[1] + responseString;
        }

        @Override
        protected void onPostExecute(String s) {
            /* Parse the Json object */
            super.onPostExecute(s);

            JSONObject jsonObject = null;
            List<List<HashMap<String, String>>> routes = null;
            try {

                jsonObject = new JSONObject(s.substring(1));
                DirectionsParser directionsParser = new DirectionsParser();
                routes = directionsParser.parse(jsonObject);

            } catch (JSONException e) {

                e.printStackTrace();

            }

            ArrayList<LatLng> points;
            PolylineOptions polylineOptions = null;

            if(routes != null) {

                for (List<HashMap<String, String>> path : routes) {

                    points = new ArrayList<>();
                    polylineOptions = new PolylineOptions();

                    for (HashMap<String, String> point : path) {

                        if (point.get("lat") != null && point.get("lng") != null) {

                            double lat = Double.parseDouble(point.get("lat"));
                            double lng = Double.parseDouble(point.get("lng"));

                            points.add(new LatLng(lat, lng));

                        }

                    }
                    /* Add routes on a GoogleMap */
                    polylineOptions.addAll(points);
                    polylineOptions.width(15);
                    if(s.charAt(0) == '0') {
                        // The route is read if we finished it.
                        polylineOptions.color(Color.RED);

                    }
                    else {
                        // The route is blue if we didn't finish it.
                        polylineOptions.color(Color.BLUE);

                    }
                    polylineOptions.geodesic(true);

                }

            }

            if(polylineOptions != null) {

                mMap.addPolyline(polylineOptions);

            }
            else {

                Toast.makeText(getApplicationContext(), "Some routes are not found!", Toast.LENGTH_LONG).show();

            }


        }
    }

    protected String getRequestURL(LatLng origin, LatLng dest) {

        /* Formatting the request string that is sent to the GoogleMap API */
        String str_org = "origin=" + Double.toString(origin.latitude) + "," + Double.toString(origin.longitude); // Place of departure.
        String str_dest = "destination=" + Double.toString(dest.latitude) + "," + Double.toString(dest.longitude); // Place of arrival.
        String sensor = "sensor=false";
        String mode = "mode=driving";
        String key = "key=AIzaSyC6DUjcUfWCmAZkx7qxN0QTxGJrXQ26CeE";
        String param = str_org + "&" + str_dest + "&" + sensor + "&" + mode + "&" + key;

        String output = "json";
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + param;
        return url;

    }


    protected void addRoute(LatLng fromLocation, LatLng toLocation, String arriveTime) {

        /* Add a route between two adjacent events */
        if(date.compareTo(currentDate) < 0 || (arriveTime.compareTo(currentTime) <= 0 && date.compareTo(currentDate) == 0)) {

            String reqUrl = null;
            reqUrl = getRequestURL(fromLocation, toLocation);

            DrawRoute drawRoute = new DrawRoute();
            drawRoute.execute(reqUrl, "0");

        }
        else {

            String reqUrl = null;
            reqUrl = getRequestURL(fromLocation, toLocation);

            DrawRoute drawRoute = new DrawRoute();
            drawRoute.execute(reqUrl, "1");

        }


    }

    public void backToDay(View view) {

        /* Return the upper activity that shows agenda of a date */
        Intent intent = new Intent(getApplicationContext(), SingleActivityActivity.class);
        intent.putExtra("tripname", title);
        intent.putExtra("date", date);
        startActivity(intent);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_route);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        Intent oldIntent = getIntent();
        title = oldIntent.getStringExtra("tripname");
        date = oldIntent.getStringExtra("date");
        TextView setView = (TextView) findViewById(R.id.textView11);
        setView.setText(title);
        setView = (TextView) findViewById(R.id.textView14);
        String newDate = "";

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat targetFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.US);

        try {

            newDate = targetFormat.format(dateFormat.parse(date));

        } catch (ParseException e) {

            e.printStackTrace();

        }

        setView.setText(newDate);
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        SQLiteDatabase tripManager = this.openOrCreateDatabase("TripManager", MODE_PRIVATE, null);
        Cursor cActivity = tripManager.rawQuery("SELECT arrive, leave, latitude, longitude, location FROM activities WHERE tripname = '" + title + "' and date = '" + date + "' and legal = 1 ORDER BY arrive", null);
        int arriveIndex = cActivity.getColumnIndex("arrive");
        int leaveIndex = cActivity.getColumnIndex("leave");
        int latitudeIndex = cActivity.getColumnIndex("latitude");
        int longitudeIndex = cActivity.getColumnIndex("longitude");
        int locationIndex = cActivity.getColumnIndex("location");
        cActivity.moveToFirst();

        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        DateFormat timeFormat = new SimpleDateFormat("HH:mm");
        Date date = new Date();
        currentDate = dateFormat.format(date);
        currentTime = timeFormat.format(date);

        while(!cActivity.isAfterLast() && (cActivity.getString(latitudeIndex).equals("") || cActivity.getString(longitudeIndex).equals(""))) {

            cActivity.moveToNext();

        }

        if(!cActivity.isAfterLast()) {

            LatLng lastLocation = new LatLng(Double.valueOf(cActivity.getString(latitudeIndex)), Double.valueOf(cActivity.getString(longitudeIndex)));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastLocation, 14));
            // The first event is marked yellow.
            mMap.addMarker(new MarkerOptions().position(lastLocation).title(cActivity.getString(locationIndex)).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)));
            cActivity.moveToNext();

            /* Iterate through all events */
            while(!cActivity.isAfterLast()) {

               while(!cActivity.isAfterLast() && (cActivity.getString(latitudeIndex).equals("") || cActivity.getString(longitudeIndex).equals(""))) {

                   cActivity.moveToNext();

               }
               if(!cActivity.isAfterLast()) {

                   LatLng newLocation = new LatLng(Double.valueOf(cActivity.getString(latitudeIndex)), Double.valueOf(cActivity.getString(longitudeIndex)));
                   addRoute(lastLocation, newLocation, cActivity.getString(arriveIndex));
                   addMarker(newLocation, cActivity.getString(leaveIndex), cActivity.getString(locationIndex));
                   lastLocation = newLocation;
                   cActivity.moveToNext();

               }

            }


        }



    }
}
