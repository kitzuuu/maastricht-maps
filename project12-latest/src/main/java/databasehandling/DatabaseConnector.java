package databasehandling;

import static java.lang.StringTemplate.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnector {

// Check whether database has been preprocessed after connecting.
// If not, start the lengthy preprocessing by creating DatabasePreprocessing class
// If so, don't do that

    private static final String localHost = "jdbc:mysql://localhost:3306/gtfs";
    private static final String username = "root";
    private static final String password = "WalramD1!";

    public static Connection getConnection() throws SQLException {
        try {
            return DriverManager.getConnection(localHost, username, password);
        } catch (SQLException e) {
            System.out.println(STR."Unable to connect to database: \{e.getMessage()}");
            throw e;

        }
    }

}
