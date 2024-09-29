package dao;

import java.sql.Timestamp;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.Calendar;
import java.util.Date;
import java.sql.Types;
import java.time.LocalDateTime;

import entity.*;
import entity.CartaCredito.TipoCarta;
import entity.Prenotazione.StatoPrenotazione;
import entity.Transazione.TipoTransazione;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import entity.Utente.TipoUtente;

public class DAO {
    private Connection connection;

    public DAO(Connection connection) {
        this.connection = connection;
    }

    public String createAccount(Utente user) throws SQLException {
        String queryCheckExistingUsername = "SELECT COUNT(*) FROM Utenti WHERE username = ?";
        String queryCheckExistingQRCode = "SELECT COUNT(*) FROM Utenti WHERE qrcodeId = ?";
        String queryInsertUser = "INSERT INTO Utenti (qrcodeId, username, password, tipo_utente) VALUES (?, ?, ?, ?)";
    
        try (
            PreparedStatement statementCheckExistingUsername = connection.prepareStatement(queryCheckExistingUsername);
            PreparedStatement statementCheckExistingQRCode = connection.prepareStatement(queryCheckExistingQRCode);
            PreparedStatement statementInsertUser = connection.prepareStatement(queryInsertUser);
        ) {
            String qrcodeId = generateUniqueQRCode(); // Genera un QRCode univoco
    
            // Verifica se l'username è già presente nel database
            statementCheckExistingUsername.setString(1, user.getUsername());
            if (!isFieldUnique(user.getUsername(), statementCheckExistingUsername)) {
                // Se l'username non è univoco, restituisci null
                return null;
            }

            if (!isUsernameAcceptable(user.getUsername())) {
                return null;
            }

            if (!isPasswordAcceptable(user.getPassword())) {
                // la password non è corretta
                return null;
            }

            // Verifica se il QRCode è già presente nel database
            statementCheckExistingQRCode.setString(1, qrcodeId);
            if (isFieldUnique(qrcodeId, statementCheckExistingQRCode)) {
                // Inserisci il nuovo utente con il QRCode generato
                statementInsertUser.setString(1, qrcodeId);
                statementInsertUser.setString(2, user.getUsername());
                statementInsertUser.setString(3, user.getPassword());
                statementInsertUser.setObject(4, user.getTipoUtente().name(), Types.OTHER);
    
                int rowsAffected = statementInsertUser.executeUpdate();
                if (rowsAffected > 0) {
                    // Se la registrazione ha avuto successo, restituisci il QRCode
                    return qrcodeId;
                }
            }
        }
        // Se qualcosa va storto, restituisci null
        return null;
    }

    public boolean isUsernamePresent(String username) throws SQLException {
        String queryCheckExistingUsername = "SELECT COUNT(*) FROM Utenti WHERE username = ?";
        try (PreparedStatement statementCheckExistingUsername = connection.prepareStatement(queryCheckExistingUsername)) {
            statementCheckExistingUsername.setString(1, username);
            try (ResultSet resultSet = statementCheckExistingUsername.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    public boolean upgradeToPremium(String username) throws SQLException {
        String queryCheckUserType = "SELECT tipo_utente FROM Utenti WHERE username = ?";
        String queryUpgradeToPremium = "UPDATE Utenti SET tipo_utente = 'Premium' WHERE username = ? AND tipo_utente = 'Base'";
        boolean isUpgraded = false;
    
        try (PreparedStatement checkStatement = connection.prepareStatement(queryCheckUserType)) {
            checkStatement.setString(1, username);
    
            try (ResultSet resultSet = checkStatement.executeQuery()) {
                if (resultSet.next()) {
                    String currentUserType = resultSet.getString("tipo_utente");
    
                    // Se il tipo utente è "base", aggiorna a "premium"
                    if ("Base".equals(currentUserType)) {
                        try (PreparedStatement upgradeStatement = connection.prepareStatement(queryUpgradeToPremium)) {
                            upgradeStatement.setString(1, username);

                            int rowsUpdated = upgradeStatement.executeUpdate();
                            if (rowsUpdated > 0) {
                                isUpgraded = true;
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }
    
        return isUpgraded;
    }    

    public boolean downgradeToNonPremium(String username) throws SQLException {
        String queryDowngradeToBase = "UPDATE Utenti SET tipo_utente = 'Base' WHERE username = ? AND tipo_utente = 'Premium'";
        boolean isDowngraded = false;
    
        try (PreparedStatement downgradeStatement = connection.prepareStatement(queryDowngradeToBase)) {
            downgradeStatement.setString(1, username);
    
            int rowsUpdated = downgradeStatement.executeUpdate();
            if (rowsUpdated > 0) {
                isDowngraded = true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }

        return isDowngraded;
    }

    public boolean deleteAccount(String username) throws SQLException {
        String queryDeleteUser = "DELETE FROM Utenti WHERE username = ?";

        try (PreparedStatement statementDeleteUser = connection.prepareStatement(queryDeleteUser)) {
            statementDeleteUser.setString(1, username);

            int rowsAffected = statementDeleteUser.executeUpdate();

            if (rowsAffected > 0) {
                return true;
            } else {
                return false;
            }
        }
    }

    public String getQRCodeByUsername(String username) throws SQLException {
        String qrCode = null;
    
        // Query SQL per recuperare il QRCode dall'utente con lo username specificato
        String sql = "SELECT qrcodeId FROM Utenti WHERE username = ?";
    
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            // Imposta il parametro nella query preparata
            stmt.setString(1, username);
    
            // Esegue la query
            try (ResultSet rs = stmt.executeQuery()) {
                // Se esiste un risultato, assegna il valore del QRCode alla variabile qrCode
                if (rs.next()) {
                    qrCode = rs.getString("qrcodeId");
                }
            }
        } catch (SQLException e) {
            throw e;
        }
    
        return qrCode;
    }    

    public String getIdUtenteByVehicle(String qrCode) throws SQLException {
        String query = "SELECT id_utente FROM Veicoli WHERE id_utente = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, qrCode);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getString("id_utente");
                } else {
                    return null; // Nessun veicolo trovato con il QR code specificato
                }
            }
        }
    }

    public String getUsernameByQrcode(String qrcodeId) throws SQLException {
        String username = null;
        String sql = "SELECT username FROM utenti WHERE qrcodeid =?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, qrcodeId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    username = rs.getString("username");   
                }
            }
        } catch (SQLException e) {
            
            throw e;
        }
        return username;
    }

    public boolean createCreditCard(CartaCredito creditCard) throws SQLException {
        String queryInsertCard = "INSERT INTO CartaDiCredito (tipo_carta, numero_carta, data_scadenza, cvv, id_utente) VALUES (?,?,?,?,?)";
        try (PreparedStatement statementInsertCard = connection.prepareStatement(queryInsertCard);) {
            if (!isNumeroCartaAcceptable(creditCard.getNumeroCarta())) {
                
                return false;
            } else {
                if (!isCvvAcceptable(creditCard.getCvv())) {
                    
                    return false;
                } else {
                    if (!isDateAcceptable(creditCard.getDataScadenza())) {
                        
                        return false;
                    }

                    statementInsertCard.setObject(1, creditCard.getTipoCarta().name(), Types.OTHER);
                    statementInsertCard.setString(2, creditCard.getNumeroCarta());
                    statementInsertCard.setDate(3, creditCard.getDataScadenza());
                    statementInsertCard.setString(4, creditCard.getCvv());
                    statementInsertCard.setString(5, creditCard.getIdUtente());
    
                    // Print the SQL query with bound parameters
                    
    
                    int rowsAffected = statementInsertCard.executeUpdate();
                    
                    if (rowsAffected > 0) {
                        // Se la registrazione ha avuto successo, restituisci true
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            
            e.printStackTrace();
        }
        return false;
    }

    private boolean isUsernameAcceptable(String username) {
        if (username.length() < 8) {
            return false;
        }
        if (!Character.isLetter(username.charAt(0))) {
            return false;
        }
        return true;
    }

    private boolean isPasswordAcceptable(String password) {
        if (password.length() < 8) {
            return false;
        }

        boolean hasUpperCase = false;
        boolean hasLowerCase = false;
        boolean hasDigit = false;
        boolean hasSpecialChar = false;

        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) {
                hasUpperCase = true;
            }
            else {
                if (Character.isLowerCase(c)) {
                    hasLowerCase = true;
                } else {
                    if (Character.isDigit(c)) {
                        hasDigit = true;
                    }
                    else {
                        if (c!=' ') {
                            hasSpecialChar = true;
                        }
                    }
                }
            }
        }
        return hasUpperCase && hasLowerCase && hasDigit && hasSpecialChar;
    }

    private boolean isFieldUnique(String fieldValue, PreparedStatement statement) throws SQLException {
        try (ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next() && resultSet.getInt(1) == 0;
        }
    }
    

    private String generateUniqueQRCode() {
        String qrcodeId;
        boolean unique = false;
        // Genera un QRCode univoco utilizzando UUID fino a quando non è univoco nel database
        do {
            qrcodeId = UUID.randomUUID().toString();
            try {
                unique = isQRCodeUnique(qrcodeId);
            } catch (SQLException e) {
                // Gestisci l'eccezione o stampala per il debug
                e.printStackTrace();
                // Puoi anche decidere di terminare l'iterazione e ritornare null in caso di eccezione
                return null;
            }
        } while (!unique);
        return qrcodeId;
    }
    
    private boolean isQRCodeUnique(String qrcodeId) throws SQLException {
        String queryCheckUniqueQRCode = "SELECT COUNT(*) FROM Utenti WHERE qrcodeId = ?";
        try (PreparedStatement statement = connection.prepareStatement(queryCheckUniqueQRCode)) {
            statement.setString(1, qrcodeId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) == 0;
            }
        }
    } 

    private boolean isNumeroCartaAcceptable(String numeroCarta) {
        if (numeroCarta.length() != 16) {
            // il numero della carta non ha 16 caratteri
            return false;
        }
        else {
            if (!numeroCarta.matches("\\d+")) {
                return false;
            }
        }

        int sum = 0;
        boolean alternate = false;
        for (int i=numeroCarta.length()-1; i>=0; i--) {
            int digit = Integer.parseInt(numeroCarta.substring(i, i + 1));
            if (alternate) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }
            sum += digit;
            alternate = !alternate;
        }
        return sum % 10 == 0;
    }

    private boolean isDateAcceptable(Date expiryDate) {
        Calendar currentDate = Calendar.getInstance();
        Calendar expiryCalendar = Calendar.getInstance();
        expiryCalendar.setTime(expiryDate);
        expiryCalendar.set(Calendar.DAY_OF_MONTH, expiryCalendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        return expiryCalendar.after(currentDate);
    }

    private boolean isCvvAcceptable(String cvv) {
        if (!cvv.matches("\\d+")) {
            // il cvv contiene caratteri non numerici
            return false;
        }
        else {
            if (cvv.length() != 3 && cvv.length() != 4) {
                // il cvv ha lunghezza minore di 3 o maggiore di 4
                return false;
            }
            return true;
        }
    }

    public Utente getUserByUsernameAndPassword(String username, String password) throws SQLException {
        Utente user = null;
        String query = "SELECT * FROM Utenti WHERE username = ? AND password = ?";
        
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, username);
            statement.setString(2, password);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    user = new Utente(
                        resultSet.getString("qrcodeId"),
                        resultSet.getString("username"),
                        resultSet.getString("password"),
                        Utente.TipoUtente.valueOf(resultSet.getString("tipo_utente"))
                    );
                }
            }
        }
    
        return user;
    }

    public boolean modificaCosti(String costoSosta, String costoRicarica) throws SQLException {
        String query = "UPDATE costi SET costo_euro_ora = ?, costo_euro_kw = ? WHERE id_costo = 1"; // Assuming a single row table for prices
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            BigDecimal costoSostaBigDecimal = new BigDecimal(costoSosta);
            BigDecimal costoRicaricaBigDecimal = new BigDecimal(costoRicarica);
            
            stmt.setBigDecimal(1, costoSostaBigDecimal);
            stmt.setBigDecimal(2, costoRicaricaBigDecimal);
            int rowsUpdated = stmt.executeUpdate();
            return rowsUpdated > 0;
        }
    }

    public String getCurrentCostoSosta() throws SQLException {
        String query = "SELECT costo_euro_ora FROM Costi WHERE id_costo = 1";
        try (PreparedStatement stmt = connection.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getString("costo_euro_ora");
            }
        }

        throw new SQLException("Current cost of parking (costo_euro_ora) not found in database.");
    }

    public String getCurrentCostoRicarica() throws SQLException {
        String query = "SELECT costo_euro_kw FROM Costi WHERE id_costo = 1";
        try (PreparedStatement stmt = connection.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getString("costo_euro_kw");
            }
        }
        // Handle case where no result is found or other errors
        throw new SQLException("Current cost of charging (costo_euro_kw) not found in database.");
    }

    public List<Transazione> getFilteredTransactions(LocalDateTime dataOraInizio, LocalDateTime dataOraFine, List<String> filterArray) throws SQLException {
        
        String query = "SELECT * FROM Transazioni WHERE timestamp_transazione >=? AND timestamp_transazione <=?";


        if (filterArray != null && !filterArray.isEmpty()) {
            List<String> tipoTransazioneFilters = new ArrayList<>();
            List<String> idUtenteFilters = new ArrayList<>();
            
            for (String filter : filterArray) {
                
                switch (filter) {
                    case "sosta":
                        tipoTransazioneFilters.add("'sosta'");
                        break;
                    case "ricarica":
                        tipoTransazioneFilters.add("'ricarica'");
                        break;
                    case "abbonamento":
                        tipoTransazioneFilters.add("'premium'");
                        break;
                    case "base":
                        idUtenteFilters.add("id_utente IN (SELECT qrcodeId FROM Utenti WHERE tipo_utente = 'Base')");
                        break;
                    case "premium":
                        idUtenteFilters.add("id_utente IN (SELECT qrcodeId FROM Utenti WHERE tipo_utente = 'Premium')");
                        break;
                }
            }

            if (!tipoTransazioneFilters.isEmpty()) {
                query += " AND tipo_transazione IN (" + String.join(", ", tipoTransazioneFilters) + ")";
            }

            if (!idUtenteFilters.isEmpty()) {
                query += " AND (" + String.join(" OR ", idUtenteFilters) + ")";
            }
        }
        
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            // Converti i LocalDateTime in oggetti Timestamp
            java.sql.Timestamp inicio = java.sql.Timestamp.valueOf(dataOraInizio);
            java.sql.Timestamp fine = java.sql.Timestamp.valueOf(dataOraFine);
        
            stmt.setTimestamp(1, inicio);
            stmt.setTimestamp(2, fine);
        
            try (ResultSet rs = stmt.executeQuery()) {
                List<Transazione> transazioni = new ArrayList<>();
                while (rs.next()) {
                    Transazione transazione = new Transazione();
                    transazione.setId_transazione(rs.getInt("id_transazione"));
                    transazione.setId_utente(rs.getString("id_utente"));
                    transazione.setId_veicolo(rs.getInt("id_veicolo"));
                    transazione.setTipo_transazione(TipoTransazione.valueOf(rs.getString("tipo_transazione")));
                    transazione.setImporto(rs.getFloat("importo"));
                    transazione.setData_transazione(rs.getTimestamp("timestamp_transazione").toLocalDateTime());
                    transazioni.add(transazione);
                }
                return transazioni;
            }
        }
    }

    public String getUserIdByUsername(String username) throws SQLException {
        String query = "SELECT qrcodeid FROM Utenti WHERE username = ?";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, username);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getString("qrcodeId");
                }
            }
        }
        return "Utente non trovato";
    }
    
    public int getIdVeicoloInsideParkingByTarga(String targa) throws SQLException {
        String query = "SELECT id_veicolo FROM Veicoli WHERE targa = ? AND dentroParcheggio = true";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, targa);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("id_veicolo");
                }
            }
        }
        return -1;
    }

    public int getIdVeicoloDentroParcheggioByTarga(String targa) throws SQLException {
        String query = "SELECT id_veicolo FROM Veicoli WHERE targa = ? AND dentroParcheggio = true";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, targa);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("id_veicolo");
                }
            }
        }
        return -1;
    }    

    public int getIdVeicoloByIdUtente(String idUtente) throws SQLException {
        String query = "SELECT id_veicolo FROM Veicoli WHERE id_utente = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, idUtente);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("id_veicolo");
                }
            }
        }
        return -1;
    }

    public boolean isCreditCardPresent(String idUtente) throws SQLException {
        String query = "SELECT * FROM cartadicredito WHERE id_utente = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, idUtente);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return true;
                }
                return false;
            }
        }
    }

    public boolean creaIscrizione(Transazione transazione) throws SQLException {
        String query = "INSERT INTO transazioni (id_utente, tipo_transazione, importo, timestamp_transazione) VALUES (?, ?, ?, ?)";
        Timestamp tempo = Timestamp.valueOf(transazione.getData_transazione());
        try (PreparedStatement statementInsertIscrizione = connection.prepareStatement(query)) {
            statementInsertIscrizione.setString(1, transazione.getId_utente());
            statementInsertIscrizione.setString(2, transazione.getTipo_transazione().name());
            statementInsertIscrizione.setDouble(3, transazione.getImporto());
            statementInsertIscrizione.setTimestamp(4, tempo);

            int rowsAffected = statementInsertIscrizione.executeUpdate();
            if (rowsAffected > 0) {
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean updatePrenotazione(int id, boolean multaPagata, StatoPrenotazione stato) throws SQLException {
        String query = "UPDATE prenotazioni SET multa_pagata = ?, stato = ? WHERE id_prenotazione = ?";
        try (PreparedStatement statementUpdatePrenotazione = connection.prepareStatement(query)) {
            statementUpdatePrenotazione.setBoolean(1, multaPagata);
            statementUpdatePrenotazione.setObject(2, stato.name(), Types.OTHER);
            statementUpdatePrenotazione.setInt(3, id);
    
            int rowsAffected = statementUpdatePrenotazione.executeUpdate();
            if (rowsAffected > 0) {
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean creaPagamento(Transazione transazione) throws SQLException {
        String query = "INSERT INTO transazioni (id_utente, id_veicolo, tipo_transazione, importo, timestamp_transazione) VALUES (?, ?, ?, ?, ?)";
        Timestamp tempo = Timestamp.valueOf(transazione.getData_transazione());
        try (PreparedStatement statementInsertPagamento = connection.prepareStatement(query)) {
            statementInsertPagamento.setString(1, transazione.getId_utente());
            if (transazione.getId_veicolo() != 0) {
                statementInsertPagamento.setInt(2, transazione.getId_veicolo());
            } else {
                statementInsertPagamento.setNull(2, Types.INTEGER);
            }
            statementInsertPagamento.setObject(3, transazione.getTipo_transazione().name(), Types.OTHER);
            statementInsertPagamento.setDouble(4, transazione.getImporto());
            statementInsertPagamento.setTimestamp(5, tempo);
    
            int rowsAffected = statementInsertPagamento.executeUpdate();

            return rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SQLException("Errore durante l'inserimento della transazione", e);
        }
    }
    
    public boolean parkCar(String posto) {
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT occupato FROM Parcheggio WHERE posto_auto = ?");
            statement.setString(1, posto);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                boolean occupato = resultSet.getBoolean("occupato");
                if (!occupato) {
                    // Il parcheggio è disponibile, lo occupa
                    PreparedStatement updateStatement = connection.prepareStatement("UPDATE Parcheggio SET occupato = true WHERE posto_auto = ?");
                    updateStatement.setString(1, posto);
                    updateStatement.executeUpdate();
                    return true;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean leaveParking(String posto) {
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT occupato FROM Parcheggio WHERE posto_auto = ?");
            statement.setString(1, posto);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                boolean occupato = resultSet.getBoolean("occupato");
                if (occupato) {
                    // Il parcheggio è occupato, lo libera
                    PreparedStatement updateStatement = connection.prepareStatement("UPDATE Parcheggio SET occupato = false WHERE posto_auto = ?");
                    updateStatement.setString(1, posto);
                    updateStatement.executeUpdate();
                    return true;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public Map<String, Boolean> statoOccupazioneParcheggi() {
        Map<String, Boolean> occupazioneParcheggi = new HashMap<>();
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT posto_auto, occupato FROM Parcheggio");
            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                String posto = resultSet.getString("posto_auto");
                boolean occupato = resultSet.getBoolean("occupato");
                occupazioneParcheggi.put(posto, occupato);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return occupazioneParcheggi;
    }

    /*public List<Parcheggio> getPostiAutoOccupati() throws SQLException {
        List<Parcheggio> postiOccupati = new ArrayList<>();
        String query = "SELECT * FROM Parcheggio WHERE occupato = true";

        try (PreparedStatement statement = connection.prepareStatement(query);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                Parcheggio parcheggio = mapRowToParcheggio(resultSet);
                postiOccupati.add(parcheggio);
            }
        }

        return postiOccupati;
    }*/

    /*public List<Parcheggio> getPostiAutoDisponibili() {
        List<Parcheggio> postiDisponibili = new ArrayList<>();
        String query = "SELECT * FROM Parcheggio WHERE occupato = false";

        try (PreparedStatement statement = connection.prepareStatement(query);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                Parcheggio parcheggio = mapRowToParcheggio(resultSet);
                postiDisponibili.add(parcheggio);
            }
        } catch (SQLException e) {
            // Gestisci l'eccezione o stampala per il debug
            e.printStackTrace();
            // Puoi anche decidere di lanciare l'eccezione per farla gestire da chi chiama il metodo
            throw new RuntimeException("Errore durante il recupero dei posti auto disponibili", e);
        }

        return postiDisponibili;
    }*/

    public int findVeicoloByTarga(String targa) throws SQLException {
        String query = "SELECT id FROM veicoli WHERE targa = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, targa);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                } else {
                    throw new SQLException("Veicolo non trovato per la targa: " + targa);
                }
            }
        }
    }
    

    public boolean PostoDisponibile(String posto) throws SQLException {
        String query = "SELECT occupato FROM Parcheggio WHERE posto_auto = ?";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, posto);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return !resultSet.getBoolean("occupato");
                }
            }
        }

        return false; // Se il posto non esiste nel database o c'è un errore nella query, ritorna false
    }

    public List<Parcheggio> getAllParcheggi() throws SQLException {
        String query = "SELECT * FROM Parcheggio";
    
        List<Parcheggio> parcheggiList = new ArrayList<>();
        
        try (PreparedStatement statement = connection.prepareStatement(query);
             ResultSet resultSet = statement.executeQuery()) {
            
            while (resultSet.next()) {
                Parcheggio parcheggio = new Parcheggio();
                Integer idParcheggio = resultSet.getInt("id_posto_auto");
                parcheggio.setId_parcheggio(idParcheggio!= null? idParcheggio.intValue() : null);
                parcheggio.setOccupato(resultSet.getBoolean("occupato"));
                parcheggio.setRiservato(resultSet.getBoolean("riservato"));
                Integer idVeicolo = resultSet.getInt("id_veicolo");
                parcheggio.setId_veicolo(idVeicolo!= null? idVeicolo.intValue() : null);
                
                parcheggiList.add(parcheggio);
            }
        } catch (SQLException e) {
            throw new SQLException("Errore durante l'esecuzione della query", e);
        }
        
        return parcheggiList;
    }

    public List<Parcheggio> getAllParcheggiRiservati() throws Exception {
        String query = "SELECT * FROM Parcheggio WHERE riservato = true";

        List<Parcheggio> parcheggiList = new ArrayList<>();
        
        try (PreparedStatement statement = connection.prepareStatement(query);
             ResultSet resultSet = statement.executeQuery()) {
            
            while (resultSet.next()) {
                Parcheggio parcheggio = new Parcheggio();
                Integer idParcheggio = resultSet.getInt("id_posto_auto");
                parcheggio.setId_parcheggio(idParcheggio!= null? idParcheggio.intValue() : null);
                parcheggio.setOccupato(resultSet.getBoolean("occupato"));
                parcheggio.setRiservato(resultSet.getBoolean("riservato"));
                Integer idVeicolo = resultSet.getInt("id_veicolo");
                parcheggio.setId_veicolo(idVeicolo!= null? idVeicolo.intValue() : null);
                
                parcheggiList.add(parcheggio);
            }
        }
        
        return parcheggiList;
    }

    public List<Prenotazione> getActivePrenotazioni() throws Exception {
        List<Prenotazione> prenotazioni = new ArrayList<>();

        String query = "SELECT id_prenotazione, id_utente, id_veicolo, tempo_arrivo, durata_permanenza, carta_credito, stato, multa_pagata, id_posto_auto " +
                       "FROM Prenotazioni " +
                       "WHERE stato = 'attiva';";

        try (PreparedStatement statement = connection.prepareStatement(query);
            ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                Prenotazione prenotazione = new Prenotazione();
                prenotazione.setId_prenotazione(resultSet.getInt("id_prenotazione"));
                prenotazione.setId_utente(resultSet.getString("id_utente"));
                prenotazione.setId_veicolo(resultSet.getInt("id_veicolo"));
                prenotazione.setTempo_arrivo(resultSet.getTimestamp("tempo_arrivo").toLocalDateTime());
                prenotazione.setDurata_permanenza(resultSet.getInt("durata_permanenza"));
                prenotazione.setCarta_credito(resultSet.getInt("carta_credito"));
                prenotazione.setStato(StatoPrenotazione.valueOf(resultSet.getString("stato")));
                prenotazione.setMulta_pagata(resultSet.getBoolean("multa_pagata"));
                prenotazione.setId_posto_auto(resultSet.getInt("id_posto_auto"));
                prenotazioni.add(prenotazione);
            }

            resultSet.close();
            statement.close();
        }

        return prenotazioni;
    }

    public List<Prenotazione> fetchActiveReservationsWithNoVehicle() throws SQLException {
        List<Prenotazione> prenotazioni = new ArrayList<>();
    
        String query = "SELECT id_prenotazione, id_utente, id_veicolo, tempo_arrivo, durata_permanenza, carta_credito, stato, multa_pagata, id_posto_auto " +
                       "FROM Prenotazioni " +
                       "WHERE stato = 'attiva' AND id_veicolo IS NULL;";
    
        try (PreparedStatement statement = connection.prepareStatement(query);
             ResultSet resultSet = statement.executeQuery()) {
    
            while (resultSet.next()) {
                Prenotazione prenotazione = new Prenotazione();
                prenotazione.setId_prenotazione(resultSet.getInt("id_prenotazione"));
                prenotazione.setId_utente(resultSet.getString("id_utente"));
                prenotazione.setTempo_arrivo(resultSet.getTimestamp("tempo_arrivo").toLocalDateTime());
                prenotazione.setDurata_permanenza(resultSet.getInt("durata_permanenza"));
                prenotazione.setCarta_credito(resultSet.getInt("carta_credito"));
                prenotazione.setStato(StatoPrenotazione.valueOf(resultSet.getString("stato")));
                prenotazione.setMulta_pagata(resultSet.getBoolean("multa_pagata"));
                prenotazione.setId_posto_auto(resultSet.getInt("id_posto_auto"));
                prenotazioni.add(prenotazione);
            }
    
            resultSet.close();
            statement.close();
        }
    
        return prenotazioni;
    }   
    
    public List<Prenotazione> fetchActiveReservationsWithVehicle() throws SQLException {
        List<Prenotazione> prenotazioni = new ArrayList<>();
        
        String query = "SELECT id_prenotazione, id_utente, id_veicolo, tempo_arrivo, durata_permanenza, carta_credito, stato, multa_pagata, id_posto_auto " +
                       "FROM Prenotazioni " +
                       "WHERE stato = 'attiva' AND id_veicolo IS NOT NULL;";
        
        try (PreparedStatement statement = connection.prepareStatement(query);
             ResultSet resultSet = statement.executeQuery()) {
        
            while (resultSet.next()) {
                Prenotazione prenotazione = new Prenotazione();
                prenotazione.setId_prenotazione(resultSet.getInt("id_prenotazione"));
                prenotazione.setId_utente(resultSet.getString("id_utente"));
                prenotazione.setId_veicolo(resultSet.getInt("id_veicolo"));
                prenotazione.setTempo_arrivo(resultSet.getTimestamp("tempo_arrivo").toLocalDateTime());
                prenotazione.setDurata_permanenza(resultSet.getInt("durata_permanenza"));
                prenotazione.setCarta_credito(resultSet.getInt("carta_credito"));
                prenotazione.setStato(StatoPrenotazione.valueOf(resultSet.getString("stato")));
                prenotazione.setMulta_pagata(resultSet.getBoolean("multa_pagata"));
                prenotazione.setId_posto_auto(resultSet.getInt("id_posto_auto"));
                prenotazioni.add(prenotazione);
            }
        
        }
        
        return prenotazioni;
    }      

    public Integer findAvailableSpot(LocalDateTime tempoArrivo, long durataPermanenza) throws SQLException {
        String query = "SELECT p.id_posto_auto " +
                       "FROM Parcheggio p " +
                       "WHERE NOT EXISTS (" +
                       "    SELECT 1 " +
                       "    FROM Prenotazioni pr " +
                       "    WHERE pr.id_posto_auto = p.id_posto_auto " +
                       "    AND stato = 'attiva' " +
                       "    AND ((pr.tempo_arrivo <= ? AND (pr.tempo_arrivo + INTERVAL '1 minute' * pr.durata_permanenza) >= ?) " +
                       "    OR (pr.tempo_arrivo < ? AND (pr.tempo_arrivo + INTERVAL '1 minute' * pr.durata_permanenza) >= ?) " +
                       "    OR (pr.tempo_arrivo <= ? AND (pr.tempo_arrivo + INTERVAL '1 minute' * pr.durata_permanenza) > ?))" +
                       ") " +
                       "ORDER BY p.id_posto_auto " +
                       "LIMIT 1";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            Timestamp tempoArrivoTimestamp = Timestamp.valueOf(tempoArrivo);
            LocalDateTime tempoPartenza = tempoArrivo.plusMinutes(durataPermanenza);
            Timestamp tempoPartenzaTimestamp = Timestamp.valueOf(tempoPartenza);

            statement.setTimestamp(1, tempoArrivoTimestamp);
            statement.setTimestamp(2, tempoArrivoTimestamp);
            statement.setTimestamp(3, tempoArrivoTimestamp);
            statement.setTimestamp(4, tempoPartenzaTimestamp);
            statement.setTimestamp(5, tempoArrivoTimestamp);
            statement.setTimestamp(6, tempoPartenzaTimestamp);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    int idPostoAuto = resultSet.getInt("id_posto_auto");
                    return idPostoAuto;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return -1; // Nessun posto disponibile
    }  

    public boolean isSpotAvailable(LocalDateTime tempoArrivo, long durataPermanenza) throws SQLException {
        String query = "SELECT COUNT(*) AS prenotazioni " +
                       "FROM Prenotazioni " +
                       "WHERE id_parcheggio = ? " +
                       "AND stato = 'attiva' " +
                       "AND ((tempo_arrivo <= ? AND (tempo_arrivo + durata_permanenza * interval '1 minute') >= ?) " +
                       "OR (tempo_arrivo < ? AND (tempo_arrivo + durata_permanenza * interval '1 minute') >= ?) " +
                       "OR (tempo_arrivo <= ? AND (tempo_arrivo + durata_permanenza * interval '1 minute') > ?))";
    
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            Timestamp tempoArrivoTimestamp = Timestamp.valueOf(tempoArrivo);
            LocalDateTime tempoPartenza = tempoArrivo.plusMinutes(durataPermanenza);
            Timestamp tempoPartenzaTimestamp = Timestamp.valueOf(tempoPartenza);
    
            statement.setTimestamp(2, tempoArrivoTimestamp);
            statement.setTimestamp(3, tempoArrivoTimestamp);
            statement.setTimestamp(4, tempoArrivoTimestamp);
            statement.setTimestamp(5, tempoPartenzaTimestamp);
            statement.setTimestamp(6, tempoArrivoTimestamp);
            statement.setTimestamp(7, tempoPartenzaTimestamp);
    
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    // Se il numero di prenotazioni che si sovrappongono è 0, allora il posto è disponibile
                    return resultSet.getInt("prenotazioni") == 0;
                }
            }
        }
    
        return false; // Se il posto non esiste nel database o c'è un errore nella query, ritorna false
    }

    public boolean createPrenotazione(Prenotazione prenotazione) throws SQLException {
        String query = "INSERT INTO Prenotazioni (id_utente, id_posto_auto, tempo_arrivo, durata_permanenza, carta_credito, stato, multa_pagata, timestamp_creazione) VALUES (?,?,?,?,?,?,?,?)";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, prenotazione.getId_utente());
            stmt.setInt(2, prenotazione.getId_posto_auto());
            stmt.setTimestamp(3, Timestamp.valueOf(prenotazione.getTempo_arrivo()));
            stmt.setLong(4, prenotazione.getDurata_permanenza());
            stmt.setInt(5, prenotazione.getCarta_credito());
            stmt.setObject(6, prenotazione.getStato().name(), Types.OTHER);
            stmt.setBoolean(7, prenotazione.isMulta_pagata());
            stmt.setTimestamp(8, Timestamp.valueOf(prenotazione.getTimestamp_creazione()));
            return stmt.executeUpdate() > 0; // restituisce true se l'inserimento è stato eseguito con successo
        }
    }

    public List<Transazione> getTransazioniByQrCode(String qrCode) throws SQLException {
        List<Transazione> transazioni = new ArrayList<>();
        String query = "SELECT * FROM Transazioni WHERE id_utente =?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, qrCode);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                TipoTransazione tipoTransazione = TipoTransazione.valueOf(rs.getString("tipo_transazione"));
                float importo = rs.getFloat("importo");
                LocalDateTime timestampTransazione = rs.getTimestamp("timestamp_transazione").toLocalDateTime();
    
                Transazione transazione = new Transazione(tipoTransazione, importo, timestampTransazione);
                transazioni.add(transazione);
            }
        }
        return transazioni;
    }

    public List<Prenotazione> getPrenotazioniAttiveByQrCode(String qrCode) throws SQLException {
        List<Prenotazione> prenotazioni = new ArrayList<>();
        String query = "SELECT * FROM prenotazioni WHERE id_utente =? AND stato = 'attiva' AND id_veicolo IS NULL";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, qrCode);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                int idPrenotazione = rs.getInt("id_prenotazione");
                int idPostoAuto = rs.getInt("id_posto_auto");
                LocalDateTime tempoArrivo = rs.getTimestamp("tempo_arrivo").toLocalDateTime();
                int durataPermanenza = rs.getInt("durata_permanenza");
                LocalDateTime tempoUscita = tempoArrivo.plusMinutes(durataPermanenza);
                LocalDateTime timestampCreazione = rs.getTimestamp("timestamp_creazione").toLocalDateTime();
    
                Prenotazione prenotazione = new Prenotazione(idPrenotazione, idPostoAuto, tempoArrivo, durataPermanenza, tempoUscita, timestampCreazione);
                prenotazioni.add(prenotazione);
            }
        } catch(SQLException e) {
            throw new SQLException("Errore durante l'esecuzione della query");
        }
        return prenotazioni;
    }

    // Delete
    public boolean deletePrenotazione(int idPrenotazione) throws SQLException {
        String query = "DELETE FROM Prenotazioni WHERE id_prenotazione = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, idPrenotazione);

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                return true;
            } else {
                return false;
            }
        }
    }

    public List<String> getVeicoliNelParcheggio(String qrCode) throws SQLException {
        String query = "SELECT targa " +
                       "FROM Veicoli WHERE id_utente =? AND dentroParcheggio = true";
        List<String> targhe = new ArrayList<>();
    
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, qrCode);
    
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    targhe.add(rs.getString("targa"));
                }
            }
        }
    
        return targhe;
    }

    public boolean hasVehiclesInParking(String qrCode) throws SQLException {
        String query = "SELECT COUNT(*) FROM Veicoli WHERE id_utente = ? AND dentroParcheggio = true";
        
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, qrCode);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt(1);
                    return count > 0;
                }
            }
        }
        
        return false;
    }    

    public List<Ricarica> getRicaricheOrderByData() throws SQLException {
        List<Ricarica> ricariche = new ArrayList<>();
    
        String query = "SELECT * FROM ricariche WHERE effettuata = FALSE ORDER BY timestamp_creazione ASC";
    
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Ricarica ricarica = new Ricarica();
    
                    ricarica.setDurataRicarica(rs.getInt("durataRicarica"));
                    ricarica.setTimestampCreazione(rs.getTimestamp("timestamp_creazione").toLocalDateTime());
                    ricarica.setEffettuata(rs.getBoolean("effettuata")); // add this line to set effettuata
    
                    ricariche.add(ricarica);
                }
            }
        } catch (SQLException e) {
            throw new SQLException("Errore durante la lettura delle ricariche", e);
        }
    
        return ricariche;
    }

    public TipoUtente getUserTypeByToken(String token) throws SQLException {
        String query = "SELECT tipo_utente FROM utenti WHERE token = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, token);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String tipoUtenteStr = rs.getString("tipo_utente");
                return TipoUtente.valueOf(tipoUtenteStr);
            } else {
                throw new SQLException("Token non valido");
            }
        }
    }

    public boolean upgradeToCard(CartaCredito newCard) throws SQLException {
        String query = "UPDATE cartadicredito SET tipo_carta =?, numero_carta =?, data_scadenza =?, cvv =? WHERE id_utente =?";
        
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            if (!isNumeroCartaAcceptable(newCard.getNumeroCarta())) {
                
                return false;
            } else {
                if (!isCvvAcceptable(newCard.getCvv())) {
                    
                    return false;
                } else {
                    if (!isDateAcceptable(newCard.getDataScadenza())) {
                        
                        return false;
                    }

                    stmt.setObject(1, newCard.getTipoCarta(), Types.OTHER);
                    stmt.setString(2, newCard.getNumeroCarta());
                    stmt.setDate(3, newCard.getDataScadenza());
                    stmt.setString(4, newCard.getCvv());
                    stmt.setString(5, newCard.getIdUtente());
                    int rowsAffected = stmt.executeUpdate();
                    
                    return rowsAffected > 0;
                }
            }
        } catch (SQLException e) {
            
            throw e;
        }
    }

    public void saveRicarica(Ricarica newRicarica) {
        String query = "INSERT INTO Ricariche (id_veicolo, durataRicarica, percentualeRicarica, id_utente, timestamp_creazione, effettuata) VALUES (?,?,?,?,?,?)";
        
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, newRicarica.getIdVeicolo());
            stmt.setInt(2, newRicarica.getDurataRicarica());
            stmt.setInt(3, newRicarica.getPercentualeRicarica());
            stmt.setString(4, newRicarica.getId_utente());
            stmt.setTimestamp(5, Timestamp.valueOf(newRicarica.getTimestampCreazione()));
            stmt.setBoolean(6, false);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Ricarica> getAllRicariche() throws SQLException {
        List<Ricarica> ricariche = new ArrayList<>();
        String query = "SELECT r.id_ricarica, r.id_veicolo, r.percentualeRicarica, r.durataRicarica, r.id_utente, r.timestamp_creazione, r.effettuata, p.id_posto_auto " +
                       "FROM Ricariche r " +
                       "JOIN Parcheggio p ON r.id_veicolo = p.id_veicolo " +
                       "WHERE r.effettuata = false " +
                       "AND r.iniziata = false " +
                       "ORDER BY r.timestamp_creazione ASC"; // Ordina per timestamp_creazione in ordine crescente
        
        try (PreparedStatement statement = connection.prepareStatement(query);
             ResultSet resultSet = statement.executeQuery()) {
        
            while (resultSet.next()) {
                Ricarica ricarica = new Ricarica();
                ricarica.setIdRicarica(resultSet.getInt("id_ricarica"));
                ricarica.setIdVeicolo(resultSet.getInt("id_veicolo"));
                ricarica.setPercentualeRicarica(resultSet.getInt("percentualeRicarica"));
                ricarica.setDurataRicarica(resultSet.getInt("durataRicarica"));
                ricarica.setId_utente(resultSet.getString("id_utente"));
                ricarica.setTimestampCreazione(resultSet.getTimestamp("timestamp_creazione").toLocalDateTime());
                ricarica.setEffettuata(resultSet.getBoolean("effettuata"));
                ricarica.setIdPostoAuto(resultSet.getInt("id_posto_auto")); // Aggiungi id_posto_auto
                ricariche.add(ricarica);
            }
        }
        
        return ricariche;
    }    
      
    public Costi getCosti() throws SQLException {
        String query = "SELECT * FROM costi";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Costi costi = new Costi();
                    costi.setCostoEuroKw(rs.getDouble("costo_euro_kw"));
                    costi.setCostoEuroOra(rs.getDouble("costo_euro_ora"));
                    
                    return costi;
                } else {
                    throw new SQLException("Costi non trovati");
                }
            }
        }
    }

    public Ricarica updateRicaricaEffettuata(int idRicarica, boolean effettuata) throws SQLException {
        String query = "UPDATE ricariche SET effettuata = ? WHERE id_ricarica = ?";
        String queryGetRicarica = "SELECT durataRicarica, id_veicolo, id_utente FROM ricariche WHERE id_ricarica = ?";
    
        try (PreparedStatement stmt = connection.prepareStatement(query);
             PreparedStatement stmtGetRicarica = connection.prepareStatement(queryGetRicarica)) {
            stmt.setBoolean(1, effettuata);
            stmt.setInt(2, idRicarica);
    
            stmt.executeUpdate();
    
            stmtGetRicarica.setInt(1, idRicarica);
            try (ResultSet rs = stmtGetRicarica.executeQuery()) {
                if (rs.next()) {
                    Ricarica ricarica = new Ricarica();
                    ricarica.setDurataRicarica(rs.getInt("durataRicarica"));
                    ricarica.setIdVeicolo(rs.getInt("id_veicolo"));
                    ricarica.setId_utente(rs.getString("id_utente"));
                    return ricarica;
                } else {
                    throw new SQLException("Ricarica non trovata");
                }
            }
        }
    }

    public String getIdUtenteByIdRicarica(int idRicarica) throws SQLException {
        String query = "SELECT r.id_utente, v.targa " +
                       "FROM ricariche r " +
                       "JOIN veicoli v ON r.id_veicolo = v.id_veicolo " +
                       "WHERE r.id_ricarica = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, idRicarica);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String idUtente = rs.getString("id_utente");
                    String targa = rs.getString("targa");
                    return "{\"id_utente\": \"" + idUtente + "\", \"targa\": \"" + targa + "\"}";
                } else {
                    return null; // or throw an exception if you prefer
                }
            }
        }
    }

    public void updateVeicoloAndTempoRicaricaEffettiva(int idVeicolo, int capacita, String modello, int idRicarica, int durataRicarica) throws SQLException {
        String queryVeicoli = "UPDATE veicoli SET capacita_batteria =?, modello =? WHERE id_veicolo =?";
        String queryRicariche = "UPDATE ricariche SET durataRicarica =?, iniziata = true WHERE id_ricarica =?";
        
        try (PreparedStatement stmtVeicoli = connection.prepareStatement(queryVeicoli);
             PreparedStatement stmtRicariche = connection.prepareStatement(queryRicariche)) {
            stmtVeicoli.setInt(1, capacita);
            stmtVeicoli.setString(2, modello);
            stmtVeicoli.setInt(3, idVeicolo);
            
            int rowsUpdatedVeicoli = stmtVeicoli.executeUpdate();
            
            if (rowsUpdatedVeicoli == 0) {
                throw new SQLException("Veicolo non trovato");
            }
            
            stmtRicariche.setInt(1, durataRicarica);
            stmtRicariche.setInt(2, idRicarica);
            
            int rowsUpdatedRicariche = stmtRicariche.executeUpdate();
            
            if (rowsUpdatedRicariche == 0) {
                throw new SQLException("Ricarica non trovata");
            }
        }
    }

    public CartaCredito getCardByQRCode(String idUtente) throws SQLException {
        String query = "SELECT id_carta, tipo_carta, numero_carta, data_scadenza, cvv, id_utente FROM cartadicredito WHERE id_utente = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, idUtente);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    CartaCredito carta = new CartaCredito();
                    carta.setIdCarta(rs.getInt("id_carta"));
                    carta.setTipoCarta(TipoCarta.valueOf(rs.getString("tipo_carta"))); // <--- Corretto
                    carta.setNumeroCarta(rs.getString("numero_carta"));
                    carta.setDataScadenza(rs.getDate("data_scadenza"));
                    carta.setCvv(rs.getString("cvv"));
                    carta.setIdUtente(rs.getString("id_utente"));
                    
                    return carta;
                } else {
                    throw new SQLException("Credit card not found for user " + idUtente);
                }
            }
        }
    }

    public int getDurataPermanenza(String qrCode, LocalDateTime currentTime) throws SQLException {
        // Query per controllare se l'utente ha una prenotazione attiva
        String query = "SELECT durata_permanenza FROM Prenotazioni " +
                       "WHERE id_utente =? " +
                       "AND stato = 'attiva' " +
                       "AND tempo_arrivo <=? " +
                       "AND ? <= tempo_arrivo + interval '15 minutes'";
        
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, qrCode);
            statement.setObject(2, Timestamp.valueOf(currentTime)); // currentTime
            statement.setObject(3, Timestamp.valueOf(currentTime)); // currentTime
            
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("durata_permanenza");
                } else {
                    return -1; // Nessuna prenotazione attiva trovata
                }
            }
        }
    }

    public int[] getParkingSpotForPremiumUser(String qrCode, LocalDateTime currentTime) throws SQLException {
        // Query per controllare se l'utente è premium
        String query = "SELECT tipo_utente FROM Utenti WHERE qrcodeId = ?";
        
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, qrCode);
            
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    String userType = resultSet.getString("tipo_utente");
                    if (userType.equals("Premium")) {
                        // Query per controllare se l'utente ha una prenotazione attiva e id_veicolo non è null
                        query = "SELECT id_prenotazione, id_posto_auto FROM Prenotazioni " +
                                "WHERE id_utente = ? " +
                                "AND stato = 'attiva' " +
                                "AND tempo_arrivo <= ? " +
                                "AND ? <= tempo_arrivo + interval '15 minutes' " +
                                "AND id_veicolo IS NULL";
                        
                        try (PreparedStatement statement2 = connection.prepareStatement(query)) {
                            statement2.setString(1, qrCode);
                            statement2.setObject(2, Timestamp.valueOf(currentTime)); // currentTime
                            statement2.setObject(3, Timestamp.valueOf(currentTime)); // currentTime
                            
                            try (ResultSet resultSet2 = statement2.executeQuery()) {
                                if (resultSet2.next()) {
                                    int idPrenotazione = resultSet2.getInt("id_prenotazione");
                                    int idPostoAuto = resultSet2.getInt("id_posto_auto");
                                    return new int[] {idPrenotazione, idPostoAuto};
                                } else {
                                    return new int[] {-1, -1}; // Nessuna prenotazione attiva trovata
                                }
                            }
                        }
                    } else {
                        return new int[] {-1, -1}; // Utente non premium
                    }
                } else {
                    return new int[] {-1, -1}; // Utente non trovato
                }
            }
        }
    }    

    public void InsertVeicoloInPrenotazione(int idPrenotazione, int idVeicolo) throws SQLException {
        // SQL query to update the reservation with the given vehicle ID
        String query = "UPDATE Prenotazioni SET id_veicolo = ? WHERE id_prenotazione = ?";
    
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, idVeicolo);
            statement.setInt(2, idPrenotazione);
    
            int rowsAffected = statement.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("No reservation found with ID " + idPrenotazione + " or unable to update the reservation with the vehicle ID.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SQLException("Error associating vehicle with reservation ID " + idPrenotazione + ": " + e.getMessage(), e);
        }
    }       

    public void deleteRichiestaRicarica(String qrCode, int idVeicolo) throws SQLException {
        String query = "DELETE FROM Ricariche WHERE id_utente = ? AND id_veicolo = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, qrCode);
            statement.setInt(2, idVeicolo);
            statement.executeUpdate();
        }
    }
    
    public void setVeicolo(int idVeicolo) throws SQLException {
        String query = "UPDATE Veicoli SET dentroParcheggio = false WHERE id_veicolo = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, idVeicolo);
            int affectedRows = statement.executeUpdate();
            
            if (affectedRows == 0) {
                // Se nessuna riga è stata aggiornata, l'idVeicolo potrebbe non esistere
                throw new SQLException("Nessun veicolo trovato con id: " + idVeicolo);
            }
        }
    }
    
    public void processActiveReservation(int idPostoAuto) throws SQLException {
        // Query per verificare se esiste una prenotazione attiva per il posto auto
        String queryCheck = "SELECT id_prenotazione FROM Prenotazioni WHERE id_posto_auto = ? AND stato = 'attiva'";
        
        // Query per aggiornare lo stato della prenotazione a 'pagata' e impostare id_veicolo a NULL
        String queryUpdate = "UPDATE Prenotazioni SET stato = 'pagata' WHERE id_posto_auto = ? AND stato = 'attiva'";

        try (PreparedStatement statementCheck = connection.prepareStatement(queryCheck);
            PreparedStatement statementUpdate = connection.prepareStatement(queryUpdate)) {
            
            // Verifica se esiste una prenotazione attiva
            statementCheck.setInt(1, idPostoAuto);
            try (ResultSet resultSet = statementCheck.executeQuery()) {
                if (resultSet.next()) {
                    // Se una prenotazione attiva esiste, aggiorna lo stato e imposta id_veicolo a NULL
                    statementUpdate.setInt(1, idPostoAuto);
                    int rowsAffected = statementUpdate.executeUpdate();
                    
                    // Verifica se l'aggiornamento è andato a buon fine
                    if (rowsAffected == 0) {
                        throw new SQLException("Impossibile aggiornare lo stato della prenotazione.");
                    }
                }
            }
        }
    }  

    public int getIdPostoAutoByIdVeicolo(int idVeicolo) throws SQLException {
        String query = "SELECT id_posto_auto FROM Parcheggio WHERE id_veicolo = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, idVeicolo);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("id_posto_auto");
                } else {
                    return -1; // ID non valido
                }
            }
        }
    }

    public int TrovaIdPostoAuto(String qrcode) throws SQLException {
        String query = "SELECT id_posto FROM Veicoli WHERE id_utente = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, qrcode);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("id_veicolo");
                }
            }
        }
        return -1;
    }
    
    public int getFirstAvailableParkingSpot() throws SQLException {
        String query = "SELECT id_posto_auto FROM Parcheggio " +
                       "WHERE occupato = false AND riservato = false " +
                       "LIMIT 1";
    
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("id_posto_auto");
                } else {
                    return -1; // No available parking spots found
                }
            }
        }
    }

    public int getRicaricaByQrCodeAndIdVeicolo(String qrcode, int idVeicolo) throws SQLException {
        int durata = -1;

        String query = "SELECT durataricarica FROM ricariche WHERE id_utente = ? AND id_veicolo = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, qrcode);
            stmt.setInt(2, idVeicolo);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    durata = rs.getInt("durataRicarica");
                }
            }
        } catch (SQLException e) {
            throw new SQLException("Errore durante la lettura delle ricariche");
        }

        return durata;
    }

    public void updateParcheggioOccupato(int idPostoAuto, boolean occupato, int idVeicolo, String qrCode) throws SQLException {
        String query = "UPDATE Parcheggio SET occupato = ?, id_veicolo = ?, id_utente = ? WHERE id_posto_auto = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setBoolean(1, occupato);
            statement.setInt(2, idVeicolo);
            statement.setString(3, qrCode);
            statement.setInt(4, idPostoAuto);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw e;
        }
    }   
    
    public void updateParcheggioLibero(int idPostoAuto, boolean occupato) throws SQLException {
        String query = "UPDATE Parcheggio SET occupato = ?, id_veicolo = ?, id_utente = ? WHERE id_posto_auto = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setBoolean(1, occupato);
            
            // Setta id_veicolo e id_utente a null
            statement.setNull(2, java.sql.Types.INTEGER);
            statement.setNull(3, java.sql.Types.VARCHAR);
            
            statement.setInt(4, idPostoAuto);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw e;
        }
    }     
    
    public int createVeicolo(String targa, String qrCode, LocalDateTime dataIngresso) throws SQLException {
        String query = "INSERT INTO Veicoli (targa, id_utente, dentroParcheggio, data_ingresso) VALUES (?,?,?,?) RETURNING id_veicolo";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, targa);
            statement.setString(2, qrCode);
            statement.setBoolean(3, true); // dentroParcheggio = true
            statement.setTimestamp(4, Timestamp.valueOf(dataIngresso)); // data_ingresso
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("id_veicolo"); // returns the generated serial key
                } else {
                    throw new SQLException("Failed to retrieve generated key");
                }
            }
        }
    }

    public Parcheggio getParcheggioById(int id) throws SQLException {
        String query = "SELECT * FROM Parcheggio WHERE id_posto_auto = ?";
        
        Parcheggio parcheggio = null;
        
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, id);
            
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    parcheggio = new Parcheggio();
                    parcheggio.setId_parcheggio(resultSet.getInt("id_posto_auto"));
                    parcheggio.setOccupato(resultSet.getBoolean("occupato"));
                    parcheggio.setRiservato(resultSet.getBoolean("riservato"));
                    Integer idVeicolo = resultSet.getInt("id_veicolo");
                    parcheggio.setId_veicolo(idVeicolo != null ? idVeicolo.intValue() : null);
                    String idUtente = resultSet.getString("id_utente");
                    parcheggio.setId_utente(idUtente);
                }
            }
        } catch (SQLException e) {
            throw new SQLException("Errore durante l'esecuzione della query", e);
        }
        
        return parcheggio;
    }

    public LocalDateTime getDataOraIngresso(String targa) throws SQLException {
        String query = "SELECT data_ingresso FROM veicoli WHERE targa = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, targa);

            try (ResultSet resultSet = stmt.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getTimestamp("data_ingresso").toLocalDateTime();
                } else {
                    return null;
                }
            }
        }
    }
}