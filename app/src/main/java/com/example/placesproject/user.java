package com.example.placesproject;

public class user {
    private String ten, cmt;

    public user(String ten, String cmt) {
        this.ten = ten;
        this.cmt = cmt;
    }

    public String getTen() {
        return ten;
    }

    public void setTen(String ten) {
        this.ten = ten;
    }

    public String getCmt() {
        return cmt;
    }

    public void setCmt(String cmt) {
        this.cmt = cmt;
    }
}
