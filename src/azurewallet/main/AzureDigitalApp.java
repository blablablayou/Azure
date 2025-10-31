package azurewallet.main;

import azurewallet.models.UserAccount;
import azurewallet.models.VoucherSystem;
import azurewallet.system.FileManager;
import azurewallet.system.AdminControl;
import java.util.*;
import java.text.DecimalFormat;

public class AzureDigitalApp {
    private final FileManager fileManager;
    private final Map<String, UserAccount> users;
    private final BackgroundScheduler scheduler;
    private final Scanner sc = new Scanner(System.in);
    private final DecimalFormat df = new DecimalFormat("#,##0.00");
    private static final double WITHDRAW_FEE = 15.0;

    public AzureDigitalApp() {
        fileManager = new FileManager();
        users = fileManager.loadUsers();
        scheduler = new BackgroundScheduler(fileManager, users);
        scheduler.runScheduler();
        showStartupSummary();
    }

    private void showStartupSummary() {
        System.out.println("\n=== Azure Digital Wallet Startup Summary ===");
        System.out.println("Users found: " + fileManager.getTotalUsersCount());
        System.out.println("Active vouchers: " + fileManager.getTotalVouchersCount());
        System.out.println("Last scheduler run: " + fileManager.readLastSchedulerRun());
        System.out.println("Total system revenue: PHP " + fileManager.readSystemRevenue());
        System.out.println("============================================\n");
    }

    public void start() {
        while (true) {
            System.out.println("=== AZURE DIGITAL WALLET ===");
            System.out.println("[1] Register");
            System.out.println("[2] Login");
            System.out.println("[3] Admin Login");
            System.out.println("[0] Exit");
            System.out.print("Choose: ");
            String choice = sc.nextLine();

            switch (choice) {
                case "1" -> register();
                case "2" -> login();
                case "3" -> adminLogin();
                case "0" -> {
                    fileManager.saveUsers(users);
                    System.out.println("Exiting system...");
                    return;
                }
                default -> System.out.println("Invalid option.");
            }
        }
    }

    private void register() {
        System.out.println("\n--- USER REGISTRATION ---");
        System.out.print("Enter username (or B to go back): ");
        String username = sc.nextLine().trim().toLowerCase();
        if (username.equalsIgnoreCase("b") || username.equals("0")) return;
        if (users.containsKey(username)) {
            System.out.println("Username already exists.");
            return;
        }

        System.out.print("Enter mobile number (or B to go back): ");
        String mobile = sc.nextLine().trim();
        if (mobile.equalsIgnoreCase("b") || mobile.equals("0")) return;

        System.out.print("Enter 4-digit PIN (or B to go back): ");
        String pin = sc.nextLine().trim();
        if (pin.equalsIgnoreCase("b") || pin.equals("0")) return;
        if (pin.length() != 4) {
            System.out.println("PIN must be 4 digits.");
            return;
        }

        UserAccount newUser = new UserAccount(username, pin, mobile);
        users.put(username, newUser);
        fileManager.saveUsers(users);
        System.out.println("Registration successful.");
    }

    private void login() {
        System.out.println("\n--- LOGIN ---");
        System.out.print("Username (or B to go back): ");
        String username = sc.nextLine().trim().toLowerCase();
        if (username.equalsIgnoreCase("b") || username.equals("0")) return;
        if (!users.containsKey(username)) {
            System.out.println("User not found.");
            return;
        }

        UserAccount acc = users.get(username);
        if (acc.isLocked()) {
            long minsLeft = (acc.getLockEndTime() - System.currentTimeMillis()) / 60000;
            System.out.println("Account is locked. Try again in " + Math.max(minsLeft, 1) + " minute(s).");
            return;
        }

        System.out.print("Enter PIN (or B to go back): ");
        String pin = sc.nextLine().trim();
        if (pin.equalsIgnoreCase("b") || pin.equals("0")) return;
        if (!acc.verifyPin(pin)) {
            acc.registerFailedAttempt();
            fileManager.saveUsers(users);
            System.out.println("Incorrect PIN.");
            return;
        }

        acc.resetLock();
        fileManager.saveUsers(users);
        acc.viewVoucherNotification(fileManager);
        userMenu(acc);
    }

    private void userMenu(UserAccount acc) {
        while (true) {
            System.out.println("\n=== WALLET MENU ===");
            System.out.println("[1] Deposit");
            System.out.println("[2] Withdraw");
            System.out.println("[3] Pay Online");
            System.out.println("[4] Send Money to User");
            System.out.println("[5] Redeem Voucher");
            System.out.println("[6] Redeem Points");
            System.out.println("[7] View Balance");
            System.out.println("[8] View Transactions");
            System.out.println("[9] View My Vouchers");
            System.out.println("[10] Logout");
            System.out.print("Choose: ");
            String ch = sc.nextLine();

            switch (ch) {
                case "1" -> deposit(acc);
                case "2" -> withdraw(acc);
                case "3" -> payOnline(acc);
                case "4" -> sendToUser(acc);
                case "5" -> redeemVoucher(acc);
                case "6" -> redeemPoints(acc);
                case "7" -> acc.displayBalance();
                case "8" -> fileManager.showTransactions(acc.getUsername());
                case "9" -> acc.viewMyVouchers(fileManager);
                case "10" -> {
                    fileManager.saveUsers(users);
                    System.out.println("Logged out successfully.");
                    return;
                }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    private void deposit(UserAccount acc) {
        System.out.print("Enter amount to deposit (0/B to go back): ");
        String input = sc.nextLine().trim();
        if (input.equalsIgnoreCase("b") || input.equals("0")) return;
        double amount = Double.parseDouble(input);

        if (amount <= 0 || amount > acc.getDepositLimit()) {
            System.out.println("Invalid or exceeds limit (" + df.format(acc.getDepositLimit()) + ")");
            return;
        }
        acc.deposit(amount);
        acc.addTotalTransacted(amount);
        fileManager.logTransaction(acc.getUsername(), "Deposit", amount);
        int pointsEarned = (int) (amount / 1000);
        if (pointsEarned > 0) {
            acc.addPoints(pointsEarned);
            fileManager.logPoints(acc.getUsername(), "earned", pointsEarned, "from deposit");
        }
        fileManager.saveUsers(users);
        System.out.println("Deposit successful. Balance: PHP " + df.format(acc.getBalance()));
    }

    private void withdraw(UserAccount acc) {
        System.out.print("Enter amount to withdraw (0/B to go back): ");
        String input = sc.nextLine().trim();
        if (input.equalsIgnoreCase("b") || input.equals("0")) return;
        double amount = Double.parseDouble(input);

        if (amount <= 0 || amount > acc.getWithdrawLimit()) {
            System.out.println("Invalid or exceeds limit (" + df.format(acc.getWithdrawLimit()) + ")");
            return;
        }
        double totalAmount = amount + WITHDRAW_FEE;
        if (totalAmount > acc.getBalance()) {
            System.out.println("Insufficient balance including fee of PHP 15.00.");
            return;
        }
        acc.withdraw(totalAmount);
        fileManager.logTransaction(acc.getUsername(), "Withdraw", amount);
        fileManager.logSystemRevenue(WITHDRAW_FEE);
        fileManager.saveUsers(users);
        System.out.println("Withdraw successful. PHP 15.00 fee applied. New balance: PHP " + df.format(acc.getBalance()));
    }

    private void sendToUser(UserAccount sender) {
        System.out.print("Enter recipient username (or B to go back): ");
        String recipient = sc.nextLine().trim().toLowerCase();
        if (recipient.equalsIgnoreCase("b") || recipient.equals("0")) return;

        if (!users.containsKey(recipient)) {
            System.out.println("User not found.");
            return;
        }

        if (recipient.equals(sender.getUsername())) {
            System.out.println("You cannot send money to yourself.");
            return;
        }

        System.out.print("Enter amount to send (0/B to go back): ");
        String input = sc.nextLine().trim();
        if (input.equalsIgnoreCase("b") || input.equals("0")) return;
        double amount = Double.parseDouble(input);

        if (amount <= 0 || amount > sender.getSendLimit()) {
            System.out.println("Invalid or exceeds limit (" + df.format(sender.getSendLimit()) + ")");
            return;
        }

        if (amount > sender.getBalance()) {
            System.out.println("Insufficient balance.");
            return;
        }

        UserAccount receiver = users.get(recipient);
        sender.withdraw(amount);
        receiver.deposit(amount);
        fileManager.logTransaction(sender.getUsername(), "Sent to " + recipient, amount);
        fileManager.logTransaction(recipient, "Received from " + sender.getUsername(), amount);
        fileManager.saveUsers(users);
        System.out.println("Successfully sent PHP " + df.format(amount) + " to " + recipient);
    }

    private void payOnline(UserAccount acc) {
        System.out.println("\n--- PAY ONLINE ---");
        System.out.print("Enter merchant name (or B to go back): ");
        String merchant = sc.nextLine();
        if (merchant.equalsIgnoreCase("b") || merchant.equals("0")) return;

        System.out.print("Enter amount (or B to go back): ");
        String input = sc.nextLine().trim();
        if (input.equalsIgnoreCase("b") || input.equals("0")) return;
        double amount = Double.parseDouble(input);

        if (amount <= 0 || amount > acc.getSendLimit()) {
            System.out.println("Invalid or exceeds limit (" + df.format(acc.getSendLimit()) + ")");
            return;
        }
        if (amount > acc.getBalance()) {
            System.out.println("Insufficient balance.");
            return;
        }
        acc.withdraw(amount);
        fileManager.logTransaction(acc.getUsername(), "Paid to " + merchant, amount);
        fileManager.saveUsers(users);
        System.out.println("Payment of PHP " + df.format(amount) + " to " + merchant + " successful.");
    }

    private void redeemVoucher(UserAccount acc) {
        System.out.print("Enter voucher code (or B to go back): ");
        String code = sc.nextLine().trim();
        if (code.equalsIgnoreCase("b") || code.equals("0")) return;
        double value = VoucherSystem.redeemVoucher(acc, code, fileManager);
        if (value > 0) {
            fileManager.saveUsers(users);
            System.out.println("Voucher redeemed successfully! +PHP " + df.format(value));
        } else {
            System.out.println("Invalid or expired voucher.");
        }
    }

    private void redeemPoints(UserAccount acc) {
        System.out.print("Enter points to redeem (0/B to go back): ");
        String input = sc.nextLine().trim();
        if (input.equalsIgnoreCase("b") || input.equals("0")) return;
        int pts = Integer.parseInt(input);
        if (pts <= 0 || pts > acc.getPoints()) {
            System.out.println("Invalid points.");
            return;
        }
        double value = pts * 1.0;
        acc.redeemPoints(pts, value);
        fileManager.logPoints(acc.getUsername(), "redeemed", pts, "converted to PHP " + df.format(value));
        fileManager.saveUsers(users);
        System.out.println("Redeemed " + pts + " points = PHP " + df.format(value));
    }

    private void adminLogin() {
        AdminControl admin = new AdminControl(fileManager, users, scheduler);
        admin.menu(sc);
    }
}