package azurewallet.system;

import azurewallet.models.UserAccount;
import azurewallet.main.BackgroundScheduler;
import java.util.Map;
import java.util.Scanner;
import java.io.*;
import java.time.LocalDateTime;

public class AdminControl {
    private static final String DATA_DIR = System.getProperty("user.dir") + "/src/azurewallet/data/";
    private static final String ADMIN_PASS = "admin123";
    private static final String ADMIN_LOG = DATA_DIR + "admin_log.txt";
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
        System.out.println("+==========================================================+");
        System.out.println("|                     ADMIN LOGIN PANEL                    |");
        System.out.println("+==========================================================+");
        System.out.print("| Enter admin password (0/B to go back): ");
        String pass = sc.nextLine().trim();
        if (pass.equals("0") || pass.equalsIgnoreCase("B")) return;
        if (!pass.equals(ADMIN_PASS)) {
            System.out.println("| Access denied.                                           |");
            System.out.println("+==========================================================+");
            return;
        }

        logAdminAction("Admin logged in.");

        while (true) {
            System.out.println("+==========================================================+");
            System.out.println("|                        ADMIN PANEL                       |");
            System.out.println("+==========================================================+");
            System.out.println("| [1] View All Users                                       |");
            System.out.println("| [2] Trigger Scheduler Manually                           |");
            System.out.println("| [3] View System Summary                                  |");
            System.out.println("| [4] View System Revenue                                  |");
            System.out.println("| [5] View Admin Activity Log                              |");
            System.out.println("| [6] Delete Specific User                                 |");
            System.out.println("| [7] Delete All Users                                     |");
            System.out.println("| [8] Clear All Text Files                                 |");
            System.out.println("| [9] Generate Monthly Vouchers                            |");
            System.out.println("| [10] Exit Admin Panel                                    |");
            System.out.println("+----------------------------------------------------------+");
            System.out.print("| Choose: ");
            String choice = sc.nextLine();
            System.out.println("+----------------------------------------------------------+");

            switch (choice) {
                case "1" -> {
                    viewAllUsers();
                    logAdminAction("Viewed all users.");
                }
                case "2" -> {
                    scheduler.runScheduler();
                    System.out.println("| Scheduler executed manually.                            |");
                    logAdminAction("Scheduler manually executed.");
                }
                case "3" -> {
                    showSystemSummary();
                    logAdminAction("Viewed system summary.");
                }
                case "4" -> {
                    double total = fileManager.readSystemRevenue();
                    System.out.printf("| Total Fees Collected: PHP %,.2f%n", total);
                    logAdminAction("Viewed system revenue.");
                }
                case "5" -> viewAdminLog();
                case "6" -> deleteSpecificUser(sc);
                case "7" -> deleteAllUsers(sc);
                case "8" -> clearAllTextFiles(sc);
                case "9" -> {
                    generateMonthlyVouchers();
                    logAdminAction("Generated vouchers manually.");
                }
                case "10" -> {
                    logAdminAction("Admin logged out.");
                    System.out.println("| Exiting Admin Panel...                                  |");
                    System.out.println("+==========================================================+");
                    return;
                }
                default -> {
                    System.out.println("| Invalid choice.                                         |");
                    System.out.println("+==========================================================+");
                }
            }
        }
    }

    private void viewAllUsers() {
        System.out.println("+==========================================================+");
        System.out.println("|                    REGISTERED USERS LIST                 |");
        System.out.println("+==========================================================+");
        for (UserAccount u : users.values()) {
            System.out.printf("| Username: %-46s|\n", u.getUsername());
            System.out.printf("| Mobile: %-48s|\n", u.getMobile());
            System.out.printf("| Balance: PHP %-41.2f|\n", u.getBalance());
            System.out.printf("| Rank: %-49s|\n", u.getRank());
            System.out.printf("| Points: %-47d|\n", u.getPoints());
            System.out.println("+----------------------------------------------------------+");
        }
        System.out.println("+==========================================================+");
    }

    private void showSystemSummary() {
        System.out.println("+==========================================================+");
        System.out.println("|                   SYSTEM SUMMARY DASHBOARD               |");
        System.out.println("+==========================================================+");
        System.out.printf("| Total Users:              %-28d |\n", fileManager.getTotalUsersCount());
        System.out.printf("| Total Active Vouchers:    %-28d |\n", fileManager.getTotalVouchersCount());
        System.out.printf("| Last Scheduler Run:       %-28s |\n", fileManager.readLastSchedulerRun());
        System.out.printf("| Total System Revenue:     PHP %-20.2f |\n", fileManager.readSystemRevenue());
        System.out.println("+==========================================================+");
    }

    private void deleteSpecificUser(Scanner sc) {
        System.out.println("+==========================================================+");
        System.out.println("|                   DELETE SPECIFIC USER                   |");
        System.out.println("+==========================================================+");
        System.out.print("| Enter username (0/B to go back): ");
        String target = sc.nextLine().trim().toLowerCase();
        if (target.equals("0") || target.equalsIgnoreCase("B")) return;

        if (!users.containsKey(target)) {
            System.out.println("| User not found.                                         |");
            System.out.println("+==========================================================+");
            return;
        }

        System.out.print("| Confirm delete user '" + target + "'? (Y/N): ");
        String confirm = sc.nextLine().trim().toUpperCase();
        if (confirm.equals("Y")) {
            users.remove(target);
            fileManager.saveUsers(users);
            System.out.println("| User '" + target + "' deleted successfully.              |");
            logAdminAction("Deleted user: " + target);
        } else {
            System.out.println("| Deletion cancelled.                                     |");
        }
        System.out.println("+==========================================================+");
    }

    private void deleteAllUsers(Scanner sc) {
        System.out.println("+==========================================================+");
        System.out.println("|                    DELETE ALL USERS                      |");
        System.out.println("+==========================================================+");
        System.out.print("| Are you sure you want to delete ALL users? (Y/N): ");
        String confirm = sc.nextLine().trim().toUpperCase();
        if (confirm.equals("Y")) {
            users.clear();
            fileManager.saveUsers(users);
            System.out.println("| All user accounts deleted successfully.                  |");
            logAdminAction("Deleted all users.");
        } else {
            System.out.println("| Operation cancelled.                                    |");
        }
        System.out.println("+==========================================================+");
    }

    private void clearAllTextFiles(Scanner sc) {
        System.out.println("+==========================================================+");
        System.out.println("|                   CLEAR ALL TEXT FILES                   |");
        System.out.println("+==========================================================+");
        System.out.print("| WARNING: Clear ALL system data? (Y/N): ");
        String confirm = sc.nextLine().trim().toUpperCase();
        if (confirm.equals("Y")) {
            String[] files = {
                "users.txt", "transactions.txt", "vouchers.txt",
                "voucher_log.txt", "points_log.txt", "interest_log.txt",
                "system_revenue.txt", "scheduler_log.txt", ADMIN_LOG
            };
            for (String file : files) {
                try (PrintWriter pw = new PrintWriter(DATA_DIR + file)) {
                    pw.print("");
                } catch (IOException e) {
                    System.out.println("| Error clearing " + file);
                }
            }
            users.clear();
            fileManager.saveUsers(users);
            System.out.println("| All text files cleared successfully.                     |");
            logAdminAction("Cleared all system text files.");
        } else {
            System.out.println("| Operation cancelled.                                    |");
        }
        System.out.println("+==========================================================+");
    }

    private void viewAdminLog() {
        System.out.println("+==========================================================+");
        System.out.println("|                   ADMIN ACTIVITY LOG                     |");
        System.out.println("+==========================================================+");
        try (BufferedReader br = new BufferedReader(new FileReader(ADMIN_LOG))) {
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println("| " + line);
            }
        } catch (IOException e) {
            System.out.println("| Error reading admin log.                                |");
        }
        System.out.println("+==========================================================+");
    }

    private void generateMonthlyVouchers() {
        System.out.println("+==========================================================+");
        System.out.println("|                MANUAL VOUCHER GENERATION                 |");
        System.out.println("+==========================================================+");
        azurewallet.models.VoucherSystem.generateMonthlyVouchers(users);
        System.out.println("| Vouchers generated successfully for all users.           |");
        System.out.println("+==========================================================+");
    }
}