package com.staysync.data;

import com.staysync.model.RoomStatus;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseSeeder {

    private static final String DB_URL = "jdbc:sqlite:staySync.db";

    public static void seedIfEmpty() {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            try (var rs = conn.createStatement().executeQuery("SELECT COUNT(*) FROM rooms")) {
                if (rs.next() && rs.getInt(1) > 0) return;
            }
            seedRooms(conn);
            seedBookings(conn);
            seedCompletedBookings(conn);
            System.out.println("[Seeder] Sample data inserted successfully.");
        } catch (SQLException e) {
            System.err.println("[Seeder] Error: " + e.getMessage());
        }
    }

    private static void seedRooms(Connection conn) throws SQLException {
        String sql = "INSERT INTO rooms (room_no, type, price, status) VALUES (?,?,?,?)";
        Object[][] rooms = {
            {"101", "Single",  1200.00, "BOOKED"},
            {"102", "Single",  1200.00, "BOOKED"},
            {"103", "Single",  1200.00, "CLEANING"},
            {"104", "Double",  2200.00, "BOOKED"},
            {"105", "Double",  2200.00, "BOOKED"},
            {"106", "Double",  2200.00, "AVAILABLE"},
            {"201", "Suite",   4500.00, "BOOKED"},
            {"202", "Suite",   4500.00, "AVAILABLE"},
            {"203", "Suite",   4500.00, "MAINTENANCE"},
            {"301", "Deluxe",  6800.00, "BOOKED"},
            {"302", "Deluxe",  6800.00, "AVAILABLE"},
            {"303", "Deluxe",  6800.00, "CLEANING"},
        };
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Object[] r : rooms) {
                ps.setString(1, (String) r[0]);
                ps.setString(2, (String) r[1]);
                ps.setDouble(3, (Double) r[2]);
                ps.setString(4, (String) r[3]);
                ps.executeUpdate();
            }
        }
    }

    private static void seedBookings(Connection conn) throws SQLException {
        String sql = """
            INSERT INTO bookings
            (id, customer_name, customer_phone, room_no, check_in_date, check_out_date, nights, total_price, booking_status)
            VALUES (?,?,?,?,?,?,?,?,?)""";
        Object[][] bookings = {
            {1,  "Arjun Mehta",     "9876543210", "101", "2026-04-05", "2026-04-10", 5,  6000.00,  "ACTIVE"},
            {2,  "Priya Shah",      "8765432109", "104", "2026-04-06", "2026-04-09", 3,  6600.00,  "ACTIVE"},
            {3,  "Rohit Sharma",    "7654321098", "201", "2026-04-07", "2026-04-14", 7,  31500.00, "ACTIVE"},
            {4,  "Neha Gupta",      "6543210987", "301", "2026-04-08", "2026-04-11", 3,  20400.00, "ACTIVE"},
            {5,  "Ishita Verma",    "9123456780", "102", "2026-04-09", "2026-04-12", 3,  3600.00,  "ACTIVE"},
            {6,  "Anil Kumar",      "9234567801", "105", "2026-04-09", "2026-04-11", 2,  4400.00,  "ACTIVE"},
        };
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Object[] b : bookings) {
                ps.setInt(1,    (Integer) b[0]);
                ps.setString(2, (String)  b[1]);
                ps.setString(3, (String)  b[2]);
                ps.setString(4, (String)  b[3]);
                ps.setString(5, (String)  b[4]);
                ps.setString(6, (String)  b[5]);
                ps.setInt(7,    (Integer) b[6]);
                ps.setDouble(8, (Double)  b[7]);
                ps.setString(9, (String)  b[8]);
                ps.executeUpdate();
            }
        }
    }

    private static void seedCompletedBookings(Connection conn) throws SQLException {
        String sql = """
            INSERT INTO completed_bookings
            (customer_name, customer_phone, room_no, room_type, check_in_date, check_out_date,
             nights, total_price, payment_method, amount_paid, checkout_timestamp)
            VALUES (?,?,?,?,?,?,?,?,?,?,?)""";
        Object[][] completed = {
            {"Sana Kapoor",   "9988776655", "102", "Single", "2026-04-01", "2026-04-04", 3,  3600.00,  "Cash",    3600.00,  "2026-04-04T11:30:00"},
            {"Vikram Nair",   "8877665544", "105", "Double", "2026-03-28", "2026-04-02", 5,  11000.00, "Card",    11000.00, "2026-04-02T14:15:00"},
            {"Divya Reddy",   "7766554433", "202", "Suite",  "2026-03-25", "2026-04-01", 7,  31500.00, "Card",    31500.00, "2026-04-01T10:45:00"},
            {"Karan Joshi",   "6655443322", "302", "Deluxe", "2026-03-20", "2026-03-25", 5,  34000.00, "Deposit", 34000.00, "2026-03-25T12:00:00"},
            {"Meera Pillai",  "5544332211", "103", "Single", "2026-03-15", "2026-03-17", 2,  2400.00,  "Cash",    2400.00,  "2026-03-17T09:20:00"},
            {"Rhea Malhotra", "9432108765", "106", "Double", "2026-03-12", "2026-03-15", 3,  6600.00,  "Card",    6600.00,  "2026-03-15T15:40:00"},
            {"Aditya Menon",  "9543210987", "203", "Suite",  "2026-03-09", "2026-03-12", 3,  13500.00, "Cash",    13500.00, "2026-03-12T10:10:00"},
            {"Pooja Arora",   "9654321098", "101", "Single", "2026-03-06", "2026-03-08", 2,  2400.00,  "Deposit", 2400.00,  "2026-03-08T12:25:00"},
            {"Sameer Khan",   "9765432109", "302", "Deluxe", "2026-03-03", "2026-03-06", 3,  20400.00, "Card",    20400.00, "2026-03-06T17:05:00"},
        };
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Object[] c : completed) {
                ps.setString(1,  (String) c[0]);
                ps.setString(2,  (String) c[1]);
                ps.setString(3,  (String) c[2]);
                ps.setString(4,  (String) c[3]);
                ps.setString(5,  (String) c[4]);
                ps.setString(6,  (String) c[5]);
                ps.setInt(7,     (Integer) c[6]);
                ps.setDouble(8,  (Double)  c[7]);
                ps.setString(9,  (String)  c[8]);
                ps.setDouble(10, (Double)  c[9]);
                ps.setString(11, (String)  c[10]);
                ps.executeUpdate();
            }
        }
    }
}
