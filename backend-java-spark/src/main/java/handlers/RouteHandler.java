package handlers;

import dao.DAO;

import java.sql.Connection;
import java.sql.SQLException;
import static spark.Spark.*;
import spark.Response;
import util.ErrorResponse;
import util.JwtUtil;
import entity.*;
import entity.CartaCredito.TipoCarta;
import entity.Utente.TipoUtente;
import entity.Prenotazione.StatoPrenotazione;
import entity.Transazione.TipoTransazione;

import com.google.gson.Gson; // Importa la classe Gson da com.google.gson
import com.google.gson.JsonElement;
import com.google.gson.JsonObject; // Importa la classe JsonObject da com.google.gson
import com.google.gson.JsonParser; // Importa la classe JsonParser da com.google.gson
import com.google.gson.JsonSyntaxException;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.text.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.time.Duration;
import java.util.Arrays;
import java.nio.charset.StandardCharsets;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import java.time.Instant;

public class RouteHandler {
    private final DAO dao;
    private final Gson gson;
    private static final String SECRET_KEY = "my_secret_key_1234567890";
    private static Coda codaPrenotazioni = new Coda(); // Crea un'istanza di Coda
    private static List<Ricarica> ricariche = new ArrayList<Ricarica>();

    public RouteHandler(Connection connection) {
        this.dao = new DAO(connection);
        this.gson = new Gson();
    }

    public void initRoutes() {
        get("/api/check_ricarica_effettuata", (req, res) -> {
            int idRicarica = Integer.parseInt(req.queryParams("id_ricarica"));
            
            try {
                String response = dao.getIdUtenteByIdRicarica(idRicarica);
                
                if (response!= null) {
                    res.status(200);
                    return response;
                } else {
                    res.status(404);
                    return "{\"message\": \"Ricarica effettuata not found\"}";
                }
            } catch (Exception e) {
                res.status(500);
                return "{\"message\": \"Error checking ricarica effettuata\"}";
            }
        });

        post("/api/signin", (req, res) -> {
            
            String requestBody = req.body(); // Ottieni il corpo della richiesta
        
            // Parsa il corpo della richiesta JSON per ottenere i dati dell'utente
            JsonObject jsonObject = JsonParser.parseString(requestBody).getAsJsonObject();
            String username = jsonObject.get("username").getAsString();
            String password = jsonObject.get("password").getAsString();
            TipoCarta tipoCarta = TipoCarta.valueOf(jsonObject.get("tipoCarta").getAsString());
            String numeroCarta = jsonObject.get("numeroCarta").getAsString();
            String scadenza = jsonObject.get("dataScadenza").getAsString();
            String cvv = jsonObject.get("cvv").getAsString();
            TipoUtente tipoUtente = TipoUtente.valueOf(jsonObject.get("userType").getAsString());
            numeroCarta = numeroCarta.replaceAll("\\s", "");
        
            Date date;
            date = new SimpleDateFormat("MM/yy").parse(scadenza);
            java.sql.Date dataScadenza = new java.sql.Date(date.getTime());
        
            try {
                if (username != null && password != null && tipoUtente != null && numeroCarta != null && dataScadenza != null && cvv != null && tipoCarta != null) {
                    Utente newUser = new Utente(null, username, password, tipoUtente); // null al posto del QRCode
                    
                    if (dao.isUsernamePresent(username)) {
                        res.status(409);
                        
                        return "Username esistente";
                    }
        
                    String qrcodeFromDB = dao.createAccount(newUser);
                    if (qrcodeFromDB != null) {
                        JsonObject responseJson = new JsonObject();
                        
                        // Ritorna il QRCode come parte della risposta
                        CartaCredito newCard = new CartaCredito(numeroCarta, dataScadenza, cvv, qrcodeFromDB, tipoCarta); // Creazione della carta con il tipo specificato
                        boolean response = dao.createCreditCard(newCard);
                        if (response) {
                            String token = generateJWT(newUser.getUsername(), newUser.getTipoUtente(), qrcodeFromDB);
                            
                            responseJson.addProperty("token", token);
                            responseJson.addProperty("message", "SignIn successo!");
        
                            if (tipoUtente == TipoUtente.Premium) {
                                LocalDateTime currentTime = LocalDateTime.now();
                                creaPagamento(username, "premium", res, currentTime, 10.00);
                            }
        
                            res.status(200);
                            res.header("Content-Type", "application/json; charset=UTF-8");
                            return responseJson.toString();
                        } else {
                            res.status(422);
                            
                            response = dao.deleteAccount(username);
                            if (response) {
                                
                            } else {
                                
                            }
        
                            return "Dati della carta non validi";
                        }
                    } else {
                        res.status(500);
                        
                        return "Errore durante la registrazione";
                    }
                } else {
                    res.status(400);
                    
                    return "Parametri mancanti";
                }
            } catch (SQLException e) {
                res.status(500);
                
                return "Errore del server";
            }
        }); 

        post("/api/login", (req, res) -> {
            String requestBody = req.body(); // Ottieni il corpo della richiesta
            
            // Parsa il corpo della richiesta JSON per ottenere username e password
            JsonObject jsonObject = JsonParser.parseString(requestBody).getAsJsonObject();
            String username = jsonObject.get("Username").getAsString();
            String password = jsonObject.get("Password").getAsString();
            
            try {
                if (username!= null && password!= null) {
                    // Verifica le credenziali nel database utilizzando DAO
                    Utente user = dao.getUserByUsernameAndPassword(username, password);
                    
                    if (user!= null) {
                        String token = generateJWT(user.getUsername(), user.getTipoUtente(), user.getQrcode());
                        
                        // Prepara la risposta con il token
                        JsonObject responseJson = new JsonObject();
                        responseJson.addProperty("token", token);
                        responseJson.addProperty("message", "Login successo!");
                        
                        res.status(200);
                        res.header("Content-Type", "application/json; charset=UTF-8");
                        return responseJson.toString();
                    } else {
                        res.status(401);
                        return "Credenziali non valide";
                    }
                } else {
                    res.status(400);
                    return "Parametri mancanti";
                }
            } catch (SQLException e) {
                res.status(500);
                return "Errore del server";
            }
        });   

        get("/api/currentPrices", (request, response) -> {
            try {
                // Ottieni la carta di credito associata all'utente
                Costi costi = dao.getCosti();
                if (costi == null) {
                    response.status(404);
                    return "{\"message\": \"Credit card not found for the user\"}";
                }
        
                Map<String, Double> pricesJson = new HashMap<>();
                pricesJson.put("costoSosta", costi.getCostoEuroOra());
                pricesJson.put("costoRicarica", costi.getCostoEuroKw());
        
                // Converti la mappa in una stringa JSON
                String jsonString = new Gson().toJson(pricesJson);
        
                response.status(200);
                return jsonString;
            } catch (SQLException e) {
                // Gestione delle eccezioni SQL
                response.status(500);
                return "{\"message\": \"Errore del Server\"}";
            }
        });

        get("/api/currentCard", (request, response) -> {
            String qrCode = request.queryParams("qrCode");

            try {
                // Ottieni la carta di credito associata all'utente
                CartaCredito card = dao.getCardByQRCode(qrCode);
                if (card == null) {
                    response.status(404);
                    return "{\"message\": \"Carta di credito non trovata\"}";
                }

                // Formattazione della data di scadenza nel formato "MM/YY"
                SimpleDateFormat dateFormat = new SimpleDateFormat("MM/yy");
                String dataScadenzaFormatted = dateFormat.format(card.getDataScadenza());
                
                // Crea un oggetto JSON con i dettagli della carta di credito
                Map<String, Object> cardJson = new HashMap<>();
                cardJson.put("tipo_carta", card.getTipoCarta());
                cardJson.put("numero_carta", card.getNumeroCarta());
                cardJson.put("data_scadenza", dataScadenzaFormatted);
                cardJson.put("cvv", card.getCvv());

                // Converti la mappa in una stringa JSON
                String jsonString = new Gson().toJson(cardJson);

                response.status(200);
                return jsonString;
            } catch (SQLException e) {
                // Gestione delle eccezioni SQL
                response.status(500);
                
                return "{\"message\": \"Errore del server\"}";
            }
        });       

        post("/api/upgradeToPremium", (request, response) -> {            
            String authHeader = request.headers("Authorization");
            Claims claims;
        
            try {
                claims = extractAndValidateToken(authHeader); // Chiamata al metodo di estrazione e convalida del token
            } catch (RuntimeException e) {
                response.status(401);
                return "{\"message\": \"Aggiornamento ad Utente Premium non riuscito\"}";
            }
        
            String username = claims.getSubject();
            String qrCode = claims.get("qrCode", String.class);
            LocalDateTime currentTime = LocalDateTime.now();
        
            try {
                if (dao.isUsernamePresent(username)) {
                    // Verifica se l'utente ha una carta di credito associata
                    if (!dao.isCreditCardPresent(qrCode)) {
                        response.status(422);
                        return "{\"message\": \"Non hai nessuna Carta di Credito associata per effettuare il pagamento\"}";
                    }
        
                    if (dao.upgradeToPremium(username)) {
                        // Aggiorna il tipo di utente a premium
                        TipoUtente newType = TipoUtente.Premium;
        
                        // Genera un nuovo token JWT con il nuovo tipo utente premium
                        String newToken = generateJWT(username, newType, qrCode);
        
                        try {
                            creaPagamento(username, "premium", response, currentTime, 10.00);
                        } catch (Exception e) {
                            dao.downgradeToNonPremium(username);
                            response.status(402);
                            return "{\"message\": \"Errore durante il pagamento\"}";
                        }
                                   
                        response.status(200);
                        return "{\"message\": \"Sei un utente premium!\", \"Token\": \"" + newToken + "\"}";
                    } else {
                        response.status(400);
                        return "{\"message\": \"Aggiornamento ad Utente Premium non riuscito\"}";
                    }
                } else {
                    response.status(404);
                    return "{\"message\": \"Utente non trovato\"}";
                }
            } catch (SQLException e) {
                // Gestione delle eccezioni SQL
                response.status(500);
                return "{\"message\": \"Errore interno\"}";
            }
        });

        post("/api/upgradeToCard", (request, response) -> {
            String authHeader = request.headers("Authorization");
            Claims claims;
        
            try {
                claims = extractAndValidateToken(authHeader);
            } catch (RuntimeException e) {
                response.status(401);
                return "{\"message\": \"Aggiornamento ad Utente Premium non riuscito\"}";
            }
        
            String qrCode = claims.get("qrCode", String.class);
        
            String requestBody = request.body();
        
            JsonObject jsonObject = JsonParser.parseString(requestBody).getAsJsonObject();
        
            TipoCarta tipoCarta = TipoCarta.valueOf(jsonObject.get("tipoCarta").getAsString());
            String numeroCarta = jsonObject.get("numeroCarta").getAsString();
            String scadenza = jsonObject.get("dataScadenza").getAsString();
            String cvv = jsonObject.get("cvv").getAsString();
            numeroCarta = numeroCarta.replaceAll("\\s", "");
        
            Date date;
            date = new SimpleDateFormat("MM/yy").parse(scadenza);
            java.sql.Date dataScadenza = new java.sql.Date(date.getTime());
        
            try {
                if (numeroCarta != null && dataScadenza != null && cvv != null && tipoCarta != null) {
        
                    CartaCredito card = dao.getCardByQRCode(qrCode);
                    
                    if (card != null) {
                        CartaCredito newCard = new CartaCredito(numeroCarta, dataScadenza, cvv, qrCode, tipoCarta);
                        try {
                            boolean res = dao.upgradeToCard(newCard);
                            if (res) {
                                response.status(200);
                                return "{\"message\": \"Carta di credito aggiornata con successo\"}";
                            } else {
                                response.status(400);
                                return "{\"message\": \"Dati della carta di credito non validi\"}";
                            }
                        } catch (SQLException e) {
                            response.status(500);
                            return "{\"message\": \"Errore durante la connessione al database\"}";
                        }
                    } else {
                        response.status(404);
                        return "{\"message\": \"Carta di credito non trovata\"}";
                    }
                } else {
                    response.status(400);
                    return "{\"message\": \"Dati della carta di credito non validi\"}";
                }
            } catch (Exception e) {
                response.status(400);
                return "{\"message\": \"Errore durante la validazione dei dati della carta di credito: " + e.getMessage() + "\"}";
            }
        });        
        
        post("/api/modificaPrezzi", (req, res) -> {
            String requestBody = req.body(); // Ottieni il corpo della richiesta
            
            // Parsa il corpo della richiesta JSON per ottenere costoSosta e costoRicarica
            JsonObject jsonObject = JsonParser.parseString(requestBody).getAsJsonObject();
            String costoSosta = jsonObject.get("CostoSosta").getAsString();
            String costoRicarica = jsonObject.get("CostoRicarica").getAsString();
        
            try {
                if (costoSosta != null && costoRicarica != null) {
                    
                    // Aggiorna i prezzi nel database utilizzando DAO
                    boolean isUpdated = dao.modificaCosti(costoSosta, costoRicarica);
                    if (isUpdated) {
                        // Prepara la risposta con un messaggio di successo
                        JsonObject responseJson = new JsonObject();
                        responseJson.addProperty("message", "Prezzi modificati con successo!");
        
                        res.status(200);
                        res.header("Content-Type", "application/json; charset=UTF-8");
                        return responseJson.toString();
                    } else {
                        res.status(500);
                        return "Errore durante l'aggiornamento dei prezzi";
                    }
                } else {
                    res.status(400);
                    return "Parametri mancanti";
                }
            } catch (SQLException e) { 
                res.status(500);
                return "Errore del server";
            }
        });        

        delete("/api/cancellaPrenotazione", (req, res) -> {  
            String requestBody = req.body();
            JsonObject jsonObject = JsonParser.parseString(requestBody).getAsJsonObject();
            int id_prenotazione = jsonObject.get("id_prenotazione").getAsInt();
            
            try {
                boolean response = dao.deletePrenotazione(id_prenotazione);
                if (response) {
                    
                    res.status(200); // Restituisci un codice di stato 200
                    return "Prenotazione cancellata con successo";
                } else {
                    
                    res.status(400); // Restituisci un codice di stato 400
                    return "Errore durante la cancellazione della prenotazione";
                }
            } catch (SQLException e) {
                
                res.status(500); // Restituisci un codice di stato 500
                return "Errore del server";
            }
        });      

        get("/api/checkParkedVehiclesOfUser", (req, res) -> {
            String qrCode = req.queryParams("qrCode");
        
            if (qrCode != null) {
                try {
                    return dao.hasVehiclesInParking(qrCode);
                } catch (SQLException e) {
                    res.status(500);
                    return gson.toJson("Internal Server Error");
                }
            } else {
                res.status(400);
                return gson.toJson("Bad Request");
            }
        });        
        
        post("/api/transazioniUtenti", (req, res) -> {  
            String requestBody = req.body(); // Ottieni il corpo della richiesta
        
            JsonObject jsonObject = JsonParser.parseString(requestBody).getAsJsonObject();
            String dataOraInizioString = jsonObject.get("dataOraInizio").getAsString();
            String dataOraFineString = jsonObject.get("dataOraFine").getAsString();
            String filters = jsonObject.get("filters").getAsString();
            List<String> result = Arrays.asList(filters.split(","));
        
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
            LocalDateTime dataOraInizio = LocalDateTime.parse(dataOraInizioString, formatter);
            LocalDateTime dataOraFine = LocalDateTime.parse(dataOraFineString, formatter);
        
            try {
                if (dataOraInizio != null && dataOraFine != null) {
                    if ((result.contains("base") || result.contains("premium")) && (result.contains("sosta") || result.contains("ricarica") || result.contains("abbonamento"))) {
                        List<Transazione> transazioni = dao.getFilteredTransactions(dataOraInizio, dataOraFine, result);
        
                        if (transazioni != null && !transazioni.isEmpty()) {
                            List<Map<String, Object>> transazioniJson = new ArrayList<>();
        
                            for (Transazione transazione : transazioni) {
                                String username = dao.getUsernameByQrcode(transazione.getId_utente());
                                Map<String, Object> transazioneJson = new HashMap<>();
                                transazioneJson.put("username", username);
                                transazioneJson.put("tipoTransazione", transazione.getTipo_transazione());
                                transazioneJson.put("importo", transazione.getImporto());
                                DateTimeFormatter formatterResp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                                transazioneJson.put("dataTransazione", transazione.getData_transazione().format(formatterResp));
        
                                transazioniJson.add(transazioneJson);     
                            }
        
                            String jsonResponse = gson.toJson(transazioniJson);
                            res.type("application/json");
                            return jsonResponse;
                        } else {
                            res.status(404);
                            return "Nessuna transazione trovata";
                        }
                    } else {
                        res.status(400);
                        return "Filtri mancanti";
                    }
                } else {
                    res.status(400);
                    return "Parametri mancanti";
                }
            } catch (SQLException e) {
                res.status(500);
                return "Errore del server";
            }
        });        

        get("/api/ricariche", (req, res) -> {
            try {
                List<Ricarica> ricariche = dao.getAllRicariche();
                
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                
                List<Map<String, Object>> ricaricheMap = ricariche.stream().map(ricarica -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("idRicarica", ricarica.getIdRicarica());
                    map.put("idVeicolo", ricarica.getIdVeicolo());
                    map.put("percentualeRicarica", ricarica.getPercentualeRicarica());
                    map.put("durataRicarica", ricarica.getDurataRicarica());
                    map.put("id_utente", ricarica.getId_utente());
                    map.put("timestampCreazione", ricarica.getTimestampCreazione().format(formatter)); // Format to "dd/MM/yyyy HH:mm"
                    map.put("effettuata", ricarica.isEffettuata());
                    map.put("id_posto_auto", ricarica.getIdPostoAuto());
                    return map;
                }).collect(Collectors.toList());
        
                String jsonResponse = gson.toJson(ricaricheMap);
                
                res.status(200);
                res.type("application/json");
                return jsonResponse;
            } catch (SQLException e) {
                res.status(500);
                return "{\"message\":\"Errore del server\"}";
            }
        });        

        post("/api/confermaRicarica", (req, res) -> {
            // Verifica dell'header di autorizzazione
            String authHeader = req.headers("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                res.status(401);
                return "{\"error\": \"Missing or invalid Authorization header\"}";
            }
        
            String token = authHeader.substring(7); // Estrai il token dalla stringa dell'header Authorization
            String qrCode;
        
            try {
                Claims claims = validateToken(token); // Validazione del token JWT
                qrCode = claims.get("qrCode", String.class);
            } catch (RuntimeException e) {
                // Gestione delle eccezioni di token non valido
                res.status(401);
                return "{\"error\": \"" + e.getMessage() + "\"}";
            }
        
            String requestBody = req.body();
            if (requestBody == null || requestBody.trim().isEmpty()) {
                res.status(400);
                return "{\"error\": \"Invalid request body\"}";
            }
        
            try {
                JsonObject jsonObject = gson.fromJson(requestBody, JsonObject.class);
        
                // Stampa di debug per vedere il contenuto del JSON
        
                if (!jsonObject.has("percentualeRicarica") || !jsonObject.has("veicolo")) {
                    res.status(400);
                    return "{\"error\": \"Missing required fields: percentualeRicarica and targa are required\"}";
                }
        
                int percentualeRicarica = jsonObject.get("percentualeRicarica").getAsInt();
        
                // Recupera l'id del veicolo basato sull'id_utente
                String targa = jsonObject.get("veicolo").getAsString();

                int idVeicolo = dao.getIdVeicoloInsideParkingByTarga(targa); 
        
                if (idVeicolo == -1) {
                    res.status(404);
                    return "{\"error\": \"Vehicle not found for the provided user\"}";
                }
        
                LocalDateTime timestampCreazione = LocalDateTime.now();
        
                // Crea un nuovo oggetto Ricarica
                Ricarica newRicarica = new Ricarica(qrCode, idVeicolo, percentualeRicarica, 30, timestampCreazione);
        
                // Recupera le ricariche ordinate per data
                List<Ricarica> ricariche = dao.getRicaricheOrderByData();
        
                // Calcola il tempo di attesa totale
                int totalMinutes = 0;
                for (Ricarica ricarica : ricariche) {
                    totalMinutes += ricarica.getDurataRicarica();
                }
        
                // Aggiungi la nuova ricarica alla coda
                codaPrenotazioni.addRicarica(newRicarica);
        
                // Salva la nuova ricarica nel database
                dao.saveRicarica(newRicarica);
        
                // Ritorna un messaggio di successo con i minuti totali
                JsonObject responseJson = new JsonObject();
                responseJson.addProperty("totalMinutes", totalMinutes);
                res.status(200);
                return responseJson.toString(); // Converti in stringa JSON
            } catch (JsonSyntaxException e) {
                res.status(400);
                return "{\"error\": \"Invalid JSON request body\"}";
            }
        });
        
        post("/api/prenotaposto", (req, res) -> {
            String authHeader = req.headers("Authorization");
            Claims claims;
        
            try {
                claims = extractAndValidateToken(authHeader);
            } catch (RuntimeException e) {
                res.status(401);
                return "{\"message\": \"Prenotazione non effettuata\"}";
            }
        
            String qrCode = claims.get("qrCode", String.class);

            String requestBody = req.body(); // Ottieni il corpo della richiesta
            // Parsa il corpo della richiesta JSON per ottenere i dati dell'utente
            JsonObject jsonObject = JsonParser.parseString(requestBody).getAsJsonObject();
            String ingresso = jsonObject.get("Ingresso").getAsString();
            String uscita = jsonObject.get("Uscita").getAsString();
        
            // Parsing delle date e della durata
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
            LocalDateTime tempoArrivo = LocalDateTime.parse(ingresso, formatter);
            LocalDateTime tempoUscita = LocalDateTime.parse(uscita, formatter).plusMinutes(15);
            long durataPermanenzaInMinuti = Duration.between(tempoArrivo, tempoUscita).toMinutes();
            
            CartaCredito cartaDiCredito = dao.getCardByQRCode(qrCode);
            if (cartaDiCredito == null) {
                res.status(400); // Bad Request
                return "{\"message\": \"Carta di credito non trovata\"}";
            }

            // Lista di posti_auto
            List<Parcheggio> parcheggiList = dao.getAllParcheggiRiservati();
            
            // Lista delle prenotazioni attive
            List<Prenotazione> prenotazioniAttiveList = dao.getActivePrenotazioni();

            // Rilevo collisioni e ottengo identificativo posto auto libero
            int idPostoAutoLibero = findFirstAvailablePostoAuto(prenotazioniAttiveList, tempoArrivo, tempoUscita, parcheggiList);
            
            if (idPostoAutoLibero != -1) {
                // Creazione della prenotazione
                LocalDateTime timestampCreazione = LocalDateTime.now();

                Prenotazione prenotazione = new Prenotazione(qrCode, 0, idPostoAutoLibero, tempoArrivo, durataPermanenzaInMinuti, cartaDiCredito.getIdCarta(), StatoPrenotazione.attiva, timestampCreazione);

                dao.createPrenotazione(prenotazione);
                res.status(200);
                return "{\"message\": \"Prenotazione effettuata con successo\"}";
            } else {
                res.status(409);  // 409 Conflict
                return "{\"message\": \"Nessun posto auto disponibile in quell'orario\"}";
            }
        });
        
        get("/api/veicoliNelParcheggio", (req, res) -> {
            String qrCode = req.queryParams("qrCode");
            
            if (qrCode != null) {
                try {
                    List<String> targhe = dao.getVeicoliNelParcheggio(qrCode);

                    return gson.toJson(targhe);
                } catch (SQLException e) {  
                    res.status(500);
                    return "Errore del Server";
                }
            } else {
                res.status(400);
                return "Errore del Server";
            }
        });

        get("/api/cardTypes", (req, res) -> {
            // Ottieni i nomi degli enum TipoCarta dalla classe CartaCredito
            List<String> cardTypes = new ArrayList<>();
            for (CartaCredito.TipoCarta tipoCarta : CartaCredito.TipoCarta.values()) {
                cardTypes.add(tipoCarta.name());
            }

            // Converte i tipi di carta in formato JSON e li restituisce
            return gson.toJson(cardTypes);
        });
        
        post("/api/richiediRicarica", (req, res) -> {
            try {
                // Recupera le richieste dalla database in ordine di data
                ricariche = dao.getRicaricheOrderByData();
                
                // Calcola il tempo di attesa totale
                int totalMinutes = 0;
                for (Ricarica ricarica : ricariche) {
                    totalMinutes += ricarica.getDurataRicarica();
                }
                
                res.status(200);
                return String.valueOf(totalMinutes);
                
            } catch (SQLException e) {
                res.status(500);
                return "Errore durante la lettura del tempo di attesa";
            }
        });                 

        get("/api/cardTypes", (req, res) -> {
            

            // Ottieni i nomi degli enum TipoCarta dalla classe CartaCredito
            List<String> cardTypes = new ArrayList<>();
            for (CartaCredito.TipoCarta tipoCarta : CartaCredito.TipoCarta.values()) {
                cardTypes.add(tipoCarta.name());
            }

            // Converte i tipi di carta in formato JSON e li restituisce
            
            return gson.toJson(cardTypes);
        });

        post("/api/sensore_occupazione", (req, res) -> {
            String requestBody = req.body();

        
            try {
                // Parsing del JSON
                JsonObject jsonObject = JsonParser.parseString(requestBody).getAsJsonObject();
                // Verifica e decodifica del token JWT
                try {
                    JwtUtil.handleMqttRequest(jsonObject);
                } catch (RuntimeException e) {
                    res.status(401);
                    return "{\"message\": \"Token non valido o scaduto\"}";
                }
                int idPostoAuto = jsonObject.get("id_sensore").getAsInt();
                boolean statoOccupazione = jsonObject.get("stato_occupazione").getAsBoolean();
                String username = jsonObject.get("username").getAsString();
                String targa = jsonObject.get("targa").getAsString();
                String qrCode = dao.getQRCodeByUsername(username);
                int idVeicolo = dao.getIdVeicoloInsideParkingByTarga(targa);

                // se è stato occupato
                if (statoOccupazione)
                    dao.updateParcheggioOccupato(idPostoAuto, statoOccupazione, idVeicolo, qrCode);
                else {
                    dao.updateParcheggioLibero(idPostoAuto, statoOccupazione);
                    dao.setVeicolo(idVeicolo);
                }

                res.status(200);

                // Se è stato occupato
                if (statoOccupazione)
                    return "{\"message\": \"Sensore " + idPostoAuto + " ha rilevato occupazione nel posto auto " + idPostoAuto + "\"}";
                else // Altrimenti è libero
                return "{\"message\": \"Sensore " + idPostoAuto + " ha rilevato liberazione nel posto auto " + idPostoAuto + "\"}";
            } catch (SQLException e) {
                res.status(500);
                return "{\"error\": \"Errore interno del server\"}";
            } catch (Exception e) {
                res.status(400);

                return "{\"error\": \"Richiesta non valida\"}";
            }
        });      
        
        post("/api/ricarica_effettuata", (req, res) -> {
            try {
                // Recupera il payload dalla richiesta MQTT
                String payload = req.body();

                // Parso il payload come JsonObject
                JsonObject jsonObject = new Gson().fromJson(payload, JsonObject.class);

                // Gestione della richiesta e validazione del token usando JwtUtil
                JwtUtil.handleMqttRequest(jsonObject);
                
                // Estrae l'ID ricarica dalla richiesta e tempo di creazione ed energia
                int idRicarica = jsonObject.get("id_ricarica").getAsInt();
                String simTimeInput = jsonObject.get("timestamp").getAsString();
                double energia = jsonObject.get("kw_usati").getAsDouble();
                
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
                LocalDateTime currentTime = LocalDateTime.parse(simTimeInput, formatter);
                
                // Aggiorna il campo effettuata a true nel database e mi ritorna le info per creare il pagamento
                Ricarica ricaricaEffettuata = dao.updateRicaricaEffettuata(idRicarica, true);
                
                String username = dao.getUsernameByQrcode(ricaricaEffettuata.getId_utente());
                
                Costi costi = dao.getCosti();
                double costoRicarica = costi.getCostoEuroKw();
                
                // Calcola l'importo della ricarica
                double importo = energia * costoRicarica;
                
                // Crea il pagamento
                creaPagamento(username, "ricarica", res, currentTime, importo, ricaricaEffettuata.getIdVeicolo());
                
                res.status(200);
                return "{\"message\": \"Ricarica completata con successo\"}";
            } catch (SQLException e) {
                res.status(500);
                return "{\"error\": \"Errore del Server\"}";
            } catch (RuntimeException e) {
                res.status(401); // Errore di autenticazione
                return "{\"error\": \"" + e.getMessage() + "\"}";
            }
        });            

        post("/api/verifica_ingresso", (req, res) -> {
            String requestBody = req.body();

            // Parsing del JSON
            JsonObject jsonObject = JsonParser.parseString(requestBody).getAsJsonObject();
            // Verifica e decodifica del token JWT
            try {
                JwtUtil.handleMqttRequest(jsonObject);
            } catch (RuntimeException e) {
                res.status(401);
                return "{\"message\": \"Token non valido o scaduto\"}";
            }
            String usernameInput = jsonObject.get("username").getAsString();
            String licensePlateInput = jsonObject.get("targa").getAsString(); 
            String simTimeInput = jsonObject.get("timestamp").getAsString();
        
            // Formattazione del timestamp
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
            LocalDateTime currentTime = LocalDateTime.parse(simTimeInput, formatter);
        
            // Controllo se i campi sono vuoti
            if (usernameInput == null || usernameInput.isEmpty() || licensePlateInput == null || licensePlateInput.isEmpty()) {
                res.status(400);

                return "{\"message\": \"Username o Targa non sono stati rilevati correttamente\"}";
            }
        
            try {
                String qrCode = dao.getUserIdByUsername(usernameInput);

        
                // Controllo se l'utente è registrato
                if (qrCode == null || qrCode.equals("Utente non trovato")) {
                    res.status(404);

                    return "{\"message\": \"Utente non trovato\"}";
                }
        
                // Controllo se il veicolo è già nel parcheggio
                int idVeicolo = dao.getIdVeicoloDentroParcheggioByTarga(licensePlateInput);
        
                if (idVeicolo != -1) {
                    res.status(409);

                    return "{\"message\": \"L'auto con targa " + licensePlateInput + " è già dentro il parcheggio\"}";
                }

                // Controlla se l'utente è un utente premium e ha una prenotazione attiva
                int[] result = dao.getParkingSpotForPremiumUser(qrCode, currentTime);
                int idPrenotazione = result[0];
                int idPostoAuto = result[1];

                if (idPostoAuto != -1) {
                    // Utente premium con una prenotazione attiva, assegna un posto auto
                    res.status(200);
                    idVeicolo = dao.createVeicolo(licensePlateInput, qrCode, currentTime);
                    int durataPermanenza = dao.getDurataPermanenza(qrCode, currentTime);

                    dao.InsertVeicoloInPrenotazione(idPrenotazione, idVeicolo);
                    return "{\"message\": \"Ingresso permesso per username: " + usernameInput + ", targa: " + licensePlateInput + "\", " +
                        "\"info\": \"Ingresso: " + simTimeInput + ", Durata Permanenza: " + durataPermanenza + " minuti, " +
                        "ID Posto Auto: " + idPostoAuto + "\"}";
                }
        
                // Controlliamo se c'è un posto disponibile
                if (dao.getFirstAvailableParkingSpot() == -1)
                    return "{\"message\": \"Nessun posto auto disponibile\"}";

                idVeicolo = dao.createVeicolo(licensePlateInput, qrCode, currentTime);
                res.status(200);

                return "{\"message\": \"Ingresso permesso per username: " + usernameInput + ", targa: " + licensePlateInput + "\"}";
            } catch (SQLException e) {

                res.status(500);
                return "{\"message\": \"Errore interno del server\"}";
            }
        }); 
        
        post("/api/verifica_uscita", (req, res) -> {
            String requestBody = req.body();

            // Parsing del JSON
            JsonObject jsonObject = JsonParser.parseString(requestBody).getAsJsonObject();
            // Verifica e decodifica del token JWT
            try {
                JwtUtil.handleMqttRequest(jsonObject);
            } catch (RuntimeException e) {
                res.status(401);
                return "{\"message\": \"Token non valido o scaduto\"}";
            }
            String usernameInput = jsonObject.get("username").getAsString();
            String licensePlateInput = jsonObject.get("targa").getAsString(); 
            String simTimeInput = jsonObject.get("timestamp").getAsString();
        
            // Formattazione del timestamp
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
            LocalDateTime oraUscita = LocalDateTime.parse(simTimeInput, formatter);
        
            // Controllo se i campi sono vuoti
            if (usernameInput == null || usernameInput.isEmpty() || licensePlateInput == null || licensePlateInput.isEmpty()) {
                res.status(400);

                return "{\"message\": \"Username o Targa non sono stati rilevati correttamente\"}";
            }
        
            try {
                String qrCode = dao.getUserIdByUsername(usernameInput);

        
                // Controllo se l'utente è registrato
                if (qrCode == null || qrCode.equals("Utente non trovato")) {
                    res.status(404);

                    return "{\"message\": \"Utente non trovato\"}";
                }
        
                // Controllo se il veicolo è nel parcheggio
                int idVeicolo = dao.getIdVeicoloDentroParcheggioByTarga(licensePlateInput);
        
                if (idVeicolo == -1) {
                    res.status(409);

                    return "{\"message\": \"L'auto con targa " + licensePlateInput + " non e' dentro il parcheggio\"}";
                }

                // Controllo se auto sta uscendo con lo stesso utente con la quale è entrata
                if (!qrCode.equals(dao.getIdUtenteByVehicle(qrCode))) {
                    res.status(409);

                    return "{\"message\": \"Uscita negata per l'auto con targa " + licensePlateInput + ": l'utente che sta cercando di uscire con l'auto non è lo stesso che l'ha fatta entrare\"}";
                }

                // Effettuiamo pagamenti digitali (tramite carta di credito registrata nel sito)
                LocalDateTime oraInterno = dao.getDataOraIngresso(licensePlateInput);
                String formattedOraIngresso = oraInterno.format(formatter);
                LocalDateTime oraIngresso = LocalDateTime.parse(formattedOraIngresso, formatter);

                // controllo uscita sia dopo ingresso
                if (oraIngresso.isAfter(oraUscita)) {
                    res.status(409);
                    return "{\"message\": \"Attendi: l'orario simulato è precedente all'orario di ingresso dell'auto...\"}";
                }

                // Calcolo durata sosta
                Duration duration = Duration.between(oraIngresso, oraUscita);

                int inMinutes = (int) duration.toMinutes();

                // Recupero costi sosta e ricarica
                Costi costi = dao.getCosti();
                double costoEuroOra = costi.getCostoEuroOra();
                double costoEuroMinuto = costoEuroOra / 60;

                double importo = costoEuroMinuto * inMinutes;

                // creo pagamento per sosta parcheggio
                creaPagamento(usernameInput, "sosta", res, oraUscita, importo, idVeicolo);

                int idPostoAuto = dao.getIdPostoAutoByIdVeicolo(idVeicolo);

                // Controllo se era una prenotazione
                dao.processActiveReservation(idPostoAuto);
                
                res.status(200);
                return "{\"message\": \"Uscita permessa per username: " + usernameInput + ", targa: " + licensePlateInput + ", posto auto: " + idPostoAuto + "\"}";
            } catch (SQLException e) {

                res.status(500);
                return "{\"message\": \"Errore interno del server\"}";
            }
        }); 

        post("/api/leave", (req, res) -> {
            String requestBody = req.body();
            JsonObject jsonObject = JsonParser.parseString(requestBody).getAsJsonObject();
            String postoAuto = jsonObject.get("postoAuto").getAsString();

            boolean left = dao.leaveParking(postoAuto);

            JsonObject responseJson = new JsonObject();
            if (left) {
                responseJson.addProperty("message", "Auto lasciata con successo!");
            } else {
                responseJson.addProperty("message", "Impossibile lasciare l'auto. Il posto è già libero o non è stato occupato.");
            }

            return responseJson.toString();
        }); 

        get("/api/transazioniUtente", (req, res) -> {
            String qrCode = req.queryParams("qrCode"); // Ottieni il QR code dalla richiesta
            
            try {
                List<Transazione> transazioni = dao.getTransazioniByQrCode(qrCode);

                List<Map<String, Object>> transazioniJson = new ArrayList<>();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

                for (Transazione transazione : transazioni) {
                    
                    Map<String, Object> transazioneJson = new HashMap<>();
                    transazioneJson.put("tipoTransazione", transazione.getTipo_transazione());
                    transazioneJson.put("importo", transazione.getImporto());
                    transazioneJson.put("dataTransazione", transazione.getData_transazione().format(formatter));
                    transazioniJson.add(transazioneJson);
                }

                String jsonResponse = gson.toJson(transazioniJson);
                
                return jsonResponse;

            } catch (SQLException e) {
                res.status(500);
                
                return "Errore del server";
            }
        });

        get("/api/statoParcheggio", (req, res) -> {
            try {
                List<Parcheggio> parcheggi = dao.getAllParcheggi();
                List<Map<String, Object>> parcheggiJson = new ArrayList<>();
                for (Parcheggio parcheggio : parcheggi) {
                    Map<String, Object> parcheggioJson = new HashMap<>();
                    parcheggioJson.put("postoAuto", parcheggio.getId_parcheggio());
                    parcheggioJson.put("riservato", parcheggio.getRiservato());
                    parcheggioJson.put("occupato", parcheggio.isOccupato());
                    parcheggiJson.add(parcheggioJson);
                }
                
                return gson.toJson(parcheggiJson);
            } catch (Exception e) {
                // Restituisce una risposta di errore
                return gson.toJson(new ErrorResponse("Errore del Server"));
            }
        });

        // Visualizzazione delle prenotazioni
        get("/api/mieprenotazioni", (req, res) -> {
            String qrCode = req.queryParams("qrCode"); // Ottieni il QR code dalla richiesta
            
            try {
                // Recupera le prenotazioni associate all'utente con il QR code
                List<Prenotazione> prenotazioni = dao.getPrenotazioniAttiveByQrCode(qrCode);
                List<Map<String, Object>> prenotazioniJson = new ArrayList<>();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    
                for (Prenotazione prenotazione : prenotazioni) {
                    Map<String, Object> prenotazioneJson = new HashMap<>();
                    prenotazioneJson.put("idPostoAuto", prenotazione.getId_posto_auto());
                    prenotazioneJson.put("tempoArrivo", prenotazione.getTempo_arrivo().format(formatter));
                    prenotazioneJson.put("durataPermanenza", prenotazione.getDurata_permanenza());
                    prenotazioneJson.put("tempoUscita", prenotazione.getTempoUscita().format(formatter));
                    prenotazioneJson.put("timestampCreazione", prenotazione.getTimestamp_creazione().format(formatter));
                    prenotazioneJson.put("id_prenotazione", prenotazione.getId_prenotazione());
                    prenotazioniJson.add(prenotazioneJson);
                }
                return gson.toJson(prenotazioniJson);
            } catch(SQLException e) {
                res.status(500);
                return "{\"message\": \"Errore del Server\"}";
            }
        });  

        get("/api/all_active_reservations", (req, res) -> {
            try {

        
                // Recupera la lista di prenotazioni attive
                List<Prenotazione> prenotazioniList = dao.fetchActiveReservationsWithNoVehicle();

        
                List<Map<String, Object>> prenotazioni = prenotazioniList.stream()
                    .map(prenotazione -> {
                        String username = "";
                        String ingressoString = "";
                        try {
                            username = dao.getUsernameByQrcode(prenotazione.getId_utente());
                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                            ingressoString = prenotazione.getTempo_arrivo().format(formatter);
                        } catch (SQLException e) {

                            res.status(500);
                        }
                        
                        Map<String, Object> prenotazioneAttiva = Map.of(
                            "id", prenotazione.getId_prenotazione(),
                            "username", username,
                            "tempo_arrivo", ingressoString,
                            "carta_di_credito", prenotazione.getCarta_credito(),
                            "stato", prenotazione.getStato()
                        );
        

                        return prenotazioneAttiva;
                    })
                    .collect(Collectors.toList());
        
                // Converte la lista in formato JSON
                String jsonOutput = gson.toJson(prenotazioni);

        
                res.status(200);
                return jsonOutput;
            } catch (SQLException e) {

                res.status(500);
                return "{\"message\": \"Errore del Server\"}";
            }
        }); 

        get("/api/all_parking_spots_active_with_vehicle", (req, res) -> {
            try {
                // Recupera la lista di prenotazioni attive
                List<Prenotazione> prenotazioniList = dao.fetchActiveReservationsWithVehicle();

            
                List<Map<String, Object>> prenotazioni = prenotazioniList.stream()
                    .map(prenotazione -> {
                        String username = "";
                        String ingressoString = "";
                        try {
                            username = dao.getUsernameByQrcode(prenotazione.getId_utente());
                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                            ingressoString = prenotazione.getTempo_arrivo().format(formatter);
                        } catch (SQLException e) {

                            res.status(500);
                        }
                        
                        Map<String, Object> prenotazioneAttiva = Map.of(
                            "id", prenotazione.getId_prenotazione(),
                            "username", username,
                            "id_veicolo", prenotazione.getId_veicolo(),
                            "tempo_arrivo", ingressoString,
                            "durata_permanenza", prenotazione.getDurata_permanenza(),
                            "stato", prenotazione.getStato(),
                            "multa_pagata", prenotazione.isMulta_pagata(),
                            "id_posto_auto", prenotazione.getId_posto_auto()
                        );
            

                        return prenotazioneAttiva;
                    })
                    .collect(Collectors.toList());
            
                // Converte la lista in formato JSON
                String jsonOutput = gson.toJson(prenotazioni);

            
                res.status(200);
                return jsonOutput;
            } catch (SQLException e) {

                res.status(500);
                return "{\"message\": \"Errore del Server\"}";
            }
        });  
        
        post("/api/aggiornamento_ricarica", (req, res) -> {
            try {
                // Recupera il payload dalla richiesta
                String payload = req.body();
                
                // Parsifica il payload JSON in un JsonObject
                JsonObject jsonObject = JsonParser.parseString(payload).getAsJsonObject();
        
                // Verifica e valida il token dal payload
                JwtUtil.handleMqttRequest(jsonObject);
        
                // Estrai i dati necessari dal JSON dopo la validazione del token
                int idVeicolo = jsonObject.get("id_veicolo").getAsInt();
                int capacita = jsonObject.get("capacita").getAsInt();
                String modello = jsonObject.get("modello").getAsString();
                int idRicarica = jsonObject.get("id_ricarica").getAsInt();
                int durataRicarica = jsonObject.get("durata").getAsInt();
                int idPostoAuto = jsonObject.get("id_posto_auto").getAsInt();
        
                // Aggiorna il veicolo nel database
                dao.updateVeicoloAndTempoRicaricaEffettiva(idVeicolo, capacita, modello, idRicarica, durataRicarica);
        
                // Restituisci una risposta di successo
                res.status(200);
                return "{\"message\": \"MWBot occupato nel posto auto " + idPostoAuto + "\"}";
        
            } catch (RuntimeException e) {
                res.status(401); // Unauthorized
                return "{\"error\": \"Token non valido o non autorizzato\"}";
            } catch (SQLException e) {
                res.status(500); // Internal Server Error
                return "{\"error\": \"Errore del Server\"}";
            }
        });
        

        get("/api/all_parking_spots", (req, res) -> {
            try {
                // Recupera la lista di posti auto disponibili
                List<Parcheggio> parcheggiList = dao.getAllParcheggi();
                
                // Crea una lista di mappe con ID dei posti auto e il loro stato
                List<Map<String, Object>> parkingSpots = parcheggiList.stream()
                    .map(parcheggio -> {
                        Map<String, Object> parkingSpot = Map.of(
                            "id", parcheggio.getId_parcheggio(),
                            "occupato", parcheggio.isOccupato(),
                            "riservato", parcheggio.getRiservato()
                        );
                        return parkingSpot;
                    })
                    .collect(Collectors.toList());
                
                // Converte la lista in formato JSON
                res.status(200);
                return gson.toJson(parkingSpots);
            } catch(SQLException e) {
                res.status(500);
                return "{\"message\": \"Errore del Server\"}";
            }
        });

        get("/api/parking/:spot_id", (req, res) -> {
            int spotId = Integer.parseInt(req.params(":spot_id"));
            try {
                // Recupera lo stato di occupazione del posto auto dal database
                Parcheggio parcheggio = dao.getParcheggioById(spotId);
                boolean occupied = parcheggio.isOccupato();
        
                // Crea un oggetto JSON con lo stato di occupazione
                JsonObject json = new JsonObject();
                json.addProperty("occupied", occupied);
        
                // Ritorna la risposta JSON
                res.status(200);
                return json.toString();
            } catch (SQLException e) {
                res.status(500);
                return "{\"message\": \"Errore del Server\"}";
            }
        });

        get("/api/healthcheck", (req, res) -> {
            res.status(200);
            return "{\"message\": \"Backend is up and running\"}";
        });
    }

    private Integer findFirstAvailablePostoAuto(List<Prenotazione> prenotazioniAttiveList, 
                                                LocalDateTime tempoArrivo, LocalDateTime tempoUscita, List<Parcheggio> idPostiAuto) {
        for (Parcheggio idPostoAuto : idPostiAuto) {
            boolean collisionFound = false;
            for (Prenotazione prenotazione : prenotazioniAttiveList) {
                LocalDateTime prenotazioneArrivo = prenotazione.getTempo_arrivo();
                LocalDateTime prenotazioneUscita = prenotazioneArrivo.plusMinutes(prenotazione.getDurata_permanenza());

                // Verifica la sovrapposizione tra la nuova prenotazione e quella esistente
                if (!(tempoUscita.isBefore(prenotazioneArrivo) || tempoArrivo.isAfter(prenotazioneUscita))) {
                    // Verifica se il posto auto è lo stesso
                    if (prenotazione.getId_posto_auto() == idPostoAuto.getId_parcheggio()) {
                        collisionFound = true;
                        break;
                    }
                }
            }
            if (!collisionFound) {
                return idPostoAuto.getId_parcheggio();
            }
        }
        return -1; // Nessun posto auto disponibile trovato
    }

    private String creaPagamento(String username, String tipo, Response res, LocalDateTime currentTime, double... args) {
        boolean check, verifica;
        TipoTransazione tipoTransazione;
        try {
            tipoTransazione = TipoTransazione.valueOf(tipo);
        } catch (IllegalArgumentException e) {
            res.status(400);
            return "Tipo di transazione non valido";
        }
    
        double importo = 0;
    
        try {
            String id_utente = dao.getUserIdByUsername(username);

    
            // Pagamento Abbonamento
            if (tipo.equals("premium")) {
                tipoTransazione = TipoTransazione.premium;
                importo = args[0];

                check = dao.isCreditCardPresent(id_utente);

                if (check) {
                    Transazione transazione = new Transazione(id_utente, tipoTransazione, importo, currentTime);
                    verifica = dao.creaPagamento(transazione);

                    if (verifica) {
                        return "Pagamento effettuato con successo";
                    } else {
                        res.status(500);
                        return "Errore durante il pagamento";
                    }
                }
            }

            // Pagamento Multa
            if (tipo.equals("multa")) {
                tipoTransazione = TipoTransazione.multa;
                importo = args[0];
                check = dao.isCreditCardPresent(id_utente);
                if (check) {
                    Transazione transazione = new Transazione(id_utente, tipoTransazione, importo, currentTime);
                    verifica = dao.creaPagamento(transazione);
                    if (verifica) {
                        return "Pagamento effettuato con successo";
                    } else {
                        res.status(500);
                        return "Errore durante il pagamento";
                    }
                }
            }
    
            // Pagamento Sosta
            if (tipo.equals("sosta")) {
                int id_veicolo = (int) args[1];
                importo = args[0];
                tipoTransazione = TipoTransazione.sosta;

                check = dao.isCreditCardPresent(id_utente);

                if (check) {
                    Transazione transazione = new Transazione(id_utente, id_veicolo, tipoTransazione, importo, currentTime);
                    verifica = dao.creaPagamento(transazione);

                    if (verifica) {
                        return "Pagamento parcheggio effettuato con successo";
                    } else {
                        res.status(500);
                        return "Errore durante il pagamento del parcheggio";
                    }
                }
            }
    
            // Pagamento Ricarica
            if (tipo.equals("ricarica")) {
                int id_veicolo = (int) args[1];
                importo = args[0];
                tipoTransazione = TipoTransazione.ricarica;

                check = dao.isCreditCardPresent(id_utente);

                if (check) {
                    Transazione transazione = new Transazione(id_utente, id_veicolo, tipoTransazione, importo, currentTime);
                    verifica = dao.creaPagamento(transazione);

                    if (verifica) {
                        return "Pagamento ricarica effettuato con successo";
                    } else {
                        res.status(500);
                        return "Errore durante il pagamento della ricarica";
                    }
                }
            }
    
            res.status(400);
            return "Parametri errati";
        } catch (SQLException e) {
            e.printStackTrace();
            res.status(500);
            return "Errore del server";
        }
    }    

    public String generateJWT(String username, TipoUtente userType, String qrCode) {
        Instant now = Instant.now(); // già in UTC
        Instant expiration = now.plusSeconds(3600); // 1 hour expiration
    
        String userTypeJson = new Gson().toJson(userType); // Serialize the TipoUtente object to a JSON string
    
        String token = Jwts.builder()
               .setSubject(username)
               .claim("userType", userTypeJson)
               .claim("qrCode", qrCode)
               .setIssuedAt(Date.from(now)) // Convert Instant to Date
               .setExpiration(Date.from(expiration)) // Convert Instant to Date
               .setHeaderParam("kid", "my-key-id")
               .signWith(SignatureAlgorithm.HS256, SECRET_KEY.getBytes(StandardCharsets.UTF_8))
               .compact();
    
        return token;
    }

    private Claims extractAndValidateToken(String authHeader) {    
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Missing or invalid Authorization header");
        }
    
        String token = authHeader.substring(7); // Estrai il token dalla stringa dell'header Authorization
    
        try {
            // Implementazione della logica di convalida del token JWT
            Claims claims = validateToken(token);
            return claims;
        } catch (RuntimeException e) {
            // Gestione delle eccezioni di token non valido
            throw new RuntimeException("Invalid token", e);
        }
    }    
    
    public static Claims validateToken(String token) {
        try {
            Claims claims = Jwts.parser()
                                .setSigningKey(SECRET_KEY.getBytes(StandardCharsets.UTF_8))
                                .parseClaimsJws(token)
                                .getBody();
            return claims;
        } catch (SignatureException e) {
            throw new RuntimeException("Invalid JWT signature");
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            throw new RuntimeException("Expired JWT token");
        } catch (Exception e) {
            throw new RuntimeException("Invalid JWT token");
        }
    }
}