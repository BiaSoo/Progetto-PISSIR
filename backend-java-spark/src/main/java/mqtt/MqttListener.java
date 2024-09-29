package mqtt;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MqttListener implements MqttCallback, Runnable {

    private static final Logger logger = Logger.getLogger(MqttListener.class.getName());
    private MqttClient client;
    private String brokerUrl;
    private String[] topics;
    private CloseableHttpClient httpClient;

    public MqttListener(String brokerUrl, String[] topics) {
        this.brokerUrl = brokerUrl;
        this.topics = topics;
        this.httpClient = HttpClients.createDefault();
    }

    @Override
    public void run() {
        try {
            client = new MqttClient(brokerUrl, MqttClient.generateClientId());
            client.setCallback(this);
            client.connect();

            for (String topic : topics) {
                client.subscribe(topic);
                logger.info("Sottoscritto al topic " + topic);
            }

            logger.info("Connessione al broker " + brokerUrl + " stabilita.");
        } catch (MqttException e) {
            logger.log(Level.SEVERE, "Errore nella connessione al broker MQTT: " + e.getMessage(), e);
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        logger.log(Level.WARNING, "Connessione al broker MQTT persa.", cause);
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        String payload = new String(message.getPayload());
        logger.info("Messaggio ricevuto sul topic " + topic + ": " + payload);
    
        switch (topic) {
            case "parcheggio/verifica_ingresso":
                handleVerificaIngresso(payload);
                break;
            case "parcheggio/verifica_uscita":
                handleVerificaUscita(payload);
                break;
            case "parcheggio/richiesta_tutti_id_posto_auto":          
                handleRichiestaTuttiIdPostoAuto();
                break;
            case "parcheggio/sensore_occupazione":
                handleInviaDatiSensore(payload);
                break;
            case "parcheggio/richiesta_tutte_prenotazioni_attive":
                handleRichiestaPrenotazioniAttive();
                break;
            case "parcheggio/multe":
                handleMulte(payload);
                break;
            case "parcheggio/richiesta_tutte_prenotazioni_attive_auto_dentro":
                handleRIchiestaUsciteAutomatiche();
                break;
            case "parcheggio/richiesta_ricariche":
                heandleRichiestaRicariche();
                break;
            case "parcheggio/aggiornamento_ricarica":
                handleAggiornamentoRicarica(payload);
                break;
            case "parcheggio/ricarica_effettuata":
                handleRicaricaEffettuata(payload);
                break;
            default:
                logger.warning("Topic sconosciuto: " + topic);
                break;
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // Non utilizzato per il listener
    }

    private void handleRichiestaTuttiIdPostoAuto() {
        try {
            // Recupera la lista di ID dei posti auto disponibili
            String jsonResponse = makeApiGetCall("http://localhost:8080/api/all_parking_spots");
            publishMqttMessage("parcheggio/richiesta_tutti_id_posto_auto/risposta", jsonResponse);
        } catch (IOException | MqttException e) {
            logger.log(Level.SEVERE, "Errore durante la gestione della richiesta ID posti auto dal simulatore.", e);
        }
    }

    private void handleRIchiestaUsciteAutomatiche() {
        try {
            logger.log(Level.INFO, "BEFORE sending reply uscite automatiche");
            // Recupera la lista di ID dei posti auto disponibili
            String jsonResponse = makeApiGetCall("http://localhost:8080/api/all_parking_spots_active_with_vehicle");
            publishMqttMessage("parcheggio/richiesta_uscite_automatiche/risposta", jsonResponse);
            logger.log(Level.INFO, "SENT reply uscite automatiche: " + jsonResponse);
        } catch (IOException | MqttException e) {
            logger.log(Level.SEVERE, "Errore durante la gestione della richiesta ID posti auto dal simulatore.", e);
        }
    }

    private void handleRichiestaPrenotazioniAttive() {
        try {
            // Recupera la lista di di tutte le prenotazioni attive
            String jsonResponse = makeApiGetCall("http://localhost:8080/api/all_active_reservations");
            publishMqttMessage("parcheggio/richiesta_tutte_prenotazioni_attive/risposta", jsonResponse);
        } catch (IOException | MqttException e) {
            logger.log(Level.SEVERE, "Errore durante la gestione della richiesta di tutte le prenotazioni attive.", e);
        }   
    }

    private void handleVerificaIngresso(String payload) { //verificato in simulatore + verificato in routehandler
        logger.info("Gestendo messaggio di verifica ingresso: " + payload);
        String url = "http://localhost:8080/api/verifica_ingresso";
        try {
            // Invia il payload direttamente all'API REST
            String jsonResponse = makeApiPostCall(url, payload);
            publishMqttMessage("parcheggio/verifica_ingresso/risposta", jsonResponse);
        } catch (IOException | MqttException e) {
            logger.log(Level.SEVERE, "Errore durante la gestione della verifica ingresso: " + e.getMessage(), e);
        }
    }

    private void handleVerificaUscita(String payload) { //verificato in simulatore + verificato in routehandler
        logger.info("Gestendo messaggio di verifica uscita: " + payload);
        String url = "http://localhost:8080/api/verifica_uscita";
        try {
            // Invia il payload direttamente all'API REST
            String jsonResponse = makeApiPostCall(url, payload);
            publishMqttMessage("parcheggio/verifica_uscita/risposta", jsonResponse);
        } catch (IOException | MqttException e) {
            logger.log(Level.SEVERE, "Errore durante la gestione della verifica uscita: " + e.getMessage(), e);
        }
    }

    private void handleInviaDatiSensore(String payload) {//verificato in simulatore + verificato in routehandler
        logger.info("Gestendo messaggio di invio dati sensore: " + payload);
        String url = "http://localhost:8080/api/sensore_occupazione";
        try {
            String jsonResponse = makeApiPostCall(url, payload);
            publishMqttMessage("parcheggio/sensore_occupazione/risposta", jsonResponse);
        } catch (IOException | MqttException e) {
            logger.log(Level.SEVERE, "Errore durante la gestione dell'invio dati sensore: " + e.getMessage(), e);
        }
    }

    private void handleMulte(String payload) {
        logger.info("Gestendo messaggio di multe: " + payload);
        String url = "http://localhost:8080/api/multe";
        try {
            // Invia il payload direttamente all'API REST
            String jsonResponse = makeApiPostCall(url, payload);
            publishMqttMessage("parcheggio/multe/risposta", jsonResponse);
        } catch (IOException | MqttException e) {
            logger.log(Level.SEVERE, "Errore durante la gestione delle multe: " + e.getMessage(), e);
        }
    }

    private void handleAggiornamentoRicarica(String payload) {//verificato in simulatore + verificato in routehandler
        logger.info("Gestendo messaggio di aggiornamento ricarica: " + payload);
        String url = "http://localhost:8080/api/aggiornamento_ricarica";
        try {
            String jsonResponse = makeApiPostCall(url, payload);
            publishMqttMessage("parcheggio/presa_in_carico_bot", jsonResponse);
        } catch (IOException | MqttException e) {
            logger.log(Level.SEVERE, "Errore durante la gestione dell'aggiornamento ricarica: " + e.getMessage(), e);
        }
    }

    private void heandleRichiestaRicariche() {
        try {
            // Recupera la lista di di tutte le prenotazioni attive
            String jsonResponse = makeApiGetCall("http://localhost:8080/api/ricariche");
            publishMqttMessage("parcheggio/richiesta_ricariche/risposta", jsonResponse);
        } catch (IOException | MqttException e) {
            logger.log(Level.SEVERE, "Errore durante la gestione della richiesta di tutte le prenotazioni attive.", e);
        }   
    }

    private void handleRicaricaEffettuata(String payload) {//verificato in simulatore + verificato in routehandler
        logger.info("Gestendo messaggio di aggiornamento ricarica: " + payload);
        String url = "http://localhost:8080/api/ricarica_effettuata";
        try {
            makeApiPostCall(url, payload);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Errore durante la gestione dell'aggiornamento ricarica: " + e.getMessage(), e);
        }
    }

    private String makeApiGetCall(String url) throws IOException {
        HttpGet httpGet = new HttpGet(url);

        try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
            // Recupera il codice di stato HTTP
            int statusCode = response.getStatusLine().getStatusCode();

            // Leggi il contenuto della risposta
            String responseBody = EntityUtils.toString(response.getEntity());

            if (statusCode == 200) {
                // Codice di stato 200 OK, restituisci il corpo della risposta
                return responseBody;
            } else {
                // Codice di stato diverso da 200 OK, logga l'errore e restituisci un messaggio di errore
                logger.log(Level.WARNING, "Errore nella richiesta API: Codice di stato " + statusCode);
                return "{\"message\": \"Errore nella richiesta API: Codice di stato " + statusCode + "\"}";
            }
        }
    }

    private String makeApiPostCall(String url, String payload) throws IOException {
        HttpPost httpPost = new HttpPost(url);
        httpPost.setEntity(new StringEntity(payload));
        httpPost.setHeader("Content-type", "application/json");

        try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
            return EntityUtils.toString(response.getEntity());
        }
    }

    private void publishMqttMessage(String topic, String message) throws MqttException {
        MqttMessage mqttMessage = new MqttMessage(message.getBytes());
        client.publish(topic, mqttMessage);
    }
}

