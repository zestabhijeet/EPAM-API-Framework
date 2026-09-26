package main;

import files.ConfigManager;

import java.sql.*;

public class DbTest {
    public Connection getconnection() throws ClassNotFoundException {
        try {
            Class.forName(ConfigManager.getProperty("DB.Url"));
            return DriverManager.getConnection(
                    ConfigManager.getProperty("DB.Url"),
                    ConfigManager.getProperty("DB.password"),
                    ConfigManager.getProperty("DB.credtinals")
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    //Assuming User/People Schema is created
    //Values are added to the table
    public void fetchUserDetails(){
        String querydml = """
                SELECT name, height, mass FROM User WHERE id = ?                                                                                                    \s
               """;
        try(Connection con = getconnection()){
            PreparedStatement ostmt = con.prepareStatement(querydml);
            ResultSet rs = ostmt.executeQuery();
            while(rs.next()){
             System.out.println(rs.getString("name") + " " + rs.getString("height")+ " " + rs.getString("mass"));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
