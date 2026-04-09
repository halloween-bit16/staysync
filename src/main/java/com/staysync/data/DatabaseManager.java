package com.staysync.data;

import com.staysync.model.*;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {

    private static final String DB_URL = "jdbc:sqlite:staySync.db";

    public static synchronized void initialize() {
        try (Connection conn = connect()) {
            createTables(conn);
            DatabaseSeeder.seedIfEmpty();
        } catch (SQLException e) {
            System.err.println("[DB] Init error: " + e.getMessage());
        }
    }

    private static Connection connect() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    private static void createTables(Connection conn) throws SQLException {
        try (Statement s = conn.createStatement()) {
            s.executeUpdate("""
                CREATE TABLE IF NOT EXISTS rooms (
                    room_no TEXT PRIMARY KEY,
                    type    TEXT NOT NULL,
                    price   REAL NOT NULL,
                    status  TEXT NOT NULL DEFAULT 'AVAILABLE'
                )""");
            s.executeUpdate("""
                CREATE TABLE IF NOT EXISTS bookings (
                    id             INTEGER PRIMARY KEY,
                    customer_name  TEXT NOT NULL,
                    customer_phone TEXT NOT NULL,
                    room_no        TEXT NOT NULL,
                    check_in_date  TEXT,
                    check_out_date TEXT,
                    nights         INTEGER DEFAULT 0,
                    total_price    REAL    DEFAULT 0,
                    booking_status TEXT    NOT NULL DEFAULT 'ACTIVE'
                )""");
            s.executeUpdate("""
                CREATE TABLE IF NOT EXISTS completed_bookings (
                    id                 INTEGER PRIMARY KEY AUTOINCREMENT,
                    customer_name      TEXT,
                    customer_phone     TEXT,
                    room_no            TEXT,
                    room_type          TEXT,
                    check_in_date      TEXT,
                    check_out_date     TEXT,
                    nights             INTEGER,
                    total_price        REAL,
                    payment_method     TEXT,
                    amount_paid        REAL,
                    checkout_timestamp TEXT
                )""");
            s.executeUpdate("""
                CREATE TABLE IF NOT EXISTS audit_log (
                    id    INTEGER PRIMARY KEY AUTOINCREMENT,
                    entry TEXT
                )""");
            s.executeUpdate("""
                CREATE TABLE IF NOT EXISTS settings (
                    key   TEXT PRIMARY KEY,
                    value TEXT
                )""");
            s.executeUpdate("""
                CREATE TABLE IF NOT EXISTS discount_codes (
                    code             TEXT PRIMARY KEY,
                    description      TEXT,
                    discount_percent REAL NOT NULL,
                    active           INTEGER NOT NULL DEFAULT 1
                )""");
        }
    }

    public static synchronized void loadAll() {
        List<Room> rooms = loadRooms();
        DataStore.getRooms().setAll(rooms);

        List<Booking> bookings = loadActiveBookings(rooms);
        DataStore.getBookings().setAll(bookings);

        DataStore.getCustomers().clear();
        bookings.forEach(b -> DataStore.getCustomers().add(b.getCustomer()));

        DataStore.getCompletedBookings().setAll(loadCompletedBookings());
        DataStore.getAuditLog().setAll(loadAuditEntries());
        DataStore.getDiscountCodes().setAll(loadDiscountCodes());
    }

    private static List<Room> loadRooms() {
        List<Room> list = new ArrayList<>();
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT room_no, type, price, status FROM rooms");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                RoomStatus st = RoomStatus.AVAILABLE;
                try { st = RoomStatus.valueOf(rs.getString("status")); }
                catch (IllegalArgumentException ignored) {}
                list.add(new Room(rs.getString("room_no"), rs.getString("type"),
                                  rs.getDouble("price"), st));
            }
        } catch (SQLException e) {
            System.err.println("[DB] Load rooms: " + e.getMessage());
        }
        return list;
    }

    private static List<Booking> loadActiveBookings(List<Room> rooms) {
        List<Booking> list = new ArrayList<>();
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM bookings WHERE booking_status = 'ACTIVE'");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String roomNo = rs.getString("room_no");
                Room room = rooms.stream()
                        .filter(r -> r.getRoomNo().equals(roomNo))
                        .findFirst().orElse(null);
                if (room == null) continue;

                Customer cust = new Customer(
                        rs.getString("customer_name"),
                        rs.getString("customer_phone"), roomNo);

                LocalDate ci = parseDate(rs.getString("check_in_date"));
                LocalDate co = parseDate(rs.getString("check_out_date"));

                list.add(new Booking(
                        rs.getInt("id"), cust, room, ci, co,
                        rs.getInt("nights"), rs.getDouble("total_price"),
                        rs.getString("booking_status")));
            }
        } catch (SQLException e) {
            System.err.println("[DB] Load bookings: " + e.getMessage());
        }
        return list;
    }

    private static List<CompletedBooking> loadCompletedBookings() {
        List<CompletedBooking> list = new ArrayList<>();
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM completed_bookings ORDER BY id DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                LocalDate ci = parseDate(rs.getString("check_in_date"));
                LocalDate co = parseDate(rs.getString("check_out_date"));
                LocalDateTime ts = parseDateTime(rs.getString("checkout_timestamp"));
                list.add(new CompletedBooking(
                        rs.getInt("id"),
                        rs.getString("customer_name"), rs.getString("customer_phone"),
                        rs.getString("room_no"), rs.getString("room_type"),
                        ci, co, rs.getInt("nights"), rs.getDouble("total_price"),
                        rs.getString("payment_method"), rs.getDouble("amount_paid"), ts));
            }
        } catch (SQLException e) {
            System.err.println("[DB] Load completed: " + e.getMessage());
        }
        return list;
    }

    private static List<String> loadAuditEntries() {
        List<String> list = new ArrayList<>();
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT entry FROM audit_log ORDER BY id DESC LIMIT 1000");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(rs.getString("entry"));
        } catch (SQLException e) {
            System.err.println("[DB] Load audit: " + e.getMessage());
        }
        return list;
    }

    private static List<DiscountCode> loadDiscountCodes() {
        List<DiscountCode> list = new ArrayList<>();
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT code, description, discount_percent, active FROM discount_codes ORDER BY code");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new DiscountCode(
                        rs.getString("code"),
                        rs.getString("description"),
                        rs.getDouble("discount_percent"),
                        rs.getInt("active") == 1));
            }
        } catch (SQLException e) {
            System.err.println("[DB] Load discount codes: " + e.getMessage());
        }
        return list;
    }

    public static synchronized void saveRoom(Room room) {
        exec("INSERT OR REPLACE INTO rooms (room_no, type, price, status) VALUES (?,?,?,?)",
                ps -> {
                    ps.setString(1, room.getRoomNo());
                    ps.setString(2, room.getType());
                    ps.setDouble(3, room.getPrice());
                    ps.setString(4, room.getStatus().name());
                });
        CsvManager.saveRoomsCsv();
    }

    public static synchronized void updateRoomStatus(String roomNo, RoomStatus status) {
        exec("UPDATE rooms SET status = ? WHERE room_no = ?",
                ps -> { ps.setString(1, status.name()); ps.setString(2, roomNo); });
        CsvManager.saveRoomsCsv();
    }

    public static synchronized void saveBooking(Booking b) {
        exec("""
             INSERT OR REPLACE INTO bookings
             (id,customer_name,customer_phone,room_no,check_in_date,check_out_date,nights,total_price,booking_status)
             VALUES (?,?,?,?,?,?,?,?,?)""",
                ps -> {
                    ps.setInt(1, b.getId());
                    ps.setString(2, b.getCustomer().getName());
                    ps.setString(3, b.getCustomer().getPhone());
                    ps.setString(4, b.getRoom().getRoomNo());
                    ps.setString(5, b.getCheckInDate()  != null ? b.getCheckInDate().toString()  : null);
                    ps.setString(6, b.getCheckOutDate() != null ? b.getCheckOutDate().toString() : null);
                    ps.setInt(7, b.getNights());
                    ps.setDouble(8, b.getTotalPrice());
                    ps.setString(9, b.getStatus());
                });
        CsvManager.appendBookingCsv(b);
    }

    public static synchronized void deleteBooking(int id) {
        exec("DELETE FROM bookings WHERE id = ?",
                ps -> ps.setInt(1, id));
    }

    public static synchronized void saveCompletedBooking(CompletedBooking cb) {
        exec("""
             INSERT INTO completed_bookings
             (customer_name,customer_phone,room_no,room_type,check_in_date,check_out_date,
              nights,total_price,payment_method,amount_paid,checkout_timestamp)
             VALUES (?,?,?,?,?,?,?,?,?,?,?)""",
                ps -> {
                    ps.setString(1, cb.getCustomerName());
                    ps.setString(2, cb.getCustomerPhone());
                    ps.setString(3, cb.getRoomNo());
                    ps.setString(4, cb.getRoomType());
                    ps.setString(5, cb.getCheckInDate()  != null ? cb.getCheckInDate().toString()  : null);
                    ps.setString(6, cb.getCheckOutDate() != null ? cb.getCheckOutDate().toString() : null);
                    ps.setInt(7, cb.getNights());
                    ps.setDouble(8, cb.getTotalPrice());
                    ps.setString(9, cb.getPaymentMethod());
                    ps.setDouble(10, cb.getAmountPaid());
                    ps.setString(11, cb.getCheckoutTimestamp() != null
                            ? cb.getCheckoutTimestamp().toString() : null);
                });
        CsvManager.appendCompletedBookingCsv(cb);
    }

    public static synchronized void saveAuditEntry(String entry) {
        exec("INSERT INTO audit_log (entry) VALUES (?)",
                ps -> ps.setString(1, entry));
        CsvManager.appendAuditCsv(entry);
    }

    public static synchronized void saveDiscountCode(DiscountCode dc) {
        exec("INSERT OR REPLACE INTO discount_codes (code, description, discount_percent, active) VALUES (?,?,?,?)",
                ps -> {
                    ps.setString(1, dc.getCode());
                    ps.setString(2, dc.getDescription());
                    ps.setDouble(3, dc.getDiscountPercent());
                    ps.setInt(4, dc.isActive() ? 1 : 0);
                });
    }

    public static synchronized void deactivateDiscountCode(String code) {
        exec("UPDATE discount_codes SET active = 0 WHERE code = ?",
                ps -> ps.setString(1, code));
    }

    public static synchronized void reactivateDiscountCode(String code) {
        exec("UPDATE discount_codes SET active = 1 WHERE code = ?",
                ps -> ps.setString(1, code));
    }

    public static synchronized void saveSetting(String key, String value) {
        exec("INSERT OR REPLACE INTO settings (key, value) VALUES (?,?)",
                ps -> { ps.setString(1, key); ps.setString(2, value); });
    }

    public static synchronized String loadSetting(String key, String defaultValue) {
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT value FROM settings WHERE key = ?")) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("value");
            }
        } catch (SQLException e) {
            System.err.println("[DB] Load setting: " + e.getMessage());
        }
        return defaultValue;
    }

    @FunctionalInterface
    private interface StatementSetter {
        void set(PreparedStatement ps) throws SQLException;
    }

    private static synchronized void exec(String sql, StatementSetter setter) {
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            setter.set(ps);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[DB] exec error (" + sql.substring(0, Math.min(40, sql.length())) + "): " + e.getMessage());
        }
    }

    private static LocalDate parseDate(String s) {
        try { return (s != null && !s.isEmpty()) ? LocalDate.parse(s) : null; }
        catch (Exception e) { return null; }
    }

    private static LocalDateTime parseDateTime(String s) {
        try { return (s != null && !s.isEmpty()) ? LocalDateTime.parse(s) : null; }
        catch (Exception e) { return null; }
    }
}
