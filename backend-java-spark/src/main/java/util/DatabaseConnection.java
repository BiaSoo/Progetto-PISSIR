package util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private static final String URL = "jdbc:postgresql://localhost:5432/GestioneParcheggio";
    private static final String USERNAME = "postgres";
    private static final String PASSWORD = "1234";
    private static Connection connection; // Dichiarazione della variabile di istanza per la connessione

    public static Connection getConnection() throws SQLException {
        connection = DriverManager.getConnection(URL, USERNAME, PASSWORD); // Inizializzazione della connessione
        return connection;
    }
    
    // Metodo per chiudere la connessione al database
    public static void closeConnection() throws SQLException {
        if (connection != null) {
            connection.close();
        }
    }
}
