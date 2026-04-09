package com.staysync.data;

import com.staysync.model.CompletedBooking;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BillGenerator {

    private static final String BILLS_DIR = "bills";
    private static final DateTimeFormatter FILE_TS_FMT =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final DateTimeFormatter DISPLAY_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter DISPLAY_DT_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    static {
        try {
            Files.createDirectories(Paths.get(BILLS_DIR));
        } catch (IOException e) {
            System.err.println("[Bill] Could not create bills directory: " + e.getMessage());
        }
    }

    private BillGenerator() {}

    public static File generate(CompletedBooking cb) {
        String ts = FILE_TS_FMT.format(LocalDateTime.now());
        String fileName = "BILL_" + cb.getRoomNo().replaceAll("[^A-Za-z0-9]", "") + "_" + ts + ".txt";
        File file = Paths.get(BILLS_DIR, fileName).toFile();

        try (PrintWriter pw = new PrintWriter(
                new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(file), StandardCharsets.UTF_8)))) {

            String sep  = "=".repeat(54);
            String line = "-".repeat(54);

            pw.println(sep);
            pw.println(center("STAYSYNC HOTEL", 54));
            pw.println(center("Hotel Management System", 54));
            pw.println(center("Tel: +91-98765-43210", 54));
            pw.println(sep);
            pw.println(center("RECEIPT / BILL", 54));
            pw.println(sep);
            pw.println();

            pw.printf("  Bill No   : BILL-%03d%n", cb.getId() > 0 ? cb.getId() : (int)(Math.random()*9000)+1000);
            pw.printf("  Date      : %s%n",
                cb.getCheckoutTimestamp() != null
                    ? DISPLAY_DT_FMT.format(cb.getCheckoutTimestamp())
                    : DISPLAY_DT_FMT.format(LocalDateTime.now()));

            pw.println(line);
            pw.println("  GUEST DETAILS");
            pw.println(line);
            pw.printf("  Name      : %s%n", cb.getCustomerName());
            pw.printf("  Phone     : %s%n", cb.getCustomerPhone());

            pw.println(line);
            pw.println("  ROOM DETAILS");
            pw.println(line);
            pw.printf("  Room No   : %s%n", cb.getRoomNo());
            pw.printf("  Room Type : %s%n", cb.getRoomType());
            pw.printf("  Check-In  : %s%n",
                cb.getCheckInDate() != null ? DISPLAY_FMT.format(cb.getCheckInDate()) : "N/A");
            pw.printf("  Check-Out : %s%n",
                cb.getCheckOutDate() != null ? DISPLAY_FMT.format(cb.getCheckOutDate()) : "N/A");
            pw.printf("  Duration  : %d night(s)%n", cb.getNights());

            pw.println(line);
            pw.println("  CHARGES");
            pw.println(line);

            double pricePerNight = cb.getNights() > 0
                    ? cb.getTotalPrice() / cb.getNights()
                    : cb.getTotalPrice();

            pw.printf("  Room Rate : $%.2f x %d night(s)%n", pricePerNight, cb.getNights());
            pw.printf("  %-30s %10s%n", "Room charges:", String.format("$%.2f", cb.getTotalPrice()));

            pw.println(line);
            pw.printf("  %-30s %10s%n", "TOTAL AMOUNT:", String.format("$%.2f", cb.getTotalPrice()));
            pw.println(line);

            pw.println();
            pw.println("  PAYMENT DETAILS");
            pw.println(line);
            pw.printf("  Method    : %s%n", cb.getPaymentMethod());
            pw.printf("  Amount Paid: $%.2f%n", cb.getAmountPaid());
            double change = cb.getAmountPaid() - cb.getTotalPrice();
            if (change > 0) {
                pw.printf("  Change    : $%.2f%n", change);
            }
            pw.printf("  Status    : %s%n", cb.getAmountPaid() >= cb.getTotalPrice() ? "PAID" : "PARTIAL");

            pw.println();
            pw.println(sep);
            pw.println(center("Thank you for staying with StaySync!", 54));
            pw.println(center("We hope to see you again soon.", 54));
            pw.println(sep);
            pw.println();
            pw.printf("  Generated : %s%n", DISPLAY_DT_FMT.format(LocalDateTime.now()));

        } catch (IOException e) {
            System.err.println("[Bill] Failed to generate bill: " + e.getMessage());
            return null;
        }

        return file;
    }

    private static String center(String text, int width) {
        if (text.length() >= width) return text;
        int pad = (width - text.length()) / 2;
        return " ".repeat(pad) + text;
    }
}
