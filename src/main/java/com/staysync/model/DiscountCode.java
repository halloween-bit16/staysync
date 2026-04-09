package com.staysync.model;

public class DiscountCode {

    private final String code;
    private final String description;
    private final double discountPercent;
    private boolean active;

    public DiscountCode(String code, String description, double discountPercent, boolean active) {
        this.code            = code;
        this.description     = description;
        this.discountPercent = discountPercent;
        this.active          = active;
    }

    public String  getCode()            { return code; }
    public String  getDescription()     { return description; }
    public double  getDiscountPercent() { return discountPercent; }
    public boolean isActive()           { return active; }
    public void    setActive(boolean v) { this.active = v; }
}
