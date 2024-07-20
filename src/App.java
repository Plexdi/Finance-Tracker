import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class App {
public static void main(String[] args) {
    Scanner myObj = new Scanner(System.in);
    Connection conn = null;
    boolean renews = false;
    try {
        // Explicitly load the PostgreSQL JDBC driver
        Class.forName("org.postgresql.Driver");
    } catch (ClassNotFoundException e) {
        System.out.println("PostgreSQL JDBC driver not found.");
        e.printStackTrace();
        return;
    }

    String url = "jdbc:postgresql://localhost:5432/postgres";
    String user = "postgres";
    String password = "Thanh2007@Plexdi";

    try {
        conn = DriverManager.getConnection(url, user, password);
        System.out.println("Connected to PostgreSQL database!");
    } catch (SQLException e) {
        System.out.println("Connection failure.");
        e.printStackTrace();
        return;
    }

    while ( true ){
        int index = 1;
        String[] options = {"Add Subscription", "Remove Subscription", "Edit Subscription" };
        for (String i : options){
            System.out.println(index + ". " + i);
            index++;
        }
        
        System.out.print("Enter the task number: ");
        int userOption = myObj.nextInt();
        myObj.nextLine();

        if (userOption == 1){
            System.out.print("Enter subscription name: ");
            String Subname = myObj.nextLine();

            System.out.print("Enter monthly cost: ");
            float monthlyCost = myObj.nextFloat();
            myObj.nextLine(); 

            
            System.out.print("Enter when your subscription start (today or DD//MM/YYYY): ");
            String StartDate = myObj.nextLine().toLowerCase();
            LocalDate date = null;
            if (StartDate.equals("today")) {
                date = LocalDate.now();
                System.out.println("Start date: " + date);
            } else {
                try{
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                    date = LocalDate.parse(StartDate, formatter);
                    System.out.println(date);
                } catch ( DateTimeParseException e ) {
                    System.out.println(e);
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
        } else if ( userOption == 2 ){ 
            while (true) {
                System.out.println("Enter your subscription name: ");
                String subscriptionName = myObj.nextLine();
                boolean sub = checkSubscriptions(subscriptionName, conn);
                if ( sub == true ) {
                    removeSubscriptions(subscriptionName, conn);
                    break;
                } else {
                    System.err.println("subscription does not exists");
                    continue;
                }
            }
        } else if ( userOption == 3 ){ 
            System.out.println("You chose number three ");
        } else if ( userOption == 4 ){ 
            System.out.println("You chose number four ");
        }
    }   
}
static void AddSubscriptions( String Subnames, float monthlyCost, LocalDate date, boolean renews, Connection conn ) {
    String sql = "INSERT INTO subscriptions (service_name, monthly_cost, start_date, auto_renew) VALUES(?, ?, ?, ?)";
    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setString(1, Subnames);
        pstmt.setFloat(2, monthlyCost);
        pstmt.setDate(3, java.sql.Date.valueOf(date));
        pstmt.setBoolean((4), renews);

        int rowsInserted = pstmt.executeUpdate();
        if ( rowsInserted > 0 ){ 
            System.out.println("Saved Successfully");

        } else {
            System.err.println("No rows were inserted.");
        }
    } catch ( SQLException e ) {
        System.out.println(e.getMessage());
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

static void removeSubscriptions( String Subnames, Connection conn) {
        String sql = "DELETE FROM subscriptions WHERE service_name = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, Subnames);
    
            int rowDeleted = pstmt.executeUpdate();
            if (rowDeleted > 0){
                System.out.println("Subscription deleted successfully");
            } else {
                System.out.println("No rows was deleted");
            }
        } catch ( SQLException e ) {
            System.err.println(e);
        }
    }
}
