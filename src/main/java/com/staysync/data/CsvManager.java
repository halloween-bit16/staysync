package com.staysync.data;

import com.staysync.model.Booking;
import com.staysync.model.CompletedBooking;
import com.staysync.model.Room;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class CsvManager {

    private static final String DATA_DIR = "data";

    private static final String ROOMS_CSV              = DATA_DIR + "/rooms.csv";
    private static final String BOOKINGS_CSV           = DATA_DIR + "/bookings.csv";
    private static final String COMPLETED_BOOKINGS_CSV = DATA_DIR + "/completed_bookings.csv";
    private static final String AUDIT_CSV              = DATA_DIR + "/audit_log.csv";

    static {
        try {
            Files.createDirectories(Paths.get(DATA_DIR));
        } catch (IOException e) {
            System.err.println("[CSV] Could not create data directory: " + e.getMessage());
        }
    }

    private CsvManager() {}

    public static synchronized void saveRoomsCsv() {
        List<Room> rooms = List.copyOf(DataStore.getRooms());
        try (PrintWriter pw = openWriter(ROOMS_CSV, false)) {
            pw.println("Room No,Type,Price Per Night,Status");
            for (Room r : rooms) {
                pw.println(String.join(",",
                    q(r.getRoomNo()),
                    q(r.getType()),
                    String.format("%.2f", r.getPrice()),
                    q(r.getStatus().name())
                ));
            }
        } catch (IOException e) {
            System.err.println("[CSV] saveRoomsCsv error: " + e.getMessage());
        }
    }

    public static synchronized void appendBookingCsv(Booking b) {
        boolean exists = Files.exists(Paths.get(BOOKINGS_CSV));
        try (PrintWriter pw = openWriter(BOOKINGS_CSV, true)) {
            if (!exists) {
                pw.println("ID,Guest Name,Phone,Room No,Check-In,Check-Out,Nights,Total Price,Status");
            }
            pw.println(String.join(",",
                String.valueOf(b.getId()),
                q(b.getCustomer().getName()),
                q(b.getCustomer().getPhone()),
                q(b.getRoom().getRoomNo()),
                q(b.getCheckInDate()  != null ? b.getCheckInDate().toString()  : ""),
                q(b.getCheckOutDate() != null ? b.getCheckOutDate().toString() : ""),
                String.valueOf(b.getNights()),
                String.format("%.2f", b.getTotalPrice()),
                q(b.getStatus())
            ));
        } catch (IOException e) {
            System.err.println("[CSV] appendBookingCsv error: " + e.getMessage());
        }
    }

    public static synchronized void appendCompletedBookingCsv(CompletedBooking cb) {
        boolean exists = Files.exists(Paths.get(COMPLETED_BOOKINGS_CSV));
        try (PrintWriter pw = openWriter(COMPLETED_BOOKINGS_CSV, true)) {
            if (!exists) {
                pw.println("Guest Name,Phone,Room No,Room Type,Check-In,Check-Out," +
                           "Nights,Total Price,Payment Method,Amount Paid,Checkout Timestamp");
            }
            pw.println(String.join(",",
                q(cb.getCustomerName()),
                q(cb.getCustomerPhone()),
                q(cb.getRoomNo()),
                q(cb.getRoomType()),
                q(cb.getCheckInDate()  != null ? cb.getCheckInDate().toString()  : ""),
                q(cb.getCheckOutDate() != null ? cb.getCheckOutDate().toString() : ""),
                String.valueOf(cb.getNights()),
                String.format("%.2f", cb.getTotalPrice()),
                q(cb.getPaymentMethod()),
                String.format("%.2f", cb.getAmountPaid()),
                q(cb.getCheckoutTimestamp() != null ? cb.getCheckoutTimestamp().toString() : "")
            ));
        } catch (IOException e) {
            System.err.println("[CSV] appendCompletedBookingCsv error: " + e.getMessage());
        }
    }

    public static synchronized void appendAuditCsv(String entry) {
        boolean exists = Files.exists(Paths.get(AUDIT_CSV));
        try (PrintWriter pw = openWriter(AUDIT_CSV, true)) {
            if (!exists) {
                pw.println("Timestamp,Entry");
            }
            String ts = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .format(LocalDateTime.now());
            pw.println(q(ts) + "," + q(entry));
        } catch (IOException e) {
            System.err.println("[CSV] appendAuditCsv error: " + e.getMessage());
        }
    }

    private static PrintWriter openWriter(String path, boolean append) throws IOException {
        return new PrintWriter(new BufferedWriter(
            new OutputStreamWriter(
                new FileOutputStream(path, append), StandardCharsets.UTF_8)));
    }

    private static String q(String s) {
        if (s == null) s = "";
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }
}
