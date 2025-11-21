package com.hotel.model;
public class Room {
    private String id;
    private String type;
    private double pricePerNight;
    private boolean available;
    public Room(String id, String type, double pricePerNight, boolean available) {
        this.id = id; this.type = type; this.pricePerNight = pricePerNight; this.available = available;
    }
    public String getId(){ return id; }
    public String getType(){ return type; }
    public double getPricePerNight(){ return pricePerNight; }
    public boolean isAvailable(){ return available; }
    public void setAvailable(boolean a){ available = a; }
    @Override
    public String toString(){ return id + " | " + type + " | " + pricePerNight + " | " + (available?"Yes":"No"); }
}
