import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import handlers.RouteHandler;
import mqtt.MqttListener;
import util.DatabaseConnection;
import static spark.Spark.*;

public class Main {
    private static final Logger logger = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        port(8080); // Specifica la porta del server (puoi usare una diversa se preferisci)

        // Imposta il livello di log desiderato
        logger.setLevel(Level.INFO);

        // Aggiungi un gestore per il log sulla console
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.INFO);
        logger.addHandler(consoleHandler);

        // Abilita CORS per tutte le origini e per i metodi e le intestazioni necessarie
        enableCORS("*", "GET, POST, OPTIONS", "Content-Type, Authorization");

        try {
            // Ottieni la connessione al database utilizzando la classe DatabaseConnection
            Connection connection = DatabaseConnection.getConnection();
            if (connection != null) {
                logger.info("Connessione al database stabilita.");

                // Inizializza il gestore delle route con la connessione al database
                RouteHandler routeHandler = new RouteHandler(connection);
                // Configura le route
                routeHandler.initRoutes();
            } else {
                logger.severe("Impossibile stabilire la connessione al database.");
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore durante l'inizializzazione: " + e.getMessage(), e);
        }

        try {
            String[] topics = {
                "parcheggio/verifica_ingresso", 
                "parcheggio/verifica_uscita", 
                "parcheggio/richiesta_tutti_id_posto_auto",
                "parcheggio/sensore_occupazione",
                "parcheggio/richiesta_tutte_prenotazioni_attive",
                "parcheggio/multe",
                "parcheggio/richiesta_tutte_prenotazioni_attive_auto_dentro",
                "parcheggio/richiesta_ricariche",
                "parcheggio/aggiornamento_ricarica",
                "parcheggio/ricarica_effettuata"
            };
            MqttListener mqttListener = new MqttListener("tcp://localhost:1883", topics);
            Thread listenerThread = new Thread(mqttListener);
            listenerThread.start();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore durante la connessione al broker MQTT: " + e.getMessage(), e);
        }               
    }

    // Metodo per abilitare CORS globalmente
    private static void enableCORS(final String origin, final String methods, final String headers) {
        options("/*", (request, response) -> {
            String accessControlRequestHeaders = request.headers("Access-Control-Request-Headers");
            if (accessControlRequestHeaders != null) {
                response.header("Access-Control-Allow-Headers", accessControlRequestHeaders);
            }

            String accessControlRequestMethod = request.headers("Access-Control-Request-Method");
            if (accessControlRequestMethod != null) {
                response.header("Access-Control-Allow-Methods", accessControlRequestMethod);
            }

            return "OK";
        });

        before((request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Allow-Methods", "POST, GET, OPTIONS, PUT, DELETE");
            response.header("Access-Control-Allow-Headers", "Content-Type, Accept, Origin, X-Requested-With");
            response.header("Access-Control-Allow-Credentials", "true");
        
            if (request.requestMethod().equals("OPTIONS")) {
                response.status(200);
                halt();
            }
        });
    }
}