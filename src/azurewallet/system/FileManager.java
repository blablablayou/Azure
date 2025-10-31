package azurewallet.system;

import java.io.*;
import java.util.*;
import azurewallet.models.UserAccount;
import java.text.DecimalFormat;

public class FileManager {

    private static final String DATA_DIR = System.getProperty("user.dir") + "/src/azurewallet/data/";

    private static final String USERS_FILE = DATA_DIR + "users.txt";
    private static final String TRANSACTIONS_FILE = DATA_DIR + "transactions.txt";
    private static final String VOUCHERS_FILE = DATA_DIR + "vouchers.txt";
    private static final String VOUCHER_LOG_FILE = DATA_DIR + "voucher_log.txt";
    private static final String POINTS_LOG_FILE = DATA_DIR + "points_log.txt";
    private static final String INTEREST_LOG_FILE = DATA_DIR + "interest_log.txt";
    private static final String SYSTEM_REVENUE_FILE = DATA_DIR + "system_revenue.txt";
    private static final String SCHEDULER_FILE = DATA_DIR + "scheduler_log.txt";

    private final DecimalFormat df = new DecimalFormat("#,##0.00");

    public FileManager() {
        try {
            File folder = new File(DATA_DIR);
            if (!folder.exists()) folder.mkdirs();

            new File(USERS_FILE).createNewFile();
            new File(TRANSACTIONS_FILE).createNewFile();
            new File(VOUCHERS_FILE).createNewFile();
            new File(VOUCHER_LOG_FILE).createNewFile();
            new File(POINTS_LOG_FILE).createNewFile();
            new File(INTEREST_LOG_FILE).createNewFile();
            new File(SYSTEM_REVENUE_FILE).createNewFile();
            new File(SCHEDULER_FILE).createNewFile();
        } catch (IOException e) {
            System.out.println("Error initializing data files: " + e.getMessage());
        }
    }

    public Map<String, UserAccount> loadUsers() {
        Map<String, UserAccount> users = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(USERS_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split(",");
                if (p.length >= 9) {
                    UserAccount acc = new UserAccount(
                        p[0], p[1], p[2],
                        Double.parseDouble(p[3]),
                        Integer.parseInt(p[4]),
                        Double.parseDouble(p[5]),
                        p[6],
                        Integer.parseInt(p[7]),
                        Long.parseLong(p[8])
                    );
                    users.put(p[0], acc);
                }
            }
        } catch (IOException e) {
            System.out.println("Error loading users.");
        }
        return users;
    }

    public void saveUsers(Map<String, UserAccount> users) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(USERS_FILE))) {
            for (UserAccount u : users.values()) {
                pw.println(u.toFileFormat());
            }
        } catch (IOException e) {
            System.out.println("Error saving users.");
        }
    }

    public void logTransaction(String username, String type, double amount) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(TRANSACTIONS_FILE, true))) {
            pw.println(java.time.LocalDateTime.now() + " - " + username + ": " + type + " - PHP " + df.format(amount));
        } catch (IOException e) {
            System.out.println("Error logging transaction.");
        }
    }

    public void logPoints(String username, String action, int points, String note) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(POINTS_LOG_FILE, true))) {
            pw.println(java.time.LocalDateTime.now() + " - " + username + " " + action + " " + points + " points (" + note + ")");
        } catch (IOException e) {
            System.out.println("Error logging points.");
        }
    }

    public void logInterest(String username, double amount) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(INTEREST_LOG_FILE, true))) {
            pw.println(java.time.LocalDateTime.now() + " - " + username + ": +PHP " + df.format(amount));
        } catch (IOException e) {
            System.out.println("Error logging interest.");
        }
    }

    public void logVoucher(String username, String code, double value) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(VOUCHER_LOG_FILE, true))) {
            pw.println(java.time.LocalDateTime.now() + " - " + username + " redeemed " + code + " (PHP " + df.format(value) + ")");
        } catch (IOException e) {
            System.out.println("Error logging voucher redemption.");
        }
    }

    public void logSystemRevenue(double fee) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(SYSTEM_REVENUE_FILE, true))) {
            pw.println(java.time.LocalDateTime.now() + " - +PHP " + df.format(fee));
        } catch (IOException e) {
            System.out.println("Error logging system revenue.");
        }
    }

    public double readSystemRevenue() {
        double total = 0.0;
        try (BufferedReader br = new BufferedReader(new FileReader(SYSTEM_REVENUE_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains("PHP")) {
                    String amt = line.substring(line.indexOf("PHP") + 4).replace(",", "");
                    total += Double.parseDouble(amt);
                }
            }
        } catch (Exception ignored) {}
        return total;
    }

    public int getTotalUsersCount() {
        int count = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(USERS_FILE))) {
            while (br.readLine() != null) count++;
        } catch (IOException e) {}
        return count;
    }

    public int getTotalVouchersCount() {
        int count = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(VOUCHERS_FILE))) {
            while (br.readLine() != null) count++;
        } catch (IOException e) {}
        return count;
    }

    public void showTransactions(String username) {
        System.out.println("\n=== TRANSACTION HISTORY ===");
        try (BufferedReader br = new BufferedReader(new FileReader(TRANSACTIONS_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains(username)) System.out.println(line);
            }
        } catch (IOException e) {
            System.out.println("Error reading transactions.");
        }
    }

    public void showUserVouchers(String username) {
        System.out.println("\n=== MY VOUCHERS ===");
        try (BufferedReader br = new BufferedReader(new FileReader(VOUCHERS_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith(username + ",")) System.out.println(line);
            }
        } catch (IOException e) {
            System.out.println("Error reading vouchers.");
        }
    }

    public int countUserVouchers(String username) {
        int count = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(VOUCHERS_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith(username + ",")) count++;
            }
        } catch (IOException e) {}
        return count;
    }

    public void logSchedulerRun() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(SCHEDULER_FILE, true))) {
            pw.println(java.time.LocalDateTime.now() + " - Scheduler executed");
        } catch (IOException e) {
            System.out.println("Error logging scheduler.");
        }
    }

    public String readLastSchedulerRun() {
        String last = "N/A";
        try (BufferedReader br = new BufferedReader(new FileReader(SCHEDULER_FILE))) {
            String line;
            while ((line = br.readLine()) != null) last = line;
        } catch (IOException e) {}
        return last;
    }
}