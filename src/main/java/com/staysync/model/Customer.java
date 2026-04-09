package com.staysync.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Customer {

    private final StringProperty name;
    private final StringProperty phone;
    private final StringProperty roomNo;

    public Customer(String name, String phone, String roomNo) {
        this.name   = new SimpleStringProperty(name);
        this.phone  = new SimpleStringProperty(phone);
        this.roomNo = new SimpleStringProperty(roomNo);
    }

    public String getName()              { return name.get(); }
    public void   setName(String v)      { name.set(v); }
    public StringProperty nameProperty() { return name; }

    public String getPhone()              { return phone.get(); }
    public void   setPhone(String v)      { phone.set(v); }
    public StringProperty phoneProperty() { return phone; }

    public String getRoomNo()              { return roomNo.get(); }
    public void   setRoomNo(String v)      { roomNo.set(v); }
    public StringProperty roomNoProperty() { return roomNo; }

    @Override
    public String toString() {
        return getName() + " (Room " + getRoomNo() + ")";
    }
}
