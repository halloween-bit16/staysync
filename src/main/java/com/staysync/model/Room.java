package com.staysync.model;

import javafx.beans.property.*;

public class Room {

    private final StringProperty             roomNo;
    private final StringProperty             type;
    private final DoubleProperty             price;
    private final ObjectProperty<RoomStatus> status;

    public Room(String roomNo, String type, double price, RoomStatus status) {
        this.roomNo  = new SimpleStringProperty(roomNo);
        this.type    = new SimpleStringProperty(type);
        this.price   = new SimpleDoubleProperty(price);
        this.status  = new SimpleObjectProperty<>(status);
    }

    public String getRoomNo()              { return roomNo.get(); }
    public void   setRoomNo(String v)      { roomNo.set(v); }
    public StringProperty roomNoProperty() { return roomNo; }

    public String getType()              { return type.get(); }
    public void   setType(String v)      { type.set(v); }
    public StringProperty typeProperty() { return type; }

    public double getPrice()              { return price.get(); }
    public void   setPrice(double v)      { price.set(v); }
    public DoubleProperty priceProperty() { return price; }

    public RoomStatus getStatus()                      { return status.get(); }
    public void       setStatus(RoomStatus v)          { status.set(v); }
    public ObjectProperty<RoomStatus> statusProperty() { return status; }

    public boolean isAvailable() { return status.get() == RoomStatus.AVAILABLE; }

    @Override
    public String toString() {
        return "Room " + getRoomNo() + " (" + getType() + ") – $" + getPrice();
    }
}
