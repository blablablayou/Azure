package azurewallet.models;

import java.io.*;
import java.time.LocalDate;
import java.util.*;
import azurewallet.system.FileManager;

public class VoucherSystem {
    private static final String VOUCHERS_FILE = "src/azurewallet/data/vouchers.txt";

    // =============== VOUCHER GENERATION ===============
    public static void generateMonthlyVouchers(Map<String, UserAccount> users) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(VOUCHERS_FILE, true))) {
            for (UserAccount u : users.values()) {
                double value = getVoucherValueByRank(u.getRank());
                String code = generateVoucherCode(u.getUsername());
                pw.println(u.getUsername() + "," + code + "," + value + "," + LocalDate.now().plusMonths(1));
            }
        } catch (IOException e) {
            System.out.println("Error generating vouchers.");
        }
    }

    // =============== HOLIDAY's VOUCHER ===============
    public static void generateHolidayVoucher(Map<String, UserAccount> users) {
        LocalDate today = LocalDate.now();
        String key = String.format("%02d-%02d", today.getMonthValue(), today.getDayOfMonth());
        Map<String, String> HOLIDAYS = Map.of(
            "01-01", "NEWYR",     // New Year
            "02-25", "EDSA",      // EDSA Revolution
            "04-09", "ARAW",      // Araw ng Kagitingan
            "06-12", "INDEP",     // Independence Day
            "11-01", "SAINT",     // All Saint's Day
            "11-30", "BONI",      // Bonifacio Day
            "12-25", "XMAS",      // Christmas
            "12-30", "RIZAL"      // Rizal Day
        );

        if (HOLIDAYS.containsKey(key)) {
            String holidayCode = HOLIDAYS.get(key) + today.getYear();
            try (PrintWriter pw = new PrintWriter(new FileWriter(VOUCHERS_FILE, true))) {
                for (UserAccount u : users.values()) {
                    double value = getHolidayVoucherValue(u.getRank());
                    pw.println(u.getUsername() + "," + holidayCode + "," + value + "," + today.plusMonths(1));
                }
                System.out.println("Holiday voucher '" + holidayCode + "' generated for all users!");
            } catch (IOException e) {
                System.out.println("Error generating holiday vouchers.");
            }
        }
    }

    private static double getHolidayVoucherValue(String rank) {
        return switch (rank) {
            case "Silver" -> randomRange(150, 300);
            case "Gold" -> randomRange(400, 600);
            case "Platinum" -> randomRange(800, 1000);
            default -> randomRange(50, 100);
        };
    }

    // =============== EXISTING REDEEM ===============
    public static double redeemVoucher(UserAccount user, String code, FileManager fileManager) {
        List<String> lines = new ArrayList<>();
        double value = 0.0;
        boolean found = false;

        try (BufferedReader br = new BufferedReader(new FileReader(VOUCHERS_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split(",");
                if (p.length == 4 && p[0].equals(user.getUsername()) && p[1].equals(code)) {
                    LocalDate expiry = LocalDate.parse(p[3]);
                    if (expiry.isBefore(LocalDate.now())) {
                        System.out.println("Voucher expired.");
                        lines.add(line);
                        continue;
                    }
                    value = Double.parseDouble(p[2]);
                    found = true;
                    user.deposit(value);
                    fileManager.logVoucher(user.getUsername(), code, value);
                } else {
                    lines.add(line);
                }
            }
        } catch (IOException e) {
            System.out.println("Error redeeming voucher.");
        }

        if (found) {
            try (PrintWriter pw = new PrintWriter(new FileWriter(VOUCHERS_FILE))) {
                for (String l : lines) pw.println(l);
            } catch (IOException e) {
                System.out.println("Error updating vouchers.");
            }
        }

        return value;
    }

    // =============== VALUE ===============
    private static double getVoucherValueByRank(String rank) {
        return switch (rank) {
            case "Silver" -> randomRange(50, 100);
            case "Gold" -> randomRange(100, 250);
            case "Platinum" -> randomRange(250, 500);
            default -> randomRange(1, 20);
        };
    }

    private static double randomRange(int min, int max) {
        return (Math.random() * (max - min)) + min;
    }

    private static String generateVoucherCode(String username) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder(username.substring(0, 2).toUpperCase());
        for (int i = 0; i < 6; i++) {
            code.append(chars.charAt(new Random().nextInt(chars.length())));
        }
        return code.toString();
    }
}