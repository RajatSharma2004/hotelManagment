package com.hotel.service;
import com.hotel.db.DatabaseService;
import com.hotel.model.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class BookingService {
    private static final double TAX_RATE = 0.12;
    public BookingService() { }

    public List<Room> listAllRooms() {
        List<Room> res = new ArrayList<>();
        String q = "SELECT id,type,price_per_night,available FROM rooms";
        try (Connection c = DatabaseService.getConnection(); Statement s = c.createStatement(); ResultSet rs = s.executeQuery(q)) {
            while (rs.next()) {
                res.add(new Room(rs.getString(1), rs.getString(2), rs.getDouble(3), rs.getInt(4)==1));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return res;
    }

    public List<Room> listAvailableRooms(LocalDate from, LocalDate to) {
        List<Room> res = new ArrayList<>();
        String q = "SELECT id,type,price_per_night,available FROM rooms WHERE id NOT IN (SELECT room_id FROM bookings WHERE NOT (checkout <= ? OR checkin >= ?))";
        try (Connection c = DatabaseService.getConnection(); PreparedStatement p = c.prepareStatement(q)) {
            p.setString(1, from.toString());
            p.setString(2, to.toString());
            try (ResultSet rs = p.executeQuery()) {
                while (rs.next()) res.add(new Room(rs.getString(1), rs.getString(2), rs.getDouble(3), rs.getInt(4)==1));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return res;
    }

    private String genId(String prefix) {
        return prefix + UUID.randomUUID().toString().substring(0,8);
    }

    public String addCustomer(String name,String phone,String email) throws SQLException {
        String id = genId("C");
        String q = "INSERT INTO customers(id,name,phone,email) VALUES(?,?,?,?)";
        try (Connection c = DatabaseService.getConnection(); PreparedStatement p = c.prepareStatement(q)) {
            p.setString(1,id); p.setString(2,name); p.setString(3,phone); p.setString(4,email);
            p.executeUpdate();
        }
        return id;
    }

    public String bookRoom(String customerName,String phone,String email,String roomId, LocalDate checkin, LocalDate checkout, double discountPercent) throws SQLException {
        String rq = "SELECT price_per_night FROM rooms WHERE id = ?";
        double price = 0;
        try (Connection c = DatabaseService.getConnection(); PreparedStatement p = c.prepareStatement(rq)) {
            p.setString(1, roomId);
            try (ResultSet rs = p.executeQuery()) {
                if (!rs.next()) throw new SQLException("Room not found");
                price = rs.getDouble(1);
            }
        }
        long nights = ChronoUnit.DAYS.between(checkin, checkout);
        if (nights <= 0) throw new SQLException("Invalid dates: checkout must be after checkin");
        double base = nights * price;
        double tax = base * TAX_RATE;
        double discount = base * (discountPercent/100.0);
        double total = base + tax - discount;

        String customerId = addCustomer(customerName, phone, email);
        String bid = genId("B");
        String iq = "INSERT INTO bookings(id,customer_id,room_id,checkin,checkout,base_amount,tax,discount,total_amount) VALUES(?,?,?,?,?,?,?,?,?)";
        try (Connection c = DatabaseService.getConnection(); PreparedStatement p = c.prepareStatement(iq)) {
            p.setString(1,bid);
            p.setString(2,customerId);
            p.setString(3,roomId);
            p.setString(4,checkin.toString());
            p.setString(5,checkout.toString());
            p.setDouble(6,base);
            p.setDouble(7,tax);
            p.setDouble(8,discount);
            p.setDouble(9,total);
            p.executeUpdate();
        }
        try (Connection c = DatabaseService.getConnection(); PreparedStatement p = c.prepareStatement("UPDATE rooms SET available=0 WHERE id=?")) {
            p.setString(1, roomId); p.executeUpdate();
        }
        return bid;
    }

    public double checkout(String bookingId) throws SQLException {
        String q = "SELECT room_id,base_amount,tax,discount,total_amount FROM bookings WHERE id = ?";
        try (Connection c = DatabaseService.getConnection(); PreparedStatement p = c.prepareStatement(q)) {
            p.setString(1, bookingId);
            try (ResultSet rs = p.executeQuery()) {
                if (!rs.next()) throw new SQLException("Booking not found");
                String roomId = rs.getString(1);
                double total = rs.getDouble(4);
                try (PreparedStatement u = c.prepareStatement("UPDATE rooms SET available=1 WHERE id=?")) {
                    u.setString(1, roomId); u.executeUpdate();
                }
                return total;
            }
        }
    }

    public List<Map<String,Object>> listBookings() {
        List<Map<String,Object>> res = new ArrayList<>();
        String q = "SELECT b.id,c.name,b.room_id,b.checkin,b.checkout,b.total_amount FROM bookings b JOIN customers c ON b.customer_id=c.id";
        try (Connection c = DatabaseService.getConnection(); Statement s = c.createStatement(); ResultSet rs = s.executeQuery(q)) {
            while (rs.next()) {
                Map<String,Object> m = new HashMap<>();
                m.put("id", rs.getString(1));
                m.put("customer", rs.getString(2));
                m.put("room", rs.getString(3));
                m.put("checkin", rs.getString(4));
                m.put("checkout", rs.getString(5));
                m.put("total", rs.getDouble(6));
                res.add(m);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return res;
    }

    public void updateRoomPrice(String roomId, double newPrice) throws SQLException {
        String q = "UPDATE rooms SET price_per_night = ? WHERE id = ?";
        try (Connection c = DatabaseService.getConnection(); PreparedStatement p = c.prepareStatement(q)) {
            p.setDouble(1, newPrice); p.setString(2, roomId); p.executeUpdate();
        }
    }
}
