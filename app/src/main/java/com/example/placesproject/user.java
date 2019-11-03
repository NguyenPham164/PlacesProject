package com.example.placesproject;

public class user {
    private String ten, cmt;

    public user(String ten, String cmt) {
        this.ten = ten;
        if(ten.equalsIgnoreCase("")){
            ten = "[No name]";
        }
        this.cmt = cmt;
        if(cmt.equalsIgnoreCase("")){
            cmt = "[No Cmt]";
        }
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
