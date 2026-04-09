package com.staysync.model;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

public class Booking {

    private static final AtomicInteger ID_COUNTER = new AtomicInteger(1);

    private final int    id;
    private Customer     customer;
    private Room         room;
    private LocalDate    checkInDate;
    private LocalDate    checkOutDate;
    private int          nights;
    private double       totalPrice;
    private String       status;

    public Booking(Customer customer, Room room,
                   LocalDate checkInDate, LocalDate checkOutDate,
                   int nights, double totalPrice) {
        this.id          = ID_COUNTER.getAndIncrement();
        this.customer    = customer;
        this.room        = room;
        this.checkInDate = checkInDate;
        this.checkOutDate= checkOutDate;
        this.nights      = nights;
        this.totalPrice  = totalPrice;
        this.status      = "ACTIVE";
    }

    public Booking(int id, Customer customer, Room room,
                   LocalDate checkInDate, LocalDate checkOutDate,
                   int nights, double totalPrice, String status) {
        this.id          = id;
        this.customer    = customer;
        this.room        = room;
        this.checkInDate = checkInDate;
        this.checkOutDate= checkOutDate;
        this.nights      = nights;
        this.totalPrice  = totalPrice;
        this.status      = status;
        ID_COUNTER.accumulateAndGet(id + 1, Math::max);
    }

    public int      getId()                       { return id; }

    public Customer getCustomer()                 { return customer; }
    public void     setCustomer(Customer c)       { this.customer = c; }

    public Room     getRoom()                     { return room; }
    public void     setRoom(Room r)               { this.room = r; }

    public LocalDate getCheckInDate()             { return checkInDate; }
    public void      setCheckInDate(LocalDate d)  { this.checkInDate = d; }

    public LocalDate getCheckOutDate()            { return checkOutDate; }
    public void      setCheckOutDate(LocalDate d) { this.checkOutDate = d; }

    public int    getNights()                     { return nights; }
    public void   setNights(int n)                { this.nights = n; }

    public double getTotalPrice()                 { return totalPrice; }
    public void   setTotalPrice(double p)         { this.totalPrice = p; }

    public String getStatus()                     { return status; }
    public void   setStatus(String s)             { this.status = s; }

    @Override
    public String toString() {
        return customer.getName() + " → Room " + room.getRoomNo();
    }
}
