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
        System.out.println("+==========================================================+");
        System.out.println("|                AZURE DIGITAL WALLET STARTUP              |");
        System.out.println("+==========================================================+");
        System.out.printf("| Users found:                %-28d |\n", fileManager.getTotalUsersCount());
        System.out.printf("| Active vouchers:            %-28d |\n", fileManager.getTotalVouchersCount());
        System.out.printf("| Total system revenue:       PHP %-20.2f     |\n", fileManager.readSystemRevenue());
        System.out.println("+==========================================================+\n");
    }

    public void start() {
        while (true) {
            System.out.println("+==========================================================+");
            System.out.println("|                   AZURE DIGITAL WALLET                   |");
            System.out.println("+==========================================================+");
            System.out.println("| [1] Register        [2] Login        [3] Admin Login     |");
            System.out.println("| [0] Exit                                                 |");
            System.out.println("+----------------------------------------------------------+");
            System.out.print("| Choose: ");
            String choice = sc.nextLine();
            System.out.println("+----------------------------------------------------------+");

            switch (choice) {
                case "1" -> register();
                case "2" -> login();
                case "3" -> adminLogin();
                case "0" -> {
                    fileManager.saveUsers(users);
                    System.out.println("| Exiting system...                                       |");
                    System.out.println("+==========================================================+");
                    return;
                }
                default -> {
                    System.out.println("| Invalid option.                                         |");
                    System.out.println("+==========================================================+");
                }
            }
        }
    }

    private void register() {
        System.out.println("+==========================================================+");
        System.out.println("|                   USER REGISTRATION                      |");
        System.out.println("+==========================================================+");
        System.out.print("| Enter username (0/B to go back): ");
        String username = sc.nextLine().trim().toLowerCase();
        if (username.equals("0") || username.equalsIgnoreCase("B")) return;

        if (users.containsKey(username)) {
            System.out.println("| Username already exists.                                |");
            System.out.println("+==========================================================+");
            return;
        }

        System.out.print("| Enter mobile number (0/B to go back): ");
        String mobile = sc.nextLine().trim();
        if (mobile.equals("0") || mobile.equalsIgnoreCase("B")) return;

        System.out.print("| Enter 4-digit PIN (0/B to go back): ");
        String pin = sc.nextLine().trim();
        if (pin.equals("0") || pin.equalsIgnoreCase("B")) return;

        if (pin.length() != 4) {
            System.out.println("| PIN must be 4 digits.                                   |");
            System.out.println("+==========================================================+");
            return;
        }

        UserAccount newUser = new UserAccount(username, pin, mobile);
        users.put(username, newUser);
        fileManager.saveUsers(users);
        System.out.println("| Registration successful.                                |");
        System.out.println("+==========================================================+");
    }

    private void login() {
        System.out.println("+==========================================================+");
        System.out.println("|                         LOGIN PAGE                       |");
        System.out.println("+==========================================================+");
        System.out.print("| Username (0/B to go back): ");
        String username = sc.nextLine().trim().toLowerCase();
        if (username.equals("0") || username.equalsIgnoreCase("B")) return;

        if (!users.containsKey(username)) {
            System.out.println("| User not found.                                         |");
            System.out.println("+==========================================================+");
            return;
        }

        UserAccount acc = users.get(username);
        if (acc.isLocked()) {
            long minsLeft = (acc.getLockEndTime() - System.currentTimeMillis()) / 60000;
            System.out.printf("| Account is locked. Try again in %-4d minute(s).          |\n", Math.max(minsLeft, 1));
            System.out.println("+==========================================================+");
            return;
        }

        System.out.print("| Enter PIN (0/B to go back): ");
        String pin = sc.nextLine().trim();
        if (pin.equals("0") || pin.equalsIgnoreCase("B")) return;

        if (!acc.verifyPin(pin)) {
            acc.registerFailedAttempt();
            fileManager.saveUsers(users);
            System.out.println("| Incorrect PIN.                                          |");
            System.out.println("+==========================================================+");
            return;
        }

        acc.resetLock();
        fileManager.saveUsers(users);
        acc.viewVoucherNotification(fileManager);
        userMenu(acc);
    }

    private void userMenu(UserAccount acc) {
        while (true) {
            System.out.println("+==========================================================+");
            System.out.println("|                        WALLET MENU                       |");
            System.out.println("+==========================================================+");
            System.out.println("| [1] Deposit             [6] Send Money to User           |");
            System.out.println("| [2] Withdraw            [7] View Balance                 |");
            System.out.println("| [3] Pay Online          [8] View Transactions            |");
            System.out.println("| [4] Redeem Voucher      [9] My Vouchers                  |");
            System.out.println("| [5] Redeem Points       [10] Logout                      |");
            System.out.println("+----------------------------------------------------------+");
            System.out.print("| Choose: ");
            String ch = sc.nextLine();
            System.out.println("+----------------------------------------------------------+");

            switch (ch) {
                case "1" -> deposit(acc);
                case "2" -> withdraw(acc);
                case "3" -> payOnline(acc);
                case "4" -> redeemVoucher(acc);
                case "5" -> redeemPoints(acc);
                case "6" -> sendToUser(acc);
                case "7" -> acc.displayBalance();
                case "8" -> fileManager.showTransactions(acc.getUsername());
                case "9" -> acc.viewMyVouchers(fileManager);
                case "10" -> {
                    fileManager.saveUsers(users);
                    System.out.println("| Logged out successfully.                                 |");
                    System.out.println("+==========================================================+");
                    return;
                }
                default -> {
                    System.out.println("| Invalid choice.                                          |");
                    System.out.println("+==========================================================+");
                }
            }
        }
    }

    private void deposit(UserAccount acc) {
        System.out.println("+==========================================================+");
        System.out.println("|                        DEPOSIT FUNDS                     |");
        System.out.println("+==========================================================+");
        System.out.print("| Enter amount (0/B to go back): ");
        String input = sc.nextLine();
        if (input.equals("0") || input.equalsIgnoreCase("B")) return;
        double amount = Double.parseDouble(input);

        if (amount <= 0 || amount > acc.getDepositLimit()) {
            System.out.println("| Invalid or exceeds limit (" + df.format(acc.getDepositLimit()) + ")");
            System.out.println("+==========================================================+");
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
        System.out.println("| Deposit successful. Balance: PHP " + df.format(acc.getBalance()) + " |");
        System.out.println("+==========================================================+");
    }

    private void withdraw(UserAccount acc) {
        System.out.println("+==========================================================+");
        System.out.println("|                        WITHDRAW FUNDS                    |");
        System.out.println("+==========================================================+");
        System.out.print("| Enter amount (0/B to go back): ");
        String input = sc.nextLine();
        if (input.equals("0") || input.equalsIgnoreCase("B")) return;
        double amount = Double.parseDouble(input);

        if (amount <= 0 || amount > acc.getWithdrawLimit()) {
            System.out.println("| Invalid or exceeds limit (" + df.format(acc.getWithdrawLimit()) + ")");
            System.out.println("+==========================================================+");
            return;
        }
        double totalAmount = amount + WITHDRAW_FEE;
        if (totalAmount > acc.getBalance()) {
            System.out.println("| Insufficient balance including PHP 15.00 fee.            |");
            System.out.println("+==========================================================+");
            return;
        }
        acc.withdraw(totalAmount);
        fileManager.logTransaction(acc.getUsername(), "Withdraw", amount);
        fileManager.logSystemRevenue(WITHDRAW_FEE);
        fileManager.saveUsers(users);
        System.out.println("| Withdraw successful. Fee applied. New balance: PHP " + df.format(acc.getBalance()) + " |");
        System.out.println("+==========================================================+");
    }

    private void payOnline(UserAccount acc) {
        System.out.println("+==========================================================+");
        System.out.println("|                        PAY ONLINE                        |");
        System.out.println("+==========================================================+");
        System.out.print("| Enter merchant name (0/B to go back): ");
        String merchant = sc.nextLine();
        if (merchant.equals("0") || merchant.equalsIgnoreCase("B")) return;
        System.out.print("| Enter amount (0/B to go back): ");
        String input = sc.nextLine();
        if (input.equals("0") || input.equalsIgnoreCase("B")) return;
        double amount = Double.parseDouble(input);

        if (amount <= 0 || amount > acc.getSendLimit()) {
            System.out.println("| Invalid or exceeds limit (" + df.format(acc.getSendLimit()) + ")");
            System.out.println("+==========================================================+");
            return;
        }
        if (amount > acc.getBalance()) {
            System.out.println("| Insufficient balance.                                   |");
            System.out.println("+==========================================================+");
            return;
        }
        acc.withdraw(amount);
        fileManager.logTransaction(acc.getUsername(), "Paid to " + merchant, amount);
        fileManager.saveUsers(users);
        System.out.println("| Payment successful. PHP " + df.format(amount) + " sent to " + merchant + " |");
        System.out.println("+==========================================================+");
    }

    private void sendToUser(UserAccount acc) {
        System.out.println("+==========================================================+");
        System.out.println("|                      SEND MONEY TO USER                  |");
        System.out.println("+==========================================================+");
        System.out.print("| Enter recipient username (0/B to go back): ");
        String recipient = sc.nextLine().trim().toLowerCase();
        if (recipient.equals("0") || recipient.equalsIgnoreCase("B")) return;

        if (!users.containsKey(recipient)) {
            System.out.println("| Recipient not found.                                    |");
            System.out.println("+==========================================================+");
            return;
        }
        if (recipient.equals(acc.getUsername())) {
            System.out.println("| You cannot send money to yourself.                      |");
            System.out.println("+==========================================================+");
            return;
        }

        System.out.print("| Enter amount (0/B to go back): ");
        String input = sc.nextLine();
        if (input.equals("0") || input.equalsIgnoreCase("B")) return;
        double amount = Double.parseDouble(input);

        if (amount <= 0 || amount > acc.getSendLimit()) {
            System.out.println("| Invalid or exceeds limit (" + df.format(acc.getSendLimit()) + ")");
            System.out.println("+==========================================================+");
            return;
        }
        if (amount > acc.getBalance()) {
            System.out.println("| Insufficient balance.                                   |");
            System.out.println("+==========================================================+");
            return;
        }

        acc.withdraw(amount);
        users.get(recipient).deposit(amount);
        fileManager.logTransaction(acc.getUsername(), "Sent to " + recipient, amount);
        fileManager.logTransaction(recipient, "Received from " + acc.getUsername(), amount);
        fileManager.saveUsers(users);

        System.out.println("| Successfully sent PHP " + df.format(amount) + " to " + recipient + " |");
        System.out.println("+==========================================================+");
    }

    private void redeemVoucher(UserAccount acc) {
        System.out.println("+==========================================================+");
        System.out.println("|                       REDEEM VOUCHER                    |");
        System.out.println("+==========================================================+");
        System.out.print("| Enter voucher code (0/B to go back): ");
        String code = sc.nextLine().trim();
        if (code.equals("0") || code.equalsIgnoreCase("B")) return;
        double value = VoucherSystem.redeemVoucher(acc, code, fileManager);
        if (value > 0) {
            fileManager.saveUsers(users);
            System.out.println("| Voucher redeemed successfully! +PHP " + df.format(value) + " |");
        } else {
            System.out.println("| Invalid or expired voucher.                             |");
        }
        System.out.println("+==========================================================+");
    }

    private void redeemPoints(UserAccount acc) {
        System.out.println("+==========================================================+");
        System.out.println("|                      REDEEM POINTS                       |");
        System.out.println("+==========================================================+");
        System.out.print("| Enter points to redeem (0/B to go back): ");
        String input = sc.nextLine();
        if (input.equals("0") || input.equalsIgnoreCase("B")) return;
        int pts = Integer.parseInt(input);
        if (pts <= 0 || pts > acc.getPoints()) {
            System.out.println("| Invalid points.                                         |");
            System.out.println("+==========================================================+");
            return;
        }
        double value = pts * 1.0;
        acc.redeemPoints(pts, value);
        fileManager.logPoints(acc.getUsername(), "redeemed", pts, "converted to PHP " + df.format(value));
        fileManager.saveUsers(users);
        System.out.println("| Redeemed " + pts + " points = PHP " + df.format(value) + " |");
        System.out.println("+==========================================================+");
    }

    private void adminLogin() {
        AdminControl admin = new AdminControl(fileManager, users, scheduler);
        admin.menu(sc);
    }
}