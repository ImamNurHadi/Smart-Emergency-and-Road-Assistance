package com.example.sera;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap gMap;
    private LatLng origin;
    private LatLng destination;
    private Marker originMarker;
    private Marker destinationMarker;
    private AutoCompleteTextView searchPlace1, searchPlace2;
    private Button btnMapMode, btnStartDirections;
    private boolean isSatelliteMode = false;
    private boolean is3DMode = false;
    private ProgressBar progressBar;
    private FusedLocationProviderClient fusedLocationProviderClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        searchPlace1 = findViewById(R.id.search_place_1);
        searchPlace2 = findViewById(R.id.search_place_2);
        btnMapMode = findViewById(R.id.btn_map_mode);
        btnStartDirections = findViewById(R.id.btn_start_directions);
        progressBar = findViewById(R.id.progress_bar);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.id_map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        setupSearchListeners();
        setupMapModeToggle();
    }

    private void setupSearchListeners() {
        searchPlace1.setOnEditorActionListener((v, actionId, event) -> {
            String query = searchPlace1.getText().toString().trim();
            if (TextUtils.isEmpty(query)) return false;

            if ("here".equalsIgnoreCase(query)) {
                markCurrentLocation();
            } else {
                searchPlace(query, true);
            }
            return false;
        });

        searchPlace2.setOnEditorActionListener((v, actionId, event) -> {
            String query = searchPlace2.getText().toString().trim();
            if (TextUtils.isEmpty(query)) return false;
            searchPlace(query, false);
            return false;
        });

        btnStartDirections.setOnClickListener(v -> {
            if (origin != null && destination != null) {
                Toast.makeText(this, "Route calculation not yet implemented!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Please set both start and destination locations!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupMapModeToggle() {
        btnMapMode.setOnClickListener(v -> {
            if (is3DMode) {
                gMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                gMap.setBuildingsEnabled(false);
                btnMapMode.setText("Satellite");
                is3DMode = false;
                isSatelliteMode = false;
            } else if (isSatelliteMode) {
                gMap.setBuildingsEnabled(true);
                btnMapMode.setText("Normal");
                is3DMode = true;
                isSatelliteMode = false;
            } else {
                gMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                btnMapMode.setText("3D");
                isSatelliteMode = true;
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        gMap = googleMap;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1000);
            return;
        }

        gMap.setMyLocationEnabled(true);
        gMap.getUiSettings().setZoomControlsEnabled(true);

        LatLng defaultLocation = new LatLng(-6.200000, 106.816666); // Jakarta
        gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 15));

        gMap.setOnMarkerClickListener(marker -> {
            LatLng position = marker.getPosition();
            String message = String.format("Lat: %.5f, Lng: %.5f", position.latitude, position.longitude);
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            return false;
        });
    }

    private void markCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1000);
            return;
        }

        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                setMarker(currentLocation, "Your Current Location", true);
            } else {
                Toast.makeText(this, "Unable to find your current location", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void searchPlace(String query, boolean isStartLocation) {
        Geocoder geocoder = new Geocoder(this);
        try {
            List<Address> addresses = geocoder.getFromLocationName(query, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                LatLng location = new LatLng(address.getLatitude(), address.getLongitude());
                setMarker(location, query, isStartLocation);
            } else {
                Toast.makeText(this, "Location not found: " + query, Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error finding location: " + query, Toast.LENGTH_SHORT).show();
        }
    }

    private void setMarker(LatLng location, String title, boolean isStartLocation) {
        if (isStartLocation) {
            if (originMarker != null) originMarker.remove();
            origin = location;
            originMarker = gMap.addMarker(new MarkerOptions().position(location).title(title));
        } else {
            if (destinationMarker != null) destinationMarker.remove();
            destination = location;
            destinationMarker = gMap.addMarker(new MarkerOptions().position(location).title(title));
        }
        gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1000 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                gMap.setMyLocationEnabled(true);
            }
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
        }
    }
}
