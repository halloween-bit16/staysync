package com.staysync.model;

public enum RoomStatus {
    AVAILABLE  ("✅ Available",   "#27AE60"),
    BOOKED     ("❌ Booked",      "#e74c3c"),
    CLEANING   ("🧹 Cleaning",    "#f39c12"),
    MAINTENANCE("🔧 Maintenance", "#8e44ad");

    private final String display;
    private final String color;

    RoomStatus(String display, String color) {
        this.display = display;
        this.color   = color;
    }

    public String getDisplay() { return display; }
    public String getColor()   { return color; }
}
