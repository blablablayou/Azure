package azurewallet.models;

import java.io.*;
import java.time.LocalDate;
import java.util.*;
import azurewallet.system.FileManager;

public class VoucherSystem {

    private static final String DATA_DIR = System.getProperty("user.dir") + "/src/azurewallet/data/";
    private static final String VOUCHERS_FILE = DATA_DIR + "vouchers.txt";

    // Auto-generate vouchers monthly (called by scheduler)
    public static void generateMonthlyVouchers(Map<String, UserAccount> users) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(VOUCHERS_FILE, true))) {
            for (UserAccount u : users.values()) {
                double value = getVoucherValueByRank(u.getRank());
                String code = generateVoucherCode(u.getUsername());
                pw.println(u.getUsername() + "," + code + "," + value + "," + LocalDate.now().plusMonths(1));
            }
            System.out.println("Monthly vouchers generated successfully for all users.");
        } catch (IOException e) {
            System.out.println("Error generating vouchers.");
        }
    }

    // Admin can manually trigger voucher generation
    public static void generateManualVouchers(Map<String, UserAccount> users) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(VOUCHERS_FILE, true))) {
            for (UserAccount u : users.values()) {
                double value = getVoucherValueByRank(u.getRank());
                String code = generateVoucherCode(u.getUsername());
                pw.println(u.getUsername() + "," + code + "," + value + "," + LocalDate.now().plusMonths(1));
            }
            System.out.println("Admin-triggered vouchers successfully created for all users!");
        } catch (IOException e) {
            System.out.println("Error generating manual vouchers.");
        }
    }

    // User redeem voucher
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