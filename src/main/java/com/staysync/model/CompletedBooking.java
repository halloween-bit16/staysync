package com.staysync.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class CompletedBooking {

    private final int           id;
    private final String        customerName;
    private final String        customerPhone;
    private final String        roomNo;
    private final String        roomType;
    private final LocalDate     checkInDate;
    private final LocalDate     checkOutDate;
    private final int           nights;
    private final double        totalPrice;
    private final String        paymentMethod;
    private final double        amountPaid;
    private final LocalDateTime checkoutTimestamp;

    public CompletedBooking(int id, String customerName, String customerPhone,
                            String roomNo, String roomType,
                            LocalDate checkInDate, LocalDate checkOutDate,
                            int nights, double totalPrice,
                            String paymentMethod, double amountPaid,
                            LocalDateTime checkoutTimestamp) {
        this.id                = id;
        this.customerName      = customerName;
        this.customerPhone     = customerPhone;
        this.roomNo            = roomNo;
        this.roomType          = roomType;
        this.checkInDate       = checkInDate;
        this.checkOutDate      = checkOutDate;
        this.nights            = nights;
        this.totalPrice        = totalPrice;
        this.paymentMethod     = paymentMethod;
        this.amountPaid        = amountPaid;
        this.checkoutTimestamp = checkoutTimestamp;
    }

    public int           getId()                { return id; }
    public String        getCustomerName()       { return customerName; }
    public String        getCustomerPhone()      { return customerPhone; }
    public String        getRoomNo()             { return roomNo; }
    public String        getRoomType()           { return roomType; }
    public LocalDate     getCheckInDate()        { return checkInDate; }
    public LocalDate     getCheckOutDate()       { return checkOutDate; }
    public int           getNights()             { return nights; }
    public double        getTotalPrice()         { return totalPrice; }
    public String        getPaymentMethod()      { return paymentMethod; }
    public double        getAmountPaid()         { return amountPaid; }
    public LocalDateTime getCheckoutTimestamp()  { return checkoutTimestamp; }
}
