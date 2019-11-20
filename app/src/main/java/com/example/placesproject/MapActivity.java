package com.example.placesproject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.location.Location;
import android.os.Bundle;

import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.GeofencingApi;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.android.PolyUtil;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.internal.PolylineEncoding;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsRoute;
import com.mancj.materialsearchbar.MaterialSearchBar;
import com.mancj.materialsearchbar.adapter.SuggestionsAdapter;
import com.skyfishjy.library.RippleBackground;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback,
        GoogleMap.OnInfoWindowClickListener{

    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private PlacesClient placesClient;
    private Location mLastKnowLocation;
    private LocationCallback locationCallback;
    private View mapView;
    private Button btnFind;
    private RippleBackground rippleBg;
    private ClusterManager<Place> mClusterManager;
    private Context context;
    private GeoApiContext mGeoApiContext;
    private Polyline polyline = null;
    private static FirebaseFirestore db;
    private ArrayList<String> arrayList;
    private Place place;
    private ArrayList<Place> places;
    private ImageView imageView;

    private final float DEFAULT_ZOOM = 18;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        context = this;
        btnFind = (Button)findViewById(R.id.btn_find);
        rippleBg = findViewById(R.id.ripple_bg);
        imageView = (ImageView)findViewById(R.id.imageView);

        db = FirebaseFirestore.getInstance();

        SupportMapFragment mapFragment = (SupportMapFragment)getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync( this);
        mapView = mapFragment.getView();

        if(mGeoApiContext == null){
            mGeoApiContext = new GeoApiContext.Builder()
                    .apiKey(getString(R.string.google_maps_api_key))
                    .build();
        }

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(MapActivity.this);
        Places.initialize(MapActivity.this, getString(R.string.google_maps_api_key));
        placesClient = Places.createClient(this);
        final AutocompleteSessionToken token = AutocompleteSessionToken.newInstance();

        btnFind.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LatLng currentMarkerLocation = mMap.getCameraPosition().target;
                rippleBg.startRippleAnimation();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        rippleBg.stopRippleAnimation();
                        setUpClusterer();
                    }
                }, 4000);
            }
        });
    }

    private void setUpClusterer() {
        try {
            mMap.setOnInfoWindowClickListener(this);
            mClusterManager = new ClusterManager<Place>(this, mMap);
            mMap.setOnCameraIdleListener(mClusterManager);
            mMap.setOnMarkerClickListener(mClusterManager);
            getPlaces();
        }catch (Exception e){
            Toast.makeText(getBaseContext(), "setUpClusterer ERROR", Toast.LENGTH_SHORT).show();
        }
    }

    private void getPlaces(){
        arrayList = new ArrayList<>();
        places = new ArrayList<>();
        try {
            db.collection("Place Location")
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    //add marker
                                    mClusterManager.addItem(new Place(document.getId(), document.getGeoPoint("geo_point").getLatitude(),
                                            document.getGeoPoint("geo_point").getLongitude(),
                                            document.get("ten").toString(), document.get("diachi").toString(), (ArrayList<String>) document.get("user"), document.get("hinhanh").toString()));
                                    //list places have user-chat
                                    places.add(new Place(document.getId(), document.getGeoPoint("geo_point").getLatitude(),
                                            document.getGeoPoint("geo_point").getLongitude(),
                                            document.get("ten").toString(), document.get("diachi").toString(), (ArrayList<String>) document.get("user"), document.get("hinhanh").toString()));
                                }
                            } else {
                                Toast.makeText(getBaseContext(), "getting documents ERROR", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }catch (Exception e){
            Toast.makeText(getBaseContext(), "getPlaces ERROR", Toast.LENGTH_SHORT).show();
        }
    }

    public void zoomRoute(List<LatLng> lstLatLngRoute) {
        imageView.setVisibility(View.INVISIBLE);
        if (mMap == null || lstLatLngRoute == null || lstLatLngRoute.isEmpty()) return;

        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        for (LatLng latLngPoint : lstLatLngRoute)
            boundsBuilder.include(latLngPoint);

        int routePadding = 120;
        LatLngBounds latLngBounds = boundsBuilder.build();

        mMap.animateCamera(
                CameraUpdateFactory.newLatLngBounds(latLngBounds, routePadding),
                600,

                null
        );
    }

    public void addPolylines(LatLng start, LatLng end){
        List<LatLng> newLine = new ArrayList<>();
        newLine.add(start);
        newLine.add(end);
        Polyline polyline = mMap.addPolyline(new PolylineOptions().addAll(newLine));
        polyline.setColor(ContextCompat.getColor(context, R.color.colorPrimaryDark));
        polyline.setClickable(true);
        polyline.setJointType(1);
        zoomRoute(polyline.getPoints());
    }

    @Override
    public void onInfoWindowClick(final Marker marker) {

        if(marker.getSnippet().equals("")){
            marker.hideInfoWindow();
        }
        else{
            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage("BẠN CHỌN "+marker.getTitle()+" - "+marker.getSnippet())
                    .setCancelable(true)
                    .setPositiveButton("CHỈ HƯỚNG", new DialogInterface.OnClickListener() {
                        public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                            addPolylines(new LatLng(mLastKnowLocation.getLatitude(), mLastKnowLocation.getLongitude()), marker.getPosition());
                            dialog.dismiss();
                        }
                    })
                    .setNegativeButton("XEM CHI TIẾT", new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                            for(Place p: places){
                                if(p.getPosition().latitude == marker.getPosition().latitude && p.getPosition().longitude == marker.getPosition().longitude){
                                    Intent intent = new Intent(MapActivity.this, RepairDetails.class);
                                    intent.putExtra("ten", marker.getTitle());
                                    intent.putExtra("diachi", marker.getSnippet());
                                    intent.putExtra("chat", p.getUser());
                                    intent.putExtra("docID", p.getPlaceID());
                                    intent.putExtra("url", p.getUrlImage());
//                                    Toast.makeText(getBaseContext(), "ID: "+p.getPlaceID(), Toast.LENGTH_SHORT).show();
                                    startActivityForResult(intent, 101);
                                }
                            }
                        }
                    });
            final AlertDialog alert = builder.create();
            alert.show();
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap){
        mMap = googleMap;
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setMapToolbarEnabled(true);

        if(mapView != null && mapView.findViewById(Integer.parseInt("1")) !=null){
            View locationButton = ((View)mapView.findViewById(Integer.parseInt("1")).getParent())
                    .findViewById(Integer.parseInt("2"));
            View locationCompass = ((View) mapView.findViewById(Integer.parseInt("1")).getParent())
                    .findViewById(Integer.parseInt("5"));
            View locationZoom = ((View) mapView.findViewById(Integer.parseInt("1")).getParent())
                    .findViewById(Integer.parseInt("1"));
            View locationToolbar = ((View) mapView.findViewById(Integer.parseInt("1")).getParent())
                    .findViewById(Integer.parseInt("4"));

            //change Tool bar position
            RelativeLayout.LayoutParams layoutParams_Toolbar = (RelativeLayout.LayoutParams) locationToolbar.getLayoutParams();
            layoutParams_Toolbar.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 0);
            layoutParams_Toolbar.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
            layoutParams_Toolbar.setMargins(0,0,0,250);

            //change Zoom Control position
            RelativeLayout.LayoutParams layoutParams_Zoom = (RelativeLayout.LayoutParams) locationZoom.getLayoutParams();
            layoutParams_Zoom.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
            layoutParams_Zoom.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
            layoutParams_Zoom.setMargins(0,0,40,450);

            //change location Button
            RelativeLayout.LayoutParams layoutParams_Location = (RelativeLayout.LayoutParams)locationButton.getLayoutParams();
            layoutParams_Location.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
            layoutParams_Location.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
            layoutParams_Location.setMargins(0,0,40,250);

            // change compass position
            RelativeLayout.LayoutParams layoutParams_Compass = (RelativeLayout.LayoutParams)
                    locationCompass.getLayoutParams();
            layoutParams_Compass.addRule(RelativeLayout.ALIGN_PARENT_LEFT, 0);
            layoutParams_Compass.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0);
            layoutParams_Compass.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
            layoutParams_Compass.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
            layoutParams_Compass.setMargins(0, 250,40, 0);
        }

        //check gps
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);

        SettingsClient settingsClient = LocationServices.getSettingsClient(MapActivity.this);
        Task<LocationSettingsResponse> task = settingsClient.checkLocationSettings(builder.build());

        task.addOnSuccessListener(MapActivity.this, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                getDeviceLocation();
            }
        });
        task.addOnFailureListener(MapActivity.this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if(e instanceof ResolvableApiException){
                    ResolvableApiException resolvable = (ResolvableApiException) e;
                    try{
                        resolvable.startResolutionForResult(MapActivity.this, 51);
                    }catch (IntentSender.SendIntentException ex){
                        ex.printStackTrace();
                    }
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 51){
            if(resultCode == RESULT_OK){
                getDeviceLocation();
            }
        }
        if(requestCode == 101){
            if(resultCode == RESULT_OK){
                setUpClusterer();
            }
        }
    }
    @SuppressLint("MissingPermission")
    private void getDeviceLocation(){
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        mFusedLocationProviderClient.getLastLocation()
                .addOnCompleteListener(new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if(task.isSuccessful()){
                            mLastKnowLocation = task.getResult();
                            if(mLastKnowLocation != null){
                                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mLastKnowLocation.getLatitude(), mLastKnowLocation.getLongitude()), DEFAULT_ZOOM));
                            }else{
                                final LocationRequest locationRequest = LocationRequest.create();
                                locationRequest.setInterval(10000);
                                locationRequest.setFastestInterval(5000);
                                locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                                locationCallback = new LocationCallback(){
                                    @Override
                                    public void onLocationResult(LocationResult locationResult) {
                                        super.onLocationResult(locationResult);
                                        if(locationResult == null){
                                            return;
                                        }
                                        mLastKnowLocation = locationResult.getLastLocation();
                                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mLastKnowLocation.getLatitude(), mLastKnowLocation.getLongitude()), DEFAULT_ZOOM));
                                        mFusedLocationProviderClient.removeLocationUpdates(locationCallback);
                                    }
                                };
                                mFusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
                            }
                        }else {
                            Toast.makeText(MapActivity.this, "unable to get last location", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}