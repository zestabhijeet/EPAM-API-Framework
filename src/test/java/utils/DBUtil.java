package utils;

import files.ConfigManager;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;


public class DBUtil {

    static {
        // load properties from test resources
        ConfigManager.load(Paths.get("src/test/resources/config.properties"));
    }

    public static Connection getConnection() {
        try {
            Class.forName(ConfigManager.getProperty("db.driver"));
            return DriverManager.getConnection(
                    ConfigManager.getProperty("db.url"),
                    ConfigManager.getProperty("db.user"),
                    ConfigManager.getProperty("db.password")
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to connect H2 DB", e);
        }
    }

    // 🔹 Create Schema (Updated to match userapi.json)
    public static void createSchema() {

        // Drop existing tables to clear stale schema
        String dropOrders = "DROP TABLE IF EXISTS ORDERS;";
        String dropItems = "DROP TABLE IF EXISTS ITEMS;";
        String dropUsers = "DROP TABLE IF EXISTS USERS;";

        // Updated USERS table schema per userapi.json requirements
        String users = """
                CREATE TABLE USERS (
                    ID INT PRIMARY KEY,
                    EMAIL VARCHAR(150) NOT NULL,
                    FIRST_NAME VARCHAR(100) NOT NULL,
                    LAST_NAME VARCHAR(100) NOT NULL,
                    AVATAR VARCHAR(255)
                );
            """;

        String items = """
                CREATE TABLE ITEMS (
                    ITEM_ID INT PRIMARY KEY,
                    ITEM_DETAILS VARCHAR(255),
                    ITEM_PRICE DECIMAL(10,2)
                );
            """;

        String orders = """
                CREATE TABLE ORDERS (
                    ORDER_ID INT PRIMARY KEY,
                    ID INT,
                    ITEM_ID INT,
                    FOREIGN KEY (ID) REFERENCES USERS(ID),
                    FOREIGN KEY (ITEM_ID) REFERENCES ITEMS(ITEM_ID)
                );
            """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            // Drop tables first (in correct order due to foreign keys)
            stmt.execute(dropOrders);
            stmt.execute(dropItems);
            stmt.execute(dropUsers);
            // Create fresh tables
            stmt.execute(users);
            stmt.execute(items);
            stmt.execute(orders);
            System.out.println("✅ Schema created (Updated per userapi.json)");

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // 🔹 Insert Test Data (Updated to match userapi.json structure)
    public static void insertData() {

        // Using INSERT instead of MERGE for cleaner syntax
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            // Insert USERS data
            stmt.execute("INSERT INTO USERS (ID, EMAIL, FIRST_NAME, LAST_NAME, AVATAR) VALUES (1, 'abhi@test.com', 'Abhijeet', 'Singh', 'https://api.example.com/avatar/1.jpg')");
            stmt.execute("INSERT INTO USERS (ID, EMAIL, FIRST_NAME, LAST_NAME, AVATAR) VALUES (2, 'john@test.com', 'John', 'Doe', 'https://api.example.com/avatar/2.jpg')");
            System.out.println("✅ Users inserted");
            // Insert ITEMS data
            stmt.execute("INSERT INTO ITEMS (ITEM_ID, ITEM_DETAILS, ITEM_PRICE) VALUES (101, 'Laptop', 75000)");
            stmt.execute("INSERT INTO ITEMS (ITEM_ID, ITEM_DETAILS, ITEM_PRICE) VALUES (102, 'Mouse', 500)");
            System.out.println("✅ Items inserted");
            // Insert ORDERS data
            stmt.execute("INSERT INTO ORDERS (ORDER_ID, ID, ITEM_ID) VALUES (1001, 1, 101)");
            stmt.execute("INSERT INTO ORDERS (ORDER_ID, ID, ITEM_ID) VALUES (1002, 2, 102)");
            System.out.println("✅ Orders inserted");
            System.out.println("✅ All test data inserted (Updated per userapi.json)");

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // 🔹 Fetch User Data (Updated to return userapi.json compatible structure)
    public static void fetchData() {
        String query = """
                SELECT ID, EMAIL, FIRST_NAME, LAST_NAME, AVATAR
                FROM USERS
            """;
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            System.out.println("\n📋 Users in Database:");
            while (rs.next()) {
                System.out.println(
                        "ID: " + rs.getInt("ID") +
                                " | Name: " + rs.getString("FIRST_NAME") + " " + rs.getString("LAST_NAME") +
                                " | Email: " + rs.getString("EMAIL") +
                                " | Avatar: " + rs.getString("AVATAR")
                );
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // 🔹 Fetch User with Orders (Updated)
    public static void fetchUserOrders() {
        String query = """
                SELECT u.ID, u.EMAIL, u.FIRST_NAME, u.LAST_NAME, u.AVATAR, i.ITEM_DETAILS
                FROM ORDERS o
                JOIN USERS u ON o.ID = u.ID
                JOIN ITEMS i ON o.ITEM_ID = i.ITEM_ID
            """;
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             var rs = stmt.executeQuery(query)) {
            System.out.println("\n📦 User Orders:");
            while (rs.next()) {
                System.out.println(
                        rs.getString("FIRST_NAME") + " " + rs.getString("LAST_NAME") +
                                " ordered " +
                                rs.getString("ITEM_DETAILS")
                );
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // 🔥 Main Method (Updated)
    public static void main(String[] args) {
        createSchema();      // Step 1
        insertData();        // Step 2
        fetchData();         // Step 3
        fetchUserOrders();   // Step 4
    }
}
