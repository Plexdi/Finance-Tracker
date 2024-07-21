import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.InputMismatchException;
import java.util.Map;
import io.github.cdimascio.dotenv.Dotenv;


public class App {
    public static void main(String[] args) {
        Scanner myObj = new Scanner(System.in);
        Connection conn = null;
        boolean renews = false;
        String[] optionTwo = {"Subscription name", "Monthly Cost", "Auto Renew"};

        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            System.out.println("PostgreSQL JDBC driver not found.");
            e.printStackTrace();
            return;
        }

        String url = "jdbc:postgresql://localhost:5432/postgres";
        String user = "postgres";
        String password = "passwordUnknown";

        try {
            conn = DriverManager.getConnection(url, user, password);
            conn.setAutoCommit(false); // Ensure auto-commit is off for manual commit control
            System.out.println("Connected to PostgreSQL database!");
        } catch (SQLException e) {
            System.out.println("Connection failure.");
            e.printStackTrace();
            return;
        }

        while (true) {
            int index = 1;
            String[] options = {"Add Subscription", "Remove Subscription", "Edit Subscription", "Display All Subscriptions", "Exit"};
            for (String i : options) {
                System.out.println(index + ". " + i);
                index++;
            }

            System.out.print("Enter the task number: ");
            int userOption = 0;
            try {
                userOption = myObj.nextInt();
                myObj.nextLine(); // Consume the newline character
            } catch (InputMismatchException e) {
                System.out.println("Invalid input. Please enter a number.");
                myObj.nextLine(); // Consume the invalid input
                continue; // Prompt the user again
            }

            if (userOption == 1) {
                System.out.print("Enter subscription name: ");
                String Subname = myObj.nextLine();

                System.out.print("Enter monthly cost: ");
                float monthlyCost = 0;
                try {
                    monthlyCost = myObj.nextFloat();
                    myObj.nextLine(); // Consume the newline character
                } catch (InputMismatchException e) {
                    System.out.println("Invalid input. Please enter a valid number.");
                    myObj.nextLine(); // Consume the invalid input
                    continue; // Prompt the user again
                }

                System.out.print("Enter when your subscription start (today or DD/MM/YYYY): ");
                String StartDate = myObj.nextLine().toLowerCase();
                LocalDate date = null;
                if (StartDate.equals("today")) {
                    date = LocalDate.now();
                    System.out.println("Start date: " + date);
                } else {
                    try {
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                        date = LocalDate.parse(StartDate, formatter);
                        System.out.println(date);
                    } catch (DateTimeParseException e) {
                        System.out.println(e);
                        continue;
                    }
                }

                System.out.print("Will your subscriptions renew (yes/no or y/n): ");
                String renew = myObj.nextLine();
                if (renew.equalsIgnoreCase("yes") || renew.equalsIgnoreCase("y")) {
                    renews = true;
                } else {
                    renews = false;
                }
                AddSubscriptions(Subname, monthlyCost, date, renews, conn);
            } else if (userOption == 2) {
                while (true) {
                    System.out.println("Enter your subscription name: ");
                    String subscriptionName = myObj.nextLine();
                    boolean sub = checkSubscriptions(subscriptionName, conn);
                    if (sub) {
                        removeSubscriptions(subscriptionName, conn);
                        break;
                    } else {
                        System.err.println("Subscription does not exist.");
                    }
                }
            } else if (userOption == 3) {
                while (true) {
                    System.out.println("Enter the subscription name you want to modify: ");
                    String subName = myObj.nextLine();
                    boolean checkSub = checkSubscriptions(subName, conn);
                    if (checkSub) {
                        int x = 1; // Reset x to 1 for displaying options correctly
                        for (String i : optionTwo) {
                            System.out.println(x + ": " + i);
                            x++;
                        }
                        System.out.print("Enter the integer of the value you want to modify: ");
                        int modifyNums = 0;
                        try {
                            modifyNums = myObj.nextInt();
                            myObj.nextLine(); // Consume the newline character
                        } catch (InputMismatchException e) {
                            System.err.println("Invalid input. Please enter a number.");
                            myObj.nextLine(); // Consume the invalid input
                            continue; // Prompt the user again
                        }

                        Map<String, Object> params = new HashMap<>();
                        params.put("subname", subName);

                        switch (modifyNums) {
                            case 1:
                                System.out.print("Enter new subscription name: ");
                                String newSubName = myObj.nextLine();
                                params.put("newSubname", newSubName); // Ensure the correct key name
                                modifications("name", params, conn);
                                break;

                            case 2:
                                System.out.print("Enter your subscription new monthly cost: ");
                                float newMonthlyCost = 0;
                                try {
                                    newMonthlyCost = myObj.nextFloat();
                                    myObj.nextLine(); // Consume the newline character
                                } catch (InputMismatchException e) {
                                    System.err.println("Please enter a valid number");
                                    myObj.nextLine(); // Consume invalid input
                                    continue; // Restart the loop
                                }
                                params.put("newMonthlyCost", newMonthlyCost); // Ensure the correct key name
                                modifications("cost", params, conn);
                                break;

                            case 3:
                                System.out.print("Will your subscription renew? (Yes/No) or (Y/N): ");
                                String renewal = myObj.nextLine();

                                if (renewal.equalsIgnoreCase("yes") || renewal.equalsIgnoreCase("y")) {
                                    params.put("renew", true);
                                } else {
                                    params.put("renew", false);
                                }
                                modifications("renew", params, conn);
                                break;

                            default:
                                System.err.println("Invalid option. Please try again.");
                                break;
                        }

                        break; // Exit the while loop after successful modification
                    } else {
                        System.err.println(subName + " does not exist in your subscriptions.");
                    }
                }
            } else if (userOption == 4) {
                displayAllSubscriptions(conn);
            } else if (userOption == 5) {
                System.out.println("Exiting...");
                break;
            } else {
                System.out.println("Invalid option. Please try again.");
            }
        }

        try {
            if (conn != null) {
                conn.close();
            }
            myObj.close();
        } catch (SQLException e) {
            System.out.println("Error closing the connection.");
            e.printStackTrace();
        }
    }

    static void AddSubscriptions(String Subname, float monthlyCost, LocalDate date, boolean renews, Connection conn) {
        String sql = "INSERT INTO subscriptions (service_name, monthly_cost, start_date, auto_renew) VALUES(?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, Subname);
            pstmt.setFloat(2, monthlyCost);
            pstmt.setDate(3, java.sql.Date.valueOf(date));
            pstmt.setBoolean(4, renews);

            int rowsInserted = pstmt.executeUpdate();
            if (rowsInserted > 0) {
                System.out.println("Saved Successfully");
                conn.commit();
            } else {
                System.err.println("No rows were inserted.");
                conn.rollback();
            }
        } catch (SQLException e) {
            System.out.println("SQL error: " + e.getMessage());
            try {
                if (conn != null && !conn.getAutoCommit()) {
                    conn.rollback(); // Rollback in case of an error
                }
            } catch (SQLException rollbackEx) {
                System.out.println("Rollback error: " + rollbackEx.getMessage());
            }
        }
    }

    static boolean checkSubscriptions(String subname, Connection conn) {
        String sql = "SELECT 1 FROM subscriptions WHERE service_name = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, subname);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.out.println("SQL error: " + e.getMessage());
        }
        return false;
    }

    static void removeSubscriptions(String Subname, Connection conn) {
        String sql = "DELETE FROM subscriptions WHERE service_name = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, Subname);

            int rowsDeleted = pstmt.executeUpdate();
            if (rowsDeleted > 0) {
                System.out.println("Subscription deleted successfully");
                conn.commit();
            } else {
                System.out.println("No rows were deleted");
                conn.rollback();
            }
        } catch (SQLException e) {
            System.err.println("SQL error: " + e.getMessage());
            try {
                if (conn != null && !conn.getAutoCommit()) {
                    conn.rollback(); // Rollback in case of an error
                }
            } catch (SQLException rollbackEx) {
                System.out.println("Rollback error: " + rollbackEx.getMessage());
            }
        }
    }

    static void modifications(String modificationType, Map<String, Object> params, Connection conn) {
        String sql = null;
        switch (modificationType) {
            case "name":
                sql = "UPDATE subscriptions SET service_name = ? WHERE service_name = ?";
                break;
            case "cost":
                sql = "UPDATE subscriptions SET monthly_cost = ? WHERE service_name = ?";
                break;
            case "renew":
                sql = "UPDATE subscriptions SET auto_renew = ? WHERE service_name = ?";
                break;
            default:
                System.out.println("Invalid modification type");
                return;
        }

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            switch (modificationType) {
                case "name":
                    String newSubname = (String) params.get("newSubname");
                    String subname = (String) params.get("subname");
                    pstmt.setString(1, newSubname);
                    pstmt.setString(2, subname);
                    break;
                case "cost":
                    pstmt.setFloat(1, (Float) params.get("newMonthlyCost"));
                    pstmt.setString(2, (String) params.get("subname"));
                    break;
                case "renew":
                    pstmt.setBoolean(1, (Boolean) params.get("renew"));
                    pstmt.setString(2, (String) params.get("subname"));
                    break;
            }

            int rowsUpdated = pstmt.executeUpdate();
            if (rowsUpdated > 0) {
                System.out.println("Successfully updated subscription.");
                conn.commit(); // Commit the transaction
            } else {
                System.out.println("No subscription found with the name: " + params.get("subname"));
                conn.rollback();
            }
        } catch (SQLException e) {
            System.out.println("SQL error: " + e.getMessage());
            try {
                if (conn != null && !conn.getAutoCommit()) {
                    conn.rollback(); // Rollback in case of an error
                }
            } catch (SQLException rollbackEx) {
                System.out.println("Rollback error: " + rollbackEx.getMessage());
            }
        }
    }

    static void displayAllSubscriptions(Connection conn) {
        String sql = "SELECT * FROM subscriptions";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            try (ResultSet rs = pstmt.executeQuery()) {
                System.out.println("----------------------------------------------------------");
                System.out.printf("%-20s %-15s %-15s %-15s\n", "Subscription Name", "Monthly Cost", "Start Date", "Auto Renew");
                System.out.println("----------------------------------------------------------");
                while (rs.next()) {
                    String subname = rs.getString("service_name");
                    float monthlyCost = rs.getFloat("monthly_cost");
                    LocalDate startDate = rs.getDate("start_date").toLocalDate();
                    boolean autoRenew = rs.getBoolean("auto_renew");
                    System.out.printf("%-20s %-15.2f %-15s %-15s\n", subname, monthlyCost, startDate, autoRenew ? "Yes" : "No");
                }
                System.out.println("----------------------------------------------------------");
            }
        } catch (SQLException e) {
            System.out.println("SQL error: " + e.getMessage());
        }
    }
}