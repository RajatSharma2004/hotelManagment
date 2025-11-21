package com.hotel.app;
import com.hotel.db.DatabaseService;
import com.hotel.service.BookingService;
import java.util.*;
import java.time.LocalDate;

public class HotelConsoleApp {
    private static final Scanner sc = new Scanner(System.in);
    private static final BookingService bs = new BookingService();

    public static void main(String[] args) {
        DatabaseService.initDatabase();
        loop();
    }

    private static void loop() {
        while (true) {
            printMenu();
            String c = sc.nextLine().trim();
            switch (c) {
                case "1": listRooms(); break;
                case "2": searchAvailableAndBook(); break;
                case "3": listBookings(); break;
                case "4": doCheckout(); break;
                case "5": adminMenu(); break;
                case "6": System.out.println("Bye"); return;
                default: System.out.println("Invalid");
            }
        }
    }

    private static void printMenu() {
        System.out.println("\n=== Hotel Console ===");
        System.out.println("1) List rooms");
        System.out.println("2) Search available rooms & Book");
        System.out.println("3) List bookings");
        System.out.println("4) Checkout (by Booking ID)");
        System.out.println("5) Admin (change prices, manage rooms)");
        System.out.println("6) Exit");
        System.out.print("Choose: ");
    }

    private static void listRooms() {
        var rooms = bs.listAllRooms();
        System.out.println("ID | Type | Price | Available");
        for (var r: rooms) System.out.println(r);
    }

    private static void searchAvailableAndBook() {
        try {
            System.out.print("From (yyyy-mm-dd): "); LocalDate from = LocalDate.parse(sc.nextLine().trim());
            System.out.print("To (yyyy-mm-dd): "); LocalDate to = LocalDate.parse(sc.nextLine().trim());
            var avail = bs.listAvailableRooms(from, to);
            if (avail.isEmpty()) { System.out.println("No available rooms"); return; }
            System.out.println("Available:");
            for (var r: avail) System.out.println(r);
            System.out.print("Choose Room ID: "); String rid = sc.nextLine().trim();
            System.out.print("Customer name: "); String name = sc.nextLine().trim();
            System.out.print("Phone: "); String phone = sc.nextLine().trim();
            System.out.print("Email: "); String email = sc.nextLine().trim();
            System.out.print("Discount percent (0 if none): "); double d = Double.parseDouble(sc.nextLine().trim());
            String bid = bs.bookRoom(name, phone, email, rid, from, to, d);
            System.out.println("Booked. Booking ID: " + bid);
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void listBookings() {
        var list = bs.listBookings();
        System.out.println("ID | Customer | Room | CheckIn | CheckOut | Total");
        for (var m: list) {
            System.out.printf("%s | %s | %s | %s | %s | %.2f\n", m.get("id"), m.get("customer"), m.get("room"), m.get("checkin"), m.get("checkout"), m.get("total"));
        }
    }

    private static void doCheckout() {
        try {
            System.out.print("Booking ID: "); String id = sc.nextLine().trim();
            double total = bs.checkout(id);
            System.out.printf("Checked out. Total: %.2f\n", total);
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void adminMenu() {
        try {
            System.out.print("Admin username: "); String u = sc.nextLine().trim();
            System.out.print("Password: "); String p = sc.nextLine().trim();
            if (!DatabaseService.authenticateAdmin(u,p)) { System.out.println("Auth failed"); return; }
            while (true) {
                System.out.println("\n-- Admin --");
                System.out.println("1) Change room price");
                System.out.println("2) Add room");
                System.out.println("3) Remove room");
                System.out.println("4) Back");
                System.out.print("Choose: ");
                String c = sc.nextLine().trim();
                if (c.equals("1")) {
                    System.out.print("Room ID: "); String rid = sc.nextLine().trim();
                    System.out.print("New price: "); double np = Double.parseDouble(sc.nextLine().trim());
                    bs.updateRoomPrice(rid, np);
                    System.out.println("Price updated");
                } else if (c.equals("2")) {
                    System.out.print("New Room ID: "); String rid = sc.nextLine().trim();
                    System.out.print("Type: "); String type = sc.nextLine().trim();
                    System.out.print("Price: "); double pr = Double.parseDouble(sc.nextLine().trim());
                    try (var cconn = DatabaseService.getConnection();
                         var ps = cconn.prepareStatement("INSERT INTO rooms(id,type,price_per_night,available) VALUES(?,?,?,1)")) {
                        ps.setString(1,rid); ps.setString(2,type); ps.setDouble(3,pr); ps.executeUpdate();
                        System.out.println("Room added");
                    }
                } else if (c.equals("3")) {
                    System.out.print("Room ID to remove: "); String rid = sc.nextLine().trim();
                    try (var cconn = DatabaseService.getConnection();
                         var ps = cconn.prepareStatement("DELETE FROM rooms WHERE id = ?")) {
                        ps.setString(1,rid); ps.executeUpdate(); System.out.println("Room removed");
                    }
                } else break;
            }
        } catch (Exception e) {
            System.out.println("Admin error: " + e.getMessage());
        }
    }
}
