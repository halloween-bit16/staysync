package com.staysync.data;

import com.staysync.model.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

public class DataStore {

    private static final ReadWriteLock LOCK = new ReentrantReadWriteLock(true);

    private static final ObservableList<Room>             rooms             = FXCollections.observableArrayList();
    private static final ObservableList<Customer>         customers         = FXCollections.observableArrayList();
    private static final ObservableList<Booking>          bookings          = FXCollections.observableArrayList();
    private static final ObservableList<CompletedBooking> completedBookings = FXCollections.observableArrayList();
    private static final ObservableList<String>           auditLog          = FXCollections.observableArrayList();
    private static final ObservableList<DiscountCode>     discountCodes     = FXCollections.observableArrayList();

    private static String currentUserRole = "RECEPTIONIST";

    private static final DateTimeFormatter AUDIT_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private DataStore() {}

    public static <T> T withReadLock(Supplier<T> action) {
        LOCK.readLock().lock();
        try {
            return action.get();
        } finally {
            LOCK.readLock().unlock();
        }
    }

    public static void withWriteLock(Runnable action) {
        LOCK.writeLock().lock();
        try {
            if (Platform.isFxApplicationThread()) {
                action.run();
            } else {
                Platform.runLater(action);
            }
        } finally {
            LOCK.writeLock().unlock();
        }
    }

    public static ObservableList<Room>             getRooms()             { return rooms; }
    public static ObservableList<Customer>         getCustomers()         { return customers; }
    public static ObservableList<Booking>          getBookings()          { return bookings; }
    public static ObservableList<CompletedBooking> getCompletedBookings() { return completedBookings; }
    public static ObservableList<String>           getAuditLog()          { return auditLog; }
    public static ObservableList<DiscountCode>     getDiscountCodes()     { return discountCodes; }

    public static String  getCurrentUserRole()            { return currentUserRole; }
    public static void    setCurrentUserRole(String role) { currentUserRole = role; }
    public static boolean isAdmin()                       { return "ADMIN".equals(currentUserRole); }

    public static void addAuditEntry(String action) {
        String entry = "[" + LocalDateTime.now().format(AUDIT_FMT) + "] "
                + currentUserRole + ": " + action;
        LOCK.writeLock().lock();
        try {
            auditLog.add(0, entry);
        } finally {
            LOCK.writeLock().unlock();
        }
        DatabaseManager.saveAuditEntry(entry);
    }
}
