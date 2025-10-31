package azurewallet.system;

import azurewallet.models.UserAccount;
import azurewallet.main.BackgroundScheduler;
import azurewallet.models.VoucherSystem;
import java.util.Map;
import java.util.Scanner;
import java.io.*;
import java.time.LocalDateTime;

public class AdminControl {
    private static final String DATA_DIR = "src/data/";
    private static final String ADMIN_PASS = "admin123";
    private static final String ADMIN_LOG = "admin_log.txt";
    private final FileManager fileManager;
    private final Map<String, UserAccount> users;
    private final BackgroundScheduler scheduler;

    public AdminControl(FileManager fileManager, Map<String, UserAccount> users, BackgroundScheduler scheduler) {
        this.fileManager = fileManager;
        this.users = users;
        this.scheduler = scheduler;
        createLogFile();
    }

    private void createLogFile() {
        try {
            new File(DATA_DIR).mkdirs();
            new File(ADMIN_LOG).createNewFile();
        } catch (IOException e) {
            System.out.println("Error initializing admin log file.");
        }
    }

    private void logAdminAction(String action) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(ADMIN_LOG, true))) {
            pw.println(LocalDateTime.now() + " - " + action);
        } catch (IOException e) {
            System.out.println("Error logging admin action.");
        }
    }

    public void menu(Scanner sc) {
        System.out.print("Enter admin password: ");
        String pass = sc.nextLine().trim();
        if (pass.equalsIgnoreCase("b") || pass.equals("0")) {
            System.out.println("Returning to main menu...");
            return;
        }
        if (!pass.equals(ADMIN_PASS)) {
            System.out.println("Access denied.");
            return;
        }

        logAdminAction("Admin logged in.");

        while (true) {
            System.out.println("\n=== ADMIN PANEL ===");
            System.out.println("[1] View All Users");
            System.out.println("[2] Trigger Scheduler Manually");
            System.out.println("[3] View System Summary");
            System.out.println("[4] View System Revenue");
            System.out.println("[5] View Admin Activity Log");
            System.out.println("[6] Delete Specific User");
            System.out.println("[7] Delete All Users");
            System.out.println("[8] Clear All Text Files");
            System.out.println("[9] Generate Vouchers for All Users");
            System.out.println("[10] Exit Admin Panel");
            System.out.print("Choose: ");
            String choice = sc.nextLine();

            if (choice.equalsIgnoreCase("b") || choice.equals("0")) {
                System.out.println("Returning to previous menu...");
                return;
            }

            switch (choice) {
                case "1" -> {
                    viewAllUsers();
                    logAdminAction("Viewed all users.");
                }
                case "2" -> {
                    scheduler.runScheduler();
                    System.out.println("Scheduler executed manually.");
                    logAdminAction("Scheduler manually executed.");
                }
                case "3" -> {
                    showSystemSummary();
                    logAdminAction("Viewed system summary.");
                }
                case "4" -> {
                    double total = fileManager.readSystemRevenue();
                    System.out.println("Total Fees Collected: PHP " + String.format("%,.2f", total));
                    logAdminAction("Viewed system revenue.");
                }
                case "5" -> viewAdminLog();
                case "6" -> deleteSpecificUser(sc);
                case "7" -> deleteAllUsers(sc);
                case "8" -> clearAllTextFiles(sc);
                case "9" -> {
                    VoucherSystem.generateManualVouchers(users);
                    logAdminAction("Manually generated vouchers for all users.");
                }
                case "10" -> {
                    logAdminAction("Admin logged out.");
                    System.out.println("Exiting Admin Panel...");
                    return;
                }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    private void viewAllUsers() {
        System.out.println("\n=== REGISTERED USERS ===");
        for (UserAccount u : users.values()) {
            System.out.println("Username: " + u.getUsername());
            System.out.println("Mobile: " + u.getMobile());
            System.out.println("Balance: PHP " + String.format("%,.2f", u.getBalance()));
            System.out.println("Rank: " + u.getRank());
            System.out.println("Points: " + u.getPoints());
            System.out.println("--------------------------");
        }
    }

    private void showSystemSummary() {
        System.out.println("\n=== SYSTEM SUMMARY DASHBOARD ===");
        System.out.println("Total Users: " + fileManager.getTotalUsersCount());
        System.out.println("Total Active Vouchers: " + fileManager.getTotalVouchersCount());
        System.out.println("Last Scheduler Run: " + fileManager.readLastSchedulerRun());
        System.out.println("Total System Revenue: PHP " + String.format("%,.2f", fileManager.readSystemRevenue()));
        System.out.println("=================================");
    }

    private void deleteSpecificUser(Scanner sc) {
        System.out.print("Enter username to delete (or B to go back): ");
        String target = sc.nextLine().trim().toLowerCase();
        if (target.equalsIgnoreCase("b") || target.equals("0")) return;

        if (!users.containsKey(target)) {
            System.out.println("User not found.");
            return;
        }
        System.out.print("Are you sure you want to delete user '" + target + "'? (Y/N/B): ");
        String confirm = sc.nextLine().trim().toUpperCase();
        if (confirm.equals("B")) return;

        if (confirm.equals("Y")) {
            users.remove(target);
            fileManager.saveUsers(users);
            System.out.println("User '" + target + "' successfully deleted.");
            logAdminAction("Deleted user: " + target);
        } else {
            System.out.println("Deletion cancelled.");
        }
    }

    private void deleteAllUsers(Scanner sc) {
        System.out.print("Are you sure you want to delete ALL users? (Y/N/B): ");
        String confirm = sc.nextLine().trim().toUpperCase();
        if (confirm.equals("B")) return;

        if (confirm.equals("Y")) {
            users.clear();
            fileManager.saveUsers(users);
            System.out.println("All user accounts have been deleted.");
            logAdminAction("Deleted all users.");
        } else {
            System.out.println("Operation cancelled.");
        }
    }

    private void clearAllTextFiles(Scanner sc) {
        System.out.print("WARNING: This will clear ALL system data (logs, vouchers, users). Proceed? (Y/N/B): ");
        String confirm = sc.nextLine().trim().toUpperCase();
        if (confirm.equals("B")) return;

        if (confirm.equals("Y")) {
            String[] files = {
                "users.txt", "transactions.txt", "vouchers.txt",
                "voucher_log.txt", "points_log.txt", "interest_log.txt",
                "system_revenue.txt", "scheduler_log.txt", ADMIN_LOG
            };
            for (String file : files) {
                try (PrintWriter pw = new PrintWriter(file)) {
                    pw.print("");
                } catch (IOException e) {
                    System.out.println("Error clearing " + file);
                }
            }
            users.clear();
            fileManager.saveUsers(users);
            System.out.println("All system text files have been cleared.");
            logAdminAction("Cleared all system text files.");
        } else {
            System.out.println("Operation cancelled.");
        }
    }

    private void viewAdminLog() {
        System.out.println("\n=== ADMIN ACTIVITY LOG ===");
        try (BufferedReader br = new BufferedReader(new FileReader(ADMIN_LOG))) {
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }
        } catch (IOException e) {
            System.out.println("Error reading admin log.");
        }
    }
}