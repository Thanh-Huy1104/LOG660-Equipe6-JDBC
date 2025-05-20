package creator;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MainCreator {

    private static final String DB_URL = "jdbc:oracle:thin:@//bdlog660.ens.ad.etsmtl.ca:1521/ORCLPDB.ens.ad.etsmtl.ca";
    private static final String DB_USER = "EQUIPE206";
    private static final String DB_PASSWORD = "NulxJFxU";

    public static void main(String[] args) {
        Connection conn = null;
        try {
            try {
                Class.forName("oracle.jdbc.driver.OracleDriver");
                System.out.println("Oracle JDBC Driver Registered!");
            } catch (ClassNotFoundException e) {
                System.err.println("Oracle JDBC Driver not found. Make sure ojdbcX.jar is in your classpath.");
                e.printStackTrace();
                return;
            }

            // 2. Establish the database connection
            System.out.println("Attempting to connect to database...");
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            System.out.println("Database connection established successfully!");

            // Set auto-commit to false to manage transactions manually
            conn.setAutoCommit(false);

            // 3. Create an instance of TableCreator
            TableCreator tableCreator = new TableCreator(conn);

            // 4. Reset and create tables
            System.out.println("Starting table reset and creation process...");
            tableCreator.resetAndCreateTables();
            // The commit is handled within resetAndCreateTables after all operations succeed.

            System.out.println("Main process completed successfully.");

        } catch (SQLException e) {
            System.err.println("SQL Error occurred during the process: " + e.getMessage());
            e.printStackTrace();
            if (conn != null) {
                try {
                    System.err.println("Transaction is being rolled back.");
                    conn.rollback(); // Rollback on error
                } catch (SQLException ex) {
                    System.err.println("Error during rollback: " + ex.getMessage());
                    ex.printStackTrace();
                }
            }
        } catch (Exception e) {
            System.err.println("An unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
            if (conn != null) {
                try {
                    System.err.println("Transaction is being rolled back due to unexpected error.");
                    conn.rollback();
                } catch (SQLException ex) {
                    System.err.println("Error during rollback: " + ex.getMessage());
                }
            }
        } finally {
            // 5. Close the connection
            if (conn != null) {
                try {
                    conn.setAutoCommit(true); // Reset auto-commit before closing
                    conn.close();
                    System.out.println("Database connection closed.");
                } catch (SQLException e) {
                    System.err.println("Error closing database connection: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }
}
