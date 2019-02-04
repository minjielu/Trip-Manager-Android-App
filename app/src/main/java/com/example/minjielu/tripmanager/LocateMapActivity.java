/* This file allows user to locate an event by searching in a GoogleMap */

package com.example.minjielu.tripmanager;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LocateMapActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private LocationManager locationManager;
    private Double latitude;
    private Double longitude;

    public void updateLocation(View view) {
        /* Back to the create or edit event page with the selected coordinate information */
        Intent oldIntent = getIntent();
        Intent intent = new Intent(getApplicationContext(), CreateActivity.class);
        intent.putExtra("arrive", oldIntent.getStringExtra("arrive"));
        intent.putExtra("leave", oldIntent.getStringExtra("leave"));
        intent.putExtra("location", oldIntent.getStringExtra("location"));
        intent.putExtra("activity", oldIntent.getStringExtra("activity"));
        intent.putExtra("tripname", oldIntent.getStringExtra("tripname"));
        intent.putExtra("date", oldIntent.getStringExtra("date"));
        /* Provide guiding information to the next activity */
        if(view.getId() == R.id.button9) {
            // Return to the previous page, and a coordinate is located.

            intent.putExtra("latitude", Double.toString(latitude));
            intent.putExtra("longitude", Double.toString(longitude));

        }
        else {
            // Return to the previous page, but the coordinate is not updated

            intent.putExtra("latitude", oldIntent.getStringExtra("latitude"));
            intent.putExtra("longitude", oldIntent.getStringExtra("longitude"));

        }
        if(oldIntent.getStringExtra("Fun").equals("Update")) {

            intent.putExtra("itemid", oldIntent.getStringExtra("itemid"));
            intent.putExtra("Fun", "UpdateLocated");

        }
        else {

            intent.putExtra("Fun", "CreateLocated");

        }
        startActivity(intent);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        /* After the user allows access to user location, Zoom the map to the last known location */
        if(requestCode == 1) {
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                    Location lastKnowLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if(lastKnowLocation != null) {

                        updateLocation(lastKnowLocation);

                    }

                }
            }
        }
    }

    public void searchLocation(View view) {

        String searchString = ((TextView) findViewById(R.id.editText6)).getText().toString();
        if(!searchString.equals("")) {
            /* Search for the location specified by the user using the GoogleMap API */
            Geocoder geocoder = new Geocoder(this);
            List<Address> addresses = new ArrayList<Address>();
            try {

                addresses = geocoder.getFromLocationName(searchString, 1);

            } catch (IOException e) {

                e.printStackTrace();

            }
            Address address = addresses.get(0);
            String fullAddress = "";
            latitude = address.getLatitude();
            longitude = address.getLongitude();
            Location searchedLocation = new Location("searching");
            searchedLocation.setLatitude(latitude);
            searchedLocation.setLongitude(longitude);
            updateLocation(searchedLocation);

            for(int i = 0; i < address.getMaxAddressLineIndex(); i++) {

                fullAddress += address.getAddressLine(i) + '\n';

            }
            fullAddress += address.getAddressLine(address.getMaxAddressLineIndex());
            /* Display the confirm address form */
            TextView addressLine = (TextView) findViewById(R.id.textView9);
            LinearLayout linearLayout = (LinearLayout) findViewById(R.id.LinearLayout21);
            linearLayout.setVisibility(View.VISIBLE);
            addressLine.setText(fullAddress);

        }

    }

    protected void updateLocation(Location location) {
        /* Update the map after a new location is located */
        LatLng userLocation = new LatLng (location.getLatitude(), location.getLongitude());

        mMap.clear();
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 9));
        mMap.addMarker(new MarkerOptions().position(userLocation));

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_locate_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
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

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        /* Zoom the map directly to the last known location if permission is already granted by user */
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, 1);

        }
        else {

            Location lastKnowLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if(lastKnowLocation != null) {

                updateLocation(lastKnowLocation);

            }

        }


        LinearLayout detailLocation = (LinearLayout) findViewById(R.id.LinearLayout21);
        detailLocation.setVisibility(View.INVISIBLE);

        /* Perfrom search directly when a enter is pressed after user puts in information */
        EditText editText = (EditText) findViewById(R.id.editText6);
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_DONE) {

                    ImageView searchButton = (ImageView) findViewById(R.id.imageView);
                    searchButton.performClick();
                    handled = true;
                }
                return handled;
            }
        });

    }
}
