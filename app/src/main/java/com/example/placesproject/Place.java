package com.example.placesproject;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

import java.util.ArrayList;

public class Place implements ClusterItem {
    private String placeID;
    private LatLng mPosition;
    private String mTitle, mSnippet;
    private ArrayList<String> user;

    public Place(String placeID, double lat, double lng, String title, String snippet, ArrayList<String> user) {
        this.placeID = placeID;
        mPosition = new LatLng(lat, lng);
        mTitle = title;
        mSnippet = snippet;
        this.user = user;
    }

    public String getPlaceID() {
        return placeID;
    }

    public void setPlaceID(String placeID) {
        this.placeID = placeID;
    }

    public ArrayList<String> getUser() {
        return user;
    }

    public void setUser(ArrayList<String> user) {
        this.user = user;
    }

    @Override
    public LatLng getPosition() {
        return mPosition;
    }

    @Override
    public String getTitle() {
        return mTitle;
    }

    @Override
    public String getSnippet() {
        return mSnippet;
    }
}
