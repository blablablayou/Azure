package azurewallet.models;

import java.text.DecimalFormat;
import azurewallet.system.FileManager;


public class UserAccount {
    private final String username;
    private final String pinHash;
    private final String mobile;
    private double balance;
    private int points;
    private double totalTransacted;
    private String rank;
    private int failedAttempts;
    private long lockEndTime;
    private static final DecimalFormat df = new DecimalFormat("#,##0.00");

    public UserAccount(String username, String pin, String mobile) {
        this.username = username;
        this.pinHash = HashUtil.hash(pin);
        this.mobile = mobile;
        this.balance = 0.0;
        this.points = 0;
        this.totalTransacted = 0.0;
        this.rank = "Bronze";
        this.failedAttempts = 0;
        this.lockEndTime = 0;
    }

    public UserAccount(String username, String pinHash, String mobile, double balance, int points, double totalTransacted, String rank, int failedAttempts, long lockEndTime) {
        this.username = username;
        this.pinHash = pinHash;
        this.mobile = mobile;
        this.balance = balance;
        this.points = points;
        this.totalTransacted = totalTransacted;
        this.rank = rank;
        this.failedAttempts = failedAttempts;
        this.lockEndTime = lockEndTime;
    }

    public String getUsername() { return username; }
    public String getMobile() { return mobile; }
    public double getBalance() { return balance; }
    public int getPoints() { return points; }
    public double getTotalTransacted() { return totalTransacted; }
    public String getRank() { return rank; }
    public boolean isLocked() { return System.currentTimeMillis() < lockEndTime; }
    public long getLockEndTime() { return lockEndTime; }

    public boolean verifyPin(String input) {
        return this.pinHash.equals(HashUtil.hash(input));
    }

    public void registerFailedAttempt() {
        failedAttempts++;
        if (failedAttempts >= 3) {
            long lockDuration;
            lockDuration = switch (failedAttempts) {
                case 3 -> 60_000;
                case 6 -> 300_000;
                case 9 -> 600_000;
                default -> 1_800_000;
            };
            lockEndTime = System.currentTimeMillis() + lockDuration;
            System.out.println("Too many failed attempts. Account locked temporarily.");
        }
    }

    public void resetLock() {
        failedAttempts = 0;
        lockEndTime = 0;
    }

    public void deposit(double amount) {
        balance += amount;
        updateRank();
    }

    public void withdraw(double amount) {
        balance -= amount;
    }

    public void addTotalTransacted(double amount) {
        totalTransacted += amount;
        updateRank();
    }

    public void addPoints(int pts) {
        this.points += pts;
    }

    public void redeemPoints(int pts, double value) {
        this.points -= pts;
        this.balance += value;
    }

    public void displayBalance() {
        System.out.println("Current Balance: PHP " + df.format(balance));
        System.out.println("Total Points: " + points);
        System.out.println("Rank: " + rank);
    }

    public void updateRank() {
        if (totalTransacted >= 200000 && totalTransacted < 500000) rank = "Silver";
        else if (totalTransacted >= 500000 && totalTransacted < 1000000) rank = "Gold";
        else if (totalTransacted >= 1000000) rank = "Platinum";
        else rank = "Bronze";
    }

    public double getDepositLimit() {
        return switch (rank) {
            case "Silver" -> 150000;
            case "Gold" -> 300000;
            case "Platinum" -> 500000;
            default -> 100000;
        };
    }

    public double getWithdrawLimit() {
        return switch (rank) {
            case "Silver" -> 150000;
            case "Gold" -> 300000;
            case "Platinum" -> 500000;
            default -> 100000;
        };
    }

    public double getSendLimit() {
        return switch (rank) {
            case "Silver" -> 150000;
            case "Gold" -> 300000;
            case "Platinum" -> 500000;
            default -> 100000;
        };
    }

    public void applyMonthlyInterest() {
        double rate = switch (rank) {
            case "Silver" -> 0.0025;
            case "Gold" -> 0.004;
            case "Platinum" -> 0.006;
            default -> 0.0015;
        };
        double interest = balance * rate;
        balance += interest;
        System.out.println("Monthly interest added: PHP " + df.format(interest));
    }

    public void viewVoucherNotification(FileManager fileManager) {
        int vouchers = fileManager.countUserVouchers(username);
        if (vouchers > 0) {
            System.out.println("You have " + vouchers + " available voucher(s).");
        }
    }

    public void viewMyVouchers(FileManager fileManager) {
        fileManager.showUserVouchers(username);
    }

    public String toFileFormat() {
        return username + "," + pinHash + "," + mobile + "," + balance + "," + points + "," + totalTransacted + "," + rank + "," + failedAttempts + "," + lockEndTime;
    }
}